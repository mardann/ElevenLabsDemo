package com.tinytap.elevenlabsdemo.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed class BaseEvent (
)

@Serializable
@SerialName("user_transcript")
data class UserTranscriptEvent(
    @SerialName("user_transcript") val userTranscript: String
) : BaseEvent() {
//    override val type: String = "user_transcript"
}

@Serializable
@SerialName("agent_response")
data class AgentResponseEvent(
    @SerialName("agent_response_event") val agentResponse: AgentResponse
) : BaseEvent() {
//    override val type: String = "agent_response"
    @Serializable
    data class AgentResponse(
    @SerialName("agent_response")
    val agentResponse: String)
}

@Serializable
@SerialName("audio")
data class AudioEvent(
    @SerialName("audio_event") val audioEvent: AudioEventData
) : BaseEvent() {
    @Serializable
    data class AudioEventData(
        @SerialName("audio_base_64") val audioBase64: String,
        @SerialName("event_id") val eventId: Int
    )
}



@Serializable
@SerialName("interruption")
data class InterruptionEvent(
    @SerialName("interruption_event") val interruptionEvent: InterruptionEventData
) : BaseEvent() {

    @Serializable
    data class InterruptionEventData(
        @SerialName("reason") val reason: String
    )
}



@Serializable
@SerialName("ping")
data class PingEvent(
    @SerialName("ping_event") val pingEvent: PingEventData
) : BaseEvent() {
    //    override val type: String = "ping"
    @Serializable
    data class PingEventData(
        @SerialName("event_id") val eventId: Int,
        @SerialName("ping_ms") val pingMs: Long? = null
    )
}



@Serializable
@SerialName("pong")
data class PongEvent(
    @SerialName("event_id") val eventId: Int


) :BaseEvent()



@Serializable
@SerialName("contextual_update")
data class ContextualUpdateEvent(
    @SerialName("text") val text: String
) : BaseEvent()

@Serializable
@SerialName("conversation_initiation_metadata")
data class ConversationInitiationMetadataEvent(
    @SerialName("conversation_initiation_metadata_event") val metadata: ConversationInitiationMetadata
) : BaseEvent() {
//    override val type: String = "conversation_initiation_metadata"

    @Serializable
    data class ConversationInitiationMetadata(
        @SerialName("conversation_id") val conversationId: String,
        @SerialName("agent_output_audio_format") val agentOutputAudioFormat: String,
        @SerialName("user_input_audio_format") val userInputAudioFormat: String
    )
}


@Serializable
@SerialName("")
data class UserAudioChunkEvent(
    @SerialName("user_audio_chunk") val userAudioChunk: String,
) :BaseEvent()


val WebSocketEventSerialModule = SerializersModule {
    polymorphic(BaseEvent::class) {
        subclass(UserTranscriptEvent::class)
        subclass(AgentResponseEvent::class)
        subclass(AudioEvent::class)
        subclass(InterruptionEvent::class)
        subclass(PingEvent::class)
        subclass(PongEvent::class)
        subclass(ContextualUpdateEvent::class)
        subclass(UserAudioChunkEvent::class)
        subclass(ConversationInitiationMetadataEvent::class)
    }
}

val WebSocketEventJson = Json {
    serializersModule = WebSocketEventSerialModule
    classDiscriminator = "type"
    ignoreUnknownKeys = true
}

