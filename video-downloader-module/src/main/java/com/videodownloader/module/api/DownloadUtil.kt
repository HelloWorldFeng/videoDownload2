package com.videodownloader.module.api

import VideoInfo
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
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
        // 使用私有目录，在其下创建应用专属目录
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

        // 在私有下载目录中创建输出文件
        val outputFile = File(publicDownloadDir, "${safeFileName}.mp4")
        Log.d(TAG, "输出文件路径: ${outputFile.absolutePath}")
        

        
        try {
            // 获取URL对应的请求头
            val headers = addHeadersForUrl(url)
            Log.d(TAG, "为URL添加请求头: url=$url, 请求头数量=${headers.size}")
            
            // 创建网络连接
            Log.d(TAG, "建立网络连接: url=$url, 超时=${NETWORK_TIMEOUT_MS}ms")
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
            connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()
            
            // 设置请求头以避免403错误
            setConnectionHeaders(connection, headers)
            Log.d(TAG, "请求头设置完成")
            
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

            }
            
            inputStream.close()
            outputStream.close()
            Log.d(TAG, "下载数据流处理完成，总下载: ${totalBytesRead}字节")
            //下载完成后移动到公共目录下

            // 尝试移动文件到公共下载目录
            val moveResult = moveFilesToPublicDirectory(context, listOf(outputFile.absolutePath))

            return if (moveResult.isSuccess) {
                // 移动成功，记录下载历史
                val publicPaths = moveResult.getOrThrow()
                val finalPaths = insertInfoIntoDownloadHistory(publicPaths)
                val finalPath = finalPaths.firstOrNull() ?: publicPaths.first()
                Log.d(TAG, "下载任务完成，文件已移动到公共目录: $finalPath")
                progressCallback?.invoke(100f, 0L, "下载完成")
                Log.d(TAG, "视频下载成功完成: 文件=${finalPath}, 大小=${outputFile.length()}字节")
                Result.success(finalPath)
            } else {
                // 移动失败，下载任务标记为失败
                val error = moveResult.exceptionOrNull() ?: Exception("文件移动到公共目录失败")
                Log.e(TAG, "下载任务失败：文件无法移动到公共目录", error)
                Result.failure(error)
            }
            
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
        // 使用私有下载目录，在其下创建应用专属目录
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
                //下载完成后移动到公共目录下

                // 尝试移动文件到公共下载目录
                val moveResult = moveFilesToPublicDirectory(context, listOf(outputFile.absolutePath))

                return if (moveResult.isSuccess) {
                    // 移动成功，记录下载历史
                    val publicPaths = moveResult.getOrThrow()
                    val finalPaths = insertInfoIntoDownloadHistory(publicPaths)
                    val finalPath = finalPaths.firstOrNull() ?: publicPaths.first()
                    Log.d(TAG, "下载任务完成，文件已移动到公共目录: $finalPath")
                    progressCallback?.invoke(100f, 0L, "下载完成")
                    Log.d(TAG, "M3U8下载成功完成: 文件=${finalPath}, 大小=${outputFile.length()}字节")

                    Result.success(finalPath)
                } else {
                    // 移动失败，下载任务标记为失败
                    val error = moveResult.exceptionOrNull() ?: Exception("文件移动到公共目录失败")
                    Log.e(TAG, "下载任务失败：文件无法移动到公共目录", error)
                    Result.failure(error)
                }
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
        
        // 获取URL对应的请求头
        val headers = addHeadersForUrl(url)
        Log.d(TAG, "为M3U8播放列表添加请求头: url=$url, 请求头数量=${headers.size}")
        
        val connection = java.net.URL(url).openConnection()
        connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()
        
        // 设置请求头以避免403错误
        setConnectionHeaders(connection, headers)
        Log.d(TAG, "M3U8播放列表请求头设置完成")
        
        return connection.getInputStream().bufferedReader().use { it.readText() }
    }
    
    /**
     * 解析M3U8内容，提取TS分片URL列表
     * 支持主播放列表(Master Playlist)和媒体播放列表(Media Playlist)
     */
    private suspend fun parseM3U8Content(content: String, baseUrl: String): List<String> {
        Log.d(TAG, "解析M3U8内容，基础URL: $baseUrl")
        val lines = content.split("\n")
        val baseUri = java.net.URI(baseUrl)
        
        // 检查是否为主播放列表(Master Playlist)
        val isMasterPlaylist = lines.any { it.trim().startsWith("#EXT-X-STREAM-INF") }
        
        if (isMasterPlaylist) {
            Log.d(TAG, "检测到主播放列表(Master Playlist)，需要解析媒体播放列表")
            
            // 查找第一个媒体播放列表URL
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    // 下一行应该是媒体播放列表URL
                    if (i + 1 < lines.size) {
                        val mediaPlaylistUrl = lines[i + 1].trim()
                        if (mediaPlaylistUrl.isNotEmpty() && !mediaPlaylistUrl.startsWith("#")) {
                            // 构建完整的媒体播放列表URL
                            val fullMediaUrl = if (mediaPlaylistUrl.startsWith("http")) {
                                mediaPlaylistUrl
                            } else {
                                baseUri.resolve(mediaPlaylistUrl).toString()
                            }
                            
                            Log.d(TAG, "找到媒体播放列表URL: $fullMediaUrl")
                            
                            // 递归下载并解析媒体播放列表
                            try {
                                val mediaPlaylistContent = downloadM3U8Playlist(fullMediaUrl)
                                Log.d(TAG, "成功下载媒体播放列表，内容长度: ${mediaPlaylistContent.length}字符")
                                return parseM3U8Content(mediaPlaylistContent, fullMediaUrl)
                            } catch (e: Exception) {
                                Log.e(TAG, "下载媒体播放列表失败: $fullMediaUrl, error=${e.message}")
                                throw Exception("无法下载媒体播放列表: ${e.message}")
                            }
                        }
                    }
                }
                i++
            }
            
            throw Exception("主播放列表中未找到有效的媒体播放列表URL")
        } else {
            // 这是媒体播放列表，直接解析TS分片
            Log.d(TAG, "检测到媒体播放列表(Media Playlist)，开始解析TS分片")
            val tsUrls = mutableListOf<String>()
            
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
            
            Log.d(TAG, "媒体播放列表解析完成，共${tsUrls.size}个TS分片")
            return tsUrls
        }
    }
    
    /**
     * 下载单个TS分片
     */
    private suspend fun downloadTSSegment(url: String, outputFile: File) {
        // 获取URL对应的请求头
        val headers = addHeadersForUrl(url)
        Log.v(TAG, "为TS分片添加请求头: url=$url, 请求头数量=${headers.size}")
        
        val connection = java.net.URL(url).openConnection()
        connection.connectTimeout = NETWORK_TIMEOUT_MS.toInt()
        connection.readTimeout = NETWORK_TIMEOUT_MS.toInt()
        
        // 设置请求头以避免403错误
        setConnectionHeaders(connection, headers)
        
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


        /**
     * 将下载完成的文件从私有目录移动到公共下载目录
     * 使用MediaStore API兼容Android 10+的分区存储限制
     * @param context 应用上下文，用于访问ContentResolver和存储目录
     * @param privatePaths 私有目录下的文件路径列表
     * @return Result<List<String>> 移动成功返回公共目录路径列表，失败返回错误
     */
    private fun moveFilesToPublicDirectory(context: Context, privatePaths: List<String>): Result<List<String>> {
        val publicPaths = mutableListOf<String>()
        var hasFailure = false
        var lastException: Exception? = null
        
        privatePaths.forEach { privatePath ->
            try {
                val privateFile = File(privatePath)
                if (!privateFile.exists()) {
                    val error = Exception("私有目录文件不存在: $privatePath")
                    Log.w(TAG, "私有目录文件不存在: $privatePath")
                    hasFailure = true
                    lastException = error
                    return@forEach
                }
                
                // 使用MediaStore API保存到公共下载目录
                val publicPath = moveFileUsingMediaStore(context, privateFile)
                if (publicPath != null) {
                    publicPaths.add(publicPath)
                    // 移动成功，删除私有目录原文件
                    val deleted = privateFile.delete()
                    if (deleted) {
                        Log.d(TAG, "文件移动成功并删除原文件: $publicPath")
                    } else {
                        Log.w(TAG, "文件移动成功但删除原文件失败: $privatePath")
                    }
                } else {
                    val error = Exception("使用MediaStore移动文件失败: $privatePath")
                    Log.e(TAG, "使用MediaStore移动文件失败: $privatePath")
                    hasFailure = true
                    lastException = error
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "移动文件异常: $privatePath", e)
                hasFailure = true
                lastException = e
            }
        }
        
        return if (hasFailure) {
            // 如果有任何文件移动失败，返回失败结果
            val error = lastException ?: Exception("文件移动到公共目录失败")
            Result.failure(error)
        } else {
            // 所有文件都移动成功
            Log.d(TAG, "所有文件成功移动到公共目录: $publicPaths")
            Result.success(publicPaths)
        }
    }

    /**
     * 使用MediaStore API将文件保存到公共下载目录
     * @param context 应用上下文，用于访问ContentResolver
     * @param sourceFile 源文件
     * @return 公共目录中的文件路径，失败时返回null
     */
    private fun moveFileUsingMediaStore(context: Context, sourceFile: File): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore API
                moveFileUsingMediaStoreAPI(context, sourceFile)
            } else {
                // Android 9及以下使用传统File API
                moveFileUsingFileAPI(sourceFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "移动文件失败", e)
            null
        }
    }

    /**
     * Android 10+ 使用MediaStore API移动文件
     * @param context 应用上下文，用于访问ContentResolver
     * @param sourceFile 源文件
     */
    private fun moveFileUsingMediaStoreAPI(context: Context, sourceFile: File): String? {
        return try {
            // 处理文件名，确保在Downloads目录中唯一
            val originalName = sourceFile.name
            val timeStamp = System.currentTimeMillis()
            val finalFileName = if (isFileExistsInDownloads(context, originalName)) {
                val nameWithoutExt = sourceFile.nameWithoutExtension
                val extension = sourceFile.extension
                "${nameWithoutExt}_$timeStamp.$extension"
            } else {
                originalName
            }
            
            Log.d(TAG, "开始使用MediaStore移动文件: ${sourceFile.absolutePath} -> Downloads/$finalFileName")
            
            // 准备ContentValues
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(sourceFile.extension))
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            // 插入到MediaStore
            val resolver = context.contentResolver
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                // Android 9及以下，使用Files表
                resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            }
            
            if (uri != null) {
                // 写入文件内容
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // 获取实际的文件路径
                val publicPath = getFilePathFromUri(context, uri) ?: "/storage/emulated/0/Download/$finalFileName"
                Log.d(TAG, "MediaStore移动文件成功: $publicPath")
                return publicPath
            } else {
                Log.e(TAG, "MediaStore插入失败，uri为null")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore移动文件异常", e)
            null
        }
    }

    /**
     * Android 9及以下使用传统File API移动文件
     */
    private fun moveFileUsingFileAPI(sourceFile: File): String? {
        return try {
            val publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            
            // 确保公共下载目录存在
            if (!publicDirectory.exists()) {
                publicDirectory.mkdirs()
            }
            
            // 构建公共目录的目标文件路径
            val publicFile = File(publicDirectory, sourceFile.name)
            
            // 确保目标文件名唯一（如果存在同名文件，添加时间戳）
            val finalPublicFile = if (publicFile.exists()) {
                val nameWithoutExt = sourceFile.nameWithoutExtension
                val extension = sourceFile.extension
                val timestamp = System.currentTimeMillis()
                File(publicDirectory, "${nameWithoutExt}_$timestamp.$extension")
            } else {
                publicFile
            }
            
            Log.d(TAG, "开始使用File API移动文件: ${sourceFile.absolutePath} -> ${finalPublicFile.absolutePath}")
            
            // 复制文件到公共目录
            sourceFile.copyTo(finalPublicFile, overwrite = true)
            
            // 验证复制成功
            if (finalPublicFile.exists() && finalPublicFile.length() == sourceFile.length()) {
                Log.d(TAG, "File API移动文件成功: ${finalPublicFile.absolutePath}")
                return finalPublicFile.absolutePath
            } else {
                Log.e(TAG, "File API文件复制验证失败")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "File API移动文件异常", e)
            null
        }
    }

    /**
     * 检查文件是否已存在于Downloads目录
     * @param context 应用上下文，用于访问ContentResolver
     * @param fileName 要检查的文件名
     */
    private fun isFileExistsInDownloads(context: Context, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore查询
                val resolver = context.contentResolver
                val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(fileName)
                
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
                
                resolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    cursor.count > 0
                } ?: false
            } else {
                // Android 9及以下使用File.exists()检查
                val publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(publicDirectory, fileName)
                file.exists()
            }
        } catch (e: Exception) {
            Log.w(TAG, "检查文件是否存在时异常", e)
            false
        }
    }

    /**
     * 从Uri获取实际文件路径
     * Android 10+由于分区存储限制，可能无法获取真实路径，会返回估计路径
     * @param context 应用上下文，用于访问ContentResolver
     * @param uri 要获取路径的Uri
     */
    private fun getFilePathFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 尝试获取真实路径，如果失败则使用估计路径
                val resolver = context.contentResolver
                
                // 尝试获取文件名
                val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                val fileName = resolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (columnIndex >= 0) cursor.getString(columnIndex) else null
                    } else {
                        null
                    }
                }
                
                // 构建估计的文件路径
                if (fileName != null) {
                    "/storage/emulated/0/Download/$fileName"
                } else {
                    // 从Uri中提取文件名作为最后的回退
                    val lastSegment = uri.lastPathSegment
                    if (lastSegment != null) {
                        "/storage/emulated/0/Download/$lastSegment"
                    } else {
                        "/storage/emulated/0/Download/downloaded_file"
                    }
                }
            } else {
                // Android 9及以下，尝试获取真实路径
                val resolver = context.contentResolver
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                
                resolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        cursor.getString(columnIndex)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "从Uri获取文件路径失败", e)
            // 提供默认路径作为回退
            "/storage/emulated/0/Download/downloaded_file"
        }
    }

    /**
     * 根据文件扩展名获取MIME类型
     */
    private fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            "3gp" -> "video/3gpp"
            else -> "video/*"
        }
    }

    private fun insertInfoIntoDownloadHistory(
        filePaths: List<String>,
    ): List<String> = filePaths.onEach { it }


    /**
     * 根据URL添加请求头以避免403错误
     * @param url 目标URL
     * @return 包含请求头的Map
     */
    private fun addHeadersForUrl(url: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val uri = url.toUri()
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
     * 为HTTP连接设置请求头
     * @param connection HTTP连接对象
     * @param headers 请求头Map
     */
    private fun setConnectionHeaders(connection: java.net.URLConnection, headers: Map<String, String>) {
        headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
            Log.v(TAG, "设置请求头: $key = $value")
        }
    }
}