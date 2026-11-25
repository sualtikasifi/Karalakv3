package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.domain.model.AvatarGallery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The picture this player wears inside their avatar frame, as the string that is published to other players: "avatar:<id>" for
 * a gallery picture, or "" for the Karalak mascot (see [AvatarGallery]).
 *
 * Only these pictures exist: there is no photo taken from the player's account or uploaded by them, so this is the same for a
 * player who may be under 13 as for anyone else, and nothing personal is ever published through it.
 */
@Singleton
class AvatarPhotoResolver @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    val url: Flow<String> = settingsRepository.avatarSource.map(::resolve)

    fun currentUrl(): String = resolve(settingsRepository.avatarSource.value)

    private fun resolve(source: String): String = AvatarGallery.entryOfSource(source)?.let { AvatarGallery.published(it) }.orEmpty()
}
