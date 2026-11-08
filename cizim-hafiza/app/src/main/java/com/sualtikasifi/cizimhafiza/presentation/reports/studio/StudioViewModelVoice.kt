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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.EL_KEY
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.EL_VOICE
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.EL_MODEL
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.EL_STABILITY
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.EL_TAGS
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.STUDIO_LANG
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioViewModel.Companion.VOICE_ENGINE

// Karalak's voice (the phone's, or ElevenLabs): settings, the bank of voiced lines, listening, as extensions of [StudioViewModel].

// ---- voice

fun StudioViewModel.setVoiceOn(on: Boolean) { _state.update { it.copy(voiceOn = on, video = null) } }
fun StudioViewModel.setVoice(pitch: Float, rate: Float) { _state.update { it.copy(voicePitch = pitch, voiceRate = rate, video = null) } }

internal fun StudioViewModel.elevenKey(): String = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), EL_KEY).trim()
internal fun StudioViewModel.elevenClient(): ElevenLabs? = elevenKey().takeIf { it.isNotBlank() }?.let { ElevenLabs(it) }
internal fun StudioViewModel.updateEleven(change: (ElevenUi) -> ElevenUi) = _state.update { it.copy(eleven = change(it.eleven)) }

/** The voice settings kept on this phone; ElevenLabs is the default once a key is in. */
internal fun StudioViewModel.loadVoiceSettings() {
    val p = prefs()
    val key = elevenKey()
    val engine = p.getString(VOICE_ENGINE, null)?.let { runCatching { VoiceEngine.valueOf(it) }.getOrNull() }
        ?: if (key.isNotBlank()) VoiceEngine.ElevenLabs else VoiceEngine.Phone
    _state.update {
        it.copy(
            voiceEngine = engine,
            eleven = ElevenUi(
                keySet = key.isNotBlank(),
                keyHint = key.takeLast(4),
                voiceId = p.getString(EL_VOICE, null)?.takeIf { v -> v.isNotBlank() } ?: ElevenLabs.PRESETS.first().id,
                model = VoiceModel.of(p.getString(EL_MODEL, null)),
                stability = p.getFloat(EL_STABILITY, 0.5f),
                tags = p.getBoolean(EL_TAGS, true),
                lang = StudioLang.of(p.getString(STUDIO_LANG, null))
            )
        )
    }
    if (engine == VoiceEngine.ElevenLabs && key.isNotBlank()) refreshEleven()
}

fun StudioViewModel.setVoiceEngine(engine: VoiceEngine) {
    prefs().edit().putString(VOICE_ENGINE, engine.name).apply()
    _state.update { it.copy(voiceEngine = engine, video = null, voiceError = null) }
    if (engine == VoiceEngine.ElevenLabs) refreshEleven()
}

/** Keeps the key on this phone only (never in the app, the backup or the database), then checks it. */
fun StudioViewModel.setElevenKey(raw: String) {
    val key = raw.trim()
    com.sualtikasifi.cizimhafiza.util.SecretStore.put(prefs(), EL_KEY, key)
    updateEleven { it.copy(keySet = key.isNotBlank(), keyHint = key.takeLast(4), voices = emptyList(), status = null) }
    if (key.isNotBlank()) refreshEleven()
}

fun StudioViewModel.setElevenVoice(id: String) {
    val v = id.trim()
    prefs().edit().putString(EL_VOICE, v).apply()
    updateEleven { it.copy(voiceId = v) }
    _state.update { it.copy(video = null) }
    refreshVoicePlan()
}

fun StudioViewModel.setElevenModel(model: VoiceModel) {
    prefs().edit().putString(EL_MODEL, model.id).apply()
    updateEleven { it.copy(model = model) }
    _state.update { it.copy(video = null) }
    refreshVoicePlan()
}

fun StudioViewModel.setElevenStability(v: Float) {
    prefs().edit().putFloat(EL_STABILITY, v).apply()
    updateEleven { it.copy(stability = v) }
    _state.update { it.copy(video = null) }
    refreshVoicePlan()
}

/**
 * The language of the videos. The script already written stays as it is (rewrite it to have it in the new language);
 * a line voiced in another language is another file, so nothing already paid for is lost by switching back.
 */
fun StudioViewModel.setLang(lang: StudioLang) {
    prefs().edit().putString(STUDIO_LANG, lang.code).apply()
    updateEleven { it.copy(lang = lang) }
    // the phone's voice is opened for one language
    listener?.close()
    listener = null
    _state.update { it.copy(video = null) }
    refreshVoicePlan()
}

fun StudioViewModel.setElevenTags(on: Boolean) {
    prefs().edit().putBoolean(EL_TAGS, on).apply()
    updateEleven { it.copy(tags = on) }
    _state.update { it.copy(video = null) }
    refreshVoicePlan()
}

/** The account's voices and this month's credits, where the key is allowed to read them. */
fun StudioViewModel.refreshEleven() {
    val client = elevenClient() ?: return
    viewModelScope.launch {
        val voices = client.voices()
        val credits = client.credits()
        updateEleven {
            it.copy(
                voices = voices.getOrDefault(it.voices),
                status = listOfNotNull(
                    credits.getOrNull()?.let { c -> "Bu ay kalan: ${c.left} / ${c.limit} kredi" },
                    if (voices.isFailure && credits.isFailure) "Anahtar çalışıyor mu, bir satırı dinleyerek dene. Ses listesi ve kredi için anahtara “Voices: read” ve “User: read” izinlerini ver." else null
                ).joinToString("\n").ifBlank { null }
            )
        }
    }
}

internal fun StudioViewModel.play(file: File, onDone: () -> Unit = {}) {
    player?.release()
    // whatever played before is cut off, a music track included
    if (_state.value.real.musicPlaying != null) updateReal { it.copy(musicPlaying = null) }
    player = runCatching {
        MediaPlayer().apply {
            setDataSource(file.path)
            setOnCompletionListener { mp -> mp.release(); if (player === mp) { player = null; onDone() } }
            prepare()
            start()
        }
    }.getOrNull()
}

/** Plays one line on the speaker with the current voice: ElevenLabs (paid once, then kept) or the phone's. */
fun StudioViewModel.listen(line: SpokenLine) {
    if (line.text.isBlank()) return
    val s = _state.value
    if (s.voiceEngine == VoiceEngine.ElevenLabs) {
        // already paid for: play it; otherwise ask first — nothing is voiced without a yes
        voiceBank.cached(line, s.eleven.spec)?.let { play(it); return }
        updateEleven { it.copy(confirm = line) }
        return
    }
    speakOnPhone(line)
}

/** The person agreed to pay for hearing this line in ElevenLabs. */
fun StudioViewModel.confirmListen() {
    val line = _state.value.eleven.confirm ?: return
    updateEleven { it.copy(confirm = null) }
    val client = elevenClient() ?: run { _state.update { it.copy(voiceError = "Önce ElevenLabs anahtarını gir.") }; return }
    if (_state.value.eleven.busy) return
    updateEleven { it.copy(busy = true) }
    viewModelScope.launch {
        voiceBank.file(client, line, _state.value.eleven.spec)
            .onSuccess { f -> play(f); _state.update { it.copy(voiceError = null) } }
            .onFailure { err -> _state.update { it.copy(voiceError = err.message) } }
        updateEleven { it.copy(busy = false) }
        refreshVoicePlan()
        refreshEleven()
    }
}

/** Hear the line in the phone's free voice instead. */
fun StudioViewModel.listenFree() {
    val line = _state.value.eleven.confirm ?: return
    updateEleven { it.copy(confirm = null) }
    speakOnPhone(line)
}

fun StudioViewModel.cancelListen() = updateEleven { it.copy(confirm = null) }

/**
 * Voices every line of the current script that is not voiced yet with these settings — the only
 * way a whole script is ever sent to ElevenLabs, and only when the person presses the button.
 */
fun StudioViewModel.voiceAll() {
    val s = _state.value
    val lines = linesFor(s) ?: return
    val client = elevenClient() ?: run { _state.update { it.copy(voiceError = "Önce ElevenLabs anahtarını gir.") }; return }
    if (s.eleven.busy) return
    val spec = s.eleven.spec
    val todo = lines.filter { voiceBank.cached(it, spec) == null }.distinctBy { ElevenLabs.textFor(it, spec.model, spec.tags, spec.lang) }
    if (todo.isEmpty()) return
    updateEleven { it.copy(busy = true) }
    viewModelScope.launch {
        var failed: Throwable? = null
        for ((i, l) in todo.withIndex()) {
            updateEleven { it.copy(progress = "Seslendiriliyor ${i + 1}/${todo.size}") }
            voiceBank.file(client, l, spec).onFailure { failed = it }
            if (failed != null) break
        }
        updateEleven { it.copy(busy = false, progress = null) }
        _state.update { it.copy(voiceError = failed?.message, video = null) }
        refreshVoicePlan()
        refreshEleven()
    }
}

/** How much of the current script is voiced with the current settings; looks at the disk only. */
internal fun StudioViewModel.refreshVoicePlan(s: StudioState = _state.value) {
    val lines = linesFor(s).orEmpty().filter { it.text.isNotBlank() }
    val spec = s.eleven.spec
    val missing = lines.filter { voiceBank.cached(it, spec) == null }.distinctBy { ElevenLabs.textFor(it, spec.model, spec.tags, spec.lang) }
    updateEleven { it.copy(ready = lines.count { l -> voiceBank.cached(l, spec) != null }, missing = missing.size, missingCredits = missing.sumOf { l -> spec.credits(l) }) }
    val opening = s.real.project?.opening
    val voiced = RealLine.entries.associateWith { k ->
        opening?.line(k)?.options.orEmpty().filter { voiceBank.cached(RealCopy.spoken(k, it), spec) != null }.toSet()
    }
    if (voiced != _state.value.real.lineVoiced) updateReal { it.copy(lineVoiced = voiced) }
    refreshEndVoices(spec)
}

/** Counts the ready-made closing lines that are voiced, and keeps the voices of those that are (nothing is asked of ElevenLabs). */
internal fun StudioViewModel.refreshEndVoices(spec: VoiceSpec = _state.value.eleven.spec) {
    viewModelScope.launch {
        val (voiced, kept) = withContext(Dispatchers.IO) {
            val done = RealCopy.END_LINES.filter { voiceBank.cached(RealCopy.endLine(it), spec) != null }
            done.forEach { voiceBank.keep(RealCopy.endLine(it), spec) }
            done.size to voiceBank.keptCount()
        }
        updateReal { it.copy(endVoiced = voiced, endKept = kept) }
    }
}

/** The kept closing voices written to [uri] as one backup file, which stays when the app is uninstalled. */
fun StudioViewModel.exportEndVoices(uri: Uri) {
    viewModelScope.launch {
        val n = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { voiceBank.exportKept(it) } ?: error("Dosya açılamadı") }
        }
        _state.update { it.copy(message = n.fold({ c -> "$c kapanış sesi yedeklendi" }, { e -> "Yedeklenemedi: ${e.message}" })) }
    }
}

/** Kept closing voices read back from a backup file made by [exportEndVoices]. */
fun StudioViewModel.importEndVoices(uri: Uri) {
    viewModelScope.launch {
        val n = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { voiceBank.importKept(it) } ?: error("Dosya açılamadı") }
        }
        _state.update { it.copy(message = n.fold({ c -> if (c > 0) "$c kapanış sesi geri yüklendi" else "Dosyada kapanış sesi yok" }, { e -> "Yüklenemedi: ${e.message}" })) }
        refreshVoicePlan()
    }
}

/** A music file from the phone, added to the tracks to choose from; it is copied into the app and kept. */
fun StudioViewModel.addMusic(uri: Uri, onAdded: (String) -> Unit = { setMusic(it) }) {
    viewModelScope.launch {
        val r = withContext(Dispatchers.IO) { runCatching { RealMusic.import(context, uri) } }
        r.onSuccess { name ->
            updateReal { it.copy(customMusic = RealMusic.custom(context)) }
            // a track that was just added is meant to be used, and heard
            onAdded(name)
            _state.update { it.copy(message = "Müzik eklendi: ${RealMusic.label(name)}") }
        }.onFailure { e -> _state.update { it.copy(message = "Müzik eklenemedi: ${e.message}") } }
    }
}

/** Removes a track the person added; a video that used it goes back to no music. */
fun StudioViewModel.removeMusic(name: String) {
    viewModelScope.launch {
        if (_state.value.real.musicPlaying == name) stopPlaying()
        withContext(Dispatchers.IO) { runCatching { RealMusic.remove(context, name) } }
        if (_state.value.real.project?.featured?.music == name) setMusic(null)
        if (_state.value.forgot.music == name) setForgotMusic(null)
        updateReal { it.copy(customMusic = RealMusic.custom(context)) }
    }
}

internal fun StudioViewModel.speakOnPhone(line: SpokenLine) {
    viewModelScope.launch {
        val synth = listener ?: SpeechSynth.open(context, _state.value.lang).getOrElse { e ->
            _state.update { it.copy(voiceError = e.message) }
            return@launch
        }.also { listener = it }
        synth.tune(_state.value.voicePitch, _state.value.voiceRate)
        synth.speak(line.text)
        _state.update { it.copy(voiceError = null) }
    }
}
