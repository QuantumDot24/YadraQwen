package com.complexsoft.yadraqwen.data

import java.util.UUID

enum class MessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val isStreaming: Boolean = false,
    val thinkingContent: String? = null,
    val isThinkingComplete: Boolean = false,
    val thinkingDurationSec: Int = 0,
    val isThinkingEnabled: Boolean = false,  // ← nuevo
    val timestamp: Long = System.currentTimeMillis()
)

fun parseModelResponse(raw: String): Triple<String?, Boolean, String> {
    val openTag  = "<think>"
    val closeTag = "</think>"

    val hasOpen  = raw.contains(openTag)
    val hasClose = raw.contains(closeTag)

    return when {
        hasOpen && hasClose -> {
            val openIdx  = raw.indexOf(openTag) + openTag.length
            val closeIdx = raw.indexOf(closeTag)
            val thinking = raw.substring(openIdx, closeIdx).trim().ifBlank { null }
            val response = raw.substring(closeIdx + closeTag.length).trim()
            Triple(thinking, true, response)
        }
        hasOpen && !hasClose -> {
            val openIdx = raw.indexOf(openTag) + openTag.length
            val thinking = raw.substring(openIdx).ifBlank { null }
            Triple(thinking, false, "")
        }
        else -> Triple(null, false, raw)
    }
}