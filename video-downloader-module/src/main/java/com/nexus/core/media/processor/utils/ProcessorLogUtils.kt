package com.nexus.core.media.processor.utils

import android.util.Log

/**
 * 处理器日志工具类
 * 
 * 提供统一的日志输出接口，支持不同级别的日志记录。
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
object ProcessorLogUtils {
    
    private const val DEFAULT_TAG = "MediaProcessor"
    
    /**
     * 输出调试日志
     */
    @JvmStatic
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    /**
     * 输出信息日志
     */
    @JvmStatic
    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    /**
     * 输出警告日志
     */
    @JvmStatic
    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    /**
     * 输出错误日志
     */
    @JvmStatic
    fun e(tag: String, message: String) {
        Log.e(tag, message)
    }
    
    /**
     * 输出错误日志（带异常）
     */
    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }
    
    /**
     * 输出详细日志
     */
    @JvmStatic
    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }
    
    /**
     * 输出What a Terrible Failure日志
     */
    @JvmStatic
    fun wtf(tag: String, message: String) {
        Log.wtf(tag, message)
    }
    
    /**
     * 输出What a Terrible Failure日志（带异常）
     */
    @JvmStatic
    fun wtf(tag: String, message: String, throwable: Throwable) {
        Log.wtf(tag, message, throwable)
    }
    
    /**
     * 检查是否启用调试日志
     */
    @JvmStatic
    fun isDebugEnabled(): Boolean {
        return Log.isLoggable(DEFAULT_TAG, Log.DEBUG)
    }
    
    /**
     * 检查是否启用详细日志
     */
    @JvmStatic
    fun isVerboseEnabled(): Boolean {
        return Log.isLoggable(DEFAULT_TAG, Log.VERBOSE)
    }
}