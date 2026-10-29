package com.cartoniq.brainvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.ads.AdManager
import com.cartoniq.brainvault.data.entity.PersonalBest
import com.cartoniq.brainvault.data.entity.PlayerProfile
import com.cartoniq.brainvault.game.model.GameConfig
import com.cartoniq.brainvault.game.model.GameType
import com.cartoniq.brainvault.ui.components.*
import com.cartoniq.brainvault.ui.theme.CoinGold
import com.cartoniq.brainvault.ui.theme.NeonCyan
import com.cartoniq.brainvault.ui.theme.NeonPurple
import com.cartoniq.brainvault.ui.theme.SuccessGreen
import com.cartoniq.brainvault.viewmodel.DailyUi
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

@Composable
fun HomeScreen(
    profile: PlayerProfile, daily: DailyUi, bests: List<PersonalBest>, memoryNext: Int,
    onNav: (String) -> Unit, onWatchAd: () -> Unit,
) {
    val level = GameConfig.levelForXp(profile.xp)
    val lo = GameConfig.xpForLevel(level)
    val hi = GameConfig.xpForLevel(level + 1)
    val progress = ((profile.xp - lo).toFloat() / (hi - lo)).coerceIn(0f, 1f)
    fun best(t: String) = bests.firstOrNull { it.gameType == t }?.bestScore ?: 0
    val dailyName = stringResource(gameTitle(daily.def.type))

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header: Branding, Level, Streak, Coins, Settings
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan,
                            )
                        }
                        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StreakChip(profile.streakCount)
                    Spacer(Modifier.width(8.dp))
                    CoinChip(profile.coins)
                    IconButton(onClick = { onNav("settings") }, Modifier.size(44.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Player Level & XP Card
            item {
                NeonCard(borderColor = NeonPurple.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonPurple.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonPurple),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$level", fontWeight = FontWeight.ExtraBold, color = NeonPurple, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.level_fmt, level), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.weight(1f))
                                Text(stringResource(R.string.xp_fmt, profile.xp, hi), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = NeonPurple,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }

            // Daily Brain Challenge Hero Card
            item {
                NeonCard(borderColor = if (daily.completed) SuccessGreen.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.7f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (daily.completed) SuccessGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (daily.completed) "✓" else "★", fontSize = 22.sp, color = if (daily.completed) SuccessGreen else NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.daily_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.daily_desc, dailyName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(if (daily.completed) R.string.daily_done else R.string.daily_reward, GameConfig.DAILY_BONUS_COINS),
                        color = if (daily.completed) SuccessGreen else NeonCyan,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        stringResource(if (daily.completed) R.string.completed else R.string.start),
                        enabled = !daily.completed,
                        containerColor = if (daily.completed) MaterialTheme.colorScheme.surfaceVariant else NeonCyan,
                        contentColor = if (daily.completed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black,
                    ) { onNav("${gameRoute(daily.def.type)}/true") }
                }
            }

            // Game Cards
            item {
                GameCard(
                    title = R.string.memory_title,
                    desc = R.string.memory_desc,
                    badge = stringResource(R.string.memory_level_fmt, memoryNext),
                    best = best(GameType.MEMORY),
                    accentColor = NeonCyan,
                    icon = "🧠",
                ) { onNav("memory/false") }
            }
            item {
                GameCard(
                    title = R.string.math_title,
                    desc = R.string.math_desc,
                    badge = "Timed & Relaxed",
                    best = best(GameType.MATH),
                    accentColor = NeonPurple,
                    icon = "➗",
                ) { onNav("math/false") }
            }
            item {
                GameCard(
                    title = R.string.pattern_title,
                    desc = R.string.pattern_desc,
                    badge = "Memory Matrix",
                    best = best(GameType.PATTERN),
                    accentColor = SuccessGreen,
                    icon = "🔮",
                ) { onNav("pattern/false") }
            }

            // Quick Hub Buttons
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(stringResource(R.string.stats_title), Modifier.weight(1f)) { onNav("stats") }
                    SecondaryButton(stringResource(R.string.achievements_title), Modifier.weight(1f)) { onNav("achievements") }
                    SecondaryButton(stringResource(R.string.shop_title), Modifier.weight(1f)) { onNav("shop") }
                }
            }
            item { SecondaryButton(stringResource(R.string.watch_ad, GameConfig.AD_REWARD)) { onWatchAd() } }
        }
        BannerAd()
    }
}

private fun gameRoute(type: String) = when (type) { GameType.MEMORY -> "memory"; GameType.MATH -> "math"; else -> "pattern" }
private fun gameTitle(type: String) = when (type) { GameType.MEMORY -> R.string.memory_title; GameType.MATH -> R.string.math_title; else -> R.string.pattern_title }

@Composable
private fun GameCard(
    title: Int,
    desc: Int,
    badge: String?,
    best: Int,
    accentColor: Color,
    icon: String,
    onClick: () -> Unit
) {
    NeonCard(borderColor = accentColor.copy(alpha = 0.35f), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (best > 0) stringResource(R.string.best_fmt, best) else stringResource(R.string.no_score_yet),
                color = accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BannerAd() {
    var loaded by remember { mutableStateOf(false) }
    val mod = if (loaded) Modifier.fillMaxWidth() else Modifier.size(0.dp)
    AndroidView(
        modifier = mod,
        factory = { ctx ->
            try {
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.BANNER_ID
                    adListener = object : AdListener() { override fun onAdLoaded() { loaded = true } }
                    loadAd(AdRequest.Builder().build())
                }
            } catch (_: Throwable) { android.view.View(ctx) }
        },
    )
}
