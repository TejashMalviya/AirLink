package org.offlinemesh.mesh

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.offlinemesh.identity.IdentityManager
import org.offlinemesh.transport.ITransport
import org.offlinemesh.transport.MeshPacket
import org.offlinemesh.transport.PacketType
import org.offlinemesh.transport.TransportPacketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class RouteEntry(
    val destinationDeviceId: String,
    val nextHopEndpointId: String,
    val hopCount: Int,
    val expiryTimestamp: Long
)

data class MeshNeighbor(
    val endpointId: String,          // Next-hop endpoint ID to reach this peer
    val deviceId: String,            // Unique stable device ID
    val displayName: String,         // User chosen display name
    val hopCount: Int = 1,           // 1 = direct 1-hop physical neighbor, 2+ = multi-hop mesh peer
    val viaDisplayName: String = "Direct",
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class MeshDiagnostics(
    val packetsSent: Long = 0,
    val packetsReceived: Long = 0,
    val packetsRelayed: Long = 0,
    val duplicatesDropped: Long = 0,
    val activeRoutesCount: Int = 0,
    val connectedNeighborsCount: Int = 0
)

interface MeshPacketDeliveryCallback {
    fun onLocalPacketDelivered(packet: MeshPacket)
    fun onNeighborTableChanged(neighbors: List<MeshNeighbor>)
    fun onRouteDiscovered(destinationDeviceId: String, hopCount: Int)
}

class MeshRouter(
    private val identityManager: IdentityManager,
    private val transport: ITransport
) : TransportPacketListener {

    private val deliveryCallbacks = CopyOnWriteArrayList<MeshPacketDeliveryCallback>()

    // Direct physical neighbors: endpointId -> MeshNeighbor (hopCount = 1)
    private val physicalNeighbors = ConcurrentHashMap<String, MeshNeighbor>()

    // Known peers table (both direct 1-hop and multi-hop 2+): deviceId -> MeshNeighbor
    private val allMeshPeers = ConcurrentHashMap<String, MeshNeighbor>()

    // Routing table: destinationDeviceId -> RouteEntry
    private val routingTable = ConcurrentHashMap<String, RouteEntry>()

    // Duplicate suppression cache: packetId -> timestamp
    private val seenPacketCache = ConcurrentHashMap<String, Long>()

    private val _diagnostics = MutableStateFlow(MeshDiagnostics())
    val diagnostics: StateFlow<MeshDiagnostics> = _diagnostics.asStateFlow()

    private val _knownPeers = MutableStateFlow<List<MeshNeighbor>>(emptyList())
    val knownPeers: StateFlow<List<MeshNeighbor>> = _knownPeers.asStateFlow()

    private val routerScope = CoroutineScope(Dispatchers.IO)

    init {
        transport.registerListener(this)

        // Periodic background maintenance: clean expired cache and route entries every 10 seconds
        // (Avoids doing expensive map operations on every audio packet at 50fps!)
        routerScope.launch {
            while (isActive) {
                delay(10_000)
                cleanExpiredCache()
                // Periodic HELLO broadcast to keep mesh routes fresh
                if (physicalNeighbors.isNotEmpty()) {
                    broadcastHello()
                }
            }
        }
    }

    fun addDeliveryCallback(callback: MeshPacketDeliveryCallback) {
        if (!deliveryCallbacks.contains(callback)) {
            deliveryCallbacks.add(callback)
        }
    }

    fun removeDeliveryCallback(callback: MeshPacketDeliveryCallback) {
        deliveryCallbacks.remove(callback)
    }

    /**
     * Send packet through the mesh.
     * 1. If destination is direct neighbor, send directly to endpoint.
     * 2. If destination is in routing table, send to next-hop endpoint.
     * 3. If route unknown or broadcast, broadcast to neighbors.
     */
    fun sendOrRoute(packet: MeshPacket): Boolean {
        recordSent()

        // Cache own packet to prevent looping back
        seenPacketCache[packet.packetId] = System.currentTimeMillis()

        if (packet.destinationDeviceId == MeshPacket.BROADCAST_DESTINATION) {
            transport.broadcastPacket(packet)
            return true
        }

        // 1. Direct neighbor check
        val direct = physicalNeighbors.values.find { it.deviceId == packet.destinationDeviceId }
        if (direct != null) {
            return transport.sendPacket(packet, direct.endpointId)
        }

        // 2. Next-hop route lookup
        val route = routingTable[packet.destinationDeviceId]
        if (route != null && System.currentTimeMillis() < route.expiryTimestamp) {
            return transport.sendPacket(packet, route.nextHopEndpointId)
        }

        // 3. Unknown route: initiate route request and flood to locate destination
        discoverRoute(packet.destinationDeviceId)
        transport.broadcastPacket(packet)
        return true
    }

    fun broadcastHello() {
        val myId = identityManager.currentIdentity
        val helloPacket = MeshPacket(
            type = PacketType.PEER_HELLO,
            sourceDeviceId = myId.deviceId,
            sourceDisplayName = myId.displayName,
            destinationDeviceId = MeshPacket.BROADCAST_DESTINATION,
            hopCount = 0,
            ttl = MAX_TTL,
            payload = myId.fingerprint
        )
        sendOrRoute(helloPacket)
    }

    fun discoverRoute(targetDeviceId: String) {
        val myId = identityManager.currentIdentity
        val rreq = MeshPacket(
            type = PacketType.ROUTE_REQUEST,
            sourceDeviceId = myId.deviceId,
            sourceDisplayName = myId.displayName,
            destinationDeviceId = MeshPacket.BROADCAST_DESTINATION,
            hopCount = 0,
            ttl = MAX_TTL,
            payload = targetDeviceId
        )
        // Send RREQ to all physical neighbors
        transport.broadcastPacket(rreq)
    }

    override fun onPacketReceived(packet: MeshPacket, fromEndpointId: String) {
        recordReceived()

        // 1. Loop detection & duplicate suppression (O(1) memory lookup)
        if (seenPacketCache.putIfAbsent(packet.packetId, System.currentTimeMillis()) != null) {
            recordDuplicateDropped()
            return
        }

        // Drop if TTL exceeded
        if (packet.hopCount >= packet.ttl) {
            return
        }

        val myDeviceId = identityManager.currentIdentity.deviceId
        if (packet.sourceDeviceId == myDeviceId) {
            return // Ignore our own looped packet
        }

        val learnedHop = packet.hopCount + 1

        // 2. Dynamic Distance-Vector Route Learning
        // Learn reverse route to the original sender via fromEndpointId
        learnRoute(
            deviceId = packet.sourceDeviceId,
            displayName = packet.sourceDisplayName,
            nextHopEndpointId = fromEndpointId,
            hopCount = learnedHop
        )

        // 3. Process packet locally if destined for us or broadcast
        val isForMe = packet.destinationDeviceId == myDeviceId
        val isBroadcast = packet.destinationDeviceId == MeshPacket.BROADCAST_DESTINATION

        if (isForMe || isBroadcast) {
            handlePacketLocally(packet, fromEndpointId)
        }

        // 4. Multi-hop Forwarding (if not for me, or broadcast that still has TTL)
        if (!isForMe && identityManager.isRelayEnabled) {
            forwardPacket(packet, fromEndpointId)
        }
    }

    private fun handlePacketLocally(packet: MeshPacket, fromEndpointId: String) {
        when (packet.type) {
            PacketType.PEER_HELLO -> {
                // Remote peer announced itself (either 1 hop away or relayed across multiple hops)
                val directNeighbor = physicalNeighbors[fromEndpointId]
                val viaName = if (packet.hopCount == 0) "Direct" else (directNeighbor?.displayName ?: "Relay")

                val peer = MeshNeighbor(
                    endpointId = fromEndpointId,
                    deviceId = packet.sourceDeviceId,
                    displayName = packet.sourceDisplayName,
                    hopCount = packet.hopCount + 1,
                    viaDisplayName = viaName
                )
                allMeshPeers[packet.sourceDeviceId] = peer
                updateKnownPeers()
            }
            PacketType.ROUTE_REQUEST -> {
                val target = packet.payload
                val myDeviceId = identityManager.currentIdentity.deviceId
                if (target == myDeviceId) {
                    // Send Route Reply back along the reverse route
                    val rrep = MeshPacket(
                        type = PacketType.ROUTE_REPLY,
                        sourceDeviceId = myDeviceId,
                        sourceDisplayName = identityManager.currentIdentity.displayName,
                        destinationDeviceId = packet.sourceDeviceId,
                        hopCount = 0,
                        ttl = MAX_TTL,
                        payload = "1"
                    )
                    sendOrRoute(rrep)
                }
            }
            PacketType.ROUTE_REPLY -> {
                deliveryCallbacks.forEach { it.onRouteDiscovered(packet.sourceDeviceId, packet.hopCount) }
            }
            else -> {
                deliveryCallbacks.forEach { it.onLocalPacketDelivered(packet) }
            }
        }
    }

    /**
     * Smart forwarding with anti-echo protection:
     * - Never send the packet back to the endpoint it arrived from!
     * - If destination route is known, send ONLY to that specific next hop.
     * - If broadcast or unknown, flood to all neighbors EXCEPT fromEndpointId.
     */
    private fun forwardPacket(packet: MeshPacket, fromEndpointId: String) {
        val forwardedPacket = packet.copy(
            hopCount = packet.hopCount + 1
        )
        recordRelayed()

        if (packet.destinationDeviceId != MeshPacket.BROADCAST_DESTINATION) {
            val route = routingTable[packet.destinationDeviceId]
            if (route != null && System.currentTimeMillis() < route.expiryTimestamp) {
                // Send directly to the next hop if it's not the sender!
                if (route.nextHopEndpointId != fromEndpointId) {
                    transport.sendPacket(forwardedPacket, route.nextHopEndpointId)
                    return
                }
            }
        }

        // Flood to physical neighbors excluding the arrival endpoint (prevent radio echo!)
        physicalNeighbors.keys.forEach { ep ->
            if (ep != fromEndpointId) {
                transport.sendPacket(forwardedPacket, ep)
            }
        }
    }

    private fun learnRoute(
        deviceId: String,
        displayName: String,
        nextHopEndpointId: String,
        hopCount: Int
    ) {
        val existing = routingTable[deviceId]
        val isBetterOrNew = existing == null || hopCount <= existing.hopCount || System.currentTimeMillis() > existing.expiryTimestamp

        if (isBetterOrNew) {
            routingTable[deviceId] = RouteEntry(
                destinationDeviceId = deviceId,
                nextHopEndpointId = nextHopEndpointId,
                hopCount = hopCount,
                expiryTimestamp = System.currentTimeMillis() + ROUTE_LIFETIME_MS
            )

            // Update peer in mesh peer list
            val direct = physicalNeighbors[nextHopEndpointId]
            val via = if (hopCount == 1) "Direct" else "via ${direct?.displayName ?: "Relay"}"
            allMeshPeers[deviceId] = MeshNeighbor(
                endpointId = nextHopEndpointId,
                deviceId = deviceId,
                displayName = displayName,
                hopCount = hopCount,
                viaDisplayName = via
            )
            updateKnownPeers()
            updateDiagnostics()
        }
    }

    override fun onNeighborConnected(endpointId: String, peerName: String) {
        val parts = peerName.split(":")
        val peerDeviceId = if (parts.size >= 2) parts[0] else endpointId
        val peerDisplayName = if (parts.size >= 2) parts[1] else peerName

        val directPeer = MeshNeighbor(
            endpointId = endpointId,
            deviceId = peerDeviceId,
            displayName = peerDisplayName,
            hopCount = 1,
            viaDisplayName = "Direct"
        )
        physicalNeighbors[endpointId] = directPeer
        allMeshPeers[peerDeviceId] = directPeer

        learnRoute(peerDeviceId, peerDisplayName, endpointId, 1)
        updateKnownPeers()

        // Greet new neighbor with PEER_HELLO
        broadcastHello()
    }

    override fun onNeighborDisconnected(endpointId: String) {
        val disconnectedDirect = physicalNeighbors.remove(endpointId)
        if (disconnectedDirect != null) {
            allMeshPeers.remove(disconnectedDirect.deviceId)
        }

        // Invalidate all routes that used this disconnected endpoint as next-hop (Route Recovery)
        val routeIter = routingTable.entries.iterator()
        while (routeIter.hasNext()) {
            val entry = routeIter.next()
            if (entry.value.nextHopEndpointId == endpointId) {
                routeIter.remove()
                allMeshPeers.remove(entry.key)
            }
        }

        updateKnownPeers()
        updateDiagnostics()
    }

    private fun updateKnownPeers() {
        val list = allMeshPeers.values.toList().sortedBy { it.hopCount }
        _knownPeers.value = list
        deliveryCallbacks.forEach { it.onNeighborTableChanged(list) }
        updateDiagnostics()
    }

    private fun cleanExpiredCache() {
        val now = System.currentTimeMillis()
        if (seenPacketCache.size > MAX_CACHE_SIZE) {
            seenPacketCache.entries.removeIf { now - it.value > CACHE_EXPIRY_MS }
        }

        val expired = mutableListOf<String>()
        routingTable.entries.forEach { entry ->
            if (now > entry.value.expiryTimestamp) {
                expired.add(entry.key)
            }
        }
        expired.forEach { deviceId ->
            routingTable.remove(deviceId)
            if (!physicalNeighbors.values.any { it.deviceId == deviceId }) {
                allMeshPeers.remove(deviceId)
            }
        }
        if (expired.isNotEmpty()) {
            updateKnownPeers()
        }
    }

    private fun recordSent() {
        _diagnostics.value = _diagnostics.value.copy(
            packetsSent = _diagnostics.value.packetsSent + 1
        )
    }

    private fun recordReceived() {
        _diagnostics.value = _diagnostics.value.copy(
            packetsReceived = _diagnostics.value.packetsReceived + 1
        )
    }

    private fun recordRelayed() {
        _diagnostics.value = _diagnostics.value.copy(
            packetsRelayed = _diagnostics.value.packetsRelayed + 1
        )
    }

    private fun recordDuplicateDropped() {
        _diagnostics.value = _diagnostics.value.copy(
            duplicatesDropped = _diagnostics.value.duplicatesDropped + 1
        )
    }

    private fun updateDiagnostics() {
        _diagnostics.value = _diagnostics.value.copy(
            activeRoutesCount = routingTable.size,
            connectedNeighborsCount = physicalNeighbors.size
        )
    }

    companion object {
        const val MAX_TTL = 5
        const val ROUTE_LIFETIME_MS = 60_000L // 1 minute
        const val CACHE_EXPIRY_MS = 20_000L    // 20 seconds
        const val MAX_CACHE_SIZE = 1_000
    }
}
