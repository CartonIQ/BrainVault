package com.cartoniq.brainvault.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/** Sound + haptics, both user-toggleable. Never throws. */
class Feedback(private val view: View) {
    var sound = true
    var haptics = true
    private val tone: ToneGenerator? by lazy {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 45) } catch (t: Throwable) { null }
    }

    private fun buzz(kind: Int) { if (haptics) try { view.performHapticFeedback(kind) } catch (_: Throwable) {} }
    private fun beep(t: Int, ms: Int) { if (sound) try { tone?.startTone(t, ms) } catch (_: Throwable) {} }

    fun tap() { buzz(HapticFeedbackConstants.VIRTUAL_KEY); beep(ToneGenerator.TONE_PROP_BEEP, 40) }
    fun good() { buzz(HapticFeedbackConstants.CONFIRM.takeIf { android.os.Build.VERSION.SDK_INT >= 30 } ?: HapticFeedbackConstants.KEYBOARD_TAP); beep(ToneGenerator.TONE_PROP_ACK, 120) }
    fun bad() { buzz(HapticFeedbackConstants.LONG_PRESS); beep(ToneGenerator.TONE_PROP_NACK, 160) }
}

val LocalFeedback = compositionLocalOf<Feedback?> { null }
val LocalReducedMotion = compositionLocalOf { false }

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun formatTime(ms: Long): String {
    val s = (ms / 1000).toInt()
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}
