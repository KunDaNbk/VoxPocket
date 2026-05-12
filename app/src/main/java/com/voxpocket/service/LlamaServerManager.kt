package com.voxpocket.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.*

data class ServerLog(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false,
    val isApiLog: Boolean = false
)

class LlamaServerManager(private val context: Context) {
    private var process: Process? = null
    private var currentPort: Int = 0
    
    private val _serverLogs = MutableStateFlow<List<ServerLog>>(emptyList())
    val serverLogs: StateFlow<List<ServerLog>> = _serverLogs.asStateFlow()
    
    private val _rawLogs = MutableStateFlow("")
    val rawLogs: StateFlow<String> = _rawLogs.asStateFlow()
    
    private var logFile: java.io.File? = null
    private var logWriter: java.io.FileWriter? = null

    fun getServerAddress(): String = if (currentPort > 0) "http://127.0.0.1:$currentPort" else ""
    
    fun getCurrentPort(): Int = currentPort
    
    fun getApiLogger(): (String) -> Unit = { message -> logApiMessage(message) }
    
    private fun logApiMessage(message: String) {
        val log = ServerLog(
            timestamp = System.currentTimeMillis(),
            message = message,
            isError = false,
            isApiLog = true
        )
        val currentLogs = _serverLogs.value.toMutableList()
        currentLogs.add(log)
        _serverLogs.value = currentLogs
        
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date(log.timestamp))
        _rawLogs.value += "[$timestamp] $message\n"
        
        writeToFile("[$timestamp] $message")
    }

    suspend fun start(modelPath: String, maxRetries: Int = 3): Result<Int> = withContext(Dispatchers.IO) {
        clearLogs()
        
        repeat(maxRetries) { attempt ->
            logMessage("=== 启动尝试 ${attempt + 1}/$maxRetries ===")
            
            val result = tryStartOnce(modelPath)
            if (result.isSuccess) {
                return@withContext result
            }
            
            logError("本次启动失败: ${result.exceptionOrNull()?.message}")
            
            if (attempt < maxRetries - 1) {
                logMessage("等待 1 秒后重试...")
                delay(1000)
            }
        }
        
        return@withContext Result.failure(IllegalStateException("经过 $maxRetries 次尝试后仍无法启动服务器"))
    }
    
    private suspend fun tryStartOnce(modelPath: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            logMessage("正在启动 llama-server...")
            
            // 0. 将 URI 转换为真实文件路径
            val realModelPath = copyModelToInternalStorage(modelPath)
            logMessage("模型文件路径: $realModelPath")
            
            // 1. 杀死所有旧的 llama-server 进程
            killExistingLlamaServer()
            
            // 2. 获取二进制文件路径
            val binary = getNativeBinary()
            logMessage("二进制文件路径: ${binary.absolutePath}")
            logMessage("二进制文件存在: ${binary.exists()}")
            
            if (!binary.exists()) {
                return@withContext Result.failure(IllegalStateException("Binary not found"))
            }
            
            // 3. 使用 ServerSocket(0) 获取系统分配的空闲端口
            val port = getFreePortFromSystem()
            currentPort = port
            logMessage("系统分配空闲端口: $port")
            
            // 4. 构建启动命令
            val libDir = binary.parentFile?.absolutePath ?: "/data/app/com.voxpocket/lib/arm64"
            val shPath = "/system/bin/sh"
            val command = "export LD_LIBRARY_PATH=\"$libDir\" && ${binary.absolutePath} -m \"$realModelPath\" --host 127.0.0.1 --port $port"
            
            logMessage("库目录: $libDir")
            logMessage("执行命令: $command")
            
            // 5. 启动进程
            val processBuilder = ProcessBuilder(shPath, "-c", command)
            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(false)
            
            process = processBuilder.start()
            
            // 6. 启动日志读取协程
            val outputReader = BufferedReader(InputStreamReader(process!!.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process!!.errorStream))
            
            val readerJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    var line: String?
                    while (isActive && process?.isAlive == true) {
                        line = outputReader.readLine()
                        line?.let { logMessage(it) } ?: break
                    }
                } catch (e: Exception) {
                    logError("输出读取异常: ${e.message}")
                }
            }
            
            val errorJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    var line: String?
                    while (isActive && process?.isAlive == true) {
                        line = errorReader.readLine()
                        line?.let { logError(it) } ?: break
                    }
                } catch (e: Exception) {
                    logError("错误读取异常: ${e.message}")
                }
            }
            
            // 7. 等待服务器就绪
            logMessage("等待服务器启动...")
            val started = waitForServer(port)
            
            readerJob.cancel()
            errorJob.cancel()
            
            if (started) {
                logMessage("服务器启动成功!")
                logMessage("服务器地址: ${getServerAddress()}")
                Result.success(port)
            } else {
                logError("服务器启动超时!")
                process?.destroy()
                Result.failure(IllegalStateException("Server startup timeout"))
            }
        } catch (e: Exception) {
            logError("启动失败: ${e.message}")
            process?.destroy()
            Result.failure(e)
        } finally {
            closeLogFile()
        }
    }

    private fun copyModelToInternalStorage(uriOrPath: String): String {
        if (!uriOrPath.startsWith("content://")) {
            logMessage("模型路径已是文件路径: $uriOrPath")
            return uriOrPath
        }
        
        logMessage("检测到 content:// URI，正在复制到内部存储...")
        
        val uri = Uri.parse(uriOrPath)
        val fileName = getFileNameFromUri(uri) ?: "model.bin"
        
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        val destFile = File(modelsDir, fileName)
        
        if (destFile.exists()) {
            logMessage("模型已存在于: ${destFile.absolutePath}")
            return destFile.absolutePath
        }
        
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logMessage("模型复制完成: ${destFile.absolutePath}")
            logMessage("文件大小: ${destFile.length() / 1024 / 1024} MB")
        } catch (e: Exception) {
            logError("复制模型文件失败: ${e.message}")
            throw e
        }
        
        return destFile.absolutePath
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            logMessage("获取文件名失败: ${e.message}")
        }
        
        if (fileName == null) {
            fileName = uri.lastPathSegment ?: "model.bin"
        }
        
        return fileName
    }

    private fun getFreePortFromSystem(): Int {
        return ServerSocket(0).use { socket ->
            socket.localPort
        }
    }

    private fun killExistingLlamaServer() {
        try {
            logMessage("正在清理旧的 llama-server 进程...")
            val process = ProcessBuilder("/system/bin/sh", "-c", "pkill -9 llama-server 2>/dev/null || true").start()
            process.waitFor()
            Thread.sleep(300)
            logMessage("旧进程清理完成")
        } catch (e: Exception) {
            logMessage("清理旧进程时出错: ${e.message}")
        }
    }

    private fun getNativeBinary(): File {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        android.util.Log.d("LlamaServerManager", "nativeLibraryDir: $nativeLibDir")
        
        val possiblePaths = listOf(
            File(nativeLibDir, "libllama-server.so"),
            File(nativeLibDir.replace("arm64", "arm64-v8a"), "libllama-server.so")
        )
        
        for (file in possiblePaths) {
            android.util.Log.d("LlamaServerManager", "Checking: ${file.absolutePath}, exists: ${file.exists()}")
            if (file.exists()) {
                return file
            }
        }
        
        val fallbackFile = File(context.filesDir, "llama-server")
        if (!fallbackFile.exists()) {
            android.util.Log.d("LlamaServerManager", "Falling back to assets copy")
            try {
                context.assets.open("llama-server.bin").use { input ->
                    fallbackFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Runtime.getRuntime().exec(arrayOf("chmod", "755", fallbackFile.absolutePath)).waitFor()
            } catch (e: Exception) {
                android.util.Log.e("LlamaServerManager", "Failed to copy from assets", e)
            }
        }
        
        return fallbackFile
    }

    private suspend fun waitForServer(port: Int): Boolean {
        val client = OkHttpClient()
        val url = "http://127.0.0.1:$port/health"
        
        repeat(60) {
            delay(500)
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    return true
                }
            } catch (_: Exception) { }
        }
        return false
    }

    private fun clearLogs() {
        _serverLogs.value = emptyList()
        _rawLogs.value = ""
        prepareLogFile()
    }
    
    fun clearServerLogs() {
        _serverLogs.value = emptyList()
        _rawLogs.value = ""
    }

    private fun logMessage(message: String) {
        val log = ServerLog(message = message, isError = false)
        appendLog(log)
    }

    private fun logError(message: String) {
        val log = ServerLog(message = message, isError = true)
        appendLog(log)
    }

    private fun appendLog(log: ServerLog) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date(log.timestamp))
        val formattedMessage = "[$timestamp] ${log.message}"
        
        val currentLogs = _serverLogs.value.toMutableList()
        currentLogs.add(log)
        _serverLogs.value = currentLogs
        
        _rawLogs.value += "$formattedMessage\n"
        
        writeToFile(formattedMessage)
    }

    private fun prepareLogFile() {
        try {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "llama_server_${sdf.format(Date())}.log"
            logFile = File(logDir, fileName)
            logWriter = FileWriter(logFile, true)
        } catch (e: Exception) {
            android.util.Log.e("LlamaServerManager", "Failed to prepare log file", e)
        }
    }

    private fun writeToFile(message: String) {
        try {
            logWriter?.write("$message\n")
            logWriter?.flush()
        } catch (e: Exception) {
            android.util.Log.e("LlamaServerManager", "Failed to write to log file", e)
        }
    }

    private fun closeLogFile() {
        try {
            logWriter?.close()
            logWriter = null
            logFile = null
        } catch (e: Exception) {
            android.util.Log.e("LlamaServerManager", "Failed to close log file", e)
        }
    }

    fun stop() {
        logMessage("正在停止服务器...")
        process?.destroy()
        process = null
        currentPort = 0
        logMessage("服务器已停止")
        closeLogFile()
    }

    fun isRunning(): Boolean = process?.isAlive == true
    
    fun getLogFilePath(): String? = logFile?.absolutePath
}
