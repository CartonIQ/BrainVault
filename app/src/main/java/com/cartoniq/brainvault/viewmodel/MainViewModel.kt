package com.cartoniq.brainvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cartoniq.brainvault.BrainVaultApp
import com.cartoniq.brainvault.data.entity.*
import com.cartoniq.brainvault.game.model.Daily
import com.cartoniq.brainvault.game.model.DailyDef
import com.cartoniq.brainvault.game.model.GameConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class DailyUi(val def: DailyDef, val completed: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    val repo = (app as BrainVaultApp).repository
    val adManager = (app as BrainVaultApp).adManager

    val profile: StateFlow<PlayerProfile?> = repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val sessions: StateFlow<List<GameSession>> = repo.sessions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bests: StateFlow<List<PersonalBest>> = repo.bests.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val unlocked: StateFlow<Set<String>> = repo.achievements.map { l -> l.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val todayKey = MutableStateFlow(Daily.todayKey())
    val daily: StateFlow<DailyUi> = todayKey.flatMapLatest { k ->
        repo.dailyFlow(k).map { DailyUi(Daily.forKey(k), it?.completed == true) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DailyUi(Daily.forKey(todayKey.value), false))

    /** Call on resume so a date or time-zone change is picked up. */
    fun refreshDate() { todayKey.value = Daily.todayKey() }

    fun setSound(v: Boolean) = update { it.copy(sound = v) }
    fun setHaptics(v: Boolean) = update { it.copy(haptics = v) }
    fun setReducedMotion(v: Boolean) = update { it.copy(reducedMotion = v) }
    fun setTheme(v: Int) = update { it.copy(theme = v) }
    private fun update(f: (PlayerProfile) -> PlayerProfile) { viewModelScope.launch { repo.updateProfile(f) } }

    fun unlockSkin(skinId: String, cost: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.unlockSkin(skinId, cost)
            onResult(ok)
        }
    }

    fun selectSkin(skinId: String) {
        viewModelScope.launch { repo.selectSkin(skinId) }
    }

    fun grantAdReward() {
        viewModelScope.launch { repo.grantCoins(GameConfig.AD_REWARD, "ad:${UUID.randomUUID()}", "AD") }
    }

    fun resetAll() { viewModelScope.launch { repo.resetAll() } }
}
