package com.voxpocket.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxpocket.data.database.AppDatabase
import com.voxpocket.data.repository.ChatRepository
import com.voxpocket.service.LlamaServerManager
import com.voxpocket.ui.screen.ChatScreen
import com.voxpocket.ui.screen.ConversationListScreen
import com.voxpocket.ui.screen.SettingsScreen
import com.voxpocket.ui.theme.VoxPocketTheme
import com.voxpocket.util.PreferencesHelper
import com.voxpocket.viewmodel.ChatViewModel
import com.voxpocket.viewmodel.ChatViewModelFactory

enum class Screen {
    CONVERSATION_LIST,
    CHAT,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private lateinit var serverManager: LlamaServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        val repository = ChatRepository(db)
        serverManager = LlamaServerManager(this)
        val preferencesHelper = PreferencesHelper(this)

        setContent {
            VoxPocketTheme {
                val viewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(repository, serverManager, preferencesHelper)
                )

                var currentScreen by remember { mutableStateOf(Screen.CONVERSATION_LIST) }
                val uiState by viewModel.uiState.collectAsState()
                val conversations by viewModel.conversations.collectAsState(emptyList())
                val messages by viewModel.messages.collectAsState(emptyList())
                val partialResponse by viewModel.partialResponse.collectAsState()
                val thinkingContent by viewModel.thinkingContent.collectAsState()
                val showThinking by viewModel.showThinking.collectAsState()
                val serverLogs by viewModel.serverLogs.collectAsState(emptyList())
                val rawLogs by viewModel.rawLogs.collectAsState()

                when (currentScreen) {
                    Screen.CONVERSATION_LIST -> {
                        ConversationListScreen(
                            conversations = conversations,
                            onSelectConversation = {
                                viewModel.selectConversation(it)
                                currentScreen = Screen.CHAT
                            },
                            onCreateConversation = {
                                viewModel.createNewConversation()
                                currentScreen = Screen.CHAT
                            },
                            onDeleteConversation = {
                                viewModel.deleteConversation(it)
                            },
                            onOpenSettings = {
                                currentScreen = Screen.SETTINGS
                            }
                        )
                    }
                    Screen.CHAT -> {
                        ChatScreen(
                            uiState = uiState,
                            messages = messages,
                            partialResponse = partialResponse,
                            thinkingContent = thinkingContent,
                            showThinking = showThinking,
                            onSendMessage = { viewModel.sendMessage(it) },
                            onCancelMessage = { viewModel.cancelCurrentRequest() },
                            onBackClick = { currentScreen = Screen.CONVERSATION_LIST },
                            onToggleThinking = { viewModel.toggleThinkingVisibility() },
                            onToggleStreaming = { viewModel.toggleStreaming() }
                        )
                    }
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            uiState = uiState,
                            savedModelPath = preferencesHelper.getModelPath(),
                            serverLogs = serverLogs,
                            rawLogs = rawLogs,
                            onBackClick = { currentScreen = Screen.CONVERSATION_LIST },
                            onSelectModel = { preferencesHelper.saveModelPath(it) },
                            onStartModel = { viewModel.startModel(it) },
                            onStopModel = { viewModel.stopModel() },
                            onClearLogs = { viewModel.clearLogs() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverManager.stop()
    }
}
