package com.cartoniq.brainvault.game.model

import androidx.annotation.StringRes
import com.cartoniq.brainvault.R
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

object GameType {
    const val MEMORY = "MEMORY"
    const val MATH = "MATH"
    const val PATTERN = "PATTERN"
    val all = listOf(MEMORY, MATH, PATTERN)
}

/** Card and game skins unlockable with in-game coins */
data class SkinDef(
    val id: String,
    val name: String,
    val price: Int,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val description: String,
)

/** Every tunable number lives here. */
object GameConfig {
    const val HINT_COST = 50
    const val AD_REWARD = 50
    const val DAILY_BONUS_COINS = 45
    const val DAILY_BONUS_XP = 35
    const val ACHIEVEMENT_COINS = 30
    const val ACHIEVEMENT_XP = 25

    const val MEMORY_MAX_LEVEL = 7
    const val MEMORY_DAILY_LEVEL = 3
    const val MEMORY_MISMATCH_DELAY_MS = 650L
    const val MEMORY_HINT_MS = 1200L

    const val MATH_TIMED_SECONDS = 60
    const val MATH_QUESTIONS = 10
    const val MATH_FEEDBACK_MS = 500L
    const val MATH_DAILY_MIN_ANSWERED = 5
    const val MATH_DAILY_MIN_ACCURACY = 60

    const val PATTERN_START_LENGTH = 3
    const val PATTERN_LIVES = 2
    const val PATTERN_DAILY_TARGET = 4
    const val PATTERN_ACHIEVEMENT_LENGTH = 6

    const val INTERSTITIAL_EVERY_ROUNDS = 4
    const val INTERSTITIAL_COOLDOWN_MS = 180_000L

    val AVAILABLE_SKINS = listOf(
        SkinDef("NEON_CYAN", "Neon Cyber", 0, 0xFF00E5FF, 0xFF00B0FF, "Default high-tech cybernetic vault glow"),
        SkinDef("PURPLE_HAZE", "Purple Haze", 150, 0xFFB388FF, 0xFF7C4DFF, "Mystic ultraviolet neon energy"),
        SkinDef("EMERALD_MATRIX", "Emerald Matrix", 250, 0xFF00E676, 0xFF00B0FF, "Tactical digital matrix phosphor green"),
        SkinDef("SOLAR_GOLD", "Solar Amber", 400, 0xFFFFD700, 0xFFFF9100, "Prestigious glowing solar vault core"),
    )

    fun getSkin(id: String?): SkinDef =
        AVAILABLE_SKINS.firstOrNull { it.id == id } ?: AVAILABLE_SKINS[0]

    /** XP needed to reach [level]: 25 * (l-1) * l  -> L2=50, L3=150, L4=300 ... */
    fun xpForLevel(level: Int): Int = 25 * (level - 1) * level
    fun levelForXp(xp: Int): Int {
        var l = 1
        while (xpForLevel(l + 1) <= xp) l++
        return l
    }

    data class Reward(val baseCoins: Int, val bonusCoins: Int, val xp: Int)

    fun memoryReward(level: Int, moves: Int, pairs: Int) = Reward(
        baseCoins = 8 + 2 * level,
        bonusCoins = if (moves <= pairs * 2) 8 else 0,
        xp = 12 + 4 * level,
    )

    fun mathReward(correct: Int, total: Int): Reward {
        if (total == 0) return Reward(0, 0, 0)
        val acc = correct * 100 / total
        return Reward(min(30, correct * 2 + 2), if (acc >= 90 && correct >= 5) 8 else 0, 8 + correct * 2)
    }

    fun patternReward(highest: Int): Reward {
        if (highest <= 0) return Reward(0, 0, 2)
        return Reward(min(35, highest * 3), if (highest >= 8) 10 else 0, 6 + highest * 3)
    }

    fun streakBonus(days: Int): Int = min(30, days * 5)
}

data class DailyDef(val key: String, val type: String, val seed: Long, val version: Int)

object Daily {
    const val VERSION = 1

    fun todayKey(cal: Calendar = Calendar.getInstance()): String =
        String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

    fun yesterdayKey(cal: Calendar = Calendar.getInstance()): String {
        val y = cal.clone() as Calendar
        y.add(Calendar.DAY_OF_MONTH, -1)
        return todayKey(y)
    }

    /** Seed = 64-bit FNV-1a of "brainvault:v<VERSION>:<yyyy-MM-dd>". Type = seed mod 3. */
    fun forKey(key: String): DailyDef {
        var h = -3750763034362895579L
        for (c in "brainvault:v$VERSION:$key") {
            h = h xor c.code.toLong()
            h *= 1099511628211L
        }
        val type = GameType.all[((h and Long.MAX_VALUE) % 3).toInt()]
        return DailyDef(key, type, h, VERSION)
    }
}

enum class RewardType { BASE, PERFORMANCE, DAILY, ACHIEVEMENT, STREAK }

data class RewardLine(val type: RewardType, val coins: Int, val xp: Int, val achievementId: String? = null)

data class RewardSummary(
    val lines: List<RewardLine> = emptyList(),
    val newBest: Boolean = false,
    val alreadyRecorded: Boolean = false,
    val oldLevel: Int = 1,
    val newLevel: Int = 1,
    val streak: Int = 0,
) {
    val totalCoins get() = lines.sumOf { it.coins }
    val totalXp get() = lines.sumOf { it.xp }
}

object Achievements {
    data class Def(val id: String, @StringRes val title: Int, @StringRes val desc: Int)

    val all = listOf(
        Def("first_steps", R.string.ach_first_steps, R.string.ach_first_steps_d),
        Def("memory_match", R.string.ach_memory_match, R.string.ach_memory_match_d),
        Def("quick_thinker", R.string.ach_quick_thinker, R.string.ach_quick_thinker_d),
        Def("pattern_learner", R.string.ach_pattern_learner, R.string.ach_pattern_learner_d),
        Def("daily_focus", R.string.ach_daily_focus, R.string.ach_daily_focus_d),
        Def("personal_best", R.string.ach_personal_best, R.string.ach_personal_best_d),
        Def("dedicated_player", R.string.ach_dedicated, R.string.ach_dedicated_d),
        Def("streak_master", R.string.ach_streak_master, R.string.ach_streak_master_d),
        Def("vault_conqueror", R.string.ach_vault_conqueror, R.string.ach_vault_conqueror_d),
    )

    fun find(id: String?) = all.firstOrNull { it.id == id }

    const val DEDICATED_GAMES = 10
    const val QUICK_THINKER_MIN_QUESTIONS = 10
    const val QUICK_THINKER_ACCURACY = 80

    /** Pure: which achievements the just-finished session satisfies. */
    fun satisfied(
        type: String, completed: Boolean, correct: Int, total: Int, metric: Int,
        completedCount: Int, newBest: Boolean, dailyDone: Boolean, currentStreak: Int = 0,
    ): List<String> = buildList {
        if (completedCount >= 1) add("first_steps")
        if (type == GameType.MEMORY && completed) add("memory_match")
        if (type == GameType.MATH && total >= QUICK_THINKER_MIN_QUESTIONS && correct * 100 / total >= QUICK_THINKER_ACCURACY) add("quick_thinker")
        if (type == GameType.PATTERN && metric >= GameConfig.PATTERN_ACHIEVEMENT_LENGTH) add("pattern_learner")
        if (dailyDone) add("daily_focus")
        if (newBest) add("personal_best")
        if (completedCount >= DEDICATED_GAMES) add("dedicated_player")
        if (currentStreak >= 3) add("streak_master")
        if (completedCount >= 25) add("vault_conqueror")
    }
}
