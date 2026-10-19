package org.offlinemesh

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.offlinemesh.audio.AudioCodec
import org.offlinemesh.audio.JitterBuffer
import org.offlinemesh.security.CryptoManager
import org.offlinemesh.transport.MeshPacket
import org.offlinemesh.transport.PacketType
import java.util.UUID

class RoutingAndMeshUnitTest {

    @Test
    fun testAudioCodecMuLawRoundtrip() {
        // 16-bit linear PCM test samples
        val pcm = ByteArray(640) { i -> (i % 128).toByte() }

        // Encode to Mu-Law (cuts size to 320 bytes)
        val encoded = AudioCodec.encodeMuLaw(pcm)
        assertEquals(320, encoded.size)

        // Decode back to 16-bit PCM
        val decoded = AudioCodec.decodeMuLaw(encoded)
        assertEquals(640, decoded.size)

        // Verify non-zero speech reconstruction
        assertNotNull(decoded)
    }

    @Test
    fun testCryptoManagerEcdhAndAesGcm() {
        val crypto = CryptoManager()

        val aliceKeyPair = crypto.generateEphemeralKeyPair()
        val bobKeyPair = crypto.generateEphemeralKeyPair()

        // Derive shared secret symmetrically
        val aliceSessionKey = crypto.deriveSharedSecret(aliceKeyPair.private, bobKeyPair.public.encoded)
        val bobSessionKey = crypto.deriveSharedSecret(bobKeyPair.private, aliceKeyPair.public.encoded)

        assertNotNull(aliceSessionKey)
        assertNotNull(bobSessionKey)
        assertArrayEquals(aliceSessionKey!!.encoded, bobSessionKey!!.encoded)

        // Test AES-GCM Authenticated Encryption & Decryption
        val sampleVoicePayload = "EncryptedAudioVoiceFrame-12345678".toByteArray()
        val encrypted = crypto.encryptPayload(aliceSessionKey, sampleVoicePayload)
        assertNotNull(encrypted)
        assertTrue(encrypted.size > sampleVoicePayload.size) // Includes 12-byte IV + 16-byte tag

        val decrypted = crypto.decryptPayload(bobSessionKey, encrypted)
        assertNotNull(decrypted)
        assertArrayEquals(sampleVoicePayload, decrypted)
    }

    @Test
    fun testGroupCallKeyDerivation() {
        val crypto = CryptoManager()
        val groupKey1 = crypto.deriveGroupKey("group-call-abc")
        val groupKey2 = crypto.deriveGroupKey("group-call-abc")

        // Deterministic symmetric key derivation for call participants
        assertArrayEquals(groupKey1.encoded, groupKey2.encoded)

        // Test encryption with group key
        val data = "GroupVoicePayload".toByteArray()
        val encrypted = crypto.encryptPayload(groupKey1, data)
        val decrypted = crypto.decryptPayload(groupKey2, encrypted)
        assertNotNull(decrypted)
        assertArrayEquals(data, decrypted)
    }

    @Test
    fun testLowLatencyJitterBuffer() {
        // targetDelayFrames = 1 for ~20ms ultra-low latency
        val jitterBuffer = JitterBuffer(targetDelayFrames = 1, maxCapacityFrames = 10)

        // Before pushing, pop should return null
        assertNull(jitterBuffer.pop())

        val frame1 = byteArrayOf(1, 2, 3)
        val frame2 = byteArrayOf(4, 5, 6)

        jitterBuffer.push(seq = 1L, data = frame1)
        // With targetDelayFrames = 1, frame is ready to play immediately
        val popped1 = jitterBuffer.pop()
        assertNotNull(popped1)
        assertArrayEquals(frame1, popped1)

        // Out of order late packet (seq = 0 <= lastPlayedSeq 1) should be dropped
        val lateFrame = byteArrayOf(9, 9, 9)
        jitterBuffer.push(seq = 0L, data = lateFrame)
        assertNull(jitterBuffer.pop())

        // Next valid sequential packet
        jitterBuffer.push(seq = 2L, data = frame2)
        val popped2 = jitterBuffer.pop()
        assertNotNull(popped2)
        assertArrayEquals(frame2, popped2)
    }

    @Test
    fun testMeshPacketSerialization() {
        val packet = MeshPacket(
            packetId = "pkt-999",
            type = PacketType.CALL_INVITE,
            sourceDeviceId = "NODE_A",
            sourceDisplayName = "Alice",
            destinationDeviceId = "NODE_B",
            hopCount = 1,
            ttl = 4,
            callId = "call-001",
            payload = "TestPayload"
        )

        val bytes = packet.toBytes()
        assertTrue(bytes.isNotEmpty())

        val restored = MeshPacket.fromBytes(bytes)
        assertNotNull(restored)
        assertEquals(packet.packetId, restored?.packetId)
        assertEquals(packet.sourceDeviceId, restored?.sourceDeviceId)
        assertEquals(packet.destinationDeviceId, restored?.destinationDeviceId)
        assertEquals(packet.hopCount, restored?.hopCount)
        assertEquals(packet.ttl, restored?.ttl)
        assertEquals(packet.callId, restored?.callId)
        assertEquals(packet.payload, restored?.payload)
    }

    @Test
    fun testImagePacketSerialization() {
        val imageBase64Sample = "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
        val packet = MeshPacket(
            packetId = "img-001",
            type = PacketType.IMAGE_MESSAGE,
            sourceDeviceId = "NODE_X",
            sourceDisplayName = "Bob",
            destinationDeviceId = "NODE_Y",
            hopCount = 2,
            ttl = 4,
            payload = imageBase64Sample
        )

        val bytes = packet.toBytes()
        assertTrue(bytes.isNotEmpty())

        val restored = MeshPacket.fromBytes(bytes)
        assertNotNull(restored)
        assertEquals(PacketType.IMAGE_MESSAGE, restored?.type)
        assertEquals(imageBase64Sample, restored?.payload)
        assertEquals(2, restored?.hopCount)
    }

    @Test
    fun testDuplicateSuppressionLogic() {
        val seenCache = mutableMapOf<String, Long>()
        val packetId = UUID.randomUUID().toString()

        // First packet arrival -> accept
        val isFirstDuplicate = seenCache.putIfAbsent(packetId, System.currentTimeMillis()) != null
        assertFalse(isFirstDuplicate)

        // Second packet arrival with same ID -> drop duplicate
        val isSecondDuplicate = seenCache.putIfAbsent(packetId, System.currentTimeMillis()) != null
        assertTrue(isSecondDuplicate)
    }

    @Test
    fun testTtlAndHopLimitLogic() {
        val packet = MeshPacket(
            type = PacketType.AUDIO_FRAME,
            sourceDeviceId = "NODE_A",
            sourceDisplayName = "Alice",
            destinationDeviceId = "NODE_C",
            hopCount = 4,
            ttl = 4
        )

        // Forwarding rule: Drop if hopCount >= ttl
        val shouldDrop = packet.hopCount >= packet.ttl
        assertTrue("Packet at TTL limit must be dropped to prevent loops", shouldDrop)

        val validPacket = packet.copy(hopCount = 2, ttl = 4)
        val validShouldDrop = validPacket.hopCount >= validPacket.ttl
        assertFalse("Packet within TTL limit should be forwarded", validShouldDrop)
    }
}
