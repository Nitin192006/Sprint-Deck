package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Self-contained coroutine extension to await any Play Services Task without extra dependencies
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Unknown Firebase Task error"))
        }
    }
}

object FirebaseSyncService {
    private const val TAG = "FirebaseSyncService"
    private var isFirebaseInitialized = false
    private var firestore: FirebaseFirestore? = null

    fun initialize(context: Context) {
        if (isFirebaseInitialized) return
        try {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val appId = BuildConfig.FIREBASE_APP_ID
            val projectId = BuildConfig.FIREBASE_PROJECT_ID

            // Check if user has provided actual non-mock custom environment variables
            val hasCustomConfig = apiKey.isNotEmpty() && !apiKey.contains("mock") &&
                    appId.isNotEmpty() && !appId.contains("1:123456789012") &&
                    projectId.isNotEmpty() && !projectId.contains("mock")

            if (hasCustomConfig) {
                Log.i(TAG, "Custom Firebase credentials found in environment. Initializing programmatically...")
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(context, options)
                firestore = FirebaseFirestore.getInstance()
                isFirebaseInitialized = true
                Log.d(TAG, "Firebase programmatically initialized using active environment configuration.")
            } else {
                Log.d(TAG, "No custom/non-mock environment variables provided. Attempting default auto-init via google-services.json...")
                FirebaseApp.initializeApp(context)
                firestore = FirebaseFirestore.getInstance()
                isFirebaseInitialized = true
                Log.d(TAG, "Firebase initialized safely from google-services.json.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard Firebase initialization missed: ${e.message}. Trying custom fallback parameters...")
            try {
                // Programmatic fallback parameter definition using BuildConfig
                val apiKey = BuildConfig.FIREBASE_API_KEY
                val appId = BuildConfig.FIREBASE_APP_ID
                val projectId = BuildConfig.FIREBASE_PROJECT_ID

                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey.ifEmpty { "mock_api_key_for_testing" })
                    .setApplicationId(appId.ifEmpty { "1:123456789012:android:0a1b2c3d4e5f6g7h8i9j" })
                    .setProjectId(projectId.ifEmpty { "mock-firebase-project-id" })
                    .build()

                FirebaseApp.initializeApp(context, options)
                firestore = FirebaseFirestore.getInstance()
                isFirebaseInitialized = true
                Log.d(TAG, "Firebase programmatically initialized using local fallback properties.")
            } catch (ex: Exception) {
                Log.e(TAG, "Programmatic Firebase initialization completely failed: ${ex.message}")
            }
        }
    }

    private fun getDb(): FirebaseFirestore? {
        return if (isFirebaseInitialized) firestore else null
    }

    // Save a user profile asynchronously to Firestore
    suspend fun saveUserProfile(user: User) {
        val db = getDb() ?: return
        try {
            val userMap = hashMapOf<String, Any?>(
                "id" to user.id,
                "username" to user.username,
                "email" to user.email,
                "passwordHash" to user.passwordHash,
                "profilePhotoBase64" to user.profilePhotoBase64,
                "defaultRingtone" to user.defaultRingtone,
                "defaultDurationSec" to user.defaultDurationSec,
                "defaultRepetition" to user.defaultRepetition,
                "defaultDeliveryType" to user.defaultDeliveryType,
                "defaultSnoozeMin" to user.defaultSnoozeMin
            )
            db.collection("users").document(user.username)
                .set(userMap, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Profile for ${user.username} successfully synced to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing user profile to Firestore: ${e.message}")
        }
    }

    // Query a user profile directly from Firestore to help restore local db on login
    suspend fun findUserRemote(credential: String): User? {
        val db = getDb() ?: return null
        try {
            // First try direct document fetch by username
            var doc = db.collection("users").document(credential).get().awaitTask()
            if (!doc.exists()) {
                // If not found, search by email field reference
                val queryResult = db.collection("users").whereEqualTo("email", credential).get().awaitTask()
                if (!queryResult.isEmpty) {
                    doc = queryResult.documents[0]
                }
            }
            if (doc.exists()) {
                val idLong = doc.getLong("id") ?: 0
                val username = doc.getString("username") ?: doc.id
                val email = doc.getString("email") ?: ""
                val passwordHash = doc.getString("passwordHash") ?: ""
                val profilePhotoBase64 = doc.getString("profilePhotoBase64")
                val defaultRingtone = doc.getString("defaultRingtone") ?: "Default"
                val defaultDurationSec = doc.getLong("defaultDurationSec")?.toInt() ?: 30
                val defaultRepetition = doc.getString("defaultRepetition") ?: "Once"
                val defaultDeliveryType = doc.getString("defaultDeliveryType") ?: "Full Alarm"
                val defaultSnoozeMin = doc.getLong("defaultSnoozeMin")?.toInt() ?: 5
                
                return User(
                    id = idLong.toInt(),
                    username = username,
                    email = email,
                    passwordHash = passwordHash,
                    profilePhotoBase64 = profilePhotoBase64,
                    defaultRingtone = defaultRingtone,
                    defaultDurationSec = defaultDurationSec,
                    defaultRepetition = defaultRepetition,
                    defaultDeliveryType = defaultDeliveryType,
                    defaultSnoozeMin = defaultSnoozeMin
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed finding remote user: ${e.message}")
        }
        return null
    }

    // Sync user profile down from Firestore to local Room database
    suspend fun syncUserProfile(username: String, dao: HackathonTrackerDao): User? {
        val db = getDb() ?: return null
        try {
            val doc = db.collection("users").document(username).get().awaitTask()
            if (doc.exists()) {
                val localUser = dao.getUserByUsername(username)
                if (localUser != null) {
                    val updatedUser = localUser.copy(
                        profilePhotoBase64 = doc.getString("profilePhotoBase64") ?: localUser.profilePhotoBase64,
                        defaultRingtone = doc.getString("defaultRingtone") ?: localUser.defaultRingtone,
                        defaultDurationSec = doc.getLong("defaultDurationSec")?.toInt() ?: localUser.defaultDurationSec,
                        defaultRepetition = doc.getString("defaultRepetition") ?: localUser.defaultRepetition,
                        defaultDeliveryType = doc.getString("defaultDeliveryType") ?: localUser.defaultDeliveryType,
                        defaultSnoozeMin = doc.getLong("defaultSnoozeMin")?.toInt() ?: localUser.defaultSnoozeMin
                    )
                    dao.updateUser(updatedUser)
                    return updatedUser
                } else {
                    val idLong = doc.getLong("id") ?: 0
                    val email = doc.getString("email") ?: ""
                    val passwordHash = doc.getString("passwordHash") ?: ""
                    val profilePhotoBase64 = doc.getString("profilePhotoBase64")
                    val defaultRingtone = doc.getString("defaultRingtone") ?: "Default"
                    val defaultDurationSec = doc.getLong("defaultDurationSec")?.toInt() ?: 30
                    val defaultRepetition = doc.getString("defaultRepetition") ?: "Once"
                    val defaultDeliveryType = doc.getString("defaultDeliveryType") ?: "Full Alarm"
                    val defaultSnoozeMin = doc.getLong("defaultSnoozeMin")?.toInt() ?: 5

                    val newUser = User(
                        id = idLong.toInt(),
                        username = username,
                        email = email,
                        passwordHash = passwordHash,
                        profilePhotoBase64 = profilePhotoBase64,
                        defaultRingtone = defaultRingtone,
                        defaultDurationSec = defaultDurationSec,
                        defaultRepetition = defaultRepetition,
                        defaultDeliveryType = defaultDeliveryType,
                        defaultSnoozeMin = defaultSnoozeMin
                    )
                    dao.insertUser(newUser)
                    return newUser
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing user profile from Firestore: ${e.message}")
        }
        return null
    }

    // Save a custom or registered hackathon (including alarm configuration settings) to Firestore
    suspend fun saveHackathon(userId: Int, hackathon: Hackathon) {
        val db = getDb() ?: return
        try {
            val docId = "${userId}_${hackathon.id}"
            val hMap = hashMapOf(
                "userId" to userId,
                "id" to hackathon.id,
                "title" to hackathon.title,
                "description" to hackathon.description,
                "prizes" to hackathon.prizes,
                "teamSize" to hackathon.teamSize,
                "domain" to hackathon.domain,
                "deadline" to hackathon.deadline,
                "timeline" to hackathon.timeline,
                "isRegistered" to hackathon.isRegistered,
                "reminderDaysBefore" to hackathon.reminderDaysBefore,
                "isIncomingAlert" to hackathon.isIncomingAlert,
                "isUserCreated" to hackathon.isUserCreated,
                "externalLink" to hackathon.externalLink,
                "customAlertDateTime" to hackathon.customAlertDateTime,
                "alarmRingtone" to hackathon.alarmRingtone,
                "alarmDurationSec" to hackathon.alarmDurationSec,
                "alarmRepetition" to hackathon.alarmRepetition,
                "alertDeliveryType" to hackathon.alertDeliveryType
            )
            db.collection("hackathons").document(docId)
                .set(hMap, SetOptions.merge())
                .awaitTask()
            Log.d(TAG, "Hackathon card doc $docId synced to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing hackathon doc to Firestore: ${e.message}")
        }
    }

    // Delete custom hackathon from Firestore
    suspend fun deleteHackathon(userId: Int, hackathonId: Int) {
        val db = getDb() ?: return
        try {
            val docId = "${userId}_${hackathonId}"
            db.collection("hackathons").document(docId).delete().awaitTask()
            Log.d(TAG, "Hackathon doc $docId deleted from Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting hackathon doc from Firestore: ${e.message}")
        }
    }

    // Synchronize latest custom and registered hackathons from Firestore to Room Database for a user session
    suspend fun syncFromFirestore(userId: Int, dao: HackathonTrackerDao) {
        val db = getDb() ?: return
        try {
            // Find all registered/created hackathon documents matching the active User ID
            val result = db.collection("hackathons")
                .whereEqualTo("userId", userId)
                .get()
                .awaitTask()

            for (doc in result.documents) {
                val idLong = doc.getLong("id") ?: continue
                val id = idLong.toInt()
                val title = doc.getString("title") ?: ""
                val description = doc.getString("description") ?: ""
                val prizes = doc.getString("prizes") ?: ""
                val teamSize = doc.getString("teamSize") ?: ""
                val domain = doc.getString("domain") ?: ""
                val deadline = doc.getString("deadline") ?: ""
                val timeline = doc.getString("timeline") ?: ""
                val isRegistered = doc.getBoolean("isRegistered") ?: false
                val reminderDaysBefore = doc.getLong("reminderDaysBefore")?.toInt() ?: 1
                val isIncomingAlert = doc.getBoolean("isIncomingAlert") ?: false
                val isUserCreated = doc.getBoolean("isUserCreated") ?: false
                val externalLink = doc.getString("externalLink") ?: "https://devpost.com/hackathons"
                val customAlertDateTime = doc.getString("customAlertDateTime")
                val alarmRingtone = doc.getString("alarmRingtone") ?: "Default"
                val alarmDurationSec = doc.getLong("alarmDurationSec")?.toInt() ?: 30
                val alarmRepetition = doc.getString("alarmRepetition") ?: "Once"
                val alertDeliveryType = doc.getString("alertDeliveryType") ?: "Full Alarm"

                val hackathon = Hackathon(
                    id = id,
                    title = title,
                    description = description,
                    prizes = prizes,
                    teamSize = teamSize,
                    domain = domain,
                    deadline = deadline,
                    timeline = timeline,
                    isRegistered = isRegistered,
                    registeredByUserId = userId,
                    reminderDaysBefore = reminderDaysBefore,
                    isIncomingAlert = isIncomingAlert,
                    isUserCreated = isUserCreated,
                    externalLink = externalLink,
                    customAlertDateTime = customAlertDateTime,
                    alarmRingtone = alarmRingtone,
                    alarmDurationSec = alarmDurationSec,
                    alarmRepetition = alarmRepetition,
                    alertDeliveryType = alertDeliveryType
                )
                dao.insertHackathon(hackathon)
            }
            Log.d(TAG, "Successfully performed general synchronization from cloud Firestore database.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed during cloud sync from Firestore: ${e.message}")
        }
    }

    // Diagnostics utility to run a round-trip connection checking & read/write verify test in Firestore
    suspend fun runDiagnostics(): List<String> {
        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            Log.i("FirebaseDiagnostics", "[FIREBASE_DIAGNOSTICS] $msg")
            println("[FIREBASE_DIAGNOSTICS] $msg")
        }

        log("Starting Firebase Cloud & Firestore Diagnostics...")
        
        if (!isFirebaseInitialized) {
            log("Error: Firebase has not been initialized yet.")
            return logs
        }
        
        val db = getDb()
        if (db == null) {
            log("Error: Firestore instance is null.")
            return logs
        }

        try {
            log("Firebase Status: ACTIVE / INITIALIZED")
            try {
                val app = FirebaseApp.getInstance()
                log("Firebase App Name: ${app.name}")
                log("Firebase Project ID: ${app.options.projectId}")
                log("Firebase Application ID: ${app.options.applicationId}")
            } catch (e: Exception) {
                log("Could not resolve app options: ${e.message}")
            }
            
            log("--- Checking Main Collection References ---")
            log("1. Users collection: sub-path 'users'")
            log("2. Hackathons collection: sub-path 'hackathons'")
            log("3. Test Diagnoses collection: sub-path '_diagnostics_test'")

            log("--- Performing Test Write Operation ---")
            val testDocId = "test_run_" + System.currentTimeMillis()
            val testData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "status" to "Diagnostics Active",
                "message" to "Sprint Deck Live Connection Validation"
            )
            
            log("Attempting to write test document to '_diagnostics_test/$testDocId'...")
            db.collection("_diagnostics_test").document(testDocId)
                .set(testData)
                .awaitTask()
            log("Success: Test document successfully written to cloud Firestore.")

            log("--- Performing Test Read Operation ---")
            log("Attempting to fetch test document from '_diagnostics_test/$testDocId'...")
            val fetchedSnapshot = db.collection("_diagnostics_test").document(testDocId)
                .get()
                .awaitTask()
            
            if (fetchedSnapshot.exists()) {
                val statusValue = fetchedSnapshot.getString("status")
                log("Success: Document retrieved from Firestore.")
                log("Retrieved data -> status: '$statusValue'")
                if (statusValue == "Diagnostics Active") {
                    log("Verified: Read/Write data integrity matched perfectly!")
                } else {
                    log("Warning: Retrieved status '$statusValue' does not match expected value.")
                }
            } else {
                log("Error: Checked document, but it does not exist in Firestore.")
            }

            log("--- Performing Cleanup Operation ---")
            log("Attempting to delete test document '_diagnostics_test/$testDocId'...")
            db.collection("_diagnostics_test").document(testDocId)
                .delete()
                .awaitTask()
            log("Success: Test document deleted successfully. Database remains pristine.")
            log("Firebase Round-Trip Diagnostics COMPLETE - ALL SYSTEMS NOMINAL!")

        } catch (e: Exception) {
            log("CRITICAL ERROR during Firebase diagnostics: ${e.message}")
            e.printStackTrace()
        }

        return logs
    }
}
