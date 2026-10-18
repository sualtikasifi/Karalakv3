package com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sualtikasifi.cizimhafiza.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Karalak's moods. [key] names the sprite files (`thinking_open.webp`), and is also the word the
 * assistant answers with, so the two must never drift apart.
 */
enum class MascotEmotion(val key: String, val label: String) {
    Neutral("neutral", "Sakin"),
    Thinking("thinking", "Düşünüyor"),
    Suspicious("suspicious", "Şüpheli"),
    Excited("excited", "Heyecanlı"),
    Confident("confident", "Emin"),
    Shocked("shocked", "Şaşkın"),
    Embarrassed("embarrassed", "Utanmış"),
    Laughing("laughing", "Gülüyor"),
    Disappointed("disappointed", "Üzgün"),
    Victory("victory", "Zafer"),
    Point("point", "Gösteriyor"),
    Wave("wave", "El sallıyor"),
    ThumbsUp("thumbsup", "Başparmak"),
    Facepalm("facepalm", "Tüh"),
    Shrug("shrug", "Omuz silkme");

    companion object {
        fun of(key: String?): MascotEmotion = entries.find { it.key == key?.trim()?.lowercase() } ?: Neutral
    }
}

enum class MouthState(val key: String) { Closed("closed"), Half("half"), Open("open"), Blink("blink") }

/** Which mouth to show for a voice level (0..1, see PcmOps.envelope). */
fun mouthFor(level: Float): MouthState = when {
    level < 0.12f -> MouthState.Closed
    level < 0.45f -> MouthState.Half
    else -> MouthState.Open
}

/**
 * The poses Karalak is drawn with. They live on karalak.com (studio/karalak/), not in the APK:
 * every player would otherwise download a few megabytes of artwork only the panel uses, and new
 * poses can go up without an app release. A copy is kept on the phone per manifest version.
 *
 * Until the artwork is up the studio still works: it falls back to the magnifier mascot already in
 * the app, bobbing while it talks.
 */
class MascotSprites private constructor(private val files: Map<String, File>, val fallback: Bitmap?) {
    val count: Int get() = files.size
    val ready: Boolean get() = files.isNotEmpty()

    // Decoded on demand: sixty poses at full size would hold ~60 MB; a video only ever cycles through a handful.
    private val cache = object : android.util.LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun bitmap(key: String): Bitmap? {
        cache.get(key)?.let { return it }
        val f = files[key] ?: return null
        return BitmapFactory.decodeFile(f.path)?.also { cache.put(key, it) }
    }

    /**
     * The best picture for a mood and mouth: the exact one, else the nearest mouth of that mood (gesture sheets have
     * no half-open mouth), else the same mouth while calm, else whatever calm picture exists.
     */
    fun frame(emotion: MascotEmotion, mouth: MouthState): Bitmap? {
        if (files.isEmpty()) return null
        val mouths = when (mouth) {
            MouthState.Closed -> listOf(MouthState.Closed, MouthState.Blink, MouthState.Half)
            MouthState.Half -> listOf(MouthState.Half, MouthState.Open, MouthState.Closed)
            MouthState.Open -> listOf(MouthState.Open, MouthState.Half, MouthState.Closed)
            MouthState.Blink -> listOf(MouthState.Blink, MouthState.Closed)
        }
        for (e in listOf(emotion, MascotEmotion.Neutral)) {
            for (m in mouths) bitmap(key(e, m))?.let { return it }
        }
        return files.keys.firstOrNull()?.let { bitmap(it) }
    }

    companion object {
        const val BASE = "https://karalak.com/studio/karalak/"

        fun key(e: MascotEmotion, m: MouthState) = "${e.key}_${m.key}"

        /** Reads the manifest, downloads any pose not yet on the phone, decodes them all. Never fails: worst case is the fallback. */
        suspend fun load(context: Context): MascotSprites = withContext(Dispatchers.IO) {
            val fallback = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.qm_mascot) }.getOrNull()
            val root = File(context.filesDir, "studio_mascot").apply { mkdirs() }
            val manifest = runCatching { fetch(BASE + "manifest.json").toString(Charsets.UTF_8) }.getOrNull()
                ?.also { File(root, "manifest.json").writeText(it) }
                ?: File(root, "manifest.json").takeIf { it.exists() }?.readText()
            val parsed = manifest?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
            val version = parsed?.get("version")?.jsonPrimitive?.intOrNull ?: 0
            val files = parsed?.get("files")?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                .filter { Regex("^[a-z]+_(closed|half|open|blink)\\.webp$").matches(it) }
            val dir = File(root, "v$version").apply { mkdirs() }
            // an older version's copies are no use once the manifest moved on
            root.listFiles()?.filter { it.isDirectory && it.name != dir.name }?.forEach { it.deleteRecursively() }
            val local = HashMap<String, File>()
            files.forEach { name ->
                val f = File(dir, name)
                if (!f.exists()) runCatching { f.writeBytes(fetch(BASE + "v$version/" + name)) }
                if (f.exists() && f.length() > 0) local[name.removeSuffix(".webp")] = f
            }
            MascotSprites(local, fallback)
        }

        private fun fetch(url: String): ByteArray {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                useCaches = false
            }
            try {
                check(conn.responseCode == 200) { "HTTP ${conn.responseCode}" }
                return conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
