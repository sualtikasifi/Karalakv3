package com.sualtikasifi.cizimhafiza.data.repository

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** A notice from the developer panel, already in the player's language. */
data class Announcement(val id: String, val title: String, val body: String)

/**
 * The announcement window on the home screen, set from the developer panel (`config/announcement`):
 * `{active, id, untilMillis, texts: {tr|en|pt|es: {title, body}}}`. Shown once per `id` on a device; the language is the
 * phone's, then English, then Turkish. Read through [ConfigDocFetcher] like the economy, so it costs one read per half hour at most.
 */
@Singleton
class AnnouncementRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs get() = context.getSharedPreferences("announcement", Context.MODE_PRIVATE)
    private val _current = MutableStateFlow<Announcement?>(null)

    /** The notice to show now, or null when there is none, it ran out, or this device has already closed it. */
    val current: StateFlow<Announcement?> = _current.asStateFlow()

    fun refresh() {
        scope.launch { ConfigDocFetcher.load(context, firestore, PATH, TTL_MILLIS) { _current.value = parse(it) } }
    }

    fun dismiss() {
        _current.value?.let { a -> prefs.edit { putString(KEY_SEEN, a.id) } }
        _current.value = null
    }

    private fun parse(data: Map<String, Any?>?): Announcement? {
        if (data == null || data["active"] != true) return null
        val id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val until = (data["untilMillis"] as? Number)?.toLong() ?: 0L
        if (until > 0 && System.currentTimeMillis() > until) return null
        if (prefs.getString(KEY_SEEN, null) == id) return null
        val texts = data["texts"] as? Map<*, *> ?: return null
        fun pick(lang: String): Pair<String, String>? {
            val m = texts[lang] as? Map<*, *> ?: return null
            val title = (m["title"] as? String).orEmpty().trim()
            val body = (m["body"] as? String).orEmpty().trim()
            return if (title.isEmpty() && body.isEmpty()) null else title to body
        }
        val (title, body) = pick(Locale.getDefault().language) ?: pick("en") ?: pick("tr") ?: return null
        return Announcement(id, title, body)
    }

    private companion object {
        const val PATH = "config/announcement"
        const val KEY_SEEN = "seen_id"
        const val TTL_MILLIS = 30L * 60 * 1000
    }
}
