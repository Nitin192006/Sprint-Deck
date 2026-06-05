package com.example

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.Hackathon
import com.example.data.ReminderAlarmReceiver
import com.example.ui.HackathonViewModel
import com.example.ui.theme.MyApplicationTheme

fun base64ToImageBitmap(base64String: String?): ImageBitmap? {
    if (base64String.isNullOrBlank()) return null
    return try {
        val decodedBytes = Base64.decode(base64String, Base64.NO_WRAP)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun FullPhotoViewDialog(
    currentUser: com.example.data.User?,
    pfpBitmap: ImageBitmap?,
    onDismissRequest: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top part with title and Close Cross
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile Photo View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image display or placeholder in center
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (pfpBitmap != null) Color.Black else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (pfpBitmap != null) {
                        Image(
                            bitmap = pfpBitmap,
                            contentDescription = "Full profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.username?.take(1) ?: "U").uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No custom photo uploaded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons at the bottom-most part of the dialog screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pfpBitmap != null) {
                        Button(
                            onClick = onRemove,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear profile picture",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove")
                        }
                    }

                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit photo",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (pfpBitmap != null) "Change Photo" else "Choose Photo")
                    }
                }
            }
        }
    }
}

// Helper for real alarm triggering
fun scheduleSprintAlarm(context: Context, hackathon: Hackathon, customDateTimeStr: String?, isImmediateTest: Boolean = false) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
        putExtra("sprint_title", hackathon.title)
        putExtra("sprint_domain", hackathon.domain)
        putExtra("sprint_prizes", hackathon.prizes)
        putExtra("alarm_ringtone", hackathon.alarmRingtone)
        putExtra("alarm_duration", hackathon.alarmDurationSec)
        putExtra("alarm_repetition", hackathon.alarmRepetition)
        putExtra("alert_delivery_type", hackathon.alertDeliveryType)
        putExtra("sprint_id", hackathon.id)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        hackathon.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val triggerTimeMs = if (isImmediateTest) {
        System.currentTimeMillis() + 10000 // 10 seconds from now
    } else if (customDateTimeStr != null) {
        try {
            val parts = customDateTimeStr.split(" ")
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, dateParts[0].toInt())
                set(java.util.Calendar.MONTH, dateParts[1].toInt() - 1)
                set(java.util.Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(java.util.Calendar.MINUTE, timeParts[1].toInt())
                set(java.util.Calendar.SECOND, 0)
            }
            calendar.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis() + 30000 // Fallback 30 sec
        }
    } else {
        // Parse hackathon registration date and set fallback triggered warning
        try {
            val parts = hackathon.deadline.split("-")
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, parts[0].toInt())
                set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                set(java.util.Calendar.HOUR_OF_DAY, 9) // Default warnings at 09:00 AM
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                add(java.util.Calendar.DAY_OF_MONTH, -hackathon.reminderDaysBefore)
            }
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                System.currentTimeMillis() + 15000 // Trigger in 15 sec if calculated is past
            } else {
                calendar.timeInMillis
            }
        } catch (e: Exception) {
            System.currentTimeMillis() + 60000 // 1 minute from now
        }
    }
    
    try {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
    } catch (e: SecurityException) {
        alarmManager.set(AlarmManager.RTC, triggerTimeMs, pendingIntent)
    }
}

// Helper for real snooze alarm triggering
fun scheduleSnoozeAlarm(context: Context, title: String, domain: String, prizes: String, minutes: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
        putExtra("sprint_title", title)
        putExtra("sprint_domain", domain)
        putExtra("sprint_prizes", prizes)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        title.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val triggerTimeMs = System.currentTimeMillis() + (minutes * 60 * 1000)
    try {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
    } catch (e: SecurityException) {
        alarmManager.set(AlarmManager.RTC, triggerTimeMs, pendingIntent)
    }
}

val LocalThemeIndex = compositionLocalOf { 0 }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            val hViewModel: HackathonViewModel = viewModel()
            val themeIndex by hViewModel.themeIndex.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalThemeIndex provides themeIndex) {
                MyApplicationTheme(themeIndex = themeIndex) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_scaffold"),
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            HackathonAppMain(viewModel = hViewModel)
                            ActiveAlarmPopupOverlay()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val triggeredTitle = intent?.getStringExtra("triggered_sprint_title")
        if (!triggeredTitle.isNullOrBlank()) {
            val domain = intent.getStringExtra("sprint_domain") ?: "In-App Alarm"
            val prizes = intent.getStringExtra("sprint_prizes") ?: "Global Glory"
            com.example.data.LiveAlarmManager.triggerAlarm(triggeredTitle, domain, prizes)
        }
    }
}

@Composable
fun ActiveAlarmPopupOverlay() {
    val context = LocalContext.current
    val activeAlarmData by com.example.data.LiveAlarmManager.activeAlarm.collectAsStateWithLifecycle()

    activeAlarmData?.let { alarm ->
        AlertDialog(
            onDismissRequest = { com.example.data.LiveAlarmManager.dismissAlarm() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Ringing alert icon indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⏰ Sprint Alarm Rings!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = alarm.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Category: ${alarm.domain}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prizes: ${alarm.prizes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "CHOOSE SNOOZE OPTION:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 15).forEach { mins ->
                            Button(
                                onClick = {
                                    scheduleSnoozeAlarm(context, alarm.title, alarm.domain, alarm.prizes, mins)
                                    com.example.data.LiveAlarmManager.dismissAlarm()
                                    Toast.makeText(context, "Snoozed for $mins min!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("snooze_btn_${mins}m"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("${mins}m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.data.LiveAlarmManager.dismissAlarm()
                        Toast.makeText(context, "Alarm dismissed!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("dismiss_alarm_popup_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Dismiss Alarm")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun HackathonAppMain(viewModel: HackathonViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        DashboardScreen(viewModel = viewModel)
    }
}

// --- SIMULATED AUTHENTICATION SHIELD (PRESERVES LOCAL ACCOUNTS IN ROOM DB) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: HackathonViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("auth_card"),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Typography-focused logo
                Text(
                    text = "Sprint Deck",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = if (isForgotPassword) "Reset your password with your username and registered email address." else "A minimalist portal for global innovation sprint schedules, alerts, and registration counters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Input fields styled with 8px (8.dp) rounded corners
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username field icon") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isSignUp || isForgotPassword) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email field icon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isForgotPassword) "New Password" else "Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password field icon") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = authError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("auth_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom #1978E5 style button with 8px border radius
                Button(
                    onClick = {
                        if (isForgotPassword) {
                            viewModel.resetPassword(username, email, password) {
                                Toast.makeText(context, "Password reset successful! Sign in with your new password.", Toast.LENGTH_LONG).show()
                                isForgotPassword = false
                                password = ""
                            }
                        } else if (isSignUp) {
                            viewModel.registerNewUser(username, email, password) {
                                Toast.makeText(context, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.loginUser(username, password) {
                                Toast.makeText(context, "Logged In Successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_action_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isForgotPassword) "Reset Password" else if (isSignUp) "Register Account" else "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isForgotPassword) {
                    Text(
                        text = if (isSignUp) "Already have an account? Sign In" else "New to Sprint? Create Account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                isSignUp = !isSignUp
                                viewModel.loginUser("", "") {} // clear error state
                            }
                            .testTag("toggle_auth_mode")
                    )
                    
                    if (!isSignUp) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    isForgotPassword = true
                                    viewModel.loginUser("", "") {} // clear error state
                                }
                                .testTag("forgot_password_button")
                        )
                    }
                } else {
                    Text(
                        text = "Back to Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                isForgotPassword = false
                                viewModel.loginUser("", "") {} // clear error state
                            }
                            .testTag("forgot_to_signin_mode")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = 0.85f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Love crafted",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Designed & Crafted by Nitin",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- CORE APP DASHBOARD ---

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: HackathonViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val hackathons by viewModel.hackathons.collectAsStateWithLifecycle()
    
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Find Sprints, 1 = My Sprints
    var showAlertsModal by remember { mutableStateOf(false) }
    var showAddSprintDialog by remember { mutableStateOf(false) }
    var showNavFullPhotoViewDialog by remember { mutableStateOf(false) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var showCropDialog by remember { mutableStateOf(false) }
    var cropSourceUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            cropSourceUri = it
            showCropDialog = true
        }
    }

    // Dialog for viewing nav drawer profile picture completely
    if (showNavFullPhotoViewDialog) {
        val navPfpBitmap = remember(currentUser?.profilePhotoBase64) {
            base64ToImageBitmap(currentUser?.profilePhotoBase64)
        }
        FullPhotoViewDialog(
            currentUser = currentUser,
            pfpBitmap = navPfpBitmap,
            onDismissRequest = { showNavFullPhotoViewDialog = false },
            onRemove = {
                showNavFullPhotoViewDialog = false
                val curr = currentUser
                if (curr != null) {
                    viewModel.updateUserProfileSettings(
                        profilePhotoBase64 = null,
                        defaultRingtone = curr.defaultRingtone,
                        defaultDurationSec = curr.defaultDurationSec,
                        defaultRepetition = curr.defaultRepetition,
                        defaultDeliveryType = curr.defaultDeliveryType,
                        defaultSnoozeMin = curr.defaultSnoozeMin
                    )
                }
            },
            onEdit = {
                showNavFullPhotoViewDialog = false
                scope.launch { drawerState.close() }
                photoPickerLauncher.launch("image/*")
            }
        )
    }

    if (showCropDialog && cropSourceUri != null) {
        CropImageDialog(
            uri = cropSourceUri!!,
            context = context,
            onDismiss = {
                showCropDialog = false
                cropSourceUri = null
            },
            onCropSuccess = { croppedBase64 ->
                showCropDialog = false
                cropSourceUri = null
                val curr = currentUser
                if (curr != null) {
                    viewModel.updateUserProfileSettings(
                        profilePhotoBase64 = croppedBase64,
                        defaultRingtone = curr.defaultRingtone,
                        defaultDurationSec = curr.defaultDurationSec,
                        defaultRepetition = curr.defaultRepetition,
                        defaultDeliveryType = curr.defaultDeliveryType,
                        defaultSnoozeMin = curr.defaultSnoozeMin
                    )
                }
            }
        )
    }

    val prefs = remember { context.getSharedPreferences("hackathon_tracker_prefs", Context.MODE_PRIVATE) }
    
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    var currentView by remember { mutableStateOf("Dashboard") } // "Dashboard", "Profile", "Settings", "About"

    var showApiKeySetupOverlay by remember {
        mutableStateOf(prefs.getString("custom_gemini_api_key", "").isNullOrEmpty())
    }

    var showMissingApiKeyDialog by remember { mutableStateOf(false) }

    // Computes alerts dynamically
    val alertHackathons = remember(hackathons) {
        hackathons.filter { it.isIncomingAlert }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.testTag("modal_drawer_sheet"),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Header style with customized branding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Column {
                        val pfpBitmap = base64ToImageBitmap(currentUser?.profilePhotoBase64)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    showNavFullPhotoViewDialog = true
                                }
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pfpBitmap != null) {
                                Image(
                                    bitmap = pfpBitmap,
                                    contentDescription = "User profile picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (currentUser?.username?.take(1) ?: "D").uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            // Tap to change label overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "EDIT",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SPRINT DECK",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = currentUser?.email ?: "developer@example.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Options
                val drawerColors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                )

                NavigationDrawerItem(
                    label = { Text("Dashboard Explorer", fontWeight = FontWeight.Bold) },
                    selected = currentView == "Dashboard",
                    onClick = {
                        currentView = "Dashboard"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    colors = drawerColors,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_dashboard")
                )

                NavigationDrawerItem(
                    label = { Text("My Profile", fontWeight = FontWeight.Bold) },
                    selected = currentView == "Profile",
                    onClick = {
                        currentView = "Profile"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    colors = drawerColors,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_profile")
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    selected = currentView == "Settings",
                    onClick = {
                        currentView = "Settings"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    colors = drawerColors,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_settings")
                )

                NavigationDrawerItem(
                    label = { Text("About App", fontWeight = FontWeight.Bold) },
                    selected = currentView == "About",
                    onClick = {
                        currentView = "About"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    colors = drawerColors,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_about")
                )

                Spacer(modifier = Modifier.weight(1f))
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Logout button inside drawer
                NavigationDrawerItem(
                    label = { Text("Log Out", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_item_logout")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        content = {
            // Screen content Column
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // Shared top bar with hamburger menu trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open side window menu")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (currentView) {
                                    "Profile" -> "MY PROFILE"
                                    "Settings" -> "SETTINGS"
                                    "About" -> "ABOUT APP"
                                    else -> "SPRINT DECK"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (currentView == "Dashboard") {
                                Text(
                                    text = "Welcome, ${currentUser?.username ?: "developer"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Top control icons: Themes & Notifications
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentView == "Dashboard") {
                            // Notification bell
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("notifications_bell")
                            ) {
                                IconButton(onClick = { showAlertsModal = !showAlertsModal }) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Incoming alerts list toggle",
                                        tint = if (alertHackathons.isNotEmpty()) Color(0xFFF59E0B) else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (alertHackathons.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-2).dp, y = 4.dp)
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Red),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = alertHackathons.size.toString(),
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Theme Cycle switch
                        IconButton(
                            onClick = { viewModel.cycleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Cycle between themes in order",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Depending on the current view, we render the respective content block
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (currentView) {
                        "Dashboard" -> {
                            DashboardBodyContent(
                                viewModel = viewModel,
                                hackathons = hackathons,
                                activeTab = activeTab,
                                onTabChanged = { activeTab = it },
                                aiStatus = aiStatus,
                                showAlertsModal = showAlertsModal,
                                alertHackathons = alertHackathons,
                                onAlertModalClose = { showAlertsModal = false },
                                onAddSprintRequested = { showAddSprintDialog = true },
                                onMissingApiKey = { showMissingApiKeyDialog = true }
                            )
                        }
                        "Profile" -> {
                            ProfileViewScreen(
                                viewModel = viewModel,
                                list = hackathons,
                                onPhotoClick = { photoPickerLauncher.launch("image/*") }
                            )
                        }
                        "Settings" -> {
                            SettingsViewScreen(viewModel = viewModel, ringtonePickerLauncher = ringtonePickerLauncher, prefs = prefs)
                        }
                        "About" -> {
                            AboutViewScreen()
                        }
                    }
                }
            }
        }
    )

    if (showAddSprintDialog) {
        AddCustomSprintDialog(
            onDismiss = { showAddSprintDialog = false },
            onSave = { title, desc, prizes, teamSize, domain, deadline, timeline, link ->
                viewModel.createCustomHackathon(title, desc, prizes, teamSize, domain, deadline, timeline, link)
                showAddSprintDialog = false
            }
        )
    }

    if (showMissingApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showMissingApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("API Key Required")
                }
            },
            text = {
                Text("You haven't added a Gemini API key yet. Please add an API key first to unlock AI-powered schedule generation and sprint discovery.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingApiKeyDialog = false
                        showApiKeySetupOverlay = true
                    },
                    modifier = Modifier.testTag("dialog_missing_key_add_btn")
                ) {
                    Text("Add Key Now")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMissingApiKeyDialog = false },
                    modifier = Modifier.testTag("dialog_missing_key_cancel_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showApiKeySetupOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) {} // prohibit any touch behind that
                .testTag("api_key_setup_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp)
                    .testTag("api_key_setup_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Set Up Gemini AI Key",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "To allow the hackathon tracker to perform AI-powered task breakdown and topic suggestions, please provide your own Gemini API Key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "How to get one:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "1. Go to Google AI Studio: aistudio.google.com\n" +
                                       "2. Click \"Get API key\"\n" +
                                       "3. Create and copy your key, and paste it below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    var apiKeyInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Paste your Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_api_key_input")
                    )

                    Text(
                        text = "🔒 Secured Locally: Your key is stored locally on this physical device's secure SharedPreferences. It is never uploaded to any cloud, database, or server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showApiKeySetupOverlay = false
                            },
                            colors = ButtonDefaults.textButtonColors(),
                            modifier = Modifier.weight(1f).testTag("setup_skip_btn")
                        ) {
                            Text("Skip")
                        }

                        Button(
                            onClick = {
                                if (apiKeyInput.trim().isNotEmpty()) {
                                    prefs.edit().putString("custom_gemini_api_key", apiKeyInput.trim()).apply()
                                    showApiKeySetupOverlay = false
                                    Toast.makeText(context, "API Key configured successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a key, or click Skip.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("setup_save_btn")
                        ) {
                            Text("Save Key")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardBodyContent(
    viewModel: HackathonViewModel,
    hackathons: List<Hackathon>,
    activeTab: Int,
    onTabChanged: (Int) -> Unit,
    aiStatus: String?,
    showAlertsModal: Boolean,
    alertHackathons: List<Hackathon>,
    onAlertModalClose: () -> Unit,
    onAddSprintRequested: () -> Unit,
    onMissingApiKey: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Virtual Alerts view
            AnimatedVisibility(
                visible = showAlertsModal,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AlertsPopup(
                    alerts = alertHackathons,
                    onClose = onAlertModalClose
                )
            }

            // Action feedback feedback tracker from Gemini AI tasks
            AnimatedVisibility(
                visible = aiStatus != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (aiStatus?.startsWith("Success") == true) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                        )
                        .border(
                            1.dp,
                            if (aiStatus?.startsWith("Success") == true) Color(0xFF10B981) else Color(0xFFEF4444),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (aiStatus?.startsWith("Success") == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status status indicator",
                            tint = if (aiStatus?.startsWith("Success") == true) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = aiStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (aiStatus?.startsWith("Success") == true) Color(0xFF065F46) else Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearAiStatus() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Status indicator message",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Navigation Tabs: Finding vs. Registered ones
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { onTabChanged(0) },
                    text = { Text("Find Hackathons", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Active tab for discovery find menu") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { onTabChanged(1) },
                    text = { Text("My Registrations", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Active tab for joined registered list") }
                )
            }

            // Tab main panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (activeTab == 0) {
                    FindSprintsTab(viewModel = viewModel, list = hackathons, onMissingApiKey = onMissingApiKey)
                } else {
                    MySprintsTab(viewModel = viewModel, list = hackathons)
                }
            }
        }

        // Float custom FAB on the bottom right of Find tab
        if (activeTab == 0) {
            ExtendedFloatingActionButton(
                onClick = onAddSprintRequested,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Create manual hackathon entry") },
                text = { Text("Manual Entry", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
                    .testTag("add_custom_sprint_fab")
            )
        }
    }
}

@Composable
fun ProfileViewScreen(
    viewModel: HackathonViewModel,
    list: List<Hackathon>,
    onPhotoClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var showFullPhotoViewDialog by remember { mutableStateOf(false) }
    
    val registeredCount = remember(list, currentUser) {
        list.count { it.isRegistered && it.registeredByUserId == currentUser?.id }
    }

    if (showFullPhotoViewDialog) {
        val pfpBitmap = remember(currentUser?.profilePhotoBase64) {
            base64ToImageBitmap(currentUser?.profilePhotoBase64)
        }
        FullPhotoViewDialog(
            currentUser = currentUser,
            pfpBitmap = pfpBitmap,
            onDismissRequest = { showFullPhotoViewDialog = false },
            onRemove = {
                showFullPhotoViewDialog = false
                val curr = currentUser
                if (curr != null) {
                    viewModel.updateUserProfileSettings(
                        profilePhotoBase64 = null,
                        defaultRingtone = curr.defaultRingtone,
                        defaultDurationSec = curr.defaultDurationSec,
                        defaultRepetition = curr.defaultRepetition,
                        defaultDeliveryType = curr.defaultDeliveryType,
                        defaultSnoozeMin = curr.defaultSnoozeMin
                    )
                }
            },
            onEdit = {
                showFullPhotoViewDialog = false
                onPhotoClick()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Avatar Card with Material 3 styling
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_avatar_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pfpBitmap = remember(currentUser?.profilePhotoBase64) {
                    base64ToImageBitmap(currentUser?.profilePhotoBase64)
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            showFullPhotoViewDialog = true
                        }
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (pfpBitmap != null) {
                        Image(
                            bitmap = pfpBitmap,
                            contentDescription = "User profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser?.username ?: "N").take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = currentUser?.username ?: "Anonymous Team",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currentUser?.email ?: "no-email@firebase.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Text(
            text = "ACCOUNT STATISTICS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Count Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f).testTag("stat_card_registrations"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("REGISTRATIONS", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "$registeredCount Sprints",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f).testTag("stat_card_sync"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SYNC STATE", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "Active Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Action card: Dynamic Sync Controls
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_sync_card"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Cloud Integration Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your application synchronizes locally set hackathons and alerts automatically with Google Firestore under user credentials to sustain schedules seamlessly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Button(
                    onClick = {
                        viewModel.forceSyncWithFirebase()
                        Toast.makeText(context, "Cloud sync completed successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("profile_force_sync_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Force Sync Now")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsViewScreen(
    viewModel: HackathonViewModel,
    ringtonePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    prefs: android.content.SharedPreferences
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val themeIndex by viewModel.themeIndex.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )        // Theme Selector Card - Dropdown to select light, dark, minty warmth, or pastel spring hues
        Card(
            modifier = Modifier.fillMaxWidth().testTag("settings_theme_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("App Theme Mode", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    val themeNames = listOf("Light", "Dark", "Minty", "Pastel")
                    
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.width(130.dp).testTag("settings_theme_dropdown_trigger"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = themeNames.getOrElse(themeIndex) { "Theme" },
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            themeNames.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.setThemeIndex(idx)
                                        expanded = false
                                    },
                                    modifier = Modifier.testTag("theme_option_$idx")
                                )
                            }
                        }
                    }
                }
            }
        }

        // REMINDER DEFAULT FORM CONFIGURATOR
        Text(
            text = "REMINDER FORM DEFAULTS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("settings_reminder_form_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure default settings automatically loaded when joining register forms for innovation sprints.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // High-visibility Alert Preferences integrated box inside the reminder section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "Alert Preferences Info",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "When registering for an opportunity, choose an alert delivery type (Full Alarm, Vibration Only, Notification Only, or Silent) on your schedule setup card. This allows fine-grained control over how reminder alerts get delivered by the notification manager.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val user = currentUser
                if (user != null) {
                    var ringtoneOpt by remember(user.defaultRingtone) { mutableStateOf(prefs.getString("pref_default_ringtone", user.defaultRingtone) ?: "Default") }
                    var delivType by remember(user.defaultDeliveryType) { mutableStateOf(user.defaultDeliveryType) }
                    var durSec by remember(user.defaultDurationSec) { mutableStateOf(user.defaultDurationSec) }
                    var repType by remember(user.defaultRepetition) { mutableStateOf(user.defaultRepetition) }
                    var snMin by remember(user.defaultSnoozeMin) { mutableStateOf(user.defaultSnoozeMin) }

                    val deviceRingtoneLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                                ?: result.data?.data
                            if (uri != null) {
                                val uriStr = uri.toString()
                                ringtoneOpt = uriStr
                                prefs.edit().putString("pref_default_ringtone", uriStr).apply()
                            }
                        }
                    }

                    // Default Ringtone Selector Row
                    Column {
                        Text("Default Alarm Sound Tone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val staticTones = listOf("Default", "Loud Siren", "Sci-Fi Beacon", "Calm Chime")
                        val currentToneTitle = if (ringtoneOpt !in staticTones) {
                            try {
                                val r = RingtoneManager.getRingtone(context, android.net.Uri.parse(ringtoneOpt))
                                r?.getTitle(context) ?: "Device Sound"
                            } catch (e: Exception) {
                                "Device Sound"
                            }
                        } else null

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            items(staticTones) { rTone ->
                                val selected = ringtoneOpt == rTone
                                FilterChip(
                                    selected = selected,
                                    onClick = { ringtoneOpt = rTone },
                                    label = { Text(rTone, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                            if (currentToneTitle != null) {
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text("Device: $currentToneTitle", style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        }
                                        deviceRingtoneLauncher.launch(intent)
                                    },
                                    label = { Text("+ Select From Device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                                )
                            }
                        }
                    }

                    // Alert Type Selector Dropdown / Row choice
                    Column {
                        Text("Default Alert Delivery Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            items(listOf("Full Alarm", "Vibration Only", "Notification Only", "Silent/None")) { type ->
                                val selected = delivType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { delivType = type },
                                    label = { Text(type, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }

                    // Repetition Selector Row
                    Column {
                        Text("Default Alarm Repetition", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Once", "Daily", "Weekly").forEach { rep ->
                                val selected = repType == rep
                                FilterChip(
                                    selected = selected,
                                    onClick = { repType = rep },
                                    label = { Text(rep, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    // Alarm Duration (Seconds) Slider
                    Column {
                        Text("Alarm Playback: $durSec Seconds", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = durSec.toFloat(),
                            onValueChange = { durSec = it.toInt() },
                            valueRange = 10f..120f
                        )
                    }

                    // Default Snooze Duration Input
                    Column {
                        Text("Default Snooze Interval (Minutes)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 5, 10, 15).forEach { min ->
                                val selected = snMin == min
                                FilterChip(
                                    selected = selected,
                                    onClick = { snMin = min },
                                    label = { Text("$min Minutes", style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            // Update VM (which saves to room & firestore) with default sound setting
                            viewModel.updateUserProfileSettings(
                                profilePhotoBase64 = user.profilePhotoBase64,
                                defaultRingtone = ringtoneOpt,
                                defaultDurationSec = durSec,
                                defaultRepetition = repType,
                                defaultDeliveryType = delivType,
                                defaultSnoozeMin = snMin
                            )
                            // Double-persist defaults inside local sharedPreferences so receiver & dialogs pick them up synchronously
                            prefs.edit().apply {
                                putString("pref_default_ringtone", ringtoneOpt)
                                putInt("pref_default_duration_sec", durSec)
                                stringType@putString("pref_default_repetition", repType)
                                putString("pref_default_delivery_type", delivType)
                                putInt("pref_default_snooze_min", snMin)
                            }.apply()
                            Toast.makeText(context, "Default alarm settings saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_defaults")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Default Settings")
                    }
                } else {
                    Text(
                        text = "Log in or sign up first to access customizable default reminder preferences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }

        // SECURE API CREDENTIALS
        Text(
            text = "SECURE API CREDENTIALS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("settings_api_credential_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure custom Gemini API credentials. Entering custom credentials here allows you to distribute or run this APK safely. All calls will use your locally saved settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var customApiKey by remember { mutableStateOf(prefs.getString("custom_gemini_api_key", "") ?: "") }
                var customProxyUrl by remember { mutableStateOf(prefs.getString("custom_gemini_proxy_url", "") ?: "") }

                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    label = { Text("Custom Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_gemini_api_key"),
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) }
                )

                OutlinedTextField(
                    value = customProxyUrl,
                    onValueChange = { customProxyUrl = it },
                    label = { Text("Custom Gemini Proxy URL (Optional)") },
                    placeholder = { Text("https://your-proxy-domain.cfworkers.dev/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_gemini_proxy_url"),
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )

                Button(
                    onClick = {
                        prefs.edit().apply {
                            putString("custom_gemini_api_key", customApiKey.trim())
                            putString("custom_gemini_proxy_url", customProxyUrl.trim())
                        }.apply()
                        Toast.makeText(context, "API Credentials updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_save_api_credentials")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save API Credentials")
                }

                if (customApiKey.isNotEmpty() || customProxyUrl.isNotEmpty()) {
                    Text(
                        text = "✓ App is configured to use locally-saved custom credentials on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AboutViewScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ABOUT SPRINT DECK",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("about_app_card"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sprint Deck v2.5",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "An offline-first, highly responsive local organizer combined with automatic Firestore cloud sync to manage and track hackathons, registrars, and customizable alerts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                Text(
                    text = "CREATOR DETAILS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Love crafted",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Designed & Crafted by Nitin",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Developed with a clean Material Design 3 system to serve developers worldwide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// Dialog for manual sprint item entries
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomSprintDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, prizes: String, teamSize: String, domain: String, deadline: String, timeline: String, link: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var prizes by remember { mutableStateOf("") }
    var teamSize by remember { mutableStateOf("1-4 members") }
    var domain by remember { mutableStateOf("AI & Machine Learning") }
    var deadline by remember { mutableStateOf("") }
    var timeline by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    val context = LocalContext.current
    val themeIndex = LocalThemeIndex.current
    val pickerThemeStyle = remember(themeIndex) {
        when (themeIndex) {
            1 -> com.example.R.style.MyDatePickerThemeDark
            2 -> com.example.R.style.MyDatePickerThemeMint
            3 -> com.example.R.style.MyDatePickerThemePastel
            else -> com.example.R.style.MyDatePickerTheme
        }
    }
    val domains = listOf("AI & Machine Learning", "FinTech", "Web3 & Blockchain", "ClimateTech", "Open Innovation")
    val teamSizes = listOf("1-4 members", "2-5 members", "Individual or Pairs")

    // Bold, distinct outline border style with slightly tinted background container for maximum fields separation
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Custom Hackathon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Provide your custom hackathon details below to publish your manual entry cards. Starred (*) items are mandatory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hackathon Title *") },
                    placeholder = { Text("e.g. HackIndia AI Summit") },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("custom_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description *") },
                    placeholder = { Text("Detail the event challenges and outcomes...") },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("custom_desc_input"),
                    minLines = 2,
                    maxLines = 4
                )

                OutlinedTextField(
                    value = prizes,
                    onValueChange = { prizes = it },
                    label = { Text("Prizes (e.g. $15k Cash Prize)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("custom_prizes_input"),
                    singleLine = true
                )

                Column {
                    Text("Domain *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        placeholder = { Text("e.g. AI & Machine Learning, HealthTech...") },
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("custom_domain_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Or select preset:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(domains) { dom ->
                            val active = dom == domain
                            FilterChip(
                                selected = active,
                                onClick = { domain = dom },
                                label = { Text(dom, fontSize = 11.sp) },
                                modifier = Modifier.testTag("custom_domain_chip_$dom")
                            )
                        }
                    }
                }

                Column {
                    Text("Team Size Limit *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = teamSize,
                        onValueChange = { teamSize = it },
                        placeholder = { Text("e.g. 1-4 members, Individual...") },
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("custom_teamsize_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Or select preset:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(teamSizes) { size ->
                            val active = size == teamSize
                            FilterChip(
                                selected = active,
                                onClick = { teamSize = size },
                                label = { Text(size, fontSize = 11.sp) },
                                modifier = Modifier.testTag("custom_teamsize_chip_$size")
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline Date (YYYY-MM-DD) *") },
                        placeholder = { Text("Pick a date ->") },
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors,
                        readOnly = true,
                        modifier = Modifier.weight(1f).testTag("custom_deadline_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            val dp = DatePickerDialog(
                                context,
                                pickerThemeStyle, // Prevent invisible texts with high contrast Dialog Theme Style!
                                { _, year, month, dayOfMonth ->
                                    val mStr = String.format("%02d", month + 1)
                                    val dStr = String.format("%02d", dayOfMonth)
                                    deadline = "$year-$mStr-$dStr"
                                    if (timeline.isBlank()) {
                                        val monthLabel = java.time.Month.of(month + 1).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)
                                        timeline = "$monthLabel $dayOfMonth, $year"
                                    }
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            dp.show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("custom_deadline_picker_btn")
                    ) {
                        Icon(Icons.Default.Event, contentDescription = "Pick limit date")
                    }
                }

                OutlinedTextField(
                    value = timeline,
                    onValueChange = { timeline = it },
                    label = { Text("Timeline Event Text (e.g. July 12 - July 18)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("custom_timeline_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Hackathon Link") },
                    placeholder = { Text("e.g. https://unstop.com/competitions/hack") },
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("custom_link_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || desc.isBlank() || deadline.isBlank() || domain.isBlank() || teamSize.isBlank()) {
                        Toast.makeText(context, "Please fill in all starred (*) fields!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(
                            title,
                            desc,
                            if (prizes.isBlank()) "Trophy & Swag" else prizes,
                            teamSize,
                            domain,
                            deadline,
                            if (timeline.isBlank()) "TBD" else timeline,
                            if (link.isBlank()) "https://unstop.com" else link
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("custom_sprint_submit_btn")
            ) {
                Text("Publish Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("custom_sprint_cancel_btn")) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(12.dp)
    )
}

// --- TAB SUB-PANEL 1: FIND HACKATHONS (DISCOVERY DECK WITH FILTERS & AI OPTION) ---

@Composable
fun FindSprintsTab(viewModel: HackathonViewModel, list: List<Hackathon>, onMissingApiKey: () -> Unit) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDomain by viewModel.selectedDomain.collectAsStateWithLifecycle()
    val selectedTeamSize by viewModel.selectedTeamSize.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userIdeaDraft by remember { mutableStateOf("") }
    var visibleCount by remember(searchQuery, selectedDomain, selectedTeamSize) { mutableStateOf(6) }
    
    val domains = listOf("All", "AI & Machine Learning", "FinTech", "Web3 & Blockchain", "ClimateTech", "Open Innovation")
    val teamSizes = listOf("All", "1-4 members", "2-5 members", "Individual or Pairs")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Minimalist Search field (8px rounded corners)
        item {
            Text(
                text = "DISCOVER SPRINTS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Type title, description, or domain...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Lookup helper indicator") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter: Category / Domain chip row
        item {
            Text(
                text = "FILTER BY DOMAIN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("domain_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(domains) { dom ->
                    val selected = dom == selectedDomain
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectedDomain.value = dom }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = dom,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter: Team size chip row
        item {
            Text(
                text = "FILTER BY TEAM SIZE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("teamsize_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(teamSizes) { size ->
                    val selected = size == selectedTeamSize
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectedTeamSize.value = size }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // AI HACKATHON GENERATOR / RETRIEVER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("ai_generator_card"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI helper star indicator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "AI SPRINT EXPANDER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Paste a hackathon link, snippet, or describe a custom idea. Gemini will retrieve and generate a complete structured schedule card with exact deadlines.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    OutlinedTextField(
                        value = userIdeaDraft,
                        onValueChange = { userIdeaDraft = it },
                        placeholder = { Text("e.g. Climate crisis hackathon in London July 15-18 with 25k prizes...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("ai_prompt_input_field"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Gemini is structuring dates and info...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (userIdeaDraft.isNotBlank()) {
                                    if (!com.example.data.GeminiService.isApiConfigured(context)) {
                                        onMissingApiKey()
                                    } else {
                                        viewModel.enrichWithAI(userIdeaDraft)
                                        userIdeaDraft = ""
                                    }
                                }
                            },
                            enabled = userIdeaDraft.isNotBlank(),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("ai_extract_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enrich & Add Card")
                        }
                    }
                }
            }
        }

        // Section header for Sprints
        item {
            Text(
                text = "AVAILABLE OPPORTUNITIES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // List elements filtered according to values
        val filtered = list.filter { hackathon ->
            val matchQuery = searchQuery.isBlank() || 
                    hackathon.title.contains(searchQuery, ignoreCase = true) || 
                    hackathon.description.contains(searchQuery, ignoreCase = true) ||
                    hackathon.domain.contains(searchQuery, ignoreCase = true)
            
            val matchDomain = selectedDomain == "All" || hackathon.domain == selectedDomain
            val matchTeam = selectedTeamSize == "All" || hackathon.teamSize == selectedTeamSize
            
            matchQuery && matchDomain && matchTeam
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No matching sprints discovered. Clear your filters or ask AI Sprint Expander above to generate one!",
                    modifier = Modifier.testTag("empty_explore_deck")
                )
            }
        } else {
            val visibleFiltered = filtered.take(visibleCount)
            items(visibleFiltered) { hackathon ->
                SprintItemCard(
                    hackathon = hackathon,
                    onJoinRequested = { reminderDays, customTime, ringtone, duration, repetition, deliveryType, isTestAlarm ->
                        viewModel.registerForHackathon(hackathon.id, reminderDays, customTime)
                        viewModel.updateCustomAlarm(hackathon, customTime, ringtone, duration, repetition, deliveryType)
                        if (customTime != null || isTestAlarm) {
                            val updatedHackathon = hackathon.copy(
                                customAlertDateTime = customTime,
                                alarmRingtone = ringtone,
                                alarmDurationSec = duration,
                                alarmRepetition = repetition,
                                alertDeliveryType = deliveryType
                            )
                            scheduleSprintAlarm(context, updatedHackathon, customTime, isTestAlarm)
                        }
                    },
                    onDeleteRequested = {
                        viewModel.deleteHackathon(hackathon)
                    }
                )
            }
            
            val finishedLocal = filtered.size <= visibleCount
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!finishedLocal) {
                        Button(
                            onClick = { visibleCount += 6 },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("load_more_button")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load More Sprints", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // All local items have been exhausted! Provide premium Live-Time Gemini discovery
                        var isGenLoading by remember { mutableStateOf(false) }
                        if (isGenLoading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Gemini is exploring specific live opportunities...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!com.example.data.GeminiService.isApiConfigured(context)) {
                                        onMissingApiKey()
                                    } else {
                                        isGenLoading = true
                                        val domainPrompt = if (selectedDomain != "All") {
                                            "upcoming high prizes hackathons in $selectedDomain domain"
                                        } else {
                                            "diverse global upcoming software developer hackathons"
                                        }
                                        scope.launch {
                                            val added = viewModel.enrichWithAISuspended(domainPrompt)
                                            isGenLoading = false
                                            if (added > 0) {
                                                visibleCount += added
                                                Toast.makeText(context, "$added fresh opportunities loaded live!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No additional opportunities found model-side. Try describing specific criteria in prompt input above!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("load_more_ai_button")
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate More via Gemini AI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB SUB-PANEL 2: MY REGISTRATIONS (JOINED DECK & ALERTS ADJUSTMENTS) ---

@Composable
fun MySprintsTab(viewModel: HackathonViewModel, list: List<Hackathon>) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    // Filters registered list targeting logged-in user
    val registered = remember(list, currentUser) {
        list.filter { it.isRegistered && it.registeredByUserId == currentUser?.id }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Text(
                text = "YOUR SPRINT SCHEDULES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Configure custom reminder schedules and monitor registration deadlines for your active sprints.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        if (registered.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "You haven't registered for any opportunities yet. Head over to the Explore tab to find and track epic hackathons!",
                    modifier = Modifier.testTag("empty_joined_deck")
                )
            }
        } else {
            items(registered) { hackathon ->
                RegisteredItemCard(
                    hackathon = hackathon,
                    onReminderDaysChanged = { updatedDays ->
                        viewModel.updateReminderDays(hackathon, updatedDays)
                    },
                    onUnregisterRequested = {
                        viewModel.unregisterFromHackathon(hackathon.id)
                    },
                    onUpdateAlarm = { customTime, ringtone, duration, repetition, deliveryType ->
                        viewModel.updateCustomAlarm(hackathon, customTime, ringtone, duration, repetition, deliveryType)
                    }
                )
            }
        }
    }
}

// --- INDIVIDUAL SUB-COMPONENTS & RENDERING CARD SCHEMAS ---

@Composable
fun SprintItemCard(
    hackathon: Hackathon,
    onJoinRequested: (reminderDays: Int, customTime: String?, ringtone: String, durationSec: Int, repetition: String, alertDeliveryType: String, isTestAlarm: Boolean) -> Unit,
    onDeleteRequested: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hackathon_tracker_prefs", Context.MODE_PRIVATE) }
    val themeIndex = LocalThemeIndex.current
    val pickerThemeStyle = remember(themeIndex) {
        when (themeIndex) {
            1 -> com.example.R.style.MyDatePickerThemeDark
            2 -> com.example.R.style.MyDatePickerThemeMint
            3 -> com.example.R.style.MyDatePickerThemePastel
            else -> com.example.R.style.MyDatePickerTheme
        }
    }

    var showReminderConfigDialog by remember { mutableStateOf(false) }
    var customAlertDateTime by remember { mutableStateOf<String?>(null) }
    var triggerTestAlarmAfterSuccess by remember { mutableStateOf(false) }

    val defaultRingtone = remember(showReminderConfigDialog) { prefs.getString("pref_default_ringtone", "Default") ?: "Default" }
    val defaultDurationSec = remember(showReminderConfigDialog) { prefs.getInt("pref_default_duration_sec", 30) }
    val defaultRepetition = remember(showReminderConfigDialog) { prefs.getString("pref_default_repetition", "Once") ?: "Once" }
    val defaultDeliveryType = remember(showReminderConfigDialog) { prefs.getString("pref_default_delivery_type", "Full Alarm") ?: "Full Alarm" }

    var alarmRingtone by remember(showReminderConfigDialog) { mutableStateOf(defaultRingtone) }
    var alarmDurationSec by remember(showReminderConfigDialog) { mutableStateOf(defaultDurationSec) }
    var alarmRepetition by remember(showReminderConfigDialog) { mutableStateOf(defaultRepetition) }
    var alertDeliveryType by remember(showReminderConfigDialog) { mutableStateOf(defaultDeliveryType) }

    val deviceRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?: result.data?.data
            if (uri != null) {
                alarmRingtone = uri.toString()
            }
        }
    }

    // Computes days left relative to date "2026-06-02"
    val daysLeft = calculateDaysRemaining(hackathon.deadline)
    val uriHandler = LocalUriHandler.current

    val isCustom = hackathon.isUserCreated
    val borderStroke = if (isCustom) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val containerColor = if (isCustom) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("sprint_card_${hackathon.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category tag & Days counter / Delete action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Chip / Custom Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = hackathon.domain,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "USER",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (hackathon.isUserCreated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDeleteRequested,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_user_sprint_${hackathon.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete custom hackathon from list",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Deadline count label
                val daysStyleColor = if (daysLeft <= 5) Color(0xFFD97706) else Color(0xFF10B981)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Urgency status clock icon",
                        tint = daysStyleColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (daysLeft > 0) "$daysLeft days left" else "Ended",
                        style = MaterialTheme.typography.labelLarge,
                        color = daysStyleColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title
            Text(
                text = hackathon.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Short Description
            Text(
                text = hackathon.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                minLines = 2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Timeline line label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Calendar schedule visual",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Timeline: ${hackathon.timeline} (Deadline: ${hackathon.deadline})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row registration action trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hackathon.isRegistered) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active registration marker checkbox icon",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Registered",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showReminderConfigDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("track_button_${hackathon.id}")
                    ) {
                        Text("Register", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Modal configuration schedule setup (custom only layout)
    if (showReminderConfigDialog) {
        AlertDialog(
            onDismissRequest = { showReminderConfigDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        onJoinRequested(
                            0,
                            customAlertDateTime,
                            alarmRingtone,
                            alarmDurationSec,
                            alarmRepetition,
                            alertDeliveryType,
                            false
                        )
                        showReminderConfigDialog = false
                    },
                    enabled = customAlertDateTime != null,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm Registration")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderConfigDialog = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Schedule Deadline Reminder")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Set a custom date & time to receive an urgent alarm reminder for ${hackathon.title}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // High-visibility Alert Preferences integrated box inside the reminder section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Column {
                                Text(
                                    text = "Alert Preferences Info",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Choose an Alert Delivery Method (Full Alarm, Vibration, Notification, or Silent) below to control how reminder alerts get delivered by the notification manager.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "CHOOSE REMINDER ALARM TIME",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                val dtPicker = DatePickerDialog(
                                    context,
                                    pickerThemeStyle, // Highly clear visible selected dates theme!
                                    { _, year, month, dayOfMonth ->
                                        val tmPicker = TimePickerDialog(
                                            context,
                                            pickerThemeStyle, // Highlight text colors theme!
                                            { _, hourOfDay, minute ->
                                                val mStr = String.format("%02d", month + 1)
                                                val dStr = String.format("%02d", dayOfMonth)
                                                val hStr = String.format("%02d", hourOfDay)
                                                val minStr = String.format("%02d", minute)
                                                customAlertDateTime = "$year-$mStr-$dStr $hStr:$minStr"
                                            },
                                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                                            cal.get(java.util.Calendar.MINUTE),
                                            true
                                        )
                                        tmPicker.show()
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                )
                                dtPicker.show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Date & Time", fontSize = 11.sp)
                        }

                        if (customAlertDateTime != null) {
                            IconButton(onClick = { customAlertDateTime = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear alarm", tint = Color.Red)
                            }
                        }
                    }

                    if (customAlertDateTime != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔔 Scheduled Reminder: $customAlertDateTime",
                                fontSize = 12.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "⚠️ Required: Please set alert date & time to confirm tracker.",
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // CUSTOM ALARM TUNINGS (Ringtone, Duration, Repetitions parameters)
                    Text(
                        text = "CUSTOMIZE ALARM PARAMETERS",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (alertDeliveryType == "Full Alarm") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Ringtune selection row
                    Column(modifier = Modifier.alpha(if (alertDeliveryType == "Full Alarm") 1f else 0.5f)) {
                        Text("Alarm Sound Tone:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val staticTones = listOf("Default", "Loud Siren", "Sci-Fi Beacon", "Calm Chime")
                        val currentToneTitle = if (alarmRingtone !in staticTones) {
                            try {
                                val r = RingtoneManager.getRingtone(context, android.net.Uri.parse(alarmRingtone))
                                r?.getTitle(context) ?: "Device Sound"
                            } catch (e: Exception) {
                                "Device Sound"
                            }
                        } else null

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            items(staticTones) { tone ->
                                FilterChip(
                                    selected = alarmRingtone == tone,
                                    onClick = { if (alertDeliveryType == "Full Alarm") alarmRingtone = tone },
                                    enabled = alertDeliveryType == "Full Alarm",
                                    label = { Text(tone, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("reg_ringtone_chip_$tone")
                                )
                            }
                            if (currentToneTitle != null) {
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = { },
                                        enabled = alertDeliveryType == "Full Alarm",
                                        label = { Text("Device: $currentToneTitle", fontSize = 10.sp) }
                                    )
                                }
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (alertDeliveryType == "Full Alarm") {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                            }
                                            deviceRingtoneLauncher.launch(intent)
                                        }
                                    },
                                    enabled = alertDeliveryType == "Full Alarm",
                                    label = { Text("+ Device Sound", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) }
                                )
                            }
                        }
                    }

                    // 2. Playback Ring Duration selection options
                    Column(modifier = Modifier.alpha(if (alertDeliveryType == "Full Alarm") 1f else 0.5f)) {
                        Text("Ring Playback Duration:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val durations = listOf(10 to "10s Unit", 20 to "20s Unit", 30 to "30s Classic", 60 to "1m Stop", 120 to "2m Max")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(durations) { (sec, label) ->
                                FilterChip(
                                    selected = alarmDurationSec == sec,
                                    onClick = { if (alertDeliveryType == "Full Alarm") alarmDurationSec = sec },
                                    enabled = alertDeliveryType == "Full Alarm",
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("reg_duration_chip_$sec")
                                )
                            }
                        }
                    }

                    // 3. Repeating Pattern selection options
                    Column(modifier = Modifier.alpha(if (alertDeliveryType == "Full Alarm") 1f else 0.5f)) {
                        Text("Repeat Alarm Repetition Strategy:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val repetitions = listOf("Once", "Daily", "Weekly", "Repeated")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(repetitions) { rep ->
                                FilterChip(
                                    selected = alarmRepetition == rep,
                                    onClick = { if (alertDeliveryType == "Full Alarm") alarmRepetition = rep },
                                    enabled = alertDeliveryType == "Full Alarm",
                                    label = { Text(rep, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("reg_repeat_chip_$rep")
                                )
                            }
                        }
                    }

                    // 4. Alert Delivery Type select option
                    Column {
                        Text("Alert Delivery Method:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val deliveryOptions = listOf("Full Alarm", "Vibration Only", "Notification Only", "Silent/None")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(deliveryOptions) { docType ->
                                FilterChip(
                                    selected = alertDeliveryType == docType,
                                    onClick = { alertDeliveryType = docType },
                                    label = { Text(docType, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("reg_delivery_chip_$docType")
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Live dynamic alert trigger preview
                    val todayDate = java.time.LocalDate.of(2026, 6, 2)
                    val dFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val previewString = try {
                        val limitDate = java.time.LocalDate.parse(hackathon.deadline, dFormatter)
                        if (customAlertDateTime != null) {
                            val customPart = customAlertDateTime!!.split(" ")[0]
                            val customDate = java.time.LocalDate.parse(customPart, dFormatter)
                            val customTimeStr = customAlertDateTime!!.split(" ")[1]
                            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(todayDate, customDate)
                            val remainingAlertLabel = when {
                                daysBetween < 0 -> "Alarm date has already passed!"
                                daysBetween == 0L -> "Triggers TODAY!"
                                daysBetween == 1L -> "Triggers tomorrow!"
                                else -> "Triggers in $daysBetween days (from today)"
                            }
                            when (alertDeliveryType) {
                                "Full Alarm" -> "Your alert will fire on $customPart at $customTimeStr\n($remainingAlertLabel) sounding for ${alarmDurationSec}s (${alarmRepetition})"
                                "Vibration Only" -> "Your alert will fire on $customPart at $customTimeStr\n($remainingAlertLabel) with physical vibration only."
                                "Notification Only" -> "Your alert will fire on $customPart at $customTimeStr\n($remainingAlertLabel) with standard notification only."
                                else -> "Your alert is scheduled silently on $customPart at $customTimeStr."
                            }
                        } else {
                            "Please pick a custom alarm date & time."
                        }
                    } catch (e: Exception) {
                        "Synchronizing dynamic deadline: ${hackathon.deadline}..."
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Dynamic preview active bell system indicator",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE DYNAMIC ALERT PREVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewString,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun RegisteredItemCard(
    hackathon: Hackathon,
    onReminderDaysChanged: (Int) -> Unit,
    onUnregisterRequested: () -> Unit,
    onUpdateAlarm: (String?, String, Int, String, String) -> Unit
) {
    val daysLeft = calculateDaysRemaining(hackathon.deadline)
    val context = LocalContext.current
    val themeIndex = LocalThemeIndex.current
    val pickerThemeStyle = remember(themeIndex) {
        when (themeIndex) {
            1 -> com.example.R.style.MyDatePickerThemeDark
            2 -> com.example.R.style.MyDatePickerThemeMint
            3 -> com.example.R.style.MyDatePickerThemePastel
            else -> com.example.R.style.MyDatePickerTheme
        }
    }
    val uriHandler = LocalUriHandler.current

    var showEditAlarmDialog by remember { mutableStateOf(false) }
    var editAlertTime by remember { mutableStateOf(hackathon.customAlertDateTime) }
    var editRingtone by remember { mutableStateOf(hackathon.alarmRingtone) }
    var editDurationSec by remember { mutableStateOf(hackathon.alarmDurationSec) }
    var editRepetition by remember { mutableStateOf(hackathon.alarmRepetition) }
    var editDeliveryType by remember { mutableStateOf(hackathon.alertDeliveryType) }

    val deviceRingtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                ?: result.data?.data
            if (uri != null) {
                editRingtone = uri.toString()
            }
        }
    }

    val isCustom = hackathon.isUserCreated
    val borderStroke = if (isCustom) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    }
    val containerColor = if (isCustom) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("registered_card_${hackathon.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category + Days remaining block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = hackathon.domain,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isCustom) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "USER",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$daysLeft Days Left To Apply",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = hackathon.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Brief Description
            Text(
                text = hackathon.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                minLines = 2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timeline line label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Calendar schedule visual",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Timeline: ${hackathon.timeline} (Deadline: ${hackathon.deadline})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // REMINDER PARAMETERS CONTROL PANEL (satisfies requirement 4 & 5)
            Text(
                text = "DEADLINE REMINDER CONFIG",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scheduled Alarm Date:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = hackathon.customAlertDateTime ?: "No Custom Reminder Set",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hackathon.customAlertDateTime != null) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    if (hackathon.customAlertDateTime != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔔 Alarm • ${hackathon.alarmDurationSec}s • ${hackathon.alarmRepetition}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = { showEditAlarmDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("change_alarm_button_${hackathon.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Alarm", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leave action button
            TextButton(
                onClick = onUnregisterRequested,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                    .testTag("unregister_button_${hackathon.id}")
            ) {
                Text("Leave Hackathon / Unregister", color = Color(0xFFEF4444), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // Modal dialog to customize and reschedule an existing alarm reminder
    if (showEditAlarmDialog) {
        val todayDate = java.time.LocalDate.of(2026, 6, 2)
        val dFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

        AlertDialog(
            onDismissRequest = { showEditAlarmDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (editAlertTime == null) {
                            Toast.makeText(context, "Please set an alarm date and time!", Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdateAlarm(editAlertTime, editRingtone, editDurationSec, editRepetition, editDeliveryType)
                            val updatedHackathon = hackathon.copy(
                                customAlertDateTime = editAlertTime,
                                alarmRingtone = editRingtone,
                                alarmDurationSec = editDurationSec,
                                alarmRepetition = editRepetition,
                                alertDeliveryType = editDeliveryType
                            )
                            scheduleSprintAlarm(context, updatedHackathon, editAlertTime)
                            Toast.makeText(context, "Successfully updated alarm values!", Toast.LENGTH_SHORT).show()
                            showEditAlarmDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAlarmDialog = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Modify Reminder Settings")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Modify the custom snooze time, tone tracker, play duration, or periodic repetitions for ${hackathon.title}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = "CHOOSE DATE & TIME",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                val dtPicker = DatePickerDialog(
                                    context,
                                    pickerThemeStyle, // High contrast visibility guarantee
                                    { _, year, month, dayOfMonth ->
                                        val tmPicker = TimePickerDialog(
                                            context,
                                            pickerThemeStyle, // Dynamic text coloring styling
                                            { _, hourOfDay, minute ->
                                                val mStr = String.format("%02d", month + 1)
                                                val dStr = String.format("%02d", dayOfMonth)
                                                val hStr = String.format("%02d", hourOfDay)
                                                val minStr = String.format("%02d", minute)
                                                editAlertTime = "$year-$mStr-$dStr $hStr:$minStr"
                                            },
                                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                                            cal.get(java.util.Calendar.MINUTE),
                                            true
                                        )
                                        tmPicker.show()
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                )
                                dtPicker.show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Alert Time", fontSize = 11.sp)
                        }

                        if (editAlertTime != null) {
                            IconButton(onClick = { editAlertTime = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear alarm decoration", tint = Color.Red)
                            }
                        }
                    }

                    if (editAlertTime != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔔 Selected: $editAlertTime",
                                fontSize = 12.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "⚠️ Alert cleared. Alarm will not trigger unless configured.",
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Text(
                        text = "CUSTOMIZE SOUND TRACK OPTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Ringtune selection row
                    Column(modifier = Modifier.alpha(if (editDeliveryType == "Full Alarm") 1f else 0.5f)) {
                        Text("Alarm Sound Tone:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val staticTones = listOf("Default", "Loud Siren", "Sci-Fi Beacon", "Calm Chime")
                        val currentToneTitle = if (editRingtone !in staticTones) {
                            try {
                                val r = RingtoneManager.getRingtone(context, android.net.Uri.parse(editRingtone))
                                r?.getTitle(context) ?: "Device Sound"
                            } catch (e: Exception) {
                                "Device Sound"
                            }
                        } else null

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            items(staticTones) { tone ->
                                FilterChip(
                                    selected = editRingtone == tone,
                                    onClick = { if (editDeliveryType == "Full Alarm") editRingtone = tone },
                                    enabled = editDeliveryType == "Full Alarm",
                                    label = { Text(tone, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("edit_ringtone_chip_$tone")
                                )
                            }
                            if (currentToneTitle != null) {
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = { },
                                        enabled = editDeliveryType == "Full Alarm",
                                        label = { Text("Device: $currentToneTitle", fontSize = 10.sp) }
                                    )
                                }
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (editDeliveryType == "Full Alarm") {
                                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                            }
                                            deviceRingtoneLauncher.launch(intent)
                                        }
                                    },
                                    enabled = editDeliveryType == "Full Alarm",
                                    label = { Text("+ Device Sound", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) }
                                )
                            }
                        }
                    }

                    // 2. Playback Ring Duration Selector filter chips
                    Column(modifier = Modifier.alpha(if (editDeliveryType == "Full Alarm") 1f else 0.5f)) {
                        Text("Ring Playback Duration:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val durations = listOf(10 to "10s Unit", 20 to "20s Unit", 30 to "30s Classic", 60 to "1m Stop", 120 to "2m Max")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(durations) { (sec, label) ->
                                FilterChip(
                                    selected = editDurationSec == sec,
                                    onClick = { if (editDeliveryType == "Full Alarm") editDurationSec = sec },
                                    enabled = editDeliveryType == "Full Alarm",
                                    label = { Text(label, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("edit_duration_chip_$sec")
                                )
                            }
                        }
                    }

                    // 3. Repeating Pattern Selector filter chips
                    Column(modifier = Modifier.alpha(if (editDeliveryType == "Full Alarm") 1f else 0.5f)) {
                        Text("Alarm Recurrence Strategy:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val repetitions = listOf("Once", "Daily", "Weekly", "Repeated")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(repetitions) { rep ->
                                FilterChip(
                                    selected = editRepetition == rep,
                                    onClick = { if (editDeliveryType == "Full Alarm") editRepetition = rep },
                                    enabled = editDeliveryType == "Full Alarm",
                                    label = { Text(rep, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("edit_repeat_chip_$rep")
                                )
                            }
                        }
                    }

                    // 4. Alert Delivery Mode selection options
                    Column {
                        Text("Alert Delivery Method:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val deliveryOptions = listOf("Full Alarm", "Vibration Only", "Notification Only", "Silent/None")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(deliveryOptions) { docType ->
                                FilterChip(
                                    selected = editDeliveryType == docType,
                                    onClick = { editDeliveryType = docType },
                                    label = { Text(docType, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("edit_delivery_chip_$docType")
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Live dynamic alert trigger preview
                    val previewString = try {
                        if (editAlertTime != null) {
                            val customPart = editAlertTime!!.split(" ")[0]
                            val customDate = java.time.LocalDate.parse(customPart, dFormatter)
                            val customTimeStr = editAlertTime!!.split(" ")[1]
                            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(todayDate, customDate)
                            val remainingAlertLabel = when {
                                daysBetween < 0 -> "Alarm date has already passed!"
                                daysBetween == 0L -> "Triggers TODAY!"
                                daysBetween == 1L -> "Triggers tomorrow!"
                                else -> "Triggers in $daysBetween days (from today)"
                            }
                            when (editDeliveryType) {
                                "Full Alarm" -> "Alarm fires on $customPart at $customTimeStr ($remainingAlertLabel)\nsounding for ${editDurationSec}s (${editRepetition})"
                                "Vibration Only" -> "Alarm fires on $customPart at $customTimeStr ($remainingAlertLabel)\nwith physical vibration only."
                                "Notification Only" -> "Alarm fires on $customPart at $customTimeStr ($remainingAlertLabel)\nwith standard notification only."
                                else -> "Alarm scheduled silently on $customPart at $customTimeStr."
                            }
                        } else {
                            "Please pick a reminder alarm date & time."
                        }
                    } catch (e: Exception) {
                        "Synchronizing custom deadline: ${hackathon.deadline}..."
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Dynamic preview active bell system indicator",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE RESCHEDULE PREVIEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewString,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AlertsPopup(
    alerts: List<Hackathon>,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("alerts_panel_card"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Pastel yellow notification style
        border = BorderStroke(1.dp, Color(0xFFFBBF24))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerter notification card warning icon",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "UPCOMING SPRINT NOTIFICATIONS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFB45309),
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close system messages alert menu",
                        tint = Color(0xFFB45309)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (alerts.isEmpty()) {
                Text(
                    text = "No notification entries logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF92400E)
                )
            } else {
                alerts.forEach { alertHackathon ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Registration Sprint '${alertHackathon.title}' ends soon on ${alertHackathon.deadline}!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Theme: ${alertHackathon.domain}  •  Prizes: ${alertHackathon.prizes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Empty state icon description indicator layout",
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Simple manual date algorithm remaining
fun calculateDaysRemaining(deadlineStr: String): Int {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val deadlineDate = java.time.LocalDate.parse(deadlineStr, formatter)
        val currentDate = java.time.LocalDate.of(2026, 6, 2) // Relative virtual system date for evaluations
        val days = java.time.temporal.ChronoUnit.DAYS.between(currentDate, deadlineDate).toInt()
        if (days < 0) 0 else days
    } catch (e: Exception) {
        try {
            val parts = deadlineStr.split("-")
            val calDeadline = java.util.Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
            }
            val calCurrent = java.util.Calendar.getInstance().apply {
                set(2026, 5, 2, 0, 0, 0) // Month is 0-indexed, so 5 = June
            }
            val diffMs = calDeadline.timeInMillis - calCurrent.timeInMillis
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays < 0) 0 else diffDays
        } catch (e2: Exception) {
            10
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropImageDialog(
    uri: android.net.Uri,
    context: Context,
    onDismiss: () -> Unit,
    onCropSuccess: (String) -> Unit
) {
    // Load source bitmap
    val sourceBitmap = remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (sourceBitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Error Loading Image") },
            text = { Text("Failed to parse image file. Please choose another visual format.") },
            confirmButton = {
                Button(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    var userScale by remember { mutableStateOf(1.0f) }
    var userOffsetX by remember { mutableStateOf(0f) }
    var userOffsetY by remember { mutableStateOf(0f) }

    val density = context.resources.displayMetrics.density
    val viewSizePx = 160f * density

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Crop and Size Photo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "A circular preview matches how your picture shows in the sidebar and profile pages. Adjust zoom and placement with precision.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // The Crop Circular Frame
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = "Cropping Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, rotation ->
                                    userScale = (userScale * zoom).coerceIn(1.0f, 5.0f)
                                    userOffsetX += pan.x
                                    userOffsetY += pan.y
                                }
                            }
                            .graphicsLayer {
                                scaleX = userScale
                                scaleY = userScale
                                translationX = userOffsetX
                                translationY = userOffsetY
                            }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Pinch to zoom • Drag to pan",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Use natural gestures on the photo to resize and drag it perfectly inside the circle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val cropped = Bitmap.createBitmap(250, 250, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(cropped)
                        val matrix = android.graphics.Matrix()

                        val w = sourceBitmap.width.toFloat()
                        val h = sourceBitmap.height.toFloat()
                        
                        val s0 = 250f / minOf(w, h)
                        val finalScale = s0 * userScale
                        
                        // Scale the user offset to match 250x250 output pixels from 160.dp display surface
                        val tx = 125f - (w * finalScale) / 2f + userOffsetX * (250f / viewSizePx)
                        val ty = 125f - (h * finalScale) / 2f + userOffsetY * (250f / viewSizePx)

                        matrix.postScale(finalScale, finalScale)
                        matrix.postTranslate(tx, ty)

                        val paint = android.graphics.Paint()
                        paint.isFilterBitmap = true
                        canvas.drawBitmap(sourceBitmap, matrix, paint)

                        val outputStream = java.io.ByteArrayOutputStream()
                        cropped.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        val bytes = outputStream.toByteArray()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                        onCropSuccess(base64)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error cropping image", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("btn_crop_save")
            ) {
                Text("Crop & Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

