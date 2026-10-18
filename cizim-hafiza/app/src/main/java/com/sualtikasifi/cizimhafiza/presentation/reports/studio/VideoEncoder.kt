package com.sualtikasifi.cizimhafiza.presentation.reports.studio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Turns frames drawn with android.graphics into an H.264 MP4 on the phone's own hardware encoder.
 *
 * Each frame is drawn into a bitmap, put on the encoder's input surface through OpenGL, and
 * stamped with its own presentation time. The stamp is what makes the file play at the right
 * speed: frames are made much faster than real time, and a surface fed through a plain canvas
 * would carry the wall-clock time they were made at instead.
 *
 * Blocking, and it must stay on one thread from start to finish (an EGL context belongs to the
 * thread that made it current), so call it from a background dispatcher without suspending.
 */
object VideoEncoder {
    private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val BIT_RATE = 6_000_000

    fun encode(
        out: File,
        width: Int,
        height: Int,
        fps: Int,
        frames: Int,
        drawFrame: (Canvas, Long) -> Unit,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean
    ) {
        val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MIME)
        var inputSurface: Surface? = null
        var egl: EglWindow? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var track = -1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var finished = false
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = codec.createInputSurface().also { inputSurface = it }
            codec.start()
            val mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
            val window = EglWindow(surface).also { egl = it }
            val quad = TextureQuad()
            val canvas = Canvas(bitmap)
            val info = MediaCodec.BufferInfo()

            fun drain(endOfStream: Boolean) {
                if (endOfStream) codec.signalEndOfInputStream()
                var idle = 0
                while (true) {
                    val index = codec.dequeueOutputBuffer(info, 5_000)
                    when {
                        index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) return
                            if (++idle > 1_000) error("Kodlayıcı videoyu bitiremedi")
                        }
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            check(!muxerStarted) { "Video biçimi iki kez değişti" }
                            track = mux.addTrack(codec.outputFormat)
                            mux.start()
                            muxerStarted = true
                        }
                        index >= 0 -> {
                            val buffer = codec.getOutputBuffer(index) ?: error("Kodlayıcı boş çıktı verdi")
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (info.size > 0 && muxerStarted) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                mux.writeSampleData(track, buffer, info)
                            }
                            codec.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                        }
                    }
                }
            }

            for (i in 0 until frames) {
                if (isCancelled()) throw CancellationException("Video oluşturma durduruldu")
                drawFrame(canvas, i * 1000L / fps)
                quad.draw(bitmap)
                window.setPresentationTime(i * 1_000_000_000L / fps)
                window.swap()
                drain(false)
                if (i % 6 == 0) onProgress(i.toFloat() / frames)
            }
            drain(true)
            check(muxerStarted) { "Video yazılamadı" }
            mux.stop()
            muxerStarted = false
            onProgress(1f)
            finished = true
        } finally {
            runCatching { egl?.release() }
            runCatching { inputSurface?.release() }
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { if (muxerStarted) muxer?.stop() }
            runCatching { muxer?.release() }
            bitmap.recycle()
            if (!finished) out.delete()
        }
    }
}

/** An EGL window on the encoder's input surface, current on the calling thread. */
private class EglWindow(surface: Surface) {
    private val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val context: EGLContext
    private val eglSurface: EGLSurface

    init {
        check(display != EGL14.EGL_NO_DISPLAY) { "EGL ekranı bulunamadı" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "EGL başlatılamadı" }
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val found = IntArray(1)
        check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, configs.size, found, 0) && found[0] > 0) { "Uygun EGL ayarı yok" }
        context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        check(context != EGL14.EGL_NO_CONTEXT) { "EGL bağlamı kurulamadı" }
        eglSurface = EGL14.eglCreateWindowSurface(display, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "EGL yüzeyi kurulamadı" }
        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) { "EGL bağlamı etkinleşmedi" }
    }

    fun setPresentationTime(nanos: Long) {
        EGLExt.eglPresentationTimeANDROID(display, eglSurface, nanos)
    }

    fun swap() {
        check(EGL14.eglSwapBuffers(display, eglSurface)) { "Kare kodlayıcıya gönderilemedi" }
    }

    fun release() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, eglSurface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(display)
    }

    private companion object {
        const val EGL_RECORDABLE_ANDROID = 0x3142
    }
}

/** Draws a bitmap over the whole viewport: the bridge from a canvas-drawn frame to the encoder. */
private class TextureQuad {
    private val program: Int
    private val positionAttr: Int
    private val texCoordAttr: Int
    private val textureUniform: Int
    private val texture: Int
    private var allocated = false

    // x, y, s, t — bitmap row 0 is the top, so the top of the screen samples t = 0
    private val vertices: FloatBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f
        ))
        position(0)
    }

    init {
        program = link(
            """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() { gl_Position = aPosition; vTexCoord = aTexCoord; }
            """.trimIndent(),
            """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() { gl_FragColor = texture2D(uTexture, vTexCoord); }
            """.trimIndent()
        )
        positionAttr = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordAttr = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureUniform = GLES20.glGetUniformLocation(program, "uTexture")
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        texture = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun draw(bitmap: Bitmap) {
        GLES20.glViewport(0, 0, bitmap.width, bitmap.height)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        if (!allocated) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            allocated = true
        } else {
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap)
        }
        GLES20.glUniform1i(textureUniform, 0)
        vertices.position(0)
        GLES20.glVertexAttribPointer(positionAttr, 2, GLES20.GL_FLOAT, false, 16, vertices)
        GLES20.glEnableVertexAttribArray(positionAttr)
        vertices.position(2)
        GLES20.glVertexAttribPointer(texCoordAttr, 2, GLES20.GL_FLOAT, false, 16, vertices)
        GLES20.glEnableVertexAttribArray(texCoordAttr)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        val err = GLES20.glGetError()
        check(err == GLES20.GL_NO_ERROR) { "OpenGL hatası $err" }
    }

    private fun link(vertexSource: String, fragmentSource: String): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        check(ok[0] == GLES20.GL_TRUE) { "Gölgelendirici bağlanamadı: " + GLES20.glGetProgramInfoLog(p) }
        return p
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
        check(ok[0] != 0) { "Gölgelendirici derlenemedi: " + GLES20.glGetShaderInfoLog(shader) }
        return shader
    }
}
