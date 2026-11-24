package com.sualtikasifi.cizimhafiza.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.repository.FriendRepository
import com.sualtikasifi.cizimhafiza.presentation.MainActivity
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives the push a Cloud Function sends (see functions/src/index.ts) when
 * a friend invites this device to a match and it's not currently in the
 * app — shows a system notification. Tapping it just opens MainActivity
 * (same PendingIntent shape as DailyEngagementWorker.showNotification): no
 * deep link needed, because the invite still exists in Firestore and
 * IncomingInviteViewModel's live listener (mounted app-wide, see NavGraph.kt)
 * shows the accept/decline banner automatically the moment the app is
 * foregrounded, same as if it had been running the whole time.
 */
@AndroidEntryPoint
class FriendInviteMessagingService : FirebaseMessagingService() {

    @Inject lateinit var friendRepository: FriendRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    // onNewToken/onMessageReceived aren't suspend functions, and this
    // service isn't a lifecycle-owning component with its own scope — a
    // small dedicated scope lets the token write survive past the callback
    // returning, same reasoning as CizimHafizaApp's applicationScope.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { runCatching { friendRepository.updateFcmToken(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!settingsRepository.notificationsEnabled.value) return

        // The Cloud Function sends a data-only payload (no `notification`
        // key) specifically so onMessageReceived always runs here — even
        // with the app backgrounded/killed — instead of the OS displaying a
        // pre-built (unlocalized, wrong-channel) notification straight from
        // the payload. Title/body are built from local string resources so
        // they follow this device's own app language, not the sender's.
        // A broadcast from the developer panel carries its own title and text (data-only as well).
        if (message.data["type"] == "broadcast") {
            showBroadcast(message.data["title"].orEmpty(), message.data["body"].orEmpty())
            return
        }
        // A duel: someone challenged this player, or the one they challenged has played (sent by the scheduled job, see
        // functions/src/duelPush.ts). Like an invite, the thing itself waits in Firestore and the app shows it when opened.
        if (message.data["type"] == "duel" || message.data["type"] == "duel_result") {
            val who = message.data["fromNickname"].orEmpty().ifBlank { return }
            val answered = message.data["type"] == "duel_result"
            showNotification(
                getString(if (answered) R.string.duel_result_notification_title else R.string.duel_notification_title, who),
                if (answered) getString(R.string.duel_result_notification_body) else nextDuelBody(),
                message.data["duelId"],
                opensDuels = true
            )
            return
        }
        val fromNickname = message.data["fromNickname"] ?: return
        val inviteId = message.data["inviteId"]
        val title = getString(R.string.friend_invite_notification_title, fromNickname)
        val body = getString(R.string.friend_invite_notification_body)

        showNotification(title, body, inviteId)
    }

    /** The text under a duel notification: the next one of the list, so the same words do not come every time (see DuelNotificationText). */
    private fun nextDuelBody(): String {
        val bodies = resources.getStringArray(R.array.duel_notification_bodies)
        val prefs = getSharedPreferences("duel_notification_rotation", Context.MODE_PRIVATE)
        val next = DuelNotificationText.nextIndex(prefs.getInt("last", -1), bodies.size, firstPick = bodies.indices.random())
        prefs.edit().putInt("last", next).apply()
        return bodies[next]
    }

    private fun showBroadcast(title: String, body: String) {
        if (title.isBlank() && body.isBlank()) return
        val context: Context = applicationContext
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        NotificationScheduler.createNewsChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, BROADCAST_REQUEST_CODE, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationScheduler.NEWS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(BROADCAST_REQUEST_CODE, notification)
    }

    private fun showNotification(title: String, body: String, inviteId: String?, opensDuels: Boolean = false) {
        val context: Context = applicationContext
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // A duel lands on the duels page, not on the home screen (see MainActivity.noteDuelNotification).
            if (opensDuels) putExtra(MainActivity.DUEL_NOTIFICATION_EXTRA, true)
        }
        // Distinct request code per invite so multiple pending invite
        // notifications don't overwrite each other's PendingIntent (unlike
        // the single daily-reminder notification, several friends could
        // invite this device before any of the notifications are opened).
        val requestCode = inviteId?.hashCode() ?: 0
        val pendingIntent = PendingIntent.getActivity(
            context, requestCode, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationScheduler.FRIEND_INVITE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    private companion object {
        const val BROADCAST_REQUEST_CODE = 7301
    }
}
