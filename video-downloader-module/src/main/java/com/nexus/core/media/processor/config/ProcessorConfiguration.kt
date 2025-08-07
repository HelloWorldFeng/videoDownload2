package com.nexus.core.media.processor.config

import android.content.Context
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * 媒体处理器配置类
 * 
 * 定义了媒体处理器的各种配置选项，
 * 包括存储路径、并发设置、网络配置等。
 * 
 * 主要配置项：
 * - 输出目录配置
 * - 并发任务数量
 * - 网络超时设置
 * - 重试策略
 * - 日志级别
 * - 安全设置
 * 
 * 使用示例：
 * ```kotlin
 * val config = ProcessorConfiguration.ConfigBuilder(context)
 *     .outputDirectory("/sdcard/Downloads/Media")
 *     .maxParallelTasks(3)
 *     .connectionTimeout(30000)
 *     .enableDebugLogging(true)
 *     .build()
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
data class ProcessorConfiguration(
    /** Android上下文 */
    val context: Context,
    
    /** 输出目录路径 */
    val outputDirectory: String,
    
    /** 最大并行任务数 */
    val maxParallelTasks: Int,
    
    /** 连接超时时间(毫秒) */
    val connectionTimeout: Long,
    
    /** 读取超时时间(毫秒) */
    val readTimeout: Long,
    
    /** 写入超时时间(毫秒) */
    val writeTimeout: Long,
    
    /** 最大重试次数 */
    val maxRetryCount: Int,
    
    /** 重试延迟时间(毫秒) */
    val retryDelayMs: Long,
    
    /** 是否启用调试日志 */
    val debugLoggingEnabled: Boolean,
    
    /** 日志级别 */
    val logLevel: LogLevel,
    
    /** 是否启用文件加密 */
    val encryptionEnabled: Boolean,
    
    /** 加密密钥 */
    val encryptionKey: String?,
    
    /** 用户代理字符串 */
    val userAgent: String,
    
    /** 是否允许移动网络下载 */
    val allowMobileNetwork: Boolean,
    
    /** 是否允许漫游网络下载 */
    val allowRoamingNetwork: Boolean,
    
    /** 缓存目录大小限制(MB) */
    val cacheSizeLimitMB: Int,
    
    /** 是否自动清理临时文件 */
    val autoCleanTempFiles: Boolean,
    
    /** 临时文件保留时间(小时) */
    val tempFileRetentionHours: Int,
    
    /** 是否启用断点续传 */
    val resumeEnabled: Boolean,
    
    /** 分片下载大小(KB) */
    val chunkSizeKB: Int,
    
    /** 进度更新间隔(毫秒) */
    val progressUpdateIntervalMs: Long,
    
    /** 自定义请求头 */
    val customHeaders: Map<String, String>,
    
    /** 代理配置 */
    val proxyConfig: ProxyConfig?
) {
    
    /**
     * 日志级别枚举
     */
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }
    
    /**
     * 代理配置
     */
    data class ProxyConfig(
        val host: String,
        val port: Int,
        val username: String? = null,
        val password: String? = null,
        val type: ProxyType = ProxyType.HTTP
    ) {
        enum class ProxyType {
            HTTP, SOCKS
        }
        
        /**
         * 创建代理对象
         */
        fun createProxy(): Proxy {
            val proxyType = when (type) {
                ProxyType.HTTP -> Proxy.Type.HTTP
                ProxyType.SOCKS -> Proxy.Type.SOCKS
            }
            return Proxy(proxyType, InetSocketAddress(host, port))
        }
    }
    
    /**
     * 配置构建器
     * 
     * 使用建造者模式构建配置对象，
     * 提供链式调用和默认值设置。
     * 
     * @param context Android上下文
     */
    class ConfigBuilder(private val context: Context) {
        
        private var outputDirectory: String = getDefaultOutputDirectory(context)
        private var maxParallelTasks: Int = 2
        private var connectionTimeout: Long = 30000L
        private var readTimeout: Long = 60000L
        private var writeTimeout: Long = 60000L
        private var maxRetryCount: Int = 3
        private var retryDelayMs: Long = 1000L
        private var debugLoggingEnabled: Boolean = false
        private var logLevel: LogLevel = LogLevel.INFO
        private var encryptionEnabled: Boolean = false
        private var encryptionKey: String? = null
        private var userAgent: String = "MediaProcessor/1.0"
        private var allowMobileNetwork: Boolean = true
        private var allowRoamingNetwork: Boolean = false
        private var cacheSizeLimitMB: Int = 500
        private var autoCleanTempFiles: Boolean = true
        private var tempFileRetentionHours: Int = 24
        private var resumeEnabled: Boolean = true
        private var chunkSizeKB: Int = 1024
        private var progressUpdateIntervalMs: Long = 500L
        private var customHeaders: Map<String, String> = emptyMap()
        private var proxyConfig: ProxyConfig? = null
        
        /**
         * 设置输出目录
         * 
         * @param directory 目录路径
         * @return 构建器实例
         */
        fun outputDirectory(directory: String) = apply {
            this.outputDirectory = directory
        }
        
        /**
         * 设置最大并行任务数
         * 
         * @param count 任务数量
         * @return 构建器实例
         */
        fun maxParallelTasks(count: Int) = apply {
            require(count > 0) { "并行任务数必须大于0" }
            this.maxParallelTasks = count
        }
        
        /**
         * 设置连接超时时间
         * 
         * @param timeout 超时时间(毫秒)
         * @return 构建器实例
         */
        fun connectionTimeout(timeout: Long) = apply {
            require(timeout > 0) { "连接超时时间必须大于0" }
            this.connectionTimeout = timeout
        }
        
        /**
         * 设置读取超时时间
         * 
         * @param timeout 超时时间(毫秒)
         * @return 构建器实例
         */
        fun readTimeout(timeout: Long) = apply {
            require(timeout > 0) { "读取超时时间必须大于0" }
            this.readTimeout = timeout
        }
        
        /**
         * 设置写入超时时间
         * 
         * @param timeout 超时时间(毫秒)
         * @return 构建器实例
         */
        fun writeTimeout(timeout: Long) = apply {
            require(timeout > 0) { "写入超时时间必须大于0" }
            this.writeTimeout = timeout
        }
        
        /**
         * 设置最大重试次数
         * 
         * @param count 重试次数
         * @return 构建器实例
         */
        fun maxRetryCount(count: Int) = apply {
            require(count >= 0) { "重试次数不能小于0" }
            this.maxRetryCount = count
        }
        
        /**
         * 设置重试延迟时间
         * 
         * @param delay 延迟时间(毫秒)
         * @return 构建器实例
         */
        fun retryDelay(delay: Long) = apply {
            require(delay >= 0) { "重试延迟时间不能小于0" }
            this.retryDelayMs = delay
        }
        
        /**
         * 启用调试日志
         * 
         * @param enabled 是否启用
         * @return 构建器实例
         */
        fun enableDebugLogging(enabled: Boolean) = apply {
            this.debugLoggingEnabled = enabled
        }
        
        /**
         * 设置日志级别
         * 
         * @param level 日志级别
         * @return 构建器实例
         */
        fun logLevel(level: LogLevel) = apply {
            this.logLevel = level
        }
        
        /**
         * 启用文件加密
         * 
         * @param enabled 是否启用
         * @param key 加密密钥
         * @return 构建器实例
         */
        fun enableEncryption(enabled: Boolean, key: String? = null) = apply {
            this.encryptionEnabled = enabled
            this.encryptionKey = key
        }
        
        /**
         * 设置用户代理
         * 
         * @param agent 用户代理字符串
         * @return 构建器实例
         */
        fun userAgent(agent: String) = apply {
            this.userAgent = agent
        }
        
        /**
         * 设置网络策略
         * 
         * @param allowMobile 是否允许移动网络
         * @param allowRoaming 是否允许漫游网络
         * @return 构建器实例
         */
        fun networkPolicy(allowMobile: Boolean, allowRoaming: Boolean) = apply {
            this.allowMobileNetwork = allowMobile
            this.allowRoamingNetwork = allowRoaming
        }
        
        /**
         * 设置缓存大小限制
         * 
         * @param sizeMB 大小限制(MB)
         * @return 构建器实例
         */
        fun cacheSizeLimit(sizeMB: Int) = apply {
            require(sizeMB > 0) { "缓存大小限制必须大于0" }
            this.cacheSizeLimitMB = sizeMB
        }
        
        /**
         * 设置临时文件清理策略
         * 
         * @param autoClean 是否自动清理
         * @param retentionHours 保留时间(小时)
         * @return 构建器实例
         */
        fun tempFilePolicy(autoClean: Boolean, retentionHours: Int = 24) = apply {
            this.autoCleanTempFiles = autoClean
            this.tempFileRetentionHours = retentionHours
        }
        
        /**
         * 启用断点续传
         * 
         * @param enabled 是否启用
         * @return 构建器实例
         */
        fun enableResume(enabled: Boolean) = apply {
            this.resumeEnabled = enabled
        }
        
        /**
         * 设置分片下载大小
         * 
         * @param sizeKB 分片大小(KB)
         * @return 构建器实例
         */
        fun chunkSize(sizeKB: Int) = apply {
            require(sizeKB > 0) { "分片大小必须大于0" }
            this.chunkSizeKB = sizeKB
        }
        
        /**
         * 设置进度更新间隔
         * 
         * @param intervalMs 更新间隔(毫秒)
         * @return 构建器实例
         */
        fun progressUpdateInterval(intervalMs: Long) = apply {
            require(intervalMs > 0) { "进度更新间隔必须大于0" }
            this.progressUpdateIntervalMs = intervalMs
        }
        
        /**
         * 设置自定义请求头
         * 
         * @param headers 请求头映射
         * @return 构建器实例
         */
        fun customHeaders(headers: Map<String, String>) = apply {
            this.customHeaders = headers.toMap()
        }
        
        /**
         * 添加自定义请求头
         * 
         * @param key 请求头名称
         * @param value 请求头值
         * @return 构建器实例
         */
        fun addCustomHeader(key: String, value: String) = apply {
            this.customHeaders = this.customHeaders.toMutableMap().apply {
                put(key, value)
            }
        }
        
        /**
         * 设置代理配置
         * 
         * @param config 代理配置
         * @return 构建器实例
         */
        fun proxyConfig(config: ProxyConfig?) = apply {
            this.proxyConfig = config
        }
        
        /**
         * 设置HTTP代理
         * 
         * @param host 代理主机
         * @param port 代理端口
         * @param username 用户名(可选)
         * @param password 密码(可选)
         * @return 构建器实例
         */
        fun httpProxy(host: String, port: Int, username: String? = null, password: String? = null) = apply {
            this.proxyConfig = ProxyConfig(host, port, username, password, ProxyConfig.ProxyType.HTTP)
        }
        
        /**
         * 设置SOCKS代理
         * 
         * @param host 代理主机
         * @param port 代理端口
         * @param username 用户名(可选)
         * @param password 密码(可选)
         * @return 构建器实例
         */
        fun socksProxy(host: String, port: Int, username: String? = null, password: String? = null) = apply {
            this.proxyConfig = ProxyConfig(host, port, username, password, ProxyConfig.ProxyType.SOCKS)
        }
        
        /**
         * 构建配置对象
         * 
         * @return 配置实例
         */
        fun build(): ProcessorConfiguration {
            // 验证输出目录
            validateOutputDirectory()
            
            return ProcessorConfiguration(
                context = context,
                outputDirectory = outputDirectory,
                maxParallelTasks = maxParallelTasks,
                connectionTimeout = connectionTimeout,
                readTimeout = readTimeout,
                writeTimeout = writeTimeout,
                maxRetryCount = maxRetryCount,
                retryDelayMs = retryDelayMs,
                debugLoggingEnabled = debugLoggingEnabled,
                logLevel = logLevel,
                encryptionEnabled = encryptionEnabled,
                encryptionKey = encryptionKey,
                userAgent = userAgent,
                allowMobileNetwork = allowMobileNetwork,
                allowRoamingNetwork = allowRoamingNetwork,
                cacheSizeLimitMB = cacheSizeLimitMB,
                autoCleanTempFiles = autoCleanTempFiles,
                tempFileRetentionHours = tempFileRetentionHours,
                resumeEnabled = resumeEnabled,
                chunkSizeKB = chunkSizeKB,
                progressUpdateIntervalMs = progressUpdateIntervalMs,
                customHeaders = customHeaders,
                proxyConfig = proxyConfig
            )
        }
        
        /**
         * 验证输出目录
         */
        private fun validateOutputDirectory() {
            try {
                val dir = File(outputDirectory)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                if (!dir.canWrite()) {
                    throw IllegalArgumentException("输出目录不可写: $outputDirectory")
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("无效的输出目录: $outputDirectory", e)
            }
        }
        
        companion object {
            /**
             * 获取默认输出目录
             */
            private fun getDefaultOutputDirectory(context: Context): String {
                return context.getExternalFilesDir("MediaProcessor")?.absolutePath
                    ?: File(context.filesDir, "MediaProcessor").absolutePath
            }
        }
    }
    
    /**
     * 获取临时文件目录
     */
    fun getTempDirectory(): String {
        return File(outputDirectory, "temp").absolutePath
    }
    
    /**
     * 获取缓存目录
     */
    fun getCacheDirectory(): String {
        return File(outputDirectory, "cache").absolutePath
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String {
        return File(outputDirectory, "logs/processor.log").absolutePath
    }
    

    
    /**
     * 获取日志目录
     */
    fun getLogDirectory(): String {
        return File(outputDirectory, "logs").absolutePath
    }
    
    /**
     * 获取是否自动清理旧文件
     */
    fun getAutoCleanupOldFiles(): Boolean {
        return autoCleanTempFiles
    }
    
    /**
     * 检查是否允许当前网络类型
     */
    fun isNetworkAllowed(isMobile: Boolean, isRoaming: Boolean): Boolean {
        return when {
            isRoaming && !allowRoamingNetwork -> false
            isMobile && !allowMobileNetwork -> false
            else -> true
        }
    }
    
    /**
     * 获取有效的用户代理字符串
     */
    fun getEffectiveUserAgent(): String {
        return if (userAgent.isBlank()) "MediaProcessor/1.0" else userAgent
    }
    
    /**
     * 验证配置的有效性
     */
    fun validate(): Boolean {
        return try {
            require(maxParallelTasks > 0) { "并行任务数必须大于0" }
            require(connectionTimeout > 0) { "连接超时时间必须大于0" }
            require(readTimeout > 0) { "读取超时时间必须大于0" }
            require(writeTimeout > 0) { "写入超时时间必须大于0" }
            require(maxRetryCount >= 0) { "重试次数不能小于0" }
            require(retryDelayMs >= 0) { "重试延迟时间不能小于0" }
            require(cacheSizeLimitMB > 0) { "缓存大小限制必须大于0" }
            require(tempFileRetentionHours > 0) { "临时文件保留时间必须大于0" }
            require(chunkSizeKB > 0) { "分片大小必须大于0" }
            require(progressUpdateIntervalMs > 0) { "进度更新间隔必须大于0" }
            
            // 验证输出目录
            val outputDir = File(outputDirectory)
            require(outputDir.exists() || outputDir.mkdirs()) { "无法创建输出目录: $outputDirectory" }
            require(outputDir.canWrite()) { "输出目录不可写: $outputDirectory" }
            
            // 验证加密配置
            if (encryptionEnabled) {
                require(!encryptionKey.isNullOrBlank()) { "启用加密时必须提供加密密钥" }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(context: Context): ProcessorConfiguration {
            return ConfigBuilder(context).build()
        }
        
        /**
         * 创建调试配置
         */
        fun createDebug(context: Context): ProcessorConfiguration {
            return ConfigBuilder(context)
                .enableDebugLogging(true)
                .logLevel(LogLevel.DEBUG)
                .maxParallelTasks(1)
                .build()
        }
        
        /**
         * 创建高性能配置
         */
        fun createHighPerformance(context: Context): ProcessorConfiguration {
            return ConfigBuilder(context)
                .maxParallelTasks(4)
                .chunkSize(2048)
                .progressUpdateInterval(1000)
                .cacheSizeLimit(1000)
                .build()
        }
    }
}