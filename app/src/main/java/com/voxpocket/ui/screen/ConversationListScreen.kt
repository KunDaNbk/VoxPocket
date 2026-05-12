package com.voxpocket.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voxpocket.data.database.Conversation
import com.voxpocket.ui.theme.VoxPocketDark
import com.voxpocket.ui.theme.VoxPocketPrimary
import com.voxpocket.ui.theme.VoxPocketSurfaceDark
import com.voxpocket.ui.theme.VoxPocketSurfaceVariant
import com.voxpocket.ui.theme.VoxPocketTextPrimary
import com.voxpocket.ui.theme.VoxPocketTextSecondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onSelectConversation: (Conversation) -> Unit,
    onCreateConversation: () -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onOpenSettings: () -> Unit
) {
    // VoxPocket 风格布局：侧边栏样式
    Scaffold(
        topBar = {
            VoxPocketTopBar(
                onOpenSettings = onOpenSettings
            )
        },
        floatingActionButton = {
            VoxPocketFloatingActionButton(
                onCreateConversation = onCreateConversation
            )
        },
        containerColor = VoxPocketDark
    ) { padding ->
        if (conversations.isEmpty()) {
            VoxPocketEmptyState(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VoxPocketSurfaceDark),
                contentPadding = padding
            ) {
                items(conversations) { conversation ->
                    VoxPocketConversationItem(
                        conversation = conversation,
                        onClick = { onSelectConversation(conversation) },
                        onDelete = { onDeleteConversation(conversation) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxPocketTopBar(onOpenSettings: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "VoxPocket",
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings, 
                contentDescription = "设置",
                tint = VoxPocketTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VoxPocketSurfaceVariant,
            titleContentColor = VoxPocketTextPrimary,
            actionIconContentColor = VoxPocketTextSecondary
        )
    )
}

@Composable
fun VoxPocketFloatingActionButton(onCreateConversation: () -> Unit) {
    FloatingActionButton(
        onClick = onCreateConversation,
        containerColor = VoxPocketPrimary,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(56.dp)
    ) {
        Icon(
                Icons.Default.Add,
                contentDescription = "新建对话",
                tint = VoxPocketTextPrimary
        )
    }
}

@Composable
fun VoxPocketConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = VoxPocketSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = VoxPocketPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = VoxPocketTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatDate(conversation.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = VoxPocketTextSecondary
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "删除",
                    tint = VoxPocketTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun VoxPocketEmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(VoxPocketDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = VoxPocketTextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "还没有对话",
                style = MaterialTheme.typography.titleMedium,
                color = VoxPocketTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击下方按钮开始新对话",
                style = MaterialTheme.typography.bodyMedium,
                color = VoxPocketTextSecondary
            )
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

