package com.cartoniq.brainvault

import com.cartoniq.brainvault.game.logic.*
import com.cartoniq.brainvault.game.model.*
import org.junit.Assert.*
import org.junit.Test

class MemoryLogicTest {
    @Test fun everySymbolAppearsExactlyTwice() {
        for (spec in MemoryLogic.specs) {
            val s = MemoryLogic.create(spec.level, 42L, revealed = false)
            assertEquals(spec.rows * spec.cols, s.cards.size)
            s.cards.groupBy { it.symbol }.values.forEach { assertEquals(2, it.size) }
        }
    }

    @Test fun matchingPairStaysAndFinishes() {
        var s = MemoryLogic.create(1, 7L, revealed = false)
        for (sym in 0 until s.spec.pairs) {
            val idx = s.cards.indices.filter { s.cards[it].symbol == sym }
            s = MemoryLogic.tap(s, idx[0]); s = MemoryLogic.tap(s, idx[1])
        }
        assertTrue(s.finished)
        assertEquals(s.spec.pairs, s.moves)
    }

    @Test fun mismatchLocksInputThenHides() {
        var s = MemoryLogic.create(2, 3L, revealed = false)
        val a = 0
        val b = s.cards.indices.first { s.cards[it].symbol != s.cards[a].symbol }
        s = MemoryLogic.tap(s, a); s = MemoryLogic.tap(s, b)
        assertTrue(s.locked)
        val c = s.cards.indices.first { s.cards[it].state == CardState.HIDDEN }
        assertSame(s, MemoryLogic.tap(s, c))
        s = MemoryLogic.resolveMismatch(s)
        assertFalse(s.locked)
        assertTrue(s.cards.all { it.state == CardState.HIDDEN })
    }

    @Test fun sameCardCannotBePairedWithItself() {
        var s = MemoryLogic.create(1, 1L, revealed = false)
        s = MemoryLogic.tap(s, 0)
        assertSame(s, MemoryLogic.tap(s, 0))
        assertEquals(0, s.moves)
    }

    @Test fun seedGivesSameBoard() {
        assertEquals(MemoryLogic.create(3, 99L, false).cards, MemoryLogic.create(3, 99L, false).cards)
    }
}

class MathGeneratorTest {
    @Test fun optionsDistinctNonNegativeAndContainAnswerOnce() {
        val g = MathGenerator(5L)
        repeat(500) { i ->
            val q = g.next(i % 20)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.toSet().size)
            assertEquals(1, q.options.count { it == q.answer })
            assertTrue(q.options.all { it >= 0 })
        }
    }

    @Test fun divisionIsExactAndNeverByZero() {
        val g = MathGenerator(11L)
        repeat(1000) {
            val q = g.next(12)
            if (q.op == '÷') {
                val (a, b) = q.text.split(" ÷ ").map { it.toInt() }
                assertTrue(b > 0); assertEquals(0, a % b); assertEquals(a / b, q.answer)
            }
        }
    }

    @Test fun seededSequenceIsDeterministic() {
        val a = MathGenerator(123L); val b = MathGenerator(123L)
        repeat(30) { assertEquals(a.next(it), b.next(it)) }
    }
}

class PatternGameTest {
    @Test fun correctSequenceAdvancesLength() {
        val g = PatternGame(8L)
        val seq = g.sequence()
        seq.dropLast(1).forEach { assertEquals(TapOutcome.CORRECT_STEP, g.tap(it)) }
        assertEquals(TapOutcome.ROUND_COMPLETE, g.tap(seq.last()))
        assertEquals(GameConfig.PATTERN_START_LENGTH + 1, g.length)
        assertEquals(GameConfig.PATTERN_START_LENGTH, g.highest)
    }

    @Test fun wrongTapCostsLifeThenEnds() {
        val g = PatternGame(8L)
        val wrong = (g.expectedNext() + 1) % PatternGame.TILES
        assertEquals(TapOutcome.WRONG, g.tap(wrong)); assertFalse(g.over)
        assertEquals(TapOutcome.WRONG, g.tap((g.expectedNext() + 1) % PatternGame.TILES)); assertTrue(g.over)
    }

    @Test fun seedSharesPrefixAcrossLengths() {
        val g1 = PatternGame(77L); val g2 = PatternGame(77L)
        assertEquals(g1.sequence(), g2.sequence())
    }
}

class DailyAndConfigTest {
    @Test fun dailyIsDeterministicPerDate() {
        assertEquals(Daily.forKey("2026-10-29"), Daily.forKey("2026-10-29"))
        assertNotEquals(Daily.forKey("2026-10-29").seed, Daily.forKey("2026-10-30").seed)
        assertTrue(Daily.forKey("2026-10-29").type in GameType.all)
    }

    @Test fun levelCurveIsMonotonic() {
        assertEquals(1, GameConfig.levelForXp(0)); assertEquals(2, GameConfig.levelForXp(50))
        assertEquals(3, GameConfig.levelForXp(150)); assertEquals(1, GameConfig.levelForXp(49))
    }

    @Test fun achievementConditions() {
        assertTrue("first_steps" in Achievements.satisfied(GameType.MEMORY, true, 2, 2, 1, 1, false, false))
        assertTrue("quick_thinker" in Achievements.satisfied(GameType.MATH, true, 9, 10, 5, 3, false, false))
        assertFalse("quick_thinker" in Achievements.satisfied(GameType.MATH, true, 7, 10, 5, 3, false, false))
        assertTrue("dedicated_player" in Achievements.satisfied(GameType.PATTERN, true, 6, 6, 6, 10, false, false))
        assertTrue("streak_master" in Achievements.satisfied(GameType.MEMORY, true, 2, 2, 1, 3, false, false, currentStreak = 3))
        assertTrue("vault_conqueror" in Achievements.satisfied(GameType.MEMORY, true, 2, 2, 1, 25, false, false))
    }

    @Test fun skinCatalogConfig() {
        assertEquals(4, GameConfig.AVAILABLE_SKINS.size)
        assertEquals("NEON_CYAN", GameConfig.getSkin("NON_EXISTENT").id)
        assertEquals("Solar Amber", GameConfig.getSkin("SOLAR_GOLD").name)
    }
}
