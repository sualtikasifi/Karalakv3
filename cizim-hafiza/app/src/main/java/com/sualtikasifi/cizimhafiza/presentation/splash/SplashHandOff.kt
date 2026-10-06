package com.sualtikasifi.cizimhafiza.presentation.splash

import android.graphics.RectF
import androidx.annotation.MainThread
import androidx.compose.runtime.mutableStateOf

/**
 * The hand-over from the system splash to [BrandSplash].
 *
 * Android 12+ always opens an app on its own splash: a plain colour with one icon in the middle, nothing more (no full
 * picture can go there). So that is where the round logo sits, and [BrandSplash] starts from exactly that picture — same
 * colour, same logo, same place — and turns it into the painted scene: the logo grows into the scene's own mascot while
 * the scene fades in around it.
 *
 * For that to be seamless the system splash must stay up until [BrandSplash] has drawn its first (identical) frame, and
 * [BrandSplash] must know where the platform actually drew the logo (OEM skins do not all use the same size). This
 * object carries both across, on the main thread.
 */
internal object SplashHandOff {
    /** The system splash's icon view, in window pixels, when the platform exposed it; null = use the theme's size. */
    val iconBounds = mutableStateOf<RectF?>(null)

    /** True once the system splash is off the screen — [BrandSplash] starts its morph only then. */
    val systemSplashGone = mutableStateOf(false)

    private var pendingRemove: (() -> Unit)? = null
    private var brandSplashDrawn = false

    /** A fresh cold start: forget whatever an earlier Activity in this process left here. */
    @MainThread
    fun reset() {
        iconBounds.value = null
        systemSplashGone.value = false
        pendingRemove = null
        brandSplashDrawn = false
    }

    /** The system splash is ready to go: [remove] it now if [BrandSplash] is already drawn, else as soon as it is. */
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
