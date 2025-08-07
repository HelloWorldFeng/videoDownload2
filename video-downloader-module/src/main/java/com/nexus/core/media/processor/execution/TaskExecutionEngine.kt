package com.nexus.core.media.processor.execution

import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.state.MediaProcessingState
import com.nexus.core.media.processor.state.ProcessingStatistics
import com.nexus.core.media.processor.utils.ProcessorLogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 任务执行引擎
 * 
 * 负责管理和执行媒体处理任务，包括任务调度、
 * 并发控制、状态管理等核心功能。
 * 
 * 主要功能：
 * - 任务队列管理
 * - 并发执行控制
 * - 任务状态跟踪
 * - 进度监控
 * - 错误处理和重试
 * 
 * 使用示例：
 * ```kotlin
 * val engine = TaskExecutionEngine(configuration)
 * val taskId = engine.addProcessingTask(mediaInfo, format, options)
 * engine.pauseProcessing(taskId)
 * engine.resumeProcessing(taskId)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class TaskExecutionEngine(
    private val configuration: ProcessorConfiguration
) {
    
    companion object {
        private const val LOG_TAG = "TaskExecutionEngine"
    }
    
    // 协程作用域
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 任务存储
    private val activeTasks = ConcurrentHashMap<String, ProcessingTask>()
    private val taskQueue = PriorityBlockingQueue<ProcessingTask>(11) { task1, task2 ->
        task2.priority.value.compareTo(task1.priority.value)
    }
    
    // 执行器
    private val taskExecutors = ConcurrentHashMap<String, TaskExecutor>()
    private val runningTaskCount = AtomicInteger(0)
    
    // 状态管理
    private val _processingState = MutableStateFlow(
        MediaProcessingState(
            activeTasks = emptyList(),
            queuedTasks = emptyList(),
            statistics = ProcessingStatistics(
                totalTasks = 0,
                completedTasks = 0,
                failedTasks = 0,
                cancelledTasks = 0,
                totalDownloadedBytes = 0L,
                averageSpeed = 0.0,
                totalProcessingTime = 0L
            )
        )
    )
    val processingState: StateFlow<MediaProcessingState> = _processingState.asStateFlow()
    
    // 统计数据
    private var totalCompletedTasks = 0
    private var totalFailedTasks = 0
    private var totalCancelledTasks = 0
    private var totalDownloadedBytes = 0L
    private var totalProcessingTime = 0L
    
    init {
        // 启动任务调度器
        startTaskScheduler()
        ProcessorLogUtils.i(LOG_TAG, "任务执行引擎初始化完成")
    }
    
    /**
     * 添加处理任务
     * 
     * @param mediaInfo 媒体信息
     * @param selectedFormat 选择的格式
     * @param processingOptions 处理选项
     * @param priority 任务优先级
     * @return 任务ID
     */
    suspend fun addProcessingTask(
        mediaInfo: MediaInfo,
        selectedFormat: MediaFormat,
        processingOptions: ProcessingOptions = ProcessingOptions(),
        priority: TaskPriority = TaskPriority.NORMAL
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // 生成输出文件路径
                val outputPath = generateOutputPath(mediaInfo, selectedFormat, processingOptions)
                
                // 创建任务
                val task = ProcessingTask.create(
                    mediaInfo = mediaInfo,
                    selectedFormat = selectedFormat,
                    outputPath = outputPath,
                    processingOptions = processingOptions,
                    priority = priority
                )
                
                // 添加到队列
                activeTasks[task.taskId] = task
                taskQueue.offer(task)
                
                ProcessorLogUtils.d(LOG_TAG, "任务已添加到队列: ${task.taskId} - ${mediaInfo.title}")
                
                // 更新状态
                updateProcessingState()
                
                // 触发任务调度
                scheduleNextTask()
                
                task.taskId
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "添加处理任务失败", e)
                throw e
            }
        }
    }
    
    /**
     * 暂停处理任务
     * 
     * @param taskId 任务ID
     */
    suspend fun pauseProcessing(taskId: String) {
        withContext(Dispatchers.IO) {
            try {
                val task = activeTasks[taskId]
                if (task?.canPause() == true) {
                    taskExecutors[taskId]?.pause()
                    
                    val updatedTask = task.updateStatus(newStatus = ProcessingStatus.PAUSED)
                    activeTasks[taskId] = updatedTask
                    
                    ProcessorLogUtils.d(LOG_TAG, "任务已暂停: $taskId")
                    updateProcessingState()
                } else {
                    ProcessorLogUtils.w(LOG_TAG, "任务无法暂停: $taskId")
                }
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "暂停任务失败: $taskId", e)
                throw e
            }
        }
    }
    
    /**
     * 恢复处理任务
     * 
     * @param taskId 任务ID
     */
    suspend fun resumeProcessing(taskId: String) {
        withContext(Dispatchers.IO) {
            try {
                val task = activeTasks[taskId]
                if (task?.canResume() == true) {
                    taskExecutors[taskId]?.resume()
                    
                    val updatedTask = task.updateStatus(newStatus = ProcessingStatus.PROCESSING)
                    activeTasks[taskId] = updatedTask
                    
                    ProcessorLogUtils.d(LOG_TAG, "任务已恢复: $taskId")
                    updateProcessingState()
                } else {
                    ProcessorLogUtils.w(LOG_TAG, "任务无法恢复: $taskId")
                }
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "恢复任务失败: $taskId", e)
                throw e
            }
        }
    }
    
    /**
     * 取消处理任务
     * 
     * @param taskId 任务ID
     */
    suspend fun cancelProcessing(taskId: String) {
        withContext(Dispatchers.IO) {
            try {
                val task = activeTasks[taskId]
                if (task?.canCancel() == true) {
                    taskExecutors[taskId]?.cancel()
                    taskExecutors.remove(taskId)
                    
                    val updatedTask = task.updateStatus(newStatus = ProcessingStatus.CANCELLED)
                    activeTasks[taskId] = updatedTask
                    
                    totalCancelledTasks++
                    runningTaskCount.decrementAndGet()
                    
                    ProcessorLogUtils.d(LOG_TAG, "任务已取消: $taskId")
                    updateProcessingState()
                    
                    // 调度下一个任务
                    scheduleNextTask()
                } else {
                    ProcessorLogUtils.w(LOG_TAG, "任务无法取消: $taskId")
                }
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "取消任务失败: $taskId", e)
                throw e
            }
        }
    }
    
    /**
     * 删除处理任务
     * 
     * @param taskId 任务ID
     * @param deleteFile 是否删除已下载的文件
     */
    suspend fun deleteProcessingTask(taskId: String, deleteFile: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val task = activeTasks[taskId]
                if (task != null) {
                    // 如果任务正在运行，先取消
                    if (task.isRunning()) {
                        cancelProcessing(taskId)
                    }
                    
                    // 删除文件
                    if (deleteFile) {
                        deleteTaskFiles(task)
                    }
                    
                    // 从存储中移除
                    activeTasks.remove(taskId)
                    taskExecutors.remove(taskId)
                    
                    ProcessorLogUtils.d(LOG_TAG, "任务已删除: $taskId")
                    updateProcessingState()
                } else {
                    ProcessorLogUtils.w(LOG_TAG, "任务不存在: $taskId")
                }
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "删除任务失败: $taskId", e)
                throw e
            }
        }
    }
    
    /**
     * 获取所有处理任务
     * 
     * @return 任务列表
     */
    suspend fun getAllProcessingTasks(): List<ProcessingTask> {
        return withContext(Dispatchers.IO) {
            activeTasks.values.toList().sortedByDescending { it.createdAt }
        }
    }
    
    /**
     * 获取指定状态的处理任务
     * 
     * @param status 任务状态
     * @return 任务列表
     */
    suspend fun getProcessingTasksByStatus(status: ProcessingStatus): List<ProcessingTask> {
        return withContext(Dispatchers.IO) {
            activeTasks.values.filter { it.status == status }
                .sortedByDescending { it.createdAt }
        }
    }
    
    /**
     * 获取任务详情
     * 
     * @param taskId 任务ID
     * @return 任务详情
     */
    suspend fun getTaskDetails(taskId: String): ProcessingTask? {
        return withContext(Dispatchers.IO) {
            activeTasks[taskId]
        }
    }
    
    /**
     * 清理已完成的任务
     * 
     * @param olderThanDays 清理多少天前的任务
     */
    suspend fun cleanupCompletedTasks(olderThanDays: Int = 7) {
        withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
                
                val tasksToRemove = activeTasks.values.filter { task ->
                    task.isFinished() && task.completedAt?.time ?: 0 < cutoffTime
                }
                
                tasksToRemove.forEach { task ->
                    activeTasks.remove(task.taskId)
                    taskExecutors.remove(task.taskId)
                }
                
                ProcessorLogUtils.i(LOG_TAG, "已清理 ${tasksToRemove.size} 个已完成的任务")
                updateProcessingState()
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "清理已完成任务失败", e)
            }
        }
    }
    
    /**
     * 启动任务调度器
     */
    private fun startTaskScheduler() {
        engineScope.launch {
            while (isActive) {
                try {
                    scheduleNextTask()
                    delay(1000) // 每秒检查一次
                } catch (e: Exception) {
                    ProcessorLogUtils.e(LOG_TAG, "任务调度器异常", e)
                    delay(5000) // 出错时等待5秒
                }
            }
        }
    }
    
    /**
     * 调度下一个任务
     */
    private suspend fun scheduleNextTask() {
        if (runningTaskCount.get() >= configuration.maxParallelTasks) {
            return
        }
        
        val nextTask = taskQueue.poll() ?: return
        
        if (nextTask.status == ProcessingStatus.PENDING) {
            executeTask(nextTask)
        }
    }
    
    /**
     * 执行任务
     */
    private suspend fun executeTask(task: ProcessingTask) {
        try {
            runningTaskCount.incrementAndGet()
            
            // 更新任务状态为准备中
            val preparingTask = task.updateStatus(newStatus = ProcessingStatus.PREPARING)
            activeTasks[task.taskId] = preparingTask
            updateProcessingState()
            
            // 创建任务执行器
            val executor = TaskExecutor(
                task = preparingTask,
                configuration = configuration,
                onProgressUpdate = { updatedTask ->
                    activeTasks[updatedTask.taskId] = updatedTask
                    updateProcessingState()
                },
                onTaskCompleted = { completedTask ->
                    handleTaskCompleted(completedTask)
                },
                onTaskFailed = { failedTask, error ->
                    handleTaskFailed(failedTask, error)
                }
            )
            
            taskExecutors[task.taskId] = executor
            
            // 启动执行
            executor.execute()
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "执行任务失败: ${task.taskId}", e)
            handleTaskFailed(task, e.message ?: "未知错误")
        }
    }
    
    /**
     * 处理任务完成
     */
    private suspend fun handleTaskCompleted(task: ProcessingTask) {
        try {
            val completedTask = task.updateStatus(newStatus = ProcessingStatus.COMPLETED)
            activeTasks[task.taskId] = completedTask
            taskExecutors.remove(task.taskId)
            
            totalCompletedTasks++
            totalDownloadedBytes += task.downloadedBytes
            task.getProcessingDuration()?.let { duration ->
                totalProcessingTime += duration
            }
            runningTaskCount.decrementAndGet()
            
            ProcessorLogUtils.i(LOG_TAG, "任务完成: ${task.taskId} - ${task.mediaInfo.title}")
            updateProcessingState()
            
            // 调度下一个任务
            scheduleNextTask()
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "处理任务完成事件失败", e)
        }
    }
    
    /**
     * 处理任务失败
     */
    private suspend fun handleTaskFailed(task: ProcessingTask, error: String) {
        try {
            val failedTask = task.updateStatus(
                newStatus = ProcessingStatus.FAILED,
                newErrorMessage = error
            )
            activeTasks[task.taskId] = failedTask
            taskExecutors.remove(task.taskId)
            
            totalFailedTasks++
            runningTaskCount.decrementAndGet()
            
            ProcessorLogUtils.e(LOG_TAG, "任务失败: ${task.taskId} - $error")
            updateProcessingState()
            
            // 检查是否需要重试
            if (task.canRetry()) {
                retryTask(failedTask)
            } else {
                // 调度下一个任务
                scheduleNextTask()
            }
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "处理任务失败事件失败", e)
        }
    }
    
    /**
     * 重试任务
     */
    private suspend fun retryTask(task: ProcessingTask) {
        try {
            delay(configuration.retryDelayMs)
            
            val retryTask = task.copy(
                status = ProcessingStatus.PENDING,
                retryCount = task.retryCount + 1,
                errorMessage = null,
                errorCode = null
            )
            
            activeTasks[task.taskId] = retryTask
            taskQueue.offer(retryTask)
            
            ProcessorLogUtils.i(LOG_TAG, "任务重试: ${task.taskId} (${retryTask.retryCount}/${task.maxRetryCount})")
            updateProcessingState()
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "重试任务失败: ${task.taskId}", e)
        }
    }
    
    /**
     * 生成输出文件路径
     */
    private fun generateOutputPath(
        mediaInfo: MediaInfo,
        format: MediaFormat,
        options: ProcessingOptions
    ): String {
        val fileName = options.filenameTemplate?.let { template ->
            // 使用自定义模板
            template.replace("{title}", mediaInfo.title)
                .replace("{id}", mediaInfo.id)
                .replace("{ext}", format.extension ?: "mp4")
        } ?: mediaInfo.getRecommendedFileName(format)
        
        return "${configuration.outputDirectory}/$fileName"
    }
    
    /**
     * 删除任务文件
     */
    private fun deleteTaskFiles(task: ProcessingTask) {
        try {
            // 删除输出文件
            val outputFile = java.io.File(task.outputFilePath)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            // 删除临时文件
            task.tempFilePath?.let { tempPath ->
                val tempFile = java.io.File(tempPath)
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            
            ProcessorLogUtils.d(LOG_TAG, "已删除任务文件: ${task.taskId}")
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "删除任务文件失败: ${task.taskId}", e)
        }
    }
    
    /**
     * 更新处理状态
     */
    private fun updateProcessingState() {
        val currentTasks = activeTasks.values.toList()
        val queuedTasks = taskQueue.toList()
        
        val statistics = com.nexus.core.media.processor.state.ProcessingStatistics(
            totalTasks = totalCompletedTasks + totalFailedTasks + totalCancelledTasks + currentTasks.size,
            completedTasks = totalCompletedTasks,
            failedTasks = totalFailedTasks,
            cancelledTasks = totalCancelledTasks,
            totalDownloadedBytes = totalDownloadedBytes,
            averageSpeed = calculateAverageSpeed().toDouble(),
            totalProcessingTime = totalProcessingTime
        )
        
        val newState = MediaProcessingState(
            activeTasks = currentTasks,
            queuedTasks = queuedTasks,
            statistics = statistics
        )
        
        _processingState.value = newState
    }
    
    /**
     * 计算平均速度
     */
    private fun calculateAverageSpeed(): Long {
        return if (totalProcessingTime > 0) {
            totalDownloadedBytes * 1000 / totalProcessingTime
        } else {
            0L
        }
    }
    
    /**
     * 清理资源
     */
    suspend fun cleanup() {
        try {
            ProcessorLogUtils.i(LOG_TAG, "开始清理任务执行引擎")
            
            // 取消所有正在运行的任务
            taskExecutors.values.forEach { executor ->
                executor.cancel()
            }
            
            // 清理数据
            activeTasks.clear()
            taskQueue.clear()
            taskExecutors.clear()
            
            // 取消协程作用域
            engineScope.cancel()
            
            ProcessorLogUtils.i(LOG_TAG, "任务执行引擎清理完成")
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "清理任务执行引擎失败", e)
        }
    }
}