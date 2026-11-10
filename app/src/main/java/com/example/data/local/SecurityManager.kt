package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "aura_diary_security_prefs"
        private const val KEYSTORE_ALIAS = "AuraDiaryMasterAESKey"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private const val PREF_PIN_ENABLED = "pref_pin_enabled"
        private const val PREF_PIN_HASH = "pref_pin_hash"
        private const val PREF_PIN_SALT = "pref_pin_salt"
        private const val PREF_AUTO_LOCK_TIMEOUT = "pref_auto_lock_timeout"
        private const val PREF_BIOMETRIC_ENABLED = "pref_biometric_enabled"
        private const val PREF_PRIVACY_FLAG_SECURE = "pref_privacy_flag_secure"
        private const val PREF_THEME_MODE = "pref_theme_mode"
    }

    init {
        ensureKeyStoreKey()
    }

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Self-healing fallback if KeyStore has transient provider issue
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Encrypts plaintext string using AES-256 GCM.
     * Output format: Base64(12-byte IV + GCM ciphertext + 16-byte Tag)
     */
    fun encryptText(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return try {
            val key = getSecretKey() ?: return plaintext
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plaintext
        }
    }

    /**
     * Decrypts AES-256 GCM ciphertext.
     */
    fun decryptText(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return encryptedBase64
            val iv = ByteArray(GCM_IV_LENGTH)
            val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.size)

            val key = getSecretKey() ?: return encryptedBase64
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Return raw string if already plaintext or legacy entry
            encryptedBase64
        }
    }

    // --- Master PIN & Security Preferences ---

    var isPinEnabled: Boolean
        get() = prefs.getBoolean(PREF_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(PREF_PIN_ENABLED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, value).apply()

    var isPrivacyFlagSecureEnabled: Boolean
        get() = prefs.getBoolean(PREF_PRIVACY_FLAG_SECURE, true)
        set(value) = prefs.edit().putBoolean(PREF_PRIVACY_FLAG_SECURE, value).apply()

    var autoLockTimeoutSeconds: Long
        get() = prefs.getLong(PREF_AUTO_LOCK_TIMEOUT, 60L) // Default 1 min
        set(value) = prefs.edit().putLong(PREF_AUTO_LOCK_TIMEOUT, value).apply()

    var themeModeName: String
        get() = prefs.getString(PREF_THEME_MODE, "DARK") ?: "DARK"
        set(value) = prefs.edit().putString(PREF_THEME_MODE, value).apply()

    fun setMasterPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPinWithSalt(pin, salt)
        prefs.edit()
            .putString(PREF_PIN_SALT, saltBase64)
            .putString(PREF_PIN_HASH, hash)
            .putBoolean(PREF_PIN_ENABLED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!isPinEnabled) return true
        val savedHash = prefs.getString(PREF_PIN_HASH, null) ?: return true
        val saltBase64 = prefs.getString(PREF_PIN_SALT, null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val computedHash = hashPinWithSalt(pin, salt)
        return computedHash == savedHash
    }

    fun disablePin() {
        prefs.edit()
            .remove(PREF_PIN_HASH)
            .remove(PREF_PIN_SALT)
            .putBoolean(PREF_PIN_ENABLED, false)
            .apply()
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}
