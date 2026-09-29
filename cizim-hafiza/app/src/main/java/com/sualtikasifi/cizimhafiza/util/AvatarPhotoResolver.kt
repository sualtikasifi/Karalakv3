package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The picture this player wears inside their avatar frame, as the string that is published to
 * other players: the Google account photo URL, or "" for the Karalak mascot.
 *
 * A Google photo is used when the account has one and the player has not picked the mascot
 * (see [SettingsRepository.avatarSource]). Publishing it means other players in the league and in
 * rooms see the same picture; it is the same URL the account already exposes publicly.
 */
@Singleton
class AvatarPhotoResolver @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) {
    val url: Flow<String> = combine(authRepository.authState, settingsRepository.avatarSource) { auth, source ->
        resolve(auth, source)
    }

    fun currentUrl(): String = resolve(authRepository.authState.value, settingsRepository.avatarSource.value)

    private fun resolve(auth: AuthState, source: String): String {
        if (source == "DINO") return ""
        return (auth as? AuthState.Linked)?.photoUrl?.takeIf { it.isNotBlank() }.orEmpty()
    }
}
