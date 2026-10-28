package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Pcm
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.File
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * The frames of an imported clip, handed out by time. Two modes:
 *
 * - [exact] (making the video): every frame in order. On Android 9+ frames are decoded a few at a
 *   time in sequence, which is many times faster than seeking for each one.
 * - preview: the nearest key frame, scaled down, so dragging the preview slider stays quick.
 *
 * Calls are serialised: the preview may ask from two coroutines at once. Only the exact mode, used
 * by one render on one thread, recycles frames it has moved past; a preview frame may still be on
 * its way to the screen, so those are left to the garbage collector.
 */
class ClipFrames(file: File, private val exact: Boolean) : Closeable {
    private val mmr = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
    val durationMs: Long = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    private val frameCount: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: 0
    } else 0
    private val lock = Any()
    private var batchStart = -1
    private var batch: List<Bitmap> = emptyList()
    private var lastKey = Long.MIN_VALUE
    private var last: Bitmap? = null
    private var closed = false

    fun frameAt(ms: Long): Bitmap? {
        synchronized(lock) {
            if (closed) return null
            val at = ms.coerceIn(0L, max(0L, durationMs - 1))
            return runCatching {
                if (exact && frameCount > 0 && durationMs > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) sequential(at)
                else single(at)
            }.getOrNull()
        }
    }

    private fun sequential(ms: Long): Bitmap? {
        val index = (ms * frameCount / durationMs).toInt().coerceIn(0, frameCount - 1)
        if (index in batchStart until batchStart + batch.size) return batch[index - batchStart]
        batch.forEach { it.recycle() }
        batch = emptyList()
        val n = min(BATCH, frameCount - index)
        batch = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mmr.getFramesAtIndex(index, n) else emptyList()
        }.getOrDefault(emptyList())
        batchStart = index
        return batch.firstOrNull() ?: single(ms)
    }

    private fun single(ms: Long): Bitmap? {
        // preview: quarter-second steps are plenty and spare the decoder
        val key = if (exact) ms else ms / 250
        if (key == lastKey) return last
        val us = ms * 1000
        val bmp = if (exact) {
            mmr.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            mmr.getScaledFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 540, 960)
        } else {
            mmr.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
        if (exact) last?.takeIf { it !== bmp }?.recycle()
        last = bmp
        lastKey = key
        return bmp
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            if (exact) {
                batch.forEach { it.recycle() }
                last?.recycle()
            }
            batch = emptyList()
            last = null
            runCatching { mmr.release() }
        }
    }

    private companion object {
        /** Frames decoded per step: about 50 MB at 1080p, less at Gemini's 720p. */
        const val BATCH = 6
    }
}

/** A clip's own sound track, decoded to mono PCM so it can be mixed with Karalak's voice. */
object ClipAudio {
    /** Null when the clip has no sound; at most [maxMs] of it. */
    fun decode(file: File, maxMs: Long): Pcm? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            var float = false
            val dec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!).also { codec = it }
            dec.configure(format, null, null, 0)
            dec.start()
            val info = MediaCodec.BufferInfo()
            var out = ShortArray(rate * 2)
            var size = 0
            var inputDone = false
            var idle = 0
            while (true) {
                if (!inputDone) {
                    val inIndex = dec.dequeueInputBuffer(5_000)
                    if (inIndex >= 0) {
                        val buffer = dec.getInputBuffer(inIndex) ?: error("Klip sesi okunamadı")
                        val n = extractor.readSampleData(buffer, 0)
                        if (n < 0 || extractor.sampleTime > maxMs * 1000) {
                            dec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            dec.queueInputBuffer(inIndex, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = dec.dequeueOutputBuffer(info, 5_000)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> if (inputDone && ++idle > 400) break
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = dec.outputFormat
                        rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        float = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    }
                    outIndex >= 0 -> {
                        idle = 0
                        val buffer = dec.getOutputBuffer(outIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            buffer.order(ByteOrder.nativeOrder())
                            val frames = info.size / ((if (float) 4 else 2) * channels)
                            if (size + frames > out.size) out = out.copyOf(max(out.size * 2, size + frames))
                            for (f in 0 until frames) {
                                var sum = 0f
                                for (c in 0 until channels) sum += if (float) buffer.float * 32767f else buffer.short.toFloat()
                                out[size++] = (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }
                        }
                        dec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
            val keep = min(size.toLong(), maxMs * rate / 1000).toInt()
            return if (keep <= 0) null else Pcm(rate, out.copyOf(keep))
        } finally {
            codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
            extractor.release()
        }
    }
}

/**
 * The "Gerçek Olsaydı" project on this phone: its script and drawings in one JSON file, the imported
 * pictures and clips beside it. Kept on disk because the work happens across apps — out to ChatGPT
 * and Gemini and back — and Android may close the panel in between.
 */
class RealStore(context: Context) {
    private val root = File(context.filesDir, "studio_real").apply { mkdirs() }
    private val mediaDir = File(root, "media").apply { mkdirs() }
    private val projectFile = File(root, "project.json")
    private val recentFile = File(root, "recent.json")
    private val libraryFile = File(root, "library.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val resolver = context.contentResolver

    fun load(): RealProject? = runCatching { json.decodeFromString<RealProject>(projectFile.readText()) }.getOrNull()

    fun save(project: RealProject?) {
        if (project == null) projectFile.delete() else projectFile.writeText(json.encodeToString(RealProject.serializer(), project))
    }

    /** Everything kept for later: finished drawings with their pictures and clips, newest first. */
    fun loadLibrary(): List<LibraryItem> = runCatching { json.decodeFromString(ListSerializer(LibraryItem.serializer()), libraryFile.readText()) }.getOrDefault(emptyList())

    fun saveLibrary(list: List<LibraryItem>) {
        libraryFile.writeText(json.encodeToString(ListSerializer(LibraryItem.serializer()), list))
    }

    /** What the files of [names] take on this phone, in bytes. */
    fun sizeOf(names: Collection<String>): Long = names.sumOf { file(it).length() }

    /** The pool's latest drawings as last read, so the list shows at once and without a network. */
    fun loadRecent(): List<RecentDrawing> = runCatching { json.decodeFromString(ListSerializer(RecentDrawing.serializer()), recentFile.readText()) }.getOrDefault(emptyList())

    fun saveRecent(list: List<RecentDrawing>) {
        recentFile.writeText(json.encodeToString(ListSerializer(RecentDrawing.serializer()), list))
    }

    fun file(name: String): File = File(mediaDir, name)

    /** Copies a picked picture or clip into the project; returns its file name. */
    fun import(uri: Uri, prefix: String, ext: String): String {
        val name = prefix.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60) + "_" + System.currentTimeMillis() + "." + ext
        val target = file(name)
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: error("Dosya açılamadı")
        require(target.length() in 1..MAX_IMPORT_BYTES) { target.delete(); "Dosya boş ya da çok büyük (en fazla 300 MB)" }
        return name
    }

    /** Copies a file of this phone's (a voiced line, say) into the project; returns its file name. */
    fun copyIn(source: File, prefix: String, ext: String): String {
        val name = prefix.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60) + "_" + System.currentTimeMillis() + "." + ext
        source.copyTo(file(name), overwrite = true)
        return name
    }

    /**
     * The app's promo video that closes every video, kept beside the project rather than in its media folder, so no
     * project or library clean-up ever touches it; null when none was added.
     */
    fun promo(): File? = root.listFiles()?.firstOrNull { it.name.startsWith(PROMO + ".") && it.length() > 0 }

    /** Makes the picked video the promo, in place of any earlier one; returns its file. */
    fun setPromo(uri: Uri): File {
        val ext = extensionOf(uri, video = true)
        val tmp = File(root, "${PROMO}_new.part")
        resolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } ?: error("Dosya açılamadı")
        require(tmp.length() in 1..MAX_IMPORT_BYTES) { tmp.delete(); "Dosya boş ya da çok büyük (en fazla 300 MB)" }
        clearPromo()
        val target = File(root, "$PROMO.$ext")
        if (!tmp.renameTo(target)) { tmp.delete(); error("Video kaydedilemedi") }
        return target
    }

    fun clearPromo() {
        root.listFiles()?.filter { it.name.startsWith(PROMO + ".") }?.forEach { it.delete() }
    }

    /**
     * Deletes the imported files the project no longer uses. A file copied in the last few minutes
     * is spared: it may belong to an import that has not reached the project yet.
     */
    fun prune(keep: Set<String>) {
        val now = System.currentTimeMillis()
        mediaDir.listFiles()?.forEach { if (it.name !in keep && now - it.lastModified() > FRESH_MS) it.delete() }
    }

    fun extensionOf(uri: Uri, video: Boolean): String = when (resolver.getType(uri)) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg", "image/jpg" -> "jpg"
        "video/mp4" -> "mp4"
        "video/quicktime" -> "mov"
        "video/webm" -> "webm"
        else -> if (video) "mp4" else "jpg"
    }

    private companion object {
        const val PROMO = "promo"
        const val MAX_IMPORT_BYTES = 300L * 1024 * 1024
        const val FRESH_MS = 5 * 60_000L
    }
}

/** The app's own background music, offered to lay under a "Gerçek Olsaydı" video. */
object RealMusic {
    /** By name (the raw resource's), in the app's own playing order. */
    val TRACKS: List<Pair<String, Int>> = listOf(
        "music_1" to R.raw.music_1,
        "music_2" to R.raw.music_2,
        "music_3" to R.raw.music_3,
        "music_4" to R.raw.music_4,
        "music_5" to R.raw.music_5
    )

    fun label(name: String): String = "Müzik " + (TRACKS.indexOfFirst { it.first == name } + 1)

    /** Where the track is kept as a file (the decoder and the player need one); it may not be there yet, see [ensure]. */
    fun target(context: Context, name: String): File = File(File(context.cacheDir, "studio_music"), "$name.m4a")

    /** The track as a file, copied out of the app the first time it is asked for. */
    fun ensure(context: Context, name: String): File {
        val f = target(context, name)
        if (f.exists() && f.length() > 0) return f
        val id = TRACKS.find { it.first == name }?.second ?: error("Bilinmeyen müzik: $name")
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, f.name + ".part")
        context.resources.openRawResource(id).use { input -> tmp.outputStream().use { input.copyTo(it) } }
        if (!tmp.renameTo(f)) { tmp.delete(); error("Müzik hazırlanamadı") }
        return f
    }
}

object RealImages {
    /** A picture fit for the video: software-backed (the frames are drawn on the CPU), at most [maxSide] px. */
    fun load(file: File, maxSide: Int = 1600): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > maxSide) {
                    val s = maxSide.toFloat() / longest
                    decoder.setTargetSize((info.size.width * s).toInt().coerceAtLeast(1), (info.size.height * s).toInt().coerceAtLeast(1))
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    /** The clip's length, or 0 when it is not a playable video. */
    fun clipMs(file: File): Long = runCatching {
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(file.absolutePath)
            if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != "yes") 0L
            else mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            mmr.release()
        }
    }.getOrDefault(0L)

    /** The picture as a small PNG, for showing to the assistant. */
    fun pngForAssistant(bmp: Bitmap, size: Int = 512): ByteArray {
        val s = size.toFloat() / max(bmp.width, bmp.height)
        val small = if (s < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt().coerceAtLeast(1), (bmp.height * s).toInt().coerceAtLeast(1), true) else bmp
        val out = java.io.ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.PNG, 100, out)
        if (small !== bmp) small.recycle()
        return out.toByteArray()
    }
}
