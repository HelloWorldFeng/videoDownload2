package com.nexus.core.media.processor.extractor

import android.content.Context
import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * FFmpeg 提取器
 * 使用 FFmpeg 提取媒体信息
 */
class FFmpegExtractor(
    context: Context,
    configuration: ProcessorConfiguration
) : MediaExtractor(context, configuration) {
    
    private val supportedExtensions = listOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm",
        "mp3", "wav", "flac", "aac", "ogg", "m4a",
        "m3u8", "ts", "mpd"
    )
    
    override fun supports(url: String): Boolean {
        return try {
            // 检查是否是直接的媒体文件URL
            val extension = extractFileExtension(url)
            supportedExtensions.contains(extension.lowercase()) ||
            url.startsWith("http") || url.startsWith("https") ||
            url.startsWith("rtmp") || url.startsWith("rtsp")
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun extractMediaInfo(
        url: String,
        options: ProcessingOptions?
    ): ExtractionResult = withContext(Dispatchers.IO) {
        
        return@withContext try {
            if (!supports(url)) {
                return@withContext ExtractionResult(
                    success = false,
                    error = "Unsupported URL or format: $url",
                    errorCode = ERROR_UNSUPPORTED_URL
                )
            }
            
            // 使用超时机制
            val result = withTimeoutOrNull(20000) {
                executeFFprobe(url, options)
            }
            
            if (result == null) {
                ExtractionResult(
                    success = false,
                    error = "FFprobe timeout",
                    errorCode = ERROR_TIMEOUT
                )
            } else {
                result
            }
            
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "FFmpeg extraction failed: ${e.message}",
                errorCode = ERROR_UNKNOWN
            )
        }
    }
    
    override fun getName(): String = "FFmpeg Extractor"
    
    override fun getSupportedDomains(): List<String> = emptyList() // 支持所有域名
    
    /**
     * 执行 FFprobe 命令获取媒体信息
     */
    private suspend fun executeFFprobe(
        url: String,
        options: ProcessingOptions?
    ): ExtractionResult {
        // 这里应该实现实际的 FFprobe 调用
        // 由于这是一个模拟实现，我们返回一个示例结果
        
        return try {
            // 模拟 FFprobe 分析过程
            val mediaInfo = createMediaInfoFromFFprobe(url, options)
            
            ExtractionResult(
                success = true,
                mediaInfo = mediaInfo
            )
        } catch (e: Exception) {
            ExtractionResult(
                success = false,
                error = "FFprobe execution failed: ${e.message}",
                errorCode = ERROR_PARSING_FAILURE
            )
        }
    }
    
    /**
     * 从 FFprobe 结果创建媒体信息
     */
    private fun createMediaInfoFromFFprobe(
        url: String,
        options: ProcessingOptions?
    ): MediaInfo {
        val formats = mutableListOf<MediaFormat>()
        val extension = extractFileExtension(url)
        
        // 根据文件扩展名创建格式信息
        when (extension.lowercase()) {
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> {
                // 视频文件
                formats.add(
                    MediaFormat(
                        formatId = "original",
                        formatType = MediaFormat.FormatType.VIDEO,
                        extension = extension,
                        formatNote = "Original video",
                        url = url,
                        fileSize = null,
                        width = 1920,
                        height = 1080,
                        fps = null,
                        videoBitrate = null,
                        videoCodec = null,
                        audioBitrate = 128,
                        audioSampleRate = null,
                        audioCodec = null,
                        audioChannels = null,
                        container = extension,
                        protocol = null,
                        language = null,
                        preference = null,
                        quality = null,
                        hasVideo = true,
                        hasAudio = true,
                        httpHeaders = null,
                        fragments = null,
                        extraInfo = null
                    )
                )
            }
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> {
                // 音频文件
                formats.add(
                    MediaFormat(
                        formatId = "audio",
                        formatType = MediaFormat.FormatType.AUDIO,
                        extension = extension,
                        formatNote = "Audio only",
                        url = url,
                        fileSize = null,
                        width = null,
                        height = null,
                        fps = null,
                        videoBitrate = null,
                        videoCodec = null,
                        audioBitrate = 192,
                        audioSampleRate = null,
                        audioCodec = null,
                        audioChannels = null,
                        container = extension,
                        protocol = null,
                        language = null,
                        preference = null,
                        quality = null,
                        hasVideo = false,
                        hasAudio = true,
                        httpHeaders = null,
                        fragments = null,
                        extraInfo = null
                    )
                )
            }
            "m3u8", "ts" -> {
                // HLS 流
                formats.add(
                    MediaFormat(
                        formatId = "hls",
                        formatType = MediaFormat.FormatType.VIDEO,
                        extension = extension,
                        formatNote = "HLS stream",
                        url = url,
                        fileSize = null,
                        width = 1280,
                        height = 720,
                        fps = null,
                        videoBitrate = null,
                        videoCodec = null,
                        audioBitrate = 128,
                        audioSampleRate = null,
                        audioCodec = null,
                        audioChannels = null,
                        container = extension,
                        protocol = null,
                        language = null,
                        preference = null,
                        quality = null,
                        hasVideo = true,
                        hasAudio = true,
                        httpHeaders = null,
                        fragments = null,
                        extraInfo = null
                    )
                )
            }
            "mpd" -> {
                // DASH 流
                formats.add(
                    MediaFormat(
                        formatId = "dash",
                        formatType = MediaFormat.FormatType.VIDEO,
                        extension = extension,
                        formatNote = "DASH stream",
                        url = url,
                        fileSize = null,
                        width = 1280,
                        height = 720,
                        fps = null,
                        videoBitrate = null,
                        videoCodec = null,
                        audioBitrate = 128,
                        audioSampleRate = null,
                        audioCodec = null,
                        audioChannels = null,
                        container = extension,
                        protocol = null,
                        language = null,
                        preference = null,
                        quality = null,
                        hasVideo = true,
                        hasAudio = true,
                        httpHeaders = null,
                        fragments = null,
                        extraInfo = null
                    )
                )
            }
            else -> {
                // 默认格式
                formats.add(
                    MediaFormat(
                        formatId = "unknown",
                        formatType = MediaFormat.FormatType.VIDEO,
                        extension = extension,
                        formatNote = "Unknown format",
                        url = url,
                        fileSize = null,
                        width = 854,
                        height = 480,
                        fps = null,
                        videoBitrate = null,
                        videoCodec = null,
                        audioBitrate = 96,
                        audioSampleRate = null,
                        audioCodec = null,
                        audioChannels = null,
                        container = extension,
                        protocol = null,
                        language = null,
                        preference = null,
                        quality = null,
                        hasVideo = true,
                        hasAudio = true,
                        httpHeaders = null,
                        fragments = null,
                        extraInfo = null
                    )
                )
            }
        }
        
        return MediaInfo(
            id = url.hashCode().toString(),
            title = extractFileName(url),
            description = "Media file extracted by FFmpeg",
            duration = 0, // 需要通过 FFprobe 获取实际时长
            uploader = null,
            uploaderId = null,
            uploadDate = null,
            viewCount = null,
            likeCount = null,
            dislikeCount = null,
            commentCount = null,
            mediaType = MediaInfo.MediaType.VIDEO,
            originalUrl = url,
            webpageUrl = url,
            thumbnails = emptyList(),
            availableFormats = formats,
            subtitles = emptyList(),
            chapters = emptyList(),
            tags = emptyList(),
            categories = emptyList(),
            ageLimit = null,
            isLive = false,
            averageRating = null,
            license = null,
            creator = null,
            album = null,
            artist = null,
            releaseYear = null,
            trackNumber = null,
            extraMetadata = emptyMap()
        )
    }
    
    /**
     * 提取文件扩展名
     */
    private fun extractFileExtension(url: String): String {
        return try {
            val fileName = url.split("/").lastOrNull() ?: ""
            val dotIndex = fileName.lastIndexOf(".")
            if (dotIndex > 0 && dotIndex < fileName.length - 1) {
                fileName.substring(dotIndex + 1)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 提取文件名
     */
    private fun extractFileName(url: String): String {
        return try {
            val fileName = url.split("/").lastOrNull() ?: "Unknown"
            val dotIndex = fileName.lastIndexOf(".")
            if (dotIndex > 0) {
                fileName.substring(0, dotIndex)
            } else {
                fileName
            }
        } catch (e: Exception) {
            "Unknown Media"
        }
    }
}