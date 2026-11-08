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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.CARD_SCALE
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.CARD_X
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.CARD_Y
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.POST_SAME_MS
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.ENDING
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.TAB
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.STEP
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.VOICE_STEP
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.MUSIC_ON
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.HOWTO_ON
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.PLATFORM
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.VOICE_MAX_MS
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.RECENT_MAX
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.RECENT_PAGE
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.RECENT_PAGES
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.MAX_REVEALS

// The "Gerçek Olsaydı" half of the studio and its library, as extensions of [StudioViewModel] (which keeps the state they work on).

// ---- Gerçek Olsaydı

internal fun StudioViewModel.updateReal(change: (RealUi) -> RealUi) = _state.update { it.copy(real = change(it.real)) }

/** Changes the project, keeps it on disk and redraws the preview. */
internal fun StudioViewModel.updateProject(saveNow: Boolean = false, change: (RealProject) -> RealProject) {
    var next: RealProject? = null
    _state.update { s ->
        val p = s.real.project ?: return@update s
        next = change(p)
        s.copy(real = s.real.copy(project = next), video = null)
    }
    val p = next ?: return
    // every piece that has a picture or a clip is kept in the library, and kept up to date as it is edited
    val library = libraryWith(p, _state.value.real.library)
    updateReal { it.copy(library = library, libraryBytes = realStore.sizeOf(library.flatMap { i -> i.media })) }
    realSaveJob?.cancel()
    realSaveJob = viewModelScope.launch(Dispatchers.IO) {
        if (!saveNow) delay(400)
        runCatching { realStore.save(p); realStore.saveLibrary(library) }
    }
    loadRealMedia(p)
    refreshPreview()
}

internal fun StudioViewModel.libraryWith(p: RealProject, old: List<LibraryItem>): List<LibraryItem> {
    val now = System.currentTimeMillis()
    var list = old
    p.reveals.filter { it.image != null || it.clip != null }.forEach { r ->
        val d = p.drawing(r.sampleId) ?: return@forEach
        val existing = list.find { it.id == r.sampleId }
        val item = LibraryItem(r.sampleId, existing?.savedAt ?: now, d, r, existing?.posts.orEmpty())
        if (existing == null) list = listOf(item) + list
        else if (existing != item) list = list.map { if (it.id == r.sampleId) item else it }
    }
    return list
}

internal fun StudioViewModel.updateReveal(sampleId: String, saveNow: Boolean = false, change: (RealReveal) -> RealReveal) =
    updateProject(saveNow) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) change(it) else it }) }

/** The project left from last time, if any, and the latest drawings read last time. */
internal fun StudioViewModel.loadReal() {
    val saved = prefs().let { CardLayout(it.getFloat(CARD_SCALE, CardLayout.DEFAULT_SCALE), it.getFloat(CARD_X, CardLayout.DEFAULT_X), it.getFloat(CARD_Y, CardLayout.DEFAULT_Y)).clamped() }
    viewModelScope.launch {
        val (p, recent) = withContext(Dispatchers.IO) { realStore.load() to realStore.loadRecent() }
        val library = withContext(Dispatchers.IO) { realStore.loadLibrary() }
        val prefs = prefs()
        val ending = runCatching { RealEnding.valueOf(prefs.getString(ENDING, null).orEmpty()) }.getOrDefault(RealEnding.Card)
        val platform = SocialPlatform.of(prefs.getString(PLATFORM, null))
        val tab = runCatching { RealTab.valueOf(prefs.getString(TAB, null).orEmpty()) }.getOrDefault(RealTab.Prepare)
        updateReal { it.copy(tab = tab, step = prefs.getInt(STEP, 1).coerceIn(1, 7), voiceStep = prefs.getInt(VOICE_STEP, 1).coerceIn(1, 5), project = p, recent = recent, card = saved, library = library, libraryBytes = realStore.sizeOf(library.flatMap { i -> i.media }), ending = ending, musicOn = prefs.getBoolean(MUSIC_ON, true), platform = platform, customMusic = RealMusic.custom(context)) }
        restoreForgotMusic(RealMusic.custom(context))
        p?.let { loadRealMedia(it) }
        loadPromo()
        loadHowto(prefs.getBoolean(HOWTO_ON, true))
    }
}

/** The how-to video that ships in the app: copied out once, its length read, opened for the preview. */
internal suspend fun StudioViewModel.loadHowto(on: Boolean) {
    val (file, ms, frames) = withContext(Dispatchers.IO) {
        val f = runCatching { RealHowto.ensure(context) }.getOrNull()
        val ms = f?.let { RealImages.clipMs(it) } ?: 0L
        Triple(f?.takeIf { ms > 0 }, ms, f?.takeIf { ms > 0 }?.let { runCatching { ClipFrames(it, exact = false) }.getOrNull() })
    }
    previewHowto?.close()
    previewHowto = frames
    updateReal { it.copy(howto = file, howtoMs = ms.coerceAtMost(RealTimeline.HOWTO_MAX), howtoOn = on) }
    refreshPreview()
}

/** Which part of the panel is open: the steps, the library or the settings. Kept on this phone. */
fun StudioViewModel.setRealTab(tab: RealTab) {
    prefs().edit().putString(TAB, tab.name).apply()
    updateReal { it.copy(tab = tab) }
}

/** Goes to one of the seven steps. Kept on this phone, so the work is picked up where it was left. */
fun StudioViewModel.setRealStep(step: Int) {
    val n = step.coerceIn(1, 7)
    prefs().edit().putInt(STEP, n).apply()
    updateReal { it.copy(step = n) }
    // the preview follows the step: the voice being set, the clip with the card on it, the start
    when (n) { 5 -> previewVoiceStep(_state.value.real.voiceStep); 6 -> previewLine(RealLine.Narration); 7 -> setPreviewTime(0L); else -> Unit }
}

/** Goes to one of the five voice steps (hook, narrator, closing, music, the line over the end card). */
fun StudioViewModel.setVoiceStep(step: Int) {
    val n = step.coerceIn(1, 5)
    prefs().edit().putInt(VOICE_STEP, n).apply()
    updateReal { it.copy(voiceStep = n) }
    previewVoiceStep(n)
}

/** Whether the video of how the game is played comes before the end card. Kept on this phone. */
fun StudioViewModel.setHowtoOn(on: Boolean) {
    prefs().edit().putBoolean(HOWTO_ON, on).apply()
    updateReal { it.copy(howtoOn = on) }
    _state.update { it.copy(video = null) }
    refreshPreview()
}

/** The promo video kept on this phone, if any: its length, and opened for the preview. */
internal suspend fun StudioViewModel.loadPromo() {
    val (file, ms, frames) = withContext(Dispatchers.IO) {
        val f = realStore.promo()
        val ms = f?.let { RealImages.clipMs(it) } ?: 0L
        Triple(f?.takeIf { ms > 0 }, ms, f?.takeIf { ms > 0 }?.let { runCatching { ClipFrames(it, exact = false) }.getOrNull() })
    }
    previewPromo?.close()
    previewPromo = frames
    updateReal { it.copy(promo = file, promoMs = if (file != null) ms.coerceAtMost(RealTimeline.PROMO_MAX) else 0L, promoBusy = false) }
    refreshPreview()
}

/** Makes the picked video the promo that closes every video, in place of any earlier one. */
fun StudioViewModel.importPromo(uri: Uri) {
    if (_state.value.real.promoBusy) return
    updateReal { it.copy(promoBusy = true) }
    viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                previewPromo?.close()
                previewPromo = null
                val f = realStore.setPromo(uri)
                if (RealImages.clipMs(f) <= 0L) { realStore.clearPromo(); error("Video okunamadı") }
            }
        }
        ok.onFailure { e -> _state.update { it.copy(message = "Tanıtım videosu eklenemedi: ${e.message}") } }
        // a promo that was just picked is meant to be used
        if (ok.isSuccess) setEnding(RealEnding.Promo)
        // a finished video made with the earlier promo is out of date
        _state.update { it.copy(video = null) }
        loadPromo()
    }
}

fun StudioViewModel.removePromo() {
    viewModelScope.launch {
        previewPromo?.close()
        previewPromo = null
        withContext(Dispatchers.IO) { runCatching { realStore.clearPromo() } }
        _state.update { it.copy(video = null) }
        loadPromo()
    }
}

/** What closes the video; kept on this phone. The promo is only used while there is one. */
fun StudioViewModel.setEnding(ending: RealEnding) {
    prefs().edit().putString(ENDING, ending.name).apply()
    updateReal { it.copy(ending = ending) }
    _state.update { it.copy(video = null) }
    refreshPreview()
}

/** Whether the app's music is mixed into the video; off, the video comes out without it. Kept on this phone. */
fun StudioViewModel.setMusicOn(on: Boolean) {
    prefs().edit().putBoolean(MUSIC_ON, on).apply()
    updateReal { it.copy(musicOn = on) }
    _state.update { it.copy(video = null) }
    refreshPreview()
}

/** Where the video will be posted; its post text follows. Kept on this phone. */
fun StudioViewModel.setPlatform(platform: SocialPlatform) {
    prefs().edit().putString(PLATFORM, platform.name).apply()
    updateReal { it.copy(platform = platform) }
}

/** How the video opens (see [RealOpening]); the preview goes back to the very start to show it. */
fun StudioViewModel.setOpening(sampleId: String, opening: RealOpening) {
    updateReveal(sampleId, saveNow = true) { it.copy(opening = opening) }
    setPreviewTime(0L)
}

/**
 * Reads the newest drawings of the reviewed pool again: the latest [RECENT_MAX], newest round first and each
 * round in play order, every one with its word, the seconds that word gives, and its player's level and
 * frame. The name comes only for players who allowed to be featured; the others stay anonymous. What was
 * read last stays when reading fails.
 */
fun StudioViewModel.refreshRecent() {
    if (_state.value.real.loadingRecent) return
    updateReal { it.copy(loadingRecent = true, recentError = null) }
    viewModelScope.launch {
        val read = runCatching {
            // who allowed their name; failing to find out leaves everyone anonymous rather than naming anyone
            val players = moderation.featuredPlayers().getOrDefault(emptyList()).associateBy { it.uid }
            val out = mutableListOf<RecentDrawing>()
            var cursor: Long? = null
            for (page in 0 until RECENT_PAGES) {
                val p = moderation.poolRuns(RECENT_PAGE, cursor).getOrThrow()
                for (run in p.runs) {
                    val player = players[run.uid]
                    run.items.forEachIndexed { i, item ->
                        if (out.size < RECENT_MAX && item.strokes.isNotEmpty() && item.word.isNotBlank()) {
                            val word = item.word.trim()
                            val seconds = runCatching { wordDao.findByText(word) }.getOrNull()?.let { GameConstants.drawingDurationSeconds(it.difficulty) } ?: 0
                            out += RecentDrawing("${run.id}#$i", word, seconds, player?.nickname.orEmpty(), player?.level ?: run.level, player?.frameId.orEmpty(), item.strokes, correct = item.isCorrect)
                        }
                    }
                }
                if (out.size >= RECENT_MAX || p.endReached || p.nextCursor == null) break
                cursor = p.nextCursor
            }
            out.toList()
        }
        read.onSuccess { list ->
            withContext(Dispatchers.IO) { runCatching { realStore.saveRecent(list) } }
            updateReal { it.copy(recent = list, loadingRecent = false, recentError = if (list.isEmpty()) "Havuzda çizim bulunamadı." else null) }
        }.onFailure { e ->
            updateReal { it.copy(loadingRecent = false, recentError = "Okunamadı: ${e.message}") }
        }
    }
}

/** Moves or resizes the Karalak card on the video; the preview follows at once and the choice is kept on this phone. */
fun StudioViewModel.setCard(layout: CardLayout) {
    val c = layout.clamped()
    updateReal { it.copy(card = c) }
    _state.update { it.copy(video = null) }
    prefs().edit().putFloat(CARD_SCALE, c.scale).putFloat(CARD_X, c.x).putFloat(CARD_Y, c.y).apply()
    refreshPreview()
}

fun StudioViewModel.resetCard() = setCard(CardLayout())

/** Clears the choice and every picture and clip, after asking when there are any. */
fun StudioViewModel.resetReal(confirmed: Boolean = false) {
    val current = _state.value.real.project ?: return
    if (!confirmed && current.media.isNotEmpty()) { updateReal { it.copy(confirmReset = true) }; return }
    updateReal { it.copy(project = null, confirmReset = false, writeError = null, step = 1, voiceStep = 1) }
    prefs().edit().putInt(STEP, 1).putInt(VOICE_STEP, 1).apply()
    _state.update { it.copy(video = null, previewMs = 0L) }
    val kept = _state.value.real.library.flatMap { it.media }.toSet()
    viewModelScope.launch {
        withContext(Dispatchers.IO) { runCatching { realStore.save(null); realStore.prune(kept) } }
        refreshPreview()
    }
}

fun StudioViewModel.cancelResetReal() = updateReal { it.copy(confirmReset = false) }

/** Puts a drawing in the video, or takes it out (with its picture and clip); at most [MAX_REVEALS]. */
fun StudioViewModel.toggleReveal(drawing: RecentDrawing) {
    if (_state.value.real.project == null) updateReal { it.copy(project = RealProject()) }
    val p = _state.value.real.project ?: return
    val has = p.reveals.any { it.sampleId == drawing.id }
    if (!has && p.reveals.size >= MAX_REVEALS) { _state.update { it.copy(message = "En fazla $MAX_REVEALS çizim") }; return }
    updateProject { proj ->
        if (has) proj.copy(reveals = proj.reveals.filterNot { it.sampleId == drawing.id }, drawings = proj.drawings.filterNot { it.id == drawing.id })
        else proj.copy(
            reveals = proj.reveals + RealReveal(drawing.id),
            drawings = proj.drawings + RealDrawing(drawing.id, drawing.strokes, drawing.word, drawing.seconds, drawing.nickname, drawing.level, drawing.frameId)
        )
    }
    pruneRealMedia()
}

internal fun StudioViewModel.updateDrawing(id: String, change: (RealDrawing) -> RealDrawing) =
    updateProject { p -> p.copy(drawings = p.drawings.map { if (it.id == id) change(it) else it }) }

/** The name on the card; empty makes the player anonymous. A leading @ is dropped (the card adds its own). */
fun StudioViewModel.setPlayerName(id: String, text: String) = updateDrawing(id) { it.copy(nickname = text.trimStart('@', ' ').take(30)) }

/**
 * How many seconds the drawing is said to have taken, by hand: the game's own figure is often longer than a quick
 * drawing really takes, and a shorter time makes the video more exciting. The countdown, the lines the assistant
 * writes, their voices and the picture prompt all read it from the drawing, so this is the one place it is set.
 */
fun StudioViewModel.setDrawingSeconds(id: String, seconds: Int) {
    val secs = seconds.coerceIn(1, 99)
    val p = _state.value.real.project ?: return
    if (p.drawing(id)?.seconds == secs) return
    updateDrawing(id) { it.copy(seconds = secs) }
    val written = p.reveals.firstOrNull { it.sampleId == id }?.let { it.hook.text.isNotBlank() || it.narration.text.isNotBlank() || it.outro.text.isNotBlank() } == true
    if (written) _state.update { it.copy(message = "Süre $secs sn oldu: kanca ve kapanışı yeniden yazdırıp seslendir.") }
}

/**
 * The level shown for the player, by hand: it is written on the card beside the name, and the player's frame follows
 * it while the frame is left on "Oto". Lets a video show a level the player does not have.
 */
fun StudioViewModel.setPlayerLevel(id: String, level: Int) = updateDrawing(id) { it.copy(level = level.coerceIn(1, 999)) }

fun StudioViewModel.setPlayerFrame(id: String, frameName: String?) = updateDrawing(id) { it.copy(frameId = frameName.orEmpty()) }

/** Brings a picture from the gallery for the card's round slot. */
fun StudioViewModel.importPlayerPhoto(id: String, uri: Uri) {
    if (_state.value.real.busy != null) return
    updateReal { it.copy(busy = id) }
    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val name = realStore.import(uri, id + "_pfp", realStore.extensionOf(uri, video = false))
                RealImages.load(realStore.file(name), maxSide = 512) ?: run { realStore.file(name).delete(); error("Görsel açılamadı") }
                name
            }
        }
        updateReal { it.copy(busy = null) }
        result.onSuccess { name ->
            updateDrawing(id) { it.copy(photo = name) }
            pruneRealMedia()
        }.onFailure { e -> _state.update { it.copy(message = "Fotoğraf eklenemedi: ${e.message}") } }
    }
}

fun StudioViewModel.clearPlayerPhoto(id: String) {
    updateDrawing(id) { it.copy(photo = null) }
    pruneRealMedia()
}

fun StudioViewModel.setRevealDescribe(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(describe = text.take(800)) }
fun StudioViewModel.setRevealMotion(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(motion = text.take(1800)) }
fun StudioViewModel.setRevealKind(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(kind = text.take(40)) }
fun StudioViewModel.setRevealParts(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(parts = text.take(1000)) }
fun StudioViewModel.setRevealMaterial(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(material = text.take(400)) }
fun StudioViewModel.setRevealScene(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(scene = text.take(400)) }
fun StudioViewModel.setRevealSize(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(size = text.take(200)) }
fun StudioViewModel.setRevealScaleHint(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(scaleHint = text.take(200)) }
fun StudioViewModel.setRevealFixed(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(fixed = text.take(400)) }

/** The scene ideas are being written for this drawing (null: none). */
internal fun StudioViewModel.setPitching(sampleId: String?, error: String? = null) = updateReal { it.copy(pitching = sampleId, pitchError = error) }

/**
 * With the real picture in front of it, the assistant thinks up the video scene: three ideas with a joke each, all in one
 * shot and one place, the thing never changing. The picture is needed (it is what the scene is written for).
 */
fun StudioViewModel.writePitches(sampleId: String) {
    val s = _state.value
    val p = s.real.project ?: return
    if (s.real.pitching != null || s.real.writing) return
    val reveal = p.reveals.find { it.sampleId == sampleId } ?: return
    val image = reveal.image
    if (image == null) { setPitching(null, "Önce görseli ekle: sahne, fotoğrafa bakılarak yazılır."); return }
    val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
    if (key.isBlank()) { setPitching(null, "Önce Asistan ayarlarından OpenRouter anahtarını gir."); return }
    val word = p.drawing(sampleId)?.word.orEmpty()
    setPitching(sampleId)
    viewModelScope.launch {
        val png = withContext(Dispatchers.IO) {
            RealImages.load(realStore.file(image), maxSide = 1024)?.let { bmp ->
                java.io.ByteArrayOutputStream().also { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            }
        }
        if (png == null) { setPitching(null, "Görsel açılamadı"); return@launch }
        RealDirector(OpenRouterClient(key), analysisModel(), _state.value.lang).pitches(word, reveal, png).onSuccess { w ->
            updateReveal(sampleId) { it.copy(pitches = w.pitches) }
            updateReal { it.copy(pitching = null, pitchError = null) }
            _state.update { it.copy(aiCost = w.costUsd, aiModel = w.model) }
        }.onFailure { e -> setPitching(null, e.message ?: "Yazılamadı") }
    }
}

/** Takes one of the written ideas as the video's scene (it can still be edited by hand). */
fun StudioViewModel.choosePitch(sampleId: String, index: Int) = updateReveal(sampleId) { r ->
    r.pitches.getOrNull(index)?.let { r.copy(motion = it.scene.take(1800)) } ?: r
}

/**
 * Lets the assistant describe the chosen drawings (their shapes, a detailed scene for each video)
 * and write the post text. Optional: the same fields can be filled in by hand.
 */
fun StudioViewModel.describeReal() {
    val s = _state.value
    val p = s.real.project ?: return
    if (s.real.writing) return
    val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
    if (key.isBlank()) { updateReal { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
    val model = analysisModel()
    val candidates = p.reveals.mapNotNull { r -> p.drawing(r.sampleId) }
    if (candidates.isEmpty()) { updateReal { it.copy(writeError = "Önce en az bir çizim seç") }; return }
    updateReal { it.copy(writing = true, writeError = null) }
    viewModelScope.launch {
        RealDirector(OpenRouterClient(key), model, _state.value.lang).describe(candidates).onSuccess { w ->
            updateReal { it.copy(writing = false) }
            val byId = w.reveals.associateBy { it.sampleId }
            updateProject { proj ->
                proj.copy(reveals = proj.reveals.map { r -> byId[r.sampleId]?.let { d -> r.copy(describe = d.describe, motion = d.motion, kind = d.kind, parts = d.parts, material = d.material, scene = d.scene, size = d.size, scaleHint = d.scaleHint, fixed = d.fixed) } ?: r })
            }
            _state.update { it.copy(caption = w.caption.ifBlank { it.caption }, hashtags = w.hashtags.ifBlank { it.hashtags }, aiCost = w.costUsd, aiModel = w.model) }
        }.onFailure { e -> updateReal { it.copy(writing = false, writeError = e.message ?: "Yazılamadı") } }
    }
}

internal fun StudioViewModel.updateLine(sampleId: String, kind: RealLine, saveNow: Boolean = false, change: (VoicedLine) -> VoicedLine) =
    updateReveal(sampleId, saveNow) { r -> r.withLine(kind, change(r.line(kind))) }

internal fun StudioViewModel.lineError(kind: RealLine, message: String) = updateReal { it.copy(lineErrors = it.lineErrors + (kind to message)) }

/** Moves the preview to where [kind] shows: the very start for the hook, the closing for the closing. */
/** Moves the preview to where the voice step being set shows. */
internal fun StudioViewModel.previewVoiceStep(n: Int) = when (n) {
    1 -> previewLine(RealLine.Hook)
    2 -> previewLine(RealLine.Narration)
    3 -> previewLine(RealLine.Outro)
    5 -> previewLine(RealLine.End)
    else -> setPreviewTime(0L)
}

internal fun StudioViewModel.previewLine(kind: RealLine) {
    val events = _state.value.real.project?.featured?.let { RealTimeline.build(it, 0L, it.endCardMs) }.orEmpty()
    val outro = events.lastOrNull { it.phase == RealPhase.Outro }
    val clip = events.firstOrNull { it.phase == RealPhase.Clip }
    val card = events.lastOrNull { it.phase == RealPhase.EndCard }
    setPreviewTime(
        when {
            kind == RealLine.Narration && clip != null -> clip.startMs + RealTimeline.NARRATION_AT + 900L
            kind == RealLine.Outro && outro != null -> outro.startMs + 400L
            kind == RealLine.End && card != null -> card.startMs + 600L
            else -> 0L
        }
    )
}

/**
 * Has the assistant write ideas for one of Karalak's lines in the video's drawing: [RealCopy.HOOK_COUNT] hooks, or
 * [RealCopy.OUTRO_COUNT] closings. The first is taken while no line is set, so the preview shows it at once; the
 * line already in the video stays on the list, with its voice.
 */
fun StudioViewModel.writeLines(kind: RealLine) {
    val s = _state.value
    val p = s.real.project ?: return
    if (s.real.lineWriting != null) return
    val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
    if (key.isBlank()) { lineError(kind, "Önce Asistan ayarlarından OpenRouter anahtarını gir."); return }
    // the lines are the video's copy: written by the strong model that also reads the drawings, not the cheap default
    val model = analysisModel()
    val reveal = p.opening ?: run { lineError(kind, "Önce en az bir çizim seç"); return }
    val drawing = p.drawing(reveal.sampleId) ?: return
    updateReal { it.copy(lineWriting = kind, lineErrors = it.lineErrors - kind) }
    viewModelScope.launch {
        RealDirector(OpenRouterClient(key), model, _state.value.lang).lines(kind, drawing, reveal).onSuccess { w ->
            updateReal { it.copy(lineWriting = null) }
            _state.update { it.copy(aiCost = w.costUsd) }
            updateLine(reveal.sampleId, kind) { l ->
                val kept = listOfNotNull(l.text.takeIf { it.isNotBlank() && it !in w.lines })
                l.copy(options = kept + w.lines).let { if (it.text.isBlank()) it.withText(w.lines.first()) else it }
            }
            previewLine(kind)
        }.onFailure { e -> updateReal { it.copy(lineWriting = null, lineErrors = it.lineErrors + (kind to (e.message ?: "Yazılamadı"))) } }
    }
}

/** Takes one of the ideas as the line in the video, silent until its voice is added, and shows it in the preview. */
fun StudioViewModel.chooseLine(kind: RealLine, text: String) {
    val id = _state.value.real.project?.opening?.sampleId ?: return
    updateLine(id, kind) { it.withText(text) }
    previewLine(kind)
}

/** No hook (the video starts on the clip) or no closing (it ends with the clip). */
fun StudioViewModel.clearLine(kind: RealLine) {
    val id = _state.value.real.project?.opening?.sampleId ?: return
    updateLine(id, kind) { it.withText("") }
}

/**
 * Rewrites one idea by hand; emptied, it leaves the list. When it is the line in the video, the video takes the new
 * words, and its voice goes with the old ones.
 */
fun StudioViewModel.editLineOption(kind: RealLine, old: String, text: String) {
    val new = text.trim().take(RealCopy.maxChars(kind))
    val id = _state.value.real.project?.opening?.sampleId ?: return
    updateLine(id, kind) { l ->
        if (old !in l.options || new == old) return@updateLine l
        if (new.isNotEmpty() && new in l.options) return@updateLine l
        val options = if (new.isEmpty()) l.options - old else l.options.map { if (it == old) new else it }
        val next = l.copy(options = options)
        if (l.text == old) next.withText(new) else next
    }
}

/** A line of one's own, put at the top of the list to edit, voice and add like the assistant's. */
fun StudioViewModel.addLineOption(kind: RealLine, text: String) {
    val line = text.trim().take(RealCopy.maxChars(kind))
    val id = _state.value.real.project?.opening?.sampleId ?: return
    if (line.isEmpty()) return
    updateLine(id, kind) { l -> if (line in l.options) l else l.copy(options = listOf(line) + l.options) }
}

/**
 * Voices one line in ElevenLabs with the current voice settings and plays it. Credits are spent only here, on the
 * person's tap, and once per line and settings: the voice is kept on this phone.
 */
fun StudioViewModel.voiceLine(kind: RealLine, text: String) {
    if (_state.value.real.lineVoicing != null || text.isBlank()) return
    val client = elevenClient() ?: run { lineError(kind, "Önce Ayarlar sekmesinde ElevenLabs anahtarını gir."); return }
    val spec = _state.value.eleven.spec
    updateReal { it.copy(lineVoicing = kind to text, lineErrors = it.lineErrors - kind) }
    viewModelScope.launch {
        voiceBank.file(client, RealCopy.spoken(kind, text), spec)
            .onSuccess { f ->
                // a closing line is paid for once and kept for good (see VoiceBank.keep)
                if (kind == RealLine.End) withContext(Dispatchers.IO) { voiceBank.keep(RealCopy.spoken(kind, text), spec) }
                play(f)
            }
            .onFailure { e -> lineError(kind, e.message ?: "Seslendirilemedi") }
        updateReal { it.copy(lineVoicing = null) }
        refreshVoicePlan()
        refreshEleven()
    }
}

/** Plays a line's voice: the one in the video for the line in the video, otherwise the one just made. */
fun StudioViewModel.listenLine(kind: RealLine, text: String) {
    val l = _state.value.real.project?.opening?.line(kind)
    val inVideo = l?.takeIf { it.text == text }?.voice?.let { realStore.file(it) }?.takeIf { it.exists() }
    (inVideo ?: voiceBank.cached(RealCopy.spoken(kind, text), _state.value.eleven.spec))?.let { play(it) }
}

/**
 * Puts a line into the video with its voice: the voice is copied into the project (so changing the voice settings
 * later does not take it away), and the opening or the closing lasts until it has been said.
 */
fun StudioViewModel.addLineToVideo(kind: RealLine, text: String) {
    val id = _state.value.real.project?.opening?.sampleId ?: return
    val spec = _state.value.eleven.spec
    val voice = voiceBank.cached(RealCopy.spoken(kind, text), spec) ?: run {
        _state.update { it.copy(message = "Önce seslendir") }
        return
    }
    // when each word is said, when ElevenLabs told: the subtitle comes in with the voice (kept with the line, like its voice)
    val words = voiceBank.timing(RealCopy.spoken(kind, text), spec)?.words.orEmpty().map { TimedWord(it.text, it.startMs, it.endMs) }
    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val ms = ClipAudio.decode(voice, VOICE_MAX_MS)?.durationMs ?: error("Ses çözülemedi")
                if (ms <= 0L) error("Ses boş")
                realStore.copyIn(voice, when (kind) { RealLine.Hook -> "hook_voice"; RealLine.Narration -> "narration_voice"; RealLine.Outro -> "outro_voice"; RealLine.End -> "end_voice" }, "mp3") to ms
            }
        }
        result.onSuccess { (name, ms) ->
            _state.update { it.copy(message = when (kind) { RealLine.Hook -> "Kanca sesi videoya eklendi"; RealLine.Narration -> "Anlatıcı sesi videoya eklendi"; RealLine.Outro -> "Kapanış sesi videoya eklendi"; RealLine.End -> "Kapanış görseli sesi videoya eklendi" }) }
            updateLine(id, kind, saveNow = true) { it.withText(text).copy(voice = name, voiceMs = ms, words = words) }
            pruneRealMedia()
            previewLine(kind)
        }.onFailure { e -> _state.update { it.copy(message = "Ses eklenemedi: ${e.message}") } }
    }
}

/** The app's music under the video of the drawing it is made of; null: none. */
fun StudioViewModel.setMusic(name: String?) {
    val id = _state.value.real.project?.opening?.sampleId ?: return
    updateReveal(id) { it.copy(music = name) }
}

fun StudioViewModel.setMusicVolume(volume: Float) {
    val id = _state.value.real.project?.opening?.sampleId ?: return
    updateReveal(id) { it.copy(musicVolume = volume.coerceIn(0f, RealMix.MAX_VOLUME)) }
}

/** Plays a track on the speaker to choose by; the same one again stops it. */
fun StudioViewModel.listenMusic(name: String) {
    if (_state.value.real.musicPlaying == name) { stopPlaying(); return }
    viewModelScope.launch {
        val file = withContext(Dispatchers.IO) { runCatching { RealMusic.ensure(context, name) }.getOrNull() }
            ?: run { _state.update { it.copy(message = "Müzik açılamadı") }; return@launch }
        play(file) { updateReal { it.copy(musicPlaying = null) } }
        updateReal { it.copy(musicPlaying = name) }
    }
}

fun StudioViewModel.stopPlaying() {
    player?.release()
    player = null
    updateReal { it.copy(musicPlaying = null) }
}

/** Makes a drawing the one the video is made of (the first with a clip); its own hook and closing come with it. */
fun StudioViewModel.moveRevealFirst(sampleId: String) {
    _state.update { it.copy(previewMs = 0L) }
    updateProject { p ->
        val r = p.reveals.find { it.sampleId == sampleId } ?: return@updateProject p
        p.copy(reveals = listOf(r) + p.reveals.filterNot { it.sampleId == sampleId })
    }
}

/** Draws a chosen drawing large on white for ChatGPT, with the prompt that keeps its oddities. */
fun StudioViewModel.shareDoodle(sampleId: String) {
    val p = _state.value.real.project ?: return
    val d = p.drawing(sampleId) ?: return
    val reveal = p.reveals.find { it.sampleId == sampleId }
    viewModelScope.launch {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "studio_share").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                File(dir, "karalak_cizim_${System.currentTimeMillis()}.png").apply { writeBytes(StrokeReplay.png(d.strokes, 1f, 1024)) }
            }.getOrNull()
        } ?: run { _state.update { it.copy(message = "Çizim hazırlanamadı") }; return@launch }
        val prompt = RealCopy.imagePrompt(d.word, d.seconds.coerceAtLeast(1), reveal)
        updateReal { it.copy(share = RealShare(file, prompt)) }
    }
}

fun StudioViewModel.clearShare() = updateReal { it.copy(share = null) }

/**
 * Hands the realistic picture to Gemini with the video prompt. The picture has to travel with the
 * prompt: asked in words alone Gemini invents its own creature instead of moving this one.
 */
fun StudioViewModel.shareToGemini(sampleId: String) {
    val p = _state.value.real.project ?: return
    val reveal = p.reveals.find { it.sampleId == sampleId } ?: return
    val image = reveal.image ?: run { _state.update { it.copy(message = "Önce ChatGPT görselini ekle") }; return }
    viewModelScope.launch {
        val file = withContext(Dispatchers.IO) {
            runCatching { copyToShare(realStore.file(image), "karalak_gercek_${System.currentTimeMillis()}", clear = true) }.getOrNull()
        } ?: run { _state.update { it.copy(message = "Görsel hazırlanamadı") }; return@launch }
        updateReal { it.copy(share = RealShare(file, RealCopy.videoPrompt(p.drawing(sampleId)?.word.orEmpty(), reveal), ShareTarget.Gemini)) }
    }
}

/** Every drawing that has its picture, each followed by that picture, for one swipe-through post. */
fun StudioViewModel.shareCarousel() {
    val p = _state.value.real.project ?: return
    val made = p.reveals.filter { it.image != null && p.drawing(it.sampleId) != null }
    if (made.isEmpty()) { _state.update { it.copy(message = "Önce görselleri ekle") }; return }
    viewModelScope.launch {
        val files = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "studio_share").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                buildList {
                    made.forEachIndexed { i, r ->
                        add(File(dir, "karalak_${i + 1}a_cizim.png").apply { writeBytes(StrokeReplay.png(p.drawing(r.sampleId)!!.strokes, 1f, 1080)) })
                        add(copyToShare(realStore.file(r.image!!), "karalak_${i + 1}b_gercek", clear = false))
                    }
                }
            }.getOrNull()
        } ?: run { _state.update { it.copy(message = "Görseller hazırlanamadı") }; return@launch }
        updateReal { it.copy(carousel = files) }
    }
}

fun StudioViewModel.clearCarousel() = updateReal { it.copy(carousel = null) }

/** A copy of [source] in the folder the share sheet may read from. */
internal fun StudioViewModel.copyToShare(source: File, name: String, clear: Boolean): File {
    val dir = File(context.cacheDir, "studio_share").apply { mkdirs() }
    if (clear) dir.listFiles()?.forEach { it.delete() }
    return File(dir, name + "." + source.extension.ifBlank { "jpg" }).also { source.copyTo(it, overwrite = true) }
}

fun StudioViewModel.imagePrompt(sampleId: String): String? {
    val p = _state.value.real.project ?: return null
    val d = p.drawing(sampleId) ?: return null
    val reveal = p.reveals.find { it.sampleId == sampleId }
    return RealCopy.imagePrompt(d.word, d.seconds.coerceAtLeast(1), reveal)
}

fun StudioViewModel.videoPrompt(sampleId: String): String? {
    val p = _state.value.real.project ?: return null
    val reveal = p.reveals.find { it.sampleId == sampleId } ?: return null
    return RealCopy.videoPrompt(p.drawing(sampleId)?.word.orEmpty(), reveal)
}

fun StudioViewModel.mediaFile(name: String): File = realStore.file(name)

/** Brings the realistic picture made in ChatGPT into the project. */
fun StudioViewModel.importImage(sampleId: String, uri: Uri) {
    if (_state.value.real.busy != null) return
    updateReal { it.copy(busy = sampleId) }
    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val name = realStore.import(uri, sampleId + "_img", realStore.extensionOf(uri, video = false))
                // opened once to be sure it is a picture, then let go
                RealImages.load(realStore.file(name)) ?: run { realStore.file(name).delete(); error("Görsel açılamadı") }
                name
            }
        }
        updateReal { it.copy(busy = null) }
        result.onSuccess { name ->
            updateProject(saveNow = true) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(image = name) else it }) }
            pruneRealMedia()
        }.onFailure { e -> _state.update { it.copy(message = "Görsel eklenemedi: ${e.message}") } }
    }
}

/**
 * How much of the clip is used, by hand: a video model keeps the odd shape for the first seconds and then drifts back
 * to an ordinary one, so the clip can be cut where it is still right. The video's end then holds its last frame.
 */
fun StudioViewModel.setClipSeconds(sampleId: String, seconds: Double) {
    val r = _state.value.real.project?.reveals?.firstOrNull { it.sampleId == sampleId } ?: return
    val file = r.clip?.let { realStore.file(it) } ?: return
    viewModelScope.launch {
        val full = withContext(Dispatchers.IO) { RealImages.clipMs(file) }
        val ms = (seconds * 1000).toLong().coerceIn(RealTimeline.CLIP_MIN, maxOf(RealTimeline.CLIP_MIN, minOf(full, RealTimeline.CLIP_MAX)))
        updateProject { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(clipMs = ms) else it }) }
    }
}

/** Brings the clip made in Gemini into the project; only its first [RealTimeline.CLIP_MAX] ms are used. */
fun StudioViewModel.importClip(sampleId: String, uri: Uri) {
    if (_state.value.real.busy != null) return
    updateReal { it.copy(busy = sampleId) }
    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val name = realStore.import(uri, sampleId + "_clip", realStore.extensionOf(uri, video = true))
                val ms = RealImages.clipMs(realStore.file(name))
                if (ms <= 0L) { realStore.file(name).delete(); error("Video okunamadı") }
                name to ms
            }
        }
        updateReal { it.copy(busy = null) }
        result.onSuccess { (name, ms) ->
            updateProject(saveNow = true) { p ->
                p.copy(reveals = p.reveals.map {
                    if (it.sampleId == sampleId) it.copy(clip = name, clipMs = ms.coerceAtMost(RealTimeline.CLIP_MAX)) else it
                })
            }
            pruneRealMedia()
        }.onFailure { e -> _state.update { it.copy(message = "Video eklenemedi: ${e.message}") } }
    }
}

fun StudioViewModel.removeImage(sampleId: String) {
    updateProject(saveNow = true) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(image = null) else it }) }
    pruneRealMedia()
}

fun StudioViewModel.removeClip(sampleId: String) {
    updateProject(saveNow = true) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(clip = null, clipMs = 0L) else it }) }
    pruneRealMedia()
}

/** Opens the project's clips once for the preview; lets go of the ones it no longer uses. */
internal fun StudioViewModel.loadRealMedia(p: RealProject) {
    val wanted = p.media
    previewClips.keys.filter { it !in wanted }.forEach { k -> previewClips.remove(k)?.close() }
    val missing = p.reveals.mapNotNull { r -> r.clip?.takeIf { !previewClips.containsKey(it) } }
    if (missing.isEmpty()) return
    viewModelScope.launch {
        withContext(Dispatchers.IO) {
            missing.forEach { name ->
                runCatching { ClipFrames(realStore.file(name), exact = false) }.getOrNull()?.let { if (previewClips.putIfAbsent(name, it) != null) it.close() }
            }
        }
        refreshPreview()
    }
}

/** Lets go of imported files nothing uses any more: not the project's, and never the library's. */
internal fun StudioViewModel.pruneRealMedia() {
    val r = _state.value.real
    val keep = (r.project?.media ?: emptySet()) + r.library.flatMap { it.media }
    viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.prune(keep) } }
}

// ---- library

fun StudioViewModel.toggleLibrary() = updateReal { it.copy(libraryShown = !it.libraryShown) }

fun StudioViewModel.openLibraryItem(id: String?) = updateReal { it.copy(libraryView = id) }

/** Ticks a piece to be part of the next video, or unticks it; at most [MAX_REVEALS], and only pieces with a clip. */
fun StudioViewModel.toggleLibrarySelect(id: String) {
    val r = _state.value.real
    val item = r.library.find { it.id == id } ?: return
    if (id in r.librarySelected) { updateReal { it.copy(librarySelected = it.librarySelected - id) }; return }
    if (!item.hasClip) { _state.update { it.copy(message = "Bu kaydın videosu yok; yalnızca videolular birleşir") }; return }
    if (r.librarySelected.size >= MAX_REVEALS) { _state.update { it.copy(message = "En fazla $MAX_REVEALS video seçilir") }; return }
    updateReal { it.copy(librarySelected = it.librarySelected + id) }
}

/** Brings the ticked pieces back as the project, in the order they were ticked, each with its card, lines and voices; the video is the first one's. */
fun StudioViewModel.libraryMakeVideo() {
    val r = _state.value.real
    val items = r.librarySelected.mapNotNull { id -> r.library.find { it.id == id } }.filter { it.hasClip }
    if (items.isEmpty()) { _state.update { it.copy(message = "Önce videosu olan kayıtları seç") }; return }
    val project = RealProject(drawings = items.map { it.drawing }, reveals = items.map { it.reveal })
    updateReal { it.copy(project = project, librarySelected = emptyList(), libraryShown = false, tab = RealTab.Prepare, step = 7) }
    prefs().edit().putString(TAB, RealTab.Prepare.name).putInt(STEP, 7).apply()
    _state.update { it.copy(video = null, previewMs = 0L, message = if (items.size == 1) "Hazır; aşağıdan videoyu oluştur" else "${items.size} kayıt alındı; video en üsttekiyle yapılır (Başa al ile değiştir)") }
    viewModelScope.launch { withContext(Dispatchers.IO) { runCatching { realStore.save(project) } } }
    loadRealMedia(project)
    refreshPreview()
}

/** Changes one kept piece and keeps the library on disk up to date. */
internal fun StudioViewModel.updateLibraryItem(id: String, change: (LibraryItem) -> LibraryItem) {
    val library = _state.value.real.library.map { if (it.id == id) change(it) else it }
    updateReal { it.copy(library = library) }
    viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.saveLibrary(library) } }
}

/**
 * Notes that the video of the featured drawing is being posted on the chosen platform, with the hook it carries and
 * how it opens, so its numbers can be typed in later (see [VideoPost]). A video shared again to the same place within
 * half an hour is the same posting.
 */
fun StudioViewModel.logPost() {
    val r = _state.value.real
    val f = r.project?.featured ?: return
    val item = r.library.find { it.id == f.sampleId } ?: return
    val now = System.currentTimeMillis()
    if (item.posts.any { it.platform == r.platform.name && now - it.postedAt < POST_SAME_MS }) return
    val post = VideoPost(java.util.UUID.randomUUID().toString(), r.platform.name, now, f.hook.text.trim(), f.opening.name)
    updateLibraryItem(item.id) { it.copy(posts = listOf(post) + it.posts) }
}

/** Types in (or corrects) a posting's result. */
fun StudioViewModel.updatePost(itemId: String, postId: String, change: (VideoPost) -> VideoPost) =
    updateLibraryItem(itemId) { item -> item.copy(posts = item.posts.map { if (it.id == postId) change(it) else it }) }

fun StudioViewModel.deletePost(itemId: String, postId: String) =
    updateLibraryItem(itemId) { item -> item.copy(posts = item.posts.filterNot { it.id == postId }) }

/** Takes a piece out of the library and deletes its files, unless the current video still uses them. */
fun StudioViewModel.deleteLibraryItem(id: String) {
    val library = _state.value.real.library.filterNot { it.id == id }
    updateReal { it.copy(library = library, librarySelected = it.librarySelected - id, libraryView = null, libraryBytes = realStore.sizeOf(library.flatMap { i -> i.media })) }
    viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.saveLibrary(library) } }
    pruneRealMedia()
}

fun StudioViewModel.libraryFile(name: String): File = realStore.file(name)

/** What one piece's files take on this phone. */
fun StudioViewModel.libraryBytesOf(item: LibraryItem): Long = realStore.sizeOf(item.media)
