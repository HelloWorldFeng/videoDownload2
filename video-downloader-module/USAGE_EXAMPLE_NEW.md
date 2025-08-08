# 视频下载模块使用示例（重构版）

## 概述

重构后的视频下载模块提供了更简洁、更易用的API接口。主要改进包括：

- **简化的API设计**：通过 `VideoDownloaderManager` 提供统一入口
- **清晰的数据结构**：所有数据类集中在 `VideoDownloaderData.kt` 中
- **更好的状态管理**：使用 `StateFlow` 进行响应式状态监听
- **详细的中文注释**：提高代码可读性和维护性

## 核心组件

### 1. VideoDownloaderManager
主要的API管理器，提供所有下载功能的入口。

### 2. VideoDownloaderData.kt
包含所有数据类定义：
- `VideoInfo`：视频信息
- `DownloadStatus`：下载状态枚举
- `DownloadProgress`：下载进度信息
- `DownloadTaskInfo`：下载任务信息
- `DownloadConfig`：下载配置
- `DownloadResult`：下载结果

### 3. Task.kt
定义下载任务和状态管理。

### 4. DownloaderV2.kt
核心下载器实现。

## 使用示例

### 基本使用

```kotlin
import com.videodownloader.module.api.VideoDownloaderManager
import com.videodownloader.module.api.VideoInfo

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. 初始化下载管理器
        VideoDownloaderManager.initialize(this)
        
        // 2. 从备份恢复任务（可选）
        VideoDownloaderManager.restoreFromBackup()
        
        // 3. 开始下载
        startVideoDownload()
        
        // 4. 监听任务状态
        observeDownloadStates()
    }
    
    private fun startVideoDownload() {
        // 创建视频信息
        val videoInfo = VideoInfo(
            url = "https://example.com/video.mp4",
            title = "示例视频",
            duration = "10:30",
            thumbnail = "https://example.com/thumbnail.jpg"
        )
        
        // 开始下载，带进度回调
        val taskId = VideoDownloaderManager.startDownload(videoInfo) { progress, speed, status ->
            runOnUiThread {
                updateProgressUI(progress, speed, status)
            }
        }
        
        Log.d("Download", "开始下载任务: $taskId")
    }
    
    private fun observeDownloadStates() {
        lifecycleScope.launch {
            VideoDownloaderManager.observeTaskStates().collect { taskStates ->
                // 处理所有任务状态变化
                taskStates.forEach { (taskId, state) ->
                    when (state.downloadState) {
                        is Task.DownloadState.Completed -> {
                            Log.d("Download", "任务完成: $taskId")
                            showDownloadComplete(taskId)
                        }
                        is Task.DownloadState.Error -> {
                            Log.e("Download", "任务失败: $taskId, 错误: ${state.downloadState.message}")
                            showDownloadError(taskId, state.downloadState.message)
                        }
                        is Task.DownloadState.Running -> {
                            Log.d("Download", "任务运行中: $taskId")
                        }
                        else -> {
                            // 处理其他状态
                        }
                    }
                }
            }
        }
    }
    
    private fun updateProgressUI(progress: Float, speed: Long, status: String) {
        // 更新进度条
        progressBar.progress = progress.toInt()
        
        // 更新速度显示
        val speedText = when {
            speed > 1024 * 1024 -> "${speed / (1024 * 1024)} MB/s"
            speed > 1024 -> "${speed / 1024} KB/s"
            else -> "$speed B/s"
        }
        speedTextView.text = speedText
        
        // 更新状态文本
        statusTextView.text = status
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        VideoDownloaderManager.release()
    }
}
```

### 任务管理

```kotlin
class DownloadManagerActivity : AppCompatActivity() {
    
    private fun manageDownloadTasks() {
        // 获取所有任务状态
        val allTasks = VideoDownloaderManager.getAllTaskStates()
        
        allTasks.forEach { (taskId, state) ->
            when (state.downloadState) {
                is Task.DownloadState.Running -> {
                    // 可以取消正在运行的任务
                    showCancelOption(taskId)
                }
                is Task.DownloadState.Error -> {
                    // 可以重启失败的任务
                    showRestartOption(taskId)
                }
                is Task.DownloadState.Completed -> {
                    // 可以移除已完成的任务
                    showRemoveOption(taskId)
                }
                else -> {
                    // 处理其他状态
                }
            }
        }
    }
    
    private fun cancelDownload(taskId: String) {
        VideoDownloaderManager.cancelDownload(taskId)
        Toast.makeText(this, "已取消下载", Toast.LENGTH_SHORT).show()
    }
    
    private fun restartDownload(taskId: String) {
        VideoDownloaderManager.restartDownload(taskId) { progress, speed, status ->
            // 处理重启后的进度回调
            updateTaskProgress(taskId, progress, speed, status)
        }
        Toast.makeText(this, "已重启下载", Toast.LENGTH_SHORT).show()
    }
    
    private fun removeTask(taskId: String) {
        VideoDownloaderManager.removeTask(taskId)
        Toast.makeText(this, "已移除任务", Toast.LENGTH_SHORT).show()
    }
}
```

### M3U8视频下载

```kotlin
private fun downloadM3U8Video() {
    val videoInfo = VideoInfo(
        url = "https://example.com/playlist.m3u8",
        title = "直播回放视频",
        duration = "1:30:00"
    )
    
    // M3U8下载会自动识别格式
    val taskId = VideoDownloaderManager.startDownload(videoInfo) { progress, speed, status ->
        // M3U8下载进度回调
        Log.d("M3U8", "进度: $progress%, 速度: $speed, 状态: $status")
    }
    
    Log.d("M3U8", "开始M3U8下载: $taskId")
}
```

### 批量下载

```kotlin
private fun batchDownload(videoList: List<VideoInfo>) {
    val taskIds = mutableListOf<String>()
    
    videoList.forEach { videoInfo ->
        val taskId = VideoDownloaderManager.startDownload(videoInfo) { progress, speed, status ->
            Log.d("Batch", "视频 ${videoInfo.title} 进度: $progress%")
        }
        taskIds.add(taskId)
    }
    
    // 监听批量下载完成
    lifecycleScope.launch {
        VideoDownloaderManager.observeTaskStates().collect { taskStates ->
            val completedCount = taskIds.count { taskId ->
                taskStates[taskId]?.downloadState is Task.DownloadState.Completed
            }
            
            if (completedCount == taskIds.size) {
                Log.d("Batch", "批量下载全部完成")
                showBatchDownloadComplete()
            }
        }
    }
}
```

## 注意事项

1. **初始化**：使用前必须调用 `VideoDownloaderManager.initialize(context)`
2. **权限**：确保应用有存储权限
3. **网络**：确保设备有网络连接
4. **资源释放**：在适当时机调用 `VideoDownloaderManager.release()`
5. **状态监听**：使用 `StateFlow` 进行响应式状态监听，避免内存泄漏

## 文件结构

```
com.videodownloader.module/
├── api/
│   ├── VideoDownloaderData.kt      # 数据类定义
│   ├── VideoDownloaderManager.kt   # API管理器
│   └── DownloadUtil.kt            # 下载工具类
├── download/
│   ├── Task.kt                    # 任务定义
│   ├── DownloaderV2.kt           # 核心下载器
│   └── TaskFactory.kt            # 任务工厂
└── Ext.kt                        # 扩展函数
```

这个重构版本提供了更清晰的架构和更易用的API，同时保持了原有的所有功能。