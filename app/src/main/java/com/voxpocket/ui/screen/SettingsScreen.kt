package com.voxpocket.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voxpocket.service.ServerLog
import com.voxpocket.ui.theme.VoxPocketDark
import com.voxpocket.ui.theme.VoxPocketPrimary
import com.voxpocket.ui.theme.VoxPocketSurfaceVariant
import com.voxpocket.ui.theme.VoxPocketTextPrimary
import com.voxpocket.ui.theme.VoxPocketTextSecondary
import com.voxpocket.viewmodel.ChatUiState
import com.voxpocket.viewmodel.ServerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: ChatUiState,
    savedModelPath: String?,
    serverLogs: List<ServerLog>,
    rawLogs: String,
    onBackClick: () -> Unit,
    onSelectModel: (String) -> Unit,
    onStartModel: (String) -> Unit,
    onStopModel: () -> Unit,
    onClearLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    var modelPath by remember { mutableStateOf(savedModelPath ?: "") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val path = it.toString()
            modelPath = path
            onSelectModel(path)
        }
    }

    Scaffold(
        topBar = {
            VoxPocketSettingsTopBar(
                onBackClick = onBackClick
            )
        },
        containerColor = VoxPocketDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
            .background(VoxPocketDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 模型设置区域
            VoxPocketModelSettings(
                modelPath = modelPath,
                serverStatus = uiState.serverStatus,
                serverPort = uiState.serverPort,
                error = uiState.error,
                onSelectModel = { filePicker.launch(arrayOf("*/*")) },
                onStartModel = { onStartModel(modelPath) },
                onStopModel = onStopModel
            )

            // 服务器日志区域
            ServerLogSection(
                logs = serverLogs,
                rawLogs = rawLogs,
                onClearLogs = onClearLogs
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxPocketSettingsTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "设置",
                fontWeight = FontWeight.SemiBold
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
fun VoxPocketModelSettings(
    modelPath: String,
    serverStatus: ServerStatus,
    serverPort: Int,
    error: String?,
    onSelectModel: () -> Unit,
    onStartModel: () -> Unit,
    onStopModel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "模型设置",
            style = MaterialTheme.typography.titleMedium,
                color = VoxPocketTextPrimary,
                fontWeight = FontWeight.SemiBold
        )

        // 选择模型按钮
        VoxPocketPrimaryButton(
            onClick = onSelectModel,
            icon = Icons.Default.FolderOpen,
            text = "选择模型文件",
            enabled = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 显示已选择的模型路径
        if (modelPath.isNotEmpty()) {
            VoxPocketPathDisplayCard(
                path = modelPath
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 启动/停止按钮
        when (serverStatus) {
            ServerStatus.STOPPED -> {
                VoxPocketPrimaryButton(
                    onClick = onStartModel,
                    text = "启动模型",
                    enabled = modelPath.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ServerStatus.STARTING -> {
                VoxPocketDisabledButton(
                    text = "启动中...",
                    showProgress = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ServerStatus.RUNNING -> {
                VoxPocketDangerButton(
                    onClick = onStopModel,
                    text = "停止模型",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ServerStatus.ERROR -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    error?.let {
                        VoxPocketErrorCard(error = it)
                    }
                    VoxPocketPrimaryButton(
                        onClick = onStartModel,
                        text = "重试",
                        enabled = modelPath.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 状态指示器
            VoxPocketStatusIndicator(
                status = serverStatus,
                serverPort = serverPort
            )
    }
}

@Composable
fun VoxPocketPrimaryButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
                    containerColor = VoxPocketPrimary,
                    contentColor = VoxPocketTextPrimary,
                    disabledContainerColor = VoxPocketSurfaceVariant,
                    disabledContentColor = VoxPocketTextSecondary
                ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun VoxPocketDisabledButton(
    text: String,
    showProgress: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {},
        enabled = false,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = VoxPocketSurfaceVariant,
                    disabledContentColor = VoxPocketTextSecondary
                ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = VoxPocketTextSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun VoxPocketDangerButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626),
            contentColor = VoxPocketTextPrimary
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VoxPocketPathDisplayCard(path: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = VoxPocketSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = path,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
                color = VoxPocketTextSecondary,
            maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VoxPocketErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(14.dp),
            color = Color(0xFFEF4444),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun VoxPocketStatusIndicator(status: ServerStatus, serverPort: Int = 0) {
    val (statusText, statusColor) = when (status) {
        ServerStatus.STOPPED -> "已停止" to Color(0xFF9CA3AF)
        ServerStatus.STARTING -> "启动中..." to Color(0xFFF59E0B)
        ServerStatus.RUNNING -> "运行中" to Color(0xFF10B981)
        ServerStatus.ERROR -> "错误" to Color(0xFFEF4444)
    }

    val displayText = if (status == ServerStatus.RUNNING && serverPort > 0) {
        "$statusText (端口: $serverPort)"
    } else {
        statusText
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = statusColor
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "模型状态: $displayText",
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLogSection(
    logs: List<ServerLog>,
    rawLogs: String,
    onClearLogs: () -> Unit
) {
    var showRawLogs by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除日志", color = VoxPocketTextPrimary) },
            text = { Text("确定要清除所有日志吗？", color = VoxPocketTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearLogs()
                        showClearDialog = false
                    }
                ) {
                    Text("确定", color = VoxPocketPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消", color = VoxPocketTextSecondary)
                }
            },
            containerColor = VoxPocketSurfaceVariant
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "服务器日志",
                style = MaterialTheme.typography.titleMedium,
                color = VoxPocketTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showRawLogs,
                    onClick = { showRawLogs = !showRawLogs },
                    label = { Text("原始日志", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VoxPocketPrimary,
                    selectedLabelColor = VoxPocketTextPrimary
                    )
                )

                if (logs.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                    contentDescription = "清除日志",
                    tint = VoxPocketTextSecondary
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0D1117)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (showRawLogs) {
                SelectionContainer {
                    Text(
                        text = rawLogs,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF10B981)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "暂无日志",
                            style = MaterialTheme.typography.bodyMedium,
                    color = VoxPocketTextSecondary
                        )
                    } else {
                        logs.forEach { log ->
                            LogEntry(log = log)
                        }
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            Text(
                text = "日志已保存到文件",
                style = MaterialTheme.typography.bodySmall,
                color = VoxPocketTextSecondary
            )
        }
    }
}

@Composable
fun LogEntry(log: ServerLog) {
    val color = if (log.isError) Color(0xFFEF4444) else Color(0xFF10B981)

    val sdf = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { sdf.format(java.util.Date(log.timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "[$timeStr]",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = Color(0xFF6B7280)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = color
        )
    }
}
