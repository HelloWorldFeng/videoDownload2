package com.nexus.core.media.processor.config

import com.nexus.core.media.processor.cache.CacheConfig
import com.nexus.core.media.processor.log.LogConfig
import com.nexus.core.media.processor.model.TaskPriority
import com.nexus.core.media.processor.network.NetworkConfig
import com.nexus.core.media.processor.security.SecurityConfig
import com.nexus.core.media.processor.storage.StorageConfig
import java.io.File

/**
 * 媒体处理器配置
 */
data class MediaProcessorConfig(
    // 任务执行配置
    val maxConcurrentTasks: Int = 3,
    val maxRetryCount: Int = 3,
    val taskTimeoutMs: Long = 30 * 60 * 1000L, // 30分钟
    val defaultPriority: TaskPriority = TaskPriority.NORMAL,
    
    // 性能配置
    val enableHardwareAcceleration: Boolean = true,
    val maxMemoryUsageMB: Int = 512,
    val enableBackgroundProcessing: Boolean = true,
    
    // 网络配置
    val networkConfig: NetworkConfig = NetworkConfig(),
    
    // 存储配置
    val storageConfig: StorageConfig = StorageConfig(),
    
    // 缓存配置
    val cacheConfig: CacheConfig = CacheConfig(),
    
    // 日志配置
    val logConfig: LogConfig = LogConfig(),
    
    // 安全配置
    val securityConfig: SecurityConfig = SecurityConfig(),
    
    // 调试配置
    val enableDebugMode: Boolean = false,
    val enablePerformanceMonitoring: Boolean = false,
    val enableDetailedLogging: Boolean = false
) {
    companion object {
        /**
         * 默认配置
         */
        fun default(): MediaProcessorConfig {
            return MediaProcessorConfig()
        }
        
        /**
         * 高性能配置
         */
        fun highPerformance(): MediaProcessorConfig {
            return MediaProcessorConfig(
                maxConcurrentTasks = 5,
                maxMemoryUsageMB = 1024,
                enableHardwareAcceleration = true,
                networkConfig = NetworkConfig(
                    connectTimeout = 10000,
                    readTimeout = 30000,
                    writeTimeout = 30000,
                    retryOnConnectionFailure = true,
                    enableGzip = true
                ),
                cacheConfig = CacheConfig(
                    maxMemorySize = 128 * 1024 * 1024,
                    maxDiskSize = 1024 * 1024 * 1024,
                    enableMemoryCache = true,
                    enableDiskCache = true
                )
            )
        }
        
        /**
         * 低内存配置
         */
        fun lowMemory(): MediaProcessorConfig {
            return MediaProcessorConfig(
                maxConcurrentTasks = 1,
                maxMemoryUsageMB = 128,
                enableHardwareAcceleration = false,
                cacheConfig = CacheConfig(
                    maxMemorySize = 32 * 1024 * 1024,
                    maxDiskSize = 256 * 1024 * 1024,
                    enableMemoryCache = true,
                    enableDiskCache = true
                )
            )
        }
        
        /**
         * 调试配置
         */
        fun debug(): MediaProcessorConfig {
            return MediaProcessorConfig(
                enableDebugMode = true,
                enablePerformanceMonitoring = true,
                enableDetailedLogging = true,
                logConfig = LogConfig(
                    enableFileLogging = true,
                    enableMemoryLogging = true,
                    maxLogFileSizeMB = 50,
                    maxLogFiles = 10
                )
            )
        }
    }
    
    /**
     * 验证配置
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (maxConcurrentTasks <= 0) {
            errors.add("最大并发任务数必须大于0")
        }
        
        if (maxConcurrentTasks > 10) {
            errors.add("最大并发任务数不应超过10")
        }
        
        if (maxRetryCount < 0) {
            errors.add("最大重试次数不能为负数")
        }
        
        if (taskTimeoutMs <= 0) {
            errors.add("任务超时时间必须大于0")
        }
        
        if (maxMemoryUsageMB <= 0) {
            errors.add("最大内存使用量必须大于0")
        }
        
        // 验证子配置
        errors.addAll(networkConfig.validate().map { "网络配置: $it" })
        errors.addAll(storageConfig.validate().map { "存储配置: $it" })
        errors.addAll(cacheConfig.validate().map { "缓存配置: $it" })
        errors.addAll(logConfig.validate().map { "日志配置: $it" })
        errors.addAll(securityConfig.validate().map { "安全配置: $it" })
        
        return errors
    }
    
    /**
     * 是否为有效配置
     */
    fun isValid(): Boolean {
        return validate().isEmpty()
    }
    
    /**
     * 获取配置摘要
     */
    fun getSummary(): String {
        return buildString {
            appendLine("媒体处理器配置摘要:")
            appendLine("- 最大并发任务: $maxConcurrentTasks")
            appendLine("- 最大重试次数: $maxRetryCount")
            appendLine("- 任务超时: ${taskTimeoutMs / 1000}秒")
            appendLine("- 最大内存使用: ${maxMemoryUsageMB}MB")
            appendLine("- 硬件加速: ${if (enableHardwareAcceleration) "启用" else "禁用"}")
            appendLine("- 后台处理: ${if (enableBackgroundProcessing) "启用" else "禁用"}")
            appendLine("- 调试模式: ${if (enableDebugMode) "启用" else "禁用"}")
        }
    }
    

}