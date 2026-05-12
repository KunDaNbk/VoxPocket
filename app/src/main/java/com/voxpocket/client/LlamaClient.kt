package com.voxpocket.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonNull
import com.voxpocket.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object LlamaHttpClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}

sealed class StreamChunk {
    data class Thinking(val content: String) : StreamChunk()
    data class FinalAnswer(val content: String) : StreamChunk()
}

class ReasoningParser {
    private val thinkingBuffer = StringBuilder()
    private val answerBuffer = StringBuilder()
    private var isInThinkingMode = false
    
    private val thinkStartTag = "<think>"
    private val thinkEndTag = "</think>"

    fun parseChunk(chunk: String): List<StreamChunk> {
        val result = mutableListOf<StreamChunk>()
        var remaining = chunk
        
        while (remaining.isNotEmpty()) {
            if (!isInThinkingMode) {
                val thinkStart = remaining.indexOf(thinkStartTag)
                if (thinkStart != -1) {
                    val beforeThink = remaining.substring(0, thinkStart)
                    if (beforeThink.isNotEmpty()) {
                        answerBuffer.append(beforeThink)
                        result.add(StreamChunk.FinalAnswer(beforeThink))
                    }
                    isInThinkingMode = true
                    remaining = remaining.substring(thinkStart + thinkStartTag.length)
                } else {
                    answerBuffer.append(remaining)
                    result.add(StreamChunk.FinalAnswer(remaining))
                    remaining = ""
                }
            } else {
                val thinkEnd = remaining.indexOf(thinkEndTag)
                if (thinkEnd != -1) {
                    val thinkingContent = remaining.substring(0, thinkEnd)
                    thinkingBuffer.append(thinkingContent)
                    result.add(StreamChunk.Thinking(thinkingContent))
                    isInThinkingMode = false
                    remaining = remaining.substring(thinkEnd + thinkEndTag.length)
                } else {
                    thinkingBuffer.append(remaining)
                    result.add(StreamChunk.Thinking(remaining))
                    remaining = ""
                }
            }
        }

        return result
    }

    fun getFullThinking(): String = thinkingBuffer.toString()
    fun getFullAnswer(): String = answerBuffer.toString()
    fun getFullResponse(): String = getFullThinking() + getFullAnswer()

    fun reset() {
        isInThinkingMode = false
        thinkingBuffer.clear()
        answerBuffer.clear()
    }
}

class LlamaClient(
    private val port: Int = 8080,
    private val logger: ((String) -> Unit)? = null
) {
    private val gson = Gson()
    private val thinkStartTag = "<think>"
    private val thinkEndTag = "</think>"

    private fun log(message: String) {
        logger?.invoke(message)
    }
    
    private fun filterThinkTags(content: String): Pair<String, String> {
        var remaining = content
        val thinkingBuilder = StringBuilder()
        val answerBuilder = StringBuilder()
        var isInThinking = false
        
        while (remaining.isNotEmpty()) {
            if (!isInThinking) {
                val startIdx = remaining.indexOf(thinkStartTag)
                if (startIdx != -1) {
                    answerBuilder.append(remaining.substring(0, startIdx))
                    remaining = remaining.substring(startIdx + thinkStartTag.length)
                    isInThinking = true
                } else {
                    answerBuilder.append(remaining)
                    remaining = ""
                }
            } else {
                val endIdx = remaining.indexOf(thinkEndTag)
                if (endIdx != -1) {
                    thinkingBuilder.append(remaining.substring(0, endIdx))
                    remaining = remaining.substring(endIdx + thinkEndTag.length)
                    isInThinking = false
                } else {
                    thinkingBuilder.append(remaining)
                    remaining = ""
                }
            }
        }
        
        return Pair(answerBuilder.toString().trimStart(), thinkingBuilder.toString())
    }

    suspend fun chat(messages: List<ChatMessage>): Triple<String, String?, String?> = withContext(Dispatchers.IO) {
        val requestData = mapOf(
            "model" to "deepseek-r1",
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "temperature" to 0.7,
            "max_tokens" to 4096
        )
        val json = gson.toJson(requestData)
        
        log("========================================")
        log("[API 请求] POST /v1/chat/completions (非流式)")
        log("请求体: $json")
        log("消息数量: ${messages.size}")
        log("========================================")

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/v1/chat/completions")
            .post(requestBody)
            .build()

        val response = LlamaHttpClient.client.newCall(request).execute()
        log("[API 响应] 状态码: ${response.code}")
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            log("[API 错误] $errorBody")
            throw Exception("API request failed: ${response.code}, $errorBody")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        log("[API 响应] 响应体长度: ${responseBody.length} 字符")
        log("========================================")

        val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
        val choices = jsonResponse.getAsJsonArray("choices")
        val message = choices[0].asJsonObject.getAsJsonObject("message")
        
        var content = message.get("content").asString
        var reasoningContent: String? = null
        
        if (message.has("reasoning_content") && !message.get("reasoning_content").isJsonNull && 
            message.get("reasoning_content") !is JsonNull) {
            reasoningContent = message.get("reasoning_content").asString
            log("[思考内容] 长度: ${reasoningContent.length} 字符")
        } else {
            val (cleanContent, thinking) = filterThinkTags(content)
            if (thinking.isNotEmpty()) {
                reasoningContent = thinking
                content = cleanContent
                log("[过滤思考标签] content 长度: ${content.length}, thinking 长度: ${thinking.length}")
            } else {
                log("[思考内容] 无")
            }
        }
        
        log("[最终回复] 长度: ${content.length} 字符")
        log("[最终回复预览] ${content.take(200)}${if (content.length > 200) "..." else ""}")
        log("========================================")
        
        return@withContext Triple(content.trimStart(), reasoningContent, responseBody)
    }

    data class StreamResponse(
        val content: String,
        val thinking: String?
    )
    
    fun chatStream(messages: List<ChatMessage>): Flow<StreamResponse> = flow {
        val requestData = mapOf(
            "model" to "deepseek-r1",
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "temperature" to 0.7,
            "max_tokens" to 4096,
            "stream" to true
        )
        val json = gson.toJson(requestData)
        
        log("========================================")
        log("[API 流式请求] POST /v1/chat/completions (stream=true)")
        log("消息数量: ${messages.size}")
        log("========================================")

        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("http://127.0.0.1:$port/v1/chat/completions")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .post(requestBody)
            .build()

        val response = LlamaHttpClient.client.newCall(request).execute()
        log("[API 响应] 状态码: ${response.code}")
        
        if (!response.isSuccessful) {
            val errorBody = try {
                response.body?.string() ?: "Unknown error"
            } catch (e: Exception) {
                "Failed to read error body: ${e.message}"
            }
            log("[API 错误] $errorBody")
            throw Exception("Stream request failed: ${response.code}, $errorBody")
        }

        log("[API 流式响应] 开始接收数据...")
        
        val inputStream = response.body?.byteStream()
        if (inputStream == null) {
            log("[错误] 响应体为空")
            throw Exception("Empty response body")
        }
        
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        
        var fullAnswer = StringBuilder()
        var fullThinking = StringBuilder()
        var isInThinkingBlock = false
        var pendingAnswer = StringBuilder()
        var pendingThinking = StringBuilder()
        var isFirstContent = true
        
        try {
            var line: String?
            var lineCount = 0
            var isSSEFormat = false
            var hasReceivedData = false
            val buffer = StringBuilder()
            
            while (true) {
                line = reader.readLine()
                if (line == null) {
                    if (pendingAnswer.isNotEmpty()) {
                        val answer = if (isFirstContent) pendingAnswer.toString().trimStart() else pendingAnswer.toString()
                        fullAnswer.append(answer)
                        emit(StreamResponse(answer, null))
                        pendingAnswer.clear()
                        isFirstContent = false
                    }
                    if (pendingThinking.isNotEmpty()) {
                        val thinking = pendingThinking.toString()
                        fullThinking.append(thinking)
                        emit(StreamResponse("", thinking))
                        pendingThinking.clear()
                    }
                    log("[API 流式响应] 连接关闭")
                    break
                }
                
                lineCount++
                buffer.append(line).append("\n")
                
                if (line.startsWith("data: ")) {
                    isSSEFormat = true
                    hasReceivedData = true
                    val jsonStr = line.substring(6).trim()
                    log("[流式数据 $lineCount] ${jsonStr.take(100)}...")
                    
                    if (jsonStr == "[DONE]") {
                        if (pendingAnswer.isNotEmpty()) {
                            val answer = if (isFirstContent) pendingAnswer.toString().trimStart() else pendingAnswer.toString()
                            fullAnswer.append(answer)
                            emit(StreamResponse(answer, null))
                            pendingAnswer.clear()
                            isFirstContent = false
                        }
                        if (pendingThinking.isNotEmpty()) {
                            val thinking = pendingThinking.toString()
                            fullThinking.append(thinking)
                            emit(StreamResponse("", thinking))
                            pendingThinking.clear()
                        }
                        log("[API 流式响应] 完成 (共 $lineCount 行)")
                        log("========================================")
                        break
                    }

                    try {
                        val jsonResponse = gson.fromJson(jsonStr, JsonObject::class.java)
                        
                        if (jsonResponse.has("choices")) {
                            val choices = jsonResponse.getAsJsonArray("choices")
                            if (choices != null && choices.size() > 0) {
                                val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                
                                if (delta != null) {
                                    if (delta.has("content")) {
                                        val content = delta.get("content").asString
                                        
                                        if (content == thinkStartTag) {
                                            if (pendingAnswer.isNotEmpty()) {
                                                val answer = if (isFirstContent) pendingAnswer.toString().trimStart() else pendingAnswer.toString()
                                                fullAnswer.append(answer)
                                                emit(StreamResponse(answer, null))
                                                pendingAnswer.clear()
                                                isFirstContent = false
                                            }
                                            isInThinkingBlock = true
                                        } else if (content == thinkEndTag) {
                                            isInThinkingBlock = false
                                            if (pendingThinking.isNotEmpty()) {
                                                val thinking = pendingThinking.toString()
                                                fullThinking.append(thinking)
                                                emit(StreamResponse("", thinking))
                                                pendingThinking.clear()
                                            }
                                        } else if (isInThinkingBlock) {
                                            pendingThinking.append(content)
                                        } else {
                                            pendingAnswer.append(content)
                                            if (pendingAnswer.length >= 3) {
                                                val answer = if (isFirstContent) pendingAnswer.toString().trimStart() else pendingAnswer.toString()
                                                fullAnswer.append(answer)
                                                emit(StreamResponse(answer, null))
                                                pendingAnswer.clear()
                                                isFirstContent = false
                                            }
                                        }
                                    }
                                    
                                    if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull) {
                                        val reasoning = delta.get("reasoning_content").asString
                                        fullThinking.append(reasoning)
                                        emit(StreamResponse("", reasoning))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        log("[解析错误] ${e.message}, 原始行: $jsonStr")
                    }
                } else {
                    if (!isSSEFormat && lineCount <= 5 && !hasReceivedData) {
                        try {
                            val testJson = gson.fromJson(line, JsonObject::class.java)
                            if (testJson.has("choices")) {
                                hasReceivedData = true
                                log("[检测] 服务器返回完整 JSON（非 SSE 格式）")
                                
                                val choices = testJson.getAsJsonArray("choices")
                                if (choices.size() > 0) {
                                    val message = choices[0].asJsonObject.getAsJsonObject("message")
                                    
                                    var rawContent = ""
                                    if (message.has("reasoning_content") && !message.get("reasoning_content").isJsonNull) {
                                        val reasoning = message.get("reasoning_content").asString
                                        fullThinking.append(reasoning)
                                        emit(StreamResponse("", reasoning))
                                    }
                                    
                                    if (message.has("content")) {
                                        rawContent = message.get("content").asString
                                    }
                                    
                                    val (cleanContent, thinking) = filterThinkTags(rawContent)
                                    if (thinking.isNotEmpty() && fullThinking.isEmpty()) {
                                        fullThinking.append(thinking)
                                        emit(StreamResponse("", thinking))
                                    }
                                    
                                    val trimmedContent = cleanContent.trimStart()
                                    val words = trimmedContent.split(Regex("(?<=\\.)|(?=\\.)|(?<=\\s)|(?=\\s)"))
                                    for (word in words) {
                                        if (word.isNotBlank()) {
                                            fullAnswer.append(word)
                                            emit(StreamResponse(word, null))
                                            kotlinx.coroutines.delay(20)
                                        }
                                    }
                                    log("[内容] 流式发送完成: ${fullAnswer.length} 字符")
                                }
                                break
                            }
                        } catch (e: Exception) {
                            // 忽略解析错误
                        }
                    }
                    
                    if (lineCount > 30 && !hasReceivedData) {
                        log("[警告] 长时间未收到有效数据")
                        val fullResponse = buffer.toString()
                        try {
                            val jsonResponse = gson.fromJson(fullResponse, JsonObject::class.java)
                            if (jsonResponse.has("choices")) {
                                val choices = jsonResponse.getAsJsonArray("choices")
                                if (choices.size() > 0) {
                                    val message = choices[0].asJsonObject.getAsJsonObject("message")
                                    
                                    if (message.has("reasoning_content") && !message.get("reasoning_content").isJsonNull) {
                                        val reasoning = message.get("reasoning_content").asString
                                        fullThinking.append(reasoning)
                                        emit(StreamResponse("", reasoning))
                                    }
                                    
                                    if (message.has("content")) {
                                        val rawContent = message.get("content").asString
                                        val (cleanContent, thinking) = filterThinkTags(rawContent)
                                        if (thinking.isNotEmpty() && fullThinking.isEmpty()) {
                                            fullThinking.append(thinking)
                                            emit(StreamResponse("", thinking))
                                        }
                                        
                                        val trimmedContent = cleanContent.trimStart()
                                        val words = trimmedContent.split(Regex("(?<=\\s)"))
                                        for (word in words) {
                                            if (word.isNotBlank()) {
                                                fullAnswer.append(word)
                                                emit(StreamResponse(word, null))
                                                kotlinx.coroutines.delay(30)
                                            }
                                        }
                                    }
                                }
                                break
                            }
                        } catch (e: Exception) {
                            log("[解析错误] ${e.message}")
                        }
                    }
                }
            }
            
            if (pendingAnswer.isNotEmpty()) {
                val answer = if (isFirstContent) pendingAnswer.toString().trimStart() else pendingAnswer.toString()
                fullAnswer.append(answer)
                emit(StreamResponse(answer, null))
            }
            
            log("[流式完成] 最终回复长度: ${fullAnswer.length}, 思考内容长度: ${fullThinking.length}")
            log("========================================")
        } catch (e: Exception) {
            val errorMsg = e.message ?: "未知流式错误: ${e.javaClass.name}"
            log("[流式异常] $errorMsg")
            throw Exception(errorMsg)
        } finally {
            try {
                reader.close()
            } catch (e: Exception) {
                log("[关闭错误] ${e.message}")
            }
        }
    }.flowOn(Dispatchers.IO)
}
