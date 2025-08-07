package com.nexus.core.media.processor.state

import com.nexus.core.media.processor.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 媒体处理状态
 * 
 * 管理整个媒体处理器的状态信息，
 * 包括任务状态、统计信息、系统状态等。
 * 
 * 主要功能：
 * - 任务状态跟踪
 * - 统计信息管理
 * - 状态变化通知
 * - 历史记录维护
 * 
 * 使用示例：
 * ```kotlin
 * val state = MediaProcessingState(
 *     activeTasks = taskList,
 *     queuedTasks = queueList,
 *     statistics = stats
 * )
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
data class MediaProcessingState(
    val activeTasks: List<ProcessingTask> = emptyList(),
    val queuedTasks: List<ProcessingTask> = emptyList(),
    val statistics: ProcessingStatistics = ProcessingStatistics(),
    val systemStatus: SystemStatus = SystemStatus.IDLE,
    val lastUpdated: Date = Date()
) {
    
    /**
     * 获取正在运行的任务
     */
    fun getRunningTasks(): List<ProcessingTask> {
        return activeTasks.filter { it.isRunning() }
    }
    
    /**
     * 获取已暂停的任务
     */
    fun getPausedTasks(): List<ProcessingTask> {
        return activeTasks.filter { it.status == ProcessingStatus.PAUSED }
    }
    
    /**
     * 获取已完成的任务
     */
    fun getCompletedTasks(): List<ProcessingTask> {
        return activeTasks.filter { it.status == ProcessingStatus.COMPLETED }
    }
    
    /**
     * 获取失败的任务
     */
    fun getFailedTasks(): List<ProcessingTask> {
        return activeTasks.filter { it.status == ProcessingStatus.FAILED }
    }
    
    /**
     * 获取总进度
     */
    fun getOverallProgress(): Float {
        if (activeTasks.isEmpty()) return 0f
        
        val totalProgress = activeTasks.sumOf { it.progressPercentage.toDouble() }
        return (totalProgress / activeTasks.size).toFloat()
    }
    
    /**
     * 获取总下载速度
     */
    fun getTotalDownloadSpeed(): Long {
        return getRunningTasks().sumOf { it.processingSpeed.toLongOrNull() ?: 0L }
    }
    
    /**
     * 检查是否有活动任务
     */
    fun hasActiveTasks(): Boolean {
        return activeTasks.any { it.isRunning() }
    }
    
    /**
     * 检查是否有错误
     */
    fun hasErrors(): Boolean {
        return activeTasks.any { it.status == ProcessingStatus.FAILED }
    }
}

/**
 * 系统状态枚举
 */
enum class SystemStatus {
    IDLE,           // 空闲
    PROCESSING,     // 处理中
    PAUSED,         // 已暂停
    ERROR,          // 错误状态
    MAINTENANCE     // 维护状态
}

/**
 * 媒体处理状态管理器
 * 
 * 负责管理和维护媒体处理器的全局状态，
 * 提供状态查询、更新、监听等功能。
 * 
 * 主要功能：
 * - 状态集中管理
 * - 状态变化监听
 * - 历史状态记录
 * - 状态持久化
 * 
 * 使用示例：
 * ```kotlin
 * val stateManager = MediaProcessingStateManager()
 * stateManager.updateTaskState(task)
 * stateManager.currentState.collect { state -> ... }
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class MediaProcessingStateManager {
    
    companion object {
        private const val MAX_HISTORY_SIZE = 100
        private const val STATE_UPDATE_DEBOUNCE_MS = 100L
    }
    
    // 当前状态
    private val _currentState = MutableStateFlow(
        MediaProcessingState()
    )
    val currentState: StateFlow<MediaProcessingState> = _currentState.asStateFlow()
    
    // 任务映射
    private val taskMap = ConcurrentHashMap<String, ProcessingTask>()
    
    // 状态历史
    private val stateHistory = mutableListOf<StateHistoryEntry>()
    
    // 统计信息
    private var cumulativeStatistics = ProcessingStatistics()
    
    // 状态更新防抖
    private var lastUpdateTime = 0L
    
    /**
     * 状态历史条目
     */
    data class StateHistoryEntry(
        val state: MediaProcessingState,
        val timestamp: Long,
        val changeType: StateChangeType,
        val description: String
    )
    
    /**
     * 状态变化类型
     */
    enum class StateChangeType {
        TASK_ADDED,
        TASK_UPDATED,
        TASK_REMOVED,
        TASK_COMPLETED,
        TASK_FAILED,
        SYSTEM_STATUS_CHANGED,
        STATISTICS_UPDATED
    }
    
    /**
     * 添加任务
     * 
     * @param task 处理任务
     */
    fun addTask(task: ProcessingTask) {
        synchronized(this) {
            taskMap[task.taskId] = task
            updateState(
                changeType = StateChangeType.TASK_ADDED,
                description = "添加任务: ${task.mediaInfo.title}"
            )
        }
    }
    
    /**
     * 更新任务状态
     * 
     * @param task 更新的任务
     */
    fun updateTask(task: ProcessingTask) {
        synchronized(this) {
            val oldTask = taskMap[task.taskId]
            taskMap[task.taskId] = task
            
            val changeType = when {
                oldTask?.status != task.status && task.status == ProcessingStatus.COMPLETED -> {
                    StateChangeType.TASK_COMPLETED
                }
                oldTask?.status != task.status && task.status == ProcessingStatus.FAILED -> {
                    StateChangeType.TASK_FAILED
                }
                else -> StateChangeType.TASK_UPDATED
            }
            
            updateState(
                changeType = changeType,
                description = "更新任务: ${task.mediaInfo.title} - ${task.status}"
            )
        }
    }
    
    /**
     * 移除任务
     * 
     * @param taskId 任务ID
     */
    fun removeTask(taskId: String) {
        synchronized(this) {
            val removedTask = taskMap.remove(taskId)
            if (removedTask != null) {
                updateState(
                    changeType = StateChangeType.TASK_REMOVED,
                    description = "移除任务: ${removedTask.mediaInfo.title}"
                )
            }
        }
    }
    
    /**
     * 批量更新任务
     * 
     * @param tasks 任务列表
     */
    fun updateTasks(tasks: List<ProcessingTask>) {
        synchronized(this) {
            tasks.forEach { task ->
                taskMap[task.taskId] = task
            }
            
            updateState(
                changeType = StateChangeType.TASK_UPDATED,
                description = "批量更新 ${tasks.size} 个任务"
            )
        }
    }
    
    /**
     * 更新系统状态
     * 
     * @param systemStatus 系统状态
     */
    fun updateSystemStatus(systemStatus: SystemStatus) {
        synchronized(this) {
            val currentState = _currentState.value
            if (currentState.systemStatus != systemStatus) {
                updateState(
                    changeType = StateChangeType.SYSTEM_STATUS_CHANGED,
                    description = "系统状态变更: ${currentState.systemStatus} -> $systemStatus",
                    newSystemStatus = systemStatus
                )
            }
        }
    }
    
    /**
     * 更新统计信息
     * 
     * @param statistics 统计信息
     */
    fun updateStatistics(statistics: ProcessingStatistics) {
        synchronized(this) {
            cumulativeStatistics = statistics
            updateState(
                changeType = StateChangeType.STATISTICS_UPDATED,
                description = "统计信息更新"
            )
        }
    }
    
    /**
     * 获取任务
     * 
     * @param taskId 任务ID
     * @return 任务或null
     */
    fun getTask(taskId: String): ProcessingTask? {
        return taskMap[taskId]
    }
    
    /**
     * 获取所有任务
     * 
     * @return 任务列表
     */
    fun getAllTasks(): List<ProcessingTask> {
        return taskMap.values.toList()
    }
    
    /**
     * 获取指定状态的任务
     * 
     * @param status 任务状态
     * @return 任务列表
     */
    fun getTasksByStatus(status: ProcessingStatus): List<ProcessingTask> {
        return taskMap.values.filter { it.status == status }
    }
    
    /**
     * 获取状态历史
     * 
     * @param limit 限制数量
     * @return 历史记录列表
     */
    fun getStateHistory(limit: Int = 50): List<StateHistoryEntry> {
        return synchronized(this) {
            stateHistory.takeLast(limit)
        }
    }
    
    /**
     * 获取统计信息
     * 
     * @return 统计信息
     */
    fun getStatistics(): ProcessingStatistics {
        return cumulativeStatistics
    }
    
    /**
     * 清理已完成的任务
     * 
     * @param olderThanMs 清理多久之前的任务（毫秒）
     */
    fun cleanupCompletedTasks(olderThanMs: Long = 24 * 60 * 60 * 1000L) {
        synchronized(this) {
            val cutoffTime = System.currentTimeMillis() - olderThanMs
            val tasksToRemove = mutableListOf<String>()
            
            taskMap.forEach { (taskId, task) ->
                if (task.isFinished() && 
                    (task.completedAt?.time ?: 0) < cutoffTime) {
                    tasksToRemove.add(taskId)
                }
            }
            
            tasksToRemove.forEach { taskId ->
                taskMap.remove(taskId)
            }
            
            if (tasksToRemove.isNotEmpty()) {
                updateState(
                    changeType = StateChangeType.TASK_REMOVED,
                    description = "清理 ${tasksToRemove.size} 个已完成任务"
                )
            }
        }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        synchronized(this) {
            taskMap.clear()
            cumulativeStatistics = ProcessingStatistics()
            
            updateState(
                changeType = StateChangeType.SYSTEM_STATUS_CHANGED,
                description = "状态管理器重置",
                newSystemStatus = SystemStatus.IDLE
            )
        }
    }
    
    /**
     * 更新状态
     */
    private fun updateState(
        changeType: StateChangeType,
        description: String,
        newSystemStatus: SystemStatus? = null
    ) {
        val currentTime = System.currentTimeMillis()
        
        // 防抖处理
        if (currentTime - lastUpdateTime < STATE_UPDATE_DEBOUNCE_MS && 
            changeType == StateChangeType.TASK_UPDATED) {
            return
        }
        
        lastUpdateTime = currentTime
        
        // 构建新状态
        val activeTasks = taskMap.values.toList()
        val queuedTasks = activeTasks.filter { it.status == ProcessingStatus.PENDING }
        
        // 计算实时统计
        val realtimeStatistics = calculateRealtimeStatistics(activeTasks)
        
        // 合并统计信息
        val mergedStatistics = mergeStatistics(cumulativeStatistics, realtimeStatistics)
        
        val newState = MediaProcessingState(
            activeTasks = activeTasks,
            queuedTasks = queuedTasks,
            statistics = mergedStatistics,
            systemStatus = newSystemStatus ?: determineSystemStatus(activeTasks),
            lastUpdated = Date(currentTime)
        )
        
        // 更新状态
        _currentState.value = newState
        
        // 记录历史
        addToHistory(newState, changeType, description)
    }
    
    /**
     * 计算实时统计
     */
    private fun calculateRealtimeStatistics(tasks: List<ProcessingTask>): ProcessingStatistics {
        val runningTasks = tasks.filter { it.isRunning() }
        val completedTasks = tasks.filter { it.status == ProcessingStatus.COMPLETED }
        val failedTasks = tasks.filter { it.status == ProcessingStatus.FAILED }
        val cancelledTasks = tasks.filter { it.status == ProcessingStatus.CANCELLED }
        
        val totalDownloadedBytes = tasks.sumOf { it.downloadedBytes }
        val totalSpeed = runningTasks.sumOf { it.processingSpeed.toLongOrNull() ?: 0L }
        
        val totalProcessingTime = completedTasks.sumOf { task ->
            task.getProcessingDuration() ?: 0L
        }
        
        return ProcessingStatistics(
            totalTasks = tasks.size,
            completedTasks = completedTasks.size,
            failedTasks = failedTasks.size,
            cancelledTasks = cancelledTasks.size,
            totalDownloadedBytes = totalDownloadedBytes,
            averageSpeed = if (runningTasks.isNotEmpty()) (totalSpeed / runningTasks.size).toDouble() else 0.0,
            totalProcessingTime = totalProcessingTime
        )
    }
    
    /**
     * 合并统计信息
     */
    private fun mergeStatistics(
        cumulative: ProcessingStatistics,
        realtime: ProcessingStatistics
    ): ProcessingStatistics {
        return ProcessingStatistics(
            totalTasks = cumulative.totalTasks + realtime.totalTasks,
            completedTasks = cumulative.completedTasks + realtime.completedTasks,
            failedTasks = cumulative.failedTasks + realtime.failedTasks,
            cancelledTasks = cumulative.cancelledTasks + realtime.cancelledTasks,
            totalDownloadedBytes = cumulative.totalDownloadedBytes + realtime.totalDownloadedBytes,
            averageSpeed = realtime.averageSpeed, // 使用实时速度
            totalProcessingTime = cumulative.totalProcessingTime + realtime.totalProcessingTime
        )
    }
    
    /**
     * 确定系统状态
     */
    private fun determineSystemStatus(tasks: List<ProcessingTask>): SystemStatus {
        return when {
            tasks.any { it.status == ProcessingStatus.FAILED } -> SystemStatus.ERROR
            tasks.any { it.isRunning() } -> SystemStatus.PROCESSING
            tasks.any { it.status == ProcessingStatus.PAUSED } -> SystemStatus.PAUSED
            else -> SystemStatus.IDLE
        }
    }
    
    /**
     * 添加到历史记录
     */
    private fun addToHistory(
        state: MediaProcessingState,
        changeType: StateChangeType,
        description: String
    ) {
        val historyEntry = StateHistoryEntry(
            state = state,
            timestamp = System.currentTimeMillis(),
            changeType = changeType,
            description = description
        )
        
        stateHistory.add(historyEntry)
        
        // 限制历史记录大小
        while (stateHistory.size > MAX_HISTORY_SIZE) {
            stateHistory.removeAt(0)
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        synchronized(this) {
            taskMap.clear()
            stateHistory.clear()
            cumulativeStatistics = ProcessingStatistics()
            
            _currentState.value = MediaProcessingState(
                systemStatus = SystemStatus.IDLE
            )
        }
    }
}