package com.cartoniq.brainvault.ui.components

import androidx.compose.ui.graphics.Color

/** Distinct glyph + colour per symbol; shape differs so colour is never the only cue. Names live in R.array.symbol_names. */
object Symbols {
    val glyphs = listOf("●", "▲", "■", "◆", "★", "✚", "♥", "☾", "☀", "♣", "♠", "☂")
    val colors = listOf(
        Color(0xFF00E5FF), Color(0xFFB388FF), Color(0xFF00E676), Color(0xFFFFC400),
        Color(0xFFFF5370), Color(0xFF40C4FF), Color(0xFFFF80AB), Color(0xFFFFAB40),
        Color(0xFFFFEA00), Color(0xFF69F0AE), Color(0xFFE040FB), Color(0xFF82B1FF),
    )
}
