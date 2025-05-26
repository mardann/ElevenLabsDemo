package com.yourcompany.elevenlabssdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ElevenLabsWebSocket(
    private val url: String,
    private val eventListener: (ElevenLabsEvent) -> Unit,
    private val errorListener: (Throwable) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Disable timeout for WebSocket
        .build()
    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true
        classDiscriminator = "type" }
    private val outgoing = Channel<ElevenLabsEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                // Start coroutine to send outgoing events
                scope.launch {
                    for (event in outgoing) {
                        try {
                            val jsonString = json.encodeToString(event)
                            ws.send(jsonString)
                        } catch (e: Exception) {
                            errorListener(e)
                        }
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val event = json.decodeFromString(ElevenLabsEvent.serializer(), text)
                    eventListener(event)
                } catch (e: SerializationException) {
                    errorListener(e)
                }
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                // Not expected for ElevenLabs protocol, but handle if needed
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                errorListener(t)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(code, reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                // Optionally notify about closure
            }
        })
    }

    fun send(event: ElevenLabsEvent) {
        scope.launch {
            outgoing.send(event)
        }
    }

    fun close(code: Int = 1000, reason: String = "Normal Closure") {
        webSocket?.close(code, reason)
        outgoing.close()
    }
}