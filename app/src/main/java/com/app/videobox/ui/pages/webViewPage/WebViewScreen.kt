package com.app.videobox.ui.pages.webViewPage

import VideoInfo
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.ModalBottomSheetV3
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

@SuppressLint("UnusedBoxWithConstraintsScope")
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
            WebPathBar(
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
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            content = {
                DraggableResolveButton(
                    viewModel = viewModel,
                    maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() },
                    maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                )
            }
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
fun DraggableResolveButton(
    viewModel: WebViewModel,
    maxWidthPx: Float,
    maxHeightPx: Float
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    // 按钮尺寸
    val buttonSize = 56.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }

    // 计算安全的拖动边界
    val maxOffsetX = maxWidthPx - buttonSizePx
    val maxOffsetY = maxHeightPx - buttonSizePx
    // 获取状态栏高度
    val statusBarHeightPx = with(density) {
        WindowInsets.statusBars.getTop(this).toFloat()
    }

    // 初始化按钮位置（右下角）
    LaunchedEffect(isInitialized) {
        if (!isInitialized) {
            // 设置初始位置为右下角
            offsetX = maxOffsetX - with(density) { 30.dp.toPx() } // 右边距
            offsetY = maxOffsetY - with(density) { 130.dp.toPx() } // 底部边距，对应原来的 padding
            isInitialized = true
        }
    }

    FloatingVideoButton(
        viewModel = viewModel,
        modifier = Modifier
            .offset {
                IntOffset(
                    offsetX.roundToInt(),
                    offsetY.roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    // 计算新位置
                    val newX = offsetX + dragAmount.x
                    val newY = offsetY + dragAmount.y

                    // 更精确的边界限制
                    offsetX = newX.coerceIn(
                        0f, // 不允许超出左边界
                        maxOffsetX // 不允许超出右边界
                    )
                    offsetY = newY.coerceIn(
                        statusBarHeightPx,
                        // 不允许超出状态栏
                        maxOffsetY
                    )
                }
            }
    )
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

    ModalBottomSheetV3(
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
            //视频信息样式
            info.run {
                item(span = {GridItemSpan(maxLineSpan)}) {
                    VideoInfoPreview(
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

                    Box{
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(221.dp,52.dp)
                                .background(color = Color(0xFF464748), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = info.resolution,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = fileSizeText,
                                fontSize = 12.sp,
                                color = Color(0xFFF4F4F4)
                            )
                        }
                    }
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
    modifier: Modifier = Modifier,
    viewModel: WebViewModel
) {
    //解析任务状态
    val videoInfoState = viewModel.resolveStateFlow.collectAsStateWithLifecycle().value


    when (videoInfoState) {
        is WebViewModel.ResolveVideoState.Idle -> {
            AsyncImageImpl(
                modifier = modifier
                    .padding(bottom = 130.dp, end = 30.dp)
                    .size(56.dp)
                    .singClick {

                    },
                model = R.drawable.icon_resolve_not,
                contentDescription = null
            )
        }

        is WebViewModel.ResolveVideoState.Loading->{
            val iconComposition by rememberLottieComposition(LottieCompositionSpec.Asset("resolve_loading_btn.json"))
            LottieAnimation(
                composition = iconComposition,
                iterations = LottieConstants.IterateForever,
                modifier = modifier
                    .padding(bottom = 130.dp, end = 30.dp)
                    .size(56.dp)
                ,
                contentScale = ContentScale.FillWidth
            )
        }

        is WebViewModel.ResolveVideoState.ResolveSuccess -> {
            val iconComposition by rememberLottieComposition(LottieCompositionSpec.Asset("resolve_success_btn.json"))
            LottieAnimation(
                composition = iconComposition,
                iterations = LottieConstants.IterateForever,
                modifier = modifier
                    .padding(bottom = 130.dp, end = 30.dp)
                    .size(56.dp)
                    .singClick{
                        viewModel.postAction(WebViewModel.Action.ShowResolveDialog)
                    }
                ,
                contentScale = ContentScale.FillWidth
            )

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
fun VideoInfoPreview(
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
            contentDescription = stringResource(R.string.thumbnail),
        )

        Column(
            modifier = Modifier
                .padding(14.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = stringResource(R.string.download),
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
        }
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
    contentDescription: String? = null,
) {
    StateAsyncImageImpl(
        modifier = modifier
            .height(180.dp)
            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
            .clip(MaterialTheme.shapes.extraSmall),
        model = imageModel,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        onLoadingComposable = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF1C1D1E),
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
                        color = Color(0xFF1C1D1E),
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