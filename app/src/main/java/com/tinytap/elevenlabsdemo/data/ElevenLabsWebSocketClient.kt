package com.tinytap.elevenlabsdemo.data

import android.util.Log
import com.tinytap.elevenlabsdemo.data.model.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ElevenLabsWebSocketClient(
    private val websocketUrl: String,
    private val listener: Listener
) {
    interface Listener {
        fun onOpen() {}
        fun onUserTranscript(transcript: String) {}
        fun onAgentResponse(response: AgentResponseEvent.AgentResponse) {}
        fun onAudio(audioBase64: String, eventId: Int) {}
        fun onInterruption(reason: String) {}
        fun onPing(eventId: Int, pingMs: Long?) {}
        fun onPong(eventId: Int) {}
        fun onContextualUpdate(text: String) {}
        fun onConversationInitiationMetadata(metadata: ConversationInitiationMetadataEvent.ConversationInitiationMetadata) {}
        fun onClosing(code: Int, reason: String) {}
        fun onClosed(code: Int, reason: String) {}
        fun onFailure(t: Throwable, response: Response?) {}
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun <T : BaseEvent> sendEvent(event: T) {
        val jsonString = WebSocketEventJson.encodeToString(PolymorphicSerializer(BaseEvent::class), event)

        jsonString.chunked(4000).forEachIndexed {index, part ->
            Log.d("WebSocketClient", "Sending message ($index): $part")
        }

        webSocket?.send(jsonString)
    }

    fun sendAudioChunk(base64Chunk: String){
        val audioChunk = WebSocketEventJson.encodeToString(UserAudioChunkEvent(userAudioChunk = base64Chunk))
        audioChunk.chunked(5000).forEachIndexed {index, part ->
            Log.d("WebSocketClient", "Sending AudioChunk ($index): $part")
        }

        webSocket?.send(audioChunk)
    }

    fun connect() {
        Log.d("WebSocketClient", "Connecting to $websocketUrl")
        val request = Request.Builder()
            .url(websocketUrl)
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketClient", "WebSocket opened.")
                listener.onOpen()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketClient", "Received message: $text")
                handleEvent(text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("WebSocketClient", "Received binary message (not used)")
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "WebSocket closing (code=$code, reason=$reason)")
                listener.onClosing(code, reason)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "WebSocket closed (code=$code, reason=$reason)")
                listener.onClosed(code, reason)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketClient", "WebSocket failure: ${t.message}", t)
                listener.onFailure(t, response)
            }
        })
    }

    fun disconnect(code: Int = 1000, reason: String = "Client disconnect") {
        Log.d("WebSocketClient", "Disconnecting WebSocket (code=$code, reason=$reason)")
        webSocket?.close(code, reason)
    }

    private fun handleEvent(jsonString: String) {
        try {
            val event = WebSocketEventJson.decodeFromString(PolymorphicSerializer(BaseEvent::class), jsonString)
            when (event) {
                is UserTranscriptEvent -> listener.onUserTranscript(event.userTranscript.userTranscript)
                is AgentResponseEvent -> listener.onAgentResponse(event.agentResponse)
                is AudioEvent -> listener.onAudio(event.audioEvent.audioBase64, event.audioEvent.eventId)
                is InterruptionEvent -> listener.onInterruption(event.interruptionEvent.reason)
                is PingEvent -> listener.onPing(event.pingEvent.eventId, event.pingEvent.pingMs)
                is PongEvent -> listener.onPong(event.eventId)
                is ContextualUpdateEvent -> listener.onContextualUpdate(event.text)
                is ConversationInitiationMetadataEvent -> listener.onConversationInitiationMetadata(event.metadata)
                else -> Log.d("WebSocketClient", "Unknown event type or not handled: ${event}")
            }
        } catch (e: Exception) {
            Log.e("WebSocketClient", "Failed to parse event: ${e.message}", e)
        }
    }
}
