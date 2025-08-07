package com.nexus.core.media.processor.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import android.text.format.Formatter
import com.nexus.core.media.processor.model.*
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.pow

/**
 * 媒体处理器工具类
 * 提供各种实用工具方法
 */
object MediaProcessorUtils {
    
    private const val TAG = "MediaProcessorUtils"
    
    // 时间格式化器
    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val durationFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    // URL正则表达式
    private val urlPattern = Pattern.compile(
        "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    )
    
    // 视频文件扩展名
    private val videoExtensions = setOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "m3u8"
    )
    
    // 音频文件扩展名
    private val audioExtensions = setOf(
        "mp3", "aac", "wav", "flac", "ogg", "m4a", "wma", "opus", "aiff"
    )
    
    /**
     * 验证URL是否有效
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            urlPattern.matcher(url).matches() && URL(url) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 从URL提取域名
     */
    fun extractDomain(url: String): String? {
        return try {
            URL(url).host
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 检测媒体类型
     */
    fun detectMediaType(url: String, mimeType: String? = null): MediaInfo.MediaType {
        // 优先使用MIME类型
        mimeType?.let { mime ->
            when {
                mime.startsWith("video/") -> return MediaInfo.MediaType.VIDEO
                mime.startsWith("audio/") -> return MediaInfo.MediaType.AUDIO
                else -> { /* 继续使用URL扩展名判断 */ }
            }
        }
        
        // 根据URL扩展名判断
        val extension = getFileExtension(url)?.lowercase()
        return when (extension) {
            in videoExtensions -> MediaInfo.MediaType.VIDEO
            in audioExtensions -> MediaInfo.MediaType.AUDIO
            else -> MediaInfo.MediaType.UNKNOWN
        }
    }
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(fileName: String): String? {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1)
        } else {
            null
        }
    }
    
    /**
     * 移除文件扩展名
     */
    fun removeFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(0, lastDotIndex)
        } else {
            fileName
        }
    }
    
    /**
     * 清理文件名（移除非法字符）
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace("[\\/:*?\"<>|]".toRegex(), "_")
            .replace("\\s+".toRegex(), " ")
            .trim()
            .take(255) // 限制文件名长度
    }
    
    /**
     * 生成唯一文件名
     */
    fun generateUniqueFileName(baseName: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "${sanitizeFileName(baseName)}_${timestamp}_${random}.$extension"
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        
        return String.format(
            "%.1f %s",
            bytes / 1024.0.pow(digitGroups.toDouble()),
            units[digitGroups]
        )
    }
    
    /**
     * 格式化比特率
     */
    fun formatBitrate(bitrate: Int): String {
        return when {
            bitrate >= 1000000 -> String.format("%.1f Mbps", bitrate / 1000000.0)
            bitrate >= 1000 -> String.format("%.1f Kbps", bitrate / 1000.0)
            else -> "$bitrate bps"
        }
    }
    
    /**
     * 格式化时长
     */
    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00:00"
        
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    /**
     * 格式化速度
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Date(timestamp))
    }
    
    /**
     * 格式化剩余时间
     */
    fun formatRemainingTime(seconds: Long): String {
        if (seconds <= 0) return "未知"
        
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
            seconds < 86400 -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "${hours}小时${minutes}分钟"
            }
            else -> {
                val days = seconds / 86400
                val hours = (seconds % 86400) / 3600
                "${days}天${hours}小时"
            }
        }
    }
    
    /**
     * 计算下载进度百分比
     */
    fun calculateProgress(downloaded: Long, total: Long): Int {
        return if (total <= 0) 0 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)
    }
    
    /**
     * 计算预估剩余时间
     */
    fun calculateRemainingTime(downloaded: Long, total: Long, speed: Long): Long {
        if (speed <= 0 || total <= downloaded) return 0
        return (total - downloaded) / speed
    }
    
    /**
     * 生成MD5哈希
     */
    fun generateMD5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 生成SHA256哈希
     */
    fun generateSHA256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 检查网络连接
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * 获取网络类型
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.UNKNOWN
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> if (networkInfo?.isConnected == true) NetworkType.UNKNOWN else NetworkType.UNKNOWN
            }
        }
    }
    
    /**
     * 获取可用存储空间
     */
    fun getAvailableStorageSpace(path: String): Long {
        return try {
            val stat = StatFs(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBytes
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取总存储空间
     */
    fun getTotalStorageSpace(path: String): Long {
        return try {
            val stat = StatFs(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.totalBytes
            } else {
                @Suppress("DEPRECATION")
                stat.blockCount.toLong() * stat.blockSize.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 检查存储空间是否足够
     */
    fun hasEnoughStorage(path: String, requiredBytes: Long): Boolean {
        val availableBytes = getAvailableStorageSpace(path)
        return availableBytes >= requiredBytes
    }
    
    /**
     * 创建目录
     */
    fun createDirectory(path: String): Boolean {
        return try {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除文件或目录
     */
    fun deleteFileOrDirectory(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取文件大小
     */
    fun getFileSize(path: String): Long {
        return try {
            File(path).length()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 检查文件是否存在
     */
    fun fileExists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 移动文件
     */
    fun moveFile(sourcePath: String, targetPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val targetFile = File(targetPath)
            
            // 确保目标目录存在
            targetFile.parentFile?.mkdirs()
            
            sourceFile.renameTo(targetFile)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 复制文件
     */
    fun copyFile(sourcePath: String, targetPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val targetFile = File(targetPath)
            
            // 确保目标目录存在
            targetFile.parentFile?.mkdirs()
            
            sourceFile.copyTo(targetFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取临时文件路径
     */
    fun getTempFilePath(context: Context, fileName: String): String {
        return File(context.cacheDir, "temp_$fileName").absolutePath
    }
    
    /**
     * 清理临时文件
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
    }
    
    /**
     * 验证文件完整性（通过大小）
     */
    fun validateFileIntegrity(filePath: String, expectedSize: Long): Boolean {
        val actualSize = getFileSize(filePath)
        return actualSize == expectedSize && actualSize > 0
    }
    
    /**
     * 获取系统信息
     */
    fun getSystemInfo(): Map<String, String> {
        return mapOf(
            "Android版本" to Build.VERSION.RELEASE,
            "API级别" to Build.VERSION.SDK_INT.toString(),
            "设备型号" to Build.MODEL,
            "设备制造商" to Build.MANUFACTURER,
            "CPU架构" to Build.SUPPORTED_ABIS.joinToString(", "),
            "可用内存" to formatFileSize(Runtime.getRuntime().freeMemory()),
            "总内存" to formatFileSize(Runtime.getRuntime().totalMemory()),
            "最大内存" to formatFileSize(Runtime.getRuntime().maxMemory())
        )
    }
}