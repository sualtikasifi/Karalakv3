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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotSprites
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.PlayerCard
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.ArchiveRun
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryArchive
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryPick
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemorySample
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemorySource
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.memory.MemoryWordStat
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipAudio
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.CardLayout
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.LibraryItem
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
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
    Guess("Karalak Bilir mi?", "Karalak çizimleri canlı izleyip tahmin ediyor"),
    Memory("Hafıza Tuzağı", "Çizenlerin çoğu kendi çizimini tanıyamadı"),
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
    val spec: VoiceSpec get() = VoiceSpec(voiceId, model, stability.toDouble(), tags)
}

/** Whose drawings "Karalak Bilir mi?" uses: this phone's own, or a player's who agreed to appear in videos. */
enum class GuessSource { Mine, Players }

/** A player who turned on "Videolarda öne çık", with their drawings from the reviewed Quick Match pool. */
data class FeaturedRuns(val player: com.sualtikasifi.cizimhafiza.domain.model.FeaturedPlayer, val drawings: List<StudioDrawing>)

/** Which spoken line of the script an edit is about. */
sealed interface LineRef {
    data object Intro : LineRef
    data object Outro : LineRef
    data class Stage(val round: Int, val stage: Int) : LineRef
    data class Reaction(val round: Int) : LineRef
    data object MemoryHook : LineRef
    data object MemoryParadox : LineRef
    data object MemoryOutro : LineRef
    data class MemoryPickLine(val pick: Int) : LineRef
}

/** "Hafıza Tuzağı": the archive's numbers, the chosen word, its kept drawings, the picks and the script. */
data class MemoryUi(
    val roundsSeen: Int = 0,
    val words: List<MemoryWordStat> = emptyList(),
    val scanning: Boolean = false,
    val scanStatus: String? = null,
    val selected: MemoryWordStat? = null,
    val samples: List<MemorySample> = emptyList(),
    val script: MemoryScript? = null,
    val writing: Boolean = false,
    val writeError: String? = null
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
    /** The drawing whose picture, clip or reaction is being worked on. */
    val busy: String? = null,
    /** Starting over while the project has pictures in it: asks before throwing them away. */
    val confirmReset: Boolean = false,
    /** Where the Karalak card sits on the video and how big it is, kept on this phone. */
    val card: CardLayout = CardLayout(),
    /** A picture ready to hand to ChatGPT or Gemini, with the prompt to paste. */
    val share: RealShare? = null,
    /** The swipe-through post: each drawing followed by what it became, ready for the share sheet. */
    val carousel: List<File>? = null
)

enum class ShareTarget { ChatGpt, Gemini }

class RealShare(val file: File, val prompt: String, val target: ShareTarget = ShareTarget.ChatGpt)

data class StudioState(
    val format: StudioFormat = StudioFormat.Guess,
    val loading: Boolean = true,
    /** This phone's own drawings. */
    val drawings: List<StudioDrawing> = emptyList(),
    val source: GuessSource = GuessSource.Mine,
    val featured: List<FeaturedRuns> = emptyList(),
    val featuredLoading: Boolean = false,
    val featuredStatus: String? = null,
    val featuredUid: String? = null,
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
    val memory: MemoryUi = MemoryUi(),
    val real: RealUi = RealUi()
) {
    /** The drawings on offer: this phone's, or the chosen featured player's. */
    val pool: List<StudioDrawing> get() = if (source == GuessSource.Mine) drawings else featured.find { it.player.uid == featuredUid }?.drawings.orEmpty()
    val chosen: List<StudioDrawing> get() = selected.mapNotNull { id -> pool.find { it.id == id } }
    val canWrite: Boolean get() = selected.isNotEmpty() && !writing
    val ready: Boolean get() = !rendering && when (format) {
        StudioFormat.Guess -> script != null
        StudioFormat.Memory -> memory.script?.picks?.isNotEmpty() == true
        StudioFormat.RealLife -> real.project?.shown?.isNotEmpty() == true
    }
}

private const val DEFAULT_TAGS = "#çizimoyunu #karalak #yapayzeka #oyunönerisi #çizim"
private const val MAX_DRAWINGS = 5

/**
 * The video studio, in three formats:
 *
 * - "Karalak Bilir mi?": Karalak watches the player's drawings build up and guesses out loud, on
 *   what looks like a video call. The drawings are this phone's own, or those of a player who
 *   turned on "Videolarda öne çık" (reviewed pool rounds only, name and level but never a photo).
 * - "Hafıza Tuzağı": how many could not recognise their own drawing of a word, from the anonymous
 *   archive the panel keeps.
 * - "Gerçek Olsaydı": quick drawings of one word from that archive with Karalak's jokes, then some
 *   of them as real pictures and clips made in ChatGPT and Gemini and brought back here.
 *
 * The assistant writes Karalak's lines from the drawings themselves, the phone's Turkish voice
 * says them, and the phone renders the 1080×1920 video with sound.
 */
@HiltViewModel
class StudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val drawingResultDao: DrawingResultDao,
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
    private val wordDao: WordDao,
    private val moderation: ModerationRepository,
    private val archive: MemoryArchive
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
    private var player: MediaPlayer? = null
    private val voiceBank = VoiceBank(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val realStore = RealStore(context)
    private var realSaveJob: Job? = null
    /** Imported pictures by file name, decoded once. */
    /** Imported clips by file name, opened for quick preview frames. */
    private val previewClips = ConcurrentHashMap<String, ClipFrames>()

    init {
        loadDrawings()
        loadPlayer()
        loadMascot()
        refreshMemory()
        loadReal()
        loadVoiceSettings()
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
            _state.update { it.copy(player = if (it.source == GuessSource.Players) it.player else if (it.showName) cards.first else cards.second) }
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

    fun setFormat(f: StudioFormat) {
        _state.update { it.copy(format = f, video = null, renderError = null, previewMs = 0L) }
        if (f == StudioFormat.Memory) refreshMemory()
        if (f == StudioFormat.RealLife && _state.value.real.recent.isEmpty()) refreshRecent()
        refreshPreview()
    }

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

    /** Switches between this phone's drawings and featured players'; a new source starts a new script. */
    fun setSource(source: GuessSource) {
        if (_state.value.source == source) return
        _state.update {
            it.copy(
                source = source, selected = emptyList(), script = null, video = null, renderError = null,
                player = if (source == GuessSource.Mine) (if (it.showName) ownCard else anonymousCard) else null,
                featuredUid = if (source == GuessSource.Mine) it.featuredUid else null
            )
        }
        if (source == GuessSource.Players && _state.value.featured.isEmpty()) loadFeatured()
        refreshPreview()
    }

    /**
     * Finds the players who agreed to appear in videos, and their drawings in the reviewed Quick Match pool (only
     * reviewed rounds: nothing a reviewer has not looked at goes into a video).
     */
    fun loadFeatured() {
        if (_state.value.featuredLoading) return
        _state.update { it.copy(featuredLoading = true, featuredStatus = "İzin veren oyuncular aranıyor…") }
        viewModelScope.launch {
            val outcome = runCatching {
                val players = moderation.featuredPlayers().getOrThrow().associateBy { it.uid }
                if (players.isEmpty()) return@runCatching emptyList<FeaturedRuns>()
                val byUid = LinkedHashMap<String, MutableList<StudioDrawing>>()
                // negative, so they never meet this phone's own (positive Room ids); counted, so no two are alike
                var nextId = -1L
                var cursor: Long? = null
                var read = 0
                for (page in 0 until FEATURED_POOL_PAGES) {
                    val p = moderation.poolRuns(SCAN_PAGE, cursor).getOrThrow()
                    read += p.runs.size
                    p.runs.filter { it.uid in players }.forEach { run ->
                        val list = byUid.getOrPut(run.uid) { mutableListOf() }
                        run.items.forEach { item ->
                            val strokes = item.strokes.filter { it.isNotEmpty() }
                            if (strokes.isNotEmpty() && item.word.isNotBlank()) {
                                list += StudioDrawing(nextId--, item.word, "", strokes, item.isCorrect)
                            }
                        }
                    }
                    _state.update { it.copy(featuredStatus = "${players.size} oyuncu izin vermiş · havuzda $read tur okundu") }
                    if (p.endReached || p.nextCursor == null) break
                    cursor = p.nextCursor
                }
                byUid.mapNotNull { (uid, list) -> players[uid]?.let { FeaturedRuns(it, list.take(MAX_FEATURED_DRAWINGS)) } }
                    .sortedByDescending { it.drawings.size }
            }
            _state.update {
                it.copy(
                    featuredLoading = false,
                    featured = outcome.getOrDefault(it.featured),
                    // a new search numbers the drawings afresh, so an old selection would point at others
                    selected = if (outcome.isSuccess && it.source == GuessSource.Players) emptyList() else it.selected,
                    script = if (outcome.isSuccess && it.source == GuessSource.Players) null else it.script,
                    featuredStatus = outcome.fold(
                        onSuccess = { list -> if (list.isEmpty()) "Henüz havuzda çizimi olan, izin vermiş oyuncu yok." else "${list.size} oyuncu · ${list.sumOf { f -> f.drawings.size }} çizim" },
                        onFailure = { e -> "Okunamadı: ${e.message}" }
                    )
                )
            }
        }
    }

    /** Opens one featured player: their drawings, and their tile with their name, level and frame — never a photo. */
    fun selectFeatured(uid: String) {
        val f = _state.value.featured.find { it.player.uid == uid } ?: return
        _state.update { it.copy(featuredUid = uid, selected = emptyList(), script = null, video = null, player = null) }
        viewModelScope.launch {
            val card = withContext(Dispatchers.IO) {
                val level = f.player.level
                val frame = AvatarFrame.resolve(f.player.frameId, level)
                val frameBitmap = runCatching { BitmapFactory.decodeResource(context.resources, frame.drawableRes) }.getOrNull()
                val dino = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.avatar_dino) }.getOrNull()
                PlayerCard(f.player.nickname, level, frameBitmap, frame.faceDiameterFraction, frame.faceOffsetXFraction, frame.faceOffsetYFraction, dino)
            }
            _state.update { if (it.featuredUid == uid) it.copy(player = card) else it }
            refreshPreview()
        }
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
            if (ref is LineRef.MemoryHook || ref is LineRef.MemoryParadox || ref is LineRef.MemoryOutro || ref is LineRef.MemoryPickLine) {
                val ms = s.memory.script ?: return@update s
                val nextMemory = when (ref) {
                    LineRef.MemoryHook -> ms.copy(hook = change(ms.hook))
                    LineRef.MemoryParadox -> ms.copy(paradox = change(ms.paradox))
                    LineRef.MemoryOutro -> ms.copy(outro = change(ms.outro))
                    is LineRef.MemoryPickLine -> ms.copy(picks = ms.picks.mapIndexed { i, p ->
                        if (i == ref.pick) p.copy(comment = change(p.comment ?: SpokenLine("", MascotEmotion.Laughing)).takeIf { it.text.isNotBlank() }) else p
                    })
                    else -> ms
                }
                return@update s.copy(memory = s.memory.copy(script = nextMemory), video = null)
            }
            val sc = s.script ?: return@update s
            val next = when (ref) {
                LineRef.Intro -> sc.copy(intro = change(sc.intro))
                LineRef.Outro -> sc.copy(outro = change(sc.outro))
                is LineRef.Reaction -> sc.copy(rounds = sc.rounds.mapIndexed { i, r -> if (i == ref.round) r.copy(reaction = change(r.reaction)) else r })
                is LineRef.Stage -> sc.copy(rounds = sc.rounds.mapIndexed { i, r ->
                    if (i == ref.round) r.copy(stages = r.stages.mapIndexed { k, l -> if (k == ref.stage) change(l) else l }) else r
                })
                else -> sc
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

    // ---- Hafıza Tuzağı

    private fun updateMemory(change: (MemoryUi) -> MemoryUi) = _state.update { it.copy(memory = change(it.memory)) }

    /** Re-reads the archive's numbers: every word drawn at least [MEMORY_MIN_DRAWN] times. */
    fun refreshMemory() {
        viewModelScope.launch {
            val words = archive.words(MEMORY_MIN_DRAWN)
            val seen = archive.roundsSeen()
            updateMemory { m -> m.copy(words = words, roundsSeen = seen, selected = m.selected?.let { sel -> words.find { it.key == sel.key } ?: sel }) }
        }
    }

    /**
     * Reads the whole Quick Match pool, the queue and the daily challenges waiting for review into
     * the archive. Each round is counted once however often it is read, so running it again only
     * adds what is new.
     */
    fun scanAll() {
        if (_state.value.memory.scanning) return
        updateMemory { it.copy(scanning = true, scanStatus = "Havuz okunuyor…") }
        viewModelScope.launch {
            var added = 0
            var read = 0
            val outcome = runCatching {
                var cursor: Long? = null
                for (page in 0 until SCAN_POOL_PAGES) {
                    val p = moderation.poolRuns(SCAN_PAGE, cursor).getOrThrow()
                    read += p.runs.size
                    added += archive.ingest(p.runs.map { ArchiveRun(it.id, MemorySource.Match, it.items) })
                    updateMemory { it.copy(scanStatus = "Havuz: $read tur okundu, $added yeni") }
                    if (p.endReached || p.nextCursor == null) break
                    cursor = p.nextCursor
                }
                cursor = null
                for (page in 0 until SCAN_QUEUE_PAGES) {
                    val p = moderation.pendingRuns(SCAN_PAGE, cursor).getOrThrow()
                    read += p.runs.size
                    added += archive.ingest(p.runs.map { ArchiveRun(it.id, MemorySource.Match, it.items) })
                    if (p.endReached || p.nextCursor == null) break
                    cursor = p.nextCursor
                }
                val daily = moderation.pendingDailyChallenges(SCAN_DAILY).getOrThrow()
                read += daily.size
                added += archive.ingest(daily.map { ArchiveRun(it.id, MemorySource.Daily, it.items) })
            }
            updateMemory {
                it.copy(
                    scanning = false,
                    scanStatus = outcome.fold(
                        onSuccess = { "$read tur okundu, $added yeni tur eklendi" },
                        onFailure = { e -> "Durdu ($read okundu, $added eklendi): ${e.message}" }
                    )
                )
            }
            refreshMemory()
        }
    }

    /** Opens a word: its kept drawings, and a first script with the forgotten ones in the order they were kept. */
    fun selectWord(stat: MemoryWordStat) {
        viewModelScope.launch {
            val samples = archive.samples(stat.key)
            val drawSeconds = runCatching { wordDao.findByText(stat.word) }.getOrNull()
                ?.let { GameConstants.drawingDurationSeconds(it.difficulty) } ?: 7
            val picks = samples.filter { !it.correct }.take(DEFAULT_PICKS).map { MemoryPick(it.id, false, null) }
            val script = MemoryScript(
                word = stat.word, drawn = stat.drawn, forgot = stat.forgot, delaySec = stat.avgDelaySec, drawSeconds = drawSeconds,
                hook = SpokenLine(MemoryCopy.hookText(stat.word, stat.drawn, stat.forgot, stat.avgDelaySec), MascotEmotion.Shocked),
                picks = picks,
                paradox = SpokenLine(MemoryCopy.paradoxText(stat.word, drawSeconds), MascotEmotion.Thinking),
                outro = SpokenLine(MemoryCopy.OUTRO, MascotEmotion.Point)
            )
            updateMemory { it.copy(selected = stat, samples = samples, script = script, writeError = null) }
            _state.update { it.copy(video = null, previewMs = 0L) }
            refreshPreview()
        }
    }

    /** Adds a kept drawing to the run, or takes it out; at most [MAX_PICKS]. */
    fun togglePick(sample: MemorySample) {
        _state.update { s ->
            val ms = s.memory.script ?: return@update s
            val has = ms.picks.any { it.sampleId == sample.id }
            val picks = when {
                has -> ms.picks.filterNot { it.sampleId == sample.id }
                ms.picks.size >= MAX_PICKS -> return@update s.copy(message = "En fazla $MAX_PICKS çizim")
                else -> ms.picks + MemoryPick(sample.id, sample.correct, null)
            }
            s.copy(memory = s.memory.copy(script = ms.copy(picks = picks)), video = null)
        }
        refreshPreview()
    }

    /** Lets the assistant choose the most interesting forgotten drawings and give Karalak a line for each. */
    fun writeMemory() {
        val s = _state.value
        val stat = s.memory.selected ?: return
        val script = s.memory.script ?: return
        if (s.memory.writing) return
        val key = prefs().getString(AssistantViewModel.KEY, "").orEmpty()
        if (key.isBlank()) { updateMemory { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val model = prefs().getString(AssistantViewModel.MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL }
        updateMemory { it.copy(writing = true, writeError = null) }
        viewModelScope.launch {
            val candidates = s.memory.samples.filter { !it.correct }
            MemoryDirector(OpenRouterClient(key), model).pick(stat, candidates, DEFAULT_PICKS).onSuccess { w ->
                updateMemory { it.copy(writing = false, script = script.copy(picks = w.picks, outro = w.outro ?: script.outro)) }
                _state.update {
                    it.copy(
                        caption = w.caption.ifBlank { it.caption }, hashtags = w.hashtags.ifBlank { it.hashtags },
                        aiCost = w.costUsd, video = null
                    )
                }
                refreshPreview()
            }.onFailure { e -> updateMemory { it.copy(writing = false, writeError = e.message ?: "Seçilemedi") } }
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
            val item = LibraryItem(r.sampleId, existing?.savedAt ?: now, d, r)
            if (existing == null) list = listOf(item) + list
            else if (existing != item) list = list.map { if (it.id == r.sampleId) item else it }
        }
        return list
    }

    private fun updateReveal(sampleId: String, change: (RealReveal) -> RealReveal) =
        updateProject { p -> p.copy(reveals = p.reveals.map { if (it.sampleId == sampleId) change(it) else it }) }

    /** The project left from last time, if any, and the latest drawings read last time. */
    private fun loadReal() {
        val saved = prefs().let { CardLayout(it.getFloat(CARD_SCALE, CardLayout.DEFAULT_SCALE), it.getFloat(CARD_X, CardLayout.DEFAULT_X), it.getFloat(CARD_Y, CardLayout.DEFAULT_Y)).clamped() }
        viewModelScope.launch {
            val (p, recent) = withContext(Dispatchers.IO) { realStore.load() to realStore.loadRecent() }
            val library = withContext(Dispatchers.IO) { realStore.loadLibrary() }
            updateReal { it.copy(project = p, recent = recent, card = saved, library = library, libraryBytes = realStore.sizeOf(library.flatMap { i -> i.media })) }
            p?.let { loadRealMedia(it) }
        }
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
        updateReal { it.copy(project = null, confirmReset = false, writeError = null) }
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

    fun setRevealDescribe(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(describe = text.take(300)) }
    fun setRevealMotion(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(motion = text.take(300)) }
    fun setRevealWordEn(sampleId: String, text: String) = updateReveal(sampleId) { it.copy(wordEn = text.take(60)) }

    /**
     * Lets the assistant describe the chosen drawings (the word in English, their shapes, how each would move)
     * and write the post text. Optional: the same fields can be filled in by hand.
     */
    fun describeReal() {
        val s = _state.value
        val p = s.real.project ?: return
        if (s.real.writing) return
        val key = prefs().getString(AssistantViewModel.KEY, "").orEmpty()
        if (key.isBlank()) { updateReal { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val model = prefs().getString(AssistantViewModel.MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL }
        val candidates = p.reveals.mapNotNull { r -> p.drawing(r.sampleId) }
        if (candidates.isEmpty()) { updateReal { it.copy(writeError = "Önce en az bir çizim seç") }; return }
        updateReal { it.copy(writing = true, writeError = null) }
        viewModelScope.launch {
            RealDirector(OpenRouterClient(key), model).describe(candidates).onSuccess { w ->
                updateReal { it.copy(writing = false) }
                val byId = w.reveals.associateBy { it.sampleId }
                updateProject { proj ->
                    proj.copy(reveals = proj.reveals.map { r -> byId[r.sampleId]?.let { d -> r.copy(wordEn = d.wordEn, describe = d.describe, motion = d.motion) } ?: r })
                }
                _state.update { it.copy(caption = w.caption.ifBlank { it.caption }, hashtags = w.hashtags.ifBlank { it.hashtags }, aiCost = w.costUsd) }
            }.onFailure { e -> updateReal { it.copy(writing = false, writeError = e.message ?: "Yazılamadı") } }
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
            val prompt = RealCopy.imagePrompt(d.word, reveal?.wordEn.orEmpty(), d.seconds.coerceAtLeast(1), reveal?.describe.orEmpty())
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
            updateReal { it.copy(share = RealShare(file, RealCopy.videoPrompt(reveal.wordEn.ifBlank { p.drawing(sampleId)?.word.orEmpty() }, reveal.motion), ShareTarget.Gemini)) }
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
        return RealCopy.imagePrompt(d.word, reveal?.wordEn.orEmpty(), d.seconds.coerceAtLeast(1), reveal?.describe.orEmpty())
    }

    fun videoPrompt(sampleId: String): String? {
        val p = _state.value.real.project ?: return null
        val reveal = p.reveals.find { it.sampleId == sampleId } ?: return null
        return RealCopy.videoPrompt(reveal.wordEn.ifBlank { p.drawing(sampleId)?.word.orEmpty() }, reveal.motion)
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

    /** Makes the ticked pieces the current video, in the order they were ticked; the card, name and frame come with each. */
    fun libraryMakeVideo() {
        val r = _state.value.real
        val items = r.librarySelected.mapNotNull { id -> r.library.find { it.id == id } }.filter { it.hasClip }
        if (items.isEmpty()) { _state.update { it.copy(message = "Önce videosu olan kayıtları seç") }; return }
        val project = RealProject(drawings = items.map { it.drawing }, reveals = items.map { it.reveal })
        updateReal { it.copy(project = project, librarySelected = emptyList(), libraryShown = false) }
        _state.update { it.copy(video = null, previewMs = 0L, message = "${items.size} video hazırlandı; aşağıdan videoyu oluştur") }
        viewModelScope.launch { withContext(Dispatchers.IO) { runCatching { realStore.save(project) } } }
        loadRealMedia(project)
        refreshPreview()
    }

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

    private fun elevenKey(): String = prefs().getString(EL_KEY, "").orEmpty().trim()
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
                    tags = p.getBoolean(EL_TAGS, true)
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
        prefs().edit().putString(EL_KEY, key).apply()
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

    private fun play(file: File) {
        player?.release()
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(file.path)
                setOnCompletionListener { mp -> mp.release(); if (player === mp) player = null }
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
        val todo = lines.filter { voiceBank.cached(it, spec) == null }.distinctBy { ElevenLabs.textFor(it, spec.model, spec.tags) }
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
        val missing = lines.filter { voiceBank.cached(it, spec) == null }.distinctBy { ElevenLabs.textFor(it, spec.model, spec.tags) }
        updateEleven { it.copy(ready = lines.count { l -> voiceBank.cached(l, spec) != null }, missing = missing.size, missingCredits = missing.sumOf { l -> spec.credits(l) }) }
    }

    private fun speakOnPhone(line: SpokenLine) {
        viewModelScope.launch {
            val synth = listener ?: SpeechSynth.open(context).getOrElse { e ->
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

    /**
     * The video of the current format, drawn from the current state; null while something it needs
     * is missing. [clipSource] opens an imported clip by file name: quick key frames for the
     * preview, every frame for the real thing.
     */
    private fun videoFor(
        s: StudioState, voice: Map<Int, FloatArray>?, preview: Boolean,
        clipSource: (String) -> ClipFrames? = { previewClips[it] }
    ): VoicedVideo? {
        if (s.format == StudioFormat.RealLife) {
            val p = s.real.project ?: return null
            val shown = p.shown
            if (shown.isEmpty()) return null
            return RealRenderer(
                assetsOrLoad(), p,
                clips = shown.mapNotNull { r -> r.clip?.let(clipSource)?.let { r.sampleId to it } }.toMap(),
                clipFiles = shown.mapNotNull { r -> r.clip?.let { r.sampleId to realStore.file(it) } }.toMap(),
                clipGain = CLIP_GAIN,
                layout = s.real.card,
                photoFiles = p.drawings.mapNotNull { d -> d.photo?.let { d.id to realStore.file(it) } }.toMap()
            )
        }
        val sp = sprites ?: return null
        return when (s.format) {
            StudioFormat.Guess -> {
                val script = s.script ?: (if (preview) previewScript(s) else null) ?: return null
                val player = s.player ?: return null
                GuessRenderer(assetsOrLoad(), sp, script, s.pool.associateBy { it.id }, player, voice)
            }
            StudioFormat.Memory -> {
                val script = s.memory.script ?: return null
                val pickedIds = script.picks.map { it.sampleId }.toSet()
                val mosaic = s.memory.samples.filter { !it.correct && it.id !in pickedIds } + s.memory.samples.filter { !it.correct && it.id in pickedIds }
                MemoryRenderer(assetsOrLoad(), sp, script, s.memory.samples.associateBy { it.id }, mosaic.take(9), voice)
            }
            StudioFormat.RealLife -> null
        }
    }

    /** The current format's spoken lines, in the order the video says them. */
    private fun linesFor(s: StudioState): List<SpokenLine>? = when (s.format) {
        StudioFormat.Guess -> s.script?.lines()
        StudioFormat.Memory -> s.memory.script?.lines()
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
            val out = File(dir, "karalak_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4")
            val job = coroutineContext[Job]
            val result = runCatching {
                // 1. Karalak's voice, one file per line
                val voices = HashMap<Int, Pcm>()
                if (s.voiceOn && s.voiceEngine == VoiceEngine.ElevenLabs) {
                    // making the video never pays: it uses only lines already voiced with "Seslendir"
                    val spec = s.eleven.spec
                    val missing = lines.count { voiceBank.cached(it, spec) == null }
                    if (missing > 0) error("$missing cümlenin ElevenLabs sesi yok. Önce Ses bölümünde “Seslendir”e bas.")
                    lines.forEachIndexed { i, l ->
                        _state.update { it.copy(renderStage = "Karalak'ın sesi hazırlanıyor ${i + 1}/${lines.size}", progress = 0.15f * i / lines.size) }
                        val f = voiceBank.cached(l, spec) ?: error("“${l.text.take(30)}” sesi bulunamadı")
                        val pcm = withContext(Dispatchers.IO) { ClipAudio.decode(f, VOICE_MAX_MS) } ?: error("“${l.text.take(30)}” sesi çözülemedi")
                        voices[i] = PcmOps.resample(pcm, RATE)
                    }
                } else if (s.voiceOn && lines.isNotEmpty()) {
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
                // every frame of an imported clip, opened for this render only
                val renderClips = HashMap<String, ClipFrames?>()
                try {
                    withContext(Dispatchers.Default) {
                        // 2. timing from the real voice, the voice and any clip sound laid on one track, and that track encoded
                        val envelopes = if (s.voiceOn) voices.mapValues { (_, pcm) -> PcmOps.envelope(pcm, GuessRenderer.FPS) } else null
                        val renderer = videoFor(s, envelopes, preview = false) { name ->
                            renderClips.getOrPut(name) { runCatching { ClipFrames(realStore.file(name), exact = true) }.getOrNull() }
                        } ?: error("Video hazırlanamadı")
                        _state.update { it.copy(renderStage = "Ses birleştiriliyor", progress = 0.17f) }
                        val placed = mutableListOf<Placed>()
                        if (s.voiceOn) placed += renderer.voiceStarts.mapNotNull { (index, start) -> voices[index]?.let { Placed(it, start) } }
                        renderer.sounds.forEach { snd ->
                            runCatching { ClipAudio.decode(snd.file, snd.durationMs) }.getOrNull()?.let { placed += Placed(PcmOps.resample(it, RATE), snd.startMs, snd.gain) }
                        }
                        val audio = if (placed.isNotEmpty()) AacEncoder.encode(PcmOps.mix(renderer.totalMs, RATE, placed)) else null
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
                } finally {
                    renderClips.values.forEach { it?.close() }
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
        super.onCleared()
    }

    private companion object {
        const val EL_KEY = "elevenlabs_key"
        const val EL_VOICE = "elevenlabs_voice"
        const val EL_MODEL = "elevenlabs_model"
        const val EL_STABILITY = "elevenlabs_stability"
        const val EL_TAGS = "elevenlabs_tags"
        const val VOICE_ENGINE = "studio_voice_engine"
        const val CARD_SCALE = "studio_real_card_scale"
        const val CARD_X = "studio_real_card_x"
        const val CARD_Y = "studio_real_card_y"
        /** No line of Karalak's runs this long; a longer decode means something went wrong. */
        const val VOICE_MAX_MS = 60_000L
        /** The voice track's sample rate: what every AAC encoder takes. */
        const val RATE = 44_100
        /** A word shows up in "Hafıza Tuzağı" once this many have drawn it. */
        const val MEMORY_MIN_DRAWN = 5
        const val DEFAULT_PICKS = 6
        const val MAX_PICKS = 8
        const val SCAN_PAGE = 25
        const val SCAN_POOL_PAGES = 120
        const val SCAN_QUEUE_PAGES = 20
        const val SCAN_DAILY = 500
        /** Featured players' drawings are looked for in this many pages of the pool (newest first). */
        const val FEATURED_POOL_PAGES = 40
        const val MAX_FEATURED_DRAWINGS = 60
        /** "Gerçek Olsaydı": the latest pool drawings shown (read 10 rounds at a time), and how many go in one video. */
        const val RECENT_MAX = 100
        const val RECENT_PAGE = 10
        const val RECENT_PAGES = 6
        const val MAX_REVEALS = 5
        /** The clips' own sound, a little under Karalak's voice. */
        const val CLIP_GAIN = 0.85f
        val GOOGLE_PHOTO = Regex("^https://lh[0-9]+\\.googleusercontent\\.com/\\S*$")
    }
}
