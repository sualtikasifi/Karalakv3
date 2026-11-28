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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Text
import com.sualtikasifi.cizimhafiza.presentation.common.pressable
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedStyle
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.FitText
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.sualtikasifi.cizimhafiza.presentation.common.ButtonLettering
import com.sualtikasifi.cizimhafiza.presentation.common.NoticeCard
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPress
import com.sualtikasifi.cizimhafiza.presentation.common.PressArt
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import com.sualtikasifi.cizimhafiza.presentation.common.softShadow
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
    StakeBlock.CHILD_MODE -> R.string.stake_block_child
    StakeBlock.NOT_LINKED -> R.string.stake_block_link
    StakeBlock.LEVEL -> R.string.stake_block_level
    StakeBlock.ACCOUNT_AGE -> R.string.stake_block_age
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
     * Opens the gold duel's window. Whatever stops a wager says so there (a child, a short account, a friend under level 10,
     * today's limits ...): the plain duel has a button of its own now, so nothing is skipped silently.
     */
    fun start(friendUid: String, wordCount: Int) {
        when {
            legal.childMode.value -> { _picker.value = StakePickerState(wordCount, loading = false, block = StakeBlock.CHILD_MODE); return }
            !StakeRules.roundFits(wordCount) || friendUid.isEmpty() -> { _picker.value = StakePickerState(wordCount, loading = false, block = StakeBlock.REFUSED); return }
        }
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

/**
 * The gold-duel window: one painted picture (stake_window, 910 x 1463 px: the dog and the cat facing off under a purple
 * ribbon, a cream bar for the rules, a purple bar for today's allowance, four rows with a green "win" pill and a pink
 * "lose" pill each, and the orange way out). Every word on it is lettered live in the picture's own proportions; the
 * rows and the orange button are the buttons. A message (checking, not allowed) takes the rows' place on a cream panel.
 */
@Composable
fun StakePickerDialog(state: StakePickerState, friendName: String, gold: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            Modifier.fillMaxWidth(0.94f).widthIn(max = 400.dp).aspectRatio(StakeW / StakeH)
        ) {
            val w = maxWidth
            val h = maxHeight
            val usage = state.usage
            // the amount chosen: the smallest one that can be played, until another row is touched
            val firstOk = if (state.block == null && !state.loading && usage != null)
                StakeRules.STAKES.firstOrNull { gold >= it && usage.canStake(it) } ?: 0 else 0
            val chosen = androidx.compose.runtime.remember(firstOk) { androidx.compose.runtime.mutableIntStateOf(firstOk) }
            Image(
                painter = painterResource(R.drawable.stake_window), contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds, modifier = Modifier.fillMaxSize()
            )
            LetteredText(
                text = stringResource(R.string.stake_picker_title), size = 28.sp, fill = Color.White, outline = Color(0xFF3E0B66),
                title = true, minScale = 0.5f,
                modifier = Modifier.offset(w * 0.258f, h * 0.101f).size(w * 0.489f, h * 0.058f)
            )
            // the red cross, drawn into the picture
            PaintedPress(R.drawable.stake_window, 0.88681f, 0.12509f, 0.10989f, 0.06835f, w, h, corner = 40.dp,
                art = PressArt(R.drawable.stake_press_5_cut, R.drawable.stake_press_5_patch, 0.88352f, 0.12303f, 0.11648f, 0.07314f), onClick = onDismiss)
            // the rules, and the one thing the house keeps
            FitText(
                text = stringResource(R.string.stake_picker_sub, friendName),
                style = PaintedStyle(color = Ink, fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                maxLines = 2, minScale = 0.7f,
                modifier = Modifier.offset(w * 0.165f, h * 0.336f).size(w * 0.67f, h * 0.052f)
            )
            FitText(
                text = stringResource(R.string.stake_burn_note),
                style = PaintedStyle(color = InkSoft, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                minScale = 0.7f,
                modifier = Modifier.offset(w * 0.275f, h * 0.391f).size(w * 0.445f, h * 0.028f)
            )
            if (state.block != null || state.loading || usage == null) {
                val panel = Modifier.offset(w * 0.03f, h * 0.417f).size(w * 0.94f, h * 0.378f)
                if (state.block != null) {
                    NoticeCard(stringResource(R.string.stake_block_title), stringResource(stakeMessageRes(state.block)), panel)
                } else {
                    NoticeCard(
                        stringResource(R.string.gift_loading), "", panel,
                        icon = { androidx.compose.material3.CircularProgressIndicator(color = Color.White, strokeWidth = 3.5.dp, modifier = Modifier.size(34.dp)) }
                    )
                }
            } else {
                // the purple bar: what is left today
                FitText(
                    text = stringResource(R.string.stake_left_pill, StakeRules.MAX_DUELS_PER_DAY - usage.count, StakeRules.MAX_STAKED_PER_DAY - usage.gold),
                    style = PaintedStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start),
                    minScale = 0.65f, contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.offset(w * 0.321f, h * 0.434f).size(w * 0.553f, h * 0.034f)
                )
                StakeRules.STAKES.forEachIndexed { i, stake ->
                    StakeRow(stake, gold >= stake && usage.canStake(stake), stake == chosen.intValue, { chosen.intValue = it }, w, h, i.coerceAtMost(3))
                }
            }
            // the start button: the picture's own orange, caving in with its label; with nothing to start it just closes
            val canStart = chosen.intValue > 0
            PaintedPress(R.drawable.stake_window, 0.21648f, 0.79699f, 0.56044f, 0.08749f, w, h, corner = 24.dp,
                art = PressArt(R.drawable.stake_press_4_cut, R.drawable.stake_press_4_patch, 0.21319f, 0.79494f, 0.56813f, 0.09228f),
                onClick = { if (canStart) onPick(chosen.intValue) else onDismiss() }) {
                ButtonLettering(
                    text = stringResource(if (canStart) R.string.stake_start else R.string.stake_ok), sizeSp = 25f, fill = Color.White, outline = Color(0xFF9A3B00),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

private const val StakeW = 910f
private const val StakeH = 1463f

/** The four rows: their painted outline (top, bottom), in picture pixels. */
private val StakeRowOutline = arrayOf(intArrayOf(702, 808), intArrayOf(814, 921), intArrayOf(928, 1035), intArrayOf(1041, 1151))
/** Their cut-outs for the press (coins sticking out included). */
private val StakeRowArt = listOf(
    PressArt(R.drawable.stake_press_0_cut, R.drawable.stake_press_0_patch, 0.04725f, 0.47779f, 0.90000f, 0.07724f),
    PressArt(R.drawable.stake_press_1_cut, R.drawable.stake_press_1_patch, 0.04725f, 0.55434f, 0.90000f, 0.07792f),
    PressArt(R.drawable.stake_press_2_cut, R.drawable.stake_press_2_patch, 0.04725f, 0.63226f, 0.90000f, 0.07792f),
    PressArt(R.drawable.stake_press_3_cut, R.drawable.stake_press_3_patch, 0.04725f, 0.70950f, 0.90000f, 0.07997f)
)

/** One amount to put in: its coins painted at the left, the amount, and the two pills that say what it comes to. */
@Composable
private fun StakeRow(
    stake: Int, enabled: Boolean, selected: Boolean, onSelect: (Int) -> Unit,
    w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, index: Int
) {
    val fill = if (enabled) Color.White else Color(0xFFEDE6D8)
    val (t, b) = StakeRowOutline[index].let { it[0] to it[1] }
    // the row's own outline (x 46..858 of the 910 px picture, corners about a third of its height), so the chosen
    // frame lies exactly on the painted one
    val rowShape = RoundedCornerShape(percent = 34)
    PaintedPress(
        R.drawable.stake_window, 46f / StakeW, t / StakeH, 812f / StakeW, (b - t) / StakeH, w, h, corner = 22.dp, enabled = enabled,
        art = StakeRowArt[index],
        onClick = { onSelect(stake) }
    ) {
        if (selected) {
            // the chosen row: a gold-orange frame on its outline and a tick
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize()
                    .border(3.5.dp, Brush.verticalGradient(listOf(Color(0xFFFFB347), Color(0xFFF47721))), rowShape)
            )
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.TopStart).offset(maxWidth * 0.012f, (-7).dp).size(22.dp)
                    .background(Color(0xFFF47721), androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
            ) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        if (!enabled) androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().clip(rowShape).background(Color(0x66F2E3C8)))
        // the amount between the coins and the pills, then the whole prize on the green pill and the whole loss on the pink one
        ButtonLettering(
            text = "$stake", sizeSp = 27f, fill = fill, outline = if (enabled) Color(0xFF5A2815) else Color(0xFF9C8B78),
            modifier = Modifier.offset(maxWidth * 0.1897f, 0.dp).size(maxWidth * 0.234f, maxHeight)
        )
        ButtonLettering(
            text = "+${StakeRules.winnerPayout(stake)}", sizeSp = 23f, fill = fill,
            outline = if (enabled) Color(0xFF1B6B2A) else Color(0xFF8E8A7A),
            modifier = Modifier.offset(maxWidth * 0.4298f, maxHeight * 0.17f).size(maxWidth * 0.2525f, maxHeight * 0.68f)
        )
        ButtonLettering(
            text = "−$stake", sizeSp = 23f, fill = fill,
            outline = if (enabled) Color(0xFFA11D18) else Color(0xFF8E8A7A),
            modifier = Modifier.offset(maxWidth * 0.6946f, maxHeight * 0.17f).size(maxWidth * 0.2783f, maxHeight * 0.68f)
        )
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
