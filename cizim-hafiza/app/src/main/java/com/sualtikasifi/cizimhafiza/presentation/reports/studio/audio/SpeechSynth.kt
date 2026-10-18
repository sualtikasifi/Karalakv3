package com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Karalak's voice: the phone's own Turkish text-to-speech, written to files so the lines can be
 * timed, lip-synced and mixed into the video. Free and offline once the Turkish voice is on the
 * phone; [open] says when it is not.
 */
class SpeechSynth private constructor(private val tts: TextToSpeech) {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { utteranceId?.let { pending.remove(it)?.complete(true) } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { utteranceId?.let { pending.remove(it)?.complete(false) } }
            override fun onError(utteranceId: String?, errorCode: Int) { utteranceId?.let { pending.remove(it)?.complete(false) } }
        })
    }

    /** Pitch and pace of the character; a touch high and quick reads as a lively young voice. */
    fun tune(pitch: Float, rate: Float) {
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)
    }

    /** Writes [text] as a WAV file and reads it back; null when the engine could not say it. */
    suspend fun synth(text: String, dir: File): Pcm? {
        val id = UUID.randomUUID().toString()
        val file = File(dir, "line_$id.wav")
        val done = CompletableDeferred<Boolean>()
        pending[id] = done
        val queued = tts.synthesizeToFile(text, Bundle(), file, id)
        if (queued != TextToSpeech.SUCCESS) { pending.remove(id); return null }
        val ok = runCatching { withTimeout(30_000) { done.await() } }.getOrDefault(false)
        if (!ok || !file.exists()) return null
        return runCatching { WavReader.parse(file.readBytes()) }.getOrNull().also { file.delete() }
    }

    /** Says [text] out loud on the phone's speaker, to try the voice before recording. */
    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
    }

    fun close() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }

    companion object {
        /** Opens the engine with Turkish selected, or explains why it cannot. */
        suspend fun open(context: Context): Result<SpeechSynth> {
            var engine: TextToSpeech? = null
            val status = suspendCancellableCoroutine { cont ->
                engine = TextToSpeech(context.applicationContext) { s -> if (cont.isActive) cont.resume(s) }
                cont.invokeOnCancellation { runCatching { engine?.shutdown() } }
            }
            val tts = engine ?: return Result.failure(IllegalStateException("Seslendirme açılamadı"))
            if (status != TextToSpeech.SUCCESS) {
                runCatching { tts.shutdown() }
                return Result.failure(IllegalStateException("Telefonun seslendirme motoru açılamadı"))
            }
            val lang = tts.setLanguage(Locale("tr", "TR"))
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                runCatching { tts.shutdown() }
                return Result.failure(
                    IllegalStateException("Türkçe ses yüklü değil. Ayarlar → Sistem → Diller → Metin okuma çıkışı → Google'dan Türkçe sesi indir.")
                )
            }
            return Result.success(SpeechSynth(tts))
        }
    }
}
