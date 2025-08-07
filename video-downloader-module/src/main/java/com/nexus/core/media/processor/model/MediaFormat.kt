package com.nexus.core.media.processor.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 媒体格式数据类
 * 
 * 描述媒体的具体格式信息，包括视频、音频参数，
 * 文件大小、编码格式等详细信息。
 * 
 * 主要信息：
 * - 格式基本信息（ID、类型、扩展名）
 * - 视频参数（分辨率、帧率、编码）
 * - 音频参数（比特率、采样率、编码）
 * - 文件信息（大小、URL）
 * 
 * 使用示例：
 * ```kotlin
 * val format = MediaFormat(
 *     formatId = "22",
 *     formatType = MediaFormat.FormatType.VIDEO,
 *     extension = "mp4",
 *     width = 1280,
 *     height = 720,
 *     videoBitrate = 1000,
 *     audioCodec = "aac"
 * )
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
@Parcelize
@Serializable
data class MediaFormat(
    /** 格式唯一标识符 */
    val formatId: String,
    
    /** 格式类型 */
    val formatType: FormatType,
    
    /** 文件扩展名 */
    val extension: String?,
    
    /** 格式描述 */
    val formatNote: String?,
    
    /** 直接下载URL */
    val url: String?,
    
    /** 文件大小(字节) */
    val fileSize: Long?,
    
    /** 视频宽度(像素) */
    val width: Int?,
    
    /** 视频高度(像素) */
    val height: Int?,
    
    /** 视频帧率(fps) */
    val fps: Float?,
    
    /** 视频比特率(kbps) */
    val videoBitrate: Int?,
    
    /** 视频编码格式 */
    val videoCodec: String?,
    
    /** 音频比特率(kbps) */
    val audioBitrate: Int?,
    
    /** 音频采样率(Hz) */
    val audioSampleRate: Int?,
    
    /** 音频编码格式 */
    val audioCodec: String?,
    
    /** 音频声道数 */
    val audioChannels: Int?,
    
    /** 容器格式 */
    val container: String?,
    
    /** 协议类型 */
    val protocol: String?,
    
    /** 语言代码 */
    val language: String?,
    
    /** 格式偏好值(越高越好) */
    val preference: Int?,
    
    /** 质量评分 */
    val quality: Float?,
    
    /** 是否包含视频 */
    val hasVideo: Boolean,
    
    /** 是否包含音频 */
    val hasAudio: Boolean,
    
    /** HTTP请求头 */
    val httpHeaders: @RawValue Map<String, String>?,
    
    /** 分片信息 */
    @Transient
    val fragments: List<FragmentInfo>? = null,
    
    /** 额外信息 */
    @Transient
    val extraInfo: @RawValue Map<String, Any>? = null
) : Parcelable {
    
    /**
     * 格式类型枚举
     */
    enum class FormatType {
        VIDEO,          // 视频格式（包含视频和音频）
        AUDIO,          // 纯音频格式
        VIDEO_ONLY,     // 纯视频格式（无音频）
        SUBTITLE,       // 字幕格式
        THUMBNAIL,      // 缩略图格式
        UNKNOWN         // 未知格式
    }
    
    /**
     * 获取格式化的分辨率字符串
     * 
     * @return 分辨率字符串（如："1920x1080"）
     */
    fun getResolutionString(): String? {
        return if (width != null && height != null) {
            "${width}x${height}"
        } else {
            null
        }
    }
    
    /**
     * 获取质量标签
     * 
     * @return 质量标签（如："720p"、"1080p"）
     */
    fun getQualityLabel(): String? {
        return height?.let { h ->
            when {
                h >= 2160 -> "4K"
                h >= 1440 -> "1440p"
                h >= 1080 -> "1080p"
                h >= 720 -> "720p"
                h >= 480 -> "480p"
                h >= 360 -> "360p"
                h >= 240 -> "240p"
                else -> "${h}p"
            }
        }
    }
    
    /**
     * 获取格式化的文件大小
     * 
     * @return 格式化的文件大小字符串
     */
    fun getFormattedFileSize(): String? {
        return fileSize?.let { size ->
            when {
                size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
                size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
                size >= 1024 -> String.format("%.1f KB", size / 1024.0)
                else -> "$size B"
            }
        }
    }
    
    /**
     * 获取格式化的比特率
     * 
     * @return 格式化的比特率字符串
     */
    fun getFormattedBitrate(): String? {
        val totalBitrate = (videoBitrate ?: 0) + (audioBitrate ?: 0)
        return if (totalBitrate > 0) {
            when {
                totalBitrate >= 1000 -> String.format("%.1f Mbps", totalBitrate / 1000.0)
                else -> "$totalBitrate kbps"
            }
        } else {
            null
        }
    }
    
    /**
     * 获取音频信息描述
     * 
     * @return 音频信息字符串
     */
    fun getAudioDescription(): String? {
        val parts = mutableListOf<String>()
        
        audioCodec?.let { parts.add(it.uppercase()) }
        audioBitrate?.let { parts.add("${it}kbps") }
        audioSampleRate?.let { parts.add("${it}Hz") }
        audioChannels?.let { 
            val channelDesc = when (it) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1"
                8 -> "7.1"
                else -> "${it}ch"
            }
            parts.add(channelDesc)
        }
        
        return if (parts.isNotEmpty()) parts.joinToString(" ") else null
    }
    
    /**
     * 获取视频信息描述
     * 
     * @return 视频信息字符串
     */
    fun getVideoDescription(): String? {
        val parts = mutableListOf<String>()
        
        getQualityLabel()?.let { parts.add(it) }
        videoCodec?.let { parts.add(it.uppercase()) }
        fps?.let { 
            if (it > 30) {
                parts.add("${it.toInt()}fps")
            }
        }
        videoBitrate?.let { parts.add("${it}kbps") }
        
        return if (parts.isNotEmpty()) parts.joinToString(" ") else null
    }
    
    /**
     * 获取完整的格式描述
     * 
     * @return 完整的格式描述字符串
     */
    fun getFullDescription(): String {
        val parts = mutableListOf<String>()
        
        // 添加格式类型
        when (formatType) {
            FormatType.VIDEO -> {
                getVideoDescription()?.let { parts.add(it) }
                if (hasAudio) {
                    getAudioDescription()?.let { parts.add("+ $it") }
                }
            }
            FormatType.AUDIO -> {
                getAudioDescription()?.let { parts.add(it) }
            }
            FormatType.VIDEO_ONLY -> {
                getVideoDescription()?.let { parts.add("$it (Video Only)") }
            }
            else -> {
                formatNote?.let { parts.add(it) }
            }
        }
        
        // 添加文件大小
        getFormattedFileSize()?.let { parts.add("($it)") }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString(" ")
        } else {
            formatId
        }
    }
    
    /**
     * 检查是否为高质量格式
     * 
     * @return true表示高质量
     */
    fun isHighQuality(): Boolean {
        return when (formatType) {
            FormatType.VIDEO -> height != null && height!! >= 720
            FormatType.AUDIO -> audioBitrate != null && audioBitrate!! >= 128
            else -> false
        }
    }
    
    /**
     * 检查是否支持硬件解码
     * 
     * @return true表示支持硬件解码
     */
    fun supportsHardwareDecoding(): Boolean {
        return videoCodec?.lowercase() in listOf("h264", "h.264", "avc1", "hevc", "h265", "h.265")
    }
    
    /**
     * 获取估算的下载时间
     * 
     * @param speedKbps 下载速度(kbps)
     * @return 估算的下载时间(秒)
     */
    fun getEstimatedDownloadTime(speedKbps: Int): Long? {
        return fileSize?.let { size ->
            val sizeKb = size / 1024
            sizeKb / speedKbps
        }
    }
    
    /**
     * 检查格式兼容性
     * 
     * @param targetFormat 目标格式
     * @return 兼容性评分(0-100)
     */
    fun getCompatibilityScore(targetFormat: MediaFormat): Int {
        var score = 0
        
        // 格式类型匹配
        if (formatType == targetFormat.formatType) score += 30
        
        // 编码格式匹配
        if (videoCodec == targetFormat.videoCodec) score += 20
        if (audioCodec == targetFormat.audioCodec) score += 20
        
        // 分辨率匹配
        if (width == targetFormat.width && height == targetFormat.height) score += 15
        
        // 容器格式匹配
        if (container == targetFormat.container) score += 10
        
        // 扩展名匹配
        if (extension == targetFormat.extension) score += 5
        
        return score
    }
    
    /**
     * 转换为简化格式信息
     * 
     * @return 简化的格式信息
     */
    fun toSimpleFormat(): SimpleMediaFormat {
        return SimpleMediaFormat(
            formatId = formatId,
            formatType = formatType,
            extension = extension,
            qualityLabel = getQualityLabel(),
            fileSize = fileSize,
            description = getFullDescription()
        )
    }
    
    companion object {
        /**
         * 创建音频格式
         */
        fun createAudioFormat(
            formatId: String,
            extension: String?,
            url: String?,
            audioBitrate: Int?,
            audioCodec: String?,
            fileSize: Long? = null
        ): MediaFormat {
            return MediaFormat(
                formatId = formatId,
                formatType = FormatType.AUDIO,
                extension = extension,
                formatNote = null,
                url = url,
                fileSize = fileSize,
                width = null,
                height = null,
                fps = null,
                videoBitrate = null,
                videoCodec = null,
                audioBitrate = audioBitrate,
                audioSampleRate = null,
                audioCodec = audioCodec,
                audioChannels = null,
                container = null,
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
        }
        
        /**
         * 创建视频格式
         */
        fun createVideoFormat(
            formatId: String,
            extension: String?,
            url: String?,
            width: Int?,
            height: Int?,
            videoBitrate: Int?,
            videoCodec: String?,
            audioBitrate: Int? = null,
            audioCodec: String? = null,
            fileSize: Long? = null
        ): MediaFormat {
            return MediaFormat(
                formatId = formatId,
                formatType = FormatType.VIDEO,
                extension = extension,
                formatNote = null,
                url = url,
                fileSize = fileSize,
                width = width,
                height = height,
                fps = null,
                videoBitrate = videoBitrate,
                videoCodec = videoCodec,
                audioBitrate = audioBitrate,
                audioSampleRate = null,
                audioCodec = audioCodec,
                audioChannels = null,
                container = null,
                protocol = null,
                language = null,
                preference = null,
                quality = null,
                hasVideo = true,
                hasAudio = audioBitrate != null,
                httpHeaders = null,
                fragments = null,
                extraInfo = null
            )
        }
    }
}

/**
 * 片段信息
 * 
 * 用于描述分片下载的片段信息。
 */
@Parcelize
data class FragmentInfo(
    val url: String,
    val duration: Float?,
    val fileSize: Long?
) : Parcelable

/**
 * 简化的媒体格式
 * 
 * 用于列表显示等场景的轻量级格式信息。
 */
@Parcelize
data class SimpleMediaFormat(
    val formatId: String,
    val formatType: MediaFormat.FormatType,
    val extension: String?,
    val qualityLabel: String?,
    val fileSize: Long?,
    val description: String
) : Parcelable