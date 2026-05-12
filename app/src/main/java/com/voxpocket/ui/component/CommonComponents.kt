package com.voxpocket.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voxpocket.ui.theme.VoxPocketDark
import com.voxpocket.ui.theme.VoxPocketPrimary
import com.voxpocket.ui.theme.VoxPocketSurfaceDark
import com.voxpocket.ui.theme.VoxPocketSurfaceVariant
import com.voxpocket.ui.theme.VoxPocketTextPrimary
import com.voxpocket.ui.theme.VoxPocketTextSecondary

@Composable
fun VoxPocketLoadingDialog(
    message: String = "加载中...",
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = VoxPocketSurfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = VoxPocketPrimary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                color = VoxPocketTextPrimary
                )
            }
        }
    }
}

@Composable
fun VoxPocketConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoxPocketSurfaceVariant,
        title = {
            Text(
                text = title,
                color = VoxPocketTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                color = VoxPocketTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = VoxPocketPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissText,
                    color = VoxPocketTextSecondary
                )
            }
        }
    )
}

@Composable
fun VoxPocketErrorDialog(
    title: String = "出错了",
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoxPocketSurfaceVariant,
        title = {
            Text(
                text = title,
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                color = VoxPocketTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "知道了",
                    color = VoxPocketPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun VoxPocketFullScreenLoading(
    message: String = "加载中..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxPocketDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = VoxPocketPrimary,
                strokeWidth = 3.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = VoxPocketTextSecondary
            )
        }
    }
}

@Composable
fun VoxPocketEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxPocketDark)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icon()
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = VoxPocketTextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VoxPocketTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            action?.invoke()
        }
    }
}

@Composable
fun VoxPocketSnackbar(
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit = {}
) {
    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = VoxPocketSurfaceVariant,
        contentColor = VoxPocketTextPrimary,
        action = {
            action?.let {
                TextButton(onClick = {
                    onAction?.invoke()
                    onDismiss()
                }) {
                    Text(
                        text = it,
                        color = VoxPocketPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Text(text = message)
    }
}
