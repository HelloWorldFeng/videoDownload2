package com.videodownloader.module.api

import VideoInfo
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.io.File
import java.net.URI

object DownloadUtil {

    private const val TAG = "DownloadUtil"
    private const val NETWORK_TIMEOUT_MS = 30000L
    private const val BUFFER_SIZE = 8192

    /**
     * 下载视频文件到外部存储公共下载目录
     * @param context 应用上下文，用于获取存储目录
     * @param videoInfo 视频信息对象，包含下载URL和标题等
     * @param taskId 下载任务唯一标识符
     * @param progressCallback 下载进度回调函数，参数为(进度百分比, 下载速度, 状态信息)
     * @return Result<String> 成功时返回文件绝对路径，失败时返回异常信息
     */
    suspend fun downloadMP4Video(
        context: Context,
        videoInfo: VideoInfo? = null,
        taskId: String,
        progressCallback: ((Float, Long, String) -> Unit)?,
    ): Result<String> {
        if (videoInfo == null) {
            Log.e(TAG, "VideoInfo为空，无法开始下载")
            return Result.failure(Throwable("fetch_info_error_msg"))
        }
        
        return try {
            downloadVideoInternal(context, videoInfo, taskId, progressCallback)
        } catch (e: Exception) {
            Log.e(TAG, "下载视频失败: taskId=$taskId, error=${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 内部下载实现方法
     * 使用外部存储公共下载目录，用户可在文件管理器中直接访问
     */
    private suspend fun downloadVideoInternal(
        context: Context,
        videoInfo: VideoInfo,
        taskId: String,
        progressCallback: ((Float, Long, String) -> Unit)?
    ): Result<String> {
        // 使用外部存储的公共下载目录，在其下创建应用专属目录
        val baseDownloadDir = context.getExternalFilesDir("Downloads")
        val appName = context.getString(context.applicationInfo.labelRes).ifEmpty { "VideoDownloader" }
        val publicDownloadDir = File(baseDownloadDir, appName)
        val url = videoInfo.url // 下载链接
        
        Log.d(TAG, "开始下载视频: taskId=$taskId, url=$url, title=${videoInfo.title}")
        Log.d(TAG, "使用应用专属下载目录: ${publicDownloadDir.absolutePath}")
        
        // 生成安全的文件名，移除特殊字符，确保文件系统兼容性
        val safeFileName = videoInfo.title
            .replace("[^a-zA-Z0-9\u4e00-\u9fa5._-]".toRegex(), "_") // 保留中文、英文、数字、点、下划线、横线
            .take(100) // 限制文件名长度，避免文件系统限制

        // 确保应用专属下载目录存在
        if (!publicDownloadDir.exists()) {
            val created = publicDownloadDir.mkdirs()
            Log.d(TAG, "创建应用专属下载目录: path=${publicDownloadDir.absolutePath}, 创建结果=$created")
            if (!created) {
                Log.e(TAG, "无法创建应用专属下载目录")
                return Result.failure(Exception("无法创建下载目录"))
            }
        }

        // 在公共下载目录中创建输出文件
        val outputFile = File(publicDownloadDir, "${safeFileName}.mp4")
        Log.d(TAG, "输出文件路径: ${outputFile.absolutePath}")
        

        
        try {
            // 创建网络连接
            Log.d(TAG, "建立网络连接: url=$url, 超时=${NETWORK_TIMEOUT_MS}ms")
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
            connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()
            
            // 设置User-Agent避免某些服务器拒绝请求
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0")
            Log.d(TAG, "设置User-Agent请求头")
            
            val inputStream = connection.getInputStream()
            val outputStream = outputFile.outputStream()
            Log.d(TAG, "网络连接建立成功，开始读取数据流")
            
            val buffer = ByteArray(BUFFER_SIZE)
            var totalBytesRead = 0L
            val contentLength = connection.contentLength.toLong()
            var bytesRead: Int
            Log.d(TAG, "内容长度: $contentLength 字节")
            
            val startTime = System.currentTimeMillis()
            var lastProgressPercent = -1 // 记录上次回调的进度百分比
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                // 计算下载进度 - 使用浮点数计算避免精度丢失
                val progress = if (contentLength > 0) {
                    // 使用浮点数计算，然后转换为整数，确保小数部分不被丢失
                    ((totalBytesRead.toDouble() * 100.0) / contentLength.toDouble()).toFloat()
                } else {
                    // 如果无法获取文件大小，基于已下载字节数估算
                    minOf((totalBytesRead / (1024 * 1024)).toFloat(), 95f) // 每MB 1%，最多95%
                }
                
                // 计算下载速度 (字节/秒) - 避免除零错误
                val elapsedMillis = System.currentTimeMillis() - startTime
                val downloadSpeed = if (elapsedMillis > 1000) { // 至少1秒后才计算速度
                    (totalBytesRead * 1000) / elapsedMillis // 使用毫秒计算更精确
                } else {
                    0L // 开始阶段速度为0
                }
                
                // 每1%进度回调一次，避免频繁回调
                val progressPercent = progress.toInt()
                if (progressPercent != lastProgressPercent && progressCallback != null) {
                    progressCallback(progress, downloadSpeed, "下载中...")
                    lastProgressPercent = progressPercent
                    Log.d(TAG, "下载进度: ${progressPercent}%, 已下载: ${totalBytesRead}字节/${contentLength}字节, 速度: ${downloadSpeed}字节/秒")
                }
                
                // 减少延迟，提高响应性
                delay(100)
            }
            
            inputStream.close()
            outputStream.close()
            Log.d(TAG, "下载数据流处理完成，总下载: ${totalBytesRead}字节")
            
            // 最终进度回调
            progressCallback?.invoke(100f, 0L, "下载完成")
            
            Log.d(TAG, "视频下载成功完成: 文件=${outputFile.absolutePath}, 大小=${outputFile.length()}字节")
            return Result.success(outputFile.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "下载过程中发生未知错误: ${e.message}", e)
            progressCallback?.invoke(-1f, 0L, "下载失败")
            return Result.failure(Exception("下载失败: ${e.message}"))
        }
    }


    /**
     * 下载M3U8视频文件到外部存储公共下载目录
     * @param context 应用上下文，用于获取存储目录
     * @param videoInfo 视频信息对象，包含M3U8播放列表URL和标题等
     * @param title 视频标题，用于生成文件名
     * @param progressCallback 下载进度回调函数，参数为(进度百分比, 下载速度, 状态信息)
     * @return Result<String> 成功时返回文件绝对路径，失败时返回异常信息
     */
    suspend fun downloadM3U8Video(
        context: Context,
        videoInfo: VideoInfo? = null,
        title: String,
        progressCallback: ((Float, Long, String) -> Unit)?,
    ): Result<String> {
        if (videoInfo == null) {
            Log.e(TAG, "VideoInfo为空，无法开始M3U8下载")
            return Result.failure(Throwable("fetch_info_error_msg"))
        }

        return try {
            downloadM3U8VideoInternal(context, videoInfo, title, progressCallback)
        } catch (e: Exception) {
            Log.e(TAG, "M3U8下载失败: title=$title, error=${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * M3U8下载内部实现方法
     * 使用外部存储公共下载目录，用户可在文件管理器中直接访问
     */
    private suspend fun downloadM3U8VideoInternal(
        context: Context,
        videoInfo: VideoInfo,
        title: String,
        progressCallback: ((Float, Long, String) -> Unit)?
    ): Result<String> {
        // 使用外部存储的公共下载目录，在其下创建应用专属目录
        val baseDownloadDir = context.getExternalFilesDir("Downloads")
        val appName = context.getString(context.applicationInfo.labelRes).ifEmpty { "VideoDownloader" }
        val publicDownloadDir = File(baseDownloadDir, appName)
        if (!publicDownloadDir.exists()) {
            publicDownloadDir.mkdirs()
            Log.d(TAG, "创建应用下载目录: ${publicDownloadDir.absolutePath}")
        }
        
        // 生成安全的文件名
        val safeFileName = title
            .replace("[^a-zA-Z0-9\u4e00-\u9fa5._-]".toRegex(), "_")
            .take(100)
        val outputFile = File(publicDownloadDir, "$safeFileName.mp4")
        
        Log.d(TAG, "M3U8下载开始: url=${videoInfo.url}, 输出文件=${outputFile.absolutePath}")
        
        try {
            // 第一步：下载并解析M3U8播放列表
            Log.d(TAG, "步骤1: 下载M3U8播放列表文件")
            progressCallback?.invoke(5f, 0L, "下载播放列表...")
            val m3u8Content = downloadM3U8Playlist(videoInfo.url)
            Log.d(TAG, "M3U8播放列表内容长度: ${m3u8Content.length}字符")
            
            // 第二步：解析TS分片URL列表
            progressCallback?.invoke(10f, 0L, "解析播放列表...")
            val tsUrls = parseM3U8Content(m3u8Content, videoInfo.url)
            Log.d(TAG, "步骤2: 解析到${tsUrls.size}个TS分片")
            
            if (tsUrls.isEmpty()) {
                throw Exception("M3U8播放列表中未找到TS分片")
            }
            
            // 第三步：创建临时目录存储TS分片（使用应用私有目录）
            val privateCacheDir = context.cacheDir // 使用应用私有缓存目录
            val tempDir = File(privateCacheDir, "m3u8_temp_${System.currentTimeMillis()}")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
                Log.d(TAG, "创建应用私有临时目录: ${tempDir.absolutePath}")
            }
            
            // 第四步：下载所有TS分片
            Log.d(TAG, "步骤3: 开始下载${tsUrls.size}个TS分片")
            val tsFiles = mutableListOf<File>()
            val startTime = System.currentTimeMillis()
            
            tsUrls.forEachIndexed { index, tsUrl ->
                val tsFile = File(tempDir, "segment_${index.toString().padStart(4, '0')}.ts")
                Log.d(TAG, "下载TS分片 ${index + 1}/${tsUrls.size}: $tsUrl")
                
                try {
                    downloadTSSegment(tsUrl, tsFile)
                    tsFiles.add(tsFile)
                    
                    // 更新进度（下载阶段占80%）
                    val downloadProgress = 10f + ((index + 1) * 70f) / tsUrls.size
                    
                    // 计算下载速度
                    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                    val avgSpeed = if (elapsedSeconds > 0) {
                        ((index + 1) / elapsedSeconds * 100).toLong()
                    } else {
                        0L
                    }
                    
                    Log.v(TAG, "TS分片下载完成: segment_${index.toString().padStart(4, '0')}.ts, 大小: ${tsFile.length()}字节")
                    
                    // 调用进度回调
                    progressCallback?.invoke(downloadProgress, avgSpeed, "下载分片 ${index + 1}/${tsUrls.size}")
                    
                    delay(50) // 减少延迟
                    
                } catch (e: Exception) {
                    Log.e(TAG, "下载TS分片失败: index=$index, url=$tsUrl, error=${e.message}")
                    throw Exception("TS分片下载失败: ${e.message}")
                }
            }
            
            // 第五步：合并TS分片
            Log.d(TAG, "步骤4: 开始合并${tsFiles.size}个TS分片为MP4文件")
            progressCallback?.invoke(85f, 0L, "合并视频文件...")
            
            val success = mergeTSFilesDirectly(tsFiles, outputFile)
            if (success) {
                // 清理临时文件
                Log.d(TAG, "步骤5: 清理临时文件")
                cleanupTempFiles(tempDir)

                
                progressCallback?.invoke(100f, 0L, "下载完成")
                Log.d(TAG, "M3U8下载成功完成: 文件=${outputFile.absolutePath}, 大小=${outputFile.length()}字节")
                return Result.success(outputFile.absolutePath)
            } else {
                throw Exception("TS分片合并失败")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "M3U8视频下载失败: ${e.message}", e)
            progressCallback?.invoke(-1f, 0L, "下载失败")
            return Result.failure(Exception("M3U8下载失败: ${e.message}"))
        }
    }
    
    /**
     * 下载M3U8播放列表文件内容
     */
    private suspend fun downloadM3U8Playlist(url: String): String {
        Log.d(TAG, "下载M3U8播放列表: $url")
        val connection = java.net.URL(url).openConnection()
        connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0")
        
        return connection.getInputStream().bufferedReader().use { it.readText() }
    }
    
    /**
     * 解析M3U8内容，提取TS分片URL列表
     */
    private fun parseM3U8Content(content: String, baseUrl: String): List<String> {
        Log.d(TAG, "解析M3U8内容，基础URL: $baseUrl")
        val lines = content.split("\n")
        val tsUrls = mutableListOf<String>()
        val baseUri = java.net.URI(baseUrl)
        
        lines.forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                // 这是一个TS分片URL
                val tsUrl = if (trimmedLine.startsWith("http")) {
                    trimmedLine
                } else {
                    // 相对URL，需要与基础URL合并
                    baseUri.resolve(trimmedLine).toString()
                }
                tsUrls.add(tsUrl)
                Log.v(TAG, "解析到TS分片: $tsUrl")
            }
        }
        
        Log.d(TAG, "M3U8解析完成，共${tsUrls.size}个TS分片")
        return tsUrls
    }
    
    /**
     * 下载单个TS分片
     */
    private suspend fun downloadTSSegment(url: String, outputFile: File) {
        val connection = java.net.URL(url).openConnection()
        connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0")
        
        connection.getInputStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output, BUFFER_SIZE)
            }
        }
        
        Log.v(TAG, "TS分片下载完成: ${outputFile.name}, 大小: ${outputFile.length()}字节")
    }
    
    /**
     * 直接合并TS文件
     * TS格式支持简单的二进制拼接，无需复杂的编解码处理
     */
    private suspend fun mergeTSFilesDirectly(tsFiles: List<File>, outputFile: File): Boolean {
        return try {
            Log.d(TAG, "使用直接拼接方式合并TS文件")
            
            // 确保输出文件的父目录存在
            outputFile.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    parentDir.mkdirs()
                    Log.d(TAG, "创建输出目录: ${parentDir.absolutePath}")
                }
            }
            
            // 按顺序合并所有TS文件
            outputFile.outputStream().use { outputStream ->
                tsFiles.sortedBy { file ->
                    // 从文件名中提取序号进行排序
                    val fileName = file.nameWithoutExtension
                    val numberPart = fileName.substringAfterLast("_")
                    numberPart.toIntOrNull() ?: 0
                }.forEach { tsFile ->
                    if (tsFile.exists() && tsFile.length() > 0) {
                        Log.v(TAG, "合并TS文件: ${tsFile.name}, 大小: ${tsFile.length()}字节")
                        tsFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream, BUFFER_SIZE)
                        }
                    } else {
                        Log.w(TAG, "跳过无效TS文件: ${tsFile.name}")
                    }
                }
            }
            
            val success = outputFile.exists() && outputFile.length() > 0
            Log.d(TAG, "直接合并结果: success=$success, 文件大小: ${outputFile.length()}字节")
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "直接合并TS文件失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 清理临时文件和目录
     */
    private fun cleanupTempFiles(tempDir: File) {
        try {
            if (tempDir.exists()) {
                val deletedFiles = tempDir.listFiles()?.size ?: 0
                tempDir.deleteRecursively()
                Log.d(TAG, "清理临时目录完成: ${tempDir.absolutePath}, 删除${deletedFiles}个文件")
            }
        } catch (e: Exception) {
            Log.w(TAG, "清理临时文件失败: ${e.message}")
        }
    }


}