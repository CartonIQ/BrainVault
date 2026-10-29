package com.cartoniq.brainvault.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cartoniq.brainvault.BrainVaultApp
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.data.entity.GameSession
import com.cartoniq.brainvault.game.logic.*
import com.cartoniq.brainvault.game.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MemoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BrainVaultApp).repository

    enum class Phase { LOADING, PREVIEW, PLAYING, DONE }

    data class Ui(
        val phase: Phase = Phase.LOADING,
        val state: MemoryState? = null,
        val elapsedMs: Long = 0,
        val paused: Boolean = false,
        val hintIndex: Int? = null,
        val messageRes: Int? = null,
        val score: Int = 0,
        val result: RewardSummary? = null,
        val bestScore: Int = 0,
        val level: Int = 1,
        val daily: Boolean = false,
        val sfx: Int = 0,
        val sfxN: Int = 0,
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    private var started = false
    private var dailyDef: DailyDef? = null
    private var sessionId = ""
    private var recorded = false
    private var hintBusy = false
    private var level = 1
    private var runJob: Job? = null
    private var hideJob: Job? = null

    fun start(daily: DailyDef?) {
        if (started) return
        started = true
        dailyDef = daily
        viewModelScope.launch {
            if (daily != null) repo.ensureDaily(daily)
            val lvl = if (daily != null) GameConfig.MEMORY_DAILY_LEVEL
            else min(GameConfig.MEMORY_MAX_LEVEL, (repo.best(GameType.MEMORY)?.bestMetric ?: 0) + 1)
            begin(lvl)
        }
    }

    fun replay() = begin(level)
    fun nextLevel() = begin(min(GameConfig.MEMORY_MAX_LEVEL, level + 1))

    private fun begin(lvl: Int) {
        runJob?.cancel(); hideJob?.cancel()
        level = lvl
        sessionId = UUID.randomUUID().toString()
        recorded = false
        hintBusy = false
        val s = MemoryLogic.create(lvl, dailyDef?.seed, revealed = true)
        _ui.value = Ui(phase = Phase.PREVIEW, state = s, level = lvl, daily = dailyDef != null)
        runJob = viewModelScope.launch {
            delay(s.spec.previewMs)
            _ui.update { it.copy(phase = Phase.PLAYING, state = it.state?.let(MemoryLogic::hideAll)) }
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(200)
                val now = SystemClock.elapsedRealtime()
                val d = now - last
                last = now
                _ui.update { if (it.phase == Phase.PLAYING && !it.paused) it.copy(elapsedMs = it.elapsedMs + d) else it }
            }
        }
    }

    fun tap(index: Int) {
        val u = _ui.value
        val s = u.state ?: return
        if (u.phase != Phase.PLAYING || u.paused || recorded || s.locked) return
        val n = MemoryLogic.tap(s, index)
        if (n === s) return
        if (n.pending != null) {
            _ui.update { it.copy(state = n, sfx = 2, sfxN = it.sfxN + 1) }
            hideJob?.cancel()
            hideJob = viewModelScope.launch {
                delay(GameConfig.MEMORY_MISMATCH_DELAY_MS)
                _ui.update { it.copy(state = it.state?.let(MemoryLogic::resolveMismatch)) }
            }
        } else if (n.finished) {
            finish(n)
        } else {
            val matched = n.matchedPairs > s.matchedPairs
            _ui.update { it.copy(state = n, sfx = if (matched) 1 else 0, sfxN = it.sfxN + if (matched) 1 else 0) }
        }
    }

    fun togglePause() { _ui.update { if (it.phase == Phase.PLAYING) it.copy(paused = !it.paused) else it } }
    fun onBackground() { _ui.update { if (it.phase == Phase.PLAYING) it.copy(paused = true) else it } }
    fun clearMessage() { _ui.update { it.copy(messageRes = null) } }

    fun useHint() {
        val u = _ui.value
        val s = u.state ?: return
        if (hintBusy || recorded || u.phase != Phase.PLAYING || u.paused || u.hintIndex != null) return
        if (MemoryLogic.hintIndex(s, Random) == null) {
            _ui.update { it.copy(messageRes = R.string.hint_unavailable) }
            return
        }
        hintBusy = true
        viewModelScope.launch {
            val key = "hint:${UUID.randomUUID()}"
            if (!repo.spendCoins(GameConfig.HINT_COST, key, "HINT")) {
                _ui.update { it.copy(messageRes = R.string.hint_not_enough) }
                hintBusy = false
                return@launch
            }
            val cur = _ui.value.state
            val idx = cur?.let { MemoryLogic.hintIndex(it, Random) }
            if (idx == null) {
                repo.grantCoins(GameConfig.HINT_COST, "refund:$key", "HINT_REFUND")
                _ui.update { it.copy(messageRes = R.string.hint_unavailable) }
                hintBusy = false
                return@launch
            }
            _ui.update { it.copy(hintIndex = idx) }
            delay(GameConfig.MEMORY_HINT_MS)
            _ui.update { it.copy(hintIndex = null) }
            hintBusy = false
        }
    }

    private fun finish(state: MemoryState) {
        if (recorded) return
        recorded = true
        runJob?.cancel(); hideJob?.cancel()
        val sec = (_ui.value.elapsedMs / 1000).toInt()
        val score = MemoryLogic.score(state, sec)
        _ui.update { it.copy(phase = Phase.DONE, state = state, score = score, hintIndex = null, sfx = 1, sfxN = it.sfxN + 1) }
        val pairs = state.spec.pairs
        val session = GameSession(
            sessionId, GameType.MEMORY, score, true, System.currentTimeMillis(), level,
            state.moves, pairs, state.moves, level, 0,
        )
        val reward = GameConfig.memoryReward(level, state.moves, pairs)
        val d = dailyDef
        viewModelScope.launch {
            val before = repo.best(GameType.MEMORY)?.bestScore ?: 0
            val summary = repo.recordSession(session, reward, d, true)
            _ui.update { it.copy(result = summary, bestScore = max(before, score)) }
        }
    }
}
