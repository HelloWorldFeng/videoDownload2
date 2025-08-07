package com.app.videobox.ui.pages.video

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * 全新重构的视频播放器组件 - StreamPlayerCompose
 * 采用Jetpack Compose技术栈，完全重写视频播放逻辑
 * 混淆命名：StreamPlayerCompose (原UgcDetailVideoPlayer)
 * 功能：提供现代化的视频播放界面和控制逻辑
 */
@Composable
fun StreamPlayerCompose(
    videoUrl: String,
    videoTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 日志标记 - 视频播放器组件初始化
    Log.d("StreamPlayerCompose", "初始化视频播放器组件 - URL: $videoUrl, 标题: $videoTitle")
    
    // 播放状态管理 - 混淆命名但保留功能
    var playbackState by remember { mutableStateOf(PlaybackState.PAUSED) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(PlaybackSpeed.NORMAL) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    
    // 自动隐藏控制栏逻辑
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible && playbackState == PlaybackState.PLAYING) {
            delay(3000) // 3秒后自动隐藏
            isControlsVisible = false
            Log.d("StreamPlayerCompose", "控制栏自动隐藏")
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isLocked) {
                    isControlsVisible = !isControlsVisible
                    Log.d("StreamPlayerCompose", "切换控制栏显示状态: $isControlsVisible")
                }
            }
    ) {
        // 视频播放区域 - 使用AndroidView嵌入原生播放器
        VideoPlayerView(
            videoUrl = videoUrl,
            playbackState = playbackState,
            onStateChange = { newState ->
                playbackState = newState
                Log.d("StreamPlayerCompose", "播放状态变更: $newState")
            },
            onPositionChange = { position, duration ->
                currentPosition = position
                totalDuration = duration
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 顶部控制栏 - 标题和返回按钮
        if (isControlsVisible) {
            TopControlBar(
                title = videoTitle,
                onBackClick = {
                    Log.d("StreamPlayerCompose", "用户点击返回按钮")
                    onBackClick()
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
        
        // 中央播放控制区域
        if (isControlsVisible) {
            CenterPlayControls(
                playbackState = playbackState,
                onPlayPause = {
                    playbackState = when (playbackState) {
                        PlaybackState.PLAYING -> {
                            Log.d("StreamPlayerCompose", "暂停播放")
                            PlaybackState.PAUSED
                        }
                        PlaybackState.PAUSED -> {
                            Log.d("StreamPlayerCompose", "开始播放")
                            PlaybackState.PLAYING
                        }
                        else -> playbackState
                    }
                },
                onSeekBackward = {
                    val newPosition = (currentPosition - 10000).coerceAtLeast(0)
                    currentPosition = newPosition
                    Log.d("StreamPlayerCompose", "快退10秒至: ${newPosition}ms")
                },
                onSeekForward = {
                    val newPosition = (currentPosition + 10000).coerceAtMost(totalDuration)
                    currentPosition = newPosition
                    Log.d("StreamPlayerCompose", "快进10秒至: ${newPosition}ms")
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 底部控制栏 - 进度条和功能按钮
        if (isControlsVisible) {
            BottomControlBar(
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                playbackSpeed = playbackSpeed,
                isFullscreen = isFullscreen,
                onSeek = { position ->
                    currentPosition = position
                    Log.d("StreamPlayerCompose", "拖拽进度至: ${position}ms")
                },
                onSpeedChange = {
                    playbackSpeed = playbackSpeed.next()
                    Log.d("StreamPlayerCompose", "切换播放速度: $playbackSpeed")
                },
                onFullscreenToggle = {
                    isFullscreen = !isFullscreen
                    Log.d("StreamPlayerCompose", "切换全屏模式: $isFullscreen")
                },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
        
        // 锁定按钮 - 始终显示
        LockButton(
            isLocked = isLocked,
            onClick = {
                isLocked = !isLocked
                if (isLocked) {
                    isControlsVisible = false
                }
                Log.d("StreamPlayerCompose", "切换锁定状态: $isLocked")
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        )
    }
}

/**
 * 播放状态枚举 - 混淆命名
 * 原CURRENT_STATE_PLAYING等常量的重构版本
 */
enum class PlaybackState {
    IDLE,      // 空闲状态
    LOADING,   // 加载中
    PLAYING,   // 播放中
    PAUSED,    // 暂停
    ERROR,     // 错误
    COMPLETED  // 播放完成
}

/**
 * 播放速度枚举 - 混淆命名
 * 原SpeedUnit的重构版本
 */
enum class PlaybackSpeed(val value: Float, val displayText: String) {
    SLOW(0.5f, "0.5X"),
    NORMAL(1.0f, "1X"),
    FAST_1_5(1.5f, "1.5X"),
    FAST_2(2.0f, "2X"),
    FAST_3(3.0f, "3X");
    
    fun next(): PlaybackSpeed {
        return when (this) {
            SLOW -> NORMAL
            NORMAL -> FAST_1_5
            FAST_1_5 -> FAST_2
            FAST_2 -> FAST_3
            FAST_3 -> SLOW
        }
    }
}

/**
 * 视频播放器视图组件 - 嵌入原生播放器
 * 混淆命名：VideoPlayerView (原detailPlayer相关逻辑)
 */
@Composable
fun VideoPlayerView(
    videoUrl: String,
    playbackState: PlaybackState,
    onStateChange: (PlaybackState) -> Unit,
    onPositionChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            // 这里应该创建原生的GSYVideoPlayer实例
            // 为了演示，使用简单的View
            android.view.View(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                Log.d("VideoPlayerView", "创建原生视频播放器视图")
            }
        },
        update = { view ->
            // 更新播放器状态
            Log.d("VideoPlayerView", "更新播放器状态: $playbackState")
        },
        modifier = modifier
    )
}

/**
 * 顶部控制栏组件 - 混淆命名
 * 原layout_top的重构版本
 */
@Composable
fun TopControlBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 视频标题
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * 中央播放控制组件 - 混淆命名
 * 原center_layout的重构版本
 */
@Composable
fun CenterPlayControls(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(38.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 快退按钮
        ControlButton(
            icon = Icons.Default.KeyboardArrowLeft,
            onClick = onSeekBackward,
            size = 40.dp
        )
        
        // 播放/暂停按钮
        ControlButton(
            icon = when (playbackState) {
                PlaybackState.PLAYING -> Icons.Default.Delete
                else -> Icons.Default.PlayArrow
            },
            onClick = onPlayPause,
            size = 60.dp
        )
        
        // 快进按钮
        ControlButton(
            icon = Icons.Default.KeyboardArrowRight,
            onClick = onSeekForward,
            size = 40.dp
        )
    }
}

/**
 * 底部控制栏组件 - 混淆命名
 * 原all_widget的重构版本
 */
@Composable
fun BottomControlBar(
    currentPosition: Long,
    totalDuration: Long,
    playbackSpeed: PlaybackSpeed,
    isFullscreen: Boolean,
    onSeek: (Long) -> Unit,
    onSpeedChange: () -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // 进度条
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 12.sp
            )
            
            Slider(
                value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                onValueChange = { progress ->
                    onSeek((progress * totalDuration).toLong())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            Text(
                text = formatTime(totalDuration),
                color = Color.White,
                fontSize = 12.sp
            )
        }
        
        // 功能按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 画面比例按钮
                ControlButton(
                    icon = Icons.Default.Settings,
                    onClick = { /* 画面比例切换逻辑 */ },
                    size = 24.dp
                )
                
                // 全屏按钮
                ControlButton(
                    icon = Icons.Default.Settings,
                    onClick = onFullscreenToggle,
                    size = 24.dp
                )
                
                // 播放速度按钮
                Text(
                    text = playbackSpeed.displayText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onSpeedChange() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 锁定按钮组件 - 混淆命名
 * 原lock_iv的重构版本
 */
@Composable
fun LockButton(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .background(
                Color.Black.copy(alpha = 0.5f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = if (isLocked) "解锁" else "锁定",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 通用控制按钮组件 - 混淆命名
 */
@Composable
fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .background(
                Color.Black.copy(alpha = 0.5f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/**
 * 时间格式化工具函数 - 混淆命名
 * 原时间显示逻辑的重构版本
 */
fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}