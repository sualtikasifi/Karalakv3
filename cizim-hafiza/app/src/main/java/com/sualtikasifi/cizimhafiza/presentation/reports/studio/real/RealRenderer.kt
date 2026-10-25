package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import com.sualtikasifi.cizimhafiza.R
import com.sualtikasifi.cizimhafiza.domain.model.AvatarFrame
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.ClipSound
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Ease
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import java.io.File
import kotlin.math.hypot
import kotlin.math.min

/**
 * Draws a "Gerçek Olsaydı" video in 1080×1920: one drawing's, the project's featured one. It opens on the game's own
 * drawing screen: the drawing is drawn again on its canvas while the timer runs down, its word on the notepad and the
 * game's jokers on the board under the canvas, while Karalak says the hook; then the clip opens out of the drawing. The clip plays
 * full frame with its own sound and, in the top left, the Karalak card (the game's wooden frame with its logo): the
 * player's drawing in the middle of it, the player's frame with the default picture in the round slot at the bottom
 * and their name (or "anonymous") beside it. Its size and place are the [layout] set in the panel. When it has a
 * closing line, the clip's last frame is then held under it. Karalak's voice says the hook and the closing when they
 * were voiced, never over the clip; the app's music, when chosen, plays under it all. The right edge and the bottom
 * band stay clear for the buttons and caption TikTok, Reels and Shorts put there.
 */
class RealRenderer(
    private val assets: StudioAssets,
    private val project: RealProject,
    /** Clips by drawing id. */
    private val clips: Map<String, ClipFrames>,
    private val clipFiles: Map<String, File>,
    /** How loud the clips' own sound is. */
    private val clipGain: Float,
    layout: CardLayout = CardLayout(),
    /** Pictures picked by hand for the round slot, by drawing id; any drawing without one gets the default picture. */
    private val photoFiles: Map<String, File> = emptyMap(),
    /** The featured reveal's lines said aloud: the hook's voice, laid at the start of the opening, and the closing's, at its start. */
    hookVoiceFile: File? = null,
    outroVoiceFile: File? = null,
    /** Its background music ([RealReveal.music]), under the whole video. */
    musicFile: File? = null
) : VoicedVideo {
    /** The video is one drawing's: the project's featured one. */
    private val featured = project.featured
    private val shown = listOfNotNull(featured)
    val events: List<RealEvent> = RealTimeline.build(featured)
    override val totalMs: Long = RealTimeline.totalMs(events)
    override val voiceStarts: List<Pair<Int, Long>> = emptyList()
    override val sounds: List<ClipSound> = buildList {
        val f = featured ?: return@buildList
        // where Karalak speaks, for the music to step back
        val voices = mutableListOf<LongRange>()
        var clipRange: LongRange? = null
        events.forEach { e ->
            when (e.phase) {
                RealPhase.Hook -> if (hookVoiceFile != null && f.hook.voice != null) {
                    add(ClipSound(hookVoiceFile, e.startMs + RealTimeline.VOICE_AT, f.hook.voiceMs, 1f))
                    voices += (e.startMs + RealTimeline.VOICE_AT).let { it..it + f.hook.voiceMs }
                }
                RealPhase.Clip -> {
                    clipRange = e.clipAtMs until e.clipAtMs + e.clipMs
                    if (clipGain > 0f) clipFiles[f.sampleId]?.let { add(ClipSound(it, e.clipAtMs, e.clipMs, clipGain)) }
                }
                RealPhase.Outro -> if (outroVoiceFile != null && f.outro.voice != null) {
                    add(ClipSound(outroVoiceFile, e.startMs + RealTimeline.VOICE_AT, f.outro.voiceMs, 1f))
                    voices += (e.startMs + RealTimeline.VOICE_AT).let { it..it + f.outro.voiceMs }
                }
            }
        }
        if (musicFile != null && f.music != null && f.musicVolume > 0f) {
            val total = RealTimeline.totalMs(events)
            val clip = clipRange
            add(ClipSound(musicFile, 0L, total, f.musicVolume.coerceAtMost(RealMix.MAX_VOLUME)) { ms -> RealMix.gainAt(ms, total, voices, clip) })
        }
    }

    private val text = TextPainter()
    /** For pictures only: its colour is never set, so a picture is always drawn at full strength. */
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val ink = StrokeReplay.inkPaint(0xFF1B1B22.toInt())
    private val replays = HashMap<String, StrokeReplay>()
    private val src = Rect()
    private val card = assets.picture(R.drawable.real_card)
    private val dino = assets.picture(R.drawable.avatar_dino)
    private val scene: Bitmap? = if (events.firstOrNull()?.phase == RealPhase.Hook) assets.picture(R.drawable.bg_draw) else null
    private val sceneInk = StrokeReplay.inkPaint(0xFF1B1B22.toInt())
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val plain = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = C.WHITE }
    private val circle = Path()
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeJoin = Paint.Join.ROUND }
    /** The guessing screen's scene, for its painted joker tiles. */
    private val guessScene = if (scene != null) assets.picture(R.drawable.bg_guess) else null

    // the card is 941 x 1672 (real_card): everything below is placed in its own pixels, then scaled
    private val lay = layout.clamped()
    private val cardW = CARD_W * lay.scale
    private val cardH = cardW * 1672f / 941f
    private val cardBox = RectF(
        lay.x * (W - cardW), lay.y * (H - cardH),
        lay.x * (W - cardW) + cardW, lay.y * (H - cardH) + cardH
    )
    private val k = cardW / 941f
    /** The parchment in the middle of the card, where the drawing goes. */
    private val parchment = RectF(122f, 435f, 816f, 1322f).let { r -> RectF(x(r.left), y(r.top), x(r.right), y(r.bottom)) }
    /** The round slot at the bottom left (centre and diameter), for the frame and picture. */
    private val slotCx = x(199f)
    private val slotCy = y(1421f)
    private val slotD = 168f * k
    /** The dark plate beside it, for the name. */
    private val plate = RectF(305f, 1359f, 834f, 1488f).let { r -> RectF(x(r.left), y(r.top), x(r.right), y(r.bottom)) }

    private fun x(tx: Float) = cardBox.left + tx * k
    private fun y(ty: Float) = cardBox.top + ty * k

    /** The frame picked by hand (any frame, whatever the level); none picked: the one the player's level gives. */
    private fun frameOf(d: RealDrawing): AvatarFrame =
        AvatarFrame.entries.find { it.name == d.frameId } ?: AvatarFrame.resolve(d.frameId, d.level.coerceAtLeast(1))

    private val handPhotos = HashMap<String, Bitmap?>()

    /** The picture picked by hand, read once; otherwise the default one. */
    private fun photoOf(d: RealDrawing): Bitmap? = (d.photo?.let { photoFiles[d.id] }?.let { f -> handPhotos.getOrPut(d.id) { RealImages.load(f, maxSide = 512) } }) ?: dino

    private fun replay(id: String): StrokeReplay? = project.drawing(id)?.let { d -> replays.getOrPut(id) { StrokeReplay(d.strokes) } }

    override fun draw(canvas: Canvas, t: Long) {
        val e = RealTimeline.at(events, t)
        val reveal = shown.getOrNull(e.index) ?: return
        if (e.phase == RealPhase.Hook) { drawHook(canvas, e, reveal, t); return }
        if (e.phase == RealPhase.Outro) { drawOutro(canvas, e, reveal, t); return }
        canvas.drawColor(C.INK)
        val frame = clips[reveal.sampleId]?.frameAt((t - e.clipAtMs).coerceIn(0L, e.clipMs))
        if (frame != null) cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        val drawing = project.drawing(reveal.sampleId) ?: return
        val local = t - e.startMs
        drawCard(canvas, drawing, Ease.backOut(min(1f, local / 450f)))
    }

    /** The Karalak card with the player's drawing, frame, picture and name on it; it pops in from its top left corner. */
    private fun drawCard(canvas: Canvas, d: RealDrawing, a: Float) {
        val bmp = card ?: return
        canvas.save()
        canvas.scale(a, a, cardBox.left, cardBox.top)
        canvas.drawBitmap(bmp, null, cardBox, fill)
        val m = 8f * lay.scale
        replay(d.id)?.draw(canvas, RectF(parchment.left + m, parchment.top + m, parchment.right - m, parchment.bottom - m), 1f, ink, 0.03f)
        drawAvatar(canvas, d)
        val name = if (d.named) "@${d.nickname}" else "Anonim oyuncu"
        text.draw(canvas, name, plate.centerX(), plate.centerY(), (plate.width() - 28f * lay.scale).toInt(), 38f * lay.scale, 1, assets.display, C.WHITE)
        canvas.restore()
    }

    /** The player's frame with the default picture in it, in the card's round slot: never their own photo. */
    private fun drawAvatar(canvas: Canvas, d: RealDrawing) {
        val frame = frameOf(d)
        val frameBmp = assets.picture(frame.drawableRes)
        val faceD = slotD * 0.94f
        val size = (faceD / (frame.faceDiameterFraction * 1.06f)).coerceAtMost(slotD * 1.5f)
        val fx = slotCx + size * frame.faceOffsetXFraction
        val fy = slotCy + size * frame.faceOffsetYFraction
        val photo = photoOf(d)
        if (photo != null) {
            val shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val s = faceD / min(photo.width, photo.height)
            shader.setLocalMatrix(Matrix().apply {
                setScale(s, s)
                postTranslate(fx - photo.width * s / 2f, fy - photo.height * s / 2f)
            })
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawCircle(fx, fy, faceD / 2f, paint)
        }
        frameBmp?.let { canvas.drawBitmap(it, null, RectF(slotCx - size / 2f, slotCy - size / 2f, slotCx + size / 2f, slotCy + size / 2f), fill) }
    }

    /**
     * The opening: the game's drawing screen (bg_draw, its top and bottom kept and the plain middle squeezed, as the
     * game does on a short screen), the player's name in the header, the word on the notepad, the timer ring running
     * down while the drawing is drawn again on the canvas, and the game's jokers on the board under it. It is all there
     * from the very first frame, which is also the video's cover. At the end the clip opens out of the drawing in a
     * growing circle.
     */
    private fun drawHook(canvas: Canvas, e: RealEvent, reveal: RealReveal, t: Long) {
        val d = project.drawing(reveal.sampleId) ?: return
        val local = t - e.startMs
        canvas.drawColor(C.CREAM)
        scene?.let { drawScene(canvas, it) }

        // header: who drew it, in the pill beside the back button
        val name = if (d.named) "@${d.nickname}" else "Anonim oyuncu"
        text.draw(canvas, name, art(152f + 148f), art(88f + 41f), art(296f - 36f).toInt(), 44f, 1, assets.display, SCENE_INK)

        // the word on the notepad, sized as the game sizes it (short words large, long ones smaller), with its orange underline
        val word = d.word.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("tr")) else it.toString() }
        val wordSize = (art(425f) * 0.97f / (word.length.coerceAtLeast(1) * 0.5f)).coerceIn(WORD_MIN, WORD_MAX)
        text.draw(canvas, word, art(125f + 222.5f), art(232f + 56f), art(445f - 20f).toInt(), wordSize, 2, assets.display, SCENE_INK)
        plain.color = UNDERLINE
        canvas.drawRoundRect(RectF(art(235f), art(343f), art(485f), art(351f)), art(4f), art(4f), plain)

        // the drawing, drawn again in the time the opening leaves before the clip
        val drawEnd = (e.durationMs - RealTimeline.HOOK_REVEAL - RealTimeline.HOOK_HOLD).coerceAtLeast(1L)
        val drawn = (local.toFloat() / drawEnd).coerceIn(0f, 1f)
        replay(d.id)?.draw(canvas, hookBox, drawn, sceneInk, 0.02f)

        // the timer ring, running down with the drawing
        if (d.seconds > 0) drawTimer(canvas, d.seconds, drawn)

        // the board under the canvas: the game's jokers, as the app shows them (the hook is only ever said, never written)
        drawJokers(canvas, local)

        // the first clip opens out of the drawing
        val openFrom = e.durationMs - RealTimeline.HOOK_REVEAL
        if (local >= openFrom) {
            val frame = clips[reveal.sampleId]?.frameAt(0L) ?: return
            val p = Ease.out(((local - openFrom).toFloat() / RealTimeline.HOOK_REVEAL).coerceIn(0f, 1f))
            val cx = hookBox.centerX()
            val cy = hookBox.centerY()
            val r = p * hypot(maxOf(cx, W - cx), maxOf(cy, H - cy))
            circle.reset()
            circle.addCircle(cx, cy, r, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(circle)
            cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
            canvas.restore()
            edge.strokeWidth = 10f * (1f - p)
            if (edge.strokeWidth > 0.5f) canvas.drawCircle(cx, cy, r, edge)
        }
    }

    /**
     * The closing: the clip's last frame held, drifting slowly closer so it does not look stuck, the card still on it,
     * and Karalak's reaction as a white subtitle above the band the apps cover.
     */
    private fun drawOutro(canvas: Canvas, e: RealEvent, reveal: RealReveal, t: Long) {
        canvas.drawColor(C.INK)
        val frame = clips[reveal.sampleId]?.frameAt((e.clipMs - LAST_FRAME_BACK).coerceAtLeast(0L))
        if (frame != null) {
            val z = 1f + 0.04f * Ease.out(e.progress(t))
            canvas.save()
            canvas.scale(z, z, W / 2f, H / 2f)
            cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
            canvas.restore()
        }
        project.drawing(reveal.sampleId)?.let { drawCard(canvas, it, 1f) }

        val line = reveal.outro.text.trim()
        val (w, h) = text.measure(line, SUB_W, SUB_SIZE, 2, assets.display, C.INK, spacing = 0.8f, balanced = true)
        if (h <= 0f) return
        val pop = 0.86f + 0.14f * Ease.backOut(min(1f, (t - e.startMs) / 260f))
        canvas.save()
        canvas.scale(pop, pop, W / 2f, SUB_CY)
        val box = RectF(W / 2f - w / 2f - 38f, SUB_CY - h / 2f - 24f, W / 2f + w / 2f + 38f, SUB_CY + h / 2f + 24f)
        plain.color = C.WHITE
        plain.alpha = 240
        canvas.drawRoundRect(box, 32f, 32f, plain)
        plain.alpha = 255
        text.draw(canvas, line, W / 2f, SUB_CY, SUB_W, SUB_SIZE, 2, assets.display, C.INK, spacing = 0.8f, balanced = true)
        canvas.restore()
    }

    /**
     * The guessing screen's joker row, as the game has it, on the tool board: its four painted tiles cut out of bg_guess
     * (İpucu, İlk harf, Harf sayısı, Atla) with what GuessScreen's JokerTile lays on each — the hint's play mark, the
     * two jokers' pictures in their gold frame over a soft pulsing glow, gently floating, with how many are left in a
     * white badge, and Atla's skip mark — at the game's own sizes, the whole row scaled to fit the board.
     */
    private fun drawJokers(canvas: Canvas, t: Long) {
        val bmp = guessScene ?: return
        val k = bmp.width / ART_W
        val s = ROW_SCALE
        val left = W / 2f - (ROW_RIGHT - ROW_LEFT) * k * s / 2f
        val top = boardCy - TILE_H * k * s / 2f
        // one art unit of bg_guess on the video, and one of the game's dp (the scene is 841 art units on a 411 dp phone)
        val u = k * s
        val dp = u * ART_W / 411f
        GUESS_TILES.forEach { tile ->
            val box = RectF(left + (tile.x - ROW_LEFT) * u, top, left + (tile.x + tile.w - ROW_LEFT) * u, top + TILE_H * u)
            // the painted tile, cut out along its rounded edge
            circle.reset()
            circle.addRoundRect(box, box.height() * 0.17f, box.height() * 0.17f, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(circle)
            src.set(((tile.x - 2f) * k).toInt(), ((TILE_TOP - 2f) * k).toInt(), ((tile.x + tile.w + 2f) * k).toInt(), ((TILE_TOP + TILE_H + 2f) * k).toInt())
            canvas.drawBitmap(bmp, src, RectF(box.left - 2f * u, box.top - 2f * u, box.right + 2f * u, box.bottom + 2f * u), fill)
            canvas.restore()

            val icon = 66f * u
            val labelSize = 12f * dp
            val joker = tile.art != 0
            // the column: icon, 2 dp, label; the jokers sit a little higher over their tile's thick lip
            val labelH = labelSize * 1.1f
            val plate = if (joker) icon + 2f * 3f * dp else icon
            val colH = plate + 2f * dp + labelH
            val colTop = box.top + (box.height() - (if (joker) 10f * dp else 0f) - colH) / 2f
            val cx = box.centerX()
            val iconCy = colTop + plate / 2f
            if (joker) {
                drawJokerArt(canvas, tile.art, cx, iconCy, icon, dp, t)
            } else if (tile.skip) {
                drawSkip(canvas, cx, iconCy, icon, tile.labelColor)
            } else {
                drawPlay(canvas, cx, iconCy, icon, HINT_ICON)
            }
            val labelCy = colTop + plate + 2f * dp + labelH / 2f
            lettered(canvas, tile.label, cx, labelCy, labelSize, tile.labelColor, tile.outline, box.width() - 8f * dp, dp)
            if (tile.badge > 0) {
                val d = box.height() * 0.245f
                val bcx = box.right - box.width() * 0.03f - d / 2f
                val bcy = box.top + box.height() * 0.04f + d / 2f
                plain.color = C.WHITE
                canvas.drawCircle(bcx, bcy, d / 2f, plain)
                ring.strokeWidth = 1.5f * dp
                ring.color = tile.badgeColor
                canvas.drawCircle(bcx, bcy, d / 2f - ring.strokeWidth / 2f, ring)
                text.draw(canvas, tile.badge.toString(), bcx, bcy, d.toInt(), 13f * dp, 1, assets.display, tile.badgeColor)
            }
        }
    }

    /** A joker's picture as the tile shows it: over a soft pulsing glow, on a faint light plate in a cream-and-gold frame, floating. */
    private fun drawJokerArt(canvas: Canvas, art: Int, cx: Float, cy0: Float, size: Float, dp: Float, t: Long) {
        val cy = cy0 + kotlin.math.sin(t * 2.0 * Math.PI / FLOAT_MS).toFloat() * 2f * dp
        val pad = 3f * dp
        val outer = size + pad * 2f
        // the glow pulses between 0.28 and 0.62 over 1.7 s, there and back
        val phase = (t % (GLOW_MS * 2)).toFloat() / GLOW_MS
        val glow = 0.28f + 0.34f * Ease.out(if (phase <= 1f) phase else 2f - phase)
        val r = outer * 0.75f
        plain.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(withAlpha(GLOW, glow), withAlpha(GLOW, glow * 0.35f), 0x00FFF1B8),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, plain)
        plain.shader = null
        val plateBox = RectF(cx - outer / 2f, cy - outer / 2f, cx + outer / 2f, cy + outer / 2f)
        plain.color = 0x40FFFFFF
        canvas.drawRoundRect(plateBox, 11f * dp, 11f * dp, plain)
        ring.strokeWidth = 2f * dp
        ring.shader = LinearGradient(0f, plateBox.top, 0f, plateBox.bottom, FRAME_TOP, FRAME_BOTTOM, Shader.TileMode.CLAMP)
        val inset = ring.strokeWidth / 2f
        canvas.drawRoundRect(RectF(plateBox.left + inset, plateBox.top + inset, plateBox.right - inset, plateBox.bottom - inset), 11f * dp, 11f * dp, ring)
        ring.shader = null
        // as JokerArt: rounded, the picture a little larger than its frame
        val bmp = assets.picture(art) ?: return
        val artBox = RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        circle.reset()
        circle.addRoundRect(artBox, size * 0.24f, size * 0.24f, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(circle)
        val grow = size * 0.11f
        canvas.drawBitmap(bmp, null, RectF(artBox.left - grow, artBox.top - grow, artBox.right + grow, artBox.bottom + grow), fill)
        canvas.restore()
    }

    /** Material's play-circle (the hint is a rewarded ad): a filled circle with the triangle cut out of it. */
    private fun drawPlay(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val q = size / 24f
        val x0 = cx - size / 2f
        val y0 = cy - size / 2f
        circle.reset()
        circle.fillType = Path.FillType.EVEN_ODD
        circle.addCircle(cx, cy, 10f * q, Path.Direction.CW)
        circle.moveTo(x0 + 10f * q, y0 + 16.5f * q)
        circle.lineTo(x0 + 16f * q, y0 + 12f * q)
        circle.lineTo(x0 + 10f * q, y0 + 7.5f * q)
        circle.close()
        plain.color = color
        canvas.drawPath(circle, plain)
        circle.fillType = Path.FillType.WINDING
    }

    /** Material's skip-next: a triangle and a bar. */
    private fun drawSkip(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val q = size / 24f
        val x0 = cx - size / 2f
        val y0 = cy - size / 2f
        circle.reset()
        circle.moveTo(x0 + 6f * q, y0 + 18f * q)
        circle.lineTo(x0 + 14.5f * q, y0 + 12f * q)
        circle.lineTo(x0 + 6f * q, y0 + 6f * q)
        circle.close()
        circle.addRect(x0 + 16f * q, y0 + 6f * q, x0 + 18f * q, y0 + 18f * q, Path.Direction.CW)
        plain.color = color
        canvas.drawPath(circle, plain)
    }

    /**
     * A tile's label as the game letters it, centred on ([cx], [cy]): with an [outline], white over that edge with a
     * soft shadow (LetteredText); without, plain in [color]. It shrinks to fit [maxW].
     */
    private fun lettered(canvas: Canvas, line: String, cx: Float, cy: Float, size: Float, color: Int, outline: Int?, maxW: Float, dp: Float) {
        label.typeface = assets.display
        label.textAlign = Paint.Align.CENTER
        label.textSize = size
        while (label.measureText(line) > maxW && label.textSize > size * 0.55f) label.textSize *= 0.94f
        val fm = label.fontMetrics
        val y = cy - (fm.ascent + fm.descent) / 2f
        if (outline != null) {
            label.style = Paint.Style.STROKE
            label.strokeWidth = (size / dp * 0.11f).coerceIn(1.3f, 4.2f) * dp * (label.textSize / size)
            label.color = outline
            label.setShadowLayer(3f * dp, 0f, 2f * dp, 0x66000000)
            canvas.drawText(line, cx, y, label)
            label.clearShadowLayer()
            label.style = Paint.Style.FILL
            label.color = C.WHITE
        } else {
            label.style = Paint.Style.FILL
            label.color = color
        }
        canvas.drawText(line, cx, y, label)
    }

    private fun withAlpha(color: Int, a: Float): Int = (color and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)

    /** bg_draw at the video's width: its top and bottom as painted, its plain middle squeezed into what is left. */
    private fun drawScene(canvas: Canvas, bmp: Bitmap) {
        val k = W.toFloat() / bmp.width
        val topDst = SCENE_TOP * k
        val bottomDst = SCENE_BOTTOM * k
        src.set(0, 0, bmp.width, SCENE_TOP)
        canvas.drawBitmap(bmp, src, RectF(0f, 0f, W.toFloat(), topDst), fill)
        src.set(0, SCENE_TOP, bmp.width, bmp.height - SCENE_BOTTOM)
        canvas.drawBitmap(bmp, src, RectF(0f, topDst, W.toFloat(), H - bottomDst), fill)
        src.set(0, bmp.height - SCENE_BOTTOM, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, src, RectF(0f, H - bottomDst, W.toFloat(), H.toFloat()), fill)
    }

    /** The game's countdown ring in the top right: the seconds left and the ring emptying as the drawing is drawn. */
    private fun drawTimer(canvas: Canvas, seconds: Int, drawn: Float) {
        val size = art(98f)
        val cx = art(712f) + size / 2f
        val cy = art(82f) + size / 2f
        plain.color = C.WHITE
        canvas.drawCircle(cx, cy, size / 2f, plain)
        ring.strokeWidth = 2f
        ring.color = 0x33000000
        canvas.drawCircle(cx, cy, size / 2f, ring)
        val left = 1f - drawn
        val shown = kotlin.math.ceil(seconds * left - 0.001f).toInt().coerceAtLeast(0)
        val color = if (shown <= 1) TIMER_WARNING else TIMER
        val inset = 9f
        val arc = RectF(cx - size / 2f + inset, cy - size / 2f + inset, cx + size / 2f - inset, cy + size / 2f - inset)
        ring.strokeWidth = 11f
        ring.color = TIMER_TRACK
        canvas.drawArc(arc, -90f, 360f, false, ring)
        ring.color = color
        if (left > 0f) canvas.drawArc(arc, -90f, 360f * left, false, ring)
        text.draw(canvas, shown.toString(), cx, cy, size.toInt(), 58f, 1, assets.display, color)
    }

    /** A place on bg_draw, in the game's art units (the picture is 841 of them wide), as video pixels. */
    private fun art(v: Float) = v * W / ART_W

    /** The game's canvas: from below the notepad to above the tool board, the painted "+ Süre" pill left clear. */
    private val hookBox = RectF(art(36f) + 24f, art(438f) + 120f, art(806f) - 24f, H - art(ART_H - 1386f) - 16f)

    /** The middle of the tool board under the canvas, where the game's tool buttons sit. */
    private val boardCy = H - art(ART_H - 1433f) + art(49f)

    /** Fills [dst] with [bmp], cropped to its shape. */
    private fun cover(canvas: Canvas, bmp: Bitmap, dst: RectF) {
        if (bmp.isRecycled) return
        val aspect = dst.width() / dst.height()
        var sw = bmp.width.toFloat()
        var sh = sw / aspect
        if (sh > bmp.height) {
            sh = bmp.height.toFloat()
            sw = sh * aspect
        }
        val sx = (bmp.width - sw) / 2f
        val sy = (bmp.height - sh) / 2f
        src.set(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
        canvas.drawBitmap(bmp, src, dst, fill)
    }

    companion object {
        const val W = 1080
        const val H = 1920
        const val FPS = 30
        /** The card's width on the frame; its height follows its own shape. */
        private const val CARD_W = 450f

        /** bg_draw in the game's art units, and the rows of its own pixels kept as painted at the top and the bottom. */
        private const val ART_W = 841f
        private const val ART_H = 1870f
        private const val SCENE_TOP = 720
        private const val SCENE_BOTTOM = 801
        /**
         * GuessScreen's joker row in bg_guess's art units: the tiles' top and height, and where the row starts and
         * ends; [ROW_SCALE] fits it on bg_draw's tool board.
         */
        private const val TILE_TOP = 1406f
        private const val TILE_H = 164f
        private const val ROW_LEFT = 48f
        private const val ROW_RIGHT = 797f
        private const val ROW_SCALE = 0.82f
        /** The joker pictures float over 2.3 s and their glow pulses over 1.7 s, as in the game. */
        private const val FLOAT_MS = 2_300.0
        private const val GLOW_MS = 1_700L
        private const val GLOW = 0xFFFFF1B8.toInt()
        private const val FRAME_TOP = 0xFFFFF3C4.toInt()
        private const val FRAME_BOTTOM = 0xFFE8B84A.toInt()
        private const val HINT_ICON = 0xFFF26A1B.toInt()

        /** One tile of the row as GuessScreen lays it out: its place, its label and colours, and for a joker its picture and count. */
        private class GuessTile(
            val x: Float, val w: Float, val label: String, val labelColor: Int, val outline: Int?,
            val art: Int = 0, val badge: Int = 0, val badgeColor: Int = 0, val skip: Boolean = false
        )

        /** İpucu, the two jokers (with how many are left), Atla. */
        private val GUESS_TILES = listOf(
            GuessTile(48f, 172f, "İpucu", 0xFFB4501A.toInt(), null),
            GuessTile(237f, 171f, "İlk harf", C.WHITE, 0xFF4A1F8A.toInt(), R.drawable.joker_first_letter, 3, 0xFF6A2DBF.toInt()),
            GuessTile(426f, 178f, "Harf sayısı", C.WHITE, 0xFF0B4F8A.toInt(), R.drawable.joker_letter_count, 3, 0xFF1B7FE0.toInt()),
            GuessTile(621f, 176f, "Atla", 0xFF4A2A10.toInt(), null, skip = true)
        )

        /** The closing's subtitle: its widest line, its size, and its middle, kept above the apps' caption band. */
        private const val SUB_W = 860
        private const val SUB_SIZE = 70f
        private const val SUB_CY = 1360f
        /** How far before its end the clip's held frame is taken, clear of a last frame that may come out black. */
        private const val LAST_FRAME_BACK = 60L
        /** The game's 24 sp and 34 sp for the word, on a 411 dp phone, in video pixels. */
        private const val WORD_MIN = 63f
        private const val WORD_MAX = 89f

        private const val SCENE_INK = 0xFF3A2A22.toInt()
        private const val UNDERLINE = 0xFFF58A1F.toInt()
        private const val TIMER = 0xFFF97316.toInt()
        private const val TIMER_WARNING = 0xFFE23A2C.toInt()
        private const val TIMER_TRACK = 0xFFFFE3CC.toInt()
    }
}
