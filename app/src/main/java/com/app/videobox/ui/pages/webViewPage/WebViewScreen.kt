package com.app.videobox.ui.pages.webViewPage

import VideoInfo
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.App.Companion.coroutineScope
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.ModalBottomSheetM3
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.ui.widgets.webView.WebViewWidget
import com.app.videobox.ui.widgets.webView.rememberWebViewState
import com.app.videobox.utils.VideoResolve
import com.app.videobox.R
import com.app.videobox.ext.toDurationText
import com.app.videobox.ext.toFileSizeText
import com.app.videobox.ext.toHttpsUrl
import com.app.videobox.ui.widgets.GradientButton
import com.app.videobox.ui.widgets.StateAsyncImageImpl
import com.blankj.utilcode.util.ToastUtils
import com.videodownloader.module.download.DownloaderV2
import com.videodownloader.module.download.TaskFactory
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

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
class WebViewScreen(
    private val url: String,
) : Screen {
    
    companion object {
        private const val TAG = "WebViewScreen"
    }
    
    @SuppressLint("ContextCastToActivity")
    @Composable
    override fun Content() {
        val context = LocalContext.current as BaseActivity
        val viewModel: WebViewModel = koinViewModel()

        WebPageScreen(inputUrl = url,viewModel = viewModel)
    }
}

@Composable
fun WebPageScreen(
    inputUrl: String,
    viewModel: WebViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var currentUrl = inputUrl
    var progress by remember { mutableStateOf(0) }
    val webViewState = rememberWebViewState(currentUrl)

    BackHandler {
        if (webViewState.canGoBack()) {
            viewModel.postAction(WebViewModel.Action.ResetResolve)
            webViewState.goBack()
        }else{
            navigator.pop()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .singClick {}){
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
                    }else{
                        navigator.pop()
                    }
                },
                onHome = {
                    navigator.pop()
                },
                onAd = {

                }
            )


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
                onResolverUrl = { hlsUrl,title,imgUrl,ext->
                    viewModel.postAction(WebViewModel.Action.ResolveUrl(hlsUrl,title,imgUrl,ext))
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
            onClick = {
                viewModel.postAction(WebViewModel.Action.ShowResolveDialog)
            },
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        //解析出来的信息弹窗状态
        ResolveInfoDialog(
            viewModel,
            onClickDownload = { info->
                //新方法-创建下载任务
                val task = if (info.ext == "m3u8"){
                    TaskFactory.createM3U8Task(
                        videoInfo = info,
                        newTitle = info.title,
                    )
                }else{
                    TaskFactory.createMP4Task(
                        videoInfo = info,
                        newTitle = info.title,
                    )
                }
                downloader.enqueue(task)
                //旧方法-创建下载任务
//                handleVideoResourceDownload(videoUrl = "videoUrl", videoTitle = "test")
            }
        )

    }

}
@Composable
fun ResolveInfoDialog(
    viewModel: WebViewModel,
    onClickDownload:(videoUrl: VideoInfo)-> Unit,
) {
    val resolveDialogState = viewModel.resolveDialogStateFlow.collectAsStateWithLifecycle().value
    val state = viewModel.resolveStateFlow.collectAsStateWithLifecycle().value
    when (resolveDialogState) {
        is WebViewModel.ResolveDialogState.Hidden -> {}

        is WebViewModel.ResolveDialogState.Showing -> {
            if (state is WebViewModel.ResolveVideoState.ResolveSuccess){
                ResolveDialog(
                    state = state,
                    onClickDownload = onClickDownload,
                    onDismissRequest = { viewModel.postAction(WebViewModel.Action.HideResolveDialog) },
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolveDialog(
    modifier: Modifier = Modifier,
    state: WebViewModel.ResolveVideoState.ResolveSuccess,
    onDismissRequest: () -> Unit,
    onClickDownload:(videoUrl: VideoInfo)-> Unit,
){
    val sheetStateV3 = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { sheetStateV3.show() }
    val scope = rememberCoroutineScope()
    BackHandler { scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() } }

    ModalBottomSheetM3(
        sheetState = sheetStateV3,
        contentPadding = PaddingValues(),
        onDismissRequest = {
            scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() }
        },
    ){
        ResolveDialogImpl(
            modifier = modifier,
            info = state.info,
            onClickDownload = onClickDownload,
            onNavigateBack = {
                scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() }
            }
        )
    }
}
@Composable
private fun ResolveDialogImpl(
    modifier: Modifier,
    info: VideoResolve.VideoInfo,
    onNavigateBack: () -> Unit,
    onClickDownload:(videoUrl: VideoInfo)-> Unit,
) {
    val lazyGridState = rememberLazyGridState()
    val context = LocalActivity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {

        // 具体内容
        LazyVerticalGrid(
            modifier = Modifier,
            state = lazyGridState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(8.dp),
        ){
            item(span = {GridItemSpan(maxLineSpan)}){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .padding(top = 12.dp, bottom = 14.dp)
                            .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.download),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            //视频信息样式
            info.run {
                item(span = {GridItemSpan(maxLineSpan)}) {
                    FormatVideoPreview(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 18.dp),
                        title = title,
                        thumbnailUrl = thumbnail.toHttpsUrl(),
                        duration = duration.roundToInt(),
                    )
                }
            }


            //视频列表
            info.run {
                item(span = {GridItemSpan(maxLineSpan)}){
                    val fileSizeText = info.size.toFileSizeText()
                    Text(
                        text = resolution
                    )
                }
            }

            item(span = {GridItemSpan(maxLineSpan)}){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .fillMaxWidth()
                ) {

                    GradientButton(onClick = {
                        // 本地函数：执行下载逻辑
                        fun startDownload() {
                            val newVideoInfo = VideoInfo(
                                id = System.currentTimeMillis().toString(),
                                title = info.title,
                                duration = 0,
                                thumbnail = "",
                                url = info.originUrl,
                                ext = info.ext,
                            )
                            onClickDownload.invoke(newVideoInfo)
                            onNavigateBack.invoke()
                        }
                        startDownload()
                    }, text = stringResource(R.string.download))

                }
            }
        }
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

@Composable
fun FormatVideoPreview(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailUrl: String,
    duration: Int,
) {
    Box(modifier = modifier
        .wrapContentWidth()
        .wrapContentHeight(Alignment.Top, unbounded = false)) {
        MediaImage(
            modifier = Modifier,
            imageModel = thumbnailUrl,
            isAudio = false,
            contentDescription = stringResource(R.string.thumbnail),
        )
        Surface(
            modifier = Modifier
                .padding(2.dp)
                .align(Alignment.BottomEnd),
            color = Color.Black.copy(alpha = 0.68f),
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            val durationText = duration.toDurationText()
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = durationText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }

    }
}


@Composable
fun MediaImage(
    modifier: Modifier = Modifier,
    imageModel: String,
    isAudio: Boolean = false,
    contentDescription: String? = null,
) {
    StateAsyncImageImpl(
        modifier = modifier
            .height(180.dp)
            .aspectRatio(if (!isAudio) 16f / 9f else 1f, matchHeightConstraintsFirst = true)
            .clip(MaterialTheme.shapes.extraSmall),
        model = imageModel,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        onLoadingComposable = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImageImpl(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(70.dp, 58.dp),
                    model = R.drawable.ic_launcher_background,
                    contentDescription = null
                )
            }
        },
        onErrorComposable = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFFFFFFF).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImageImpl(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(70.dp, 58.dp),
                    model = R.drawable.ic_launcher_background,
                    contentDescription = null
                )
            }
        }
    )
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