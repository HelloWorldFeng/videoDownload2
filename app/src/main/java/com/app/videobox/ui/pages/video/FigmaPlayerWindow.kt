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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * 基于Figma设计的视频播放器窗口组件
 * 设计文件：Stream Box - 视频播放窗口-竖屏备份 6
 * 功能：实现Figma设计稿中的播放器界面，集成本地播放器模块
 */
@Composable
fun FigmaPlayerWindow(
    videoUrl: String,
    videoTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("FigmaPlayerWindow", "初始化Figma播放器窗口 - URL: $videoUrl, 标题: $videoTitle")
    
    // 播放状态管理
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("00:00") }
    var totalTime by remember { mutableStateOf("00:00") }
    var progress by remember { mutableStateOf(0f) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    
    // 自动隐藏控制栏
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible && isPlaying) {
            delay(4000) // 4秒后自动隐藏
            isControlsVisible = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isLocked) {
                    isControlsVisible = !isControlsVisible
                    Log.d("FigmaPlayerWindow", "切换控制栏显示: $isControlsVisible")
                }
            }
    ) {
        // 视频播放区域 - 集成本地播放器
        VideoPlayerContainer(
            videoUrl = videoUrl,
            isPlaying = isPlaying,
            onPlayingChange = { playing ->
                isPlaying = playing
                Log.d("FigmaPlayerWindow", "播放状态变更: $playing")
            },
            onTimeUpdate = { current, total ->
                currentTime = formatFigmaTime(current)
                totalTime = formatFigmaTime(total)
                progress = if (total > 0) current.toFloat() / total else 0f
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // iPhone状态栏模拟（基于Figma设计）
        if (isControlsVisible) {
            StatusBarSection(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
            )
        }
        
        // 顶部控制栏
        if (isControlsVisible) {
            TopControlSection(
                title = videoTitle,
                onBackClick = {
                    Log.d("FigmaPlayerWindow", "用户点击返回")
                    onBackClick()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 44.dp) // 状态栏高度
            )
        }
        
        // 中央播放控制
        if (isControlsVisible) {
            CenterControlSection(
                isPlaying = isPlaying,
                onPlayPause = {
                    isPlaying = !isPlaying
                    Log.d("FigmaPlayerWindow", "播放/暂停切换: $isPlaying")
                },
                onSeekBackward = {
                    Log.d("FigmaPlayerWindow", "快退10秒")
                    // TODO: 实现快退逻辑
                },
                onSeekForward = {
                    Log.d("FigmaPlayerWindow", "快进10秒")
                    // TODO: 实现快进逻辑
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 底部控制栏
        if (isControlsVisible) {
            BottomControlSection(
                currentTime = currentTime,
                totalTime = totalTime,
                progress = progress,
                playbackSpeed = playbackSpeed,
                isFullscreen = isFullscreen,
                onProgressChange = { newProgress ->
                    progress = newProgress
                    Log.d("FigmaPlayerWindow", "进度调整: $newProgress")
                    // TODO: 实现进度跳转
                },
                onSpeedChange = {
                    playbackSpeed = when (playbackSpeed) {
                        0.5f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 0.5f
                    }
                    Log.d("FigmaPlayerWindow", "播放速度调整: $playbackSpeed")
                },
                onFullscreenToggle = {
                    isFullscreen = !isFullscreen
                    Log.d("FigmaPlayerWindow", "全屏切换: $isFullscreen")
                    // TODO: 实现全屏切换
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            )
        }
        
        // 锁定按钮（左侧）
        if (isControlsVisible || isLocked) {
            FigmaLockButton(
                isLocked = isLocked,
                onLockToggle = {
                    isLocked = !isLocked
                    Log.d("FigmaPlayerWindow", "锁定状态切换: $isLocked")
                    if (isLocked) {
                        isControlsVisible = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
            )
        }
    }
}

/**
 * iPhone状态栏模拟组件
 * 基于Figma设计中的"Bars/Status Bars/iPhone/Light"组件
 */
@Composable
fun StatusBarSection(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间显示
        Text(
            text = "9:41",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        // 右侧状态图标
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 信号强度
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "信号",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            // WiFi
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "WiFi",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            // 电池
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "电池",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 顶部控制栏组件
 * 包含返回按钮和视频标题
 */
@Composable
fun TopControlSection(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp),
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
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            maxLines = 1
        )
    }
}

/**
 * 中央播放控制组件
 * 包含播放/暂停、快退、快进按钮
 */
@Composable
fun CenterControlSection(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 快退10秒
        IconButton(
            onClick = onSeekBackward,
            modifier = Modifier
                .size(56.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "快退10秒",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // 播放/暂停按钮
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(72.dp)
                .background(
                    Color.White.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Delete else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }
        
        // 快进10秒
        IconButton(
            onClick = onSeekForward,
            modifier = Modifier
                .size(56.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "快进10秒",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 底部控制栏组件
 * 包含进度条、时间显示、播放速度、全屏按钮等
 */
@Composable
fun BottomControlSection(
    currentTime: String,
    totalTime: String,
    progress: Float,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    onProgressChange: (Float) -> Unit,
    onSpeedChange: () -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // 进度条
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Red,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        
        // 底部控制行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间显示
            Text(
                text = "$currentTime / $totalTime",
                color = Color.White,
                fontSize = 14.sp
            )
            
            // 右侧控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放速度
                TextButton(
                    onClick = onSpeedChange,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        fontSize = 14.sp
                    )
                }
                
                // 画面比例
                IconButton(
                    onClick = { /* TODO: 实现画面比例切换 */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "画面比例",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 全屏按钮
                IconButton(
                    onClick = onFullscreenToggle
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 锁定按钮组件
 */
@Composable
fun FigmaLockButton(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onLockToggle,
        modifier = modifier
            .size(48.dp)
            .background(
                Color.Black.copy(alpha = 0.5f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Settings,
            contentDescription = if (isLocked) "解锁" else "锁定",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 视频播放器容器组件
 * 集成本地播放器模块（UgcDetailVideoPlayer或StreamPlayerCompose）
 */
@Composable
fun VideoPlayerContainer(
    videoUrl: String,
    isPlaying: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    onTimeUpdate: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            // 创建UgcDetailVideoPlayer实例
            UgcDetailVideoPlayer(ctx).apply {
                Log.d("VideoPlayerContainer", "创建UgcDetailVideoPlayer实例")
                
                // 配置播放器
                setUp(videoUrl, true, "")
                
                // 设置播放状态监听
                setVideoAllCallBack(object : com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack() {
                    override fun onPrepared(url: String?, vararg objects: Any?) {
                        super.onPrepared(url, *objects)
                        Log.d("VideoPlayerContainer", "视频准备完成")
                    }
                })
            }
        },
        update = { player ->
            // 根据外部状态更新播放器
            Log.d("VideoPlayerContainer", "更新播放器状态: isPlaying=$isPlaying")
            // 简化更新逻辑，避免复杂的状态检查
            try {
                if (isPlaying) {
                    player.onVideoResume()
                } else {
                    player.onVideoPause()
                }
            } catch (e: Exception) {
                Log.e("VideoPlayerContainer", "更新播放器状态异常: ${e.message}")
            }
        },
        modifier = modifier
    )
}

/**
 * 时间格式化工具函数
 */
fun formatFigmaTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}