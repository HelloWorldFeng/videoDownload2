package com.app.videobox.ui.widgets.webView

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.app.videobox.ui.pages.webViewPage.WebViewModel


@Composable
fun WebViewWidget(
    webViewState: WebViewState,
    onUrlChanged: (String) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onResolverUrl: (String, String, String, String) -> Unit,
    onBack: () -> Unit = {},
    onResetResolve: () -> Unit = {},
    viewModel: WebViewModel
) {
    // 用于去重的Set
    val discoveredVideoUrls = remember { mutableSetOf<String>() }
    val coroutineScope = rememberCoroutineScope()

    val webViewClient = remember {
        object : AccompanistWebViewClient() {

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    onUrlChanged(it)
                }
            }
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                if (url.isNullOrEmpty()) return
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                return if (request?.url?.scheme?.contains("http") == true)
                    super.shouldOverrideUrlLoading(view, request)
                else true
            }


            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let {
                    if (viewModel.isVideoUrl(request.url.toString()) && !discoveredVideoUrls.contains(request.url.toString())){
                        onResetResolve.invoke()
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onLoadResource(view: WebView, url: String?) {
                super.onLoadResource(view, url)
                url?.let {
                    // 检查加载的资源是否为视频
                    viewModel.checkVideoUrl(url, discoveredVideoUrls, onResolverUrl, view)
                }
            }
        }
    }
    val webViewChromeClient = remember { object : AccompanistWebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            onProgressChanged.invoke(newProgress)
            super.onProgressChanged(view, newProgress)
        }
    } }

    WebView(
        state = webViewState,
        client = webViewClient,
        chromeClient = webViewChromeClient,
        modifier = Modifier.fillMaxSize(),
        captureBackPresses = true,
        onBack = onBack,
        factory = { context ->
            WebView(context).apply {
                settings.run {
                    javaScriptCanOpenWindowsAutomatically = true
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
                
            }
        },
    )
}



