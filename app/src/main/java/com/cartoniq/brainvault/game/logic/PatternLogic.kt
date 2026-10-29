package com.cartoniq.brainvault.game.logic

import com.cartoniq.brainvault.game.model.GameConfig
import kotlin.random.Random

enum class PatternPhase { READY, PLAYBACK, INPUT, ROUND_SUCCESS, ROUND_FAILURE, PAUSED }
enum class TapOutcome { CORRECT_STEP, ROUND_COMPLETE, WRONG }

/** Pure sequence rules. A seed gives the same full sequence for every player (daily challenge). */
class PatternGame(seed: Long?, val tiles: Int = TILES) {
    companion object { const val TILES = 9; const val MAX_LENGTH = 60 }

    private val full: List<Int>
    var length = GameConfig.PATTERN_START_LENGTH; private set
    var inputIndex = 0; private set
    var lives = GameConfig.PATTERN_LIVES; private set
    var highest = 0; private set
    var score = 0; private set
    val over get() = lives <= 0

    init {
        val rng = if (seed != null) Random(seed) else Random(System.nanoTime())
        full = List(MAX_LENGTH) { rng.nextInt(tiles) }
    }

    fun sequence(): List<Int> = full.take(length)
    fun expectedNext(): Int = full[inputIndex]

    fun tap(tile: Int): TapOutcome {
        if (over) return TapOutcome.WRONG
        if (tile != full[inputIndex]) {
            lives--
            inputIndex = 0
            return TapOutcome.WRONG
        }
        inputIndex++
        if (inputIndex < length) return TapOutcome.CORRECT_STEP
        highest = length
        score += length * 10
        if (length < MAX_LENGTH) length++
        inputIndex = 0
        return TapOutcome.ROUND_COMPLETE
    }
}
