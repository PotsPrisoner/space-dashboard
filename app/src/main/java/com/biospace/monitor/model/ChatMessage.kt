package com.biospace.monitor.model

data class ChatMessage(
    val id: String = "",
    val nick: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val uid: String = "",
    val mine: Boolean = false,
    val isSystem: Boolean = false
)
