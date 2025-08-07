package com.nexus.core.media.processor.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Date

/**
 * 媒体信息数据类
 * 
 * 包含从媒体URL提取的所有相关信息，
 * 包括基本信息、格式列表、缩略图等。
 * 
 * 主要信息：
 * - 基本媒体信息（标题、描述、时长等）
 * - 可用格式列表
 * - 缩略图信息
 * - 上传者信息
 * - 统计数据
 * 
 * 使用示例：
 * ```kotlin
 * val mediaInfo = extractor.extractMediaInfo("https://example.com/video")
 * val title = mediaInfo.title
 * val formats = mediaInfo.availableFormats
 * val bestFormat = mediaInfo.getOptimalFormat()
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
@Parcelize
@Serializable
data class MediaInfo(
    /** 媒体唯一标识符 */
    val id: String,
    
    /** 媒体标题 */
    val title: String,
    
    /** 媒体描述 */
    val description: String?,
    
    /** 媒体时长(秒) */
    val duration: Long?,
    
    /** 上传者名称 */
    val uploader: String?,
    
    /** 上传者ID */
    val uploaderId: String?,
    
    /** 上传日期 */
    @Transient
    val uploadDate: Date? = null,
    
    /** 观看次数 */
    val viewCount: Long?,
    
    /** 点赞数 */
    val likeCount: Long?,
    
    /** 点踩数 */
    val dislikeCount: Long?,
    
    /** 评论数 */
    val commentCount: Long?,
    
    /** 媒体类型 */
    val mediaType: MediaType,
    
    /** 原始URL */
    val originalUrl: String,
    
    /** 网页URL */
    val webpageUrl: String?,
    
    /** 可用格式列表 */
    val availableFormats: List<MediaFormat>,
    
    /** 缩略图列表 */
    @Transient
    val thumbnails: List<ThumbnailInfo> = emptyList(),
    
    /** 字幕列表 */
    @Transient
    val subtitles: List<SubtitleInfo> = emptyList(),
    
    /** 章节信息 */
    @Transient
    val chapters: List<ChapterInfo> = emptyList(),
    
    /** 标签列表 */
    val tags: List<String>,
    
    /** 分类信息 */
    val categories: List<String>,
    
    /** 年龄限制 */
    val ageLimit: Int?,
    
    /** 是否为直播 */
    val isLive: Boolean,
    
    /** 平均评分 */
    val averageRating: Float?,
    
    /** 许可证信息 */
    val license: String?,
    
    /** 创建者信息 */
    val creator: String?,
    
    /** 专辑信息 */
    val album: String?,
    
    /** 艺术家信息 */
    val artist: String?,
    
    /** 发行年份 */
    val releaseYear: Int?,
    
    /** 音轨编号 */
    val trackNumber: Int?,
    
    /** 额外元数据 */
    @Transient
    val extraMetadata: @RawValue Map<String, Any> = emptyMap()
) : Parcelable {
    
    /**
     * 媒体类型枚举
     */
    enum class MediaType {
        VIDEO,      // 视频
        AUDIO,      // 音频
        PLAYLIST,   // 播放列表
        LIVE,       // 直播
        UNKNOWN     // 未知类型
    }
    
    /**
     * 获取最佳质量格式
     * 
     * 根据质量优先级选择最佳格式。
     * 
     * @param preferredType 偏好的格式类型
     * @return 最佳格式，如果没有可用格式则返回null
     */
    fun getOptimalFormat(preferredType: MediaFormat.FormatType? = null): MediaFormat? {
        if (availableFormats.isEmpty()) return null
        
        val filteredFormats = if (preferredType != null) {
            availableFormats.filter { it.formatType == preferredType }
        } else {
            availableFormats
        }
        
        if (filteredFormats.isEmpty()) return availableFormats.firstOrNull()
        
        // 按质量排序，选择最高质量
        return filteredFormats.maxByOrNull { format ->
            when {
                format.height != null -> format.height!! * (format.width ?: 1)
                format.audioBitrate != null -> format.audioBitrate!!
                else -> 0
            }
        }
    }
    
    /**
     * 获取指定质量的格式
     * 
     * @param quality 目标质量
     * @return 匹配的格式列表
     */
    fun getFormatsByQuality(quality: MediaQuality): List<MediaFormat> {
        return availableFormats.filter { format ->
            when (quality) {
                MediaQuality.LOW -> {
                    format.height != null && format.height!! <= 480
                }
                MediaQuality.MEDIUM -> {
                    format.height != null && format.height!! in 481..720
                }
                MediaQuality.HIGH -> {
                    format.height != null && format.height!! in 721..1080
                }
                MediaQuality.ULTRA -> {
                    format.height != null && format.height!! > 1080
                }
                MediaQuality.AUDIO_ONLY -> {
                    format.formatType == MediaFormat.FormatType.AUDIO
                }
                MediaQuality.BEST -> {
                    true // 返回所有格式，后续会排序选择最佳
                }
                MediaQuality.WORST -> {
                    true // 返回所有格式，后续会排序选择最差
                }
                MediaQuality.UHD_4K -> {
                    format.height != null && format.height!! >= 2160
                }
                MediaQuality.QHD_2K -> {
                    format.height != null && format.height!! >= 1440 && format.height!! < 2160
                }
                MediaQuality.FHD_1080P -> {
                    format.height != null && format.height!! >= 1080 && format.height!! < 1440
                }
                MediaQuality.HD_720P -> {
                    format.height != null && format.height!! >= 720 && format.height!! < 1080
                }
                MediaQuality.SD_480P -> {
                    format.height != null && format.height!! >= 480 && format.height!! < 720
                }
                MediaQuality.SD_360P -> {
                    format.height != null && format.height!! >= 360 && format.height!! < 480
                }
                MediaQuality.SD_240P -> {
                    format.height != null && format.height!! < 360
                }
            }
        }
    }
    
    /**
     * 获取最佳缩略图
     * 
     * @return 最高分辨率的缩略图
     */
    fun getBestThumbnail(): ThumbnailInfo? {
        return thumbnails.maxByOrNull { it.width * it.height }
    }
    
    /**
     * 获取指定语言的字幕
     * 
     * @param language 语言代码
     * @return 匹配的字幕列表
     */
    fun getSubtitlesByLanguage(language: String): List<SubtitleInfo> {
        return subtitles.filter { it.language.equals(language, ignoreCase = true) }
    }
    
    /**
     * 检查是否有视频格式
     * 
     * @return true表示有视频格式
     */
    fun hasVideoFormats(): Boolean {
        return availableFormats.any { it.formatType == MediaFormat.FormatType.VIDEO }
    }
    
    /**
     * 检查是否有音频格式
     * 
     * @return true表示有音频格式
     */
    fun hasAudioFormats(): Boolean {
        return availableFormats.any { it.formatType == MediaFormat.FormatType.AUDIO }
    }
    
    /**
     * 获取格式化的时长字符串
     * 
     * @return 格式化的时长（如："1:23:45"）
     */
    fun getFormattedDuration(): String? {
        return duration?.let { seconds ->
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            
            when {
                hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
                else -> String.format("%d:%02d", minutes, secs)
            }
        }
    }
    
    /**
     * 获取格式化的文件大小
     * 
     * @param format 指定格式
     * @return 格式化的文件大小
     */
    fun getFormattedFileSize(format: MediaFormat): String? {
        return format.fileSize?.let { size ->
            when {
                size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
                size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                size >= 1024 -> String.format("%.1f KB", size / 1024.0)
                else -> "$size B"
            }
        }
    }
    
    /**
     * 获取格式化的观看次数
     * 
     * @return 格式化的观看次数
     */
    fun getFormattedViewCount(): String? {
        return viewCount?.let { count ->
            when {
                count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
                count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }
    }
    
    /**
     * 检查媒体是否可下载
     * 
     * @return true表示可下载
     */
    fun isDownloadable(): Boolean {
        return availableFormats.isNotEmpty() && !isLive
    }
    
    /**
     * 获取推荐的文件名
     * 
     * @param format 指定格式
     * @param includeQuality 是否包含质量信息
     * @return 推荐的文件名
     */
    fun getRecommendedFileName(format: MediaFormat, includeQuality: Boolean = true): String {
        val sanitizedTitle = title.replace(Regex("[^\\w\\s-_.]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        val qualityInfo = if (includeQuality && format.height != null) {
            "_${format.height}p"
        } else {
            ""
        }
        
        val extension = format.extension ?: "mp4"
        
        return "${sanitizedTitle}${qualityInfo}.${extension}"
    }
    
    /**
     * 转换为简化的信息对象
     * 
     * @return 简化的媒体信息
     */
    fun toSimpleInfo(): SimpleMediaInfo {
        return SimpleMediaInfo(
            id = id,
            title = title,
            duration = duration,
            uploader = uploader,
            viewCount = viewCount,
            mediaType = mediaType,
            originalUrl = originalUrl,
            thumbnailUrl = getBestThumbnail()?.url,
            isDownloadable = isDownloadable()
        )
    }
    
    companion object {
        /**
         * 创建空的媒体信息
         */
        fun createEmpty(url: String): MediaInfo {
            return MediaInfo(
                id = "",
                title = "未知标题",
                description = null,
                duration = null,
                uploader = null,
                uploaderId = null,
                uploadDate = null,
                viewCount = null,
                likeCount = null,
                dislikeCount = null,
                commentCount = null,
                mediaType = MediaType.UNKNOWN,
                originalUrl = url,
                webpageUrl = null,
                availableFormats = emptyList(),
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
}

/**
 * 简化的媒体信息
 * 
 * 用于列表显示等场景的轻量级媒体信息。
 */
@Parcelize
data class SimpleMediaInfo(
    val id: String,
    val title: String,
    val duration: Long?,
    val uploader: String?,
    val viewCount: Long?,
    val mediaType: MediaInfo.MediaType,
    val originalUrl: String,
    val thumbnailUrl: String?,
    val isDownloadable: Boolean
) : Parcelable