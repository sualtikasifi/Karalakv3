package com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio

import android.content.Context
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

    class ApiError(val code: Int, message: String) : Exception(message)

    /** The line as speech, MP3 bytes. */
    suspend fun speak(text: String, voiceId: String, model: VoiceModel, stability: Double): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val body = buildJsonObject {
                put("text", text)
                put("model_id", model.id)
                put("language_code", "tr")
                putJsonObject("voice_settings") { put("stability", stability) }
            }
            val conn = open("/v1/text-to-speech/$voiceId?output_format=mp3_44100_128").apply {
                requestMethod = "POST"
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "audio/mpeg")
            }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                if (code !in 200..299) throw ApiError(code, errorMessage(code, readText(conn, code)))
                conn.inputStream.use { it.readBytes() }.also { if (it.size < 256) throw ApiError(code, "ElevenLabs boş ses döndürdü") }
            } finally {
                conn.disconnect()
            }
        }
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

        /** ElevenLabs' own voices, a starting point until Karalak has a voice of its own. */
        val PRESETS = listOf(
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

        /** What is sent for a line: with v3, its mood as a tag in front. The subtitle always shows the plain line. */
        fun textFor(line: SpokenLine, model: VoiceModel, tags: Boolean): String {
            val text = line.text.trim()
            val tag = if (model.tags && tags) tagFor(line.emotion) else null
            return if (tag == null) text else "$tag $text"
        }

        /** A file name that changes whenever anything that changes the sound does. */
        fun cacheName(text: String, voiceId: String, model: VoiceModel, stability: Double): String {
            val digest = MessageDigest.getInstance("SHA-1").digest("$voiceId|${model.id}|$stability|$text".toByteArray(Charsets.UTF_8))
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
data class VoiceSpec(val voiceId: String, val model: VoiceModel, val stability: Double, val tags: Boolean) {
    /** What ElevenLabs charges for the line, in credits: a character each, half on Flash. */
    fun credits(line: SpokenLine): Int {
        val chars = ElevenLabs.textFor(line, model, tags).length
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
        val text = ElevenLabs.textFor(line, spec.model, spec.tags)
        return File(dir, ElevenLabs.cacheName(text, spec.voiceId, spec.model, ElevenLabs.stabilityFor(spec.model, spec.stability)))
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
        val text = ElevenLabs.textFor(line, spec.model, spec.tags)
        val s = ElevenLabs.stabilityFor(spec.model, spec.stability)
        val voiceId = spec.voiceId
        val model = spec.model
        val bytes = client.speak(text, voiceId, model, s).getOrElse { return Result.failure(it) }
        return runCatching {
            withContext(Dispatchers.IO) {
                val tmp = File(dir, f.name + ".part")
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(f)) { tmp.delete(); error("Ses kaydedilemedi") }
                prune()
                f
            }
        }
    }

    private fun prune() {
        val files = dir.listFiles { x -> x.name.endsWith(".mp3") }?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_FILES).forEach { it.delete() }
    }

    private companion object {
        const val MAX_FILES = 600
    }
}
