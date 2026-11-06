package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.LineRules
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.SpeechTiming
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.SpokenLine
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.mascot.MascotEmotion
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

/** One idea for the video: the joke in one sentence, and the scene (a beat sheet) that plays it. */
@Serializable
data class ScenePitch(val joke: String, val scene: String)

/**
 * A drawing turned real: its realistic picture (made in ChatGPT, kept for sharing as a swipe-through post and as
 * the starting frame for Gemini) and the clip of that picture moving, with sound (made in Gemini), and what Karalak
 * says around that clip in the video: the [hook] it opens with and the [outro] it closes on. [image], [clip] and the
 * lines' voices are file names in the project's media folder. Each drawing is a video of its own.
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
    /** What the drawing is, as the assistant sorted it: "nesne", "canlı", "yiyecek" or "bitki" (empty: not sorted yet). It decides the picture prompt's rules. */
    val kind: String = "",
    /** One line per part: the drawn part and the real part of the thing it stands for ("uzun dikdörtgen gövde → şeffaf plastik silindir"). */
    val parts: String = "",
    /** The real materials the thing is made of, in the picture prompt (plastic and rubber, fur, dough …). */
    val material: String = "",
    /** The one real place the picture is set in. */
    val scene: String = "",
    /** The thing's real size ("yaklaşık 15 cm, avuca sığar"): written into the picture and video prompts so it does not grow in the video. */
    val size: String = "",
    /** Something small and familiar beside it in the picture that shows its size ("yanında bir tükenmez kalem"). */
    val scaleHint: String = "",
    /** What never changes about it ("sert plastik ve metal: bükülmez, esnemez, kendi kendine hareket etmez"). */
    val fixed: String = "",
    /** The ideas for the video scene the assistant wrote after looking at the real picture; one is chosen into [motion]. */
    val pitches: List<ScenePitch> = emptyList(),
    val image: String? = null,
    val clip: String? = null,
    val clipMs: Long = 0L,
    val hook: VoicedLine = VoicedLine(),
    /** What Karalak says over the clip, as a deadpan documentary narrator: the comedy of the video is written here, not left to the video model. */
    val narration: VoicedLine = VoicedLine(),
    val outro: VoicedLine = VoicedLine(),
    /** The app's background music under the video (a [RealMusic] track's name); null: none. */
    val music: String? = null,
    /** How loud the music is, before it steps back under Karalak's voice and the clip (see [RealMix]). */
    val musicVolume: Float = RealMix.DEFAULT_VOLUME,
    /** How the video opens: on the game's drawing screen, or with a glimpse of the real thing first. */
    val opening: RealOpening = RealOpening.Classic,
    /**
     * What Karalak says over the end card (one of [RealCopy.END_LINES], or one's own): the card stays up for as long as it
     * is being said. Its options are the ready-made lines; its text is empty (a silent card) until one is chosen.
     */
    val endLine: VoicedLine = VoicedLine(options = RealCopy.END_LINES)
) {
    val media: Set<String> get() = listOfNotNull(image, clip, hook.voice, narration.voice, outro.voice, endLine.voice).toSet()

    fun line(kind: RealLine): VoicedLine = when (kind) {
        RealLine.Hook -> hook
        RealLine.Narration -> narration
        RealLine.Outro -> outro
        RealLine.End -> endLine
    }

    fun withLine(kind: RealLine, line: VoicedLine): RealReveal = when (kind) {
        RealLine.Hook -> copy(hook = line)
        RealLine.Narration -> copy(narration = line)
        RealLine.Outro -> copy(outro = line)
        RealLine.End -> copy(endLine = line)
    }

    /** How long the opening lasts: long enough to see the drawing drawn, and to hear all of the hook when it is voiced. */
    val hookMs: Long get() = RealTimeline.hookMs(if (hook.voice != null) hook.voiceMs else 0L, opening == RealOpening.Teaser)

    /** How long the closing lasts: long enough to read it, and to hear all of it when it is voiced. */
    val outroMs: Long get() = RealTimeline.outroMs(outro.text, if (outro.voice != null) outro.voiceMs else 0L)

    /** How long the end card stays: as long as Karalak's line over it takes to say, or a fixed beat when it is silent. */
    val endCardMs: Long get() = RealTimeline.endCardMs(if (endLine.voice != null) endLine.voiceMs else 0L)
}

/**
 * How a "Gerçek Olsaydı" video opens. [Classic]: the game's drawing screen, the drawing drawn again. [Teaser]: a glimpse
 * of the real thing first (the clip's frame, with the hook over it) that shrinks back into the canvas, and then the
 * drawing is drawn: the viewer sees what it will become before they see the doodle, which is what keeps them watching.
 */
enum class RealOpening { Classic, Teaser }

/** What closes the video: a short end card with the call to play, the app's own promo video, or nothing after the closing. */
enum class RealEnding { Card, Promo, Nothing }

/** The things Karalak says in a "Gerçek Olsaydı" video: the opening hook, the closing reaction and the line over the end card. */
enum class RealLine { Hook, Narration, Outro, End }

/**
 * One of Karalak's lines in a video: the [text] the video shows and says (empty: none), the assistant's [options]
 * to pick it from (each can also be rewritten by hand), and the [text] said aloud in ElevenLabs, [voice] (a file in
 * the project's media folder) lasting [voiceMs]; no voice: the line is only written. The voice always belongs to the
 * current [text]: changing the words drops it.
 */
@Serializable
data class VoicedLine(
    val text: String = "",
    val options: List<String> = emptyList(),
    val voice: String? = null,
    val voiceMs: Long = 0L,
    /** When each word is said in [voice] (from ElevenLabs, when it told): the subtitle comes in with them. */
    val words: List<TimedWord> = emptyList()
) {
    /** The line set to [words]; its voice goes unless the words stay the same. */
    fun withText(words: String): VoicedLine = if (words == text) this else copy(text = words, voice = null, voiceMs = 0L, words = emptyList())

    /** When each of the subtitle's words is said (see [SpeechTiming.rangesFor]); null without a voice or its timings. */
    fun wordRanges(): List<LongRange>? =
        if (voice == null || words.isEmpty()) null else SpeechTiming(words.map { SpeechTiming.Word(it.text, it.startMs, it.endMs) }).rangesFor(text)
}

/** One word of a voiced line and when it is said, in ms from the start of the voice. */
@Serializable
data class TimedWord(val text: String, val startMs: Long, val endMs: Long)

/**
 * "Gerçek Olsaydı": drawings by different players, each turned into a realistic picture and a clip with sound,
 * each a short video of its own. Several can be made at once and kept (the library holds them all); the video is
 * made from the [featured] one: its drawing drawn again on the game's own screen under its hook, its clip with the
 * player's card over it, and Karalak's closing reaction over the clip's last seconds.
 */
@Serializable
data class RealProject(
    val drawings: List<RealDrawing> = emptyList(),
    val reveals: List<RealReveal> = emptyList()
) {
    fun drawing(id: String): RealDrawing? = drawings.find { it.id == id }
    val media: Set<String> get() = (reveals.flatMap { it.media } + drawings.mapNotNull { it.photo }).toSet()

    /** The reveals with their clip; the pictures alone are shared on their own. */
    val shown: List<RealReveal> get() = reveals.filter { it.clip != null && drawing(it.sampleId) != null }

    /** The one the video is made of: the first with a clip. */
    val featured: RealReveal? get() = shown.firstOrNull()

    /** The one the video opens on, and its hook and closing are written for: [featured], or the first chosen while none has a clip. */
    val opening: RealReveal? get() = featured ?: reveals.firstOrNull { drawing(it.sampleId) != null }
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
 * its realistic picture, its clip and Karalak's lines with their voices: a video ready to be made again. [id] is the drawing's id, so saving the same drawing again updates its entry.
 * The [reveal]'s files live in the project's media folder, which nothing in the library is ever pruned from.
 */
@Serializable
data class LibraryItem(
    val id: String, val savedAt: Long, val drawing: RealDrawing, val reveal: RealReveal,
    /** Every time this piece was posted, with how it did (see [VideoPost]). */
    val posts: List<VideoPost> = emptyList()
) {
    val media: Set<String> get() = reveal.media + listOfNotNull(drawing.photo)
    val hasClip: Boolean get() = reveal.clip != null
}

enum class RealPhase { Hook, Clip, Outro, EndCard, Promo }

/**
 * One stretch of the video: the opening, a clip playing from [clipAtMs] for [clipMs], the closing over that clip's
 * last seconds (it keeps playing; only a closing longer than what is left of it holds its last frame), or the app's
 * own promo video after it all. [index] is the clip's, in the reveals shown.
 */
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
    /** The how-to video, which plays without its sound in the phone on the end card, is cut at this length. */
    const val HOWTO_MAX = 12_000L
    /** On the end card the how-to video starts after the card has opened, and the card stays a moment after it ends. */
    const val END_VIDEO_LEAD = 400L
    const val END_VIDEO_TAIL = 700L
    /** The narration starts this long into the clip: a beat to see the thing before it is explained. */
    const val NARRATION_AT = 700L
    const val CLIP_MIN = 1_000L
    /** A voiced opening is never shorter than this, so the drawing has time to be drawn. */
    const val HOOK_MIN = 2_000L
    /** A silent opening: the drawing drawn, held, and the clip opening out of it. */
    const val HOOK_SILENT = 2_200L
    /** The glimpse of the real thing a [RealOpening.Teaser] adds in front of the opening, and how much of it is held before it shrinks away. */
    const val TEASE_MS = 900L
    const val TEASE_HOLD = 450L
    /** The end of the opening, in which the first clip opens out of the drawing. */
    const val HOOK_REVEAL = 650L
    /** The finished drawing stays still for a moment before the clip opens out of it. */
    const val HOOK_HOLD = 300L

    /** When a voiced line starts, a beat after its stretch begins. */
    const val VOICE_AT = 150L
    /** After the voice ends, the first clip's own sound waits this long, so it never covers the voice's last syllable. */
    private const val VOICE_TAIL = 300L
    /** However long a voice runs, the opening never goes past this; the rest plays over the first clip. */
    const val HOOK_VOICE_MAX = 4_500L

    /**
     * How long the opening lasts. Its hook is never written on screen, only said: silent, a fixed beat for the drawing;
     * voiced ([voiceMs] above 0), until the voice has finished.
     */
    fun hookMs(voiceMs: Long = 0L, teaser: Boolean = false): Long =
        (if (voiceMs <= 0L) HOOK_SILENT else maxOf(HOOK_MIN, VOICE_AT + voiceMs + VOICE_TAIL).coerceAtMost(HOOK_VOICE_MAX)) + if (teaser) TEASE_MS else 0L

    /** The closing stays long enough to read; voiced, until it has been said and a breath after. */
    const val OUTRO_MIN = 1_500L
    const val OUTRO_MAX = 2_600L
    private const val OUTRO_TAIL = 350L
    const val OUTRO_VOICE_MAX = 5_000L

    /** How long the closing with [outro] lasts; 0 when there is none. Like [hookMs], but shorter: it is one quick reaction. */
    fun outroMs(outro: String, voiceMs: Long = 0L): Long {
        if (outro.isBlank()) return 0L
        val read = (1_000L + outro.trim().length * 30L).coerceIn(OUTRO_MIN, OUTRO_MAX)
        return if (voiceMs <= 0L) read else maxOf(read, VOICE_AT + voiceMs + OUTRO_TAIL).coerceAtMost(OUTRO_VOICE_MAX)
    }

    /** The clip always plays at least this long, or all of it when shorter, before the closing comes in over it. */
    const val OUTRO_LEAD = 2_000L

    /** The app's promo video at the end is cut after this long. */
    const val PROMO_MAX = 15_000L

    /**
     * The end card after the closing is the game's own picture (what the game is, in three steps) and it stays for as
     * long as Karalak's line over it is being said, with a breath after; silent, it stays [END_CARD_SILENT_MS] (it takes
     * a little reading). However long the voice, it is never shorter than [END_CARD_MIN_MS] nor longer than [END_CARD_MAX_MS].
     */
    const val END_CARD_SILENT_MS = 5_000L
    const val END_CARD_MIN_MS = 2_500L
    const val END_CARD_MAX_MS = 9_000L
    private const val END_TAIL = 700L

    fun endCardMs(voiceMs: Long = 0L): Long =
        if (voiceMs <= 0L) END_CARD_SILENT_MS else (VOICE_AT + voiceMs + END_TAIL).coerceIn(END_CARD_MIN_MS, END_CARD_MAX_MS)

    /**
     * The video of one reveal: its opening and closing as [RealReveal.hookMs] and [RealReveal.outroMs] say, and the
     * app's promo video of [promoMs] after it (0: none).
     */
    fun build(reveal: RealReveal?, promoMs: Long = 0L, endCardMs: Long = 0L, howtoMs: Long = 0L): List<RealEvent> =
        if (reveal == null) emptyList() else build(listOf(reveal), reveal.hookMs, reveal.outroMs, promoMs, endCardMs, howtoMs)

    /**
     * The opening (when [hookMs] is above 0), the clips back to back, the closing (when [outroMs] is above 0) over the
     * last clip's last seconds, and the promo (when [promoMs] is above 0). The clip keeps playing under the closing:
     * it comes in [outroMs] before the clip ends, but never before the clip has played [OUTRO_LEAD]; what does not fit
     * holds the clip's last frame.
     */
    fun build(shown: List<RealReveal>, hookMs: Long = 0L, outroMs: Long = 0L, promoMs: Long = 0L, endCardMs: Long = 0L, howtoMs: Long = 0L): List<RealEvent> {
        val out = mutableListOf<RealEvent>()
        var t = 0L
        if (hookMs > 0 && shown.isNotEmpty()) {
            out += RealEvent(RealPhase.Hook, 0, 0L, hookMs)
            t = hookMs
        }
        shown.forEachIndexed { i, r ->
            val ms = min(r.clipMs, CLIP_MAX).coerceAtLeast(CLIP_MIN)
            out += RealEvent(RealPhase.Clip, i, t, ms, clipAtMs = t, clipMs = ms)
            t += ms
        }
        // the closing comes in over the last clip's end: it carries that clip's timing to keep playing it
        val lastAt = out.indexOfLast { it.phase == RealPhase.Clip }
        if (outroMs > 0 && lastAt >= 0) {
            val last = out[lastAt]
            val from = maxOf(last.startMs + min(OUTRO_LEAD, last.clipMs), last.endMs - outroMs)
            out[lastAt] = last.copy(durationMs = from - last.startMs)
            out += RealEvent(RealPhase.Outro, last.index, from, outroMs, clipAtMs = last.clipAtMs, clipMs = last.clipMs)
            t = maxOf(t, from + outroMs)
        }
        // what follows the drawing's own video: the promo, or else the short end card (with the how-to video in its phone)
        if (lastAt >= 0) {
            if (promoMs > 0) out += RealEvent(RealPhase.Promo, 0, t, promoMs.coerceAtMost(PROMO_MAX))
            else if (endCardMs > 0) {
                // the how-to video plays inside the phone on the end card: the card stays at least as long as it takes
                val ms = if (howtoMs > 0) maxOf(endCardMs, howtoMs.coerceAtMost(HOWTO_MAX) + END_VIDEO_LEAD + END_VIDEO_TAIL) else endCardMs
                out += RealEvent(RealPhase.EndCard, 0, t, ms)
            }
        }
        return out
    }

    /** The stretch at [t]; the last one once the video is over. [events] is never empty (a video needs a clip). */
    fun at(events: List<RealEvent>, t: Long): RealEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<RealEvent>): Long = events.lastOrNull()?.endMs ?: 0L

    /** Where the drawing's own video ends and the promo starts: the whole video when there is no promo. */
    fun mainMs(events: List<RealEvent>): Long = events.lastOrNull { it.phase != RealPhase.Promo }?.endMs ?: 0L
}

/**
 * How the background music sits under a video, as a share of its chosen volume at each moment: it steps back under
 * Karalak's voice, a little under the clip's own sound, comes in quickly and goes out slowly at the very end.
 */
object RealMix {
    const val DEFAULT_VOLUME = 0.25f
    const val MAX_VOLUME = 0.6f
    const val UNDER_VOICE = 0.55f
    const val UNDER_CLIP = 0.7f
    private const val RAMP = 180L
    private const val FADE_IN = 300L
    const val FADE_OUT = 900L

    /** A clip's own sound comes in over this long, so a loud first frame does not land on the end of the hook. */
    const val CLIP_FADE_IN = 120L

    /** A clip's own sound under Karalak's closing, which is said over the clip's last seconds. */
    const val CLIP_UNDER_VOICE = 0.4f

    /** How loud a clip's own sound is at [ms] from its start: it comes in over [CLIP_FADE_IN]. */
    fun clipGainAt(ms: Long): Float = (ms.toFloat() / CLIP_FADE_IN).coerceIn(0f, 1f)

    /**
     * How loud the clip's own sound is at [ms] of the video, the clip starting at [clipAtMs]: it comes in softly and
     * steps back while Karalak speaks in [voices], so the closing said over it is heard.
     */
    fun clipGainAt(ms: Long, clipAtMs: Long, voices: List<LongRange>): Float =
        clipGainAt(ms - clipAtMs) * duck(ms, voices, CLIP_UNDER_VOICE)

    /** The share at [ms] of a video of [totalMs], with Karalak speaking in [voices] and the clip playing in [clip]. */
    fun gainAt(ms: Long, totalMs: Long, voices: List<LongRange>, clip: LongRange?): Float {
        var g = if (clip != null && ms in clip) UNDER_CLIP else 1f
        g = min(g, duck(ms, voices, UNDER_VOICE))
        g *= (ms.toFloat() / FADE_IN).coerceIn(0f, 1f)
        g *= ((totalMs - ms).toFloat() / FADE_OUT).coerceIn(0f, 1f)
        return g
    }

    /** [floor] while someone speaks in [voices], 1 away from them, with a short ramp between. */
    private fun duck(ms: Long, voices: List<LongRange>, floor: Float): Float {
        var g = 1f
        voices.forEach { w ->
            val away = when {
                ms < w.first -> w.first - ms
                ms > w.last -> ms - w.last
                else -> 0L
            }
            if (away < RAMP) g = min(g, floor + (1f - floor) * away / RAMP)
        }
        return g
    }
}

object RealCopy {
    /** How many ideas for the opening line, and for the closing one, the assistant writes at once. */
    const val HOOK_COUNT = 10
    const val OUTRO_COUNT = 5
    /** Longer than this and a line no longer fits the board or the subtitle, nor is read in time. */
    const val HOOK_CHARS = 80
    private val SECONDS = Regex("(\\d+)(\\s*(?:saniye|sn|seconds?|secs?|segundos?|seg))", RegexOption.IGNORE_CASE)
    private val NUMBERING = Regex("^\\d+[.)]\\s+")

    /** What kind of thing a drawing is: it decides whether the real one may have fur, eyes and limbs at all. */
    enum class Kind { Object, Creature, Food, Plant, Unknown;
        companion object {
            fun of(text: String?): Kind {
                val t = text.orEmpty().trim().lowercase(java.util.Locale("tr"))
                return when {
                    t.startsWith("nesne") || t.startsWith("obje") || t.startsWith("eşya") || t.startsWith("object") -> Object
                    t.startsWith("canlı") || t.startsWith("hayvan") || t.startsWith("yaratık") || t.startsWith("creature") || t.startsWith("animal") -> Creature
                    t.startsWith("yiyecek") || t.startsWith("yemek") || t.startsWith("içecek") || t.startsWith("food") -> Food
                    t.startsWith("bitki") || t.startsWith("ağaç") || t.startsWith("çiçek") || t.startsWith("plant") -> Plant
                    else -> Unknown
                }
            }
        }
    }

    /**
     * What to paste into ChatGPT with the doodle, in Turkish so it can be read and corrected by hand. The drawing is the
     * blueprint, in the spirit of "children's drawings made real": every line keeps its shape and every quirk stays; only the
     * surfaces become real.
     *
     * Short, in sections, and in positive words: a long prompt full of "do not" lines, with an invitation to be "weird or a
     * little scary", made a model turn a drawn syringe into a living pink sausage with eyes. So what the thing IS comes
     * first (an object has no eyes, fur or limbs; only a creature has fur and skin), then which drawn part is which real
     * part, then the materials, one real place and the style (a phone photograph, not an illustration).
     */
    fun imagePrompt(word: String, seconds: Int, reveal: RealReveal?): String = buildString {
        val thing = word.ifBlank { "nesne" }
        val kind = Kind.of(reveal?.kind)
        append("Ekteki çizim, bir çizim oyununda \"").append(thing).append("\" kelimesi için ").append(seconds).append(" saniyede yapılmış hızlı bir karalama. ")
        append("Bu çizimi, çizildiği gibi gerçekten var olan bir \"").append(thing).append("\" hâline getir ve onu gerçek bir telefon fotoğrafı olarak göster: bir çocuk çiziminin gerçek hâli. ")
        append("Şekli çizimdeki gibi, yüzeyi ve ortamı tamamen gerçek olsun.\n\n")
        append(
            when (kind) {
                Kind.Object -> "Bu bir NESNE. Canlı değil: gözü, yüzü, ağzı, kolu, bacağı, teni, tüyü yok."
                Kind.Creature -> "Bu bir CANLI. Gerçek tüyü, derisi, pulu ya da kabuğu olsun; ama vücudu çizimdeki vücut olsun."
                Kind.Food -> "Bu bir YİYECEK ya da İÇECEK. Canlı değil: gözü, yüzü, ağzı, kolu, bacağı yok. Gerçek, yenebilir malzemeden yapılmış gibi görünsün."
                Kind.Plant -> "Bu bir BİTKİ. Gerçek yaprağı, gövdesi, kabuğu ya da çiçeği olsun; gözü, yüzü, ağzı, kolu, bacağı yok."
                Kind.Unknown -> "Önce \"$thing\" kelimesinin bir nesne, canlı, yiyecek ya da bitki olduğuna karar ver ve ona göre davran. Nesnelerin, yiyeceklerin ve bitkilerin gözü, yüzü, ağzı, kolu, bacağı, teni, tüyü olmaz; yalnızca canlıların tüyü ya da derisi olur."
            }
        ).append("\n")
        if (kind == Kind.Creature) append("Vücudunu gerçek hayvana benzetip düzeltme: gövde, baş, bacak sayısı ve bunların yerleri çizimdeki gibi kalsın (örneğin tüp gibi bir gövde, ince bacaklar, yanlış yere oturmuş bir baş); yalnızca yüzeyi gerçek tüy, deri ya da pul olsun.\n")
        append("Çizimin kalın çizgileri yüzeye basılmış çizgi olarak kalmasın: her çizgi gerçek bir kenar, kıvrım, dikiş, boğum ya da gölge olsun. Şekil üç boyutlu, hacimli ve gerçek dokulu olsun; düz bir disk, çıkartma ya da boyama gibi görünmesin.\n\n")
        val parts = reveal?.parts?.lines()?.map { it.trim().trimStart('-', '•', ' ') }?.filter { it.isNotEmpty() }.orEmpty()
        if (parts.isNotEmpty()) {
            append("Çizimden gerçeğe eşleme:\n")
            parts.forEach { append("- ").append(it).append('\n') }
            append('\n')
        }
        val describe = reveal?.describe.orEmpty().trim().trimEnd('.')
        if (describe.isNotEmpty()) append("Çizimin şekli: ").append(describe).append(".\n\n")
        append("Siluetini, duruşunu, oranlarını ve parça sayısını aynen koru. Çizimdeki tuhaf, beceriksiz oranlar gerçek hâlinde de kalsın. ")
        append("Çizimde olmayan hiçbir şey ekleme; çizimdeki hiçbir parçayı da çıkarma. Normal bir \"").append(thing).append("\" hâline getirme.\n")
        val material = reveal?.material.orEmpty().trim().trimEnd('.')
        append("Malzeme: ")
        append(if (material.isNotEmpty()) material else "gerçek bir \"$thing\" neyden yapılıyorsa o gerçek malzemeler")
        append("; gerçek ışık, yansıma ve gölgeler.\n")
        val size = reveal?.size.orEmpty().trim().trimEnd('.')
        if (size.isNotEmpty()) {
            append("Ölçü: gerçek boyutu ").append(size).append(". Fotoğrafta bu gerçek boyutuyla görünsün")
            reveal?.scaleHint.orEmpty().trim().trimEnd('.').takeIf { it.isNotEmpty() }?.let { append("; boyutu anlaşılsın diye ").append(it) }
            append(".\n")
        }
        append("Tarz: sıradan bir telefon fotoğrafı, gerçek dokular; arka plan da gerçek bir yer. İllüstrasyon, çizgi film, anime, boyama ve 3B görsel olmasın.\n")
        val scene = reveal?.scene.orEmpty().trim().trimEnd('.')
        append("Sahne: ")
        append(if (scene.isNotEmpty()) scene else "tek, gerçek ve sıradan bir yer (\"$thing\" nerede olurdu ya da bulunurdu)")
        append("; yumuşak gün ışığı, göz hizasında çekim.\n")
        append("Ton: komik ve sevimli bir tuhaflık; ürkütücü ya da korkutucu olmasın. Çizimi herkes bir bakışta içinde tanımalı.\n")
        append("Yerleşim: çizim kare bir kâğıdın ortasına sığacak şekilde çizilmiş. O kâğıdı kadrajın genişliği kadar, dikeyde ortada bir kare say ve nesneyi bu karenin içinde, çizimin kâğıttaki yeri ve oranlarıyla aynen yerleştir; karenin dışı sahnenin devamı olsun. Video bu görselden başlayacak ve çizim ona kayarak dönüşecek.\n")
        append("Çıktı: tek görsel, dikey 9:16. Kâğıt, kalem çizgisi, yazı, çerçeve ve fazladan karakter olmasın.")
    }

    /**
     * What goes to Gemini together with the picture made from the doodle, in Turkish so it can be corrected by hand.
     * Kept short and built from the continuity notes the assistant filled in ([RealReveal.fixed], [RealReveal.size],
     * [RealReveal.scene]) and the chosen scene ([RealReveal.motion], a beat sheet). The rules answer what went wrong
     * in the first videos: the room changed when a second person came in (one shot, one place, nobody comes through a
     * door), the object grew to a metre (its real size is said, and that it is held in a hand), and a rigid needle turned
     * into a spring (the thing never changes shape; only a person moves it).
     */
    fun videoPrompt(word: String, reveal: RealReveal?): String {
        val thing = word.ifBlank { "nesne" }
        val creature = Kind.of(reveal?.kind) == Kind.Creature
        val story = reveal?.motion.orEmpty().trim().trimEnd('.').ifBlank {
            if (creature) "Hayvan tuhaf vücuduyla doğal bir şekilde yürür, otlar ya da etrafına bakar; tuhaf göründüğünü fark etmez"
            else "Sıradan biri (\"$thing\" nesnesine) merakla bakar, onu doğal olarak kullanır, tuhaf göründüğünü fark etse de ciddiyetini bozmadan işine devam eder"
        }
        val fixed = reveal?.fixed.orEmpty().trim().trimEnd('.')
        val size = reveal?.size.orEmpty().trim().trimEnd('.')
        val place = reveal?.scene.orEmpty().trim().trimEnd('.')
        if (creature) return creatureVideoPrompt(thing, story, size, place)
        return buildString {
            append("Ekteki fotoğrafı ilk kare olarak kullan. 8 saniyelik, dikey 9:16, sesli, sabit tutulan bir telefonla çekilmiş gerçek bir video; TEK ÇEKİM.\n\n")
            append("Fotoğraftaki \"").append(thing).append("\" tuhaf hâliyle baştan sona görünür kalır; yerine sıradan, normal bir \"").append(thing).append("\" KOYMA.\n")
            reveal?.describe.orEmpty().trim().trimEnd('.').takeIf { it.isNotEmpty() }?.let { append("Tuhaf şekli: ").append(it).append(". Bu şekil videonun her karesinde aynıdır, hiçbir zaman düzgün, normal bir şekle dönmez.\n") }
            append("Nesne: \"").append(thing).append("\" fotoğraftaki gibi kalır: aynı şekil, oranlar, renkler, malzeme ve yer. ")
            append(if (fixed.isNotEmpty()) "$fixed. " else "Sert, katı bir nesne gibi davranır: bükülmez, esnemez, şekil ya da malzeme değiştirmez, canlanmaz, göz ya da yüz çıkmaz. ")
            if (size.isNotEmpty()) append("Gerçek boyutu ").append(size).append(": kişinin yanında bu ölçüde görünür, büyümez ya da küçülmez. ")
            append("Yeniden tasarlama, güzelleştirme, ekleme ya da çıkarma yok.\n")
            append("Mekân: fotoğraftaki yer baştan sona AYNI kalır")
            if (place.isNotEmpty()) append(" (").append(place).append(")")
            append(": aynı duvar, yüzey, eşyalar ve ışık. Kesme, sahne değişimi, yeni oda, kapı açılması ya da yeni bir kişinin girişi yok.\n")
            append("Kişi: en fazla bir kişi ve yalnızca elleri ile kolları görünür; kadrajın kenarından girer, yüzü ve gövdesi görünmez, başka kimse girmez.\n")
            append("Hareket kuralı: nesne yerinde SABİT durur ve hiç hareket etmez; parçaları birbirine göre kıpırdamaz, yeri ve duruşu değişmez. Eller ona dokunmaz ya da yalnızca kenarına bir an hafifçe değer; nesnenin üstünü örtmez (en fazla %10'unu kapatır), çünkü örtülen yeri model baştan, normal hâliyle çizer. Elin işi göstermek, yaklaşmak, havada işaret etmek, nesnenin yanında tezgâha vurmak gibi hareketlerdir. Nesne kendiliğinden hareket etmez, bükülmez, yaylanmaz, şekil değiştirmez. Nesne yenmez, ısırılmaz, açılmaz, kesilmez, giyilmez, kırılmaz; hiçbir parçası çıkmaz ya da eksilmez; normal, sıradan bir kullanım gerektiren eylem yok.\n")
            append("Kadraj: ilk karedeki gibi KALIR; yakınlaşma, kayma ve kişiye dönme yok. Nesne baştan sona kadrajın içinde ve aynı yerde görünür.\n\n")
            append("Olay: ").append(story).append(".\n\n")
            append("Kamera: sabit ya da çok yavaş yaklaşan; kesme yok.\n")
            append("Ses: olanlara uyan gerçekçi sesler (adım, nesnenin kendi sesi, eller), gerçek bir kayıt gibi. Müzik yok, ekranda yazı yok; sahnede açıkça yazılmadıkça kimse konuşmasın.")
        }
    }

    /**
     * The video prompt for a living thing, in the spirit of children's drawings turned into real animals: the creature
     * itself is the star, alive and at home in its place, its drawn body kept as it is (never swapped for an ordinary
     * animal) and made to work; no person is needed.
     */
    private fun creatureVideoPrompt(thing: String, story: String, size: String, place: String): String = buildString {
        append("Ekteki fotoğrafı ilk kare olarak kullan. 8 saniyelik, dikey 9:16, sesli, sabit tutulan bir telefonla çekilmiş gerçek bir video; TEK ÇEKİM.\n\n")
        append("Canlı: fotoğraftaki \"").append(thing).append("\" baştan sona AYNI kalır: aynı tuhaf vücut, oranlar, bacak sayısı ve yerleri, renk ve tüy ya da deri. Onu normal bir hayvana çevirme, düzeltme, yerine sıradan bir hayvan koyma. ")
        if (size.isNotEmpty()) append("Gerçek boyutu ").append(size).append(". ")
        append("Ekleme ya da çıkarma yok.\n")
        append("Hareket: hayvan canlı ve doğal davranır; tuhaf vücudu bu hareketi gerçekten yapıyormuş gibi çalışır (yavaş, ağır, inandırıcı). Vücudu şekil değiştirmez.\n")
        append("Mekân: fotoğraftaki yer baştan sona AYNI kalır")
        if (place.isNotEmpty()) append(" (").append(place).append(")")
        append(". Kesme, sahne değişimi yok. Kadrajda insan yok.\n\n")
        append("Olay: ").append(story).append(".\n\n")
        append("Kamera: sabit ya da çok yavaş yaklaşan; kesme yok. Gerçek bir doğa ya da telefon kaydı gibi.\n")
        append("Ses: ortama ve hayvana uyan gerçekçi sesler (adım, nefes, rüzgâr, hayvanın kendi sesi). Müzik yok, ekranda yazı yok, konuşma yok.")
    }

    /**
     * What the assistant is asked, with the real picture in front of it, to come up with the video scene: several jokes
     * thought through, the best three given, each a beat sheet that obeys the same continuity rules as [videoPrompt].
     */
    fun pitchPrompt(word: String, reveal: RealReveal): String = buildString {
        val thing = word.ifBlank { "nesne" }
        append("Ekteki fotoğraf, bir çocuk çiziminin gerçek hâli: \"").append(thing).append("\". Bu fotoğrafı ilk kare alıp 8 saniyelik, TEK ÇEKİMLİK komik bir video yapacağız.\n\n")
        append("Bilinenler:\n")
        if (reveal.kind.isNotBlank()) append("- Türü: ").append(reveal.kind).append('\n')
        if (reveal.size.isNotBlank()) append("- Gerçek boyutu: ").append(reveal.size).append('\n')
        if (reveal.material.isNotBlank()) append("- Malzemesi: ").append(reveal.material).append('\n')
        if (reveal.fixed.isNotBlank()) append("- Değişmeyenler: ").append(reveal.fixed).append('\n')
        if (reveal.scene.isNotBlank()) append("- Mekân: ").append(reveal.scene).append('\n')
        if (reveal.describe.isNotBlank()) append("- Çizimin tuhaf şekli: ").append(reveal.describe).append('\n')
        append("\nÖNCE \"candidates\" listesine 6 farklı espri fikri yaz, her biri TEK kısa cümle. SONRA bunlardan birbirinden FARKLI mekanizmalı en iyi 3'ünü seç ve \"ideas\" içinde sahnesiyle yaz. Espri, nesnenin gerçek kullanımından ya da çizimin tuhaf özelliğinden doğsun. ")
        append("Espri mekanizmaları: ciddi, sakin bir tepki (kişi nesnenin tuhaflığını hiç yadırgamaz); makul ama yanlış kullanım; üç denemede giderek artan absürtlük; hiçbir şey olmamış gibi davranma; nesnenin kendi sesi ve zamanlaması.\n\n")
        val creature = Kind.of(reveal.kind) == Kind.Creature
        append("KESİN KURALLAR (her fikir bunlara uymalı, yazmadan önce tek tek kontrol et):\n")
        append("1. Tek çekim, tek mekân: fotoğraftaki yer baştan sona aynı. Kapı açılması, yeni oda, kesme, ikinci bir kişinin girişi YOK.\n")
        if (creature) {
            append("2. Kadrajda insan yok: yıldız hayvanın kendisi. Hayvan doğal davranır (yürür, otlar, yüzer, bakar, kaçar); sıradan bir hayvanla DEĞİŞMEZ.\n")
            append("3. Hayvanın tuhaf vücudu (çizimdeki gibi gövde, bacak, baş) baştan sona aynı kalır ve espri bu vücudun doğal davranışı yaparkenki hâlinden doğar; vücut şekil değiştirmez.\n")
        } else {
            append("2. En fazla bir kişi ve yalnızca elleri ile kolları görünür (yüz ve gövde yok); eller kadrajın kenarından girer. Kadraj ilk fotoğraftaki gibi kalır, nesne baştan sona aynı yerde görünür.\n")
            append("3. Nesne şekil, malzeme ya da boyut değiştirmez: sertse bükülmez, yaylanmaz, sallanmaz, canlanmaz, göz ya da yüz çıkmaz. Nesne yerinde SABİT kalır, hiç hareket etmez, parçaları birbirine göre kıpırdamaz; eller ona dokunmaz ya da yalnızca kenarına bir an değer ve üstünü örtmez (en fazla %10'u), çünkü örtülen yeri model normal hâliyle yeniden çizer. Elin işi göstermek, yaklaşmak, işaret etmek, yanına vurmaktır. Nesne yenmez, ısırılmaz, açılmaz, kesilmez, giyilmez, kırılmaz; parçası çıkmaz ya da eksilmez; sıradan, doğru kullanım gerektiren eylem (yemek, içmek, giymek, açmak, kesmek) YOK, çünkü model nesneyi normal hâline çevirir.\n")
        }
        append("4. Boyut gerçek kalır: kişinin elinde ya da yanında nesnenin gerçek ölçüsünde görünür.\n")
        append("5. 8 saniyeye sığar: 0–2 sn kurulum, 2–5 sn gelişme, 5–8 sn sürpriz; her aralıkta tek eylem.\n")
        append("6. Sonda duyulacak bir ses fikri ver (adım, nesnenin sesi, bir tık); konuşma yok.\n")
        append("7. Çizimdeki tuhaf özellik videonun her anında net görünür kalır ve espri onunla ilgilidir; yerine sıradan bir benzeri geçmez.\n\n")
        append("Her fikir için: \"joke\" esprinin ne olduğunu anlatan TEK cümle; \"scene\" üç zaman aralığıyla yazılmış sahne (mekân, kişi, ne yaptığı, ses). Hepsini TÜRKÇE yaz.\n")
        append("Yalnızca şu JSON'u yaz: {\"candidates\":[\"…\",\"…\",\"…\",\"…\",\"…\",\"…\"],\"ideas\":[{\"joke\":\"…\",\"scene\":\"…\"},{\"joke\":\"…\",\"scene\":\"…\"},{\"joke\":\"…\",\"scene\":\"…\"}]}")
    }

    /** The scene ideas out of the assistant's answer; null when none could be read. */
    fun parsePitches(text: String): List<ScenePitch>? {
        val o = StudioCopy.objectIn(text) ?: return null
        val ideas = runCatching {
            o["ideas"]!!.jsonArray.mapNotNull { e ->
                val p = e.jsonObject
                val joke = p["joke"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val scene = p["scene"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (scene.isEmpty()) null else ScenePitch(joke, scene)
            }
        }.getOrNull().orEmpty()
        return ideas.take(3).takeIf { it.isNotEmpty() }
    }

    /** One drawing the assistant described: `index` is 1-based. */
    data class RevealPick(
        val index: Int, val wordEn: String, val describe: String, val motion: String,
        val kind: String = "", val parts: String = "", val material: String = "", val scene: String = "",
        val size: String = "", val scaleHint: String = "", val fixed: String = ""
    )

    /**
     * The assistant's description of the chosen drawings:
     * `{"reveals":[{"index":1,"word_en":"…","describe":"…","motion":"…"}],"caption":"…","hashtags":"…"}`.
     */
    data class Picks(val reveals: List<RevealPick>, val caption: String, val hashtags: String)

    /** The opening line as Karalak says it: eagerly ("[excited]" in front, with v3). */
    fun hookLine(text: String): SpokenLine = SpokenLine(text.trim(), MascotEmotion.Excited)

    /** The narration as Karalak says it: calm and completely serious, like a nature documentary. */
    fun narrationLine(text: String): SpokenLine = SpokenLine(text.trim(), MascotEmotion.Neutral)

    /** The closing line as Karalak says it: playfully, a reaction and a question. */
    fun outroLine(text: String): SpokenLine = SpokenLine(text.trim(), MascotEmotion.Point)

    /** The line over the end card as Karalak says it: cheerfully, an invitation. */
    fun endLine(text: String): SpokenLine = SpokenLine(text.trim(), MascotEmotion.Wave)

    fun spoken(kind: RealLine, text: String): SpokenLine = when (kind) {
        RealLine.Hook -> hookLine(text)
        RealLine.Narration -> narrationLine(text)
        RealLine.Outro -> outroLine(text)
        RealLine.End -> endLine(text)
    }

    /** Longest a line of Karalak's may be, by what it is for: the end card's lines are whole sentences, the hook one breath. */
    fun maxChars(kind: RealLine): Int = when (kind) { RealLine.End -> END_CHARS; RealLine.Narration -> NARRATION_CHARS; else -> HOOK_CHARS }
    const val NARRATION_CHARS = 110
    const val NARRATION_COUNT = 5
    const val END_CHARS = 140

    /**
     * The ready-made lines for over the end card, to choose from and voice: each says what the game is and asks the
     * viewer in, in a few seconds. The end card itself is the same picture in every video.
     */
    val END_LINES: List<String> = listOf(
        "Karalak'ta önce çiz, sonra hatırla! Bakalım kendi karalamanı tanıyabilecek misin?",
        "Çiz, hatırla, tahmin et! Karalak, Google Play'de seni bekliyor!",
        "Birkaç saniyede çiz, sonra ne çizdiğini hatırla. Kulağa kolay geliyor, değil mi?",
        "Çizmek kolay, hatırlamak zor! Karalak'ta kendini dene!",
        "Kendi çizimini tanıyamazsan üzülme, kimse tanıyamıyor! Hemen dene!",
        "Saniyeler içinde çiz, sonra kendi eserine bak: bu neydi?",
        "Çizim yeteneğin değil, hafızan konuşsun! Karalak'ta buluşalım!",
        "Arkadaşlarınla yarış: kim kendi çizimini daha iyi hatırlayacak?"
    )

    /**
     * The assistant's ideas for the opening line: `{"hooks":["…",…]}`, cleaned (see [cleanHook]), the same one
     * only once, at most [HOOK_COUNT]. [seconds] are the opening drawing's.
     */
    fun parseHooks(text: String, seconds: Int): List<String> = parseLines(text, "hooks", seconds, HOOK_COUNT, LineRules.HOOK_WORDS)

    /** The assistant's ideas for the narration over the clip: `{"narrations":["…",…]}`, cleaned, at most [NARRATION_COUNT]. */
    fun parseNarrations(text: String, seconds: Int): List<String> = parseLines(text, "narrations", seconds, NARRATION_COUNT, LineRules.NARRATION_WORDS, NARRATION_CHARS)

    /** The assistant's ideas for the closing line: `{"outros":["…",…]}`, cleaned the same way, at most [OUTRO_COUNT]. */
    fun parseOutros(text: String, seconds: Int): List<String> = parseLines(text, "outros", seconds, OUTRO_COUNT, LineRules.CLOSING_WORDS)

    /**
     * The ideas under [key], cleaned. Ideas that run past [maxWords] are left out as long as enough others are left to
     * choose from ([MIN_TO_CHOOSE]); when too few are, the long ones are cut instead of leaving nothing.
     */
    private fun parseLines(text: String, key: String, seconds: Int, count: Int, maxWords: Int, maxChars: Int = HOOK_CHARS): List<String> {
        val o = StudioCopy.objectIn(text) ?: return emptyList()
        val raw = runCatching { o[key]!!.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull } }.getOrNull().orEmpty()
        val cleaned = raw.map { cleanHook(it, seconds, maxChars) }.filter { it.isNotEmpty() }.distinctBy { StudioCopy.normalize(it) }
        val fitting = cleaned.filterNot { LineRules.tooLong(it, maxWords) }
        val use = if (fitting.size >= minOf(MIN_TO_CHOOSE, cleaned.size)) fitting else cleaned.map { LineRules.fit(it, maxWords) }.distinctBy { StudioCopy.normalize(it) }
        return use.take(count)
    }

    /** Fewest ideas worth offering: below it a long idea is cut rather than dropped. */
    private const val MIN_TO_CHOOSE = 3

    /**
     * One line as the video shows it: no quotes or list number around it, and any time it gives set to the
     * drawing's real [seconds] (0: not known, left as written), so the video never claims a time the game did not give.
     */
    fun cleanHook(text: String, seconds: Int, maxChars: Int = HOOK_CHARS): String {
        var s = text.trim().trim('"', '“', '”', '\'', '«', '»').trim().replace(NUMBERING, "").trim()
        if (seconds > 0) s = SECONDS.replace(s) { m -> "$seconds" + m.groupValues[2] }
        return if (s.length <= maxChars) s else s.take(maxChars).substringBeforeLast(' ', s.take(maxChars)).trim()
    }

    fun parsePicks(text: String, candidates: Int): Picks? {
        val o = StudioCopy.objectIn(text) ?: return null
        fun str(p: JsonObject, key: String) = p[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val reveals = runCatching {
            o["reveals"]!!.jsonArray.mapNotNull { e ->
                val p = e.jsonObject
                val i = p["index"]?.jsonPrimitive?.intOrNull?.takeIf { it in 1..candidates }?.minus(1) ?: return@mapNotNull null
                // "parts" is a list of lines, or one text with the lines in it
                val parts = when (val e = p["parts"]) {
                    is kotlinx.serialization.json.JsonArray -> e.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }.joinToString("\n")
                    is kotlinx.serialization.json.JsonPrimitive -> e.contentOrNull.orEmpty().trim()
                    else -> ""
                }
                RevealPick(i, str(p, "word_en"), str(p, "describe"), str(p, "motion"), str(p, "kind"), parts, str(p, "material"), str(p, "scene"), str(p, "size"), str(p, "scale_hint"), str(p, "fixed"))
            }.distinctBy { it.index }
        }.getOrNull().orEmpty()
        if (reveals.isEmpty()) return null
        return Picks(reveals, str(o, "caption"), str(o, "hashtags"))
    }
}
