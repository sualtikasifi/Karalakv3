package com.sualtikasifi.cizimhafiza.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.MainActivity
import com.sualtikasifi.cizimhafiza.util.SettingsRepository

/**
 * "Ücretsiz altının hazır!" / "Ücretsiz kasan hazır!" — a notification for the moment the wait after taking the home
 * screen's free gold or free chest (each one a rewarded ad) is over, so a player who took one and left is brought back to
 * take the next.
 *
 * One alarm per gift, armed when the gift is taken (SettingsRepository.claimAdGold / claimAdChest), after an account restore
 * and on every start; it is the same inexact-but-Doze-piercing alarm as [ChestReadyNotifier] and is put back after a reboot
 * or an update. A gift that is already ready, or that was never taken, has no alarm: nothing is announced twice and nobody is
 * reminded of something they have not started. Off in Settings means off (the same "notifications_enabled" switch), and the
 * channel is its own, so it can also be muted without losing the others.
 */
object GiftReadyNotifier {

    const val CHANNEL_ID = "gift_ready"
    const val ACTION_GIFT_READY = "com.sualtikasifi.cizimhafiza.GIFT_READY"
    const val EXTRA_KIND = "kind"

    enum class Kind(val requestCode: Int, val notificationId: Int) {
        GOLD(4301, 1301),
        CHEST(4302, 1302)
    }

    private const val TAG = "GiftReadyNotifier"

    fun createChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.gift_ready_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    /** Re-arms (or cancels) both alarms from the two "next available" times; 0 or a time in the past means nothing waits. */
    fun sync(context: Context, goldNextAtMillis: Long, chestNextAtMillis: Long) {
        val now = System.currentTimeMillis()
        arm(context, Kind.GOLD, goldNextAtMillis, now)
        arm(context, Kind.CHEST, chestNextAtMillis, now)
    }

    private fun arm(context: Context, kind: Kind, atMillis: Long, now: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        runCatching {
            if (atMillis > now) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent(context, kind))
            } else {
                alarmManager.cancel(pendingIntent(context, kind))
            }
        }.onFailure { Log.w(TAG, "Gift alarm not set", it) }
    }

    private fun pendingIntent(context: Context, kind: Kind): PendingIntent = PendingIntent.getBroadcast(
        context,
        kind.requestCode,
        Intent(context, GiftReadyReceiver::class.java).setAction(ACTION_GIFT_READY).putExtra(EXTRA_KIND, kind.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun show(context: Context, kind: Kind) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context, kind.requestCode,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (title, body) = when (kind) {
            Kind.GOLD -> context.getString(R.string.gift_gold_notif_title) to context.getString(R.string.gift_gold_notif_body, SettingsRepository.AD_GOLD_AMOUNT)
            Kind.CHEST -> context.getString(R.string.gift_chest_notif_title) to context.getString(R.string.gift_chest_notif_body)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(kind.notificationId, notification)
    }
}

/** Fires when a gift's wait is over, and re-arms both alarms after a reboot or an app update wiped them. */
class GiftReadyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("cizim_hafiza_settings", Context.MODE_PRIVATE)
        val goldNext = prefs.getLong(SettingsRepository.KEY_AD_GOLD_NEXT_AT, 0L)
        val chestNext = prefs.getLong(SettingsRepository.KEY_AD_CHEST_NEXT_AT, 0L)
        when (intent.action) {
            GiftReadyNotifier.ACTION_GIFT_READY -> {
                // Same switch as every other reminder: off means off.
                if (!prefs.getBoolean("notifications_enabled", true)) return
                val kind = runCatching { GiftReadyNotifier.Kind.valueOf(intent.getStringExtra(GiftReadyNotifier.EXTRA_KIND).orEmpty()) }.getOrNull() ?: return
                // Only if it really is ready now: a gift taken again since (its wait restarted) has its own, later alarm.
                val next = if (kind == GiftReadyNotifier.Kind.GOLD) goldNext else chestNext
                if (next > System.currentTimeMillis()) return
                GiftReadyNotifier.show(context, kind)
            }
            else -> GiftReadyNotifier.sync(context, goldNext, chestNext)
        }
    }
}
