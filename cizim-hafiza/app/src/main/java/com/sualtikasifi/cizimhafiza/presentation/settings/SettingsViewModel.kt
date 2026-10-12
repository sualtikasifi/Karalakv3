package com.sualtikasifi.cizimhafiza.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.data.local.WordPoolSynchronizer
import com.sualtikasifi.cizimhafiza.domain.model.SupportedLanguage
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.model.RedeemResult
import com.sualtikasifi.cizimhafiza.domain.repository.CouponRepository
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wordPoolSynchronizer: WordPoolSynchronizer,
    private val couponRepository: CouponRepository,
    authRepository: AuthRepository
) : ViewModel() {

    // --- Kupon kodu ---

    private val _coupon = MutableStateFlow(CouponUiState())
    val coupon: StateFlow<CouponUiState> = _coupon.asStateFlow()

    private var couponFailures = 0
    private var couponLockedUntilMillis = 0L

    fun setCouponCode(text: String) {
        _coupon.update {
            it.copy(code = text.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(CouponCodes.MAX_LENGTH), message = null)
        }
    }

    /**
     * Uses the typed code. After five misses in a row the box rests for half a minute, so the field cannot be used
     * to guess codes one after another.
     */
    fun redeemCoupon() {
        val state = _coupon.value
        if (state.busy || state.code.isBlank()) return
        val now = System.currentTimeMillis()
        if (now < couponLockedUntilMillis) {
            _coupon.update { it.copy(message = CouponMessage.Error(R.string.coupon_error_wait)) }
            return
        }
        _coupon.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val result = couponRepository.redeem(state.code)
            val message = when (result) {
                is RedeemResult.Success -> {
                    couponFailures = 0
                    val coupon = result.coupon
                    if (coupon.gold > 0) settingsRepository.earnGold(coupon.gold)
                    if (coupon.xp > 0) settingsRepository.addXp(coupon.xp)
                    // Chests are not put away: each is opened right now, one opening animation after another.
                    val opened = coupon.chests.flatMap { (tier, n) -> List(n) { settingsRepository.openChestNow(tier) } }
                    if (opened.isNotEmpty()) _coupon.update { it.copy(chestRewards = it.chestRewards + opened) }
                    CouponMessage.Ok(coupon.gold, coupon.xp, opened.size)
                }
                RedeemResult.Invalid, RedeemResult.NotFound -> miss(R.string.coupon_error_not_found)
                RedeemResult.Expired -> miss(R.string.coupon_error_expired)
                RedeemResult.Exhausted -> miss(R.string.coupon_error_exhausted)
                RedeemResult.AlreadyUsed -> CouponMessage.Error(R.string.coupon_error_used)
                RedeemResult.NetworkError -> CouponMessage.Error(R.string.coupon_error_network)
            }
            _coupon.update {
                it.copy(busy = false, message = message, code = if (message is CouponMessage.Ok) "" else it.code)
            }
        }
    }

    /** The chest on screen has finished opening: the next one (if the code gave several) comes up. */
    fun consumeCouponChest() = _coupon.update { it.copy(chestRewards = it.chestRewards.drop(1)) }

    private fun miss(@androidx.annotation.StringRes res: Int): CouponMessage {
        couponFailures++
        if (couponFailures >= 5) {
            couponFailures = 0
            couponLockedUntilMillis = System.currentTimeMillis() + 30_000L
        }
        return CouponMessage.Error(res)
    }

    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabled

    // A small nudge on the "Hesap" row rather than an interruption: a player
    // without a linked Google account loses everything on a lost or reset phone.
    // Disappears the moment they link, since the condition is just "still anonymous".
    val showAccountNudge: StateFlow<Boolean> = authRepository.authState
        .map { it !is AuthState.Linked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)
    /** Whether a Google account is linked; the account row keeps a travelling light until it is. */
    val accountLinked: StateFlow<Boolean> = authRepository.authState
        .map { it is AuthState.Linked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = true)
    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabled
    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled

    // AppCompatDelegate.getApplicationLocales() is empty until the user has
    // manually picked a language once — absent that, Locale.getDefault()
    // mirrors whatever the system resolved this process to (the same JVM
    // default DailyChallengeShareUtil and LeaguePeriod.monthYearLabel read).
    // Either way the raw code is run through SupportedLanguage.resolve
    // rather than trusted directly: a system language this app does not
    // ship (Hindi, say) has to resolve to English here too, matching what
    // res/values/strings.xml (no locale qualifier — now English, not
    // Turkish) is actually showing that player, rather than defaulting
    // this label to a language the screen was never displaying.
    private val _language = MutableStateFlow(
        SupportedLanguage.resolve(
            AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
        ).code
    )
    val language: StateFlow<String> = _language.asStateFlow()

    /** The master switch ("Tüm Sesler"): turning it off silences music and vibration too, turning it on brings both back. */
    fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
        settingsRepository.setMusicEnabled(enabled)
        settingsRepository.setVibrationEnabled(enabled)
    }
    /** Turning vibration on also turns the master switch on: a lit "Titreşim" under a dark "Tüm Sesler" makes no sense. */
    fun setVibrationEnabled(enabled: Boolean) {
        settingsRepository.setVibrationEnabled(enabled)
        if (enabled) settingsRepository.setSoundEnabled(true)
    }
    val musicEnabled: StateFlow<Boolean> = settingsRepository.musicEnabled

    /** Turning music on also turns the master switch on, for the same reason as [setVibrationEnabled]. */
    fun setMusicEnabled(enabled: Boolean) {
        settingsRepository.setMusicEnabled(enabled)
        if (enabled) settingsRepository.setSoundEnabled(true)
    }

    fun setNotificationsEnabled(enabled: Boolean) = settingsRepository.setNotificationsEnabled(enabled)

    fun setLanguage(languageTag: String) {
        if (_language.value == languageTag) return
        _language.value = languageTag
        // Triggers an Activity recreation (AppCompatActivity applies the new
        // locale immediately) — the word pool resync below runs on the
        // synchronizer's own CoroutineScope so it isn't cancelled by that.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        wordPoolSynchronizer.syncAsync()
    }

}

data class CouponUiState(
    val code: String = "",
    val busy: Boolean = false,
    val message: CouponMessage? = null,
    /** Chests a code just gave, waiting to be opened in turn. */
    val chestRewards: List<com.sualtikasifi.cizimhafiza.domain.model.ChestReward> = emptyList()
)

sealed interface CouponMessage {
    data class Ok(val gold: Int, val xp: Int, val chests: Int) : CouponMessage
    data class Error(@androidx.annotation.StringRes val res: Int) : CouponMessage
}
