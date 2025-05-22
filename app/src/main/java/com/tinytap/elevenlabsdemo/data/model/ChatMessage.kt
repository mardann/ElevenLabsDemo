package com.tinytap.elevenlabsdemo.data.model

enum class Sender { USER, BOT }

data class ChatMessage(
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis()
)