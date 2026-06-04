package com.example.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val profilePhotoBase64: String? = null,
    val defaultRingtone: String = "Default",
    val defaultDurationSec: Int = 30,
    val defaultRepetition: String = "Once",
    val defaultDeliveryType: String = "Full Alarm",
    val defaultSnoozeMin: Int = 5
)

@Entity(tableName = "hackathons")
data class Hackathon(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val prizes: String,
    val teamSize: String,
    val domain: String,
    val deadline: String, // format "YYYY-MM-DD" e.g. "2026-06-20"
    val timeline: String, // human-readable e.g. "June 15 - June 18"
    val isRegistered: Boolean = false,
    val registeredByUserId: Int? = null,
    val reminderDaysBefore: Int = 1, // days before alert
    val isIncomingAlert: Boolean = false, // standard incoming alert
    val isUserCreated: Boolean = false,
    val externalLink: String = "https://devpost.com/hackathons",
    val customAlertDateTime: String? = null, // custom alarm date and time format "YYYY-MM-DD HH:mm"
    val alarmRingtone: String = "Default",
    val alarmDurationSec: Int = 30,
    val alarmRepetition: String = "Once",
    val alertDeliveryType: String = "Full Alarm"
)

@Dao
interface HackathonTrackerDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET passwordHash = :newPassword WHERE username = :username AND email = :email")
    suspend fun resetUserPassword(username: String, email: String, newPassword: String): Int

    @Query("SELECT * FROM hackathons")
    fun getAllHackathons(): Flow<List<Hackathon>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHackathon(hackathon: Hackathon): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hackathons: List<Hackathon>)

    @Update
    suspend fun updateHackathon(hackathon: Hackathon)

    @Delete
    suspend fun deleteHackathon(hackathon: Hackathon)

    @Query("UPDATE hackathons SET isRegistered = :isReg, registeredByUserId = :userId, reminderDaysBefore = :reminderDays WHERE id = :hackathonId")
    suspend fun updateRegistrationStatus(hackathonId: Int, isReg: Boolean, userId: Int?, reminderDays: Int)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Handle migration adding externalLink and customAlertDateTime
        db.execSQL("ALTER TABLE hackathons ADD COLUMN externalLink TEXT NOT NULL DEFAULT 'https://devpost.com/hackathons'")
        db.execSQL("ALTER TABLE hackathons ADD COLUMN customAlertDateTime TEXT DEFAULT NULL")
    }
}

@Database(entities = [User::class, Hackathon::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackerDao(): HackathonTrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hackathon_tracker_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
