package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reads what the Play Store remembers about the link a new player installed from. The download page
 * (hosting/indir, karalak.com/f/CODE) adds `referrer=karalak_friend%3DCODE` to the Play Store address, so a brand-new
 * install can be tied to whoever shared the link even though the app was not installed to open a deep link.
 */
object InstallReferrerReader {
    /** The friend code from the install link, or null (no referrer, not a friend link, Play unavailable, a malformed code). */
    suspend fun friendCode(context: Context): String? = runCatching {
        val raw = readReferrer(context) ?: return@runCatching null
        // "karalak_friend=482913" — anything else (organic installs, other campaigns) is ignored
        val code = raw.split('&').firstNotNullOfOrNull { part ->
            part.split('=', limit = 2).takeIf { it.size == 2 && it[0] == "karalak_friend" }?.get(1)
        } ?: return@runCatching null
        code.takeIf { it.length == 6 && it.all(Char::isLetterOrDigit) }
    }.getOrNull()

    private suspend fun readReferrer(context: Context): String? = suspendCancellableCoroutine { cont ->
        val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                val value = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    runCatching { client.installReferrer.installReferrer }.getOrNull()
                } else null
                runCatching { client.endConnection() }
                if (cont.isActive) cont.resume(value)
            }

            override fun onInstallReferrerServiceDisconnected() {
                if (cont.isActive) cont.resume(null)
            }
        })
        cont.invokeOnCancellation { runCatching { client.endConnection() } }
    }
}
