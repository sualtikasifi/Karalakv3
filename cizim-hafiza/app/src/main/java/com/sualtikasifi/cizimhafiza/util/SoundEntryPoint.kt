package com.sualtikasifi.cizimhafiza.util

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets a composable that has no ViewModel (the chest celebration dialogs) reach the app's [SoundManager]. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SoundEntryPoint {
    fun soundManager(): SoundManager
}
