package com.sualtikasifi.cizimhafiza.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import com.sualtikasifi.cizimhafiza.domain.model.Chest
import com.sualtikasifi.cizimhafiza.domain.model.ChestTier
import com.sualtikasifi.cizimhafiza.presentation.chests.ChestDetailDialog
import com.sualtikasifi.cizimhafiza.presentation.theme.CizimHafizaTheme

/**
 * Debug builds only: shows the chest window on its own so its layout can be looked at in the CI emulator.
 * Extras: tier = silver|gold|rare, state = start|unlocking|ready|busy|used|won|announce|announce_long|gift|gift_blocked|gift_inbox|report|phrases|invite (won = the "you won a chest" window; announce* = the developer-panel notice window).
 */
@dagger.hilt.android.AndroidEntryPoint
class ChestPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tier = when (intent.getStringExtra("tier")) {
            "gold" -> ChestTier.GOLD
            "rare" -> ChestTier.RARE
            else -> ChestTier.SILVER
        }
        val state = intent.getStringExtra("state") ?: "start"
        val now = System.currentTimeMillis()
        val chest = when (state) {
            "unlocking", "used" -> Chest("x", tier, unlockStartedAtMillis = now - tier.unlockDurationMillis * 4 / 10)
            "ready" -> Chest("x", tier, unlockStartedAtMillis = now - tier.unlockDurationMillis - 1000)
            else -> Chest("x", tier)
        }
        setContent {
            CizimHafizaTheme {
                // the tuning arrows (when the mode is on in the title_nudge preference), as in MainActivity
                com.sualtikasifi.cizimhafiza.presentation.common.TitleNudgeOverlay()
                if (state.startsWith("announce")) {
                    val long = state == "announce_long"
                    com.sualtikasifi.cizimhafiza.presentation.common.AnnouncementDialog(
                        title = if (long) "Yeni sezon başladı: Bahar Festivali ve sürpriz ödüller seni bekliyor!" else "Öğretmenler gününüz kutlu olsun!",
                        body = if (long) "Bu hafta boyunca her günlük meydan okuma ekstra altın veriyor. Arkadaşlarınla düello yap, ligde yüksel ve özel Bahar kasasını kazan. Ayrıntılar için Ayarlar > Hakkında bölümüne göz atabilirsin. İyi eğlenceler!\n\nKodu girmeyi unutma: BAHAR2026" else "\"OGRETMEN\" kodu ile 500 altını kap!",
                        onClose = { finish() }
                    )
                    return@CizimHafizaTheme
                }
                if (state == "invite") {
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                        com.sualtikasifi.cizimhafiza.presentation.common.IncomingInviteBanner(
                            invite = com.sualtikasifi.cizimhafiza.domain.model.MatchInvite("i", "u", "Karalak4365", "123456", 0L),
                            isResponding = false, onAccept = {}, onDecline = {}
                        )
                    }
                    return@CizimHafizaTheme
                }
                if (state == "phrases") {
                    com.sualtikasifi.cizimhafiza.presentation.online.PhrasesWindow(
                        phrases = com.sualtikasifi.cizimhafiza.presentation.online.PRESET_PHRASES,
                        onPick = { finish() },
                        onDismiss = { finish() }
                    )
                    return@CizimHafizaTheme
                }
                if (state == "report") {
                    com.sualtikasifi.cizimhafiza.presentation.common.ReportPlayerDialog(
                        nickname = "Karalak4365",
                        sendState = com.sualtikasifi.cizimhafiza.presentation.common.ReportSendState.Idle,
                        onReport = {},
                        onDismiss = { finish() }
                    )
                    return@CizimHafizaTheme
                }
                if (state == "pen_picker") {
                    com.sualtikasifi.cizimhafiza.presentation.mainmenu.PenPickerSheet(
                        items = com.sualtikasifi.cizimhafiza.domain.model.PenSkin.entries.filter { it.isStoreItem || it.unlockLevel >= 0 }.take(18)
                            .mapIndexed { i, skin -> com.sualtikasifi.cizimhafiza.presentation.mainmenu.PenSkinUiItem(skin, unlocked = i < 6, selected = i == 1) },
                        onSelect = {}, onDismiss = { finish() }
                    )
                    return@CizimHafizaTheme
                }
                if (state == "avatar_picker") {
                    com.sualtikasifi.cizimhafiza.presentation.mainmenu.AvatarFramePickerSheet(
                        items = com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame.entries
                            .mapIndexed { i, f -> com.sualtikasifi.cizimhafiza.presentation.mainmenu.AvatarFrameUiItem(f, unlocked = i < 8, selected = i == 2) },
                        photoSource = "DINO", onPhotoSource = {}, onSelect = {}, onDismiss = { finish() }
                    )
                    return@CizimHafizaTheme
                }
                if (state == "duel_create") {
                    com.sualtikasifi.cizimhafiza.presentation.duel.CreateDuelScreen(
                        opponentName = "Elif", opponentUid = "u", onBack = { finish() }, onChallengeStarted = { _, _ -> }
                    )
                    return@CizimHafizaTheme
                }
                if (state.startsWith("stake")) {
                    if (state == "stake_result") {
                        val st = com.sualtikasifi.cizimhafiza.domain.model.DuelStatus.COMPLETE
                        com.sualtikasifi.cizimhafiza.presentation.duel.StakeResultDialog(
                            listOf(
                                com.sualtikasifi.cizimhafiza.domain.repository.StakeSettlement("Elif", 500, 950, st),
                                com.sualtikasifi.cizimhafiza.domain.repository.StakeSettlement("Can", 250, 0, st),
                                com.sualtikasifi.cizimhafiza.domain.repository.StakeSettlement("Deniz", 100, 100, com.sualtikasifi.cizimhafiza.domain.model.DuelStatus.EXPIRED)
                            )
                        ) { finish() }
                    } else if (state == "stake_blocked") {
                        com.sualtikasifi.cizimhafiza.presentation.duel.StakePickerDialog(
                            com.sualtikasifi.cizimhafiza.presentation.duel.StakePickerState(10, loading = false, block = com.sualtikasifi.cizimhafiza.domain.model.StakeBlock.FRIEND_NOT_READY),
                            "Elif", 800, {}, { finish() }
                        )
                    } else {
                        com.sualtikasifi.cizimhafiza.presentation.duel.StakePickerDialog(
                            com.sualtikasifi.cizimhafiza.presentation.duel.StakePickerState(10, loading = false, usage = com.sualtikasifi.cizimhafiza.domain.model.StakeUsage(1500, 2, 500)),
                            "Elif", 800, {}, { finish() }
                        )
                    }
                    return@CizimHafizaTheme
                }
                if (state.startsWith("gift")) {
                    val g = com.sualtikasifi.cizimhafiza.presentation.gifts.GiftSendState("u", "Elif", loading = false, usage = com.sualtikasifi.cizimhafiza.domain.model.GiftUsage(300, 1, 300))
                    when (state) {
                        "gift_blocked2" -> com.sualtikasifi.cizimhafiza.presentation.gifts.GiftSendDialog(g.copy(usage = null, blocked = com.sualtikasifi.cizimhafiza.domain.model.GiftBlock.FRIEND_NOT_READY), 900, {}, { finish() })
                        "gift_blocked" -> com.sualtikasifi.cizimhafiza.presentation.gifts.GiftSendDialog(g.copy(usage = null, blocked = com.sualtikasifi.cizimhafiza.domain.model.GiftBlock.LEVEL), 900, {}, { finish() })
                        "gift_inbox" -> com.sualtikasifi.cizimhafiza.presentation.gifts.GiftInboxDialog(
                            listOf(
                                com.sualtikasifi.cizimhafiza.domain.model.IncomingGift("a", "u", "Elif", com.sualtikasifi.cizimhafiza.domain.model.GiftKind.GOLD, "", 1, 500, 400),
                                com.sualtikasifi.cizimhafiza.domain.model.IncomingGift("b", "u", "Can", com.sualtikasifi.cizimhafiza.domain.model.GiftKind.JOKER, "FIRST_LETTER", 3, 495, 0)
                            )
                        ) { finish() }
                        else -> com.sualtikasifi.cizimhafiza.presentation.gifts.GiftSendDialog(g, 900, {}, { finish() })
                    }
                    return@CizimHafizaTheme
                }
                if (state == "won") {
                    com.sualtikasifi.cizimhafiza.presentation.common.ChestWonDialog(chest = chest, onDismiss = { finish() })
                    return@CizimHafizaTheme
                }
                ChestDetailDialog(
                    chest = chest,
                    nowMillis = now,
                    anotherUnlocking = state == "busy",
                    speedupAvailable = state == "unlocking",
                    instantAvailable = state == "unlocking",
                    bubble = null,
                    onBubbleDone = {},
                    onTapChest = {},
                    onStart = {},
                    onOpen = {},
                    onSpeedup = {},
                    onInstant = {},
                    onDismiss = { finish() }
                )
            }
        }
    }
}
