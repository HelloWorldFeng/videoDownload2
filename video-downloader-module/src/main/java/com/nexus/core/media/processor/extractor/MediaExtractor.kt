package com.nexus.core.media.processor.extractor

import android.content.Context
import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.ProcessingOptions
import com.nexus.core.media.processor.model.MediaInfo

/**
 * 媒体提取器基类
 * 定义媒体信息提取的通用接口
 */
abstract class MediaExtractor(
    protected val context: Context,
    protected val configuration: ProcessorConfiguration
) {
    
    /**
     * 检查是否支持指定的URL
     */
    abstract fun supports(url: String): Boolean
    
    /**
     * 提取媒体信息
     */
    abstract suspend fun extractMediaInfo(
        url: String,
        options: ProcessingOptions? = null
    ): ExtractionResult
    
    /**
     * 获取提取器名称
     */
    abstract fun getName(): String
    
    /**
     * 获取支持的域名列表
     */
    abstract fun getSupportedDomains(): List<String>
    
    /**
     * 清理资源
     */
    open fun cleanup() {
        // 默认实现，子类可以重写
    }
    
    /**
     * 提取结果
     */
    data class ExtractionResult(
        val success: Boolean,
        val mediaInfo: MediaInfo? = null,
        val error: String? = null,
        val errorCode: Int = 0
    )
    
    /**
     * 错误代码常量
     */
    companion object {
        const val ERROR_UNSUPPORTED_URL = 1001
        const val ERROR_NETWORK_FAILURE = 1002
        const val ERROR_PARSING_FAILURE = 1003
        const val ERROR_INVALID_RESPONSE = 1004
        const val ERROR_TIMEOUT = 1005
        const val ERROR_UNKNOWN = 9999
    }
}