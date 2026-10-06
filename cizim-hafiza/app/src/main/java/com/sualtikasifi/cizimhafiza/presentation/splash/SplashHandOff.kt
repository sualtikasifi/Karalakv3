package com.sualtikasifi.cizimhafiza.presentation.splash

import androidx.annotation.MainThread
import androidx.compose.runtime.mutableStateOf

/**
 * The hand-over from the system splash to [BrandSplash].
 *
 * Android 12+ always opens an app on its own splash: one colour and one icon, nothing more (it cannot be turned off and
 * cannot hold a picture). Ours is held on screen until [BrandSplash] has drawn its first frame underneath it, then
 * slides up off it (MainActivity), so the painted scene is revealed rather than cut to. This object carries that
 * hand-shake across, on the main thread.
 */
internal object SplashHandOff {
    /** True once the system splash starts leaving — [BrandSplash] starts its timeline then. */
    val systemSplashGone = mutableStateOf(false)

    private var pendingRemove: (() -> Unit)? = null
    private var brandSplashDrawn = false

    /** A fresh cold start: forget whatever an earlier Activity in this process left here. */
    @MainThread
    fun reset() {
        systemSplashGone.value = false
        pendingRemove = null
        brandSplashDrawn = false
    }

    /** The system splash is ready to go: send it off ([remove]) now if [BrandSplash] is already drawn, else once it is. */
    @MainThread
    fun hold(remove: () -> Unit) {
        if (brandSplashDrawn) {
            remove()
            systemSplashGone.value = true
        } else {
            pendingRemove = remove
        }
    }

    /** [BrandSplash]'s first frame is on screen (or a safety timeout fired): let the system splash go. */
    @MainThread
    fun brandSplashDrawn() {
        brandSplashDrawn = true
        pendingRemove?.let {
            pendingRemove = null
            it()
            systemSplashGone.value = true
        }
    }
}
