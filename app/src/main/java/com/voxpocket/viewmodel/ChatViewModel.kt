package com.voxpocket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxpocket.client.LlamaClient
import com.voxpocket.data.database.Conversation
import com.voxpocket.data.database.Message
import com.voxpocket.data.model.ChatMessage
import com.voxpocket.data.repository.ChatRepository
import com.voxpocket.service.LlamaServerManager
import com.voxpocket.util.PreferencesHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.voxpocket.service.ServerLog

data class ChatUiState(
    val currentConversationId: String? = null,
    val isLoading: Boolean = false,
    val serverStatus: ServerStatus = ServerStatus.STOPPED,
    val serverPort: Int = 0,
    val error: String? = null,
    val statusMessage: String? = null,
    val useStreaming: Boolean = true,
    val messageCount: Int = 0
)

enum class ServerStatus { STOPPED, STARTING, RUNNING, ERROR }

class ChatViewModel(
    private val repository: ChatRepository,
    private val serverManager: LlamaServerManager,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    
    private val apiLogger: (String) -> Unit = serverManager.getApiLogger()
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _partialResponse = MutableStateFlow("")
    val partialResponse: StateFlow<String> = _partialResponse.asStateFlow()

    private val _thinkingContent = MutableStateFlow("")
    val thinkingContent: StateFlow<String> = _thinkingContent.asStateFlow()

    private val _showThinking = MutableStateFlow(true)
    val showThinking: StateFlow<Boolean> = _showThinking.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<ServerLog>>(emptyList())
    val serverLogs: StateFlow<List<ServerLog>> = _serverLogs.asStateFlow()

    private val _rawLogs = MutableStateFlow("")
    val rawLogs: StateFlow<String> = _rawLogs.asStateFlow()

    val conversations = repository.getAllConversations()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private var currentRequestJob: Job? = null

    init {
        viewModelScope.launch {
            serverManager.serverLogs.collect { logs ->
                _serverLogs.value = logs
            }
        }
        viewModelScope.launch {
            serverManager.rawLogs.collect { raw ->
                _rawLogs.value = raw
            }
        }
    }

    private fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            repository.getMessagesByConversationId(conversationId).collect { list ->
                _messages.value = list
                _uiState.update { it.copy(messageCount = list.size) }
                android.util.Log.d("ChatViewModel", "loadMessages: 共 ${list.size} 条消息")
            }
        }
    }

    fun selectConversation(conversation: Conversation) {
        _uiState.update { it.copy(currentConversationId = conversation.id) }
        loadMessages(conversation.id)
    }

    fun createNewConversation() = viewModelScope.launch {
        val conversation = repository.createConversation()
        _uiState.update { it.copy(currentConversationId = conversation.id) }
        _messages.value = emptyList()
        _uiState.update { it.copy(messageCount = 0) }
        android.util.Log.d("ChatViewModel", "createNewConversation: 新建对话 ID=${conversation.id}")
    }

    fun deleteConversation(conversation: Conversation) = viewModelScope.launch {
        repository.deleteConversation(conversation)
        if (_uiState.value.currentConversationId == conversation.id) {
            _uiState.update { it.copy(currentConversationId = null) }
            _messages.value = emptyList()
            _uiState.update { it.copy(messageCount = 0) }
        }
    }

    fun sendMessage(content: String) {
        val useStreaming = _uiState.value.useStreaming
        apiLogger("[发送方式] ${if (useStreaming) "流式" else "非流式"}请求")
        
        if (useStreaming) {
            sendMessageStream(content)
        } else {
            sendMessageSync(content)
        }
    }
    
    private fun sendMessageSync(content: String) = viewModelScope.launch {
        apiLogger("[用户发送消息] $content (非流式)")
        _uiState.update { it.copy(statusMessage = "正在发送消息...", error = null) }
        
        val conversationId = _uiState.value.currentConversationId ?: run {
            apiLogger("[错误] 未选择对话")
            return@launch
        }
        val serverPort = _uiState.value.serverPort.takeIf { it > 0 } ?: 8080

        val userMessage = repository.addMessage(conversationId, "user", content)
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(userMessage)
        _messages.value = currentMessages
        _uiState.update { it.copy(messageCount = currentMessages.size) }
        
        apiLogger("[消息添加成功] 用户消息已添加到本地，消息数: ${currentMessages.size}")
        _uiState.update { it.copy(isLoading = true, statusMessage = "等待AI响应...") }

        try {
            val history = currentMessages.map { ChatMessage(it.role, it.content) }
            apiLogger("[API 调用] 准备发送请求到端口 $serverPort，消息数: ${history.size}")
            
            val client = LlamaClient(serverPort, apiLogger)
            _uiState.update { it.copy(statusMessage = "AI正在思考...") }
            
            val (response, reasoningContent, _) = client.chat(history)
            apiLogger("[API 响应] 收到回复，长度: ${response.length}")
            if (reasoningContent != null) {
                apiLogger("[思考内容] 长度: ${reasoningContent.length}")
            }

            val assistantMessage = repository.addMessage(conversationId, "assistant", response, reasoningContent)
            val updatedMessages = _messages.value.toMutableList()
            updatedMessages.add(assistantMessage)
            _messages.value = updatedMessages
            _uiState.update { it.copy(messageCount = updatedMessages.size, statusMessage = "回复完成") }
            
            apiLogger("[完成] 非流式回复完成")
        } catch (e: Exception) {
            val errorMsg = e.message ?: "未知错误: ${e.javaClass.simpleName}"
            apiLogger("[API 错误] $errorMsg")
            _uiState.update { it.copy(error = errorMsg, statusMessage = "回复失败: $errorMsg") }
            
            val updatedMessages = _messages.value.toMutableList()
            updatedMessages.remove(userMessage)
            _messages.value = updatedMessages
            _uiState.update { it.copy(messageCount = updatedMessages.size) }
            apiLogger("[消息回滚] 已删除失败的用户消息")
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun sendMessageStream(content: String) {
        apiLogger("[用户发送消息] $content (流式)")
        _uiState.update { it.copy(statusMessage = "正在发送消息...", error = null) }
        
        val conversationId = _uiState.value.currentConversationId 
        
        if (conversationId == null) {
            apiLogger("[错误] conversationId is null!")
            _uiState.update { it.copy(statusMessage = "错误: 未选择对话") }
            return
        }
        
        val serverPort = _uiState.value.serverPort.takeIf { it > 0 } ?: 8080
        apiLogger("[API 调用] 流式请求到端口 $serverPort")

        currentRequestJob?.cancel()
        
        currentRequestJob = viewModelScope.launch {
            val userMessage = repository.addMessage(conversationId, "user", content)
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(userMessage)
            _messages.value = currentMessages
            _uiState.update { it.copy(messageCount = currentMessages.size) }
            
            if (currentMessages.size > 15) {
                apiLogger("[上下文压缩] 消息数量: ${currentMessages.size}，开始压缩旧消息思考过程")
                repository.compressContext(conversationId, keepMessages = 10)
            }
            
            apiLogger("[消息添加成功] 用户消息已添加到本地，消息数: ${currentMessages.size}")
            _uiState.update { it.copy(isLoading = true, statusMessage = "AI正在思考...") }
            _partialResponse.value = ""
            _thinkingContent.value = ""

            try {
                val fullAnswer = StringBuilder()
                val fullThinking = StringBuilder()
                val history = currentMessages.map { ChatMessage(it.role, it.content) }
                
                apiLogger("[消息历史] 共 ${history.size} 条消息")
                
                val client = LlamaClient(serverPort, apiLogger)
                _uiState.update { it.copy(statusMessage = "正在接收AI回复...") }
                apiLogger("[流式开始] 开始接收数据...")

                client.chatStream(history).collect { streamResponse ->
                    if (streamResponse.thinking != null && streamResponse.thinking.isNotEmpty()) {
                        fullThinking.append(streamResponse.thinking)
                        val currentThinking = fullThinking.toString()
                        _thinkingContent.value = currentThinking
                        _uiState.update { it.copy(statusMessage = "AI正在思考... (${currentThinking.length}字)") }
                    }
                    
                    if (streamResponse.content.isNotEmpty()) {
                        fullAnswer.append(streamResponse.content)
                        val currentAnswer = fullAnswer.toString()
                        _partialResponse.value = currentAnswer
                        _uiState.update { it.copy(statusMessage = "正在生成回复... (${currentAnswer.length}字)") }
                    }
                }

                apiLogger("[流式完成] 最终回复长度: ${fullAnswer.length}, 思考内容长度: ${fullThinking.length}")
                
                val assistantMessage = repository.addMessage(
                    conversationId, 
                    "assistant", 
                    fullAnswer.toString(), 
                    thinkingProcess = fullThinking.toString().takeIf { it.isNotEmpty() }
                )
                
                val finalMessages = _messages.value.toMutableList()
                finalMessages.add(assistantMessage)
                _messages.value = finalMessages
                _uiState.update { it.copy(messageCount = finalMessages.size, statusMessage = "回复完成") }
                
                _partialResponse.value = ""
                _thinkingContent.value = ""
            } catch (e: Exception) {
                val errorMsg = e.message ?: "未知错误: ${e.javaClass.simpleName}"
                apiLogger("[流式错误] $errorMsg")
                _uiState.update { it.copy(error = errorMsg, statusMessage = "回复失败: $errorMsg") }
                
                val updatedMessages = _messages.value.toMutableList()
                updatedMessages.remove(userMessage)
                _messages.value = updatedMessages
                _uiState.update { it.copy(messageCount = updatedMessages.size) }
                apiLogger("[消息回滚] 已删除失败的用户消息")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                _partialResponse.value = ""
                _thinkingContent.value = ""
            }
        }
    }
    
    fun cancelCurrentRequest() {
        apiLogger("[用户取消] 请求已取消")
        currentRequestJob?.cancel()
        currentRequestJob = null
        _uiState.update { it.copy(isLoading = false, statusMessage = "已取消", error = null) }
        _partialResponse.value = ""
        _thinkingContent.value = ""
    }

    fun startModel(modelPath: String) = viewModelScope.launch {
        _uiState.update { it.copy(serverStatus = ServerStatus.STARTING, statusMessage = "正在启动模型...") }
        val result = serverManager.start(modelPath)
        if (result.isSuccess) {
            val port = result.getOrThrow()
            _uiState.update { it.copy(serverStatus = ServerStatus.RUNNING, serverPort = port, statusMessage = "模型已启动 (端口:$port)") }
            preferencesHelper.saveModelPath(modelPath)
        } else {
            _uiState.update {
                it.copy(
                    serverStatus = ServerStatus.ERROR,
                    serverPort = 0,
                    error = result.exceptionOrNull()?.message,
                    statusMessage = "启动失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun stopModel() {
        serverManager.stop()
        _uiState.update { it.copy(serverStatus = ServerStatus.STOPPED, serverPort = 0, statusMessage = "模型已停止") }
    }

    fun toggleThinkingVisibility() {
        _showThinking.value = !_showThinking.value
    }

    fun toggleStreaming() {
        val newState = !_uiState.value.useStreaming
        _uiState.update { it.copy(useStreaming = newState) }
        apiLogger("[设置变更] 流式模式: $newState")
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
    
    fun clearLogs() {
        serverManager.clearServerLogs()
        apiLogger("[日志已清除]")
    }
    
    fun compressContext() = viewModelScope.launch {
        val conversationId = _uiState.value.currentConversationId ?: return@launch
        repository.compressContext(conversationId, keepMessages = 10)
        apiLogger("[手动压缩] 上下文压缩完成")
    }
}
