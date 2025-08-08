package com.videodownloader.module.download

import com.videodownloader.module.api.VideoInfo
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.videodownloader.module.api.DownloadUtil
import com.videodownloader.module.encodeTaskListBackup

private const val TAG = "DownloaderV2"
private const val MAX_CONCURRENCY = 3

/**
 * 视频下载器接口
 * 
 * 核心功能：
 * - 管理下载任务队列和状态
 * - 支持任务的入队、取消、重启和移除
 * - 提供任务状态的实时监听
 * - 支持任务备份和恢复
 */
interface DownloaderV2 {
    /** 任务状态映射表，提供实时状态监听 */
    val taskStateMap: StateFlow<Map<String, Task.State>>

    /**
     * 将任务加入下载队列
     * @param task 下载任务
     * @param videoInfo 视频信息
     */
    fun enqueue(task: Task, videoInfo: VideoInfo)

    /**
     * 取消指定任务
     * @param taskId 任务标识符
     */
    fun cancel(taskId: String)

    /**
     * 重启指定任务
     * @param taskId 任务标识符
     */
    fun restart(taskId: String)

    /**
     * 移除指定任务
     * @param taskId 任务标识符
     */
    fun remove(taskId: String)

    /**
     * 从备份中恢复任务
     * @param tasks 任务列表
     */
    fun restoreFromBackup(tasks: List<Task>)

    /**
     * 从备份中恢复任务和状态
     * @param tasksWithStates 任务和状态的映射
     */
    fun restoreFromBackupWithStates(tasksWithStates: Map<Task, Task.State>)
}

/**
 * 视频下载器实现类
 * 
 * 技术特性：
 * - 使用协程进行异步下载管理
 * - 通过信号量控制并发下载数量
 * - 支持MP4和M3U8两种视频格式
 * - 提供完整的任务生命周期管理
 * - 自动进行任务状态备份
 */
class DownloaderV2Impl(private val context: Context) : DownloaderV2 {

    /** 协程作用域：使用IO调度器和SupervisorJob确保异常隔离 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 任务状态映射表的内部可变状态 */
    private val _taskStateMap = MutableStateFlow<Map<String, Task.State>>(emptyMap())
    
    /** 对外暴露的只读任务状态映射表 */
    override val taskStateMap: StateFlow<Map<String, Task.State>> = _taskStateMap.asStateFlow()

    /** 并发控制信号量：限制同时下载的任务数量 */
    private val semaphore = Semaphore(MAX_CONCURRENCY)

    /**
     * 将任务加入下载队列
     * 
     * 处理逻辑：
     * 1. 检查任务是否已在运行中
     * 2. 更新任务状态为空闲状态
     * 3. 创建视图状态用于UI展示
     * 4. 启动下载工作
     */
    override fun enqueue(task: Task, videoInfo: VideoInfo) {
        Log.d(TAG, "正在将任务加入队列: ${task.id}")

        val currentState = _taskStateMap.value[task.id]?.downloadState

        // 防止重复启动已运行的任务
        if (currentState is Task.DownloadState.Running) {
            Log.w(TAG, "任务 ${task.id} 已在运行中")
            return
        }

        // 更新任务状态并创建视图状态
        updateTaskState(task.id) { currentTaskState ->
            currentTaskState?.copy(
                downloadState = Task.DownloadState.Idle,
                videoInfo = videoInfo,
            ) ?: Task.State(
                downloadState = Task.DownloadState.Idle,
                videoInfo = videoInfo,
                viewState = createViewState(task, videoInfo),
            )
        }

        // 启动下载工作
        startDownloadWork(task)
    }

    /**
     * 取消指定任务
     * 
     * 处理逻辑：
     * 1. 检查任务是否可取消
     * 2. 取消协程任务
     * 3. 更新任务状态为已取消
     */
    override fun cancel(taskId: String) {
        Log.d(TAG, "正在取消任务: $taskId")

        val currentState = _taskStateMap.value[taskId]?.downloadState

        if (currentState !is Task.DownloadState.Cancelable) {
            Log.w(TAG, "任务 $taskId 不可取消")
            return
        }

        // 取消协程任务
        currentState.job.cancel()

        // 更新状态为已取消
        updateTaskState(taskId) { currentTaskState ->
            currentTaskState?.copy(
                downloadState = Task.DownloadState.Canceled(
                    action = currentState.action,
                    progress = (currentState as? Task.DownloadState.Running)?.progress,
                ),
            )
        }
    }

    /**
     * 重启指定任务
     * 
     * 处理逻辑：
     * 1. 检查任务是否可重启
     * 2. 根据操作类型重新创建任务
     * 3. 重新加入下载队列
     */
    override fun restart(taskId: String) {
        Log.d(TAG, "正在重启任务: $taskId")

        val currentState = _taskStateMap.value[taskId]?.downloadState

        if (currentState !is Task.DownloadState.Restartable) {
            Log.w(TAG, "任务 $taskId 不可重启")
            return
        }

        // 根据操作类型重启任务
        when (currentState.action) {
            Task.RestartableAction.Download -> {
                val taskState = _taskStateMap.value[taskId]
                if (taskState != null) {
                    val task = recreateTaskFromState(taskId, taskState)
                    enqueue(task, taskState.videoInfo)
                }
            }
        }
    }

    /**
     * 移除指定任务
     * 
     * 处理逻辑：
     * 1. 如果任务正在运行，先取消
     * 2. 从状态映射表中移除
     * 3. 更新备份数据
     */
    override fun remove(taskId: String) {
        Log.d(TAG, "正在移除任务: $taskId")

        val currentState = _taskStateMap.value[taskId]?.downloadState

        // 如果任务正在运行，先取消
        if (currentState is Task.DownloadState.Cancelable) {
            currentState.job.cancel()
        }

        // 从状态映射表中移除
        _taskStateMap.value = _taskStateMap.value.toMutableMap().apply {
            remove(taskId)
        }

        // 更新备份
        val taskMap = _taskStateMap.value.mapKeys { (taskId, taskState) ->
            recreateTaskFromState(taskId, taskState)
        }
        encodeTaskListBackup(taskMap)
    }

    /**
     * 从备份中恢复任务
     * 
     * 处理逻辑：
     * 1. 遍历备份任务列表
     * 2. 为每个任务创建空闲状态
     * 3. 更新状态映射表
     */
    override fun restoreFromBackup(tasks: List<Task>) {
        Log.d(TAG, "正在从备份恢复 ${tasks.size} 个任务")

        tasks.forEach { task ->
            updateTaskState(task.id) { currentTaskState ->
                currentTaskState ?: Task.State(
                    downloadState = Task.DownloadState.Idle,
                    videoInfo = VideoInfo(
                        id = task.id,
                        title = "Unknown Video",
                        duration = 0f,
                        thumbnail = "",
                        size = 0L,
                        url = task.url,
                        ext = if (task.type == Task.VideoType.M3U8) "m3u8" else "mp4"
                    ),
                    viewState = Task.ViewState(url = task.url),
                )
            }
        }
    }

    /**
     * 从备份中恢复任务和状态
     * 
     * 处理逻辑：
     * 1. 遍历备份的任务和状态映射
     * 2. 直接恢复原始状态（保持Completed、Error等状态）
     * 3. 清理运行中的任务状态（避免恢复无效的Job引用）
     */
    override fun restoreFromBackupWithStates(tasksWithStates: Map<Task, Task.State>) {
        Log.d(TAG, "正在从备份恢复 ${tasksWithStates.size} 个任务及其状态")

        tasksWithStates.forEach { (task, originalState) ->
            updateTaskState(task.id) { currentTaskState ->
                // 如果当前没有状态，则恢复原始状态
                currentTaskState ?: run {
                    // 清理运行中的状态，避免恢复无效的Job引用
                    val cleanedState = when (originalState.downloadState) {
                        is Task.DownloadState.Running -> {
                            // 将运行中的任务恢复为空闲状态
                            originalState.copy(downloadState = Task.DownloadState.Idle)
                        }
                        else -> {
                            // 其他状态（Completed、Error、Canceled等）直接恢复
                            originalState
                        }
                    }
                    Log.d(TAG, "恢复任务 ${task.id} 状态: ${cleanedState.downloadState::class.simpleName}")
                    cleanedState
                }
            }
        }
    }

    /**
     * 启动下载工作
     * 
     * 核心下载逻辑：
     * 1. 使用信号量控制并发
     * 2. 根据视频类型选择下载方法
     * 3. 处理下载异常和状态更新
     */
    private fun startDownloadWork(task: Task) {
        val job = scope.launch {
            semaphore.withPermit {
                try {
                    // 更新为运行状态
                    updateTaskState(task.id) { currentTaskState ->
                        currentTaskState?.copy(
                            downloadState = Task.DownloadState.Running(
                                job = coroutineContext[Job]!!,
                                taskId = task.id,
                            ),
                        )
                    }

                    // 根据视频类型执行下载
                    when (task.type) {
                        Task.VideoType.MP4 -> executeMP4Download(task)
                        Task.VideoType.M3U8 -> executeM3U8Download(task)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "下载任务 ${task.id} 时发生错误", e)
                    updateTaskState(task.id) { currentTaskState ->
                        currentTaskState?.copy(
                            downloadState = Task.DownloadState.Error(
                                throwable = e,
                                action = Task.RestartableAction.Download,
                            ),
                        )
                    }
                }
            }
        }

        // 立即更新为运行状态（包含job引用）
        updateTaskState(task.id) { currentTaskState ->
            currentTaskState?.copy(
                downloadState = Task.DownloadState.Running(
                    job = job,
                    taskId = task.id,
                ),
            )
        }
    }

    /**
     * 执行MP4视频下载
     * 
     * @param task 下载任务
     */
    private suspend fun executeMP4Download(task: Task) {
        Log.d(TAG, "开始执行MP4下载任务: ${task.id}")

        val result = DownloadUtil.downloadMP4Video(
            context = context,
            videoInfo = _taskStateMap.value[task.id]?.videoInfo ?: VideoInfo(
                id = task.id,
                title = "Unknown Video",
                duration = 0f,
                thumbnail = "",
                size = 0L,
                url = task.url,
                ext = "mp4"
            ),
            taskId = task.id,
            progressCallback = { progressPercentage, long, text ->
                Log.d("下载", "下载进度::${progressPercentage}，${long},${text} ")
                val progress = progressPercentage / 100f
                val pattern2 = """\d+\.?\d*\s*[KMG]iB/s""".toRegex()
                val speedText = pattern2.find(text)?.value ?: ""
                updateDownloadProgress(task.id, progress, text, speedText)
            },
        )

        handleDownloadResult(task.id, result)
    }

    /**
     * 更新下载进度
     * 
     * @param taskId 任务标识符
     * @param progress 下载进度
     * @param progressText 进度描述文本
     * @param speed 下载速度
     */
    private fun updateDownloadProgress(taskId: String, progress: Float, progressText: String, speed: String) {
        updateTaskState(taskId) { currentTaskState ->
            val currentDownloadState = currentTaskState?.downloadState
            if (currentDownloadState is Task.DownloadState.Running) {
                currentTaskState.copy(
                    downloadState = currentDownloadState.copy(
                        progress = progress,
                        speed = speed,
                    ),
                )
            } else {
                currentTaskState
            }
        }
    }

    /**
     * 处理下载结果
     * 
     * @param taskId 任务标识符
     * @param result 下载结果
     */
    private fun handleDownloadResult(taskId: String, result: Result<String>) {
        if (result.isSuccess) {
            updateTaskState(taskId) { currentTaskState ->
                currentTaskState?.copy(
                    downloadState = Task.DownloadState.Completed(
                        filePath = result.getOrNull()
                    ),
                )
            }
        } else {
            updateTaskState(taskId) { currentTaskState ->
                currentTaskState?.copy(
                    downloadState = Task.DownloadState.Error(
                        throwable = result.exceptionOrNull() ?: Exception("未知错误"),
                        action = Task.RestartableAction.Download,
                    ),
                )
            }
        }
    }

    /**
     * 创建视图状态
     * 
     * @param task 下载任务
     * @param videoInfo 视频信息
     * @return 视图状态对象
     */
    private fun createViewState(task: Task, videoInfo: VideoInfo): Task.ViewState {
        return Task.ViewState(
            url = task.url,
            title = videoInfo.title ?: "",
        )
    }

    /**
     * 从任务状态重新创建任务对象
     * 
     * @param taskId 任务标识符
     * @param taskState 任务状态
     * @return 重新创建的任务对象
     */
    private fun recreateTaskFromState(taskId: String, taskState: Task.State): Task {
        val videoType = if (taskId.endsWith("_m3u8")) {
            Task.VideoType.M3U8
        } else {
            Task.VideoType.MP4
        }
        
        return Task(
            url = taskState.viewState.url,
            type = videoType,
            id = taskId,
        )
    }


    /**
     * 执行M3U8视频下载
     * 
     * @param task 下载任务
     */
    private suspend fun executeM3U8Download(task: Task) {
        Log.d(TAG, "开始执行M3U8下载任务: ${task.id}")

        val videoInfo = _taskStateMap.value[task.id]?.videoInfo ?: VideoInfo(
            id = task.id,
            title = "Unknown Video",
            duration = 0f,
            thumbnail = "",
            size = 0L,
            url = task.url,
            ext = "m3u8"
        )
        val result = DownloadUtil.downloadM3U8Video(
            context = context,
            videoInfo = videoInfo,
            title = videoInfo.title?.ifEmpty { "HD_video:${System.currentTimeMillis()}" } ?: "HD_video:${System.currentTimeMillis()}",
            progressCallback = { progressPercentage, long, text ->
                val progress = progressPercentage / 100f
                val pattern2 = """\d+\.?\d*\s*[KMG]iB/s""".toRegex()
                val speedText = pattern2.find(text)?.value ?: ""
                Log.d("下载", "M3U8下载中-----> :${progress},----speedText:${speedText}")
                updateDownloadProgress(task.id, progress, text, speedText)
            }
        )

        handleDownloadResult(task.id, result)
    }

    /**
     * 更新任务状态
     * 
     * 线程安全的状态更新方法：
     * 1. 应用状态更新函数
     * 2. 更新状态映射表
     * 3. 自动备份任务数据
     * 
     * @param taskId 任务标识符
     * @param update 状态更新函数
     */
    private fun updateTaskState(taskId: String, update: (Task.State?) -> Task.State?) {
        _taskStateMap.value = _taskStateMap.value.toMutableMap().apply {
            val newState = update(this[taskId])
            if (newState != null) {
                this[taskId] = newState
            }
        }

        // 自动备份任务数据
        val taskMap = _taskStateMap.value.mapKeys { (taskId, taskState) ->
            recreateTaskFromState(taskId, taskState)
        }
        encodeTaskListBackup(taskMap)
    }

    /**
     * 销毁下载器
     * 取消所有协程任务，释放资源
     */
    fun destroy() {
        scope.cancel()
    }
}
