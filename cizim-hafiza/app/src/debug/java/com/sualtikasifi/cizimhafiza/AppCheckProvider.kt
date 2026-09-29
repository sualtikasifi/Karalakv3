package com.sualtikasifi.cizimhafiza

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug builds attest with the debug provider (its token is registered once in the Firebase console). */
internal object AppCheckProvider {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
