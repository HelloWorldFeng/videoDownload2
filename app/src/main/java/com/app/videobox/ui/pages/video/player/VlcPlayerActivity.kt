package com.app.videobox.ui.pages.video.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

/**
 * VLC播放器Activity
 * 提供全屏视频播放界面和完整的播放控制功能
 * 
 * 功能特性：
 * - 全屏播放体验
 * - 播放控制界面
 * - 进度条和时间显示
 * - 音量和亮度控制
 * - 播放速度调节
 * - 手势控制支持
 * - 自动隐藏控制栏
 */
class VlcPlayerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "VlcPlayerActivity"
        private const val EXTRA_VIDEO_PATH = "extra_video_path"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"
        private const val EXTRA_AUTO_PLAY = "extra_auto_play"
        
        /**
         * 启动VLC播放器Activity
         * @param context 上下文
         * @param videoPath 视频文件路径或URL
         * @param videoTitle 视频标题
         * @param autoPlay 是否自动播放
         */
        fun start(context: Context, videoPath: String, videoTitle: String? = null, autoPlay: Boolean = true) {
            val intent = Intent(context, VlcPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_AUTO_PLAY, autoPlay)
                
                // 如果不是Activity上下文，添加NEW_TASK标志
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            Log.d(TAG, "启动VLC播放器Activity: $videoPath")
        }
    }

    // VLC播放器相关
    private var vlcPlayer: VlcVideoPlayer? = null
    private var playerManager: VlcPlayerManager? = null
    private var vlcVideoLayout: VLCVideoLayout? = null
    
    // 播放参数
    private var videoPath: String? = null
    private var videoTitle: String? = null
    private var autoPlay: Boolean = true
    
    // UI状态
    private var isControlsVisible = mutableStateOf(true)
    private var isPlaying = mutableStateOf(false)
    private var currentPosition = mutableStateOf(0L)
    private var totalDuration = mutableStateOf(0L)
    private var playbackSpeed = mutableStateOf(1.0f)
    private var volume = mutableStateOf(100)
    private var isLoading = mutableStateOf(false)
    private var errorMessage = mutableStateOf<String?>(null)
    
    // 新增功能状态
    private var isFullScreen = mutableStateOf(true) // 默认已经是全屏
    private var isLandscape = mutableStateOf(false) // 当前是否横屏
    private var isOrientationLocked = mutableStateOf(false) // 是否锁定屏幕方向
    private var showSpeedMenu = mutableStateOf(false) // 是否显示倍速菜单
    
    // 自动隐藏控制界面的任务
    private var hideControlsJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "VLC播放器Activity创建")
        
        // 获取传入参数
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, true)
        
        if (videoPath == null) {
            Log.e(TAG, "视频路径为空，关闭Activity")
            Toast.makeText(this, "视频路径无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 设置全屏模式
        setupFullScreen()
        
        // 初始化播放器
        initializePlayer()
        
        // 观察播放器管理器的状态流
        observePlayerManagerStates()
        
        // 设置UI - 使用明确的setContent调用避免版本兼容性问题
        Log.d(TAG, "开始设置Compose UI内容")
        try {
            // 使用明确的参数调用，避免默认参数版本
            setContent(parent = null, content = {
                VlcPlayerScreen(
                    videoTitle = videoTitle ?: extractFileNameFromPath(videoPath ?: ""),
                    isControlsVisible = isControlsVisible.value,
                    isPlaying = isPlaying.value,
                    currentPosition = currentPosition.value,
                    totalDuration = totalDuration.value,
                    playbackSpeed = playbackSpeed.value,
                    volume = volume.value,
                    isLoading = isLoading.value,
                    errorMessage = errorMessage.value,
                    isFullScreen = isFullScreen.value,
                    isLandscape = isLandscape.value,
                    isOrientationLocked = isOrientationLocked.value,
                    showSpeedMenu = showSpeedMenu.value,
                    onPlayPauseClick = { 
                        Log.d(TAG, "播放/暂停按钮点击")
                        togglePlayPause() 
                    },
                    onSeekTo = { position -> 
                        Log.d(TAG, "拖拽进度条到: $position")
                        seekTo(position) 
                    },
                    onSeekForward = { seekForward() },
                    onSeekBackward = { seekBackward() },
                    onSpeedChange = { speed -> setPlaybackSpeed(speed) },
                    onVolumeChange = { vol -> setVolume(vol) },
                    onBackClick = { 
                        Log.d(TAG, "返回按钮点击")
                        finish() 
                    },
                    onControlsVisibilityChange = { visible -> 
                        Log.d(TAG, "控制界面显示状态变更: $visible")
                        isControlsVisible.value = visible
                        if (visible) {
                            // 显示控制栏时自动隐藏
                            Log.d(TAG, "启动3秒自动隐藏定时器")
                            scheduleControlsHide()
                        }
                    },
                    onFullScreenToggle = { toggleFullScreen() },
                    onOrientationToggle = { toggleOrientation() },
                    onOrientationLockToggle = { toggleOrientationLock() },
                    onSpeedMenuToggle = { toggleSpeedMenu() },
                    onSpeedSelect = { speed -> setPlaybackSpeedFromMenu(speed) },
                    vlcVideoLayoutProvider = { vlcVideoLayout }
                )
            })
            Log.d(TAG, "Compose UI内容设置成功")
            
            // 启动时也启动自动隐藏定时器
            Log.d(TAG, "onCreate: 启动初始的控制界面自动隐藏定时器")
            scheduleControlsHide()
            
        } catch (e: Exception) {
            Log.e(TAG, "设置Compose UI内容失败: ${e.message}", e)
            // 降级处理：使用传统View方式
            Toast.makeText(this, "UI初始化失败，请重试", Toast.LENGTH_LONG).show()
            finish()
        }
        
        Log.d(TAG, "VLC播放器Activity初始化完成")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "VLC播放器Activity恢复")
        
        // 恢复播放
        vlcPlayer?.let { player ->
            if (player.isPlaying()) {
                isPlaying.value = true
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "VLC播放器Activity暂停")
        
        // 暂停播放
        vlcPlayer?.pause()
        isPlaying.value = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VLC播放器Activity销毁")
        
        // 取消自动隐藏任务
        hideControlsJob?.cancel()
        Log.d(TAG, "自动隐藏任务已取消")
        
        // 释放播放器资源
        playerManager?.release()
        vlcPlayer?.release()
        vlcVideoLayout = null
        
        Log.d(TAG, "VLC播放器资源释放完成")
    }
    
    /**
     * 设置全屏模式和屏幕方向
     */
    private fun setupFullScreen() {
        // 允许自动旋转，根据视频内容和设备方向自动调整
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        
        // 隐藏状态栏和导航栏
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        Log.d(TAG, "全屏模式设置完成 - 支持自动旋转")
    }
    
    /**
     * 初始化播放器
     */
    private fun initializePlayer() {
        Log.d(TAG, "开始初始化播放器")
        
        try {
            // 创建VLC视频布局 - 这是正确的显示组件
            vlcVideoLayout = VLCVideoLayout(this)
            
            // 创建VLC播放器实例
            vlcPlayer = VlcVideoPlayer(this)
            
            // 将VLCVideoLayout连接到VLC播放器的MediaPlayer
            vlcPlayer?.getMediaPlayer()?.let { mediaPlayer ->
                vlcVideoLayout?.let { layout ->
                    // 使用attachViews方法连接VLCVideoLayout到MediaPlayer
                    mediaPlayer.attachViews(layout, null, false, false)
                    Log.d(TAG, "VLCVideoLayout已通过attachViews连接到MediaPlayer")
                    
                    // 设置默认视频缩放模式为拉伸填充
                    try {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                        Log.d(TAG, "已设置默认视频缩放模式为拉伸填充")
                    } catch (e: Exception) {
                        Log.e(TAG, "设置默认视频缩放模式失败: ${e.message}", e)
                    }
                }
            }
            
            // 获取播放器管理器单例
            playerManager = VlcPlayerManager.getInstance()
            
            // 初始化播放器管理器
            playerManager?.initializePlayer(this, vlcPlayer!!)
            
            // 播放视频
            videoPath?.let { path ->
                Log.d(TAG, "开始播放视频: $path")
                playerManager?.playVideo(path, videoTitle, autoPlay)
            }
            
            Log.d(TAG, "播放器初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "播放器初始化失败", e)
            errorMessage.value = "播放器初始化失败: ${e.message}"
        }
    }
    
    /**
     * 观察播放器管理器的状态流
     */
    private fun observePlayerManagerStates() {
        Log.d(TAG, "开始观察播放器管理器状态流")
        
        // 观察播放进度
        lifecycleScope.launch {
            playerManager?.playbackProgress?.collect { progress ->
                currentPosition.value = progress.currentTime
                totalDuration.value = progress.totalTime
                Log.d(TAG, "从VlcPlayerManager获取进度: currentTime=${progress.currentTime}, totalTime=${progress.totalTime}")
            }
        }
        
        // 观察播放状态
        lifecycleScope.launch {
            playerManager?.playbackState?.collect { state ->
                when (state) {
                    VlcPlayerManager.PlaybackState.PREPARING -> {
                        isLoading.value = true
                        errorMessage.value = null
                        Log.d(TAG, "播放器状态: 准备中")
                    }
                    VlcPlayerManager.PlaybackState.BUFFERING -> {
                        isLoading.value = true
                        Log.d(TAG, "播放器状态: 缓冲中")
                    }
                    VlcPlayerManager.PlaybackState.PLAYING -> {
                        isLoading.value = false
                        isPlaying.value = true
                        Log.d(TAG, "播放器状态: 播放中 - 隐藏加载图标")
                    }
                    VlcPlayerManager.PlaybackState.PAUSED -> {
                        isLoading.value = false  // 确保暂停时也隐藏加载图标
                        isPlaying.value = false
                        Log.d(TAG, "播放器状态: 暂停 - 隐藏加载图标")
                    }
                    VlcPlayerManager.PlaybackState.STOPPED -> {
                        isLoading.value = false  // 确保停止时也隐藏加载图标
                        isPlaying.value = false
                        Log.d(TAG, "播放器状态: 停止 - 隐藏加载图标")
                    }
                    VlcPlayerManager.PlaybackState.COMPLETED -> {
                        isLoading.value = false  // 确保完成时也隐藏加载图标
                        isPlaying.value = false
                        Log.d(TAG, "播放器状态: 播放完成 - 隐藏加载图标")
                    }
                    VlcPlayerManager.PlaybackState.ERROR -> {
                        isLoading.value = false
                        isPlaying.value = false
                        errorMessage.value = "播放出现错误"
                        Log.e(TAG, "播放器状态: 错误 - 隐藏加载图标")
                    }
                    VlcPlayerManager.PlaybackState.INITIALIZED -> {
                        isLoading.value = false  // 初始化完成后隐藏加载图标
                        Log.d(TAG, "播放器状态: 已初始化 - 隐藏加载图标")
                    }
                    VlcPlayerManager.PlaybackState.IDLE -> {
                        isLoading.value = false  // 空闲状态隐藏加载图标
                        isPlaying.value = false
                        Log.d(TAG, "播放器状态: 空闲 - 隐藏加载图标")
                    }
                    else -> {
                        Log.d(TAG, "播放器状态: $state")
                    }
                }
            }
        }
        
        // 观察错误状态
        lifecycleScope.launch {
            playerManager?.errorState?.collect { error ->
                if (error != null) {
                    errorMessage.value = error
                    Log.e(TAG, "播放器错误: $error")
                }
            }
        }
    }
    
    /**
     * 切换播放/暂停状态
     */
    private fun togglePlayPause() {
        try {
            if (isPlaying.value) {
                playerManager?.pause()
                Log.d(TAG, "暂停播放")
            } else {
                playerManager?.resume()
                Log.d(TAG, "恢复播放")
            }
        } catch (e: Exception) {
            Log.e(TAG, "切换播放状态失败", e)
        }
    }
    
    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    private fun seekTo(position: Long) {
        try {
            playerManager?.seekTo(position)
            Log.d(TAG, "跳转到位置: ${position}ms")
        } catch (e: Exception) {
            Log.e(TAG, "跳转失败", e)
        }
    }
    
    /**
     * 快进10秒
     */
    private fun seekForward() {
        try {
            val newPosition = currentPosition.value + 10000 // 10秒 = 10000毫秒
            val maxPosition = totalDuration.value
            val targetPosition = if (maxPosition > 0) {
                minOf(newPosition, maxPosition)
            } else {
                newPosition
            }
            seekTo(targetPosition)
            Log.d(TAG, "快进10秒到位置: ${targetPosition}ms")
        } catch (e: Exception) {
            Log.e(TAG, "快进失败", e)
        }
    }
    
    /**
     * 快退10秒
     */
    private fun seekBackward() {
        try {
            val newPosition = currentPosition.value - 10000 // 10秒 = 10000毫秒
            val targetPosition = maxOf(newPosition, 0L)
            seekTo(targetPosition)
            Log.d(TAG, "快退10秒到位置: ${targetPosition}ms")
        } catch (e: Exception) {
            Log.e(TAG, "快退失败", e)
        }
    }
    
    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    private fun setPlaybackSpeed(speed: Float) {
        try {
            playerManager?.setPlaybackSpeed(speed)
            playbackSpeed.value = speed
            Log.d(TAG, "设置播放速度: ${speed}x")
        } catch (e: Exception) {
            Log.e(TAG, "设置播放速度失败", e)
        }
    }
    
    /**
     * 设置音量
     * @param vol 音量值
     */
    private fun setVolume(vol: Int) {
        try {
            playerManager?.setVolume(vol)
            volume.value = vol
            Log.d(TAG, "设置音量: $vol")
        } catch (e: Exception) {
            Log.e(TAG, "设置音量失败", e)
        }
    }
    
    /**
     * 定时隐藏控制栏
     */
    /**
     * 安排控制界面自动隐藏 - 3秒后执行
     * 如果已有隐藏任务在执行，会先取消之前的任务
     */
    private fun scheduleControlsHide() {
        Log.d(TAG, "scheduleControlsHide: 开始安排控制界面自动隐藏")
        
        // 取消之前的隐藏任务（如果存在）
        hideControlsJob?.cancel()
        Log.d(TAG, "scheduleControlsHide: 已取消之前的隐藏任务")
        
        // 启动新的隐藏任务
        hideControlsJob = lifecycleScope.launch {
            Log.d(TAG, "scheduleControlsHide: 开始3秒倒计时")
            delay(3000) // 3秒后隐藏
            
            Log.d(TAG, "scheduleControlsHide: 3秒倒计时结束，当前控制界面可见状态: ${isControlsVisible.value}")
            if (isControlsVisible.value) {
                Log.d(TAG, "scheduleControlsHide: 执行隐藏控制界面")
                isControlsVisible.value = false
            } else {
                Log.d(TAG, "scheduleControlsHide: 控制界面已经隐藏，无需操作")
            }
        }
        
        Log.d(TAG, "scheduleControlsHide: 新的隐藏任务已启动")
    }
    
    /**
     * 切换全屏模式
     * 注意：当前播放器默认就是全屏模式，此功能主要用于退出全屏
     */
    private fun toggleFullScreen() {
        Log.d(TAG, "切换视频拉伸模式: 当前=${isFullScreen.value}")
        try {
            vlcPlayer?.getMediaPlayer()?.let { mediaPlayer ->
                if (isFullScreen.value) {
                    // 切换到原始比例模式
                    mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_ORIGINAL
                    isFullScreen.value = false
                    Log.d(TAG, "已切换到原始比例模式")
                } else {
                    // 切换到拉伸填充模式
                    mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                    isFullScreen.value = true
                    Log.d(TAG, "已切换到拉伸填充模式")
                }
            } ?: run {
                Log.w(TAG, "MediaPlayer未初始化，无法切换视频缩放模式")
            }
        } catch (e: Exception) {
            Log.e(TAG, "切换视频缩放模式失败: ${e.message}", e)
        }
    }
    
    /**
     * 切换横竖屏方向
     */
    private fun toggleOrientation() {
        Log.d(TAG, "切换屏幕方向: 当前横屏=${isLandscape.value}, 锁定=${isOrientationLocked.value}")
        
        if (isOrientationLocked.value) {
            Log.w(TAG, "屏幕方向已锁定，无法切换")
            Toast.makeText(this, "屏幕方向已锁定", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isLandscape.value) {
            // 切换到竖屏
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isLandscape.value = false
            Log.d(TAG, "切换到竖屏模式")
        } else {
            // 切换到横屏
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            isLandscape.value = true
            Log.d(TAG, "切换到横屏模式")
        }
    }
    
    /**
     * 锁定/解锁屏幕方向
     */
    private fun toggleOrientationLock() {
        Log.d(TAG, "切换方向锁定: 当前锁定=${isOrientationLocked.value}")
        
        if (isOrientationLocked.value) {
            // 解锁方向 - 允许自动旋转
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            isOrientationLocked.value = false
            Log.d(TAG, "已解锁屏幕方向，允许自动旋转")
            Toast.makeText(this, "已解锁屏幕方向", Toast.LENGTH_SHORT).show()
        } else {
            // 锁定当前方向
            requestedOrientation = if (isLandscape.value) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            isOrientationLocked.value = true
            Log.d(TAG, "已锁定屏幕方向: ${if (isLandscape.value) "横屏" else "竖屏"}")
            Toast.makeText(this, "已锁定屏幕方向", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 设置播放倍速
     */
    private fun setPlaybackSpeedFromMenu(speed: Float) {
        Log.d(TAG, "从倍速菜单设置播放速度: $speed")
        playerManager?.setPlaybackSpeed(speed)
        playbackSpeed.value = speed
        showSpeedMenu.value = false // 关闭倍速菜单
    }
    
    /**
     * 切换倍速菜单显示状态
     */
    private fun toggleSpeedMenu() {
        Log.d(TAG, "切换倍速菜单显示: 当前=${showSpeedMenu.value}")
        showSpeedMenu.value = !showSpeedMenu.value
    }
    
    /**
     * 从文件路径中提取文件名
     * @param path 文件路径
     * @return 文件名，如果提取失败则返回"视频播放"
     */
    private fun extractFileNameFromPath(path: String): String {
        return try {
            if (path.isBlank()) {
                "视频播放"
            } else {
                // 处理网络URL
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    val fileName = path.substringAfterLast("/")
                    if (fileName.isNotBlank() && fileName.contains(".")) {
                        fileName.substringBeforeLast(".")
                    } else {
                        "在线视频"
                    }
                } else {
                    // 处理本地文件路径
                    val file = File(path)
                    val fileName = file.nameWithoutExtension
                    if (fileName.isNotBlank()) fileName else "视频播放"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "提取文件名失败: ${e.message}")
            "视频播放"
        }
    }
}

/**
 * VLC播放器UI组件 - 根据Figma设计稿重新设计的竖屏播放界面
 * 完全按照Stream Box设计稿实现，包含状态栏、播放控制、进度条等所有元素
 */
@Composable
fun VlcPlayerScreen(
    videoTitle: String,
    isControlsVisible: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    playbackSpeed: Float,
    volume: Int,
    isLoading: Boolean,
    errorMessage: String?,
    isFullScreen: Boolean,
    isLandscape: Boolean,
    isOrientationLocked: Boolean,
    showSpeedMenu: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onControlsVisibilityChange: (Boolean) -> Unit,
    onFullScreenToggle: () -> Unit,
    onOrientationToggle: () -> Unit,
    onOrientationLockToggle: () -> Unit,
    onSpeedMenuToggle: () -> Unit,
    onSpeedSelect: (Float) -> Unit,
    vlcVideoLayoutProvider: () -> VLCVideoLayout?
) {
    // 主容器 - 深色背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040C15)) // 根据Figma设计的背景色
            .clickable {
                Log.d("VlcPlayerScreen", "视频区域点击事件触发，当前控制界面可见状态: $isControlsVisible")
                val newVisibility = !isControlsVisible
                Log.d("VlcPlayerScreen", "准备切换控制界面可见状态到: $newVisibility")
                onControlsVisibilityChange(newVisibility)
            }
    ) {
        // VLC视频播放区域 - 铺满整个屏幕
        AndroidView(
            factory = { context ->
                vlcVideoLayoutProvider() ?: VLCVideoLayout(context)
            },
            modifier = Modifier
                .fillMaxSize() // 填满整个屏幕，提供沉浸式播放体验
        )
        

        
        // 播放控制界面 - 根据Figma设计实现，添加淡入淡出动画
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                videoTitle = videoTitle,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                isLoading = isLoading,
                isFullScreen = isFullScreen,
                isLandscape = isLandscape,
                isOrientationLocked = isOrientationLocked,
                showSpeedMenu = showSpeedMenu,
                onPlayPauseClick = onPlayPauseClick,
                onSeekTo = onSeekTo,
                onSeekForward = onSeekForward,
                onSeekBackward = onSeekBackward,
                onBackClick = onBackClick,
                onFullScreenToggle = onFullScreenToggle,
                onOrientationToggle = onOrientationToggle,
                onOrientationLockToggle = onOrientationLockToggle,
                onSpeedMenuToggle = onSpeedMenuToggle,
                onSpeedSelect = onSpeedSelect,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 错误提示
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}



/**
 * 播放控制覆盖层 - 根据Figma设计实现完整的播放控制界面
 */
@Composable
fun PlayerControlsOverlay(
    videoTitle: String,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    isLoading: Boolean,
    isFullScreen: Boolean,
    isLandscape: Boolean,
    isOrientationLocked: Boolean,
    showSpeedMenu: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onBackClick: () -> Unit,
    onFullScreenToggle: () -> Unit,
    onOrientationToggle: () -> Unit,
    onOrientationLockToggle: () -> Unit,
    onSpeedMenuToggle: () -> Unit,
    onSpeedSelect: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        // 顶部控制区域
        TopControlsSection(
            videoTitle = videoTitle,
            isFullScreen = isFullScreen,
            isLandscape = isLandscape,
            isOrientationLocked = isOrientationLocked,
            showSpeedMenu = showSpeedMenu,
            onBackClick = onBackClick,
            onFullScreenToggle = onFullScreenToggle,
            onOrientationToggle = onOrientationToggle,
            onOrientationLockToggle = onOrientationLockToggle,
            onSpeedMenuToggle = onSpeedMenuToggle,
            onSpeedSelect = onSpeedSelect,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp) // 顶部间距
        )
        
        // 中央播放控制区域
        CenterPlayControls(
            isPlaying = isPlaying,
            isLoading = isLoading,
            onPlayPauseClick = onPlayPauseClick,
            onSeekForward = onSeekForward,
            onSeekBackward = onSeekBackward,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // 底部控制区域
        BottomControlsSection(
            currentPosition = currentPosition,
            totalDuration = totalDuration,
            onSeekTo = onSeekTo,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp) // 为底部导航栏留空间
        )
    }
}

/**
 * 顶部控制区域 - 包含返回按钮、视频标题和新功能按钮
 */
@Composable
fun TopControlsSection(
    videoTitle: String,
    isFullScreen: Boolean,
    isLandscape: Boolean,
    isOrientationLocked: Boolean,
    showSpeedMenu: Boolean,
    onBackClick: () -> Unit,
    onFullScreenToggle: () -> Unit,
    onOrientationToggle: () -> Unit,
    onOrientationLockToggle: () -> Unit,
    onSpeedMenuToggle: () -> Unit,
    onSpeedSelect: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 主要控制行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 视频标题
            Text(
                text = videoTitle,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 功能按钮行
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 倍速播放按钮
                IconButton(
                    onClick = onSpeedMenuToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = if (showSpeedMenu) 0.4f else 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "倍速播放",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 锁定旋转按钮
                IconButton(
                    onClick = onOrientationLockToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = if (isOrientationLocked) 0.4f else 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isOrientationLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isOrientationLocked) "解锁旋转" else "锁定旋转",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 横竖屏切换按钮
                IconButton(
                    onClick = onOrientationToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.ScreenRotation else Icons.Default.ScreenLockRotation,
                        contentDescription = if (isLandscape) "切换到竖屏" else "切换到横屏",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 视频拉伸按钮
                IconButton(
                    onClick = onFullScreenToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.AspectRatio else Icons.Default.Fullscreen,
                        contentDescription = if (isFullScreen) "原始比例" else "拉伸填充",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // 倍速菜单
        if (showSpeedMenu) {
            SpeedSelectionMenu(
                onSpeedSelect = onSpeedSelect,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
            )
        }
    }
}

/**
 * 中央播放控制区域 - 包含后退10秒、播放/暂停、前进10秒
 */
@Composable
fun CenterPlayControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 后退10秒按钮
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    CircleShape
                )
                .clickable { onSeekBackward() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "后退10秒",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // 中央播放/暂停按钮 - 移除加载图标显示
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Color.White.copy(alpha = 0.9f),
                    CircleShape
                )
                .clickable { onPlayPauseClick() },
            contentAlignment = Alignment.Center
        ) {
            // 直接根据播放状态显示播放/暂停图标，不再显示加载图标
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "暂停",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // 前进10秒按钮
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    CircleShape
                )
                .clickable { onSeekForward() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "前进10秒",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 底部控制区域 - 包含进度条和时间显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControlsSection(
    currentPosition: Long,
    totalDuration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 进度条
    val progress = if (totalDuration > 0) {
        (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    var sliderPosition by remember { mutableStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentPosition, totalDuration) {
        if (!isDragging) {
            sliderPosition = progress
            Log.d("BottomControlsSection", "进度条更新: position=$currentPosition, duration=$totalDuration, progress=$progress")
        }
    }
    
    // 时间显示和进度条在同一水平线上 - 按照用户要求调整布局
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 当前时长：11px字体，白色 - 位于进度条左侧
        Text(
            text = formatTime(currentPosition),
            color = Color.White,
            fontSize = 11.sp, // 按照Figma设计图：11px
            fontWeight = FontWeight.Normal
        )
        
        // 自定义进度条样式，按照Figma设计图配置
        Slider(
            value = sliderPosition,
            onValueChange = { value ->
                isDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                val seekPosition = (sliderPosition * totalDuration).toLong()
                onSeekTo(seekPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White, // 圆点主体颜色：白色
                activeTrackColor = Color(0xFFFF6B35), // 橙色进度条
                inactiveTrackColor = Color.Gray.copy(alpha = 0.4f), // 灰色背景
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f) // 进度条占据剩余空间
                .height(1.dp), // 设置进度条高度：1px（按照Figma设计图）
            thumb = {
                // 自定义圆点样式：14px尺寸，黑色填充，4px白色边框（按照Figma设计图）
                Box(
                    modifier = Modifier
                        .size(14.dp) // 圆点尺寸：14px
                        .background(
                            Color.Black, // 填充颜色：黑色（#000000）
                            CircleShape
                        )
                        .border(
                            width = 4.dp, // 边框宽度：4px
                            color = Color.White, // 边框颜色：白色（#FFFFFF）
                            shape = CircleShape
                        )
                )
            }
        )
        
        // 总时长：11px字体，白色 - 位于进度条右侧
        Text(
            text = formatTime(totalDuration),
            color = Color.White,
            fontSize = 11.sp, // 按照Figma设计图：11px
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * 倍速选择菜单
 */
@Composable
fun SpeedSelectionMenu(
    onSpeedSelect: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedOptions = listOf(
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "1.0x",
        1.25f to "1.25x",
        1.5f to "1.5x",
        2.0f to "2.0x"
    )
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "播放速度",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            speedOptions.forEach { (speed, label) ->
                TextButton(
                    onClick = { onSpeedSelect(speed) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * 格式化时间显示
 * @param timeMs 时间（毫秒）
 * @return 格式化的时间字符串
 */
fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}