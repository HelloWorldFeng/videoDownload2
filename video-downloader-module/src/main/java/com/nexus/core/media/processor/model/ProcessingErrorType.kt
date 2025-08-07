package com.nexus.core.media.processor.model

/**
 * 处理错误类型枚举
 * 
 * 定义媒体处理过程中可能出现的各种错误类型。
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
enum class ProcessingErrorType {
    /** 未知错误 */
    UNKNOWN,
    
    /** 网络错误 */
    NETWORK_ERROR,
    
    /** 媒体提取错误 */
    EXTRACTION_ERROR,
    
    /** 下载错误 */
    DOWNLOAD_ERROR,
    
    /** 处理错误 */
    PROCESSING_ERROR,
    
    /** 存储错误 */
    STORAGE_ERROR,
    
    /** 配置错误 */
    CONFIGURATION_ERROR,
    
    /** 权限错误 */
    PERMISSION_ERROR,
    
    /** 任务错误 */
    TASK_ERROR,
    
    /** 安全错误 */
    SECURITY_ERROR,
    
    /** 不支持的格式 */
    UNSUPPORTED_FORMAT,
    
    /** 超时错误 */
    TIMEOUT_ERROR,
    
    /** 操作已取消 */
    CANCELLED
}