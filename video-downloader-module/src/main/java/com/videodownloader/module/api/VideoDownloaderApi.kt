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
)