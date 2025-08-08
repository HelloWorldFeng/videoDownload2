package com.videodownloader.module.api

import android.content.Context
import android.util.Log
import com.videodownloader.module.decodeTaskListBackup
import com.videodownloader.module.download.DownloaderV2
import com.videodownloader.module.download.DownloaderV2Impl
import com.videodownloader.module.download.Task
import com.videodownloader.module.download.TaskFactory
import kotlinx.coroutines.flow.StateFlow

/**
 * 视频下载管理器
 * 
 * 这是一个简化的API管理器，提供统一的视频下载功能入口。
 * 主要功能：
 * - 管理下载任务的生命周期
 * - 提供简洁的API接口
 * - 处理任务状态监听
 * - 支持MP4和M3U8格式视频下载
 * 
 * 使用示例：
 * ```kotlin
 * // 初始化
 * VideoDownloaderManager.initialize(context)
 * 
 * // 开始下载
 * val videoInfo = VideoInfo(url = "https://example.com/video.mp4", title = "示例视频")
 * VideoDownloaderManager.startDownload(videoInfo) { progress, speed, status ->
 *     // 处理下载进度
 * }
 * 
 * // 监听任务状态
 * VideoDownloaderManager.observeTaskStates().collect { taskStates ->
 *     // 处理任务状态变化
 * }
 * ```
 */
object VideoDownloaderManager {
    
    private const val TAG = "VideoDownloaderManager"
    
    private var downloader: DownloaderV2? = null
    private var isInitialized = false
    
    /**
     * 初始化下载管理器
     * @param context 应用上下文
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.w(TAG, "VideoDownloaderManager已经初始化，跳过重复初始化")
            return
        }
        
        try {
            downloader = DownloaderV2Impl(context)
            isInitialized = true
            Log.i(TAG, "VideoDownloaderManager初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "VideoDownloaderManager初始化失败", e)
            throw e
        }
    }
    
    /**
     * 检查是否已初始化
     */
    private fun ensureInitialized() {
        if (!isInitialized || downloader == null) {
            throw IllegalStateException("VideoDownloaderManager未初始化，请先调用initialize(context)")
        }
    }
    
    /**
     * 开始下载视频
     * @param videoInfo 视频信息
     * @param progressCallback 下载进度回调
     * @return 任务ID，用于后续操作
     */
    fun startDownload(
        videoInfo: VideoInfo,
        progressCallback: ((Float, Long, String) -> Unit)? = null
    ): String {
        ensureInitialized()
        
        Log.d(TAG, "开始下载视频: url=${videoInfo.url}, title=${videoInfo.title}")
        
        // 根据URL判断视频类型并创建任务
        val taskWithState = TaskFactory.createTaskByType(videoInfo)
        val task = taskWithState.task
        
        // 将任务加入下载队列
        downloader!!.enqueue(task, videoInfo)
        
        Log.i(TAG, "视频下载任务已加入队列: taskId=${task.id}")
        return task.id
    }
    
    /**
     * 取消下载任务
     * @param taskId 任务ID
     */
    fun cancelDownload(taskId: String) {
        ensureInitialized()
        Log.d(TAG, "取消下载任务: taskId=$taskId")
        downloader!!.cancel(taskId)
    }
    
    /**
     * 重启下载任务
     * @param taskId 任务ID
     */
    fun restartDownload(taskId: String) {
        ensureInitialized()
        Log.d(TAG, "重启下载任务: taskId=$taskId")
        downloader!!.restart(taskId)
    }
    
    /**
     * 移除下载任务
     * @param taskId 任务ID
     */
    fun removeTask(taskId: String) {
        ensureInitialized()
        Log.d(TAG, "移除下载任务: taskId=$taskId")
        downloader!!.remove(taskId)
    }
    
    /**
     * 获取任务状态流
     * @return 任务状态的StateFlow
     */
    fun observeTaskStates(): StateFlow<Map<String, Task.State>> {
        ensureInitialized()
        return downloader!!.taskStateMap
    }
    
    /**
     * 获取指定任务的状态
     * @param taskId 任务ID
     * @return 任务状态，如果任务不存在则返回null
     */
    fun getTaskState(taskId: String): Task.State? {
        ensureInitialized()
        return downloader!!.taskStateMap.value[taskId]
    }
    
    /**
     * 获取所有任务状态
     * @return 所有任务状态的Map
     */
    fun getAllTaskStates(): Map<String, Task.State> {
        ensureInitialized()
        return downloader!!.taskStateMap.value
    }
    
    /**
     * 从备份恢复任务
     */
    fun restoreFromBackup() {
        ensureInitialized()
        Log.d(TAG, "从备份恢复下载任务")
        // 从备份中解码任务列表和状态
        val backupTasks = decodeTaskListBackup()
        downloader!!.restoreFromBackupWithStates(backupTasks)
    }
    
    /**
     * 根据URL判断视频类型
     * @param url 视频URL
     * @return 视频类型
     */
    fun getVideoType(url: String): Task.VideoType {
        return when {
            url.contains(".m3u8", ignoreCase = true) -> Task.VideoType.M3U8
            url.contains(".mp4", ignoreCase = true) -> Task.VideoType.MP4
            else -> Task.VideoType.MP4 // 默认为MP4
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        if (isInitialized) {
            downloader = null
            isInitialized = false
            Log.i(TAG, "VideoDownloaderManager资源已释放")
        }
    }
}