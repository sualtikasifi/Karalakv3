package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * The legal pages on karalak.com (hosting/gizlilik, hosting/kosullar, hosting/hesap-silme). Each page carries all four
 * languages and shows the one in `?lang=`, so the link opens in the language the app is showing.
 */
object LegalLinks {
    private const val BASE = "https://karalak.com"

    fun privacyPolicy(context: Context): String = "$BASE/gizlilik?lang=${language(context)}"

    fun termsOfUse(context: Context): String = "$BASE/kosullar?lang=${language(context)}"

    /**
     * Opens [url] in a Custom Tab rather than handing off to the browser app: it opens over the game, in its colours,
     * and back returns straight here (a bare ACTION_VIEW could land on the browser's first-run screen).
     */
    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(android.graphics.Color.parseColor("#8A4E12"))
                        .build()
                )
                .build()
                .launchUrl(context, uri)
        }.onFailure {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun language(context: Context): String {
        val code = context.resources.configuration.locales[0]?.language.orEmpty()
        return if (code in setOf("tr", "en", "pt", "es")) code else "en"
    }
}
