package com.app.videobox.utils

import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频元数据工具类
 * 
 * 提供获取视频文件元数据信息的功能：
 * - 视频时长（毫秒）
 * - 文件大小（字节）
 * - 视频分辨率
 * - 视频比特率
 * 
 * 设计目的：
 * - 统一视频元数据获取逻辑
 * - 提供异步和同步两种获取方式
 * - 支持Compose组件中的响应式使用
 * - 确保UI线程不被阻塞
 */
object VideoMetadataUtils {
    
    private const val TAG = "VideoMetadataUtils"
    
    /**
     * 视频元数据信息数据类
     * 
     * @property duration 视频时长（毫秒）
     * @property fileSize 文件大小（字节）
     * @property width 视频宽度（像素）
     * @property height 视频高度（像素）
     * @property bitrate 视频比特率（bps）
     */
    data class VideoMetadata(
        val duration: Long = 0L,
        val fileSize: Long = 0L,
        val width: Int = 0,
        val height: Int = 0,
        val bitrate: Int = 0
    )
    
    /**
     * 同步获取视频文件的元数据信息
     * 
     * @param filePath 视频文件路径
     * @return VideoMetadata 视频元数据信息，获取失败时返回默认值
     */
    fun getVideoMetadata(filePath: String): VideoMetadata {
        val file = File(filePath)
        if (!file.exists()) {
            Log.w(TAG, "视频文件不存在: $filePath")
            return VideoMetadata()
        }
        
        val retriever = MediaMetadataRetriever()
        return try {
            // 设置数据源
            retriever.setDataSource(filePath)
            
            // 获取视频时长（毫秒）
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            
            // 获取文件大小
            val fileSize = file.length()
            
            // 获取视频分辨率
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            
            // 获取视频比特率
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            
            Log.d(TAG, "获取视频元数据成功: 文件=$filePath, 时长=${duration}ms, 大小=${fileSize}bytes, 分辨率=${width}x${height}, 比特率=${bitrate}bps")
            
            VideoMetadata(
                duration = duration,
                fileSize = fileSize,
                width = width,
                height = height,
                bitrate = bitrate
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取视频元数据失败: $filePath", e)
            VideoMetadata(fileSize = file.length()) // 至少返回文件大小
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.w(TAG, "释放MediaMetadataRetriever失败", e)
            }
        }
    }
    
    /**
     * 异步获取视频文件的元数据信息
     * 
     * @param filePath 视频文件路径
     * @return VideoMetadata 视频元数据信息，获取失败时返回默认值
     */
    suspend fun getVideoMetadataAsync(filePath: String): VideoMetadata = withContext(Dispatchers.IO) {
        getVideoMetadata(filePath)
    }
    
    /**
     * 获取视频时长（毫秒）
     * 兼容原有的FileUtils.getVideoDuration方法
     * 
     * @param file 视频文件
     * @return 视频时长（毫秒），获取失败时返回0
     */
    fun getVideoDuration(file: File): Long {
        return getVideoMetadata(file.absolutePath).duration
    }
    
    /**
     * 获取视频时长（秒）
     * 
     * @param filePath 视频文件路径
     * @return 视频时长（秒），获取失败时返回0
     */
    fun getVideoDurationInSeconds(filePath: String): Int {
        return (getVideoMetadata(filePath).duration / 1000).toInt()
    }
}

/**
 * Compose组件中使用的视频元数据Hook
 * 
 * 在Compose组件中异步获取视频元数据，避免阻塞UI线程
 * 
 * @param filePath 视频文件路径，为null时不执行获取操作
 * @return VideoMetadataUtils.VideoMetadata 视频元数据信息
 */
@Composable
fun rememberVideoMetadata(filePath: String?): VideoMetadataUtils.VideoMetadata {
    var metadata by remember { mutableStateOf(VideoMetadataUtils.VideoMetadata()) }
    
    LaunchedEffect(filePath) {
        if (!filePath.isNullOrBlank()) {
            try {
                metadata = VideoMetadataUtils.getVideoMetadataAsync(filePath)
            } catch (e: Exception) {
                Log.e("rememberVideoMetadata", "获取视频元数据失败: $filePath", e)
                // 保持默认值
            }
        }
    }
    
    return metadata
}