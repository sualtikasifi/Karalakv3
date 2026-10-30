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
     * When each of [display]'s words is said, start to end in ms, or null when nothing is known. When the voice said
     * exactly as many words as the subtitle shows, they are its own times; otherwise the time the voice spoke is shared
     * out over the subtitle's words by their length, which is right on average and never out of order.
     */
    fun rangesFor(display: String): List<LongRange>? {
        if (words.isEmpty()) return null
        val shown = wordRanges(display)
        if (shown.isEmpty()) return null
        if (shown.size == words.size) return words.map { it.startMs..maxOf(it.startMs, it.endMs) }
        val from = words.first().startMs
        val span = (words.last().endMs - from).coerceAtLeast(1L)
        val total = shown.sumOf { it.last - it.first + 1 }.coerceAtLeast(1)
        var before = 0
        return shown.map { r ->
            val len = r.last - r.first + 1
            val start = from + (span.toDouble() * before / total).roundToLong()
            before += len
            start..(from + (span.toDouble() * before / total).roundToLong())
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

/**
 * A subtitle that comes in as it is said: every character gets the moment it appears, and fades in over [FADE_MS] from
 * there, so the words come up letter by letter with the voice rather than all at once. The text is laid out whole
 * from the start (the letters not yet said are only transparent), so nothing moves as it fills in.
 */
object Reveal {
    /** A letter shows a hair before its sound: the eye reads ahead of the ear. */
    const val LEAD_MS = 60L

    /** How long one letter takes to come in. */
    const val FADE_MS = 110L

    /** A line nobody says is read in at this pace, letters per second. */
    const val READ_CPS = 16

    /** The pause sent after every voiced line (see ElevenLabs.textFor): the words are over this long before the voice is. */
    const val VOICE_TAIL_MS = 350L

    /**
     * When each character of [text] appears, in ms from the moment the line starts. [words] are the times of its words
     * as said (see [SpeechTiming.rangesFor]): each word's letters are spread over the time it takes to say it, and what
     * lies between words comes with the next one. Without them, a voiced line ([voiceMs] above 0) is spread evenly over
     * the voice, and a silent one is read in at [READ_CPS].
     */
    fun charStarts(text: String, words: List<LongRange>?, voiceMs: Long): LongArray {
        val n = text.length
        val out = LongArray(n)
        if (n == 0) return out
        val shown = SpeechTiming.wordRanges(text)
        if (words != null && words.size == shown.size && shown.isNotEmpty()) {
            val set = BooleanArray(n)
            shown.forEachIndexed { j, r ->
                val len = r.last - r.first + 1
                val w = words[j]
                val span = (w.last - w.first).coerceAtLeast(0L)
                for (k in 0 until len) {
                    out[r.first + k] = w.first + span * k / len
                    set[r.first + k] = true
                }
            }
            // spaces and anything before a word come with it; anything after the last word, with that word's end
            var next = words.last().last
            for (i in n - 1 downTo 0) {
                if (set[i]) next = out[i] else out[i] = next
            }
            return out
        }
        if (voiceMs > 0L) {
            val speaking = (voiceMs - VOICE_TAIL_MS).coerceAtLeast(voiceMs * 6 / 10)
            for (i in 0 until n) out[i] = speaking * i / n
            return out
        }
        for (i in 0 until n) out[i] = i * 1000L / READ_CPS
        return out
    }

    /** How far character [i] has come in, 0 to 1, at [elapsedMs] into the line. */
    fun alpha(starts: LongArray, i: Int, elapsedMs: Long): Float =
        ((elapsedMs + LEAD_MS - starts[i]).toFloat() / FADE_MS).coerceIn(0f, 1f)

    /** When the whole line is in, in ms from its start. */
    fun doneAt(starts: LongArray): Long = if (starts.isEmpty()) 0L else (starts.last() + FADE_MS - LEAD_MS).coerceAtLeast(0L)
}
