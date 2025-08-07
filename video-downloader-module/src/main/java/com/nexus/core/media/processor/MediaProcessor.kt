package com.nexus.core.media.processor

import android.content.Context
import com.nexus.core.media.processor.cache.CacheManager
import com.nexus.core.media.processor.config.MediaProcessorConfig
import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.execution.TaskExecutionEngine
import com.nexus.core.media.processor.fetcher.MediaInfoExtractor
import com.nexus.core.media.processor.logging.LogManager
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.model.TaskPriority
import com.nexus.core.media.processor.state.ProcessingStatistics
import com.nexus.core.media.processor.network.NetworkManager
import com.nexus.core.media.processor.security.SecurityManager
import com.nexus.core.media.processor.state.MediaProcessingState
import com.nexus.core.media.processor.state.MediaProcessingStateManager
import com.nexus.core.media.processor.storage.MediaStorageManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 媒体处理器主类
 * 提供媒体下载、转换和处理的统一接口
 */
class MediaProcessor private constructor(
    private val context: Context,
    private val config: MediaProcessorConfig
) {
    companion object {
        @Volatile
        private var INSTANCE: MediaProcessor? = null
        
        /**
         * 获取媒体处理器实例
         */
        fun getInstance(context: Context, config: MediaProcessorConfig? = null): MediaProcessor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaProcessor(
                    context.applicationContext,
                    config ?: MediaProcessorConfig.default()
                ).also { INSTANCE = it }
            }
        }
        
        /**
         * 销毁实例
         */
        fun destroy() {
            INSTANCE?.cleanup()
            INSTANCE = null
        }
    }
    
    // 核心组件
    private val logManager = LogManager.getInstance(context, convertLogConfig(config.logConfig))
    private val securityManager = SecurityManager(context)
    private val networkManager = NetworkManager(context, config.networkConfig)
    private val cacheManager = CacheManager(context, config.cacheConfig)
    private val storageManager = MediaStorageManager(config.toProcessorConfiguration(context))
    private val stateManager = MediaProcessingStateManager()
    private val mediaInfoExtractor = MediaInfoExtractor(config.toProcessorConfiguration(context))
    private val taskExecutionEngine = TaskExecutionEngine(
        configuration = config.toProcessorConfiguration(context)
    )
    
    // 协程作用域
    private val processorScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineName("MediaProcessor")
    )
    
    // 监听器管理
    private val progressListeners = ConcurrentHashMap<String, (ProcessingTask) -> Unit>()
    private val stateListeners = ConcurrentHashMap<String, (MediaProcessingState) -> Unit>()
    
    init {
        logManager.i("MediaProcessor", "媒体处理器初始化完成")
        
        // 监听任务状态变化
        processorScope.launch {
            taskExecutionEngine.processingState.collect { state ->
                progressListeners.values.forEach { listener ->
                    try {
                        state.activeTasks.forEach { task ->
                            listener(task)
                        }
                    } catch (e: Exception) {
                        logManager.e("MediaProcessor", "进度监听器执行失败", e)
                    }
                }
            }
        }
        
        // 监听全局状态变化
        processorScope.launch {
            stateManager.currentState.collect { state ->
                stateListeners.values.forEach { listener ->
                    try {
                        listener(state)
                    } catch (e: Exception) {
                        logManager.e("MediaProcessor", "状态监听器执行失败", e)
                    }
                }
            }
        }
    }
    
    /**
     * 获取媒体信息
     */
    suspend fun getMediaInfo(url: String): Result<MediaInfo> {
        return try {
            logManager.d("MediaProcessor", "开始获取媒体信息: $url")
            val mediaInfo = mediaInfoExtractor.extractMediaInfo(url)
            logManager.i("MediaProcessor", "成功获取媒体信息: ${mediaInfo.title}")
            Result.success(mediaInfo)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "获取媒体信息失败: $url", e)
            Result.failure(e)
        }
    }
    
    /**
     * 添加处理任务
     */
    suspend fun addTask(
        url: String,
        options: ProcessingOptions = ProcessingOptions()
    ): Result<String> {
        return try {
            logManager.d("MediaProcessor", "开始添加处理任务: $url")
            
            // 获取媒体信息
            val mediaInfo = mediaInfoExtractor.extractMediaInfo(url)
            
            // 选择最佳格式
            val selectedFormat = selectBestFormat(mediaInfo, options)
                ?: return Result.failure(IllegalArgumentException("未找到合适的媒体格式"))
            
            // 添加到执行引擎
            val taskId = taskExecutionEngine.addProcessingTask(
                mediaInfo = mediaInfo,
                selectedFormat = selectedFormat,
                processingOptions = options,
                priority = TaskPriority.NORMAL
            )
            
            logManager.i("MediaProcessor", "成功添加处理任务: $taskId")
            Result.success(taskId)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "添加处理任务失败: $url", e)
            Result.failure(e)
        }
    }
    
    /**
     * 暂停任务
     */
    suspend fun pauseTask(taskId: String): Result<Unit> {
        return try {
            taskExecutionEngine.pauseProcessing(taskId)
            logManager.i("MediaProcessor", "任务已暂停: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "暂停任务失败: $taskId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 恢复任务
     */
    suspend fun resumeTask(taskId: String): Result<Unit> {
        return try {
            logManager.d("MediaProcessor", "恢复任务: $taskId")
            taskExecutionEngine.resumeProcessing(taskId)
            logManager.i("MediaProcessor", "成功恢复任务: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "恢复任务失败: $taskId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 取消任务
     */
    suspend fun cancelTask(taskId: String): Result<Unit> {
        return try {
            logManager.d("MediaProcessor", "取消任务: $taskId")
            taskExecutionEngine.cancelProcessing(taskId)
            logManager.i("MediaProcessor", "成功取消任务: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "取消任务失败: $taskId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            logManager.d("MediaProcessor", "删除任务: $taskId")
            taskExecutionEngine.deleteProcessingTask(taskId)
            logManager.i("MediaProcessor", "成功删除任务: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "删除任务失败: $taskId", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取任务信息
     */
    suspend fun getTask(taskId: String): ProcessingTask? {
        return taskExecutionEngine.getTaskDetails(taskId)
    }
    
    /**
     * 获取所有任务
     */
    suspend fun getAllTasks(): List<ProcessingTask> {
        return taskExecutionEngine.getAllProcessingTasks()
    }
    
    /**
     * 获取指定状态的任务
     */
    suspend fun getTasksByStatus(status: ProcessingStatus): List<ProcessingTask> {
        return taskExecutionEngine.getProcessingTasksByStatus(status)
    }
    
    /**
     * 获取处理状态流
     */
    val stateFlow: StateFlow<MediaProcessingState>
        get() = stateManager.currentState
    
    /**
     * 获取任务更新流
     */
    val taskUpdates: Flow<ProcessingTask>
        get() = taskExecutionEngine.processingState.map { state ->
            state.activeTasks
        }.flatMapConcat { tasks ->
            kotlinx.coroutines.flow.flowOf(*tasks.toTypedArray())
        }
    
    /**
     * 添加进度监听器
     */
    fun addProgressListener(key: String, listener: (ProcessingTask) -> Unit) {
        progressListeners[key] = listener
    }
    
    /**
     * 移除进度监听器
     */
    fun removeProgressListener(key: String) {
        progressListeners.remove(key)
    }
    
    /**
     * 添加状态监听器
     */
    fun addStateListener(key: String, listener: (MediaProcessingState) -> Unit) {
        stateListeners[key] = listener
    }
    
    /**
     * 移除状态监听器
     */
    fun removeStateListener(key: String) {
        stateListeners.remove(key)
    }
    
    /**
     * 清理已完成的任务
     */
    suspend fun cleanupCompletedTasks(): Result<Int> {
        return try {
            logManager.d("MediaProcessor", "开始清理已完成任务")
            taskExecutionEngine.cleanupCompletedTasks()
            val cleanedCount = 0 // TaskExecutionEngine.cleanupCompletedTasks() 返回 Unit
            logManager.i("MediaProcessor", "成功清理已完成任务")
            Result.success(cleanedCount)
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "清理已完成任务失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取处理统计信息
     */
    fun getStatistics(): ProcessingStatistics {
        return stateManager.getStatistics()
    }
    
    /**
     * 选择最佳格式
     */
    private fun selectBestFormat(
        mediaInfo: MediaInfo,
        options: ProcessingOptions
    ): MediaFormat? {
        val formats = mediaInfo.availableFormats
        if (formats.isEmpty()) return null
        
        return when (options.preferredQuality) {
            MediaQuality.BEST -> {
                if (options.audioOnly) {
                    formats.filter { it.hasAudio && !it.hasVideo }
                        .maxByOrNull { it.audioBitrate ?: 0 }
                } else {
                    formats.filter { it.hasVideo }
                        .maxByOrNull { (it.height ?: 0) * (it.width ?: 0) }
                }
            }
            MediaQuality.WORST -> {
                if (options.audioOnly) {
                    formats.filter { it.hasAudio && !it.hasVideo }
                        .minByOrNull { it.audioBitrate ?: Int.MAX_VALUE }
                } else {
                    formats.filter { it.hasVideo }
                        .minByOrNull { (it.height ?: Int.MAX_VALUE) * (it.width ?: Int.MAX_VALUE) }
                }
            }
            else -> {
                // 根据指定质量选择
                val targetHeight = when (options.preferredQuality) {
                    MediaQuality.UHD_4K -> 2160
                    MediaQuality.QHD_2K -> 1440
                    MediaQuality.FHD_1080P -> 1080
                    MediaQuality.HD_720P -> 720
                    MediaQuality.SD_480P -> 480
                    MediaQuality.SD_360P -> 360
                    MediaQuality.SD_240P -> 240
                    else -> 720
                }
                
                if (options.audioOnly) {
                    formats.filter { it.hasAudio && !it.hasVideo }.firstOrNull()
                } else {
                    formats.filter { it.hasVideo && (it.height ?: 0) <= targetHeight }
                        .maxByOrNull { it.height ?: 0 }
                        ?: formats.filter { it.hasVideo }.minByOrNull { it.height ?: Int.MAX_VALUE }
                }
            }
        }
    }
    
    /**
     * 生成任务ID
     */
    private fun generateTaskId(): String {
        return "task_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        try {
            logManager.i("MediaProcessor", "开始清理媒体处理器资源")
            
            // 取消协程作用域
            processorScope.cancel()
            
            // 清理各个组件
            runBlocking {
                // 清理任务执行引擎（如果需要的话）
                // taskExecutionEngine.cleanup()
                // storageManager.cleanup() // 如果没有cleanup方法则注释掉
                cacheManager.clearAll()
                // networkManager.cleanup() // 如果没有cleanup方法则注释掉
            }
            
            // 清理监听器
            progressListeners.clear()
            stateListeners.clear()
            
            logManager.i("MediaProcessor", "媒体处理器资源清理完成")
        } catch (e: Exception) {
            logManager.e("MediaProcessor", "清理媒体处理器资源失败", e)
        }
    }
}

/**
 * MediaProcessorConfig 转换为 ProcessorConfiguration 的扩展函数
 */
private fun MediaProcessorConfig.toProcessorConfiguration(context: Context): ProcessorConfiguration {
    return ProcessorConfiguration.ConfigBuilder(context)
        .outputDirectory(this.storageConfig.outputDirectory ?: "/sdcard/Downloads/Media")
        .maxParallelTasks(this.maxConcurrentTasks)
        .connectionTimeout(this.networkConfig.connectTimeout)
        .readTimeout(this.networkConfig.readTimeout)
        .writeTimeout(this.networkConfig.writeTimeout)
        .enableDebugLogging(this.logConfig.enableFileLogging)
        .build()
}

/**
 * 转换LogConfig类型
 */
private fun convertLogConfig(logConfig: com.nexus.core.media.processor.log.LogConfig): com.nexus.core.media.processor.logging.LogConfig {
    return com.nexus.core.media.processor.logging.LogConfig(
        minLevel = when (logConfig.minLogLevel) {
            com.nexus.core.media.processor.log.LogConfig.LogLevel.VERBOSE -> com.nexus.core.media.processor.logging.LogLevel.VERBOSE
            com.nexus.core.media.processor.log.LogConfig.LogLevel.DEBUG -> com.nexus.core.media.processor.logging.LogLevel.DEBUG
            com.nexus.core.media.processor.log.LogConfig.LogLevel.INFO -> com.nexus.core.media.processor.logging.LogLevel.INFO
            com.nexus.core.media.processor.log.LogConfig.LogLevel.WARN -> com.nexus.core.media.processor.logging.LogLevel.WARN
            com.nexus.core.media.processor.log.LogConfig.LogLevel.ERROR -> com.nexus.core.media.processor.logging.LogLevel.ERROR
            com.nexus.core.media.processor.log.LogConfig.LogLevel.FATAL -> com.nexus.core.media.processor.logging.LogLevel.ERROR
        },
        enableLogcat = logConfig.enableLogcat,
        enableFileLogging = logConfig.enableFileLogging,
        enableMemoryLogging = logConfig.enableMemoryLogging,
        logDirectory = logConfig.logDirectory,
        maxFileSize = logConfig.maxLogFileSizeMB * 1024 * 1024,
        maxBackupFiles = logConfig.maxLogFiles,
        maxMemoryEntries = logConfig.maxMemoryEntries,
        asyncLogging = logConfig.enableAsyncLogging,
        bufferSize = logConfig.bufferSize,
        flushInterval = logConfig.flushIntervalMs
    )
}