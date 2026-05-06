package com.focusapp.ui.timer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import timber.log.Timber
import java.util.Random

/**
 * Generates ambient focus sounds (white noise / rain) programmatically via [AudioTrack].
 * No asset files required — all audio is synthesised at runtime.
 *
 * Lifecycle: call [setMode] to switch sounds; call [release] when done.
 */
class FocusAudioPlayer {

    enum class SoundMode { OFF, WHITE_NOISE, RAIN }

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(8192)

    @Volatile private var running = false
    private var audioTrack: AudioTrack? = null
    private var playerThread: Thread? = null
    private var currentMode = SoundMode.OFF

    fun setMode(mode: SoundMode) {
        if (mode == currentMode) return
        stopInternal()
        currentMode = mode
        if (mode != SoundMode.OFF) startInternal(mode)
    }

    fun release() {
        stopInternal()
        currentMode = SoundMode.OFF
    }

    // ---- internals ----

    private fun startInternal(mode: SoundMode) {
        running = true
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
        } catch (e: Exception) {
            Timber.e(e, "FocusAudioPlayer: failed to initialise AudioTrack")
            running = false
            return
        }

        playerThread = Thread {
            val buf = ShortArray(bufferSize / 2)
            val rng = Random()
            // Brown noise state (Paul Kellet's pink/brown approximation)
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0
            var b3 = 0.0; var b4 = 0.0; var b5 = 0.0

            while (running) {
                for (i in buf.indices) {
                    val white = rng.nextGaussian()
                    buf[i] = when (mode) {
                        SoundMode.WHITE_NOISE -> {
                            (white * 7000).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        SoundMode.RAIN -> {
                            // 6-pole IIR low-pass for brown/rain noise character
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = (b0 + b1 + b2 + b3 + b4 + b5 + white * 0.5362)
                            (pink * 3500).toInt().coerceIn(-32767, 32767).toShort()
                        }
                        SoundMode.OFF -> 0
                    }
                }
                audioTrack?.write(buf, 0, buf.size)
            }
            Timber.d("FocusAudioPlayer: generator thread exiting")
        }.also {
            it.name = "focus-audio"
            it.isDaemon = true
            it.start()
        }
        Timber.d("FocusAudioPlayer: started mode=$mode")
    }

    private fun stopInternal() {
        running = false
        playerThread?.join(400)
        playerThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) { }
        audioTrack = null
        Timber.d("FocusAudioPlayer: stopped")
    }
}
