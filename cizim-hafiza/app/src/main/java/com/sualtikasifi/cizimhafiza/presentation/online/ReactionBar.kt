package com.sualtikasifi.cizimhafiza.presentation.online

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Menu
import com.sualtikasifi.cizimhafiza.presentation.common.LetteredText
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedDialog
import com.sualtikasifi.cizimhafiza.presentation.common.PaintedPillButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.annotation.StringRes
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.OnlinePlayer
import com.sualtikasifi.cizimhafiza.domain.model.Reaction
import com.sualtikasifi.cizimhafiza.presentation.common.PillShape
import com.sualtikasifi.cizimhafiza.presentation.common.RaisedCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Anything older than this was already said before you walked in — see [ReactionOverlay]. */
private const val FRESH_REACTION_WINDOW_MS = 10_000L

/** How long a per-sender chat bubble (see [rememberActiveReactionsByUid]) stays up before fading. */
private const val CHAT_BUBBLE_MILLIS = 3_200L

/**
 * Tracks, per sender uid, their most recent still-fresh [Reaction] — lets a
 * caller (see WaitingRoomScreen) anchor a chat bubble on that sender's own
 * player card instead of one shared pop-up that doesn't say who's talking.
 * Each entry clears itself on its own timer, keyed to that exact reaction —
 * so a second message from the same player restarts its bubble's clock
 * instead of the first message's timer wiping out the second one early.
 */
@Composable
fun rememberActiveReactionsByUid(reactions: List<Reaction>): Map<String, Reaction> {
    val activeByUid = remember { mutableStateMapOf<String, Reaction>() }
    var processedCount by remember { mutableStateOf(0) }
    LaunchedEffect(reactions.size) {
        // A rematch/reset can start a fresh, shorter reaction history under
        // the same screen instance — without this, processedCount would
        // stay stuck above the new (smaller) size and every future message
        // would silently never get picked up.
        if (reactions.size < processedCount) processedCount = 0
        if (reactions.size <= processedCount) return@LaunchedEffect
        val newOnes = reactions.subList(processedCount, reactions.size)
        processedCount = reactions.size
        for (reaction in newOnes) {
            if (System.currentTimeMillis() - reaction.sentAtMillis > FRESH_REACTION_WINDOW_MS) continue
            activeByUid[reaction.uid] = reaction
            launch {
                delay(CHAT_BUBBLE_MILLIS)
                if (activeByUid[reaction.uid] == reaction) activeByUid.remove(reaction.uid)
            }
        }
    }
    return activeByUid
}

/** A short preset emoji reaction — kept to a fixed set (no free text) so there's nothing to moderate. */
data class PresetReaction(val emoji: String, val key: String)

/** A short preset chat phrase — same "nothing to moderate" reasoning as [PresetReaction], just text instead of an emoji. */
data class PresetPhrase(val key: String, @StringRes val textRes: Int)

/**
 * The full emoji catalog — kept purpose-built for this game (reactions to a
 * drawing/round, not a general-purpose keyboard-wide picker) rather than
 * huge, per the "yararlı ama aşırı olmasın" brief. Emoji-only: no caption
 * text is shown or sent (see ReactionOverlay) — that's what the phrase sheet
 * is for.
 *
 * Order matters: [ReactionSendRow]'s 5 quick-send slots default to this
 * list's first 5 entries before any usage data exists (sortedByDescending is
 * stable, so an all-zero tie keeps catalog order), then per-device usage
 * takes over. The rest is reachable through the "+" picker sheet.
 *
 * Every key here needs its own reply branch in BotChatBrain.replyPool (see
 * BotChatBrainReplyCoverageTest) — adding an entry without one fails that
 * test rather than quietly having Sude answer it with the generic pool.
 */
val EMOJI_CATALOG = listOf(
    PresetReaction("😂", "funny"),
    PresetReaction("👏", "nice"),
    PresetReaction("😅", "hard"),
    PresetReaction("🔥", "fire"),
    PresetReaction("😱", "shock"),
    PresetReaction("👋", "hi"),
    PresetReaction("😢", "cry"),
    PresetReaction("😡", "angry"),
    PresetReaction("🤔", "think"),
    PresetReaction("😴", "sleepy"),
    PresetReaction("🥳", "party"),
    PresetReaction("😎", "cool"),
    PresetReaction("🙌", "raise_hands"),
    PresetReaction("💪", "strong"),
    PresetReaction("👍", "thumbs_up"),
    PresetReaction("❤️", "heart"),
    PresetReaction("🎉", "tada"),
    PresetReaction("🏆", "trophy")
)

val PRESET_PHRASES = listOf(
    PresetPhrase("chat_selam", R.string.chat_selam),
    PresetPhrase("chat_hazir_misin", R.string.chat_hazir_misin),
    PresetPhrase("chat_bol_sans", R.string.chat_bol_sans),
    PresetPhrase("chat_iyi_eglenceler", R.string.chat_iyi_eglenceler),
    PresetPhrase("chat_hos_geldin", R.string.chat_hos_geldin),
    PresetPhrase("chat_harikasin", R.string.chat_harikasin),
    PresetPhrase("chat_super_cizim", R.string.chat_super_cizim),
    PresetPhrase("chat_bildin", R.string.chat_bildin),
    PresetPhrase("chat_aynen_oyle", R.string.chat_aynen_oyle),
    PresetPhrase("chat_vay_canina", R.string.chat_vay_canina),
    PresetPhrase("chat_cok_iyiydi", R.string.chat_cok_iyiydi),
    PresetPhrase("chat_tam_isabet", R.string.chat_tam_isabet),
    PresetPhrase("chat_ne_ciziyorsun", R.string.chat_ne_ciziyorsun),
    PresetPhrase("chat_az_kaldi", R.string.chat_az_kaldi),
    PresetPhrase("chat_elinden_geliyor", R.string.chat_elinden_geliyor),
    PresetPhrase("chat_detaylara_bak_sen", R.string.chat_detaylara_bak_sen),
    PresetPhrase("chat_bilecegim", R.string.chat_bilecegim),
    PresetPhrase("chat_zor_bir_tane", R.string.chat_zor_bir_tane),
    PresetPhrase("chat_aklima_geldi", R.string.chat_aklima_geldi),
    PresetPhrase("chat_ipucu_ver", R.string.chat_ipucu_ver),
    PresetPhrase("chat_ne_bu_oyle", R.string.chat_ne_bu_oyle),
    PresetPhrase("chat_anlamadim_hic", R.string.chat_anlamadim_hic),
    PresetPhrase("chat_sanat_bu", R.string.chat_sanat_bu),
    PresetPhrase("chat_bir_daha_dene", R.string.chat_bir_daha_dene),
    PresetPhrase("chat_yakaladim_seni", R.string.chat_yakaladim_seni),
    PresetPhrase("chat_bu_turu_kazanacagim", R.string.chat_bu_turu_kazanacagim),
    PresetPhrase("chat_rovans_istiyorum", R.string.chat_rovans_istiyorum),
    PresetPhrase("chat_kafa_kafaya", R.string.chat_kafa_kafaya),
    PresetPhrase("chat_gorusuruz", R.string.chat_gorusuruz),
    PresetPhrase("chat_tekrar_oynayalim_mi", R.string.chat_tekrar_oynayalim_mi)
)

@StringRes
fun presetPhraseTextRes(messageKey: String): Int? =
    PRESET_PHRASES.find { it.key == messageKey }?.textRes

/**
 * A single compact dock: a "Sohbet" pill opening a scrollable sheet of
 * [PRESET_PHRASES] (the primary way to say something), plus a row of 6
 * fixed-width emoji slots beside it — the first opens the full
 * [EMOJI_CATALOG] picker sheet, the other 5 are this device's 5 most-used
 * entries from that catalog (see [emojiUsageCounts]) so the quick row
 * surfaces what a player actually reaches for instead of a fixed set
 * nothing here ever changes.
 *
 * Every slot gets an equal [Modifier.weight] share of the row instead of a
 * fixed size in a [horizontalScroll] container — six same-size circles
 * always divide this row's width exactly, on any screen, so there is
 * nothing left over to scroll (the fixed-34dp version could run a few
 * pixels short of its own row and visibly rock side to side for a range
 * that could never actually reveal a 7th item).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionSendRow(
    onSend: (emoji: String, messageKey: String) -> Unit,
    modifier: Modifier = Modifier,
    phraseUsageCounts: Map<String, Int> = emptyMap(),
    emojiUsageCounts: Map<String, Int> = emptyMap(),
    reactions: List<Reaction> = emptyList(),
    myUid: String? = null,
    players: List<OnlinePlayer> = emptyList()
) {
    var historyOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var phrasesOpen by remember { mutableStateOf(false) }
    // Most-sent-from-this-device first, least/never-sent last — sortedByDescending
    // is stable, so phrases tied on usage (typically 0) keep the catalog's
    // own order instead of shuffling every recomposition.
    val orderedPhrases = remember(phraseUsageCounts) {
        PRESET_PHRASES.sortedByDescending { phraseUsageCounts[it.key] ?: 0 }
    }
    val orderedEmojis = remember(emojiUsageCounts) {
        EMOJI_CATALOG.sortedByDescending { emojiUsageCounts[it.key] ?: 0 }
    }

    // The dock is one painted piece (ls_dock, 1740 x 904 px): the dog and the leaves above a cream plank for the ready
    // phrases and a brown one for the emojis. Everything inside is placed in the picture's own units, so it scales with it.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val s = maxWidth / 1740f
        val fontScale0 = androidx.compose.ui.platform.LocalDensity.current.fontScale
        fun fs(px: Float) = (px * s.value / fontScale0).sp
        Box(Modifier.fillMaxWidth().height(s * 904f)) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ls_dock),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            // Cream plank: the chat button (opens the whole conversation) and the phrases, sliding sideways.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(s * 166f, s * 394f)
                    .size(s * 132f, s * 132f)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16))))
                    .border(s * 5f, Color(0xFFA9440A), CircleShape)
                    .clickable { historyOpen = true }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = stringResource(R.string.reaction_chat_button),
                    tint = Color.White,
                    modifier = Modifier.size(s * 76f)
                )
            }
            // Phrases, most used first from the left; the last button opens every phrase in a window.
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(s * 14f),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(s * 318f, s * 394f).size(s * 1090f, s * 132f)
            ) {
                items(orderedPhrases.size, key = { orderedPhrases[it].key }) { i ->
                    val phrase = orderedPhrases[i]
                    DockChip(stringResource(phrase.textRes), s, fs(58f), light = true) { onSend("", phrase.key) }
                }
            }
            DockAllButton(
                icon = Icons.Filled.Menu,
                description = stringResource(R.string.reaction_chat_sheet_title),
                s = s,
                onBrown = false,
                modifier = Modifier.offset(s * 1428f, s * 394f).size(s * 132f, s * 132f)
            ) { phrasesOpen = true }
            // Brown plank: the same, for emojis.
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(s * 8f),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(s * 130f, s * 636f).size(s * 1290f, s * 128f)
            ) {
                items(orderedEmojis.size, key = { orderedEmojis[it].key }) { i ->
                    val preset = orderedEmojis[i]
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(s * 128f)
                            .clip(CircleShape)
                            .clickable { onSend(preset.emoji, preset.key) }
                    ) {
                        Text(text = preset.emoji, fontSize = fs(84f))
                    }
                }
            }
            DockAllButton(
                icon = Icons.Filled.Apps,
                description = stringResource(R.string.reaction_emoji_picker_content_description),
                s = s,
                onBrown = true,
                modifier = Modifier.offset(s * 1448f, s * 642f).size(s * 116f, s * 116f)
            ) { pickerOpen = true }
        }
    }

    if (historyOpen) {
        ChatHistoryDialog(reactions = reactions, myUid = myUid, players = players, onSend = onSend, onDismiss = { historyOpen = false })
    }

    // Every ready phrase, in the same most-used-first order as the dock.
    if (phrasesOpen) {
        PaintedDialog(
            title = stringResource(R.string.reaction_chat_sheet_title),
            onDismiss = { phrasesOpen = false },
            buttons = {
                PaintedPillButton(text = stringResource(R.string.close), onClick = { phrasesOpen = false }, modifier = Modifier.fillMaxWidth())
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                orderedPhrases.forEach { phrase ->
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16))))
                            .border(2.dp, Color(0xFFA9440A), shape)
                            .clickable {
                                phrasesOpen = false
                                onSend("", phrase.key)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(phrase.textRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Every emoji, again most used first.
    if (pickerOpen) {
        PaintedDialog(
            title = stringResource(R.string.reaction_emoji_sheet_title),
            onDismiss = { pickerOpen = false },
            buttons = {
                PaintedPillButton(text = stringResource(R.string.close), onClick = { pickerOpen = false }, modifier = Modifier.fillMaxWidth())
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                orderedEmojis.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { preset ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF8E3B4))
                                    .clickable {
                                        pickerOpen = false
                                        onSend(preset.emoji, preset.key)
                                    }
                            ) {
                                Text(text = preset.emoji, fontSize = 26.sp)
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** The round button at the right end of a plank of the dock that opens the full list. */
@Composable
private fun DockAllButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    s: androidx.compose.ui.unit.Dp,
    onBrown: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(s * 30f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(
                if (onBrown) androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF8A5530), Color(0xFF6A3E1E)))
                else androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16)))
            )
            .border(s * 5f, if (onBrown) Color(0xFFD9A55A) else Color(0xFFA9440A), shape)
            .clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(s * 70f))
    }
}
/** A ready phrase on the cream plank of the dock: an orange pill. */
@Composable
private fun DockChip(text: String, s: androidx.compose.ui.unit.Dp, size: androidx.compose.ui.unit.TextUnit, light: Boolean, onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight(0.86f)
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16))))
            .border(s * 4f, Color(0xFFA9440A), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = s * 34f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = size, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * Shows the most recent reaction — including your own, so sending one
 * doesn't look like it silently failed — as a short-lived, hard-to-miss
 * pop-up. [players] (the room's current roster) is only used to label the
 * bubble with who sent it, since a room can have up to
 * [GameConstants.MAX_ROOM_SIZE] players and the emoji alone stops being
 * enough to tell who reacted once there's more than one other player.
 */
@Composable
fun ReactionOverlay(reactions: List<Reaction>, myUid: String?, players: List<OnlinePlayer>, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf<Reaction?>(null) }

    // Keyed on the reaction's own identity (who sent it + when), not just
    // list size — a plain size-based key can coincidentally repeat (e.g.
    // across a rematch's fresh reaction stream) and silently miss re-firing
    // the animation for a genuinely new reaction that happens to land the
    // list back at a previously-seen size.
    val latestKey = reactions.lastOrNull()?.let { it.uid to it.sentAtMillis }
    LaunchedEffect(latestKey) {
        val latest = reactions.lastOrNull() ?: return@LaunchedEffect
        // Only genuinely new messages pop up. observeReactions replays the
        // room's whole history on subscribe, so without this the last thing
        // anyone said — possibly hours ago, possibly your own message from a
        // previous session — would flash on screen the moment you walked
        // into the room, as if it had just been sent.
        if (System.currentTimeMillis() - latest.sentAtMillis > FRESH_REACTION_WINDOW_MS) return@LaunchedEffect
        visible = latest
        delay(2500)
        visible = null
    }

    AnimatedVisibility(
        visible = visible != null,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(tween(150)),
        exit = scaleOut(animationSpec = tween(200)) + fadeOut(tween(200)),
        modifier = modifier
    ) {
        val reaction = visible
        if (reaction != null) {
            // A proper "raised card" (the app's one elevation idiom — see
            // AppComponents.raisedSurface) instead of a flat, thin-bordered
            // Surface: a flat pill with just a 2dp outline reads as a bare
            // line of text with no real background next to the app's bold
            // chunky cards everywhere else.
            RaisedCard(corner = 22.dp, face = MaterialTheme.colorScheme.surface, border = MaterialTheme.colorScheme.primary) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    val senderName = if (reaction.uid == myUid) {
                        stringResource(R.string.online_you_label, "")
                    } else {
                        players.find { it.uid == reaction.uid }?.displayName
                    }
                    if (!senderName.isNullOrBlank()) {
                        Text(
                            text = senderName.trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val phraseTextRes = presetPhraseTextRes(reaction.messageKey)
                    if (phraseTextRes != null) {
                        // A chat phrase (see PRESET_PHRASES): the text itself
                        // is the whole message, shown big instead of an emoji.
                        // Capped at 2 lines + a max width so a longer phrase
                        // still fits the fixed-height overlay slot both
                        // WaitingRoomScreen and OnlineResultScreen reserve for
                        // this composable.
                        Text(
                            text = stringResource(phraseTextRes),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                    } else {
                        // A plain emoji send (see EMOJI_CATALOG): just the
                        // emoji, no caption underneath — text belongs to the
                        // chat sheet now, not the quick-emoji row.
                        Text(text = reaction.emoji, fontSize = 36.sp)
                    }
                }
            }
        }
    }
}

/**
 * A persistent chat row for the result comparison screen — [ReactionOverlay]'s
 * replacement there. That pop-up had its own solid card, so every time a
 * message arrived it inserted real height into the layout (or, seen from the
 * player's side, popped a solid block over whatever was drawn where it
 * landed) and then vanished again a couple of seconds later — the "message
 * destroys the background behind it" complaint. This row's height never
 * changes: it always shows one line (the latest message, or an empty-state
 * hint), and tapping it opens the full history instead of only ever
 * revealing the single most recent thing anyone said.
 *
 * [reactions] lives in the screen's own ViewModel state (a live Firestore
 * listener started in that ViewModel's scope), so it is naturally forgotten
 * the moment the player leaves the result screen — nothing here persists it
 * on its own.
 */
@Composable
fun ChatRow(
    reactions: List<Reaction>,
    myUid: String?,
    players: List<OnlinePlayer>,
    modifier: Modifier = Modifier,
    onSend: ((emoji: String, messageKey: String) -> Unit)? = null
) {
    var historyOpen by remember { mutableStateOf(false) }
    RaisedCard(corner = 18.dp, onClick = { historyOpen = true }, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            val latest = reactions.lastOrNull()
            if (latest == null) {
                Text(
                    text = stringResource(R.string.online_chat_row_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val senderName = if (latest.uid == myUid) {
                    stringResource(R.string.online_you_label, "").trim()
                } else {
                    players.find { it.uid == latest.uid }?.displayName.orEmpty()
                }
                val preview = presetPhraseTextRes(latest.messageKey)?.let { stringResource(it) } ?: latest.emoji
                Text(
                    text = if (senderName.isNotBlank()) "$senderName: $preview" else preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (reactions.isNotEmpty()) {
                Text(
                    text = "${reactions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (historyOpen) {
        ChatHistoryDialog(reactions = reactions, myUid = myUid, players = players, onSend = onSend, onDismiss = { historyOpen = false })
    }
}

/**
 * The chat window [ChatRow] opens on tap: the painted "Sohbet" frame with every message of the match, oldest first, and
 * — on the frame's bottom plank — the ready-made phrases and emojis to answer with (there is no typing in this game).
 * Everything is laid out in the frame picture's own units (941 x 1672), so it scales with it.
 */
@Composable
private fun ChatHistoryDialog(
    reactions: List<Reaction>,
    myUid: String?,
    players: List<OnlinePlayer>,
    onSend: ((emoji: String, messageKey: String) -> Unit)?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val frameW = minOf(maxWidth - 8.dp, (maxHeight - 16.dp) / (1672f / 941f))
            val u = frameW / 941f
            val fontScale0 = androidx.compose.ui.platform.LocalDensity.current.fontScale
            fun fs(px: Float) = (px * u.value / fontScale0).sp
            Box(
                Modifier
                    .size(frameW, frameW * (1672f / 941f))
                    .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ls_chat_frame),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                // The title, on the top plank.
                LetteredText(
                    text = stringResource(R.string.online_chat_history_title),
                    size = fs(84f),
                    outline = Color(0xFF5A2815),
                    maxLines = 1,
                    title = true,
                    modifier = Modifier.offset(u * 230f, u * 66f).size(u * 480f, u * 112f)
                )
                // The close badge, on the frame's top right corner.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(u * 770f, u * 30f)
                        .size(u * 96f, u * 96f)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFF6A5A), Color(0xFFD42A1E))))
                        .border(u * 6f, Color.White, CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = fs(54f), fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                        color = Color.White
                    )
                }
                // Messages, on the paper.
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LaunchedEffect(reactions.size) {
                    if (reactions.isNotEmpty()) listState.animateScrollToItem(reactions.lastIndex)
                }
                Box(Modifier.offset(u * 140f, u * 262f).size(u * 662f, u * 1150f)) {
                    if (reactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.online_chat_empty_window),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fs(34f)),
                            color = Color(0xFF8A5A2E),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = u * 40f)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(u * 14f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(reactions.size) { index ->
                                val reaction = reactions[index]
                                val mine = reaction.uid == myUid
                                val sender = players.find { it.uid == reaction.uid }
                                val name = if (mine) stringResource(R.string.online_you_label, "").trim() else sender?.displayName.orEmpty()
                                val text = presetPhraseTextRes(reaction.messageKey)?.let { stringResource(it) } ?: reaction.emoji
                                val time = remember(reaction.sentAtMillis) {
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(reaction.sentAtMillis))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    if (sender != null) PlayerPicture(sender, u * 70f) else Spacer(Modifier.size(u * 70f))
                                    Spacer(Modifier.width(u * 10f))
                                    Column(
                                        Modifier
                                            .widthIn(max = u * 400f)
                                            .background(
                                                if (mine) Color(0xFFCFE4FA) else Color(0xFFF8E3B4),
                                                androidx.compose.foundation.shape.RoundedCornerShape(u * 26f)
                                            )
                                            .padding(horizontal = u * 22f, vertical = u * 10f)
                                    ) {
                                        if (name.isNotBlank()) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontSize = fs(30f)),
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = if (mine) Color(0xFF1F5FA8) else Color(0xFFE8650F),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = fs(34f)),
                                            color = Color(0xFF3A2416)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f, fill = true))
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = fs(26f)),
                                        color = Color(0xFF8A5A2E)
                                    )
                                }
                            }
                        }
                    }
                }
                // The answers, on the bottom plank: ready-made phrases and emojis, scrolling sideways.
                if (onSend != null) {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(u * 12f),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = u * 6f),
                        modifier = Modifier.offset(u * 215f, u * 1496f).size(u * 512f, u * 90f)
                    ) {
                        items(PRESET_PHRASES.size) { i ->
                            val phrase = PRESET_PHRASES[i]
                            ChatChip(stringResource(phrase.textRes), u, fs(30f)) { onSend("", phrase.key) }
                        }
                        items(EMOJI_CATALOG.size) { i ->
                            val e = EMOJI_CATALOG[i]
                            ChatChip(e.emoji, u, fs(36f)) { onSend(e.emoji, e.key) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatChip(text: String, u: androidx.compose.ui.unit.Dp, size: androidx.compose.ui.unit.TextUnit, onClick: () -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(u * 64f)
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFB14E), Color(0xFFF47A16))))
            .border(1.5.dp, Color(0xFFA9440A), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = u * 22f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = size, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = Color.White,
            maxLines = 1
        )
    }
}
