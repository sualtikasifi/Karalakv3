package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.Locale

/** One thing Karalak says, and the face it says it with. */
@Serializable
data class SpokenLine(val text: String, val emotion: MascotEmotion)

/** Reading the assistant's answers and comparing words, shared by every video format. */
object StudioCopy {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TR = Locale("tr")

    /** The JSON object inside an answer, whatever the model wrapped around it (code fences, a sentence first). */
    fun objectIn(text: String): JsonObject? {
        val from = text.indexOf('{')
        val to = text.lastIndexOf('}')
        if (from < 0 || to <= from) return null
        return runCatching { json.parseToJsonElement(text.substring(from, to + 1)).jsonObject }.getOrNull()
    }

    /** Lower-case, Turkish letters folded to plain ones, punctuation gone: "Kara Kedi!" → "kara kedi". */
    fun normalize(s: String): String = s.lowercase(TR)
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i').replace('ö', 'o').replace('ş', 's').replace('ü', 'u').replace('â', 'a')
        .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** A guess at how long a line takes to say, for the preview before any voice is recorded. */
    fun estimateMs(text: String): Long = 350L + text.length * 62L
}
