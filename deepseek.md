### 🧩 核心挑战

- **数据流解析**：需要处理网络层的实时数据流（通常是 SSE），实现边接收边显示。
- **实时更新**：UI 需要随着文本“块”的到达而平滑地追加更新，同时处理好性能与状态。
- **混合渲染**：需要在流式输出中正确解析和渲染 Markdown 格式（如**加粗**、代码块等），并处理好思考过程内容的差异化展示。

### 🌊 实现流式对话

实现流式对话的关键在于网络请求的流式处理和后端返回的特定数据格式。

1. **网络层：使用 OkHttp 处理 SSE**：DeepSeek 的流式输出采用 SSE 协议，OkHttp 是处理此任务的理想选择。

   kotlin

   ```
   // 在 ViewModel 或 Repository 中
   import okhttp3.*
   import okhttp3.MediaType.Companion.toMediaTypeOrNull
   import okhttp3.RequestBody.Companion.toRequestBody
   import kotlinx.coroutines.flow.Flow
   import kotlinx.coroutines.flow.callbackFlow
   
   fun streamDeepSeekResponse(prompt: String): Flow<String> = callbackFlow {
       val client = OkHttpClient.Builder()
           .readTimeout(0, TimeUnit.MILLISECONDS) // 流式连接，设置无限读取超时
           .build()
   
       val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
       val body = """
           {
               "model": "deepseek-chat",
               "messages": [{"role": "user", "content": "$prompt"}],
               "stream": true,
               "thinking": true
           }
       """.trimIndent().toRequestBody(jsonMediaType)
   
       val request = Request.Builder()
           .url("https://api.deepseek.com/v1/chat/completions") // 替换为你的DeepSeek接口地址
           .addHeader("Authorization", "Bearer YOUR_DEEPSEEK_API_KEY")
           .post(body)
           .build()
   
       client.newCall(request).enqueue(object : Callback {
           override fun onFailure(call: Call, e: IOException) {
               close(e)
           }
   
           override fun onResponse(call: Call, response: Response) {
               response.body?.source()?.let { source ->
                   while (!source.exhausted()) {
                       val line = source.readUtf8Line() ?: break
                       if (line.startsWith("data: ")) {
                           val data = line.removePrefix("data: ").trim()
                           if (data == "[DONE]") {
                               break
                           }
                           try {
                               // 解析 JSON，提取 content (或 reasoning_content)
                           } catch (e: Exception) {
                               // 处理解析错误
                           }
                       }
                   }
               }
               close()
           }
       })
       awaitClose { /* 取消请求 */ }
   }
   ```

   

   **💡 优化建议：**

   - 对于长会话或多轮对话，建议采用 **Retrofit** 做统一的请求管理与封装，再用 OkHttp 的 SSE 能力处理流式。
   - Android 侧需处理 DeepSeek 特有的 `reasoning_content`（思考过程）和常规的 `content` 字段，实现两者的分离或差异化展示。
   - 注意部分服务端可能返回 `application/x-ndjson`，此时需配置 OkHttp 正确接收该 Content-Type，防止触发 `IllegalStateException`。

2. **UI 层：Jetpack Compose 绑定流式数据**

   kotlin

   ```
   @Composable
   fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
       val messagesState by viewModel.messages.collectAsState()
       val scrollState = rememberLazyListState()
   
       LaunchedEffect(messagesState.size) {
           scrollState.animateScrollToItem(messagesState.size - 1)
       }
   
       LazyColumn(state = scrollState) {
           items(messagesState) { message ->
               // 根据类型渲染思考内容或普通内容
           }
       }
   
       // 输入框……
   }
   ```

   

### 🧠 展示思考过程

为了展示 DeepSeek 的思考过程，你需要在前端区分处理 `reasoning_content` 和 `content` 字段。典型的设计模式如下：

kotlin

```
// 数据模型示例
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String = "",
    val reasoningText: String = "", // 后端返回的思考过程
    val isThinking: Boolean = false,
    val isStreaming: Boolean = false
)

// 消息气泡的 Compose 实现
@Composable
fun AssitantMessageBubble(message: ChatMessage) {
    Column {
        // 展示思考过程 (可折叠)
        if (message.reasoningText.isNotEmpty()) {
            val showReasoning = remember { mutableStateOf(false) }
            TextButton(onClick = { showReasoning.value = !showReasoning.value }) {
                Text(if(showReasoning.value) "隐藏思考" else "查看思考过程")
            }
            if (showReasoning.value) {
                Text(
                    text = "💭 ${message.reasoningText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
        // 展示最终回复
        Text(text = message.content)
    }
}
```



### ✨ 渲染 Markdown

处理流式输出的 Markdown 时，直接实时渲染会出现**格式闪烁**问题（如 `**Hello**` 刚出现时显示为星号）。因此在流式过程中，建议先以纯文本显示；流式结束后，再用 Markwon 进行最终渲染，确保格式的完整性。

1. **集成 Markwon 库**
   在 `build.gradle.kts` (Module: app) 中添加依赖：

   kotlin

   ```
   dependencies { 
       implementation("io.noties.markwon:core:4.6.2")
       // 可选扩展
       implementation("io.noties.markwon:ext-strikethrough:4.6.2")
       implementation("io.noties.markwon:ext-tables:4.6.2")
       implementation("io.noties.markwon:ext-prism4j:4.6.2") // 代码高亮
   }
   ```

   

2. **创建 Compose 可用的 Markdown 组件**
   由于 Markwon 是基于 View 体系的，需要通过 `AndroidView` 桥接到 Compose 中。

   kotlin

   ```
   @Composable
   fun MarkdownText(
       text: String,
       modifier: Modifier = Modifier
   ) {
       val context = LocalContext.current
       val markwon = remember { Markwon.create(context) }
   
       AndroidView(
           modifier = modifier,
           factory = { ctx ->
               TextView(ctx).apply {
                   // 设置文本大小、内边距等样式
                   textSize = 16f
                   setLineSpacing(4f, 1f)
               }
           },
           update = { textView ->
               markwon.setMarkdown(textView, text)
           }
       )
   }
   ```

   

3. **解决格式闪烁：使用缓冲器**
   创建一个 `MarkdownBuffer` 对象，对未闭合的标记进行暂存，待标记闭合后再渲染，可营造“打字机”效果并杜绝闪烁。

   kotlin

   ```
   class MarkdownBuffer {
       private val unclosedPattern = Regex("""(\*{1,2}|_{1,2}|`{1,3}|~{2})(?!.*\1)""")
       private val codeFencePattern = Regex("""(?s)```\w*\s*.*""")
   
       fun bufferForStreaming(rawText: String): String {
           // 简单占位：将未闭合符号替换为空白字符
           // 更好的实践是隐藏整个不完整的代码块
           if (codeFencePattern.containsMatchIn(rawText)) {
               // 处理不完整代码块...
           }
           return rawText.replace(unclosedPattern, "")
       }
   }
   ```

   

4. **完整集成示例**
   在 ViewModel 中整合 SSE 流式读取、思考过程提取、Markdown 缓冲渲染的全流程：

   kotlin

   ```
   // ViewModel 伪代码示例
   class ChatViewModel : ViewModel() {
       private val _messages = MutableList<ChatMessage>()
       val messages = _messages // 暴露给 UI
       
       suspend fun sendPrompt(prompt: String) {
           // 添加用户消息
           _messages.add(ChatMessage(role = "user", content = prompt))
   
           // 创建空的 AI 消息占位
           val aiMessageIndex = _messages.size
           _messages.add(ChatMessage(role = "assistant", isStreaming = true))
   
           // 使用回调方式处理流式数据
           streamDeepSeekResponse(prompt).collect { chunk ->
               // 解析 chunk，提取 content 和 reasoning_content
               val currentMessage = _messages[aiMessageIndex]
               
               // 更新思考内容
               val reasoning = extractReasoning(chunk)
               if (reasoning != null) {
                   _messages[aiMessageIndex] = currentMessage.copy(
                       reasoningText = currentMessage.reasoningText + reasoning
                   )
               }
   
               // 更新回复内容，应用 Markdown 缓冲
               val content = extractContent(chunk)
               if (content != null) {
                   val buffer = MarkdownBuffer()
                   val displayText = buffer.bufferForStreaming(currentMessage.content + content)
                   _messages[aiMessageIndex] = currentMessage.copy(
                       content = currentMessage.content + content,
                       displayText = displayText // UI 绑定此字段显示
                   )
               }
           }
   
           // 流式结束，进行最终渲染
           _messages[aiMessageIndex] = _messages[aiMessageIndex].copy(
               isStreaming = false,
               displayText = _messages[aiMessageIndex].content // 最终完整渲染
           )
       }
   }
   ```

   

### 💎 方案总结与关键决策

| 技术环节          | 推荐方案                               | 关键决策点                                                   |
| :---------------- | :------------------------------------- | :----------------------------------------------------------- |
| **网络流式传输**  | OkHttp 直接读取 SSE 流                 | **核心项**：必须处理 `content` 与 `reasoning_content` 字段，并注意服务器 Content-Type |
| **UI 架构模式**   | MVVM + Kotlin Flow + StateFlow         | **核心项**：确保数据流的生命周期安全，UI 层仅负责消费状态。  |
| **思考过程展示**  | 独立可折叠区域 + 后台 SSE 事件流       | **差异化项**：采用简洁的 `Callout` 样式展示，不打断对话流。  |
| **Markdown 渲染** | Markwon 库 (View-based) + Compose 桥接 | **核心项**：解决流式渲染的格式闪烁问题，可使用轻量缓冲器延迟渲染。 |
| **代码高亮**      | Markwon `ext-prism4j` 扩展             | **体验提升项**：极大改善技术问答场景的可读性。               |

### 📦 上下文自动压缩方案

这套方案需要解决三个问题：**何时压缩**、**如何压缩**、**如何做到无感**。

#### 1. 触发时机：发送前拦截

在 `ChatViewModel` 的 `sendPrompt` 方法中，构建 `messages` 列表请求体之前，先调用压缩逻辑。

kotlin

```
class ChatViewModel : ViewModel() {

    suspend fun sendPrompt(prompt: String) {
        // 1. 添加用户消息到本地消息列表
        _messages.add(ChatMessage(role = "user", content = prompt))
        
        // 2. 从本地消息列表中提取要发送的上下文
        val messagesToSend = mutableListOf<ChatMessage>()
        messagesToSend.addAll(_messages) // 完整历史
        
        // 3. 自动压缩，直到满足 token 限制（假设限制 60000 token）
        val compressor = ContextCompressor(maxTokens = 60000)
        val compressedMessages = compressor.compress(messagesToSend)
        
        // 4. 构建请求体，使用压缩后的消息
        // ... 后续流式请求逻辑不变
    }
}
```



#### 2. 压缩策略：滑动窗口 + 摘要兜底

不同的策略对用户体验和连贯性影响不同，推荐混合使用：

| 策略         | 原理                                            | 优点                   | 缺点                            | 适用场景                   |
| :----------- | :---------------------------------------------- | :--------------------- | :------------------------------ | :------------------------- |
| **滑动窗口** | 保留最近 N 轮对话（例如 20 轮），丢弃更早的消息 | 实现简单，速度极快     | 可能丢失重要前期设定            | 日常闲聊、轻量问答         |
| **摘要压缩** | 对早期消息生成摘要，替换为一条“系统摘要”消息    | 保留关键信息，连贯性好 | 需要额外 API 调用，有延迟和成本 | 长任务、角色扮演、复杂推理 |
| **智能分层** | 近几轮保留原文，更早的轮次逐步替换为摘要        | 平衡效果与成本         | 实现复杂度高                    | 对连贯性要求极高的生产应用 |

**推荐初期落地：滑动窗口 + 简易摘要组合。**
当历史超过窗口时，不是简单丢弃，而是对最早的部分做一次本地快速摘要（或调用一次轻量模型），将摘要插入系统消息，这样能大大降低用户对“失忆”的感知。

#### 3. 透明化Token计数

需要一个轻量级的 token 估算器。客户端无法精确计算所有模型的 token，但可以使用通用估算：**英文约 1 token ≈ 4 字符，中文约 1 token ≈ 1.5 字符**。更精确的做法是使用 `gpt-tokenizer` 的 Kotlin 移植库（如 `com.openai:tokenizer`），不过对于 DeepSeek，因其 tokenizer 与 GPT-4 类似，可以使用同款估算。

kotlin

```
object TokenEstimator {
    fun count(text: String): Int {
        // 简化版：中英文混合估算
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val otherChars = text.length - chineseChars
        return (chineseChars * 1.2 + otherChars * 0.25).toInt()
    }
}
```



#### 4. 无感压缩代码示例

下面是一个可直接使用的上下文压缩器，它采用 **“保留最近 N 轮 + 对更早消息自动生成摘要”** 的策略。

kotlin

```
class ContextCompressor(
    private val maxTokens: Int = 40000,      // 模型上下文窗口上限
    private val safeMargin: Int = 8000,      // 为回复预留的token
    private val recentRoundsToKeep: Int = 5  // 保留最近5轮对话原文
) {
    
    suspend fun compress(allMessages: List<ChatMessage>): List<ChatMessage> {
        val targetTokens = maxTokens - safeMargin
        var totalTokens = allMessages.sumOf { TokenEstimator.count(it.content) }
        
        if (totalTokens <= targetTokens) return allMessages
        
        // 找出最近几轮的索引
        val recentStartIndex = maxOf(0, allMessages.size - recentRoundsToKeep * 2) // 每轮包含user+assistant
        val recentMessages = allMessages.subList(recentStartIndex, allMessages.size)
        val olderMessages = allMessages.subList(0, recentStartIndex)
        val recentTokens = recentMessages.sumOf { TokenEstimator.count(it.content) }
        
        if (recentTokens >= targetTokens) {
            // 连最近几轮都超长了，只能暴力保留最后几轮
            return retainLastByTokens(allMessages, targetTokens)
        }
        
        // 尝试为更早的消息生成摘要
        val summaryTokens = targetTokens - recentTokens
        if (summaryTokens > 200) { // 至少留下足够空间写摘要
            val summary = generateSummary(olderMessages)
            val compressed = listOf(
                ChatMessage(role = "system", content = "Previous conversation summary: $summary")
            ) + recentMessages
            return compressed
        } else {
            // 没空间写摘要了，保留最近的消息
            return recentMessages
        }
    }
    
    private suspend fun generateSummary(messages: List<ChatMessage>): String {
        // 调用 DeepSeek 或其他轻量接口，专门用于总结历史
        // 这里为避免压缩本身占用过多资源，可设置较低max_tokens
        // 返回一句简短的摘要
        // 示例：调用 deepseek-chat summary...
        return "User asked about Android streaming and markdown rendering, and we discussed context compression strategies."
    }
    
    private fun retainLastByTokens(messages: List<ChatMessage>, limit: Int): List<ChatMessage> {
        var count = 0
        val result = mutableListOf<ChatMessage>()
        for (msg in messages.reversed()) {
            val t = TokenEstimator.count(msg.content)
            if (count + t > limit) break
            result.add(0, msg)
            count += t
        }
        return result
    }
}
```



**让用户彻底无感**：整个过程在 `sendPrompt` 中异步执行，UI 无须任何改动。用户点击发送后，可能感受到的只是从“立即开始回复”变为“先有极短停顿（摘要生成耗时约0.5-2秒）然后立即回复”。如果希望完全无延迟，可以在后台空闲时预先生成上一段历史的摘要并缓存。