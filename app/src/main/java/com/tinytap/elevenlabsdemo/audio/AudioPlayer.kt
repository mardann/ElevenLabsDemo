package com.tinytap.elevenlabsdemo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AudioPlayer {
    private var audioTrack: AudioTrack? = null

    private var completionJob: Job? = null


    fun playBase64Audio(base64: String, scope: CoroutineScope, completed: () -> Unit) {
        try {

            if(completionJob?.isActive == true){
                Log.d("AudioPlayer", "playBase64Audio: cancle pending completed() call. new bit arrived")
                completionJob?.cancel()
            }
            val audioBytes = Base64.decode(base64, Base64.NO_WRAP)


            // PCM 16bit, mono, 24kHz is a common TTS output, but you may need to adjust
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            val durationMillis = (audioBytes.size / 2f) / sampleRate * 1000
            Log.d("AudioPlayer", "playBase64Audio: calculated duration: $durationMillis")
            completionJob = scope.launch() {
                delay(durationMillis.toLong() + 300)
                completed()
            }

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

//            Log.d("AudioPlayer", "Playing audio, bytes: ${audioBytes.size}")
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Failed to play audio: ${e.message}", e)
        }
    }

    fun release() {
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
    }
} 