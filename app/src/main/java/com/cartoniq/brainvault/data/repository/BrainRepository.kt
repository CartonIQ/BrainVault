package com.cartoniq.brainvault.data.repository

import androidx.room.withTransaction
import com.cartoniq.brainvault.data.database.BrainDatabase
import com.cartoniq.brainvault.data.entity.*
import com.cartoniq.brainvault.game.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.max

class BrainRepository(private val db: BrainDatabase) {
    private val dao = db.dao()

    val profile: Flow<PlayerProfile?> = dao.profileFlow()
    val sessions: Flow<List<GameSession>> = dao.sessionsFlow()
    val bests: Flow<List<PersonalBest>> = dao.bestsFlow()
    val achievements: Flow<List<AchievementState>> = dao.achievementsFlow()
    fun dailyFlow(key: String): Flow<DailyChallenge?> = dao.dailyFlow(key)

    suspend fun ensureProfile() {
        dao.insertProfile(PlayerProfile(coins = 100))
    }

    suspend fun best(type: String): PersonalBest? = dao.best(type)

    suspend fun updateProfile(transform: (PlayerProfile) -> PlayerProfile) = db.withTransaction {
        val p = dao.profile() ?: PlayerProfile()
        dao.upsertProfile(transform(p))
    }

    suspend fun ensureDaily(def: DailyDef) {
        dao.insertDaily(DailyChallenge(def.key, def.type, def.seed, def.version, completed = false, rewarded = false))
    }

    /** Atomic and idempotent: charges only if affordable and the key was never used. */
    suspend fun spendCoins(amount: Int, key: String, source: String): Boolean = db.withTransaction {
        val p = dao.profile() ?: return@withTransaction false
        if (amount <= 0 || p.coins < amount) return@withTransaction false
        if (dao.insertTx(RewardTransaction(key, source, -amount, 0, System.currentTimeMillis())) == -1L) return@withTransaction false
        dao.upsertProfile(p.copy(coins = p.coins - amount))
        true
    }

    suspend fun grantCoins(amount: Int, key: String, source: String): Boolean = db.withTransaction {
        val p = dao.profile() ?: return@withTransaction false
        if (amount <= 0) return@withTransaction false
        if (dao.insertTx(RewardTransaction(key, source, amount, 0, System.currentTimeMillis())) == -1L) return@withTransaction false
        dao.upsertProfile(p.copy(coins = p.coins + amount))
        true
    }

    /** Purchases a cosmetic skin if not already unlocked and user has enough coins */
    suspend fun unlockSkin(skinId: String, cost: Int): Boolean = db.withTransaction {
        val p = dao.profile() ?: return@withTransaction false
        val unlockedList = p.unlockedSkins.split(",").map { it.trim() }
        if (skinId in unlockedList) return@withTransaction true
        if (p.coins < cost) return@withTransaction false
        val txKey = "skin:$skinId:${System.currentTimeMillis()}"
        if (dao.insertTx(RewardTransaction(txKey, "SKIN_PURCHASE", -cost, 0, System.currentTimeMillis())) == -1L) return@withTransaction false
        val newUnlocked = (unlockedList + skinId).joinToString(",")
        dao.upsertProfile(p.copy(coins = p.coins - cost, unlockedSkins = newUnlocked, cardSkin = skinId))
        true
    }

    suspend fun selectSkin(skinId: String) = db.withTransaction {
        val p = dao.profile() ?: return@withTransaction
        val unlockedList = p.unlockedSkins.split(",").map { it.trim() }
        if (skinId in unlockedList) {
            dao.upsertProfile(p.copy(cardSkin = skinId))
        }
    }

    /**
     * Stores a finished session AND its rewards in one transaction. The session id is the idempotency key:
     * a second call with the same id changes nothing and returns alreadyRecorded = true.
     */
    suspend fun recordSession(
        s: GameSession, reward: GameConfig.Reward, daily: DailyDef?, dailySuccess: Boolean,
    ): RewardSummary = db.withTransaction {
        if (dao.insertSession(s) == -1L) return@withTransaction RewardSummary(alreadyRecorded = true)
        val now = System.currentTimeMillis()
        val lines = mutableListOf<RewardLine>()
        if (reward.baseCoins > 0 || reward.xp > 0) lines += RewardLine(RewardType.BASE, reward.baseCoins, reward.xp)
        if (reward.bonusCoins > 0) lines += RewardLine(RewardType.PERFORMANCE, reward.bonusCoins, 0)

        val p = dao.profile() ?: PlayerProfile()
        var dailyDone = false
        var newStreak = p.streakCount
        var newBestStreak = p.bestStreak

        if (daily != null && dailySuccess) {
            var row = dao.daily(daily.key)
            if (row == null) {
                row = DailyChallenge(daily.key, daily.type, daily.seed, daily.version, completed = false, rewarded = false)
                dao.insertDaily(row)
            }
            if (!row.rewarded) {
                dao.updateDaily(row.copy(completed = true, rewarded = true))
                lines += RewardLine(RewardType.DAILY, GameConfig.DAILY_BONUS_COINS, GameConfig.DAILY_BONUS_XP)
                dailyDone = true

                // Update daily streak intelligently
                val today = Daily.todayKey()
                val yesterday = Daily.yesterdayKey()
                newStreak = when (p.lastActiveDate) {
                    today -> p.streakCount // Already played today
                    yesterday -> p.streakCount + 1 // Consecutive day streak!
                    else -> 1 // First day or restart
                }
                newBestStreak = max(newBestStreak, newStreak)
                if (newStreak > 1) {
                    val bonus = GameConfig.streakBonus(newStreak)
                    lines += RewardLine(RewardType.STREAK, bonus, bonus / 2)
                }
            }
        }

        val prev = dao.best(s.gameType)
        val newBest = prev != null && prev.bestScore > 0 && s.score > prev.bestScore
        if (prev == null || s.score > prev.bestScore || s.metric > prev.bestMetric) {
            dao.upsertBest(PersonalBest(s.gameType, max(s.score, prev?.bestScore ?: 0), max(s.metric, prev?.bestMetric ?: 0)))
        }

        val completedCount = dao.completedCount()
        val ids = Achievements.satisfied(s.gameType, s.completed, s.correct, s.total, s.metric, completedCount, newBest, dailyDone, newStreak)
        for (id in ids) {
            if (dao.insertAchievement(AchievementState(id, now)) != -1L) {
                lines += RewardLine(RewardType.ACHIEVEMENT, GameConfig.ACHIEVEMENT_COINS, GameConfig.ACHIEVEMENT_XP, id)
            }
        }

        lines.forEachIndexed { i, l ->
            dao.insertTx(RewardTransaction("${s.sessionId}:$i:${l.type}:${l.achievementId ?: ""}", l.type.name, l.coins, l.xp, now))
        }

        val updated = p.copy(
            coins = p.coins + lines.sumOf { it.coins },
            xp = p.xp + lines.sumOf { it.xp },
            streakCount = newStreak,
            bestStreak = newBestStreak,
            lastActiveDate = if (dailyDone) Daily.todayKey() else p.lastActiveDate,
        )
        dao.upsertProfile(updated)

        RewardSummary(
            lines = lines,
            newBest = newBest,
            alreadyRecorded = false,
            oldLevel = GameConfig.levelForXp(p.xp),
            newLevel = GameConfig.levelForXp(updated.xp),
            streak = newStreak,
        )
    }

    suspend fun resetAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        ensureProfile()
    }
}
