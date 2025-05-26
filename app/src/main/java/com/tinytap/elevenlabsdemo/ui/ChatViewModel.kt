package com.tinytap.elevenlabsdemo.ui

import android.provider.SyncStateContract
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tinytap.elevenlabsdemo.audio.AudioPlayer
import com.tinytap.elevenlabsdemo.data.AGENT_ID_EDDIE
import com.tinytap.elevenlabsdemo.data.API_KEY
import com.tinytap.elevenlabsdemo.data.ElevenLabsWebSocketClient
import com.tinytap.elevenlabsdemo.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import kotlinx.serialization.ExperimentalSerializationApi

interface ChatUiModel {
    val messages: StateFlow<List<ChatMessage>>
    fun getUserInputAudioFormat(): String
    fun connect()
    fun disconnect()
    fun sendAudioMessage(base64: String)
}

class ChatViewModel : ViewModel, ChatUiModel {
    constructor(agentId: String) : super() {
        this.agentId = agentId
    }
    private val agentId: String
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: StateFlow<List<ChatMessage>> = _messages

    private var webSocketClient: ElevenLabsWebSocketClient? = null
    private var isConnected = false
    private var userInputAudioFormat: String = "pcm_16000"
    override fun getUserInputAudioFormat(): String = userInputAudioFormat


    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.elevenlabs.io/")
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    private val api by lazy { retrofit.create(ElevenLabsApi::class.java) }

    override fun connect() {
        if (isConnected) {
            Log.d("ChatViewModel", "Already connected to WebSocket.")
            return
        }
        Log.d("ChatViewModel", "Fetching signed WebSocket URL...")
        viewModelScope.launch {
            try {
                val response = api.getSignedUrl(
                    apiKey = API_KEY,
                    agentId = AGENT_ID_EDDIE
                )
                val signedUrl = response.signedUrl
                Log.d("ChatViewModel", "Connecting to WebSocket with signed URL: $signedUrl")
                webSocketClient = ElevenLabsWebSocketClient(signedUrl, object : ElevenLabsWebSocketClient.Listener {
                    override fun onOpen() {
                        webSocketClient?.sendEvent(ConversationInitiationClientData(
                            conversationConfigOverride = ConversationInitiationClientData.ConversationConfigOverride(
//                                agent = ConversationInitiationClientData.ConversationConfigOverride.Agent(
//                                    language = "en-US",
//                                    prompt = ConversationInitiationClientData.ConversationConfigOverride.Agent.Prompt(
//                                        prompt = "You are a helpful tutor named Eddy, talking to a young child. you're goal is to help them learn in a fun and friendly manner.",
//                                    ), firstMessage = "Hey there, I'm Eddy from TinyTap, What is your name?\n"
//                                )
                            )
                        ))
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
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to fetch signed WebSocket URL: ${e.message}", e)
            }
        }
    }

    override fun sendAudioMessage(base64: String) {
        Log.d("ChatViewModel", "Sending audio message, base64 length: ${base64.length}")
        AudioPlayer.playBase64Audio(base64)
        webSocketClient?.sendAudioChunk(base64)


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

interface ElevenLabsApi {
    @GET("v1/convai/conversation/get_signed_url")
    suspend fun getSignedUrl(
        @Header("xi-api-key") apiKey: String,
        @Query("agent_id") agentId: String
    ): SignedUrlResponse
}

@Serializable
data class SignedUrlResponse(
    @SerialName("signed_url") val signedUrl: String
)







