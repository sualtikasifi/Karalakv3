package com.sualtikasifi.cizimhafiza.presentation.mainmenu

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.sualtikasifi.cizimhafiza.presentation.common.LocalOnline
import com.sualtikasifi.cizimhafiza.presentation.friends.FriendStreakDialog
import com.sualtikasifi.cizimhafiza.presentation.friends.FriendStreakViewModel
import com.sualtikasifi.cizimhafiza.presentation.friends.StreakPrompt
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessInboxDialog
import com.sualtikasifi.cizimhafiza.presentation.guess.GuessInboxViewModel

/**
 * The two windows the social features open the home screen with, one at a time and only once the opening windows are done
 * ([quiet]): first what came of the player's "Bil Bakalım" links, then a friend streak (a prize, a mend, a broken streak or
 * one that ends today). There are no push messages for either, so this is where the player finds out.
 * [onShowing] reports whether one is on screen, for the prompts that must not pile on top of it.
 */
@Composable
fun SocialHomeWindows(
    quiet: Boolean,
    activity: Activity?,
    onOpenFriends: () -> Unit,
    onShowing: (Boolean) -> Unit,
    inboxViewModel: GuessInboxViewModel = hiltViewModel(),
    streakViewModel: FriendStreakViewModel = hiltViewModel(),
    giftViewModel: com.sualtikasifi.cizimhafiza.presentation.gifts.GiftInboxViewModel = hiltViewModel(),
    stakeViewModel: com.sualtikasifi.cizimhafiza.presentation.duel.StakeSettleViewModel = hiltViewModel()
) {
    val online = LocalOnline.current
    LaunchedEffect(online, quiet) { if (online && quiet) { inboxViewModel.check(); giftViewModel.check(); stakeViewModel.check() } }

    val entries by inboxViewModel.entries.collectAsState()
    val prompt by streakViewModel.prompt.collectAsState()
    val gifts by giftViewModel.gifts.collectAsState()
    val giftResults by giftViewModel.results.collectAsState()

    // The window stays as it was first shown even if a later snapshot would no longer ask for it (the reminder is marked
    // as given the moment it appears).
    var shown by remember { mutableStateOf<StreakPrompt?>(null) }
    val inboxOpen = quiet && entries.isNotEmpty()
    val giftOpen = quiet && !inboxOpen && (gifts.isNotEmpty() || giftResults.isNotEmpty())
    val stakeResults by stakeViewModel.results.collectAsState()
    val stakeOpen = quiet && !inboxOpen && !giftOpen && stakeResults.isNotEmpty()
    LaunchedEffect(prompt, quiet, inboxOpen) {
        val next = prompt
        if (quiet && !inboxOpen && shown == null && next != null) {
            shown = next
            streakViewModel.onShown(next)
        }
    }
    LaunchedEffect(inboxOpen, giftOpen, stakeOpen, shown) { onShowing(inboxOpen || giftOpen || stakeOpen || shown != null) }

    if (inboxOpen) {
        GuessInboxDialog(entries = entries, rewardGold = inboxViewModel.rewardGold, onClose = inboxViewModel::claim)
        return
    }
    if (giftOpen) {
        if (gifts.isNotEmpty()) com.sualtikasifi.cizimhafiza.presentation.gifts.GiftInboxDialog(gifts, onClaim = giftViewModel::claimAll)
        else com.sualtikasifi.cizimhafiza.presentation.gifts.GiftResultDialog(giftResults, onClose = giftViewModel::dismissResults)
        return
    }
    if (stakeOpen) {
        com.sualtikasifi.cizimhafiza.presentation.duel.StakeResultDialog(stakeResults, onClose = stakeViewModel::dismiss)
        return
    }
    val current = shown ?: return
    if (!quiet) return
    FriendStreakDialog(
        prompt = current,
        onRescued = { streakViewModel.acknowledgeRescued(it); shown = null },
        onClaim = { streakViewModel.claim(it); shown = null },
        onRescue = { p ->
            activity?.let { streakViewModel.rescue(it, p.friendUid, p.pairId) }
            shown = null
        },
        onDuel = { shown = null; streakViewModel.dismiss(); onOpenFriends() },
        onDismiss = { shown = null; streakViewModel.dismiss() }
    )
}
