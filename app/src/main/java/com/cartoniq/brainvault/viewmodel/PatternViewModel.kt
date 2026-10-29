package com.cartoniq.brainvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cartoniq.brainvault.BrainVaultApp
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.data.entity.GameSession
import com.cartoniq.brainvault.game.logic.PatternGame
import com.cartoniq.brainvault.game.logic.PatternPhase
import com.cartoniq.brainvault.game.logic.TapOutcome
import com.cartoniq.brainvault.game.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

class PatternViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BrainVaultApp).repository

    data class Ui(
        val phase: PatternPhase = PatternPhase.READY,
        val length: Int = GameConfig.PATTERN_START_LENGTH,
        val step: Int = 0,
        val lit: Int? = null,
        val hintTile: Int? = null,
        val pressed: Int? = null,
        val lives: Int = GameConfig.PATTERN_LIVES,
        val score: Int = 0,
        val highest: Int = 0,
        val done: Boolean = false,
        val result: RewardSummary? = null,
        val bestScore: Int = 0,
        val daily: Boolean = false,
        val messageRes: Int? = null,
        val sfx: Int = 0,
        val sfxN: Int = 0,
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    private var started = false
    private var dailyDef: DailyDef? = null
    private var game = PatternGame(null)
    private var sessionId = ""
    private var recorded = false
    private var hintBusy = false
    private var job: Job? = null
    private var reduced = false

    fun setReducedMotion(v: Boolean) { reduced = v }

    fun start(daily: DailyDef?) {
        if (started) return
        started = true
        dailyDef = daily
        viewModelScope.launch { if (daily != null) repo.ensureDaily(daily); begin() }
    }

    fun replay() = begin()

    private fun begin() {
        job?.cancel()
        game = PatternGame(dailyDef?.seed)
        sessionId = UUID.randomUUID().toString()
        recorded = false; hintBusy = false
        _ui.value = Ui(phase = PatternPhase.READY, daily = dailyDef != null)
        playback(initialDelay = 900)
    }

    private fun playback(initialDelay: Long = 700) {
        job?.cancel()
        val seq = game.sequence()
        _ui.update { it.copy(phase = PatternPhase.PLAYBACK, length = game.length, lives = game.lives, score = game.score, step = 0, lit = null, pressed = null, hintTile = null) }
        job = viewModelScope.launch {
            delay(initialDelay)
            val on = if (reduced) 800L else 550L
            seq.forEachIndexed { i, t ->
                _ui.update { it.copy(lit = t, step = i + 1) }
                delay(on)
                _ui.update { it.copy(lit = null) }
                delay(250)
            }
            _ui.update { it.copy(phase = PatternPhase.INPUT, step = 0) }
        }
    }

    fun tap(tile: Int) {
        val u = _ui.value
        if (u.phase != PatternPhase.INPUT || recorded) return
        when (game.tap(tile)) {
            TapOutcome.CORRECT_STEP -> _ui.update { it.copy(step = game.inputIndex, pressed = tile, sfx = 1, sfxN = it.sfxN + 1) }
            TapOutcome.ROUND_COMPLETE -> {
                _ui.update { it.copy(phase = PatternPhase.ROUND_SUCCESS, pressed = tile, score = game.score, highest = game.highest, step = 0, sfx = 1, sfxN = it.sfxN + 1) }
                job?.cancel()
                job = viewModelScope.launch { delay(900); playback() }
            }
            TapOutcome.WRONG -> {
                _ui.update { it.copy(phase = PatternPhase.ROUND_FAILURE, pressed = tile, lives = game.lives, step = 0, sfx = 2, sfxN = it.sfxN + 1) }
                job?.cancel()
                if (game.over) finish()
                else job = viewModelScope.launch { delay(1100); playback() }
            }
        }
    }

    fun togglePause() {
        val u = _ui.value
        if (u.phase == PatternPhase.PAUSED) playback()
        else if (u.phase == PatternPhase.PLAYBACK || u.phase == PatternPhase.INPUT) pauseNow()
    }

    fun onBackground() {
        val p = _ui.value.phase
        if (p == PatternPhase.PLAYBACK || p == PatternPhase.INPUT) pauseNow()
    }

    private fun pauseNow() {
        job?.cancel()
        _ui.update { it.copy(phase = PatternPhase.PAUSED, lit = null, step = 0) }
    }

    fun clearMessage() { _ui.update { it.copy(messageRes = null) } }

    fun useHint() {
        val u = _ui.value
        if (hintBusy || recorded || u.phase != PatternPhase.INPUT || u.hintTile != null) {
            if (u.phase != PatternPhase.INPUT) _ui.update { it.copy(messageRes = R.string.hint_unavailable) }
            return
        }
        hintBusy = true
        viewModelScope.launch {
            if (!repo.spendCoins(GameConfig.HINT_COST, "hint:${UUID.randomUUID()}", "HINT")) {
                _ui.update { it.copy(messageRes = R.string.hint_not_enough) }
                hintBusy = false
                return@launch
            }
            if (_ui.value.phase == PatternPhase.INPUT && !recorded) {
                _ui.update { it.copy(hintTile = game.expectedNext()) }
                delay(1200)
                _ui.update { it.copy(hintTile = null) }
            } else {
                repo.grantCoins(GameConfig.HINT_COST, "refund:${UUID.randomUUID()}", "HINT_REFUND")
            }
            hintBusy = false
        }
    }

    private fun finish() {
        if (recorded) return
        recorded = true
        val highest = game.highest
        val score = game.score
        _ui.update { it.copy(done = true, highest = highest, score = score) }
        val session = GameSession(
            sessionId, GameType.PATTERN, score, highest >= 1, System.currentTimeMillis(),
            GameConfig.PATTERN_START_LENGTH, 0, highest, highest, highest, 0,
        )
        val reward = GameConfig.patternReward(highest)
        val d = dailyDef
        val success = highest >= GameConfig.PATTERN_DAILY_TARGET
        viewModelScope.launch {
            val before = repo.best(GameType.PATTERN)?.bestScore ?: 0
            val summary = repo.recordSession(session, reward, d, success)
            _ui.update { it.copy(result = summary, bestScore = max(before, score)) }
        }
    }
}
