package com.sualtikasifi.cizimhafiza.presentation.reports.studio.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteOrder

/** One encoded AAC frame, kept in memory until the video's muxer is ready for it. */
class AudioSample(val data: ByteArray, val presentationTimeUs: Long, val flags: Int)

/** An encoded sound track: its format (with the codec-specific data a muxer needs) and its frames. */
class EncodedAudio(val format: MediaFormat, val samples: List<AudioSample>)

/**
 * Encodes a mono PCM track to AAC-LC on the phone's own encoder. Done before the video so the
 * muxer can be given both tracks the moment the video encoder reports its format. A studio voice
 * track is a few hundred kilobytes, so holding it in memory is fine.
 */
object AacEncoder {
    private const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC

    fun encode(pcm: Pcm, bitRate: Int = 128_000): EncodedAudio {
        val format = MediaFormat.createAudioFormat(MIME, pcm.sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val codec = MediaCodec.createEncoderByType(MIME)
        val out = ArrayList<AudioSample>()
        var outFormat: MediaFormat? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var pos = 0
            var inputDone = false
            var idle = 0
            while (true) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(5_000)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex) ?: error("Ses kodlayıcısı girdi vermedi")
                        buffer.clear()
                        buffer.order(ByteOrder.nativeOrder())
                        val n = minOf(pcm.samples.size - pos, buffer.remaining() / 2)
                        val pts = pos * 1_000_000L / pcm.sampleRate
                        if (n <= 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            for (i in 0 until n) buffer.putShort(pcm.samples[pos + i])
                            codec.queueInputBuffer(inIndex, 0, n * 2, pts, 0)
                            pos += n
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 5_000)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> if (inputDone && ++idle > 1_000) error("Ses kodlayıcısı bitmedi")
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = codec.outputFormat
                    outIndex >= 0 -> {
                        idle = 0
                        val buffer = codec.getOutputBuffer(outIndex) ?: error("Ses kodlayıcısı boş çıktı verdi")
                        val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        if (!isConfig && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val bytes = ByteArray(info.size)
                            buffer.get(bytes)
                            out += AudioSample(bytes, info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM.inv())
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
        return EncodedAudio(outFormat ?: error("Ses biçimi alınamadı"), out)
    }
}
