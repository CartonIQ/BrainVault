package com.cartoniq.brainvault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cartoniq.brainvault.data.database.BrainDatabase
import com.cartoniq.brainvault.data.entity.GameSession
import com.cartoniq.brainvault.data.repository.BrainRepository
import com.cartoniq.brainvault.game.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RewardIntegrityTest {
    private lateinit var db: BrainDatabase
    private lateinit var repo: BrainRepository

    @Before fun setUp() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, BrainDatabase::class.java).allowMainThreadQueries().build()
        repo = BrainRepository(db)
        repo.ensureProfile()
    }

    @After fun tearDown() { db.close() }

    private fun session(id: String) = GameSession(id, GameType.MEMORY, 400, true, 1L, 1, 4, 2, 4, 1, 0)
    private suspend fun coins() = repo.profile.first()!!.coins

    @Test fun sameSessionRewardsOnlyOnce() = runBlocking {
        val r = GameConfig.memoryReward(1, 4, 2)
        val first = repo.recordSession(session("s1"), r, null, false)
        val afterFirst = coins()
        val second = repo.recordSession(session("s1"), r, null, false)
        assertFalse(first.alreadyRecorded); assertTrue(second.alreadyRecorded)
        assertEquals(afterFirst, coins())
    }

    @Test fun dailyRewardOnlyOncePerKey() = runBlocking {
        val d = Daily.forKey("2026-10-29")
        val r = GameConfig.memoryReward(3, 12, 6)
        val a = repo.recordSession(session("a"), r, d, true)
        val b = repo.recordSession(session("b"), r, d, true)
        assertTrue(a.lines.any { it.type == RewardType.DAILY })
        assertFalse(b.lines.any { it.type == RewardType.DAILY })
    }

    @Test fun hintNeverGoesNegativeAndNeverDoubleCharges() = runBlocking {
        assertTrue(repo.spendCoins(50, "h1", "HINT"))
        assertFalse(repo.spendCoins(50, "h1", "HINT"))
        assertEquals(50, coins())
        assertTrue(repo.spendCoins(50, "h2", "HINT"))
        assertFalse(repo.spendCoins(50, "h3", "HINT"))
        assertEquals(0, coins())
    }

    @Test fun achievementsPayOnce() = runBlocking {
        val r = GameConfig.memoryReward(1, 4, 2)
        val a = repo.recordSession(session("x1"), r, null, false)
        val b = repo.recordSession(session("x2"), r, null, false)
        assertTrue(a.lines.any { it.achievementId == "first_steps" })
        assertFalse(b.lines.any { it.achievementId == "first_steps" })
    }
}
