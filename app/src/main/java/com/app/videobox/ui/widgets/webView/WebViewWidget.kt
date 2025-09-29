package com.app.videobox.ui.widgets.webView

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.videobox.ui.pages.webViewPage.WebViewModel
import com.app.videobox.ui.widgets.webView.AccompanistWebChromeClient
import com.app.videobox.ui.widgets.webView.AccompanistWebViewClient
import com.app.videobox.ui.widgets.webView.WebView
import com.app.videobox.ui.widgets.webView.WebViewState
import kotlinx.coroutines.launch

// 白屏问题跟踪日志关键词 - 用于监控WebView白屏状态
private const val WHITE_SCREEN_TAG = "WEBVIEW_WHITE_SCREEN_TRACKER"

/**
 * WebView组件 - 用于加载网页并监听视频资源
 * 
 * 优化说明：
 * 1. 使用LaunchedEffect确保WebView初始化不阻塞UI渲染
 * 2. 添加详细的错误处理和日志记录
 * 3. 优化内存管理，防止内存泄漏
 * 4. 异步处理视频URL检测，避免阻塞主线程
 * 5. 添加初始化状态管理，解决白屏问题
 * 6. 新增白屏问题跟踪日志关键词：WEBVIEW_WHITE_SCREEN_TRACKER
 */
@Composable
fun WebViewWidget(
    webViewState: WebViewState,
    onUrlChanged: (String) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onPageFinished:()->Unit = {},
    onResolverUrl: (String, String, String, String) -> Unit,
    onBack: () -> Unit = {},
    onResetResolve: () -> Unit = {},
    viewModel: WebViewModel
) {
    // 用于去重的Set - 防止重复处理相同的视频URL
    val discoveredVideoUrls = remember { mutableSetOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    
    // WebView初始化状态管理 - 增强白屏问题修复
    var isWebViewInitialized by remember { mutableStateOf(false) }
    var isPageLoading by remember { mutableStateOf(true) }
    var isPageVisible by remember { mutableStateOf(false) } // 页面真正可见状态
    
    // 记录初始状态 - 用于白屏问题跟踪
    Log.d(WHITE_SCREEN_TAG, "WebView组件初始化 - 初始化状态: $isWebViewInitialized, 加载状态: $isPageLoading")
    
    // WebView客户端 - 处理页面加载和资源拦截
    val webViewClient = remember {
        object : AccompanistWebViewClient() {
            
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WebViewWidget", "页面开始加载: $url")
                
                // 标记页面开始加载，显示加载状态
                isPageLoading = true
                Log.d(WHITE_SCREEN_TAG, "页面开始加载 - URL: $url, 白屏状态: ${!isWebViewInitialized || isPageLoading}")
                
                url?.let {
                    // 立即更新URL，确保地址栏同步
                    onUrlChanged(it)
                }
            }
            
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebViewWidget", "页面加载完成: $url")
                
                // 标记页面加载完成，但需要等待页面真正可见
                isPageLoading = false
                
                // 延迟检查页面是否真正可见，防止白屏
                coroutineScope.launch {
                    kotlinx.coroutines.delay(200) // 等待200ms确保页面渲染完成
                    isPageVisible = true
                    Log.d(WHITE_SCREEN_TAG, "页面渲染完成 - URL: $url, 初始化: $isWebViewInitialized, 加载: $isPageLoading, 可见: $isPageVisible")
                    Log.d(WHITE_SCREEN_TAG, "当前白屏状态: ${!isWebViewInitialized || isPageLoading || !isPageVisible}")
                }
                
                Log.d(WHITE_SCREEN_TAG, "页面加载完成 - URL: $url, 白屏状态: ${!isWebViewInitialized || isPageLoading || !isPageVisible}")
//                if ((!isWebViewInitialized || isPageLoading || !isPageVisible).not()){
//                    onPageFinished.invoke()
//                }

                if (url.isNullOrEmpty()) return
                
                try {
                    // 设置WebView上下文信息 - 用于后续的视频检测
                    viewModel.setWebViewContext(view, url)
                } catch (e: Exception) {
                    Log.e("WebViewWidget", "设置WebView上下文失败", e)
                }
            }
            
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val url = request?.url?.toString()
                Log.d("WebViewWidget", "URL加载请求: $url")
                
                return if (request?.url?.scheme?.contains("http") == true) {
                    super.shouldOverrideUrlLoading(view, request)
                } else {
                    Log.d("WebViewWidget", "拦截非HTTP请求: $url")
                    true
                }
            }
            
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let { req ->
                    val url = req.url.toString()
                    
                    // 异步检查是否为视频URL，避免阻塞主线程
                    coroutineScope.launch {
                        try {
                            if (viewModel.isVideoUrl(url) && !discoveredVideoUrls.contains(url)) {
                                Log.d("WebViewWidget", "检测到新的视频URL: $url")
                                onResetResolve.invoke()
                            }
                        } catch (e: Exception) {
                            Log.e("WebViewWidget", "检查视频URL时出错: $url", e)
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
            
            override fun onLoadResource(view: WebView, url: String?) {
                super.onLoadResource(view, url)
                url?.let { resourceUrl ->
                    // 异步处理视频URL检测，避免阻塞UI渲染
                    coroutineScope.launch {
                        try {
                            Log.d("WebViewWidget", "新的资源url:${url} ")
                            viewModel.checkVideoUrl(resourceUrl, discoveredVideoUrls, onResolverUrl, view)
                        } catch (e: Exception) {
                            Log.e("WebViewWidget", "检查视频资源时出错: $resourceUrl", e)
                        }
                    }
                }
            }
            
            override fun onReceivedError(
                 view: WebView,
                 request: WebResourceRequest?,
                 error: android.webkit.WebResourceError?
             ) {
                 super.onReceivedError(view, request, error)
                 Log.e("WebViewWidget", "WebView加载错误: ${error?.description}, URL: ${request?.url}")
             }
        }
    }
    
    // Chrome客户端 - 处理进度和标题更新
    val webViewChromeClient = remember { 
        object : AccompanistWebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                Log.d("WebViewWidget", "页面加载进度: $newProgress%")
                onProgressChanged.invoke(newProgress)
                super.onProgressChanged(view, newProgress)
            }
            
            override fun onReceivedTitle(view: WebView, title: String?) {
                 super.onReceivedTitle(view, title)
                 Log.d("WebViewWidget", "页面标题更新: $title")
             }
        } 
    }

    // LaunchedEffect - 管理WebView初始化状态，确保渲染时机正确
    LaunchedEffect(webViewState) {
        Log.d("WebViewWidget", "开始WebView初始化流程")
        Log.d(WHITE_SCREEN_TAG, "LaunchedEffect启动 - 开始增强初始化延迟处理")
        // 增加延迟到300ms，确保WebView完全准备就绪
        kotlinx.coroutines.delay(300)
        isWebViewInitialized = true
        Log.d("WebViewWidget", "WebView初始化状态设置完成")
        Log.d(WHITE_SCREEN_TAG, "WebView初始化完成 - 初始化: $isWebViewInitialized, 加载: $isPageLoading, 可见: $isPageVisible")
        Log.d(WHITE_SCREEN_TAG, "当前白屏状态: ${!isWebViewInitialized || isPageLoading || !isPageVisible}")
    }
    
    // 使用Box容器来管理WebView和加载状态的显示
    Box(modifier = Modifier.fillMaxSize()) {
        // WebView组件 - 使用优化的配置确保性能和稳定性
        WebView(
            state = webViewState,
            client = webViewClient,
            chromeClient = webViewChromeClient,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = true,
            onBack = onBack,
            factory = { context ->
                WebView(context).apply {
                    Log.d("WebViewWidget", "创建WebView实例")
                    
                    // 优化WebView设置以提升性能和兼容性
                    settings.run {
                        // 基础功能设置
                        javaScriptEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        domStorageEnabled = true
                        
                        // 性能优化设置
                         cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                         databaseEnabled = true
                        
                        // 媒体和文件访问设置
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        
                        // 安全设置
                        allowUniversalAccessFromFileURLs = false
                        allowFileAccessFromFileURLs = false
                        
                        // 用户体验设置
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true

                        clipToOutline = true
                        Log.d("WebViewWidget", "WebView设置配置完成")
                    }
                }
            },
            onCreated = { webView ->
                Log.d("WebViewWidget", "WebView创建完成，开始初始化")
            },
            onDispose = { webView ->
                Log.d("WebViewWidget", "WebView销毁，清理资源")
                // 清理资源，防止内存泄漏
                discoveredVideoUrls.clear()
            }
        )
        
        // 加载指示器 - 在WebView初始化或页面加载期间显示
        if (!isWebViewInitialized || isPageLoading || !isPageVisible) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                strokeWidth = 4.dp
            )
            if(isWebViewInitialized && isPageLoading.not() && isPageVisible.not()){
                onPageFinished.invoke()
            }
            Log.d("WebViewWidget", "显示加载指示器 - 初始化状态: $isWebViewInitialized, 加载状态: $isPageLoading, 可见状态: $isPageVisible")
            Log.d(WHITE_SCREEN_TAG, "显示加载指示器 - 防止白屏显示，初始化: $isWebViewInitialized, 加载: $isPageLoading, 可见: $isPageVisible")
        } else {
            Log.d(WHITE_SCREEN_TAG, "隐藏加载指示器 - WebView正常显示，无白屏问题")
        }
    }
}



