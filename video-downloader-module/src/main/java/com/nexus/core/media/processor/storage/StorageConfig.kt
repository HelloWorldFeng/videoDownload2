package com.nexus.core.media.processor.storage

import java.io.File

/**
 * 存储配置类
 * 
 * 定义媒体处理器的存储相关配置选项。
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
data class StorageConfig(
    /** 输出目录路径 */
    val outputDirectory: String? = null,
    
    /** 临时文件目录 */
    val tempDirectory: String? = null,
    
    /** 缓存目录 */
    val cacheDirectory: String? = null,
    
    /** 最大存储空间限制(MB) */
    val maxStorageSizeMB: Long = 1024L,
    
    /** 是否自动清理临时文件 */
    val autoCleanTempFiles: Boolean = true,
    
    /** 临时文件保留时间(小时) */
    val tempFileRetentionHours: Int = 24,
    
    /** 是否创建子目录 */
    val createSubdirectories: Boolean = false,
    
    /** 子目录模式 */
    val subdirectoryPattern: String = "date", // date, uploader, category
    
    /** 是否覆盖已存在的文件 */
    val overwriteExisting: Boolean = false,
    
    /** 文件名模板 */
    val filenameTemplate: String? = null,
    
    /** 是否启用文件完整性检查 */
    val enableIntegrityCheck: Boolean = true,
    
    /** 是否启用存储空间监控 */
    val enableStorageMonitoring: Boolean = true
) {
    
    /**
     * 验证配置的有效性
     * 
     * @return 错误信息列表，空列表表示配置有效
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (maxStorageSizeMB <= 0) {
            errors.add("最大存储空间限制必须大于0")
        }
        
        if (tempFileRetentionHours <= 0) {
            errors.add("临时文件保留时间必须大于0")
        }
        
        outputDirectory?.let { dir ->
            try {
                val file = File(dir)
                if (file.exists() && !file.isDirectory) {
                    errors.add("输出目录路径指向的不是目录: $dir")
                } else if (!file.exists() && !file.mkdirs()) {
                    errors.add("无法创建输出目录: $dir")
                } else {
                    // 目录存在且有效，或者成功创建
                }
            } catch (e: Exception) {
                errors.add("无效的输出目录路径: $dir")
            }
        }
        
        return errors
    }
    
    /**
     * 获取有效的输出目录
     * 
     * @param defaultDir 默认目录
     * @return 输出目录路径
     */
    fun getEffectiveOutputDirectory(defaultDir: String): String {
        return outputDirectory ?: defaultDir
    }
    
    /**
     * 获取有效的临时目录
     * 
     * @param baseDir 基础目录
     * @return 临时目录路径
     */
    fun getEffectiveTempDirectory(baseDir: String): String {
        return tempDirectory ?: "$baseDir/temp"
    }
    
    /**
     * 获取有效的缓存目录
     * 
     * @param baseDir 基础目录
     * @return 缓存目录路径
     */
    fun getEffectiveCacheDirectory(baseDir: String): String {
        return cacheDirectory ?: "$baseDir/cache"
    }
    
    companion object {
        /**
         * 创建默认配置
         */
        fun default(): StorageConfig {
            return StorageConfig()
        }
        
        /**
         * 创建高性能配置
         */
        fun highPerformance(): StorageConfig {
            return StorageConfig(
                maxStorageSizeMB = 2048L,
                autoCleanTempFiles = true,
                tempFileRetentionHours = 12,
                enableStorageMonitoring = true
            )
        }
        
        /**
         * 创建低存储配置
         */
        fun lowStorage(): StorageConfig {
            return StorageConfig(
                maxStorageSizeMB = 512L,
                autoCleanTempFiles = true,
                tempFileRetentionHours = 6,
                enableStorageMonitoring = true
            )
        }
    }
}