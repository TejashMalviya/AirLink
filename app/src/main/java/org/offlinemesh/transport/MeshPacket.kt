package org.offlinemesh.transport

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class PacketType {
    PEER_HELLO,
    ROUTE_REQUEST,
    ROUTE_REPLY,
    CALL_INVITE,
    CALL_RINGING,
    CALL_ACCEPT,
    CALL_REJECT,
    CALL_END,
    AUDIO_FRAME,
    CHAT_MESSAGE,
    IMAGE_MESSAGE
}

@Serializable
data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString().substring(0, 8),
    val type: PacketType,
    val sourceDeviceId: String,
    val sourceDisplayName: String,
    val destinationDeviceId: String, // "*" for broadcast
    val hopCount: Int = 0,
    val ttl: Int = 4,
    val timestamp: Long = System.currentTimeMillis(),
    val callId: String = "",
    val sequenceNumber: Long = 0L,
    val payload: String = "" // Base64 or plain string
) {
    fun toBytes(): ByteArray {
        return jsonConfig.encodeToString(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val BROADCAST_DESTINATION = "*"

        val jsonConfig = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromBytes(bytes: ByteArray): MeshPacket? {
            return try {
                val str = String(bytes, Charsets.UTF_8)
                jsonConfig.decodeFromString<MeshPacket>(str)
            } catch (e: Exception) {
                null
            }
        }
    }
}
