package com.cartoniq.brainvault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.game.model.*
import com.cartoniq.brainvault.ui.theme.*
import com.cartoniq.brainvault.util.LocalFeedback

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(18.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(18.dp), content = content)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.Black,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CoinChip(coins: Int) {
    val desc = stringResource(R.string.coins_cd, coins)
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.35f)),
        modifier = Modifier.semantics { contentDescription = desc },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(CoinGold))
            Spacer(Modifier.width(8.dp))
            Text(coins.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StreakChip(streak: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (streak > 0) Color(0xFFFF9100).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("$streak", fontWeight = FontWeight.Bold, color = if (streak > 0) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String, text: String, confirm: String, dismiss: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
fun HintDialog(coins: Int, onConfirm: () -> Unit, onWatchAd: () -> Unit, onDismiss: () -> Unit) {
    if (coins >= GameConfig.HINT_COST) {
        ConfirmDialog(
            stringResource(R.string.hint_title),
            stringResource(R.string.hint_message, GameConfig.HINT_COST, coins),
            stringResource(R.string.hint_use), stringResource(R.string.cancel), onConfirm, onDismiss,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.hint_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.hint_poor, GameConfig.HINT_COST, coins)) },
            confirmButton = { TextButton(onClick = { onWatchAd(); onDismiss() }) { Text(stringResource(R.string.watch_ad, GameConfig.AD_REWARD), fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
fun SfxEffect(sfx: Int, n: Int) {
    val fb = LocalFeedback.current
    LaunchedEffect(n) {
        if (n > 0) when (sfx) { 1 -> fb?.good(); 2 -> fb?.bad() }
    }
}

@Composable
fun MessageLine(resId: Int?, onShown: () -> Unit) {
    if (resId != null) {
        LaunchedEffect(resId) { kotlinx.coroutines.delay(2500); onShown() }
        Text(
            stringResource(resId),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RewardPanel(summary: RewardSummary?) {
    NeonCard(borderColor = NeonCyan.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rewards), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (summary != null && !summary.alreadyRecorded && summary.totalCoins > 0) {
                Surface(color = NeonCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                    Text("+${summary.totalCoins} COINS", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            summary == null -> Text(stringResource(R.string.saving), color = MaterialTheme.colorScheme.onSurfaceVariant)
            summary.alreadyRecorded -> Text(stringResource(R.string.already_recorded), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                if (summary.lines.isEmpty()) Text(stringResource(R.string.no_rewards), color = MaterialTheme.colorScheme.onSurfaceVariant)
                summary.lines.forEach { l ->
                    val label = when (l.type) {
                        RewardType.BASE -> stringResource(R.string.reward_base)
                        RewardType.PERFORMANCE -> stringResource(R.string.reward_bonus)
                        RewardType.DAILY -> stringResource(R.string.reward_daily)
                        RewardType.STREAK -> stringResource(R.string.reward_streak)
                        RewardType.ACHIEVEMENT -> Achievements.find(l.achievementId)?.let { stringResource(R.string.reward_achievement, stringResource(it.title)) } ?: ""
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.reward_line, l.coins, l.xp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                if (summary.streak > 1) {
                    Text("🔥 ${stringResource(R.string.streak_days, summary.streak)}!", color = Color(0xFFFF9100), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                if (summary.newBest) Text(stringResource(R.string.new_best), color = SuccessGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                if (summary.newLevel > summary.oldLevel) Text(stringResource(R.string.level_up, summary.newLevel), color = NeonPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResultPanel(
    title: String, score: Int, stats: List<Pair<String, String>>, summary: RewardSummary?, best: Int,
    primary: String?, onPrimary: () -> Unit, secondary: String?, onSecondary: () -> Unit, onHome: () -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            NeonCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.score_fmt, score), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (best > 0) Text(stringResource(R.string.best_fmt, best), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                stats.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(k, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { RewardPanel(summary) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (primary != null) PrimaryButton(primary, onClick = onPrimary)
                if (secondary != null) SecondaryButton(secondary, onClick = onSecondary)
                SecondaryButton(stringResource(R.string.home), onClick = onHome)
            }
        }
    }
}
