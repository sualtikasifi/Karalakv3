package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sualtikasifi.cizimhafiza.data.local.dao.DrawingResultDao
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.presentation.reports.console.AssistantViewModel
import com.sualtikasifi.cizimhafiza.presentation.reports.console.OpenRouterClient
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StudioState(
    val loading: Boolean = true,
    val drawings: List<StudioDrawing> = emptyList(),
    val template: StudioTemplate = StudioTemplate.Quiz,
    /** Chosen drawings, in the order they will play. */
    val selected: List<Long> = emptyList(),
    val hook: String = StudioTemplate.Quiz.defaultHook,
    val ask: String = StudioTemplate.Quiz.defaultAsk,
    val outro: String = DEFAULT_OUTRO,
    val caption: String = "",
    val hashtags: String = DEFAULT_TAGS,
    val aiRunning: Boolean = false,
    val aiError: String? = null,
    val aiCost: Double? = null,
    val preview: ImageBitmap? = null,
    val previewMs: Long = 0L,
    val totalMs: Long = 0L,
    val rendering: Boolean = false,
    val progress: Float = 0f,
    val video: File? = null,
    val renderError: String? = null,
    val message: String? = null
) {
    val chosen: List<StudioDrawing> get() = selected.mapNotNull { id -> drawings.find { it.id == id } }
    val ready: Boolean get() = chosen.size in template.minCount..template.maxCount

    /** What the grid offers: every drawing, or for a comparison only words drawn more than once. */
    val offered: List<StudioDrawing>
        get() = if (template != StudioTemplate.Versus) drawings
        else drawings.groupBy { it.word.lowercase(Locale("tr")) }.values.filter { it.size > 1 }.flatten()
}

private const val DEFAULT_OUTRO = "Sen de dene: karalak.com"
private const val DEFAULT_TAGS = "#çizimoyunu #karalak #oyunönerisi #mobiloyun #çizim"

/**
 * The video studio: turns drawings made on this phone into a vertical video with the opening line,
 * the question and the closing card, and hands it to Instagram, TikTok or the gallery.
 *
 * Only this phone's own drawings are offered — they are the maintainer's to post. Players'
 * drawings would need their permission first.
 */
@HiltViewModel
class StudioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val drawingResultDao: DrawingResultDao
) : ViewModel() {
    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private var assets: StudioAssets? = null
    private var previewJob: Job? = null
    private var renderJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                runCatching { drawingResultDao.getRecentWithWords(240) }.getOrDefault(emptyList()).mapNotNull { row ->
                    val strokes = runCatching { json.decodeFromString<List<DrawingStroke>>(row.pathDataJson) }.getOrNull()
                        ?.filter { it.isNotEmpty() }
                    if (strokes.isNullOrEmpty()) null
                    else StudioDrawing(row.id, row.word, row.category, strokes, row.isCorrect)
                }
            }
            _state.update { it.copy(loading = false, drawings = list) }
        }
    }

    fun setTemplate(t: StudioTemplate) {
        _state.update { s ->
            if (s.template == t) return@update s
            // keep wording the maintainer wrote; replace only the old template's defaults
            val hook = if (s.hook == s.template.defaultHook || s.hook.isBlank()) t.defaultHook else s.hook
            val ask = if (s.ask == s.template.defaultAsk || s.ask.isBlank()) t.defaultAsk else s.ask
            s.copy(template = t, selected = emptyList(), hook = hook, ask = ask, video = null, renderError = null)
        }
        refreshPreview()
    }

    fun toggle(d: StudioDrawing) {
        _state.update { s ->
            val sel = s.selected
            val next = when {
                d.id in sel -> sel - d.id
                sel.size >= s.template.maxCount -> if (s.template.maxCount == 1) listOf(d.id) else sel
                s.template == StudioTemplate.Versus && sel.isNotEmpty() &&
                    s.chosen.first().word.lowercase(Locale("tr")) != d.word.lowercase(Locale("tr")) -> listOf(d.id)
                else -> sel + d.id
            }
            val message = if (next == sel && d.id !in sel) "En fazla ${s.template.maxCount} çizim seçebilirsin" else null
            s.copy(selected = next, video = null, renderError = null, message = message)
        }
        refreshPreview()
    }

    fun setHook(v: String) { _state.update { it.copy(hook = v.take(140), video = null) }; refreshPreview() }
    fun setAsk(v: String) { _state.update { it.copy(ask = v.take(40), video = null) }; refreshPreview() }
    fun setOutro(v: String) { _state.update { it.copy(outro = v.take(60), video = null) }; refreshPreview() }
    fun setCaption(v: String) { _state.update { it.copy(caption = v.take(2_000)) } }
    fun setHashtags(v: String) { _state.update { it.copy(hashtags = v.take(300)) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }

    fun setPreviewTime(ms: Long) {
        _state.update { it.copy(previewMs = ms) }
        refreshPreview()
    }

    private fun spec(s: StudioState) = VideoSpec(s.template, s.chosen, StudioText(s.hook.trim(), s.ask.trim(), s.outro.trim()))

    /** Draws the frame under the slider, a moment after the last change so typing does not queue renders. */
    private fun refreshPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(80)
            val s = _state.value
            if (!s.ready) { _state.update { it.copy(preview = null, totalMs = 0L) }; return@launch }
            val image = withContext(Dispatchers.Default) {
                val renderer = StudioRenderer(assetsOrLoad(), spec(s))
                val t = s.previewMs.coerceIn(0L, renderer.totalMs)
                val bmp = Bitmap.createBitmap(StudioRenderer.W / 2, StudioRenderer.H / 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.scale(0.5f, 0.5f)
                renderer.draw(canvas, t)
                bmp.asImageBitmap() to renderer.totalMs
            }
            _state.update { it.copy(preview = image.first, totalMs = image.second) }
        }
    }

    private fun assetsOrLoad(): StudioAssets = assets ?: StudioAssets(context).also { assets = it }

    /** Asks the assistant for the opening line, the question, the closing line, the caption and the hashtags. */
    fun writeWithAi() {
        val s = _state.value
        if (s.aiRunning) return
        val prefs = context.getSharedPreferences(AssistantViewModel.PREFS, Context.MODE_PRIVATE)
        val key = prefs.getString(AssistantViewModel.KEY, "").orEmpty()
        if (key.isBlank()) { _state.update { it.copy(aiError = "Önce Asistan ayarlarından OpenRouter anahtarını gir.") }; return }
        val model = prefs.getString(AssistantViewModel.MODEL, OpenRouterClient.DEFAULT_MODEL).orEmpty().ifBlank { OpenRouterClient.DEFAULT_MODEL }
        _state.update { it.copy(aiRunning = true, aiError = null) }
        viewModelScope.launch {
            val words = s.chosen.joinToString { it.word }.ifBlank { "(henüz seçilmedi)" }
            val prompt = """
                Karalak için ${s.template.title} şablonunda dikey kısa video hazırlıyorum (${s.template.blurb}).
                Videodaki çizimlerin kelimeleri (izleyici bilmiyor, cevap bunlar): $words
                Çizim sayısı: ${s.chosen.size}

                Tam olarak şu beş satırı yaz, başka hiçbir şey yazma:
                KANCA: ilk 2 saniyede ekranda duran cümle; en fazla 70 karakter; merak uyandırsın; kelimeleri ASLA söyleme
                SORU: çizim bitince büyük yazan soru; en fazla 22 karakter
                KAPANIŞ: son ekrandaki çağrı; en fazla 32 karakter; karalak.com geçsin
                AÇIKLAMA: TikTok/Reels açıklaması; 1-2 doğal cümle; izleyiciyi yoruma çağıran bir soruyla bitsin; kelimeleri söyleme
                ETİKETLER: 5-6 etiket, # ile, Türkçe ağırlıklı
            """.trimIndent()
            val result = OpenRouterClient(key).generate(SYSTEM, prompt, model, webSearch = false)
            result.onSuccess { a ->
                val p = StudioCopy.parse(a.text)
                _state.update {
                    it.copy(
                        aiRunning = false, aiCost = a.costUsd,
                        hook = p.hook ?: it.hook, ask = p.ask ?: it.ask, outro = p.outro ?: it.outro,
                        caption = p.caption ?: it.caption, hashtags = p.hashtags ?: it.hashtags,
                        aiError = if (p.hook == null && p.caption == null) "Cevap beklenen biçimde gelmedi; tekrar dene." else null,
                        video = null
                    )
                }
                refreshPreview()
            }.onFailure { e ->
                _state.update { it.copy(aiRunning = false, aiError = e.message ?: "Yazılamadı") }
            }
        }
    }

    fun render() {
        val s = _state.value
        if (!s.ready || s.rendering) return
        _state.update { it.copy(rendering = true, progress = 0f, renderError = null, video = null) }
        renderJob = viewModelScope.launch {
            val dir = File(context.cacheDir, "studio").apply { mkdirs() }
            // a video is only kept until the next one: the gallery and the apps it is shared to keep their own copy
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "karalak_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4")
            val job = coroutineContext[Job]
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val renderer = StudioRenderer(assetsOrLoad(), spec(s))
                    VideoEncoder.encode(
                        out = out,
                        width = StudioRenderer.W,
                        height = StudioRenderer.H,
                        fps = StudioTimeline.FPS,
                        frames = StudioTimeline.frameCount(renderer.totalMs),
                        drawFrame = { canvas, t -> renderer.draw(canvas, t) },
                        onProgress = { p -> _state.update { it.copy(progress = p) } },
                        isCancelled = { job?.isActive == false }
                    )
                }
            }
            result.onSuccess { _state.update { it.copy(rendering = false, video = out, progress = 1f) } }
                .onFailure { e ->
                    val cancelled = e is kotlin.coroutines.cancellation.CancellationException
                    _state.update { it.copy(rendering = false, renderError = if (cancelled) null else (e.message ?: e::class.simpleName)) }
                }
        }
    }

    fun cancelRender() {
        renderJob?.cancel()
        _state.update { it.copy(rendering = false, progress = 0f) }
    }

    /** Copies the finished video into Movies/Karalak so it can be edited in CapCut or posted later. */
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
        super.onCleared()
    }

    private companion object {
        val SYSTEM = """
            Sen Karalak'ın sosyal medya yazarısın. Karalak bir çizim + hafıza oyunu: oyuncu kelimeleri çizer, kısa bir moladan sonra kendi çizimlerinin ne olduğunu hatırlamaya çalışır; komedi kendi çizimini tanıyamamaktan doğar. Web'de kurulumsuz oynanır: karalak.com. Android'de Google Play'de "Karalak".
            Türkçe, doğal, gençlerin konuştuğu gibi yaz; ajans dili ve abartı yok, emoji en fazla bir tane. İstenen biçimin dışına çıkma.
        """.trimIndent()
    }
}
