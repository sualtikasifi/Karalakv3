package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.sualtikasifi.cizimhafiza.BuildConfig
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.data.local.WordSeeder
import com.sualtikasifi.cizimhafiza.domain.model.DrawingStroke
import com.sualtikasifi.cizimhafiza.domain.model.ResultItem
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

/**
 * Renders finished drawings (their vector strokes) into shareable branded
 * PNG cards and hands them off to the system share sheet — either one
 * drawing at a time, or the whole game's results as a single collage — so a
 * funny drawing (or a whole round) can be sent straight to a friend without
 * leaving the app.
 */
object DrawingShareUtil {

    private const val CARD_WIDTH = 1000
    private const val CANVAS_INSET = 60f
    private const val STROKE_WIDTH_RATIO = 0.011f

    private val backgroundColor = Color.rgb(0xFB, 0xF3, 0xE7)
    private val cardWhite = Color.WHITE
    private val outline = Color.rgb(0xE8, 0xDC, 0xC9)
    private val penColor = Color.rgb(0x1E, 0x1B, 0x18)
    private val textDark = Color.rgb(0x2B, 0x21, 0x18)
    private val textMuted = Color.rgb(0x8A, 0x7F, 0x72)
    private val brandOrange = Color.rgb(0xF9, 0x73, 0x16)
    private val correctGreen = Color.rgb(0x3F, 0xA3, 0x4D)
    private val wrongRed = Color.rgb(0xE0, 0x52, 0x3F)

    fun shareDrawing(context: Context, word: String, strokes: List<DrawingStroke>) {
        val language = WordSeeder.currentLanguage(context)
        val bitmap = renderSingleCard(word, strokes, language)
        shareBitmap(context, bitmap, "karalak")
    }

    fun shareAllResults(
        context: Context,
        totalScore: Int,
        correctCount: Int,
        wrongCount: Int,
        fastestCorrectSeconds: Double?,
        items: List<ResultItem>
    ) {
        val language = WordSeeder.currentLanguage(context)
        val bitmap = renderResultsCard(context, items)
        shareBitmap(context, bitmap, "karalak_sonuc")
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, fileNamePrefix: String) {
        val file = writeToCache(context, bitmap, fileNamePrefix)
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

    /** The picture to share: the drawing, its word and the Karalak mark on the branded template card. */
    fun shareDrawingOnTemplate(context: Context, word: String, strokes: List<DrawingStroke>) {
        val language = WordSeeder.currentLanguage(context)
        val template = ShareTemplate.load(context)
        val bitmap = template.copy(Bitmap.Config.ARGB_8888, true)
        template.recycle()
        val canvas = Canvas(bitmap)
        drawStrokes(canvas, strokes, ShareTemplate.drawingRect, paddingRatio = 0.04f)
        ShareTemplate.drawWordAndCaption(context, canvas, ShareTemplate.maskedWord(word))
        shareBitmap(context, bitmap, "karalak")
    }

    private fun renderSingleCard(word: String, strokes: List<DrawingStroke>, language: String): Bitmap {
        val height = CARD_WIDTH + 260
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val canvasRect = RectF(CANVAS_INSET, CANVAS_INSET, CARD_WIDTH - CANVAS_INSET, CARD_WIDTH - CANVAS_INSET)
        drawRoundedCard(canvas, canvasRect)
        drawStrokes(canvas, strokes, canvasRect)

        val wordPaint = Paint().apply {
            color = textDark
            textSize = 56f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(word.capitalizeForWordLanguage(language), CARD_WIDTH / 2f, CARD_WIDTH + 90f, wordPaint)

        drawBrandFooter(canvas, CARD_WIDTH / 2f, CARD_WIDTH + 190f, textSize = 42f)

        return bitmap
    }

    /**
     * The whole round on the "Bu çizimleri sen de tahmin edebilir misin?" template: every drawing in a grid
     * inside the big white card, each with only the first letter of its word under it. The grid picks the
     * column count that makes the drawings biggest for however many there are.
     */
    private fun renderResultsCard(context: Context, items: List<ResultItem>): Bitmap {
        val template = ShareTemplate.loadCollage(context)
        val bitmap = template.copy(Bitmap.Config.ARGB_8888, true)
        template.recycle()
        val canvas = Canvas(bitmap)

        val area = ShareTemplate.collageRect
        val gap = 18f
        val captionHeight = 40f
        val count = items.size.coerceAtLeast(1)
        var bestCols = 1
        var bestSide = 0f
        for (cols in 1..count) {
            val rows = ceil(count / cols.toFloat()).toInt()
            val side = minOf(
                (area.width() - gap * (cols - 1)) / cols,
                (area.height() - gap * (rows - 1)) / rows - captionHeight
            )
            if (side > bestSide) {
                bestSide = side
                bestCols = cols
            }
        }
        val side = bestSide.coerceAtLeast(40f)
        val rows = ceil(count / bestCols.toFloat()).toInt()
        val gridHeight = rows * (side + captionHeight) + (rows - 1) * gap
        val top = area.top + (area.height() - gridHeight) / 2f

        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = (side * 0.13f).coerceIn(20f, 32f)
        }
        items.forEachIndexed { index, item ->
            val row = index / bestCols
            val inRow = minOf(bestCols, items.size - row * bestCols)
            val rowWidth = inRow * side + (inRow - 1) * gap
            val left = area.left + (area.width() - rowWidth) / 2f + (index % bestCols) * (side + gap)
            val cellTop = top + row * (side + captionHeight + gap)
            val rect = RectF(left, cellTop, left + side, cellTop + side)
            drawRoundedCard(canvas, rect, cornerRadius = 20f)
            drawStrokes(canvas, item.strokes, rect, paddingRatio = 0.1f)
            canvas.drawText(
                ShareTemplate.maskedWord(item.word),
                rect.centerX(),
                rect.bottom + captionHeight * 0.75f,
                captionPaint
            )
        }
        return bitmap
    }

    private fun drawBrandFooter(canvas: Canvas, centerX: Float, y: Float, textSize: Float) {
        val brandPaint = Paint().apply {
            color = brandOrange
            this.textSize = textSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Karalak", centerX, y, brandPaint)
    }

    private fun drawRoundedCard(canvas: Canvas, rect: RectF, cornerRadius: Float = 32f) {
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, Paint().apply {
            color = cardWhite
            isAntiAlias = true
        })
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, Paint().apply {
            color = outline
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        })
    }

    private fun drawBadge(canvas: Canvas, cx: Float, cy: Float, radius: Float, isCorrect: Boolean) {
        canvas.drawCircle(cx, cy, radius, Paint().apply {
            color = if (isCorrect) correctGreen else wrongRed
            isAntiAlias = true
        })
        val strokePaint = Paint().apply {
            color = cardWhite
            style = Paint.Style.STROKE
            strokeWidth = radius * 0.26f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        if (isCorrect) {
            val path = Path().apply {
                moveTo(cx - radius * 0.45f, cy)
                lineTo(cx - radius * 0.1f, cy + radius * 0.35f)
                lineTo(cx + radius * 0.5f, cy - radius * 0.35f)
            }
            canvas.drawPath(path, strokePaint)
        } else {
            canvas.drawLine(cx - radius * 0.4f, cy - radius * 0.4f, cx + radius * 0.4f, cy + radius * 0.4f, strokePaint)
            canvas.drawLine(cx + radius * 0.4f, cy - radius * 0.4f, cx - radius * 0.4f, cy + radius * 0.4f, strokePaint)
        }
    }

    private fun drawStrokes(canvas: Canvas, strokes: List<DrawingStroke>, targetRect: RectF, paddingRatio: Float = 0.08f) {
        val allPoints = strokes.asSequence().flatten()
        val minX = allPoints.minOfOrNull { it.x } ?: return
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOfOrNull { it.y } ?: return
        val maxY = allPoints.maxOf { it.y }
        val contentWidth = (maxX - minX).coerceAtLeast(1f)
        val contentHeight = (maxY - minY).coerceAtLeast(1f)

        val padding = minOf(targetRect.width(), targetRect.height()) * paddingRatio
        val availableWidth = (targetRect.width() - padding * 2).coerceAtLeast(1f)
        val availableHeight = (targetRect.height() - padding * 2).coerceAtLeast(1f)
        val scale = minOf(availableWidth / contentWidth, availableHeight / contentHeight)
        val offsetX = targetRect.left + (targetRect.width() - contentWidth * scale) / 2f
        val offsetY = targetRect.top + (targetRect.height() - contentHeight * scale) / 2f

        val paint = Paint().apply {
            color = penColor
            style = Paint.Style.STROKE
            strokeWidth = minOf(targetRect.width(), targetRect.height()) * STROKE_WIDTH_RATIO
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val dotPaint = Paint().apply {
            color = penColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        strokes.forEach { stroke ->
            if (stroke.isEmpty()) return@forEach
            // A stationary tap (e.g. a quick reminder dot) is captured as a
            // single-point "stroke" (see StrokeCanvas.DrawableCanvas) —
            // render it as a dot instead of skipping it, so the shared
            // image matches what was actually drawn in-app.
            if (stroke.size == 1) {
                val p = stroke.first()
                canvas.drawCircle(
                    offsetX + (p.x - minX) * scale,
                    offsetY + (p.y - minY) * scale,
                    paint.strokeWidth / 2f,
                    dotPaint
                )
                return@forEach
            }
            val path = Path()
            val first = stroke.first()
            path.moveTo(offsetX + (first.x - minX) * scale, offsetY + (first.y - minY) * scale)
            stroke.drop(1).forEach {
                path.lineTo(offsetX + (it.x - minX) * scale, offsetY + (it.y - minY) * scale)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun writeToCache(context: Context, bitmap: Bitmap, fileNamePrefix: String): File {
        val dir = File(context.cacheDir, "shared_drawings").apply { mkdirs() }
        val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
