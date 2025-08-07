package com.app.videobox.ui.pages.webViewPage

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.ui.widgets.webView.WebViewWidget
import com.app.videobox.ui.widgets.webView.rememberWebViewState
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.regex.Pattern

/**
 * WebView页面 - 用于浏览URL并监听视频资源
 * 功能：
 * 1. 加载任意URL进行浏览
 * 2. 监听网页资源加载，自动检测视频链接
 * 3. 支持m3u8、mp4等视频格式检测
 * 4. 检测到视频资源时提供下载选项
 * 5. 悬浮按钮显示检测到的视频数量
 * 6. 详细日志记录排查问题
 */
class WebViewScreen(private val url: String) : Screen {
    
    companion object {
        private const val TAG = "WebViewScreen"
    }
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        
        Log.d(TAG, "WebViewScreen Content() 开始渲染，初始URL: $url")

        // 检测到的视频资源列表 - 使用Set避免重复
        var detectedVideoUrls by remember { mutableStateOf<Set<VideoResource>>(emptySet()) }
        var showVideoDialog by remember { mutableStateOf(false) }
        
        // 日志记录组件生命周期
        LaunchedEffect(Unit) {
            Log.d(TAG, "WebViewScreen LaunchedEffect 启动")
        }
        
        DisposableEffect(Unit) {
            Log.d(TAG, "WebViewScreen DisposableEffect 注册")
            onDispose {
                Log.d(TAG, "WebViewScreen DisposableEffect 清理")
            }
        }
        

        WebPageScreen(inputUrl = url)
        
        // 视频资源对话框
        if (showVideoDialog) {
            VideoResourceDialog(
                videoResources = detectedVideoUrls.toList(),
                onDismiss = { 
                    Log.d(TAG, "用户关闭视频资源对话框")
                    showVideoDialog = false 
                },
                onDownload = { videoResource ->
                    Log.d(TAG, "用户选择下载视频: ${videoResource.type} - ${videoResource.url}")
                    handleVideoResourceDownload(videoResource, context)
                    showVideoDialog = false
                }
            )
        }
    }
}

@Composable
fun WebPageScreen(
    inputUrl: String,
    viewModel: WebViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var currentUrl = inputUrl
    var progress by remember { mutableStateOf(0) }
    val webViewState = rememberWebViewState(currentUrl)

    BackHandler {
        if (webViewState.canGoBack()) {
            viewModel.postAction(WebViewModel.Action.ResetResolve)
            webViewState.goBack()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .singClick {

        }){
        Column(modifier = Modifier.fillMaxSize())
        {

            // 显示当前URL的状态栏
            WebUrlBar(
                currentUrl = currentUrl,
                onUrlChanged = { newUrl ->
                    //网址栏，修改网址，回车确定新的网址
                    if (newUrl.startsWith("http")) {
                        currentUrl = newUrl
                    }else{
                        currentUrl = "https://google.com/search?q=${newUrl}"
                    }
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    webViewState.reloadNewUrl(currentUrl)
                },
                onRefresh = {
                    viewModel.postAction(WebViewModel.Action.ResetResolve)
                    webViewState.reload()
                },
                onBack = {

                    if (webViewState.canGoBack()) {
                        viewModel.postAction(WebViewModel.Action.ResetResolve)
                        webViewState.goBack()
                    }
                },
                onHome = {
                    viewModel.postAction(WebViewModel.Action.ResetResolve)
                },
                onAd = {

                }
            )

            // 显示进度条
//                if (progress < 100) {
//                    LinearProgressIndicator(
//                        progress = {
//                            progress / 100f
//                        },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }

            // WebView组件
            WebViewWidget(
                viewModel = viewModel,
                webViewState = webViewState,
                onUrlChanged = { newUrl ->
                    //webView浏览时候产生的新地址，修改到网址栏
                    if (newUrl != currentUrl) {
                        viewModel.postAction(WebViewModel.Action.ResetResolve)
                    }
                    currentUrl = newUrl
                    viewModel.reportUrl(newUrl)
                },
                onProgressChanged = { newProgress ->
                    progress = newProgress
                },
                onResolverUrl = { hlsUrl,title,imgUrl->
                    viewModel.postAction(WebViewModel.Action.ResolveUrl(hlsUrl,title,imgUrl))
                },
                onResetResolve = {
                    viewModel.postAction(WebViewModel.Action.ResetResolve)
                },
                onBack = {
                    viewModel.postAction(WebViewModel.Action.ResetResolve)
                },
            )
        }

        // 悬浮视频按钮
        FloatingVideoButton(
            onClick = {},
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

}



/**
 * 悬浮视频按钮
 */
@Composable
private fun FloatingVideoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WebViewModel
) {
    //解析任务状态
    val videoInfoState = viewModel.resolveStateFlow.collectAsStateWithLifecycle().value


    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        when (videoInfoState) {
            is WebViewModel.ResolveVideoState.Idle -> {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "视频资源",
                        tint = Color.Black
                    )
                }
            }

            is WebViewModel.ResolveVideoState.Loading->{
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "视频资源",
                        tint = Color.White
                    )
                }
            }

            is WebViewModel.ResolveVideoState.ResolveSuccess -> {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "视频资源",
                        tint = Color.Red
                    )
                }

            }
        }

    }
}

/**
 * 视频资源对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoResourceDialog(
    videoResources: List<VideoResource>,
    onDismiss: () -> Unit,
    onDownload: (VideoResource) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检测到的视频资源") },
        text = {
            Column {
                videoResources.forEach { resource ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onDownload(resource) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = resource.type,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = resource.url,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}




/**
 * 检测视频资源
 */
fun detectVideoResource(url: String): VideoResource? {
    val result = when {
        // M3U8格式检测
        url.contains(".m3u8", ignoreCase = true) -> {
            VideoResource(url, "M3U8", "HLS流媒体")
        }
        // MP4格式检测
        url.contains(".mp4", ignoreCase = true) -> {
            VideoResource(url, "MP4", "MP4视频")
        }
        // FLV格式检测
        url.contains(".flv", ignoreCase = true) -> {
            VideoResource(url, "FLV", "FLV视频")
        }
        // AVI格式检测
        url.contains(".avi", ignoreCase = true) -> {
            VideoResource(url, "AVI", "AVI视频")
        }
        // MKV格式检测
        url.contains(".mkv", ignoreCase = true) -> {
            VideoResource(url, "MKV", "MKV视频")
        }
        // WebM格式检测
        url.contains(".webm", ignoreCase = true) -> {
            VideoResource(url, "WebM", "WebM视频")
        }
        // MOV格式检测
        url.contains(".mov", ignoreCase = true) -> {
            VideoResource(url, "MOV", "MOV视频")
        }
        // TS格式检测已移除
        // 检测常见视频流媒体模式
        isStreamingVideoUrl(url) -> {
            VideoResource(url, "Stream", "流媒体视频")
        }
        else -> null
    }
    
    if (result != null) {
        Log.d("WebViewScreen", "detectVideoResource 成功检测: ${result.type} - $url")
    }
    
    return result
}

/**
 * 检测是否为流媒体视频URL
 */
private fun isStreamingVideoUrl(url: String): Boolean {
    val streamingPatterns = listOf(
        ".*/video/.*",
        ".*/stream/.*",
        ".*/live/.*",
        ".*/hls/.*",
        ".*/dash/.*"
    )
    
    return streamingPatterns.any { pattern ->
        Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(url).matches()
    }
}

/**
 * 处理视频资源下载
 */
private fun handleVideoResourceDownload(
    videoResource: VideoResource,
    context: BaseActivity
) {
    Log.d("WebViewScreen", "开始处理视频下载: ${videoResource.type} - ${videoResource.url}")
    
    context.lifecycleScope.launch {
        try {
            Log.d("WebViewScreen", "显示加载对话框")
            context.showLoadingDialog()
            
            // 获取视频下载API实例
            Log.d("WebViewScreen", "获取视频下载API实例")
            val downloadApi = com.videodownloader.module.api.VideoDownloaderApi.getInstance()
            
            // 创建视频信息对象
            val videoInfo = com.videodownloader.module.api.VideoInfo(
                id = System.currentTimeMillis().toString(),
                title = "网页视频_${videoResource.type}_${System.currentTimeMillis()}",
                description = videoResource.description,
                duration = 0,
                thumbnail = "",
                url = videoResource.url,
                formats = listOf(
                    com.videodownloader.module.api.VideoFormat(
                        ext = videoResource.type.lowercase(),
                        quality = "原画",
                        width = 0,
                        height = 0
                    )
                )
            )
            
            Log.d("WebViewScreen", "创建视频信息对象: ${videoInfo.title}")
            
            // 开始下载
            Log.d("WebViewScreen", "调用startDownload API")
            val taskId = downloadApi.startDownload(videoInfo, videoInfo.formats.first())
            
            Log.d("WebViewScreen", "下载任务创建成功，任务ID: $taskId")
            context.hideLoadingDialog()
            ToastUtils.showShort("开始下载: ${videoInfo.title}")
            
        } catch (e: Exception) {
            Log.e("WebViewScreen", "下载失败", e)
            context.hideLoadingDialog()
            ToastUtils.showShort("下载失败: ${e.message}")
        }
    }
}

/**
 * 视频资源数据类
 */
data class VideoResource(
    val url: String,
    val type: String,
    val description: String
) {
    // 重写equals和hashCode确保Set能正确去重
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as VideoResource
        
        if (url != other.url) return false
        if (type != other.type) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}