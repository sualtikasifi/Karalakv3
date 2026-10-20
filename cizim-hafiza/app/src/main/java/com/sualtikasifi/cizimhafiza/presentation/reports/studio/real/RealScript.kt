package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.min

/**
 * One drawing of the pool, copied into the project so it survives the pool changing. Each carries the [word] it was
 * drawn for, the [seconds] the player had, and who drew it: [nickname] and [level] when the player allowed to be
 * featured, otherwise [nickname] is empty and the video says "anonymous". The name, the [frameId] and the [photo]
 * can all be changed by hand in the panel; the picture inside the frame is the default one unless a [photo] is set,
 * and is never the player's own.
 */
@Serializable
data class RealDrawing(
    val id: String,
    val strokes: List<List<DrawingPoint>>,
    val word: String = "",
    val seconds: Int = 0,
    val nickname: String = "",
    val level: Int = 0,
    val frameId: String = "",
    /** A picture picked by hand for the round slot (a file name in the project's media folder); null: the default picture. */
    val photo: String? = null
) {
    val named: Boolean get() = nickname.isNotBlank()
}

/** One of the pool's latest drawings, as the list shows it ([nickname] empty: the player did not allow their name). */
@Serializable
data class RecentDrawing(
    val id: String,
    val word: String,
    val seconds: Int,
    val nickname: String,
    val level: Int,
    val frameId: String,
    val strokes: List<List<DrawingPoint>>
)

/**
 * A drawing turned real: its realistic picture (made in ChatGPT, kept for sharing as a swipe-through post and as
 * the starting frame for Gemini) and the clip of that picture moving, with sound (made in Gemini, the only thing
 * of it that goes into the video). [image] and [clip] are file names in the project's media folder.
 */
@Serializable
data class RealReveal(
    val sampleId: String,
    /** The drawing's word in English, for its prompts. */
    val wordEn: String = "",
    /** The doodle's literal shapes in English, written into the picture prompt so they are kept. */
    val describe: String = "",
    /** The small funny scene the video shows (English, a person using or reacting to it), written into the video prompt. */
    val motion: String = "",
    val image: String? = null,
    val clip: String? = null,
    val clipMs: Long = 0L
)

/**
 * "Gerçek Olsaydı": a few drawings by different players, each turned into a realistic picture and a clip
 * with sound. The video is those clips one after the other, each with its drawing, its player and the app's
 * name and Google Play badge over it.
 */
@Serializable
data class RealProject(
    val drawings: List<RealDrawing> = emptyList(),
    val reveals: List<RealReveal> = emptyList()
) {
    fun drawing(id: String): RealDrawing? = drawings.find { it.id == id }
    val media: Set<String> get() = (reveals.flatMap { listOfNotNull(it.image, it.clip) } + drawings.mapNotNull { it.photo }).toSet()

    /** Only reveals with their clip make it into the video; the pictures are shared on their own. */
    val shown: List<RealReveal> get() = reveals.filter { it.clip != null && drawing(it.sampleId) != null }
}

/**
 * Where the Karalak card sits on the video and how big it is, set by hand in the panel. [scale] multiplies the
 * default width (1 = 450 px of the 1080 px frame); [x] and [y] are where its left and top edges are, from 0 (flush
 * with the left / top) to 1 (flush with the right / bottom), so it can never leave the frame whatever the size.
 */
data class CardLayout(val scale: Float = DEFAULT_SCALE, val x: Float = DEFAULT_X, val y: Float = DEFAULT_Y) {
    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 1.6f
        const val DEFAULT_SCALE = 0.74f
        const val DEFAULT_X = 0.06f
        const val DEFAULT_Y = 0.03f
    }

    /** The same layout with every value kept inside what the sliders allow. */
    fun clamped() = CardLayout(scale.coerceIn(MIN_SCALE, MAX_SCALE), x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

/**
 * One finished piece kept in the library on this phone: a drawing with everything the card shows (name, frame, picture),
 * its realistic picture and its clip. [id] is the drawing's id, so saving the same drawing again updates its entry.
 * The [reveal]'s files live in the project's media folder, which nothing in the library is ever pruned from.
 */
@Serializable
data class LibraryItem(val id: String, val savedAt: Long, val drawing: RealDrawing, val reveal: RealReveal) {
    val media: Set<String> get() = listOfNotNull(reveal.image, reveal.clip, drawing.photo).toSet()
    val hasClip: Boolean get() = reveal.clip != null
}

enum class RealPhase { Clip }

/** One stretch of the video: a clip, playing from [clipAtMs] for [clipMs]. */
data class RealEvent(
    val phase: RealPhase,
    val index: Int,
    val startMs: Long,
    val durationMs: Long,
    val clipAtMs: Long = startMs,
    val clipMs: Long = 0L
) {
    val endMs: Long get() = startMs + durationMs
    fun progress(t: Long): Float = ((t - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

object RealTimeline {
    /** Gemini clips are about eight seconds; anything longer is cut here. */
    const val CLIP_MAX = 10_000L
    private const val CLIP_MIN = 1_000L

    fun build(shown: List<RealReveal>): List<RealEvent> {
        val out = mutableListOf<RealEvent>()
        var t = 0L
        shown.forEachIndexed { i, r ->
            val ms = min(r.clipMs, CLIP_MAX).coerceAtLeast(CLIP_MIN)
            out += RealEvent(RealPhase.Clip, i, t, ms, clipAtMs = t, clipMs = ms)
            t += ms
        }
        return out
    }

    /** The stretch at [t]; the last one once the video is over. [events] is never empty (a video needs a clip). */
    fun at(events: List<RealEvent>, t: Long): RealEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<RealEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object RealCopy {
    /** Where the video sends people: the app's page in Google Play, never the website. */
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza"
    const val STORE_LINE = "Karalak'ı Google Play'den indir: $STORE_URL"

    /** The post text with the Play link on the last line. */
    fun withStoreLink(text: String): String = if (STORE_URL in text) text else listOf(text.trim(), STORE_LINE).filter { it.isNotEmpty() }.joinToString("\n\n")

    /**
     * What to paste into ChatGPT (or Gemini) with the doodle. The drawing is the blueprint, in the
     * spirit of Tom Curtis's "children's drawings made real": every line keeps its shape and every
     * mistake stays; only the surfaces become real. A model left to itself "fixes" the drawing into
     * an ordinary animal, so the prompt says so plainly, more than once.
     */
    fun imagePrompt(word: String, wordEn: String, seconds: Int, describe: String): String = buildString {
        val thing = wordEn.ifBlank { word }
        append("The attached image is a quick doodle of \"").append(word).append("\"")
        if (wordEn.isNotBlank()) append(" (").append(wordEn).append(")")
        append(" that a player drew in ").append(seconds).append(" seconds in a drawing game.\n")
        append("Make a photo of THIS EXACT DOODLE as if it had come to life in the real world, like Tom Curtis's \"children's drawings made real\" photos.\n\n")
        append("The doodle is the blueprint, not a hint:\n")
        append("- Keep its exact silhouette, pose, proportions and layout. Trace it.\n")
        append("- Every drawn line becomes a real body part of the same shape: a single-stroke leg becomes a thin straight tube-like leg, a circle stays a round blob, a triangle stays a sharp triangle, a scribble or spiral stays a scribble or spiral, a rectangle body stays boxy.\n")
        append("- Keep every mistake: the same number of legs, missing or extra parts, a tiny or huge head, parts in odd places. Do NOT fix the anatomy, do NOT add anything that is not drawn, do NOT turn it into a normal ").append(thing).append(".\n")
        append("- Only the surfaces become real: real fur, skin, feathers, scales or material that suit a ").append(thing).append(", with real light and shadows, so it looks like a real living thing that happens to have the drawing's body.\n")
        if (describe.isNotBlank()) append("- The doodle shows: ").append(describe.trim().trimEnd('.')).append(".\n")
        append("\nStrange, funny or slightly creepy is exactly right. Anyone should recognise the drawing in it at a glance.\n")
        append("Place it in a fitting real-world spot (a field, a street, the sea, a room), shot like a casual phone photo. Vertical 9:16. No paper, no pencil lines, no text, no frame.")
    }

    /**
     * What goes to Gemini together with the picture made from the doodle. Two things have to hold at once: the
     * object stays EXACTLY as the picture shows it (Gemini otherwise "improves" it into a normal one), and yet
     * something must happen to it. An object that only sways in place is a dull video, so the prompt asks for a
     * small funny story (a person comes in and uses it, reacts to it), with real sound. [scene] is the assistant's
     * own idea for this object, edited by hand if wished; without one a generic "a person comes and uses it" is asked.
     */
    fun videoPrompt(wordEn: String, scene: String): String {
        val thing = wordEn.ifBlank { "object" }
        val story = scene.trim().trimEnd('.').ifBlank {
            "an ordinary person walks into the shot and notices the odd-looking $thing with curiosity. They use it the way anyone naturally would use a $thing, " +
                "then stop and look at it with surprise and amusement at how strange it is, and carry on"
        }
        return "Use the attached photo as the very first frame and animate it into a vertical 9:16, 8-second video that looks like a casual phone video, with sound.\n\n" +
            "The photo is a real version of a child's quick doodle of a $thing, and its clumsy, odd shape is the whole point. The $thing itself must stay EXACTLY as it is in the photo: the same silhouette, proportions, colours, materials, place and light. " +
            "Do NOT redesign, smooth out, fix or \"improve\" it. Do NOT make it more professional or more realistic. " +
            "Do NOT add eyes, faces, arms, legs, wings, windows, decorations or any part to the $thing that is not already in the photo, and do not remove any. " +
            "It must still look like a child's drawing made real.\n\n" +
            "What happens: " + story + ". " +
            "It must not just sit there and sway: something happens to it, with a clear beginning, one funny moment and a small reaction at the end, all within the 8 seconds. " +
            "Any person is an ordinary real person, natural and unhurried, seen from the side, from behind or in part; they never change the $thing's shape.\n\n" +
            "Camera: handheld phone feel, fixed or a very slow push-in; no cuts.\n\n" +
            "Sound: generate the audio too. Realistic sound that goes with what happens (the $thing's own noises, hands and footsteps, the place around it), like a real recording. No music, no text on screen."
    }

    /** One drawing the assistant described: `index` is 1-based. */
    data class RevealPick(val index: Int, val wordEn: String, val describe: String, val motion: String)

    /**
     * The assistant's description of the chosen drawings:
     * `{"reveals":[{"index":1,"word_en":"…","describe":"…","motion":"…"}],"caption":"…","hashtags":"…"}`.
     */
    data class Picks(val reveals: List<RevealPick>, val caption: String, val hashtags: String)

    fun parsePicks(text: String, candidates: Int): Picks? {
        val o = GuessCopy.objectIn(text) ?: return null
        fun str(p: JsonObject, key: String) = p[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val reveals = runCatching {
            o["reveals"]!!.jsonArray.mapNotNull { e ->
                val p = e.jsonObject
                val i = p["index"]?.jsonPrimitive?.intOrNull?.takeIf { it in 1..candidates }?.minus(1) ?: return@mapNotNull null
                RevealPick(i, str(p, "word_en"), str(p, "describe"), str(p, "motion"))
            }.distinctBy { it.index }
        }.getOrNull().orEmpty()
        if (reveals.isEmpty()) return null
        return Picks(reveals, str(o, "caption"), str(o, "hashtags"))
    }
}
