# 媒体处理器模块使用示例

这是一个功能强大的Android媒体处理模块，提供统一的媒体下载、转换和处理功能，支持多种媒体平台和格式。

## 快速开始

### 1. 初始化模块

```kotlin
import com.nexus.core.media.processor.MediaProcessorModule
import com.nexus.core.media.processor.config.MediaProcessorConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 使用默认配置初始化
        MediaProcessorModule.initialize(this)
        
        // 或使用预设配置
        MediaProcessorModule.initialize(this, MediaProcessorConfig.highPerformance())
        
        // 或使用构建器模式自定义配置
        MediaProcessorModule.initializeWithBuilder(this) {
            maxConcurrentTasks(5)
            taskTimeout(60000)
            maxRetryCount(3)
            enableHardwareAcceleration(true)
            maxMemoryUsage(512)
            enableDebugMode(BuildConfig.DEBUG)
        }
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查模块是否已初始化
        if (!MediaProcessorModule.isInitialized()) {
            throw IllegalStateException("MediaProcessorModule 未初始化")
        }
        
        // 设置监听器
        setupListeners()
        
        // 检查权限
        checkPermissions()
    }
}
```

### 2. 设置监听器

```kotlin
import com.nexus.core.media.processor.model.ProcessingTask
import com.nexus.core.media.processor.model.ProcessingStatus
import com.nexus.core.media.processor.model.MediaProcessingState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private fun setupListeners() {
        // 添加进度监听器
        MediaProcessorModule.addProgressListener("main_activity") { task ->
            runOnUiThread {
                updateProgressUI(task)
                println("处理进度: ${task.mediaInfo.title} - ${task.progress}% (${formatSpeed(task.speed)})")
            }
        }
        
        // 添加状态监听器
        MediaProcessorModule.addStateListener("main_activity") { state ->
            runOnUiThread {
                updateStateUI(state)
                println("活跃任务数: ${state.activeTasks}, 总任务数: ${state.totalTasks}")
            }
        }
        
        // 使用Flow监听任务更新
        lifecycleScope.launch {
            MediaProcessorModule.getTaskUpdates().collect { task ->
                when (task.status) {
                    ProcessingStatus.PENDING -> {
                        println("任务等待中: ${task.mediaInfo.title}")
                    }
                    ProcessingStatus.PROCESSING -> {
                        println("任务处理中: ${task.mediaInfo.title}")
                    }
                    ProcessingStatus.COMPLETED -> {
                        println("任务完成: ${task.mediaInfo.title}")
                        showCompletionNotification(task)
                    }
                    ProcessingStatus.FAILED -> {
                        println("任务失败: ${task.mediaInfo.title} - ${task.error}")
                        showErrorDialog(task)
                    }
                    ProcessingStatus.PAUSED -> {
                        println("任务暂停: ${task.mediaInfo.title}")
                    }
                    ProcessingStatus.CANCELLED -> {
                        println("任务取消: ${task.mediaInfo.title}")
                    }
                }
            }
        }
        
        // 使用Flow监听全局状态
        lifecycleScope.launch {
            MediaProcessorModule.getStateFlow().collect { state ->
                updateGlobalStateUI(state)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 移除监听器，避免内存泄漏
        MediaProcessorModule.removeProgressListener("main_activity")
        MediaProcessorModule.removeStateListener("main_activity")
    }
        
    
    private fun checkPermissions() {
        if (!MediaProcessorModule.hasAllPermissions()) {
            val missingPermissions = MediaProcessorModule.getMissingPermissions()
            println("缺失权限: ${missingPermissions.joinToString(", ")}")
            // 请求权限
            requestPermissions(missingPermissions.toTypedArray(), 1001)
        }
    }
    
    private fun updateProgressUI(task: ProcessingTask) {
        // 更新进度条
        // progressBar.progress = task.progress
        // speedText.text = formatSpeed(task.speed)
        // etaText.text = formatDuration(task.estimatedTimeRemaining)
    }
    
    private fun updateStateUI(state: MediaProcessingState) {
        // 更新状态UI
        // activeTasksText.text = "活跃任务: ${state.activeTasks}"
        // totalTasksText.text = "总任务: ${state.totalTasks}"
    }
    
    private fun updateGlobalStateUI(state: MediaProcessingState) {
        // 更新全局状态UI
        // statusText.text = when {
        //     state.activeTasks > 0 -> "处理中..."
        //     state.totalTasks > 0 -> "已完成"
        //     else -> "空闲"
        // }
    }
    
    private fun showCompletionNotification(task: ProcessingTask) {
        // 显示完成通知
        Toast.makeText(this, "处理完成: ${task.mediaInfo.title}", Toast.LENGTH_SHORT).show()
    }
    
    private fun showErrorDialog(task: ProcessingTask) {
        // 显示错误对话框
        AlertDialog.Builder(this)
            .setTitle("处理失败")
            .setMessage("${task.mediaInfo.title}\n错误: ${task.error}")
            .setPositiveButton("重试") { _, _ ->
                lifecycleScope.launch {
                    MediaProcessorModule.resumeTask(task.id)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun formatSpeed(bytesPerSecond: Long): String {
        return MediaProcessorModule.formatFileSize(bytesPerSecond) + "/s"
    }
}
```

### 3. 获取媒体信息并开始处理

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nexus.core.media.processor.model.ProcessingOptions
import com.nexus.core.media.processor.model.MediaType

class MediaProcessingHelper {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun processMedia(url: String) {
        scope.launch {
            try {
                // 显示加载状态
                showLoading("正在获取媒体信息...")
                
                // 获取媒体信息
                val result = MediaProcessorModule.getMediaInfo(url)
                
                hideLoading()
                
                if (result.isSuccess) {
                    val mediaInfo = result.getOrNull()!!
                    
                    // 显示媒体信息
                    println("媒体标题: ${mediaInfo.title}")
                    println("媒体时长: ${MediaProcessorModule.formatDuration(mediaInfo.duration)}")
                    println("上传者: ${mediaInfo.uploader}")
                    println("媒体类型: ${mediaInfo.mediaType}")
                    
                    // 获取可用格式
                    val availableFormats = mediaInfo.formats
                    println("可用格式数量: ${availableFormats.size}")
                    
                    // 开始处理任务
                    val taskResult = MediaProcessorModule.addTask(
                        url = url,
                        options = ProcessingOptions(
                            outputFormat = "mp4",
                            quality = "best",
                            downloadSubtitles = true,
                            downloadThumbnail = true,
                            customFileName = null // 使用默认文件名
                        )
                    )
                    
                    if (taskResult.isSuccess) {
                        val taskId = taskResult.getOrNull()!!
                        println("处理任务已创建: $taskId")
                        showSuccess("开始处理: ${mediaInfo.title}")
                    } else {
                        val error = taskResult.exceptionOrNull()
                        showError("创建任务失败: ${error?.message}")
                    }
                    
                } else {
                    val error = result.exceptionOrNull()
                    showError("获取媒体信息失败: ${error?.message}")
                }
                
            } catch (e: Exception) {
                hideLoading()
                showError("处理失败: ${e.message}")
            }
        }
    }
    
    fun processWithCustomOptions(url: String, mediaType: MediaType, quality: String) {
        scope.launch {
            try {
                // 获取媒体信息
                val result = MediaProcessorModule.getMediaInfo(url)
                
                if (result.isSuccess) {
                    val mediaInfo = result.getOrNull()!!
                    
                    // 根据媒体类型和质量要求配置选项
                    val options = ProcessingOptions(
                        outputFormat = when (mediaType) {
                            MediaType.VIDEO -> "mp4"
                            MediaType.AUDIO -> "mp3"
                            MediaType.PLAYLIST -> "mp4"
                            else -> "mp4"
                        },
                        quality = quality,
                        downloadSubtitles = mediaType == MediaType.VIDEO,
                        downloadThumbnail = true,
                        customFileName = "custom_${System.currentTimeMillis()}"
                    )
                    
                    val taskResult = MediaProcessorModule.addTask(url, options)
                    
                    if (taskResult.isSuccess) {
                        val taskId = taskResult.getOrNull()!!
                        println("自定义处理任务已创建: $taskId")
                        showSuccess("开始处理: ${mediaInfo.title}")
                    } else {
                        val error = taskResult.exceptionOrNull()
                        showError("创建任务失败: ${error?.message}")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    showError("获取媒体信息失败: ${error?.message}")
                }
                
            } catch (e: Exception) {
                showError("处理失败: ${e.message}")
            }
        }
    }
    
    private fun showLoading(message: String) {
        // 显示加载对话框
    }
    
    private fun hideLoading() {
        // 隐藏加载对话框
    }
    
    private fun showError(message: String) {
        // 显示错误信息
    }
    
    private fun showSuccess(message: String) {
        // 显示成功信息
    }
}
```

### 4. 管理处理任务

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // 获取所有任务
    suspend fun getAllTasks(): List<ProcessingTask> {
        val result = MediaProcessorModule.getAllTasks()
        return result.getOrElse { emptyList() }
    }
    
    // 获取活跃任务
    suspend fun getActiveTasks(): List<ProcessingTask> {
        val result = MediaProcessorModule.getActiveTasks()
        return result.getOrElse { emptyList() }
    }
    
    // 获取已完成任务
    suspend fun getCompletedTasks(): List<ProcessingTask> {
        val result = MediaProcessorModule.getCompletedTasks()
        return result.getOrElse { emptyList() }
    }
    
    // 暂停任务
    fun pauseTask(taskId: String) {
        scope.launch {
            val result = MediaProcessorModule.pauseTask(taskId)
            if (result.isSuccess) {
                println("任务已暂停: $taskId")
            } else {
                println("暂停任务失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    // 恢复任务
    fun resumeTask(taskId: String) {
        scope.launch {
            val result = MediaProcessorModule.resumeTask(taskId)
            if (result.isSuccess) {
                println("任务已恢复: $taskId")
            } else {
                println("恢复任务失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    // 取消任务
    fun cancelTask(taskId: String) {
        scope.launch {
            val result = MediaProcessorModule.cancelTask(taskId)
            if (result.isSuccess) {
                println("任务已取消: $taskId")
            } else {
                println("取消任务失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    // 删除任务
    fun deleteTask(taskId: String) {
        scope.launch {
            val result = MediaProcessorModule.deleteTask(taskId)
            if (result.isSuccess) {
                println("任务已删除: $taskId")
            } else {
                println("删除任务失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    // 批量操作
    fun pauseAllTasks() {
        scope.launch {
            val activeTasks = getActiveTasks()
            activeTasks.forEach { task ->
                pauseTask(task.id)
            }
        }
    }
    
    fun resumeAllTasks() {
        scope.launch {
            val allTasks = getAllTasks()
            val pausedTasks = allTasks.filter { it.status == TaskStatus.PAUSED }
            pausedTasks.forEach { task ->
                resumeTask(task.id)
            }
        }
    }
    
    fun clearCompletedTasks() {
        scope.launch {
            val result = MediaProcessorModule.clearCompletedTasks()
            if (result.isSuccess) {
                println("已清理完成的任务")
            } else {
                println("清理任务失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    // 获取统计信息
    fun getStatistics() {
        scope.launch {
            val result = MediaProcessorModule.getStatistics()
            if (result.isSuccess) {
                val stats = result.getOrNull()!!
                println("统计信息:")
                println("- 总任务数: ${stats.totalTasks}")
                println("- 活跃任务数: ${stats.activeTasks}")
                println("- 已完成任务数: ${stats.completedTasks}")
                println("- 失败任务数: ${stats.failedTasks}")
                println("- 总下载大小: ${MediaProcessorModule.formatFileSize(stats.totalDownloadedBytes)}")
            }
        }
    }
    
    private fun showError(message: String) {
        // 显示错误信息
    }
}
```

### 5. 监听处理状态（使用Flow）

```kotlin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProcessingStateObserver {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun startObserving() {
        scope.launch {
            MediaProcessorModule.getStateFlow().collect { state ->
                // 处理整体状态变化
                println("活跃任务数: ${state.activeTasks}")
                println("总任务数: ${state.totalTasks}")
                println("已完成任务数: ${state.completedTasks}")
                println("失败任务数: ${state.failedTasks}")
                
                // 更新UI
                updateTaskCounts(state.activeTasks, state.totalTasks)
                updateProcessingStatus(state)
                
                // 处理存储问题
                if (state.hasStorageIssue) {
                    showStorageWarning()
                }
                
                // 处理网络问题
                if (state.hasNetworkIssue) {
                    showNetworkWarning()
                }
            }
        }
        
        // 监听任务更新
        scope.launch {
            MediaProcessorModule.getTaskUpdates().collect { task ->
                when (task.status) {
                     TaskStatus.PENDING -> {
                         println("任务排队中: ${task.mediaInfo.title}")
                     }
                     TaskStatus.PROCESSING -> {
                         println("任务处理中: ${task.mediaInfo.title} - ${task.progress}%")
                         updateTaskProgress(task)
                     }
                     TaskStatus.COMPLETED -> {
                         println("任务完成: ${task.mediaInfo.title}")
                         showTaskCompletedNotification(task)
                     }
                     TaskStatus.FAILED -> {
                         println("任务失败: ${task.mediaInfo.title} - ${task.error}")
                         showTaskFailedNotification(task)
                     }
                     TaskStatus.PAUSED -> {
                         println("任务暂停: ${task.mediaInfo.title}")
                     }
                     TaskStatus.CANCELLED -> {
                         println("任务取消: ${task.mediaInfo.title}")
                     }
                 }
            }
        }
    }
    
    private fun updateTaskCounts(activeTasks: Int, totalTasks: Int) {
        // 更新任务计数UI
    }
    
    private fun updateProcessingStatus(state: MediaProcessingState) {
        // 更新处理状态UI
    }
    
    private fun updateTaskProgress(task: ProcessingTask) {
        // 更新任务进度UI
    }
    
    private fun showStorageWarning() {
        // 显示存储警告
    }
    
    private fun showNetworkWarning() {
        // 显示网络警告
    }
    
    private fun showTaskCompletedNotification(task: ProcessingTask) {
        // 显示任务完成通知
    }
    
    private fun showTaskFailedNotification(task: ProcessingTask) {
        // 显示任务失败通知
    }
}
```

### 6. 权限和存储管理

```kotlin
class PermissionHelper {
    
    fun checkPermissions(): Boolean {
        if (!MediaProcessorModule.hasAllPermissions()) {
            val missingPermissions = MediaProcessorModule.getMissingPermissions()
            println("缺失权限: ${missingPermissions.joinToString(", ")}")
            requestPermissions(missingPermissions)
            return false
        }
        
        return true
    }
    
    fun checkStorage(): Boolean {
        val availableSpace = MediaProcessorModule.getAvailableStorageSpace()
        val requiredSpace = 100 * 1024 * 1024 // 100MB
        
        if (availableSpace < requiredSpace) {
            showStorageWarning(requiredSpace, availableSpace)
            return false
        }
        
        return true
    }
    
    fun getOutputDirectory(): String {
        return MediaProcessorModule.getOutputDirectory()
    }
    
    fun setCustomOutputDirectory(path: String): Boolean {
        val result = MediaProcessorModule.setOutputDirectory(path)
        return result.isSuccess
    }
    
    fun getStorageInfo(): StorageInfo {
        return MediaProcessorModule.getStorageInfo()
    }
    
    private fun requestPermissions(permissions: List<String>) {
        // 请求权限的实现
    }
    
    private fun showStorageWarning(required: Long, available: Long) {
        val requiredMB = required / (1024 * 1024)
        val availableMB = available / (1024 * 1024)
        println("存储空间不足: 需要 ${requiredMB}MB，可用 ${availableMB}MB")
    }
}
```

## 高级用法

### 自定义配置

```kotlin
// 创建自定义配置
val customConfig = ModuleConfig.Builder()
    .setDownloadPath("/sdcard/MyApp/Downloads")
    .setMaxConcurrentDownloads(5)
    .setNetworkTimeoutMs(60000)
    .setMaxRetryCount(5)
    .setEnableResumeDownload(true)
    .setEnableDownloadNotification(true)
    .setLogLevel(LogLevel.DEBUG)
    .build()

// 使用自定义配置初始化
val api = VideoDownloaderApi.initialize(context) {
    applyConfig(customConfig)
}
```

### 错误处理

```kotlin
try {
    val videoInfo = downloadApi.fetchVideoInfo(url)
    val taskId = downloadApi.startDownload(videoInfo, format)
} catch (e: NetworkException) {
    // 网络错误
    showError("网络连接失败，请检查网络设置")
} catch (e: StorageException) {
    // 存储错误
    showError("存储空间不足或无法写入文件")
} catch (e: PermissionException) {
    // 权限错误
    requestPermissions()
} catch (e: VideoFetchException) {
    // 视频获取错误
    showError("无法获取视频信息，请检查URL是否正确")
} catch (e: Exception) {
    // 其他错误
    showError("未知错误: ${e.message}")
}
```

## 注意事项

1. **权限**: 确保应用有存储和网络权限
2. **生命周期**: 在Activity/Fragment销毁时取消注册回调
3. **线程安全**: API调用是线程安全的，可以在任何线程调用
4. **内存管理**: 及时清理不需要的回调，避免内存泄漏
5. **错误处理**: 始终使用try-catch包装API调用

```kotlin
override fun onDestroy() {
    super.onDestroy()
    // 清理回调，避免内存泄漏
    downloadApi.clearAllCallbacks()
}
```

这个模块提供了完整的视频下载功能，你可以根据自己的UI需求来展示下载进度和状态。