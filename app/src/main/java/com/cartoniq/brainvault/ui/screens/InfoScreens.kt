package com.cartoniq.brainvault.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.data.entity.GameSession
import com.cartoniq.brainvault.data.entity.PersonalBest
import com.cartoniq.brainvault.data.entity.PlayerProfile
import com.cartoniq.brainvault.game.model.Achievements
import com.cartoniq.brainvault.game.model.GameConfig
import com.cartoniq.brainvault.game.model.GameType
import com.cartoniq.brainvault.game.model.SkinDef
import com.cartoniq.brainvault.ui.components.*
import com.cartoniq.brainvault.ui.theme.CoinGold
import com.cartoniq.brainvault.ui.theme.NeonCyan
import com.cartoniq.brainvault.ui.theme.NeonPurple
import com.cartoniq.brainvault.ui.theme.SuccessGreen

@Composable
fun StatsScreen(profile: PlayerProfile, sessions: List<GameSession>, bests: List<PersonalBest>, unlocked: Int, onBack: () -> Unit) {
    fun of(t: String) = sessions.filter { it.gameType == t }
    fun best(t: String) = bests.firstOrNull { it.gameType == t }
    val mem = of(GameType.MEMORY); val math = of(GameType.MATH); val pat = of(GameType.PATTERN)
    val memTurns = mem.sumOf { it.total }
    val mathTotal = math.sumOf { it.total }
    val avgResp = if (mathTotal > 0) math.sumOf { it.avgResponseMs * it.total } / mathTotal else 0L

    @Composable fun row(k: String, v: String, accent: Color? = null) = Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(k, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, fontWeight = FontWeight.Bold, color = accent ?: MaterialTheme.colorScheme.onSurface)
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.stats_title), onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                NeonCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                    Text(stringResource(R.string.overview), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Spacer(Modifier.height(6.dp))
                    row(stringResource(R.string.total_games), sessions.size.toString())
                    row(stringResource(R.string.completed_games), sessions.count { it.completed }.toString(), SuccessGreen)
                    row(stringResource(R.string.level), GameConfig.levelForXp(profile.xp).toString(), NeonPurple)
                    row("Current Daily Streak", "${profile.streakCount} days", Color(0xFFFF9100))
                    row("Best Streak", "${profile.bestStreak} days", Color(0xFFFF9100))
                    row(stringResource(R.string.achievements_title), "$unlocked/${Achievements.all.size}")
                }
            }
            item {
                NeonCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                    Text(stringResource(R.string.memory_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    row(stringResource(R.string.games_played), mem.size.toString())
                    row(stringResource(R.string.best_score), (best(GameType.MEMORY)?.bestScore ?: 0).toString(), NeonCyan)
                    row(stringResource(R.string.highest_level), (best(GameType.MEMORY)?.bestMetric ?: 0).toString())
                    row(stringResource(R.string.total_moves), memTurns.toString())
                    row(stringResource(R.string.accuracy), if (memTurns > 0) "${mem.sumOf { it.correct } * 100 / memTurns}%" else "–")
                }
            }
            item {
                NeonCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                    Text(stringResource(R.string.math_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    row(stringResource(R.string.games_played), math.size.toString())
                    row(stringResource(R.string.best_score), (best(GameType.MATH)?.bestScore ?: 0).toString(), NeonPurple)
                    row(stringResource(R.string.best_streak), "${best(GameType.MATH)?.bestMetric ?: 0}x", Color(0xFFFF9100))
                    row(stringResource(R.string.accuracy), if (mathTotal > 0) "${math.sumOf { it.correct } * 100 / mathTotal}%" else "–")
                    row(stringResource(R.string.avg_response), if (mathTotal > 0) stringResource(R.string.seconds_fmt, avgResp / 1000.0) else "–")
                }
            }
            item {
                NeonCard(borderColor = SuccessGreen.copy(alpha = 0.3f)) {
                    Text(stringResource(R.string.pattern_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    row(stringResource(R.string.games_played), pat.size.toString())
                    row(stringResource(R.string.best_score), (best(GameType.PATTERN)?.bestScore ?: 0).toString(), SuccessGreen)
                    row(stringResource(R.string.highest_length), "${best(GameType.PATTERN)?.bestMetric ?: 0} steps")
                }
            }
            item {
                Text(stringResource(R.string.stats_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AchievementsScreen(unlocked: Set<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.achievements_title), onBack) {
            Surface(color = NeonPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                Text("${unlocked.size}/${Achievements.all.size} UNLOCKED", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Achievements.all) { a ->
                val done = a.id in unlocked
                NeonCard(borderColor = if (done) SuccessGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (done) SuccessGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (done) "🏆" else "🔒", fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(a.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(a.desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.ach_reward, GameConfig.ACHIEVEMENT_COINS, GameConfig.ACHIEVEMENT_XP), style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                        }
                        Text(
                            stringResource(if (done) R.string.unlocked else R.string.locked),
                            color = if (done) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopScreen(
    profile: PlayerProfile,
    onBack: () -> Unit,
    onBuy: (String, Int) -> Unit,
    onSelect: (String) -> Unit,
) {
    val unlockedList = profile.unlockedSkins.split(",").map { it.trim() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.shop_title), onBack) {
            CoinChip(profile.coins)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                NeonCard(borderColor = CoinGold.copy(alpha = 0.4f)) {
                    Text("Unlock Cyber Vault Skins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CoinGold)
                    Spacer(Modifier.height(4.dp))
                    Text("Customize your Memory Grid cards and neon accents with coins earned from games and daily challenges.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(GameConfig.AVAILABLE_SKINS) { skin ->
                val isUnlocked = skin.id in unlockedList
                val isEquipped = profile.cardSkin == skin.id
                val skinColor = Color(skin.primaryColorHex)

                NeonCard(borderColor = if (isEquipped) skinColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = skinColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, skinColor),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("◆", fontSize = 24.sp, color = skinColor)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(skin.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (isEquipped) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = skinColor.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                                        Text(stringResource(R.string.skin_equipped), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = skinColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                            Text(skin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    when {
                        isEquipped -> {
                            SecondaryButton(stringResource(R.string.skin_equipped), enabled = false) {}
                        }
                        isUnlocked -> {
                            PrimaryButton(stringResource(R.string.skin_equip), containerColor = skinColor) { onSelect(skin.id) }
                        }
                        else -> {
                            val canAfford = profile.coins >= skin.price
                            PrimaryButton(
                                stringResource(R.string.skin_buy, skin.price),
                                containerColor = if (canAfford) CoinGold else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (canAfford) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                enabled = canAfford,
                            ) { onBuy(skin.id, skin.price) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    profile: PlayerProfile, onBack: () -> Unit, onAbout: () -> Unit,
    onSound: (Boolean) -> Unit, onHaptics: (Boolean) -> Unit, onReduced: (Boolean) -> Unit,
    onTheme: (Int) -> Unit, onReset: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.settings_title), onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                NeonCard {
                    Text("Audio & Haptics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    SwitchRow(stringResource(R.string.sound), profile.sound, onSound)
                    SwitchRow(stringResource(R.string.haptics), profile.haptics, onHaptics)
                    SwitchRow(stringResource(R.string.reduced_motion), profile.reducedMotion, onReduced)
                }
            }
            item {
                NeonCard {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    listOf(0 to R.string.theme_system, 1 to R.string.theme_dark, 2 to R.string.theme_light).forEach { (v, label) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = profile.theme == v, role = Role.RadioButton, onClick = { onTheme(v) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = profile.theme == v, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(label))
                        }
                    }
                }
            }
            item { SecondaryButton(stringResource(R.string.about_title)) { onAbout() } }
            item { SecondaryButton(stringResource(R.string.reset_progress)) { confirmReset = true } }
            item {
                Text(
                    stringResource(R.string.version_fmt, "1.0.0"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
    if (confirmReset) ConfirmDialog(
        stringResource(R.string.reset_title),
        stringResource(R.string.reset_message),
        stringResource(R.string.reset_confirm),
        stringResource(R.string.cancel),
        { confirmReset = false; onReset() },
        { confirmReset = false }
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        TopBar(stringResource(R.string.about_title), onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                NeonCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                    Text("BrainVault Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                NeonCard(borderColor = SuccessGreen.copy(alpha = 0.4f)) {
                    Text("100% Offline Promise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    Spacer(Modifier.height(6.dp))
                    Text("All game logic, algorithms, seeded daily challenges, user progress, and rewards run locally on your device. Zero internet required.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                NeonCard {
                    Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
