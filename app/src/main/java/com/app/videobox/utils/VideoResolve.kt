package com.app.videobox.utils

import android.util.Log
import androidx.core.net.toUri
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFprobe
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoResolve {
    
    /**
     * 视频信息数据类
     */
    data class VideoInfo(
        val duration: Float,           // 视频时长（秒）
        val width: Int,               // 视频宽度
        val height: Int,              // 视频高度
        val resolution: String,       // 分辨率（如：720p, 1080p）
        val bitrate: Long,            // 码率（bps）
        val size: Long,               // 文件大小（字节），m3u8可能为0
        val format: String,           // 格式
        val codec: String,            // 编码格式
        val frameRate: String,         // 帧率
        var title: String,               //视频标题
        val thumbnail: String,           //视频封面
        val originUrl: String,           //视频资源链接
        val ext: String                 //视频类型
    )
    
    /**
     * 解析视频信息
     * 对应命令：ffprobe -v error -headers "User-Agent: Mozilla/5.0" -show_format -show_streams "video_url"
     * 
     * @param videoUrl 视频URL（支持m3u8、mp4等）
     * @param title 视频标题文案
     * @return VideoInfo 视频信息
     */
    suspend fun getVideoInfo(videoUrl: String, title: String? = null, imgUrl: String, ext: String): Result<VideoInfo> =
        withContext(Dispatchers.IO) {
            try {
                val command = mutableListOf<String>()
                
                // 基础参数
                command.add("-v")
                command.add("error")

                // 根据URL构建合适的请求头
                val headers = buildHeadersForUrl(videoUrl)
                command.add("-headers")
                command.add(headers)
                
                // 输出格式为JSON
                command.add("-print_format")
                command.add("json")
                
                // 显示格式和流信息
                command.add("-show_format")
                command.add("-show_streams")
                
                // 视频URL
                command.add(videoUrl)
                
                val rc = FFprobe.execute(command.toTypedArray())

                if (rc == 0) {
                    val output = Config.getLastCommandOutput()
                    if (output.isNotEmpty()) {
                        val videoInfo = parseVideoInfoFromJson(output,videoUrl,title,imgUrl,ext)
                        Result.success(videoInfo)
                    } else {
                        Result.failure(Exception("FFprobe输出为空"))
                    }
                } else {
                    val errorOutput = Config.getLastCommandOutput()
                    Result.failure(Exception("FFprobe执行失败: $errorOutput"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 根据URL构建合适的请求头
     */
    private fun buildHeadersForUrl(videoUrl: String): String {
        val uri = videoUrl.toUri()
        val host = uri.host ?: ""
        
        return buildString {
            // 基础User-Agent
            append("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\r\n")
            append("Accept: */*\r\n")
            append("Accept-Encoding: identity;q=1, *;q=0\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: keep-alive\r\n")
            append("DNT: 1\r\n")
            append("Pragma: no-cache\r\n")
            append("Priority: i\r\n")
            append("Range: bytes=0-\r\n")
            append("Sec-Fetch-Dest: video\r\n")
            append("Sec-Fetch-Mode: no-cors\r\n")
            append("Sec-Fetch-Site: cross-site\r\n")
            append("Sec-GPC: 1\r\n")
            append("Upgrade-Insecure-Requests: 1\r\n")
            
            // 根据域名添加特定头
            when {
                host.contains("cdreader.com") -> {
                    // 为cdreader.com添加Referer
                    append("Referer: https://cdreader.com/\r\n")
                    append("Origin: https://cdreader.com\r\n")
                }
                host.contains("phncdn.com") -> {
                    // 为pornhub CDN添加特定头
                    append("Referer: https://www.pornhub.com/\r\n")
                    append("Origin: https://www.pornhub.com\r\n")
                }
                host.contains("amazonaws.com") -> {
                    // AWS CDN通常不需要特殊头
                    append("X-Requested-With: XMLHttpRequest\r\n")
                }
                else -> {
                    // 默认添加通用Referer
                    val scheme = uri.scheme ?: "https"
                    val referer = "$scheme://$host/"
                    append("Referer: $referer\r\n")
                }
            }
        }
    }

    /**
     * 清理和验证FFprobe的JSON输出
     */
    private fun cleanJsonOutput(jsonOutput: String): String {
        if (jsonOutput.isBlank()) {
            throw IllegalArgumentException("FFprobe输出为空")
        }
        
        // 查找JSON开始位置（第一个{）
        val jsonStart = jsonOutput.indexOf('{')
        if (jsonStart == -1) {
            throw IllegalArgumentException("未找到有效的JSON内容")
        }
        
        // 查找JSON结束位置（最后一个}）
        val jsonEnd = jsonOutput.lastIndexOf('}')
        if (jsonEnd == -1 || jsonEnd <= jsonStart) {
            throw IllegalArgumentException("JSON格式不完整")
        }
        
        // 提取JSON部分
        val jsonContent = jsonOutput.substring(jsonStart, jsonEnd + 1)
        
        // 检查是否包含必要的字段
        if (!jsonContent.contains("\"format\"") && !jsonContent.contains("\"streams\"")) {
            Log.w("VideoResolve", "JSON内容可能不完整: $jsonContent")
        }
        
        return jsonContent
    }

    /**
     * 从JSON输出解析视频信息
     */
    private fun parseVideoInfoFromJson(
        jsonOutput: String,
        videoUrl: String,
        title: String?,
        imgUrl: String,
        ext: String
    ): VideoInfo {
        // 清理和验证JSON输出
        val cleanedJson = cleanJsonOutput(jsonOutput)
        
        val jsonElement = try {
            JsonParser.parseString(cleanedJson)
        } catch (e: Exception) {
            Log.e("VideoResolve", "JSON解析失败: ${e.message}")
            Log.e("VideoResolve", "原始输出: $jsonOutput")
            Log.e("VideoResolve", "清理后输出: $cleanedJson")
            throw e
        }
        
        val jsonObject = jsonElement.asJsonObject
        
        // 获取格式信息
        val format = jsonObject.getAsJsonObject("format")
        val duration = try {
            format?.get("duration")?.asDouble?.toFloat() ?: 0f
        } catch (e: Exception) {
            Log.w("VideoResolve", "解析duration失败: ${e.message}")
            0f
        }
        val bitrate = try {
            format?.get("bit_rate")?.asLong ?: 0L
        } catch (e: Exception) {
            Log.w("VideoResolve", "解析bitrate失败: ${e.message}")
            0L
        }
        val size = try {
            format?.get("size")?.asLong ?: 0L
        } catch (e: Exception) {
            Log.w("VideoResolve", "解析size失败: ${e.message}")
            0L
        }
        val formatName = try {
            format?.get("format_name")?.asString ?: "unknown"
        } catch (e: Exception) {
            Log.w("VideoResolve", "解析format_name失败: ${e.message}")
            "unknown"
        }
        
        // 获取视频流信息
        val streams = try {
            jsonObject.getAsJsonArray("streams")
        } catch (e: Exception) {
            Log.w("VideoResolve", "获取streams失败: ${e.message}")
            null
        }
        
        var width = 0
        var height = 0
        var codec = "unknown"
        var frameRate = "unknown"
        
        streams?.forEach { element ->
            try {
                val stream = element.asJsonObject
                if (stream.get("codec_type")?.asString == "video") {
                    width = try {
                        stream.get("width")?.asInt ?: 0
                    } catch (e: Exception) {
                        Log.w("VideoResolve", "解析width失败: ${e.message}")
                        0
                    }
                    height = try {
                        stream.get("height")?.asInt ?: 0
                    } catch (e: Exception) {
                        Log.w("VideoResolve", "解析height失败: ${e.message}")
                        0
                    }
                    codec = try {
                        stream.get("codec_name")?.asString ?: "unknown"
                    } catch (e: Exception) {
                        Log.w("VideoResolve", "解析codec失败: ${e.message}")
                        "unknown"
                    }
                    
                    // 解析帧率
                    val rFrameRate = try {
                        stream.get("r_frame_rate")?.asString ?: ""
                    } catch (e: Exception) {
                        Log.w("VideoResolve", "解析帧率失败: ${e.message}")
                        ""
                    }
                    if (rFrameRate.isNotEmpty() && rFrameRate != "0/0") {
                        frameRate = rFrameRate
                    }
                    return@forEach
                }
            } catch (e: Exception) {
                Log.w("VideoResolve", "解析stream元素失败: ${e.message}")
            }
        }
        
        // 计算分辨率描述
        val resolution = when {
            height >= 2160 -> "1080p"
            height >= 1440 -> "1080p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height >= 360 -> "360p"
            height >= 240 -> "240p"
            else -> "${width}x${height}"
        }

        // 智能估算文件大小
        val estimateSize: Long = if (bitrate < 100_000L) {
            // 经验码率表（Kbps）
            val bitrateKbps = when {
                resolution.contains("360", true) -> 600f
                resolution.contains("480", true) -> 1200f
                resolution.contains("720", true) -> 2000f
                resolution.contains("1080", true) -> 4000f
                resolution.contains("2k", true) || resolution.contains("1440", true) -> 8000f
                resolution.contains("4k", true) || resolution.contains("2160", true) -> 16000f
                else -> 1000f
            }
            // Kbps × 秒 ÷ 8 × 1024 = 字节
            (bitrateKbps * duration / 8f * 1024f).toLong()
        } else {
            // 用码率（bps）
            ((bitrate * duration) / 8f).toLong()
        }

        // 生成随机标题
        val finalTitle = if (title.isNullOrEmpty()) {
            generateRandomTitle()
        } else {
            title
        }

        return VideoInfo(
            duration = duration,
            width = width,
            height = height,
            resolution = resolution,
            bitrate = bitrate,
            size = estimateSize,
            format = formatName,
            codec = codec,
            frameRate = frameRate,
            title = finalTitle,
            thumbnail = imgUrl,
            originUrl = videoUrl,
            ext = ext,
        )
    }
    
    /**
     * 格式化时长显示
     */
    fun formatDuration(seconds: Float): String {
        val hours = (seconds / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        val secs = (seconds % 60).toInt()
        
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%02d:%02d", minutes, secs)
        }
    }
    
    /**
     * 格式化文件大小显示
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * 格式化码率显示
     */
    fun formatBitrate(bps: Long): String {
        return when {
            bps >= 1024 * 1024 -> String.format("%.1f Mbps", bps / (1024.0 * 1024.0))
            bps >= 1024 -> String.format("%.1f Kbps", bps / 1024.0)
            else -> "$bps bps"
        }
    }
    
    /**
     * 估算视频文件大小（字节）
     * @param bitrateBps 码率（bps）
     * @param durationSec 时长（秒）
     * @return 文件大小（字节）
     */
    private fun estimateVideoFileSize(bitrateBps: Long, durationSec: Float): Long {
        // bps * 秒 = 总比特数，/8 = 字节
        return ((bitrateBps * durationSec) / 8f).toLong()
    }

    /**
     * 生成随机标题
     * @return 随机生成的标题
     */
    private fun generateRandomTitle(): String {
        val random = java.util.Random()
        val number = random.nextInt(99999) + 1
        
        return "VideoPlayHD_$number"
    }
}