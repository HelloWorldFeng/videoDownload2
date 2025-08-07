package com.nexus.core.media.processor.fetcher

import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.utils.ProcessorLogUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 媒体信息提取器
 * 
 * 负责从各种媒体平台提取媒体信息，
 * 包括视频、音频的元数据和可用格式。
 * 
 * 主要功能：
 * - 多平台支持
 * - 媒体信息解析
 * - 格式列表获取
 * - 缓存机制
 * - 错误处理
 * 
 * 支持的平台：
 * - YouTube
 * - Bilibili
 * - 通用HTTP媒体
 * - 本地文件
 * 
 * 使用示例：
 * ```kotlin
 * val extractor = MediaInfoExtractor(configuration)
 * val mediaInfo = extractor.extractMediaInfo(url)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class MediaInfoExtractor(
    private val configuration: ProcessorConfiguration
) {
    
    companion object {
        private const val LOG_TAG = "MediaInfoExtractor"
        
        // 平台识别正则
        private val YOUTUBE_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?"
            + "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)"
            + "([a-zA-Z0-9_-]{11})"
        )
        
        private val BILIBILI_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?bilibili\\.com/video/"
            + "(av\\d+|BV[a-zA-Z0-9]+)"
        )
        
        private val GENERIC_VIDEO_PATTERN = Pattern.compile(
            ".*\\.(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ts|m3u8)(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE
        )
        
        private val GENERIC_AUDIO_PATTERN = Pattern.compile(
            ".*\\.(mp3|aac|flac|wav|ogg|m4a|wma|opus)(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE
        )
    }
    
    // HTTP客户端
    private val httpClient: OkHttpClient by lazy {
        createHttpClient()
    }
    
    // 平台提取器
    private val platformExtractors = mutableMapOf<String, PlatformExtractor>()
    
    // 缓存
    private val infoCache = mutableMapOf<String, CachedMediaInfo>()
    
    init {
        initializePlatformExtractors()
    }
    
    /**
     * 缓存的媒体信息
     */
    private data class CachedMediaInfo(
        val mediaInfo: MediaInfo,
        val timestamp: Long
    )
    
    /**
     * 提取媒体信息
     * 
     * @param url 媒体URL
     * @param useCache 是否使用缓存
     * @return 媒体信息
     */
    suspend fun extractMediaInfo(
        url: String,
        useCache: Boolean = true
    ): MediaInfo = withContext(Dispatchers.IO) {
        try {
            ProcessorLogUtils.d(LOG_TAG, "开始提取媒体信息: $url")
            
            // 检查缓存
            if (useCache) {
                val cached = getCachedInfo(url)
                if (cached != null) {
                    ProcessorLogUtils.d(LOG_TAG, "使用缓存的媒体信息: $url")
                    return@withContext cached
                }
            }
            
            // 识别平台
            val platform = identifyPlatform(url)
            ProcessorLogUtils.d(LOG_TAG, "识别平台: $platform")
            
            // 提取信息
            val mediaInfo = when (platform) {
                "youtube" -> extractYouTubeInfo(url)
                "bilibili" -> extractBilibiliInfo(url)
                "generic_video" -> extractGenericVideoInfo(url)
                "generic_audio" -> extractGenericAudioInfo(url)
                "local_file" -> extractLocalFileInfo(url)
                else -> extractGenericMediaInfo(url)
            }
            
            // 缓存结果
            if (useCache) {
                cacheInfo(url, mediaInfo)
            }
            
            ProcessorLogUtils.i(LOG_TAG, "媒体信息提取完成: ${mediaInfo.title}")
            mediaInfo
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "提取媒体信息失败: $url", e)
            throw MediaExtractionException("提取媒体信息失败: ${e.message}", e)
        }
    }
    
    /**
     * 批量提取媒体信息
     * 
     * @param urls URL列表
     * @param useCache 是否使用缓存
     * @return 媒体信息列表
     */
    suspend fun extractMediaInfoBatch(
        urls: List<String>,
        useCache: Boolean = true
    ): List<MediaInfo> = withContext(Dispatchers.IO) {
        try {
            ProcessorLogUtils.d(LOG_TAG, "批量提取媒体信息: ${urls.size} 个URL")
            
            val results = urls.map { url ->
                async {
                    try {
                        extractMediaInfo(url, useCache)
                    } catch (e: Exception) {
                        ProcessorLogUtils.e(LOG_TAG, "批量提取失败: $url", e)
                        null
                    }
                }
            }.awaitAll()
            
            val successResults = results.filterNotNull()
            ProcessorLogUtils.i(LOG_TAG, "批量提取完成: ${successResults.size}/${urls.size}")
            
            successResults
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "批量提取媒体信息失败", e)
            throw e
        }
    }
    
    /**
     * 识别平台
     */
    private fun identifyPlatform(url: String): String {
        return when {
            YOUTUBE_PATTERN.matcher(url).find() -> "youtube"
            BILIBILI_PATTERN.matcher(url).find() -> "bilibili"
            GENERIC_VIDEO_PATTERN.matcher(url).find() -> "generic_video"
            GENERIC_AUDIO_PATTERN.matcher(url).find() -> "generic_audio"
            url.startsWith("file://") || !url.contains("://") -> "local_file"
            else -> "generic"
        }
    }
    
    /**
     * 提取YouTube信息
     */
    private suspend fun extractYouTubeInfo(url: String): MediaInfo {
        val extractor = platformExtractors["youtube"] ?: throw MediaExtractionException("YouTube提取器未初始化")
        return extractor.extract(url)
    }
    
    /**
     * 提取Bilibili信息
     */
    private suspend fun extractBilibiliInfo(url: String): MediaInfo {
        val extractor = platformExtractors["bilibili"] ?: throw MediaExtractionException("Bilibili提取器未初始化")
        return extractor.extract(url)
    }
    
    /**
     * 提取通用视频信息
     */
    private suspend fun extractGenericVideoInfo(url: String): MediaInfo {
        return try {
            val response = httpClient.newCall(
                Request.Builder()
                    .url(url)
                    .head()
                    .build()
            ).execute()
            
            response.use {
                if (!it.isSuccessful) {
                    throw MediaExtractionException("无法访问媒体文件: ${it.code}")
                }
                
                val contentType = it.header("Content-Type") ?: "video/mp4"
                val contentLength = it.body?.contentLength() ?: -1L
                val fileName = extractFileNameFromUrl(url)
                
                // 创建基本格式信息
                val format = MediaFormat(
                    formatId = "direct",
                    formatType = if (contentType.startsWith("video/")) MediaFormat.FormatType.VIDEO else MediaFormat.FormatType.AUDIO,
                    extension = getExtensionFromUrl(url),
                    formatNote = "直接链接",
                    url = url,
                    fileSize = if (contentLength > 0) contentLength else null,
                    width = null,
                    height = null,
                    fps = null,
                    videoBitrate = null,
                    videoCodec = if (contentType.startsWith("video/")) "unknown" else null,
                    audioBitrate = null,
                    audioSampleRate = null,
                    audioCodec = if (contentType.startsWith("audio/")) "unknown" else "unknown",
                    audioChannels = null,
                    container = getContainerFromContentType(contentType),
                    protocol = null,
                    language = null,
                    preference = null,
                    quality = null,
                    hasVideo = contentType.startsWith("video/"),
                    hasAudio = true,
                    httpHeaders = null,
                    fragments = null,
                    extraInfo = null
                )
                
                MediaInfo(
                    id = generateIdFromUrl(url),
                    title = fileName,
                    description = "通用视频文件",
                    duration = null,
                    uploader = null,
                    uploaderId = null,
                    uploadDate = null,
                    viewCount = null,
                    likeCount = null,
                    dislikeCount = null,
                    commentCount = null,
                    mediaType = if (contentType.startsWith("video/")) MediaInfo.MediaType.VIDEO else MediaInfo.MediaType.AUDIO,
                    originalUrl = url,
                    webpageUrl = null,
                    availableFormats = listOf(format),
                    thumbnails = emptyList(),
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
        } catch (e: Exception) {
            throw MediaExtractionException("提取通用视频信息失败: ${e.message}", e)
        }
    }
    
    /**
     * 提取通用音频信息
     */
    private suspend fun extractGenericAudioInfo(url: String): MediaInfo {
        return extractGenericVideoInfo(url).copy(
            mediaType = MediaInfo.MediaType.AUDIO,
            description = "通用音频文件"
        )
    }
    
    /**
     * 提取本地文件信息
     */
    private suspend fun extractLocalFileInfo(url: String): MediaInfo {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = if (url.startsWith("file://")) {
                    url.substring(7)
                } else {
                    url
                }
                
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    throw MediaExtractionException("文件不存在: $filePath")
                }
                
                val fileName = file.name
                val fileSize = file.length()
                val extension = file.extension.lowercase()
                
                val mediaType = when (extension) {
                    in listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts") -> MediaInfo.MediaType.VIDEO
                    in listOf("mp3", "aac", "flac", "wav", "ogg", "m4a", "wma", "opus") -> MediaInfo.MediaType.AUDIO
                    else -> MediaInfo.MediaType.VIDEO
                }
                
                val format = MediaFormat(
                    formatId = "local",
                    formatType = if (mediaType == MediaInfo.MediaType.VIDEO) MediaFormat.FormatType.VIDEO else MediaFormat.FormatType.AUDIO,
                    extension = extension,
                    formatNote = "本地文件",
                    url = url,
                    fileSize = fileSize,
                    width = null,
                    height = null,
                    fps = null,
                    videoBitrate = null,
                    videoCodec = if (mediaType == MediaInfo.MediaType.VIDEO) "unknown" else null,
                    audioBitrate = null,
                    audioSampleRate = null,
                    audioCodec = "unknown",
                    audioChannels = null,
                    container = extension,
                    protocol = null,
                    language = null,
                    preference = null,
                    quality = null,
                    hasVideo = mediaType == MediaInfo.MediaType.VIDEO,
                    hasAudio = true,
                    httpHeaders = null,
                    fragments = null,
                    extraInfo = null
                )
                
                MediaInfo(
                    id = generateIdFromUrl(url),
                    title = fileName,
                    description = "本地媒体文件",
                    duration = null,
                    uploader = null,
                    uploaderId = null,
                    uploadDate = null,
                    viewCount = null,
                    likeCount = null,
                    dislikeCount = null,
                    commentCount = null,
                    mediaType = mediaType,
                    originalUrl = url,
                    webpageUrl = url,
                    availableFormats = listOf(format),
                    thumbnails = emptyList(),
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
                    extraMetadata = mapOf(
                        "file_path" to filePath,
                        "file_size" to fileSize.toString()
                    )
                )
                
            } catch (e: Exception) {
                throw MediaExtractionException("提取本地文件信息失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 提取通用媒体信息
     */
    private suspend fun extractGenericMediaInfo(url: String): MediaInfo {
        return try {
            // 尝试作为视频处理
            extractGenericVideoInfo(url)
        } catch (e: Exception) {
            // 如果失败，创建最基本的信息
            val fileName = extractFileNameFromUrl(url)
            val format = MediaFormat(
                formatId = "unknown",
                formatType = MediaFormat.FormatType.UNKNOWN,
                extension = getExtensionFromUrl(url),
                formatNote = "未知格式",
                url = url,
                fileSize = null,
                width = null,
                height = null,
                fps = null,
                videoBitrate = null,
                videoCodec = null,
                audioBitrate = null,
                audioSampleRate = null,
                audioCodec = null,
                audioChannels = null,
                container = null,
                protocol = null,
                language = null,
                preference = null,
                quality = null,
                hasVideo = false,
                hasAudio = false,
                httpHeaders = null,
                fragments = null,
                extraInfo = null
            )
            
            MediaInfo(
                id = generateIdFromUrl(url),
                title = fileName,
                description = "未知媒体类型",
                duration = null,
                uploader = null,
                uploaderId = null,
                uploadDate = null,
                viewCount = null,
                likeCount = null,
                dislikeCount = null,
                commentCount = null,
                mediaType = MediaInfo.MediaType.UNKNOWN,
                originalUrl = url,
                webpageUrl = null,
                availableFormats = listOf(format),
                thumbnails = emptyList(),
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
    }
    
    /**
     * 初始化平台提取器
     */
    private fun initializePlatformExtractors() {
        try {
            // YouTube提取器
            platformExtractors["youtube"] = YouTubeExtractor(httpClient)
            
            // Bilibili提取器
            platformExtractors["bilibili"] = BilibiliExtractor(httpClient)
            
            ProcessorLogUtils.d(LOG_TAG, "平台提取器初始化完成")
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "初始化平台提取器失败", e)
        }
    }
    
    /**
     * 获取缓存信息
     */
    private fun getCachedInfo(url: String): MediaInfo? {
        val cached = infoCache[url] ?: return null
        
        // 检查缓存是否过期（1小时）
        val cacheExpiry = 60 * 60 * 1000L
        if (System.currentTimeMillis() - cached.timestamp > cacheExpiry) {
            infoCache.remove(url)
            return null
        }
        
        return cached.mediaInfo
    }
    
    /**
     * 缓存信息
     */
    private fun cacheInfo(url: String, mediaInfo: MediaInfo) {
        infoCache[url] = CachedMediaInfo(mediaInfo, System.currentTimeMillis())
        
        // 限制缓存大小
        if (infoCache.size > 100) {
            val oldestEntry = infoCache.minByOrNull { it.value.timestamp }
            oldestEntry?.let { infoCache.remove(it.key) }
        }
    }
    
    /**
     * 从URL提取文件名
     */
    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val urlObj = URL(url)
            val path = urlObj.path
            val fileName = path.substringAfterLast('/')
            if (fileName.isNotEmpty()) fileName else "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * 从URL获取扩展名
     */
    private fun getExtensionFromUrl(url: String): String? {
        val fileName = extractFileNameFromUrl(url)
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).lowercase()
        } else {
            null
        }
    }
    
    /**
     * 从Content-Type获取容器格式
     */
    private fun getContainerFromContentType(contentType: String): String? {
        return when {
            contentType.contains("mp4") -> "mp4"
            contentType.contains("webm") -> "webm"
            contentType.contains("avi") -> "avi"
            contentType.contains("mkv") -> "mkv"
            contentType.contains("mp3") -> "mp3"
            contentType.contains("aac") -> "aac"
            contentType.contains("flac") -> "flac"
            contentType.contains("ogg") -> "ogg"
            else -> null
        }
    }
    
    /**
     * 从URL生成ID
     */
    private fun generateIdFromUrl(url: String): String {
        return url.hashCode().toString()
    }
    
    /**
     * 创建HTTP客户端
     */
    private fun createHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(configuration.connectionTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(configuration.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(configuration.writeTimeout, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", configuration.userAgent)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * 清理缓存
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            infoCache.clear()
            ProcessorLogUtils.d(LOG_TAG, "缓存已清理")
        }
    }
    
    /**
     * 清理资源
     */
    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            try {
                clearCache()
                platformExtractors.values.forEach { it.cleanup() }
                platformExtractors.clear()
                ProcessorLogUtils.d(LOG_TAG, "媒体信息提取器清理完成")
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "清理媒体信息提取器失败", e)
            }
        }
    }
}

/**
 * 媒体提取异常
 */
class MediaExtractionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 平台提取器接口
 */
interface PlatformExtractor {
    suspend fun extract(url: String): MediaInfo
    suspend fun cleanup()
}

/**
 * YouTube提取器
 */
class YouTubeExtractor(private val httpClient: OkHttpClient) : PlatformExtractor {
    
    override suspend fun extract(url: String): MediaInfo {
        // TODO: 实现YouTube信息提取
        throw MediaExtractionException("YouTube提取器尚未实现")
    }
    
    override suspend fun cleanup() {
        // 清理资源
    }
}

/**
 * Bilibili提取器
 */
class BilibiliExtractor(private val httpClient: OkHttpClient) : PlatformExtractor {
    
    override suspend fun extract(url: String): MediaInfo {
        // TODO: 实现Bilibili信息提取
        throw MediaExtractionException("Bilibili提取器尚未实现")
    }
    
    override suspend fun cleanup() {
        // 清理资源
    }
}