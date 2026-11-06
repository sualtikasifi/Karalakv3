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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotChest
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.forgot.ForgotScript
import com.sualtikasifi.cizimhafiza.domain.model.Coupon
import com.sualtikasifi.cizimhafiza.domain.model.CouponCodes
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

/** The studio's video formats. */
enum class StudioFormat(val title: String, val blurb: String) {
    Forgot("Çizen Bile Bilemedi", "Çizenin bile tanıyamadığı çizim; doğru cevap kupon kodu"),
    RealLife("Gerçek Olsaydı", "Çizimler gerçek hayata dönüşüyor")
}

/** Who gives Karalak its voice: the phone's own text-to-speech, or ElevenLabs. */
enum class VoiceEngine { Phone, ElevenLabs }

/** ElevenLabs settings as the panel shows them; the key itself never leaves SharedPreferences. */
data class ElevenUi(
    val keySet: Boolean = false,
    /** The key's last characters, so the panel can show which one is in. */
    val keyHint: String = "",
    val voiceId: String = ElevenLabs.PRESETS.first().id,
    val model: VoiceModel = VoiceModel.V3,
    /** 0 creative · 0.5 natural · 1 robust. */
    val stability: Float = 0.5f,
    /** v3 only: say each line the way Karalak's face says it ("[laughs]", "[surprised]"…). */
    val tags: Boolean = true,
    /** The language of the videos: what the assistant writes in and what the voice speaks. */
    val lang: StudioLang = StudioLang.Tr,
    /** The account's own voices, when the key may list them. */
    val voices: List<ElevenLabs.Voice> = emptyList(),
    val status: String? = null,
    val busy: Boolean = false,
    /** The current script's lines already voiced with these settings, the ones still to voice, and what those would cost. */
    val ready: Int = 0,
    val missing: Int = 0,
    val missingCredits: Int = 0,
    /** "Seslendiriliyor 3/14" while voicing. */
    val progress: String? = null,
    /** A line not yet voiced that the person wants to hear: asks before spending credits on it. */
    val confirm: SpokenLine? = null
) {
    val spec: VoiceSpec get() = VoiceSpec(voiceId, model, stability.toDouble(), tags, lang)
}

/** Which spoken line of the script an edit is about. */
sealed interface LineRef {
    data object ForgotHook : LineRef
    data class ForgotGuess(val index: Int) : LineRef
    data object ForgotCode : LineRef
    data object ForgotOutro : LineRef
}

/** "Çizen Bile Bilemedi": the chosen drawing, what its drawer wrote, how the code is given, what it gives, and the script. */
data class ForgotUi(
    val drawingId: Long? = null,
    val script: ForgotScript? = null,
    val mode: ForgotMode = ForgotMode.Word,
    /** What the code gives, all set by hand: gold, XP and one chest (or none). */
    val chest: ForgotChest = ForgotChest.Gold,
    val gold: Int = 0,
    val xp: Int = 0,
    /** How many accounts may use the code, and the date and time it stops working (device time zone). */
    val maxUses: Int = 100,
    val expiresAtMillis: Long = ForgotCopy.defaultExpiry(),
    /** The code written out on the end card in [ForgotMode.Shown]; a fresh one on demand. */
    val shownCode: String = CouponCodes.generate(),
    val writing: Boolean = false,
    val writeError: String? = null,
    val couponBusy: Boolean = false,
    val couponStatus: String? = null,
    /** How many of the phone's drawings the list shows: [FORGOT_PAGE] at first, that many more each time "Daha fazla" is pressed. */
    val shown: Int = FORGOT_PAGE
)

/** "Gerçek Olsaydı": the pool's latest drawings, and the project (the chosen drawings of different words) on this phone. */
data class RealUi(
    /** Finished pieces kept on this phone (picture, clip, card details), newest first. */
    val library: List<LibraryItem> = emptyList(),
    /** Library pieces ticked to be made into a video, in the order they were ticked. */
    val librarySelected: List<String> = emptyList(),
    /** The library piece open in the viewer. */
    val libraryView: String? = null,
    val libraryShown: Boolean = false,
    val libraryBytes: Long = 0L,
    val recent: List<RecentDrawing> = emptyList(),
    val loadingRecent: Boolean = false,
    val recentError: String? = null,
    val project: RealProject? = null,
    val writing: Boolean = false,
    val writeError: String? = null,
    /** The drawing whose video scene ideas are being written, and why the last try failed. */
    val pitching: String? = null,
    val pitchError: String? = null,
    /** Which of Karalak's lines the assistant is writing ideas for. */
    val lineWriting: RealLine? = null,
    val lineErrors: Map<RealLine, String> = emptyMap(),
    /** The line being voiced in ElevenLabs right now. */
    val lineVoicing: Pair<RealLine, String>? = null,
    /** The video drawing's ideas already voiced with the current voice settings (paid for, kept on this phone), per line. */
    val lineVoiced: Map<RealLine, Set<String>> = emptyMap(),
    /** The music track playing on the speaker to choose from, if any. */
    val musicPlaying: String? = null,
    /** The drawing whose picture, clip or reaction is being worked on. */
    val busy: String? = null,
    /** Starting over while the project has pictures in it: asks before throwing them away. */
    val confirmReset: Boolean = false,
    /** Where the Karalak card sits on the video and how big it is, kept on this phone. */
    val card: CardLayout = CardLayout(),
    /** The app's promo video (one, kept on this phone) and its length; whether it closes the videos is [ending]'s. */
    val promo: File? = null,
    val promoMs: Long = 0L,
    val promoBusy: Boolean = false,
    /** What closes every video: a short end card (the default), the promo video, or nothing after Karalak's closing. */
    val ending: RealEnding = RealEnding.Card,
    /** The video that shows how the game is played (shipped in the app, no sound) and its length; shown before the end card while [howtoOn]. */
    val howto: File? = null,
    val howtoMs: Long = 0L,
    val howtoOn: Boolean = true,
    /** Whether the app's music is mixed into the video; off, it is left out so a trending sound can be added in the app it is posted to. */
    val musicOn: Boolean = true,
    /** The music the person added from their own files (names, see RealMusic), after the app's own tracks. */
    val customMusic: List<String> = emptyList(),
    /** How many of the ready-made closing lines are voiced with the current settings, and how many voices are kept for good. */
    val endVoiced: Int = 0,
    val endKept: Int = 0,
    /** Where the video is going to be posted: what its post text ends with. */
    val platform: SocialPlatform = SocialPlatform.TikTok,
    /** A picture ready to hand to ChatGPT or Gemini, with the prompt to paste. */
    val share: RealShare? = null,
    /** The swipe-through post: each drawing followed by what it became, ready for the share sheet. */
    val carousel: List<File>? = null,
    /** Which part of "Gerçek Olsaydı" is open, which of its seven steps, and which of the voices' five; kept on this phone. */
    val tab: RealTab = RealTab.Prepare,
    val step: Int = 1,
    val voiceStep: Int = 1
)

enum class RealTab { Prepare, Library, Settings }

enum class ShareTarget { ChatGpt, Gemini }

class RealShare(val file: File, val prompt: String, val target: ShareTarget = ShareTarget.ChatGpt)

data class StudioState(
    val format: StudioFormat = StudioFormat.RealLife,
    val loading: Boolean = true,
    /** This phone's own drawings. */
    val drawings: List<StudioDrawing> = emptyList(),
    val mascotPoses: Int = 0,
    val mascotLoading: Boolean = true,
    val aiCost: Double? = null,
    /** The model that wrote the last one (a fallback may have stepped in for the chosen one). */
    val aiModel: String? = null,
    /** The model that reads the drawings in "Gerçek Olsaydı" (an OpenRouter id), kept on this phone. */
    val analysisModel: String = OpenRouterClient.ANALYSIS_MODEL,
    val caption: String = "",
    val hashtags: String = DEFAULT_TAGS,
    val voiceOn: Boolean = true,
    val voiceEngine: VoiceEngine = VoiceEngine.Phone,
    val eleven: ElevenUi = ElevenUi(),
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
    val message: String? = null,
    val forgot: ForgotUi = ForgotUi(),
    val real: RealUi = RealUi()
) {
    /** The language the videos are written and spoken in. */
    val lang: StudioLang get() = eleven.lang

    val ready: Boolean get() = !rendering && when (format) {
        StudioFormat.Forgot -> forgot.script != null
        StudioFormat.RealLife -> real.project?.shown?.isNotEmpty() == true
    }
}

/** The most accounts one code may be given to. */
private const val MAX_COUPON_USES = 1_000_000

/** The drawing list of "Çizen Bile Bilemedi" grows by this many at a time (three rows of three). */
const val FORGOT_PAGE = 9

private const val DEFAULT_TAGS = "#çizimoyunu #karalak #yapayzeka #oyunönerisi #çizim"

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
    @ApplicationContext private val context: Context,
    private val drawingResultDao: DrawingResultDao,
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
    private val wordDao: WordDao,
    private val moderation: ModerationRepository,
    private val couponRepository: CouponRepository
) : ViewModel() {
    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private var assets: StudioAssets? = null
    private var sprites: MascotSprites? = null
    private var previewJob: Job? = null
    private var renderJob: Job? = null
    private var listener: SpeechSynth? = null
    private var player: MediaPlayer? = null
    private val voiceBank = VoiceBank(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val realStore = RealStore(context)
    private var realSaveJob: Job? = null
    /** Imported clips by file name, opened for quick preview frames. */
    private val previewClips = ConcurrentHashMap<String, ClipFrames>()
    /** The promo video, opened the same way for the preview. */
    @Volatile private var previewPromo: ClipFrames? = null
    @Volatile private var previewHowto: ClipFrames? = null

    init {
        loadDrawings()
        loadMascot()
        loadReal()
        loadVoiceSettings()
        _state.update { it.copy(analysisModel = analysisModel()) }
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

    private fun prefs() = context.getSharedPreferences(AssistantViewModel.PREFS, Context.MODE_PRIVATE)

    /** The model that reads the drawings in "Gerçek Olsaydı": the one typed in the panel, else [OpenRouterClient.ANALYSIS_MODEL]. */
    private fun analysisModel(): String =
        prefs().getString(OpenRouterClient.ANALYSIS_MODEL_PREF, null).orEmpty().trim().ifBlank { OpenRouterClient.ANALYSIS_MODEL }

    fun setAnalysisModel(id: String) {
        prefs().edit().putString(OpenRouterClient.ANALYSIS_MODEL_PREF, id.trim()).apply()
        _state.update { it.copy(analysisModel = id.trim().ifBlank { OpenRouterClient.ANALYSIS_MODEL }) }
    }

    fun editLine(ref: LineRef, text: String) = updateLine(ref) { it.copy(text = text.take(160)) }

    fun editEmotion(ref: LineRef, emotion: MascotEmotion) = updateLine(ref) { it.copy(emotion = emotion) }

    private fun updateLine(ref: LineRef, change: (SpokenLine) -> SpokenLine) {
        _state.update { s ->
            if (ref is LineRef.ForgotHook || ref is LineRef.ForgotGuess || ref is LineRef.ForgotCode || ref is LineRef.ForgotOutro) {
                val fs = s.forgot.script ?: return@update s
                val next = when (ref) {
                    LineRef.ForgotHook -> fs.copy(hook = change(fs.hook))
                    is LineRef.ForgotGuess -> fs.copy(guesses = fs.guesses.mapIndexed { i, l -> if (i == ref.index) change(l) else l })
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

    // ---- Çizen Bile Bilemedi

    private fun updateForgot(change: (ForgotUi) -> ForgotUi) = _state.update { it.copy(forgot = change(it.forgot)) }

    /** The script for [d] with the panel's choices; the lines of [previous] are kept (the code line follows the mode). */
    private fun buildForgot(d: StudioDrawing, ui: ForgotUi, previous: ForgotScript?): ForgotScript {
        val other = ForgotCopy.codeLineFor(if (ui.mode == ForgotMode.Word) ForgotMode.Shown else ForgotMode.Word)
        return ForgotScript(
            drawingId = d.id, word = d.word,
            hook = previous?.hook ?: SpokenLine(ForgotCopy.HOOK, MascotEmotion.Shocked),
            guesses = previous?.guesses ?: ForgotCopy.placeholderGuesses().take(ForgotCopy.DEFAULT_GUESSES),
            guessAt = previous?.guessAt ?: ForgotTimeline.stopsFor(ForgotCopy.DEFAULT_GUESSES, emptyList()),
            codeLine = previous?.codeLine?.takeIf { it.text != other } ?: SpokenLine(ForgotCopy.codeLineFor(ui.mode), MascotEmotion.Point),
            outro = previous?.outro ?: SpokenLine(ForgotCopy.OUTRO, MascotEmotion.Wave),
            mode = ui.mode,
            reward = ForgotCopy.rewardLabel(ui.gold, ui.xp, ui.chest.chests),
            code = if (ui.mode == ForgotMode.Word) ForgotCopy.wordCode(d.word).orEmpty() else ui.shownCode,
            percent = previous?.percent ?: ForgotCopy.randomPercent(),
            showPercent = previous?.showPercent ?: false
        )
    }

    /** Takes one of this phone's drawings and makes a first script around it. */
    fun selectForgot(d: StudioDrawing) {
        _state.update { s ->
            val ui = s.forgot.copy(drawingId = d.id, writeError = null, couponStatus = null)
            s.copy(forgot = ui.copy(script = buildForgot(d, ui, null)), video = null, previewMs = 0L, renderError = null)
        }
        refreshPreview()
    }

    /** Applies a change to the panel's choices and rebuilds the script around them, keeping its lines. */
    private fun rebuildForgot(change: (ForgotUi) -> ForgotUi) {
        _state.update { s ->
            val ui = change(s.forgot).copy(couponStatus = null)
            val d = s.drawings.find { it.id == ui.drawingId }
            s.copy(forgot = ui.copy(script = if (d != null && ui.script != null) buildForgot(d, ui, ui.script) else ui.script), video = null)
        }
        refreshPreview()
    }

    /** Shows the next [FORGOT_PAGE] drawings in the list. */
    fun showMoreForgot() = updateForgot { it.copy(shown = it.shown + FORGOT_PAGE) }

    fun setForgotMode(mode: ForgotMode) = rebuildForgot { it.copy(mode = mode) }
    fun setForgotChest(chest: ForgotChest) = rebuildForgot { it.copy(chest = chest) }
    fun setForgotGold(n: Int) = rebuildForgot { it.copy(gold = n.coerceIn(0, CouponCodes.MAX_GOLD)) }
    fun setForgotXp(n: Int) = rebuildForgot { it.copy(xp = n.coerceIn(0, CouponCodes.MAX_XP)) }
    fun setForgotUses(n: Int) = updateForgot { it.copy(maxUses = n.coerceIn(0, MAX_COUPON_USES), couponStatus = null) }
    fun setForgotExpiry(millis: Long) = updateForgot { it.copy(expiresAtMillis = millis, couponStatus = null) }
    fun newForgotCode() = rebuildForgot { it.copy(shownCode = CouponCodes.generate()) }

    /** How many guesses Karalak makes (1 to 3); the stops are spread evenly again, and the lines already there stay. */
    fun setForgotGuessCount(n: Int) = editForgotScript { sc ->
        val count = n.coerceIn(1, ForgotTimeline.MAX_GUESSES)
        val stock = ForgotCopy.placeholderGuesses()
        sc.copy(guesses = List(count) { sc.guesses.getOrNull(it) ?: stock[it] }, guessAt = ForgotTimeline.stopsFor(count, emptyList()))
    }

    /** Sets where guess [index] stops the drawing (0 to 1), held between its neighbours' stops: what the panel's bar drags. */
    fun setForgotStop(index: Int, share: Float) = editForgotScript { sc ->
        sc.copy(guessAt = ForgotTimeline.moveStop(sc.stops, index, share))
    }

    /** Moves where guess [index] stops the drawing by [deltaPercent] points, for the fine nudge beside the bar. */
    fun stepForgotStop(index: Int, deltaPercent: Int) {
        val now = _state.value.forgot.script?.stops?.getOrNull(index) ?: return
        setForgotStop(index, now + deltaPercent / 100f)
    }

    fun setForgotShowPercent(on: Boolean) = editForgotScript { it.copy(showPercent = on) }
    fun stepForgotPercent(delta: Int) = editForgotScript { it.copy(percent = (it.percent + delta).coerceIn(1, 99)) }

    private fun editForgotScript(change: (ForgotScript) -> ForgotScript) {
        _state.update { s ->
            val sc = s.forgot.script ?: return@update s
            s.copy(forgot = s.forgot.copy(script = change(sc)), video = null)
        }
        refreshPreview()
    }

    /** Lets the assistant look at the drawing and write Karalak's guesses, hook and closing, and the post text. */
    fun writeForgot() {
        val s = _state.value
        val d = s.drawings.find { it.id == s.forgot.drawingId } ?: return
        if (s.forgot.writing) return
        val key = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), AssistantViewModel.KEY)
        if (key.isBlank()) { updateForgot { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val model = analysisModel()
        updateForgot { it.copy(writing = true, writeError = null) }
        viewModelScope.launch {
            ForgotDirector(OpenRouterClient(key), model, _state.value.lang).write(d, s.forgot.script?.stops.orEmpty()).onSuccess { w ->
                _state.update { st ->
                    val cur = st.forgot.script ?: return@update st.copy(forgot = st.forgot.copy(writing = false))
                    val c = w.lines
                    st.copy(
                        forgot = st.forgot.copy(
                            writing = false,
                            script = cur.copy(
                                hook = c.hook ?: cur.hook,
                                // one line per stop, the assistant's where it wrote one, else what was there
                                guesses = List(cur.guesses.size) { k -> c.guesses.getOrNull(k) ?: cur.guesses[k] },
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

    /** Creates the video's kupon kodu in Firestore, never over an existing code (its use count would start again). */
    fun createForgotCoupon() {
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
                        val coupon = Coupon(code, ui.gold, ui.xp, ui.chest.chests, ui.maxUses, 0, ui.expiresAtMillis)
                        couponRepository.create(coupon).fold(
                            onSuccess = { "Kupon hazır: $code · ilk ${ui.maxUses} kişi · ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(ui.expiresAtMillis))}'e kadar · ${script.reward}" },
                            onFailure = { e -> "Kupon oluşturulamadı: ${e.message}" }
                        )
                    }
                },
                onFailure = { e -> "Kupon kontrol edilemedi: ${e.message}" }
            )
            updateForgot { it.copy(couponBusy = false, couponStatus = status) }
        }
    }

    // ---- Gerçek Olsaydı

    private fun updateReal(change: (RealUi) -> RealUi) = _state.update { it.copy(real = change(it.real)) }

    /** Changes the project, keeps it on disk and redraws the preview. */
    private fun updateProject(saveNow: Boolean = false, change: (RealProject) -> RealProject) {
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

    private fun libraryWith(p: RealProject, old: List<LibraryItem>): List<LibraryItem> {
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

    private fun updateReveal(sampleId: String, saveNow: Boolean = false, change: (RealReveal) -> RealReveal) =
        updateProject(saveNow) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) change(it) else it }) }

    /** The project left from last time, if any, and the latest drawings read last time. */
    private fun loadReal() {
        val saved = prefs().let { CardLayout(it.getFloat(CARD_SCALE, CardLayout.DEFAULT_SCALE), it.getFloat(CARD_X, CardLayout.DEFAULT_X), it.getFloat(CARD_Y, CardLayout.DEFAULT_Y)).clamped() }
        viewModelScope.launch {
            val (p, recent) = withContext(Dispatchers.IO) { realStore.load() to realStore.loadRecent() }
            val library = withContext(Dispatchers.IO) { realStore.loadLibrary() }
            val prefs = prefs()
            val ending = runCatching { RealEnding.valueOf(prefs.getString(ENDING, null).orEmpty()) }.getOrDefault(RealEnding.Card)
            val platform = SocialPlatform.of(prefs.getString(PLATFORM, null))
            val tab = runCatching { RealTab.valueOf(prefs.getString(TAB, null).orEmpty()) }.getOrDefault(RealTab.Prepare)
            updateReal { it.copy(tab = tab, step = prefs.getInt(STEP, 1).coerceIn(1, 7), voiceStep = prefs.getInt(VOICE_STEP, 1).coerceIn(1, 5), project = p, recent = recent, card = saved, library = library, libraryBytes = realStore.sizeOf(library.flatMap { i -> i.media }), ending = ending, musicOn = prefs.getBoolean(MUSIC_ON, true), platform = platform, customMusic = RealMusic.custom(context)) }
            p?.let { loadRealMedia(it) }
            loadPromo()
            loadHowto(prefs.getBoolean(HOWTO_ON, true))
        }
    }

    /** The how-to video that ships in the app: copied out once, its length read, opened for the preview. */
    private suspend fun loadHowto(on: Boolean) {
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
    fun setRealTab(tab: RealTab) {
        prefs().edit().putString(TAB, tab.name).apply()
        updateReal { it.copy(tab = tab) }
    }

    /** Goes to one of the seven steps. Kept on this phone, so the work is picked up where it was left. */
    fun setRealStep(step: Int) {
        val n = step.coerceIn(1, 7)
        prefs().edit().putInt(STEP, n).apply()
        updateReal { it.copy(step = n) }
        // the preview follows the step: the voice being set, the clip with the card on it, the start
        when (n) { 5 -> previewVoiceStep(_state.value.real.voiceStep); 6 -> previewLine(RealLine.Narration); 7 -> setPreviewTime(0L); else -> Unit }
    }

    /** Goes to one of the five voice steps (hook, narrator, closing, music, the line over the end card). */
    fun setVoiceStep(step: Int) {
        val n = step.coerceIn(1, 5)
        prefs().edit().putInt(VOICE_STEP, n).apply()
        updateReal { it.copy(voiceStep = n) }
        previewVoiceStep(n)
    }

    /** Whether the video of how the game is played comes before the end card. Kept on this phone. */
    fun setHowtoOn(on: Boolean) {
        prefs().edit().putBoolean(HOWTO_ON, on).apply()
        updateReal { it.copy(howtoOn = on) }
        _state.update { it.copy(video = null) }
        refreshPreview()
    }

    /** The promo video kept on this phone, if any: its length, and opened for the preview. */
    private suspend fun loadPromo() {
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
    fun importPromo(uri: Uri) {
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

    fun removePromo() {
        viewModelScope.launch {
            previewPromo?.close()
            previewPromo = null
            withContext(Dispatchers.IO) { runCatching { realStore.clearPromo() } }
            _state.update { it.copy(video = null) }
            loadPromo()
        }
    }

    /** What closes the video; kept on this phone. The promo is only used while there is one. */
    fun setEnding(ending: RealEnding) {
        prefs().edit().putString(ENDING, ending.name).apply()
        updateReal { it.copy(ending = ending) }
        _state.update { it.copy(video = null) }
        refreshPreview()
    }

    /** Whether the app's music is mixed into the video; off, the video comes out without it. Kept on this phone. */
    fun setMusicOn(on: Boolean) {
        prefs().edit().putBoolean(MUSIC_ON, on).apply()
        updateReal { it.copy(musicOn = on) }
        _state.update { it.copy(video = null) }
        refreshPreview()
    }

    /** Where the video will be posted; its post text follows. Kept on this phone. */
    fun setPlatform(platform: SocialPlatform) {
        prefs().edit().putString(PLATFORM, platform.name).apply()
        updateReal { it.copy(platform = platform) }
    }

    /** How the video opens (see [RealOpening]); the preview goes back to the very start to show it. */
    fun setOpening(sampleId: String, opening: RealOpening) {
        updateReveal(sampleId, saveNow = true) { it.copy(opening = opening) }
        setPreviewTime(0L)
    }

    /**
     * Reads the newest drawings of the reviewed pool again: the latest [RECENT_MAX], newest round first and each
     * round in play order, every one with its word, the seconds that word gives, and its player's level and
     * frame. The name comes only for players who allowed to be featured; the others stay anonymous. What was
     * read last stays when reading fails.
     */
    fun refreshRecent() {
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
                                out += RecentDrawing("${run.id}#$i", word, seconds, player?.nickname.orEmpty(), player?.level ?: run.level, player?.frameId.orEmpty(), item.strokes)
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
    fun setCard(layout: CardLayout) {
        val c = layout.clamped()
        updateReal { it.copy(card = c) }
        _state.update { it.copy(video = null) }
        prefs().edit().putFloat(CARD_SCALE, c.scale).putFloat(CARD_X, c.x).putFloat(CARD_Y, c.y).apply()
        refreshPreview()
    }

    fun resetCard() = setCard(CardLayout())

    /** Clears the choice and every picture and clip, after asking when there are any. */
    fun resetReal(confirmed: Boolean = false) {
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

    fun cancelResetReal() = updateReal { it.copy(confirmReset = false) }

    /** Puts a drawing in the video, or takes it out (with its picture and clip); at most [MAX_REVEALS]. */
    fun toggleReveal(drawing: RecentDrawing) {
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

    private fun updateDrawing(id: String, change: (RealDrawing) -> RealDrawing) =
        updateProject { p -> p.copy(drawings = p.drawings.map { if (it.id == id) change(it) else it }) }

    /** The name on the card; empty makes the player anonymous. A leading @ is dropped (the card adds its own). */
    fun setPlayerName(id: String, text: String) = updateDrawing(id) { it.copy(nickname = text.trimStart('@', ' ').take(30)) }

    /**
     * How many seconds the drawing is said to have taken, by hand: the game's own figure is often longer than a quick
     * drawing really takes, and a shorter time makes the video more exciting. The countdown, the lines the assistant
     * writes, their voices and the picture prompt all read it from the drawing, so this is the one place it is set.
     */
    fun setDrawingSeconds(id: String, seconds: Int) {
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
    fun setPlayerLevel(id: String, level: Int) = updateDrawing(id) { it.copy(level = level.coerceIn(1, 999)) }

    fun setPlayerFrame(id: String, frameName: String?) = updateDrawing(id) { it.copy(frameId = frameName.orEmpty()) }

    /** Brings a picture from the gallery for the card's round slot. */
    fun importPlayerPhoto(id: String, uri: Uri) {
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

    fun clearPlayerPhoto(id: String) {
        updateDrawing(id) { it.copy(photo = null) }
        pruneRealMedia()
    }

    fun setRevealDescribe(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(describe = text.take(800)) }
    fun setRevealMotion(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(motion = text.take(1800)) }
    fun setRevealKind(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(kind = text.take(40)) }
    fun setRevealParts(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(parts = text.take(1000)) }
    fun setRevealMaterial(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(material = text.take(400)) }
    fun setRevealScene(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(scene = text.take(400)) }
    fun setRevealSize(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(size = text.take(200)) }
    fun setRevealScaleHint(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(scaleHint = text.take(200)) }
    fun setRevealFixed(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(fixed = text.take(400)) }

    /** The scene ideas are being written for this drawing (null: none). */
    private fun setPitching(sampleId: String?, error: String? = null) = updateReal { it.copy(pitching = sampleId, pitchError = error) }

    /**
     * With the real picture in front of it, the assistant thinks up the video scene: three ideas with a joke each, all in one
     * shot and one place, the thing never changing. The picture is needed (it is what the scene is written for).
     */
    fun writePitches(sampleId: String) {
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
    fun choosePitch(sampleId: String, index: Int) = updateReveal(sampleId) { r ->
        r.pitches.getOrNull(index)?.let { r.copy(motion = it.scene.take(1800)) } ?: r
    }

    /**
     * Lets the assistant describe the chosen drawings (their shapes, a detailed scene for each video)
     * and write the post text. Optional: the same fields can be filled in by hand.
     */
    fun describeReal() {
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

    private fun updateLine(sampleId: String, kind: RealLine, saveNow: Boolean = false, change: (VoicedLine) -> VoicedLine) =
        updateReveal(sampleId, saveNow) { r -> r.withLine(kind, change(r.line(kind))) }

    private fun lineError(kind: RealLine, message: String) = updateReal { it.copy(lineErrors = it.lineErrors + (kind to message)) }

    /** Moves the preview to where [kind] shows: the very start for the hook, the closing for the closing. */
    /** Moves the preview to where the voice step being set shows. */
    private fun previewVoiceStep(n: Int) = when (n) {
        1 -> previewLine(RealLine.Hook)
        2 -> previewLine(RealLine.Narration)
        3 -> previewLine(RealLine.Outro)
        5 -> previewLine(RealLine.End)
        else -> setPreviewTime(0L)
    }

    private fun previewLine(kind: RealLine) {
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
    fun writeLines(kind: RealLine) {
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
    fun chooseLine(kind: RealLine, text: String) {
        val id = _state.value.real.project?.opening?.sampleId ?: return
        updateLine(id, kind) { it.withText(text) }
        previewLine(kind)
    }

    /** No hook (the video starts on the clip) or no closing (it ends with the clip). */
    fun clearLine(kind: RealLine) {
        val id = _state.value.real.project?.opening?.sampleId ?: return
        updateLine(id, kind) { it.withText("") }
    }

    /**
     * Rewrites one idea by hand; emptied, it leaves the list. When it is the line in the video, the video takes the new
     * words, and its voice goes with the old ones.
     */
    fun editLineOption(kind: RealLine, old: String, text: String) {
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
    fun addLineOption(kind: RealLine, text: String) {
        val line = text.trim().take(RealCopy.maxChars(kind))
        val id = _state.value.real.project?.opening?.sampleId ?: return
        if (line.isEmpty()) return
        updateLine(id, kind) { l -> if (line in l.options) l else l.copy(options = listOf(line) + l.options) }
    }

    /**
     * Voices one line in ElevenLabs with the current voice settings and plays it. Credits are spent only here, on the
     * person's tap, and once per line and settings: the voice is kept on this phone.
     */
    fun voiceLine(kind: RealLine, text: String) {
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
    fun listenLine(kind: RealLine, text: String) {
        val l = _state.value.real.project?.opening?.line(kind)
        val inVideo = l?.takeIf { it.text == text }?.voice?.let { realStore.file(it) }?.takeIf { it.exists() }
        (inVideo ?: voiceBank.cached(RealCopy.spoken(kind, text), _state.value.eleven.spec))?.let { play(it) }
    }

    /**
     * Puts a line into the video with its voice: the voice is copied into the project (so changing the voice settings
     * later does not take it away), and the opening or the closing lasts until it has been said.
     */
    fun addLineToVideo(kind: RealLine, text: String) {
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
    fun setMusic(name: String?) {
        val id = _state.value.real.project?.opening?.sampleId ?: return
        updateReveal(id) { it.copy(music = name) }
    }

    fun setMusicVolume(volume: Float) {
        val id = _state.value.real.project?.opening?.sampleId ?: return
        updateReveal(id) { it.copy(musicVolume = volume.coerceIn(0f, RealMix.MAX_VOLUME)) }
    }

    /** Plays a track on the speaker to choose by; the same one again stops it. */
    fun listenMusic(name: String) {
        if (_state.value.real.musicPlaying == name) { stopPlaying(); return }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { runCatching { RealMusic.ensure(context, name) }.getOrNull() }
                ?: run { _state.update { it.copy(message = "Müzik açılamadı") }; return@launch }
            play(file) { updateReal { it.copy(musicPlaying = null) } }
            updateReal { it.copy(musicPlaying = name) }
        }
    }

    fun stopPlaying() {
        player?.release()
        player = null
        updateReal { it.copy(musicPlaying = null) }
    }

    /** Makes a drawing the one the video is made of (the first with a clip); its own hook and closing come with it. */
    fun moveRevealFirst(sampleId: String) {
        _state.update { it.copy(previewMs = 0L) }
        updateProject { p ->
            val r = p.reveals.find { it.sampleId == sampleId } ?: return@updateProject p
            p.copy(reveals = listOf(r) + p.reveals.filterNot { it.sampleId == sampleId })
        }
    }

    /** Draws a chosen drawing large on white for ChatGPT, with the prompt that keeps its oddities. */
    fun shareDoodle(sampleId: String) {
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

    fun clearShare() = updateReal { it.copy(share = null) }

    /**
     * Hands the realistic picture to Gemini with the video prompt. The picture has to travel with the
     * prompt: asked in words alone Gemini invents its own creature instead of moving this one.
     */
    fun shareToGemini(sampleId: String) {
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
    fun shareCarousel() {
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

    fun clearCarousel() = updateReal { it.copy(carousel = null) }

    /** A copy of [source] in the folder the share sheet may read from. */
    private fun copyToShare(source: File, name: String, clear: Boolean): File {
        val dir = File(context.cacheDir, "studio_share").apply { mkdirs() }
        if (clear) dir.listFiles()?.forEach { it.delete() }
        return File(dir, name + "." + source.extension.ifBlank { "jpg" }).also { source.copyTo(it, overwrite = true) }
    }

    fun imagePrompt(sampleId: String): String? {
        val p = _state.value.real.project ?: return null
        val d = p.drawing(sampleId) ?: return null
        val reveal = p.reveals.find { it.sampleId == sampleId }
        return RealCopy.imagePrompt(d.word, d.seconds.coerceAtLeast(1), reveal)
    }

    fun videoPrompt(sampleId: String): String? {
        val p = _state.value.real.project ?: return null
        val reveal = p.reveals.find { it.sampleId == sampleId } ?: return null
        return RealCopy.videoPrompt(p.drawing(sampleId)?.word.orEmpty(), reveal)
    }

    fun mediaFile(name: String): File = realStore.file(name)

    /** Brings the realistic picture made in ChatGPT into the project. */
    fun importImage(sampleId: String, uri: Uri) {
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
    fun setClipSeconds(sampleId: String, seconds: Double) {
        val r = _state.value.real.project?.reveals?.firstOrNull { it.sampleId == sampleId } ?: return
        val file = r.clip?.let { realStore.file(it) } ?: return
        viewModelScope.launch {
            val full = withContext(Dispatchers.IO) { RealImages.clipMs(file) }
            val ms = (seconds * 1000).toLong().coerceIn(RealTimeline.CLIP_MIN, maxOf(RealTimeline.CLIP_MIN, minOf(full, RealTimeline.CLIP_MAX)))
            updateProject { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(clipMs = ms) else it }) }
        }
    }

    /** Brings the clip made in Gemini into the project; only its first [RealTimeline.CLIP_MAX] ms are used. */
    fun importClip(sampleId: String, uri: Uri) {
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

    fun removeImage(sampleId: String) {
        updateProject(saveNow = true) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(image = null) else it }) }
        pruneRealMedia()
    }

    fun removeClip(sampleId: String) {
        updateProject(saveNow = true) { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) it.copy(clip = null, clipMs = 0L) else it }) }
        pruneRealMedia()
    }

    /** Opens the project's clips once for the preview; lets go of the ones it no longer uses. */
    private fun loadRealMedia(p: RealProject) {
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
    private fun pruneRealMedia() {
        val r = _state.value.real
        val keep = (r.project?.media ?: emptySet()) + r.library.flatMap { it.media }
        viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.prune(keep) } }
    }

    // ---- library

    fun toggleLibrary() = updateReal { it.copy(libraryShown = !it.libraryShown) }

    fun openLibraryItem(id: String?) = updateReal { it.copy(libraryView = id) }

    /** Ticks a piece to be part of the next video, or unticks it; at most [MAX_REVEALS], and only pieces with a clip. */
    fun toggleLibrarySelect(id: String) {
        val r = _state.value.real
        val item = r.library.find { it.id == id } ?: return
        if (id in r.librarySelected) { updateReal { it.copy(librarySelected = it.librarySelected - id) }; return }
        if (!item.hasClip) { _state.update { it.copy(message = "Bu kaydın videosu yok; yalnızca videolular birleşir") }; return }
        if (r.librarySelected.size >= MAX_REVEALS) { _state.update { it.copy(message = "En fazla $MAX_REVEALS video seçilir") }; return }
        updateReal { it.copy(librarySelected = it.librarySelected + id) }
    }

    /** Brings the ticked pieces back as the project, in the order they were ticked, each with its card, lines and voices; the video is the first one's. */
    fun libraryMakeVideo() {
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
    private fun updateLibraryItem(id: String, change: (LibraryItem) -> LibraryItem) {
        val library = _state.value.real.library.map { if (it.id == id) change(it) else it }
        updateReal { it.copy(library = library) }
        viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.saveLibrary(library) } }
    }

    /**
     * Notes that the video of the featured drawing is being posted on the chosen platform, with the hook it carries and
     * how it opens, so its numbers can be typed in later (see [VideoPost]). A video shared again to the same place within
     * half an hour is the same posting.
     */
    fun logPost() {
        val r = _state.value.real
        val f = r.project?.featured ?: return
        val item = r.library.find { it.id == f.sampleId } ?: return
        val now = System.currentTimeMillis()
        if (item.posts.any { it.platform == r.platform.name && now - it.postedAt < POST_SAME_MS }) return
        val post = VideoPost(java.util.UUID.randomUUID().toString(), r.platform.name, now, f.hook.text.trim(), f.opening.name)
        updateLibraryItem(item.id) { it.copy(posts = listOf(post) + it.posts) }
    }

    /** Types in (or corrects) a posting's result. */
    fun updatePost(itemId: String, postId: String, change: (VideoPost) -> VideoPost) =
        updateLibraryItem(itemId) { item -> item.copy(posts = item.posts.map { if (it.id == postId) change(it) else it }) }

    fun deletePost(itemId: String, postId: String) =
        updateLibraryItem(itemId) { item -> item.copy(posts = item.posts.filterNot { it.id == postId }) }

    /** Takes a piece out of the library and deletes its files, unless the current video still uses them. */
    fun deleteLibraryItem(id: String) {
        val library = _state.value.real.library.filterNot { it.id == id }
        updateReal { it.copy(library = library, librarySelected = it.librarySelected - id, libraryView = null, libraryBytes = realStore.sizeOf(library.flatMap { i -> i.media })) }
        viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.saveLibrary(library) } }
        pruneRealMedia()
    }

    fun libraryFile(name: String): File = realStore.file(name)

    /** What one piece's files take on this phone. */
    fun libraryBytesOf(item: LibraryItem): Long = realStore.sizeOf(item.media)

    // ---- voice

    fun setVoiceOn(on: Boolean) { _state.update { it.copy(voiceOn = on, video = null) } }
    fun setVoice(pitch: Float, rate: Float) { _state.update { it.copy(voicePitch = pitch, voiceRate = rate, video = null) } }

    private fun elevenKey(): String = com.sualtikasifi.cizimhafiza.util.SecretStore.get(prefs(), EL_KEY).trim()
    private fun elevenClient(): ElevenLabs? = elevenKey().takeIf { it.isNotBlank() }?.let { ElevenLabs(it) }
    private fun updateEleven(change: (ElevenUi) -> ElevenUi) = _state.update { it.copy(eleven = change(it.eleven)) }

    /** The voice settings kept on this phone; ElevenLabs is the default once a key is in. */
    private fun loadVoiceSettings() {
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

    fun setVoiceEngine(engine: VoiceEngine) {
        prefs().edit().putString(VOICE_ENGINE, engine.name).apply()
        _state.update { it.copy(voiceEngine = engine, video = null, voiceError = null) }
        if (engine == VoiceEngine.ElevenLabs) refreshEleven()
    }

    /** Keeps the key on this phone only (never in the app, the backup or the database), then checks it. */
    fun setElevenKey(raw: String) {
        val key = raw.trim()
        com.sualtikasifi.cizimhafiza.util.SecretStore.put(prefs(), EL_KEY, key)
        updateEleven { it.copy(keySet = key.isNotBlank(), keyHint = key.takeLast(4), voices = emptyList(), status = null) }
        if (key.isNotBlank()) refreshEleven()
    }

    fun setElevenVoice(id: String) {
        val v = id.trim()
        prefs().edit().putString(EL_VOICE, v).apply()
        updateEleven { it.copy(voiceId = v) }
        _state.update { it.copy(video = null) }
        refreshVoicePlan()
    }

    fun setElevenModel(model: VoiceModel) {
        prefs().edit().putString(EL_MODEL, model.id).apply()
        updateEleven { it.copy(model = model) }
        _state.update { it.copy(video = null) }
        refreshVoicePlan()
    }

    fun setElevenStability(v: Float) {
        prefs().edit().putFloat(EL_STABILITY, v).apply()
        updateEleven { it.copy(stability = v) }
        _state.update { it.copy(video = null) }
        refreshVoicePlan()
    }

    /**
     * The language of the videos. The script already written stays as it is (rewrite it to have it in the new language);
     * a line voiced in another language is another file, so nothing already paid for is lost by switching back.
     */
    fun setLang(lang: StudioLang) {
        prefs().edit().putString(STUDIO_LANG, lang.code).apply()
        updateEleven { it.copy(lang = lang) }
        // the phone's voice is opened for one language
        listener?.close()
        listener = null
        _state.update { it.copy(video = null) }
        refreshVoicePlan()
    }

    fun setElevenTags(on: Boolean) {
        prefs().edit().putBoolean(EL_TAGS, on).apply()
        updateEleven { it.copy(tags = on) }
        _state.update { it.copy(video = null) }
        refreshVoicePlan()
    }

    /** The account's voices and this month's credits, where the key is allowed to read them. */
    fun refreshEleven() {
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

    private fun play(file: File, onDone: () -> Unit = {}) {
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
    fun listen(line: SpokenLine) {
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
    fun confirmListen() {
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
    fun listenFree() {
        val line = _state.value.eleven.confirm ?: return
        updateEleven { it.copy(confirm = null) }
        speakOnPhone(line)
    }

    fun cancelListen() = updateEleven { it.copy(confirm = null) }

    /**
     * Voices every line of the current script that is not voiced yet with these settings — the only
     * way a whole script is ever sent to ElevenLabs, and only when the person presses the button.
     */
    fun voiceAll() {
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
    private fun refreshVoicePlan(s: StudioState = _state.value) {
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
    private fun refreshEndVoices(spec: VoiceSpec = _state.value.eleven.spec) {
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
    fun exportEndVoices(uri: Uri) {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { voiceBank.exportKept(it) } ?: error("Dosya açılamadı") }
            }
            _state.update { it.copy(message = n.fold({ c -> "$c kapanış sesi yedeklendi" }, { e -> "Yedeklenemedi: ${e.message}" })) }
        }
    }

    /** Kept closing voices read back from a backup file made by [exportEndVoices]. */
    fun importEndVoices(uri: Uri) {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { voiceBank.importKept(it) } ?: error("Dosya açılamadı") }
            }
            _state.update { it.copy(message = n.fold({ c -> if (c > 0) "$c kapanış sesi geri yüklendi" else "Dosyada kapanış sesi yok" }, { e -> "Yüklenemedi: ${e.message}" })) }
            refreshVoicePlan()
        }
    }

    /** A music file from the phone, added to the tracks to choose from; it is copied into the app and kept. */
    fun addMusic(uri: Uri) {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { RealMusic.import(context, uri) } }
            r.onSuccess { name ->
                updateReal { it.copy(customMusic = RealMusic.custom(context)) }
                // a track that was just added is meant to be used, and heard
                setMusic(name)
                _state.update { it.copy(message = "Müzik eklendi: ${RealMusic.label(name)}") }
            }.onFailure { e -> _state.update { it.copy(message = "Müzik eklenemedi: ${e.message}") } }
        }
    }

    /** Removes a track the person added; a video that used it goes back to no music. */
    fun removeMusic(name: String) {
        viewModelScope.launch {
            if (_state.value.real.musicPlaying == name) stopPlaying()
            withContext(Dispatchers.IO) { runCatching { RealMusic.remove(context, name) } }
            if (_state.value.real.project?.featured?.music == name) setMusic(null)
            updateReal { it.copy(customMusic = RealMusic.custom(context)) }
        }
    }

    private fun speakOnPhone(line: SpokenLine) {
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

    // ---- preview

    fun setPreviewTime(ms: Long) {
        _state.update { it.copy(previewMs = ms) }
        refreshPreview()
    }

    private fun refreshPreview() {
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

    private fun assetsOrLoad(): StudioAssets = assets ?: StudioAssets(context).also { assets = it }

    // ---- the video

    /**
     * The video of the current format, drawn from the current state; null while something it needs
     * is missing. [clipSource] opens an imported clip by file name: quick key frames for the
     * preview, every frame for the real thing.
     */
    private fun videoFor(
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
                val drawing = s.drawings.find { it.id == script.drawingId } ?: return null
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
    private fun linesFor(s: StudioState): List<SpokenLine>? = when (s.format) {
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

    private companion object {
        const val EL_KEY = "elevenlabs_key"
        const val EL_VOICE = "elevenlabs_voice"
        const val EL_MODEL = "elevenlabs_model"
        const val EL_STABILITY = "elevenlabs_stability"
        const val EL_TAGS = "elevenlabs_tags"
        const val STUDIO_LANG = "studio_lang"
        const val VOICE_ENGINE = "studio_voice_engine"
        const val CARD_SCALE = "studio_real_card_scale"
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
