package com.app.videobox.ui.pages.video.playerV2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder

/**
 * 视频播放Activity - 重构版本
 * 
 * 使用方法：
 * VideoPlayActivity.start(context, "视频标题", "视频URL")
 * 
 * 例如：
 * VideoPlayActivity.start(this, "测试视频", "file:///storage/emulated/0/Download/video.mp4")
 * VideoPlayActivity.start(this, "网络视频", "https://example.com/video.mp4")
 * 
 * 支持外部Intent：
 * - 从文件管理器选择视频文件
 * - 从其他应用分享视频链接
 * - 直接打开视频文件
 */
class VideoPlayActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val VIDEO_ACTIVITY_TAG = "VideoPlayActivity"

        /**
         * 启动视频播放Activity
         * @param context 上下文
         * @param title 视频标题
         * @param videoUrl 视频URL（支持本地文件和网络URL）
         */
        fun start(context: Context, title: String, videoUrl: String) {
            val intent = Intent(context, VideoPlayActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_VIDEO_URL, videoUrl)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 处理外部 Intent 和内部 Intent
        val (title, videoUrl) = handleIntent(intent)

        Log.d(VIDEO_ACTIVITY_TAG, "视频播放Activity启动 - 标题: $title, URL: $videoUrl")

        setContent {
            enableEdgeToEdge()
            VideoPlayerPage(
                title = title,
                videoUrl = videoUrl,
            )
        }
    }

    /**
     * 处理 Intent，提取视频标题和URL
     * 支持内部 Intent（通过 start 方法）和外部 Intent（ACTION_VIEW）
     */
    private fun handleIntent(intent: Intent): Pair<String, String> {
        Log.d(VIDEO_ACTIVITY_TAG, "处理Intent - Action: ${intent.action}")

        // 首先检查是否是内部 Intent
        val internalTitle = intent.getStringExtra(EXTRA_TITLE)
        val internalVideoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)

        if (!internalTitle.isNullOrEmpty() && !internalVideoUrl.isNullOrEmpty()) {
            Log.d(VIDEO_ACTIVITY_TAG, "内部Intent - 原始URL: $internalVideoUrl")
            // 处理内部 Intent，返回处理后的URL（修复bug：之前返回的是原始URL）
            val processedUrl = processVideoUrl(internalVideoUrl)
            Log.d(VIDEO_ACTIVITY_TAG, "内部Intent - 处理后URL: $processedUrl")
            return Pair(internalTitle, processedUrl)
        }

        // 处理外部 Intent (ACTION_VIEW)
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null) {
                val videoUrl = data.toString()
                Log.d(VIDEO_ACTIVITY_TAG, "外部Intent - 原始URL: $videoUrl")
                val title = extractTitleFromIntent(intent, data)
                val processedUrl = processVideoUrl(videoUrl)
                Log.d(VIDEO_ACTIVITY_TAG, "外部Intent - 处理后URL: $processedUrl")
                return Pair(title, processedUrl)
            }
        }

        // 默认值
        Log.w(VIDEO_ACTIVITY_TAG, "无效Intent，使用默认值")
        return Pair("Video Play", "")
    }

    /**
     * 视频URL路径处理器 - 专门解决VLC播放本地文件的URI格式问题
     * 确保本地文件路径能正确播放，特别是包含中文字符和特殊字符的文件名
     *
     * VLC要求：
     * 1. 本地文件必须使用file://协议
     * 2. 文件路径必须正确编码，特别是中文字符
     * 3. 避免VLC误判为SMB2协议
     */
    private fun processVideoUrl(videoUrl: String): String {
        Log.d(VIDEO_ACTIVITY_TAG, "开始处理视频URL: $videoUrl")

        return when {
            // 处理本地文件路径（以 /storage 开头）
            videoUrl.startsWith("/storage") -> {
                Log.d(VIDEO_ACTIVITY_TAG, "检测到本地存储路径")
                processLocalFilePath(videoUrl)
            }

            // 处理已有的 file:// URI
            videoUrl.startsWith("file://") -> {
                Log.d(VIDEO_ACTIVITY_TAG, "检测到file://协议URI")
                processFileUri(videoUrl)
            }

            // 处理 content:// URI（Android内容提供者）
            videoUrl.startsWith("content://") -> {
                Log.d(VIDEO_ACTIVITY_TAG, "检测到content://协议URI，尝试转换为真实路径")
                processContentUri(videoUrl)
            }

            // 处理网络URL（http/https）
            videoUrl.startsWith("http://") || videoUrl.startsWith("https://") -> {
                Log.d(VIDEO_ACTIVITY_TAG, "检测到网络URL，直接返回")
                videoUrl // 网络URL直接返回
            }

            // 其他情况尝试作为本地路径处理
            else -> {
                Log.d(VIDEO_ACTIVITY_TAG, "未知格式，尝试作为本地路径处理")
                processLocalFilePath(videoUrl)
            }
        }
    }

    /**
     * 处理本地文件路径，转换为VLC兼容的file:// URI格式
     * 专门解决中文文件名和特殊字符的编码问题，支持URL编码路径解码
     */
    private fun processLocalFilePath(filePath: String): String {
        try {
            // 检查路径是否包含URL编码字符（%字符）
            val decodedPath = if (filePath.contains("%")) {
                try {
                    val decoded = URLDecoder.decode(filePath, "UTF-8")
                    Log.d(VIDEO_ACTIVITY_TAG, "URL解码本地路径: $filePath -> $decoded")
                    decoded
                } catch (e: Exception) {
                    Log.w(VIDEO_ACTIVITY_TAG, "本地路径URL解码失败，使用原始路径: ${e.message}")
                    filePath
                }
            } else {
                filePath
            }

            val file = File(decodedPath)
            Log.d(VIDEO_ACTIVITY_TAG, "文件存在检查: ${file.exists()}, 文件路径: ${file.absolutePath}")

            if (!file.exists()) {
                // 如果解码后的文件不存在，尝试原始路径
                if (decodedPath != filePath) {
                    Log.w(VIDEO_ACTIVITY_TAG, "解码后的文件不存在，尝试原始路径")
                    val originalFile = File(filePath)
                    if (originalFile.exists()) {
                        Log.d(VIDEO_ACTIVITY_TAG, "使用原始路径: $filePath")
                        val uri = Uri.fromFile(originalFile)
                        return uri.toString()
                    }
                }
                Log.w(VIDEO_ACTIVITY_TAG, "文件不存在: 解码路径=$decodedPath, 原始路径=$filePath")
                return filePath // 文件不存在时返回原始路径
            }

            // 使用Uri.fromFile()创建正确的file:// URI
            // 这个方法会自动处理特殊字符的编码
            val uri = Uri.fromFile(file)
            val uriString = uri.toString()

            Log.d(VIDEO_ACTIVITY_TAG, "本地文件URI转换: $filePath -> $uriString")
            return uriString

        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "处理本地文件路径时发生错误: ${e.message}", e)
            return filePath // 发生异常时返回原始路径
        }
    }

    /**
     * 处理已有的file:// URI，确保格式正确
     * 重新验证并规范化URI格式，解决URL编码的中文文件名问题
     */
    private fun processFileUri(fileUri: String): String {
        try {
            val uri = Uri.parse(fileUri)
            val path = uri.path

            if (path != null) {
                Log.d(VIDEO_ACTIVITY_TAG, "从file:// URI提取路径: $path")

                // URL解码处理，解决中文文件名被编码的问题
                val decodedPath = try {
                    URLDecoder.decode(path, "UTF-8")
                } catch (e: Exception) {
                    Log.w(VIDEO_ACTIVITY_TAG, "URL解码失败，使用原始路径: ${e.message}")
                    path
                }

                Log.d(VIDEO_ACTIVITY_TAG, "URL解码结果: $path -> $decodedPath")

                // 验证解码后的文件是否存在
                val file = File(decodedPath)
                if (file.exists()) {
                    // 重新生成标准化的file:// URI
                    val standardUri = Uri.fromFile(file)
                    val standardUriString = standardUri.toString()
                    Log.d(VIDEO_ACTIVITY_TAG, "file:// URI标准化: $fileUri -> $standardUriString")
                    return standardUriString
                } else {
                    // 如果解码后的文件不存在，尝试原始路径
                    Log.w(VIDEO_ACTIVITY_TAG, "解码后的文件不存在，尝试原始路径")
                    val originalFile = File(path)
                    if (originalFile.exists()) {
                        val standardUri = Uri.fromFile(originalFile)
                        val standardUriString = standardUri.toString()
                        Log.d(VIDEO_ACTIVITY_TAG, "使用原始路径生成URI: $standardUriString")
                        return standardUriString
                    } else {
                        Log.w(VIDEO_ACTIVITY_TAG, "file:// URI对应的文件不存在: 解码路径=$decodedPath, 原始路径=$path")
                    }
                }
            }

            // 如果无法处理，返回原始URI
            return fileUri

        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "处理file:// URI时发生错误: ${e.message}", e)
            return fileUri
        }
    }

    /**
     * 处理content:// URI，转换为VLC可以播放的格式
     * content URI无法直接被VLC播放，需要转换为真实文件路径或复制到临时文件
     * Android 10+分区存储下，直接文件路径访问可能受限，优先使用临时文件复制
     */
    private fun processContentUri(contentUri: String): String {
        return try {
            val uri = Uri.parse(contentUri)
            Log.d(VIDEO_ACTIVITY_TAG, "开始处理content URI: $uri")

            // 优先方案：复制到临时文件（避免分区存储权限问题）
            Log.d(VIDEO_ACTIVITY_TAG, "优先尝试复制到临时文件，避免分区存储权限问题")
            val tempFilePath = copyContentToTempFile(uri)
            if (tempFilePath != null) {
                Log.d(VIDEO_ACTIVITY_TAG, "成功复制到临时文件: $tempFilePath")
                return processLocalFilePath(tempFilePath)
            }

            // 备用方案：尝试获取真实文件路径（仅适用于Android 9及以下）
            Log.d(VIDEO_ACTIVITY_TAG, "临时文件复制失败，尝试真实路径访问")
            val realPath = getRealPathFromContentUri(uri)
            if (realPath != null && File(realPath).exists()) {
                Log.d(VIDEO_ACTIVITY_TAG, "成功获取真实路径: $realPath")

                // 检查是否是外部存储路径，如果是则可能有权限问题
                if (realPath.startsWith("/storage/emulated/0/") || realPath.contains("Download")) {
                    Log.w(VIDEO_ACTIVITY_TAG, "检测到外部存储路径，可能存在分区存储权限限制")
                    // 对于外部存储文件，强制使用临时文件复制
                    val forceTemp = copyContentToTempFile(uri)
                    if (forceTemp != null) {
                        Log.d(VIDEO_ACTIVITY_TAG, "强制复制到临时文件成功: $forceTemp")
                        return processLocalFilePath(forceTemp)
                    }
                }

                return processLocalFilePath(realPath)
            }

            // 最后方案：返回原始URI
            Log.w(VIDEO_ACTIVITY_TAG, "所有转换方法失败，返回原始content URI")
            contentUri

        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "处理content URI时发生错误: ${e.message}", e)
            contentUri // 发生异常时返回原始URI
        }
    }

    /**
     * 从content URI获取真实文件路径
     * 支持不同的content provider
     */
    private fun getRealPathFromContentUri(uri: Uri): String? {
        return try {
            when {
                // Documents Provider
                DocumentsContract.isDocumentUri(this, uri) -> {
                    Log.d(VIDEO_ACTIVITY_TAG, "处理Documents Provider URI")
                    getRealPathFromDocumentUri(uri)
                }

                // MediaStore Provider
                uri.authority == "media" -> {
                    Log.d(VIDEO_ACTIVITY_TAG, "处理MediaStore Provider URI")
                    getRealPathFromMediaStore(uri)
                }

                // 其他Provider
                else -> {
                    Log.d(VIDEO_ACTIVITY_TAG, "处理其他Provider URI")
                    getRealPathFromGenericUri(uri)
                }
            }
        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "获取真实路径时发生错误: ${e.message}", e)
            null
        }
    }

    /**
     * 从Documents Provider获取真实路径
     */
    private fun getRealPathFromDocumentUri(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            Log.d(VIDEO_ACTIVITY_TAG, "Document ID: $docId")

            when (uri.authority) {
                "com.android.providers.downloads.documents" -> {
                    // Downloads Provider
                    if (docId.startsWith("raw:")) {
                        // 直接的文件路径
                        return docId.substring(4)
                    }

                    // 尝试构建下载文件路径
                    val fileName = getDisplayNameFromUri(uri)
                    if (fileName != null) {
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        )
                        val file = File(downloadsDir, fileName)
                        if (file.exists()) {
                            return file.absolutePath
                        }
                    }
                    null
                }

                "com.android.providers.media.documents" -> {
                    // Media Provider
                    val split = docId.split(":")
                    if (split.size >= 2) {
                        val type = split[0]
                        val id = split[1]

                        val contentUri = when (type) {
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> MediaStore.Files.getContentUri("external")
                        }

                        return getRealPathFromMediaStore(contentUri, "_id=?", arrayOf(id))
                    }
                    null
                }

                else -> {
                    Log.w(VIDEO_ACTIVITY_TAG, "未知的Documents Provider: ${uri.authority}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "处理Documents Provider URI时发生错误: ${e.message}", e)
            null
        }
    }

    /**
     * 从MediaStore获取真实路径
     */
    private fun getRealPathFromMediaStore(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): String? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    cursor.getString(columnIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "从MediaStore获取路径时发生错误: ${e.message}", e)
            null
        }
    }

    /**
     * 从通用URI获取真实路径
     */
    private fun getRealPathFromGenericUri(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (columnIndex >= 0) {
                        cursor.getString(columnIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "从通用URI获取路径时发生错误: ${e.message}", e)
            null
        }
    }

    /**
     * 从URI获取显示名称
     */
    private fun getDisplayNameFromUri(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "获取显示名称时发生错误: ${e.message}", e)
            null
        }
    }

    /**
     * 将content URI的内容复制到临时文件
     * 专门解决分区存储权限问题的可靠方案
     */
    private fun copyContentToTempFile(uri: Uri): String? {
        return try {
            // 获取文件名，去除特殊字符避免文件系统问题
            val originalFileName = getDisplayNameFromUri(uri) ?: "temp_video_${System.currentTimeMillis()}.mp4"
            val safeFileName = sanitizeFileName(originalFileName)

            val tempDir = File(cacheDir, "video_temp").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(tempDir, safeFileName)

            // 如果文件已存在且大小合理，直接返回
            if (targetFile.exists() && targetFile.length() > 1024) {
                Log.d(VIDEO_ACTIVITY_TAG, "临时文件已存在，直接使用: ${targetFile.absolutePath}")
                return targetFile.absolutePath
            }

            Log.d(VIDEO_ACTIVITY_TAG, "开始复制content到临时文件: ${targetFile.absolutePath}")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    // 使用缓冲区提高复制效率
                    val buffer = ByteArray(8192)
                    var totalBytes = 0L
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        // 每复制1MB输出一次进度日志
                        if (totalBytes % (1024 * 1024) == 0L) {
                            Log.d(VIDEO_ACTIVITY_TAG, "复制进度: ${totalBytes / (1024 * 1024)}MB")
                        }
                    }

                    outputStream.flush()
                    Log.d(VIDEO_ACTIVITY_TAG, "复制完成，总大小: ${totalBytes} bytes")
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d(VIDEO_ACTIVITY_TAG, "复制成功，文件: ${targetFile.absolutePath}, 大小: ${targetFile.length()} bytes")
                targetFile.absolutePath
            } else {
                Log.e(VIDEO_ACTIVITY_TAG, "复制失败，文件不存在或大小为0")
                null
            }

        } catch (e: Exception) {
            Log.e(VIDEO_ACTIVITY_TAG, "复制content到临时文件时发生错误: ${e.message}", e)
            null
        }
    }

    /**
     * 清理文件名中的特殊字符，确保文件系统兼容性
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[#@\\s]+"), "_")  // 替换 # @ 空格为下划线
            .replace(Regex("[⋯]+"), "...")   // 替换特殊省略号
            .replace(Regex("[\\[\\]]+"), "_") // 替换方括号
            .replace(Regex("_{2,}"), "_")     // 多个下划线合并为一个
            .trim('_')                       // 去除首尾下划线
    }

    /**
     * 从 Intent 中提取视频标题
     */
    private fun extractTitleFromIntent(intent: Intent, uri: Uri): String {
        // 尝试从 Intent 的 extras 中获取标题
        val title = intent.getStringExtra(Intent.EXTRA_TITLE)
        if (!title.isNullOrEmpty()) {
            return title
        }

        // 尝试从 URI 的查询参数中获取标题
        val queryTitle = uri.getQueryParameter("title")
        if (!queryTitle.isNullOrEmpty()) {
            return queryTitle
        }

        // 从文件路径中提取文件名作为标题
        return when (uri.scheme) {
            "file" -> {
                val path = uri.path
                if (!path.isNullOrEmpty()) {
                    val fileName = path.substringAfterLast("/", "Unknown Video")
                    // 移除文件扩展名
                    fileName.substringBeforeLast(".", fileName)
                } else {
                    "Local Video"
                }
            }
            "content" -> {
                // 对于 content URI，尝试获取文件名
                try {
                    val cursor = contentResolver.query(uri, arrayOf("_display_name"), null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val displayName = it.getString(0)
                            if (!displayName.isNullOrEmpty()) {
                                return displayName.substringBeforeLast(".", displayName)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略异常，使用默认标题
                }
                "Content Video"
            }
            else -> {
                // 对于网络 URL，使用域名或默认标题
                val host = uri.host
                if (!host.isNullOrEmpty()) {
                    "Video from $host"
                } else {
                    "Network Video"
                }
            }
        }
    }
}