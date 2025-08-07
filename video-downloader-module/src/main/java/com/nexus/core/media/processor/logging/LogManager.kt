package com.nexus.core.media.processor.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * 日志级别枚举
 */
enum class LogLevel(val priority: Int, val tag: String) {
    VERBOSE(2, "V"),
    DEBUG(3, "D"),
    INFO(4, "I"),
    WARN(5, "W"),
    ERROR(6, "E"),
    FATAL(7, "F")
}

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val threadName: String = Thread.currentThread().name,
    val className: String? = null,
    val methodName: String? = null,
    val lineNumber: Int? = null
) {
    /**
     * 格式化日志条目
     */
    fun format(formatter: LogFormatter): String {
        return formatter.format(this)
    }
    
    /**
     * 获取格式化时间戳
     */
    fun getFormattedTimestamp(pattern: String = "yyyy-MM-dd HH:mm:ss.SSS"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

/**
 * 日志格式化器接口
 */
interface LogFormatter {
    fun format(entry: LogEntry): String
}

/**
 * 默认日志格式化器
 */
class DefaultLogFormatter : LogFormatter {
    override fun format(entry: LogEntry): String {
        val timestamp = entry.getFormattedTimestamp()
        val location = if (entry.className != null && entry.methodName != null) {
            "${entry.className}.${entry.methodName}:${entry.lineNumber ?: "?"}"
        } else {
            ""
        }
        
        val baseMessage = "$timestamp ${entry.level.tag}/${entry.tag} [${entry.threadName}] $location: ${entry.message}"
        
        return if (entry.throwable != null) {
            "$baseMessage\n${Log.getStackTraceString(entry.throwable)}"
        } else {
            baseMessage
        }
    }
}

/**
 * 简洁日志格式化器
 */
class CompactLogFormatter : LogFormatter {
    override fun format(entry: LogEntry): String {
        val timestamp = entry.getFormattedTimestamp("HH:mm:ss.SSS")
        return "$timestamp ${entry.level.tag}/${entry.tag}: ${entry.message}"
    }
}

/**
 * JSON日志格式化器
 */
class JsonLogFormatter : LogFormatter {
    override fun format(entry: LogEntry): String {
        val json = buildString {
            append("{")
            append("\"timestamp\":${entry.timestamp},")
            append("\"level\":\"${entry.level.name}\",")
            append("\"tag\":\"${entry.tag}\",")
            append("\"message\":\"${entry.message.replace("\"", "\\\"")}\",")
            append("\"thread\":\"${entry.threadName}\",")
            if (entry.className != null) {
                append("\"class\":\"${entry.className}\",")
            }
            if (entry.methodName != null) {
                append("\"method\":\"${entry.methodName}\",")
            }
            if (entry.lineNumber != null) {
                append("\"line\":${entry.lineNumber},")
            }
            if (entry.throwable != null) {
                val stackTrace = Log.getStackTraceString(entry.throwable).replace("\"", "\\\"")
                append("\"exception\":\"$stackTrace\",")
            }
            // 移除最后的逗号
            if (endsWith(",")) {
                deleteCharAt(length - 1)
            }
            append("}")
        }
        return json
    }
}

/**
 * 日志输出器接口
 */
interface LogAppender {
    fun append(entry: LogEntry)
    fun flush()
    fun close()
}

/**
 * Android Logcat 输出器
 */
class LogcatAppender : LogAppender {
    override fun append(entry: LogEntry) {
        val message = if (entry.throwable != null) {
            "${entry.message}\n${Log.getStackTraceString(entry.throwable)}"
        } else {
            entry.message
        }
        
        when (entry.level) {
            LogLevel.VERBOSE -> Log.v(entry.tag, message)
            LogLevel.DEBUG -> Log.d(entry.tag, message)
            LogLevel.INFO -> Log.i(entry.tag, message)
            LogLevel.WARN -> Log.w(entry.tag, message)
            LogLevel.ERROR -> Log.e(entry.tag, message)
            LogLevel.FATAL -> Log.wtf(entry.tag, message)
        }
    }
    
    override fun flush() {
        // Logcat 不需要刷新
    }
    
    override fun close() {
        // Logcat 不需要关闭
    }
}

/**
 * 文件日志输出器
 */
class FileAppender(
    private val logFile: File,
    private val formatter: LogFormatter = DefaultLogFormatter(),
    private val maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    private val maxBackupFiles: Int = 5
) : LogAppender {
    
    private var writer: PrintWriter? = null
    private val writtenBytes = AtomicLong(0)
    
    init {
        initWriter()
    }
    
    private fun initWriter() {
        try {
            // 确保父目录存在
            logFile.parentFile?.mkdirs()
            
            // 检查文件大小，如果超过限制则轮转
            if (logFile.exists() && logFile.length() > maxFileSize) {
                rotateLogFile()
            }
            
            writer = PrintWriter(FileWriter(logFile, true))
            writtenBytes.set(if (logFile.exists()) logFile.length() else 0)
        } catch (e: Exception) {
            Log.e("FileAppender", "Failed to initialize log file writer", e)
        }
    }
    
    override fun append(entry: LogEntry) {
        try {
            val formattedMessage = formatter.format(entry)
            writer?.println(formattedMessage)
            
            val messageBytes = formattedMessage.toByteArray().size + 1 // +1 for newline
            val newSize = writtenBytes.addAndGet(messageBytes.toLong())
            
            // 检查是否需要轮转日志文件
            if (newSize > maxFileSize) {
                rotateLogFile()
            }
        } catch (e: Exception) {
            Log.e("FileAppender", "Failed to write log entry", e)
        }
    }
    
    override fun flush() {
        writer?.flush()
    }
    
    override fun close() {
        writer?.close()
        writer = null
    }
    
    private fun rotateLogFile() {
        try {
            close()
            
            // 轮转备份文件
            for (i in maxBackupFiles - 1 downTo 1) {
                val oldFile = File("${logFile.absolutePath}.$i")
                val newFile = File("${logFile.absolutePath}.${i + 1}")
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile)
                }
            }
            
            // 将当前文件重命名为 .1
            val backupFile = File("${logFile.absolutePath}.1")
            logFile.renameTo(backupFile)
            
            // 重新初始化写入器
            initWriter()
        } catch (e: Exception) {
            Log.e("FileAppender", "Failed to rotate log file", e)
        }
    }
}

/**
 * 内存日志输出器
 */
class MemoryAppender(
    private val maxEntries: Int = 1000
) : LogAppender {
    
    private val logEntries = ConcurrentLinkedQueue<LogEntry>()
    
    override fun append(entry: LogEntry) {
        logEntries.offer(entry)
        
        // 保持最大条目数限制
        while (logEntries.size > maxEntries) {
            logEntries.poll()
        }
    }
    
    override fun flush() {
        // 内存输出器不需要刷新
    }
    
    override fun close() {
        logEntries.clear()
    }
    
    /**
     * 获取所有日志条目
     */
    fun getLogEntries(): List<LogEntry> {
        return logEntries.toList()
    }
    
    /**
     * 获取指定级别的日志条目
     */
    fun getLogEntries(level: LogLevel): List<LogEntry> {
        return logEntries.filter { it.level == level }
    }
    
    /**
     * 获取指定时间范围的日志条目
     */
    fun getLogEntries(startTime: Long, endTime: Long): List<LogEntry> {
        return logEntries.filter { it.timestamp in startTime..endTime }
    }
}

/**
 * 日志管理器配置
 */
data class LogConfig(
    val minLevel: LogLevel = LogLevel.DEBUG,
    val enableLogcat: Boolean = true,
    val enableFileLogging: Boolean = true,
    val enableMemoryLogging: Boolean = true,
    val logDirectory: File? = null,
    val maxFileSize: Long = 10 * 1024 * 1024,
    val maxBackupFiles: Int = 5,
    val maxMemoryEntries: Int = 1000,
    val asyncLogging: Boolean = true,
    val bufferSize: Int = 100,
    val flushInterval: Long = 5000L, // 5秒
    val formatter: LogFormatter = DefaultLogFormatter()
)

/**
 * 日志管理器
 * 
 * 负责媒体处理器的日志管理，
 * 支持多种输出方式和格式化选项。
 * 
 * 主要功能：
 * - 多级别日志记录
 * - 多种输出方式
 * - 异步日志处理
 * - 日志轮转和清理
 * 
 * 使用示例：
 * ```kotlin
 * val logManager = LogManager(context, config)
 * logManager.d("TAG", "Debug message")
 * logManager.e("TAG", "Error message", exception)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class LogManager(
    private val context: Context,
    private val config: LogConfig = LogConfig()
) {
    
    companion object {
        private const val DEFAULT_TAG = "MediaProcessor"
        private const val LOG_FILE_NAME = "media_processor.log"
        
        @Volatile
        private var instance: LogManager? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(context: Context, config: LogConfig = LogConfig()): LogManager {
            return instance ?: synchronized(this) {
                instance ?: LogManager(context, config).also { instance = it }
            }
        }
    }
    
    private val appenders = mutableListOf<LogAppender>()
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 日志事件流
    private val _logEvents = MutableSharedFlow<LogEntry>(replay = 0)
    val logEvents: SharedFlow<LogEntry> = _logEvents.asSharedFlow()
    
    // 统计信息
    private val logCounts = mutableMapOf<LogLevel, AtomicLong>()
    private var startTime = System.currentTimeMillis()
    
    init {
        initializeAppenders()
        initializeLogCounts()
        
        if (config.asyncLogging) {
            startAsyncLogging()
        }
        
        // 定期刷新日志
        startPeriodicFlush()
    }
    
    private fun initializeAppenders() {
        // Logcat 输出器
        if (config.enableLogcat) {
            appenders.add(LogcatAppender())
        }
        
        // 文件输出器
        if (config.enableFileLogging) {
            val logDir = config.logDirectory ?: File(context.filesDir, "logs")
            val logFile = File(logDir, LOG_FILE_NAME)
            appenders.add(
                FileAppender(
                    logFile = logFile,
                    formatter = config.formatter,
                    maxFileSize = config.maxFileSize,
                    maxBackupFiles = config.maxBackupFiles
                )
            )
        }
        
        // 内存输出器
        if (config.enableMemoryLogging) {
            appenders.add(MemoryAppender(config.maxMemoryEntries))
        }
    }
    
    private fun initializeLogCounts() {
        LogLevel.values().forEach { level ->
            logCounts[level] = AtomicLong(0)
        }
    }
    
    private fun startAsyncLogging() {
        coroutineScope.launch {
            for (entry in logChannel) {
                processLogEntry(entry)
            }
        }
    }
    
    private fun startPeriodicFlush() {
        coroutineScope.launch {
            while (isActive) {
                delay(config.flushInterval)
                flush()
            }
        }
    }
    
    /**
     * 记录 VERBOSE 级别日志
     */
    fun v(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }
    
    /**
     * 记录 DEBUG 级别日志
     */
    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    /**
     * 记录 INFO 级别日志
     */
    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    /**
     * 记录 WARN 级别日志
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    /**
     * 记录 ERROR 级别日志
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    /**
     * 记录 FATAL 级别日志
     */
    fun f(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.FATAL, tag, message, throwable)
    }
    
    /**
     * 记录日志
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level.priority < config.minLevel.priority) {
            return
        }
        
        val stackTrace = Thread.currentThread().stackTrace
        val callerElement = findCallerStackElement(stackTrace)
        
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            className = callerElement?.className,
            methodName = callerElement?.methodName,
            lineNumber = callerElement?.lineNumber
        )
        
        // 更新统计
        logCounts[level]?.incrementAndGet()
        
        // 发送日志事件
        _logEvents.tryEmit(entry)
        
        if (config.asyncLogging) {
            // 异步处理
            logChannel.trySend(entry)
        } else {
            // 同步处理
            processLogEntry(entry)
        }
    }
    
    /**
     * 处理日志条目
     */
    private fun processLogEntry(entry: LogEntry) {
        appenders.forEach { appender ->
            try {
                appender.append(entry)
            } catch (e: Exception) {
                // 避免日志记录本身出错导致的循环
                System.err.println("Failed to append log entry: ${e.message}")
            }
        }
    }
    
    /**
     * 查找调用者堆栈元素
     */
    private fun findCallerStackElement(stackTrace: Array<StackTraceElement>): StackTraceElement? {
        var found = false
        for (element in stackTrace) {
            if (element.className == LogManager::class.java.name) {
                found = true
                continue
            }
            if (found && !element.className.startsWith("java.lang.Thread")) {
                return element
            }
        }
        return null
    }
    
    /**
     * 刷新所有输出器
     */
    fun flush() {
        appenders.forEach { appender ->
            try {
                appender.flush()
            } catch (e: Exception) {
                System.err.println("Failed to flush appender: ${e.message}")
            }
        }
    }
    
    /**
     * 获取内存中的日志条目
     */
    fun getMemoryLogs(): List<LogEntry> {
        return appenders.filterIsInstance<MemoryAppender>()
            .firstOrNull()?.getLogEntries() ?: emptyList()
    }
    
    /**
     * 获取日志统计信息
     */
    fun getLogStatistics(): Map<LogLevel, Long> {
        return logCounts.mapValues { it.value.get() }
    }
    
    /**
     * 获取运行时间
     */
    fun getUptime(): Long {
        return System.currentTimeMillis() - startTime
    }
    
    /**
     * 清理旧日志文件
     */
    fun cleanupOldLogs(olderThanMs: Long = 7 * 24 * 60 * 60 * 1000L) {
        coroutineScope.launch {
            try {
                val logDir = config.logDirectory ?: File(context.filesDir, "logs")
                if (!logDir.exists()) return@launch
                
                val cutoffTime = System.currentTimeMillis() - olderThanMs
                
                logDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < cutoffTime) {
                        file.delete()
                        d("LogManager", "Deleted old log file: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                e("LogManager", "Failed to cleanup old logs", e)
            }
        }
    }
    
    /**
     * 导出日志
     */
    fun exportLogs(outputFile: File, includeMemoryLogs: Boolean = true): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            
            outputFile.printWriter().use { writer ->
                // 写入文件日志
                val logDir = config.logDirectory ?: File(context.filesDir, "logs")
                val logFile = File(logDir, LOG_FILE_NAME)
                
                if (logFile.exists()) {
                    writer.println("=== File Logs ===")
                    logFile.readLines().forEach { line ->
                        writer.println(line)
                    }
                    writer.println()
                }
                
                // 写入内存日志
                if (includeMemoryLogs) {
                    writer.println("=== Memory Logs ===")
                    getMemoryLogs().forEach { entry ->
                        writer.println(config.formatter.format(entry))
                    }
                }
                
                // 写入统计信息
                writer.println("\n=== Statistics ===")
                getLogStatistics().forEach { (level, count) ->
                    writer.println("${level.name}: $count")
                }
                writer.println("Uptime: ${getUptime()}ms")
            }
            
            true
        } catch (e: Exception) {
            e("LogManager", "Failed to export logs", e)
            false
        }
    }
    
    /**
     * 关闭日志管理器
     */
    fun close() {
        coroutineScope.cancel()
        
        appenders.forEach { appender ->
            try {
                appender.close()
            } catch (e: Exception) {
                System.err.println("Failed to close appender: ${e.message}")
            }
        }
        
        appenders.clear()
        logBuffer.clear()
        logChannel.close()
    }
}