package com.app.videobox.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.app.videobox.ui.pages.DownloadListScreenWrapper
import com.blankj.utilcode.util.ToastUtils
import com.videodownloader.module.api.*
import kotlinx.coroutines.launch

/**
 * 下载测试页面
 * 用于测试M3U8和其他视频链接的下载功能
 */
class DownloadTestScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        
        // 测试链接列表
        val testUrls = listOf(
            TestUrl(
                "M3U8测试链接",
                "https://zshipricf.farsunpteltd.com/playlet-hls/hls_1749038810_1_60361.m3u8?verify=1754279476-48hdUAa3XNzORJlrjXaUbsTo60w%2BZrinau7yNv8rD7k%3D",
                "用户提供的M3U8流媒体测试链接"
            ),
            TestUrl(
                "YouTube测试",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "YouTube视频测试链接"
            ),
            TestUrl(
                "Bilibili测试",
                "https://www.bilibili.com/video/BV1xx411c7mu",
                "Bilibili视频测试链接"
            )
        )
        
        // 下载任务状态
        var downloadTasks by remember { mutableStateOf<List<DownloadTask>>(emptyList()) }
        
        // 获取下载API实例
        val downloadApi = remember {
            try {
                VideoDownloaderApi.getInstance()
            } catch (e: Exception) {
                null
            }
        }
        
        // 注册下载回调
        LaunchedEffect(downloadApi) {
            downloadApi?.let { api ->
                val callback = object : DownloadCallback {
                    override fun onDownloadStarted(task: DownloadTask) {
                        downloadTasks = downloadTasks + task
                        ToastUtils.showShort("开始下载: ${task.videoInfo.title}")
                    }
                    
                    override fun onDownloadProgress(task: DownloadTask, progress: Int, speed: Long) {
                        downloadTasks = downloadTasks.map { 
                            if (it.id == task.id) task else it 
                        }
                    }
                    
                    override fun onDownloadCompleted(task: DownloadTask) {
                        downloadTasks = downloadTasks.map { 
                            if (it.id == task.id) task else it 
                        }
                        ToastUtils.showShort("下载完成: ${task.videoInfo.title}")
                    }
                    
                    override fun onDownloadFailed(task: DownloadTask, error: String) {
                        downloadTasks = downloadTasks.map { 
                            if (it.id == task.id) task else it 
                        }
                        ToastUtils.showShort("下载失败: $error")
                    }
                }
                
                api.registerDownloadCallback(callback)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "下载测试",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E1E1E)
                    )
                )
            },
            containerColor = Color.Black
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 测试链接部分
                item {
                    Text(
                        text = "测试链接",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(testUrls) { testUrl ->
                    TestUrlCard(
                        testUrl = testUrl,
                        onDownloadClick = { url ->
                            downloadApi?.let { api ->
                                context.lifecycleScope.launch {
                                    try {
                                        context.showLoadingDialog()
                                        val videoInfo = api.fetchVideoInfo(url)
                                        context.hideLoadingDialog()
                                        
                                        // 自动选择第一个格式开始下载
                                        val format = videoInfo.formats.firstOrNull()
                                        if (format != null) {
                                            api.startDownload(videoInfo, format)
                                        } else {
                                            ToastUtils.showShort("没有可用的下载格式")
                                        }
                                    } catch (e: Exception) {
                                        context.hideLoadingDialog()
                                        ToastUtils.showShort("获取视频信息失败: ${e.message}")
                                    }
                                }
                            } ?: ToastUtils.showShort("下载API未初始化")
                        }
                    )
                }
                
                // 下载任务部分
                if (downloadTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "下载任务 (${downloadTasks.size})",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(downloadTasks) { task ->
                        DownloadTaskCard(task = task)
                    }
                }
                
                // 跳转到下载列表按钮
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navigator.push(DownloadListScreenWrapper())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        )
                    ) {
                        Text(
                            text = "查看完整下载列表",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 测试URL数据类
 */
data class TestUrl(
    val name: String,
    val url: String,
    val description: String
)

/**
 * 测试URL卡片组件
 */
@Composable
private fun TestUrlCard(
    testUrl: TestUrl,
    onDownloadClick: (String) -> Unit
) {
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
            Text(
                text = testUrl.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = testUrl.description,
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = testUrl.url,
                color = Color(0xFF007AFF),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { onDownloadClick(testUrl.url) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "开始下载",
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 下载任务卡片组件
 */
@Composable
private fun DownloadTaskCard(task: DownloadTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.videoInfo.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = when (task.status) {
                        DownloadStatus.PENDING -> "等待中"
                        DownloadStatus.DOWNLOADING -> "下载中"
                        DownloadStatus.PAUSED -> "已暂停"
                        DownloadStatus.COMPLETED -> "已完成"
                        DownloadStatus.FAILED -> "失败"
                        DownloadStatus.CANCELLED -> "已取消"
                    },
                    color = when (task.status) {
                        DownloadStatus.DOWNLOADING -> Color(0xFF007AFF)
                        DownloadStatus.COMPLETED -> Color(0xFF34C759)
                        DownloadStatus.FAILED -> Color(0xFFFF3B30)
                        else -> Color.Gray
                    },
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = task.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF007AFF),
                trackColor = Color(0xFF3A3A3C)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${task.progress}%",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                Text(
                    text = formatSpeed(task.downloadSpeed),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 格式化下载速度
 */
private fun formatSpeed(speed: Long): String {
    return when {
        speed < 1024 -> "${speed}B/s"
        speed < 1024 * 1024 -> "${speed / 1024}KB/s"
        else -> "${speed / (1024 * 1024)}MB/s"
    }
}

/**
 * 下载测试页面包装器
 */
class DownloadTestScreenWrapper : Screen {
    @Composable
    override fun Content() {
        DownloadTestScreen().Content()
    }
}