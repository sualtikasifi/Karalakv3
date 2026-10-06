package com.sualtikasifi.cizimhafiza.presentation.splash

import androidx.compose.runtime.mutableStateOf

/**
 * Tells [BrandSplash] when the system splash starts leaving.
 *
 * Android 12+ always opens an app on its own splash: one colour and one icon (it cannot be turned off and cannot hold
 * a picture). The moment the app's first frame is drawn it slides up off the window (MainActivity), revealing the
 * painted scene that is already the window's background; [BrandSplash]'s loading bar starts filling from then on.
 */
internal object SplashHandOff {
    /** True once the system splash starts leaving. */
    val systemSplashGone = mutableStateOf(false)

    /** A fresh cold start: forget whatever an earlier Activity in this process left here. */
    fun reset() {
        systemSplashGone.value = false
    }
}
