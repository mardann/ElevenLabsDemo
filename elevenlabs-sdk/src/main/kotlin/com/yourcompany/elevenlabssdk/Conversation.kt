package com.yourcompany.elevenlabssdk

import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable

/**
 * Callbacks for conversation events.
 */
class ConversationCallbacks {
    var onConnect: ((conversationId: String) -> Unit)? = null
    var onMessage: ((message: String, role: String) -> Unit)? = null
    var onError: ((error: Throwable, info: String?) -> Unit)? = null
    var onStatusChange: ((status: ConversationStatus) -> Unit)? = null
    var onModeChange: ((mode: ConversationMode) -> Unit)? = null
}

/**
 * Conversation status.
 */
enum class ConversationStatus {
    CONNECTING, CONNECTED, DISCONNECTED, ERROR
}

/**
 * Conversation mode (expand as needed).
 */
enum class ConversationMode {
    IDLE, LISTENING, SPEAKING
}


class Conversation private constructor(
    private val config: SessionConfig,
    private val callbacks: ConversationCallbacks
) {
    private var webSocket: ElevenLabsWebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        suspend fun startSession(
            config: SessionConfig,
            callbacks: ConversationCallbacks
        ): Conversation {
            val conversation = Conversation(config, callbacks)
            conversation.connect()
            return conversation
        }
    }

    private fun connect() {
        val url = config.signedUrl ?: run {
            requireNotNull(config.agentId) { "Either signedUrl or agentId must be provided" }
            "${ElevenLabsSdk.DEFAULT_API_ORIGIN}${ElevenLabsSdk.DEFAULT_API_PATHNAME}${config.agentId}"
        }

        webSocket = ElevenLabsWebSocket(
            url = url,
            eventListener = { event -> handleEvent(event) },
            errorListener = { error -> callbacks.onError?.invoke(error, null) }
        )
        callbacks.onStatusChange?.invoke(ConversationStatus.CONNECTING)
        webSocket?.connect()
    }

    private fun handleEvent(event: ElevenLabsEvent) {
        when (event) {
            is ElevenLabsEvent.ConversationInitiationMetadata -> {
                callbacks.onConnect?.invoke(event.conversationId ?: "")
                callbacks.onStatusChange?.invoke(ConversationStatus.CONNECTED)
            }
            is ElevenLabsEvent.Message -> {
                callbacks.onMessage?.invoke(event.message, event.role)
            }
            is ElevenLabsEvent.Error -> {
                callbacks.onError?.invoke(Exception(event.error), event.info)
                callbacks.onStatusChange?.invoke(ConversationStatus.ERROR)
            }
            is ElevenLabsEvent.AgentSpeechStart -> {
                callbacks.onModeChange?.invoke(ConversationMode.SPEAKING)
            }
            is ElevenLabsEvent.AgentSpeechEnd -> {
                callbacks.onModeChange?.invoke(ConversationMode.IDLE)
            }
            is ElevenLabsEvent.AgentThinking -> {
                callbacks.onModeChange?.invoke(ConversationMode.IDLE)
            }
            is ElevenLabsEvent.AgentReady,
            is ElevenLabsEvent.WaitingForUser -> {
                callbacks.onModeChange?.invoke(ConversationMode.LISTENING)
            }
            is ElevenLabsEvent.ConversationEnd -> {
                callbacks.onStatusChange?.invoke(ConversationStatus.DISCONNECTED)
            }
            // Add more event handling as needed
            else -> { /* Optionally log or handle other events */ }
        }
    }

    fun endSession() {
        webSocket?.close()
        callbacks.onStatusChange?.invoke(ConversationStatus.DISCONNECTED)
    }

    fun sendEvent(event: ElevenLabsEvent) {
        webSocket?.send(event)
    }

    private var recorder: VoiceRecorder? = null


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        recorder = VoiceRecorder()
        recorder?.start { audioChunk ->
            // Encode to base64 and send as user audio event
            val base64Audio = ElevenLabsSdk.arrayBufferToBase64(audioChunk)
            val userAudioEvent = ElevenLabsEvent.UserAudioEvent(audio = base64Audio)
            sendEvent(userAudioEvent)
        }
        callbacks.onModeChange?.invoke(ConversationMode.LISTENING)
    }

    fun stopRecording() {
        recorder?.stop()
        // Optionally send a user_audio_end event
        sendEvent(ElevenLabsEvent.UserAudioEndEvent())
        callbacks.onModeChange?.invoke(ConversationMode.IDLE)
    }
}