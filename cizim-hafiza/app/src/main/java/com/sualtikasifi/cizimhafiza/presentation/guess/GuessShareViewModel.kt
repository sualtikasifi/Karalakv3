package com.sualtikasifi.cizimhafiza.presentation.guess

import androidx.lifecycle.ViewModel
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.domain.repository.GuessRepository
import com.sualtikasifi.cizimhafiza.util.GuessCodec
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * What the result screen needs to turn one of the player's drawings into a "Bil Bakalım" link: whether they may (a player
 * who may be under 13 never publishes a drawing) and the call that makes it.
 */
@HiltViewModel
class GuessShareViewModel @Inject constructor(
    private val repository: GuessRepository,
    private val settingsRepository: SettingsRepository,
    legalConsentStore: LegalConsentStore
) : ViewModel() {

    val childMode: StateFlow<Boolean> = legalConsentStore.childMode

    /** [language] is the language of the word ([com.sualtikasifi.cizimhafiza.presentation.common.currentWordLanguage]). */
    suspend fun create(word: String, strokes: List<DrawingStroke>, language: String): Result<String> =
        repository.createLink(
            word = word,
            strokes = strokes,
            seconds = 0,
            lang = GuessCodec.langFor(language),
            ownerName = settingsRepository.nicknameOrDefault
        )
}
