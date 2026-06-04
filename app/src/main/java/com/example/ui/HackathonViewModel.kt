package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HackathonViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HackathonRepository(db.trackerDao())
    private val prefs = application.getSharedPreferences("hackathon_tracker_prefs", android.content.Context.MODE_PRIVATE)

    // App Preferences
    private val _themeIndex = MutableStateFlow(0)
    val themeIndex: StateFlow<Int> = _themeIndex.asStateFlow()

    // Derived theme readability flag (e.g., whether theme is dark obsidian or cyberpunk, etc.)
    val isDarkTheme: StateFlow<Boolean> = _themeIndex.map { it == 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun cycleTheme() {
        val nextValue = (_themeIndex.value + 1) % 4
        setThemeIndex(nextValue)
    }

    fun setThemeIndex(index: Int) {
        _themeIndex.value = index
        prefs.edit().putInt("theme_index", index).apply()
        // Backward compatibility
        prefs.edit().putBoolean("is_dark_theme", index == 1).apply()
    }

    fun toggleTheme() {
        cycleTheme()
    }

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Hackathons List from Database
    val hackathons: StateFlow<List<Hackathon>> = repository.allHackathons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Loading state (e.g. for AI generation)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aiStatus = MutableStateFlow<String?>(null)
    val aiStatus: StateFlow<String?> = _aiStatus.asStateFlow()

    // Filters
    val searchQuery = MutableStateFlow("")
    val selectedDomain = MutableStateFlow("All")
    val selectedTeamSize = MutableStateFlow("All")

    init {
        // Load stored theme index
        _themeIndex.value = prefs.getInt("theme_index", if (prefs.getBoolean("is_dark_theme", false)) 1 else 0)

        // Initialize Firebase Sync dynamically
        FirebaseSyncService.initialize(application)

        viewModelScope.launch {
            // Seed base items if needed
            repository.prepopulateIfEmpty()

            // Auto Sign-In if active session exists
            val savedUserId = prefs.getInt("logged_in_user_id", -1)
            if (savedUserId != -1) {
                var user = repository.findUserById(savedUserId)
                if (user != null) {
                    _currentUser.value = user
                    // Perform background synchronization from Firestore
                    val syncedUser = FirebaseSyncService.syncUserProfile(user.username, db.trackerDao())
                    if (syncedUser != null) {
                        _currentUser.value = syncedUser
                        user = syncedUser
                    }
                    FirebaseSyncService.syncFromFirestore(user.id, db.trackerDao())
                }
            }
        }
    }

    // Perform User Registration / Login
    fun registerNewUser(username: String, email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val trimmedUsername = username.trim()
            val trimmedEmail = email.trim()
            val trimmedPass = pass.trim()
            if (trimmedUsername.isBlank() || trimmedEmail.isBlank() || trimmedPass.isBlank()) {
                _authError.value = "All fields are required."
                return@launch
            }
            val existing = repository.findUserByUsername(trimmedUsername)
            if (existing != null) {
                _authError.value = "Username already exists."
                return@launch
            }
            val existingEmail = repository.findUserByEmail(trimmedEmail)
            if (existingEmail != null) {
                _authError.value = "Email is already registered."
                return@launch
            }
            val newUser = User(username = trimmedUsername, email = trimmedEmail, passwordHash = trimmedPass)
            val id = repository.registerNewUser(newUser)
            val resolvedUser = newUser.copy(id = id.toInt())
            _currentUser.value = resolvedUser
            
            // Persist session
            prefs.edit().putInt("logged_in_user_id", resolvedUser.id).apply()
            
            // Sync user profile to Firestore
            FirebaseSyncService.saveUserProfile(resolvedUser)
            
            onSuccess()
        }
    }

    fun loginUser(credential: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val trimmedCred = credential.trim()
            val trimmedPass = pass.trim()
            if (trimmedCred.isBlank() || trimmedPass.isBlank()) {
                _authError.value = "Please fill in all fields."
                return@launch
            }
            // Check lookup by username first, fallback to email lookup
            var user = repository.findUserByUsername(trimmedCred)
            if (user == null) {
                user = repository.findUserByEmail(trimmedCred)
            }
            
            // Fallback: If user not found locally, check Firestore to restore user account
            if (user == null) {
                try {
                    val remoteUser = FirebaseSyncService.findUserRemote(trimmedCred)
                    if (remoteUser != null) {
                        if (remoteUser.passwordHash == trimmedPass) {
                            // Insert remote user back into Room
                            db.trackerDao().insertUser(remoteUser)
                            user = remoteUser
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HackathonViewModel", "Error recovering user remotely: ${e.message}")
                }
            }
            
            if (user == null || user.passwordHash != trimmedPass) {
                _authError.value = "Invalid username, email, or password."
                return@launch
            }
            var activeUser = user
            val syncedUser = FirebaseSyncService.syncUserProfile(user.username, db.trackerDao())
            if (syncedUser != null) {
                _currentUser.value = syncedUser
                activeUser = syncedUser
            }
            _currentUser.value = activeUser
            
            // Persist session
            prefs.edit().putInt("logged_in_user_id", activeUser.id).apply()
            
            // Sync latest registered hackathons and alarms from cloud
            FirebaseSyncService.syncFromFirestore(activeUser.id, db.trackerDao())
            
            onSuccess()
        }
    }

    fun resetPassword(username: String, email: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val trimmedUsername = username.trim()
            val trimmedEmail = email.trim()
            val trimmedPass = newPass.trim()
            if (trimmedUsername.isBlank() || trimmedEmail.isBlank() || trimmedPass.isBlank()) {
                _authError.value = "All fields are required to reset password."
                return@launch
            }
            val success = repository.resetUserPassword(trimmedUsername, trimmedEmail, trimmedPass)
            if (success) {
                _authError.value = null
                onSuccess()
            } else {
                _authError.value = "Invalid username or email combination."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user_id").apply()
    }

    fun updateUserProfileSettings(
        profilePhotoBase64: String?,
        defaultRingtone: String,
        defaultDurationSec: Int,
        defaultRepetition: String,
        defaultDeliveryType: String,
        defaultSnoozeMin: Int
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                profilePhotoBase64 = profilePhotoBase64,
                defaultRingtone = defaultRingtone,
                defaultDurationSec = defaultDurationSec,
                defaultRepetition = defaultRepetition,
                defaultDeliveryType = defaultDeliveryType,
                defaultSnoozeMin = defaultSnoozeMin
            )
            repository.updateUser(updated)
            _currentUser.value = updated
            FirebaseSyncService.saveUserProfile(updated)
        }
    }

    // Join / Register for a hackathon
    fun registerForHackathon(hackathonId: Int, reminderDays: Int, customAlertTime: String? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val currentList = repository.allHackathons.first()
            val hackathon = currentList.find { it.id == hackathonId }
            if (hackathon != null) {
                val updated = hackathon.copy(
                    isRegistered = true,
                    registeredByUserId = user.id,
                    reminderDaysBefore = reminderDays,
                    customAlertDateTime = customAlertTime
                )
                repository.updateHackathon(updated)
                // Sync registered card to cloud
                FirebaseSyncService.saveHackathon(user.id, updated)
            } else {
                repository.registerForHackathon(hackathonId, user.id, reminderDays)
                // Retrieve updated from database to secure accurate values in Firestore
                val freshList = repository.allHackathons.first()
                val freshH = freshList.find { it.id == hackathonId }
                if (freshH != null) {
                    FirebaseSyncService.saveHackathon(user.id, freshH)
                }
            }
        }
    }

    // Leave / Unregister from a hackathon
    fun unregisterFromHackathon(hackathonId: Int) {
        val user = _currentUser.value
        viewModelScope.launch {
            repository.unregisterFromHackathon(hackathonId)
            if (user != null) {
                val currentList = repository.allHackathons.first()
                val hackathon = currentList.find { it.id == hackathonId }
                if (hackathon != null) {
                    val unregisteredCopy = hackathon.copy(
                        isRegistered = false,
                        registeredByUserId = null
                    )
                    FirebaseSyncService.saveHackathon(user.id, unregisteredCopy)
                }
            }
        }
    }

    // Update reminder days prior
    fun updateReminderDays(hackathon: Hackathon, days: Int) {
        val user = _currentUser.value
        viewModelScope.launch {
            val updated = hackathon.copy(reminderDaysBefore = days)
            repository.updateHackathon(updated)
            if (user != null) {
                FirebaseSyncService.saveHackathon(user.id, updated)
            }
        }
    }

    // Update custom alarm date time
    fun updateCustomAlarm(
        hackathon: Hackathon,
        customAlertTime: String?,
        ringtone: String = hackathon.alarmRingtone,
        durationSec: Int = hackathon.alarmDurationSec,
        repetition: String = hackathon.alarmRepetition,
        alertDeliveryType: String = hackathon.alertDeliveryType
    ) {
        val user = _currentUser.value
        viewModelScope.launch {
            val updated = hackathon.copy(
                customAlertDateTime = customAlertTime,
                alarmRingtone = ringtone,
                alarmDurationSec = durationSec,
                alarmRepetition = repetition,
                alertDeliveryType = alertDeliveryType
            )
            repository.updateHackathon(updated)
            if (user != null) {
                FirebaseSyncService.saveHackathon(user.id, updated)
            }
        }
    }

    // Delete a custom user hackathon
    fun deleteHackathon(hackathon: Hackathon) {
        val user = _currentUser.value
        viewModelScope.launch {
            repository.deleteHackathon(hackathon)
            if (user != null) {
                FirebaseSyncService.deleteHackathon(user.id, hackathon.id)
            }
        }
    }

    // Create a manual custom user hackathon entry
    fun createCustomHackathon(
        title: String,
        description: String,
        prizes: String,
        teamSize: String,
        domain: String,
        deadline: String,
        timeline: String,
        link: String
    ) {
        val user = _currentUser.value
        viewModelScope.launch {
            val newH = Hackathon(
                title = title,
                description = description,
                prizes = prizes,
                teamSize = teamSize,
                domain = domain,
                deadline = deadline,
                timeline = timeline,
                isRegistered = false,
                isUserCreated = true, // Exclusively true for manual entries
                externalLink = if (link.isBlank()) "https://unstop.com" else link
            )
            val generatedId = repository.insertHackathon(newH)
            if (user != null) {
                val copyWithId = newH.copy(id = generatedId.toInt())
                FirebaseSyncService.saveHackathon(user.id, copyWithId)
            }
        }
    }

    // Generate enriched hackathon with Gemini
    suspend fun enrichWithAISuspended(inputIdea: String): Int {
        if (inputIdea.isBlank()) return 0
        _isLoading.value = true
        _aiStatus.value = "Connecting to Gemini 3.5 Flash Model..."
        return try {
            val newHackathons = GeminiService.extractHackathonsWithAI(inputIdea)
            if (!newHackathons.isNullOrEmpty()) {
                var addedCount = 0
                for (h in newHackathons) {
                    repository.insertHackathon(h)
                    addedCount++
                }
                _aiStatus.value = "Success! Generated and added $addedCount sprint cards to your opportunities catalog."
                addedCount
            } else {
                _aiStatus.value = "Failed to retrieve details. Try adding more description/dates."
                0
            }
        } catch (e: Exception) {
            _aiStatus.value = "Error: ${e.localizedMessage ?: "Unknown server response"}"
            0
        } finally {
            _isLoading.value = false
        }
    }

    fun enrichWithAI(inputIdea: String) {
        viewModelScope.launch {
            enrichWithAISuspended(inputIdea)
        }
    }

    fun clearAiStatus() {
        _aiStatus.value = null
    }

    fun forceSyncWithFirebase() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            FirebaseSyncService.syncFromFirestore(user.id, db.trackerDao())
        }
    }
}
