package com.voxpocket.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxpocket.data.database.Message
import com.voxpocket.ui.component.Avatar
import com.voxpocket.ui.component.MessageBubble
import com.voxpocket.ui.component.StreamingThinkingIndicator
import com.voxpocket.ui.theme.VoxPocketAIBubble
import com.voxpocket.ui.theme.VoxPocketDark
import com.voxpocket.ui.theme.VoxPocketErrorRed
import com.voxpocket.ui.theme.VoxPocketPrimary
import com.voxpocket.ui.theme.VoxPocketSurfaceDark
import com.voxpocket.ui.theme.VoxPocketSurfaceVariant
import com.voxpocket.ui.theme.VoxPocketTextPrimary
import com.voxpocket.ui.theme.VoxPocketTextSecondary
import com.voxpocket.viewmodel.ChatUiState
import com.voxpocket.viewmodel.ServerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    messages: List<Message>,
    partialResponse: String,
    thinkingContent: String,
    showThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onCancelMessage: () -> Unit,
    onBackClick: () -> Unit,
    onToggleThinking: () -> Unit,
    onToggleStreaming: () -> Unit
) {
    var inputText by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()
    val serverReady = uiState.serverStatus == ServerStatus.RUNNING
    val useStreaming = uiState.useStreaming

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            VoxPocketChatTopBar(
                onBackClick = onBackClick,
                useStreaming = useStreaming,
                onToggleStreaming = onToggleStreaming,
                showThinking = showThinking,
                onToggleThinking = onToggleThinking,
                messageCount = uiState.messageCount
            )
        },
        containerColor = VoxPocketDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = uiState.isLoading || uiState.statusMessage != null || uiState.error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                StatusMessageBar(
                    isLoading = uiState.isLoading,
                    statusMessage = uiState.statusMessage,
                    error = uiState.error
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                .weight(1f)
                .background(VoxPocketDark),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (uiState.isLoading && thinkingContent.isNotEmpty()) {
                    item(key = "thinking_indicator") {
                        StreamingThinkingIndicator(
                            thinkingContent = thinkingContent,
                            isLoading = uiState.isLoading
                        )
                    }
                }

                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        showThinking = showThinking
                    )
                }

                if (uiState.isLoading && partialResponse.isNotEmpty()) {
                    item(key = "partial_response") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            Avatar(
                                isUser = false,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            Surface(
                                color = VoxPocketAIBubble,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text(
                                    text = partialResponse,
                                    modifier = Modifier.padding(12.dp),
                                    color = VoxPocketTextPrimary
                                )
                            }
                        }
                    }
                }

                if (uiState.isLoading && thinkingContent.isEmpty() && partialResponse.isEmpty()) {
                    item(key = "loading_indicator") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(
                                isUser = false,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            
                            Surface(
                color = VoxPocketAIBubble,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = VoxPocketPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在思考...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VoxPocketTextSecondary
                    )
                }
            }
                        }
                    }
                }
            }

            VoxPocketInputArea(
        inputText = inputText,
        onInputChange = { inputText = it },
        onSend = { 
            if (inputText.text.isNotBlank()) {
                onSendMessage(inputText.text)
                inputText = TextFieldValue()
            }
        },
        onCancel = onCancelMessage,
        isLoading = uiState.isLoading,
        serverReady = serverReady,
        placeholder = if (!serverReady) "请先启动模型..." else "输入消息..."
    )

            uiState.error?.let { error ->
                if (!uiState.isLoading) {
                    VoxPocketErrorSnackbar(error = error)
                }
            }
        }
    }
}

@Composable
fun StatusMessageBar(
    isLoading: Boolean,
    statusMessage: String?,
    error: String?
) {
    Surface(
        color = when {
            error != null -> VoxPocketErrorRed.copy(alpha = 0.95f)
            isLoading -> VoxPocketPrimary.copy(alpha = 0.95f)
            else -> VoxPocketSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = when {
                    error != null -> "❌ 回复失败"
                    statusMessage != null -> statusMessage
                    isLoading -> "处理中..."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxPocketChatTopBar(
    onBackClick: () -> Unit,
    useStreaming: Boolean = true,
    onToggleStreaming: () -> Unit = {},
    showThinking: Boolean = true,
    onToggleThinking: () -> Unit = {},
    messageCount: Int = 0
) {
    TopAppBar(
        title = {
            Column {
                Text(
            text = "VoxPocket",
            fontWeight = FontWeight.SemiBold
        )
                if (messageCount > 0) {
                    Text(
                        text = "$messageCount 条消息",
                        style = MaterialTheme.typography.bodySmall,
                        color = VoxPocketTextSecondary
                    )
                }
            }
        },
        actions = {
            AssistChip(
                onClick = onToggleThinking,
                label = {
                    Text(
                        text = if (showThinking) "思考" else "隐藏",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        if (showThinking) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (showThinking) VoxPocketPrimary.copy(alpha = 0.2f) else VoxPocketSurfaceDark
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(
                onClick = onToggleStreaming,
                label = {
                    Text(
                        text = if (useStreaming) "流式" else "非流式",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Stream,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (useStreaming) VoxPocketPrimary.copy(alpha = 0.2f) else VoxPocketSurfaceDark
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack, 
                    contentDescription = "返回",
            tint = VoxPocketTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VoxPocketSurfaceVariant,
            titleContentColor = VoxPocketTextPrimary,
            navigationIconContentColor = VoxPocketTextSecondary
        )
    )
}

@Composable
fun VoxPocketInputArea(
    inputText: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean,
    serverReady: Boolean,
    placeholder: String = "输入消息..."
) {
    Surface(
        color = VoxPocketSurfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(VoxPocketSurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && serverReady,
                        textStyle = LocalTextStyle.current.copy(
                            color = VoxPocketTextPrimary,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(VoxPocketPrimary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputText.text.isEmpty()) {
                                    Text(
                                        text = if (isLoading) "AI 正在回复..." else placeholder,
                                        color = VoxPocketTextSecondary,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = when {
                        isLoading -> VoxPocketErrorRed
                        serverReady && inputText.text.isNotBlank() -> VoxPocketPrimary
                        else -> VoxPocketSurfaceDark
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    shadowElevation = 4.dp
                ) {
                    if (isLoading) {
                        IconButton(
                            onClick = onCancel,
                            enabled = true
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "取消",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onSend,
                            enabled = serverReady && inputText.text.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "发送",
                                tint = if (serverReady && inputText.text.isNotBlank()) 
                        Color.White 
                    else 
                        VoxPocketTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoxPocketLoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = VoxPocketPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "生成中...",
                style = MaterialTheme.typography.bodyMedium,
                color = VoxPocketTextSecondary
            )
    }
}

@Composable
fun VoxPocketErrorSnackbar(error: String) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = VoxPocketErrorRed,
        contentColor = Color.White
    ) {
        Text(
            text = "回复失败: $error",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
