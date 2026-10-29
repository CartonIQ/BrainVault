package com.cartoniq.brainvault.game.logic

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class MathQuestion(val text: String, val op: Char, val answer: Int, val options: List<Int>)

/** Deterministic when a seed is supplied (daily challenge). Difficulty depends only on the question index. */
class MathGenerator(seed: Long?) {
    private val rng: Random = if (seed != null) Random(seed) else Random(System.nanoTime())

    fun next(index: Int): MathQuestion {
        val d = (1 + index / 4).coerceAtMost(4)
        val ops = when (d) {
            1 -> "+-"
            2 -> "+-×"
            else -> "+-×÷"
        }
        val op = ops[rng.nextInt(ops.length)]
        val a: Int
        val b: Int
        val ans: Int
        when (op) {
            '+' -> {
                val m = 10 * d + 10
                a = rng.nextInt(1, m + 1); b = rng.nextInt(1, m + 1); ans = a + b
            }
            '-' -> {
                val m = 10 * d + 10
                val x = rng.nextInt(1, m + 1); val y = rng.nextInt(1, m + 1)
                a = max(x, y); b = min(x, y); ans = a - b
            }
            '×' -> {
                val m = 5 + d
                a = rng.nextInt(2, m + 1); b = rng.nextInt(2, m + 1); ans = a * b
            }
            else -> {
                val q = rng.nextInt(2, 6 + d)
                b = rng.nextInt(2, 6 + d); a = q * b; ans = q
            }
        }
        return MathQuestion("$a $op $b", op, ans, options(ans))
    }

    private fun options(ans: Int): List<Int> {
        val opts = linkedSetOf(ans)
        var spread = max(3, ans / 5)
        var guard = 0
        while (opts.size < 4) {
            val off = rng.nextInt(1, spread + 1) * (if (rng.nextBoolean()) 1 else -1)
            val c = ans + off
            if (c >= 0) opts.add(c)
            if (++guard > 40) { spread += 3; guard = 0 }
        }
        return opts.toList().shuffled(rng)
    }
}
