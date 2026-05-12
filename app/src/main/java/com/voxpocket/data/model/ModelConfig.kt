package com.voxpocket.data.model

data class ModelConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 1024,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val systemPrompt: String? = null
)

