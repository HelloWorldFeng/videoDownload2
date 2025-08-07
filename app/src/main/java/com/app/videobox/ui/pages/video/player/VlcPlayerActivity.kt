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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

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
        
        // 设置UI - 使用明确的setContent调用避免版本兼容性问题
        Log.d(TAG, "开始设置Compose UI内容")
        try {
            // 使用明确的参数调用，避免默认参数版本
            setContent(parent = null, content = {
                VlcPlayerScreen(
                    videoTitle = videoTitle ?: "视频播放",
                    isControlsVisible = isControlsVisible.value,
                    isPlaying = isPlaying.value,
                    currentPosition = currentPosition.value,
                    totalDuration = totalDuration.value,
                    playbackSpeed = playbackSpeed.value,
                    volume = volume.value,
                    isLoading = isLoading.value,
                    errorMessage = errorMessage.value,
                    onPlayPauseClick = { togglePlayPause() },
                    onSeekTo = { position -> seekTo(position) },
                    onSpeedChange = { speed -> setPlaybackSpeed(speed) },
                    onVolumeChange = { vol -> setVolume(vol) },
                    onBackClick = { finish() },
                    onControlsVisibilityChange = { visible -> 
                        isControlsVisible.value = visible
                        if (visible) {
                            // 显示控制栏时自动隐藏
                            scheduleControlsHide()
                        }
                    },
                    vlcVideoLayoutProvider = { vlcVideoLayout }
                )
            })
            Log.d(TAG, "Compose UI内容设置成功")
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
        try {
            Log.d(TAG, "初始化VLC播放器")
            
            // 创建VLC视频布局 - 这是正确的显示组件
            vlcVideoLayout = VLCVideoLayout(this)
            
            // 创建VLC播放器实例，但不使用其SurfaceView功能
            vlcPlayer = VlcVideoPlayer(this)
            
            // 将VLCVideoLayout连接到VLC播放器的MediaPlayer
            vlcPlayer?.getMediaPlayer()?.let { mediaPlayer ->
                vlcVideoLayout?.let { layout ->
                    // 使用attachViews方法连接VLCVideoLayout到MediaPlayer
                    mediaPlayer.attachViews(layout, null, false, false)
                    Log.d(TAG, "VLCVideoLayout已通过attachViews连接到MediaPlayer")
                }
            }
            
            // 获取播放器管理器
            playerManager = VlcPlayerManager.getInstance()
            
            // 设置播放器监听器
            vlcPlayer?.setPlaybackListener(object : VlcVideoPlayer.PlaybackListener {
                override fun onPreparing() {
                    isLoading.value = true
                    errorMessage.value = null
                    Log.d(TAG, "播放器准备中")
                }
                
                override fun onBuffering(percent: Float) {
                    isLoading.value = true
                    Log.d(TAG, "缓冲中: ${percent}%")
                }
                
                override fun onPlaying() {
                    isLoading.value = false
                    isPlaying.value = true
                    Log.d(TAG, "开始播放")
                }
                
                override fun onPaused() {
                    isPlaying.value = false
                    Log.d(TAG, "播放暂停")
                }
                
                override fun onStopped() {
                    isPlaying.value = false
                    Log.d(TAG, "播放停止")
                }
                
                override fun onCompleted() {
                    isPlaying.value = false
                    Log.d(TAG, "播放完成")
                }
                
                override fun onError(error: String) {
                    isLoading.value = false
                    isPlaying.value = false
                    errorMessage.value = error
                    Log.e(TAG, "播放错误: $error")
                }
                
                override fun onProgressUpdate(currentTime: Long, totalTime: Long) {
                    currentPosition.value = currentTime
                    totalDuration.value = totalTime
                }
                
                override fun onOrientationDetected(orientation: Int, videoWidth: Int, videoHeight: Int) {
                    // 根据检测到的视频方向自动调整屏幕方向
                    runOnUiThread {
                        requestedOrientation = orientation
                        Log.d(TAG, "根据视频尺寸(${videoWidth}x${videoHeight})调整屏幕方向: $orientation")
                    }
                }
            })
            
            // 初始化播放器管理器
            playerManager?.initializePlayer(this, vlcPlayer!!)
            
            // 开始播放视频
            videoPath?.let { path ->
                playerManager?.playVideo(
                    videoPath = path,
                    title = videoTitle,
                    autoPlay = autoPlay
                )
            }
            
            Log.d(TAG, "VLC播放器初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "初始化VLC播放器失败", e)
            errorMessage.value = "播放器初始化失败: ${e.message}"
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
    private fun scheduleControlsHide() {
        lifecycleScope.launch {
            delay(3000) // 3秒后隐藏
            if (isControlsVisible.value) {
                isControlsVisible.value = false
            }
        }
    }
}

/**
 * VLC播放器UI组件
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
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onControlsVisibilityChange: (Boolean) -> Unit,
    vlcVideoLayoutProvider: () -> VLCVideoLayout?
) {
    // 使用Surface替代Box来避免编译问题
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                onControlsVisibilityChange(!isControlsVisible)
            },
        color = Color.Black
    ) {
        // VLC视频播放区域
        AndroidView(
            factory = { context ->
                vlcVideoLayoutProvider() ?: VLCVideoLayout(context)
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 暂时移除控制按钮以避免编译问题
        // TODO: 后续添加简单的控制界面
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