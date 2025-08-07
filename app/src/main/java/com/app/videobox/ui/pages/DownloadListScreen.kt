package com.app.videobox.ui.pages

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.pages.video.VideoPlayerManager
import com.videodownloader.module.api.*
import com.app.videobox.ui.pages.video.player.VlcVideoPlayer
import com.app.videobox.ui.pages.video.player.VlcPlayerManager
import com.app.videobox.ui.pages.video.player.VlcPlayerActivity
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers

/**
 * 下载列表页面数据模型
 * 包含下载项的基本信息
 */
data class DownloadItem(
    val id: String,
    val fileName: String,
    val fileSize: String,
    val downloadTime: String,
    val thumbnailUrl: String? = null,
    val isSelected: Boolean = false
)

/**
 * 下载列表页面状态管理
 * 控制编辑模式和选中状态
 */
data class DownloadListState(
    val isEditMode: Boolean = false,
    val selectedItems: Set<String> = emptySet(),
    val downloadItems: List<DownloadItem> = emptyList()
)

/**
 * 视频下载列表页面
 * 显示所有视频下载任务的状态和进度
 */
class VideoDownloadListScreen : Screen {
    
    companion object {
        private const val TAG = "VideoDownloadListScreen"
    }
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        
        Log.d(TAG, "VideoDownloadListScreen Content() 开始渲染")
        
        // 下载任务列表状态
        var downloadTasks by remember { mutableStateOf<List<DownloadTask>>(emptyList()) }
        
        // 获取下载API实例
        val downloadApi = remember {
            try {
                val api = VideoDownloaderApi.getInstance()
                Log.d(TAG, "成功获取VideoDownloaderApi实例")
                api
            } catch (e: Exception) {
                Log.e(TAG, "获取VideoDownloaderApi实例失败", e)
                null
            }
        }
        
        // 实时更新任务列表的函数，确保在主线程中更新UI状态
        val updateTaskList = remember {
            {
                downloadApi?.let { api ->
                    // 确保在主线程中更新状态
                    context.lifecycleScope.launch(Dispatchers.Main.immediate) {
                        try {
                            val newTasks = api.getAllDownloadTasks()
                            Log.d(TAG, "实时更新任务列表，当前任务数量: ${newTasks.size}")
                            
                            // 检查任务是否有变化
                            val hasChanges = newTasks.size != downloadTasks.size || 
                                (newTasks.isNotEmpty() && downloadTasks.isNotEmpty() && 
                                 newTasks.zip(downloadTasks).any { (new, old) ->
                                     new.progress != old.progress || new.status != old.status || new.downloadSpeed != old.downloadSpeed
                                 })
                            
                            if (hasChanges || downloadTasks.isEmpty()) {
                                Log.d(TAG, "检测到任务状态变化或首次加载，更新UI")
                                downloadTasks = newTasks
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "更新任务列表失败", e)
                        }
                    }
                }
            }
        }
        
        // 定时器实现实时更新，确保网络进度条动态刷新
        LaunchedEffect(downloadApi) {
            downloadApi?.let {
                var forceUpdateCounter = 0
                while (true) {
                    // 检查是否有正在下载的任务
                    val hasDownloadingTasks = downloadTasks.any { task -> 
                        task.status == DownloadStatus.DOWNLOADING 
                    }
                    
                    // 每10秒强制更新一次，或者有下载任务时每500ms更新
                    if (hasDownloadingTasks || forceUpdateCounter >= 20) {
                        Log.v(TAG, "定时器触发进度更新，正在下载任务数: ${downloadTasks.count { it.status == DownloadStatus.DOWNLOADING }}, 强制更新: ${forceUpdateCounter >= 20}")
                        updateTaskList()
                        forceUpdateCounter = 0
                    } else {
                        forceUpdateCounter++
                    }
                    
                    // 每500ms检查一次，确保进度条流畅更新
                    delay(500)
                }
            }
        }
        
        // 注册下载回调
        DisposableEffect(downloadApi) {
            val callback = downloadApi?.let { api ->
                Log.d(TAG, "开始注册下载回调")
                
                val downloadCallback = object : DownloadCallback {
                    override fun onDownloadStarted(task: DownloadTask) {
                        Log.d(TAG, "下载开始: ${task.videoInfo.title} (ID: ${task.id})")
                        updateTaskList()
                    }
                    
                    override fun onDownloadProgress(task: DownloadTask, progress: Int, speed: Long) {
                        Log.v(TAG, "下载进度更新: ${task.videoInfo.title} - ${progress}% (${speed}B/s)")
                        // 立即更新UI，确保进度条实时响应
                        updateTaskList()
                    }
                    
                    override fun onDownloadCompleted(task: DownloadTask) {
                        Log.d(TAG, "下载完成: ${task.videoInfo.title} (ID: ${task.id})")
                        updateTaskList()
                    }
                    
                    override fun onDownloadFailed(task: DownloadTask, error: String) {
                        Log.e(TAG, "下载失败: ${task.videoInfo.title} (ID: ${task.id}) - 错误: $error")
                        updateTaskList()
                    }
                    
                    override fun onDownloadPaused(task: DownloadTask) {
                        Log.d(TAG, "下载暂停: ${task.videoInfo.title} (ID: ${task.id})")
                        updateTaskList()
                    }
                    
                    override fun onDownloadResumed(task: DownloadTask) {
                        Log.d(TAG, "下载恢复: ${task.videoInfo.title} (ID: ${task.id})")
                        updateTaskList()
                    }
                    
                    override fun onDownloadCancelled(task: DownloadTask) {
                        Log.d(TAG, "下载取消: ${task.videoInfo.title} (ID: ${task.id})")
                        updateTaskList()
                    }
                }
                
                api.registerDownloadCallback(downloadCallback)
                Log.d(TAG, "下载回调注册完成")
                
                // 初始加载任务列表，确保在主线程中更新状态
                context.lifecycleScope.launch(Dispatchers.Main.immediate) {
                    val initialTasks = api.getAllDownloadTasks()
                    Log.d(TAG, "初始加载任务列表，任务数量: ${initialTasks.size}")
                    downloadTasks = initialTasks
                }
                
                downloadCallback
            }
            
            onDispose {
                callback?.let { cb ->
                    Log.d(TAG, "清理下载回调")
                    downloadApi?.unregisterDownloadCallback(cb)
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 标题栏
            TitleBar(
                title = "下载列表",
                onBack = { navigator.pop() }
            )
            
            if (downloadApi == null) {
                // API未初始化的提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "视频下载模块未初始化",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            } else if (downloadTasks.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无下载任务",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "在主页搜索框输入视频链接开始下载",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // 下载任务列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloadTasks, key = { it.id }) { task ->
                        VideoDownloadTaskItem(
                            task = task,
                            onPauseClick = {
                                Log.d(TAG, "用户点击暂停按钮: ${task.videoInfo.title} (ID: ${task.id})")
                                if (task.status == DownloadStatus.DOWNLOADING) {
                                    context.lifecycleScope.launch {
                                        try {
                                            downloadApi.pauseDownload(task.id)
                                            Log.d(TAG, "暂停下载请求已发送: ${task.id}")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "暂停下载失败: ${task.id}", e)
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "任务状态不允许暂停: ${task.status}")
                                }
                            },
                            onResumeClick = {
                                Log.d(TAG, "用户点击恢复按钮: ${task.videoInfo.title} (ID: ${task.id})")
                                if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.FAILED) {
                                    context.lifecycleScope.launch {
                                        try {
                                            downloadApi.resumeDownload(task.id)
                                            Log.d(TAG, "恢复下载请求已发送: ${task.id}")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "恢复下载失败: ${task.id}", e)
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "任务状态不允许恢复: ${task.status}")
                                }
                            },
                            onCancelClick = {
                                Log.d(TAG, "用户点击取消按钮: ${task.videoInfo.title} (ID: ${task.id})")
                                if (task.status != DownloadStatus.COMPLETED) {
                                    context.lifecycleScope.launch {
                                        try {
                                            downloadApi.cancelDownload(task.id)
                                            Log.d(TAG, "取消下载请求已发送: ${task.id}")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "取消下载失败: ${task.id}", e)
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "已完成的任务无法取消")
                                }
                            },
                            onPlayClick = {
                                Log.d(TAG, "用户点击播放按钮: ${task.videoInfo.title} (ID: ${task.id})")
                                if (task.status == DownloadStatus.COMPLETED) {
                                    try {
                                        // 检查文件是否存在
                                        val videoFile = File(task.filePath)
                                        if (videoFile.exists()) {
                                            Log.d(TAG, "使用VLC播放器播放视频: ${task.filePath}")
                                            
                                            // 启动VLC播放器Activity
                                            VlcPlayerActivity.start(
                                                context = context,
                                                videoPath = task.filePath,
                                                videoTitle = task.videoInfo.title,
                                                autoPlay = true
                                            )
                                            
                                            Log.d(TAG, "VLC播放器Activity启动成功: ${task.videoInfo.title}")
                                        } else {
                                            Log.e(TAG, "视频文件不存在: ${task.filePath}")
                                            // 回退到原有播放器
                                            try {
                                                Log.d(TAG, "回退到原有播放器: ${task.filePath}")
                                                VideoPlayerManager.launchVideoPlayer(
                                                    context = context,
                                                    videoUrl = task.filePath,
                                                    videoTitle = task.videoInfo.title
                                                )
                                            } catch (fallbackException: Exception) {
                                                Log.e(TAG, "原有播放器也启动失败: ${task.id}", fallbackException)
                                                // TODO: 显示错误提示给用户
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "启动VLC播放器失败: ${task.id}", e)
                                        // 回退到原有播放器
                                        try {
                                            Log.d(TAG, "回退到原有播放器: ${task.filePath}")
                                            VideoPlayerManager.launchVideoPlayer(
                                                context = context,
                                                videoUrl = task.filePath,
                                                videoTitle = task.videoInfo.title
                                            )
                                        } catch (fallbackException: Exception) {
                                            Log.e(TAG, "原有播放器也启动失败: ${task.id}", fallbackException)
                                            // TODO: 显示错误提示给用户
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "任务未完成，无法播放: ${task.status}")
                                }
                            },
                            onRetryClick = {
                                Log.d(TAG, "用户点击重新下载按钮: ${task.videoInfo.title} (ID: ${task.id})")
                                if (task.status == DownloadStatus.FAILED) {
                                    context.lifecycleScope.launch {
                                        try {
                                            downloadApi.retryDownload(task.id)
                                            Log.d(TAG, "重新下载请求已发送: ${task.id}")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "重新下载失败: ${task.id}", e)
                                        }
                                    }
                                } else {
                                    Log.w(TAG, "任务状态不允许重新下载: ${task.status}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 视频下载任务项组件
 */
@Composable
private fun VideoDownloadTaskItem(
    task: DownloadTask,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    // 添加日志记录任务渲染
    Log.v("VideoDownloadTaskItem", "渲染任务项: ${task.videoInfo.title} - 状态: ${task.status} - 进度: ${task.progress}%")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 视频标题
            Text(
                text = task.videoInfo.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 格式和质量信息
            Text(
                text = "${task.format.quality} • ${task.format.ext.uppercase()}",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = task.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when (task.status) {
                    DownloadStatus.DOWNLOADING -> Color(0xFF4CAF50)
                    DownloadStatus.COMPLETED -> Color(0xFF2196F3)
                    DownloadStatus.FAILED -> Color(0xFFF44336)
                    DownloadStatus.PAUSED -> Color(0xFFFF9800)
                    else -> Color.Gray
                },
                trackColor = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 状态和操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态信息
                Column {
                    Text(
                        text = getVideoDownloadStatusText(task.status),
                        color = getVideoDownloadStatusColor(task.status),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (task.status == DownloadStatus.DOWNLOADING) {
                        Text(
                            text = "${task.progress}% • ${formatDownloadSpeed(task.downloadSpeed)}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "${task.progress}%",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // 操作按钮
                Row {
                    when (task.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(
                                onClick = {
                                    Log.d("VideoDownloadTaskItem", "暂停按钮被点击: ${task.id}")
                                    onPauseClick()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "暂停",
                                    tint = Color.White
                                )
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(
                                onClick = {
                                    Log.d("VideoDownloadTaskItem", "恢复按钮被点击: ${task.id}")
                                    onResumeClick()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "继续",
                                    tint = Color.White
                                )
                            }
                        }
                        DownloadStatus.FAILED -> {
                            // 失败状态显示重新下载按钮
                            IconButton(
                                onClick = {
                                    Log.d("VideoDownloadTaskItem", "重新下载按钮被点击: ${task.id}")
                                    onRetryClick()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "重新下载",
                                    tint = Color(0xFFFF9800)
                                )
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            // 已完成任务显示播放按钮
                            IconButton(
                                onClick = {
                                    Log.d("VideoDownloadTaskItem", "播放按钮被点击: ${task.id}")
                                    onPlayClick()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放",
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                        else -> {
                            Log.v("VideoDownloadTaskItem", "任务状态无操作按钮: ${task.status}")
                        }
                    }
                    
                    if (task.status != DownloadStatus.COMPLETED) {
                        IconButton(
                            onClick = {
                                Log.d("VideoDownloadTaskItem", "取消按钮被点击: ${task.id}")
                                onCancelClick()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "取消",
                                tint = Color.Red
                            )
                        }
                    } else {
                        Log.v("VideoDownloadTaskItem", "已完成任务无取消按钮: ${task.id}")
                    }
                }
            }
        }
    }
}

/**
 * 获取视频下载状态文本
 */
private fun getVideoDownloadStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.PENDING -> "等待中"
        DownloadStatus.DOWNLOADING -> "下载中"
        DownloadStatus.PAUSED -> "已暂停"
        DownloadStatus.COMPLETED -> "已完成"
        DownloadStatus.FAILED -> "下载失败"
        DownloadStatus.CANCELLED -> "已取消"
    }
}

/**
 * 获取视频下载状态颜色
 */
private fun getVideoDownloadStatusColor(status: DownloadStatus): Color {
    return when (status) {
        DownloadStatus.PENDING -> Color.Gray
        DownloadStatus.DOWNLOADING -> Color(0xFF4CAF50)
        DownloadStatus.PAUSED -> Color(0xFFFF9800)
        DownloadStatus.COMPLETED -> Color(0xFF2196F3)
        DownloadStatus.FAILED -> Color(0xFFF44336)
        DownloadStatus.CANCELLED -> Color.Gray
    }
}

/**
 * 格式化下载速度
 */
private fun formatDownloadSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond >= 1024 * 1024 -> "${bytesPerSecond / (1024 * 1024)}MB/s"
        bytesPerSecond >= 1024 -> "${bytesPerSecond / 1024}KB/s"
        else -> "${bytesPerSecond}B/s"
    }
}

/**
 * 下载列表主页面
 * 支持普通浏览和编辑模式切换
 * 
 * @param modifier 修饰符
 * @param onNavigateBack 返回导航回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    // 页面状态管理
    var downloadListState by remember {
        mutableStateOf(
            DownloadListState(
                downloadItems = getSampleDownloadItems()
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // 深色背景
    ) {
        // 顶部状态栏区域
        StatusBarSection()
        
        // 标题栏区域
        TitleBarSection(
            isEditMode = downloadListState.isEditMode,
            selectedCount = downloadListState.selectedItems.size,
            totalCount = downloadListState.downloadItems.size,
            onSelectAll = {
                downloadListState = if (downloadListState.selectedItems.size == downloadListState.downloadItems.size) {
                    // 取消全选
                    downloadListState.copy(selectedItems = emptySet())
                } else {
                    // 全选
                    downloadListState.copy(
                        selectedItems = downloadListState.downloadItems.map { it.id }.toSet()
                    )
                }
            },
            onCancelEdit = {
                downloadListState = downloadListState.copy(
                    isEditMode = false,
                    selectedItems = emptySet()
                )
            }
        )
        
        // 下载列表内容
        Box(
            modifier = Modifier.weight(1f)
        ) {
            DownloadListContent(
                downloadItems = downloadListState.downloadItems,
                isEditMode = downloadListState.isEditMode,
                selectedItems = downloadListState.selectedItems,
                onItemLongPress = { item ->
                    // 长按进入编辑模式
                    downloadListState = downloadListState.copy(
                        isEditMode = true,
                        selectedItems = setOf(item.id)
                    )
                },
                onItemClick = { item ->
                    if (downloadListState.isEditMode) {
                        // 编辑模式下切换选中状态
                        val newSelectedItems = if (downloadListState.selectedItems.contains(item.id)) {
                            downloadListState.selectedItems - item.id
                        } else {
                            downloadListState.selectedItems + item.id
                        }
                        downloadListState = downloadListState.copy(selectedItems = newSelectedItems)
                    } else {
                        // 普通模式下播放视频
                        // TODO: 实现视频播放逻辑
                    }
                }
            )
        }
        
        // 底部操作栏
        AnimatedVisibility(
            visible = downloadListState.isEditMode && downloadListState.selectedItems.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BottomActionBar(
                selectedCount = downloadListState.selectedItems.size,
                onDeleteClick = {
                    // TODO: 实现删除逻辑
                    val remainingItems = downloadListState.downloadItems.filter { 
                        !downloadListState.selectedItems.contains(it.id) 
                    }
                    downloadListState = downloadListState.copy(
                        downloadItems = remainingItems,
                        selectedItems = emptySet(),
                        isEditMode = false
                    )
                }
            )
        }
    }
}

/**
 * 状态栏区域组件
 * 模拟iPhone状态栏样式
 */
@Composable
private fun StatusBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.Transparent)
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
        
        // 右侧状态图标区域
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 信号强度图标
            Box(
                modifier = Modifier
                    .size(18.dp, 12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
            // WiFi图标
            Box(
                modifier = Modifier
                    .size(15.dp, 12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
            // 电池图标
            Box(
                modifier = Modifier
                    .size(24.dp, 12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * 标题栏区域组件
 * 包含标题、全选按钮、取消按钮等
 */
@Composable
private fun TitleBarSection(
    isEditMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditMode) {
            // 编辑模式：显示取消按钮
            TextButton(
                onClick = onCancelEdit,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp
                )
            }
        } else {
            // 普通模式：显示标题
            Text(
                text = "Download List",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (isEditMode) {
            // 编辑模式：显示全选按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全选圆圈
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedCount == totalCount && totalCount > 0) {
                                Color(0xFF007AFF)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSelectAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Transparent,
                                shape = CircleShape
                            )
                            .then(
                                if (selectedCount != totalCount || totalCount == 0) {
                                    Modifier.background(
                                        Color.Transparent,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (selectedCount == totalCount && totalCount > 0) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    
                    // 圆圈边框
                    if (selectedCount != totalCount || totalCount == 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .then(
                                    Modifier.background(
                                        Color.Transparent,
                                        shape = CircleShape
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .padding(1.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                            )
                        }
                    }
                }
                
                TextButton(
                    onClick = onSelectAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = "Select All",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * 下载列表内容组件
 * 显示下载项列表
 */
@Composable
private fun DownloadListContent(
    downloadItems: List<DownloadItem>,
    isEditMode: Boolean,
    selectedItems: Set<String>,
    onItemLongPress: (DownloadItem) -> Unit,
    onItemClick: (DownloadItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(downloadItems) { item ->
            DownloadItemCard(
                item = item,
                isEditMode = isEditMode,
                isSelected = selectedItems.contains(item.id),
                onLongPress = { onItemLongPress(item) },
                onClick = { onItemClick(item) }
            )
        }
    }
}

/**
 * 下载项卡片组件
 * 单个下载项的UI展示
 */
@Composable
private fun DownloadItemCard(
    item: DownloadItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择圆圈（编辑模式下显示）
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF007AFF) else Color.Transparent
                        )
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }
                }
            }
            
            // 缩略图
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3A3A3A))
            ) {
                // 播放按钮图标
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                }
            }
            
            // 文件信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.fileName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.fileSize,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = item.downloadTime,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * 底部操作栏组件
 * 编辑模式下的操作按钮
 */
@Composable
private fun BottomActionBar(
    selectedCount: Int,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFF2A2A2A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDeleteClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B30)
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .height(52.dp)
                    .widthIn(min = 120.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Delete ($selectedCount)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 获取示例下载数据
 * 用于演示界面效果
 */
private fun getSampleDownloadItems(): List<DownloadItem> {
    return listOf(
        DownloadItem(
            id = "1",
            fileName = "34247378sfdf43EQEadas.fdffdf43E.mp4",
            fileSize = "267MB",
            downloadTime = "12:33"
        ),
        DownloadItem(
            id = "2",
            fileName = "How to make tiramisu? You can",
            fileSize = "123MB",
            downloadTime = "12:33"
        ),
        DownloadItem(
            id = "3",
            fileName = "Racing Car Championship 2024",
            fileSize = "456MB",
            downloadTime = "11:45"
        ),
        DownloadItem(
            id = "4",
            fileName = "Short Video Collection",
            fileSize = "89MB",
            downloadTime = "10:22"
        ),
        DownloadItem(
            id = "5",
            fileName = "Stream Box Tutorial Guide",
            fileSize = "234MB",
            downloadTime = "09:15"
        )
    )
}