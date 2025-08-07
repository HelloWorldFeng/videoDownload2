package com.nexus.core.media.processor.api

import android.content.Context
import com.nexus.core.media.processor.MediaProcessorCore
import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.state.MediaProcessingState

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 媒体处理器API接口
 * 
 * 提供简化的API接口，隐藏内部实现细节，
 * 方便其他项目集成和使用。
 * 
 * 主要功能：
 * - 媒体信息获取
 * - 处理任务管理
 * - 处理状态监控
 * - 配置管理
 * 
 * 使用示例：
 * ```kotlin
 * // 初始化API
 * val api = MediaProcessorApi.initializeApi(context) {
 *     outputDirectory = "/sdcard/Downloads/Media"
 *     maxParallelTasks = 3
 * }
 * 
 * // 获取媒体信息
 * val mediaInfo = api.extractMediaInfo("https://example.com/video")
 * 
 * // 开始处理
 * val taskId = api.initiateProcessing(mediaInfo, mediaInfo.getOptimalFormat()!!)
 * 
 * // 监控处理状态
 * api.processingState.collect { state ->
 *     // 处理状态变化
 * }
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class MediaProcessorApi private constructor(
    private val processorCore: MediaProcessorCore
) {
    
    private val apiExecutionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val processingCallbackRegistry = CopyOnWriteArrayList<ProcessingCallback>()
    private val progressCallbackRegistry = CopyOnWriteArrayList<ProcessingProgressCallback>()
    private val statusCallbackRegistry = CopyOnWriteArrayList<ProcessingStatusCallback>()
    
    init {
        // 监听处理状态变化并分发给回调
        apiExecutionScope.launch {
            processingState.collect { state ->
                handleProcessingStateChange(state)
            }
        }
    }
    
    companion object {
        private const val LOG_TAG = "MediaProcessorApi"
        
        @Volatile
        private var API_INSTANCE: MediaProcessorApi? = null
        
        /**
         * 初始化媒体处理器API
         * 
         * @param context Android上下文
         * @param configurationBuilder 配置构建器
         * @return API实例
         */
        fun initializeApi(
            context: Context,
            configurationBuilder: ProcessorConfiguration.ConfigBuilder.() -> Unit = {}
        ): MediaProcessorApi {
            return API_INSTANCE ?: synchronized(this) {
                API_INSTANCE ?: run {
                    val processorCore = MediaProcessorCore.initializeProcessor(context, configurationBuilder)
                    val api = MediaProcessorApi(processorCore)
                    API_INSTANCE = api
                    // API初始化完成
                    api
                }
            }
        }
        
        /**
         * 获取已初始化的API实例
         * 
         * @return API实例
         * @throws IllegalStateException API未初始化
         */
        fun getApiInstance(): MediaProcessorApi {
            return API_INSTANCE ?: throw IllegalStateException(
                "MediaProcessorApi 尚未初始化，请先调用 initializeApi() 方法"
            )
        }
        
        /**
         * 检查API是否已初始化
         * 
         * @return true表示已初始化，false表示未初始化
         */
        fun isApiInitialized(): Boolean = API_INSTANCE != null
    }
    
    /**
     * 处理状态流
     * 
     * 可以订阅此流来监控所有处理任务的状态变化
     */
    val processingState: StateFlow<MediaProcessingState>
        get() = kotlinx.coroutines.flow.MutableStateFlow(MediaProcessingState()).asStateFlow()
    
    /**
     * 获取媒体信息
     * 
     * @param mediaUrl 媒体URL
     * @param extractorName 使用的抓取器名称，默认为"youtubedl"
     * @return 媒体信息
     * @throws Exception 获取失败时抛出异常
     */
    suspend fun extractMediaInfo(
        mediaUrl: String,
        extractorName: String = "youtubedl"
    ): MediaInfo {
        return try {
            val extractor = processorCore.getMediaExtractor(extractorName)
            val result = extractor.extractMediaInfo(mediaUrl)
            // 将ExtractionResult转换为MediaInfo
            MediaInfo(
                id = result.mediaInfo?.id ?: "",
                title = result.mediaInfo?.title ?: "Unknown",
                description = result.mediaInfo?.description,
                duration = result.mediaInfo?.duration,
                uploader = result.mediaInfo?.uploader,
                uploaderId = result.mediaInfo?.uploaderId,
                uploadDate = result.mediaInfo?.uploadDate,
                viewCount = result.mediaInfo?.viewCount,
                likeCount = result.mediaInfo?.likeCount,
                dislikeCount = result.mediaInfo?.dislikeCount,
                commentCount = result.mediaInfo?.commentCount,
                mediaType = result.mediaInfo?.mediaType ?: MediaInfo.MediaType.UNKNOWN,
                originalUrl = mediaUrl,
                webpageUrl = result.mediaInfo?.webpageUrl,
                availableFormats = result.mediaInfo?.availableFormats ?: emptyList(),
                thumbnails = result.mediaInfo?.thumbnails ?: emptyList(),
                subtitles = result.mediaInfo?.subtitles ?: emptyList(),
                chapters = result.mediaInfo?.chapters ?: emptyList(),
                tags = result.mediaInfo?.tags ?: emptyList(),
                categories = result.mediaInfo?.categories ?: emptyList(),
                ageLimit = result.mediaInfo?.ageLimit,
                isLive = result.mediaInfo?.isLive ?: false,
                averageRating = result.mediaInfo?.averageRating,
                license = result.mediaInfo?.license,
                creator = result.mediaInfo?.creator,
                album = result.mediaInfo?.album,
                artist = result.mediaInfo?.artist,
                releaseYear = result.mediaInfo?.releaseYear,
                trackNumber = result.mediaInfo?.trackNumber,
                extraMetadata = result.mediaInfo?.extraMetadata ?: emptyMap()
            )
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 开始处理任务
     * 
     * @param mediaInfo 媒体信息
     * @param selectedFormat 选择的格式
     * @param processingOptions 处理选项
     * @param taskPriority 任务优先级
     * @return 任务ID
     * @throws Exception 创建任务失败时抛出异常
     */
    suspend fun initiateProcessing(
        mediaInfo: MediaInfo,
        selectedFormat: MediaFormat,
        processingOptions: ProcessingOptions = ProcessingOptions(),
        taskPriority: TaskPriority = TaskPriority.NORMAL
    ): String {
        return try {
            // 创建处理任务的逻辑需要实现
            "task_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 暂停处理任务
     * 
     * @param taskId 任务ID
     */
    suspend fun pauseProcessing(taskId: String) {
        try {
            // 暂停处理任务的逻辑需要实现
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 恢复处理任务
     * 
     * @param taskId 任务ID
     */
    suspend fun resumeProcessing(taskId: String) {
        try {
            // 恢复处理任务的逻辑需要实现
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 取消处理任务
     * 
     * @param taskId 任务ID
     */
    suspend fun cancelProcessing(taskId: String) {
        try {
            // 取消处理任务的逻辑需要实现
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 删除处理任务
     * 
     * @param taskId 任务ID
     * @param deleteFile 是否删除已下载的文件
     */
    suspend fun deleteProcessingTask(taskId: String, deleteFile: Boolean = false) {
        try {
            // 删除处理任务的逻辑需要实现
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 获取所有处理任务
     * 
     * @return 任务列表
     */
    suspend fun getAllProcessingTasks(): List<ProcessingTask> {
        return emptyList() // 返回空列表，实际逻辑需要实现
    }
    
    /**
     * 获取指定状态的处理任务
     * 
     * @param status 任务状态
     * @return 任务列表
     */
    suspend fun getProcessingTasksByStatus(status: ProcessingStatus): List<ProcessingTask> {
        return emptyList() // 返回空列表，实际逻辑需要实现
    }
    
    /**
     * 注册处理回调
     * 
     * @param callback 回调接口
     */
    fun registerProcessingCallback(callback: ProcessingCallback) {
        if (!processingCallbackRegistry.contains(callback)) {
            processingCallbackRegistry.add(callback)
        }
    }
    
    /**
     * 注销处理回调
     * 
     * @param callback 回调接口
     */
    fun unregisterProcessingCallback(callback: ProcessingCallback) {
        if (processingCallbackRegistry.remove(callback)) {
            // 回调已注销
        }
    }
    
    /**
     * 注册进度回调
     * 
     * @param callback 进度回调接口
     */
    fun registerProgressCallback(callback: ProcessingProgressCallback) {
        if (!progressCallbackRegistry.contains(callback)) {
            progressCallbackRegistry.add(callback)
        }
    }
    
    /**
     * 注销进度回调
     * 
     * @param callback 进度回调接口
     */
    fun unregisterProgressCallback(callback: ProcessingProgressCallback) {
        if (progressCallbackRegistry.remove(callback)) {
            // 回调已注销
        }
    }
    
    /**
     * 注册状态回调
     * 
     * @param callback 状态回调接口
     */
    fun registerStatusCallback(callback: ProcessingStatusCallback) {
        if (!statusCallbackRegistry.contains(callback)) {
            statusCallbackRegistry.add(callback)
        }
    }
    
    /**
     * 注销状态回调
     * 
     * @param callback 状态回调接口
     */
    fun unregisterStatusCallback(callback: ProcessingStatusCallback) {
        if (statusCallbackRegistry.remove(callback)) {
            // 回调已注销
        }
    }
    
    /**
     * 处理状态变化
     */
    private fun handleProcessingStateChange(state: MediaProcessingState) {
        // 分发给各种回调
        state.activeTasks.forEach { task ->
            when {
                task.hasProgressUpdate() -> {
                    progressCallbackRegistry.forEach { callback ->
                        callback.onProgress(task, task.progressPercentage, task.processingSpeed)
                    }
                }
                task.hasStatusChange() -> {
                    statusCallbackRegistry.forEach { callback ->
                        callback.onStatusChanged(task, task.status)
                    }
                }
            }
        }
        
        // 分发给通用回调
        processingCallbackRegistry.forEach { callback ->
            state.activeTasks.forEach { task ->
                when (task.status) {
                    ProcessingStatus.PROCESSING -> callback.onProcessingStarted(task)
                    ProcessingStatus.COMPLETED -> callback.onProcessingCompleted(task)
                    ProcessingStatus.FAILED -> callback.onProcessingFailed(task, task.errorMessage ?: "未知错误")
                    ProcessingStatus.CANCELLED -> callback.onProcessingCancelled(task)
                    ProcessingStatus.PAUSED -> callback.onProcessingPaused(task)
                    else -> {}
                }
            }
        }
    }
    
    /**
     * 清理API资源
     */
    suspend fun cleanupApi() {
        processingCallbackRegistry.clear()
        progressCallbackRegistry.clear()
        statusCallbackRegistry.clear()
        apiExecutionScope.cancel()
        processorCore.cleanupProcessor()
    }
}