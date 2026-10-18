package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.PlayerCard
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
import javax.inject.Inject

/** The studio's video formats. */
enum class StudioFormat(val title: String, val blurb: String) {
    Guess("Karalak Bilir mi?", "Karalak çizimleri canlı izleyip tahmin ediyor"),
    Memory("Hafıza Tuzağı", "Çizenlerin çoğu kendi çizimini tanıyamadı"),
    RealLife("Gerçek Olsaydı", "Çizimler gerçek hayata dönüşüyor")
}

/** Which spoken line of the script an edit is about. */
sealed interface LineRef {
    data object Intro : LineRef
    data object Outro : LineRef
    data class Stage(val round: Int, val stage: Int) : LineRef
    data class Reaction(val round: Int) : LineRef
}

data class StudioState(
    val format: StudioFormat = StudioFormat.Guess,
    val loading: Boolean = true,
    val drawings: List<StudioDrawing> = emptyList(),
    /** Chosen drawings, in the order they will play. */
    val selected: List<Long> = emptyList(),
    /** Off: the right-hand tile says "Oyuncu" with the mascot picture instead of this account's name and photo. */
    val showName: Boolean = true,
    val player: PlayerCard? = null,
    val mascotPoses: Int = 0,
    val mascotLoading: Boolean = true,
    val writing: Boolean = false,
    val writeProgress: String? = null,
    val writeError: String? = null,
    val aiCost: Double? = null,
    val script: GuessScript? = null,
    val caption: String = "",
    val hashtags: String = DEFAULT_TAGS,
    val voiceOn: Boolean = true,
    val voicePitch: Float = 1.2f,
    val voiceRate: Float = 1.05f,
    val voiceError: String? = null,
    val preview: ImageBitmap? = null,
    val previewMs: Long = 0L,
    val totalMs: Long = 0L,
    val rendering: Boolean = false,
    val renderStage: String = "",
    val progress: Float = 0f,
    val video: File? = null,
    val renderError: String? = null,
    val message: String? = null
) {
    val chosen: List<StudioDrawing> get() = selected.mapNotNull { id -> drawings.find { it.id == id } }
    val canWrite: Boolean get() = selected.isNotEmpty() && !writing
    val ready: Boolean get() = script != null && !rendering
}

private const val DEFAULT_TAGS = "#çizimoyunu #karalak #yapayzeka #oyunönerisi #çizim"
private const val MAX_DRAWINGS = 5

/**
 * The video studio. Its first format, "Karalak Bilir mi?": Karalak watches the player's drawings
 * build up and guesses out loud, on what looks like a video call. The assistant writes Karalak's
 * lines from the drawings themselves, the phone's Turkish voice says them, and the phone renders
 * the 1080×1920 video with sound.
 *
 * Only this phone's own drawings are offered — they are the maintainer's to post.
 */
@HiltViewModel
class StudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val drawingResultDao: DrawingResultDao,
    private val settings: SettingsRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private var assets: StudioAssets? = null
    private var sprites: MascotSprites? = null
    private var ownCard: PlayerCard? = null
    private var anonymousCard: PlayerCard? = null
    private var previewJob: Job? = null
    private var renderJob: Job? = null
    private var listener: SpeechSynth? = null
    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadDrawings()
        loadPlayer()
        loadMascot()
    }

    // ---- loading

    private fun loadDrawings() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                runCatching { drawingResultDao.getRecentWithWords(240) }.getOrDefault(emptyList()).mapNotNull { row ->
                    val strokes = runCatching { json.decodeFromString<List<List<DrawingPoint>>>(row.pathDataJson) }.getOrNull()
                        ?.filter { it.isNotEmpty() }
                    if (strokes.isNullOrEmpty()) null
                    else StudioDrawing(row.id, row.word, row.category, strokes, row.isCorrect, row.userAnswer)
                }
            }
            _state.update { it.copy(loading = false, drawings = list) }
        }
    }

    private fun loadPlayer() {
        viewModelScope.launch {
            val cards = withContext(Dispatchers.IO) {
                val level = settings.playerLevelNow().coerceAtLeast(1)
                val frame = AvatarFrame.resolve(settings.selectedAvatarFrameId.value, level)
                val frameBitmap = runCatching { BitmapFactory.decodeResource(context.resources, frame.drawableRes) }.getOrNull()
                val dino = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.avatar_dino) }.getOrNull()
                val url = (auth.authState.value as? AuthState.Linked)?.photoUrl
                val photo = if (settings.avatarSource.value != "DINO" && url != null && GOOGLE_PHOTO.matches(url)) download(url) else null
                val name = settings.nickname.value.trim().ifBlank { "Oyuncu" }
                val own = PlayerCard(name, level, frameBitmap, frame.faceDiameterFraction, frame.faceOffsetXFraction, frame.faceOffsetYFraction, photo ?: dino)
                own to own.copy(name = "Oyuncu", photo = dino)
            }
            ownCard = cards.first
            anonymousCard = cards.second
            _state.update { it.copy(player = if (it.showName) cards.first else cards.second) }
            refreshPreview()
        }
    }

    fun reloadMascot() = loadMascot()

    private fun loadMascot() {
        _state.update { it.copy(mascotLoading = true) }
        viewModelScope.launch {
            val loaded = MascotSprites.load(context)
            sprites = loaded
            _state.update { it.copy(mascotLoading = false, mascotPoses = loaded.count) }
            refreshPreview()
        }
    }

    private fun download(url: String): Bitmap? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8_000; readTimeout = 10_000 }
        try { conn.inputStream.use { BitmapFactory.decodeStream(it) } } finally { conn.disconnect() }
    }.getOrNull()

    // ---- choices

    fun setFormat(f: StudioFormat) { _state.update { it.copy(format = f) } }

    fun toggle(d: StudioDrawing) {
        _state.update { s ->
            val next = when {
                d.id in s.selected -> s.selected - d.id
                s.selected.size >= MAX_DRAWINGS -> return@update s.copy(message = "En fazla $MAX_DRAWINGS çizim; en iyisi 3")
                else -> s.selected + d.id
            }
            // a new set of drawings needs a new script
            s.copy(selected = next, script = null, video = null, renderError = null)
        }
        refreshPreview()
    }

    fun setShowName(show: Boolean) {
        _state.update { it.copy(showName = show, player = if (show) ownCard else anonymousCard, script = null, video = null) }
        refreshPreview()
    }

    fun setCaption(v: String) { _state.update { it.copy(caption = v.take(2_000)) } }
    fun setHashtags(v: String) { _state.update { it.copy(hashtags = v.take(300)) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }

    // ---- the script

    private fun prefs() = context.getSharedPreferences(AssistantViewModel.PREFS, Context.MODE_PRIVATE)

    private fun director(): GuessDirector? {
        val key = prefs().getString(AssistantViewModel.KEY, "").orEmpty()
        if (key.isBlank()) return null
        val model = prefs().getString(AssistantViewModel.MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL }
        return GuessDirector(OpenRouterClient(key), model)
    }

    /** Shows the chosen drawings to Karalak and has the whole script written. */
    fun writeScript() {
        val s = _state.value
        if (!s.canWrite) return
        val d = director() ?: run { _state.update { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val name = s.player?.name ?: "Oyuncu"
        _state.update { it.copy(writing = true, writeError = null, writeProgress = "Karalak çizimlere bakıyor…") }
        viewModelScope.launch {
            val result = d.write(s.chosen, name) { p ->
                _state.update {
                    it.copy(writeProgress = if (p.done < s.chosen.size) "Karalak ${p.done + 1}. çizime bakıyor…" else "Açılış ve kapanış yazılıyor…")
                }
            }
            result.onSuccess { w ->
                _state.update {
                    it.copy(
                        writing = false, writeProgress = null, script = w.script, aiCost = d.spentUsd,
                        caption = w.caption.ifBlank { it.caption }, hashtags = w.hashtags.ifBlank { it.hashtags },
                        video = null, previewMs = 0L
                    )
                }
                refreshPreview()
            }.onFailure { e ->
                _state.update { it.copy(writing = false, writeProgress = null, writeError = e.message ?: "Yazılamadı", aiCost = d.spentUsd.takeIf { c -> c > 0 }) }
            }
        }
    }

    /** After a verdict was flipped by hand: the reactions and the closing line must agree with it again. */
    fun rewriteFrame() {
        val s = _state.value
        val script = s.script ?: return
        val d = director() ?: return
        _state.update { it.copy(writing = true, writeError = null, writeProgress = "Tepkiler yeniden yazılıyor…") }
        viewModelScope.launch {
            d.rewriteFrame(script, s.player?.name ?: "Oyuncu").onSuccess { w ->
                _state.update { it.copy(writing = false, writeProgress = null, script = w.script, caption = w.caption.ifBlank { it.caption }, video = null) }
                refreshPreview()
            }.onFailure { e -> _state.update { it.copy(writing = false, writeProgress = null, writeError = e.message) } }
        }
    }

    fun editLine(ref: LineRef, text: String) = updateLine(ref) { it.copy(text = text.take(160)) }
    fun editEmotion(ref: LineRef, emotion: MascotEmotion) = updateLine(ref) { it.copy(emotion = emotion) }

    private fun updateLine(ref: LineRef, change: (SpokenLine) -> SpokenLine) {
        _state.update { s ->
            val sc = s.script ?: return@update s
            val next = when (ref) {
                LineRef.Intro -> sc.copy(intro = change(sc.intro))
                LineRef.Outro -> sc.copy(outro = change(sc.outro))
                is LineRef.Reaction -> sc.copy(rounds = sc.rounds.mapIndexed { i, r -> if (i == ref.round) r.copy(reaction = change(r.reaction)) else r })
                is LineRef.Stage -> sc.copy(rounds = sc.rounds.mapIndexed { i, r ->
                    if (i == ref.round) r.copy(stages = r.stages.mapIndexed { k, l -> if (k == ref.stage) change(l) else l }) else r
                })
            }
            s.copy(script = next, video = null)
        }
        refreshPreview()
    }

    /** The judge's call on one drawing, when a right answer was written differently ("pisi" for "kedi"). */
    fun toggleVerdict(round: Int) {
        _state.update { s ->
            val sc = s.script ?: return@update s
            s.copy(script = sc.copy(rounds = sc.rounds.mapIndexed { i, r -> if (i == round) r.copy(correct = !r.correct) else r }), video = null)
        }
        refreshPreview()
    }

    // ---- voice

    fun setVoiceOn(on: Boolean) { _state.update { it.copy(voiceOn = on, video = null) } }
    fun setVoice(pitch: Float, rate: Float) { _state.update { it.copy(voicePitch = pitch, voiceRate = rate, video = null) } }

    /** Plays one line on the speaker with the current voice settings. */
    fun listen(text: String) {
        viewModelScope.launch {
            val synth = listener ?: SpeechSynth.open(context).getOrElse { e ->
                _state.update { it.copy(voiceError = e.message) }
                return@launch
            }.also { listener = it }
            synth.tune(_state.value.voicePitch, _state.value.voiceRate)
            synth.speak(text)
            _state.update { it.copy(voiceError = null) }
        }
    }

    // ---- preview

    fun setPreviewTime(ms: Long) {
        _state.update { it.copy(previewMs = ms) }
        refreshPreview()
    }

    private fun refreshPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(60)
            val s = _state.value
            val script = s.script ?: previewScript(s)
            val player = s.player
            val sp = sprites
            if (script == null || player == null || sp == null) { _state.update { it.copy(preview = null, totalMs = 0L) }; return@launch }
            val (image, total) = withContext(Dispatchers.Default) {
                val renderer = GuessRenderer(assetsOrLoad(), sp, script, s.drawings.associateBy { it.id }, player, voice = null)
                val bmp = Bitmap.createBitmap(GuessRenderer.W / 2, GuessRenderer.H / 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.scale(0.5f, 0.5f)
                renderer.draw(canvas, s.previewMs.coerceIn(0L, renderer.totalMs))
                bmp.asImageBitmap() to renderer.totalMs
            }
            _state.update { it.copy(preview = image, totalMs = total) }
        }
    }

    /** Before the assistant has written anything: a stand-in script so the layout can be seen with the chosen drawings. */
    private fun previewScript(s: StudioState): GuessScript? {
        if (s.chosen.isEmpty()) return null
        val name = s.player?.name ?: "Oyuncu"
        return GuessScript(
            SpokenLine("$name bana ${s.chosen.size} çizim gönderdi. Hepsini bilirsem o kaybeder!", MascotEmotion.Confident),
            s.chosen.map { d ->
                com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessRound(
                    d.id, d.word,
                    listOf(SpokenLine("Hmm, bu ne olabilir?", MascotEmotion.Thinking), SpokenLine("Bir şeye benziyor…", MascotEmotion.Suspicious), SpokenLine("Buldum!", MascotEmotion.Excited)),
                    d.word, true, SpokenLine("Bildim!", MascotEmotion.Victory), d.answer.takeIf { it.isNotBlank() }
                )
            },
            SpokenLine("Beni yenebilir misin? karalak.com'da çiz!", MascotEmotion.Wave)
        )
    }

    private fun assetsOrLoad(): StudioAssets = assets ?: StudioAssets(context).also { assets = it }

    // ---- the video

    fun render() {
        val s = _state.value
        val script = s.script ?: return
        val player = s.player ?: return
        val sp = sprites ?: return
        if (s.rendering) return
        _state.update { it.copy(rendering = true, progress = 0f, renderError = null, video = null, renderStage = "Hazırlanıyor…") }
        renderJob = viewModelScope.launch {
            val dir = File(context.cacheDir, "studio").apply { mkdirs() }
            dir.listFiles()?.forEach { it.deleteRecursively() }
            val out = File(dir, "karalak_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4")
            val job = coroutineContext[Job]
            val result = runCatching {
                // 1. Karalak's voice, one file per line
                val lines = script.lines()
                val voices = HashMap<Int, Pcm>()
                if (s.voiceOn) {
                    val synth = SpeechSynth.open(context).getOrThrow()
                    try {
                        synth.tune(s.voicePitch, s.voiceRate)
                        lines.forEachIndexed { i, l ->
                            _state.update { it.copy(renderStage = "Karalak konuşuyor ${i + 1}/${lines.size}", progress = 0.15f * i / lines.size) }
                            val pcm = synth.synth(l.text, dir) ?: error("“${l.text.take(30)}” seslendirilemedi")
                            voices[i] = PcmOps.resample(pcm, RATE)
                        }
                    } finally {
                        synth.close()
                    }
                }
                withContext(Dispatchers.Default) {
                    // 2. timing from the real voice, the voice laid on one track, and that track encoded
                    val envelopes = if (s.voiceOn) voices.mapValues { (_, pcm) -> PcmOps.envelope(pcm, GuessRenderer.FPS) } else null
                    val renderer = GuessRenderer(assetsOrLoad(), sp, script, s.drawings.associateBy { it.id }, player, envelopes)
                    val audio = if (s.voiceOn) {
                        _state.update { it.copy(renderStage = "Ses birleştiriliyor", progress = 0.17f) }
                        val placed = renderer.events.filter { it.line != null && it.lineIndex >= 0 }.mapNotNull { e ->
                            voices[e.lineIndex]?.let { Placed(it, e.startMs) }
                        }
                        AacEncoder.encode(PcmOps.mix(renderer.totalMs, RATE, placed))
                    } else null
                    // 3. the picture, full HD when the phone's encoder takes it, 720p otherwise
                    val frames = (renderer.totalMs * GuessRenderer.FPS / 1000).toInt().coerceAtLeast(1)
                    val draw: (Canvas, Long, Float) -> Unit = { canvas, t, scale ->
                        if (scale != 1f) { canvas.save(); canvas.scale(scale, scale) }
                        renderer.draw(canvas, t)
                        if (scale != 1f) canvas.restore()
                    }
                    fun encodeAt(w: Int, h: Int) = com.sualtikasifi.cizimhafiza.presentation.reports.studio.VideoEncoder.encode(
                        out = out, width = w, height = h, fps = GuessRenderer.FPS, frames = frames,
                        drawFrame = { canvas, t -> draw(canvas, t, w.toFloat() / GuessRenderer.W) },
                        onProgress = { p -> _state.update { it.copy(renderStage = "Video oluşturuluyor", progress = 0.2f + 0.8f * p) } },
                        isCancelled = { job?.isActive == false },
                        audio = audio
                    )
                    try {
                        encodeAt(GuessRenderer.W, GuessRenderer.H)
                    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        encodeAt(720, 1280)
                    }
                }
            }
            result.onSuccess { _state.update { it.copy(rendering = false, video = out, progress = 1f, renderStage = "") } }
                .onFailure { e ->
                    val cancelled = e is kotlin.coroutines.cancellation.CancellationException
                    _state.update { it.copy(rendering = false, renderStage = "", renderError = if (cancelled) null else (e.message ?: e::class.simpleName)) }
                }
        }
    }

    fun cancelRender() {
        renderJob?.cancel()
        _state.update { it.copy(rendering = false, progress = 0f, renderStage = "") }
    }

    /** Copies the finished video into Movies/Karalak, to edit in CapCut or post later. */
    fun saveToGallery() {
        val file = _state.value.video ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _state.update { it.copy(message = "Galeriye kaydetmek Android 10 ve üstünde var; Paylaş ile kaydedebilirsin.") }
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Karalak")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                        ?: error("Galeri kaydı açılamadı")
                    resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: error("Galeriye yazılamadı")
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }.isSuccess
            }
            _state.update { it.copy(message = if (ok) "Galeriye kaydedildi: Filmler/Karalak" else "Galeriye kaydedilemedi") }
        }
    }

    override fun onCleared() {
        renderJob?.cancel()
        listener?.close()
        super.onCleared()
    }

    private companion object {
        /** The voice track's sample rate: what every AAC encoder takes. */
        const val RATE = 44_100
        val GOOGLE_PHOTO = Regex("^https://lh[0-9]+\\.googleusercontent\\.com/\\S*$")
    }
}
