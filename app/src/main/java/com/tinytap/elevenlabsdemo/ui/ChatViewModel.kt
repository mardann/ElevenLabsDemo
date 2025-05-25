package com.tinytap.elevenlabsdemo.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinytap.elevenlabsdemo.audio.AudioPlayer
import com.tinytap.elevenlabsdemo.data.ElevenLabsWebSocketClient
import com.tinytap.elevenlabsdemo.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ChatUiModel {
    val messages: StateFlow<List<ChatMessage>>
    fun getUserInputAudioFormat(): String
    fun connect()
    fun disconnect()
    fun sendAudioMessage(base64: String)
}

class ChatViewModel(private val agentId: String) : ViewModel(), ChatUiModel {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages

    private var webSocketClient: ElevenLabsWebSocketClient? = null
    private var isConnected = false

    // Store the required audio format for user input (default: pcm_16000)
    private var userInputAudioFormat: String = "pcm_16000"
    override fun getUserInputAudioFormat(): String = userInputAudioFormat

    override fun connect() {
        if (isConnected) {
            Log.d("ChatViewModel", "Already connected to WebSocket.")
            return
        }
        Log.d("ChatViewModel", "Connecting to WebSocket with agentId: $agentId")
        webSocketClient = ElevenLabsWebSocketClient(agentId, object : ElevenLabsWebSocketClient.Listener {
            override fun onOpen() {
                isConnected = true
                Log.d("ChatViewModel", "WebSocket connection opened.")
            }
            override fun onUserTranscript(transcript: String) {
                Log.d("ChatViewModel", "Received user transcript: $transcript")
                viewModelScope.launch {
                    _messages.value += ChatMessage(transcript, Sender.USER)
                }
            }
            override fun onAgentResponse(response: AgentResponseEvent.AgentResponse) {
                Log.d("ChatViewModel", "Received agent response: $response")
                viewModelScope.launch {
                    _messages.value += ChatMessage(response.agentResponse, Sender.BOT)
                }
            }
            override fun onAudio(audioBase64: String, eventId: Int) {
                Log.d("ChatViewModel", "Received audio event (eventId=$eventId), base64 length: ${audioBase64.length}")
                AudioPlayer.playBase64Audio(audioBase64)
            }
            override fun onInterruption(reason: String) {
                Log.d("ChatViewModel", "Received interruption: $reason")
                // Optionally show interruption in chat or UI
            }
            override fun onPing(eventId: Int, pingMs: Long?) {
                Log.d("ChatViewModel", "Received ping (eventId=$eventId, pingMs=$pingMs), sending pong.")
                val pong = PongEvent(eventId = eventId)
                webSocketClient?.sendEvent(pong)
            }
            override fun onPong(eventId: Int) {
                Log.d("ChatViewModel", "Received pong (eventId=$eventId)")
            }
            override fun onContextualUpdate(text: String) {
                Log.d("ChatViewModel", "Received contextual update: $text")
                // Optionally show context update in chat or UI
            }
            override fun onClosing(code: Int, reason: String) {
                isConnected = false
                Log.d("ChatViewModel", "WebSocket closing (code=$code, reason=$reason)")
            }
            override fun onClosed(code: Int, reason: String) {
                isConnected = false
                Log.d("ChatViewModel", "WebSocket closed (code=$code, reason=$reason)")
            }
            override fun onFailure(t: Throwable, response: okhttp3.Response?) {
                isConnected = false
                Log.e("ChatViewModel", "WebSocket failure: ${t.message}", t)
            }
            override fun onConversationInitiationMetadata(metadata: ConversationInitiationMetadataEvent.ConversationInitiationMetadata) {
                Log.d("ChatViewModel", "Received conversation initiation metadata: $metadata")
                userInputAudioFormat = metadata.userInputAudioFormat
            }
        })
        webSocketClient?.connect()
    }



    override fun sendAudioMessage(base64: String) {
        Log.d("ChatViewModel", "Sending audio message, base64 length: ${base64.length}")
        AudioPlayer.playBase64Audio(base64)
        val event = UserAudioChunkEvent(userAudioChunk = base64)
        webSocketClient?.sendEvent(event)
    }

    override fun disconnect() {
        Log.d("ChatViewModel", "Disconnecting WebSocket.")
        webSocketClient?.disconnect()
        isConnected = false
    }

    fun clearChat() {
        Log.d("ChatViewModel", "Clearing chat history.")
        _messages.value = emptyList()
    }
}







