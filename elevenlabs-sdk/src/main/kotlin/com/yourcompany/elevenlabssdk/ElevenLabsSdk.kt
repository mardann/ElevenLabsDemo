package com.yourcompany.elevenlabssdk

import kotlinx.serialization.Serializable

object ElevenLabsSdk {
    const val VERSION = "1.1.3"
    internal const val DEFAULT_API_ORIGIN = "wss://api.elevenlabs.io"
    internal const val DEFAULT_API_PATHNAME = "/v1/convai/conversation?agent_id="
    internal const val INPUT_SAMPLE_RATE = 16000.0
    internal const val SAMPLE_RATE = 16000.0
    internal const val IO_BUFFER_DURATION = 0.005
    internal const val VOLUME_UPDATE_INTERVAL = 0.1
    internal const val FADE_OUT_DURATION = 2.0
    internal const val BUFFER_SIZE = 1024

    // WebSocket message size limits
    internal const val MAX_WEBSOCKET_MESSAGE_SIZE = 1024 * 1024 // 1MB
    internal const val SAFE_MESSAGE_SIZE = 750 * 1024 // 750KB
    internal const val MAX_REQUESTED_MESSAGE_SIZE = 8 * 1024 * 1024 // 8MB

    fun arrayBufferToBase64(data: ByteArray): String =
        android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)

    fun base64ToArrayBuffer(base64: String): ByteArray =
        android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
}

@Serializable
data class SessionConfig(
    val agentId: String? = null,
    val signedUrl: String? = null,
    val overrides: ConversationConfigOverride? = null
)