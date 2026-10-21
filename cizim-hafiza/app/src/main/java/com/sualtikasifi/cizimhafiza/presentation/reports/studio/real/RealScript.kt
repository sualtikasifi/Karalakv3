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
