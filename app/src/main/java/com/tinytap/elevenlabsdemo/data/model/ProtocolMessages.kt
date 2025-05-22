package com.tinytap.elevenlabsdemo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ConversationRequest(
    val text: String,
    val history: List<ConversationHistory>? = null
)

@Serializable
data class ConversationHistory(
    val role: String, // "user" or "assistant"
    val content: String
)

@Serializable
data class ConversationResponse(
    val response: String
)