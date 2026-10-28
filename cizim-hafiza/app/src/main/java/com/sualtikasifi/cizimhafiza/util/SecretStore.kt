package com.sualtikasifi.cizimhafiza.util

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the developer panel's API keys (OpenRouter, ElevenLabs) encrypted in the preferences file: AES-GCM with a key that
 * lives in the Android Keystore and never leaves it, so a copy of the preferences (a backup, a rooted phone's file dump)
 * is not a copy of the keys. A key stored in plain text by an older version is read once and written back encrypted.
 */
object SecretStore {
    private const val ALIAS = "karalak_panel_secrets"
    private const val PROVIDER = "AndroidKeyStore"
    private const val PREFIX = "enc1:"

    fun get(prefs: SharedPreferences, key: String): String {
        val stored = prefs.getString(key, "").orEmpty()
        if (stored.isEmpty()) return ""
        if (!stored.startsWith(PREFIX)) {
            // Saved in plain text by an older version: move it under the Keystore key.
            runCatching { put(prefs, key, stored) }
            return stored
        }
        return runCatching { decrypt(stored.removePrefix(PREFIX)) }.getOrDefault("")
    }

    /** An empty [value] removes the entry. A Keystore that refuses leaves the old value alone and throws. */
    fun put(prefs: SharedPreferences, key: String, value: String) {
        if (value.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        prefs.edit().putString(key, PREFIX + encrypt(value)).apply()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, raw, 0, IV_BYTES)) }
        return String(cipher.doFinal(raw, IV_BYTES, raw.size - IV_BYTES), Charsets.UTF_8)
    }

    private const val IV_BYTES = 12
}
