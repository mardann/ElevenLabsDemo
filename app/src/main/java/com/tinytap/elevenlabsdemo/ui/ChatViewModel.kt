package com.tinytap.elevenlabsdemo.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinytap.elevenlabsdemo.audio.AudioPlayer
import com.tinytap.elevenlabsdemo.data.ElevenLabsWebSocketClient
import com.tinytap.elevenlabsdemo.data.model.ChatMessage
import com.tinytap.elevenlabsdemo.data.model.Sender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatViewModel(private val agentId: String) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var webSocketClient: ElevenLabsWebSocketClient? = null
    private var isConnected = false

    fun connect() {
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
            override fun onAgentResponse(response: String) {
                Log.d("ChatViewModel", "Received agent response: $response")
                viewModelScope.launch {
                    _messages.value += ChatMessage(response, Sender.BOT)
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
                val pong = PongEvent(event_id = eventId)
                val json = Json.encodeToString(pong)
                webSocketClient?.send(json)
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
        })
        webSocketClient?.connect()
    }

    fun sendMessage(text: String) {
        Log.d("ChatViewModel", "Submitting user message: $text")
        _messages.value += ChatMessage(text, Sender.USER)
        val event = UserTranscriptEvent(userTranscript = text)
        val json = Json.encodeToString(event)
        Log.d("ChatViewModel", "Sending user_transcript event: $json")
        webSocketClient?.send(json)
    }

    fun disconnect() {
        Log.d("ChatViewModel", "Disconnecting WebSocket.")
        webSocketClient?.disconnect()
        isConnected = false
    }

    fun clearChat() {
        Log.d("ChatViewModel", "Clearing chat history.")
        _messages.value = emptyList()
    }
}

@Serializable
data class UserTranscriptEvent(
    val type: String = "user_transcript",
    @SerialName("user_transcript")
    val userTranscript: String
)

@Serializable
data class PongEvent(
    val type: String = "pong",
    val event_id: Int
) 