package com.app.videobox.ui.pages.webViewPage

import VideoInfo
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.FIRST_IN_HOME
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.ModalBottomSheetV3
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.ui.widgets.webView.WebViewWidget
import com.app.videobox.ui.widgets.webView.rememberWebViewState
import com.app.videobox.utils.VideoResolve
import com.app.videobox.utils.WebViewHttpContext
import com.app.videobox.R
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.UserHelper
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ext.toDurationText
import com.app.videobox.ext.toFileSizeText
import com.app.videobox.ext.toHttpsUrl
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.ui.MainActivity
import com.app.videobox.ui.widgets.GradientButton
import com.app.videobox.ui.widgets.StateAsyncImageImpl
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.VideoThumbnailExtractor
import com.blankj.utilcode.util.SPStaticUtils
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.videodownloader.module.download.DownloaderV2
import com.videodownloader.module.download.TaskFactory
import kotlinx.coroutines.launch
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
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WebPageScreen(
    inputUrl: String,
    viewModel: WebViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val context = LocalContext.current as Activity

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var currentUrl by remember { mutableStateOf(inputUrl) }
    val webViewState = rememberWebViewState(currentUrl)
    var showGuiderMask = remember { mutableStateOf(value = SPStaticUtils.getBoolean(FIRST_IN_HOME,true)) }

    val backAction = {
        if (webViewState.canGoBack()) {
            viewModel.postAction(WebViewModel.Action.ResetResolve)
            webViewState.goBack()
        }else{
            AdManager.getFullAdFromPool(
                context,
                adType = AD_TYPE_INT,
                adScene = "i_search_back",
                closeAction = {
                    context.finish()
                })
        }

        EventReportUtils.reportTDParams("browser_search_click",
            params = mutableMapOf(
                "action" to "back",
            ), desc = "搜索页面点击")
    }
    BackHandler {
        backAction.invoke()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .singClick {}){
        Column(modifier = Modifier.fillMaxSize())
        {
            // 显示当前URL的状态栏 - 优先渲染，不依赖WebView状态
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
                    backAction.invoke()
                },
                onHome = {
                    context.finish()
                    EventReportUtils.reportTDParams("browser_search_click",
                        params = mutableMapOf(
                            "action" to "home",
                        ), desc = "搜索页面点击")
                },
                onAd = {
                    EventReportUtils.reportTDParams("browser_search_click",
                        params = mutableMapOf(
                            "action" to "adremove",
                        ), desc = "搜索页面点击")
                }
            )

            // WebView容器 - 使用Box包装以避免阻塞父布局渲染
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 占用剩余空间
            ) {
                // WebView组件 - 异步加载，不阻塞UI线程
                WebViewWidget(
                    viewModel = viewModel,
                    webViewState = webViewState,
                    onUrlChanged = { newUrl ->
                        //webView浏览时候产生的新地址，修改到网址栏
                        if (newUrl != currentUrl) {
                            viewModel.postAction(WebViewModel.Action.ResetResolve)
                        }
                        currentUrl = newUrl
                    },
                    onProgressChanged = { newProgress ->
                        // 可以在这里添加加载进度显示逻辑
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
        }

        //引导蒙层
        val clickMask = {
            showGuiderMask.value = false
        }
        if (showGuiderMask.value && RemoteConfigManager.showGuider) {
            Box(Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.8f))
                .singClick {
                    clickMask.invoke()
                }
            )
            {
                //引导箭头样式指向->悬浮视频按钮
                Column(
                    modifier = Modifier
                        .padding(end = 90.dp, bottom = 150.dp)
                        .align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GradientButton(
                        modifier = Modifier.size(216.dp,57.dp),
                        text = "parsing in progress!",
                        onClick = {}
                    )
                    AsyncImageImpl(
                        modifier = Modifier
                            .align(Alignment.End)
                            .size(91.dp,132.dp),
                        model = R.drawable.guider_arrow
                    )
                }

            }
        }

        // 悬浮视频按钮
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            content = {
                DraggableResolveButton(
                    viewModel = viewModel,
                    currentUrl = currentUrl,
                    onClick = {
                        clickMask.invoke()
                    },
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
                val host = info.url.toUri().host
                EventReportUtils.reportTDParams("download_pop_click", params = mutableMapOf(
                    "action" to "download",
                    "URL" to (host?:info.url)
                ), desc = "视频下载弹窗点击")

                AdManager.getFullAdFromPool(
                    context,
                    adScene = "i_download",
                    adType = AD_TYPE_INT,
                    closeAction = {}
                )
                //旧方法-创F建下载任务
//                handleVideoResourceDownload(videoUrl = "videoUrl", videoTitle = "test")
            }
        )

        //空信息弹窗
        EmptyResolveDialog(
            viewModel = viewModel,
            currentUrl = currentUrl,
            onDismissRequest = {
                viewModel.postAction(WebViewModel.Action.HideEmptyResolveDialog)
            },
            onClick = {
                viewModel.postAction(WebViewModel.Action.HideEmptyResolveDialog)
            }
        )

    }

}

@Composable
fun DraggableResolveButton(
    viewModel: WebViewModel,
    maxWidthPx: Float,
    maxHeightPx: Float,
    currentUrl: String,
    onClick: () -> Unit
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
        currentUrl = currentUrl,
        onClick = onClick,
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
        is WebViewModel.DialogState.Hidden -> {}

        is WebViewModel.DialogState.Showing -> {
            if (state is WebViewModel.ResolveVideoState.ResolveSuccess){
                EventReportUtils.reportTDParams("download_pop_show", params = mutableMapOf(
                    "download_type" to "success"
                ), desc = "视频下载弹窗展示")

                ResolveDialog(
                    state = state,
                    viewModel = viewModel,
                    onClickDownload = onClickDownload,
                    onDismissRequest = {
                        viewModel.postAction(WebViewModel.Action.HideResolveDialog)
                        EventReportUtils.reportTDParams("download_pop_click", params = mutableMapOf(
                            "action" to "close",
                        ), desc = "视频下载弹窗点击")
                                       },
                )
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyResolveDialog(
    viewModel: WebViewModel,
    onDismissRequest: () -> Unit,
    onClick: () -> Unit,
    currentUrl: String,
){
    val resolveDialogState = viewModel.resolveEmptyDialogStateFlow.collectAsStateWithLifecycle().value

    when (resolveDialogState) {
        WebViewModel.DialogState.Hidden -> {}
        WebViewModel.DialogState.Showing -> {
            EventReportUtils.reportTDParams("download_pop_show", params = mutableMapOf(
                "download_type" to "fail"
            ), desc = "视频下载弹窗展示")

            EventReportUtils.reportTDParams("browser_nodownload_click", params = mutableMapOf(
                "web" to currentUrl
            ), desc = "下载按钮点击-无内容弹窗展示")

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
                Column(Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally)
                {
                    val emptyLottie by rememberLottieComposition(LottieCompositionSpec.Asset("resolve_dialog_empty.json"))

                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        AsyncImageImpl(
                            modifier = Modifier
                                .size(24.dp)
                                .singClick {
                                    onDismissRequest.invoke()
                                },
                            model = R.drawable.icon_cancel
                        )
                    }
                    Text(
                        text = stringResource(R.string.play_video_before_downloading),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    LottieAnimation(
                        composition = emptyLottie,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = stringResource(R.string.please_play_the_video_you_want_to_download),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.this_helps_us_detect_the_video_source_and_activate_the_download_button),
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))

                    GradientButton(
                        text = stringResource(R.string.got_it),
                        onClick = {
                            onClick.invoke()
                            onDismissRequest.invoke()
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.some_websites_may_not_support_downloading),
                        fontSize = 12.sp,
                        color = Color(0xFF898989)
                    )
                }
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
    viewModel: WebViewModel,
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
            viewModel = viewModel,
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
    info: VideoResolve.ResolveVideoInfo,
    viewModel: WebViewModel,
    onNavigateBack: () -> Unit,
    onClickDownload:(videoUrl: VideoInfo)-> Unit,
) {
    val context = LocalContext.current
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    
    // 状态管理：存储从视频提取的缩略图Bitmap
    var extractedThumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isExtractingThumbnail by remember { mutableStateOf(false) }
    var extractionError by remember { mutableStateOf<String?>(null) }
    
    // 使用LaunchedEffect在组件初始化时提取视频缩略图
    LaunchedEffect(info.originUrl) {
        if (info.originUrl.isNotEmpty()) {
            isExtractingThumbnail = true
            extractionError = null
            
            try {
                val videoThumbnailExtractor = VideoThumbnailExtractor()
                val bitmap = videoThumbnailExtractor.extractThumbnailFromUrl(
                    videoUrl = info.originUrl,
                    width = 320,
                    height = 240
                )
                extractedThumbnail = bitmap
                Log.d("ResolveDialogImpl", "成功提取视频缩略图: ${info.originUrl}")
            } catch (e: Exception) {
                extractionError = e.message
                Log.e("ResolveDialogImpl", "提取视频缩略图失败: ${e.message}", e)
            } finally {
                isExtractingThumbnail = false
            }
        }
    }

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
                    // 根据缩略图提取状态决定显示内容
                    val thumbnailToUse = when {
                        extractedThumbnail != null -> extractedThumbnail!! // 使用提取的缩略图Bitmap
                        isExtractingThumbnail -> null // 正在提取中，显示加载状态
                        extractionError != null -> thumbnail.toHttpsUrl() // 提取失败，回退到原始缩略图URL
                        else -> thumbnail.toHttpsUrl() // 默认使用原始缩略图URL
                    }

                    VideoInfoPreview(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 18.dp),
                        title = title,
                        thumbnailUrl = if (thumbnailToUse is android.graphics.Bitmap) {
                            // 如果是Bitmap，需要转换为可用的图片源
                            // StateAsyncImageImpl支持Bitmap作为imageModel
                            thumbnailToUse.toString() // 临时处理，实际应该直接传递Bitmap
                        } else {
                            thumbnailToUse as? String ?: thumbnail.toHttpsUrl()
                        },
                        duration = duration.roundToInt(),
                        extractedBitmap = if (thumbnailToUse is android.graphics.Bitmap) thumbnailToUse else null,
                        isLoadingThumbnail = isExtractingThumbnail
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
                                .size(221.dp, 52.dp)
                                .background(
                                    color = Color(0xFF464748),
                                    shape = RoundedCornerShape(12.dp)
                                )
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
                            // 获取WebView的HTTP上下文信息
                            val httpContext = WebViewHttpContext.getHttpContext(
                                webView = viewModel.getCurrentWebView(),
                                currentUrl = viewModel.getCurrentPageUrl()
                            )

                            val newVideoInfo = VideoInfo(
                                id = System.currentTimeMillis().toString(),
                                title = info.title,
                                duration = info.duration,
                                thumbnail = info.thumbnail,
                                size = info.size,
                                url = info.originUrl,
                                ext = info.ext,
                                // 传递WebView的HTTP上下文信息
                                httpHeaders = httpContext.httpHeaders,
                                cookies = httpContext.cookies,
                                referer = httpContext.referer,
                                userAgent = httpContext.userAgent
                            )
                            onClickDownload.invoke(newVideoInfo)
                            onNavigateBack.invoke()
                            ToastUtils.showLong(context.getString(R.string.start_download_task))
                        }
                        // 检查外部存储权限 - Android 13+不需要存储权限
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Android 13+ 直接下载
                            startDownload()
                        } else {
                            // Android 12及以下需要检查权限
                            context.let { ctx ->
                                if (XXPermissions.isGranted(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    // 权限已授予，直接下载
                                    startDownload()
                                } else {
                                    // 申请权限
                                    XXPermissions.with(ctx)
                                        .permission(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,))
                                        .request(object : OnPermissionCallback {
                                            override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                                                if (all) {
                                                    // 权限授予成功，开始下载
                                                    startDownload()
                                                }
                                            }

                                            override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                                                // 权限被拒绝，提示用户
                                                ToastUtils.showShort("Storage permission denied, unable to download")
                                                Log.w("FormatDialog", "存储权限被拒绝，无法下载到公共目录")
                                            }
                                        })
                                }
                            } ?: startDownload() // 如果context为空，直接下载
                        }
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
    viewModel: WebViewModel,
    currentUrl: String,
    onClick: () -> Unit
) {
    //解析任务状态
    val videoInfoState = viewModel.resolveStateFlow.collectAsStateWithLifecycle().value
    val context = LocalContext.current as Activity

    when (videoInfoState) {
        is WebViewModel.ResolveVideoState.Idle -> {
            AsyncImageImpl(
                modifier = modifier
                    .padding(bottom = 130.dp, end = 30.dp)
                    .size(56.dp)
                    .singClick {
                        onClick.invoke()
                        if (RemoteConfigManager.checkUrlInBlackUrl(currentUrl) && UserHelper.powerUser) {
                            ToastUtils.showLong(context.getString(R.string.due_to_legal))
                            return@singClick
                        }

                        viewModel.postAction(WebViewModel.Action.ShowEmptyResolveDialog)

                        EventReportUtils.reportTDParams(
                            "browser_download_click",
                            params = mutableMapOf(
                                "click_type" to "none",
                            ), desc = "下载按钮点击"
                        )
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
                    .singClick {
                        onClick.invoke()
                        if (RemoteConfigManager.checkUrlInBlackUrl(currentUrl) && UserHelper.powerUser) {
                            ToastUtils.showLong(context.getString(R.string.due_to_legal))
                            return@singClick
                        }

                        AdManager.getFullAdFromPool(
                            context,
                            adType = AD_TYPE_INT,
                            adScene = "i_video_download",
                            closeAction = {
                                viewModel.postAction(WebViewModel.Action.ShowResolveDialog)
                            })
                        EventReportUtils.reportTDParams(
                            "browser_download_click",
                            params = mutableMapOf(
                                "click_type" to "content",
                            ), desc = "下载按钮点击"
                        )
                    }
                ,
                contentScale = ContentScale.FillWidth
            )

        }
    }

}


@Composable
fun VideoInfoPreview(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailUrl: String,
    duration: Int,
    extractedBitmap: android.graphics.Bitmap? = null,
    isLoadingThumbnail: Boolean = false
) {
    Box(modifier = modifier
        .padding(top = 15.dp)
        .padding(horizontal = 40.dp)
        .fillMaxWidth()
        .wrapContentHeight(Alignment.Top, unbounded = false)) {
        // 根据状态显示不同内容
        if (isLoadingThumbnail) {
            // 显示加载指示器
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(320.dp, 240.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Loading...",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 48.dp)
                )
            }
        } else {
            MediaImage(
                modifier = Modifier.align(Alignment.Center),
                imageModel = extractedBitmap ?: thumbnailUrl,
                contentDescription = stringResource(R.string.thumbnail),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
        )
        {
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
    imageModel: Any,
    contentDescription: String? = null,
) {
    StateAsyncImageImpl(
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth()
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
                        .size(28.dp),
                    model = R.drawable.icon_place,
                    contentDescription = null,
                    contentScale = ContentScale.Inside
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
                        .size(28.dp),
                    model = R.drawable.icon_place,
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