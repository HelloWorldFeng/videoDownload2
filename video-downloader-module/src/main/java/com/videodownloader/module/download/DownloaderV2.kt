package com.videodownloader.module.download

import VideoInfo
import android.content.Context
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.videodownloader.module.api.DownloadUtil
import com.videodownloader.module.download.Task.DownloadState
import com.videodownloader.module.download.Task.DownloadState.Canceled
import com.videodownloader.module.download.Task.DownloadState.Completed
import com.videodownloader.module.download.Task.DownloadState.Idle
import com.videodownloader.module.download.Task.DownloadState.Running
import com.videodownloader.module.download.Task.RestartableAction.Download
import com.videodownloader.module.download.Task.TypeInfo
import com.videodownloader.module.decodeTaskListBackup
import com.videodownloader.module.encodeTaskListBackup
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "DownloaderV2"

private const val MAX_CONCURRENCY = 3

interface DownloaderV2 {
    fun getTaskStateMap(): SnapshotStateMap<Task, Task.State>

    fun cancel(task: Task): Boolean

    fun cancel(taskId: String): Boolean {
        return getTaskStateMap().keys.find { it.id == taskId }?.let { cancel(it) } ?: false
    }

    fun restart(task: Task)

    /** Enqueue a [Task] with an empty [Task.State] */
    fun enqueue(task: Task)

    fun enqueue(task: Task, state: Task.State)

    fun enqueue(taskWithState: TaskFactory.TaskWithState) {
        val (task, state) = taskWithState
        enqueue(task, state)
    }

    fun remove(task: Task): Boolean
}

/**
 * TODO:
 *     - Notification
 *     - Custom commands
 *     - States for ViewModels
 */
@OptIn(FlowPreview::class)
class DownloaderV2Impl(private val context: Context) : DownloaderV2 {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskStateMap = mutableStateMapOf<Task, Task.State>()
    private val snapshotFlow = snapshotFlow { taskStateMap.toMap() }

    init {
        scope.launch(Dispatchers.Default) {
            snapshotFlow
                .onEach {
                    doYourWork()
                }
                .map { it.countRunning() }
                .distinctUntilChanged()
                .collect {
                }
        }

        scope.launch(Dispatchers.IO) {
            // don't write before we read
            enqueueFromBackup()

            snapshotFlow
                .distinctUntilChanged()
                .collect {
                    it.forEach { Log.d(TAG, "${it.value.downloadState}") }
                    encodeTaskListBackup(it)
                }
        }
    }

    private fun enqueueFromBackup() {
        val taskList = decodeTaskListBackup()
                .mapValues { (_, state) ->
                    val preState = state.downloadState
                    val downloadState =
                        when (preState) {
                            is Idle -> {
                                Canceled(action = Download)
                            }
                            is Running -> {
                                Canceled(action = Download, progress = preState.progress)
                            }

                            else -> {
                                preState
                            }
                        }
                    state.copy(downloadState = downloadState)
                }
        taskList.forEach(::enqueue)
    }

    private fun Map<Task, Task.State>.countRunning(): Int = count { (_, state) ->
        state.downloadState is Running
    }

    override fun getTaskStateMap(): SnapshotStateMap<Task, Task.State> {
        return taskStateMap
    }

    override fun enqueue(task: Task) {
        // 创建默认的 VideoInfo 对象
        val defaultVideoInfo = VideoInfo(
            id = task.id,
            title = task.url,
            duration = 0f,
            size = 0,
            thumbnail = "",
            url = task.url,
            ext = "mp4"
        )
        taskStateMap += task to Task.State(Idle, defaultVideoInfo, Task.ViewState(url = task.url, title = task.url))
    }

    override fun enqueue(task: Task, state: Task.State) {
        taskStateMap += task to state
    }

    /**
     * Noted the caller is responsible for stopping the [task] before removing it
     *
     * @return true if the task was removed
     */
    override fun remove(task: Task): Boolean {
        if (taskStateMap.contains(task)) {
            taskStateMap.remove(task)
            return true
        }
        return false
    }

    override fun cancel(task: Task): Boolean = task.cancelImpl()

    override fun restart(task: Task) {
        task.restartImpl()
    }

    private var Task.state: Task.State?
        get() = taskStateMap[this]
        set(value) {
            if (value != null) {
                taskStateMap[this] = value
            }
        }

    private var Task.downloadState: DownloadState?
        get() = state?.downloadState
        set(value) {
            val prevState = state
            if (prevState != null && value != null) {
                taskStateMap[this] = prevState.copy(downloadState = value)
            }
        }

    private var Task.info: VideoInfo?
        get() = state?.videoInfo
        set(value) {
            val prevState = state
            if (prevState != null && value != null) {
                taskStateMap[this] = prevState.copy(videoInfo = value)
            }
        }

    private var Task.viewState: Task.ViewState?
        get() = state?.viewState
        set(value) {
            val prevState = state
            if (prevState != null && value != null) {
                taskStateMap[this] = prevState.copy(viewState = value)
            }
        }

    private val Task.notificationId: Int
        get() = id.hashCode()

    /** Processes pending tasks, prioritizing downloads. */
    private fun doYourWork() {
        if (taskStateMap.countRunning() >= MAX_CONCURRENCY) return

        taskStateMap.entries
            .sortedBy { (_, state) -> state.downloadState }
            .firstOrNull { (_, state) ->
                 state.downloadState == Idle
            }
            ?.let { (task, state) ->
                when (state.downloadState) {
                    Idle -> task.download()
                    else -> {
                        throw IllegalStateException()
                    }
                }
            }
    }

    

    private fun Task.download() {

        when (type) {
            is TypeInfo.M3U8 -> {
                executeM3U8()
            }
            is TypeInfo.MP4 -> {
                executeMP4()
            }
        }
    }

    private fun Task.executeMP4() {
        val taskInfo = info
        if (taskInfo == null) {
            Log.w("下载", "任务信息为空，无法开始下载")
            return
        }
        
        scope.launch(Dispatchers.Default) {
            DownloadUtil.downloadMP4Video(
                context = context,
                videoInfo = taskInfo,
                taskId = id,
                progressCallback = { progressPercentage, long, text ->
                    Log.d("下载", "下载进度::${progressPercentage}，${long},${text} ")
                    val progress = progressPercentage / 100f
                    when (val preState = downloadState) {
                        is Running -> {
                            val speedText = Formatter.formatFileSize(context,long)
                            downloadState = preState.copy(progress = progress, speed = speedText)
                        }
                        else -> {
                            // 任务可能已被删除，忽略进度更新
                            Log.d("下载", "任务状态异常或已被删除，忽略进度更新")
                        }
                    }
                },
            )
                .onSuccess { pathList ->
                    Log.d("下载", "下载成功::${pathList} ")
                    downloadState = Completed(pathList)
                }
                .onFailure { throwable ->
                    Log.d("下载", "下载失败::${throwable.message} ")
                    downloadState = DownloadState.Error(throwable = throwable, action = Download)
                }
        }
            .also { job -> downloadState = Running(job = job, taskId = id) }
    }

    private fun Task.cancelImpl(): Boolean {
        val preState = downloadState
        if (preState == null) {
            Log.w("下载", "任务状态为空，可能已被删除")
            return false
        }
        
        when (preState) {
            is DownloadState.Cancelable, -> {
                preState.job.cancel()
                val progress = if (preState is Running) preState.progress else null
                downloadState = Canceled(action = preState.action, progress = progress)
                return true
            }

            Idle -> {
                // 对于空闲状态的任务，直接设置为已取消状态
                downloadState = Canceled(action = Download)
                return true
            }
            else -> {
                return false
            }
        }
    }

    private fun Task.restartImpl() {
        val preState = downloadState
        if (preState == null) {
            Log.w("下载", "任务状态为空，无法重启")
            return
        }
        
        when (preState) {
            is DownloadState.Restartable -> {
                // 重启任务时，统一设置为空闲状态，等待调度器重新开始下载
                downloadState = Idle
            }
            else -> {
                throw IllegalStateException("无法重启当前状态的任务: ${preState::class.simpleName}")
            }
        }
    }


    /**
     * Execute an M3U8 task
     *
     * @see TypeInfo.M3U8
     */
    private fun Task.executeM3U8() {
        val currentState = downloadState
        check(currentState == Idle) { "任务状态必须为Idle才能开始M3U8下载，当前状态: $currentState" }
        check(type is TypeInfo.M3U8)
        
        val taskInfo = info
        if (taskInfo == null) {
            Log.w("下载", "任务信息为空，无法开始M3U8下载")
            return
        }
        
        scope.launch(Dispatchers.Default) {
            DownloadUtil.downloadM3U8Video(
                context = context,
                videoInfo = taskInfo,
                title = taskInfo.title.ifEmpty { "HD_video:${System.currentTimeMillis()}" },
                progressCallback = { progressPercentage, long, text ->
                    val progress = progressPercentage / 100f
                    when (val preState = downloadState) {
                        is Running -> {
                            val pattern2 = """\d+\.?\d*\s*[KMG]iB/s""".toRegex()
                            val speedText = pattern2.find(text)?.value?:""
                            Log.d("下载", "M3U8下载中-----> :${progress},----speedText:${speedText}")
                            downloadState = preState.copy(progress = progress, speed = speedText)
                        }
                        else -> {
                            // 任务可能已被删除，忽略进度更新
                            Log.d("下载", "M3U8任务状态异常或已被删除，忽略进度更新")
                        }
                    }
                }
            )
                .onSuccess { pathList ->
                    Log.d("下载", "FFmpeg---> 下载成功::${pathList} ")

                    downloadState = Completed(pathList)
                }
                .onFailure { throwable ->
                    Log.d("下载", "FFmpeg--->下载失败::${throwable.message} ")

                    downloadState = DownloadState.Error(throwable = throwable, action = Download)
                }

        }.also { job ->
            downloadState = Running(job = job, taskId = id) 
            Log.d("FFmpeg下载", "任务状态已设置为Running，当前状态: ${this.downloadState}")
        }
    }
}
