package com.app.videobox.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.ui.pages.video.VideoPlayerManager
import com.app.videobox.ui.widgets.TitleBar

/**
 * Figma播放器测试页面
 * 用于测试和展示基于Figma设计的视频播放器
 */
class FigmaPlayerTestScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        
        // 测试视频数据
        val testVideos = remember {
            listOf(
                TestVideo(
                    title = "测试视频 1 - Figma播放器演示",
                    url = "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
                    description = "展示Figma设计的播放器界面"
                ),
                TestVideo(
                    title = "测试视频 2 - 竖屏播放测试",
                    url = "https://sample-videos.com/zip/10/mp4/SampleVideo_640x360_1mb.mp4",
                    description = "测试竖屏模式下的播放效果"
                ),
                TestVideo(
                    title = "测试视频 3 - 控制栏功能测试",
                    url = "https://sample-videos.com/zip/10/mp4/SampleVideo_1920x1080_1mb.mp4",
                    description = "测试播放控制、进度条、全屏等功能"
                )
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题栏
            TitleBar(
                title = "Figma播放器测试",
                onBack = {
                    navigator.pop()
                }
            )
            
            // 说明文本
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Figma播放器功能说明",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "• 基于Figma设计稿\"Stream Box - 视频播放窗口-竖屏备份 6\"实现\n" +
                                "• 支持iPhone状态栏模拟\n" +
                                "• 包含播放控制、进度条、速度调节等功能\n" +
                                "• 集成本地UgcDetailVideoPlayer播放器\n" +
                                "• 支持锁定模式和全屏切换\n" +
                                "• 采用Material Design 3设计规范",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 20.sp
                    )
                }
            }
            
            // 测试视频列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(testVideos) { video ->
                    TestVideoCard(
                        video = video,
                        onPlayClick = {
                            // 使用Figma播放器播放视频
                            VideoPlayerManager.launchVideoPlayer(
                                context = context,
                                videoUrl = video.url,
                                videoTitle = video.title,
                                playerType = VideoPlayerManager.PlayerType.FIGMA
                            )
                        }
                    )
                }
                
                // 播放器类型切换说明
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "播放器类型说明",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "当前默认使用Figma播放器，如果启动失败会自动回退到传统播放器。\n" +
                                        "可通过VideoPlayerManager.setDefaultPlayerType()切换默认播放器类型。",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 测试视频数据类
 */
data class TestVideo(
    val title: String,
    val url: String,
    val description: String
)

/**
 * 测试视频卡片组件
 */
@Composable
fun TestVideoCard(
    video: TestVideo,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 视频信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = video.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "URL: ${video.url}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 播放按钮
            FilledTonalButton(
                onClick = onPlayClick,
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}