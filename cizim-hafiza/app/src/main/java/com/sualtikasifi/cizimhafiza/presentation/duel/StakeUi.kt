package com.sualtikasifi.cizimhafiza.presentation.duel

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DuelStatus
import com.sualtikasifi.cizimhafiza.domain.model.StakeBlock
import com.sualtikasifi.cizimhafiza.domain.model.StakeRules
import com.sualtikasifi.cizimhafiza.domain.model.StakeUsage
import com.sualtikasifi.cizimhafiza.domain.repository.DuelRepository
import com.sualtikasifi.cizimhafiza.domain.repository.StakeSettlement
import com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.util.LegalConsentStore
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Ink = Color(0xFF3B2314)
private val InkSoft = Color(0xFF7A5A40)

@StringRes
fun stakeMessageRes(block: StakeBlock): Int = when (block) {
    StakeBlock.CHILD_MODE -> R.string.gift_block_child
    StakeBlock.NOT_LINKED -> R.string.gift_block_link
    StakeBlock.LEVEL -> R.string.stake_block_level
    StakeBlock.ACCOUNT_AGE -> R.string.gift_block_age
    StakeBlock.FRIENDSHIP_AGE -> R.string.gift_block_friendship
    StakeBlock.FRIEND_NOT_READY -> R.string.stake_block_friend
    StakeBlock.FRIEND_LEVEL -> R.string.stake_block_friend_level
    StakeBlock.BANNED -> R.string.stake_block_banned
    StakeBlock.DISABLED -> R.string.stake_block_disabled
    StakeBlock.OFFLINE -> R.string.gift_block_offline
    StakeBlock.NO_GOLD -> R.string.gift_block_gold
    StakeBlock.LIMIT -> R.string.stake_block_limit
    StakeBlock.TOO_LATE -> R.string.stake_block_late
    StakeBlock.REFUSED -> R.string.stake_block_refused
}

data class StakePickerState(val wordCount: Int, val loading: Boolean = true, val usage: StakeUsage? = null, val block: StakeBlock? = null)

/** The "Altın bahsi ekle?" step of the challenge page. Skips itself whenever a wager cannot be offered. */
@HiltViewModel
class StakeViewModel @Inject constructor(
    private val repository: DuelRepository,
    private val settings: SettingsRepository,
    private val legal: LegalConsentStore,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _picker = MutableStateFlow<StakePickerState?>(null)
    val picker: StateFlow<StakePickerState?> = _picker.asStateFlow()
    val gold get() = settings.goldBalance

    /**
     * Opens the picker. A wager that cannot be offered says why (so a player testing it is never left guessing) and
     * offers the plain duel; only a child, or a round too long for the ten minutes, goes straight on without a word.
     */
    fun start(friendUid: String, wordCount: Int, withoutStake: () -> Unit) {
        if (legal.childMode.value || !StakeRules.roundFits(wordCount) || friendUid.isEmpty()) { withoutStake(); return }
        val created = auth.currentUser?.metadata?.creationTimestamp ?: 0L
        val mine = when {
            auth.currentUser?.isAnonymous != false -> StakeBlock.NOT_LINKED
            settings.playerLevelNow() < StakeRules.MIN_LEVEL -> StakeBlock.LEVEL
            created > 0 && System.currentTimeMillis() - created < StakeRules.MIN_CHALLENGER_ACCOUNT_DAYS * 86_400_000L -> StakeBlock.ACCOUNT_AGE
            settings.goldBalance.value < StakeRules.STAKES.first() -> StakeBlock.NO_GOLD
            else -> null
        }
        if (mine != null) { _picker.value = StakePickerState(wordCount, loading = false, block = mine); return }
        _picker.value = StakePickerState(wordCount)
        viewModelScope.launch {
            val friend = repository.friendStakeBlock(friendUid)
            val usage = repository.stakeUsage(friendUid).getOrNull()
            _picker.value = when {
                friend != null -> StakePickerState(wordCount, loading = false, block = friend)
                usage == null -> StakePickerState(wordCount, loading = false, block = StakeBlock.OFFLINE)
                else -> StakePickerState(wordCount, loading = false, usage = usage)
            }
        }
    }

    fun close() { _picker.value = null }
}

@Composable
fun StakePickerDialog(state: StakePickerState, friendName: String, gold: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AppWindowDialog(title = stringResource(R.string.stake_picker_title), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.stake_picker_sub, friendName), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            val usage = state.usage
            if (state.block != null) {
                Text(stringResource(stakeMessageRes(state.block)), color = Color(0xFFB3401A), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            } else if (state.loading || usage == null) {
                Text(stringResource(R.string.gift_loading), color = InkSoft, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    stringResource(R.string.stake_left, StakeRules.MAX_DUELS_PER_DAY - usage.count, StakeRules.MAX_STAKED_PER_DAY - usage.gold),
                    color = InkSoft, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                StakeRules.STAKES.forEach { stake ->
                    val ok = gold >= stake && usage.canStake(stake)
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFFAEC), Color(0xFFFBE9C4))), shape)
                            .border(2.dp, Color(0xFFE2C28A), shape)
                            .clickable(enabled = ok) { onPick(stake) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.gift_gold_amount, stake), color = if (ok) Ink else Color(0xFFB0A090), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(stringResource(R.string.stake_win_hint, StakeRules.netWin(stake), stake), color = InkSoft, fontSize = 12.sp, maxLines = 1)
                        }
                        Image(painterResource(R.drawable.icon_gold_coin), null, Modifier.size(24.dp))
                    }
                }
            }
            PaintedPillButton(
                stringResource(R.string.stake_none), { onPick(0) }, primary = false, height = 46.dp, textSize = 17.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

/** What each settled wager did to the wallet. */
@Composable
fun StakeResultDialog(results: List<StakeSettlement>, onClose: () -> Unit) {
    AppWindowDialog(
        title = stringResource(R.string.stake_result_title), onDismiss = onClose, centerTitle = true,
        footer = {
            PaintedPillButton(stringResource(R.string.close), onClose, height = 50.dp, textSize = 21.sp, attention = true,
                modifier = Modifier.fillMaxWidth(0.62f).padding(top = 14.dp, bottom = 8.dp).align(Alignment.CenterHorizontally))
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            results.take(8).forEach { r ->
                val line = when {
                    r.status == DuelStatus.DECLINED || r.status == DuelStatus.EXPIRED -> stringResource(R.string.stake_settle_refund, r.otherName, r.paid)
                    r.status == DuelStatus.FORFEIT && r.paid > 0 -> stringResource(R.string.stake_settle_forfeit_win, r.otherName, r.paid - r.stake)
                    r.status == DuelStatus.FORFEIT -> stringResource(R.string.stake_settle_forfeit_loss, r.otherName, r.stake)
                    r.paid == r.stake -> stringResource(R.string.stake_settle_tie, r.otherName, r.stake)
                    r.paid > r.stake -> stringResource(R.string.stake_settle_won, r.otherName, r.paid - r.stake)
                    else -> stringResource(R.string.stake_settle_lost, r.otherName, r.stake)
                }
                Text(line, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            if (results.size > 8) Spacer(Modifier.width(1.dp))
        }
    }
}

/** Settles the wagers that came due while the player was away and reports what they paid, once per launch. */
@HiltViewModel
class StakeSettleViewModel @Inject constructor(
    private val repository: DuelRepository,
    private val auth: FirebaseAuth,
    private val legal: LegalConsentStore
) : ViewModel() {
    private val _results = MutableStateFlow<List<StakeSettlement>>(emptyList())
    val results: StateFlow<List<StakeSettlement>> = _results.asStateFlow()
    private var checked = false

    fun check() {
        if (checked || legal.childMode.value || auth.currentUser?.isAnonymous != false) return
        checked = true
        viewModelScope.launch { _results.value = repository.settleWagers() }
    }

    fun dismiss() { _results.value = emptyList() }
}
