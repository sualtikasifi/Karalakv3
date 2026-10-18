package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.ClipFrames
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDirector
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealDrawing
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealImages
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealProject
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRenderer
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealReveal
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealRoast
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealScript
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.real.RealStore
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
    data object RealHook : LineRef
    data object RealBridge : LineRef
    data object RealOutro : LineRef
    data class RealRoastLine(val roast: Int) : LineRef
    data class RealRevealLine(val reveal: Int) : LineRef
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

/** "Gerçek Olsaydı": words with enough kept drawings, the chosen word's drawings, and the project on this phone. */
data class RealUi(
    val words: List<MemoryWordStat> = emptyList(),
    val samples: List<MemorySample> = emptyList(),
    val project: RealProject? = null,
    val writing: Boolean = false,
    val writeError: String? = null,
    /** The drawing whose picture, clip or reaction is being worked on. */
    val busy: String? = null,
    val clipSound: Boolean = true,
    /** A word picked while the current project has pictures in it: asks before throwing them away. */
    val confirmWord: MemoryWordStat? = null,
    /** A drawing ready to hand to ChatGPT, with the prompt to paste. */
    val share: RealShare? = null
)

class RealShare(val file: File, val prompt: String)

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
        StudioFormat.RealLife -> real.project?.script?.roasts?.isNotEmpty() == true
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
    private val json = Json { ignoreUnknownKeys = true }
    private val realStore = RealStore(context)
    private var realSaveJob: Job? = null
    /** Imported pictures by file name, decoded once. */
    private val photoCache = ConcurrentHashMap<String, Bitmap>()
    /** Imported clips by file name, opened for quick preview frames. */
    private val previewClips = ConcurrentHashMap<String, ClipFrames>()

    init {
        loadDrawings()
        loadPlayer()
        loadMascot()
        refreshMemory()
        loadReal()
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
        if (f == StudioFormat.RealLife) refreshRealWords()
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
        if (ref is LineRef.RealHook || ref is LineRef.RealBridge || ref is LineRef.RealOutro || ref is LineRef.RealRoastLine || ref is LineRef.RealRevealLine) {
            updateRealScript { sc ->
                when (ref) {
                    LineRef.RealHook -> sc.copy(hook = change(sc.hook))
                    LineRef.RealBridge -> sc.copy(bridge = change(sc.bridge))
                    LineRef.RealOutro -> sc.copy(outro = change(sc.outro))
                    is LineRef.RealRoastLine -> sc.copy(roasts = sc.roasts.mapIndexed { i, r -> if (i == ref.roast) r.copy(line = change(r.line)) else r })
                    is LineRef.RealRevealLine -> sc.copy(reveals = sc.reveals.mapIndexed { i, r -> if (i == ref.reveal) r.copy(line = change(r.line)) else r })
                    else -> sc
                }
            }
            return
        }
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
            refreshRealWords()
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
        realSaveJob?.cancel()
        realSaveJob = viewModelScope.launch(Dispatchers.IO) {
            if (!saveNow) delay(400)
            runCatching { realStore.save(p) }
        }
        loadRealMedia(p)
        refreshPreview()
    }

    private fun updateRealScript(change: (RealScript) -> RealScript) = updateProject { it.copy(script = change(it.script)) }

    /** The project left from last time, if any, with its word's drawings. */
    private fun loadReal() {
        viewModelScope.launch {
            val p = withContext(Dispatchers.IO) { realStore.load() }
            val samples = p?.let { archive.samples(it.wordKey) }.orEmpty()
            updateReal { it.copy(project = p, samples = samples) }
            p?.let { loadRealMedia(it) }
            refreshRealWords()
        }
    }

    /** Every archived word with at least [REAL_MIN_SAMPLES] kept drawings, most drawings first. */
    fun refreshRealWords() {
        viewModelScope.launch {
            val words = archive.words(1).filter { it.samples >= REAL_MIN_SAMPLES }.sortedByDescending { it.samples }
            updateReal { it.copy(words = words) }
        }
    }

    /**
     * Opens a word. Its own project, if this is the word already open, is kept; another word starts
     * a new one — after asking, when the current one already has pictures in it.
     */
    fun selectRealWord(stat: MemoryWordStat, confirmed: Boolean = false) {
        val current = _state.value.real.project
        if (current != null && current.wordKey == stat.key) {
            viewModelScope.launch { val samples = archive.samples(stat.key); updateReal { it.copy(samples = samples, confirmWord = null) } }
            return
        }
        if (!confirmed && current != null && current.media.isNotEmpty()) {
            updateReal { it.copy(confirmWord = stat) }
            return
        }
        viewModelScope.launch {
            val samples = archive.samples(stat.key)
            val seconds = runCatching { wordDao.findByText(stat.word) }.getOrNull()?.let { GameConstants.drawingDurationSeconds(it.difficulty) } ?: 7
            val first = samples.take(MAX_ROASTS)
            val project = RealProject(
                wordKey = stat.key,
                script = RealScript(
                    word = stat.word, drawSeconds = seconds,
                    hook = SpokenLine(RealCopy.hookText(stat.word, seconds), MascotEmotion.Excited),
                    roasts = first.map { RealRoast(it.id, SpokenLine("", MascotEmotion.Laughing)) },
                    bridge = SpokenLine(RealCopy.BRIDGE, MascotEmotion.Thinking),
                    reveals = emptyList(),
                    outro = SpokenLine(RealCopy.OUTRO, MascotEmotion.Point)
                ),
                drawings = first.map { RealDrawing(it.id, it.strokes) }
            )
            updateReal { it.copy(samples = samples, project = project, confirmWord = null, writeError = null) }
            _state.update { it.copy(video = null, previewMs = 0L) }
            withContext(Dispatchers.IO) { runCatching { realStore.save(project); realStore.prune(emptySet()) } }
            loadRealMedia(project)
            refreshPreview()
        }
    }

    fun cancelRealWord() = updateReal { it.copy(confirmWord = null) }

    /** Puts a drawing in the run, or takes it out (and out of the reveals); at most [MAX_ROASTS]. */
    fun toggleRoast(id: String, strokes: List<List<DrawingPoint>>) {
        val p = _state.value.real.project ?: return
        val has = p.script.roasts.any { it.sampleId == id }
        if (!has && p.script.roasts.size >= MAX_ROASTS) { _state.update { it.copy(message = "En fazla $MAX_ROASTS çizim") }; return }
        updateProject { proj ->
            val sc = proj.script
            if (has) proj.copy(script = sc.copy(roasts = sc.roasts.filterNot { it.sampleId == id }, reveals = sc.reveals.filterNot { it.sampleId == id }))
            else proj.copy(
                script = sc.copy(roasts = sc.roasts + RealRoast(id, SpokenLine("", MascotEmotion.Laughing))),
                drawings = if (proj.drawing(id) != null) proj.drawings else proj.drawings + RealDrawing(id, strokes)
            )
        }
        pruneRealMedia()
    }

    /** Marks a drawing of the run to be turned real, or unmarks it; at most [MAX_REVEALS]. */
    fun toggleReveal(sampleId: String) {
        val p = _state.value.real.project ?: return
        val has = p.script.reveals.any { it.sampleId == sampleId }
        if (!has && p.script.reveals.size >= MAX_REVEALS) { _state.update { it.copy(message = "En fazla $MAX_REVEALS dönüşüm") }; return }
        updateRealScript { sc ->
            if (has) sc.copy(reveals = sc.reveals.filterNot { it.sampleId == sampleId })
            else sc.copy(reveals = sc.reveals + RealReveal(sampleId, SpokenLine("", MascotEmotion.Shocked)))
        }
        pruneRealMedia()
    }

    fun setRevealDescribe(sampleId: String, text: String) =
        updateRealScript { sc -> sc.copy(reveals = sc.reveals.map { if (it.sampleId == sampleId) it.copy(describe = text.take(300)) else it }) }

    fun setClipSound(on: Boolean) { updateReal { it.copy(clipSound = on) }; _state.update { it.copy(video = null) } }

    /** Lets the assistant pick the run, write a joke for each and choose which to turn real. */
    fun writeReal() {
        val s = _state.value
        val p = s.real.project ?: return
        if (s.real.writing) return
        val key = prefs().getString(AssistantViewModel.KEY, "").orEmpty()
        if (key.isBlank()) { updateReal { it.copy(writeError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val model = prefs().getString(AssistantViewModel.MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL }
        // the drawings already chosen first, then the rest of the word's drawings
        val candidates = (p.script.roasts.mapNotNull { r -> p.drawing(r.sampleId) } + s.real.samples.map { RealDrawing(it.id, it.strokes) })
            .distinctBy { it.id }
        updateReal { it.copy(writing = true, writeError = null) }
        viewModelScope.launch {
            RealDirector(OpenRouterClient(key), model).pick(p.script.word, p.script.drawSeconds, candidates, MAX_ROASTS, MAX_REVEALS).onSuccess { w ->
                updateReal { it.copy(writing = false) }
                updateProject { proj ->
                    val old = proj.script.reveals.associateBy { it.sampleId }
                    val used = (w.roasts.map { it.sampleId } + w.reveals.map { it.sampleId }).toSet()
                    proj.copy(
                        wordEn = w.wordEn.ifBlank { proj.wordEn },
                        script = proj.script.copy(
                            roasts = w.roasts,
                            // a drawing that already has its picture keeps it
                            reveals = w.reveals.map { r -> old[r.sampleId]?.let { o -> r.copy(image = o.image, clip = o.clip, clipMs = o.clipMs) } ?: r },
                            outro = w.outro ?: proj.script.outro
                        ),
                        drawings = (proj.drawings + candidates).distinctBy { it.id }.filter { it.id in used }
                    )
                }
                _state.update { it.copy(caption = w.caption.ifBlank { it.caption }, hashtags = w.hashtags.ifBlank { it.hashtags }, aiCost = w.costUsd) }
                pruneRealMedia()
            }.onFailure { e -> updateReal { it.copy(writing = false, writeError = e.message ?: "Yazılamadı") } }
        }
    }

    /** Draws a chosen drawing large on white for ChatGPT, with the prompt that keeps its oddities. */
    fun shareDoodle(sampleId: String) {
        val p = _state.value.real.project ?: return
        val d = p.drawing(sampleId) ?: return
        val reveal = p.script.reveals.find { it.sampleId == sampleId }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.cacheDir, "studio_share").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    File(dir, "karalak_cizim_${System.currentTimeMillis()}.png").apply { writeBytes(StrokeReplay.png(d.strokes, 1f, 1024)) }
                }.getOrNull()
            } ?: run { _state.update { it.copy(message = "Çizim hazırlanamadı") }; return@launch }
            val prompt = RealCopy.imagePrompt(p.script.word, p.wordEn, p.script.drawSeconds, reveal?.describe.orEmpty())
            updateReal { it.copy(share = RealShare(file, prompt)) }
        }
    }

    fun clearShare() = updateReal { it.copy(share = null) }

    fun imagePrompt(sampleId: String): String? {
        val p = _state.value.real.project ?: return null
        val reveal = p.script.reveals.find { it.sampleId == sampleId }
        return RealCopy.imagePrompt(p.script.word, p.wordEn, p.script.drawSeconds, reveal?.describe.orEmpty())
    }

    fun videoPrompt(): String = RealCopy.videoPrompt(_state.value.real.project?.wordEn.orEmpty())

    fun mediaFile(name: String): File = realStore.file(name)

    /** Brings the realistic picture made in ChatGPT into the project. */
    fun importImage(sampleId: String, uri: Uri) {
        if (_state.value.real.busy != null) return
        updateReal { it.copy(busy = sampleId) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val name = realStore.import(uri, sampleId + "_img", realStore.extensionOf(uri, video = false))
                    val bmp = RealImages.load(realStore.file(name)) ?: run { realStore.file(name).delete(); error("Görsel açılamadı") }
                    photoCache[name] = bmp
                    name
                }
            }
            updateReal { it.copy(busy = null) }
            result.onSuccess { name ->
                updateProject(saveNow = true) { p -> p.copy(script = p.script.copy(reveals = p.script.reveals.map { if (it.sampleId == sampleId) it.copy(image = name) else it })) }
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
                    p.copy(script = p.script.copy(reveals = p.script.reveals.map {
                        if (it.sampleId == sampleId) it.copy(clip = name, clipMs = ms.coerceAtMost(RealTimeline.CLIP_MAX)) else it
                    }))
                }
                pruneRealMedia()
            }.onFailure { e -> _state.update { it.copy(message = "Video eklenemedi: ${e.message}") } }
        }
    }

    fun removeImage(sampleId: String) {
        updateProject(saveNow = true) { p -> p.copy(script = p.script.copy(reveals = p.script.reveals.map { if (it.sampleId == sampleId) it.copy(image = null) else it })) }
        pruneRealMedia()
    }

    fun removeClip(sampleId: String) {
        updateProject(saveNow = true) { p -> p.copy(script = p.script.copy(reveals = p.script.reveals.map { if (it.sampleId == sampleId) it.copy(clip = null, clipMs = 0L) else it })) }
        pruneRealMedia()
    }

    /** Shows Karalak the drawing and its realistic picture, and has it react in one line. */
    fun reactReal(sampleId: String) {
        val s = _state.value
        val p = s.real.project ?: return
        if (s.real.busy != null) return
        val reveal = p.script.reveals.find { it.sampleId == sampleId } ?: return
        val photo = reveal.image?.let { photoCache[it] } ?: return
        val d = p.drawing(sampleId) ?: return
        val key = prefs().getString(AssistantViewModel.KEY, "").orEmpty()
        if (key.isBlank()) { _state.update { it.copy(message = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val model = prefs().getString(AssistantViewModel.MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL }
        updateReal { it.copy(busy = sampleId) }
        viewModelScope.launch {
            val png = withContext(Dispatchers.Default) { RealImages.pngForAssistant(photo) }
            RealDirector(OpenRouterClient(key), model).react(p.script.word, d, png).onSuccess { (line, cost) ->
                updateRealScript { sc -> sc.copy(reveals = sc.reveals.map { if (it.sampleId == sampleId) it.copy(line = line) else it }) }
                _state.update { it.copy(aiCost = cost) }
            }.onFailure { e -> _state.update { it.copy(message = e.message ?: "Tepki yazılamadı") } }
            updateReal { it.copy(busy = null) }
        }
    }

    /** Decodes the project's pictures and opens its clips once; lets go of the ones it no longer uses. */
    private fun loadRealMedia(p: RealProject) {
        val wanted = p.media
        photoCache.keys.filter { it !in wanted }.forEach { photoCache.remove(it) }
        previewClips.keys.filter { it !in wanted }.forEach { k -> previewClips.remove(k)?.close() }
        val missing = p.script.reveals.flatMap { r -> listOfNotNull(r.image?.takeIf { !photoCache.containsKey(it) }, r.clip?.takeIf { !previewClips.containsKey(it) }) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                p.script.reveals.forEach { r ->
                    r.image?.takeIf { !photoCache.containsKey(it) }?.let { name -> RealImages.load(realStore.file(name))?.let { photoCache[name] = it } }
                    r.clip?.takeIf { !previewClips.containsKey(it) }?.let { name ->
                        runCatching { ClipFrames(realStore.file(name), exact = false) }.getOrNull()?.let { if (previewClips.putIfAbsent(name, it) != null) it.close() }
                    }
                }
            }
            refreshPreview()
        }
    }

    private fun pruneRealMedia() {
        val keep = _state.value.real.project?.media ?: return
        viewModelScope.launch(Dispatchers.IO) { runCatching { realStore.prune(keep) } }
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
            StudioFormat.RealLife -> {
                val p = s.real.project ?: return null
                if (p.script.roasts.isEmpty()) return null
                val reveals = p.script.reveals
                RealRenderer(
                    assetsOrLoad(), sp, p.script, p.drawings.associateBy { it.id },
                    photos = reveals.mapNotNull { r -> r.image?.let { photoCache[it] }?.let { r.sampleId to it } }.toMap(),
                    clips = reveals.mapNotNull { r -> r.clip?.let(clipSource)?.let { r.sampleId to it } }.toMap(),
                    clipFiles = reveals.mapNotNull { r -> r.clip?.let { r.sampleId to realStore.file(it) } }.toMap(),
                    clipGain = if (s.real.clipSound) CLIP_GAIN else 0f,
                    voice = voice
                )
            }
        }
    }

    /** The current format's spoken lines, in the order the video says them. */
    private fun linesFor(s: StudioState): List<SpokenLine>? = when (s.format) {
        StudioFormat.Guess -> s.script?.lines()
        StudioFormat.Memory -> s.memory.script?.lines()
        StudioFormat.RealLife -> s.real.project?.script?.lines()
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
        // a line edited in the last moment would otherwise wait for a save that never comes
        _state.value.real.project?.let { p -> runCatching { realStore.save(p) } }
        previewClips.values.forEach { it.close() }
        previewClips.clear()
        super.onCleared()
    }

    private companion object {
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
        /** "Gerçek Olsaydı": a word needs this many kept drawings, the run this many, and this many turn real. */
        const val REAL_MIN_SAMPLES = 3
        const val MAX_ROASTS = 5
        const val MAX_REVEALS = 3
        /** The clips' own sound, a little under Karalak's voice. */
        const val CLIP_GAIN = 0.85f
        val GOOGLE_PHOTO = Regex("^https://lh[0-9]+\\.googleusercontent\\.com/\\S*$")
    }
}
