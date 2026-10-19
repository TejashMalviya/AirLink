package org.offlinemesh.discovery

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.offlinemesh.identity.IdentityManager
import org.offlinemesh.transport.ITransport
import org.offlinemesh.transport.MeshPacket
import org.offlinemesh.transport.TransportPacketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class NearbyConnectionsManager(
    private val context: Context,
    private val identityManager: IdentityManager
) : ITransport {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val listeners = CopyOnWriteArrayList<TransportPacketListener>()

    // Maps connected endpointId -> Remote Peer Display Name
    private val connectedEndpoints = ConcurrentHashMap<String, String>()

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    private val _activeNeighborsCount = MutableStateFlow(0)
    val activeNeighborsCount: StateFlow<Int> = _activeNeighborsCount.asStateFlow()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Auto-accept connection for zero-click mesh peering
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            connectedEndpoints[endpointId] = connectionInfo.endpointName
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                val peerName = connectedEndpoints[endpointId] ?: "Nearby Peer"
                _activeNeighborsCount.value = connectedEndpoints.size
                listeners.forEach { it.onNeighborConnected(endpointId, peerName) }
            } else {
                connectedEndpoints.remove(endpointId)
                _activeNeighborsCount.value = connectedEndpoints.size
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            _activeNeighborsCount.value = connectedEndpoints.size
            listeners.forEach { it.onNeighborDisconnected(endpointId) }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (!connectedEndpoints.containsKey(endpointId)) {
                val myName = "${identityManager.currentIdentity.deviceId}:${identityManager.currentIdentity.displayName}"
                connectionsClient.requestConnection(
                    myName,
                    endpointId,
                    connectionLifecycleCallback
                )
            }
        }

        override fun onEndpointLost(endpointId: String) {
            // Endpoint lost from scanning range
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val packet = MeshPacket.fromBytes(bytes) ?: return
                listeners.forEach { it.onPacketReceived(packet, endpointId) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Payload transfer status update
        }
    }

    fun startMesh() {
        if (_isMeshActive.value) return
        val myIdentity = identityManager.currentIdentity
        val localEndpointName = "${myIdentity.deviceId}:${myIdentity.displayName}"

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startAdvertising(
            localEndpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            _isMeshActive.value = true
        }.addOnFailureListener {
            // Log or handle advertising failure
        }

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            _isMeshActive.value = true
        }.addOnFailureListener {
            // Log or handle discovery failure
        }
    }

    fun stopMesh() {
        try {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
        } catch (ignored: Exception) {}
        connectedEndpoints.clear()
        _activeNeighborsCount.value = 0
        _isMeshActive.value = false
    }

    override fun registerListener(listener: TransportPacketListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun unregisterListener(listener: TransportPacketListener) {
        listeners.remove(listener)
    }

    override fun sendPacket(packet: MeshPacket, endpointId: String): Boolean {
        return try {
            val payload = Payload.fromBytes(packet.toBytes())
            connectionsClient.sendPayload(endpointId, payload)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun broadcastPacket(packet: MeshPacket): Int {
        var count = 0
        val payloadBytes = packet.toBytes()
        connectedEndpoints.keys.forEach { ep ->
            try {
                connectionsClient.sendPayload(ep, Payload.fromBytes(payloadBytes))
                count++
            } catch (ignored: Exception) {}
        }
        return count
    }

    override fun getConnectedEndpoints(): List<String> {
        return connectedEndpoints.keys.toList()
    }

    fun getConnectedEndpointNames(): Map<String, String> {
        return connectedEndpoints.toMap()
    }

    companion object {
        const val SERVICE_ID = "org.offlinemesh.calling"
    }
}
