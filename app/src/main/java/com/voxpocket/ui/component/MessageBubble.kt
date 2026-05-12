package com.voxpocket.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxpocket.data.database.Message
import com.voxpocket.ui.theme.VoxPocketPrimary
import com.voxpocket.ui.theme.VoxPocketAIBubble
import com.voxpocket.ui.theme.VoxPocketTextPrimary
import com.voxpocket.ui.theme.VoxPocketTextSecondary
import com.voxpocket.ui.theme.VoxPocketUserBubble
import com.voxpocket.ui.theme.VoxPocketDark

@Composable
fun MessageBubble(
    message: Message,
    showThinking: Boolean = true
) {
    val isUser = message.role == "user"
    val thinkingContent: String = message.thinkingProcess ?: ""
    val hasThinking: Boolean = thinkingContent.isNotEmpty()
    
    var isThinkingExpanded by remember(message.id) { mutableStateOf(true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Avatar(
                isUser = false
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (hasThinking && !isUser) {
                AnimatedVisibility(
                    visible = showThinking,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        ThinkingCard(
                            thinkingContent = thinkingContent,
                            isExpanded = isThinkingExpanded,
                            onToggle = { 
                                isThinkingExpanded = !isThinkingExpanded
                            }
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = !showThinking,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ThinkingHiddenCard(
                        thinkingLength = thinkingContent.length
                    )
                }
            }

            Surface(
                color = if (isUser) VoxPocketUserBubble else VoxPocketAIBubble,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SelectionContainer {
                        if (isUser) {
                            Text(
                                text = message.content,
                                color = VoxPocketTextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            MarkdownText(content = message.content)
                        }
                    }
                }
            }
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Avatar(
                isUser = true
            )
        }
    }
}

@Composable
fun Avatar(
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isUser) VoxPocketUserBubble else VoxPocketPrimary,
        shape = CircleShape,
        modifier = modifier.size(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = if (isUser) "用户" else "AI",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                tint = VoxPocketTextPrimary
            )
        }
    }
}

@Composable
fun ThinkingCard(
    thinkingContent: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "思考过程",
                    tint = VoxPocketPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "思考过程",
                    style = MaterialTheme.typography.titleSmall,
                    color = VoxPocketTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${thinkingContent.length}字)",
                    style = MaterialTheme.typography.bodySmall,
                    color = VoxPocketTextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = VoxPocketTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Divider(
                         color = VoxPocketTextSecondary.copy(alpha = 0.3f),
                         thickness = 0.5f.dp
                     )
                    SelectionContainer {
                        Text(
                            text = thinkingContent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall,
                            color = VoxPocketTextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThinkingHiddenCard(
    thinkingLength: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = VoxPocketPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = VoxPocketPrimary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "有思考过程",
                tint = VoxPocketPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "思考过程已隐藏 (${thinkingLength}字)",
                style = MaterialTheme.typography.bodyMedium,
                color = VoxPocketPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StreamingThinkingIndicator(
    thinkingContent: String,
    isLoading: Boolean
) {
    if (!isLoading) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "思考中",
                    tint = VoxPocketPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "思考中...",
                    style = MaterialTheme.typography.titleSmall,
                    color = VoxPocketTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = VoxPocketPrimary,
                    trackColor = VoxPocketTextSecondary.copy(alpha = 0.3f)
                )
            }

            if (thinkingContent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    color = VoxPocketTextSecondary.copy(alpha = 0.2f),
                    thickness = 0.5f.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thinkingContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    color = VoxPocketTextSecondary,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MarkdownText(content: String) {
    val annotatedString = remember(content) {
        buildAnnotatedString {
            var inCodeBlock = false
            val lines = content.split("\n")
            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                
                when {
                    line.startsWith("```") -> {
                        inCodeBlock = !inCodeBlock
                        if (inCodeBlock) {
                            append("  ") // 代码块开始标记
                        }
                    }
                    inCodeBlock -> {
                        withStyle(SpanStyle(
                            color = Color(0xFF6B8E23), // 绿色代码
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )) {
                            append(line)
                        }
                    }
                    line.startsWith("### ") -> {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )) {
                            appendInlineFormatted(line.removePrefix("### "))
                        }
                    }
                    line.startsWith("## ") -> {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )) {
                            appendInlineFormatted(line.removePrefix("## "))
                        }
                    }
                    line.startsWith("# ") -> {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )) {
                            appendInlineFormatted(line.removePrefix("# "))
                        }
                    }
                    line.startsWith("> ") -> {
                        withStyle(SpanStyle(
                            color = Color(0xFFA9A9A9), // 灰色
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )) {
                            append("│ ")
                            appendInlineFormatted(line.removePrefix("> "))
                        }
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        append("• ")
                        appendInlineFormatted(line.substring(2))
                    }
                    line.matches(Regex("^\\d+\\. .+$")) -> {
                        val numberPart = line.takeWhile { it != '.' } + ". "
                        append(numberPart)
                        appendInlineFormatted(line.substring(numberPart.length))
                    }
                    line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> {
                        append("────────────────────────")
                    }
                    else -> {
                        appendInlineFormatted(line)
                    }
                }
                
                if (i != lines.lastIndex) {
                    append("\n")
                }
                i++
            }
        }
    }

    Text(
        text = annotatedString,
        color = Color.White, // 默认白色
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineFormatted(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                i += 2
                val end = text.indexOf("**", i)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineFormatted(text.substring(i, end))
                    }
                    i = end + 2
                } else {
                    append("**")
                }
            }
            i + 1 < text.length && text[i] == '*' && 
                    (i == 0 || text[i - 1] == ' ') && 
                    (text.indexOf('*', i + 1) != -1) -> {
                i += 1
                val end = text.indexOf('*', i)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        appendInlineFormatted(text.substring(i, end))
                    }
                    i = end + 1
                } else {
                    append("*")
                }
            }
            i + 1 < text.length && text[i] == '~' && text[i + 1] == '~' -> {
                i += 2
                val end = text.indexOf("~~", i)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendInlineFormatted(text.substring(i, end))
                    }
                    i = end + 2
                } else {
                    append("~~")
                }
            }
            text[i] == '`' -> {
                i += 1
                val end = text.indexOf('`', i)
                if (end != -1) {
                    withStyle(SpanStyle(
                        color = Color(0xFF6B8E23), // 绿色代码
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = Color(0xFF333333) // 灰底
                    )) {
                        append(text.substring(i, end))
                    }
                    i = end + 1
                } else {
                    append("`")
                }
            }
            text[i] == '[' -> {
                i += 1
                val linkTextEnd = text.indexOf(']', i)
                val urlStart = text.indexOf('(', linkTextEnd)
                val urlEnd = text.indexOf(')', urlStart)
                if (linkTextEnd != -1 && urlStart != -1 && urlEnd != -1) {
                    val linkText = text.substring(i, linkTextEnd)
                    withStyle(SpanStyle(
                        color = Color(0xFF64B5F6), // 蓝色链接
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(linkText)
                    }
                    i = urlEnd + 1
                } else {
                    append("[")
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
