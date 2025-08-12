package com.app.videobox.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R

/**
 * 视频下载中页面 - 基于Figma设计稿实现
 * 展示当前正在下载的视频列表和下载进度
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDownloadingScreen() {
    // 模拟下载数据
    val downloadingItems = remember {
        listOf(
            VideoDownloadingItem(
                id = "1",
                title = "APK Downloader Download Android APK files directly from the Google Play App Store - …",
                url = "https://www.gdaily.org , google-pla...",
                description = "Google Play Store APK is the market APP of Google Store. To open the PLAY store normally, you need to install the Google framework first. If your system does not have the Google framework",
                progress = 0.65f,
                isDownloading = true
            ),
            VideoDownloadingItem(
                id = "2",
                title = "(Download) Google Play Store APK 26, Play Store installation file - GDaily",
                url = "https://www.gdaily.org , google-pla...",
                description = "Google Play Store APK is the market APP of Google Store. To open the PLAY store normally, you need to install the Google framework first. If your system does not have the Google framework, please flash the system and install Google Gapps as the user's Google.",
                progress = 0.35f,
                isDownloading = true
            ),
            VideoDownloadingItem(
                id = "3",
                title = "(Download) Google Play Store APK 26, Play Store installation file - GDaily",
                url = "https://www.gdaily.org , google-pla...",
                description = "Google Play Store APK is the market APP of Google Store. To open the PLAY store normally, you need to install the Google framework first. If your system does not have the Google framework, please flash the system and install Google Gapps as the user's Google.",
                progress = 0.85f,
                isDownloading = true
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 状态栏区域
        StatusBarSection()
        
        // 搜索栏区域
        SearchBarSection()
        
        // 下载进度指示器
        DownloadProgressIndicator()
        
        // 下载列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(downloadingItems) { item ->
                VideoDownloadingItemCard(item = item)
            }
        }
    }
}

/**
 * 状态栏组件 - 模拟iPhone状态栏
 */
@Composable
private fun StatusBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间显示
        Text(
            text = "9:41",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        
        // 右侧状态图标
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 信号强度
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "信号",
                modifier = Modifier.size(16.dp),
                tint = Color.Black
            )
            // WiFi
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "WiFi",
                modifier = Modifier.size(16.dp),
                tint = Color.Black
            )
            // 电池
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "电池",
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
        }
    }
}

/**
 * 搜索栏组件
 */
@Composable
private fun SearchBarSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.search_or_type_url),
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 下载进度指示器
 */
@Composable
private fun DownloadProgressIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 下载图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(0xFF4CAF50),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "下载",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 进度信息
        Column {
            Text(
                text = "正在下载 3 个文件",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "总进度: 62%",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 暂停按钮
        IconButton(
            onClick = { /* 暂停下载 */ }
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "暂停",
                tint = Color.Gray
            )
        }
    }
}

/**
 * 下载项目卡片
 */
@Composable
private fun VideoDownloadingItemCard(item: VideoDownloadingItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // URL和图标行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.url,
                    fontSize = 12.sp,
                    color = Color.Blue,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Google图标组
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when (it) {
                                        0 -> Color(0xFF4285F4) // 蓝色
                                        1 -> Color(0xFFEA4335) // 红色
                                        2 -> Color(0xFFFBBC05) // 黄色
                                        else -> Color(0xFF34A853) // 绿色
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 标题
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 描述
            Text(
                text = item.description,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 进度条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "下载进度",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = item.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * 视频下载项目数据类
 */
data class VideoDownloadingItem(
    val id: String,
    val title: String,
    val url: String,
    val description: String,
    val progress: Float,
    val isDownloading: Boolean
)