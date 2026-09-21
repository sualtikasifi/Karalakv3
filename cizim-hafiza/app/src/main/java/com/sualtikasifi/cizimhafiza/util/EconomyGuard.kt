package com.sualtikasifi.cizimhafiza.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Tamper seal for the local economy (gold, jokers, store items).
 *
 * Those numbers live in plain SharedPreferences, so on a rooted phone (or
 * with a backup editor) anybody could type in a fortune. Every legitimate
 * write also stores an HMAC of the whole economy, computed with a key that
 * lives in the Android Keystore and can never be read out of the device. An
 * edited file no longer matches its seal, and SettingsRepository resets the
 * economy to zero when it notices.
 *
 * It is a speed bump, not a vault — real protection for anything that
 * competes with other players (league, prizes) has to be checked server-side.
 */
object EconomyGuard {

    private const val ALIAS = "karalak_economy_seal"
    private const val PROVIDER = "AndroidKeyStore"

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    /** False on a fresh install, after a wipe, and on a new phone — nothing exists to compare against yet. */
    fun hasKey(): Boolean = runCatching { keyStore().containsAlias(ALIAS) }.getOrDefault(false)

    private fun key(): SecretKey {
        val store = keyStore()
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER)
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN).build())
        return generator.generateKey()
    }

    /** The seal for [payload]; empty if the Keystore is unusable (then nothing can be verified either). */
    fun sign(payload: String): String = runCatching {
        val mac = Mac.getInstance("HmacSHA256").apply { init(key()) }
        Base64.encodeToString(mac.doFinal(payload.toByteArray()), Base64.NO_WRAP)
    }.getOrDefault("")

    fun verify(payload: String, seal: String): Boolean {
        val expected = sign(payload)
        // An unusable Keystore signs everything as "" — accept rather than wipe a real player's gold.
        return expected.isEmpty() || expected == seal
    }
}
