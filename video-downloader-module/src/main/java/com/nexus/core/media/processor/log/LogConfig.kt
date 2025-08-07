package com.nexus.core.media.processor.log

import java.io.File

/**
 * 日志配置类
 * 
 * 定义媒体处理器的日志相关配置选项。
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
data class LogConfig(
    /** 是否启用Logcat输出 */
    val enableLogcat: Boolean = true,
    
    /** 是否启用文件日志 */
    val enableFileLogging: Boolean = true,
    
    /** 是否启用内存日志 */
    val enableMemoryLogging: Boolean = true,
    
    /** 日志目录 */
    val logDirectory: File? = null,
    
    /** 最大日志文件大小(MB) */
    val maxLogFileSizeMB: Long = 10,
    
    /** 最大日志文件数量 */
    val maxLogFiles: Int = 5,
    
    /** 最大内存日志条目数 */
    val maxMemoryEntries: Int = 1000,
    
    /** 是否启用异步日志 */
    val enableAsyncLogging: Boolean = true,
    
    /** 日志缓冲区大小 */
    val bufferSize: Int = 100,
    
    /** 刷新间隔(毫秒) */
    val flushIntervalMs: Long = 5000L,
    
    /** 最小日志级别 */
    val minLogLevel: LogLevel = LogLevel.DEBUG,
    
    /** 是否启用详细日志 */
    val enableVerboseLogging: Boolean = false,
    
    /** 是否启用性能监控日志 */
    val enablePerformanceLogging: Boolean = false
) {
    
    /**
     * 日志级别枚举
     */
    enum class LogLevel(val priority: Int) {
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARN(5),
        ERROR(6),
        FATAL(7)
    }
    
    /**
     * 验证配置的有效性
     * 
     * @return 错误信息列表，空列表表示配置有效
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (maxLogFileSizeMB <= 0) {
            errors.add("最大日志文件大小必须大于0")
        }
        
        if (maxLogFiles <= 0) {
            errors.add("最大日志文件数量必须大于0")
        }
        
        if (maxMemoryEntries <= 0) {
            errors.add("最大内存日志条目数必须大于0")
        }
        
        if (bufferSize <= 0) {
            errors.add("日志缓冲区大小必须大于0")
        }
        
        if (flushIntervalMs <= 0) {
            errors.add("刷新间隔必须大于0")
        }
        
        logDirectory?.let { dir ->
            try {
                if (dir.exists() && !dir.isDirectory) {
                    errors.add("日志目录路径指向的不是目录: ${dir.absolutePath}")
                } else if (!dir.exists() && !dir.mkdirs()) {
                    errors.add("无法创建日志目录: ${dir.absolutePath}")
                } else {
                    // 目录存在且有效，或者成功创建
                }
            } catch (e: Exception) {
                errors.add("无效的日志目录路径: ${dir.absolutePath}")
            }
        }
        
        return errors
    }
    
    /**
     * 获取有效的日志目录
     * 
     * @param defaultDir 默认目录
     * @return 日志目录路径
     */
    fun getEffectiveLogDirectory(defaultDir: String): String {
        return logDirectory?.absolutePath ?: "$defaultDir/logs"
    }
    
    /**
     * 是否应该记录指定级别的日志
     * 
     * @param level 日志级别
     * @return 是否应该记录
     */
    fun shouldLog(level: LogLevel): Boolean {
        return level.priority >= minLogLevel.priority
    }
    
    companion object {
        /**
         * 创建默认配置
         */
        fun default(): LogConfig {
            return LogConfig()
        }
        
        /**
         * 创建调试配置
         */
        fun debug(): LogConfig {
            return LogConfig(
                enableFileLogging = true,
                enableMemoryLogging = true,
                enableVerboseLogging = true,
                enablePerformanceLogging = true,
                minLogLevel = LogLevel.VERBOSE,
                maxLogFileSizeMB = 50,
                maxLogFiles = 10
            )
        }
        
        /**
         * 创建生产环境配置
         */
        fun production(): LogConfig {
            return LogConfig(
                enableLogcat = false,
                enableFileLogging = true,
                enableMemoryLogging = false,
                enableVerboseLogging = false,
                enablePerformanceLogging = false,
                minLogLevel = LogLevel.WARN,
                maxLogFileSizeMB = 20,
                maxLogFiles = 3
            )
        }
        
        /**
         * 创建最小配置
         */
        fun minimal(): LogConfig {
            return LogConfig(
                enableLogcat = true,
                enableFileLogging = false,
                enableMemoryLogging = false,
                enableVerboseLogging = false,
                enablePerformanceLogging = false,
                minLogLevel = LogLevel.ERROR
            )
        }
    }
}