package org.offlinemesh.identity

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.UUID

data class DeviceIdentity(
    val deviceId: String,
    val displayName: String,
    val publicKeyBytes: ByteArray,
    val fingerprint: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeviceIdentity
        return deviceId == other.deviceId
    }
    override fun hashCode(): Int = deviceId.hashCode()
}

class IdentityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var currentIdentity: DeviceIdentity
        private set

    var isRelayEnabled: Boolean
        get() = prefs.getBoolean(KEY_RELAY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_RELAY_ENABLED, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    /**
     * True if the user has set a custom display name (not the auto-generated default).
     * Used to decide whether to show the onboarding screen.
     */
    val hasCustomDisplayName: Boolean
        get() = prefs.getBoolean(KEY_HAS_CUSTOM_NAME, false)

    init {
        currentIdentity = loadOrCreateIdentity()
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim().ifEmpty { defaultDeviceName() }
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, trimmed)
            .putBoolean(KEY_HAS_CUSTOM_NAME, true)
            .apply()
        currentIdentity = currentIdentity.copy(displayName = trimmed)
    }

    private fun loadOrCreateIdentity(): DeviceIdentity {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        var displayName = prefs.getString(KEY_DISPLAY_NAME, null)
        val pubKeyHex = prefs.getString(KEY_PUBLIC_KEY, null)

        if (deviceId == null || pubKeyHex == null) {
            deviceId = UUID.randomUUID().toString().substring(0, 8).uppercase()
            displayName = displayName ?: defaultDeviceName()
            val keyPair = generateEcKeyPair()
            val pubBytes = keyPair.public.encoded
            val hex = pubBytes.toHexString()
            val fp = computeFingerprint(pubBytes)

            prefs.edit()
                .putString(KEY_DEVICE_ID, deviceId)
                .putString(KEY_DISPLAY_NAME, displayName)
                .putString(KEY_PUBLIC_KEY, hex)
                .putString(KEY_FINGERPRINT, fp)
                .apply()

            return DeviceIdentity(
                deviceId = deviceId,
                displayName = displayName,
                publicKeyBytes = pubBytes,
                fingerprint = fp
            )
        } else {
            val pubBytes = pubKeyHex.hexToByteArray()
            val fp = prefs.getString(KEY_FINGERPRINT, null) ?: computeFingerprint(pubBytes)
            return DeviceIdentity(
                deviceId = deviceId,
                displayName = displayName ?: defaultDeviceName(),
                publicKeyBytes = pubBytes,
                fingerprint = fp
            )
        }
    }

    private fun defaultDeviceName(): String {
        val model = Build.MODEL ?: "Android"
        return "AirLink-$model"
    }

    private fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    private fun computeFingerprint(publicKeyBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(publicKeyBytes)
        return digest.take(8).joinToString(":") { "%02X".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "airlink_identity_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PUBLIC_KEY = "public_key_hex"
        private const val KEY_FINGERPRINT = "fingerprint"
        private const val KEY_RELAY_ENABLED = "relay_enabled"
        private const val KEY_HAS_CUSTOM_NAME = "has_custom_name"
        private const val KEY_DARK_MODE = "dark_mode"

        fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

        fun String.hexToByteArray(): ByteArray {
            val len = length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(this[i], 16) shl 4) +
                        Character.digit(this[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }
}
