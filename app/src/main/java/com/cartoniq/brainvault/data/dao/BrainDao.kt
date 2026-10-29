package com.cartoniq.brainvault.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cartoniq.brainvault.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrainDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun profileFlow(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun profile(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfile(p: PlayerProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(p: PlayerProfile)

    @Query("SELECT * FROM game_session ORDER BY timestamp DESC")
    fun sessionsFlow(): Flow<List<GameSession>>

    /** Returns -1 when the session id already exists (idempotent). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(s: GameSession): Long

    @Query("SELECT COUNT(*) FROM game_session WHERE completed = 1")
    suspend fun completedCount(): Int

    @Query("SELECT * FROM personal_best")
    fun bestsFlow(): Flow<List<PersonalBest>>

    @Query("SELECT * FROM personal_best WHERE gameType = :type")
    suspend fun best(type: String): PersonalBest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBest(b: PersonalBest)

    @Query("SELECT * FROM achievement")
    fun achievementsFlow(): Flow<List<AchievementState>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(a: AchievementState): Long

    @Query("SELECT * FROM daily_challenge WHERE dateKey = :key")
    suspend fun daily(key: String): DailyChallenge?

    @Query("SELECT * FROM daily_challenge WHERE dateKey = :key")
    fun dailyFlow(key: String): Flow<DailyChallenge?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDaily(d: DailyChallenge): Long

    @Update
    suspend fun updateDaily(d: DailyChallenge)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTx(t: RewardTransaction): Long
}
