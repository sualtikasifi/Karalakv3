package com.sualtikasifi.cizimhafiza.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.sualtikasifi.cizimhafiza.util.NonFatal

/**
 * The topic the developer panel's "Bildirim gönder" writes to. A device is in it while notifications are on and the
 * player is 13 or older: a child account is never sent marketing or news (see LegalConsentStore.childMode).
 */
object BroadcastTopic {
    const val NAME = "news"

    fun sync(subscribed: Boolean) {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        val task = if (subscribed) messaging.subscribeToTopic(NAME) else messaging.unsubscribeFromTopic(NAME)
        task.addOnFailureListener { NonFatal.report("BroadcastTopic", "topic change failed", it) }
    }
}
