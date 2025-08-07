package com.nexus.core.media.processor

import android.content.Context
import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.engine.TaskExecutionEngine
import com.nexus.core.media.processor.extractor.MediaExtractor
import com.nexus.core.media.processor.extractor.YoutubeDlExtractor
import com.nexus.core.media.processor.extractor.FFmpegExtractor
import com.nexus.core.media.processor.storage.DataStorageHandler
import com.nexus.core.media.processor.security.AccessControlManager
import com.nexus.core.media.processor.security.CryptoSecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

/**
 * 媒体处理器核心模块
 * 
 * 功能特性：
 * - 媒体信息抓取与解析
 * - 多协议媒体下载（HTTP/HTTPS/M3U8等）
 * - 任务管理与状态监控
 * - 存储路径管理
 * - 权限自动申请
 * - 安全加密保护
 * 
 * 使用示例：
 * ```kotlin
 * // 初始化模块
 * val processor = MediaProcessorCore.initializeProcessor(context) {
 *     outputDirectory = "/sdcard/Downloads/Media"
 *     maxParallelTasks = 3
 *     enableDataEncryption = true
 * }
 * 
 * // 获取媒体信息
 * val mediaInfo = processor.extractMediaInfo("https://example.com/video.mp4")
 * 
 * // 开始处理
 * val taskId = processor.initiateProcessing(mediaInfo)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class MediaProcessorCore private constructor(
    private val applicationContext: Context,
    private val processorConfig: ProcessorConfiguration
) {
    
    companion object {
        @Volatile
        private var SINGLETON_INSTANCE: MediaProcessorCore? = null
        
        /**
         * 初始化媒体处理器核心模块
         * 
         * @param context Android上下文
         * @param configurationBuilder 配置构建器
         * @return 处理器实例
         */
        fun initializeProcessor(
            context: Context,
            configurationBuilder: ProcessorConfiguration.ConfigBuilder.() -> Unit = {}
        ): MediaProcessorCore {
            return SINGLETON_INSTANCE ?: synchronized(this) {
                SINGLETON_INSTANCE ?: run {
                    val configuration = ProcessorConfiguration.ConfigBuilder(context).apply(configurationBuilder).build()
                    val processor = MediaProcessorCore(context.applicationContext, configuration)
                    processor.setupCoreComponents()
                    SINGLETON_INSTANCE = processor
                    processor
                }
            }
        }
        
        /**
         * 获取已初始化的处理器实例
         * 
         * @return 处理器实例，如果未初始化则抛出异常
         * @throws IllegalStateException 处理器未初始化
         */
        fun getProcessorInstance(): MediaProcessorCore {
            return SINGLETON_INSTANCE ?: throw IllegalStateException(
                "MediaProcessorCore 尚未初始化，请先调用 initializeProcessor() 方法"
            )
        }
        
        /**
         * 检查处理器是否已初始化
         * 
         * @return true表示已初始化，false表示未初始化
         */
        fun isProcessorInitialized(): Boolean = SINGLETON_INSTANCE != null
    }
    
    // 处理器作用域
    private val processorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 核心组件
    private lateinit var taskExecutionEngine: TaskExecutionEngine
    private lateinit var dataStorageHandler: DataStorageHandler
    private lateinit var accessControlManager: AccessControlManager
    private lateinit var cryptoSecurityManager: CryptoSecurityManager
    
    // 媒体抓取器映射
    private val mediaExtractorRegistry = mutableMapOf<String, MediaExtractor>()
    
    /**
     * 初始化处理器组件
     */
    private fun setupCoreComponents() {
        // 初始化存储处理器
        dataStorageHandler = DataStorageHandler(applicationContext, processorConfig)
        
        // 初始化权限管理器
        accessControlManager = AccessControlManager(applicationContext, processorConfig)
        
        // 初始化安全管理器
        cryptoSecurityManager = CryptoSecurityManager(applicationContext, processorConfig)
        
        // 初始化任务执行引擎
        taskExecutionEngine = TaskExecutionEngine(applicationContext, processorConfig)
        
        // 注册默认媒体抓取器
        registerDefaultExtractors()
    }
    
    /**
     * 注册默认的媒体抓取器
     */
    private fun registerDefaultExtractors() {
        // YouTube-DL 抓取器（支持大多数媒体网站）
        registerMediaExtractor("youtubedl", YoutubeDlExtractor(applicationContext, processorConfig))
        
        // FFmpeg 抓取器（支持直链和流媒体）
        registerMediaExtractor("ffmpeg", FFmpegExtractor(applicationContext, processorConfig))
    }
    
    /**
     * 注册自定义媒体抓取器
     * 
     * @param extractorName 抓取器名称
     * @param extractor 抓取器实例
     */
    fun registerMediaExtractor(extractorName: String, extractor: MediaExtractor) {
        mediaExtractorRegistry[extractorName] = extractor
    }
    
    /**
     * 获取媒体抓取器
     * 
     * @param extractorName 抓取器名称，默认为"youtubedl"
     * @return 媒体抓取器实例
     * @throws IllegalArgumentException 抓取器不存在
     */
    fun getMediaExtractor(extractorName: String = "youtubedl"): MediaExtractor {
        return mediaExtractorRegistry[extractorName] ?: throw IllegalArgumentException(
            "媒体抓取器 '$extractorName' 不存在，请先注册或使用默认抓取器"
        )
    }
    
    /**
     * 获取任务执行引擎
     * 
     * @return 任务执行引擎实例
     */
    fun getTaskExecutionEngine(): TaskExecutionEngine = taskExecutionEngine
    
    /**
     * 获取存储处理器
     * 
     * @return 存储处理器实例
     */
    fun getDataStorageHandler(): DataStorageHandler = dataStorageHandler
    
    /**
     * 获取权限管理器
     * 
     * @return 权限管理器实例
     */
    fun getAccessControlManager(): AccessControlManager = accessControlManager
    
    /**
     * 获取安全管理器
     * 
     * @return 安全管理器实例
     */
    fun getCryptoSecurityManager(): CryptoSecurityManager = cryptoSecurityManager
    
    /**
     * 获取处理器配置
     * 
     * @return 处理器配置实例
     */
    fun getProcessorConfiguration(): ProcessorConfiguration = processorConfig
    
    /**
     * 清理处理器资源
     */
    suspend fun cleanupProcessor() {
        taskExecutionEngine.cleanup()
        dataStorageHandler.clearAll()
        processorScope.coroutineContext.cancelChildren()
    }
}