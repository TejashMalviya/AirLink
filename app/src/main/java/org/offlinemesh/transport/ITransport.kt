package org.offlinemesh.transport

interface TransportPacketListener {
    fun onPacketReceived(packet: MeshPacket, fromEndpointId: String)
    fun onNeighborConnected(endpointId: String, peerName: String)
    fun onNeighborDisconnected(endpointId: String)
}

interface ITransport {
    fun registerListener(listener: TransportPacketListener)
    fun unregisterListener(listener: TransportPacketListener)
    fun sendPacket(packet: MeshPacket, endpointId: String): Boolean
    fun broadcastPacket(packet: MeshPacket): Int
    fun getConnectedEndpoints(): List<String>
}
