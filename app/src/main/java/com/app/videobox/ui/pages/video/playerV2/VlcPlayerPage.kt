package com.app.videobox.ui.pages.video.playerV2

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.media.AudioManager
import android.widget.FrameLayout
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.R
import kotlinx.coroutines.launch

// 媒体流协调器日志标签
private const val MEDIA_ORCHESTRATOR_TAG = "MediaStreamOrchestrator"

/**
 * 检测视频文件是否位于应用私有目录
 * 应用私有目录的文件访问受到Android系统限制，硬件解码器可能无法正常工作
 */
private fun isInAppPrivateDirectory(context: Context, videoPath: String): Boolean {
    val appPrivatePatterns = listOf(
        "/Android/data/${context.packageName}/",     // 应用私有数据目录
        "/Android/obb/${context.packageName}/",      // 应用私有OBB目录
        context.filesDir.absolutePath,               // 内部存储文件目录
        context.cacheDir.absolutePath,               // 内部存储缓存目录
        context.externalCacheDir?.absolutePath ?: "", // 外部存储缓存目录
    )

    val isPrivate = appPrivatePatterns.any { pattern ->
        pattern.isNotEmpty() && videoPath.contains(pattern)
    }

    Log.d(MEDIA_ORCHESTRATOR_TAG, "文件位置检测 - 路径: $videoPath")
    Log.d(MEDIA_ORCHESTRATOR_TAG, "文件位置检测 - 是否为应用私有目录: $isPrivate")

    return isPrivate
}

/**
 * VLC实例创建智能降级策略 - 专门处理应用私有目录兼容性
 */
private fun createVLCWithSmartFallback(context: Context, options: ArrayList<String>, isPrivateFile: Boolean): LibVLC {
    try {
        val libVLC = LibVLC(context, options)
        Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC实例创建成功 - ${if(isPrivateFile) "私有目录优化" else "标准"}配置")
        return libVLC
    } catch (e: Exception) {
        Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC主配置创建失败，尝试降级配置: ${e.message}")

        // 第一级降级：简化配置但保持文件位置适配
        try {
            val fallbackOptions = ArrayList<String>().apply {
                add("--no-spu")                // 禁用字幕(关键)
                add("--no-osd")                // 禁用屏幕显示

                if (isPrivateFile) {
                    add("--no-mediacodec")     // 私有目录强制软件解码
                    add("--codec=avcodec")     // 使用ffmpeg解码器
                    add("--vout=android_window") // 兼容性输出
                }

                add("--aout=opensles")         // 音频输出
                add("--verbose=1")             // 基础日志
            }
            val fallbackLibVLC = LibVLC(context, fallbackOptions)
            Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC第一级降级配置创建成功")
            return fallbackLibVLC
        } catch (fallbackException: Exception) {
            Log.w(MEDIA_ORCHESTRATOR_TAG, "第一级降级失败，尝试最小化配置: ${fallbackException.message}")

            // 第二级降级：最小化配置
            try {
                val minimalOptions = ArrayList<String>().apply {
                    add("--no-spu")            // 仅保留禁用字幕(最关键)
                    if (isPrivateFile) {
                        add("--no-mediacodec") // 私有目录必须禁用硬件解码
                    }
                }
                val minimalLibVLC = LibVLC(context, minimalOptions)
                Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC最小化配置创建成功")
                return minimalLibVLC
            } catch (minimalException: Exception) {
                Log.e(MEDIA_ORCHESTRATOR_TAG, "所有降级配置均失败，使用默认配置: ${minimalException.message}")

                // 最后尝试：完全默认配置
                val defaultLibVLC = LibVLC(context, ArrayList())
                Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC默认配置创建成功")
                return defaultLibVLC
            }
        }
    }
}

/**
 * 视频内容实体 - 用于传递视频信息的数据载体
 *
 * @param id 视频唯一标识符
 * @param preview 预览图URL地址
 * @param name 视频显示名称
 * @param video 视频流媒体地址
 * @param videoType 视频内容类型标识
 * @param videoCreationDate 视频创建日期信息
 */
data class VideoResultEntity(
    val id: Int = 0,
    val preview: String = "",
    val name: String = "",
    val video: String = "",
    val videoType: Int = 0,
    val videoCreationDate: String = ""
)

/**
 * 媒体流控制器接口 - 对外暴露播放控制能力
 *
 * 提供统一的播放控制接口，隐藏内部VLC实现细节
 * 采用接口隔离原则，只暴露必要的控制方法
 */
class VideoPlayerController(
    val setFullScreen: (Boolean) -> Unit,           // 全屏模式切换控制
    val setAspectRatio: (Float) -> Unit,           // 画面宽高比调整
    val setPlaybackSpeed: (Float) -> Unit,         // 播放速率动态调节
    val stopAndRelease: () -> Unit                 // 停止播放释放资源
)

/**
 * 媒体流协调器 - VLC架构的全新视频播放组件
 *
 * 使用VLC LibVLC作为底层播放引擎，采用策略模式设计
 * 实现了完整的手势识别、UI控制、生命周期管理等功能
 *
 * 架构特点：
 * - 状态管理：使用Compose状态机制
 * - 手势处理：自定义手势识别策略
 * - UI控制：分层UI控制逻辑
 * - 生命周期：完整的播放器生命周期管理
 */
@ExperimentalAnimationApi
@Composable
fun CustomVideoPlayer(
    video: VideoResultEntity,
    isVideoEnded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onProvideController: ((VideoPlayerController) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    MediaStreamOrchestrator(
        contentEntity = video,
        playbackCompletionHandler = isVideoEnded,
        containerModifier = modifier,
        controllerProvider = onProvideController,
        navigationCallback = onBack,
        downloadActionCallback = onDownload
    )
}

/**
 * 媒体流协调器主体实现
 *
 * 这是全新设计的VLC播放器架构，采用组合模式和策略模式
 * 与原ExoPlayer实现完全不同的内部结构
 */
@ExperimentalAnimationApi
@Composable
private fun MediaStreamOrchestrator(
    contentEntity: VideoResultEntity,
    playbackCompletionHandler: (Boolean) -> Unit,
    containerModifier: Modifier = Modifier,
    controllerProvider: ((VideoPlayerController) -> Unit)? = null,
    navigationCallback: (() -> Unit)? = null,
    downloadActionCallback: (() -> Unit)? = null
) {
    val runtimeContext = LocalContext.current
    val executionActivity = runtimeContext as? Activity
    val lifecycleMonitor = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // === 播放进度管理器初始化 ===
    val progressManager = remember { VideoProgressManager(runtimeContext) }
    var hasRestoredProgress by remember { mutableStateOf(false) }          // 进度恢复状态标记
    var lastProgressSaveTime by remember { mutableStateOf(0L) }            // 上次保存进度时间
    val snackbarHostState = remember { SnackbarHostState() }               // 用户提示状态管理

    // === 播放器核心状态管理区域 ===
    var screenDisplayMode by remember { mutableStateOf(false) }          // 全屏显示模式(false=窗口模式, true=全屏模式)
    var contentAspectRatio by remember { mutableStateOf(16f / 9f) }       // 内容宽高比例
    var playbackVelocity by remember { mutableStateOf(1.0f) }            // 播放速度系数
    var interfaceLockState by remember { mutableStateOf(false) }          // 界面锁定状态
    var orientationLandscape by remember { mutableStateOf(false) }        // 横屏模式状态
    var uiElementsVisible by remember { mutableStateOf(true) }            // UI控件显示状态
    var playbackActiveState by remember { mutableStateOf(true) }          // 播放激活状态
    var mediaCurrentPosition by remember { mutableStateOf(0L) }           // 媒体当前位置
    var mediaTotalDuration by remember { mutableStateOf(0L) }            // 媒体总时长
    var bufferingProgress by remember { mutableStateOf(0L) }              // 缓冲进度位置
    var seekOperationActive by remember { mutableStateOf(false) }         // 拖拽操作状态
    var previewSeekPosition by remember { mutableStateOf(0L) }           // 预览拖拽位置

    Log.d(MEDIA_ORCHESTRATOR_TAG, "媒体流协调器初始化 - 内容: ${contentEntity.name}")

    // === 自动隐藏控件策略实现 ===
    var lastInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var gestureProcessingActive by remember { mutableStateOf(false) }     // 手势处理激活标记
    val autoHideIntervalMs = 3000L                                        // 自动隐藏时间间隔

    /**
     * 自动隐藏控件协程策略
     * 基于用户最后交互时间和手势状态决定是否隐藏UI控件
     */
    LaunchedEffect(uiElementsVisible, interfaceLockState, gestureProcessingActive) {
        if (uiElementsVisible && !interfaceLockState && !gestureProcessingActive) {
            val operationStartTime = System.currentTimeMillis()
            lastInteractionTimestamp = operationStartTime
            Log.d(MEDIA_ORCHESTRATOR_TAG, "UI自动隐藏策略启动 - 延迟: ${autoHideIntervalMs}ms")

            while (uiElementsVisible && !interfaceLockState && !gestureProcessingActive) {
                kotlinx.coroutines.delay(500)  // 检查间隔优化
                if (System.currentTimeMillis() - lastInteractionTimestamp > autoHideIntervalMs) {
                    uiElementsVisible = false
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "UI自动隐藏策略触发 - 控件已隐藏")
                    break
                }
            }
        }
    }

    /**
     * 用户交互事件处理器
     * 统一处理所有用户交互，重置自动隐藏计时器
     */
    fun processUserInteraction(interactionSource: String = "") {
        lastInteractionTimestamp = System.currentTimeMillis()
        if (!uiElementsVisible) uiElementsVisible = true
        if (interactionSource.isNotEmpty()) {
            Log.d(MEDIA_ORCHESTRATOR_TAG, "用户交互事件: $interactionSource - 重置UI隐藏计时")
        }
    }

    // === 手势识别系统实现 ===
    var gestureIndicatorText by remember { mutableStateOf("") }
    var gestureIndicatorVisible by remember { mutableStateOf(false) }
    var gestureStartPositionX by remember { mutableStateOf(0f) }
    var gestureStartPositionY by remember { mutableStateOf(0f) }
    var currentGestureType by remember { mutableStateOf("") }            // "seek", "volume", "brightness"
    var gestureSeekStartPoint by remember { mutableStateOf(0L) }
    var gestureVolumeStartLevel by remember { mutableStateOf(0) }
    var gestureBrightnessStartLevel by remember { mutableStateOf(0f) }
    var gestureCumulativeDeltaX by remember { mutableStateOf(0f) }       // 累计水平位移
    var gestureCumulativeDeltaY by remember { mutableStateOf(0f) }       // 累计垂直位移

    // 系统服务获取
    val systemAudioManager = runtimeContext.getSystemService(Activity.AUDIO_SERVICE) as AudioManager
    val maximumVolumeLevel = systemAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val activityWindow = executionActivity?.window
    val systemContentResolver = runtimeContext.contentResolver

    // === VLC媒体播放器核心架构 ===
    val vlcLibraryInstance = remember {
        // 检测当前视频文件是否在应用私有目录
        val isPrivateFile = isInAppPrivateDirectory(runtimeContext, contentEntity.video)
        Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC实例初始化开始 - 文件位置: ${if(isPrivateFile) "应用私有目录" else "公共目录"}")

        val options = ArrayList<String>().apply {
            // === 核心界面配置 - 所有文件类型通用 ===
            add("--no-spu")                    // 禁用字幕处理(关键配置)
            add("--no-osd")                    // 禁用屏幕显示
            add("--no-video-title-show")        // 禁用视频标题显示
            add("--no-snapshot-preview")        // 禁用快照预览
            add("--no-video-on-top")           // 禁用视频置顶

            // === 根据文件位置选择解码策略 ===
            if (isPrivateFile) {
                Log.d(MEDIA_ORCHESTRATOR_TAG, "应用私有目录文件 - 使用软件解码器配置")

                // 强制软件解码 - 解决私有目录MediaCodec失败问题
                add("--no-mediacodec")             // 完全禁用MediaCodec硬件解码器
                add("--no-mediacodec-dr")          // 禁用MediaCodec直接渲染
                add("--no-omxil")                  // 禁用OMX硬件解码器
                add("--no-omxil-dr")               // 禁用OMX直接渲染
                add("--codec=avcodec")             // 强制使用ffmpeg软件解码器

                // 软件渲染配置 - 避免硬件加速问题
                add("--swscale-mode=0")            // 软件缩放模式
                add("--no-hwdec")                  // 禁用硬件解码加速
                add("--vout=android_window")       // 使用兼容性更好的输出模块

                // 私有目录专用音频配置
                add("--aout=opensles")             // OpenSL ES音频输出
                add("--no-audio-time-stretch")     // 禁用音频时间拉伸(减少复杂度)

                // 私有目录文件缓存优化
                add("--file-caching=2000")         // 增加文件缓存到2秒
                add("--network-caching=1000")       // 减少网络缓存(主要是文件访问)
                add("--live-caching=500")          // 直播缓存0.5秒

                // Surface兼容性配置 - 解决window request问题
                add("--no-overlay")                // 禁用覆盖层
                add("--no-video-deco")             // 禁用视频装饰
                add("--android-display-chroma=YV12") // 使用兼容性更好的色彩格式

            } else {
                Log.d(MEDIA_ORCHESTRATOR_TAG, "公共目录文件 - 使用硬件加速配置")

                // 硬件加速配置 - 公共目录可以正常使用
                add("--aout=opensles")              // OpenSL ES音频输出
                add("--audio-time-stretch")         // 音频时间拉伸支持

                // 硬件解码器配置 - 性能优先
                add("--no-mediacodec-dr")           // 禁用MediaCodec直接渲染(稳定性)
                add("--no-omxil-dr")               // 禁用OMX直接渲染(稳定性)
                add("--swscale-mode=0")            // 软件缩放模式(兼容性)

                // 公共目录缓存配置 - 平衡性能和兼容性
                add("--network-caching=1500")       // 网络缓存1.5秒
                add("--file-caching=1500")         // 文件缓存1.5秒
                add("--live-caching=500")          // 直播缓存0.5秒
            }

            // === 通用时钟同步配置 ===
            add("--clock-jitter=0")            // 时钟抖动控制
            add("--clock-synchro=0")           // 时钟同步控制

            // === 通用日志配置 ===
            add("--verbose=1")                 // 基础日志输出
            add("--no-stats")                  // 禁用统计信息
        }

        // 创建VLC实例 - 使用三级降级策略确保兼容性
        createVLCWithSmartFallback(runtimeContext, options, isPrivateFile)
    }

    val vlcMediaPlayerCore = remember {
        Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer核心初始化开始 - HLS流兼容性配置")
        val mediaPlayer = MediaPlayer(vlcLibraryInstance)

        // 配置播放器高级选项，针对HLS和网络流优化
        try {
            // HLS流播放优化设置
            mediaPlayer.setVideoTitleDisplay(MediaPlayer.Position.Disable, 0)
            Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer HLS优化配置应用成功")
        } catch (e: Exception) {
            Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer HLS配置应用失败，使用默认配置", e)
        }

        // 配置播放器事件监听器 - 增强错误处理
        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    playbackActiveState = true
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: 开始播放 - 流类型检测完成")
                }
                MediaPlayer.Event.Paused -> {
                    playbackActiveState = false
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: 暂停播放")
                }
                MediaPlayer.Event.Stopped -> {
                    playbackActiveState = false
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: 停止播放")
                }
                MediaPlayer.Event.EndReached -> {
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: 播放结束")
                    
                    // === 播放完成后清除进度记录 ===
                    // 当视频播放完成时，自动清除该视频的进度保存记录
                    coroutineScope.launch {
                        try {
                            Log.i(MEDIA_ORCHESTRATOR_TAG, "[进度清理] 播放完成，开始清除进度记录 - 视频: ${contentEntity.video.take(50)}...")
                            val removeResult = progressManager.removeProgress(contentEntity.video)
                            if (removeResult) {
                                Log.i(MEDIA_ORCHESTRATOR_TAG, "[进度清理] 播放完成清除成功 - 视频: ${contentEntity.video.take(50)}...")
                            } else {
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "[进度清理] 播放完成清除跳过 - 无进度记录: ${contentEntity.video.take(50)}...")
                            }
                        } catch (e: Exception) {
                            Log.e(MEDIA_ORCHESTRATOR_TAG, "[进度清理] 播放完成清除异常 - 视频: ${contentEntity.video.take(50)}..., 错误: ${e.message}", e)
                        }
                    }
                    
                    playbackCompletionHandler.invoke(true)
                }
                MediaPlayer.Event.TimeChanged -> {
                    if (!seekOperationActive) {
                        mediaCurrentPosition = event.timeChanged
                        
                        // 每30秒输出一次详细的播放状态日志
                        if (mediaCurrentPosition % 30000 < 1000) {
                            val progressPercent = if (mediaTotalDuration > 0) (mediaCurrentPosition.toFloat() / mediaTotalDuration.toFloat()) * 100 else 0f
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "[播放状态] 当前播放进度 - 位置: ${mediaCurrentPosition}ms, 总时长: ${mediaTotalDuration}ms, 进度: ${String.format("%.1f", progressPercent)}%")
                        }
                        
                        // === 播放进度自动保存逻辑 ===
                        // 当视频时长有效且播放位置有效时，异步保存播放进度
                        if (mediaTotalDuration > 0 && mediaCurrentPosition > 0) {
                            Log.v(MEDIA_ORCHESTRATOR_TAG, "[进度保存] 触发保存检查 - 位置: ${mediaCurrentPosition}ms, 时长: ${mediaTotalDuration}ms")
                            
                            coroutineScope.launch {
                                try {
                                    val saveResult = progressManager.saveProgress(
                                        videoUrl = contentEntity.video,
                                        currentPosition = mediaCurrentPosition,
                                        totalDuration = mediaTotalDuration
                                    )
                                    if (saveResult) {
                                        Log.d(MEDIA_ORCHESTRATOR_TAG, "[进度保存] 保存成功 - 位置: ${mediaCurrentPosition}ms")
                                    }
                                } catch (e: Exception) {
                                    Log.e(MEDIA_ORCHESTRATOR_TAG, "[进度保存] 保存异常 - 位置: ${mediaCurrentPosition}ms, 错误: ${e.message}", e)
                                }
                            }
                        } else {
                            Log.v(MEDIA_ORCHESTRATOR_TAG, "[进度保存] 跳过保存 - 无效参数: 位置=${mediaCurrentPosition}ms, 时长=${mediaTotalDuration}ms")
                        }
                    } else {
                        Log.v(MEDIA_ORCHESTRATOR_TAG, "[播放状态] 跳过时间更新 - 正在执行拖拽操作")
                    }
                }
                MediaPlayer.Event.LengthChanged -> {
                    mediaTotalDuration = event.lengthChanged
                    Log.i(MEDIA_ORCHESTRATOR_TAG, "[媒体信息] 时长更新 - ${mediaTotalDuration}ms (${formatTimeDisplay(mediaTotalDuration)})")
                    
                    // === 播放进度恢复逻辑 ===
                    // 当媒体时长获取成功且尚未恢复进度时，尝试恢复上次播放进度
                    if (mediaTotalDuration > 0 && !hasRestoredProgress) {
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 开始尝试恢复播放进度 - 视频: ${contentEntity.video.take(50)}...")
                        
                        coroutineScope.launch {
                            try {
                                val savedProgress = progressManager.getProgress(contentEntity.video)
                                
                                if (savedProgress != null && savedProgress.position > 0) {
                                    Log.d(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 找到保存的进度 - 位置: ${savedProgress.position}ms, 保存时长: ${savedProgress.duration}ms, 当前时长: ${mediaTotalDuration}ms")
                                    
                                    // 验证保存的进度是否有效（不超过当前视频时长）
                                    if (savedProgress.position < mediaTotalDuration) {
                                        Log.i(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 进度有效，开始恢复到位置: ${savedProgress.position}ms")
                                        
                                        mediaPlayer.time = savedProgress.position
                                        mediaCurrentPosition = savedProgress.position
                                        hasRestoredProgress = true
                                        
                                        val progressPercent = (savedProgress.position.toFloat() / mediaTotalDuration.toFloat()) * 100
                                        Log.i(MEDIA_ORCHESTRATOR_TAG, 
                                            "[进度恢复] 恢复成功 - 位置: ${formatTimeDisplay(savedProgress.position)} / ${formatTimeDisplay(mediaTotalDuration)} (${String.format("%.1f", progressPercent)}%)")
                                        
                                        // === 显示进度恢复提示 ===
                                        snackbarHostState.showSnackbar(
                                            message = "Restored to last playback position: ${formatTimeDisplay(savedProgress.position)} (${String.format("%.1f", progressPercent)}%)",
                                            duration = SnackbarDuration.Short
                                        )
                                    } else {
                                        // 如果保存的进度超过当前视频时长，清除无效进度
                                        Log.w(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 检测到无效进度 - 保存位置: ${savedProgress.position}ms > 当前时长: ${mediaTotalDuration}ms")
                                        
                                        coroutineScope.launch {
                                            try {
                                                progressManager.removeProgress(contentEntity.video)
                                                Log.i(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 无效进度已清除")
                                            } catch (e: Exception) {
                                                Log.e(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 清除无效进度失败 - 错误: ${e.message}", e)
                                            }
                                        }
                                        hasRestoredProgress = true // 标记为已处理，避免重复尝试
                                    }
                                } else {
                                    Log.i(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 未找到保存的进度记录")
                                    hasRestoredProgress = true // 标记为已处理
                                }
                            } catch (e: Exception) {
                                Log.e(MEDIA_ORCHESTRATOR_TAG, "[进度恢复] 恢复异常 - 视频: ${contentEntity.video.take(50)}..., 错误: ${e.message}", e)
                                hasRestoredProgress = true // 标记为已处理，避免重复尝试
                            }
                        }
                    }
                }
                MediaPlayer.Event.PositionChanged -> {
                    // 更新缓冲进度（VLC中用position近似表示）
                    if (mediaTotalDuration > 0) {
                        bufferingProgress = (event.positionChanged * mediaTotalDuration).toLong()
                    }
                }
                MediaPlayer.Event.EncounteredError -> {
                    Log.e(MEDIA_ORCHESTRATOR_TAG, "VLC播放错误: 播放过程中遇到错误")
                    // 尝试重新加载媒体 - 改进版本
                    try {
                        if (contentEntity.video.isNotEmpty()) {
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复: 开始重新加载媒体")

                            // 先释放当前媒体
                            try {
                                mediaPlayer.media?.let { oldMedia ->
                                    oldMedia.release()
                                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复: 旧媒体已释放")
                                }
                                mediaPlayer.media = null
                            } catch (releaseException: Exception) {
                                Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复: 旧媒体释放失败", releaseException)
                            }

                            // 创建新媒体 - 使用简化配置
                            val newMedia = Media(vlcLibraryInstance, android.net.Uri.parse(contentEntity.video))
                            mediaPlayer.media = newMedia

                            // 使用Handler延迟播放，确保媒体加载完成
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    mediaPlayer.play()
                                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复: 延迟播放启动")
                                } catch (playException: Exception) {
                                    Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复: 延迟播放失败", playException)
                                }
                            }, 200)

                            Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复: 媒体重新加载完成")
                        }
                    } catch (retryException: Exception) {
                        Log.e(MEDIA_ORCHESTRATOR_TAG, "VLC错误恢复失败", retryException)
                        playbackCompletionHandler.invoke(false)
                    }
                }
                MediaPlayer.Event.Buffering -> {
                    val bufferPercent = event.buffering
                    Log.v(MEDIA_ORCHESTRATOR_TAG, "VLC缓冲事件: ${bufferPercent}%")
                    // 可以在这里显示缓冲进度
                }
                MediaPlayer.Event.Opening -> {
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: 正在打开媒体流")
                }
                MediaPlayer.Event.MediaChanged -> {
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: 媒体内容已更改")
                }
                else -> {
                    Log.v(MEDIA_ORCHESTRATOR_TAG, "VLC播放事件: ${event.type}")
                }
            }
        }

        Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer核心创建完成 - 事件监听器配置完毕")
        mediaPlayer
    }

    // === 媒体内容加载与播放控制 - HLS流优化处理 ===
    LaunchedEffect(contentEntity.video) {
        if (contentEntity.video.isNotEmpty()) {
            Log.d(MEDIA_ORCHESTRATOR_TAG, "开始加载媒体内容: ${contentEntity.video}")

            try {
                val mediaUri = Uri.parse(contentEntity.video)
                val mediaUrl = contentEntity.video

                // 检测流类型并应用相应优化配置
                val isHlsStream = mediaUrl.contains(".m3u8") || mediaUrl.contains("hls")
                val isHttpsStream = mediaUrl.startsWith("https://")

                Log.d(MEDIA_ORCHESTRATOR_TAG, "媒体流类型分析: HLS=${isHlsStream}, HTTPS=${isHttpsStream}")

                // 创建VLC媒体对象，使用兼容性验证的选项
                val vlcMedia = Media(vlcLibraryInstance, mediaUri).apply {
                    // 为HLS流添加基础优化选项
                    if (isHlsStream) {
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "应用HLS流基础配置")
                        try {
                            addOption(":network-caching=2000")        // HLS流增加缓存
                            addOption(":live-caching=1000")           // 直播缓存
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "HLS缓存配置应用成功")
                        } catch (e: Exception) {
                            Log.w(MEDIA_ORCHESTRATOR_TAG, "HLS特殊配置应用失败，使用默认配置", e)
                        }
                    }

                    // 为网络流添加基础配置
                    if (isHttpsStream) {
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "应用HTTPS流基础配置")
                        try {
                            addOption(":network-caching=1500")       // 网络缓存
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "HTTPS缓存配置应用成功")
                        } catch (e: Exception) {
                            Log.w(MEDIA_ORCHESTRATOR_TAG, "HTTPS配置应用失败，使用默认配置", e)
                        }
                    }

                    // 通用兼容性配置 - 仅使用经过验证的选项
                    try {
                        addOption(":file-caching=1500")              // 文件缓存
                        addOption(":network-caching=1500")           // 网络缓存(通用)
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "通用缓存配置应用成功")
                    } catch (e: Exception) {
                        Log.w(MEDIA_ORCHESTRATOR_TAG, "通用配置应用失败，使用默认配置", e)
                    }

                    // 移除可能不兼容的选项:
                    // :hls-segment-threads=2     (可能不支持)
                    // :demux-filter=hls         (可能不支持)
                    // :http-reconnect           (可能不支持)
                    // :http-continuous          (可能不支持)
                    // :no-http-forward-cookies  (可能不支持)
                    // :clock-jitter=0           (已在LibVLC级别配置)
                    // :avcodec-hw=any           (可能导致兼容性问题)
                }

                // 设置媒体到播放器
                vlcMediaPlayerCore.media = vlcMedia

                Log.d(MEDIA_ORCHESTRATOR_TAG, "媒体内容加载完成，准备播放 - 流类型: ${if (isHlsStream) "HLS" else "标准"}")

                // 使用Handler延迟播放，确保媒体加载完成
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        if (playbackActiveState) {
                            vlcMediaPlayerCore.play()
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "媒体自动播放启动（延迟执行）")
                        }
                    } catch (autoPlayException: Exception) {
                        Log.w(MEDIA_ORCHESTRATOR_TAG, "媒体自动播放失败", autoPlayException)
                    }
                }, 100)

            } catch (e: Exception) {
                Log.e(MEDIA_ORCHESTRATOR_TAG, "媒体内容加载失败", e)

                // 错误恢复：尝试使用简化配置重新加载
                try {
                    Log.w(MEDIA_ORCHESTRATOR_TAG, "使用降级配置重试媒体加载")
                    val fallbackUri = Uri.parse(contentEntity.video)
                    val fallbackMedia = Media(vlcLibraryInstance, fallbackUri)
                    vlcMediaPlayerCore.media = fallbackMedia
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "降级配置媒体加载成功")
                } catch (fallbackException: Exception) {
                    Log.e(MEDIA_ORCHESTRATOR_TAG, "降级配置媒体加载也失败，进行资源清理", fallbackException)

                    // 清理可能已创建的媒体对象
                    try {
                        vlcMediaPlayerCore.media?.let { media ->
                            media.release()
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "降级失败后: 媒体对象已清理")
                        }
                        vlcMediaPlayerCore.media = null
                    } catch (cleanupException: Exception) {
                        Log.w(MEDIA_ORCHESTRATOR_TAG, "降级失败后: 媒体清理失败", cleanupException)
                    }

                    playbackCompletionHandler.invoke(false)
                }
            }
        }
    }

    // === 播放状态控制器实现 ===
    LaunchedEffect(playbackActiveState) {
        if (playbackActiveState) {
            if (!vlcMediaPlayerCore.isPlaying) {
                vlcMediaPlayerCore.play()
                Log.d(MEDIA_ORCHESTRATOR_TAG, "播放控制: 开始播放")
            }
        } else {
            if (vlcMediaPlayerCore.isPlaying) {
                vlcMediaPlayerCore.pause()
                Log.d(MEDIA_ORCHESTRATOR_TAG, "播放控制: 暂停播放")
            }
        }
    }

    // === 播放速度控制器实现 ===
    LaunchedEffect(playbackVelocity) {
        vlcMediaPlayerCore.rate = playbackVelocity
        Log.d(MEDIA_ORCHESTRATOR_TAG, "播放速度调整: ${playbackVelocity}x")
    }

    // === 屏幕方向控制器实现 ===
    LaunchedEffect(orientationLandscape) {
        executionActivity?.requestedOrientation = if (orientationLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        Log.d(MEDIA_ORCHESTRATOR_TAG, "屏幕方向切换: ${if (orientationLandscape) "横屏" else "竖屏"}")
    }

    // === 全屏模式控制器实现 ===
    LaunchedEffect(screenDisplayMode) {
        executionActivity?.let { activity ->
            if (screenDisplayMode) {
                // 进入全屏模式
                activity.window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
                // 全屏时自动切换到横屏
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Log.d(MEDIA_ORCHESTRATOR_TAG, "全屏模式: 已启用 - 隐藏系统UI并切换横屏")
            } else {
                // 退出全屏模式
                activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                // 退出全屏时恢复竖屏
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Log.d(MEDIA_ORCHESTRATOR_TAG, "全屏模式: 已退出 - 显示系统UI并恢复竖屏")
            }
        }
    }

    // === 播放器控制器接口暴露 ===
    DisposableEffect(Unit) {
        val controllerInterface =
            _root_ide_package_.com.app.videobox.ui.pages.video.playerV2.VideoPlayerController(
                setFullScreen = { fullscreenEnabled ->
                    screenDisplayMode = fullscreenEnabled
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "外部控制器: 全屏模式切换 - $fullscreenEnabled")
                },
                setAspectRatio = { ratio ->
                    contentAspectRatio = ratio
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "外部控制器: 宽高比调整 - $ratio")
                },
                setPlaybackSpeed = { speedMultiplier ->
                    playbackVelocity = speedMultiplier
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "外部控制器: 播放速度调整 - ${speedMultiplier}x")
                },
                stopAndRelease = {
                    vlcMediaPlayerCore.stop()
                    vlcMediaPlayerCore.media?.release()
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "外部控制器: 停止播放并释放资源")
                }
            )
        controllerProvider?.invoke(controllerInterface)
        onDispose { }
    }

    // === 生命周期监听器实现 ===
    DisposableEffect(lifecycleMonitor) {
        val lifecycleObserver = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (!vlcMediaPlayerCore.isPlaying && playbackActiveState) {
                            vlcMediaPlayerCore.play()
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_START - 恢复播放")
                        }
                    }
                    Lifecycle.Event.ON_STOP -> {
                        if (vlcMediaPlayerCore.isPlaying) {
                            vlcMediaPlayerCore.pause()
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_STOP - 暂停播放")
                        }
                        
                        // === 生命周期暂停时保存播放进度 ===
                        if (mediaTotalDuration > 0 && mediaCurrentPosition > 0) {
                            coroutineScope.launch {
                                try {
                                    val saved = progressManager.saveProgress(
                                        videoUrl = contentEntity.video,
                                        currentPosition = mediaCurrentPosition,
                                        totalDuration = mediaTotalDuration
                                    )
                                    if (saved) {
                                        Log.d(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_STOP - 播放进度已保存")
                                    }
                                } catch (e: Exception) {
                                    Log.w(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_STOP - 进度保存失败", e)
                                }
                            }
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        // === 生命周期销毁时最终保存播放进度 ===
                        if (mediaTotalDuration > 0 && mediaCurrentPosition > 0) {
                            coroutineScope.launch {
                                try {
                                    val saved = progressManager.saveProgress(
                                        videoUrl = contentEntity.video,
                                        currentPosition = mediaCurrentPosition,
                                        totalDuration = mediaTotalDuration
                                    )
                                    if (saved) {
                                        Log.d(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_DESTROY - 最终进度已保存")
                                    }
                                } catch (e: Exception) {
                                    Log.w(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_DESTROY - 最终进度保存失败", e)
                                }
                            }
                        }
                        
                        vlcMediaPlayerCore.stop()
                        vlcMediaPlayerCore.media?.release()
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "生命周期: ON_DESTROY - 释放播放器资源")
                    }
                    else -> {}
                }
            }
        }
        lifecycleMonitor.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleMonitor.lifecycle.removeObserver(lifecycleObserver)
            Log.d(MEDIA_ORCHESTRATOR_TAG, "移除生命周期监听器")
        }
    }

    // === 主UI容器与手势处理系统 ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(uiElementsVisible, interfaceLockState) {
                detectTapGestures(
                    onTap = {
                        processUserInteraction("视频区域点击")
                        if (!interfaceLockState) {
                            uiElementsVisible = !uiElementsVisible
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "点击手势: UI控件显示状态切换为 $uiElementsVisible")
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        gestureStartPositionX = startOffset.x
                        gestureStartPositionY = startOffset.y
                        currentGestureType = ""
                        gestureIndicatorVisible = false
                        gestureCumulativeDeltaX = 0f
                        gestureCumulativeDeltaY = 0f
                        gestureProcessingActive = true

                        val screenWidth = size.width
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "手势开始: 位置(${startOffset.x}, ${startOffset.y})")

                        // 手势类型识别策略
                        when {
                            startOffset.x < screenWidth * 0.2f -> {
                                currentGestureType = "brightness"
                                gestureBrightnessStartLevel = try {
                                    Settings.System.getInt(systemContentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                                } catch (e: Exception) { 0.5f }
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "手势类型: 亮度调节 - 起始值: $gestureBrightnessStartLevel")
                            }
                            startOffset.x > screenWidth * 0.8f -> {
                                currentGestureType = "volume"
                                gestureVolumeStartLevel = systemAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "手势类型: 音量调节 - 起始值: $gestureVolumeStartLevel")
                            }
                            else -> {
                                currentGestureType = "seek"
                                gestureSeekStartPoint = mediaCurrentPosition
                                previewSeekPosition = mediaCurrentPosition
                                seekOperationActive = true
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "手势类型: 进度调节 - 起始位置: ${gestureSeekStartPoint}ms")
                            }
                        }

                        if (!uiElementsVisible) uiElementsVisible = true
                    },
                    onDrag = { change, dragAmount ->
                        val deltaX = dragAmount.x
                        val deltaY = dragAmount.y

                        when (currentGestureType) {
                            "seek" -> {
                                gestureCumulativeDeltaX += deltaX
                                val seekOffsetMs = (gestureCumulativeDeltaX / size.width * mediaTotalDuration).toLong()
                                val newPosition = (gestureSeekStartPoint + seekOffsetMs).coerceIn(0L, mediaTotalDuration)
                                previewSeekPosition = newPosition

                                gestureIndicatorText = if (seekOffsetMs > 0) {
                                    "Fast forward ${seekOffsetMs / 1000} seconds"
                                } else {
                                    "Fast retreat for ${-seekOffsetMs / 1000} seconds"
                                }
                                gestureIndicatorVisible = true
                                Log.v(MEDIA_ORCHESTRATOR_TAG, "进度手势: 偏移=${seekOffsetMs}ms, 新位置=${newPosition}ms")
                            }
                            "volume" -> {
                                gestureCumulativeDeltaY += deltaY
                                val volumeChangePercent = (-gestureCumulativeDeltaY / size.height).coerceIn(-1f, 1f)
                                val newVolumeLevel = (gestureVolumeStartLevel + volumeChangePercent * maximumVolumeLevel)
                                    .toInt().coerceIn(0, maximumVolumeLevel)

                                systemAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolumeLevel, 0)
                                gestureIndicatorText = "音量 ${(newVolumeLevel * 100 / maximumVolumeLevel)}%"
                                gestureIndicatorVisible = true
                                Log.v(MEDIA_ORCHESTRATOR_TAG, "音量手势: 新音量=$newVolumeLevel")
                            }
                            "brightness" -> {
                                gestureCumulativeDeltaY += deltaY
                                val brightnessChangePercent = (-gestureCumulativeDeltaY / size.height).coerceIn(-1f, 1f)
                                val newBrightnessLevel = (gestureBrightnessStartLevel + brightnessChangePercent).coerceIn(0f, 1f)

                                try {
                                    val windowLayoutParams = activityWindow?.attributes
                                    windowLayoutParams?.let {
                                        it.screenBrightness = newBrightnessLevel
                                        activityWindow.attributes = it
                                    }
                                } catch (e: Exception) {
                                    Log.w(MEDIA_ORCHESTRATOR_TAG, "亮度调节失败", e)
                                }

                                gestureIndicatorText = "亮度 ${(newBrightnessLevel * 100).toInt()}%"
                                gestureIndicatorVisible = true
                                Log.v(MEDIA_ORCHESTRATOR_TAG, "亮度手势: 新亮度=$newBrightnessLevel")
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentGestureType == "seek") {
                            vlcMediaPlayerCore.time = previewSeekPosition
                            seekOperationActive = false
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "进度手势结束: 跳转到位置 ${previewSeekPosition}ms")
                        }

                        gestureIndicatorVisible = false
                        gestureCumulativeDeltaX = 0f
                        gestureCumulativeDeltaY = 0f
                        gestureProcessingActive = false
                        currentGestureType = ""

                        processUserInteraction("手势操作结束")
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "手势操作结束")
                    },
                    onDragCancel = {
                        if (currentGestureType == "seek") {
                            seekOperationActive = false
                        }

                        gestureIndicatorVisible = false
                        gestureCumulativeDeltaX = 0f
                        gestureCumulativeDeltaY = 0f
                        gestureProcessingActive = false
                        currentGestureType = ""

                        processUserInteraction("手势操作取消")
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "手势操作取消")
                    }
                )
            }
    ) {
        // === VLC视频渲染画面 - Android兼容性优化 ===
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { context ->
                Log.d(MEDIA_ORCHESTRATOR_TAG, "创建VLC视频渲染视图 - Android兼容性配置")

                try {
                    val vlcVideoLayout = VLCVideoLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Android视频渲染优化配置
                        setKeepScreenOn(true)                    // 保持屏幕常亮
                        isFocusable = false                      // 禁用焦点(避免窗口请求)
                        isFocusableInTouchMode = false          // 禁用触摸模式焦点
                        isClickable = false                      // 禁用点击事件
                    }

                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC视频布局创建成功，准备关联MediaPlayer")

                    // 将VLC MediaPlayer与视频布局关联 - 优化参数配置
                    // 参数说明：
                    // vlcVideoLayout: 视频渲染布局
                    // null: 禁用字幕显示布局(解决字幕Surface错误)
                    // false: 禁用字幕启用标志
                    // false: 禁用视频自动调整大小
                    vlcMediaPlayerCore.attachViews(vlcVideoLayout, null, false, false)

                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer与视频布局关联成功")

                    vlcVideoLayout

                } catch (e: Exception) {
                    Log.e(MEDIA_ORCHESTRATOR_TAG, "VLC视频渲染视图创建失败", e)

                    // 降级处理：创建简单的FrameLayout作为占位符
                    val fallbackLayout = FrameLayout(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }

                    Log.w(MEDIA_ORCHESTRATOR_TAG, "使用降级视频布局")
                    fallbackLayout
                }
            },
            update = { vlcVideoLayout ->
                Log.v(MEDIA_ORCHESTRATOR_TAG, "更新VLC视频渲染视图 - 检查布局状态")

                // 检查视频布局是否仍然正确关联
                try {
                    if (vlcVideoLayout is VLCVideoLayout) {
                        // 确保视频布局保持正确的显示状态
                        vlcVideoLayout.setKeepScreenOn(true)
                        Log.v(MEDIA_ORCHESTRATOR_TAG, "VLC视频布局状态检查完成")
                    }
                } catch (e: Exception) {
                    Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC视频布局状态检查失败", e)
                }
            }
        )

        // === UI控件显示条件控制 ===
        if (uiElementsVisible) {
            // 透明交互层 - 处理控件区域外的点击
            Box(
                Modifier
                    .matchParentSize()
                    .singClick {
                        uiElementsVisible = false
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "透明交互层点击: 隐藏UI控件")
                    }
            )

            // === 顶部导航栏区域 ===
            if (!interfaceLockState) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        // 返回按钮
                        AsyncImageImpl(
                            modifier = Modifier.size(34.dp).singClick {
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "顶部导航: 返回按钮点击")
                                navigationCallback?.invoke()
                            },
                            model = R.drawable.icon_back_1,
                            contentDescription = null
                        )

                        // 视频标题显示
                        Text(
                            text = contentEntity.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // 播放速度选择器
                        var speedMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            AsyncImageImpl(
                                modifier = Modifier.size(26.dp).singClick{
                                    speedMenuExpanded = true
                                    Log.d(MEDIA_ORCHESTRATOR_TAG, "底部控制: 播放速度菜单展开")
                                },
                                model = R.drawable.icon_speed_menu
                            )
                            DropdownMenu(
                                expanded = speedMenuExpanded,
                                onDismissRequest = { speedMenuExpanded = false }
                            ) {
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speedOption ->
                                    DropdownMenuItem(
                                        text = { Text("${speedOption}x") },
                                        onClick = {
                                            playbackVelocity = speedOption
                                            speedMenuExpanded = false
                                            Log.d(MEDIA_ORCHESTRATOR_TAG, "播放速度选择: ${speedOption}x")
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        // 界面锁定按钮
                        AsyncImageImpl(
                            modifier = Modifier.size(26.dp).singClick{
                                interfaceLockState = true
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "底部控制: 界面锁定激活")
                            },
                            model = R.drawable.icon_lock
                        )
                    }

                }

            }

            // === 中央控制按钮区域 ===
            if (!interfaceLockState) {
                Row(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 快退按钮
                    AsyncImageImpl(
                        modifier = Modifier.size(40.dp)
                            .padding(5.dp)
                            .singClick{
                                val newPosition = (mediaCurrentPosition - 10000).coerceAtLeast(0L)
                                vlcMediaPlayerCore.time = newPosition
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "中央控制: 快退10秒 -> ${newPosition}ms")
                            },
                        model = R.drawable.icon_quick_retreat,
                    )

                    Spacer(Modifier.width(16.dp))

                    // 播放/暂停主按钮
                    IconButton(
                        onClick = {
                            playbackActiveState = !playbackActiveState
                            Log.d(MEDIA_ORCHESTRATOR_TAG, "中央控制: 播放状态切换 -> $playbackActiveState")
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            if (playbackActiveState) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放/暂停",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }


                    Spacer(Modifier.width(16.dp))

                    // 快进按钮
                    AsyncImageImpl(
                        modifier = Modifier.size(40.dp)
                            .padding(5.dp)
                            .singClick{
                                val newPosition = (mediaCurrentPosition + 10000).coerceAtMost(mediaTotalDuration)
                                vlcMediaPlayerCore.time = newPosition
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "中央控制: 快进10秒 -> ${newPosition}ms")
                            },
                        model = R.drawable.icon_quick_fast,
                    )
                }
            }

            // === 中央时间显示区域 ===
            if (!interfaceLockState) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${formatTimeDisplay(if (seekOperationActive) previewSeekPosition else mediaCurrentPosition)} / ${formatTimeDisplay(mediaTotalDuration)}",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            // === 底部控制栏区域 ===
            if (!interfaceLockState) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // 底部功能按钮行
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    )
                    {
                        // 横竖屏切换按钮
                        AsyncImageImpl(
                            modifier = Modifier.size(26.dp).singClick{
                                orientationLandscape = !orientationLandscape
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "底部控制: 屏幕方向切换 -> ${if (orientationLandscape) "横屏" else "竖屏"}")
                            },
                            model = R.drawable.icon_screen_land
                        )

                        Spacer(Modifier.width(10.dp))
                        // 全屏模式按钮
//                        AsyncImageImpl(
//                            modifier = Modifier.size(26.dp).singClick{
//                                screenDisplayMode = !screenDisplayMode
//                                Log.d(MEDIA_ORCHESTRATOR_TAG, "底部控制: 全屏模式切换 -> ${if (screenDisplayMode) "全屏" else "窗口"}")
//                            },
//                            model = if (screenDisplayMode) R.drawable.icon_full_screen else R.drawable.icon_full_screen
//                        )

                    }
                    // 进度条控制
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimeDisplay(if (seekOperationActive) previewSeekPosition else mediaCurrentPosition),
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        androidx.compose.material.Slider(
                            value = if (mediaTotalDuration > 0) {
                                (if (seekOperationActive) previewSeekPosition else mediaCurrentPosition) / mediaTotalDuration.toFloat()
                            } else 0f,
                            onValueChange = { progressRatio ->
                                val targetPosition = (progressRatio * mediaTotalDuration).toLong()
                                vlcMediaPlayerCore.time = targetPosition
                                Log.d(MEDIA_ORCHESTRATOR_TAG, "进度条控制: 跳转到 ${targetPosition}ms")
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material.SliderDefaults.colors(
                                thumbColor = Color(0xFFFC7A46),
                                activeTrackColor = Color.White
                            )
                        )

                        Text(
                            text = formatTimeDisplay(mediaTotalDuration),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }

            // === 界面锁定状态下的解锁按钮 ===
            if (interfaceLockState) {
                IconButton(
                    onClick = {
                        interfaceLockState = false
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "界面锁定: 解锁成功")
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(24.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = "解锁", tint = Color.White)
                }
            }
        } else {
            // UI隐藏状态下的点击恢复区域
            Box(
                Modifier
                    .matchParentSize()
                    .singClick {
                        uiElementsVisible = true
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "隐藏状态点击: 恢复UI控件显示")
                    }
            )
        }

        // === 手势提示显示区域 ===
        if (gestureIndicatorVisible) {
            Box(Modifier.align(Alignment.Center)) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = CircleShape
                ) {
                    Text(
                        text = gestureIndicatorText,
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
        
        // === 进度恢复提示显示区域 ===
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // === 资源释放与清理 - 改进版本 ===
    DisposableEffect(Unit) {
        onDispose {
            Log.d(MEDIA_ORCHESTRATOR_TAG, "组件销毁: 开始VLC资源清理流程")

            try {
                // 第一步：停止播放
                if (vlcMediaPlayerCore.isPlaying) {
                    vlcMediaPlayerCore.stop()
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC播放器已停止")
                }

                // 第二步：分离视图
                try {
                    vlcMediaPlayerCore.detachViews()
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC视图已分离")
                } catch (e: Exception) {
                    Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC视图分离失败", e)
                }

                // 第三步：释放媒体
                try {
                    vlcMediaPlayerCore.media?.let { media ->
                        media.release()
                        Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC媒体对象已释放")
                    }
                    vlcMediaPlayerCore.media = null
                } catch (e: Exception) {
                    Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC媒体释放失败", e)
                }

                // 第四步：释放播放器
                try {
                    vlcMediaPlayerCore.release()
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer已释放")
                } catch (e: Exception) {
                    Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC MediaPlayer释放失败", e)
                }

                // 第五步：释放LibVLC实例（最后）
                try {
                    vlcLibraryInstance.release()
                    Log.d(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC实例已释放")
                } catch (e: Exception) {
                    Log.w(MEDIA_ORCHESTRATOR_TAG, "VLC LibVLC实例释放失败", e)
                }

                Log.d(MEDIA_ORCHESTRATOR_TAG, "组件销毁: VLC资源清理流程完成")

            } catch (e: Exception) {
                Log.e(MEDIA_ORCHESTRATOR_TAG, "VLC资源清理过程中发生错误", e)
            }
        }
    }
}



/**
 * 时间格式化显示工具
 * 将毫秒时间戳转换为可读的时间格式
 */
private fun formatTimeDisplay(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}