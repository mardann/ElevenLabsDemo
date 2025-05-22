package com.tinytap.elevenlabsdemo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log

object AudioPlayer {
    private var audioTrack: AudioTrack? = null

    fun playBase64Audio(base64: String) {
        try {
            val audioBytes = Base64.decode(base64, Base64.DEFAULT)
            // PCM 16bit, mono, 24kHz is a common TTS output, but you may need to adjust
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioTrack?.release()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
            audioTrack?.write(audioBytes, 0, audioBytes.size)
            Log.d("AudioPlayer", "Playing audio, bytes: ${audioBytes.size}")
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio: ${e.message}", e)
        }
    }

    fun release() {
        audioTrack?.release()
        audioTrack = null
    }
} 