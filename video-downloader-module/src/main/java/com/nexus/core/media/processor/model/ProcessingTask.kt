package com.nexus.core.media.processor.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel
import java.util.Date
import java.util.UUID

/**
 * 处理任务数据类
 * 
 * 表示一个媒体处理任务的完整信息，
 * 包括任务状态、进度、配置等。
 * 
 * 主要信息：
 * - 任务基本信息（ID、状态、优先级）
 * - 媒体信息和格式
 * - 处理进度和统计
 * - 错误信息
 * - 文件路径
 * 
 * 使用示例：
 * ```kotlin
 * val task = ProcessingTask.create(
 *     mediaInfo = mediaInfo,
 *     selectedFormat = format,
 *     outputPath = "/sdcard/Downloads/video.mp4"
 * )
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
@Parcelize
data class ProcessingTask(
    /** 任务唯一标识符 */
    val taskId: String,
    
    /** 媒体信息 */
    val mediaInfo: MediaInfo,
    
    /** 选择的格式 */
    val selectedFormat: MediaFormat,
    
    /** 处理选项 */
    val processingOptions: ProcessingOptions,
    
    /** 任务优先级 */
    val priority: TaskPriority,
    
    /** 当前状态 */
    val status: ProcessingStatus,
    
    /** 进度百分比 (0-100) */
    val progressPercentage: Int,
    
    /** 已下载字节数 */
    val downloadedBytes: Long,
    
    /** 总字节数 */
    val totalBytes: Long,
    
    /** 处理速度描述 */
    val processingSpeed: String,
    
    /** 剩余时间(秒) */
    val remainingTimeSeconds: Long?,
    
    /** 输出文件路径 */
    val outputFilePath: String,
    
    /** 临时文件路径 */
    val tempFilePath: String?,
    
    /** 创建时间 */
    val createdAt: Date,
    
    /** 开始时间 */
    val startedAt: Date?,
    
    /** 完成时间 */
    val completedAt: Date?,
    
    /** 错误信息 */
    val errorMessage: String?,
    
    /** 错误代码 */
    val errorCode: String?,
    
    /** 重试次数 */
    val retryCount: Int,
    
    /** 最大重试次数 */
    val maxRetryCount: Int,
    
    /** 是否可恢复 */
    val isResumable: Boolean,
    
    /** 恢复位置 */
    val resumePosition: Long,
    
    /** 任务元数据 */
    @IgnoredOnParcel
    val metadata: Map<String, Any> = emptyMap(),
    
    /** 内部状态标记 */
    private val _stateFlags: Int = 0
) : Parcelable {
    
    /**
     * 检查是否有进度更新
     */
    fun hasProgressUpdate(): Boolean {
        return (_stateFlags and STATE_FLAG_PROGRESS_UPDATED) != 0
    }
    
    /**
     * 检查是否有状态变化
     */
    fun hasStatusChange(): Boolean {
        return (_stateFlags and STATE_FLAG_STATUS_CHANGED) != 0
    }
    
    /**
     * 获取处理耗时
     * 
     * @return 处理耗时(毫秒)
     */
    fun getProcessingDuration(): Long? {
        return if (startedAt != null) {
            val endTime = completedAt ?: Date()
            endTime.time - startedAt.time
        } else {
            null
        }
    }
    
    /**
     * 获取格式化的处理耗时
     * 
     * @return 格式化的耗时字符串
     */
    fun getFormattedDuration(): String? {
        return getProcessingDuration()?.let { duration ->
            val seconds = duration / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            
            when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
                minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
                else -> "${seconds}s"
            }
        }
    }
    
    /**
     * 获取格式化的剩余时间
     * 
     * @return 格式化的剩余时间字符串
     */
    fun getFormattedRemainingTime(): String? {
        return remainingTimeSeconds?.let { seconds ->
            val minutes = seconds / 60
            val hours = minutes / 60
            
            when {
                hours > 0 -> String.format("%d小时%d分钟", hours, minutes % 60)
                minutes > 0 -> String.format("%d分钟", minutes)
                else -> "${seconds}秒"
            }
        }
    }
    
    /**
     * 获取下载进度描述
     * 
     * @return 进度描述字符串
     */
    fun getProgressDescription(): String {
        return when (status) {
            ProcessingStatus.PENDING -> "等待中..."
            ProcessingStatus.PREPARING -> "准备中..."
            ProcessingStatus.PROCESSING -> {
                if (totalBytes > 0) {
                    val downloadedMB = downloadedBytes / (1024.0 * 1024.0)
                    val totalMB = totalBytes / (1024.0 * 1024.0)
                    String.format("%.1f/%.1f MB (%d%%)", downloadedMB, totalMB, progressPercentage)
                } else {
                    "$progressPercentage%"
                }
            }
            ProcessingStatus.PAUSED -> "已暂停 ($progressPercentage%)"
            ProcessingStatus.COMPLETED -> "已完成"
            ProcessingStatus.FAILED -> "失败: ${errorMessage ?: "未知错误"}"
            ProcessingStatus.CANCELLED -> "已取消"
        }
    }
    
    /**
     * 检查任务是否正在运行
     * 
     * @return true表示正在运行
     */
    fun isRunning(): Boolean {
        return status in listOf(
            ProcessingStatus.PREPARING,
            ProcessingStatus.PROCESSING
        )
    }
    
    /**
     * 检查任务是否已完成（成功或失败）
     * 
     * @return true表示已完成
     */
    fun isFinished(): Boolean {
        return status in listOf(
            ProcessingStatus.COMPLETED,
            ProcessingStatus.FAILED,
            ProcessingStatus.CANCELLED
        )
    }
    
    /**
     * 检查任务是否可以暂停
     * 
     * @return true表示可以暂停
     */
    fun canPause(): Boolean {
        return status == ProcessingStatus.PROCESSING
    }
    
    /**
     * 检查任务是否可以恢复
     * 
     * @return true表示可以恢复
     */
    fun canResume(): Boolean {
        return status == ProcessingStatus.PAUSED && isResumable
    }
    
    /**
     * 检查任务是否可以取消
     * 
     * @return true表示可以取消
     */
    fun canCancel(): Boolean {
        return status in listOf(
            ProcessingStatus.PENDING,
            ProcessingStatus.PREPARING,
            ProcessingStatus.PROCESSING,
            ProcessingStatus.PAUSED
        )
    }
    
    /**
     * 检查任务是否可以重试
     * 
     * @return true表示可以重试
     */
    fun canRetry(): Boolean {
        return status == ProcessingStatus.FAILED && retryCount < maxRetryCount
    }
    
    /**
     * 获取平均下载速度
     * 
     * @return 平均速度(字节/秒)
     */
    fun getAverageSpeed(): Long? {
        return getProcessingDuration()?.let { duration ->
            if (duration > 0) {
                downloadedBytes * 1000 / duration
            } else {
                null
            }
        }
    }
    
    /**
     * 获取格式化的平均速度
     * 
     * @return 格式化的速度字符串
     */
    fun getFormattedAverageSpeed(): String? {
        return getAverageSpeed()?.let { speed ->
            when {
                speed >= 1024 * 1024 -> String.format("%.1f MB/s", speed / (1024.0 * 1024.0))
                speed >= 1024 -> String.format("%.1f KB/s", speed / 1024.0)
                else -> "${speed} B/s"
            }
        }
    }
    
    /**
     * 创建任务副本并更新状态
     * 
     * @param newStatus 新状态
     * @param newProgress 新进度
     * @param newDownloadedBytes 新的已下载字节数
     * @param newSpeed 新的速度描述
     * @param newRemainingTime 新的剩余时间
     * @param newErrorMessage 新的错误信息
     * @param newErrorCode 新的错误代码
     * @return 更新后的任务副本
     */
    fun updateStatus(
        newStatus: ProcessingStatus? = null,
        newProgress: Int? = null,
        newDownloadedBytes: Long? = null,
        newSpeed: String? = null,
        newRemainingTime: Long? = null,
        newErrorMessage: String? = null,
        newErrorCode: String? = null
    ): ProcessingTask {
        var flags = _stateFlags
        
        if (newStatus != null && newStatus != status) {
            flags = flags or STATE_FLAG_STATUS_CHANGED
        }
        
        if (newProgress != null && newProgress != progressPercentage) {
            flags = flags or STATE_FLAG_PROGRESS_UPDATED
        }
        
        return copy(
            status = newStatus ?: status,
            progressPercentage = newProgress ?: progressPercentage,
            downloadedBytes = newDownloadedBytes ?: downloadedBytes,
            processingSpeed = newSpeed ?: processingSpeed,
            remainingTimeSeconds = newRemainingTime ?: remainingTimeSeconds,
            errorMessage = newErrorMessage ?: errorMessage,
            errorCode = newErrorCode ?: errorCode,
            startedAt = if (newStatus == ProcessingStatus.PROCESSING && startedAt == null) Date() else startedAt,
            completedAt = if (newStatus?.isFinished() == true && completedAt == null) Date() else completedAt,
            _stateFlags = flags
        )
    }
    
    /**
     * 转换为简化的任务信息
     * 
     * @return 简化的任务信息
     */
    fun toSimpleTask(): SimpleProcessingTask {
        return SimpleProcessingTask(
            taskId = taskId,
            title = mediaInfo.title,
            status = status,
            progressPercentage = progressPercentage,
            outputFilePath = outputFilePath,
            createdAt = createdAt
        )
    }
    
    companion object {
        private const val STATE_FLAG_PROGRESS_UPDATED = 1
        private const val STATE_FLAG_STATUS_CHANGED = 2
        
        /**
         * 创建新的处理任务
         * 
         * @param mediaInfo 媒体信息
         * @param selectedFormat 选择的格式
         * @param outputPath 输出路径
         * @param processingOptions 处理选项
         * @param priority 任务优先级
         * @return 新的处理任务
         */
        fun create(
            mediaInfo: MediaInfo,
            selectedFormat: MediaFormat,
            outputPath: String,
            processingOptions: ProcessingOptions = ProcessingOptions(),
            priority: TaskPriority = TaskPriority.NORMAL
        ): ProcessingTask {
            return ProcessingTask(
                taskId = UUID.randomUUID().toString(),
                mediaInfo = mediaInfo,
                selectedFormat = selectedFormat,
                processingOptions = processingOptions,
                priority = priority,
                status = ProcessingStatus.PENDING,
                progressPercentage = 0,
                downloadedBytes = 0L,
                totalBytes = selectedFormat.fileSize ?: 0L,
                processingSpeed = "",
                remainingTimeSeconds = null,
                outputFilePath = outputPath,
                tempFilePath = null,
                createdAt = Date(),
                startedAt = null,
                completedAt = null,
                errorMessage = null,
                errorCode = null,
                retryCount = 0,
                maxRetryCount = 3,
                isResumable = true,
                resumePosition = 0L,
                metadata = emptyMap()
            )
        }
    }
}

/**
 * 简化的处理任务
 * 
 * 用于列表显示等场景的轻量级任务信息。
 */
@Parcelize
data class SimpleProcessingTask(
    val taskId: String,
    val title: String,
    val status: ProcessingStatus,
    val progressPercentage: Int,
    val outputFilePath: String,
    val createdAt: Date
) : Parcelable

/**
 * 任务优先级枚举
 */
enum class TaskPriority(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    URGENT(4)
}

/**
 * 处理状态枚举
 */
enum class ProcessingStatus {
    PENDING,        // 等待中
    PREPARING,      // 准备中
    PROCESSING,     // 处理中
    PAUSED,         // 已暂停
    COMPLETED,      // 已完成
    FAILED,         // 失败
    CANCELLED;      // 已取消
    
    /**
     * 检查状态是否为已完成（成功或失败）
     */
    fun isFinished(): Boolean {
        return this in listOf(COMPLETED, FAILED, CANCELLED)
    }
    
    /**
     * 检查状态是否为活跃状态
     */
    fun isActive(): Boolean {
        return this in listOf(PREPARING, PROCESSING)
    }
}