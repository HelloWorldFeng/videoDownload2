# 媒体处理器模块 (MediaProcessor)

这是一个功能强大的Android媒体处理模块，提供统一的媒体下载、转换和处理功能，支持多种媒体平台和格式。

## 特性

### 🚀 核心功能
- **统一媒体处理**: 支持视频、音频的下载、转换和处理
- **多平台支持**: 支持YouTube、Bilibili、抖音等主流媒体平台
- **多格式支持**: 支持MP4、WebM、MP3、AAC等多种媒体格式
- **智能格式选择**: 自动选择最佳媒体格式和质量
- **高性能处理**: 支持硬件加速和并发处理
- **任务队列管理**: 智能的任务调度和优先级管理

### 🛡️ 安全与稳定
- **安全验证**: URL验证、内容过滤和安全检查
- **权限管理**: 自动检查和管理Android权限
- **存储管理**: 智能存储空间管理和文件组织
- **网络管理**: 网络状态监控、代理支持和自动重连
- **错误处理**: 完善的异常处理和重试机制
- **数据完整性**: 文件完整性校验和哈希验证

### 📊 状态监控
- **实时状态流**: 基于Kotlin Flow的实时状态监听
- **进度监控**: 详细的处理进度和速度统计
- **任务管理**: 完整的任务生命周期管理
- **统计信息**: 处理统计和性能监控
- **日志系统**: 多级别日志记录和输出

### ⚙️ 高度可配置
- **灵活配置**: 支持多种预设配置和自定义选项
- **模块化设计**: 清晰的架构分层，易于扩展和维护
- **缓存系统**: 多级缓存支持，提升性能
- **Android集成**: 完善的Android服务和通知支持

## 架构设计

```
com.nexus.core.media.processor/
├── MediaProcessorModule.kt               # 模块主入口
├── MediaProcessor.kt                     # 媒体处理器核心类
├── api/                                  # 对外API接口
│   ├── MediaProcessorApi.kt              # 主要API接口
│   └── ProcessingCallback.kt             # 回调接口
├── config/                               # 配置管理
│   └── MediaProcessorConfig.kt           # 配置类
├── model/                                # 数据模型
│   ├── MediaInfo.kt                      # 媒体信息
│   ├── ProcessingTask.kt                 # 处理任务
│   ├── ProcessingOptions.kt              # 处理选项
│   └── MediaProcessingState.kt           # 处理状态
├── engine/                               # 任务执行引擎
│   └── TaskExecutionEngine.kt            # 任务执行引擎
├── extractor/                            # 媒体信息提取
│   └── MediaInfoExtractor.kt             # 媒体信息提取器
├── storage/                              # 存储管理
│   └── MediaStorageManager.kt            # 存储管理器
├── cache/                                # 缓存管理
│   └── CacheManager.kt                   # 缓存管理器
├── network/                              # 网络管理
│   └── NetworkManager.kt                 # 网络管理器
├── security/                             # 安全管理
│   └── SecurityManager.kt                # 安全管理器
├── log/                                  # 日志管理
│   └── LogManager.kt                     # 日志管理器
├── permission/                           # 权限管理
│   └── PermissionManager.kt              # 权限管理器
├── service/                              # Android服务
│   └── MediaProcessorService.kt          # 后台服务
├── utils/                                # 工具类
│   └── MediaProcessorUtils.kt            # 工具类集合
└── exception/                            # 异常处理
    └── MediaProcessorExceptions.kt       # 异常定义
```

## 快速开始

### 1. 添加依赖

在你的`build.gradle`文件中添加模块依赖：

```gradle
dependencies {
    implementation project(':video-downloader-module')
}
```

### 2. 初始化模块

```kotlin
import com.nexus.core.media.processor.MediaProcessorModule
import com.nexus.core.media.processor.config.MediaProcessorConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 使用默认配置初始化
        MediaProcessorModule.initialize(this)
        
        // 或使用自定义配置
        MediaProcessorModule.initialize(this, MediaProcessorConfig.highPerformance())
        
        // 或使用构建器模式
        MediaProcessorModule.initializeWithBuilder(this) {
            maxConcurrentTasks(5)
            enableHardwareAcceleration(true)
            enableDebugMode(BuildConfig.DEBUG)
        }
    }
}
```

### 3. 开始处理媒体

```kotlin
class MainActivity : AppCompatActivity() {
    
    private fun processMedia(url: String) {
        lifecycleScope.launch {
            try {
                // 获取媒体信息
                val result = MediaProcessorModule.getMediaInfo(url)
                if (result.isSuccess) {
                    val mediaInfo = result.getOrNull()!!
                    
                    // 开始处理任务
                    val taskResult = MediaProcessorModule.addTask(url)
                    if (taskResult.isSuccess) {
                        val taskId = taskResult.getOrNull()!!
                        Toast.makeText(this@MainActivity, "处理开始: $taskId", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### 4. 监听处理状态

```kotlin
// 监听进度更新
MediaProcessorModule.addProgressListener("main") { task ->
    println("${task.mediaInfo.title}: ${task.progress}%")
}

// 监听状态变化
MediaProcessorModule.addStateListener("main") { state ->
    println("活跃任务数: ${state.activeTasks}")
}

// 使用Flow监听
lifecycleScope.launch {
    MediaProcessorModule.getTaskUpdates().collect { task ->
        when (task.status) {
            ProcessingStatus.COMPLETED -> println("处理完成: ${task.mediaInfo.title}")
            ProcessingStatus.FAILED -> println("处理失败: ${task.error}")
            else -> {}
        }
    }
}
```

## 详细使用说明

请查看 [USAGE_EXAMPLE.md](USAGE_EXAMPLE.md) 获取完整的使用示例和API文档。

## 主要类说明

### MediaProcessorModule
模块主入口类，提供统一的对外接口：
- `initialize()`: 初始化模块
- `getMediaInfo()`: 获取媒体信息
- `addTask()`: 添加处理任务
- `pauseTask()`: 暂停任务
- `resumeTask()`: 恢复任务
- `cancelTask()`: 取消任务
- `deleteTask()`: 删除任务

### MediaProcessor
媒体处理器核心类，负责任务执行和状态管理：
- 任务生命周期管理
- 状态流和事件分发
- 资源管理和清理

### MediaInfo
媒体信息数据模型：
- 媒体基本信息（标题、描述、时长等）
- 可用格式列表
- 缩略图和字幕信息
- 格式筛选和选择方法

### ProcessingTask
处理任务数据模型：
- 任务状态和进度
- 处理速度和剩余时间
- 错误信息和重试次数
- 输出文件信息

## 权限要求

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" 
    tools:ignore="ScopedStorage" />

<!-- 前台服务权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- 通知权限 (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 注意事项

1. **模块初始化**: 使用前必须先调用 `MediaProcessorModule.initialize()`
2. **权限检查**: 使用前请确保应用有必要的存储、网络和通知权限
3. **生命周期管理**: 在Activity/Fragment销毁时记得移除监听器
4. **线程安全**: 所有API调用都是线程安全的，支持协程
5. **错误处理**: 建议使用Result类型处理返回值
6. **内存管理**: 及时清理不需要的监听器，避免内存泄漏
7. **配置验证**: 自定义配置会自动验证，无效配置会抛出异常

## 配置选项

### 预设配置
- `MediaProcessorConfig.default()`: 默认配置
- `MediaProcessorConfig.highPerformance()`: 高性能配置
- `MediaProcessorConfig.lowMemory()`: 低内存配置
- `MediaProcessorConfig.debug()`: 调试配置

### 主要配置参数
- `maxConcurrentTasks`: 最大并发任务数
- `taskTimeoutMs`: 任务超时时间
- `maxRetryCount`: 最大重试次数
- `enableHardwareAcceleration`: 启用硬件加速
- `maxMemoryUsageMB`: 最大内存使用量
- `enableDebugMode`: 启用调试模式

## 版本信息

- **版本**: 2.0.0
- **最低Android版本**: API 21 (Android 5.0)
- **目标Android版本**: API 34 (Android 14)
- **Kotlin版本**: 1.9.0+
- **协程版本**: 1.7.0+

## 许可证

本模块采用MIT许可证，详情请查看LICENSE文件。