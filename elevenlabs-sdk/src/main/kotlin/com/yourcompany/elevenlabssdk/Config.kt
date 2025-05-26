package com.yourcompany.elevenlabssdk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Language {
    @SerialName("en") EN,
    @SerialName("ja") JA,
    @SerialName("zh") ZH,
    @SerialName("de") DE,
    @SerialName("hi") HI,
    @SerialName("fr") FR,
    @SerialName("ko") KO,
    @SerialName("pt") PT,
    @SerialName("it") IT,
    @SerialName("es") ES,
    @SerialName("id") ID,
    @SerialName("nl") NL,
    @SerialName("tr") TR,
    @SerialName("pl") PL,
    @SerialName("sv") SV,
    @SerialName("bg") BG,
    @SerialName("ro") RO,
    @SerialName("ar") AR,
    @SerialName("cs") CS,
    @SerialName("el") EL,
    @SerialName("fi") FI,
    @SerialName("ms") MS,
    @SerialName("da") DA,
    @SerialName("ta") TA,
    @SerialName("uk") UK,
    @SerialName("ru") RU,
    @SerialName("hu") HU,
    @SerialName("no") NO,
    @SerialName("vi") VI
}

@Serializable
data class AgentPrompt(
    val prompt: String? = null
)

@Serializable
data class TTSConfig(
    @SerialName("voice_id") val voiceId: String? = null
)

@Serializable
data class AgentConfig(
    val prompt: AgentPrompt? = null,
    @SerialName("first_message") val firstMessage: String? = null,
    val language: Language? = null
)

@Serializable
data class ConversationConfigOverride(
    val agent: AgentConfig? = null,
    val tts: TTSConfig? = null
)