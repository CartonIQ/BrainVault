package com.cartoniq.brainvault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cartoniq.brainvault.data.dao.BrainDao
import com.cartoniq.brainvault.data.entity.*

@Database(
    entities = [
        PlayerProfile::class, GameSession::class, PersonalBest::class,
        AchievementState::class, DailyChallenge::class, RewardTransaction::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BrainDatabase : RoomDatabase() {
    abstract fun dao(): BrainDao

    companion object {
        /** Safe Room migration from v1 to v2 adding streak and skin fields without wiping player data. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_profile ADD COLUMN streakCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE player_profile ADD COLUMN bestStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE player_profile ADD COLUMN lastActiveDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE player_profile ADD COLUMN cardSkin TEXT NOT NULL DEFAULT 'NEON_CYAN'")
                db.execSQL("ALTER TABLE player_profile ADD COLUMN unlockedSkins TEXT NOT NULL DEFAULT 'NEON_CYAN'")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun build(context: Context): BrainDatabase =
            Room.databaseBuilder(context.applicationContext, BrainDatabase::class.java, "brainvault.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
