package com.cartoniq.brainvault.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cartoniq.brainvault.R
import com.cartoniq.brainvault.data.entity.PlayerProfile
import com.cartoniq.brainvault.game.model.GameType
import com.cartoniq.brainvault.ui.screens.*
import com.cartoniq.brainvault.ui.theme.BrainVaultTheme
import com.cartoniq.brainvault.util.Feedback
import com.cartoniq.brainvault.util.LocalFeedback
import com.cartoniq.brainvault.util.LocalReducedMotion
import com.cartoniq.brainvault.util.findActivity
import com.cartoniq.brainvault.viewmodel.MainViewModel

@Composable
fun AppRoot(mainVm: MainViewModel = viewModel()) {
    val profile by mainVm.profile.collectAsStateWithLifecycle()
    val view = LocalView.current
    val feedback = remember { Feedback(view) }
    val p = profile
    BrainVaultTheme(p?.theme ?: 0) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (p == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else {
                feedback.sound = p.sound
                feedback.haptics = p.haptics
                CompositionLocalProvider(LocalFeedback provides feedback, LocalReducedMotion provides p.reducedMotion) {
                    AppNav(mainVm, p)
                }
            }
        }
    }
}

@Composable
private fun AppNav(vm: MainViewModel, profile: PlayerProfile, nav: NavHostController = rememberNavController()) {
    val ctx = LocalContext.current
    val daily by vm.daily.collectAsStateWithLifecycle()
    val bests by vm.bests.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshDate() }

    val watchAd: () -> Unit = {
        val act = ctx.findActivity()
        if (act == null) Toast.makeText(ctx, R.string.ad_unavailable, Toast.LENGTH_LONG).show()
        else vm.adManager.showRewarded(
            act,
            onReward = {
                vm.grantAdReward()
                Toast.makeText(ctx, ctx.getString(R.string.ad_reward_granted, com.cartoniq.brainvault.game.model.GameConfig.AD_REWARD), Toast.LENGTH_SHORT).show()
            },
            onUnavailable = { Toast.makeText(ctx, R.string.ad_unavailable, Toast.LENGTH_LONG).show() },
        )
    }
    val back: () -> Unit = { nav.popBackStack() }
    val dailyArg = listOf(navArgument("daily") { type = NavType.BoolType })

    NavHost(nav, startDestination = "home") {
        composable("home") {
            val memNext = minOf(com.cartoniq.brainvault.game.model.GameConfig.MEMORY_MAX_LEVEL, (bests.firstOrNull { it.gameType == GameType.MEMORY }?.bestMetric ?: 0) + 1)
            HomeScreen(profile, daily, bests, memNext, onNav = { nav.navigate(it) }, onWatchAd = watchAd)
        }
        composable("memory/{daily}", dailyArg) { e ->
            MemoryScreen(
                daily = if (e.arguments?.getBoolean("daily") == true) daily.def else null,
                coins = profile.coins,
                cardSkinId = profile.cardSkin,
                onExit = back,
                onWatchAd = watchAd,
            )
        }
        composable("math/{daily}", dailyArg) { e ->
            MathScreen(
                daily = if (e.arguments?.getBoolean("daily") == true) daily.def else null,
                coins = profile.coins,
                onExit = back,
                onWatchAd = watchAd,
            )
        }
        composable("pattern/{daily}", dailyArg) { e ->
            PatternScreen(
                daily = if (e.arguments?.getBoolean("daily") == true) daily.def else null,
                coins = profile.coins,
                onExit = back,
                onWatchAd = watchAd,
            )
        }
        composable("stats") { StatsScreen(profile, sessions, bests, unlocked.size, back) }
        composable("achievements") { AchievementsScreen(unlocked, back) }
        composable("shop") {
            ShopScreen(
                profile = profile,
                onBack = back,
                onBuy = { skinId, cost ->
                    vm.unlockSkin(skinId, cost) { success ->
                        if (success) Toast.makeText(ctx, ctx.getString(R.string.skin_unlocked_toast, skinId), Toast.LENGTH_SHORT).show()
                        else Toast.makeText(ctx, R.string.skin_not_enough_coins, Toast.LENGTH_SHORT).show()
                    }
                },
                onSelect = { skinId -> vm.selectSkin(skinId) },
            )
        }
        composable("settings") {
            SettingsScreen(profile, back, { nav.navigate("about") }, vm::setSound, vm::setHaptics, vm::setReducedMotion, vm::setTheme, vm::resetAll)
        }
        composable("about") { AboutScreen(back) }
    }
}
