package com.nexus.core.media.processor

import android.content.Context
import com.nexus.core.media.processor.api.MediaProcessorApi
import com.nexus.core.media.processor.config.MediaProcessorConfig
import com.nexus.core.media.processor.exception.ConfigurationException
import com.nexus.core.media.processor.logging.LogManager
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.model.MediaInfo.MediaType
import com.nexus.core.media.processor.state.MediaProcessingState
import com.nexus.core.media.processor.state.ProcessingStatistics
import com.nexus.core.media.processor.permission.PermissionManager
import com.nexus.core.media.processor.service.MediaProcessorService
import com.nexus.core.media.processor.utils.MediaProcessorUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 媒体处理器模块
 * 提供统一的对外接口和模块管理功能
 */
object MediaProcessorModule {
    
    private const val TAG = "MediaProcessorModule"
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var moduleContext: Context? = null
    
    @Volatile
    private var moduleConfig: MediaProcessorConfig? = null
    
    @Volatile
    private var mediaProcessor: MediaProcessor? = null
    
    @Volatile
    private var permissionManager: PermissionManager? = null
    
    /**
     * 初始化模块
     * @param context Android上下文
     * @param config 配置信息
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        config: MediaProcessorConfig = MediaProcessorConfig.default()
    ) {
        synchronized(this) {
            if (isInitialized) {
                LogManager.getInstance(context).w(TAG, "模块已经初始化，忽略重复初始化")
                return
            }
            
            try {
                // 验证配置
                val configErrors = config.validate()
                if (configErrors.isNotEmpty()) {
                    throw ConfigurationException(
                        "配置验证失败: ${configErrors.joinToString(", ")}"
                    )
                }
                
                // 保存上下文和配置
                moduleContext = context.applicationContext
                moduleConfig = config
                
                // 初始化日志管理器
                val logManager = LogManager.getInstance(context)
                
                // 初始化权限管理器
                permissionManager = PermissionManager(moduleContext!!)
                
                // 初始化媒体处理器
                mediaProcessor = MediaProcessor.getInstance(moduleContext!!, config)
                
                isInitialized = true
                
                logManager.i(TAG, "媒体处理器模块初始化成功")
                logManager.d(TAG, "配置信息已加载")
                
            } catch (e: Exception) {
                LogManager.getInstance(context).e(TAG, "模块初始化失败", e)
                throw e
            }
        }
    }
    
    /**
     * 销毁模块
     */
    @JvmStatic
    fun destroy() {
        synchronized(this) {
            if (!isInitialized) {
                return
            }
            
            try {
                val logManager = LogManager.getInstance(moduleContext!!)
                logManager.i(TAG, "开始销毁媒体处理器模块")
                
                // 销毁媒体处理器
                MediaProcessor.destroy()
                mediaProcessor = null
                
                // 清理权限管理器
                permissionManager = null
                
                // 清理上下文和配置
                moduleContext = null
                moduleConfig = null
                
                isInitialized = false
                
                logManager.i(TAG, "媒体处理器模块销毁完成")
                
            } catch (e: Exception) {
                LogManager.getInstance(moduleContext!!).e(TAG, "模块销毁失败", e)
            }
        }
    }
    
    /**
     * 检查模块是否已初始化
     */
    @JvmStatic
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * 获取模块配置
     */
    @JvmStatic
    fun getConfig(): MediaProcessorConfig {
        checkInitialized()
        return moduleConfig!!
    }
    
    /**
     * 获取媒体处理器实例
     */
    @JvmStatic
    fun getMediaProcessor(): MediaProcessor {
        checkInitialized()
        return mediaProcessor!!
    }
    
    /**
     * 获取权限管理器实例
     */
    @JvmStatic
    fun getPermissionManager(): PermissionManager {
        checkInitialized()
        return permissionManager!!
    }
    
    /**
     * 获取模块上下文
     */
    @JvmStatic
    fun getContext(): Context {
        checkInitialized()
        return moduleContext!!
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 获取媒体信息
     */
    @JvmStatic
    suspend fun getMediaInfo(url: String): Result<MediaInfo> {
        return getMediaProcessor().getMediaInfo(url)
    }
    
    /**
     * 添加处理任务
     */
    @JvmStatic
    suspend fun addTask(
        url: String,
        options: ProcessingOptions = ProcessingOptions()
    ): Result<String> {
        return getMediaProcessor().addTask(url, options)
    }
    
    /**
     * 暂停任务
     */
    @JvmStatic
    suspend fun pauseTask(taskId: String): Result<Unit> {
        return getMediaProcessor().pauseTask(taskId)
    }
    
    /**
     * 恢复任务
     */
    @JvmStatic
    suspend fun resumeTask(taskId: String): Result<Unit> {
        return getMediaProcessor().resumeTask(taskId)
    }
    
    /**
     * 取消任务
     */
    @JvmStatic
    suspend fun cancelTask(taskId: String): Result<Unit> {
        return getMediaProcessor().cancelTask(taskId)
    }
    
    /**
     * 删除任务
     */
    @JvmStatic
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return getMediaProcessor().deleteTask(taskId)
    }
    
    /**
     * 获取任务信息
     */
    @JvmStatic
    suspend fun getTask(taskId: String): ProcessingTask? {
        return getMediaProcessor().getTask(taskId)
    }
    
    /**
     * 获取所有任务
     */
    @JvmStatic
    suspend fun getAllTasks(): List<ProcessingTask> {
        return getMediaProcessor().getAllTasks()
    }
    
    /**
     * 获取指定状态的任务
     */
    @JvmStatic
    suspend fun getTasksByStatus(status: ProcessingStatus): List<ProcessingTask> {
        return getMediaProcessor().getTasksByStatus(status)
    }
    
    /**
     * 获取处理状态流
     */
    @JvmStatic
    fun getStateFlow(): StateFlow<MediaProcessingState> {
        return getMediaProcessor().stateFlow
    }
    
    /**
     * 获取任务更新流
     */
    @JvmStatic
    fun getTaskUpdates(): Flow<ProcessingTask> {
        return getMediaProcessor().taskUpdates
    }
    
    /**
     * 添加进度监听器
     */
    @JvmStatic
    fun addProgressListener(key: String, listener: (ProcessingTask) -> Unit) {
        getMediaProcessor().addProgressListener(key, listener)
    }
    
    /**
     * 移除进度监听器
     */
    @JvmStatic
    fun removeProgressListener(key: String) {
        getMediaProcessor().removeProgressListener(key)
    }
    
    /**
     * 添加状态监听器
     */
    @JvmStatic
    fun addStateListener(key: String, listener: (MediaProcessingState) -> Unit) {
        getMediaProcessor().addStateListener(key, listener)
    }
    
    /**
     * 移除状态监听器
     */
    @JvmStatic
    fun removeStateListener(key: String) {
        getMediaProcessor().removeStateListener(key)
    }
    
    /**
     * 清理已完成的任务
     */
    @JvmStatic
    suspend fun cleanupCompletedTasks(): Result<Int> {
        return getMediaProcessor().cleanupCompletedTasks()
    }
    
    /**
     * 获取处理统计信息
     */
    @JvmStatic
    fun getStatistics(): ProcessingStatistics {
        return getMediaProcessor().getStatistics()
    }
    
    // ========== 服务相关方法 ==========
    
    /**
     * 启动后台服务
     */
    @JvmStatic
    fun startService() {
        checkInitialized()
        MediaProcessorService.startService(moduleContext!!)
    }
    
    /**
     * 停止后台服务
     */
    @JvmStatic
    fun stopService() {
        checkInitialized()
        MediaProcessorService.stopService(moduleContext!!)
    }
    
    /**
     * 通过服务开始处理媒体
     */
    @JvmStatic
    @JvmOverloads
    fun startProcessingWithService(
        mediaUrl: String,
        options: ProcessingOptions = ProcessingOptions()
    ) {
        checkInitialized()
        MediaProcessorService.startProcessing(moduleContext!!, mediaUrl, options)
    }
    
    // ========== 权限相关方法 ==========
    
    /**
     * 检查是否有所有必需权限
     */
    @JvmStatic
    fun hasAllPermissions(): Boolean {
        return getPermissionManager().hasAllPermissions()
    }
    
    /**
     * 获取缺失的权限
     */
    @JvmStatic
    fun getMissingPermissions(): List<String> {
        return getPermissionManager().getMissingPermissions()
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 验证URL是否有效
     */
    @JvmStatic
    fun isValidUrl(url: String?): Boolean {
        return MediaProcessorUtils.isValidUrl(url)
    }
    
    /**
     * 检测媒体类型
     */
    @JvmStatic
    @JvmOverloads
    fun detectMediaType(url: String, mimeType: String? = null): MediaInfo.MediaType {
        return MediaProcessorUtils.detectMediaType(url, mimeType)
    }
    
    /**
     * 格式化文件大小
     */
    @JvmStatic
    fun formatFileSize(bytes: Long): String {
        return MediaProcessorUtils.formatFileSize(bytes)
    }
    
    /**
     * 格式化时长
     */
    @JvmStatic
    fun formatDuration(seconds: Long): String {
        return MediaProcessorUtils.formatDuration(seconds)
    }
    
    /**
     * 检查网络连接
     */
    @JvmStatic
    fun isNetworkAvailable(): Boolean {
        checkInitialized()
        return MediaProcessorUtils.isNetworkAvailable(moduleContext!!)
    }
    
    /**
     * 获取网络类型
     */
    @JvmStatic
    fun getNetworkType(): NetworkType {
        checkInitialized()
        return MediaProcessorUtils.getNetworkType(moduleContext!!)
    }
    
    /**
     * 获取系统信息
     */
    @JvmStatic
    fun getSystemInfo(): Map<String, String> {
        return MediaProcessorUtils.getSystemInfo()
    }
    
    /**
     * 获取模块版本信息
     */
    @JvmStatic
    fun getVersionInfo(): Map<String, String> {
        return mapOf(
            "模块名称" to "MediaProcessor",
            "版本号" to "1.0.0",
            "构建时间" to "2024-01-01",
            "API级别" to "1"
        )
    }
    
    /**
     * 获取模块状态信息
     */
    @JvmStatic
    suspend fun getModuleStatus(): Map<String, Any> {
        return mapOf(
            "已初始化" to isInitialized,
            "配置有效" to (moduleConfig?.isValid() ?: false),
            "网络可用" to if (isInitialized) isNetworkAvailable() else false,
            "权限完整" to if (isInitialized) hasAllPermissions() else false,
            "活跃任务数" to if (isInitialized) getAllTasks().count { it.isRunning() } else 0,
            "总任务数" to if (isInitialized) getAllTasks().size else 0
        )
    }
    
    /**
     * 检查模块是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException(
                "MediaProcessorModule 尚未初始化，请先调用 initialize() 方法"
            )
        }
    }
    
    // ========== 配置构建器 ==========
    
    /**
     * 配置构建器
     */
    class ConfigBuilder {
        private var config = MediaProcessorConfig.default()
        
        fun maxConcurrentTasks(count: Int) = apply {
            config = config.copy(maxConcurrentTasks = count)
        }
        
        fun maxRetryCount(count: Int) = apply {
            config = config.copy(maxRetryCount = count)
        }
        
        fun taskTimeout(timeoutMs: Long) = apply {
            config = config.copy(taskTimeoutMs = timeoutMs)
        }
        
        fun enableHardwareAcceleration(enable: Boolean) = apply {
            config = config.copy(enableHardwareAcceleration = enable)
        }
        
        fun maxMemoryUsage(memoryMB: Int) = apply {
            config = config.copy(maxMemoryUsageMB = memoryMB)
        }
        
        fun enableDebugMode(enable: Boolean) = apply {
            config = config.copy(enableDebugMode = enable)
        }
        
        fun build(): MediaProcessorConfig = config
    }
    
    /**
     * 创建配置构建器
     */
    @JvmStatic
    fun configBuilder(): ConfigBuilder = ConfigBuilder()
    
    /**
     * 使用构建器初始化模块
     */
    @JvmStatic
    fun initializeWithBuilder(
        context: Context,
        builderAction: ConfigBuilder.() -> Unit
    ) {
        val config = configBuilder().apply(builderAction).build()
        initialize(context, config)
    }
}