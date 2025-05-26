package com.yourcompany.elevenlabssdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@JsonClassDiscriminator("type")
@Serializable
sealed class ElevenLabsEvent {

    @Serializable
    @SerialName("conversation_initiation_metadata")
    data class ConversationInitiationMetadata(
        @SerialName("conversation_id") val conversationId: String? = null,
        @SerialName("sample_rate") val sampleRate: Int? = null,
        @SerialName("format") val format: String? = null,
        @SerialName("agent_id") val agentId: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("message")
    data class Message(
        val message: String,
        val role: String,
        @SerialName("conversation_id") val conversationId: String? = null,
        @SerialName("message_id") val messageId: String? = null,
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("error")
    data class Error(
        val error: String,
        val info: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("audio_chunk")
    data class AudioChunk(
        @SerialName("audio") val audio: String, // base64-encoded audio
        @SerialName("is_final") val isFinal: Boolean? = null,
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_speech_end")
    data class AgentSpeechEnd(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_speech_start")
    data class AgentSpeechStart(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_thinking")
    data class AgentThinking(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_ready")
    data class AgentReady(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("waiting_for_user")
    data class WaitingForUser(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("conversation_end")
    data class ConversationEnd(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_interrupted")
    data class AgentInterrupted(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_muted")
    data class AgentMuted(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_unmuted")
    data class AgentUnmuted(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_typing")
    data class AgentTyping(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("agent_idle")
    data class AgentIdle(
        @SerialName("timestamp") val timestamp: String? = null
    ) : ElevenLabsEvent()


    @Serializable
    @SerialName("user_audio")
    data class UserAudioEvent(
        val audio: String // base64-encoded PCM
    ) : ElevenLabsEvent()

    @Serializable
    @SerialName("user_audio_end")
    class UserAudioEndEvent : ElevenLabsEvent()
}