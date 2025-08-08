package com.videodownloader.module.api

import kotlinx.serialization.Serializable

/**
 * 视频下载器数据类
 * 
 * 设计目的：
 * - 提供简洁的数据结构定义
 * - 统一视频下载相关的数据模型
 * - 支持序列化和反序列化
 * - 便于数据传输和存储
 */

/**
 * 视频信息数据类
 * 
 * 包含视频的基本信息：
 * - 视频标识、标题、时长
 * - 缩略图、文件大小、下载链接
 * - 文件格式扩展名
 * 
 * @property id 视频唯一标识符
 * @property title 视频标题
 * @property duration 视频时长（秒）
 * @property thumbnail 缩略图URL
 * @property size 文件大小（字节）
 * @property url 视频下载链接
 * @property ext 文件扩展名
 */
@Serializable
data class VideoInfo(
    /** 视频唯一标识符 */
    val id: String,
    
    /** 视频标题 */
    val title: String,
    
    /** 视频时长（秒） */
    val duration: Float,
    
    /** 缩略图URL */
    val thumbnail: String,
    
    /** 文件大小（字节） */
    val size: Long,
    
    /** 视频下载链接 */
    val url: String,
    
    /** 文件扩展名 */
    val ext: String
)

/**
 * 下载状态枚举
 * 
 * 定义下载任务的各种状态：
 * - IDLE: 空闲状态，任务已创建但未开始
 * - DOWNLOADING: 下载中状态
 * - PAUSED: 暂停状态
 * - COMPLETED: 完成状态
 * - FAILED: 失败状态
 * - CANCELLED: 取消状态
 */
@Serializable
enum class DownloadStatus {
    /** 空闲状态：任务已创建但未开始下载 */
    IDLE,
    
    /** 下载中状态：任务正在进行下载 */
    DOWNLOADING,
    
    /** 暂停状态：任务被用户暂停 */
    PAUSED,
    
    /** 完成状态：任务下载成功完成 */
    COMPLETED,
    
    /** 失败状态：任务下载失败 */
    FAILED,
    
    /** 取消状态：任务被用户取消 */
    CANCELLED
}

/**
 * 下载进度信息数据类
 * 
 * 包含下载过程中的进度信息：
 * - 下载进度百分比
 * - 下载速度
 * - 已下载和总文件大小
 * 
 * @property progress 下载进度（0-100）
 * @property speed 下载速度（字节/秒）
 * @property downloadedBytes 已下载字节数
 * @property totalBytes 总文件大小（字节）
 */
@Serializable
data class DownloadProgress(
    /** 下载进度（0-100） */
    val progress: Int,
    
    /** 下载速度（字节/秒） */
    val speed: Long,
    
    /** 已下载字节数 */
    val downloadedBytes: Long,
    
    /** 总文件大小（字节） */
    val totalBytes: Long
)

/**
 * 下载任务信息数据类
 * 
 * 包含完整的下载任务信息：
 * - 任务基本信息
 * - 下载状态和进度
 * - 文件路径和错误信息
 * 
 * @property taskId 任务唯一标识符
 * @property videoInfo 视频信息
 * @property status 下载状态
 * @property progress 下载进度信息
 * @property filePath 下载文件路径
 * @property errorMessage 错误信息（如果有）
 * @property createdAt 任务创建时间戳
 * @property updatedAt 任务更新时间戳
 */
@Serializable
data class DownloadTaskInfo(
    /** 任务唯一标识符 */
    val taskId: String,
    
    /** 视频信息 */
    val videoInfo: VideoInfo,
    
    /** 下载状态 */
    val status: DownloadStatus,
    
    /** 下载进度信息 */
    val progress: DownloadProgress,
    
    /** 下载文件路径 */
    val filePath: String,
    
    /** 错误信息（如果有） */
    val errorMessage: String? = null,
    
    /** 任务创建时间戳 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 任务更新时间戳 */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 下载配置数据类
 * 
 * 包含下载器的配置参数：
 * - 下载目录路径
 * - 并发下载数量限制
 * - 网络超时设置
 * - 重试次数配置
 * 
 * @property downloadPath 下载目录路径
 * @property maxConcurrentDownloads 最大并发下载数
 * @property networkTimeoutMs 网络超时时间（毫秒）
 * @property maxRetryCount 最大重试次数
 * @property enableResumeDownload 是否启用断点续传
 */
@Serializable
data class DownloadConfig(
    /** 下载目录路径 */
    val downloadPath: String = "/storage/emulated/0/Download/VideoBox",
    
    /** 最大并发下载数 */
    val maxConcurrentDownloads: Int = 3,
    
    /** 网络超时时间（毫秒） */
    val networkTimeoutMs: Long = 30000,
    
    /** 最大重试次数 */
    val maxRetryCount: Int = 3,
    
    /** 是否启用断点续传 */
    val enableResumeDownload: Boolean = true
)

/**
 * 下载结果数据类
 * 
 * 包含下载操作的结果信息：
 * - 操作是否成功
 * - 结果数据或错误信息
 * - 操作时间戳
 * 
 * @property success 操作是否成功
 * @property data 结果数据（泛型）
 * @property errorMessage 错误信息（如果有）
 * @property timestamp 操作时间戳
 */
@Serializable
data class DownloadResult<T>(
    /** 操作是否成功 */
    val success: Boolean,
    
    /** 结果数据（泛型） */
    val data: T? = null,
    
    /** 错误信息（如果有） */
    val errorMessage: String? = null,
    
    /** 操作时间戳 */
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 创建成功结果
         * 
         * @param data 结果数据
         * @return 成功的下载结果
         */
        fun <T> success(data: T): DownloadResult<T> {
            return DownloadResult(
                success = true,
                data = data
            )
        }
        
        /**
         * 创建失败结果
         * 
         * @param errorMessage 错误信息
         * @return 失败的下载结果
         */
        fun <T> failure(errorMessage: String): DownloadResult<T> {
            return DownloadResult(
                success = false,
                errorMessage = errorMessage
            )
        }
    }
}