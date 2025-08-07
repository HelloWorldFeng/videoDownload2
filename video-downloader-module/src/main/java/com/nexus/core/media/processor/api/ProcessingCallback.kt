package com.nexus.core.media.processor.api

import com.nexus.core.media.processor.model.ProcessingTask
import com.nexus.core.media.processor.model.ProcessingStatus

/**
 * 媒体处理回调接口
 * 
 * 定义了媒体处理过程中的各种回调方法，
 * 用于通知处理状态的变化和结果。
 * 
 * 主要回调事件：
 * - 处理开始
 * - 处理完成
 * - 处理失败
 * - 处理取消
 * - 处理暂停
 * 
 * 使用示例：
 * ```kotlin
 * val callback = object : ProcessingCallback {
 *     override fun onProcessingStarted(task: ProcessingTask) {
 *         // 处理开始
 *     }
 *     
 *     override fun onProcessingCompleted(task: ProcessingTask) {
 *         // 处理完成
 *     }
 *     
 *     override fun onProcessingFailed(task: ProcessingTask, error: String) {
 *         // 处理失败
 *     }
 * }
 * 
 * api.registerProcessingCallback(callback)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
interface ProcessingCallback {
    
    /**
     * 处理开始回调
     * 
     * 当媒体处理任务开始执行时调用此方法。
     * 
     * @param task 处理任务信息
     */
    fun onProcessingStarted(task: ProcessingTask)
    
    /**
     * 处理完成回调
     * 
     * 当媒体处理任务成功完成时调用此方法。
     * 
     * @param task 处理任务信息
     */
    fun onProcessingCompleted(task: ProcessingTask)
    
    /**
     * 处理失败回调
     * 
     * 当媒体处理任务失败时调用此方法。
     * 
     * @param task 处理任务信息
     * @param error 错误信息
     */
    fun onProcessingFailed(task: ProcessingTask, error: String)
    
    /**
     * 处理取消回调
     * 
     * 当媒体处理任务被取消时调用此方法。
     * 
     * @param task 处理任务信息
     */
    fun onProcessingCancelled(task: ProcessingTask)
    
    /**
     * 处理暂停回调
     * 
     * 当媒体处理任务被暂停时调用此方法。
     * 
     * @param task 处理任务信息
     */
    fun onProcessingPaused(task: ProcessingTask)
}

/**
 * 媒体处理进度回调接口
 * 
 * 专门用于处理进度更新的回调接口，
 * 提供更详细的进度信息。
 * 
 * 使用示例：
 * ```kotlin
 * val progressCallback = object : ProcessingProgressCallback {
 *     override fun onProgress(task: ProcessingTask, percentage: Int, speed: String) {
 *         // 更新进度UI
 *         progressBar.progress = percentage
 *         speedText.text = speed
 *     }
 * }
 * 
 * api.registerProgressCallback(progressCallback)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
interface ProcessingProgressCallback {
    
    /**
     * 进度更新回调
     * 
     * 当处理进度发生变化时调用此方法。
     * 
     * @param task 处理任务信息
     * @param percentage 完成百分比 (0-100)
     * @param speed 处理速度描述
     */
    fun onProgress(task: ProcessingTask, percentage: Int, speed: String)
}

/**
 * 媒体处理状态回调接口
 * 
 * 专门用于处理状态变化的回调接口，
 * 提供更精确的状态控制。
 * 
 * 使用示例：
 * ```kotlin
 * val statusCallback = object : ProcessingStatusCallback {
 *     override fun onStatusChanged(task: ProcessingTask, newStatus: ProcessingStatus) {
 *         when (newStatus) {
 *             ProcessingStatus.PROCESSING -> showProcessingUI()
 *             ProcessingStatus.PAUSED -> showPausedUI()
 *             ProcessingStatus.COMPLETED -> showCompletedUI()
 *             else -> {}
 *         }
 *     }
 * }
 * 
 * api.registerStatusCallback(statusCallback)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
interface ProcessingStatusCallback {
    
    /**
     * 状态变化回调
     * 
     * 当处理任务状态发生变化时调用此方法。
     * 
     * @param task 处理任务信息
     * @param newStatus 新的状态
     */
    fun onStatusChanged(task: ProcessingTask, newStatus: ProcessingStatus)
}

/**
 * 简化的处理回调适配器
 * 
 * 提供默认实现，用户只需要重写需要的方法。
 * 
 * 使用示例：
 * ```kotlin
 * val callback = object : SimpleProcessingCallback() {
 *     override fun onProcessingCompleted(task: ProcessingTask) {
 *         // 只关心完成事件
 *     }
 * }
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
abstract class SimpleProcessingCallback : ProcessingCallback {
    
    override fun onProcessingStarted(task: ProcessingTask) {
        // 默认空实现
    }
    
    override fun onProcessingCompleted(task: ProcessingTask) {
        // 默认空实现
    }
    
    override fun onProcessingFailed(task: ProcessingTask, error: String) {
        // 默认空实现
    }
    
    override fun onProcessingCancelled(task: ProcessingTask) {
        // 默认空实现
    }
    
    override fun onProcessingPaused(task: ProcessingTask) {
        // 默认空实现
    }
}

/**
 * 简化的进度回调适配器
 * 
 * 提供默认实现，用户只需要重写需要的方法。
 * 
 * 使用示例：
 * ```kotlin
 * val progressCallback = object : SimpleProgressCallback() {
 *     override fun onProgress(task: ProcessingTask, percentage: Int, speed: String) {
 *         // 处理进度更新
 *     }
 * }
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
abstract class SimpleProgressCallback : ProcessingProgressCallback {
    
    override fun onProgress(task: ProcessingTask, percentage: Int, speed: String) {
        // 默认空实现
    }
}

/**
 * 简化的状态回调适配器
 * 
 * 提供默认实现，用户只需要重写需要的方法。
 * 
 * 使用示例：
 * ```kotlin
 * val statusCallback = object : SimpleStatusCallback() {
 *     override fun onStatusChanged(task: ProcessingTask, newStatus: ProcessingStatus) {
 *         // 处理状态变化
 *     }
 * }
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
abstract class SimpleStatusCallback : ProcessingStatusCallback {
    
    override fun onStatusChanged(task: ProcessingTask, newStatus: ProcessingStatus) {
        // 默认空实现
    }
}