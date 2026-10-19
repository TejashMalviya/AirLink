package org.offlinemesh.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.offlinemesh.identity.IdentityManager
import org.offlinemesh.mesh.MeshNeighbor
import org.offlinemesh.mesh.MeshPacketDeliveryCallback
import org.offlinemesh.mesh.MeshRouter
import org.offlinemesh.transport.MeshPacket
import org.offlinemesh.transport.PacketType
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val text: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean
)

class ChatManager(
    private val identityManager: IdentityManager,
    private val meshRouter: MeshRouter
) : MeshPacketDeliveryCallback {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        meshRouter.addDeliveryCallback(this)
    }

    fun sendMessage(recipientDeviceId: String, text: String) {
        val myId = identityManager.currentIdentity
        val packet = MeshPacket(
            type = PacketType.CHAT_MESSAGE,
            sourceDeviceId = myId.deviceId,
            sourceDisplayName = myId.displayName,
            destinationDeviceId = recipientDeviceId,
            payload = text
        )

        val localMsg = ChatMessage(
            senderName = "Me",
            text = text,
            isMe = true
        )
        _messages.value = _messages.value + localMsg

        meshRouter.sendOrRoute(packet)
    }

    fun sendImage(recipientDeviceId: String, base64Image: String, caption: String = "") {
        val myId = identityManager.currentIdentity
        val packet = MeshPacket(
            type = PacketType.IMAGE_MESSAGE,
            sourceDeviceId = myId.deviceId,
            sourceDisplayName = myId.displayName,
            destinationDeviceId = recipientDeviceId,
            payload = base64Image
        )

        val localMsg = ChatMessage(
            senderName = "Me",
            text = caption,
            imageBase64 = base64Image,
            isMe = true
        )
        _messages.value = _messages.value + localMsg

        meshRouter.sendOrRoute(packet)
    }

    override fun onLocalPacketDelivered(packet: MeshPacket) {
        when (packet.type) {
            PacketType.CHAT_MESSAGE -> {
                val incoming = ChatMessage(
                    id = packet.packetId,
                    senderName = packet.sourceDisplayName,
                    text = packet.payload,
                    timestamp = packet.timestamp,
                    isMe = false
                )
                _messages.value = _messages.value + incoming
            }
            PacketType.IMAGE_MESSAGE -> {
                val incoming = ChatMessage(
                    id = packet.packetId,
                    senderName = packet.sourceDisplayName,
                    text = "",
                    imageBase64 = packet.payload,
                    timestamp = packet.timestamp,
                    isMe = false
                )
                _messages.value = _messages.value + incoming
            }
            else -> {}
        }
    }

    override fun onNeighborTableChanged(neighbors: List<MeshNeighbor>) {}
    override fun onRouteDiscovered(destinationDeviceId: String, hopCount: Int) {}
}
