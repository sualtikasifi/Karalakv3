package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * The passcode in front of Bot Eğitim (see BotTrainingGate).
 *
 * Deliberately not a security control, and never treated as one: this is a
 * constant in an APK anyone can unzip. It stops accidents and casual poking,
 * which is the entire threat — the screen behind it writes shared data every
 * player's opponent is built from, and it should not be wandered into. The
 * people meant to get in (a handful of friends helping train it) are told
 * the number directly.
 */
val DEVELOPER_ACCESS_CODE: String = com.sualtikasifi.cizimhafiza.BuildConfig.DEVELOPER_ACCESS_CODE

/**
 * The passcode in front of the report inbox (see DrawingReportsGate) — a
 * separate code from [DEVELOPER_ACCESS_CODE] on purpose. That one is handed
 * to several friends for a temporary training task; this one guards other
 * players' drawings and reported uids and is known to nobody but whoever
 * maintains the game, so it stays its own number even after Bot Eğitim's
 * code has been shared around and forgotten.
 */
val REPORTS_ACCESS_CODE: String = com.sualtikasifi.cizimhafiza.BuildConfig.REPORTS_ACCESS_CODE

/**
 * The longest passcode either gate's text field will accept. Fixed on purpose: the fields used to
 * stop at the length of the code baked into the build, so a build made with a shorter code than the
 * one the maintainer types (a stale local.properties or CI secret) capped the box at that shorter
 * length and made the real code impossible to enter.
 */
const val MAX_CODE_INPUT_LENGTH = 16

/**
 * How many taps on the version line in Settings reveal the report inbox's
 * passcode gate.
 *
 * Deliberately NOT 7: that count is the exact, widely-known convention for
 * Android's own hidden "Developer options" screen, which means a fair
 * number of curious or technical players will try tapping any version line
 * seven times purely out of habit. Landing on a passcode prompt on the
 * first try would all but confirm something is hidden here; requiring more
 * taps than the famous shortcut means that reflex fails silently instead.
 */
const val DEVELOPER_REVEAL_TAPS = 15

/**
 * Who may open the developer door at all: only the maintainer's own verified Google account
 * ([com.sualtikasifi.cizimhafiza.domain.model.Moderation.REVIEWER_EMAIL]), in a store build. Nobody else gets as far as the passcode
 * screen: the version-line taps do nothing for any other account (or for a phone signed in to none), and the passcode screen and the
 * tools behind it close themselves if they are ever reached any other way, or if the account is signed out while they are open.
 * Debug builds (never published) stay open so the previews work.
 *
 * Client-side only, as the passcodes are: the real lock on the data is the Firestore rules' own reviewer() check on the same account.
 */
object DeveloperGate {
    fun allows(email: String?, emailVerified: Boolean, debugBuild: Boolean): Boolean =
        debugBuild || (email == com.sualtikasifi.cizimhafiza.domain.model.Moderation.REVIEWER_EMAIL && emailVerified)
}

/** [DeveloperGate] for the account signed in on this phone right now. */
fun developerAllowed(): Boolean {
    if (com.sualtikasifi.cizimhafiza.BuildConfig.DEBUG) return true
    return runCatching {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        DeveloperGate.allows(user?.email, user?.isEmailVerified == true, debugBuild = false)
    }.getOrDefault(false)
}

/** [developerAllowed], kept up to date: it turns false the moment the account is signed out or switched while a developer screen is open. */
@androidx.compose.runtime.Composable
fun rememberDeveloperAllowed(): Boolean {
    var allowed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(developerAllowed()) }
    if (!com.sualtikasifi.cizimhafiza.BuildConfig.DEBUG) {
        androidx.compose.runtime.DisposableEffect(Unit) {
            val auth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }.getOrNull()
            val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { allowed = developerAllowed() }
            auth?.addAuthStateListener(listener)
            onDispose { auth?.removeAuthStateListener(listener) }
        }
    }
    return allowed
}
