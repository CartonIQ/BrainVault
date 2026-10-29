package com.cartoniq.brainvault.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cartoniq.brainvault.BrainVaultApp
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.data.entity.GameSession
import com.cartoniq.brainvault.game.logic.MathGenerator
import com.cartoniq.brainvault.game.logic.MathQuestion
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

class MathViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BrainVaultApp).repository

    enum class Phase { CHOOSE, PLAYING, DONE }

    data class Ui(
        val phase: Phase = Phase.CHOOSE,
        val timed: Boolean = true,
        val question: MathQuestion? = null,
        val index: Int = 0,
        val score: Int = 0,
        val correct: Int = 0,
        val answered: Int = 0,
        val streak: Int = 0,
        val bestStreak: Int = 0,
        val remainingMs: Long = GameConfig.MATH_TIMED_SECONDS * 1000L,
        val paused: Boolean = false,
        val selected: Int? = null,
        val hintShown: Boolean = false,
        val avgResponseMs: Long = 0,
        val result: RewardSummary? = null,
        val bestScore: Int = 0,
        val daily: Boolean = false,
        val messageRes: Int? = null,
        val sfx: Int = 0,
        val sfxN: Int = 0,
        val bestOnRecord: Int = 0,
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    private var started = false
    private var dailyDef: DailyDef? = null
    private var gen = MathGenerator(null)
    private var sessionId = ""
    private var recorded = false
    private var hintBusy = false
    private var qMs = 0L
    private var respSum = 0L
    private var tickJob: Job? = null
    private var nextJob: Job? = null

    fun start(daily: DailyDef?) {
        if (started) return
        started = true
        dailyDef = daily
        viewModelScope.launch {
            val best = repo.best(GameType.MATH)?.bestScore ?: 0
            if (daily != null) { repo.ensureDaily(daily); begin(false) }
            else _ui.update { it.copy(bestOnRecord = best) }
        }
    }

    fun choose(timed: Boolean) = begin(timed)
    fun backToChoose() {
        tickJob?.cancel(); nextJob?.cancel()
        viewModelScope.launch {
            val best = repo.best(GameType.MATH)?.bestScore ?: 0
            _ui.value = Ui(bestOnRecord = best)
        }
    }
    fun replay() = begin(_ui.value.timed)

    private fun begin(timed: Boolean) {
        tickJob?.cancel(); nextJob?.cancel()
        gen = MathGenerator(dailyDef?.seed)
        sessionId = UUID.randomUUID().toString()
        recorded = false; hintBusy = false; qMs = 0; respSum = 0
        _ui.value = Ui(phase = Phase.PLAYING, timed = timed, question = gen.next(0), daily = dailyDef != null,
            bestOnRecord = _ui.value.bestOnRecord)
        tickJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(100)
                val now = SystemClock.elapsedRealtime()
                val d = now - last
                last = now
                val u = _ui.value
                if (u.phase != Phase.PLAYING || u.paused || recorded) continue
                if (u.selected == null) qMs += d
                if (u.timed) {
                    val r = max(0L, u.remainingMs - d)
                    _ui.update { it.copy(remainingMs = r) }
                    if (r == 0L) finish()
                }
            }
        }
    }

    fun answer(option: Int) {
        val u = _ui.value
        val q = u.question ?: return
        if (u.phase != Phase.PLAYING || u.paused || u.selected != null || recorded) return
        val ok = option == q.answer
        respSum += qMs
        val answered = u.answered + 1
        val streak = if (ok) u.streak + 1 else 0
        _ui.update {
            it.copy(
                selected = option, answered = answered,
                correct = it.correct + if (ok) 1 else 0, streak = streak, bestStreak = max(it.bestStreak, streak),
                score = it.score + if (ok) 10 + min(u.streak, 5) * 2 else 0,
                avgResponseMs = respSum / answered, sfx = if (ok) 1 else 2, sfxN = it.sfxN + 1,
            )
        }
        nextJob?.cancel()
        nextJob = viewModelScope.launch {
            delay(GameConfig.MATH_FEEDBACK_MS)
            if (recorded) return@launch
            if (!u.timed && answered >= GameConfig.MATH_QUESTIONS) { finish(); return@launch }
            qMs = 0
            _ui.update { it.copy(index = it.index + 1, question = gen.next(it.index + 1), selected = null, hintShown = false) }
        }
    }

    fun togglePause() { _ui.update { if (it.phase == Phase.PLAYING) it.copy(paused = !it.paused) else it } }
    fun onBackground() { _ui.update { if (it.phase == Phase.PLAYING) it.copy(paused = true) else it } }
    fun clearMessage() { _ui.update { it.copy(messageRes = null) } }

    fun useHint() {
        val u = _ui.value
        if (hintBusy || recorded || u.phase != Phase.PLAYING || u.paused || u.selected != null || u.hintShown) {
            if (u.hintShown) _ui.update { it.copy(messageRes = R.string.hint_unavailable) }
            return
        }
        hintBusy = true
        val idx = u.index
        viewModelScope.launch {
            val ok = repo.spendCoins(GameConfig.HINT_COST, "hint:${UUID.randomUUID()}", "HINT")
            if (!ok) _ui.update { it.copy(messageRes = R.string.hint_not_enough) }
            else _ui.update { if (it.index == idx) it.copy(hintShown = true) else it }
            hintBusy = false
        }
    }

    private fun finish() {
        if (recorded) return
        recorded = true
        tickJob?.cancel(); nextJob?.cancel()
        val u = _ui.value
        _ui.update { it.copy(phase = Phase.DONE, selected = null) }
        val completed = u.answered >= 3
        val accOk = u.answered >= GameConfig.MATH_DAILY_MIN_ANSWERED &&
            u.correct * 100 / u.answered >= GameConfig.MATH_DAILY_MIN_ACCURACY
        val session = GameSession(
            sessionId, GameType.MATH, u.score, completed, System.currentTimeMillis(),
            if (u.timed) 1 else 0, 0, u.correct, u.answered, u.bestStreak, u.avgResponseMs,
        )
        val reward = GameConfig.mathReward(u.correct, u.answered)
        val d = dailyDef
        viewModelScope.launch {
            val before = repo.best(GameType.MATH)?.bestScore ?: 0
            val summary = repo.recordSession(session, reward, d, accOk)
            _ui.update { it.copy(result = summary, bestScore = max(before, u.score)) }
        }
    }
}
