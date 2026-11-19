package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

/**
 * "Bil Bakalım Story": the picture for an Instagram story (the drawing on a painted board; the title and the call to get the app are part of the template) and the question
 * with its four answers, which the maker puts into an Instagram quiz sticker by hand.
 */
object RiddleStory {
    const val W = 1080
    const val H = 1920
    const val LETTERS = "ABCD"

    /** What the assistant wrote: the question, the right answer and three wrong ones that look like it. */
    class Written(val question: String, val answer: String, val others: List<String>)

    fun prompt(word: String, category: String): String = """
        Karalak, çizim tahmin oyunudur. Ekteki resim bir oyuncunun çizimi; çizenin yazdığı kelime: "$word"${if (category.isNotBlank()) " (kategori: $category)" else ""}.
        ÖNCE RESME BAK. Kelimenin birden çok anlamı olabilir (ör. "fare": hayvan mı, bilgisayar faresi mi; "yüzük", "kalem", "ay"…): resimde ne çizilmişse o anlamı kullan, resimle uyuşmayan anlamı kullanma.
        Bu çizim Instagram story'de 4 şıklı bir soru olacak. Yaz:
        1. "question": çizim hakkında kısa, eğlenceli bir soru (en çok 40 karakter, sonu soru işaretiyle). Kelimeyi söyleme. Her seferinde farklı bir kalıp seç ("Bu çizim ne?", "Sence ne çizilmiş?", "Bu ne olabilir?", "Çizen ne yapmaya çalışmış?"…).
        2. "answer": doğru cevap, yani "$word"; ilk harfi büyük, en çok 18 karakter.
        3. "others": resimdeki şeyle karıştırılabilecek 3 FARKLI kelime: resimdeki şeyin AYNI TÜRDEN (hayvansa hayvan, eşyaysa eşya, bilgisayar parçasıysa bilgisayar parçası) ve biçim olarak ona benzeyen şeyler olsun. Hepsi bir ya da iki kelime, her biri en çok 18 karakter. Doğru cevapla aynı olmasın, birbirine benzemesin, anlamsız ya da saçma olmasın; resimle hiç ilgisi olmayan bir türden şık yazma.
        Yalnızca şu JSON'u yaz: {"question":"…","answer":"…","others":["…","…","…"]}
    """.trimIndent()

    const val SYSTEM = "Sen Karalak oyununun sosyal medya yazarısın. Kısa, net, Türkçe yazarsın. Yalnızca JSON döndürürsün."

    /** Reads the assistant's JSON; null when it is not usable (no question, or fewer than three different wrong answers). */
    fun parse(raw: String, answerWord: String): Written? = runCatching {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val o = Json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
        val q = o["question"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().take(60)
        val a = o["answer"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().take(24).ifBlank { answerWord }
        val others = o["others"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.take(24) }.orEmpty()
            .filter { it.isNotBlank() && !it.equals(a, ignoreCase = true) }
            .distinctBy { it.lowercase() }
        if (q.isBlank() || others.size < 3) null else Written(q, a, others.take(3))
    }.getOrNull()

    /**
     * Puts the answer among the three others on a place of its own choosing: any of the four except the one used last time, so
     * the right letter does not stay the same from story to story. [last] is -1 when there is no previous one.
     * Returns the four answers in order A..D and the index of the right one.
     */
    fun arrange(answer: String, others: List<String>, last: Int, random: Random = Random.Default): Pair<List<String>, Int> {
        require(others.size == 3) { "three wrong answers" }
        val places = (0..3).filter { it != last }
        val at = places[random.nextInt(places.size)]
        val shuffled = others.shuffled(random)
        val out = ArrayList<String>(4)
        var k = 0
        for (i in 0..3) out.add(if (i == at) answer else shuffled[k++])
        return out to at
    }

    /** The question and answers as one text to paste: the right one is marked. */
    fun asText(question: String, options: List<String>, correct: Int): String = buildString {
        append(question).append('\n')
        options.forEachIndexed { i, o -> append(LETTERS[i]).append(") ").append(o).append(if (i == correct) "   ✓" else "").append('\n') }
    }.trim()

    // ---- the picture

    /**
     * The paper board's inside, in story-picture pixels: the template (1080 x 1920, the picture's own size) was measured by filling the
     * paper from its middle: it spans x 194..876, y 306..915, so its centre is (535, 610.5). This rectangle has that same centre, with
     * room left to the frame and its rounded corners; the drawing is centred on it.
     */
    private val PAPER = RectF(224f, 335.5f, 846f, 885.5f)

    fun render(context: Context, assets: StudioAssets, strokes: List<DrawingStroke>): Bitmap {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val template = BitmapFactory.decodeResource(context.resources, R.drawable.story_bilbakalim)
        if (template != null) {
            // 1 : 1, the template is the story's size
            c.drawBitmap(template, 0f, 0f, null)
            template.recycle()
        } else c.drawColor(Color.rgb(0xF4, 0xD4, 0x9A))
        drawStrokes(c, strokes, PAPER)
        return out
    }

    /** The drawing alone, black on white, as PNG bytes: what the assistant is shown so that it reads the right meaning of the word. */
    fun drawingPng(strokes: List<DrawingStroke>, size: Int = 768): ByteArray {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        drawStrokes(c, strokes, RectF(0f, 0f, size.toFloat(), size.toFloat()))
        val out = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return out.toByteArray()
    }

    private fun drawStrokes(c: Canvas, strokes: List<DrawingStroke>, target: RectF) {
        val all = strokes.asSequence().flatten()
        val minX = all.minOfOrNull { it.x } ?: return
        val maxX = all.maxOf { it.x }
        val minY = all.minOfOrNull { it.y } ?: return
        val maxY = all.maxOf { it.y }
        val cw = (maxX - minX).coerceAtLeast(1f)
        val ch = (maxY - minY).coerceAtLeast(1f)
        val pad = minOf(target.width(), target.height()) * 0.07f
        val scale = minOf((target.width() - pad * 2) / cw, (target.height() - pad * 2) / ch)
        val ox = target.left + (target.width() - cw * scale) / 2f
        val oy = target.top + (target.height() - ch * scale) / 2f
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x1E, 0x1B, 0x18); style = Paint.Style.STROKE
            strokeWidth = minOf(target.width(), target.height()) * 0.013f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.color; style = Paint.Style.FILL }
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            if (stroke.size == 1) {
                val p = stroke.first()
                c.drawCircle(ox + (p.x - minX) * scale, oy + (p.y - minY) * scale, ink.strokeWidth / 2f, dot)
                continue
            }
            val path = Path()
            val f = stroke.first()
            path.moveTo(ox + (f.x - minX) * scale, oy + (f.y - minY) * scale)
            for (i in 1 until stroke.size) path.lineTo(ox + (stroke[i].x - minX) * scale, oy + (stroke[i].y - minY) * scale)
            c.drawPath(path, ink)
        }
    }
}
