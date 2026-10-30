package com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong

/**
 * When each word of a spoken line is said, from ElevenLabs' character timings. Times are in ms from the start of the
 * voice file. What was sent to the voice is not exactly what the subtitle shows (a mood tag in front, a pause at the
 * end, a web address spelled out), so the timings are kept per spoken word and fitted to the subtitle's words later
 * with [startsFor].
 */
class SpeechTiming(val words: List<Word>) {
    class Word(val text: String, val startMs: Long, val endMs: Long)

    val isEmpty: Boolean get() = words.isEmpty()

    /**
     * The moment each of [display]'s words starts, in ms, or null when nothing is known. When the voice said exactly as
     * many words as the subtitle shows, they are its own times; otherwise the words are spread over the time the voice
     * spoke in proportion to their length, which is right on average and never out of order.
     */
    fun startsFor(display: String): LongArray? {
        if (words.isEmpty()) return null
        val shown = wordRanges(display)
        if (shown.isEmpty()) return null
        if (shown.size == words.size) return LongArray(shown.size) { words[it].startMs }
        val from = words.first().startMs
        val span = (words.last().endMs - from).coerceAtLeast(1L)
        val total = shown.sumOf { it.last - it.first + 1 }.coerceAtLeast(1)
        var before = 0
        return LongArray(shown.size) { i ->
            val start = from + (span.toDouble() * before / total).roundToLong()
            before += shown[i].last - shown[i].first + 1
            start
        }
    }

    /** For keeping beside the voice file. */
    fun toJson(): String = JsonObject(
        mapOf("w" to JsonArray(words.map { w -> JsonArray(listOf(JsonPrimitive(w.text), JsonPrimitive(w.startMs), JsonPrimitive(w.endMs))) }))
    ).toString()

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** The words of a text as ranges of its characters: runs of anything but white space. */
        fun wordRanges(text: String): List<IntRange> = Regex("\\S+").findAll(text).map { it.range }.toList()

        fun fromJson(raw: String): SpeechTiming? = runCatching {
            val rows = json.parseToJsonElement(raw).jsonObject["w"]!!.jsonArray
            SpeechTiming(rows.map { r ->
                val a = r.jsonArray
                Word(a[0].jsonPrimitive.content, a[1].jsonPrimitive.content.toLong(), a[2].jsonPrimitive.content.toLong())
            })
        }.getOrNull()?.takeIf { !it.isEmpty }

        /**
         * ElevenLabs' `alignment`: one entry per character of what was sent, with when it starts and ends (seconds).
         * Words are the runs between white space; anything in square brackets (v3's mood tag) and a trailing pause
         * made only of dots were never part of the subtitle and are left out.
         */
        fun fromAlignment(characters: List<String>, starts: List<Double>, ends: List<Double>): SpeechTiming? {
            val n = minOf(characters.size, starts.size, ends.size)
            val out = mutableListOf<Word>()
            val text = StringBuilder()
            var from = 0.0
            var to = 0.0
            var depth = 0
            fun flush() {
                val t = text.toString()
                text.setLength(0)
                if (t.isNotEmpty() && t.any { it.isLetterOrDigit() }) {
                    out += Word(t, (from * 1000).roundToLong(), (to * 1000).roundToLong())
                }
            }
            for (i in 0 until n) {
                val c = characters[i]
                if (c == "[") { flush(); depth++; continue }
                if (c == "]") { depth = (depth - 1).coerceAtLeast(0); continue }
                if (depth > 0) continue
                if (c.isBlank()) { flush(); continue }
                if (text.isEmpty()) from = starts[i]
                text.append(c)
                to = ends[i]
            }
            flush()
            return SpeechTiming(out).takeIf { !it.isEmpty }
        }

        /** The `alignment` object of a with-timestamps answer. */
        fun fromAlignment(alignment: JsonObject): SpeechTiming? = runCatching {
            fun strings(key: String) = alignment[key]!!.jsonArray.map { it.jsonPrimitive.content }
            fun numbers(key: String) = alignment[key]!!.jsonArray.map { it.jsonPrimitive.doubleOrNull ?: it.jsonPrimitive.contentOrNull!!.toDouble() }
            fromAlignment(strings("characters"), numbers("character_start_times_seconds"), numbers("character_end_times_seconds"))
        }.getOrNull()
    }
}

/** Which part of a subtitle has been said by a given moment, for the karaoke colouring. */
object Karaoke {
    /** A word lights up a hair before its sound, since the eye reads ahead of the ear. */
    const val LEAD_MS = 60L

    /**
     * Where the "already said" colour ends in [text] at [elapsedMs] into the line: at the end of the last word that has
     * started, never mid-word. [starts] are the words' start times (see [SpeechTiming.startsFor]).
     */
    fun splitAt(text: String, starts: LongArray, elapsedMs: Long): Int {
        val words = SpeechTiming.wordRanges(text)
        if (words.isEmpty()) return 0
        var said = 0
        for (i in words.indices) if (i < starts.size && starts[i] <= elapsedMs + LEAD_MS) said = i + 1
        if (said == 0) return 0
        return if (said >= words.size) text.length else words[said - 1].last + 1
    }
}
