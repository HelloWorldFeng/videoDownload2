package com.nexus.core.media.processor.execution

import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.network.MediaDownloader
import com.nexus.core.media.processor.storage.MediaStorageManager
import com.nexus.core.media.processor.utils.ProcessorLogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*
import kotlin.math.max

/**
 * 任务执行器
 * 
 * 负责执行单个媒体处理任务，包括下载、
 * 转换、后处理等操作。
 * 
 * 主要功能：
 * - 媒体文件下载
 * - 进度监控
 * - 断点续传
 * - 错误处理
 * - 任务控制（暂停/恢复/取消）
 * 
 * 使用示例：
 * ```kotlin
 * val executor = TaskExecutor(
 *     task = processingTask,
 *     configuration = config,
 *     onProgressUpdate = { task -> ... },
 *     onTaskCompleted = { task -> ... },
 *     onTaskFailed = { task, error -> ... }
 * )
 * executor.execute()
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class TaskExecutor(
    private val task: ProcessingTask,
    private val configuration: ProcessorConfiguration,
    private val onProgressUpdate: suspend (ProcessingTask) -> Unit,
    private val onTaskCompleted: suspend (ProcessingTask) -> Unit,
    private val onTaskFailed: suspend (ProcessingTask, String) -> Unit
) {
    
    companion object {
        private const val LOG_TAG = "TaskExecutor"
        private const val PROGRESS_UPDATE_INTERVAL = 1000L // 1秒
        private const val SPEED_CALCULATION_WINDOW = 10 // 10个采样点
    }
    
    // 协程作用域
    private val executorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 执行状态
    private val _executionState = MutableStateFlow(ExecutionState.IDLE)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()
    
    // 下载器和存储管理器
    private var mediaDownloader: MediaDownloader? = null
    private var storageManager: MediaStorageManager? = null
    
    // 进度跟踪
    private var lastProgressUpdate = 0L
    private val speedSamples = mutableListOf<SpeedSample>()
    private var currentTask = task
    
    // 控制标志
    @Volatile
    private var isPaused = false
    @Volatile
    private var isCancelled = false
    
    /**
     * 执行状态枚举
     */
    enum class ExecutionState {
        IDLE,           // 空闲
        PREPARING,      // 准备中
        DOWNLOADING,    // 下载中
        POST_PROCESSING,// 后处理中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        CANCELLED       // 已取消
    }
    
    /**
     * 速度采样数据
     */
    private data class SpeedSample(
        val timestamp: Long,
        val downloadedBytes: Long
    )
    
    /**
     * 执行任务
     */
    suspend fun execute() {
        executorScope.launch {
            try {
                ProcessorLogUtils.d(LOG_TAG, "开始执行任务: ${task.taskId}")
                
                _executionState.value = ExecutionState.PREPARING
                
                // 准备阶段
                prepareExecution()
                
                if (isCancelled) return@launch
                
                // 下载阶段
                _executionState.value = ExecutionState.DOWNLOADING
                downloadMedia()
                
                if (isCancelled) return@launch
                
                // 后处理阶段
                if (currentTask.processingOptions.postProcessingOptions.enabled) {
                    _executionState.value = ExecutionState.POST_PROCESSING
                    performPostProcessing()
                }
                
                if (isCancelled) return@launch
                
                // 完成
                _executionState.value = ExecutionState.COMPLETED
                completeTask()
                
            } catch (e: CancellationException) {
                ProcessorLogUtils.i(LOG_TAG, "任务被取消: ${task.taskId}")
                _executionState.value = ExecutionState.CANCELLED
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "任务执行失败: ${task.taskId}", e)
                _executionState.value = ExecutionState.FAILED
                onTaskFailed(currentTask, e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 暂停任务
     */
    suspend fun pause() {
        withContext(Dispatchers.IO) {
            isPaused = true
            mediaDownloader?.pause()
            ProcessorLogUtils.d(LOG_TAG, "任务已暂停: ${task.taskId}")
        }
    }
    
    /**
     * 恢复任务
     */
    suspend fun resume() {
        withContext(Dispatchers.IO) {
            isPaused = false
            mediaDownloader?.resume()
            ProcessorLogUtils.d(LOG_TAG, "任务已恢复: ${task.taskId}")
        }
    }
    
    /**
     * 取消任务
     */
    suspend fun cancel() {
        withContext(Dispatchers.IO) {
            isCancelled = true
            mediaDownloader?.cancel()
            executorScope.cancel()
            
            // 清理临时文件
            cleanupTempFiles()
            
            ProcessorLogUtils.d(LOG_TAG, "任务已取消: ${task.taskId}")
        }
    }
    
    /**
     * 准备执行
     */
    private suspend fun prepareExecution() {
        try {
            // 更新任务状态
            currentTask = currentTask.updateStatus(
                newStatus = ProcessingStatus.PREPARING
            )
            onProgressUpdate(currentTask)
            
            // 初始化存储管理器
            storageManager = MediaStorageManager(configuration)
            
            // 验证输出目录
            val outputFile = File(currentTask.outputFilePath)
            val outputDir = outputFile.parentFile
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // 生成临时文件路径
            val tempFilePath = generateTempFilePath()
            currentTask = currentTask.copy(tempFilePath = tempFilePath)
            
            // 初始化下载器
            mediaDownloader = MediaDownloader(
                configuration = configuration,
                onProgressUpdate = { downloadedBytes, totalBytes, speed ->
                    updateDownloadProgress(downloadedBytes, totalBytes, speed)
                }
            )
            
            ProcessorLogUtils.d(LOG_TAG, "任务准备完成: ${task.taskId}")
            
        } catch (e: Exception) {
            throw Exception("准备执行失败: ${e.message}", e)
        }
    }
    
    /**
     * 下载媒体
     */
    private suspend fun downloadMedia() {
        try {
            // 更新任务状态
            currentTask = currentTask.updateStatus(newStatus = ProcessingStatus.PROCESSING)
            onProgressUpdate(currentTask)
            
            // 检查是否支持断点续传
            val resumePosition = if (currentTask.isResumable && currentTask.resumePosition > 0) {
                currentTask.resumePosition
            } else {
                0L
            }
            
            // 开始下载
            val downloadResult = mediaDownloader?.download(
                url = currentTask.selectedFormat.url ?: throw Exception("下载URL为空"),
                outputPath = currentTask.tempFilePath ?: currentTask.outputFilePath ?: "",
                resumePosition = resumePosition,
                headers = buildDownloadHeaders()
            ) ?: throw Exception("下载器初始化失败")
            
            if (!downloadResult.success) {
                throw Exception(downloadResult.errorMessage ?: "下载失败")
            }
            
            ProcessorLogUtils.d(LOG_TAG, "媒体下载完成: ${task.taskId}")
            
        } catch (e: Exception) {
            throw Exception("下载媒体失败: ${e.message}", e)
        }
    }
    
    /**
     * 执行后处理
     */
    private suspend fun performPostProcessing() {
        try {
            val postProcessing = currentTask.processingOptions.postProcessingOptions
            
            // 更新任务状态
            currentTask = currentTask.updateStatus(newStatus = ProcessingStatus.PROCESSING)
            onProgressUpdate(currentTask)
            
            val inputFile = currentTask.tempFilePath ?: currentTask.outputFilePath
            val outputFile = currentTask.outputFilePath
            
            // 视频重新编码
            if (postProcessing.reencodeVideo) {
                reencodeVideo(inputFile, outputFile, postProcessing.targetVideoCodec, postProcessing.videoBitrate)
            }
            
            // 音频重新编码
            if (postProcessing.reencodeAudio) {
                reencodeAudio(inputFile, outputFile, postProcessing.targetAudioCodec, postProcessing.audioBitrate)
            }
            
            // 添加元数据
            if (postProcessing.addMetadata) {
                addMetadata(inputFile, outputFile)
            }
            
            // 自定义FFmpeg参数
            if (postProcessing.customFFmpegArgs.isNotEmpty()) {
                applyCustomFFmpegArgs(inputFile, outputFile, postProcessing.customFFmpegArgs)
            }
            
            // 移动文件到最终位置
            if (currentTask.tempFilePath != null) {
                moveToFinalLocation()
            }
            
            ProcessorLogUtils.d(LOG_TAG, "后处理完成: ${task.taskId}")
            
        } catch (e: Exception) {
            throw Exception("后处理失败: ${e.message}", e)
        }
    }
    
    /**
     * 完成任务
     */
    private suspend fun completeTask() {
        try {
            // 更新任务状态
            currentTask = currentTask.updateStatus(
                newStatus = ProcessingStatus.COMPLETED,
                newProgress = 100
            )
            
            // 清理临时文件
            cleanupTempFiles()
            
            ProcessorLogUtils.i(LOG_TAG, "任务完成: ${task.taskId} - ${currentTask.mediaInfo.title}")
            
            onTaskCompleted(currentTask)
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "完成任务时出错: ${task.taskId}", e)
            onTaskFailed(currentTask, "完成任务时出错: ${e.message}")
        }
    }
    
    /**
     * 更新下载进度
     */
    private suspend fun updateDownloadProgress(
        downloadedBytes: Long,
        totalBytes: Long,
        currentSpeed: Long
    ) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 限制更新频率
            if (currentTime - lastProgressUpdate < PROGRESS_UPDATE_INTERVAL) {
                return
            }
            
            lastProgressUpdate = currentTime
            
            // 计算进度
            val progress = if (totalBytes > 0) {
                (downloadedBytes.toFloat() / totalBytes.toFloat() * 100f)
            } else {
                0f
            }
            
            // 更新速度采样
            updateSpeedSamples(currentTime, downloadedBytes)
            
            // 计算平均速度
            val averageSpeed = calculateAverageSpeed()
            
            // 计算剩余时间
            val remainingTime = if (averageSpeed > 0 && totalBytes > downloadedBytes) {
                (totalBytes - downloadedBytes) / averageSpeed * 1000
            } else {
                0L
            }
            
            // 更新任务
            currentTask = currentTask.updateStatus(
                newProgress = progress.toInt(),
                newDownloadedBytes = downloadedBytes,
                newSpeed = averageSpeed.toString(),
                newRemainingTime = remainingTime
            )
            
            onProgressUpdate(currentTask)
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "更新进度失败", e)
        }
    }
    
    /**
     * 更新速度采样
     */
    private fun updateSpeedSamples(timestamp: Long, downloadedBytes: Long) {
        speedSamples.add(SpeedSample(timestamp, downloadedBytes))
        
        // 保持采样窗口大小
        while (speedSamples.size > SPEED_CALCULATION_WINDOW) {
            speedSamples.removeAt(0)
        }
    }
    
    /**
     * 计算平均速度
     */
    private fun calculateAverageSpeed(): Long {
        if (speedSamples.size < 2) return 0L
        
        val firstSample = speedSamples.first()
        val lastSample = speedSamples.last()
        
        val timeDiff = lastSample.timestamp - firstSample.timestamp
        val bytesDiff = lastSample.downloadedBytes - firstSample.downloadedBytes
        
        return if (timeDiff > 0) {
            bytesDiff * 1000 / timeDiff
        } else {
            0L
        }
    }
    
    /**
     * 生成临时文件路径
     */
    private fun generateTempFilePath(): String {
        val outputFile = File(currentTask.outputFilePath)
        val tempDir = File(configuration.getTempDirectory())
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val tempFileName = "${currentTask.taskId}_${outputFile.name}.tmp"
        return File(tempDir, tempFileName).absolutePath
    }
    
    /**
     * 构建下载请求头
     */
    private fun buildDownloadHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // 添加用户代理
        headers["User-Agent"] = configuration.userAgent
        
        // 添加自定义请求头
        headers.putAll(configuration.customHeaders)
        
        // 添加格式特定的请求头
        currentTask.selectedFormat.httpHeaders?.let { formatHeaders: Map<String, String> ->
            headers.putAll(formatHeaders)
        }
        
        return headers
    }
    
    /**
     * 视频重新编码
     */
    private suspend fun reencodeVideo(
        inputPath: String,
        outputPath: String,
        targetCodec: String?,
        bitrate: String?
    ) {
        // TODO: 实现视频重新编码逻辑
        ProcessorLogUtils.d(LOG_TAG, "重新编码视频: codec=$targetCodec, bitrate=$bitrate")
    }
    
    /**
     * 音频重新编码
     */
    private suspend fun reencodeAudio(
        inputPath: String,
        outputPath: String,
        targetCodec: String?,
        bitrate: String?
    ) {
        // TODO: 实现音频重新编码逻辑
        ProcessorLogUtils.d(LOG_TAG, "重新编码音频: codec=$targetCodec, bitrate=$bitrate")
    }
    
    /**
     * 添加元数据
     */
    private suspend fun addMetadata(
        inputPath: String,
        outputPath: String
    ) {
        // TODO: 实现添加元数据逻辑
        ProcessorLogUtils.d(LOG_TAG, "添加元数据")
    }
    
    /**
     * 应用自定义FFmpeg参数
     */
    private suspend fun applyCustomFFmpegArgs(
        inputPath: String,
        outputPath: String,
        customArgs: List<String>
    ) {
        // TODO: 实现自定义FFmpeg参数逻辑
        ProcessorLogUtils.d(LOG_TAG, "应用自定义FFmpeg参数: $customArgs")
    }
    
    /**
     * 移动到最终位置
     */
    private suspend fun moveToFinalLocation() {
        try {
            val tempFile = File(currentTask.tempFilePath!!)
            val outputFile = File(currentTask.outputFilePath)
            
            if (tempFile.exists()) {
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                
                val success = tempFile.renameTo(outputFile)
                if (!success) {
                    // 如果重命名失败，尝试复制
                    tempFile.copyTo(outputFile, overwrite = true)
                    tempFile.delete()
                }
                
                ProcessorLogUtils.d(LOG_TAG, "文件已移动到最终位置: ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            throw Exception("移动文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 清理临时文件
     */
    private fun cleanupTempFiles() {
        try {
            currentTask.tempFilePath?.let { tempPath ->
                val tempFile = File(tempPath)
                if (tempFile.exists()) {
                    tempFile.delete()
                    ProcessorLogUtils.d(LOG_TAG, "已清理临时文件: $tempPath")
                }
            }
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "清理临时文件失败", e)
        }
    }
    
    /**
     * 清理资源
     */
    suspend fun cleanup() {
        try {
            cancel()
            mediaDownloader?.cleanup()
            storageManager?.cleanup()
            executorScope.cancel()
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "清理执行器资源失败", e)
        }
    }
}