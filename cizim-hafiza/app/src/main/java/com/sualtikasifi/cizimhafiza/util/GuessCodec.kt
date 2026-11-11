package com.sualtikasifi.cizimhafiza.util

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Everything a "Bil Bakalım" link is made of that is not Firestore: the answer's hash, the hints, the ids and the
 * drawing's compact form. The web page (hosting/bil/bil.js) does the same sums in JavaScript, so the two stay in step
 * byte for byte: a hash written here must be the one the page checks a guess against, and the other way round.
 *
 * **The answer never travels as text.** The link holds a salted SHA-256 of the word's folded spelling
 * ([AnswerMatcher.normalize]); a guess is checked by hashing it, and the word is shown after the round by hashing the
 * language's word list (public, it ships in every app and web build) until one matches. That keeps the answer out of
 * the network traffic and the console; it does not stop someone who deliberately hashes the list, which is fine for a
 * game between friends.
 */
object GuessCodec {
    /** How long a link lives. Mirrors firestore.rules' guessLinks (at most 31 days). */
    const val LINK_LIFETIME_MILLIS = 30L * 24 * 60 * 60 * 1000

    /** Wrong guesses allowed before the word is shown. */
    const val MAX_ATTEMPTS = 3

    /** The drawing is kept inside this square, whatever size the screen it was drawn on. */
    const val SPACE = 1000f

    /** A very busy scribble is thinned to about this many points so the link stays small (rules cap the document at ~150 KB). */
    const val MAX_POINTS = 2500

    /** No 0/O/1/l/I: an id read out loud or typed from a screenshot should survive. */
    private const val ID_ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val ID_LENGTH = 10
    private const val SALT_LENGTH = 12

    private val random = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true }

    /** The word list a drawing's language maps to, the same rule as WordSeeder.assetFileFor: English for everything but tr, es and pt. */
    fun langFor(language: String): String = if (language == "tr" || language == "es" || language == "pt") language else "en"

    fun newId(): String = randomString(ID_ALPHABET, ID_LENGTH)

    fun newSalt(): String = randomString(ID_ALPHABET, SALT_LENGTH)

    private fun randomString(alphabet: String, length: Int): String =
        buildString(length) { repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) } }

    /** A link id as the rules accept it: ten letters and digits. */
    fun isValidId(id: String): Boolean = id.length == ID_LENGTH && id.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }

    /** The salted hash of [word]'s folded spelling, as 64 lower-case hex digits. */
    fun hash(salt: String, word: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt:${AnswerMatcher.normalize(word)}".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** The shape of a word for the first hint: the length of each of its words, "4 3" for "kara kedi". */
    fun pattern(word: String): String =
        AnswerMatcher.normalize(word).split(' ').filter { it.isNotEmpty() }.joinToString(" ") { it.length.toString() }

    /** The first letter, upper case, for the second hint. */
    fun firstLetter(word: String): String = word.trim().take(1).uppercase()

    /** "_ _ _ _   _ _ _" for "4 3", with [first] written in place of the first blank when it is known. */
    fun blanks(pattern: String, first: String? = null): String {
        val words = pattern.split(' ').mapNotNull { it.toIntOrNull() }.filter { it > 0 }
        return words.mapIndexed { wi, n ->
            (0 until n).joinToString(" ") { i -> if (wi == 0 && i == 0 && !first.isNullOrBlank()) first else "_" }
        }.joinToString("   ")
    }

    /**
     * Whether [guess] is the word behind [hash]. The same forgiveness as everywhere else in the game (accents, case,
     * a slip of a letter on a long word): every word of [candidates] that the guess is close enough to is hashed, and
     * one of them has to be the answer. A guess is never compared with the answer directly, there is none to compare.
     */
    fun isCorrect(guess: String, candidates: List<String>, salt: String, hash: String): Boolean {
        if (AnswerMatcher.normalize(guess).isEmpty()) return false
        return candidates.any { AnswerMatcher.isCorrect(guess, it) && hash(salt, it) == hash }
    }

    /** The word behind [hash], found in [candidates]; null if the language's list does not hold it. */
    fun reveal(candidates: List<String>, salt: String, hash: String): String? =
        candidates.firstOrNull { hash(salt, it) == hash }

    // ---- the drawing ------------------------------------------------------------------------------------------

    /**
     * The strokes as the link carries them: fitted into a [SPACE] x [SPACE] square (aspect kept), whole numbers, each
     * stroke one flat list `[x0, y0, x1, y1, …]`: `{"v":1,"s":[[…],[…]]}`. A busy drawing is thinned, never cut.
     */
    fun encodeStrokes(strokes: List<DrawingStroke>): String {
        val drawn = strokes.filter { it.isNotEmpty() }
        val all = drawn.flatten()
        if (all.isEmpty()) return """{"v":1,"s":[]}"""
        val minX = all.minOf { it.x }
        val minY = all.minOf { it.y }
        val span = max(max(all.maxOf { it.x } - minX, all.maxOf { it.y } - minY), 1f)
        val scale = SPACE / span
        val keepEvery = max(1, (all.size + MAX_POINTS - 1) / MAX_POINTS)
        val out = JsonArray(
            drawn.map { stroke ->
                val picked = stroke.filterIndexed { i, _ -> i % keepEvery == 0 || i == stroke.lastIndex }
                JsonArray(picked.flatMap { p ->
                    listOf(
                        JsonPrimitive(((p.x - minX) * scale).roundToInt().coerceIn(0, SPACE.toInt())),
                        JsonPrimitive(((p.y - minY) * scale).roundToInt().coerceIn(0, SPACE.toInt()))
                    )
                })
            }
        )
        return buildJsonObject {
            put("v", 1)
            put("s", out)
        }.toString()
    }

    /** Reads what [encodeStrokes] wrote (or the web page wrote the same way); an unreadable drawing comes back empty. */
    fun decodeStrokes(text: String): List<DrawingStroke> = runCatching {
        val root: JsonObject = json.parseToJsonElement(text).jsonObject
        root.getValue("s").jsonArray.map { stroke ->
            val numbers = stroke.jsonArray.mapNotNull { it.jsonPrimitive.floatOrNull }
            numbers.chunked(2).filter { it.size == 2 }.map { (x, y) -> DrawingPoint(x, y) }
        }.filter { it.isNotEmpty() }
    }.getOrDefault(emptyList())
}
