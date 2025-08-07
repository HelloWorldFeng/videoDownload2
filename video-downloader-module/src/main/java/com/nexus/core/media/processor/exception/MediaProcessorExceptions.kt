package com.nexus.core.media.processor.exception

import com.nexus.core.media.processor.model.ProcessingErrorType

/**
 * 媒体处理器异常基类
 */
abstract class MediaProcessorException(
    message: String,
    cause: Throwable? = null,
    val errorType: ProcessingErrorType = ProcessingErrorType.UNKNOWN
) : Exception(message, cause) {
    
    /**
     * 获取用户友好的错误消息
     */
    abstract fun getUserFriendlyMessage(): String
    
    /**
     * 是否可重试
     */
    open fun isRetryable(): Boolean = false
    
    /**
     * 获取建议的重试延迟（毫秒）
     */
    open fun getRetryDelay(): Long = 5000L
}

/**
 * 网络相关异常
 */
class NetworkException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.NETWORK_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        return when (statusCode) {
            404 -> "媒体资源不存在或已被删除"
            403 -> "没有权限访问该媒体资源"
            429 -> "请求过于频繁，请稍后重试"
            500, 502, 503, 504 -> "服务器暂时不可用，请稍后重试"
            else -> "网络连接失败，请检查网络设置"
        }
    }
    
    override fun isRetryable(): Boolean {
        return statusCode in listOf(429, 500, 502, 503, 504) || statusCode == null
    }
    
    override fun getRetryDelay(): Long {
        return when (statusCode) {
            429 -> 30000L // 30秒
            500, 502, 503, 504 -> 10000L // 10秒
            else -> 5000L // 5秒
        }
    }
}

/**
 * 媒体信息提取异常
 */
class MediaExtractionException(
    message: String,
    cause: Throwable? = null,
    val url: String? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.EXTRACTION_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        val msg = message ?: ""
        return when {
            msg.contains("unsupported", ignoreCase = true) -> "不支持的媒体格式或平台"
            msg.contains("private", ignoreCase = true) -> "该媒体为私有内容，无法访问"
            msg.contains("geo", ignoreCase = true) -> "该媒体在当前地区不可用"
            msg.contains("age", ignoreCase = true) -> "该媒体有年龄限制"
            else -> "无法获取媒体信息，请检查链接是否正确"
        }
    }
    
    override fun isRetryable(): Boolean {
        val msg = message ?: ""
        return !msg.contains("unsupported", ignoreCase = true) &&
               !msg.contains("private", ignoreCase = true) &&
               !msg.contains("geo", ignoreCase = true)
    }
}

/**
 * 下载异常
 */
class DownloadException(
    message: String,
    cause: Throwable? = null,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L
) : MediaProcessorException(message, cause, ProcessingErrorType.DOWNLOAD_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        val msg = message ?: ""
        return when {
            msg.contains("space", ignoreCase = true) -> "存储空间不足"
            msg.contains("permission", ignoreCase = true) -> "没有存储权限"
            msg.contains("timeout", ignoreCase = true) -> "下载超时，请检查网络连接"
            msg.contains("interrupted", ignoreCase = true) -> "下载被中断"
            else -> "下载失败，请重试"
        }
    }
    
    override fun isRetryable(): Boolean {
        val msg = message ?: ""
        return !msg.contains("space", ignoreCase = true) &&
               !msg.contains("permission", ignoreCase = true)
    }
    
    /**
     * 获取下载进度百分比
     */
    fun getProgressPercentage(): Int {
        return if (totalBytes > 0) {
            ((bytesDownloaded * 100) / totalBytes).toInt()
        } else {
            0
        }
    }
}

/**
 * 处理异常
 */
class ProcessingException(
    message: String,
    cause: Throwable? = null,
    val stage: String? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.PROCESSING_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        return when (stage) {
            "conversion" -> "媒体格式转换失败"
            "merge" -> "音视频合并失败"
            "extract" -> "音频提取失败"
            "thumbnail" -> "缩略图生成失败"
            else -> "媒体处理失败"
        }
    }
    
    override fun isRetryable(): Boolean = true
}

/**
 * 存储异常
 */
class StorageException(
    message: String,
    cause: Throwable? = null,
    val path: String? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.STORAGE_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        val msg = message ?: ""
        return when {
            msg.contains("space", ignoreCase = true) -> "存储空间不足"
            msg.contains("permission", ignoreCase = true) -> "没有存储权限"
            msg.contains("readonly", ignoreCase = true) -> "存储位置为只读"
            msg.contains("not found", ignoreCase = true) -> "存储路径不存在"
            else -> "存储操作失败"
        }
    }
    
    override fun isRetryable(): Boolean {
        val msg = message ?: ""
        return !msg.contains("space", ignoreCase = true) &&
               !msg.contains("permission", ignoreCase = true) &&
               !msg.contains("readonly", ignoreCase = true)
    }
}

/**
 * 配置异常
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null,
    val configKey: String? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.CONFIGURATION_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        return "配置错误: $message"
    }
    
    override fun isRetryable(): Boolean = false
}

/**
 * 权限异常
 */
class PermissionException(
    message: String,
    cause: Throwable? = null,
    val requiredPermissions: List<String> = emptyList()
) : MediaProcessorException(message, cause, ProcessingErrorType.PERMISSION_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        return if (requiredPermissions.isNotEmpty()) {
            "需要以下权限: ${requiredPermissions.joinToString(", ")}"
        } else {
            "权限不足: $message"
        }
    }
    
    override fun isRetryable(): Boolean = false
}

/**
 * 任务异常
 */
class TaskException(
    message: String,
    cause: Throwable? = null,
    val taskId: String? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.TASK_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        val msg = message ?: ""
        return when {
            msg.contains("not found", ignoreCase = true) -> "任务不存在"
            msg.contains("cancelled", ignoreCase = true) -> "任务已被取消"
            msg.contains("timeout", ignoreCase = true) -> "任务执行超时"
            msg.contains("duplicate", ignoreCase = true) -> "重复的任务"
            else -> "任务执行失败: $msg"
        }
    }
    
    override fun isRetryable(): Boolean {
        val msg = message ?: ""
        return !msg.contains("cancelled", ignoreCase = true) &&
               !msg.contains("duplicate", ignoreCase = true)
    }
}

/**
 * 安全异常
 */
class SecurityException(
    message: String,
    cause: Throwable? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.SECURITY_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        return "安全验证失败: $message"
    }
    
    override fun isRetryable(): Boolean = false
}

/**
 * 格式不支持异常
 */
class UnsupportedFormatException(
    message: String,
    cause: Throwable? = null,
    val format: String? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.UNSUPPORTED_FORMAT) {
    
    override fun getUserFriendlyMessage(): String {
        return if (format != null) {
            "不支持的格式: $format"
        } else {
            "不支持的媒体格式"
        }
    }
    
    override fun isRetryable(): Boolean = false
}

/**
 * 超时异常
 */
class TimeoutException(
    message: String,
    cause: Throwable? = null,
    val timeoutMs: Long = 0L
) : MediaProcessorException(message, cause, ProcessingErrorType.TIMEOUT_ERROR) {
    
    override fun getUserFriendlyMessage(): String {
        return if (timeoutMs > 0) {
            "操作超时 (${timeoutMs / 1000}秒)"
        } else {
            "操作超时"
        }
    }
    
    override fun isRetryable(): Boolean = true
    
    override fun getRetryDelay(): Long = timeoutMs / 2 // 使用一半的超时时间作为重试延迟
}

/**
 * 取消异常
 */
class CancellationException(
    message: String = "操作已被取消",
    cause: Throwable? = null
) : MediaProcessorException(message, cause, ProcessingErrorType.CANCELLED) {
    
    override fun getUserFriendlyMessage(): String {
        return "操作已被取消"
    }
    
    override fun isRetryable(): Boolean = false
}

/**
 * 异常工具类
 */
object ExceptionUtils {
    
    /**
     * 包装异常为媒体处理器异常
     */
    fun wrapException(throwable: Throwable, context: String = ""): MediaProcessorException {
        return when (throwable) {
            is MediaProcessorException -> throwable
            is java.net.SocketTimeoutException -> TimeoutException(
                "网络连接超时: $context",
                throwable
            )
            is java.net.UnknownHostException -> NetworkException(
                "无法解析主机: ${throwable.message}",
                throwable
            )
            is java.net.ConnectException -> NetworkException(
                "网络连接失败: ${throwable.message}",
                throwable
            )
            is java.io.IOException -> {
                if (throwable.message?.contains("space", ignoreCase = true) == true) {
                    StorageException("存储空间不足", throwable)
                } else {
                    StorageException("IO操作失败: ${throwable.message}", throwable)
                }
            }
            is SecurityException -> SecurityException(
                "安全检查失败: ${throwable.message}",
                throwable
            )
            is IllegalArgumentException -> ConfigurationException(
                "参数错误: ${throwable.message}",
                throwable
            )
            is InterruptedException -> CancellationException(
                "操作被中断",
                throwable
            )
            else -> object : MediaProcessorException(
                "未知错误: ${throwable.message ?: throwable.javaClass.simpleName}",
                throwable
            ) {
                override fun getUserFriendlyMessage(): String {
                    return "发生未知错误，请重试"
                }
                
                override fun isRetryable(): Boolean = true
            }
        }
    }
    
    /**
     * 获取异常的根本原因
     */
    fun getRootCause(throwable: Throwable): Throwable {
        var cause = throwable
        while (cause.cause != null && cause.cause != cause) {
            cause = cause.cause!!
        }
        return cause
    }
    
    /**
     * 获取异常堆栈信息
     */
    fun getStackTraceString(throwable: Throwable): String {
        return throwable.stackTraceToString()
    }
    
    /**
     * 判断异常是否为网络相关
     */
    fun isNetworkRelated(throwable: Throwable): Boolean {
        val rootCause = getRootCause(throwable)
        return rootCause is java.net.SocketException ||
               rootCause is java.net.UnknownHostException ||
               rootCause is java.net.ConnectException ||
               rootCause is java.net.SocketTimeoutException ||
               rootCause is NetworkException
    }
    
    /**
     * 判断异常是否为存储相关
     */
    fun isStorageRelated(throwable: Throwable): Boolean {
        val rootCause = getRootCause(throwable)
        return rootCause is java.io.IOException ||
               rootCause is StorageException ||
               throwable.message?.contains("space", ignoreCase = true) == true ||
               throwable.message?.contains("permission", ignoreCase = true) == true
    }
}