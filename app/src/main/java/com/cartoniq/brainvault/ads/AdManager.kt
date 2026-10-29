package com.cartoniq.brainvault.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import com.cartoniq.brainvault.game.model.GameConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Optional ads. Every call is wrapped so an SDK failure can never block gameplay.
 * Google's official TEST ad unit IDs are used; replace them before release.
 */
class AdManager(private val app: Application) {
    companion object {
        const val BANNER_ID = "ca-app-pub-9771662959477951/9885364269"
        const val INTERSTITIAL_ID = "ca-app-pub-9771662959477951/1091258720"
        const val REWARDED_ID = "ca-app-pub-9771662959477951/9937335157"
    }

    private var ready = false
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var roundsSinceAd = 0
    private var lastInterstitialAt = 0L
    private var rewardedLoading = false
    private var interstitialLoading = false

    fun init() {
        try {
            MobileAds.initialize(app) { ready = true; loadRewarded(); loadInterstitial() }
        } catch (_: Throwable) { ready = false }
    }

    val bannerReady get() = ready

    private fun loadRewarded() {
        if (!ready || rewarded != null || rewardedLoading) return
        rewardedLoading = true
        try {
            RewardedAd.load(app, REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedLoading = false }
                override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null; rewardedLoading = false }
            })
        } catch (_: Throwable) { rewardedLoading = false }
    }

    private fun loadInterstitial() {
        if (!ready || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        try {
            InterstitialAd.load(app, INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; interstitialLoading = false }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null; interstitialLoading = false }
            })
        } catch (_: Throwable) { interstitialLoading = false }
    }

    /** [onReward] runs only after the SDK confirms the reward was earned. Otherwise [onUnavailable] or nothing. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded
        if (ad == null) { loadRewarded(); onUnavailable(); return }
        rewarded = null
        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { loadRewarded() }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { loadRewarded() }
            }
            ad.show(activity) { onReward() }
        } catch (_: Throwable) { loadRewarded(); onUnavailable() }
    }

    /** Call only from a results screen. Conservative cap: every N rounds plus a cooldown. */
    fun onRoundCompleted(activity: Activity) {
        roundsSinceAd++
        val now = SystemClock.elapsedRealtime()
        if (roundsSinceAd < GameConfig.INTERSTITIAL_EVERY_ROUNDS) return
        if (lastInterstitialAt != 0L && now - lastInterstitialAt < GameConfig.INTERSTITIAL_COOLDOWN_MS) return
        val ad = interstitial ?: run { loadInterstitial(); return }
        interstitial = null
        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { loadInterstitial() }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { loadInterstitial() }
            }
            ad.show(activity)
            roundsSinceAd = 0
            lastInterstitialAt = now
        } catch (_: Throwable) { loadInterstitial() }
    }
}
