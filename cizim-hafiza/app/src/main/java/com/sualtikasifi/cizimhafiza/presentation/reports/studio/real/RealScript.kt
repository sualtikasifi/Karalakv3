package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import com.sualtikasifi.cizimhafiza.domain.model.DrawingPoint
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.GuessCopy
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.guess.SpokenLine
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
    val image: String? = null,
    val clip: String? = null,
    val clipMs: Long = 0L,
    val hook: VoicedLine = VoicedLine(),
    val outro: VoicedLine = VoicedLine()
) {
    val media: Set<String> get() = listOfNotNull(image, clip, hook.voice, outro.voice).toSet()

    fun line(kind: RealLine): VoicedLine = when (kind) {
        RealLine.Hook -> hook
        RealLine.Outro -> outro
    }

    fun withLine(kind: RealLine, line: VoicedLine): RealReveal = when (kind) {
        RealLine.Hook -> copy(hook = line)
        RealLine.Outro -> copy(outro = line)
    }

    /** How long the opening lasts: long enough to read the line, and to hear all of it when it is voiced. */
    val hookMs: Long get() = RealTimeline.hookMs(hook.text, hook.voiceMs)

    /** How long the closing lasts, the same way. */
    val outroMs: Long get() = RealTimeline.outroMs(outro.text, outro.voiceMs)
}

/** The two things Karalak says in a "Gerçek Olsaydı" video: the opening hook and the closing reaction. */
enum class RealLine { Hook, Outro }

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
    val voiceMs: Long = 0L
) {
    /** The line set to [words]; its voice goes unless the words stay the same. */
    fun withText(words: String): VoicedLine = if (words == text) this else copy(text = words, voice = null, voiceMs = 0L)
}

/**
 * "Gerçek Olsaydı": drawings by different players, each turned into a realistic picture and a clip with sound,
 * each a short video of its own. Several can be made at once and kept (the library holds them all); the video is
 * made from the [featured] one: its drawing drawn again on the game's own screen under its hook, its clip with the
 * player's card over it, and Karalak's closing reaction on the clip's last frame.
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
data class LibraryItem(val id: String, val savedAt: Long, val drawing: RealDrawing, val reveal: RealReveal) {
    val media: Set<String> get() = reveal.media + listOfNotNull(drawing.photo)
    val hasClip: Boolean get() = reveal.clip != null
}

enum class RealPhase { Hook, Clip, Outro }

/**
 * One stretch of the video: the opening, a clip playing from [clipAtMs] for [clipMs], or the closing on that clip's
 * last frame. [index] is the clip's, in the reveals shown.
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
    private const val CLIP_MIN = 1_000L
    /** The opening stays long enough to read its line, never so long that people swipe on. */
    const val HOOK_MIN = 2_400L
    const val HOOK_MAX = 3_600L
    /** The end of the opening, in which the first clip opens out of the drawing. */
    const val HOOK_REVEAL = 450L
    /** The finished drawing stays still for a moment before the clip opens out of it. */
    const val HOOK_HOLD = 300L

    /** When the voiced line starts, a beat after the first frame. */
    const val VOICE_AT = 150L
    /** After the voice ends, the first clip's own sound waits this long. */
    private const val VOICE_TAIL = 200L
    /** However long a voice runs, the opening never goes past this; the rest plays over the first clip. */
    const val HOOK_VOICE_MAX = 7_000L

    /**
     * How long the opening with [hook] lasts; 0 when there is none. Silent, by the line's length, as it has to be read;
     * voiced ([voiceMs] above 0), at least until the voice has finished.
     */
    fun hookMs(hook: String, voiceMs: Long = 0L): Long {
        if (hook.isBlank()) return 0L
        val read = (1_200L + hook.trim().length * 30L).coerceIn(HOOK_MIN, HOOK_MAX)
        return if (voiceMs <= 0L) read else maxOf(read, VOICE_AT + voiceMs + VOICE_TAIL).coerceAtMost(HOOK_VOICE_MAX)
    }

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

    /** The video of one reveal: its opening and closing as [RealReveal.hookMs] and [RealReveal.outroMs] say. */
    fun build(reveal: RealReveal?): List<RealEvent> = if (reveal == null) emptyList() else build(listOf(reveal), reveal.hookMs, reveal.outroMs)

    /** The opening (when [hookMs] is above 0), the clips back to back, and the closing (when [outroMs] is above 0). */
    fun build(shown: List<RealReveal>, hookMs: Long = 0L, outroMs: Long = 0L): List<RealEvent> {
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
        // the closing holds the last clip's last frame: it carries that clip's timing to find it
        val last = out.lastOrNull { it.phase == RealPhase.Clip }
        if (outroMs > 0 && last != null) out += RealEvent(RealPhase.Outro, last.index, t, outroMs, clipAtMs = last.clipAtMs, clipMs = last.clipMs)
        return out
    }

    /** The stretch at [t]; the last one once the video is over. [events] is never empty (a video needs a clip). */
    fun at(events: List<RealEvent>, t: Long): RealEvent = events.firstOrNull { t < it.endMs } ?: events.last()

    fun totalMs(events: List<RealEvent>): Long = events.lastOrNull()?.endMs ?: 0L
}

object RealCopy {
    /** How many ideas for the opening line, and for the closing one, the assistant writes at once. */
    const val HOOK_COUNT = 10
    const val OUTRO_COUNT = 5
    /** Longer than this and a line no longer fits the board or the subtitle, nor is read in time. */
    const val HOOK_CHARS = 80
    private val SECONDS = Regex("(\\d+)(\\s*(?:saniye|sn))", RegexOption.IGNORE_CASE)
    private val NUMBERING = Regex("^\\d+[.)]\\s+")

    /** Where the video sends people: the app's page in Google Play, never the website. */
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.sualtikasifi.cizimhafiza"
    const val STORE_LINE = "Karalak'ı Google Play'den indir: $STORE_URL"

    /** The post text with the Play link on the last line. */
    fun withStoreLink(text: String): String = if (STORE_URL in text) text else listOf(text.trim(), STORE_LINE).filter { it.isNotEmpty() }.joinToString("\n\n")

    /**
     * What to paste into ChatGPT (or Gemini) with the doodle, in Turkish so it can be read and corrected by hand. The
     * drawing is the blueprint, in the spirit of Tom Curtis's "children's drawings made real": every line keeps its
     * shape and every mistake stays; only the surfaces become real. A model left to itself "fixes" the drawing into
     * an ordinary animal, so the prompt says so plainly, more than once. [describe] is the drawing's shapes as drawn.
     */
    fun imagePrompt(word: String, seconds: Int, describe: String): String = buildString {
        val thing = word.ifBlank { "nesne" }
        append("Ekteki görsel, bir oyuncunun çizim oyununda \"").append(thing).append("\" kelimesi için ").append(seconds).append(" saniyede yaptığı hızlı bir karalama.\n")
        append("Bu KARALAMANIN BİREBİR AYNISINI, sanki gerçek dünyada canlanmış gibi gösteren bir fotoğraf yap; Tom Curtis'in \"çocuk çizimlerinin gerçek hâli\" fotoğrafları gibi.\n\n")
        append("Karalama bir ipucu değil, bir plan:\n")
        append("- Siluetini, duruşunu, oranlarını ve düzenini aynen koru. Üstünden geçer gibi izle.\n")
        append("- Çizilen her çizgi aynı şekilde gerçek bir vücut parçası olsun: tek çizgilik bir bacak ince ve düz, boru gibi bir bacak olur, daire yuvarlak bir top olarak kalır, üçgen sivri bir üçgen kalır, çizgi karmaşası ya da spiral öyle kalır, dikdörtgen gövde köşeli kalır.\n")
        append("- Her hatayı koru: aynı bacak sayısı, eksik ya da fazla parçalar, küçücük ya da kocaman bir baş, tuhaf yerlerdeki parçalar. Anatomiyi DÜZELTME, çizilmeyen hiçbir şey EKLEME, onu normal bir \"").append(thing).append("\" hâline GETİRME.\n")
        append("- Sadece yüzeyler gerçek olsun: \"").append(thing).append("\" için uygun gerçek tüy, deri, pul ya da malzeme; gerçek ışık ve gölgeler. Gerçek, yaşayan bir şey gibi görünsün ama bedeni çizimdeki beden olsun.\n")
        if (describe.isNotBlank()) append("- Karalamanın şekli: ").append(describe.trim().trimEnd('.')).append(".\n")
        append("\nTuhaf, komik ya da biraz ürkütücü olması tam istediğimiz şey. Çizimi herkes bir bakışta içinde tanımalı.\n")
        append("Uygun, gerçek bir yere (tarla, sokak, deniz, oda) yerleştir; sıradan bir telefon fotoğrafı gibi çek. Dikey 9:16. Kâğıt yok, kalem çizgisi yok, yazı yok, çerçeve yok.")
    }

    /**
     * What goes to Gemini together with the picture made from the doodle, in Turkish so it can be corrected by hand.
     * Two things have to hold at once: the object stays EXACTLY as the picture shows it (Gemini otherwise "improves"
     * it into a normal one), and yet something must happen to it. An object that only sways in place is a dull video,
     * so the prompt asks for a small funny story (a person comes in and uses it, reacts to it), with real sound.
     * [scene] is the assistant's idea for this object, as long and as detailed as wished; without one a generic
     * "a person comes and uses it" is asked.
     */
    fun videoPrompt(word: String, scene: String): String {
        val thing = word.ifBlank { "nesne" }
        val story = scene.trim().trimEnd('.').ifBlank {
            "Sıradan biri çekime girer, tuhaf görünen o nesneyi (\"$thing\") merakla fark eder. Onu herkesin doğal olarak kullanacağı gibi kullanır, " +
                "sonra durup ne kadar tuhaf olduğuna şaşırarak gülümser ve işine devam eder"
        }
        return "Ekteki fotoğrafı ilk kare olarak kullan ve onu dikey 9:16, 8 saniyelik, sesli, sıradan bir telefon videosu gibi duran bir videoya dönüştür.\n\n" +
            "Fotoğraf, bir çocuğun \"$thing\" karalamasının gerçek hâli ve kendine has, beceriksiz, tuhaf şekli işin asıl noktası. \"$thing\" fotoğraftaki hâliyle TAMAMEN AYNI kalmalı: aynı siluet, oranlar, renkler, malzemeler, yer ve ışık. " +
            "Yeniden tasarlama, düzeltme, güzelleştirme ya da \"iyileştirme\". Daha profesyonel ya da daha gerçekçi yapma. " +
            "Fotoğrafta olmayan göz, yüz, kol, bacak, kanat, pencere, süs ya da başka bir parça EKLEME, olanlardan da çıkarma. " +
            "Hâlâ gerçek olmuş bir çocuk çizimi gibi görünmeli.\n\n" +
            "Neler oluyor: " + story + ". " +
            "Nesne öylece durup sallanmasın: ona bir şey olsun; net bir başlangıç, bir komik an ve sonda küçük bir tepki olsun, hepsi 8 saniyeye sığsın. " +
            "Sahnede biri varsa sıradan, gerçek biri olsun; doğal ve telaşsız davransın, yandan, arkadan ya da kısmen görünsün; nesnenin şeklini asla değiştirmesin.\n\n" +
            "Kamera: elde telefon hissi, sabit ya da çok yavaş yaklaşan; kesme yok.\n\n" +
            "Ses: sesi de üret. Olanlara uyan gerçekçi sesler (nesnenin kendi sesleri, eller ve ayak sesleri, çevrenin sesi), gerçek bir kayıt gibi. Müzik yok, ekranda yazı yok. Sahnede açıkça yazılmadıkça kimse konuşmasın."
    }

    /** One drawing the assistant described: `index` is 1-based. */
    data class RevealPick(val index: Int, val wordEn: String, val describe: String, val motion: String)

    /**
     * The assistant's description of the chosen drawings:
     * `{"reveals":[{"index":1,"word_en":"…","describe":"…","motion":"…"}],"caption":"…","hashtags":"…"}`.
     */
    data class Picks(val reveals: List<RevealPick>, val caption: String, val hashtags: String)

    /** The opening line as Karalak says it: eagerly ("[excited]" in front, with v3). */
    fun hookLine(text: String): SpokenLine = SpokenLine(text.trim(), MascotEmotion.Excited)

    /** The closing line as Karalak says it: playfully, a reaction and a question. */
    fun outroLine(text: String): SpokenLine = SpokenLine(text.trim(), MascotEmotion.Point)

    fun spoken(kind: RealLine, text: String): SpokenLine = when (kind) {
        RealLine.Hook -> hookLine(text)
        RealLine.Outro -> outroLine(text)
    }

    /**
     * The assistant's ideas for the opening line: `{"hooks":["…",…]}`, cleaned (see [cleanHook]), the same one
     * only once, at most [HOOK_COUNT]. [seconds] are the opening drawing's.
     */
    fun parseHooks(text: String, seconds: Int): List<String> = parseLines(text, "hooks", seconds, HOOK_COUNT)

    /** The assistant's ideas for the closing line: `{"outros":["…",…]}`, cleaned the same way, at most [OUTRO_COUNT]. */
    fun parseOutros(text: String, seconds: Int): List<String> = parseLines(text, "outros", seconds, OUTRO_COUNT)

    private fun parseLines(text: String, key: String, seconds: Int, count: Int): List<String> {
        val o = GuessCopy.objectIn(text) ?: return emptyList()
        val raw = runCatching { o[key]!!.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull } }.getOrNull().orEmpty()
        return raw.map { cleanHook(it, seconds) }.filter { it.isNotEmpty() }.distinctBy { GuessCopy.normalize(it) }.take(count)
    }

    /**
     * One line as the video shows it: no quotes or list number around it, and any time it gives set to the
     * drawing's real [seconds] (0: not known, left as written), so the video never claims a time the game did not give.
     */
    fun cleanHook(text: String, seconds: Int): String {
        var s = text.trim().trim('"', '“', '”', '\'', '«', '»').trim().replace(NUMBERING, "").trim()
        if (seconds > 0) s = SECONDS.replace(s) { m -> "$seconds" + m.groupValues[2] }
        return s.take(HOOK_CHARS).trim()
    }

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
