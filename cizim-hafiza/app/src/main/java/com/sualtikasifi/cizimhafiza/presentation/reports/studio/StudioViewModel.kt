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

/**
 * The video studio, in two formats:
 *
 * - "Çizen Bile Bilemedi": a drawing its own drawer could not recognise, Karalak's guesses, a countdown for the
 *   viewer, the drawer's wrong answer and a kupon kodu.
 * - "Gerçek Olsaydı": one player's quick drawing as a real picture and clip made in ChatGPT and Gemini
 *   and brought back here, opened on the game's screen with a hook and closed on Karalak's reaction.
 *
 * The assistant writes Karalak's lines from the drawings themselves, the phone's Turkish voice
 * (or ElevenLabs) says them, and the phone renders the video with sound.
 */
@HiltViewModel
class StudioViewModel @Inject constructor(
    @ApplicationContext internal val context: Context,
    internal val drawingResultDao: DrawingResultDao,
    internal val settings: SettingsRepository,
    internal val auth: AuthRepository,
    internal val wordDao: WordDao,
    internal val moderation: ModerationRepository,
    internal val couponRepository: CouponRepository
) : ViewModel() {
    internal val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    internal var assets: StudioAssets? = null
    internal var sprites: MascotSprites? = null
    internal var previewJob: Job? = null
    internal var renderJob: Job? = null
    internal var listener: SpeechSynth? = null
    internal var player: MediaPlayer? = null
    internal val voiceBank = VoiceBank(context)
    internal val json = Json { ignoreUnknownKeys = true }
    internal val realStore = RealStore(context)
    internal var realSaveJob: Job? = null
    /** Imported clips by file name, opened for quick preview frames. */
    internal val previewClips = ConcurrentHashMap<String, ClipFrames>()
    /** The promo video, opened the same way for the preview. */
    @Volatile internal var previewPromo: ClipFrames? = null
    @Volatile internal var previewHowto: ClipFrames? = null

    init {
        loadDrawings()
        loadMascot()
        loadReal()
        loadVoiceSettings()
        loadPublishLog()
        _state.update { it.copy(analysisModel = analysisModel()) }
    }

    // ---- loading

    internal fun loadDrawings() {
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

    fun reloadMascot() = loadMascot()

    internal fun loadMascot() {
        _state.update { it.copy(mascotLoading = true) }
        viewModelScope.launch {
            val loaded = MascotSprites.load(context)
            sprites = loaded
            _state.update { it.copy(mascotLoading = false, mascotPoses = loaded.count) }
            refreshPreview()
        }
    }

    // ---- choices

    fun setFormat(f: StudioFormat) {
        _state.update { it.copy(format = f, video = null, renderError = null, previewMs = 0L) }
        // Karalak speaks here with the voice "Gerçek Olsaydı" uses (the settings are shared): ElevenLabs, its last chosen voice
        if (f == StudioFormat.Forgot && _state.value.eleven.keySet && _state.value.voiceEngine != VoiceEngine.ElevenLabs) setVoiceEngine(VoiceEngine.ElevenLabs)
        if (f == StudioFormat.RealLife && _state.value.real.recent.isEmpty()) refreshRecent()
        refreshPreview()
    }

    fun setCaption(v: String) { _state.update { it.copy(caption = v.take(2_000)) } }
    fun setHashtags(v: String) { _state.update { it.copy(hashtags = v.take(300)) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }

    // ---- the script

    internal fun prefs() = context.getSharedPreferences(AssistantViewModel.PREFS, Context.MODE_PRIVATE)

    /** The model that reads the drawings in "Gerçek Olsaydı": the one typed in the panel, else [OpenRouterClient.ANALYSIS_MODEL]. */
    internal fun analysisModel(): String =
        prefs().getString(OpenRouterClient.ANALYSIS_MODEL_PREF, null).orEmpty().trim().ifBlank { OpenRouterClient.ANALYSIS_MODEL }

    fun setAnalysisModel(id: String) {
        prefs().edit().putString(OpenRouterClient.ANALYSIS_MODEL_PREF, id.trim()).apply()
        _state.update { it.copy(analysisModel = id.trim().ifBlank { OpenRouterClient.ANALYSIS_MODEL }) }
    }

    fun editLine(ref: LineRef, text: String) = updateLine(ref) { it.copy(text = text.take(160)) }

    fun editEmotion(ref: LineRef, emotion: MascotEmotion) = updateLine(ref) { it.copy(emotion = emotion) }

    internal fun updateLine(ref: LineRef, change: (SpokenLine) -> SpokenLine) {
        _state.update { s ->
            if (ref is LineRef.ForgotHook || ref is LineRef.ForgotGuess || ref is LineRef.ForgotAsk || ref is LineRef.ForgotCode || ref is LineRef.ForgotOutro) {
                val fs = s.forgot.script ?: return@update s
                val next = when (ref) {
                    LineRef.ForgotHook -> fs.copy(hook = change(fs.hook))
                    is LineRef.ForgotGuess -> fs.copy(guesses = fs.guesses.mapIndexed { i, l -> if (i == ref.index) change(l) else l })
                    LineRef.ForgotAsk -> fs.copy(askLine = change(fs.askLine))
                    LineRef.ForgotCode -> fs.copy(codeLine = change(fs.codeLine))
                    LineRef.ForgotOutro -> fs.copy(outro = change(fs.outro))
                    else -> fs
                }
                return@update s.copy(forgot = s.forgot.copy(script = next), video = null)
            }
            s
        }
        refreshPreview()
    }

    // ---- preview

    fun setPreviewTime(ms: Long) {
        _state.update { it.copy(previewMs = ms) }
        refreshPreview()
    }

    internal fun refreshPreview() {
        refreshVoicePlan()
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(60)
            val s = _state.value
            val video = withContext(Dispatchers.Default) { videoFor(s, voice = null, preview = true) }
            if (video == null) { _state.update { it.copy(preview = null, totalMs = 0L) }; return@launch }
            val (image, total) = withContext(Dispatchers.Default) {
                val renderer = video
                val bmp = Bitmap.createBitmap(renderer.width / 2, renderer.height / 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.scale(0.5f, 0.5f)
                renderer.draw(canvas, s.previewMs.coerceIn(0L, renderer.totalMs))
                bmp.asImageBitmap() to renderer.totalMs
            }
            _state.update { it.copy(preview = image, totalMs = total) }
        }
    }

    internal fun assetsOrLoad(): StudioAssets = assets ?: StudioAssets(context).also { assets = it }

    // ---- the video

    /**
     * The video of the current format, drawn from the current state; null while something it needs
     * is missing. [clipSource] opens an imported clip by file name: quick key frames for the
     * preview, every frame for the real thing.
     */
    internal fun videoFor(
        s: StudioState, voice: Map<Int, FloatArray>?, preview: Boolean,
        wordTimes: Map<Int, List<LongRange>>? = null,
        clipSource: (String) -> ClipFrames? = { previewClips[it] },
        promoSource: (File) -> ClipFrames? = { previewPromo },
        howtoSource: (File) -> ClipFrames? = { previewHowto }
    ): VoicedVideo? {
        if (s.format == StudioFormat.RealLife) {
            val p = s.real.project ?: return null
            // the video is one drawing's: only its clip is opened
            val f = p.featured ?: return null
            val clip = f.clip ?: return null
            val promo = s.real.promo?.takeIf { s.real.ending == RealEnding.Promo && s.real.promoMs > 0 }
            val howto = s.real.howto?.takeIf { s.real.ending == RealEnding.Card && s.real.howtoOn && s.real.howtoMs > 0 }
            return RealRenderer(
                assetsOrLoad(), p,
                clips = listOfNotNull(clipSource(clip)?.let { f.sampleId to it }).toMap(),
                clipFiles = mapOf(f.sampleId to realStore.file(clip)),
                clipGain = CLIP_GAIN,
                layout = s.real.card,
                photoFiles = p.drawings.mapNotNull { d -> d.photo?.let { d.id to realStore.file(it) } }.toMap(),
                hookVoiceFile = f.hook.voice?.let { realStore.file(it) },
                narrationVoiceFile = f.narration.voice?.let { realStore.file(it) },
                outroVoiceFile = f.outro.voice?.let { realStore.file(it) },
                musicFile = f.music?.takeIf { s.real.musicOn }?.let { RealMusic.target(context, it) },
                promo = promo?.let(promoSource),
                promoFile = promo,
                promoMs = if (promo != null) s.real.promoMs else 0L,
                howto = howto?.let(howtoSource),
                howtoMs = if (howto != null) s.real.howtoMs else 0L,
                endCardMs = if (s.real.ending == RealEnding.Card) f.endCardMs else 0L,
                endVoiceFile = f.endLine.voice?.let { realStore.file(it) }
            )
        }
        val sp = sprites ?: return null
        return when (s.format) {
            StudioFormat.Forgot -> {
                val script = s.forgot.script ?: return null
                val drawing = s.forgotDrawing(script.drawingId) ?: return null
                // the closing is the same end card with the phone as "Gerçek Olsaydı"'s, the how-to video playing in it
                val howtoFile = s.real.howto?.takeIf { s.real.howtoMs > 0 }
                ForgotRenderer(
                    assetsOrLoad(), sp, script, drawing, voice, wordTimes,
                    howto = howtoFile?.let(howtoSource), howtoMs = if (howtoFile != null) s.real.howtoMs else 0L
                )
            }
            StudioFormat.RealLife -> null
        }
    }

    /** The current format's spoken lines, in the order the video says them. */
    internal fun linesFor(s: StudioState): List<SpokenLine>? = when (s.format) {
        StudioFormat.Forgot -> s.forgot.script?.lines()
        // the video has no voice, no subtitles: nothing to say
        StudioFormat.RealLife -> emptyList()
    }

    fun render() {
        val s = _state.value
        val lines = linesFor(s) ?: return
        if (videoFor(s, null, preview = false) == null) return
        if (s.rendering) return
        _state.update { it.copy(rendering = true, progress = 0f, renderError = null, video = null, renderStage = "Hazırlanıyor…") }
        renderJob = viewModelScope.launch {
            val dir = File(context.cacheDir, "studio").apply { mkdirs() }
            dir.listFiles()?.forEach { it.deleteRecursively() }
            // a video made without the app's music is named so, to tell the two apart in the gallery
            val noMusic = if (s.format == StudioFormat.RealLife && !s.real.musicOn) "_muziksiz" else ""
            val out = File(dir, "karalak_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + noMusic + ".mp4")
            val job = coroutineContext[Job]
            val result = runCatching {
                // 1. Karalak's voice, one file per line
                val voices = HashMap<Int, Pcm>()
                // when each word is said, for the lines whose voice was made with timings (the subtitle comes in with them)
                val times = HashMap<Int, List<LongRange>>()
                if (s.voiceOn && s.voiceEngine == VoiceEngine.ElevenLabs) {
                    // making the video never pays: it uses only lines already voiced with "Seslendir"
                    val spec = s.eleven.spec
                    val missing = lines.count { voiceBank.cached(it, spec) == null }
                    if (missing > 0) error("$missing cümlenin ElevenLabs sesi yok. Önce Ses bölümünde “Seslendir”e bas.")
                    lines.forEachIndexed { i, l ->
                        _state.update { it.copy(renderStage = "Karalak'ın sesi hazırlanıyor ${i + 1}/${lines.size}", progress = 0.15f * i / lines.size) }
                        val f = voiceBank.cached(l, spec) ?: error("“${l.text.take(30)}” sesi bulunamadı")
                        val pcm = withContext(Dispatchers.IO) { ClipAudio.decode(f, VOICE_MAX_MS) } ?: error("“${l.text.take(30)}” sesi çözülemedi")
                        voices[i] = PcmOps.softEnd(PcmOps.resample(pcm, RATE))
                        voiceBank.timing(l, spec)?.rangesFor(l.text)?.let { times[i] = it }
                    }
                } else if (s.voiceOn && lines.isNotEmpty()) {
                    val synth = SpeechSynth.open(context, s.lang).getOrThrow()
                    try {
                        synth.tune(s.voicePitch, s.voiceRate)
                        lines.forEachIndexed { i, l ->
                            _state.update { it.copy(renderStage = "Karalak konuşuyor ${i + 1}/${lines.size}", progress = 0.15f * i / lines.size) }
                            val pcm = synth.synth(l.text, dir) ?: error("“${l.text.take(30)}” seslendirilemedi")
                            voices[i] = PcmOps.softEnd(PcmOps.resample(pcm, RATE))
                        }
                    } finally {
                        synth.close()
                    }
                }
                // every frame of an imported clip, opened for this render only
                val renderClips = HashMap<String, ClipFrames?>()
                var renderPromo: ClipFrames? = null
                var renderHowto: ClipFrames? = null
                try {
                    withContext(Dispatchers.Default) {
                        // 2. timing from the real voice, the voice and any clip sound laid on one track, and that track encoded
                        val envelopes = if (s.voiceOn) voices.mapValues { (_, pcm) -> PcmOps.envelope(pcm, STUDIO_FPS) } else null
                        val renderer = videoFor(
                            s, envelopes, preview = false,
                            wordTimes = times.takeIf { s.voiceOn && it.isNotEmpty() },
                            clipSource = { name -> renderClips.getOrPut(name) { runCatching { ClipFrames(realStore.file(name), exact = true) }.getOrNull() } },
                            promoSource = { f -> renderPromo ?: runCatching { ClipFrames(f, exact = true) }.getOrNull().also { renderPromo = it } },
                            howtoSource = { f -> renderHowto ?: runCatching { ClipFrames(f, exact = true) }.getOrNull().also { renderHowto = it } }
                        ) ?: error("Video hazırlanamadı")
                        _state.update { it.copy(renderStage = "Ses birleştiriliyor", progress = 0.17f) }
                        val placed = mutableListOf<Placed>()
                        if (s.voiceOn) placed += renderer.voiceStarts.mapNotNull { (index, start) -> voices[index]?.let { Placed(it, start) } }
                        // the background music is copied out of the app the first time it is used
                        if (s.format == StudioFormat.RealLife && s.real.musicOn) s.real.project?.featured?.music?.let { runCatching { RealMusic.ensure(context, it) } }
                        renderer.sounds.forEach { snd ->
                            runCatching { ClipAudio.decode(snd.file, snd.durationMs) }.getOrNull()?.let { pcm ->
                                val at = PcmOps.resample(pcm, RATE).let { if (snd.voice) PcmOps.softEnd(it) else it }
                                placed += Placed(snd.shape?.let { PcmOps.shaped(at, it) } ?: at, snd.startMs, snd.gain)
                            }
                        }
                        val audio = if (placed.isNotEmpty()) AacEncoder.encode(PcmOps.mix(renderer.totalMs, RATE, placed)) else null
                        // 3. the picture, full HD when the phone's encoder takes it, 720p otherwise
                        val frames = (renderer.totalMs * STUDIO_FPS / 1000).toInt().coerceAtLeast(1)
                        val draw: (Canvas, Long, Float) -> Unit = { canvas, t, scale ->
                            if (scale != 1f) { canvas.save(); canvas.scale(scale, scale) }
                            renderer.draw(canvas, t)
                            if (scale != 1f) canvas.restore()
                        }
                        fun encodeAt(w: Int, h: Int) = com.sualtikasifi.cizimhafiza.presentation.reports.studio.VideoEncoder.encode(
                            out = out, width = w, height = h, fps = STUDIO_FPS, frames = frames,
                            drawFrame = { canvas, t -> draw(canvas, t, w.toFloat() / renderer.width) },
                            onProgress = { p -> _state.update { it.copy(renderStage = "Video oluşturuluyor", progress = 0.2f + 0.8f * p) } },
                            isCancelled = { job?.isActive == false },
                            audio = audio
                        )
                        try {
                            encodeAt(renderer.width, renderer.height)
                        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // the same shape, 720 wide (720x1280 for 9:16, 720x1600 for 9:20)
                            encodeAt(720, (720L * renderer.height / renderer.width).toInt() / 2 * 2)
                        }
                    }
                } finally {
                    renderClips.values.forEach { it?.close() }
                    renderPromo?.close()
                    renderHowto?.close()
                }
            }
            result.onSuccess {
                // a finished Çizen Bile Bilemedi video hands the next one the next wording (unless one is picked by hand)
                if (s.format == StudioFormat.Forgot) prefs().edit().putInt(FORGOT_TURN, prefs().getInt(FORGOT_TURN, 0) + 1).apply()
                // the log of made videos: which day went out with which code
                recordMadeVideo(s)
                _state.update { it.copy(rendering = false, video = out, progress = 1f, renderStage = "") }
            }
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
        player?.release()
        player = null
        // a line edited in the last moment would otherwise wait for a save that never comes
        _state.value.real.project?.let { p -> runCatching { realStore.save(p) } }
        previewClips.values.forEach { it.close() }
        previewClips.clear()
        previewPromo?.close()
        previewPromo = null
        previewHowto?.close()
        previewHowto = null
        super.onCleared()
    }

    internal companion object {
        const val EL_KEY = "elevenlabs_key"
        const val EL_VOICE = "elevenlabs_voice"
        const val EL_MODEL = "elevenlabs_model"
        const val EL_STABILITY = "elevenlabs_stability"
        const val EL_TAGS = "elevenlabs_tags"
        const val STUDIO_LANG = "studio_lang"
        const val VOICE_ENGINE = "studio_voice_engine"
        const val CARD_SCALE = "studio_real_card_scale"
        const val FORGOT_TURN = "studio_forgot_style_turn"
        const val CARD_X = "studio_real_card_x"
        const val CARD_Y = "studio_real_card_y"
        const val POST_SAME_MS = 30 * 60_000L
        const val ENDING = "studio_real_ending"
        const val TAB = "studio_real_tab"
        const val STEP = "studio_real_step"
        const val VOICE_STEP = "studio_real_voice_step"
        const val MUSIC_ON = "studio_real_music_on"
        const val HOWTO_ON = "studio_real_howto_on"
        const val PLATFORM = "studio_real_platform"
        /** No line of Karalak's runs this long; a longer decode means something went wrong. */
        const val VOICE_MAX_MS = 60_000L
        /** The voice track's sample rate: what every AAC encoder takes. */
        const val RATE = 44_100
        /** "Gerçek Olsaydı": the latest pool drawings shown (read 10 rounds at a time), and how many go in one video. */
        const val RECENT_MAX = 100
        const val RECENT_PAGE = 10
        const val RECENT_PAGES = 6
        const val MAX_REVEALS = 5
        /** The clips' own sound, a little under Karalak's voice. */
        const val CLIP_GAIN = 0.85f
    }
}
