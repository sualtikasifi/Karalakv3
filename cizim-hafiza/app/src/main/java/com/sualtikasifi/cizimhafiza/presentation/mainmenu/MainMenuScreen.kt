package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.util.DailyChallengeState
import kotlinx.coroutines.delay
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.common.IconWell
import com.sualtikasifi.cizimhafiza.presentation.common.SecondaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.PrimaryButton
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import com.sualtikasifi.cizimhafiza.presentation.common.TintedBadge
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestSlots
import com.sualtikasifi.cizimhafiza.domain.model.Moderation
import com.sualtikasifi.cizimhafiza.domain.model.Penalty
import com.sualtikasifi.cizimhafiza.domain.model.DailyChallenge
import com.sualtikasifi.cizimhafiza.domain.model.LevelProgressState
import com.sualtikasifi.cizimhafiza.domain.model.XpAwards
import com.sualtikasifi.cizimhafiza.domain.model.LevelTier
import com.sualtikasifi.cizimhafiza.domain.model.PlayerLevel
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestsViewModel
import com.sualtikasifi.cizimhafiza.presentation.common.LevelAvatar
import com.sualtikasifi.cizimhafiza.presentation.common.artRes
import com.sualtikasifi.cizimhafiza.presentation.common.labelRes
import com.sualtikasifi.cizimhafiza.presentation.common.screenBackground
import com.sualtikasifi.cizimhafiza.presentation.theme.AppTheme
import com.sualtikasifi.cizimhafiza.util.GameConstants

/** The one gap used between every major section of the menu, so the page reads as evenly spaced top to bottom. */
private val SECTION_GAP = 9.dp

/** The daily challenge card once today's is done — see [DailyChallengeCard]. */
private val DailyDoneGreen = Color(0xFFD9EFDC)

@Composable
fun MainMenuScreen(
    onPlay: () -> Unit,
    onQuickMatch: () -> Unit,
    onPlayOnline: () -> Unit,
    onLevels: () -> Unit,
    onAchievements: () -> Unit,
    onFriends: () -> Unit,
    onSettings: () -> Unit,
    onDailyChallenge: () -> Unit,
    onChests: () -> Unit,
    onStore: () -> Unit,
    onLeague: () -> Unit,
    viewModel: MainMenuViewModel = hiltViewModel()
) {
    val hasUnseenAchievement by viewModel.hasUnseenAchievement.collectAsState()
    val accountNotLinked by viewModel.accountNotLinked.collectAsState()
    val xpEvent by viewModel.xpEvent.collectAsState()
    val adGoldNextAt by viewModel.adGoldNextAtMillis.collectAsState()
    val adChestDay by viewModel.adChestDay.collectAsState()
    val freeChestReward by viewModel.freeChestReward.collectAsState()
    val pendingFriendRequests by viewModel.pendingFriendRequests.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val dailyState by viewModel.dailyState.collectAsState()
    val penaltyWarning by viewModel.penaltyWarning.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val xpFly by com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.active.collectAsState()
    val xpFlyArrived by com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.arrivedId.collectAsState()
    val selectedFrame by viewModel.selectedFrame.collectAsState()
    val selectedPen by viewModel.selectedPen.collectAsState()
    val gold by viewModel.goldBalance.collectAsState()
    val avatarFrameItems by viewModel.avatarFrameItems.collectAsState()
    val avatarPhoto by viewModel.avatarPhoto.collectAsState()
    val avatarSource by viewModel.avatarSource.collectAsState()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsState()
    var framePickerOpen by remember { mutableStateOf(false) }
    var penPickerOpen by remember { mutableStateOf(false) }
    var featureTourOpen by remember { mutableStateOf(!viewModel.featureTourSeen) }
    val nicknameRenameUsed by viewModel.nicknameRenameUsed.collectAsState()
    val penSkinItems by viewModel.penSkinItems.collectAsState()
    var rankLadderOpen by remember { mutableStateOf(false) }
    val streakToast by viewModel.streakToast.collectAsState()
    val referralRewardXp by viewModel.referralRewardXp.collectAsState()
    // The system back gesture on the menu used to close the app outright,
    // with no way to take it back — easy to trigger by accident mid-swipe
    // and, on a game, more destructive than it looks.
    var exitPromptOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = !exitPromptOpen) { exitPromptOpen = true }
    val context = LocalContext.current
    val activity = context as? Activity

    // Midnight while the menu is open: nothing else re-reads the day, so without this the daily
    // challenge and the free chest stayed "done" until the player left and came back.
    var today by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()) }
    LaunchedEffect(today) {
        val now = com.sualtikasifi.cizimhafiza.util.TurkeyTime.now()
        val untilMidnight = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
        delay(untilMidnight + 1_000)
        today = com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().toEpochDay()
        viewModel.refreshDaily()
    }

    // The app can sit in the background across midnight; without this the
    // menu would still be showing "done for today" on a day whose challenge
    // is actually waiting to be played.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDaily()
                viewModel.checkRatingPrompt()
                viewModel.syncAchievements()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A fixed screen, not a scrolling one: the menu is the app's home base,
    // opened dozens of times a session, and every scroll gesture on it is a
    // small tax on getting to "Oyna". Fitting the daily-challenge card and
    // level badge in without scrolling meant trimming sizes throughout
    // rather than letting any one element claim its old, roomier size.
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        com.sualtikasifi.cizimhafiza.presentation.common.CappedFontScale {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenBackground()
                .padding(padding)
        ) {
            HomeProfileBar(
                nickname = nickname,
                progress = com.sualtikasifi.cizimhafiza.presentation.common.XpFlyBus.displayProgress(levelProgress, xpFly, xpFlyArrived),
                frame = selectedFrame,
                photo = avatarPhoto,
                pen = selectedPen,
                gold = gold,
                onFrameClick = { framePickerOpen = true },
                onPenClick = { penPickerOpen = true },
                onRankClick = { rankLadderOpen = true },
                onGoldClick = onStore,
                modifier = Modifier.padding(horizontal = 12.dp).padding(top = 6.dp)
            )
            // Fixed, non-scrolling page: the mode row is the one flexible part and
            // absorbs whatever height is left, so the layout fits any normal phone
            // without moving. Only a genuinely short screen falls back to scrolling.
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val compact = maxHeight < 700.dp
                val fixedEstimate = (if (compact) 470.dp else 520.dp) + (if (xpEvent != null) 60.dp else 0.dp)
                val modeHeight = (maxHeight - fixedEstimate - 18.dp).coerceIn(96.dp, 160.dp)
                val content: @Composable ColumnScope.() -> Unit = {
                    xpEvent?.let { event ->
                        XpEventBanner(event = event)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (GameConstants.ADMOB_ENABLED) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(if (compact) 112.dp else 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AdGoldButton(
                                nextAtMillis = adGoldNextAt,
                                onClick = { activity?.let(viewModel::watchAdForGold) },
                                modifier = Modifier.width(84.dp).fillMaxHeight()
                            )
                            DailyChallengeCardNarrow(
                                state = dailyState,
                                onPlay = onDailyChallenge,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            AdChestButton(
                                availableToday = adChestDay != today,
                                onClick = { activity?.let(viewModel::watchAdForChest) },
                                modifier = Modifier.width(84.dp).fillMaxHeight()
                            )
                        }
                    } else {
                        DailyChallengeCardNarrow(
                            state = dailyState,
                            onPlay = onDailyChallenge,
                            modifier = Modifier.fillMaxWidth().height(if (compact) 112.dp else 120.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(SECTION_GAP + 3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(modeHeight),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GradientModeCard(
                            imageRes = R.drawable.icon_mode_playfriend,
                            label = stringResource(R.string.menu_play_online),
                            top = Color(0xFF45AEF5),
                            bottom = Color(0xFF2181D6),
                            edge = Color(0xFF12569A),
                            onClick = onPlayOnline,
                            modifier = Modifier.weight(1f)
                        )
                        GradientModeCard(
                            imageRes = R.drawable.icon_mode_quickmatch,
                            label = stringResource(R.string.quick_match_title),
                            top = Color(0xFFFF9445),
                            bottom = Color(0xFFF2611B),
                            edge = Color(0xFFB9460F),
                            onClick = onQuickMatch,
                            modifier = Modifier.weight(1f),
                            boost = xpEvent,
                            orbit = true
                        )
                        GradientModeCard(
                            imageRes = R.drawable.icon_mode_offline,
                            label = stringResource(R.string.menu_play),
                            top = Color(0xFF63CC5E),
                            bottom = Color(0xFF35A64B),
                            edge = Color(0xFF217634),
                            onClick = onPlay,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(SECTION_GAP + 3.dp))

                    // The six small tiles stay together as one block, so spare height goes between sections, not between the tiles.
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientTile(
                                icon = Icons.Filled.Map,
                                label = stringResource(R.string.menu_levels),
                                top = Color(0xFF3BCDB6), bottom = Color(0xFF15A08E), edge = Color(0xFF0C6F62),
                                onClick = onLevels, compact = compact, modifier = Modifier.weight(1f)
                            )
                            GradientTile(
                                icon = Icons.Filled.Group,
                                label = stringResource(R.string.menu_friends),
                                top = Color(0xFFFF86B4), bottom = Color(0xFFE64A8B), edge = Color(0xFFA62A62),
                                onClick = onFriends, badgeCount = pendingFriendRequests, compact = compact, modifier = Modifier.weight(1f)
                            )
                            GradientTile(
                                icon = Icons.Filled.EmojiEvents,
                                label = stringResource(R.string.menu_achievements),
                                top = Color(0xFFFFCB47), bottom = Color(0xFFF0A012), edge = Color(0xFFB36F05),
                                onClick = onAchievements, showBadge = hasUnseenAchievement, compact = compact, modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientTile(
                                icon = Icons.Filled.Leaderboard,
                                label = stringResource(R.string.league_title),
                                top = Color(0xFF7C8CFF), bottom = Color(0xFF4353D8), edge = Color(0xFF2A3591),
                                onClick = onLeague, compact = compact, modifier = Modifier.weight(1f)
                            )
                            GradientTile(
                                icon = Icons.Filled.ShoppingBag,
                                label = stringResource(R.string.store_title),
                                top = Color(0xFF8AD65A), bottom = Color(0xFF3FA53A), edge = Color(0xFF276E24),
                                onClick = onStore, compact = compact, modifier = Modifier.weight(1f)
                            )
                            GradientTile(
                                icon = Icons.Filled.Settings,
                                label = stringResource(R.string.menu_settings),
                                top = Color(0xFF8AA0C2), bottom = Color(0xFF5D7599), edge = Color(0xFF3B4E6C),
                                onClick = onSettings, showBadge = accountNotLinked, compact = compact, modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SECTION_GAP + 3.dp))

                    HomeChestsSection(compact = compact)
                }
                FitToHeight(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 10.dp),
                    content = content
                )
            }
        }
        }

        if (exitPromptOpen) {
            AlertDialog(
                onDismissRequest = { exitPromptOpen = false },
                title = { Text(stringResource(R.string.exit_confirm_title)) },
                text = { Text(stringResource(R.string.exit_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = { exitPromptOpen = false; activity?.finish() }) {
                        Text(stringResource(R.string.exit_confirm_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exitPromptOpen = false }) {
                        Text(stringResource(R.string.exit_confirm_no))
                    }
                }
            )
        }

        // The streak the player just lost, offered back for an ad. Shown the
        // moment the menu opens, because that is exactly when they find out
        // it broke — see DailyChallengeRepository.repairStreak.
        // Gated on ads: the rescue IS watching an ad, so with ads off the
        // streak simply breaks rather than opening a dialog whose only
        // button cannot work.
        if (dailyState.rescuableStreak > 0 && GameConstants.ADMOB_ENABLED) {
            StreakRescueDialog(
                lostStreak = dailyState.rescuableStreak,
                onRescue = { activity?.let(viewModel::rescueStreak) },
                onDismiss = viewModel::dismissRescuePrompt
            )
        }

        streakToast?.let { toast ->
            LaunchedEffect(toast) {
                delay(2_500)
                viewModel.consumeStreakToast()
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                TintedBadge(
                    text = stringResource(
                        when (toast) {
                            StreakToast.Rescued -> R.string.streak_rescue_done
                            StreakToast.AdGoldEarned -> R.string.home_ad_gold_earned
                            StreakToast.AdUnavailable -> R.string.home_ad_unavailable
                        }
                    ),
                    container = MaterialTheme.colorScheme.surface,
                    content = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (referralRewardXp > 0) {
            LaunchedEffect(referralRewardXp) {
                delay(3_500)
                viewModel.consumeReferralRewardNotice()
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                TintedBadge(
                    text = stringResource(R.string.referral_reward_earned_format, referralRewardXp),
                    container = MaterialTheme.colorScheme.surface,
                    content = MaterialTheme.colorScheme.primary
                )
            }
        }

        val ratingPrompt by viewModel.ratingPrompt.collectAsState()
        if (ratingPrompt && freeChestReward == null) {
            com.sualtikasifi.cizimhafiza.presentation.common.RatingPromptDialog(
                onRate = viewModel::rateAndClaimBonus,
                onDismiss = viewModel::dismissRatingPrompt
            )
        }

        freeChestReward?.let { reward ->
            com.sualtikasifi.cizimhafiza.presentation.chests.ChestOpeningDialog(reward = reward, onDismiss = viewModel::consumeFreeChestReward)
        }

        if (rankLadderOpen) {
            RankLadderSheet(progress = levelProgress, onDismiss = { rankLadderOpen = false })
        }

        if (featureTourOpen) {
            FeatureTourDialog(onFinish = { viewModel.markFeatureTourSeen(); featureTourOpen = false })
        }

        if (penPickerOpen) {
            PenPickerSheet(
                items = penSkinItems,
                onSelect = { viewModel.selectPenSkin(it); penPickerOpen = false },
                onDismiss = { penPickerOpen = false }
            )
        }


        if (framePickerOpen) {
            AvatarFramePickerSheet(
                items = avatarFrameItems,
                photoSource = avatarSource,
                googlePhotoUrl = googlePhotoUrl,
                onPhotoSource = viewModel::selectAvatarSource,
                onSelect = { viewModel.selectAvatarFrame(it); framePickerOpen = false },
                onDismiss = { framePickerOpen = false }
            )
        }

    }
    penaltyWarning?.let { penalty ->
        PenaltyDialog(penalty = penalty, onDismiss = viewModel::dismissPenaltyWarning)
    }
}

/**
 * What the player is told when a round of theirs was rejected in review.
 *
 * Says the number out loud rather than letting the XP quietly differ from
 * what they remember: a penalty nobody notices deters nobody, and a level
 * that dropped without explanation reads as a bug.
 */
@Composable
private fun PenaltyDialog(penalty: Penalty, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        RaisedCard(corner = 24.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconWell(icon = Icons.Filled.Gavel)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.penalty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.penalty_body, penalty.xpRevoked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                // What happens NEXT is the part that changes behaviour. A
                // penalty that only reports what was taken reads as a fine;
                // saying how close the lockout is turns it into a warning,
                // which is the point.
                Text(
                    text = if (penalty.lockedUntilMillis > 0L) {
                        stringResource(R.string.penalty_locked, penalty.strike)
                    } else {
                        stringResource(
                            R.string.penalty_strikes,
                            penalty.strike,
                            // Distance to the NEXT multiple, not to three: the
                            // count is lifetime and every third offence costs
                            // a day, so offence 4 is two away from a lockout,
                            // not "already past it".
                            Moderation.STRIKES_BEFORE_LOCKOUT -
                                penalty.strike % Moderation.STRIKES_BEFORE_LOCKOUT
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                PrimaryButton(
                    text = stringResource(R.string.penalty_understood),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Either a tintable glyph ([icon], on a colored [IconWell]) or a full-color
 * illustration ([imageRes]) — never both. The illustrated set (achievements,
 * the mode cards) already carries its own color and container shape, so
 * putting it inside another tinted circle would double up on both.
 */

/**
 * A grid of every [AvatarFrame] the player has unlocked so far (plus locked
 * ones ahead, dimmed with the level that opens them), tapping an unlocked
 * one picks it — see MainMenuViewModel.selectAvatarFrame. No level-number
 * face is drawn on the swatches (unlike [LevelAvatar]): this is about
 * choosing the ring, not restating the player's level eleven times over.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AvatarFramePickerSheet(
    items: List<AvatarFrameUiItem>,
    photoSource: String,
    googlePhotoUrl: String?,
    onPhotoSource: (String) -> Unit,
    onSelect: (AvatarFrame) -> Unit,
    onDismiss: () -> Unit
) {
    com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog(title = stringResource(R.string.avatar_frame_picker_title), onDismiss = onDismiss) {
        val googleSelected = googlePhotoUrl != null && photoSource != "DINO"
        Text(
            text = stringResource(R.string.avatar_source_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = Color(0xFF3A2416)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            AvatarSourceOption(
                label = stringResource(R.string.avatar_source_karalak),
                selected = !googleSelected,
                enabled = true,
                photo = com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Dino,
                onClick = { onPhotoSource("DINO") }
            )
            AvatarSourceOption(
                label = stringResource(R.string.avatar_source_google),
                selected = googleSelected,
                enabled = googlePhotoUrl != null,
                photo = googlePhotoUrl?.let { com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto.Url(it) },
                onClick = { onPhotoSource("GOOGLE") }
            )
        }
        if (googlePhotoUrl == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.avatar_source_google_locked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.avatar_frame_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = Color(0xFF3A2416)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                gridItems(items, key = { it.frame.name }) { item ->
                    AvatarFrameSwatch(item = item, onClick = { if (item.unlocked) onSelect(item.frame) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AvatarFrameSwatch(item: AvatarFrameUiItem, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (item.selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.unlocked, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.frame.drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(if (item.unlocked) 1f else 0.35f)
            )
            if (!item.unlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (item.frame.isStoreItem) {
                            stringResource(R.string.store_in_store)
                        } else if (item.frame.isLeagueReward) {
                            stringResource(R.string.cosmetic_locked_league)
                        } else {
                            stringResource(R.string.avatar_frame_locked_level, item.frame.unlockLevel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * The menu's first call to action: today's challenge, or — once it's done —
 * the streak it just extended.
 *
 * Deliberately shows the streak in both states. Before playing it's what's
 * at stake; after playing it's the reward, and seeing it tick up is most of
 * the reason to come back tomorrow.
 */
@Composable
private fun DailyChallengeCard(state: DailyChallengeState, onPlay: () -> Unit, compact: Boolean = false) {
    val available = state.isAvailableToday
    val todayResult = state.todayResult
    val top = if (available) Color(0xFF9B6BF2) else Color(0xFF52C378)
    val bottom = if (available) Color(0xFF6440D6) else Color(0xFF2E9A55)
    val edge = if (available) Color(0xFF3F2699) else Color(0xFF1E6E3B)
    val words = DailyChallenge.WORD_COUNT
    val correct = todayResult?.correctCount ?: 0

    // The button breathes only while there is something to do.
    val transition = rememberInfiniteTransition(label = "dailyPulse")
    val pulse = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dailyPulseFraction"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .chunky(Brush.verticalGradient(listOf(top, bottom)), edge, corner = 22.dp, lift = 4.dp, rim = Color.White.copy(alpha = 0.35f))
            .then(if (available) Modifier.clickable(onClick = onPlay) else Modifier)
            .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // The calendar mark sits straight on the card — no disc behind it.
            Image(
                painter = painterResource(R.drawable.daily_calendar_icon),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 60.dp else 68.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.daily_challenge_title),
                    fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (available) {
                        stringResource(R.string.daily_challenge_ready, words)
                    } else {
                        stringResource(R.string.daily_challenge_done, correct, words)
                    },
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2
                )
            }
            if (state.currentStreak > 0) {
                // The streak: the badge says "this keeps going", the number is what it is worth.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.daily_streak_icon),
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 50.dp else 56.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.daily_challenge_multiplier_badge,
                            XpAwards.dailyStreakMultiplier(state.currentStreak)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (available) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(vertical = if (compact) 5.dp else 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.daily_play_now).uppercase(androidx.compose.ui.text.intl.Locale.current.platformLocale),
                    fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFF5B3FC4)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(vertical = 6.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "✓ " + stringResource(R.string.daily_done_badge),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.daily_challenge_resets_in, midnightCountdownText()),
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

/**
 * "HH:MM:SS" until local midnight, ticking every second. Local time, not
 * UTC: the daily challenge itself resets on [java.time.LocalDate]'s day
 * boundary (see DailyChallenge/DailyChallengeRepository), which is the
 * device's local calendar day — the countdown has to agree with the exact
 * moment the card it's showing will actually flip to "ready" again.
 */
@Composable
private fun midnightCountdownText(): String {
    var remaining by remember {
        mutableStateOf(java.time.Duration.between(com.sualtikasifi.cizimhafiza.util.TurkeyTime.now(), nextMidnight()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            remaining = java.time.Duration.between(com.sualtikasifi.cizimhafiza.util.TurkeyTime.now(), nextMidnight())
        }
    }
    val total = remaining.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun nextMidnight(): java.time.LocalDateTime =
    com.sualtikasifi.cizimhafiza.util.TurkeyTime.today().plusDays(1).atStartOfDay()

/**
 * The one deliberate way back from a broken streak.
 *
 * Deliberately a modal: someone who has just lost a 60-day streak will not
 * go looking for a button, and the offer expires within a couple of days
 * (see DailyChallengeRepository.MAX_RESCUE_GAP_DAYS).
 *
 * The action is a full button carrying a play icon and the word "ad",
 * because it opens a rewarded video. AdMob's policies require the reward
 * and the fact that an ad is coming to be stated before the tap, and a bare
 * line of tappable text stated neither — it did not even look like a
 * control.
 */
@Composable
private fun StreakRescueDialog(lostStreak: Int, onRescue: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        RaisedCard(corner = 28.dp, raise = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🔥", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.streak_rescue_title, lostStreak),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.streak_rescue_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryButton(
                    text = stringResource(R.string.streak_rescue_action),
                    onClick = onRescue,
                    icon = Icons.Filled.PlayCircle,
                    height = 54.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.streak_rescue_dismiss),
                    onClick = onDismiss,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Every rank, the level it opens at, and where the player currently stands.
 *
 * The card only ever showed the next rank and the XP left to it, which told
 * a player what was immediately ahead but nothing about the shape of the
 * climb — how many ranks exist, how far apart they are, what the top one is
 * called. A ladder someone can look at is what turns a number into a goal.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RankLadderSheet(progress: LevelProgressState, onDismiss: () -> Unit) {
    com.sualtikasifi.cizimhafiza.presentation.common.AppWindowDialog(title = stringResource(R.string.rank_ladder_title), onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(
                    R.string.rank_ladder_subtitle,
                    stringResource(progress.tier.rank.nameRes)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LevelTier.entries.forEach { tier ->
                val reached = progress.level >= tier.minLevel
                val isCurrent = tier == progress.tier
                val xpAway = (PlayerLevel.totalXpForLevel(tier.minLevel) - progress.totalXp).coerceAtLeast(0)
                RaisedCard(
                    corner = 18.dp,
                    face = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            // A rank still ahead is dimmed rather than hidden:
                            // the point of the list is seeing what is coming.
                            .alpha(if (reached) 1f else 0.55f)
                    ) {
                        Text(text = tier.rank.emoji, style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(tier.rank.nameRes),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.rank_ladder_unlock_level, tier.minLevel),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = when {
                                isCurrent -> stringResource(R.string.rank_ladder_current)
                                reached -> stringResource(R.string.rank_ladder_reached)
                                else -> stringResource(R.string.rank_ladder_remaining, xpAway)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * The daily challenge tile, built like a game's quest card:
 *  - artwork with a soft glow and, when the streak multiplies today's XP, a gold "🔥 3x" sticker on it;
 *  - the title and one pip per word — empty before playing, green/red for right/wrong afterwards,
 *    so "5 kelime" is something you see rather than read;
 *  - an edge-to-edge footer that is the call to action: a glossy gold "OYNA ▸" with a moving
 *    shine while the challenge is open, a green "✓ Tamamlandı" with the countdown to tomorrow once done.
 */
@Composable
private fun DailyChallengeCardNarrow(state: DailyChallengeState, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val available = state.isAvailableToday
    val top = if (available) Color(0xFF9B6BF2) else Color(0xFF52C378)
    val bottom = if (available) Color(0xFF6440D6) else Color(0xFF2E9A55)
    val edge = if (available) Color(0xFF3F2699) else Color(0xFF1E6E3B)
    val transition = rememberInfiniteTransition(label = "dailyTile")
    val shine = transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "dailyShine"
    )
    val arrowNudge = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dailyArrow"
    )
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val bold = androidx.compose.ui.text.font.FontWeight.ExtraBold
    val textShadow = androidx.compose.ui.text.TextStyle(
        shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(0f, 2f), 3f)
    )
    val flags = state.todayResult?.correctFlags.orEmpty()
    val multiplier = XpAwards.dailyStreakMultiplier(state.streakIfCompletedToday)

    Column(
        modifier = modifier
            .chunky(Brush.verticalGradient(listOf(top, bottom)), edge, corner = 22.dp, lift = 4.dp, rim = Color.White.copy(alpha = 0.35f))
            .then(if (available) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onPlay) else Modifier)
    ) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.38f), Color.Transparent)),
                        radius = size.minDimension / 2f
                    )
                }
                Image(
                    painter = painterResource(R.drawable.daily_calendar_icon),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).offset(y = (-8).dp).graphicsLayer { rotationZ = -6f }
                )
                run {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFE566), Color(0xFFFFB300))))
                            .border(1.5.dp, Color(0xFFB36B00), RoundedCornerShape(50))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 10.sp)
                        Text(
                            text = "${multiplier}x",
                            softWrap = false,
                            fontSize = 12.sp,
                            fontWeight = bold,
                            color = Color(0xFF4A2600),
                            maxLines = 1
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = stringResource(R.string.daily_challenge_title),
                    fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
                    fontWeight = bold,
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    color = Color.White,
                    style = textShadow,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    repeat(DailyChallenge.WORD_COUNT) { index ->
                        val flag = flags.getOrNull(index)
                        // Right: white disc with a green tick (reads on the green "done" tile). Wrong: red disc with a
                        // white cross. Not played yet: an empty ring.
                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(CircleShape)
                                .background(
                                    when (flag) {
                                        true -> Color.White
                                        false -> Color(0xFFE53935)
                                        null -> Color.White.copy(alpha = 0.22f)
                                    }
                                )
                                .border(1.5.dp, Color.White.copy(alpha = if (flag == null) 0.55f else 0.95f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            when (flag) {
                                true -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF1E9E52), modifier = Modifier.size(15.dp))
                                false -> Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                null -> Unit
                            }
                        }
                    }
                }
            }
        }
        if (available) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB300))))
                    .drawWithContent {
                        drawContent()
                        val x = size.width * shine.value
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                startX = x - 26.dp.toPx(),
                                endX = x + 26.dp.toPx()
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.daily_play_now).uppercase(androidx.compose.ui.text.intl.Locale.current.platformLocale),
                        fontFamily = com.sualtikasifi.cizimhafiza.presentation.theme.DisplayFont,
                        fontSize = 15.sp,
                        fontWeight = bold,
                        color = Color(0xFF4A2600),
                        maxLines = 1
                    )
                    Text(
                        text = "▸",
                        fontSize = 16.sp,
                        fontWeight = bold,
                        color = Color(0xFF4A2600),
                        modifier = Modifier.graphicsLayer { translationX = 3.dp.toPx() * arrowNudge.value }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(Color.Black.copy(alpha = 0.24f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⏳ " + midnightCountdownText(),
                    fontSize = 12.sp,
                    fontWeight = bold,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1
                )
                Text(
                    text = "✓ " + stringResource(R.string.daily_done_badge),
                    fontSize = 13.sp,
                    fontWeight = bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

/** The streak's XP multiplier as a gold pill: dark on gold, readable on both card colours. */
@Composable
private fun MultiplierChip(multiplier: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE566), Color(0xFFFFB300))))
            .border(1.5.dp, Color(0xFFB36B00), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "🔥", fontSize = 13.sp)
        Text(
            text = stringResource(R.string.daily_challenge_multiplier_badge, multiplier),
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = Color(0xFF4A2600),
            maxLines = 1
        )
    }
}

/**
 * Lays [content] out at its natural size and, if that is taller than the space
 * given, scales the whole thing down to fit instead of scrolling — so the home
 * screen never moves, whatever the phone's height, density or font setting.
 * Shorter content is centred vertically. Touch input follows the scaling.
 */
@Composable
private fun FitToHeight(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.layout.Layout(
        content = { Column(horizontalAlignment = Alignment.CenterHorizontally, content = content) },
        modifier = modifier.clipToBounds()
    ) { measurables, constraints ->
        val available = constraints.maxHeight
        val placeable = measurables.first().measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity)
        )
        val scale = if (placeable.height > available && placeable.height > 0) {
            (available.toFloat() / placeable.height).coerceAtLeast(0.55f)
        } else 1f
        val scaledHeight = (placeable.height * scale).toInt()
        layout(constraints.maxWidth, available) {
            val x = (constraints.maxWidth - placeable.width) / 2
            val y = ((available - scaledHeight) / 2).coerceAtLeast(0)
            placeable.placeWithLayer(x, y) {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
            }
        }
    }
}

/** One picture choice for the avatar: a round preview with its name, ringed when selected, dimmed when unavailable. */
@Composable
private fun AvatarSourceOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    photo: com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhoto?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(if (selected) 3.dp else 1.dp, if (selected) Color(0xFFFF7A21) else Color(0xFFD9B57A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photo != null) {
                com.sualtikasifi.cizimhafiza.presentation.common.AvatarPhotoFace(photo = photo, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.Group, contentDescription = null, tint = Color(0xFF7A5A44))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.Medium,
            color = Color(0xFF3A2416)
        )
    }
}
