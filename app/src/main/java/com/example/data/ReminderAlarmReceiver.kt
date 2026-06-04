package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ui.AlarmActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAlarmData(
    val title: String,
    val domain: String,
    val prizes: String
)

object LiveAlarmManager {
    private val _activeAlarm = MutableStateFlow<ActiveAlarmData?>(null)
    val activeAlarm = _activeAlarm.asStateFlow()

    fun triggerAlarm(title: String, domain: String, prizes: String) {
        _activeAlarm.value = ActiveAlarmData(title, domain, prizes)
    }

    fun dismissAlarm() {
        _activeAlarm.value = null
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "ACTION_DISMISS_ALARM") {
            val id = intent.getIntExtra("sprint_id", 0)
            LiveAlarmManager.dismissAlarm()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)
            
            // Stop any ongoing vibrator if running
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.cancel()
            return
        }

        if (action == "ACTION_SNOOZE_ALARM") {
            val id = intent.getIntExtra("sprint_id", 0)
            val title = intent.getStringExtra("sprint_title") ?: "Hackathon Deadline"
            val domain = intent.getStringExtra("sprint_domain") ?: "General"
            val prizes = intent.getStringExtra("sprint_prizes") ?: "N/A"
            val ringtone = intent.getStringExtra("alarm_ringtone") ?: "Default"
            val duration = intent.getIntExtra("alarm_duration", 30)
            val repetition = intent.getStringExtra("alarm_repetition") ?: "Once"
            val deliveryType = intent.getStringExtra("alert_delivery_type") ?: "Full Alarm"

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)
            
            // Cancel active vibrator
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.cancel()

            // Fetch preferred snooze minutes from preferences (default 5 min)
            val prefs = context.getSharedPreferences("hackathon_tracker_prefs", Context.MODE_PRIVATE)
            val defaultSnoozeMin = prefs.getInt("pref_default_snooze_min", 5)

            val triggerAtMs = System.currentTimeMillis() + (defaultSnoozeMin * 60 * 1000)

            val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("sprint_title", title)
                putExtra("sprint_domain", domain)
                putExtra("sprint_prizes", prizes)
                putExtra("alarm_ringtone", ringtone)
                putExtra("alarm_duration", duration)
                putExtra("alarm_repetition", repetition)
                putExtra("alert_delivery_type", deliveryType)
                putExtra("sprint_id", id)
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val pending = PendingIntent.getBroadcast(
                context,
                id,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMs, pending)
            }
            return
        }

        val title = intent.getStringExtra("sprint_title") ?: "Hackathon Deadline"
        val domain = intent.getStringExtra("sprint_domain") ?: "General"
        val prizes = intent.getStringExtra("sprint_prizes") ?: "N/A"
        val ringtone = intent.getStringExtra("alarm_ringtone") ?: "Default"
        val duration = intent.getIntExtra("alarm_duration", 30)
        val repetition = intent.getStringExtra("alarm_repetition") ?: "Once"
        val deliveryType = intent.getStringExtra("alert_delivery_type") ?: "Full Alarm"
        val id = intent.getIntExtra("sprint_id", 0)
        
        if (deliveryType == "Silent/None" || deliveryType == "None") {
            // Muted, nothing to do
            return
        }

        val isFullAlarm = deliveryType == "Full Alarm"
        val isVibrateOnly = deliveryType == "Vibration Only"
        val isNotificationOnly = deliveryType == "Notification Only"

        if (isFullAlarm) {
            LiveAlarmManager.triggerAlarm(title, domain, prizes)
        }
        
        // Show local Android notification with beautiful styles
        val channelId = when {
            isFullAlarm -> "hackathon_sprint_reminders_full"
            isVibrateOnly -> "hackathon_sprint_reminders_vibrate"
            else -> "hackathon_sprint_reminders_notify"
        }
        val channelName = when {
            isFullAlarm -> "Sprint Alarms (Full)"
            isVibrateOnly -> "Sprint Alarms (Vibration Only)"
            else -> "Sprint Alarms (Notification Only)"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = if (isFullAlarm) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channels for global innovation sprint countdowns"
                enableLights(true)
                if (isVibrateOnly) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setSound(null, null)
                } else if (isNotificationOnly) {
                    enableVibration(false)
                } else {
                    enableVibration(true)
                    setBypassDnd(true)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Main intent for standard notification clicks
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("triggered_sprint_title", title)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            id + 100000,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val soundUri = if (isVibrateOnly) {
            null
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        
        val dismissIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            setAction("ACTION_DISMISS_ALARM")
            putExtra("sprint_id", id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 200000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            setAction("ACTION_SNOOZE_ALARM")
            putExtra("sprint_id", id)
            putExtra("sprint_title", title)
            putExtra("sprint_domain", domain)
            putExtra("sprint_prizes", prizes)
            putExtra("alarm_ringtone", ringtone)
            putExtra("alarm_duration", duration)
            putExtra("alarm_repetition", repetition)
            putExtra("alert_delivery_type", deliveryType)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            id + 300000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
 
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ COOLDOWN REMINDER TRIGGERED!")
            .setContentText("The registration for '$title' ($domain) is closing! Prizes: $prizes")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "Snooze",
                snoozePendingIntent
            )

        if (isFullAlarm) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }

        if (soundUri != null) {
            builder.setSound(soundUri)
        }

        if (isVibrateOnly) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
        } else if (isNotificationOnly) {
            builder.setVibrate(longArrayOf(0))
        }

        notificationManager.notify(id, builder.build())
        
        // Trigger call-style physical device vibration (repeating pulses)
        if (isVibrateOnly || isFullAlarm) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000),
                        -1
                    )
                    vibrator.vibrate(effect)
                } else {
                    vibrator.vibrate(4000)
                }
            }
        }
    }
}
