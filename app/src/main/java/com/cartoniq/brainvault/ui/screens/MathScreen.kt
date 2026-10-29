package com.cartoniq.brainvault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.cartoniq.brainvault.game.model.DailyDef
import com.cartoniq.brainvault.game.model.GameConfig
import com.cartoniq.brainvault.ui.components.*
import com.cartoniq.brainvault.ui.theme.CoinGold
import com.cartoniq.brainvault.ui.theme.ErrorRed
import com.cartoniq.brainvault.ui.theme.NeonCyan
import com.cartoniq.brainvault.ui.theme.NeonPurple
import com.cartoniq.brainvault.ui.theme.SuccessGreen
import com.cartoniq.brainvault.util.LocalFeedback
import com.cartoniq.brainvault.util.findActivity
import com.cartoniq.brainvault.viewmodel.MathViewModel
import com.cartoniq.brainvault.viewmodel.MathViewModel.Phase

@Composable
fun MathScreen(daily: DailyDef?, coins: Int, onExit: () -> Unit, onWatchAd: () -> Unit, vm: MathViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.start(daily) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    var showLeave by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    val active = ui.phase == Phase.PLAYING
    BackHandler { if (active) showLeave = true else onExit() }
    SfxEffect(ui.sfx, ui.sfxN)
    LaunchedEffect(ui.phase == Phase.DONE) {
        if (ui.phase == Phase.DONE) ctx.findActivity()?.let { (it.application as BrainVaultApp).adManager.onRoundCompleted(it) }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.math_title), onBack = { if (active) showLeave = true else onExit() }) { CoinChip(coins) }
        when (ui.phase) {
            Phase.CHOOSE -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NeonCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                    Text(stringResource(R.string.choose_mode), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Select your preferred mental calculation challenge:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ui.bestOnRecord > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.best_fmt, ui.bestOnRecord), color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
                NeonCard(borderColor = NeonCyan.copy(alpha = 0.4f), onClick = { vm.choose(true) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = NeonCyan.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("⚡", fontSize = 20.sp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mode_timed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.mode_timed_d, GameConfig.MATH_TIMED_SECONDS), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                NeonCard(borderColor = NeonPurple.copy(alpha = 0.4f), onClick = { vm.choose(false) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = NeonPurple.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("🎯", fontSize = 20.sp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mode_relaxed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.mode_relaxed_d, GameConfig.MATH_QUESTIONS), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Phase.DONE -> ResultPanel(
                title = stringResource(R.string.round_over),
                score = ui.score,
                stats = listOf(
                    stringResource(R.string.correct) to "${ui.correct}/${ui.answered}",
                    stringResource(R.string.accuracy) to "${if (ui.answered > 0) ui.correct * 100 / ui.answered else 0}%",
                    stringResource(R.string.best_streak) to "${ui.bestStreak}x combo",
                    stringResource(R.string.avg_response) to stringResource(R.string.seconds_fmt, ui.avgResponseMs / 1000.0),
                ),
                summary = ui.result, best = ui.bestScore,
                primary = if (ui.daily) null else stringResource(R.string.replay), onPrimary = { vm.replay() },
                secondary = if (ui.daily) null else stringResource(R.string.change_mode), onSecondary = { vm.backToChoose() },
                onHome = onExit,
            )
            Phase.PLAYING -> {
                val q = ui.question ?: return@Column
                val isUrgent = ui.timed && ui.remainingMs <= 10000L

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(stringResource(R.string.score), ui.score.toString(), Modifier.weight(1f), NeonCyan)
                    StatBox(
                        stringResource(R.string.streak),
                        if (ui.streak > 0) "${ui.streak}x" else "0",
                        Modifier.weight(1f),
                        if (ui.streak >= 3) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurface,
                    )
                    if (ui.timed) {
                        val secs = (ui.remainingMs + 999) / 1000
                        StatBox(
                            stringResource(R.string.time_left),
                            stringResource(R.string.seconds_int, secs),
                            Modifier.weight(1f),
                            if (isUrgent) ErrorRed else NeonPurple,
                        )
                    } else {
                        StatBox(
                            stringResource(R.string.question),
                            "${minOf(ui.answered + 1, GameConfig.MATH_QUESTIONS)}/${GameConfig.MATH_QUESTIONS}",
                            Modifier.weight(1f),
                            NeonPurple,
                        )
                    }
                }

                // Combo Multiplier Indicator
                if (ui.streak >= 2) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF9100).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f)),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text("🔥 STREAK COMBO: +${minOf(ui.streak, 5) * 2} BONUS PTS PER ANSWER", color = Color(0xFFFF9100), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                MessageLine(ui.messageRes) { vm.clearMessage() }
                Spacer(Modifier.height(14.dp))

                if (ui.paused) {
                    NeonCard {
                        Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(stringResource(R.string.resume), containerColor = NeonCyan) { vm.togglePause() }
                    }
                } else {
                    // Question Card
                    NeonCard(borderColor = if (isUrgent) ErrorRed.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.4f)) {
                        Text(
                            q.text + " = ?",
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val sel = ui.selected
                    val feedbackText = when {
                        sel == null -> ""
                        sel == q.answer -> stringResource(R.string.correct_feedback)
                        else -> stringResource(R.string.wrong_feedback, q.answer)
                    }
                    Text(
                        feedbackText,
                        Modifier.fillMaxWidth().heightIn(min = 28.dp),
                        textAlign = TextAlign.Center,
                        color = if (sel == q.answer) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    if (ui.hintShown) {
                        Text(
                            stringResource(R.string.math_hint, q.op.toString(), q.answer % 10),
                            Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            textAlign = TextAlign.Center,
                            color = NeonPurple,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    // 2x2 Answer Grid
                    q.options.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { opt ->
                                val isAnswer = sel != null && opt == q.answer
                                val isWrong = sel == opt && opt != q.answer
                                val colors = when {
                                    isAnswer -> ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black, disabledContainerColor = SuccessGreen, disabledContentColor = Color.Black)
                                    isWrong -> ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White, disabledContainerColor = ErrorRed, disabledContentColor = Color.White)
                                    else -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val border = when {
                                    isAnswer -> BorderStroke(2.dp, SuccessGreen)
                                    isWrong -> BorderStroke(2.dp, ErrorRed)
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                }
                                Button(
                                    onClick = { feedback?.tap(); vm.answer(opt) },
                                    enabled = sel == null,
                                    modifier = Modifier.weight(1f).height(68.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = colors,
                                    border = border,
                                ) {
                                    Text(
                                        (if (isAnswer) "✓ " else if (isWrong) "✗ " else "") + opt,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(stringResource(if (ui.paused) R.string.resume else R.string.pause), Modifier.weight(1f)) { vm.togglePause() }
                    SecondaryButton(stringResource(R.string.hint), Modifier.weight(1f), enabled = !ui.paused && ui.selected == null) { showHint = true }
                }
            }
        }
    }
    if (showLeave) ConfirmDialog(stringResource(R.string.leave_title), stringResource(R.string.leave_message), stringResource(R.string.leave), stringResource(R.string.keep_playing), { showLeave = false; onExit() }, { showLeave = false })
    if (showHint) HintDialog(coins, onConfirm = { showHint = false; vm.useHint() }, onWatchAd = onWatchAd, onDismiss = { showHint = false })
}
