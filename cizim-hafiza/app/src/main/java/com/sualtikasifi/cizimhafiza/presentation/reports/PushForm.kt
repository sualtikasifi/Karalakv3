package com.sualtikasifi.cizimhafiza.presentation.reports

import java.time.ZoneId

/** When a push goes out: now, or a time the hourly job waits for. */
enum class PushWhen(val label: String) {
    Now("Hemen"), InOneHour("+1 saat"), InThreeHours("+3 saat"), TomorrowMorning("Yarın 10:00")
}

/** Who gets it. [key] is what the command stores (`audience`); the server builds the topic condition from it. */
enum class PushAudience(val key: String, val label: String) {
    All("all", "Herkes"),
    /** Devices not opened this week or last: they report the weekly topics (a version from before them cannot be told apart). */
    Lapsed("lapsed", "Son iki haftada girmeyenler")
}

/** The text of one language: both parts or neither. */
data class PushText(val title: String = "", val body: String = "") {
    val filled: Boolean get() = title.isNotBlank() && body.isNotBlank()
    val partial: Boolean get() = !filled && (title.isNotBlank() || body.isNotBlank())
}

/** The rules of the push form, apart from the screen so they can be tested. */
object PushForm {
    const val MAX_TITLE = 60
    const val MAX_BODY = 200

    /** The languages besides Turkish, which is the base text. */
    val EXTRA_LANGS = listOf("en" to "English", "pt" to "Português", "es" to "Español")

    private val ISTANBUL = ZoneId.of("Europe/Istanbul")

    /** What is wrong with the form as a line for the panel, or null when it can be sent. */
    fun problem(title: String, body: String, extra: Map<String, PushText>): String? {
        if (title.isBlank() || body.isBlank()) return "Türkçe başlık ve metin birlikte yazılmalı."
        EXTRA_LANGS.forEach { (code, name) ->
            if (extra[code]?.partial == true) return "$name: başlık ve metin birlikte yazılmalı (ya da ikisini de boş bırak)."
        }
        return null
    }

    /** The languages that have a full text of their own, Turkish first. */
    fun languages(extra: Map<String, PushText>): List<String> =
        listOf("tr") + EXTRA_LANGS.map { it.first }.filter { extra[it]?.filled == true }

    /** The moment a push is due ([PushWhen.Now] is 0, "no waiting"), the morning in Istanbul time. */
    fun sendAt(w: PushWhen, now: Long, zone: ZoneId = ISTANBUL): Long = when (w) {
        PushWhen.Now -> 0L
        PushWhen.InOneHour -> now + 3_600_000L
        PushWhen.InThreeHours -> now + 3 * 3_600_000L
        PushWhen.TomorrowMorning ->
            java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /** The command document (without its server timestamp): only what differs from the plain "to everyone, now" is written. */
    fun command(
        title: String, body: String, extra: Map<String, PushText>, audience: PushAudience,
        fallback: Boolean, sendAtMillis: Long, by: String
    ): Map<String, Any> = buildMap {
        put("type", "broadcast")
        put("title", title.trim().take(MAX_TITLE))
        put("body", body.trim().take(MAX_BODY))
        put("by", by)
        val texts = EXTRA_LANGS.map { it.first }.mapNotNull { code ->
            extra[code]?.takeIf { it.filled }?.let { code to mapOf("title" to it.title.trim().take(MAX_TITLE), "body" to it.body.trim().take(MAX_BODY)) }
        }.toMap()
        if (texts.isNotEmpty()) put("texts", texts)
        if (audience != PushAudience.All) put("audience", audience.key)
        // the fallback only means something for everyone (see planBroadcast): written only when it is turned off
        if (!fallback && audience == PushAudience.All && texts.isNotEmpty()) put("fallback", false)
        if (sendAtMillis > 0L) put("sendAtMillis", sendAtMillis)
    }

    /** The line the audit log and the confirmation share: who, in which languages, when. */
    fun summary(audience: PushAudience, extra: Map<String, PushText>, fallback: Boolean, w: PushWhen): String {
        val langs = languages(extra)
        return listOf(
            audience.label.lowercase(java.util.Locale("tr")),
            langs.joinToString("+"),
            if (audience == PushAudience.All && langs.size > 1 && !fallback) "metinsiz dillere gitmez" else null,
            w.label.lowercase(java.util.Locale("tr"))
        ).filterNotNull().joinToString(" · ")
    }
}
