package com.nexus.core.media.processor.state

import java.util.Date

/**
 * 处理统计信息
 * 
 * 记录媒体处理器的统计数据，
 * 包括任务数量、处理时间、速度等。
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
data class ProcessingStatistics(
    /** 总任务数 */
    val totalTasks: Int = 0,
    
    /** 已完成任务数 */
    val completedTasks: Int = 0,
    
    /** 失败任务数 */
    val failedTasks: Int = 0,
    
    /** 取消任务数 */
    val cancelledTasks: Int = 0,
    
    /** 总下载字节数 */
    val totalDownloadedBytes: Long = 0L,
    
    /** 平均处理速度 (bytes/s) */
    val averageSpeed: Double = 0.0,
    
    /** 总处理时间 (ms) */
    val totalProcessingTime: Long = 0L,
    
    /** 统计开始时间 */
    val startTime: Date = Date(),
    
    /** 最后更新时间 */
    val lastUpdated: Date = Date()
) {
    
    /**
     * 获取成功率
     */
    fun getSuccessRate(): Double {
        return if (totalTasks > 0) {
            completedTasks.toDouble() / totalTasks.toDouble() * 100.0
        } else {
            0.0
        }
    }
    
    /**
     * 获取失败率
     */
    fun getFailureRate(): Double {
        return if (totalTasks > 0) {
            failedTasks.toDouble() / totalTasks.toDouble() * 100.0
        } else {
            0.0
        }
    }
    
    /**
     * 获取活跃任务数
     */
    fun getActiveTasks(): Int {
        return totalTasks - completedTasks - failedTasks - cancelledTasks
    }
    
    /**
     * 获取格式化的处理速度
     */
    fun getFormattedSpeed(): String {
        return when {
            averageSpeed >= 1024 * 1024 -> String.format("%.2f MB/s", averageSpeed / (1024 * 1024))
            averageSpeed >= 1024 -> String.format("%.2f KB/s", averageSpeed / 1024)
            else -> String.format("%.2f B/s", averageSpeed)
        }
    }
    
    /**
     * 获取格式化的总下载量
     */
    fun getFormattedTotalDownloaded(): String {
        return when {
            totalDownloadedBytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", totalDownloadedBytes.toDouble() / (1024 * 1024 * 1024))
            totalDownloadedBytes >= 1024 * 1024 -> String.format("%.2f MB", totalDownloadedBytes.toDouble() / (1024 * 1024))
            totalDownloadedBytes >= 1024 -> String.format("%.2f KB", totalDownloadedBytes.toDouble() / 1024)
            else -> "$totalDownloadedBytes B"
        }
    }
    
    /**
     * 获取格式化的处理时间
     */
    fun getFormattedProcessingTime(): String {
        val seconds = totalProcessingTime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
            else -> "${seconds}s"
        }
    }
    
    /**
     * 重置统计信息
     */
    fun reset(): ProcessingStatistics {
        return ProcessingStatistics(
            startTime = Date(),
            lastUpdated = Date()
        )
    }
    
    /**
     * 更新最后更新时间
     */
    fun updateTimestamp(): ProcessingStatistics {
        return copy(lastUpdated = Date())
    }
}