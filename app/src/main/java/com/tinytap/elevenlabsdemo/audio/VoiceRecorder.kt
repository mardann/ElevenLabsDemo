package com.tinytap.elevenlabsdemo.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

object VoiceRecorder {
    val TAG = this::class.java.simpleName
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordedData: ByteArrayOutputStream? = null
    private var sampleRate = 16_000
    private var audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var channelConfig = AudioFormat.CHANNEL_IN_MONO

    private val maxOutPutSize = 7000

    private var recordingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var isMuted = AtomicBoolean(true)

    @SuppressLint("MissingPermission")
    fun startRecording(format: String = "pcm_16000", sendAudioChunk: (String) -> Unit) {
        Log.d(TAG, "startRecording: ")
        stopRecording()
        when (format) {
            "pcm_16000" -> {
                sampleRate = 16000
                audioEncoding = AudioFormat.ENCODING_PCM_16BIT
                channelConfig = AudioFormat.CHANNEL_IN_MONO
            }
            // Add more formats as needed
            else -> {
                sampleRate = 16000
                audioEncoding = AudioFormat.ENCODING_PCM_16BIT
                channelConfig = AudioFormat.CHANNEL_IN_MONO
            }
        }
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioEncoding,
            minBufferSize
        )

        recordedData = ByteArrayOutputStream()
        isRecording = true
        audioRecord?.startRecording()

        recordingJob = scope.launch{
            val buffer = ByteArray(minBufferSize)
            while (isRecording) {

                val read: Int = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {


                    Log.d(TAG, "startRecording: write chunk size - $read")

                    if(isMuted.get()){
                        Log.d(TAG, "startRecording: muted")
                        recordedData?.write(ByteArray(read), 0, read )
                    }else{
                        recordedData?.write(buffer, 0, read)
                    }



                    if(recordedData?.size()!! > maxOutPutSize){
                        val toByteArray = recordedData?.toByteArray()
                        val base64 = Base64.encodeToString(toByteArray, Base64.NO_WRAP)
                        Log.d(TAG, "startRecording: send chunk size - ${toByteArray?.size}")
                        sendAudioChunk(base64)
                        recordedData?.reset()
                    }

                }
            }
        }

    }

    fun stopRecording() {
        recordingJob?.cancel()
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("VoiceRecorder", "stopRecording: Error stopping AudioRecord: ${e.message}", e)
        }
        audioRecord = null

        Log.d("VoiceRecorder", "Stopped recording")
    }

//    suspend fun getBase64Audio(): String? {
//        recordingJob?.cancel()
//        isRecording = false
//        try {
//            audioRecord?.stop()
//            audioRecord?.release()
//        } catch (e: Exception) {
//            Log.w("VoiceRecorder", "getBase64Audio: Error stopping AudioRecord: ${e.message}", e)
//        }
//
//
//
//        val pcmBytes = recordedData?.toByteArray() ?: return null
//        val base64 = withContext(Dispatchers.Default) {
//
//
//
//            Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
//        }
//        recordedData?.reset()
//        recordedData = null
//        Log.d("VoiceRecorder", "Returning base64 audio, bytes: ${pcmBytes.size}")
//        return base64
//    }


} 