package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitEntry
import com.example.data.model.UserSettings

@Database(
    entities = [Habit::class, HabitEntry::class, UserSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEntryDao(): HabitEntryDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add columns to habits table
                db.execSQL("ALTER TABLE habits ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                // Populate uuid with unique values and updatedAt
                db.execSQL("UPDATE habits SET uuid = hex(randomblob(16)) WHERE uuid = ''")
                db.execSQL("UPDATE habits SET updatedAt = createdAt WHERE updatedAt = 0")

                // Add columns to habit_entries table
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN habitUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habit_entries ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE habit_entries SET uuid = hex(randomblob(16)) WHERE uuid = ''")
                db.execSQL("UPDATE habit_entries SET updatedAt = timestamp WHERE updatedAt = 0")

                // Link habitUuid from habits
                db.execSQL("UPDATE habit_entries SET habitUuid = (SELECT uuid FROM habits WHERE habits.id = habit_entries.habitId)")

                // Add unique index on uuid for habit_entries
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_entries_uuid ON habit_entries(uuid)")

                // Add columns to user_settings table
                db.execSQL("ALTER TABLE user_settings ADD COLUMN autoBackupEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN lastBackupTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "p_habits_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
