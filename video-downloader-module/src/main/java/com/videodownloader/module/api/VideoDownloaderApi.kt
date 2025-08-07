import kotlinx.serialization.Serializable

//package com.videodownloader.module.api
//
//import android.content.Context
//import android.content.SharedPreferences
//import android.net.ConnectivityManager
//import android.net.Network
//import android.net.NetworkCapabilities
//import android.net.NetworkRequest
//import android.util.Log
//import androidx.compose.runtime.mutableStateMapOf
//import androidx.compose.runtime.snapshots.SnapshotStateMap
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.delay
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import kotlinx.serialization.Serializable
//import java.net.SocketTimeoutException
//import java.net.UnknownHostException
//import java.io.IOException
//
///**
// * 视频下载API主接口
// * 提供视频信息获取、下载管理等核心功能
// */
//class VideoDownloaderApi private constructor() {
//
//    companion object {
//        private const val TAG = "VideoDownloaderApi"
//        private const val PREFS_NAME = "video_downloader_prefs"
//        private const val KEY_DOWNLOAD_TASKS = "download_tasks"
//        @Volatile
//        private var INSTANCE: VideoDownloaderApi? = null
//
//        /**
//         * 初始化视频下载API
//         * @param context 应用上下文
//         * @param config 配置回调
//         */
//        fun initialize(context: Context, config: VideoDownloaderConfig.() -> Unit = {}): VideoDownloaderApi {
//            return INSTANCE ?: synchronized(this) {
//                INSTANCE ?: VideoDownloaderApi().also {
//                    INSTANCE = it
//                    it.init(context, config)
//                }
//            }
//        }
//
//        /**
//         * 获取API实例
//         */
//        fun getInstance(): VideoDownloaderApi {
//            return INSTANCE ?: throw IllegalStateException("VideoDownloaderApi未初始化，请先调用initialize()")
//        }
//    }
//
//    private lateinit var context: Context
//    private lateinit var config: VideoDownloaderConfig
//    private val downloadTasks = mutableStateMapOf<String, DownloadTask>()
//    private val callbacks = mutableListOf<DownloadCallback>()
//    private lateinit var sharedPreferences: SharedPreferences
//    private val gson = Gson()
//
//    // 网络状态监听相关
//    private lateinit var connectivityManager: ConnectivityManager
//    private var isNetworkAvailable = true
//    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
//        override fun onAvailable(network: Network) {
//            super.onAvailable(network)
//            Log.d(TAG, "网络连接可用")
//            isNetworkAvailable = true
//            // 网络恢复时，自动恢复暂停的下载任务
//            resumeFailedDownloadsOnNetworkRestore()
//        }
//
//        override fun onLost(network: Network) {
//            super.onLost(network)
//            Log.w(TAG, "网络连接丢失")
//            isNetworkAvailable = false
//            // 网络中断时，暂停正在下载的任务
//            pauseDownloadsOnNetworkLoss()
//        }
//    }
//
//    private fun init(context: Context, configBlock: VideoDownloaderConfig.() -> Unit) {
//        this.context = context.applicationContext
//        this.config = VideoDownloaderConfig().apply(configBlock)
//        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//
//        // 初始化网络状态监听
//        initNetworkMonitoring()
//
//        // 恢复保存的下载任务
//        restoreDownloadTasks()
//
//        Log.d(TAG, "VideoDownloaderApi初始化完成: downloadPath=${config.downloadPath}, maxConcurrentDownloads=${config.maxConcurrentDownloads}, 恢复任务数量=${downloadTasks.size}")
//    }
//
//
//    /**
//     * 开始下载视频
//     * @param videoInfo 视频信息
//     * @param format 选择的格式
//     * @return 下载任务ID
//     */
//    suspend fun startDownload(videoInfo: VideoInfo): String {
//        val taskId = "task_${System.currentTimeMillis()}"
//        Log.d(TAG, "开始下载任务: taskId=$taskId, url=${videoInfo.url}, title=${videoInfo.title}")
//
//        // 生成安全的文件名，移除特殊字符
//        val safeFileName = videoInfo.title
//            .replace("[^a-zA-Z0-9\u4e00-\u9fa5._-]".toRegex(), "_") // 保留中文、英文、数字、点、下划线、横线
//            .take(100) // 限制文件名长度
//        Log.d(TAG, "生成安全文件名: 原始=${videoInfo.title}, 安全=${safeFileName}")
//
//        // 确保下载目录存在
//        val downloadDir = java.io.File(config.downloadPath)
//        if (!downloadDir.exists()) {
//            val created = downloadDir.mkdirs()
//            Log.d(TAG, "创建下载目录: path=${config.downloadPath}, 创建结果=$created")
//        } else {
//            Log.d(TAG, "下载目录已存在: path=${config.downloadPath}")
//        }
//
//        val filePath = "${config.downloadPath}/${safeFileName}.mp4"
//        Log.d(TAG, "完整文件路径: $filePath")
//
//        val task = DownloadTask(
//            id = taskId,
//            videoInfo = videoInfo,
//            status = DownloadStatus.DOWNLOADING, // 修复：直接设置为下载中状态
//            progress = 0,
//            downloadSpeed = 0L,
//            filePath = filePath
//        )
//
//        downloadTasks[taskId] = task
//        Log.d(TAG, "下载任务已创建并添加到任务列表")
//
//        // 保存到持久化存储
//        saveDownloadTasks()
//
//        // 通知回调
//        callbacks.forEach { it.onDownloadStarted(task) }
//        Log.d(TAG, "已通知 ${callbacks.size} 个回调监听器下载开始")
//
//        // 开始下载过程
//        simulateDownload(task)
//
//        return taskId
//    }
//
//    /**
//     * 暂停下载
//     */
//    suspend fun pauseDownload(taskId: String) {
//        Log.d(TAG, "暂停下载任务: taskId=$taskId")
//        downloadTasks[taskId]?.let { task ->
//            task.status = DownloadStatus.PAUSED
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[taskId] = task
//            Log.d(TAG, "下载任务已暂停: taskId=$taskId")
//            saveDownloadTasks()
//            callbacks.forEach { it.onDownloadPaused(task) }
//        } ?: Log.w(TAG, "未找到要暂停的下载任务: taskId=$taskId")
//    }
//
//    /**
//     * 恢复下载
//     */
//    suspend fun resumeDownload(taskId: String) {
//        Log.d(TAG, "恢复下载任务: taskId=$taskId")
//        downloadTasks[taskId]?.let { task ->
//            task.status = DownloadStatus.DOWNLOADING
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[taskId] = task
//            Log.d(TAG, "下载任务已恢复: taskId=$taskId")
//            saveDownloadTasks()
//            callbacks.forEach { it.onDownloadResumed(task) }
//            simulateDownload(task)
//        } ?: Log.w(TAG, "未找到要恢复的下载任务: taskId=$taskId")
//    }
//
//    /**
//     * 取消下载
//     */
//    suspend fun cancelDownload(taskId: String) {
//        Log.d(TAG, "取消下载任务: taskId=$taskId")
//        downloadTasks[taskId]?.let { task ->
//            task.status = DownloadStatus.CANCELLED
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[taskId] = task
//            Log.d(TAG, "下载任务已取消: taskId=$taskId")
//            callbacks.forEach { it.onDownloadCancelled(task) }
//            downloadTasks.remove(taskId)
//            saveDownloadTasks()
//            Log.d(TAG, "下载任务已从任务列表中移除: taskId=$taskId")
//        } ?: Log.w(TAG, "未找到要取消的下载任务: taskId=$taskId")
//    }
//
//    /**
//     * 获取所有下载任务
//     */
//    fun getAllDownloadTasks(): List<DownloadTask> {
//        return downloadTasks.values.toList()
//    }
//
//    /**
//     * 获取任务状态映射
//     * 返回可观察的状态Map，UI可以直接使用此Map进行状态监听和更新
//     * @return 任务ID到任务对象的可观察状态映射
//     */
//    fun getTaskStateMap(): SnapshotStateMap<String, DownloadTask> {
//        return downloadTasks
//    }
//
//    /**
//     * 获取指定任务
//     */
//    fun getDownloadTask(taskId: String): DownloadTask? {
//        return downloadTasks[taskId]
//    }
//
//    /**
//     * 注册下载回调
//     */
//    fun registerDownloadCallback(callback: DownloadCallback) {
//        callbacks.add(callback)
//        Log.d(TAG, "注册下载回调监听器，当前监听器数量: ${callbacks.size}")
//    }
//
//    /**
//     * 取消注册下载回调
//     */
//    fun unregisterDownloadCallback(callback: DownloadCallback) {
//        callbacks.remove(callback)
//        Log.d(TAG, "取消注册下载回调监听器，当前监听器数量: ${callbacks.size}")
//    }
//
//    /**
//     * 清除所有回调
//     */
//    fun clearAllCallbacks() {
//        val count = callbacks.size
//        callbacks.clear()
//        Log.d(TAG, "清除所有下载回调监听器，已清除 $count 个监听器")
//    }
//
//    /**
//     * 清理已完成的下载任务
//     */
//    fun clearCompletedTasks() {
//        val completedTasks = downloadTasks.values.filter {
//            it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED
//        }
//
//        completedTasks.forEach { task ->
//            downloadTasks.remove(task.id)
//        }
//
//        if (completedTasks.isNotEmpty()) {
//            saveDownloadTasks()
//            Log.d(TAG, "已清理${completedTasks.size}个已完成的任务")
//        }
//    }
//
//    /**
//     * 清理所有下载任务
//     */
//    fun clearAllTasks() {
//        downloadTasks.clear()
//        saveDownloadTasks()
//        Log.d(TAG, "所有下载任务已清除")
//    }
//
//    /**
//     * 检查存储权限
//     */
//    fun hasStoragePermission(): Boolean {
//        // 简化实现，实际应该检查权限
//        return true
//    }
//
//    /**
//     * 检查网络权限
//     */
//    fun hasNetworkPermission(): Boolean {
//        // 简化实现，实际应该检查权限
//        return true
//    }
//
//    /**
//     * 获取可用存储空间
//     */
//    fun getAvailableStorageSpace(): Long {
//        // 简化实现，返回模拟值
//        return 1024 * 1024 * 1024L // 1GB
//    }
//
//    /**
//     * 获取下载目录
//     */
//    fun getDownloadDirectory(): String {
//        return config.downloadPath
//    }
//
//    /**
//     * 初始化网络状态监听
//     */
//    private fun initNetworkMonitoring() {
//        try {
//            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//            // 检查当前网络状态
//            isNetworkAvailable = isNetworkConnected()
//            Log.d(TAG, "当前网络状态: ${if (isNetworkAvailable) "已连接" else "未连接"}")
//
//            // 注册网络状态监听
//            val networkRequest = NetworkRequest.Builder()
//                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
//                .build()
//
//            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
//            Log.d(TAG, "网络状态监听已注册")
//        } catch (e: Exception) {
//            Log.e(TAG, "初始化网络监听失败: ${e.message}", e)
//        }
//    }
//
//    /**
//     * 检查网络连接状态
//     */
//    private fun isNetworkConnected(): Boolean {
//        return try {
//            val activeNetwork = connectivityManager.activeNetwork ?: return false
//            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
//            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//        } catch (e: Exception) {
//            Log.e(TAG, "检查网络状态失败: ${e.message}")
//            false
//        }
//    }
//
//    /**
//     * 网络中断时暂停正在下载的任务
//     */
//    private fun pauseDownloadsOnNetworkLoss() {
//        GlobalScope.launch {
//            try {
//                val downloadingTasks = downloadTasks.values.filter { it.status == DownloadStatus.DOWNLOADING }
//                Log.d(TAG, "网络中断，暂停${downloadingTasks.size}个正在下载的任务")
//
//                downloadingTasks.forEach { task ->
//                    task.status = DownloadStatus.PAUSED
//                    // 重新设置任务到 Map 中以触发状态变化通知
//                    downloadTasks[task.id] = task
//                    Log.d(TAG, "因网络中断暂停任务: ${task.id}")
//                    callbacks.forEach { callback ->
//                        callback.onDownloadPaused(task)
//                        callback.onDownloadFailed(task, "网络连接中断，任务已暂停")
//                    }
//                }
//
//                if (downloadingTasks.isNotEmpty()) {
//                    saveDownloadTasks()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "暂停下载任务失败: ${e.message}", e)
//            }
//        }
//    }
//
//    /**
//     * 网络恢复时自动恢复失败的下载任务
//     */
//    private fun resumeFailedDownloadsOnNetworkRestore() {
//        GlobalScope.launch {
//            try {
//                // 等待一段时间确保网络稳定
//                delay(2000)
//
//                val pausedTasks = downloadTasks.values.filter {
//                    it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED
//                }
//
//                if (pausedTasks.isNotEmpty()) {
//                    Log.d(TAG, "网络恢复，发现${pausedTasks.size}个可恢复的任务")
//
//                    pausedTasks.forEach { task ->
//                        Log.d(TAG, "网络恢复，自动恢复任务: ${task.id}")
//                        resumeDownload(task.id)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "自动恢复下载任务失败: ${e.message}", e)
//            }
//        }
//    }
//
//    /**
//     * 重新下载失败的任务
//     * @param taskId 任务ID
//     */
//    suspend fun retryDownload(taskId: String) {
//        Log.d(TAG, "重新下载任务: taskId=$taskId")
//        downloadTasks[taskId]?.let { task ->
//            if (task.status == DownloadStatus.FAILED || task.status == DownloadStatus.PAUSED) {
//                // 检查网络状态
//                if (!isNetworkAvailable || !isNetworkConnected()) {
//                    Log.w(TAG, "网络不可用，无法重新下载")
//                    callbacks.forEach { it.onDownloadFailed(task, "网络不可用，请检查网络连接") }
//                    return
//                }
//
//                // 重置任务状态和进度
//                task.status = DownloadStatus.DOWNLOADING
//                task.progress = 0
//                task.downloadSpeed = 0L
//
//                // 重新设置任务到 Map 中以触发状态变化通知
//                downloadTasks[taskId] = task
//
//                Log.d(TAG, "重新开始下载任务: taskId=$taskId")
//                saveDownloadTasks()
//                callbacks.forEach { it.onDownloadResumed(task) }
//
//                // 开始下载
//                simulateDownload(task)
//            } else {
//                Log.w(TAG, "任务状态不允许重新下载: taskId=$taskId, status=${task.status}")
//            }
//        } ?: Log.w(TAG, "未找到要重新下载的任务: taskId=$taskId")
//    }
//
//    /**
//     * 检查网络状态并返回是否可以下载
//     */
//    fun canDownload(): Boolean {
//        return isNetworkAvailable && isNetworkConnected()
//    }
//
//    /**
//     * 获取网络状态描述
//     */
//    fun getNetworkStatus(): String {
//        return if (isNetworkAvailable && isNetworkConnected()) {
//            "网络连接正常"
//        } else {
//            "网络连接不可用"
//        }
//    }
//
//    /**
//     * 保存下载任务到SharedPreferences
//     */
//    private fun saveDownloadTasks() {
//        try {
//            val tasksJson = gson.toJson(downloadTasks.values.toList())
//            sharedPreferences.edit()
//                .putString(KEY_DOWNLOAD_TASKS, tasksJson)
//                .apply()
//            Log.d(TAG, "保存下载任务成功，任务数量: ${downloadTasks.size}")
//        } catch (e: Exception) {
//            Log.e(TAG, "保存下载任务失败", e)
//        }
//    }
//
//    /**
//     * 从SharedPreferences恢复下载任务
//     */
//    private fun restoreDownloadTasks() {
//        try {
//            val tasksJson = sharedPreferences.getString(KEY_DOWNLOAD_TASKS, null)
//            if (!tasksJson.isNullOrEmpty()) {
//                val type = object : TypeToken<List<DownloadTask>>() {}.type
//                val tasks: List<DownloadTask> = gson.fromJson(tasksJson, type)
//
//                downloadTasks.clear()
//                tasks.forEach { task ->
//                    // 重置正在下载的任务状态为暂停，避免状态不一致
//                    val restoredTask = if (task.status == DownloadStatus.DOWNLOADING) {
//                        task.copy(status = DownloadStatus.PAUSED)
//                    } else {
//                        task
//                    }
//                    downloadTasks[restoredTask.id] = restoredTask
//                }
//
//                Log.d(TAG, "恢复下载任务成功，任务数量: ${downloadTasks.size}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "恢复下载任务失败", e)
//            downloadTasks.clear()
//        }
//    }
//
//    /**
//     * 删除指定任务的持久化数据
//     */
//    private fun removeTaskFromStorage(taskId: String) {
//        downloadTasks.remove(taskId)
//        saveDownloadTasks()
//    }
//
//    /**
//     * 模拟下载过程
//     */
//    private suspend fun simulateDownload(task: DownloadTask) {
//        GlobalScope.launch {
//            try {
//                Log.d(TAG, "开始执行下载: taskId=${task.id}, url=${task.videoInfo.url}")
//
//                // 检查网络状态
//                if (!canDownload()) {
//                    Log.w(TAG, "网络不可用，下载失败: ${task.id}")
//                    task.status = DownloadStatus.FAILED
//                    // 重新设置任务到 Map 中以触发状态变化通知
//                    downloadTasks[task.id] = task
//                    saveDownloadTasks()
//                    callbacks.forEach { it.onDownloadFailed(task, "网络连接不可用，请检查网络设置") }
//                    return@launch
//                }
//
//                // 检查是否是M3U8链接
//                if (task.videoInfo.url.contains(".m3u8")) {
//                    Log.d(TAG, "检测到M3U8链接，使用M3U8下载方法")
//                    downloadM3U8(task)
//                } else {
//                    Log.d(TAG, "检测到普通视频链接，使用常规下载方法")
//                    downloadRegularVideo(task)
//                }
//            } catch (e: SocketTimeoutException) {
//                Log.e(TAG, "网络超时: ${e.message}", e)
//                handleNetworkError(task, "网络连接超时，请检查网络状态")
//            } catch (e: UnknownHostException) {
//                Log.e(TAG, "网络连接失败: ${e.message}", e)
//                handleNetworkError(task, "无法连接到服务器，请检查网络连接")
//            } catch (e: IOException) {
//                Log.e(TAG, "网络IO异常: ${e.message}", e)
//                handleNetworkError(task, "网络连接异常: ${e.message}")
//            } catch (e: Exception) {
//                Log.e(TAG, "下载过程中发生异常: taskId=${task.id}, error=${e.message}", e)
//                task.status = DownloadStatus.FAILED
//                // 重新设置任务到 Map 中以触发状态变化通知
//                downloadTasks[task.id] = task
//                saveDownloadTasks()
//                callbacks.forEach { it.onDownloadFailed(task, e.message ?: "下载失败") }
//            }
//        }
//    }
//
//    /**
//     * 处理网络错误
//     */
//    private suspend fun handleNetworkError(task: DownloadTask, errorMessage: String) {
//        Log.w(TAG, "网络错误处理: ${task.id} - $errorMessage")
//
//        // 检查是否还有重试次数
//        val currentRetryCount = task.retryCount
//        if (currentRetryCount < config.maxRetryCount) {
//            val newRetryCount = currentRetryCount + 1
//            task.retryCount = newRetryCount
//            task.status = DownloadStatus.PAUSED
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[task.id] = task
//            Log.d(TAG, "网络错误，任务暂停等待重试: ${task.id}, 重试次数: $newRetryCount/${config.maxRetryCount}")
//
//            saveDownloadTasks()
//            callbacks.forEach {
//                it.onDownloadPaused(task)
//                it.onDownloadFailed(task, "$errorMessage (将在网络恢复后自动重试 $newRetryCount/${config.maxRetryCount})")
//            }
//
//            // 延迟后重试
//            delay(5000) // 等待5秒
//            if (canDownload()) {
//                Log.d(TAG, "网络恢复，重试下载: ${task.id}")
//                task.status = DownloadStatus.DOWNLOADING
//                // 重新设置任务到 Map 中以触发状态变化通知
//                downloadTasks[task.id] = task
//                saveDownloadTasks()
//                callbacks.forEach { it.onDownloadResumed(task) }
//                simulateDownload(task)
//            }
//        } else {
//            // 重试次数用完，标记为失败
//            task.status = DownloadStatus.FAILED
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[task.id] = task
//            Log.e(TAG, "重试次数用完，下载失败: ${task.id}")
//            saveDownloadTasks()
//            callbacks.forEach { it.onDownloadFailed(task, "$errorMessage (已达到最大重试次数)") }
//        }
//    }
//
//    /**
//     * 下载M3U8视频流
//     * 解析M3U8播放列表，下载所有TS分片，然后合并为完整视频
//     */
//    private suspend fun downloadM3U8(task: DownloadTask) {
//        try {
//            val url = task.videoInfo.url
//            val outputFile = java.io.File(task.filePath)
//            Log.d(TAG, "M3U8下载开始: url=$url, 输出文件=${outputFile.absolutePath}")
//
//            // 确保输出目录存在
//            val parentDir = outputFile.parentFile
//            if (parentDir != null && !parentDir.exists()) {
//                val created = parentDir.mkdirs()
//                Log.d(TAG, "创建输出目录: path=${parentDir.absolutePath}, 创建结果=$created")
//            }
//
//            // 第一步：下载并解析M3U8播放列表
//            Log.d(TAG, "步骤1: 下载M3U8播放列表文件")
//            val m3u8Content = downloadM3U8Playlist(url)
//            Log.d(TAG, "M3U8播放列表内容长度: ${m3u8Content.length}字符")
//
//            // 第二步：解析TS分片URL列表
//            val tsUrls = parseM3U8Content(m3u8Content, url)
//            Log.d(TAG, "步骤2: 解析到${tsUrls.size}个TS分片")
//
//            if (tsUrls.isEmpty()) {
//                throw Exception("M3U8播放列表中未找到TS分片")
//            }
//
//            // 第三步：创建临时目录存储TS分片
//            val tempDir = java.io.File(parentDir, "temp_${task.id}")
//            if (!tempDir.exists()) {
//                tempDir.mkdirs()
//                Log.d(TAG, "创建临时目录: ${tempDir.absolutePath}")
//            }
//
//            // 第四步：下载所有TS分片
//            Log.d(TAG, "步骤3: 开始下载${tsUrls.size}个TS分片")
//            val tsFiles = mutableListOf<java.io.File>()
//            val startTime = System.currentTimeMillis()
//
//            tsUrls.forEachIndexed { index, tsUrl ->
//                if (task.status != DownloadStatus.DOWNLOADING) {
//                    Log.w(TAG, "下载被中断，停止TS分片下载")
//                    return@forEachIndexed
//                }
//
//                val tsFile = java.io.File(tempDir, "segment_${index.toString().padStart(4, '0')}.ts")
//                Log.d(TAG, "下载TS分片 ${index + 1}/${tsUrls.size}: $tsUrl")
//
//                try {
//                    downloadTSSegment(tsUrl, tsFile)
//                    tsFiles.add(tsFile)
//
//                    // 更新进度（下载阶段占80%）
//                    val downloadProgress = ((index + 1) * 80) / tsUrls.size
//                    task.progress = downloadProgress
//
//                    // 修复速度计算：使用浮点数除法并转换为合适单位
//                    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
//                    val avgSpeed = if (elapsedSeconds > 0) {
//                        // 计算分片/秒，保留两位小数后转换为Long（乘以100存储）
//                        ((index + 1) / elapsedSeconds * 100).toLong()
//                    } else {
//                        0L
//                    }
//                    task.downloadSpeed = avgSpeed
//
//                    // 每10%进度保存一次，避免频繁写入
//                    if (downloadProgress % 10 == 0) {
//                        saveDownloadTasks()
//                    }
//
//                    // 显示时除以100恢复真实速度值
//                    val displaySpeed = avgSpeed / 100.0
//                    Log.v(TAG, "TS分片下载完成: segment_${index.toString().padStart(4, '0')}.ts, 大小: ${tsFile.length()}字节")
//                    Log.d(TAG, "TS分片下载进度: ${index + 1}/${tsUrls.size} (${downloadProgress}%), 平均速度: ${String.format("%.2f", displaySpeed)}分片/秒")
//                    callbacks.forEach { it.onDownloadProgress(task, downloadProgress, avgSpeed) }
//
//                } catch (e: Exception) {
//                    Log.e(TAG, "下载TS分片失败: index=$index, url=$tsUrl, error=${e.message}")
//                    throw Exception("TS分片下载失败: ${e.message}")
//                }
//            }
//
//            // 第五步：使用FFmpeg合并TS分片
//            if (task.status == DownloadStatus.DOWNLOADING) {
//                Log.d(TAG, "步骤4: 开始合并${tsFiles.size}个TS分片为MP4文件")
//                task.progress = 85
//                callbacks.forEach { it.onDownloadProgress(task, 85, 0) }
//
//                val success = mergeTSFilesWithFFmpeg(tsFiles, outputFile)
//                if (success) {
//                    // 步骤4.5: 将文件后缀从.m3u8改为.mp4
//                    val finalOutputFile = if (outputFile.name.endsWith(".m3u8")) {
//                        val mp4FileName = outputFile.name.replace(".m3u8", ".mp4")
//                        val mp4File = java.io.File(outputFile.parent, mp4FileName)
//
//                        if (outputFile.renameTo(mp4File)) {
//                            Log.d(TAG, "文件重命名成功: ${outputFile.name} -> ${mp4File.name}")
//                            // 更新任务的文件路径
//                            task.filePath = mp4File.absolutePath
//                            mp4File
//                        } else {
//                            Log.w(TAG, "文件重命名失败，保持原文件名: ${outputFile.name}")
//                            outputFile
//                        }
//                    } else {
//                        outputFile
//                    }
//
//                    task.progress = 100
//                    task.status = DownloadStatus.COMPLETED
//                    task.retryCount = 0 // 重置重试计数
//                    // 重新设置任务到 Map 中以触发状态变化通知
//                    downloadTasks[task.id] = task
//                    saveDownloadTasks()
//                    Log.d(TAG, "M3U8下载成功完成: 文件=${finalOutputFile.absolutePath}, 大小=${finalOutputFile.length()}字节")
//                    callbacks.forEach { it.onDownloadCompleted(task) }
//                } else {
//                    throw Exception("FFmpeg合并TS分片失败")
//                }
//            }
//
//            // 清理临时文件
//            Log.d(TAG, "步骤5: 清理临时文件")
//            cleanupTempFiles(tempDir)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "M3U8下载失败: taskId=${task.id}, error=${e.message}", e)
//            task.status = DownloadStatus.FAILED
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[task.id] = task
//            saveDownloadTasks()
//            callbacks.forEach { it.onDownloadFailed(task, "M3U8下载失败: ${e.message}") }
//        }
//    }
//
//    /**
//     * 下载M3U8播放列表文件内容
//     */
//    private suspend fun downloadM3U8Playlist(url: String): String {
//        Log.d(TAG, "下载M3U8播放列表: $url")
//        val connection = java.net.URL(url).openConnection()
//        connection.connectTimeout = config.networkTimeoutMs.toInt()
//        connection.readTimeout = config.networkTimeoutMs.toInt()
//
//        return connection.getInputStream().bufferedReader().use { it.readText() }
//    }
//
//    /**
//     * 解析M3U8内容，提取TS分片URL列表
//     */
//    private fun parseM3U8Content(content: String, baseUrl: String): List<String> {
//        Log.d(TAG, "解析M3U8内容，基础URL: $baseUrl")
//        val lines = content.split("\n")
//        val tsUrls = mutableListOf<String>()
//        val baseUri = java.net.URI(baseUrl)
//
//        lines.forEach { line ->
//            val trimmedLine = line.trim()
//            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
//                // 这是一个TS分片URL
//                val tsUrl = if (trimmedLine.startsWith("http")) {
//                    trimmedLine
//                } else {
//                    // 相对URL，需要与基础URL合并
//                    baseUri.resolve(trimmedLine).toString()
//                }
//                tsUrls.add(tsUrl)
//                Log.v(TAG, "解析到TS分片: $tsUrl")
//            }
//        }
//
//        Log.d(TAG, "M3U8解析完成，共${tsUrls.size}个TS分片")
//        return tsUrls
//    }
//
//    /**
//     * 下载单个TS分片
//     */
//    private suspend fun downloadTSSegment(url: String, outputFile: java.io.File) {
//        val connection = java.net.URL(url).openConnection()
//        connection.connectTimeout = config.networkTimeoutMs.toInt()
//        connection.readTimeout = config.networkTimeoutMs.toInt()
//
//        connection.getInputStream().use { input ->
//            outputFile.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//
//        Log.v(TAG, "TS分片下载完成: ${outputFile.name}, 大小: ${outputFile.length()}字节")
//    }
//
//    /**
//     * 合并TS分片为MP4文件
//     * 生产级实现：优先使用简单文件合并，避免FFmpeg依赖问题
//     */
//    private suspend fun mergeTSFilesWithFFmpeg(tsFiles: List<java.io.File>, outputFile: java.io.File): Boolean {
//        return try {
//            Log.d(TAG, "开始TS分片合并，输入${tsFiles.size}个TS文件，输出: ${outputFile.absolutePath}")
//
//            // 生产级方案：直接合并TS文件（TS格式支持简单拼接）
//            // TS（Transport Stream）格式天然支持文件级别的拼接合并
//            val success = mergeTSFilesDirectly(tsFiles, outputFile)
//
//            if (success) {
//                Log.d(TAG, "TS分片合并成功，输出文件大小: ${outputFile.length()}字节")
//                return true
//            }
//
//            // 备用方案：如果直接合并失败，尝试使用FFmpeg（需要额外集成）
//            Log.w(TAG, "直接合并失败，尝试FFmpeg方案（需要FFmpeg库支持）")
//            return tryFFmpegMerge(tsFiles, outputFile)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "TS分片合并异常: ${e.message}", e)
//            false
//        }
//    }
//
//    /**
//     * 直接合并TS文件（生产级主要方案）
//     * TS格式支持简单的二进制拼接，无需复杂的编解码处理
//     */
//    private suspend fun mergeTSFilesDirectly(tsFiles: List<java.io.File>, outputFile: java.io.File): Boolean {
//        return try {
//            Log.d(TAG, "使用直接拼接方式合并TS文件")
//
//            // 确保输出文件的父目录存在
//            outputFile.parentFile?.let { parentDir ->
//                if (!parentDir.exists()) {
//                    parentDir.mkdirs()
//                    Log.d(TAG, "创建输出目录: ${parentDir.absolutePath}")
//                }
//            }
//
//            // 按顺序合并所有TS文件
//            outputFile.outputStream().use { outputStream ->
//                tsFiles.sortedBy { file ->
//                    // 从文件名中提取序号进行排序
//                    val fileName = file.nameWithoutExtension
//                    val numberPart = fileName.substringAfterLast("_")
//                    numberPart.toIntOrNull() ?: 0
//                }.forEach { tsFile ->
//                    if (tsFile.exists() && tsFile.length() > 0) {
//                        Log.v(TAG, "合并TS文件: ${tsFile.name}, 大小: ${tsFile.length()}字节")
//                        tsFile.inputStream().use { inputStream ->
//                            inputStream.copyTo(outputStream)
//                        }
//                    } else {
//                        Log.w(TAG, "跳过无效TS文件: ${tsFile.name}")
//                    }
//                }
//            }
//
//            val success = outputFile.exists() && outputFile.length() > 0
//            Log.d(TAG, "直接合并结果: success=$success, 文件大小: ${outputFile.length()}字节")
//            success
//
//        } catch (e: Exception) {
//            Log.e(TAG, "直接合并TS文件失败: ${e.message}", e)
//            false
//        }
//    }
//
//    /**
//     * FFmpeg合并方案（备用方案，需要集成FFmpeg库）
//     * 注意：此方案需要在build.gradle中添加FFmpeg Android库依赖
//     */
//    private suspend fun tryFFmpegMerge(tsFiles: List<java.io.File>, outputFile: java.io.File): Boolean {
//        return try {
//            Log.w(TAG, "FFmpeg方案暂未集成，建议添加FFmpeg Android库依赖")
//            Log.w(TAG, "推荐使用: implementation 'com.arthenica:mobile-ffmpeg-full:4.4.LTS'")
//            Log.w(TAG, "或者: implementation 'com.arthenica:ffmpeg-kit-android:5.1'")
//
//            // TODO: 集成FFmpeg Android库后的实现
//            // 示例代码（需要添加相应依赖）：
//            // val command = "-f concat -safe 0 -i ${listFile.absolutePath} -c copy -y ${outputFile.absolutePath}"
//            // val rc = FFmpeg.execute(command)
//            // return rc == RETURN_CODE_SUCCESS
//
//            false
//        } catch (e: Exception) {
//            Log.e(TAG, "FFmpeg备用方案执行失败: ${e.message}", e)
//            false
//        }
//    }
//
//    /**
//     * 清理临时文件和目录
//     */
//    private fun cleanupTempFiles(tempDir: java.io.File) {
//        try {
//            if (tempDir.exists()) {
//                val deletedFiles = tempDir.listFiles()?.size ?: 0
//                tempDir.deleteRecursively()
//                Log.d(TAG, "清理临时目录完成: ${tempDir.absolutePath}, 删除${deletedFiles}个文件")
//            }
//        } catch (e: Exception) {
//            Log.w(TAG, "清理临时文件失败: ${e.message}")
//        }
//    }
//
//    /**
//     * 下载普通视频文件
//     * 实际下载视频文件并保存到本地存储
//     */
//    private suspend fun downloadRegularVideo(task: DownloadTask) {
//        try {
//            val url = task.videoInfo.url
//            val outputFile = java.io.File(task.filePath)
//            Log.d(TAG, "普通视频下载开始: url=$url, 输出文件=${outputFile.absolutePath}")
//
//            // 确保输出目录存在
//            val parentDir = outputFile.parentFile
//            if (parentDir != null && !parentDir.exists()) {
//                val created = parentDir.mkdirs()
//                Log.d(TAG, "创建输出目录: path=${parentDir.absolutePath}, 创建结果=$created")
//            }
//
//            // 创建网络连接
//            Log.d(TAG, "建立网络连接: url=$url, 超时=${config.networkTimeoutMs}ms")
//            val connection = java.net.URL(url).openConnection()
//            connection.connectTimeout = config.networkTimeoutMs.toInt()
//            connection.readTimeout = config.networkTimeoutMs.toInt()
//
//            // 设置User-Agent避免某些服务器拒绝请求
//            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0")
//            Log.d(TAG, "设置User-Agent请求头")
//
//            val inputStream = connection.getInputStream()
//            val outputStream = outputFile.outputStream()
//            Log.d(TAG, "网络连接建立成功，开始读取数据流")
//
//            val buffer = ByteArray(8192)
//            var totalBytesRead = 0L
//            val contentLength = connection.contentLength.toLong()
//            var bytesRead: Int
//            Log.d(TAG, "内容长度: $contentLength 字节")
//
//            val startTime = System.currentTimeMillis()
//            var lastSaveTime = startTime
//            var lastProgressPercent = -1 // 记录上次保存的进度百分比
//
//            while (inputStream.read(buffer).also { bytesRead = it } != -1 && task.status == DownloadStatus.DOWNLOADING) {
//                outputStream.write(buffer, 0, bytesRead)
//                totalBytesRead += bytesRead
//
//                // 计算下载进度 - 使用浮点数计算避免精度丢失
//                val progress = if (contentLength > 0) {
//                    // 使用浮点数计算，然后转换为整数，确保小数部分不被丢失
//                    ((totalBytesRead.toDouble() * 100.0) / contentLength.toDouble()).toInt()
//                } else {
//                    // 如果无法获取文件大小，基于已下载字节数估算
//                    minOf((totalBytesRead / (1024 * 1024)).toInt(), 95) // 每MB 1%，最多95%
//                }
//
//                // 计算下载速度 (字节/秒) - 避免除零错误
//                val elapsedMillis = System.currentTimeMillis() - startTime
//                val downloadSpeed = if (elapsedMillis > 1000) { // 至少1秒后才计算速度
//                    (totalBytesRead * 1000) / elapsedMillis // 使用毫秒计算更精确
//                } else {
//                    0L // 开始阶段速度为0
//                }
//
//                // 更新任务进度和速度
//                task.progress = progress
//                task.downloadSpeed = downloadSpeed
//
//                // 重新设置任务到 Map 中以触发状态变化通知
//                downloadTasks[task.id] = task
//
//                // 优化保存策略：每5%进度或每5秒保存一次，避免频繁写入
//                val currentTime = System.currentTimeMillis()
//                val shouldSave = (progress != lastProgressPercent && progress % 5 == 0) ||
//                                (currentTime - lastSaveTime > 5000)
//
//                if (shouldSave) {
//                    saveDownloadTasks()
//                    lastSaveTime = currentTime
//                    lastProgressPercent = progress
//                    Log.d(TAG, "保存下载任务: 进度=${progress}%, 任务数量=${downloadTasks.size}")
//                }
//
//                // 详细的进度日志 - 每1%或每5秒记录一次
//                val elapsedSeconds = elapsedMillis / 1000
//                if (progress != lastProgressPercent || elapsedSeconds % 5 == 0L) {
//                    Log.d(TAG, "普通视频下载进度: ${progress}%, 已下载: ${totalBytesRead}字节/${contentLength}字节, 速度: ${downloadSpeed}字节/秒, 用时: ${elapsedSeconds}秒")
//                }
//
//                callbacks.forEach { it.onDownloadProgress(task, progress, downloadSpeed) }
//
//                // 减少延迟，提高响应性
//                delay(100)
//            }
//
//            inputStream.close()
//            outputStream.close()
//            Log.d(TAG, "普通视频下载数据流处理完成，总下载: ${totalBytesRead}字节")
//
//            if (task.status == DownloadStatus.DOWNLOADING) {
//                task.progress = 100
//                task.status = DownloadStatus.COMPLETED
//                task.retryCount = 0 // 重置重试计数
//                // 重新设置任务到 Map 中以触发状态变化通知
//                downloadTasks[task.id] = task
//                saveDownloadTasks()
//                Log.d(TAG, "普通视频下载成功完成: 文件=${outputFile.absolutePath}, 大小=${outputFile.length()}字节")
//                callbacks.forEach { it.onDownloadCompleted(task) }
//            } else {
//                Log.w(TAG, "普通视频下载被中断: 当前状态=${task.status}")
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "普通视频下载失败: taskId=${task.id}, error=${e.message}", e)
//            task.status = DownloadStatus.FAILED
//            // 重新设置任务到 Map 中以触发状态变化通知
//            downloadTasks[task.id] = task
//            saveDownloadTasks()
//            callbacks.forEach { it.onDownloadFailed(task, "视频下载失败: ${e.message}") }
//        }
//    }
//}
//
///**
// * 视频下载配置
// */
//data class VideoDownloaderConfig(
//    var downloadPath: String = "/storage/emulated/0/Download/VideoBox", // 使用公共下载目录
//    var maxConcurrentDownloads: Int = 3,
//    var networkTimeoutMs: Long = 30000,
//    var maxRetryCount: Int = 3,
//    var enableResumeDownload: Boolean = true,
//    var enableDownloadNotification: Boolean = true
//)
//
//
//
///**
// * 视频格式
// */
//data class VideoFormat(
//    val ext: String, // 文件扩展名
//    val quality: String, // 质量标识
//    val width: Int,
//    val height: Int
//)
//
///**
// * 下载任务
// */
//data class DownloadTask(
//    val id: String,
//    val videoInfo: VideoInfo,
//    var status: DownloadStatus,
//    var progress: Int,
//    var downloadSpeed: Long,
//    var filePath: String, // 改为var以支持文件重命名
//    val createdAt: Long = System.currentTimeMillis(),
//    var retryCount: Int = 0 // 添加重试计数
//)
//
///**
// * 下载状态
// */
//enum class DownloadStatus {
//    PENDING,    // 等待中
//    DOWNLOADING, // 下载中
//    PAUSED,     // 已暂停
//    COMPLETED,  // 已完成
//    FAILED,     // 失败
//    CANCELLED   // 已取消
//}
//
///**
// * 下载回调接口
// */
//interface DownloadCallback {
//    fun onDownloadStarted(task: DownloadTask) {}
//    fun onDownloadProgress(task: DownloadTask, progress: Int, speed: Long) {}
//    fun onDownloadPaused(task: DownloadTask) {}
//    fun onDownloadResumed(task: DownloadTask) {}
//    fun onDownloadCompleted(task: DownloadTask) {}
//    fun onDownloadFailed(task: DownloadTask, error: String) {}
//    fun onDownloadCancelled(task: DownloadTask) {}
//}
//
///**
// * 视频获取异常
// */
//class VideoFetchException(message: String) : Exception(message)
//
///**
// * 网络异常
// */
//class NetworkException(message: String) : Exception(message)
//
///**
// * 存储异常
// */
//class StorageException(message: String) : Exception(message)
//
///**
// * 权限异常
// */
//class PermissionException(message: String) : Exception(message)

/**
 * 视频信息
 */
@Serializable
data class VideoInfo(
    val id: String,
    val title: String,
    val duration: Int, // 秒
    val thumbnail: String,
    val url: String,
    val ext: String,
)