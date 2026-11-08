package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.data.local.dao.DrawingResultDao
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.repository.AuthRepository
import com.sualtikasifi.cizimhafiza.domain.repository.AuthState
import com.sualtikasifi.cizimhafiza.presentation.reports.console.AssistantViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.AacEncoder
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Pcm
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.PcmOps
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Placed
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.SpeechSynth
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.ElevenLabs
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceBank
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceModel
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.VoiceSpec
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotTimeline
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotMode
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotSource
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotStyle
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
import com.sualtikasifi.cizimhafiza.domain.model.CouponExistsException
import com.sualtikasifi.cizimhafiza.domain.repository.CouponRepository
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipAudio
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealEnding
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealOpening
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.SocialPlatform
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.TimedWord
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VideoPost
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMix
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealHowto
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealMusic
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealPhase
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.VoicedLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealImages
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealStore
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RecentDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealTimeline
import com.sualtikasifi.cizimhafiza.data.local.dao.WordDao
import com.sualtikasifi.cizimhafiza.domain.repository.ModerationRepository
import com.sualtikasifi.cizimhafiza.util.GameConstants
import com.sualtikasifi.cizimhafiza.util.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.FORGOT_TURN

// The "Ödüllü Karalak Düellosu" half of the studio, as extensions of [StudioViewModel] (which keeps the state they work on).

// ---- Çizen Bile Bilemedi

internal fun StudioViewModel.updateForgot(change: (ForgotUi) -> ForgotUi) = _state.update { it.copy(forgot = change(it.forgot)) }

/** The script for [d] with the panel's choices; the lines of [previous] are kept (the code line follows the mode). */
/** The wording the next video uses: the one picked by hand, else the next in the turn (the turn moves on after each video is made). */
internal fun StudioViewModel.styleFor(ui: ForgotUi): ForgotStyle = ui.styleChoice ?: ForgotStyle.forTurn(prefs().getInt(FORGOT_TURN, 0))

internal fun StudioViewModel.buildForgot(d: StudioDrawing, ui: ForgotUi, previous: ForgotScript?): ForgotScript {
    val other = ForgotCopy.codeLineFor(if (ui.mode == ForgotMode.Word) ForgotMode.Shown else ForgotMode.Word)
    return ForgotScript(
        drawingId = d.id, word = d.word,
        hook = previous?.hook ?: SpokenLine(ForgotCopy.HOOK, MascotEmotion.Shocked),
        guesses = previous?.guesses ?: ForgotCopy.placeholderGuesses().take(ForgotCopy.DEFAULT_GUESSES),
        guessAt = previous?.guessAt ?: ForgotTimeline.stopsFor(ForgotCopy.DEFAULT_GUESSES, emptyList()),
        askLine = previous?.askLine ?: SpokenLine(ForgotCopy.ASK, MascotEmotion.Suspicious),
        codeLine = previous?.codeLine?.takeIf { it.text != other } ?: SpokenLine(ForgotCopy.codeLineFor(ui.mode), MascotEmotion.Point),
        outro = previous?.outro ?: SpokenLine(ForgotCopy.OUTRO, MascotEmotion.Wave),
        mode = ui.mode,
        reward = ForgotCopy.rewardLabel(ui.gold, ui.xp, ui.chest.chests),
        code = if (ui.mode == ForgotMode.Word) ForgotCopy.wordCode(d.word).orEmpty() else ui.shownCode,
        percent = previous?.percent ?: ForgotCopy.randomPercent(),
        showPercent = previous?.showPercent ?: false,
        uses = ui.maxUses,
        expiresAtMillis = ui.expiresAtMillis,
        showFirstLetter = previous?.showFirstLetter ?: false,
        revealed = previous?.takeIf { it.word == d.word }?.revealed.orEmpty(),
        style = styleFor(ui)
    )
}

/** Takes one of this phone's drawings and makes a first script around it. */
fun StudioViewModel.selectForgot(d: StudioDrawing) {
    _state.update { s ->
        val ui = s.forgot.copy(drawingId = d.id, writeError = null, couponStatus = null)
        s.copy(forgot = ui.copy(script = buildForgot(d, ui, null)), video = null, previewMs = 0L, renderError = null)
    }
    refreshPreview()
}

/** Applies a change to the panel's choices and rebuilds the script around them, keeping its lines. */
internal fun StudioViewModel.rebuildForgot(change: (ForgotUi) -> ForgotUi) {
    _state.update { s ->
        val ui = change(s.forgot).copy(couponStatus = null)
        val d = s.forgotDrawing(ui.drawingId)
        s.copy(forgot = ui.copy(script = if (d != null && ui.script != null) buildForgot(d, ui, ui.script) else ui.script), video = null)
    }
    refreshPreview()
}

/** Shows the next [FORGOT_PAGE] drawings in the list. */
fun StudioViewModel.showMoreForgot() = updateForgot { it.copy(shown = it.shown + FORGOT_PAGE) }

/** Switches between this phone's drawings and the pool's; the pool is read the first time it is asked for (one read of the latest rounds). */
fun StudioViewModel.setForgotSource(source: ForgotSource) {
    updateForgot { it.copy(source = source, shown = FORGOT_PAGE) }
    if (source == ForgotSource.Pool && _state.value.real.recent.isEmpty()) refreshRecent()
}

/** Switches how the code is given; the code line was written for the other way, so it goes back to the stock one for this. */
fun StudioViewModel.setForgotMode(mode: ForgotMode) {
    rebuildForgot { it.copy(mode = mode) }
    editForgotScript { it.copy(codeLine = SpokenLine(ForgotCopy.codeLineFor(mode), MascotEmotion.Point)) }
}
/** Picks the wording by hand, or (null) lets the three take turns. */
fun StudioViewModel.setForgotStyle(style: ForgotStyle?) = rebuildForgot { it.copy(styleChoice = style) }
/** Shows (or hides) the code's first letter in its first box, a hint for the viewer when the answer is the code. */
fun StudioViewModel.setForgotFirstLetter(on: Boolean) = editForgotScript { it.copy(showFirstLetter = on) }

/** Writes the letter on one of the word's lines in the video (or takes it off again), whenever the maker wants. */
fun StudioViewModel.toggleForgotLetter(slot: Int) = editForgotScript { sc ->
    if (slot !in sc.slotLetters.indices) sc else sc.copy(revealed = if (slot in sc.revealed) sc.revealed - slot else sc.revealed + slot)
}

fun StudioViewModel.setForgotChest(chest: ForgotChest) = rebuildForgot { it.copy(chest = chest) }
fun StudioViewModel.setForgotGold(n: Int) = rebuildForgot { it.copy(gold = n.coerceIn(0, CouponCodes.MAX_GOLD)) }
fun StudioViewModel.setForgotXp(n: Int) = rebuildForgot { it.copy(xp = n.coerceIn(0, CouponCodes.MAX_XP)) }
// the video says how many may use the code and until when, so the script follows these two
fun StudioViewModel.setForgotUses(n: Int) = rebuildForgot { it.copy(maxUses = n.coerceIn(0, MAX_COUPON_USES)) }
fun StudioViewModel.setForgotExpiry(millis: Long) = rebuildForgot { it.copy(expiresAtMillis = millis) }
fun StudioViewModel.newForgotCode() = rebuildForgot { it.copy(shownCode = CouponCodes.generate()) }

/** How many guesses Karalak makes (1 to 3); the stops are spread evenly again, and the lines already there stay. */
fun StudioViewModel.setForgotGuessCount(n: Int) = editForgotScript { sc ->
    val count = n.coerceIn(1, ForgotTimeline.MAX_GUESSES)
    val stock = ForgotCopy.placeholderGuesses()
    sc.copy(guesses = List(count) { sc.guesses.getOrNull(it) ?: stock[it] }, guessAt = ForgotTimeline.stopsFor(count, emptyList()))
}

/** Sets where guess [index] stops the drawing (0 to 1), held between its neighbours' stops: what the panel's bar drags. */
fun StudioViewModel.setForgotStop(index: Int, share: Float) = editForgotScript { sc ->
    sc.copy(guessAt = ForgotTimeline.moveStop(sc.stops, index, share))
}

/** Moves where guess [index] stops the drawing by [deltaPercent] points, for the fine nudge beside the bar. */
fun StudioViewModel.stepForgotStop(index: Int, deltaPercent: Int) {
    val now = _state.value.forgot.script?.stops?.getOrNull(index) ?: return
    setForgotStop(index, now + deltaPercent / 100f)
}


internal fun StudioViewModel.editForgotScript(change: (ForgotScript) -> ForgotScript) {
    _state.update { s ->
        val sc = s.forgot.script ?: return@update s
        s.copy(forgot = s.forgot.copy(script = change(sc)), video = null)
    }
    refreshPreview()
}

/** Lets the assistant look at the drawing and write Karalak's guesses, hook and closing, and the post text. */
fun StudioViewModel.writeForgot() {
    val s = _state.value
    val d = s.forgotDrawing(s.forgot.drawingId) ?: return
    if (s.forgot.writing) return
    val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
    if (key.isBlank()) { updateForgot { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
    val model = OpenRouterClient.FORGOT_MODEL
    val style = s.forgot.script?.style ?: styleFor(s.forgot)
    updateForgot { it.copy(writing = true, writeError = null) }
    viewModelScope.launch {
        ForgotDirector(OpenRouterClient(key), model, _state.value.lang).write(
            d, s.forgot.script?.stops.orEmpty(), s.forgot.mode, s.forgot.script?.reward.orEmpty(),
            ForgotCopy.limitText(s.forgot.maxUses, s.forgot.expiresAtMillis, style), style,
            ForgotCopy.HookKind.forTurn(s.forgot.hookTurn), ForgotCopy.OutroAngle.forTurn(s.forgot.hookTurn)
        ).onSuccess { w ->
            _state.update { st ->
                val cur = st.forgot.script ?: return@update st.copy(forgot = st.forgot.copy(writing = false))
                val c = w.lines
                st.copy(
                    forgot = st.forgot.copy(
                        writing = false,
                        hookTurn = st.forgot.hookTurn + 1,
                        hookTried = (st.forgot.hookTried + listOfNotNull(c.hook?.text)).takeLast(HOOK_MEMORY),
                        script = cur.copy(
                            hook = c.hook ?: cur.hook,
                            // one line per stop, the assistant's where it wrote one, else what was there
                            guesses = List(cur.guesses.size) { k -> c.guesses.getOrNull(k) ?: cur.guesses[k] },
                            askLine = c.ask ?: cur.askLine, codeLine = c.codeLine ?: cur.codeLine,
                            outro = c.outro ?: cur.outro
                        )
                    ),
                    caption = c.caption.ifBlank { st.caption }, hashtags = c.hashtags.ifBlank { st.hashtags },
                    aiCost = w.costUsd, video = null
                )
            }
            refreshPreview()
        }.onFailure { e -> updateForgot { it.copy(writing = false, writeError = e.message ?: "Yazılamadı") } }
    }
}

/** The app's music under the video (null: none); kept on this phone. A different track is heard first with ▶, never started by the choice. */
fun StudioViewModel.setForgotMusic(name: String?) {
    prefs().edit().apply { if (name == null) remove(StudioViewModel.FORGOT_MUSIC) else putString(StudioViewModel.FORGOT_MUSIC, name) }.apply()
    _state.update { it.copy(forgot = it.forgot.copy(music = name), video = null) }
}

fun StudioViewModel.setForgotMusicVolume(volume: Float) {
    val v = volume.coerceIn(0f, RealMix.MAX_VOLUME)
    prefs().edit().putFloat(StudioViewModel.FORGOT_MUSIC_VOLUME, v).apply()
    _state.update { it.copy(forgot = it.forgot.copy(musicVolume = v), video = null) }
}

/** Takes back the music and its volume this phone kept, unless the track is gone (one of the person's own that was removed). */
internal fun StudioViewModel.restoreForgotMusic(custom: List<String>) {
    val p = prefs()
    val name = p.getString(StudioViewModel.FORGOT_MUSIC, null)?.takeIf { n -> RealMusic.TRACKS.any { it.first == n } || n in custom }
    updateForgot { it.copy(music = name, musicVolume = p.getFloat(StudioViewModel.FORGOT_MUSIC_VOLUME, RealMix.DEFAULT_VOLUME).coerceIn(0f, RealMix.MAX_VOLUME)) }
}

/** How many earlier hooks the "yenile" button tells the assistant to stay away from. */
private const val HOOK_MEMORY = 6

/**
 * The "yenile" button beside the opening line: asks for a new hook only (the rest of the script stays), of the other kind than
 * the last one, and different from the ones already tried. The line that was there joins the tried ones.
 */
fun StudioViewModel.rewriteForgotHook() {
    val s = _state.value
    val d = s.forgotDrawing(s.forgot.drawingId) ?: return
    val script = s.forgot.script ?: return
    if (s.forgot.hookWriting || s.forgot.writing) return
    val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
    if (key.isBlank()) { updateForgot { it.copy(hookError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
    val kind = ForgotCopy.HookKind.forTurn(s.forgot.hookTurn)
    val avoid = (s.forgot.hookTried + script.hook.text).distinct().takeLast(HOOK_MEMORY)
    updateForgot { it.copy(hookWriting = true, hookError = null) }
    viewModelScope.launch {
        ForgotDirector(OpenRouterClient(key), OpenRouterClient.FORGOT_MODEL, _state.value.lang).writeHook(d.word, kind, avoid)
            .onSuccess { (hook, cost) ->
                _state.update { st ->
                    val cur = st.forgot.script ?: return@update st.copy(forgot = st.forgot.copy(hookWriting = false))
                    st.copy(
                        forgot = st.forgot.copy(
                            hookWriting = false, hookTurn = st.forgot.hookTurn + 1,
                            hookTried = (avoid + hook.text).takeLast(HOOK_MEMORY), script = cur.copy(hook = hook)
                        ),
                        aiCost = (st.aiCost ?: 0.0) + (cost ?: 0.0), video = null
                    )
                }
                refreshPreview()
            }
            .onFailure { e -> updateForgot { it.copy(hookWriting = false, hookError = e.message ?: "Yazılamadı") } }
    }
}

/** Creates the video's kupon kodu in Firestore, never over an existing code (its use count would start again). */
fun StudioViewModel.createForgotCoupon() {
    val ui = _state.value.forgot
    val script = ui.script ?: return
    if (ui.couponBusy) return
    val code = CouponCodes.normalize(script.code)
    if (code == null) {
        updateForgot { it.copy(couponStatus = "Bu kelime kod olamaz: 4–24 harf olmalı. “Kod ekranda”yı dene.") }
        return
    }
    val problem = when {
        script.reward.isBlank() -> "Ödül boş: altın, XP ya da kasa seç."
        ui.maxUses < 1 -> "En az 1 kişi kullanabilmeli."
        ui.expiresAtMillis <= System.currentTimeMillis() -> "Bitiş zamanı geçmişte kalmış."
        else -> null
    }
    if (problem != null) { updateForgot { it.copy(couponStatus = problem) }; return }
    updateForgot { it.copy(couponBusy = true, couponStatus = null) }
    viewModelScope.launch {
        val status = couponRepository.find(code).fold(
            onSuccess = { existing ->
                if (existing != null) "$code zaten var (${existing.usedCount}/${existing.maxUses} kullanıldı). Başka kelime ya da yeni kod seç."
                else {
                    // labelled with the video it is for, so the panel's coupon tab can tell how well each video's code did
                    val label = "Düello · ${SimpleDateFormat("d MMM", Locale("tr")).format(Date())} · ${script.style.title}"
                    val coupon = Coupon(code, ui.gold, ui.xp, ui.chest.chests, ui.maxUses, 0, ui.expiresAtMillis, source = label)
                    couponRepository.create(coupon).fold(
                        onSuccess = { "Kupon hazır: $code · ilk ${ui.maxUses} kişi · ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(ui.expiresAtMillis))}'e kadar · ${script.reward}" },
                        // the name was taken between the check and the write: the repository refuses to replace it
                        onFailure = { e -> if (e is CouponExistsException) "$code zaten var. Başka kelime ya da yeni kod seç." else "Kupon oluşturulamadı: ${e.message}" }
                    )
                }
            },
            onFailure = { e -> "Kupon kontrol edilemedi: ${e.message}" }
        )
        updateForgot { it.copy(couponBusy = false, couponStatus = status) }
    }
}
