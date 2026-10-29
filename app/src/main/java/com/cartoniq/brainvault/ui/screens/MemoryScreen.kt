package com.cartoniq.brainvault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.cartoniq.brainvault.game.logic.CardState
import com.cartoniq.brainvault.game.logic.MemoryCard
import com.cartoniq.brainvault.game.model.DailyDef
import com.cartoniq.brainvault.game.model.GameConfig
import com.cartoniq.brainvault.ui.components.*
import com.cartoniq.brainvault.ui.theme.NeonCyan
import com.cartoniq.brainvault.ui.theme.NeonPurple
import com.cartoniq.brainvault.ui.theme.SuccessGreen
import com.cartoniq.brainvault.util.LocalFeedback
import com.cartoniq.brainvault.util.LocalReducedMotion
import com.cartoniq.brainvault.util.findActivity
import com.cartoniq.brainvault.util.formatTime
import com.cartoniq.brainvault.viewmodel.MemoryViewModel
import com.cartoniq.brainvault.viewmodel.MemoryViewModel.Phase

@Composable
fun MemoryScreen(
    daily: DailyDef?,
    coins: Int,
    cardSkinId: String = "NEON_CYAN",
    onExit: () -> Unit,
    onWatchAd: () -> Unit,
    vm: MemoryViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val ctx = LocalContext.current
    val skin = GameConfig.getSkin(cardSkinId)
    val skinAccent = Color(skin.primaryColorHex)

    LaunchedEffect(Unit) { vm.start(daily) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    var showLeave by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    val active = ui.phase == Phase.PREVIEW || ui.phase == Phase.PLAYING
    BackHandler { if (active) showLeave = true else onExit() }
    SfxEffect(ui.sfx, ui.sfxN)
    LaunchedEffect(ui.phase == Phase.DONE) {
        if (ui.phase == Phase.DONE) ctx.findActivity()?.let { (it.application as BrainVaultApp).adManager.onRoundCompleted(it) }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.memory_title), onBack = { if (active) showLeave = true else onExit() }) {
            CoinChip(coins)
        }
        val s = ui.state
        if (ui.phase == Phase.LOADING || s == null) return@Column
        if (ui.phase == Phase.DONE) {
            ResultPanel(
                title = stringResource(R.string.level_complete, ui.level),
                score = ui.score,
                stats = listOf(
                    stringResource(R.string.moves) to s.moves.toString(),
                    stringResource(R.string.time) to formatTime(ui.elapsedMs),
                    stringResource(R.string.accuracy) to "${s.spec.pairs * 100 / maxOf(1, s.moves)}%",
                ),
                summary = ui.result, best = ui.bestScore,
                primary = if (ui.daily) null else stringResource(R.string.next_level), onPrimary = { vm.nextLevel() },
                secondary = if (ui.daily) null else stringResource(R.string.replay), onSecondary = { vm.replay() },
                onHome = onExit,
            )
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(stringResource(R.string.level), ui.level.toString(), Modifier.weight(1f), skinAccent)
            StatBox(stringResource(R.string.moves), s.moves.toString(), Modifier.weight(1f))
            StatBox(stringResource(R.string.pairs), "${s.matchedPairs}/${s.spec.pairs}", Modifier.weight(1f), SuccessGreen)
            StatBox(stringResource(R.string.time), formatTime(ui.elapsedMs), Modifier.weight(1f))
        }
        Text(
            if (ui.phase == Phase.PREVIEW) stringResource(R.string.memorize) else if (ui.paused) stringResource(R.string.paused) else stringResource(R.string.find_pairs),
            Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium, color = if (ui.phase == Phase.PREVIEW) skinAccent else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        MessageLine(ui.messageRes) { vm.clearMessage() }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (ui.paused) {
                NeonCard(Modifier.align(Alignment.Center)) {
                    Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.paused_hidden), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(stringResource(R.string.resume), containerColor = skinAccent) { vm.togglePause() }
                }
            } else {
                val names = stringArrayResource(R.array.symbol_names)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(s.spec.cols),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(s.cards) { i, c ->
                        MemoryCardView(
                            card = c,
                            name = names[c.symbol % names.size],
                            hint = ui.hintIndex == i,
                            enabled = ui.phase == Phase.PLAYING && !s.locked,
                            accentColor = skinAccent,
                        ) {
                            feedback?.tap(); vm.tap(i)
                        }
                    }
                }
            }
        }
        if (ui.phase == Phase.PLAYING) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(if (ui.paused) R.string.resume else R.string.pause), Modifier.weight(1f)) { vm.togglePause() }
                SecondaryButton(stringResource(R.string.hint), Modifier.weight(1f), enabled = !ui.paused && !s.locked) { showHint = true }
                if (!ui.daily) SecondaryButton(stringResource(R.string.restart), Modifier.weight(1f)) { showRestart = true }
            }
        }
    }
    if (showLeave) ConfirmDialog(stringResource(R.string.leave_title), stringResource(R.string.leave_message), stringResource(R.string.leave), stringResource(R.string.keep_playing), { showLeave = false; onExit() }, { showLeave = false })
    if (showRestart) ConfirmDialog(stringResource(R.string.restart_title), stringResource(R.string.restart_message), stringResource(R.string.restart), stringResource(R.string.cancel), { showRestart = false; vm.replay() }, { showRestart = false })
    if (showHint) HintDialog(coins, onConfirm = { showHint = false; vm.useHint() }, onWatchAd = onWatchAd, onDismiss = { showHint = false })
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    name: String,
    hint: Boolean,
    enabled: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val isRevealed = card.state != CardState.HIDDEN || hint
    val isMatched = card.state == CardState.MATCHED

    // 3D Card Flip Animation
    val rotation by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = if (reduced) snap() else tween(durationMillis = 280),
        label = "cardFlip",
    )

    val isFaceUp = rotation > 90f

    val bg by animateColorAsState(
        when {
            isMatched -> SuccessGreen.copy(alpha = 0.25f)
            isFaceUp -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        if (reduced) snap() else tween(180), label = "cardBg",
    )

    val borderColor = when {
        isMatched -> SuccessGreen
        hint -> NeonPurple
        isFaceUp -> accentColor
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }

    val hiddenDesc = stringResource(R.string.card_hidden)
    val matchedDesc = stringResource(R.string.card_matched, name)
    val shownDesc = stringResource(R.string.card_shown, name)
    val desc = when { isMatched -> matchedDesc; isRevealed -> shownDesc; else -> hiddenDesc }

    Box(
        Modifier
            .aspectRatio(1f)
            .heightIn(min = 48.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(BorderStroke(if (isRevealed) 2.dp else 1.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && card.state == CardState.HIDDEN, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        if (isFaceUp) {
            // Invert the text back since the container is rotated 180deg
            Box(Modifier.graphicsLayer { rotationY = 180f }, contentAlignment = Alignment.Center) {
                Text(
                    Symbols.glyphs[card.symbol % Symbols.glyphs.size],
                    fontSize = 32.sp,
                    color = Symbols.colors[card.symbol % Symbols.colors.size],
                )
                if (isMatched) {
                    Text(
                        "✓",
                        Modifier.align(Alignment.TopEnd).padding(4.dp),
                        color = SuccessGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else {
            // Card Back - Sleek Vault Symbol
            Text(
                "◆",
                fontSize = 20.sp,
                color = accentColor.copy(alpha = 0.4f),
            )
        }
    }
}
