package com.cartoniq.brainvault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartoniq.brainvault.BrainVaultApp
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.game.logic.PatternGame
import com.cartoniq.brainvault.game.logic.PatternPhase
import com.cartoniq.brainvault.game.model.DailyDef
import com.cartoniq.brainvault.game.model.GameConfig
import com.cartoniq.brainvault.ui.components.*
import com.cartoniq.brainvault.ui.theme.CoinGold
import com.cartoniq.brainvault.ui.theme.ErrorRed
import com.cartoniq.brainvault.ui.theme.NeonCyan
import com.cartoniq.brainvault.ui.theme.NeonPurple
import com.cartoniq.brainvault.ui.theme.SuccessGreen
import com.cartoniq.brainvault.util.LocalFeedback
import com.cartoniq.brainvault.util.LocalReducedMotion
import com.cartoniq.brainvault.util.findActivity
import com.cartoniq.brainvault.viewmodel.PatternViewModel

@Composable
fun PatternScreen(daily: DailyDef?, coins: Int, onExit: () -> Unit, onWatchAd: () -> Unit, vm: PatternViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val reduced = LocalReducedMotion.current
    val ctx = LocalContext.current
    vm.setReducedMotion(reduced)
    LaunchedEffect(Unit) { vm.start(daily) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    var showLeave by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    val active = !ui.done
    BackHandler { if (active) showLeave = true else onExit() }
    SfxEffect(ui.sfx, ui.sfxN)
    LaunchedEffect(ui.done) {
        if (ui.done) ctx.findActivity()?.let { (it.application as BrainVaultApp).adManager.onRoundCompleted(it) }
    }
    val names = stringArrayResource(R.array.symbol_names)

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.pattern_title), onBack = { if (active) showLeave = true else onExit() }) { CoinChip(coins) }
        if (ui.done) {
            ResultPanel(
                title = stringResource(R.string.round_over),
                score = ui.score,
                stats = listOf(stringResource(R.string.highest_length) to "${ui.highest} steps"),
                summary = ui.result, best = ui.bestScore,
                primary = if (ui.daily) null else stringResource(R.string.replay), onPrimary = { vm.replay() },
                secondary = null, onSecondary = {}, onHome = onExit,
            )
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(stringResource(R.string.length), ui.length.toString(), Modifier.weight(1f), NeonPurple)
            StatBox(stringResource(R.string.score), ui.score.toString(), Modifier.weight(1f), NeonCyan)
            StatBox(
                stringResource(R.string.lives),
                "♥ ".repeat(maxOf(ui.lives, 0)).trim().ifEmpty { "–" },
                Modifier.weight(1f),
                if (ui.lives <= 1) ErrorRed else SuccessGreen,
            )
        }

        // Sequence Progress Step Dots
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (stepIdx in 0 until ui.length) {
                val isCompleted = stepIdx < ui.step
                val isCurrent = stepIdx == ui.step && ui.phase == PatternPhase.INPUT
                val dotColor = when {
                    isCompleted -> SuccessGreen
                    isCurrent -> NeonCyan
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isCurrent) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            1.dp,
                            if (isCurrent) NeonCyan else Color.Transparent,
                            CircleShape
                        )
                )
            }
        }

        val banner = when (ui.phase) {
            PatternPhase.READY -> stringResource(R.string.get_ready)
            PatternPhase.PLAYBACK -> if (ui.step > 0) stringResource(R.string.watch_step, ui.step, ui.length, ui.lit?.let { names[it % names.size] } ?: "") else stringResource(R.string.watch)
            PatternPhase.INPUT -> stringResource(R.string.your_turn, ui.step + 1, ui.length)
            PatternPhase.ROUND_SUCCESS -> stringResource(R.string.round_success)
            PatternPhase.ROUND_FAILURE -> stringResource(R.string.round_failure, ui.lives)
            PatternPhase.PAUSED -> stringResource(R.string.paused)
        }
        Text(
            banner,
            Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics { contentDescription = banner },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (ui.phase) {
                PatternPhase.ROUND_SUCCESS -> SuccessGreen
                PatternPhase.ROUND_FAILURE -> ErrorRed
                PatternPhase.PLAYBACK -> NeonPurple
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        MessageLine(ui.messageRes) { vm.clearMessage() }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (ui.phase == PatternPhase.PAUSED) {
                NeonCard {
                    Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.pattern_resume_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(stringResource(R.string.resume), containerColor = NeonPurple) { vm.togglePause() }
                }
            } else {
                Column(Modifier.widthIn(max = 400.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (r in 0 until 3) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (c in 0 until 3) {
                                val t = r * 3 + c
                                TileView(
                                    index = t,
                                    name = names[t % names.size],
                                    lit = ui.lit == t,
                                    hint = ui.hintTile == t,
                                    pressed = ui.pressed == t && ui.phase == PatternPhase.INPUT,
                                    enabled = ui.phase == PatternPhase.INPUT,
                                    reduced = reduced,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    feedback?.tap()
                                    vm.tap(t)
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                stringResource(if (ui.phase == PatternPhase.PAUSED) R.string.resume else R.string.pause),
                Modifier.weight(1f),
                enabled = ui.phase == PatternPhase.PLAYBACK || ui.phase == PatternPhase.INPUT || ui.phase == PatternPhase.PAUSED,
            ) { vm.togglePause() }
            SecondaryButton(stringResource(R.string.hint), Modifier.weight(1f), enabled = ui.phase == PatternPhase.INPUT) { showHint = true }
        }
    }
    if (showLeave) ConfirmDialog(stringResource(R.string.leave_title), stringResource(R.string.leave_message), stringResource(R.string.leave), stringResource(R.string.keep_playing), { showLeave = false; onExit() }, { showLeave = false })
    if (showHint) HintDialog(coins, onConfirm = { showHint = false; vm.useHint() }, onWatchAd = onWatchAd, onDismiss = { showHint = false })
}

@Composable
private fun TileView(
    index: Int,
    name: String,
    lit: Boolean,
    hint: Boolean,
    pressed: Boolean,
    enabled: Boolean,
    reduced: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val base = Symbols.colors[index % Symbols.colors.size]
    val glow = lit || hint
    val bg by animateColorAsState(
        when {
            glow -> base
            pressed -> base.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        if (reduced) snap() else tween(120), label = "tileBg",
    )
    val borderColor = when {
        glow -> Color.White
        pressed -> base
        else -> base.copy(alpha = 0.4f)
    }
    val desc = stringResource(if (glow) R.string.tile_lit else R.string.tile_plain, name)
    Box(
        modifier
            .aspectRatio(1f)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(BorderStroke(if (glow) 3.5.dp else 1.5.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Symbols.glyphs[index % Symbols.glyphs.size],
            fontSize = if (glow) 38.sp else 28.sp,
            color = if (glow) Color.Black else base,
            fontWeight = FontWeight.Bold,
        )
    }
}
