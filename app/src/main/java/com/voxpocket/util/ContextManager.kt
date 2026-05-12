package com.voxpocket.util

import com.voxpocket.data.model.ChatMessage

class ContextManager(private val maxTokens: Int = 1800) {

    fun compressContext(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()

        systemPrompt?.let {
            result.add(ChatMessage("system", it))
        }

        if (messages.isEmpty()) return result

        var totalTokens = 0
        val compressedMessages = mutableListOf<ChatMessage>()

        for (msg in messages.reversed()) {
            val msgTokens = estimateTokens(msg.content)
            if (totalTokens + msgTokens > maxTokens) break

            compressedMessages.add(0, msg)
            totalTokens += msgTokens
        }

        result.addAll(compressedMessages)
        return result
    }

    private fun estimateTokens(text: String): Int {
        return text.length / 3 + text.codePointCount(0, text.length) / 2
    }
}

