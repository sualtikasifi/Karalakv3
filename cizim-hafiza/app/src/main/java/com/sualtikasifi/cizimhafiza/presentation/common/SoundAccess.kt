package com.sualtikasifi.cizimhafiza.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.sualtikasifi.cizimhafiza.util.SoundEntryPoint
import com.sualtikasifi.cizimhafiza.util.SoundManager
import dagger.hilt.android.EntryPointAccessors

/** The app's [SoundManager], for composables that have no ViewModel to ask. */
@Composable
fun rememberSoundManager(): SoundManager {
    val context = LocalContext.current.applicationContext
    return remember { EntryPointAccessors.fromApplication(context, SoundEntryPoint::class.java).soundManager() }
}
