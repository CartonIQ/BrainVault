package com.cartoniq.brainvault.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 100,
    val xp: Int = 0,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
    /** 0 = follow system, 1 = dark, 2 = light */
    val theme: Int = 0,
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String = "",
    val cardSkin: String = "NEON_CYAN",
    val unlockedSkins: String = "NEON_CYAN",
)

@Entity(tableName = "game_session", indices = [Index("gameType")])
data class GameSession(
    @PrimaryKey val sessionId: String,
    val gameType: String,
    val score: Int,
    val completed: Boolean,
    val timestamp: Long,
    /** Memory: level; Math: 1 = timed, 0 = relaxed; Pattern: starting length */
    val difficulty: Int,
    val moves: Int,
    val correct: Int,
    val total: Int,
    /** Memory: level completed (0 if not); Math: best streak; Pattern: highest completed length */
    val metric: Int,
    val avgResponseMs: Long,
)

@Entity(tableName = "personal_best")
data class PersonalBest(
    @PrimaryKey val gameType: String,
    val bestScore: Int,
    val bestMetric: Int,
)

@Entity(tableName = "achievement")
data class AchievementState(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)

@Entity(tableName = "daily_challenge")
data class DailyChallenge(
    @PrimaryKey val dateKey: String,
    val type: String,
    val seed: Long,
    val version: Int,
    val completed: Boolean,
    val rewarded: Boolean,
)

@Entity(tableName = "reward_transaction", indices = [Index("timestamp")])
data class RewardTransaction(
    @PrimaryKey val txKey: String,
    val source: String,
    val coinDelta: Int,
    val xpDelta: Int,
    val timestamp: Long,
)
