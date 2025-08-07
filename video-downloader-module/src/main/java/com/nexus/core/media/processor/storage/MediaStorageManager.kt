package com.nexus.core.media.processor.storage

import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.utils.ProcessorLogUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

/**
 * 媒体存储管理器
 * 
 * 负责媒体文件的存储管理，包括文件操作、
 * 目录管理、空间检查、清理等功能。
 * 
 * 主要功能：
 * - 文件存储管理
 * - 目录结构维护
 * - 存储空间监控
 * - 文件完整性验证
 * - 自动清理机制
 * 
 * 使用示例：
 * ```kotlin
 * val storageManager = MediaStorageManager(configuration)
 * val outputPath = storageManager.generateOutputPath(mediaInfo, format)
 * storageManager.ensureStorageSpace(requiredSize)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class MediaStorageManager(
    private val configuration: ProcessorConfiguration
) {
    
    companion object {
        private const val LOG_TAG = "MediaStorageManager"
        private const val TEMP_FILE_PREFIX = "media_temp_"
        private const val CACHE_FILE_PREFIX = "media_cache_"
        private const val MIN_FREE_SPACE_MB = 100L // 最小剩余空间100MB
        private const val CLEANUP_THRESHOLD_DAYS = 7 // 清理7天前的临时文件
    }
    
    // 目录路径
    private val outputDirectory: File by lazy { File(configuration.outputDirectory) }
    private val tempDirectory: File by lazy { File(configuration.getTempDirectory()) }
    private val cacheDirectory: File by lazy { File(configuration.getCacheDirectory()) }
    private val logDirectory: File by lazy { File(configuration.getLogDirectory()) }
    
    // 存储统计
    private var totalStorageUsed = 0L
    private var tempStorageUsed = 0L
    private var cacheStorageUsed = 0L
    
    init {
        initializeDirectories()
        calculateStorageUsage()
    }
    
    /**
     * 存储信息
     */
    data class StorageInfo(
        val totalSpace: Long,
        val freeSpace: Long,
        val usedSpace: Long,
        val outputDirSize: Long,
        val tempDirSize: Long,
        val cacheDirSize: Long,
        val logDirSize: Long
    )
    
    /**
     * 文件操作结果
     */
    data class FileOperationResult(
        val success: Boolean,
        val message: String? = null,
        val filePath: String? = null
    )
    
    /**
     * 生成输出文件路径
     * 
     * @param mediaInfo 媒体信息
     * @param format 媒体格式
     * @param options 处理选项
     * @return 输出文件路径
     */
    suspend fun generateOutputPath(
        mediaInfo: MediaInfo,
        format: MediaFormat,
        options: ProcessingOptions = ProcessingOptions()
    ): String = withContext(Dispatchers.IO) {
        try {
            // 生成文件名
            val fileName = generateFileName(mediaInfo, format, options)
            
            // 生成子目录（如果配置了）
            val subDirectory = generateSubDirectory(mediaInfo, options)
            
            // 组合完整路径
            val fullDirectory = if (subDirectory.isNotEmpty()) {
                File(outputDirectory, subDirectory)
            } else {
                outputDirectory
            }
            
            // 确保目录存在
            if (!fullDirectory.exists()) {
                fullDirectory.mkdirs()
            }
            
            // 处理文件名冲突
            val finalFileName = resolveFileNameConflict(fullDirectory, fileName)
            
            val outputPath = File(fullDirectory, finalFileName).absolutePath
            ProcessorLogUtils.d(LOG_TAG, "生成输出路径: $outputPath")
            
            outputPath
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "生成输出路径失败", e)
            throw StorageException("生成输出路径失败: ${e.message}", e)
        }
    }
    
    /**
     * 生成临时文件路径
     * 
     * @param taskId 任务ID
     * @param extension 文件扩展名
     * @return 临时文件路径
     */
    suspend fun generateTempFilePath(
        taskId: String,
        extension: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val fileName = if (extension != null) {
                "${TEMP_FILE_PREFIX}${taskId}.$extension"
            } else {
                "${TEMP_FILE_PREFIX}${taskId}.tmp"
            }
            
            val tempFile = File(tempDirectory, fileName)
            val tempPath = tempFile.absolutePath
            
            ProcessorLogUtils.d(LOG_TAG, "生成临时文件路径: $tempPath")
            tempPath
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "生成临时文件路径失败", e)
            throw StorageException("生成临时文件路径失败: ${e.message}", e)
        }
    }
    
    /**
     * 检查存储空间
     * 
     * @param requiredSize 需要的空间大小（字节）
     * @return 是否有足够空间
     */
    suspend fun checkStorageSpace(requiredSize: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val freeSpace = outputDirectory.freeSpace
            val minRequiredSpace = requiredSize + (MIN_FREE_SPACE_MB * 1024 * 1024)
            
            val hasEnoughSpace = freeSpace >= minRequiredSpace
            
            ProcessorLogUtils.d(LOG_TAG, "存储空间检查: 需要=${formatFileSize(requiredSize)}, " +
                    "可用=${formatFileSize(freeSpace)}, 足够=$hasEnoughSpace")
            
            hasEnoughSpace
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "检查存储空间失败", e)
            false
        }
    }
    
    /**
     * 确保存储空间
     * 
     * @param requiredSize 需要的空间大小
     * @return 是否成功确保空间
     */
    suspend fun ensureStorageSpace(requiredSize: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // 首先检查当前空间
            if (checkStorageSpace(requiredSize)) {
                return@withContext true
            }
            
            ProcessorLogUtils.i(LOG_TAG, "存储空间不足，开始清理")
            
            // 清理临时文件
            cleanupTempFiles()
            
            // 清理缓存文件
            if (!checkStorageSpace(requiredSize)) {
                cleanupCacheFiles()
            }
            
            // 清理旧的输出文件（如果配置允许）
            if (!checkStorageSpace(requiredSize) && configuration.getAutoCleanupOldFiles()) {
                cleanupOldOutputFiles()
            }
            
            // 最终检查
            val hasSpace = checkStorageSpace(requiredSize)
            
            if (hasSpace) {
                ProcessorLogUtils.i(LOG_TAG, "存储空间清理成功")
            } else {
                ProcessorLogUtils.w(LOG_TAG, "存储空间清理后仍然不足")
            }
            
            hasSpace
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "确保存储空间失败", e)
            false
        }
    }
    
    /**
     * 移动文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 操作结果
     */
    suspend fun moveFile(
        sourcePath: String,
        targetPath: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val targetFile = File(targetPath)
            
            if (!sourceFile.exists()) {
                return@withContext FileOperationResult(
                    success = false,
                    message = "源文件不存在: $sourcePath"
                )
            }
            
            // 确保目标目录存在
            val targetDir = targetFile.parentFile
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            // 如果目标文件已存在，删除它
            if (targetFile.exists()) {
                targetFile.delete()
            }
            
            // 尝试重命名（最快的方式）
            val success = sourceFile.renameTo(targetFile)
            
            if (success) {
                ProcessorLogUtils.d(LOG_TAG, "文件移动成功: $sourcePath -> $targetPath")
                FileOperationResult(
                    success = true,
                    filePath = targetPath
                )
            } else {
                // 重命名失败，尝试复制后删除
                copyFile(sourcePath, targetPath)
                sourceFile.delete()
                
                ProcessorLogUtils.d(LOG_TAG, "文件复制移动成功: $sourcePath -> $targetPath")
                FileOperationResult(
                    success = true,
                    filePath = targetPath
                )
            }
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "移动文件失败: $sourcePath -> $targetPath", e)
            FileOperationResult(
                success = false,
                message = "移动文件失败: ${e.message}"
            )
        }
    }
    
    /**
     * 复制文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 操作结果
     */
    suspend fun copyFile(
        sourcePath: String,
        targetPath: String
    ): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val targetFile = File(targetPath)
            
            if (!sourceFile.exists()) {
                return@withContext FileOperationResult(
                    success = false,
                    message = "源文件不存在: $sourcePath"
                )
            }
            
            // 确保目标目录存在
            val targetDir = targetFile.parentFile
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            // 使用NIO进行高效复制
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val inputChannel = input.channel
                    val outputChannel = output.channel
                    
                    var position = 0L
                    val size = inputChannel.size()
                    
                    while (position < size) {
                        val transferred = inputChannel.transferTo(
                            position,
                            min(size - position, 1024 * 1024), // 1MB chunks
                            outputChannel
                        )
                        position += transferred
                    }
                }
            }
            
            ProcessorLogUtils.d(LOG_TAG, "文件复制成功: $sourcePath -> $targetPath")
            FileOperationResult(
                success = true,
                filePath = targetPath
            )
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "复制文件失败: $sourcePath -> $targetPath", e)
            FileOperationResult(
                success = false,
                message = "复制文件失败: ${e.message}"
            )
        }
    }
    
    /**
     * 删除文件
     * 
     * @param filePath 文件路径
     * @return 操作结果
     */
    suspend fun deleteFile(filePath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            
            if (!file.exists()) {
                return@withContext FileOperationResult(
                    success = true,
                    message = "文件不存在，无需删除"
                )
            }
            
            val success = file.delete()
            
            if (success) {
                ProcessorLogUtils.d(LOG_TAG, "文件删除成功: $filePath")
                FileOperationResult(success = true)
            } else {
                ProcessorLogUtils.w(LOG_TAG, "文件删除失败: $filePath")
                FileOperationResult(
                    success = false,
                    message = "文件删除失败"
                )
            }
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "删除文件异常: $filePath", e)
            FileOperationResult(
                success = false,
                message = "删除文件异常: ${e.message}"
            )
        }
    }
    
    /**
     * 验证文件完整性
     * 
     * @param filePath 文件路径
     * @param expectedSize 期望文件大小
     * @param expectedHash 期望文件哈希值
     * @return 验证结果
     */
    suspend fun verifyFileIntegrity(
        filePath: String,
        expectedSize: Long? = null,
        expectedHash: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            
            if (!file.exists()) {
                ProcessorLogUtils.w(LOG_TAG, "验证文件不存在: $filePath")
                return@withContext false
            }
            
            // 检查文件大小
            if (expectedSize != null && file.length() != expectedSize) {
                ProcessorLogUtils.w(LOG_TAG, "文件大小不匹配: 期望=$expectedSize, 实际=${file.length()}")
                return@withContext false
            }
            
            // 检查文件哈希
            if (expectedHash != null) {
                val actualHash = calculateFileHash(filePath)
                if (actualHash != expectedHash.lowercase()) {
                    ProcessorLogUtils.w(LOG_TAG, "文件哈希不匹配: 期望=$expectedHash, 实际=$actualHash")
                    return@withContext false
                }
            }
            
            ProcessorLogUtils.d(LOG_TAG, "文件完整性验证通过: $filePath")
            true
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "验证文件完整性失败: $filePath", e)
            false
        }
    }
    
    /**
     * 获取存储信息
     * 
     * @return 存储信息
     */
    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        try {
            calculateStorageUsage()
            
            StorageInfo(
                totalSpace = outputDirectory.totalSpace,
                freeSpace = outputDirectory.freeSpace,
                usedSpace = outputDirectory.totalSpace - outputDirectory.freeSpace,
                outputDirSize = calculateDirectorySize(outputDirectory),
                tempDirSize = calculateDirectorySize(tempDirectory),
                cacheDirSize = calculateDirectorySize(cacheDirectory),
                logDirSize = calculateDirectorySize(logDirectory)
            )
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "获取存储信息失败", e)
            StorageInfo(0, 0, 0, 0, 0, 0, 0)
        }
    }
    
    /**
     * 清理临时文件
     */
    suspend fun cleanupTempFiles(olderThanDays: Int = CLEANUP_THRESHOLD_DAYS) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            var deletedCount = 0
            var deletedSize = 0L
            
            tempDirectory.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    deletedSize += file.length()
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            ProcessorLogUtils.i(LOG_TAG, "清理临时文件完成: 删除${deletedCount}个文件, 释放${formatFileSize(deletedSize)}")
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "清理临时文件失败", e)
        }
    }
    
    /**
     * 清理缓存文件
     */
    suspend fun cleanupCacheFiles(olderThanDays: Int = CLEANUP_THRESHOLD_DAYS) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            var deletedCount = 0
            var deletedSize = 0L
            
            cacheDirectory.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    deletedSize += file.length()
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            ProcessorLogUtils.i(LOG_TAG, "清理缓存文件完成: 删除${deletedCount}个文件, 释放${formatFileSize(deletedSize)}")
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "清理缓存文件失败", e)
        }
    }
    
    /**
     * 清理旧的输出文件
     */
    private suspend fun cleanupOldOutputFiles(olderThanDays: Int = 30) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            var deletedCount = 0
            var deletedSize = 0L
            
            fun cleanDirectory(dir: File) {
                dir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        cleanDirectory(file)
                        // 如果目录为空，删除它
                        if (file.listFiles()?.isEmpty() == true) {
                            file.delete()
                        }
                    } else if (file.lastModified() < cutoffTime) {
                        deletedSize += file.length()
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
            
            cleanDirectory(outputDirectory)
            
            ProcessorLogUtils.i(LOG_TAG, "清理旧输出文件完成: 删除${deletedCount}个文件, 释放${formatFileSize(deletedSize)}")
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "清理旧输出文件失败", e)
        }
    }
    
    /**
     * 初始化目录
     */
    private fun initializeDirectories() {
        try {
            listOf(outputDirectory, tempDirectory, cacheDirectory, logDirectory).forEach { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                    ProcessorLogUtils.d(LOG_TAG, "创建目录: ${dir.absolutePath}")
                }
            }
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "初始化目录失败", e)
            throw StorageException("初始化目录失败: ${e.message}", e)
        }
    }
    
    /**
     * 生成文件名
     */
    private fun generateFileName(
        mediaInfo: MediaInfo,
        format: MediaFormat,
        options: ProcessingOptions
    ): String {
        return if (options.filenameTemplate != null) {
            // 使用自定义模板
            options.filenameTemplate
                .replace("{title}", sanitizeFileName(mediaInfo.title))
                .replace("{id}", mediaInfo.id)
                .replace("{ext}", format.extension ?: "mp4")
                .replace("{uploader}", sanitizeFileName(mediaInfo.uploader ?: "unknown"))
                .replace("{date}", SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()))
        } else {
            // 使用默认格式
            val title = sanitizeFileName(mediaInfo.title)
            val extension = format.extension ?: "mp4"
            "$title.$extension"
        }
    }
    
    /**
     * 生成子目录
     */
    private fun generateSubDirectory(
        mediaInfo: MediaInfo,
        options: ProcessingOptions
    ): String {
        return if (options.createSubdirectory) {
            when (options.subdirectoryPattern) {
                "uploader" -> sanitizeFileName(mediaInfo.uploader ?: "unknown")
                "date" -> SimpleDateFormat("yyyy/MM", Locale.getDefault()).format(Date())
                "category" -> sanitizeFileName(mediaInfo.categories.firstOrNull() ?: "uncategorized")
                else -> ""
            }
        } else {
            ""
        }
    }
    
    /**
     * 解决文件名冲突
     */
    private fun resolveFileNameConflict(directory: File, fileName: String): String {
        var finalFileName = fileName
        var counter = 1
        
        while (File(directory, finalFileName).exists()) {
            val nameWithoutExt = fileName.substringBeforeLast('.')
            val extension = fileName.substringAfterLast('.', "")
            
            finalFileName = if (extension.isNotEmpty()) {
                "${nameWithoutExt}_$counter.$extension"
            } else {
                "${nameWithoutExt}_$counter"
            }
            counter++
        }
        
        return finalFileName
    }
    
    /**
     * 清理文件名
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100) // 限制长度
    }
    
    /**
     * 计算目录大小
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "计算目录大小失败: ${directory.absolutePath}", e)
        }
        return size
    }
    
    /**
     * 计算存储使用量
     */
    private fun calculateStorageUsage() {
        try {
            totalStorageUsed = calculateDirectorySize(outputDirectory)
            tempStorageUsed = calculateDirectorySize(tempDirectory)
            cacheStorageUsed = calculateDirectorySize(cacheDirectory)
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "计算存储使用量失败", e)
        }
    }
    
    /**
     * 计算文件哈希值
     */
    private suspend fun calculateFileHash(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("MD5")
            val file = File(filePath)
            
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
            
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "计算文件哈希失败: $filePath", e)
            ""
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }
    
    /**
     * 清理资源
     */
    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            try {
                ProcessorLogUtils.i(LOG_TAG, "开始清理存储管理器")
                
                // 清理临时文件
                cleanupTempFiles(0) // 清理所有临时文件
                
                ProcessorLogUtils.i(LOG_TAG, "存储管理器清理完成")
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "清理存储管理器失败", e)
            }
        }
    }
}

/**
 * 存储异常
 */
class StorageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)