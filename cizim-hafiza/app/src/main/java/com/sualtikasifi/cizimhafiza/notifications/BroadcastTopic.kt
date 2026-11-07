package com.sualtikasifi.cizimhafiza.notifications

import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.messaging.FirebaseMessaging
import com.sualtikasifi.cizimhafiza.domain.model.SupportedLanguage
import com.sualtikasifi.cizimhafiza.util.NonFatal
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Locale

/**
 * The topics the developer panel's "Bildirim gönder" writes to. A device is in them while notifications are on and the
 * player is 13 or older: a child account is never sent marketing or news (see LegalConsentStore.childMode).
 *
 *  - [NAME] ("news"): everyone who may be sent a broadcast;
 *  - `news_<language>`: the app's language, so a push can be sent in each language its own text;
 *  - [SEEN_MARKER] ("seen_v1"): this app version keeps the weekly topics, which is what makes "not opened lately" mean
 *    something (an older install has no weekly topic and would otherwise look as if it had never been opened);
 *  - `seen_<ISO week>`: the week this device was last opened in, kept for this week and the one before.
 *
 * functions/src/adminTasks.ts builds the topic conditions from these names; keep the two in step.
 */
object BroadcastTopic {
    const val NAME = "news"
    const val SEEN_MARKER = "seen_v1"

    /** The language topics: the app's own languages. */
    fun languageTopic(code: String) = "news_$code"

    /** `seen_2026w45`: the ISO week of [date] (the week belongs to the year of its Thursday). */
    fun weekTopic(date: LocalDate): String {
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "seen_${year}w${week.toString().padStart(2, '0')}"
    }

    /** What to subscribe to and what to leave for a device on [language] on [today]; with [subscribed] false it leaves them all. */
    fun plan(subscribed: Boolean, language: String, today: LocalDate): Pair<Set<String>, Set<String>> {
        val languages = SupportedLanguage.entries.map { languageTopic(it.code) }.toSet()
        val mine = languageTopic(SupportedLanguage.resolve(language).code)
        val weeks = (0L..3L).map { weekTopic(today.minusWeeks(it)) }
        return if (subscribed) {
            // this week and the last stay; the two before them go
            setOf(NAME, SEEN_MARKER, mine, weeks[0], weeks[1]) to (languages - mine + weeks[2] + weeks[3])
        } else {
            emptySet<String>() to (languages + NAME + SEEN_MARKER + weeks.toSet())
        }
    }

    /** The language the app is showing, by the same rule as the settings screen. */
    private fun currentLanguage(): String =
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language

    fun sync(subscribed: Boolean) {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        // UTC, like the server that builds the conditions from the same week names
        val (join, leave) = plan(subscribed, currentLanguage(), LocalDate.now(java.time.ZoneOffset.UTC))
        join.forEach { topic -> messaging.subscribeToTopic(topic).addOnFailureListener { NonFatal.report("BroadcastTopic", "join $topic failed", it) } }
        leave.forEach { topic -> messaging.unsubscribeFromTopic(topic).addOnFailureListener { NonFatal.report("BroadcastTopic", "leave $topic failed", it) } }
    }
}
