package com.example.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveAlarmManager
import com.example.data.ReminderAlarmReceiver
import com.example.ui.theme.MyApplicationTheme

class AlarmActivity : ComponentActivity() {
    private var soundPlayer: SoundPlayer? = null

    companion object {
        @Volatile
        var activeInstance: AlarmActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activeInstance = this
        super.onCreate(savedInstanceState)
        
        // Show over keyguard/lock screen and keep screen turned on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        enableEdgeToEdge()

        val title = intent.getStringExtra("sprint_title") ?: "Innovation Sprint Closing"
        val domain = intent.getStringExtra("sprint_domain") ?: "General Tech"
        val prizes = intent.getStringExtra("sprint_prizes") ?: "$10,000"
        val ringtone = intent.getStringExtra("alarm_ringtone") ?: "Default"
        val duration = intent.getIntExtra("alarm_duration", 30)
        val repetition = intent.getStringExtra("alarm_repetition") ?: "Once"
        val hackathonId = intent.getIntExtra("sprint_id", 0)

        // Play the ringtone
        soundPlayer = SoundPlayer(this)
        soundPlayer?.play(ringtone, duration)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AlarmScreen(
                        modifier = Modifier.padding(innerPadding),
                        title = title,
                        domain = domain,
                        prizes = prizes,
                        ringtoneName = ringtone,
                        durationSec = duration,
                        repetition = repetition,
                        onSnooze = { mins ->
                            snoozeAlarm(title, domain, prizes, mins)
                            dismissAndExit()
                        },
                        onDismiss = {
                            dismissAndExit()
                        }
                    )
                }
            }
        }
    }

    private fun snoozeAlarm(title: String, domain: String, prizes: String, minutes: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderAlarmReceiver::class.java).apply {
            putExtra("sprint_title", title)
            putExtra("sprint_domain", domain)
            putExtra("sprint_prizes", prizes)
            putExtra("alarm_ringtone", "Default")
            putExtra("alarm_duration", 30)
            putExtra("alarm_repetition", "Once")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTimeMs = System.currentTimeMillis() + (minutes * 60 * 1000)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
        Toast.makeText(this, "Snoozed alarm for $minutes min(s)", Toast.LENGTH_SHORT).show()
    }

    fun dismissAndExit() {
        soundPlayer?.stop()
        LiveAlarmManager.dismissAlarm()
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val hackathonID = intent.getIntExtra("sprint_id", 0)
            notificationManager.cancel(hackathonID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer?.stop()
        if (activeInstance == this) {
            activeInstance = null
        }
    }
}

class SoundPlayer(private val context: Context) {
    private var ringtone: android.media.Ringtone? = null
    @Volatile private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var generatorThread: Thread? = null
    @Volatile private var activeToneGenerator: ToneGenerator? = null

    fun play(ringtoneType: String, durationSec: Int) {
        if (isPlaying) stop()
        isPlaying = true

        try {
            if (ringtoneType == "Default" || ringtoneType == "System Default" || ringtoneType.isBlank()) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
            } else if (ringtoneType.startsWith("content://") || ringtoneType.startsWith("file://") || ringtoneType.contains("/")) {
                val soundUri = android.net.Uri.parse(ringtoneType)
                ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
            } else {
                // Programmatic Tone Generation thread to ensure custom frequencies work across devices!
                generatorThread = Thread {
                    val streamType = AudioManager.STREAM_ALARM
                    val tg = try {
                        ToneGenerator(streamType, 100)
                    } catch (e: Exception) {
                        null
                    }
                    activeToneGenerator = tg
                    val toneType = when (ringtoneType) {
                        "Loud Siren" -> ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
                        "Sci-Fi Beacon" -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                        "Calm Chime" -> ToneGenerator.TONE_PROP_BEEP
                        else -> ToneGenerator.TONE_CDMA_PIP
                    }
                    val interval = when (ringtoneType) {
                        "Loud Siren" -> 700L
                        "Sci-Fi Beacon" -> 600L
                        "Calm Chime" -> 1500L
                        else -> 1000L
                    }

                    while (isPlaying && tg != null) {
                        try {
                            tg.startTone(toneType, 400)
                            Thread.sleep(interval)
                        } catch (e: InterruptedException) {
                            break
                        } catch (e: Exception) {
                            // Suppress native binder issues
                            break
                        }
                    }
                    try {
                        tg?.release()
                    } catch (e: Exception) {}
                    if (activeToneGenerator == tg) {
                        activeToneGenerator = null
                    }
                }
                generatorThread?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        stopRunnable = Runnable {
            stop()
        }
        handler.postDelayed(stopRunnable!!, durationSec * 1000L)
    }

    fun stop() {
        isPlaying = false
        stopRunnable?.let { handler.removeCallbacks(it) }
        try {
            ringtone?.stop()
        } catch (e: Exception) {}
        ringtone = null

        try {
            activeToneGenerator?.stopTone()
        } catch (e: Exception) {}
        try {
            activeToneGenerator?.release()
        } catch (e: Exception) {}
        activeToneGenerator = null

        try {
            generatorThread?.interrupt()
        } catch (e: Exception) {}
        generatorThread = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    modifier: Modifier = Modifier,
    title: String,
    domain: String,
    prizes: String,
    ringtoneName: String,
    durationSec: Int,
    repetition: String,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showSnoozeOptions by remember { mutableStateOf(false) }

    // Dynamic warning animated pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B), // Midnight Dark Navy
                        Color(0xFF431407)  // Deep Amber/Maroon Alarm Color
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper section with indicator & pulsing icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = "SYSTEM REGISTRATION COUNTDOWN ALERT",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFF87171),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Pulsing Alarm Symbol
            Surface(
                modifier = Modifier
                    .size(100.dp * scale)
                    .clip(CircleShape),
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                border = BorderStroke(2.dp, Color(0xFFEF4444))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NotificationImportant,
                        contentDescription = "Alert Pulsing Icon",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }

        // Mid section containing context info card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(2.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = domain.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Prizes: $prizes",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Alarm description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ringtone", fontSize = 11.sp, color = Color.Gray)
                        Text(text = ringtoneName, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ring Duration", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${durationSec}s", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Repetition", fontSize = 11.sp, color = Color.Gray)
                        Text(text = repetition, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Lower actions layout: Dismiss / Snooze trigger buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("alarm_activity_dismiss_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(32.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DISMISS ALARM",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showSnoozeOptions = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("alarm_activity_snooze_btn"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Snooze,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SNOOZE OPTIONS...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showSnoozeOptions) {
        AlertDialog(
            onDismissRequest = { showSnoozeOptions = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snooze Options")
                }
            },
            text = {
                Column {
                    Text("Select how many minutes you would like to delay this registration reminder:")
                    Spacer(modifier = Modifier.height(16.dp))

                    listOf(1, 5, 10, 15).forEach { mins ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSnooze(mins)
                                    showSnoozeOptions = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$mins Minute${if (mins > 1) "s" else ""}",
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
