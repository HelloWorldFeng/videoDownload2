package com.app.videobox.ui.pages.webViewPage

import android.util.Log
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.videobox.utils.VideoResolve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class WebViewModel : ViewModel() {

    //解析任务状态
    private val mResolveStateFlow: MutableStateFlow<ResolveVideoState> = MutableStateFlow(ResolveVideoState.Idle)
    val resolveStateFlow = mResolveStateFlow.asStateFlow()


    //解析结果弹窗展示状态
    private val mDialogStateFlow:MutableStateFlow<DialogState> = MutableStateFlow(DialogState.Hidden)
    val resolveDialogStateFlow = mDialogStateFlow.asStateFlow()

    private val mResolveEmptyDialogStateFlow:MutableStateFlow<DialogState> = MutableStateFlow(DialogState.Hidden)
    val resolveEmptyDialogStateFlow = mResolveEmptyDialogStateFlow.asStateFlow()
    
    // 存储WebView实例和当前URL，用于获取HTTP上下文信息
    private var currentWebView: WebView? = null
    private var currentPageUrl: String = ""

    sealed interface DialogState{
        data object Hidden: DialogState
        data object Showing: DialogState
    }

    sealed interface ResolveVideoState {
        data object Idle : ResolveVideoState
        data object Loading: ResolveVideoState
        data class ResolveSuccess(val info: VideoResolve.ResolveVideoInfo) : ResolveVideoState
    }

    sealed interface Action{
        data class ResolveUrl(
            val url: String,
            val title: String,
            val imgUrl: String,
            val ext: String, //视频资源类型 m3u8 or mp4
        ): Action
        data object ResetResolve: Action

        data object ShowResolveDialog: Action
        data object HideResolveDialog:Action

        data object ShowEmptyResolveDialog:Action
        data object HideEmptyResolveDialog:Action
    }

    fun postAction(action: Action) {
        when (action) {
            is Action.ResolveUrl -> resolveUrl(action)
            is Action.ResetResolve -> resetResolve()
            is Action.ShowResolveDialog -> showResolveDialog()
            is Action.HideResolveDialog -> hideResolveDialog()
            Action.HideEmptyResolveDialog -> hideEmptyResolveDialog()
            Action.ShowEmptyResolveDialog -> showEmptyResolveDialog()
        }
    }

    private fun showEmptyResolveDialog(){
        mResolveEmptyDialogStateFlow.update { DialogState.Showing }
    }

    private fun hideEmptyResolveDialog() {
        mResolveEmptyDialogStateFlow.update { DialogState.Hidden }
    }

    private fun hideResolveDialog() {
        mDialogStateFlow.update { DialogState.Hidden }
    }

    private fun showResolveDialog() {
        mDialogStateFlow.update { DialogState.Showing }
    }

    private fun resetResolve() {
        resolveJob?.cancel()
        resolveVideoJob?.cancel()
        mResolveStateFlow.update { ResolveVideoState.Idle }
    }
    
    /**
     * 设置当前WebView实例和页面URL
     * @param webView WebView实例
     * @param url 当前页面URL
     */
    fun setWebViewContext(webView: WebView?, url: String) {
        currentWebView = webView
        currentPageUrl = url
    }
    
    /**
     * 获取当前WebView实例
     * @return WebView实例，可能为null
     */
    fun getCurrentWebView(): WebView? = currentWebView
    
    /**
     * 获取当前页面URL
     * @return 当前页面URL
     */
    fun getCurrentPageUrl(): String = currentPageUrl

    private fun resolveUrl(action: Action.ResolveUrl) {
        val url = action.url
        val title = action.title
        val imgUrl = action.imgUrl
        val ext  = action.ext
        resolveVideoJob = viewModelScope.launch(Dispatchers.IO) {
            // 解析在线m3u8 or mp4 视频
            mResolveStateFlow.update { ResolveVideoState.Loading }
            val result = VideoResolve.getVideoInfo(url,title,imgUrl,ext)
            result.onSuccess { videoInfo ->
                withContext(Dispatchers.Main) {
                    mResolveStateFlow.update { ResolveVideoState.ResolveSuccess(videoInfo) }
                }
                Log.d("WebViewWidget", "标题: $title")
                Log.d("WebViewWidget", "封面: $imgUrl")
                Log.d("WebViewWidget", "时长: ${VideoResolve.formatDuration(videoInfo.duration)}")
                Log.d("WebViewWidget", "分辨率: ${videoInfo.resolution} (${videoInfo.width}x${videoInfo.height})")
                Log.d("WebViewWidget", "大小: ${VideoResolve.formatFileSize(videoInfo.size)}")
                Log.d("WebViewWidget", "码率: ${VideoResolve.formatBitrate(videoInfo.bitrate)}")
                Log.d("WebViewWidget", "编码: ${videoInfo.codec}")
                Log.d("WebViewWidget", "帧率: ${videoInfo.frameRate}")
                Log.d("WebViewWidget", "视频类型: ${videoInfo.ext}")
//                launch {
//                    DataRepository.reportSupported(url)
//                }
            }.onFailure { e ->
                mResolveStateFlow.update { ResolveVideoState.Idle }
                Log.d("WebViewWidget", "解析失败:${e.message} ")
            }
        }

    }





    private var resolveJob: Job?=null //解析页面 封面和标题
    private var resolveVideoJob:Job ?=null //解析页面视频资源链接
    /**
     * 检查URL是否为视频资源
     */
    fun checkVideoUrl(
        url: String,
        discoveredUrls: MutableSet<String>,
        callback: (String, String, String, String) -> Unit,
        view: WebView,
    ) {
        //pornhub不检查
        if (url.toUri().host?.contains("evtubescms.phncdn.com") == true
            || url.toUri().host?.contains("pix-cdn77.phncdn.com") == true
            || url.toUri().host?.contains("etahub.com") == true
            || url.toUri().host?.contains("video.sacdnssedge.com") == true
        ) {
            return
        }
        //tiktok不检查
//        if (url.toUri().host?.contains("tiktok") == true
//        ) {
//            return
//        }

        if (url.toUri().host?.contains("vod3.cf.dmcdn.net") == true
            && !url.contains("manifest.m3u8")){
            return
        }

        if (isVideoUrl(url) && !discoveredUrls.contains(url)) {
            discoveredUrls.add(url)
            val ext = getUrlExt(url)
            // 使用协程获取标题
            resolveJob = CoroutineScope(Dispatchers.Main).launch {
                val (title, image) = injectTitleDetectionScript(view)
                //获取到标题后，将标题和视频的url一起callback出去
                Log.d("WebViewWidget", "解析出现的视频资源----->${url} ")
                callback(url, title,image,ext)
            }
        }
    }


    fun getUrlExt(url: String): String{
        if (url.contains("m3u8",true)){
            return "m3u8"
        }else{
            return "mp4"
        }
    }

    /**
     * 判断URL是否为视频资源
     */
    fun isVideoUrl(url: String): Boolean {
        if (url.isBlank()) return false

        val lowerUrl = url.lowercase()

        // 排除日志上报URL
        if (lowerUrl.contains("data.bilibili.com/log") ||
            lowerUrl.contains("/log/web") ||
            lowerUrl.contains("analytics") ||
            lowerUrl.contains("tracking") ||
            lowerUrl.contains("beacon") ||
            lowerUrl.contains("collect") ||
            lowerUrl.contains("report")) {
            return false
        }

        // 检查URL扩展名
        val videoExtensions = if (lowerUrl.contains("bilibili")){
            listOf(".m3u8", ".mp4")
        }else{
            listOf("m3u8", "mp4")
        }

        if (lowerUrl.contains("phncdn")) {
            if (videoExtensions.any { lowerUrl.contains(it) } && lowerUrl.contains("master")) {
                return true
            }else{
                return false
            }
        }


        if (videoExtensions.any { lowerUrl.contains(it) }) {
            return true
        }

        return false
    }

    /**
     * 注入JavaScript代码来检测页面title
     */
    suspend fun injectTitleDetectionScript(webView: WebView): Pair<String, String> {
        return suspendCancellableCoroutine { continuation ->
            val script = """
            (function() {
                // 获取<title>
                var title = '';
                var headElement = document.head;
                if (headElement) {
                    var titleElement = headElement.querySelector('title');
                    if (titleElement) {
                        title = titleElement.textContent || titleElement.innerText || '';
                    }
                }
                // 获取第一个图片格式的 meta[content]
                var image = '';
                if (headElement) {
                    var metas = headElement.querySelectorAll('meta[content]');
                    var imgPattern = /\.(jpg|jpeg|png|webp)/i;
                    for (var i = 0; i < metas.length; i++) {
                        var content = metas[i].getAttribute('content');
                        if (content && imgPattern.test(content)) {
                            image = content;
                            break;
                        }
                    }
                }
                return JSON.stringify({ title: title, image: image });
            })();
        """.trimIndent()

            Log.d("WebViewWidget", "注入title和meta-image检测脚本")
            webView.evaluateJavascript(script) { result ->
                Log.d("WebViewWidget", "检测脚本执行结果: $result")
                if (!result.isNullOrEmpty() && result != "null") {
                    val json = result.trim('"')
                        .replace("\\\"", "\"")
                    try {
                        val regex = """\{"title":"(.*?)","image":"(.*?)"\}""".toRegex()
                        val match = regex.find(json)
                        val title = match?.groups?.get(1)?.value ?: ""
                        val image = match?.groups?.get(2)?.value ?: ""
                        Log.d("WebViewWidget", "发现页面标题: $title, 图片: $image")
                        continuation.resume(title to image) { _: Throwable -> }
                    } catch (e: Exception) {
                        continuation.resume("" to "") { _: Throwable -> }
                    }
                } else {
                    continuation.resume("" to "") { _: Throwable -> }
                }
            }
        }
    }




}