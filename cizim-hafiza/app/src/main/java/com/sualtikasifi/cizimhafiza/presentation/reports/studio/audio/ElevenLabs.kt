package com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio

import android.content.Context
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Backoff
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Pronunciation
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioLang
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Base64
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A speaking model the studio offers, and whether it acts out audio tags like "[laughs]". */
enum class VoiceModel(val id: String, val label: String, val tags: Boolean) {
    V3("eleven_v3", "v3 · duygulu", true),
    V2("eleven_multilingual_v2", "v2 · dengeli", false),
    Flash("eleven_flash_v2_5", "Flash · ucuz", false);

    companion object {
        fun of(id: String?): VoiceModel = entries.find { it.id == id } ?: V3
    }
}

/**
 * Karalak's voice from ElevenLabs. The key lives only on this phone (like the OpenRouter one).
 *
 * Starter plans may not ask for raw PCM, so speech comes back as MP3 and is decoded on the phone
 * like any clip. Every line is kept on disk by what was asked (voice, model, text, delivery), so
 * listening to a line and then making the video, or making it again, pays for it once.
 */
class ElevenLabs(private val apiKey: String) {

    class Voice(val id: String, val name: String, val note: String)

    /** Used and total credits this month. */
    class Credits(val used: Int, val limit: Int) {
        val left: Int get() = (limit - used).coerceAtLeast(0)
    }

    class ApiError(override val code: Int, message: String, override val retryAfterMs: Long? = null) : Exception(message), Backoff.Coded

    /** A line as speech: the MP3, and when each word of it is said when ElevenLabs told. */
    class Spoken(val mp3: ByteArray, val timing: SpeechTiming?)

    /**
     * The line as speech. It asks for the words' timings along with the sound (for the karaoke subtitle); if that is
     * refused for this voice or model, the plain sound is asked for instead, so a line is never lost for the sake of
     * its timings. A busy service (429, 5xx) is waited out and asked again, a few times, before giving up.
     */
    suspend fun speak(
        text: String, voiceId: String, model: VoiceModel, stability: Double, lang: StudioLang = StudioLang.Tr
    ): Result<Spoken> = withContext(Dispatchers.IO) {
        Backoff.retrying {
            val timed = runCatching { speakTimed(text, voiceId, model, stability, lang) }
            val e = timed.exceptionOrNull()
            if (e is ApiError && e.code in TIMESTAMPS_REFUSED) runCatching { Spoken(speakPlain(text, voiceId, model, stability, lang), null) } else timed
        }
    }

    private fun body(text: String, model: VoiceModel, stability: Double, lang: StudioLang) = buildJsonObject {
        put("text", text)
        put("model_id", model.id)
        put("language_code", lang.code)
        putJsonObject("voice_settings") { put("stability", stability) }
    }

    private fun post(path: String, body: JsonObject, accept: String): Pair<Int, ByteArray> {
        val conn = open(path).apply {
            requestMethod = "POST"
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", accept)
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val bytes = (if (code in 200..299) conn.inputStream else conn.errorStream)?.use { it.readBytes() } ?: ByteArray(0)
            if (code !in 200..299) {
                val wait = conn.getHeaderField("retry-after")?.trim()?.toLongOrNull()?.times(1000)
                throw ApiError(code, errorMessage(code, String(bytes, Charsets.UTF_8)), wait)
            }
            return code to bytes
        } finally {
            conn.disconnect()
        }
    }

    private fun speakPlain(text: String, voiceId: String, model: VoiceModel, stability: Double, lang: StudioLang): ByteArray {
        val (code, bytes) = post("/v1/text-to-speech/$voiceId?output_format=mp3_44100_128", body(text, model, stability, lang), "audio/mpeg")
        if (bytes.size < 256) throw ApiError(code, "ElevenLabs boş ses döndürdü")
        return bytes
    }

    private fun speakTimed(text: String, voiceId: String, model: VoiceModel, stability: Double, lang: StudioLang): Spoken {
        val (code, bytes) = post("/v1/text-to-speech/$voiceId/with-timestamps?output_format=mp3_44100_128", body(text, model, stability, lang), "application/json")
        val root = Json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
        val audio = root["audio_base64"]?.jsonPrimitive?.contentOrNull?.let { Base64.getDecoder().decode(it) }
        if (audio == null || audio.size < 256) throw ApiError(code, "ElevenLabs boş ses döndürdü")
        return Spoken(audio, (root["alignment"] as? JsonObject)?.let { SpeechTiming.fromAlignment(it) })
    }

    /** The account's voices, the ones made with Voice Design included. Needs the key's "Voices: read" permission. */
    suspend fun voices(): Result<List<Voice>> = withContext(Dispatchers.IO) {
        runCatching {
            val root = getJson("/v2/voices?page_size=100")
            root["voices"]!!.jsonArray.mapNotNull { e ->
                val v = e.jsonObject
                val id = v["voice_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val labels = (v["labels"] as? JsonObject)?.values?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
                Voice(id, v["name"]?.jsonPrimitive?.contentOrNull ?: id, (listOfNotNull(v["category"]?.jsonPrimitive?.contentOrNull) + labels).joinToString(" · "))
            }
        }
    }

    /** This month's credits. Needs the key's "User: read" permission. */
    suspend fun credits(): Result<Credits> = withContext(Dispatchers.IO) {
        runCatching {
            val root = getJson("/v1/user/subscription")
            Credits(root["character_count"]?.jsonPrimitive?.intOrNull ?: 0, root["character_limit"]?.jsonPrimitive?.intOrNull ?: 0)
        }
    }

    private fun open(path: String) = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 20_000
        setRequestProperty("xi-api-key", apiKey)
    }

    private fun getJson(path: String): JsonObject {
        val conn = open(path)
        try {
            val code = conn.responseCode
            val raw = readText(conn, code)
            if (code !in 200..299) throw ApiError(code, errorMessage(code, raw))
            return Json.parseToJsonElement(raw).jsonObject
        } finally {
            conn.disconnect()
        }
    }

    private fun readText(conn: HttpURLConnection, code: Int): String =
        (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

    companion object {
        private const val BASE = "https://api.elevenlabs.io"

        /** Answers that mean "not this way", not "not you": the plain sound is asked for instead. */
        private val TIMESTAMPS_REFUSED = setOf(400, 404, 405, 422)

        /** Karalak's own voice first (the default), then a few of ElevenLabs' voices to compare with. */
        val PRESETS = listOf(
            Voice("Z6T21S2OyYi1iLYXumk4", "Karalak", "Karalak'ın sesi"),
            Voice("cgSgspJ2msm6clMCkdW9", "Jessica", "neşeli, genç"),
            Voice("FGY2WhTYpPnrIDTdsKH5", "Laura", "heyecanlı"),
            Voice("pFZP5JQG7iQjIQuC4Bku", "Lily", "yumuşak"),
            Voice("TX3LPaxmHKxFdv7VOQHJ", "Liam", "enerjik, genç erkek"),
            Voice("IKne3meq5aSn9XLyUdCD", "Charlie", "kendinden emin"),
            Voice("N2lVS1w4EtoT3dr4eOWO", "Callum", "muzip")
        )

        /** v3's delivery for each of Karalak's moods; v3 reads a bracketed tag as how to say what follows. */
        fun tagFor(e: MascotEmotion): String? = when (e) {
            MascotEmotion.Neutral -> null
            MascotEmotion.Thinking -> "[curious]"
            MascotEmotion.Suspicious -> "[suspicious]"
            MascotEmotion.Excited -> "[excited]"
            MascotEmotion.Confident -> "[confidently]"
            MascotEmotion.Shocked -> "[surprised]"
            MascotEmotion.Embarrassed -> "[nervously]"
            MascotEmotion.Laughing -> "[laughs]"
            MascotEmotion.Disappointed -> "[sad]"
            MascotEmotion.Victory -> "[excited]"
            MascotEmotion.Point -> "[playfully]"
            MascotEmotion.Wave -> "[cheerfully]"
            MascotEmotion.ThumbsUp -> "[cheerfully]"
            MascotEmotion.Facepalm -> "[sighs]"
            MascotEmotion.Shrug -> "[playfully]"
        }

        /**
         * What is sent for a line: with v3, its mood as a tag in front, and always a pause at the end. ElevenLabs (v3
         * most of all) at times swallows a line's last syllable or stops dead on it; a finished sentence followed by
         * a pause gives it room to say all of it and let it ring out. The subtitle always shows the plain line.
         */
        fun textFor(line: SpokenLine, model: VoiceModel, tags: Boolean, lang: StudioLang = StudioLang.Tr): String {
            val text = withRoomToFinish(Pronunciation.forSpeech(line.text.trim(), lang))
            val tag = if (model.tags && tags) tagFor(line.emotion) else null
            return if (tag == null) text else "$tag $text"
        }

        /** [text] as a finished sentence (a full stop when it has no ending of its own) with a pause after it. */
        internal fun withRoomToFinish(text: String): String {
            if (text.isEmpty() || text.endsWith("...") || text.endsWith("…")) return text
            val sentence = if (text.last() in ".!?") text else "$text."
            return "$sentence $TAIL_PAUSE"
        }

        private const val TAIL_PAUSE = "..."

        /** A file name that changes whenever anything that changes the sound does. */
        fun cacheName(text: String, voiceId: String, model: VoiceModel, stability: Double, lang: StudioLang = StudioLang.Tr): String {
            // Turkish lines keep the names they always had, so what was already paid for is still found
            val where = if (lang == StudioLang.Tr) "" else "${lang.code}|"
            val digest = MessageDigest.getInstance("SHA-1").digest("$where$voiceId|${model.id}|$stability|$text".toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) } + ".mp3"
        }

        /** v3 takes only three stabilities: 0 (creative), 0.5 (natural), 1 (robust); the others take any. */
        fun stabilityFor(model: VoiceModel, chosen: Double): Double =
            if (model == VoiceModel.V3) listOf(0.0, 0.5, 1.0).minBy { kotlin.math.abs(it - chosen) } else chosen.coerceIn(0.0, 1.0)

        internal fun errorMessage(code: Int, raw: String): String {
            val detail = runCatching { Json.parseToJsonElement(raw).jsonObject["detail"] }.getOrNull()
            val status = (detail as? JsonObject)?.get("status")?.jsonPrimitive?.contentOrNull
            val message = (detail as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
                ?: (detail as? JsonPrimitive)?.contentOrNull ?: raw.take(200)
            val hint = when {
                status == "quota_exceeded" -> "ElevenLabs kredisi bitti. "
                status == "missing_permissions" -> "Anahtarın bu izni yok. "
                status == "voice_not_found" -> "Bu ses bulunamadı; ses ID'sini kontrol et. "
                code == 401 -> "ElevenLabs anahtarı geçersiz. "
                code == 429 -> "Çok sık istek; biraz sonra dene. "
                else -> ""
            }
            return "ElevenLabs: $hint$message"
        }
    }
}

/** How a line is to be spoken: everything that changes the sound, and so the bill. */
data class VoiceSpec(val voiceId: String, val model: VoiceModel, val stability: Double, val tags: Boolean, val lang: StudioLang = StudioLang.Tr) {
    /** What ElevenLabs charges for the line, in credits: a character each, half on Flash. */
    fun credits(line: SpokenLine): Int {
        val chars = ElevenLabs.textFor(line, model, tags, lang).length
        return if (model == VoiceModel.Flash) (chars + 1) / 2 else chars
    }
}

/**
 * The spoken lines already paid for, on this phone. Old ones go once there are too many.
 *
 * Nothing here calls ElevenLabs on its own: [cached] only looks, and [file] is called only for a
 * line the person asked to hear or asked to be voiced.
 */
class VoiceBank(context: Context) {
    private val dir = File(context.filesDir, "studio_voice").apply { mkdirs() }

    private fun target(line: SpokenLine, spec: VoiceSpec): File {
        val text = ElevenLabs.textFor(line, spec.model, spec.tags, spec.lang)
        return File(dir, ElevenLabs.cacheName(text, spec.voiceId, spec.model, ElevenLabs.stabilityFor(spec.model, spec.stability), spec.lang))
    }

    /** Where a voice file's word timings are kept: beside it, same name, ".words.json". */
    private fun timingFile(voice: File) = File(voice.parentFile, voice.name.removeSuffix(".mp3") + ".words.json")

    /**
     * When each word of the line is said, if the voice was made with timings (lines made before they were asked for
     * have none, and the subtitle then follows the voice by length). Never asks ElevenLabs.
     */
    fun timing(line: SpokenLine, spec: VoiceSpec): SpeechTiming? {
        val f = timingFile(target(line, spec)).takeIf { it.exists() } ?: return null
        return SpeechTiming.fromJson(runCatching { f.readText() }.getOrDefault(""))
    }

    /** The line's MP3 if it was already made with these settings; never asks ElevenLabs. */
    fun cached(line: SpokenLine, spec: VoiceSpec): File? = target(line, spec).takeIf { it.exists() && it.length() > 0 }

    /** The line's MP3, from disk if it was made before, from ElevenLabs (paid) otherwise. */
    suspend fun file(client: ElevenLabs, line: SpokenLine, spec: VoiceSpec): Result<File> {
        val f = target(line, spec)
        if (f.exists() && f.length() > 0) {
            f.setLastModified(System.currentTimeMillis())
            return Result.success(f)
        }
        val text = ElevenLabs.textFor(line, spec.model, spec.tags, spec.lang)
        val s = ElevenLabs.stabilityFor(spec.model, spec.stability)
        val spoken = client.speak(text, spec.voiceId, spec.model, s, spec.lang).getOrElse { return Result.failure(it) }
        return runCatching {
            withContext(Dispatchers.IO) {
                // the timings first: a voice with no timings beside it is still a voice, but not the other way round
                spoken.timing?.let { runCatching { timingFile(f).writeText(it.toJson()) } }
                val tmp = File(dir, f.name + ".part")
                tmp.writeBytes(spoken.mp3)
                if (!tmp.renameTo(f)) { tmp.delete(); error("Ses kaydedilemedi") }
                prune()
                f
            }
        }
    }

    private fun prune() {
        val files = dir.listFiles { x -> x.name.endsWith(".mp3") }?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_FILES).forEach { it.delete(); timingFile(it).delete() }
    }

    private companion object {
        const val MAX_FILES = 600
    }
}
