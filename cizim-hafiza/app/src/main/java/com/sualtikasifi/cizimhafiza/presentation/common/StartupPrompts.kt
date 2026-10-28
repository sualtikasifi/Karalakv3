package com.sualtikasifi.cizimhafiza.presentation.common

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Keeps the opening prompts from piling on top of each other. They come one at a time, in this order:
 *  1. the date of birth and the terms (LegalConsentHost);
 *  2. the ad consent form, where the law asks for one (MainActivity, through UMP);
 *  3. the home screen's own windows, "Yenilikler" first, then the daily-streak rescue, the rating prompt and the
 *     login streak (MainMenuScreen).
 * The notification permission is not part of the opening at all: it is asked on a later, quiet visit to the home screen
 * — one in which none of the above appeared — once nothing else is open (MainMenuScreen).
 */
object StartupPrompts {
    /** The ad consent flow has run its course this launch: answered, not needed, or failed for want of a connection. */
    val consentSettled = MutableStateFlow(false)

    /** The date-of-birth prompt or "Yenilikler" was on screen in this launch, so it is no time to ask for more. */
    @Volatile var openingShownThisLaunch = false
}
