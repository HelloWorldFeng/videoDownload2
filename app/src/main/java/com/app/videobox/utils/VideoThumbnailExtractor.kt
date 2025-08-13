package com.app.videobox.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import androidx.core.net.toUri

/**
 * 视频缩略图提取工具类
 * 用于从网络MP4视频链接中提取首帧作为Bitmap
 * 
 * 功能特性：
 * - 支持网络视频链接的首帧提取
 * - 异步处理，避免阻塞主线程
 * - 完善的错误处理和日志记录
 * - 内存优化，支持自定义缩略图尺寸
 * - 生产级代码质量，适用于线上环境
 * 
 * @author VideoBox Team
 * @since 2025-01-11
 */
class VideoThumbnailExtractor {
    
    companion object {
        private const val TAG = "VideoThumbnailExtractor"
        
        // 默认缩略图尺寸配置
        private const val DEFAULT_THUMBNAIL_WIDTH = 320
        private const val DEFAULT_THUMBNAIL_HEIGHT = 240
        
        // 网络连接超时配置（毫秒）
        private const val CONNECTION_TIMEOUT = 10000
        private const val READ_TIMEOUT = 15000
        
        // 支持的视频格式
        private val SUPPORTED_VIDEO_FORMATS = listOf(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "3gp", "ts"
        )
        
        // M3U8相关常量
        private const val M3U8_EXTENSION = ".m3u8"
        private const val TS_EXTENSION = ".ts"
    }
    
    /**
     * 从网络视频链接提取首帧缩略图
     * 支持普通视频文件和M3U8流媒体格式
     * 
     * @param videoUrl 视频网络链接，支持MP4等普通格式和M3U8流媒体格式
     * @param width 缩略图宽度，默认320px
     * @param height 缩略图高度，默认240px
     * @param timeUs 提取帧的时间位置（微秒），默认0（首帧）
     * @return Bitmap? 提取成功返回Bitmap对象，失败返回null
     */
    suspend fun extractThumbnailFromUrl(
        videoUrl: String,
        width: Int = DEFAULT_THUMBNAIL_WIDTH,
        height: Int = DEFAULT_THUMBNAIL_HEIGHT,
        timeUs: Long = 0L
    ): Bitmap? = withContext(Dispatchers.IO) {
        
        Log.d(TAG, "开始提取视频缩略图: $videoUrl")
        Log.d(TAG, "缩略图尺寸: ${width}x${height}, 时间位置: ${timeUs}us")
        
        // 参数验证
        if (!isValidVideoUrl(videoUrl)) {
            Log.e(TAG, "无效的视频URL: $videoUrl")
            return@withContext null
        }
        
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "无效的缩略图尺寸: ${width}x${height}")
            return@withContext null
        }
        
        // 检查是否为M3U8格式
        val actualVideoUrl = if (isM3U8Url(videoUrl)) {
            Log.d(TAG, "检测到M3U8格式，开始解析播放列表...")
            parseM3U8AndGetFirstSegment(videoUrl)
        } else {
            videoUrl
        }
        
        if (actualVideoUrl == null) {
            Log.e(TAG, "无法解析M3U8播放列表或获取视频分片")
            return@withContext null
        }
        
        Log.d(TAG, "实际视频URL: $actualVideoUrl")
        
        var retriever: MediaMetadataRetriever? = null
        
        try {
            // 创建MediaMetadataRetriever实例
            retriever = MediaMetadataRetriever()
            
            // 设置网络视频数据源
            Log.d(TAG, "设置视频数据源...")
            setDataSourceWithHeaders(retriever, actualVideoUrl)
            
            // 获取视频基本信息用于日志记录
            logVideoMetadata(retriever)
            
            // 提取指定时间位置的帧
            Log.d(TAG, "开始提取视频帧...")
            val originalBitmap = retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            if (originalBitmap == null) {
                Log.e(TAG, "无法从视频中提取帧")
                return@withContext null
            }
            
            Log.d(TAG, "原始帧尺寸: ${originalBitmap.width}x${originalBitmap.height}")
            
            // 缩放到指定尺寸
            val scaledBitmap = if (originalBitmap.width != width || originalBitmap.height != height) {
                Log.d(TAG, "缩放图片到目标尺寸: ${width}x${height}")
                val scaled = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
                
                // 释放原始Bitmap内存
                if (!originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }
                
                scaled
            } else {
                originalBitmap
            }
            
            Log.d(TAG, "视频缩略图提取成功")
            return@withContext scaledBitmap
            
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "视频URL格式错误或不支持: ${e.message}", e)
            return@withContext null
            
        } catch (e: IOException) {
            Log.e(TAG, "网络连接或读取视频数据失败: ${e.message}", e)
            return@withContext null
            
        } catch (e: SecurityException) {
            Log.e(TAG, "访问视频资源权限不足: ${e.message}", e)
            return@withContext null
            
        } catch (e: RuntimeException) {
            Log.e(TAG, "MediaMetadataRetriever运行时错误: ${e.message}", e)
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "提取视频缩略图时发生未知错误: ${e.message}", e)
            return@withContext null
            
        } finally {
            // 确保释放MediaMetadataRetriever资源
            try {
                retriever?.release()
                Log.d(TAG, "MediaMetadataRetriever资源已释放")
            } catch (e: Exception) {
                Log.w(TAG, "释放MediaMetadataRetriever资源时出现异常: ${e.message}")
            }
        }
    }
    
    /**
     * 验证视频URL的有效性
     * 
     * @param url 待验证的URL
     * @return Boolean 是否为有效的视频URL
     */
    private fun isValidVideoUrl(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }
        
        // 检查URL协议
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Log.w(TAG, "URL必须使用HTTP或HTTPS协议: $url")
            return false
        }
        
        // 检查文件扩展名（可选，因为有些URL可能没有明确的扩展名）
        val extension = url.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty() && !SUPPORTED_VIDEO_FORMATS.contains(extension) && extension != "m3u8") {
            Log.w(TAG, "可能不支持的视频格式: $extension")
            // 注意：这里只是警告，不直接返回false，因为有些视频URL可能没有扩展名
        }
        
        return true
    }
    
    /**
     * 检查URL是否为M3U8格式
     * 
     * @param url 待检查的URL
     * @return Boolean 是否为M3U8格式
     */
    private fun isM3U8Url(url: String): Boolean {
        return url.lowercase().contains(M3U8_EXTENSION)
    }
    
    /**
     * 根据URL构建合适的请求头
     * 参考VideoResolve.kt中的实现，针对不同域名设置专用请求头
     * 
     * @param videoUrl 视频URL
     * @return Map<String, String> 请求头键值对
     */
    private fun buildHeadersForUrl(videoUrl: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val uri = videoUrl.toUri()
        val host = uri.host ?: ""

        // 基础请求头
        headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        headers["Accept"] = "*/*"
        headers["Accept-Encoding"] = "identity;q=1, *;q=0"
        headers["Accept-Language"] = "zh-CN,zh;q=0.9,en;q=0.8"
        headers["Cache-Control"] = "no-cache"
        headers["Connection"] = "keep-alive"
        headers["DNT"] = "1"
        headers["Pragma"] = "no-cache"
        headers["Priority"] = "i"
        headers["Sec-Fetch-Dest"] = "video"
        headers["Sec-Fetch-Mode"] = "no-cors"
        headers["Sec-Fetch-Site"] = "cross-site"
        headers["Sec-GPC"] = "1"
        headers["Upgrade-Insecure-Requests"] = "1"

        // 根据域名添加特定头
        when {
            host.contains("cdreader.com") -> {
                headers["Referer"] = "https://cdreader.com/"
                headers["Origin"] = "https://cdreader.com"
            }
            host.contains("phncdn.com") -> {
                headers["Referer"] = "https://www.pornhub.com/"
                headers["Origin"] = "https://www.pornhub.com"
            }
            host.contains("amazonaws.com") -> {
                headers["X-Requested-With"] = "XMLHttpRequest"
            }
            else -> {
                // 默认添加通用Referer
                val scheme = uri.scheme ?: "https"
                val referer = "$scheme://$host/"
                headers["Referer"] = referer
            }
        }

        return headers
    }
    
    /**
     * 解析M3U8播放列表并获取第一个视频分片的URL
     * 
     * @param m3u8Url M3U8播放列表的URL
     * @return String? 第一个视频分片的完整URL，失败返回null
     */
    private suspend fun parseM3U8AndGetFirstSegment(m3u8Url: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        
        try {
            Log.d(TAG, "开始解析M3U8播放列表: $m3u8Url")
            
            // 创建HTTP连接
            connection = URL(m3u8Url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                
                // 使用完整的请求头配置
                val headers = buildHeadersForUrl(m3u8Url)
                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }
            
            Log.d(TAG, "M3U8请求头已设置，开始发送请求...")
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "M3U8请求失败，响应码: $responseCode")
                return@withContext null
            }
            
            // 读取M3U8内容
            reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            val m3u8Content = reader.readText()
            
            Log.d(TAG, "M3U8内容长度: ${m3u8Content.length}")
            Log.v(TAG, "M3U8内容预览: ${m3u8Content.take(500)}")
            
            // 解析M3U8内容
            val firstSegmentUrl = parseM3U8Content(m3u8Content, m3u8Url)
            
            if (firstSegmentUrl != null) {
                Log.d(TAG, "成功解析到第一个视频分片: $firstSegmentUrl")
            } else {
                Log.e(TAG, "未能从M3U8中解析到有效的视频分片")
            }
            
            return@withContext firstSegmentUrl
            
        } catch (e: IOException) {
            Log.e(TAG, "读取M3U8播放列表失败: ${e.message}", e)
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "解析M3U8播放列表时发生未知错误: ${e.message}", e)
            return@withContext null
            
        } finally {
            // 释放资源
            try {
                reader?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "释放M3U8解析资源时出现异常: ${e.message}")
            }
        }
    }
    
    /**
     * 解析M3U8文件内容，提取第一个视频分片URL
     * 
     * @param m3u8Content M3U8文件内容
     * @param baseUrl M3U8文件的基础URL，用于构建相对路径的完整URL
     * @return String? 第一个视频分片的完整URL
     */
    private suspend fun parseM3U8Content(m3u8Content: String, baseUrl: String): String? {
        try {
            val lines = m3u8Content.split("\n").map { it.trim() }
            
            // 检查是否为有效的M3U8文件
            if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) {
                Log.e(TAG, "无效的M3U8文件格式")
                return null
            }
            
            Log.d(TAG, "M3U8文件行数: ${lines.size}")
            
            // 查找第一个视频分片
            for (i in lines.indices) {
                val line = lines[i]
                
                // 跳过注释行和空行
                if (line.startsWith("#") || line.isBlank()) {
                    continue
                }
                
                // 检查是否为嵌套的M3U8文件（主播放列表）
                if (line.contains(M3U8_EXTENSION)) {
                    Log.d(TAG, "检测到嵌套M3U8文件: $line")
                    val nestedM3u8Url = resolveUrl(line, baseUrl)
                    if (nestedM3u8Url != null) {
                        // 递归解析嵌套的M3U8文件
                        Log.d(TAG, "递归解析嵌套M3U8: $nestedM3u8Url")
                        return parseM3U8AndGetFirstSegment(nestedM3u8Url)
                    }
                    continue
                }
                
                // 检查是否为视频分片文件
                if (line.contains(TS_EXTENSION) || line.contains(".mp4") || 
                    line.contains(".m4s") || !line.startsWith("#")) {
                    
                    val segmentUrl = resolveUrl(line, baseUrl)
                    if (segmentUrl != null) {
                        Log.d(TAG, "找到第一个视频分片: $segmentUrl")
                        return segmentUrl
                    }
                }
            }
            
            Log.e(TAG, "M3U8文件中未找到有效的视频分片")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "解析M3U8内容时发生错误: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 解析相对URL为绝对URL
     * 
     * @param segmentUrl 分片URL（可能是相对路径）
     * @param baseUrl 基础URL
     * @return String? 完整的绝对URL
     */
    private fun resolveUrl(segmentUrl: String, baseUrl: String): String? {
        try {
            // 如果已经是完整URL，直接返回
            if (segmentUrl.startsWith("http://") || segmentUrl.startsWith("https://")) {
                return segmentUrl
            }
            
            // 构建基础URI
            val baseUri = URI(baseUrl)
            
            // 解析相对URL
            val resolvedUri = if (segmentUrl.startsWith("/")) {
                // 绝对路径
                URI(baseUri.scheme, baseUri.authority, segmentUrl, null, null)
            } else {
                // 相对路径
                baseUri.resolve(segmentUrl)
            }
            
            val resolvedUrl = resolvedUri.toString()
            Log.v(TAG, "URL解析: $segmentUrl -> $resolvedUrl")
            
            return resolvedUrl
            
        } catch (e: Exception) {
            Log.e(TAG, "URL解析失败: $segmentUrl, 基础URL: $baseUrl, 错误: ${e.message}")
            return null
        }
    }
    
    /**
     * 为MediaMetadataRetriever设置数据源，包含请求头优化
     * 
     * @param retriever MediaMetadataRetriever实例
     * @param url 视频URL
     */
    private fun setDataSourceWithHeaders(retriever: MediaMetadataRetriever, url: String) {
        // 使用完整的请求头配置，提高兼容性
        val headers = buildHeadersForUrl(url)
        
        // MediaMetadataRetriever需要HashMap格式
        val retrieverHeaders = hashMapOf<String, String>()
        headers.forEach { (key, value) ->
            retrieverHeaders[key] = value
        }
        
        // 为视频处理优化Accept头
        retrieverHeaders["Accept"] = "video/*, application/vnd.apple.mpegurl, application/x-mpegURL"
        
        Log.d(TAG, "设置MediaMetadataRetriever请求头，数量: ${retrieverHeaders.size}")
        Log.v(TAG, "请求头详情: $retrieverHeaders")
        
        retriever.setDataSource(url, retrieverHeaders)
    }
    
    /**
     * 记录视频元数据信息用于调试
     * 
     * @param retriever MediaMetadataRetriever实例
     */
    private fun logVideoMetadata(retriever: MediaMetadataRetriever) {
        try {
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            
            Log.d(TAG, "视频元数据信息:")
            Log.d(TAG, "  时长: ${duration}ms")
            Log.d(TAG, "  分辨率: ${width}x${height}")
            Log.d(TAG, "  MIME类型: $mimeType")
            
        } catch (e: Exception) {
            Log.w(TAG, "获取视频元数据失败: ${e.message}")
        }
    }
    
    /**
     * 检查网络连接状态（可选功能）
     * 
     * @param url 视频URL
     * @return Boolean 网络连接是否可用
     */
    suspend fun checkNetworkConnectivity(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "检查网络连接状态: $url")
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "HEAD"
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "VideoBox/1.0 (Android)")
            }
            
            val responseCode = connection.responseCode
            val isConnected = responseCode in 200..299
            
            Log.d(TAG, "网络连接检查结果: $responseCode, 可用: $isConnected")
            
            connection.disconnect()
            return@withContext isConnected
            
        } catch (e: Exception) {
            Log.e(TAG, "网络连接检查失败: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * 批量提取多个时间点的缩略图
     * 
     * @param videoUrl 视频URL
     * @param timePositions 时间位置列表（微秒）
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @return List<Bitmap?> 缩略图列表，失败的位置为null
     */
    suspend fun extractMultipleThumbnails(
        videoUrl: String,
        timePositions: List<Long>,
        width: Int = DEFAULT_THUMBNAIL_WIDTH,
        height: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): List<Bitmap?> = withContext(Dispatchers.IO) {
        
        Log.d(TAG, "批量提取缩略图，位置数量: ${timePositions.size}")
        
        val results = mutableListOf<Bitmap?>()
        
        for ((index, timeUs) in timePositions.withIndex()) {
            Log.d(TAG, "提取第${index + 1}/${timePositions.size}个缩略图，时间: ${timeUs}us")
            
            val thumbnail = extractThumbnailFromUrl(videoUrl, width, height, timeUs)
            results.add(thumbnail)
            
            // 添加短暂延迟，避免过于频繁的网络请求
            if (index < timePositions.size - 1) {
                kotlinx.coroutines.delay(100)
            }
        }
        
        Log.d(TAG, "批量提取完成，成功: ${results.count { it != null }}/${results.size}")
        return@withContext results
    }
}