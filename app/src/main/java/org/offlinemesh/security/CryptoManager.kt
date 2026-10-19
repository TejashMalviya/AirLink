package org.offlinemesh.security

import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class CryptoManager {

    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    fun deriveSharedSecret(myPrivateKey: PrivateKey, peerPublicKeyBytes: ByteArray): SecretKey? {
        return try {
            val kf = KeyFactory.getInstance("EC")
            val peerPubKey = kf.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(myPrivateKey)
            ka.doPhase(peerPubKey, true)
            val sharedBytes = ka.generateSecret()
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(sharedBytes)
            SecretKeySpec(keyBytes, "AES")
        } catch (e: Exception) {
            Log.e("CryptoManager", "ECDH key derivation failed", e)
            null
        }
    }

    /**
     * Derive a deterministic group session key from the callId.
     * SHA-256(callId bytes) → AES-256 key.
     * In a production system, use a proper group key agreement protocol.
     */
    fun deriveGroupKey(callId: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(callId.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptPayload(key: SecretKey, plaintext: ByteArray): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext   // prepend IV
    }

    fun decryptPayload(key: SecretKey, data: ByteArray): ByteArray? {
        return try {
            if (data.size < 13) return null
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            null
        }
    }
}
