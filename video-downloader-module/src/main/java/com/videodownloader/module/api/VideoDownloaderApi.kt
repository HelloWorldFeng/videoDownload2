import kotlinx.serialization.Serializable

/**
 * 视频信息
 */
@Serializable
data class VideoInfo(
    val id: String,
    val title: String,
    val duration: Float, // 秒
    val thumbnail: String,
    val size: Long,
    val url: String,
    val ext: String,
    // 新增HTTP上下文信息字段
    val httpHeaders: Map<String, String> = emptyMap(), // WebView的请求头
    val cookies: String = "", // WebView的cookies
    val referer: String = "", // 来源页面URL
    val userAgent: String = "" // WebView的User-Agent
)