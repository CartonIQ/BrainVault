package com.cartoniq.brainvault.game.logic

import kotlin.math.max
import kotlin.random.Random

enum class CardState { HIDDEN, REVEALED, MATCHED }

data class MemoryCard(val symbol: Int, val state: CardState)

data class MemorySpec(val level: Int, val rows: Int, val cols: Int, val previewMs: Long) {
    val pairs get() = rows * cols / 2
}

data class MemoryState(
    val spec: MemorySpec,
    val cards: List<MemoryCard>,
    val moves: Int = 0,
    val matchedPairs: Int = 0,
    val first: Int? = null,
    val pending: Pair<Int, Int>? = null,
) {
    val finished get() = matchedPairs == spec.pairs
    val locked get() = pending != null
}

object MemoryLogic {
    const val SYMBOL_COUNT = 12

    /** Central difficulty table: level, rows, cols, preview ms. Move target = 2 x pairs. */
    val specs = listOf(
        MemorySpec(1, 2, 2, 1500),
        MemorySpec(2, 2, 3, 2200),
        MemorySpec(3, 2, 4, 2800),
        MemorySpec(4, 3, 4, 3500),
        MemorySpec(5, 4, 4, 4200),
        MemorySpec(6, 5, 4, 5000),
        MemorySpec(7, 6, 4, 6000),
    )

    fun spec(level: Int) = specs[(level - 1).coerceIn(0, specs.lastIndex)]

    fun create(level: Int, seed: Long?, revealed: Boolean): MemoryState {
        val spec = spec(level)
        val rng = if (seed != null) Random(seed) else Random(System.nanoTime())
        val symbols = (0 until spec.pairs).flatMap { listOf(it, it) }.shuffled(rng)
        val st = if (revealed) CardState.REVEALED else CardState.HIDDEN
        return MemoryState(spec, symbols.map { MemoryCard(it, st) })
    }

    fun hideAll(s: MemoryState) = s.copy(cards = s.cards.map { if (it.state == CardState.MATCHED) it else it.copy(state = CardState.HIDDEN) })

    /** Pure move. Returns the same instance when the tap is ignored. */
    fun tap(s: MemoryState, i: Int): MemoryState {
        if (i !in s.cards.indices || s.locked || s.finished) return s
        val c = s.cards[i]
        if (c.state != CardState.HIDDEN) return s
        val cards = s.cards.toMutableList()
        cards[i] = c.copy(state = CardState.REVEALED)
        val f = s.first ?: return s.copy(cards = cards, first = i)
        val moves = s.moves + 1
        return if (cards[f].symbol == c.symbol) {
            cards[f] = cards[f].copy(state = CardState.MATCHED)
            cards[i] = cards[i].copy(state = CardState.MATCHED)
            s.copy(cards = cards, moves = moves, matchedPairs = s.matchedPairs + 1, first = null)
        } else {
            s.copy(cards = cards, moves = moves, first = null, pending = f to i)
        }
    }

    fun resolveMismatch(s: MemoryState): MemoryState {
        val p = s.pending ?: return s
        val cards = s.cards.toMutableList()
        cards[p.first] = cards[p.first].copy(state = CardState.HIDDEN)
        cards[p.second] = cards[p.second].copy(state = CardState.HIDDEN)
        return s.copy(cards = cards, pending = null)
    }

    /** One face-down card to flash: the partner of the selected card if there is one, else a random hidden card. */
    fun hintIndex(s: MemoryState, rng: Random): Int? {
        if (s.locked || s.finished) return null
        val hidden = s.cards.indices.filter { s.cards[it].state == CardState.HIDDEN }
        if (hidden.isEmpty()) return null
        val f = s.first
        if (f != null) hidden.firstOrNull { s.cards[it].symbol == s.cards[f].symbol }?.let { return it }
        return hidden[rng.nextInt(hidden.size)]
    }

    /** score = 100*pairs + 20*max(0, 3*pairs - moves) + max(0, 90 - seconds) */
    fun score(s: MemoryState, seconds: Int): Int {
        val p = s.spec.pairs
        return 100 * p + 20 * max(0, 3 * p - s.moves) + max(0, 90 - seconds)
    }
}
