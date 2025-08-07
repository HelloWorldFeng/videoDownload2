# 视频下载模块集成检查清单

使用此清单确保视频下载模块正确集成到您的项目中。

## 📋 集成前准备

### 项目要求
- [ ] Android项目最低API级别 >= 21 (Android 5.0)
- [ ] 使用Kotlin语言
- [ ] 支持协程 (Coroutines)
- [ ] Gradle版本 >= 7.0

### 依赖检查
- [ ] 项目已配置Kotlin协程依赖
- [ ] 项目已配置网络请求库 (如OkHttp)
- [ ] 项目已配置JSON解析库 (如Gson/Moshi)

## 🔧 模块集成

### 1. 模块添加
- [ ] 在`settings.gradle.kts`中添加模块声明: `include(":video-downloader-module")`
- [ ] 在`app/build.gradle.kts`中添加模块依赖: `implementation(project(":video-downloader-module"))`
- [ ] 项目能够成功编译，无依赖错误

### 2. 权限配置
在`AndroidManifest.xml`中添加以下权限：
- [ ] `INTERNET` - 网络访问权限
- [ ] `ACCESS_NETWORK_STATE` - 网络状态权限
- [ ] `WRITE_EXTERNAL_STORAGE` - 写入存储权限
- [ ] `READ_EXTERNAL_STORAGE` - 读取存储权限
- [ ] `FOREGROUND_SERVICE` - 前台服务权限
- [ ] `POST_NOTIFICATIONS` - 通知权限 (Android 13+)

### 3. Application初始化
- [ ] 在Application类的`onCreate()`中调用`VideoDownloaderApi.initialize()`
- [ ] 配置了合适的下载路径
- [ ] 设置了合理的并发下载数
- [ ] 根据需要配置了其他参数

```kotlin
// 示例初始化代码
VideoDownloaderApi.initialize(this) {
    downloadPath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
    maxConcurrentDownloads = 3
    enableResumeDownload = true
}
```

## 🧪 功能测试

### 基础功能测试
- [ ] 模块初始化成功，无异常抛出
- [ ] 能够获取`VideoDownloaderApi`实例
- [ ] 权限检查功能正常: `hasStoragePermission()`, `hasNetworkPermission()`
- [ ] 存储管理功能正常: `getAvailableStorageSpace()`, `getDownloadDirectory()`

### 视频信息获取测试
- [ ] 能够成功获取YouTube视频信息
- [ ] 能够成功获取Bilibili视频信息
- [ ] 获取的视频信息包含标题、时长、格式列表等
- [ ] 错误URL能够正确抛出异常

```kotlin
// 测试代码示例
lifecycleScope.launch {
    try {
        val videoInfo = downloadApi.fetchVideoInfo("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        Log.d("Test", "视频标题: ${videoInfo.title}")
        Log.d("Test", "可用格式: ${videoInfo.formats.size}")
    } catch (e: Exception) {
        Log.e("Test", "获取视频信息失败", e)
    }
}
```

### 下载功能测试
- [ ] 能够成功创建下载任务
- [ ] 下载任务能够正常开始
- [ ] 能够暂停和恢复下载
- [ ] 能够取消下载任务
- [ ] 断点续传功能正常

### 回调功能测试
- [ ] 下载开始回调正常触发
- [ ] 下载进度回调正常更新
- [ ] 下载完成回调正常触发
- [ ] 下载失败回调正常触发
- [ ] 网络状态变化回调正常触发

```kotlin
// 回调测试示例
downloadApi.registerDownloadCallback(object : DownloadCallback {
    override fun onDownloadStarted(task: DownloadTask) {
        Log.d("Test", "✅ 下载开始回调正常")
    }
    
    override fun onDownloadProgress(task: DownloadTask, progress: Int, speed: Long) {
        Log.d("Test", "✅ 进度回调正常: $progress%")
    }
    
    override fun onDownloadCompleted(task: DownloadTask) {
        Log.d("Test", "✅ 下载完成回调正常")
    }
    
    override fun onDownloadFailed(task: DownloadTask, error: String) {
        Log.d("Test", "✅ 下载失败回调正常: $error")
    }
})
```

## 🔍 运行时检查

### 权限检查
- [ ] 应用启动时正确检查存储权限
- [ ] 缺少权限时能够正确提示用户
- [ ] Android 11+设备上存储权限处理正确
- [ ] Android 13+设备上通知权限处理正确

### 网络检查
- [ ] 网络连接正常时下载功能正常
- [ ] 网络断开时下载自动暂停
- [ ] 网络恢复时下载自动恢复
- [ ] WiFi/移动网络切换时处理正确

### 存储检查
- [ ] 存储空间充足时下载正常
- [ ] 存储空间不足时正确提示
- [ ] 下载文件保存到正确路径
- [ ] 临时文件能够正确清理

### 内存检查
- [ ] 长时间运行无内存泄漏
- [ ] Activity销毁时回调正确清理
- [ ] 大量下载任务时内存使用正常

## 🚀 性能验证

### 下载性能
- [ ] 单个文件下载速度正常
- [ ] 多个文件并发下载正常
- [ ] 大文件下载稳定性良好
- [ ] 下载过程中应用响应正常

### 资源使用
- [ ] CPU使用率在合理范围内
- [ ] 内存使用量稳定
- [ ] 电池消耗在可接受范围内
- [ ] 网络使用效率良好

## 🐛 错误处理验证

### 网络错误
- [ ] 网络超时能够正确处理
- [ ] 网络中断能够正确恢复
- [ ] 无效URL能够正确提示
- [ ] 服务器错误能够正确处理

### 存储错误
- [ ] 磁盘空间不足能够正确处理
- [ ] 文件权限错误能够正确提示
- [ ] 路径不存在能够自动创建
- [ ] 文件损坏能够正确处理

### 应用错误
- [ ] 应用崩溃后能够正确恢复
- [ ] 内存不足时能够正确处理
- [ ] 后台运行时功能正常

## 📱 设备兼容性

### Android版本
- [ ] Android 5.0 (API 21) 设备正常运行
- [ ] Android 8.0 (API 26) 设备正常运行
- [ ] Android 10 (API 29) 设备正常运行
- [ ] Android 11 (API 30) 设备正常运行
- [ ] Android 12 (API 31) 设备正常运行
- [ ] Android 13 (API 33) 设备正常运行
- [ ] Android 14 (API 34) 设备正常运行

### 设备类型
- [ ] 手机设备正常运行
- [ ] 平板设备正常运行
- [ ] 不同屏幕尺寸适配正常
- [ ] 不同分辨率适配正常

### 硬件配置
- [ ] 低配置设备 (2GB RAM) 正常运行
- [ ] 中配置设备 (4GB RAM) 正常运行
- [ ] 高配置设备 (8GB+ RAM) 正常运行

## 🔒 安全检查

### 数据安全
- [ ] 下载的文件完整性正确
- [ ] 临时文件安全清理
- [ ] 敏感信息不会泄露
- [ ] 网络传输安全

### 权限安全
- [ ] 只请求必要权限
- [ ] 权限使用符合最小化原则
- [ ] 用户拒绝权限时正确处理

## 📊 日志和监控

### 日志记录
- [ ] 关键操作有日志记录
- [ ] 错误信息记录完整
- [ ] 日志级别配置正确
- [ ] 生产环境日志不包含敏感信息

### 性能监控
- [ ] 下载速度监控正常
- [ ] 成功率统计正确
- [ ] 错误率监控正常
- [ ] 资源使用监控正常

## ✅ 最终验证

### 完整流程测试
- [ ] 从视频URL输入到下载完成的完整流程正常
- [ ] 多个视频同时下载流程正常
- [ ] 应用重启后下载任务恢复正常
- [ ] 系统重启后下载任务恢复正常

### 用户体验
- [ ] 操作响应及时
- [ ] 错误提示友好
- [ ] 进度显示准确
- [ ] 功能易于使用

---

## 🆘 常见问题解决

### 编译错误
```
错误: 找不到模块
解决: 检查settings.gradle.kts中是否正确添加模块声明
```

### 运行时错误
```
错误: VideoDownloaderApi未初始化
解决: 确保在Application.onCreate()中调用initialize()方法
```

### 权限错误
```
错误: 存储权限被拒绝
解决: 检查AndroidManifest.xml权限配置，并在运行时请求权限
```

### 网络错误
```
错误: 无法获取视频信息
解决: 检查网络连接，确认视频URL有效
```

---

**完成所有检查项后，您的视频下载模块集成就完成了！** 🎉

如有问题，请参考 [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) 获取详细说明。