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
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.MainActivity
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import kotlinx.serialization.json.Json

/**
 * "Kasan hazır!" — a push notification for the moment a chest finishes
 * unlocking, so a closed app still pulls the player back.
 *
 * Only one chest counts down at a time, so there is at most one alarm:
 * [sync] is called every time the slots change (SettingsRepository.
 * saveChestSlots) and simply re-arms the single alarm for whichever chest is
 * currently unlocking — or cancels it when none is. Same reasoning as
 * [NotificationScheduler] for an inexact alarm that still pierces Doze
 * instead of an exact one, and for re-arming after a reboot or an update.
 */
object ChestReadyNotifier {

    const val CHANNEL_ID = "chest_ready"
    const val ACTION_CHEST_READY = "com.sualtikasifi.cizimhafiza.CHEST_READY"
    const val EXTRA_TIER = "tier"

    private const val TAG = "ChestReadyNotifier"
    private const val ALARM_REQUEST_CODE = 4201
    private const val NOTIFICATION_ID = 1201

    fun createChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.chest_ready_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    /** Re-arms (or cancels) the single alarm for the chest that is counting down in [slots]. */
    fun sync(context: Context, slots: List<Chest?>) {
        val now = System.currentTimeMillis()
        val unlocking = slots.firstOrNull { it?.unlockStartedAtMillis != null && !it.isReady(now) }
        if (unlocking == null) {
            cancel(context)
        } else {
            schedule(context, unlocking.unlockStartedAtMillis!! + unlocking.tier.unlockDurationMillis, unlocking.tier)
        }
    }

    private fun schedule(context: Context, atMillis: Long, tier: ChestTier) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent(context, tier))
        }.onFailure { Log.w(TAG, "Chest alarm not set", it) }
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        runCatching { alarmManager.cancel(pendingIntent(context, ChestTier.SILVER)) }
    }

    private fun pendingIntent(context: Context, tier: ChestTier): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, ChestReadyReceiver::class.java)
            .setAction(ACTION_CHEST_READY)
            .putExtra(EXTRA_TIER, tier.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun show(context: Context, tier: ChestTier) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = context.getString(R.string.chest_ready_notif_body, context.getString(tier.labelRes()))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.chest_ready_notif_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

/** Fires when the alarm goes off, and re-arms it after a reboot or an app update wiped it. */
class ChestReadyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("cizim_hafiza_settings", Context.MODE_PRIVATE)
        when (intent.action) {
            ChestReadyNotifier.ACTION_CHEST_READY -> {
                // Same switch as every other reminder: off means off.
                if (!prefs.getBoolean("notifications_enabled", true)) return
                val tier = runCatching { ChestTier.valueOf(intent.getStringExtra(ChestReadyNotifier.EXTRA_TIER).orEmpty()) }.getOrNull() ?: return
                ChestReadyNotifier.show(context, tier)
            }
            else -> {
                val raw = prefs.getString("chest_slots", null) ?: return
                val slots = runCatching { Json.decodeFromString<List<Chest?>>(raw) }.getOrNull() ?: return
                ChestReadyNotifier.sync(context, slots)
            }
        }
    }
}
