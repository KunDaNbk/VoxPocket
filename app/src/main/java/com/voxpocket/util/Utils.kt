package com.voxpocket.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }
    
    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> formatShortDate(timestamp)
        }
    }
}

object StringUtils {
    fun truncate(text: String, maxLength: Int, suffix: String = "..."): String {
        return if (text.length > maxLength) {
            text.take(maxLength - suffix.length) + suffix
        } else {
            text
        }
    }
    
    fun extractFileName(path: String): String {
        return path.substringAfterLast("/").substringAfterLast("\\")
    }
    
    fun extractFileExtension(path: String): String {
        return path.substringAfterLast(".", "")
    }
    
    fun isValidModelFile(path: String): Boolean {
        val extension = extractFileExtension(path).lowercase()
        return extension in listOf("gguf", "bin", "model")
    }
}

object NumberUtils {
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            else -> "${seconds / 3600}时${(seconds % 3600) / 60}分"
        }
    }
    
    fun estimateTokens(text: String): Int {
        return text.length / 3 + text.codePointCount(0, text.length) / 2
    }
}

