package com.sualtikasifi.cizimhafiza.presentation.reports.studio.real

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
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
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.Lettering
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StrokeReplay
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioAssets
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.StudioColors as C
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.TextPainter
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.VoicedVideo
import com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio.Reveal
import java.io.File
import kotlin.math.hypot
import kotlin.math.min

/**
 * Draws a "Gerçek Olsaydı" video in 1080×2400, the proportions of a phone's full screen (bg_draw is 1080×2401, so the
 * game's drawing screen fits it one to one): one drawing's, the project's featured one. It opens on that screen as the
 * game has it: the match clock and progress, the sound button, the countdown ring, its word on the notepad, the
 * canvas with the "+10 Saniye" joker and the tray with the drawing tools and the "+10 sn" ad button; the drawing is
 * drawn again on its canvas while the timer runs down and Karalak says the hook; then the clip opens out of the drawing. The clip plays
 * full frame with its own sound and, in the top left, the Karalak card (the game's wooden frame with its logo): the
 * player's drawing in the middle of it, the player's frame with the default picture in the round slot at the bottom
 * and their name (or "anonymous") beside it, in a soft dark vignette that lifts it off a bright clip. Its size and place
 * are the [layout] set in the panel. A closing line comes in over the clip's last seconds while it keeps playing, as a
 * subtitle and, when voiced, in Karalak's voice, the clip's own sound stepping back under it. The app's music, when
 * chosen, plays under it all. Last comes the app's own [promo] video, with its own sound, when there is one. The video is
 * cropped to the frame, so a 9:16 clip loses a little of its sides.
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
    /** What Karalak says over the clip as a deadpan narrator ([RealReveal.narration]), laid a beat into the clip. */
    narrationVoiceFile: File? = null,
    outroVoiceFile: File? = null,
    /** Its background music ([RealReveal.music]), under the whole video. */
    musicFile: File? = null,
    /** The app's promo video, played after the drawing's own video with its own sound; [promoMs] long (0: none). */
    private val promo: ClipFrames? = null,
    promoFile: File? = null,
    promoMs: Long = 0L,
    /** The end card after the closing, [endCardMs] long (0: none; the promo, when there is one, takes its place). */
    endCardMs: Long = 0L,
    /** What Karalak says over the end card, when it is voiced. */
    endVoiceFile: File? = null
) : VoicedVideo {
    /** The video is one drawing's: the project's featured one. */
    private val featured = project.featured
    private val shown = listOfNotNull(featured)
    val events: List<RealEvent> = RealTimeline.build(featured, promoMs, endCardMs)
    override val totalMs: Long = RealTimeline.totalMs(events)
    override val width: Int get() = W
    override val height: Int get() = H
    override val voiceStarts: List<Pair<Int, Long>> = emptyList()
    override val sounds: List<ClipSound> = buildList {
        val f = featured ?: return@buildList
        // where Karalak speaks, for the clip's sound and the music to step back
        val voices = events.mapNotNull { e ->
            val line = when (e.phase) {
                RealPhase.Hook -> f.hook.takeIf { hookVoiceFile != null }
                RealPhase.Outro -> f.outro.takeIf { outroVoiceFile != null }
                RealPhase.Clip -> f.narration.takeIf { narrationVoiceFile != null }
                RealPhase.EndCard -> f.endLine.takeIf { endVoiceFile != null }
                else -> null
            }
            line?.takeIf { it.voice != null }?.let { ((if (e.phase == RealPhase.Clip) e.clipAtMs + RealTimeline.NARRATION_AT else e.startMs + RealTimeline.VOICE_AT)).let { at -> at..at + it.voiceMs } }
        }
        var clipRange: LongRange? = null
        events.forEach { e ->
            when (e.phase) {
                RealPhase.Hook -> if (hookVoiceFile != null && f.hook.voice != null) {
                    add(ClipSound(hookVoiceFile, e.startMs + RealTimeline.VOICE_AT, f.hook.voiceMs, 1f, voice = true))
                }
                RealPhase.Clip -> {
                    clipRange = e.clipAtMs until e.clipAtMs + e.clipMs
                    val at = e.clipAtMs
                    if (narrationVoiceFile != null && f.narration.voice != null) {
                        add(ClipSound(narrationVoiceFile, at + RealTimeline.NARRATION_AT, f.narration.voiceMs, 1f, voice = true))
                    }
                    if (clipGain > 0f) clipFiles[f.sampleId]?.let { add(ClipSound(it, at, e.clipMs, clipGain, shape = { ms -> RealMix.clipGainAt(at + ms, at, voices) })) }
                }
                RealPhase.Outro -> if (outroVoiceFile != null && f.outro.voice != null) {
                    add(ClipSound(outroVoiceFile, e.startMs + RealTimeline.VOICE_AT, f.outro.voiceMs, 1f, voice = true))
                }
                RealPhase.Promo -> promoFile?.let { add(ClipSound(it, e.startMs, e.durationMs, 1f, shape = RealMix::clipGainAt)) }
                // the music runs through the end card and fades out at its end; Karalak's line over it, when voiced
                RealPhase.EndCard -> if (endVoiceFile != null && f.endLine.voice != null) {
                    add(ClipSound(endVoiceFile, e.startMs + RealTimeline.VOICE_AT, f.endLine.voiceMs, 1f, voice = true))
                }
            }
        }
        // the music is the drawing's video's: it has faded out by the time the promo starts
        if (musicFile != null && f.music != null && f.musicVolume > 0f) {
            val main = RealTimeline.mainMs(events)
            val clip = clipRange
            add(ClipSound(musicFile, 0L, main, f.musicVolume.coerceAtMost(RealMix.MAX_VOLUME), shape = { ms -> RealMix.gainAt(ms, main, voices, clip) }))
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
    /** For the vignette's alpha mask (drawn in its colour) and the promo fading in. */
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

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

    /**
     * The card's own outline (its wooden frame and the logo on top), blurred: the soft dark vignette drawn under it,
     * made once at the card's size on the video. Null without the card's picture.
     */
    private val vignette: Pair<Bitmap, android.graphics.PointF>? by lazy {
        val bmp = card ?: return@lazy null
        runCatching {
            val small = Bitmap.createScaledBitmap(bmp, cardBox.width().toInt().coerceAtLeast(1), cardBox.height().toInt().coerceAtLeast(1), true)
            val offset = IntArray(2)
            val mask = small.extractAlpha(Paint().apply { maskFilter = BlurMaskFilter(VIGNETTE_BLUR * lay.scale, BlurMaskFilter.Blur.NORMAL) }, offset)
            if (small !== bmp) small.recycle()
            mask to android.graphics.PointF(offset[0].toFloat(), offset[1].toFloat())
        }.getOrNull()
    }

    /** The frame picked by hand (any frame, whatever the level); none picked: the one the player's level gives. */
    private fun frameOf(d: RealDrawing): AvatarFrame =
        AvatarFrame.entries.find { it.name == d.frameId } ?: AvatarFrame.resolve(d.frameId, d.level.coerceAtLeast(1))

    private val handPhotos = HashMap<String, Bitmap?>()

    /** The picture picked by hand, read once; otherwise the default one. */
    private fun photoOf(d: RealDrawing): Bitmap? = (d.photo?.let { photoFiles[d.id] }?.let { f -> handPhotos.getOrPut(d.id) { RealImages.load(f, maxSide = 512) } }) ?: dino

    private fun replay(id: String): StrokeReplay? = project.drawing(id)?.let { d -> replays.getOrPut(id) { StrokeReplay(d.strokes) } }

    override fun draw(canvas: Canvas, t: Long) {
        val e = RealTimeline.at(events, t)
        if (e.phase == RealPhase.Promo) { drawPromo(canvas, e, t); return }
        if (e.phase == RealPhase.EndCard) { drawEndCard(canvas, e, t); return }
        val reveal = shown.getOrNull(e.index) ?: return
        when (e.phase) {
            RealPhase.Hook -> drawHook(canvas, e, reveal, t)
            RealPhase.Outro -> drawOutro(canvas, e, reveal, t)
            else -> drawClip(canvas, e, reveal, t)
        }
        drawCaptions(canvas, t)
    }

    private fun drawClip(canvas: Canvas, e: RealEvent, reveal: RealReveal, t: Long) {
        canvas.drawColor(C.INK)
        val frame = clips[reveal.sampleId]?.frameAt((t - e.clipAtMs).coerceIn(0L, e.clipMs))
        if (frame != null) cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        val drawing = project.drawing(reveal.sampleId) ?: return
        val local = t - e.startMs
        drawCard(canvas, drawing, Ease.backOut(min(1f, local / 450f)))
    }

    /** One of Karalak's lines as a subtitle: what it says, when each letter comes in, and from when to when it is up. */
    private class Caption(val text: String, val starts: LongArray, val fromMs: Long, val untilMs: Long)

    /**
     * The hook and the closing as subtitles, coming in with Karalak's voice: by its words when ElevenLabs timed them,
     * spread over the voice otherwise, and read in at a reading pace when the line is not voiced. The hook stays up
     * through the opening (and on over the clip while a long voice is still going), the closing through the closing.
     */
    private val captions: List<Caption> = buildList {
        val f = featured ?: return@buildList
        fun line(l: VoicedLine, e: RealEvent?, keepTo: (Long, LongArray) -> Long) {
            val words = l.text.trim()
            if (words.isEmpty() || e == null) return
            val voiced = l.voice != null && l.voiceMs > 0L
            val starts = Reveal.charStarts(words, if (voiced) l.wordRanges() else null, if (voiced) l.voiceMs else 0L)
            val from = e.startMs + if (voiced) RealTimeline.VOICE_AT else CAPTION_SILENT_AT
            add(Caption(words, starts, from, keepTo(from, starts)))
        }
        val hook = events.firstOrNull { it.phase == RealPhase.Hook }
        line(f.hook, hook) { from, starts -> maxOf(hook!!.endMs, from + Reveal.doneAt(starts) + CAPTION_HOLD) }
        // the narration: a beat into the clip, up until the closing takes over
        val clip = events.firstOrNull { it.phase == RealPhase.Clip }
        val outroAt = events.firstOrNull { it.phase == RealPhase.Outro }?.startMs
        clip?.let { c ->
            val words = f.narration.text.trim()
            if (words.isNotEmpty()) {
                val voiced = f.narration.voice != null && f.narration.voiceMs > 0L
                val starts = Reveal.charStarts(words, if (voiced) f.narration.wordRanges() else null, if (voiced) f.narration.voiceMs else 0L)
                val from = c.clipAtMs + RealTimeline.NARRATION_AT
                val until = minOf(from + Reveal.doneAt(starts) + CAPTION_HOLD, outroAt ?: c.endMs)
                add(Caption(words, starts, from, maxOf(until, from + 500L)))
            }
        }
        val outro = events.firstOrNull { it.phase == RealPhase.Outro }
        line(f.outro, outro) { _, _ -> outro!!.endMs }
    }

    private val captionLettering = Lettering.onPicture(CAPTION_SIZE)

    /** The subtitles up at [t], in the game's lettering right under the joker board (where the closing sits too). */
    private fun drawCaptions(canvas: Canvas, t: Long) {
        captions.forEach { c ->
            if (t < c.fromMs || t > c.untilMs + CAPTION_FADE) return@forEach
            val alpha = if (t <= c.untilMs) 1f else 1f - (t - c.untilMs).toFloat() / CAPTION_FADE
            val (_, h) = text.measure(c.text, CAPTION_W, CAPTION_SIZE, 2, assets.display, C.WHITE, spacing = CAPTION_SPACING, balanced = true)
            text.reveal(
                canvas, c.text, c.starts, t - c.fromMs, W / 2f, CAPTION_TOP + h / 2f, CAPTION_W, CAPTION_SIZE, 2,
                assets.display, captionLettering, alpha, CAPTION_SPACING, balanced = true
            )
        }
    }

    /** The Karalak card with the player's drawing, frame, picture and name on it; it pops in from its top left corner. */
    private fun drawCard(canvas: Canvas, d: RealDrawing, a: Float) {
        val bmp = card ?: return
        canvas.save()
        canvas.scale(a, a, cardBox.left, cardBox.top)
        vignette?.let { (mask, off) ->
            shade.color = VIGNETTE
            canvas.drawBitmap(mask, cardBox.left + off.x, cardBox.top + off.y + VIGNETTE_DROP * lay.scale, shade)
        }
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
     * The opening: the game's drawing screen as the game has it (bg_draw one to one, then the header's match clock and
     * progress, the sound button and the countdown ring, the word on the notepad, the canvas with its "+10 Saniye" joker
     * and the tray's drawing tools with the "+10 sn" ad button), the timer running down while the drawing is drawn
     * again on the canvas. It is all there from the very first frame, which is also the video's cover. At the end the
     * clip opens out of the drawing in a growing circle.
     */
    private fun drawHook(canvas: Canvas, e: RealEvent, reveal: RealReveal, t: Long) {
        val d = project.drawing(reveal.sampleId) ?: return
        val local = t - e.startMs
        canvas.drawColor(C.CREAM)
        // the clip's first frame, once it opens: the screen zooms into the drawing as it fades into it (see below)
        val openFrom = e.durationMs - RealTimeline.HOOK_REVEAL
        val morph = if (local >= openFrom) ((local - openFrom).toFloat() / RealTimeline.HOOK_REVEAL).coerceIn(0f, 1f) else 0f
        val m = Ease.inOut(morph)
        canvas.save()
        if (morph > 0f) {
            // the drawing's outline now, and where the same drawing sits in the picture the clip starts from (a square sheet
            // across the frame's width, centred): the screen is scaled and slid so the one lands on the other
            val replayOf = replay(d.id)
            val from = replayOf?.bounds(hookBox) ?: RectF(hookBox)
            val to = replayOf?.bounds(RectF(0f, (H - W) / 2f, W.toFloat(), (H + W) / 2f)) ?: RectF(0f, (H - W) / 2f, W.toFloat(), (H + W) / 2f)
            val s = ((to.width() + to.height()) / (from.width() + from.height()).coerceAtLeast(1f)).coerceIn(1f, MORPH_MAX_ZOOM)
            val scale = 1f + (s - 1f) * m
            canvas.translate(from.centerX() + (to.centerX() - from.centerX()) * m, from.centerY() + (to.centerY() - from.centerY()) * m)
            canvas.scale(scale, scale)
            canvas.translate(-from.centerX(), -from.centerY())
        }
        scene?.let { canvas.drawBitmap(it, null, RectF(0f, 0f, W.toFloat(), H.toFloat()), fill) }

        // the drawing, drawn again in the time the opening leaves before the clip (after the glimpse of the real thing, when it opens with one)
        val teaser = reveal.opening == RealOpening.Teaser
        val tease = if (teaser) RealTimeline.TEASE_MS else 0L
        val box = hookBox
        val drawEnd = (e.durationMs - tease - RealTimeline.HOOK_REVEAL - RealTimeline.HOOK_HOLD).coerceAtLeast(1L)
        val drawn = ((local - tease).toFloat() / drawEnd).coerceIn(0f, 1f)
        val secondsLeft = if (d.seconds > 0) kotlin.math.ceil(d.seconds * (1f - drawn) - 0.001f).toInt().coerceAtLeast(0) else 0

        // header: the match clock and which word it is, the sound button, the countdown ring
        drawHeader(canvas, secondsLeft + MATCH_EXTRA_SECONDS)
        if (d.seconds > 0) drawTimer(canvas, d.seconds, drawn)

        // the word on the notepad, sized as the game sizes it (short words large, long ones smaller), with its orange underline
        val word = d.word.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("tr")) else it.toString() }
        val wordSize = (art(425f) * 0.97f / (word.length.coerceAtLeast(1) * 0.5f)).coerceIn(WORD_MIN, WORD_MAX)
        text.draw(canvas, word, art(125f + 222.5f), art(232f + 56f), art(445f - 20f).toInt(), wordSize, 2, assets.display, SCENE_INK)
        plain.color = UNDERLINE
        canvas.drawRoundRect(RectF(art(235f), art(343f), art(485f), art(351f)), art(4f), art(4f), plain)

        // the canvas: white dotted paper over the picture's painted one, the drawing on it, the "+10 Saniye" joker on top
        drawCanvasPaper(canvas)
        canvas.save()
        circle.reset()
        circle.addRoundRect(canvasRect, art(20f), art(20f), Path.Direction.CW)
        canvas.clipPath(circle)
        replay(d.id)?.draw(canvas, box, drawn, sceneInk, 0.02f)
        canvas.restore()
        drawTimeJoker(canvas)

        // the tray: pen (picked), eraser, undo and clear (dim until there is something drawn), and the ad button
        drawTray(canvas, drawn > 0f)

        canvas.restore()

        // a glimpse of the real thing shrinks away into the canvas, then the drawing is drawn
        if (teaser && local < tease) drawTease(canvas, reveal, local, box)

        // the first clip comes in over the zooming screen, settling from a slight push-in, so the drawing turns into it where it stands
        if (morph > 0f) {
            val frame = clips[reveal.sampleId]?.frameAt(0L) ?: return
            val a = Ease.inOut(((morph - MORPH_IN) / (MORPH_FULL - MORPH_IN)).coerceIn(0f, 1f))
            val zoom = 1f + MORPH_SETTLE * (1f - m)
            shade.color = C.WHITE
            shade.alpha = (255 * a).toInt()
            canvas.save()
            canvas.scale(zoom, zoom, W / 2f, H / 2f)
            cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()), shade)
            canvas.restore()
            shade.alpha = 255
        }
    }

    /**
     * What the opening starts with when it is a [RealOpening.Teaser]: the clip, already playing, full frame, so the very
     * first frame (the video's cover) shows what the doodle became; it holds for [RealTimeline.TEASE_HOLD], then
     * shrinks into the middle of the canvas, the way the clip later opens out of it, and the drawing starts.
     */
    private fun drawTease(canvas: Canvas, reveal: RealReveal, local: Long, box: RectF) {
        val clip = clips[reveal.sampleId] ?: return
        val frame = clip.frameAt((reveal.clipMs * TEASE_AT).toLong() + local) ?: return
        val shrink = ((local - RealTimeline.TEASE_HOLD).toFloat() / (RealTimeline.TEASE_MS - RealTimeline.TEASE_HOLD)).coerceIn(0f, 1f)
        val cx = box.centerX()
        val cy = box.centerY()
        val full = hypot(maxOf(cx, W - cx), maxOf(cy, H - cy))
        val r = full * (1f - Ease.out(shrink))
        if (r < 1f) return
        // a slow push in while it is still full frame
        val zoom = 1.07f - 0.07f * (local.toFloat() / RealTimeline.TEASE_MS).coerceIn(0f, 1f)
        circle.reset()
        circle.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(circle)
        canvas.scale(zoom, zoom, W / 2f, H / 2f)
        cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        canvas.restore()
        if (shrink > 0f) {
            edge.strokeWidth = 10f * shrink.coerceAtMost(1f)
            canvas.drawCircle(cx, cy, r, edge)
        }
    }

    /**
     * The end card, after Karalak's closing: the game's own picture (what the game is, in three steps, and where to get
     * it), the same in every video. It opens out of the middle of the clip's last frame in a growing circle, the way the
     * clip opens out of the drawing at the start, and settles in from a slight zoom so it is not a still; it stays for as
     * long as Karalak's line over it takes (see [RealReveal.endCardMs]).
     */
    private fun drawEndCard(canvas: Canvas, e: RealEvent, t: Long) {
        val local = t - e.startMs
        val open = Ease.out((local.toFloat() / END_OPEN).coerceIn(0f, 1f))
        // what the circle opens over: the clip's last frame, with the closing
        if (open < 1f && e.startMs > 0) draw(canvas, e.startMs - 1) else canvas.drawColor(C.CREAM)
        val picture = assets.picture(R.drawable.real_end_card) ?: return
        val cx = W / 2f
        val cy = H / 2f
        val r = open * hypot(cx, cy)
        circle.reset()
        circle.addCircle(cx, cy, r, Path.Direction.CW)
        val settle = Ease.out((local.toFloat() / END_SETTLE).coerceIn(0f, 1f))
        val zoom = 1f + END_ZOOM * (1f - settle)
        canvas.save()
        canvas.clipPath(circle)
        canvas.scale(zoom, zoom, cx, cy)
        // a picture near the frame's shape fills it (its sides trimmed a little); one far from it is shown whole over a soft backdrop
        if (kotlin.math.abs(picture.width.toFloat() / picture.height / (W.toFloat() / H) - 1f) <= END_COVER_SLACK) {
            cover(canvas, picture, RectF(0f, 0f, W.toFloat(), H.toFloat()))
        } else {
            fitOnBackdrop(canvas, picture, 1f)
        }
        canvas.restore()
        edge.strokeWidth = 10f * (1f - open)
        if (edge.strokeWidth > 0.5f) canvas.drawCircle(cx, cy, r, edge)
    }

    /**
     * The closing: the clip keeps playing under it, the card still on it, and Karalak's reaction as a subtitle (see
     * [drawCaptions]). Only a closing longer than what is left of the clip holds its last frame, drifting slowly closer
     * so it does not look stuck.
     */
    private fun drawOutro(canvas: Canvas, e: RealEvent, reveal: RealReveal, t: Long) {
        canvas.drawColor(C.INK)
        val held = (e.clipMs - LAST_FRAME_BACK).coerceAtLeast(0L)
        val at = (t - e.clipAtMs).coerceIn(0L, held)
        val frame = clips[reveal.sampleId]?.frameAt(at)
        if (frame != null) {
            // past the clip's end: how far into the hold, 0 to 1
            val clipEnd = e.clipAtMs + held
            val hold = if (t <= clipEnd || e.endMs <= clipEnd) 0f else ((t - clipEnd).toFloat() / (e.endMs - clipEnd)).coerceIn(0f, 1f)
            val z = 1f + 0.04f * Ease.out(hold)
            canvas.save()
            canvas.scale(z, z, W / 2f, H / 2f)
            cover(canvas, frame, RectF(0f, 0f, W.toFloat(), H.toFloat()))
            canvas.restore()
        }
        project.drawing(reveal.sampleId)?.let { drawCard(canvas, it, 1f) }
    }

    /**
     * The app's promo video, full frame with its own sound, fading in over the drawing's video's last moment. A promo
     * that is not 9:16 is shown whole over a blurred, darkened copy of itself rather than cropped.
     */
    private fun drawPromo(canvas: Canvas, e: RealEvent, t: Long) {
        val local = t - e.startMs
        val fade = (local.toFloat() / PROMO_FADE).coerceIn(0f, 1f)
        if (fade < 1f && e.startMs > 0) draw(canvas, e.startMs - 1) else canvas.drawColor(C.INK)
        val frame = promo?.frameAt(local) ?: return
        if (frame.isRecycled) return
        val full = RectF(0f, 0f, W.toFloat(), H.toFloat())
        val aspect = frame.width.toFloat() / frame.height
        if (kotlin.math.abs(aspect / (W.toFloat() / H) - 1f) <= PROMO_COVER_SLACK) {
            shade.color = C.WHITE
            shade.alpha = (255 * fade).toInt()
            cover(canvas, frame, full, shade)
            shade.alpha = 255
        } else {
            fitOnBackdrop(canvas, frame, fade)
        }
    }

    /** [bmp] shown whole, as wide as the frame allows, over a soft darkened backdrop made of itself, at [alpha] strength. */
    private fun fitOnBackdrop(canvas: Canvas, bmp: Bitmap, alpha: Float) {
        if (bmp.isRecycled) return
        val full = RectF(0f, 0f, W.toFloat(), H.toFloat())
        shade.color = C.WHITE
        shade.alpha = (255 * alpha).toInt()
        // a soft backdrop: the frame shrunk to a few pixels and spread over the screen, darkened
        val tiny = Bitmap.createScaledBitmap(bmp, 18, 32, true)
        cover(canvas, tiny, full, shade)
        if (tiny !== bmp) tiny.recycle()
        plain.color = 0x000000
        plain.alpha = (110 * alpha).toInt()
        canvas.drawRect(full, plain)
        plain.alpha = 255
        val s = min(W.toFloat() / bmp.width, H.toFloat() / bmp.height)
        val w = bmp.width * s
        val h = bmp.height * s
        canvas.drawBitmap(bmp, null, RectF((W - w) / 2f, (H - h) / 2f, (W + w) / 2f, (H + h) / 2f), shade)
        shade.alpha = 255
    }

    /** One of the game's dp on the video (the game lays its screen out on a 411 dp wide phone). */
    private val dp = W / 411f

    /** The game's canvas on the video: from below the notepad to above the tray. */
    private val canvasRect = RectF(art(36f), art(438f), art(806f), H - art(ART_H - 1386f))

    /** Where the drawing goes: the canvas. */
    private val hookBox = canvasRect

    private fun fromBottom(artY: Float) = H - art(ART_H - artY)

    private val icons = HashMap<String, Path>()
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** A Material icon from its path data (24 x 24), [size] big, centred on ([cx], [cy]). */
    private fun drawIcon(canvas: Canvas, pathData: String, cx: Float, cy: Float, size: Float, color: Int) {
        val path = icons.getOrPut(pathData) { androidx.core.graphics.PathParser.createPathFromPathData(pathData) ?: Path() }
        canvas.save()
        canvas.translate(cx - size / 2f, cy - size / 2f)
        canvas.scale(size / 24f, size / 24f)
        plain.color = color
        canvas.drawPath(path, plain)
        canvas.restore()
    }

    /** The header as DrawingScreen lays it: the match clock and which word it is in the painted pill, the sound button beside it. */
    private fun drawHeader(canvas: Canvas, clockSeconds: Int) {
        val clock = "%d:%02d".format(clockSeconds / 60, clockSeconds % 60)
        val line = "$clock  ·  1 / $PROGRESS_OF"
        label.typeface = assets.display
        label.style = Paint.Style.FILL
        label.textAlign = Paint.Align.LEFT
        label.textSize = 17f * dp
        label.color = SCENE_INK
        val icon = 18f * dp
        val gap = 5f * dp
        val tw = label.measureText(line)
        val cx = art(152f + 148f)
        val cy = art(88f + 41f)
        val left = cx - (icon + gap + tw) / 2f
        drawIcon(canvas, ICON_TIMER, left + icon / 2f, cy, icon, UNDERLINE_ICON)
        val fm = label.fontMetrics
        canvas.drawText(line, left + icon + gap, cy - (fm.ascent + fm.descent) / 2f, label)
        drawIcon(canvas, ICON_VOLUME, art(593f + 48.5f), art(88f + 41f), 26f * dp, SCENE_INK)
    }

    /** The canvas's own white dotted paper, laid over the picture's painted one (which has its "+ Süre" pill printed on it). */
    private fun drawCanvasPaper(canvas: Canvas) {
        circle.reset()
        circle.addRoundRect(canvasRect, art(20f), art(20f), Path.Direction.CW)
        canvas.save()
        canvas.clipPath(circle)
        plain.color = C.WHITE
        canvas.drawRect(canvasRect, plain)
        dotPaint.color = CANVAS_GRID
        val spacing = 22f * dp
        var y = canvasRect.top + spacing / 2f
        while (y < canvasRect.bottom) {
            var x = canvasRect.left + spacing / 2f
            while (x < canvasRect.right) {
                canvas.drawCircle(x, y, 1.2f * dp, dotPaint)
                x += spacing
            }
            y += spacing
        }
        canvas.restore()
    }

    /** A picture drawn as a nine-patch: [slice] source pixels kept from each side, its corners drawn [edge] wide. */
    private fun ninePatch(canvas: Canvas, bmp: Bitmap, dst: RectF, slice: Int, edge: Float) {
        val e = min(edge, min(dst.width(), dst.height()) / 2f)
        val sx = intArrayOf(0, slice, bmp.width - slice, bmp.width)
        val sy = intArrayOf(0, slice, bmp.height - slice, bmp.height)
        val dx = floatArrayOf(dst.left, dst.left + e, dst.right - e, dst.right)
        val dy = floatArrayOf(dst.top, dst.top + e, dst.bottom - e, dst.bottom)
        for (r in 0..2) for (c in 0..2) {
            src.set(sx[c], sy[r], sx[c + 1], sy[r + 1])
            canvas.drawBitmap(bmp, src, RectF(dx[c], dy[r], dx[c + 1], dy[r + 1]), fill)
        }
    }

    /** DrawingScreen's "+10 Saniye" joker at the canvas's top right: the orange pill with its picture and lettering, the count on its corner. */
    private fun drawTimeJoker(canvas: Canvas) {
        val outerW = (7f + 28f + 6f + 84f + 16f + 8f) * dp
        val left = canvasRect.right - art(10f) - outerW
        val top = canvasRect.top + art(8f)
        val pill = RectF(left, top + 7f * dp, left + outerW - 8f * dp, top + 47f * dp)
        assets.picture(R.drawable.st_pill_orange)?.let { ninePatch(canvas, it, pill, 72, 20f * dp) }
        val side = 28f * dp
        val artBox = RectF(pill.left + 7f * dp, pill.centerY() - side / 2f, pill.left + 7f * dp + side, pill.centerY() + side / 2f)
        assets.picture(R.drawable.joker_extra_time)?.let { bmp ->
            circle.reset()
            circle.addRoundRect(artBox, side * 0.24f, side * 0.24f, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(circle)
            val grow = side * 0.11f
            canvas.drawBitmap(bmp, null, RectF(artBox.left - grow, artBox.top - grow, artBox.right + grow, artBox.bottom + grow), fill)
            canvas.restore()
        }
        lettered(canvas, "+10 Saniye", pill.left + (7f + 28f + 6f + 42f) * dp, pill.centerY(), 16f * dp, C.WHITE, 0xFF8A3A00.toInt(), 84f * dp, dp)
        // the count, on the corner
        val d = 22f * dp
        val bx = left + outerW - d / 2f
        val by = top + d / 2f
        plain.color = 0x33000000
        canvas.drawCircle(bx, by + 1.5f * dp, d / 2f, plain)
        plain.color = 0xFF3A2112.toInt()
        canvas.drawCircle(bx, by, d / 2f, plain)
        ring.strokeWidth = 1.5f * dp
        ring.color = 0xFFFFC94D.toInt()
        canvas.drawCircle(bx, by, d / 2f - ring.strokeWidth / 2f, ring)
        text.draw(canvas, TIME_JOKERS.toString(), bx, by, d.toInt(), 12f * dp, 1, assets.display, C.WHITE)
    }

    /** The tray as DrawingScreen has it: the four paper-cream tools (pen picked) and the painted "+10 sn" ad button. */
    private fun drawTray(canvas: Canvas, drawnAny: Boolean) {
        val size = art(98f)
        val top = fromBottom(1433f)
        drawTool(canvas, art(48f), top, size, true, true, ICON_PEN)
        drawTool(canvas, art(165f), top, size, false, true, null)
        drawTool(canvas, art(290f), top, size, false, drawnAny, ICON_UNDO)
        drawTool(canvas, art(418f), top, size, false, drawnAny, ICON_DELETE)
        // the ad button: right-aligned in its slot, 2.74 : 1
        val w = art(270f)
        val h = w / 2.736f
        val slotTop = fromBottom(1428f)
        val x = art(520f) + art(278f) - w
        assets.picture(R.drawable.btn_ad_extra_time)?.let { canvas.drawBitmap(it, null, RectF(x, slotTop + (art(104f) - h) / 2f, x + w, slotTop + (art(104f) - h) / 2f + h), fill) }
    }

    private fun drawTool(canvas: Canvas, left: Float, top: Float, size: Float, selected: Boolean, enabled: Boolean, icon: String?) {
        val box = RectF(left, top, left + size, top + size)
        val r = size * 0.24f
        if (!enabled) canvas.saveLayerAlpha(RectF(left - 12f, top - 12f, left + size + 12f, top + size + 16f), 102)
        // its shadow, then the paper (or orange) face and its border
        shade.shader = null
        shade.color = if (selected) 0xFFF58A1F.toInt() else 0xFFF3EDE2.toInt()
        shade.setShadowLayer(3f * dp, 0f, 1.5f * dp, 0x55000000)
        canvas.drawRoundRect(box, r, r, shade)
        shade.clearShadowLayer()
        shade.shader = LinearGradient(
            0f, top, 0f, top + size,
            if (selected) 0xFFFFA24A.toInt() else 0xFFFFFDF8.toInt(), if (selected) 0xFFF58A1F.toInt() else 0xFFF3EDE2.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(box, r, r, shade)
        shade.shader = null
        ring.strokeWidth = 1.5f * dp
        ring.color = if (selected) 0xFFE5701A.toInt() else 0xFFB9AFA3.toInt()
        val half = ring.strokeWidth / 2f
        canvas.drawRoundRect(RectF(box.left + half, box.top + half, box.right - half, box.bottom - half), r, r, ring)
        val tint = if (selected) 0xFF4A2A10.toInt() else SCENE_INK
        if (icon != null) drawIcon(canvas, icon, box.centerX(), box.centerY(), size * 0.5f, tint) else drawEraser(canvas, box.centerX(), box.centerY(), size * 0.5f, tint)
        if (!enabled) canvas.restore()
    }

    /** EraserGlyph: a tilted rounded block with a stripe cut across it. */
    private fun drawEraser(canvas: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val bodyW = s * 0.52f
        val bodyH = s * 0.92f
        val left = (s - bodyW) / 2f
        val top = (s - bodyH) / 2f
        val corner = bodyW * 0.3f
        canvas.save()
        canvas.translate(cx - s / 2f, cy - s / 2f)
        canvas.saveLayer(0f, 0f, s, s, null)
        canvas.rotate(38f, s / 2f, s / 2f)
        plain.color = color
        canvas.drawRoundRect(RectF(left, top, left + bodyW, top + bodyH), corner, corner, plain)
        clear.strokeWidth = bodyW * 0.16f
        canvas.drawLine(left - corner, top + bodyH * 0.66f, left + bodyW + corner, top + bodyH * 0.66f, clear)
        canvas.restore()
        canvas.restore()
    }

    private val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
    }

    /**
     * A label as the game letters it, centred on ([cx], [cy]): with an [outline], white over that edge with a soft
     * shadow (LetteredText); without, plain in [color]. It shrinks to fit [maxW].
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

    /** Fills [dst] with [bmp], cropped to its shape. */
    private fun cover(canvas: Canvas, bmp: Bitmap, dst: RectF, paint: Paint = fill) {
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
        canvas.drawBitmap(bmp, src, dst, paint)
    }

    companion object {
        const val W = 1080
        const val H = 2400
        const val FPS = 30
        /** The card's width on the frame; its height follows its own shape. */
        private const val CARD_W = 450f
        /** The vignette around the card: how far it spreads and how dark it is (at the default size), a little lower than the card. */
        private const val VIGNETTE_BLUR = 54f
        private const val VIGNETTE = 0x8C000000.toInt()
        private const val VIGNETTE_DROP = 10f
        /** The promo fades in over this long; one whose shape is within this share of 9:16 fills the frame. */
        private const val PROMO_FADE = 300L
        private const val PROMO_COVER_SLACK = 0.06f

        /** bg_draw in the game's art units (the picture is 841 of them wide). */
        private const val ART_W = 841f

        private const val ART_H = 1870f

        /** The drawing turns into the clip's first frame: the screen zooms at most this much, and the frame fades in between these shares of the move, settling from a slight push-in. */
        private const val MORPH_MAX_ZOOM = 8f
        private const val MORPH_IN = 0.3f
        private const val MORPH_FULL = 0.8f
        private const val MORPH_SETTLE = 0.05f

        /** The header's match clock runs this many seconds ahead of the drawing's own, and the progress reads "1 / 10"; the "+10 Saniye" joker shows this many left. */
        private const val MATCH_EXTRA_SECONDS = 30
        private const val PROGRESS_OF = 10
        private const val TIME_JOKERS = 12

        /**
         * The subtitles (the hook and the closing): their top, right under the board, their size and width, how close
         * their two lines sit, and their timing: a silent line starts a beat in, the hook stays a moment once said, and
         * a subtitle fades out over [CAPTION_FADE].
         */
        private const val CAPTION_TOP = 2010f
        private const val CAPTION_SIZE = 66f
        private const val CAPTION_W = 880
        private const val CAPTION_SPACING = 0.86f
        private const val CAPTION_SILENT_AT = 250L
        private const val CAPTION_HOLD = 700L
        private const val CAPTION_FADE = 220L
        private const val CANVAS_GRID = 0xFFEDE2D0.toInt()
        private const val UNDERLINE_ICON = 0xFFF26A1B.toInt()

        // Material icon paths (24 x 24)
        private const val ICON_PEN = "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z"
        private const val ICON_UNDO = "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z"
        private const val ICON_DELETE = "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"
        private const val ICON_VOLUME = "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
        private const val ICON_TIMER = "M15,1H9v2h6V1zM11,14h2V8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z"

        /** Where the glimpse of the real thing is taken from the clip (a share of its length), for a teaser opening. */
        private const val TEASE_AT = 0.4f

        /** The end card picture: how long the circle takes to open over the clip, and how long the picture settles from a slight zoom. */
        private const val END_OPEN = 560L
        private const val END_SETTLE = 800L
        private const val END_ZOOM = 0.03f
        /** An end card picture within this share of the frame's shape fills the frame. */
        private const val END_COVER_SLACK = 0.25f

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
