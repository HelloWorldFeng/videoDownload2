# 媒体处理器模块集成指南

本文档记录了将媒体处理器模块集成到主项目 `wx_android_HD_Player` 中的详细步骤和配置。

## 项目结构

```
wx_android_HD_Player/
├── app/                          # 主应用模块
├── video-downloader-module/      # 媒体处理器模块
└── settings.gradle.kts           # 项目设置
```

## 集成步骤

### 1. 模块声明

在项目根目录的 `settings.gradle.kts` 中添加模块声明：

```kotlin
include(":video-downloader-module")
```

### 2. 添加模块依赖

在主应用的 `app/build.gradle.kts` 中添加模块依赖：

```kotlin
dependencies {
    // 媒体处理器模块
    implementation(project(":video-downloader-module"))
    
    // 其他依赖...
}
```

### 3. 权限配置

在主应用的 `app/src/main/AndroidManifest.xml` 中添加必要的权限：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <!-- 网络权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    
    <!-- 存储权限 -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    
    <!-- Android 11+ 存储权限 -->
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" 
        tools:ignore="ScopedStorage" />
    
    <!-- 前台服务权限 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    
    <!-- 通知权限 (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application
        android:name=".PlayerApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">
        
        <!-- 主Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 媒体处理器服务 -->
        <service
            android:name="com.nexus.core.media.processor.service.MediaProcessorService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
        
    </application>
    
</manifest>
```

### 4. Application 初始化

在应用的 `Application` 类中初始化媒体处理器模块：

```kotlin
import com.nexus.core.media.processor.MediaProcessorModule
import com.nexus.core.media.processor.config.MediaProcessorConfig
import android.os.Environment

class PlayerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化媒体处理器模块
        initMediaProcessor()
    }
    
    private fun initMediaProcessor() {
        // 使用构建器模式配置
        val config = MediaProcessorModule.ConfigBuilder(this)
            .outputDirectory(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                    ?: "${filesDir.absolutePath}/downloads"
            )
            .maxConcurrentTasks(3)
            .networkTimeout(30_000, 60_000)
            .maxRetryCount(3)
            .enableDebugLog(BuildConfig.DEBUG)
            .enableNotifications(true)
            .enableForegroundService(true)
            .build()
        
        // 初始化模块
        val result = MediaProcessorModule.initialize(config)
        if (result.isFailure) {
            Log.e("PlayerApp", "媒体处理器模块初始化失败", result.exceptionOrNull())
        }
        
        // 或者使用预设配置
        // MediaProcessorModule.initialize(MediaProcessorConfig.highPerformance(this))
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // 清理资源
        MediaProcessorModule.destroy()
    }
}
```

### 5. 在 Activity 中使用

```kotlin
import com.nexus.core.media.processor.MediaProcessorModule
import com.nexus.core.media.processor.model.ProcessingOptions
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 检查模块是否已初始化
        if (!MediaProcessorModule.isInitialized()) {
            showError("媒体处理器模块未初始化")
            return
        }
        
        // 检查权限
        checkPermissions()
        
        // 设置监听器
        setupListeners()
        
        // 示例：处理媒体
        processMedia("https://example.com/video.mp4")
    }
    
    private fun checkPermissions() {
        if (!MediaProcessorModule.hasAllPermissions()) {
            val missingPermissions = MediaProcessorModule.getMissingPermissions()
            requestPermissions(missingPermissions.toTypedArray(), 1001)
        }
    }
    
    private fun setupListeners() {
        // 添加进度监听器
        MediaProcessorModule.addProgressListener { task ->
            runOnUiThread {
                updateProgress(task.id, task.progress, task.speed)
            }
        }
        
        // 添加状态监听器
        MediaProcessorModule.addStatusListener { task ->
            runOnUiThread {
                when (task.status) {
                    TaskStatus.COMPLETED -> showSuccess("处理完成: ${task.mediaInfo.title}")
                    TaskStatus.FAILED -> showError("处理失败: ${task.error}")
                    TaskStatus.PROCESSING -> showInfo("正在处理: ${task.mediaInfo.title}")
                    else -> {}
                }
            }
        }
        
        // 使用Flow监听状态变化
        lifecycleScope.launch {
            MediaProcessorModule.getStateFlow().collect { state ->
                updateGlobalState(state)
            }
        }
    }
    
    private fun processMedia(url: String) {
        lifecycleScope.launch {
            try {
                // 获取媒体信息
                val result = MediaProcessorModule.getMediaInfo(url)
                
                if (result.isSuccess) {
                    val mediaInfo = result.getOrNull()!!
                    
                    // 开始处理任务
                    val taskResult = MediaProcessorModule.addTask(
                        url = url,
                        options = ProcessingOptions(
                            outputFormat = "mp4",
                            quality = "best",
                            downloadSubtitles = true,
                            downloadThumbnail = true
                        )
                    )
                    
                    if (taskResult.isSuccess) {
                        val taskId = taskResult.getOrNull()!!
                        showInfo("开始处理: ${mediaInfo.title}")
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
    
    private fun updateProgress(taskId: String, progress: Int, speed: Long) {
        // 更新进度条和速度显示
        val speedText = MediaProcessorModule.formatFileSize(speed) + "/s"
        // progressBar.progress = progress
        // speedTextView.text = speedText
    }
    
    private fun updateGlobalState(state: MediaProcessingState) {
        // 更新全局状态UI
        // activeTasksTextView.text = "活跃任务: ${state.activeTasks}"
        // totalTasksTextView.text = "总任务: ${state.totalTasks}"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 移除监听器
        MediaProcessorModule.removeAllProgressListeners()
        MediaProcessorModule.removeAllStatusListeners()
    }
    
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showInfo(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
```

## 配置选项

### 模块配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `downloadPath` | String | `/sdcard/Downloads` | 下载文件保存路径 |
| `maxConcurrentDownloads` | Int | `3` | 最大并发下载数 |
| `networkTimeoutMs` | Long | `30000` | 网络超时时间(毫秒) |
| `maxRetryCount` | Int | `3` | 最大重试次数 |
| `enableResumeDownload` | Boolean | `true` | 是否启用断点续传 |
| `enableDownloadNotification` | Boolean | `true` | 是否启用下载通知 |
| `logLevel` | LogLevel | `INFO` | 日志级别 |

### 预设配置

```kotlin
// 高性能配置
val config = MediaProcessorConfig.highPerformance(context)
MediaProcessorModule.initialize(config)

// 低功耗配置
val config = MediaProcessorConfig.lowPower(context)
MediaProcessorModule.initialize(config)

// 平衡配置（默认）
val config = MediaProcessorConfig.balanced(context)
MediaProcessorModule.initialize(config)
```

### 使用构建器自定义配置

```kotlin
val config = MediaProcessorModule.ConfigBuilder(context)
    // 基本设置
    .outputDirectory("/sdcard/MediaProcessor")
    .maxConcurrentTasks(4)
    .networkTimeout(30_000, 60_000)
    .maxRetryCount(3)
    
    // 功能开关
    .enableDebugLog(BuildConfig.DEBUG)
    .enableNotifications(true)
    .enableForegroundService(true)
    .enableResumeDownload(true)
    
    // 质量和格式偏好
    .preferredVideoQuality("1080p")
    .preferredAudioQuality("high")
    .preferredVideoFormat("mp4")
    .preferredAudioFormat("mp3")
    
    // 网络设置
    .userAgent("MyApp/2.0")
    .customHeaders(mapOf(
        "Referer" to "https://example.com",
        "Accept-Language" to "zh-CN,zh;q=0.9"
    ))
    
    // 存储设置
    .enableCache(true)
    .cacheSize(100 * 1024 * 1024) // 100MB
    .tempDirectory("/sdcard/MediaProcessor/temp")
    
    // 文件命名
    .fileNamingPattern("{title}_{quality}.{ext}")
    .enableSafeFileName(true)
    
    .build()

MediaProcessorModule.initialize(config)
```

### 主要配置参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `outputDirectory` | String | 应用外部文件目录 | 输出文件保存目录 |
| `maxConcurrentTasks` | Int | 2 | 最大并发处理任务数 |
| `networkTimeout` | Pair<Int, Int> | (30000, 60000) | 连接和读取超时(ms) |
| `maxRetryCount` | Int | 3 | 最大重试次数 |
| `enableNotifications` | Boolean | true | 是否启用通知 |
| `enableForegroundService` | Boolean | true | 是否启用前台服务 |
| `enableResumeDownload` | Boolean | true | 是否启用断点续传 |
| `preferredVideoQuality` | String | "best" | 首选视频质量 |
| `preferredAudioQuality` | String | "best" | 首选音频质量 |
| `userAgent` | String | 默认UA | 自定义User-Agent |
| `enableCache` | Boolean | true | 是否启用缓存 |
| `cacheSize` | Long | 50MB | 缓存大小 |

### 自定义配置示例

```kotlin
// 针对移动网络优化的配置
val mobileConfig = MediaProcessorModule.ConfigBuilder(context)
    .maxConcurrentTasks(1)  // 减少并发数
    .networkTimeout(60_000, 120_000)  // 增加超时时间
    .preferredVideoQuality("720p")  // 降低质量
    .enableNotifications(false)  // 关闭通知
    .enableForegroundService(false)  // 关闭前台服务
    .build()

// 针对WiFi网络优化的配置
val wifiConfig = MediaProcessorModule.ConfigBuilder(context)
    .maxConcurrentTasks(4)  // 增加并发数
    .networkTimeout(15_000, 30_000)  // 减少超时时间
    .preferredVideoQuality("1080p")  // 提高质量
    .enableNotifications(true)  // 启用通知
    .enableForegroundService(true)  // 启用前台服务
    .build()

// 针对存储空间有限的配置
val lowStorageConfig = MediaProcessorModule.ConfigBuilder(context)
    .preferredVideoQuality("720p")
    .preferredVideoFormat("mp4")  // 使用压缩率更好的格式
    .enableCache(false)  // 关闭缓存
    .enableResumeDownload(false)  // 关闭断点续传
    .tempDirectory(null)  // 不使用临时目录
    .build()

// 开发调试配置
val debugConfig = MediaProcessorModule.ConfigBuilder(context)
    .enableDebugLog(true)
    .maxRetryCount(1)  // 减少重试次数以快速失败
    .networkTimeout(10_000, 20_000)  // 减少超时时间
    .outputDirectory("${context.filesDir}/debug_output")
    .build()

// 根据网络类型动态选择配置
fun getConfigForNetworkType(context: Context): MediaProcessorConfig {
    return when (getNetworkType(context)) {
        NetworkType.WIFI -> wifiConfig
        NetworkType.MOBILE -> mobileConfig
        NetworkType.NONE -> throw IllegalStateException("无网络连接")
    }
}
```

## 使用示例

### 基本下载流程

```kotlin
class VideoDownloadHelper {
    private val downloadManager = VideoDownloaderManager
    
    suspend fun downloadVideo(url: String) {
        try {
            // 1. 判断视频类型并开始下载
            val taskId = downloadManager.startDownload(url)
            
            Log.d("Download", "任务创建成功: $taskId")
            
            // 2. 监听下载状态
            downloadManager.getTaskStateFlow(taskId).collect { task ->
                when (task.downloadState) {
                    is Task.DownloadState.Running -> {
                        Log.d("Download", "下载进度: ${task.downloadState.progress}%")
                    }
                    is Task.DownloadState.Completed -> {
                        Log.d("Download", "下载完成: ${task.downloadState.filePath}")
                    }
                    is Task.DownloadState.Error -> {
                        Log.e("Download", "下载失败: ${task.downloadState.message}")
                    }
                    else -> {
                        Log.d("Download", "任务状态: ${task.downloadState}")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("Download", "下载失败", e)
            throw e
        }
    }
    
    // 根据URL自动判断视频类型
    fun getVideoType(url: String): Task.VideoType {
        return downloadManager.getVideoTypeFromUrl(url)
    }
}
```

### 任务管理

```kotlin
class DownloadTaskManager {
    private val downloadManager = VideoDownloaderManager
    
    // 获取所有任务
    fun getAllTasks(): List<Task> {
        return downloadManager.getAllTaskStates()
    }
    
    // 取消所有下载
    suspend fun cancelAllDownloads() {
        val tasks = downloadManager.getAllTaskStates()
        tasks.forEach { task ->
            downloadManager.cancelTask(task.id)
        }
    }
    
    // 重启所有失败的下载
    suspend fun restartFailedDownloads() {
        val tasks = downloadManager.getAllTaskStates()
        tasks.filter { it.downloadState is Task.DownloadState.Error }
            .forEach { task ->
                downloadManager.restartTask(task.id)
            }
    }
    
    // 移除已完成任务
    suspend fun removeCompletedTasks() {
        val tasks = downloadManager.getAllTaskStates()
        tasks.filter { it.downloadState is Task.DownloadState.Completed }
            .forEach { task ->
                downloadManager.removeTask(task.id)
            }
    }
}
```

## 错误处理

### 常见异常类型

```kotlin
try {
    val taskId = VideoDownloaderManager.startDownload(url)
    
    // 监听任务状态处理错误
    VideoDownloaderManager.getTaskStateFlow(taskId).collect { task ->
        when (val state = task.downloadState) {
            is Task.DownloadState.Error -> {
                when {
                    state.message.contains("网络") -> {
                        showError("网络连接失败，请检查网络设置")
                    }
                    state.message.contains("存储") || state.message.contains("空间") -> {
                        showError("存储空间不足或无法写入文件")
                    }
                    state.message.contains("权限") -> {
                        requestPermissions()
                    }
                    state.message.contains("不支持") -> {
                        showError("该视频平台暂不支持")
                    }
                    else -> {
                        showError("下载失败: ${state.message}")
                    }
                }
            }
            is Task.DownloadState.Completed -> {
                showSuccess("下载完成: ${state.filePath}")
            }
            is Task.DownloadState.Running -> {
                updateProgress(state.progress)
            }
        }
    }
} catch (e: Exception) {
    // 其他错误
    showError("未知错误: ${e.message}")
}
```

## 性能优化建议

### 1. 内存优化

```kotlin
// 及时释放资源
override fun onDestroy() {
    super.onDestroy()
    // 取消所有正在进行的下载任务
    lifecycleScope.launch {
        VideoDownloaderManager.getAllTaskStates()
            .filter { it.downloadState is Task.DownloadState.Running }
            .forEach { task ->
                VideoDownloaderManager.cancelTask(task.id)
            }
    }
    // 释放下载管理器资源
    VideoDownloaderManager.release()
}

// 使用弱引用持有Context
class WeakDownloadObserver(activity: Activity) {
    private val activityRef = WeakReference(activity)
    
    fun observeDownloadTask(taskId: String) {
        activityRef.get()?.let { activity ->
            activity.lifecycleScope.launch {
                VideoDownloaderManager.getTaskStateFlow(taskId).collect { task ->
                    when (task.downloadState) {
                        is Task.DownloadState.Completed -> {
                            // 更新UI
                            activity.runOnUiThread {
                                // 更新完成状态
                            }
                        }
                        // 其他状态处理...
                    }
                }
            }
        }
    }
}
```

### 2. 网络优化

```kotlin
// 根据网络类型调整下载策略
class NetworkOptimizer(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    fun optimizeDownloadStrategy() {
        val networkInfo = connectivityManager.activeNetworkInfo
        when {
            networkInfo?.type == ConnectivityManager.TYPE_WIFI -> {
                // WiFi环境，可以同时下载多个任务
                Log.d("NetworkOptimizer", "WiFi环境，启用高速下载模式")
            }
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE -> {
                // 移动网络，限制并发下载
                Log.d("NetworkOptimizer", "移动网络，启用节省流量模式")
                // 可以暂停部分下载任务
                pauseNonPriorityTasks()
            }
            else -> {
                // 无网络连接，暂停所有下载
                pauseAllTasks()
            }
        }
    }
    
    private suspend fun pauseNonPriorityTasks() {
        VideoDownloaderManager.getAllTaskStates()
            .filter { it.downloadState is Task.DownloadState.Running }
            .drop(1) // 保留第一个任务继续下载
            .forEach { task ->
                VideoDownloaderManager.cancelTask(task.id)
            }
    }
    
    private suspend fun pauseAllTasks() {
        VideoDownloaderManager.getAllTaskStates()
            .filter { it.downloadState is Task.DownloadState.Running }
            .forEach { task ->
                VideoDownloaderManager.cancelTask(task.id)
            }
    }
}
```

### 3. 存储优化

```kotlin
// 定期清理已完成的任务和临时文件
class StorageOptimizer {
    
    fun startPeriodicCleanup(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                delay(TimeUnit.HOURS.toMillis(1)) // 每小时检查一次
                cleanupCompletedTasks()
                cleanupTempFiles()
            }
        }
    }
    
    private suspend fun cleanupCompletedTasks() {
        // 清理7天前完成的任务
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        VideoDownloaderManager.getAllTaskStates()
            .filter { task ->
                task.downloadState is Task.DownloadState.Completed &&
                task.id.substringAfter("_").toLongOrNull()?.let { it < sevenDaysAgo } == true
            }
            .forEach { task ->
                VideoDownloaderManager.removeTask(task.id)
            }
    }
    
    private fun cleanupTempFiles() {
        // 清理临时文件
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VideoBox")
        downloadDir.listFiles()?.filter { file ->
            file.name.endsWith(".tmp") || file.name.endsWith(".part")
        }?.forEach { file ->
            if (file.lastModified() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) {
                file.delete()
                Log.d("StorageOptimizer", "删除临时文件: ${file.name}")
            }
        }
    }
}
```

## 调试和日志

### 启用调试模式

```kotlin
// 在Application中初始化
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化视频下载管理器
        VideoDownloaderManager.initialize(this)
        
        if (BuildConfig.DEBUG) {
            // 启用详细日志
            Log.d("VideoDownloader", "调试模式已启用")
        }
    }
}
```

### 自定义日志处理

```kotlin
// 设置自定义日志处理器
LogUtils.setCustomLogger { level, tag, message, throwable ->
    when (level) {
        LogLevel.ERROR -> {
            // 上报错误到崩溃分析平台
            Crashlytics.log("$tag: $message")
            throwable?.let { Crashlytics.recordException(it) }
        }
        LogLevel.DEBUG -> {
            // 只在调试模式下输出
            if (BuildConfig.DEBUG) {
                Log.d(tag, message, throwable)
            }
        }
        else -> {
            Log.println(level.priority, tag, message)
        }
    }
}
```

## 版本兼容性

### Android版本支持

- **最低支持版本**: Android 5.0 (API 21)
- **目标版本**: Android 14 (API 34)
- **推荐版本**: Android 8.0+ (API 26)

### 权限适配

```kotlin
// Android 11+ 存储权限适配
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    if (!Environment.isExternalStorageManager()) {
        // 请求管理外部存储权限
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
}

// Android 13+ 通知权限适配
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        // 请求通知权限
        ActivityCompat.requestPermissions(this, 
            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
            REQUEST_NOTIFICATION_PERMISSION)
    }
}
```

## 常见问题

### Q: 下载失败，提示权限不足
A: 检查是否已添加存储权限，并在运行时请求权限。Android 11+需要特殊处理。

### Q: 下载速度很慢
A: 可以尝试增加并发下载数，或检查网络连接状态。

### Q: 某些视频无法下载
A: 可能是视频平台不支持，或者视频需要登录才能访问。

### Q: 应用崩溃
A: 检查是否正确初始化模块，并确保在主线程中更新UI。

## 更新日志

### v1.0.0 (2024-01-01)
- 初始版本发布
- 支持多平台视频下载
- 支持断点续传
- 支持并发下载
- 完整的权限管理
- 丰富的回调接口

---

**注意**: 本集成指南基于模块版本 1.0.0，如有更新请参考最新文档。