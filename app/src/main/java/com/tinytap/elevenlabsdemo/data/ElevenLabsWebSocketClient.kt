package com.tinytap.elevenlabsdemo.data

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ElevenLabsWebSocketClient(
    private val agentId: String,
    private val listener: Listener
) {
    interface Listener {
        fun onOpen() {}
        fun onUserTranscript(transcript: String) {}
        fun onAgentResponse(response: String) {}
        fun onAudio(audioBase64: String, eventId: Int) {}
        fun onInterruption(reason: String) {}
        fun onPing(eventId: Int, pingMs: Long?) {}
        fun onPong(eventId: Int) {}
        fun onContextualUpdate(text: String) {}
        fun onClosing(code: Int, reason: String) {}
        fun onClosed(code: Int, reason: String) {}
        fun onFailure(t: Throwable, response: Response?) {}
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect() {
        val url = "wss://api.elevenlabs.io/v1/convai/conversation?agent_id=$agentId"
        Log.d("WebSocketClient", "Connecting to $url")
        val request = Request.Builder()
            .url(url)
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

    fun send(event: String) {
        Log.d("WebSocketClient", "Sending message: $event")
        webSocket?.send(event)
    }

    fun disconnect(code: Int = 1000, reason: String = "Client disconnect") {
        Log.d("WebSocketClient", "Disconnecting WebSocket (code=$code, reason=$reason)")
        webSocket?.close(code, reason)
    }

    private fun handleEvent(json: String) {
        val type = Regex(""""type"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.getOrNull(1)
        Log.d("WebSocketClient", "Handling event of type: $type")
        when (type) {
            "user_transcript" -> {
                val transcript = Regex("""user_transcript"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.getOrNull(1)
                Log.d("WebSocketClient", "user_transcript: $transcript")
                transcript?.let { listener.onUserTranscript(it) }
            }
            "agent_response" -> {
                val response = Regex("""agent_response"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.getOrNull(1)
                Log.d("WebSocketClient", "agent_response: $response")
                response?.let { listener.onAgentResponse(it) }
            }
            "audio" -> {
                val audio = Regex("""audio_base_64"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.getOrNull(1)
                val eventId = Regex("""event_id"\s*:\s*(\d+)""").find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                Log.d("WebSocketClient", "audio event: eventId=$eventId, base64 length=${audio?.length}")
                if (audio != null) listener.onAudio(audio, eventId)
            }
            "interruption" -> {
                val reason = Regex("""reason"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.getOrNull(1)
                Log.d("WebSocketClient", "interruption: $reason")
                reason?.let { listener.onInterruption(it) }
            }
            "ping" -> {
                val eventId = Regex("""event_id"\s*:\s*(\d+)""").find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val pingMs = Regex("""ping_ms"\s*:\s*(\d+)""").find(json)?.groupValues?.getOrNull(1)?.toLongOrNull()
                Log.d("WebSocketClient", "ping: eventId=$eventId, pingMs=$pingMs")
                listener.onPing(eventId, pingMs)
            }
            "pong" -> {
                val eventId = Regex("""event_id"\s*:\s*(\d+)""").find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                Log.d("WebSocketClient", "pong: eventId=$eventId")
                listener.onPong(eventId)
            }
            "contextual_update" -> {
                val text = Regex("""text"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.getOrNull(1)
                Log.d("WebSocketClient", "contextual_update: $text")
                text?.let { listener.onContextualUpdate(it) }
            }
            else -> {
                Log.d("WebSocketClient", "Unknown event type or not handled: $type")
            }
        }
    }
}
