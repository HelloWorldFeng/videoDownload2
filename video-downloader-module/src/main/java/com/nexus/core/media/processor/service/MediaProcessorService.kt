package com.nexus.core.media.processor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nexus.core.media.processor.MediaProcessor
import com.nexus.core.media.processor.config.MediaProcessorConfig
import com.nexus.core.media.processor.model.*
import com.nexus.core.media.processor.state.MediaProcessingState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.ConcurrentHashMap

/**
 * 媒体处理器服务
 * 提供后台媒体处理功能，支持前台服务和通知
 */
class MediaProcessorService : Service() {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "media_processor_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "媒体处理器"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_PROCESSING = "com.nexus.core.media.processor.START_PROCESSING"
        const val ACTION_PAUSE_PROCESSING = "com.nexus.core.media.processor.PAUSE_PROCESSING"
        const val ACTION_RESUME_PROCESSING = "com.nexus.core.media.processor.RESUME_PROCESSING"
        const val ACTION_CANCEL_PROCESSING = "com.nexus.core.media.processor.CANCEL_PROCESSING"
        
        const val EXTRA_MEDIA_URL = "media_url"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_PROCESSING_OPTIONS = "processing_options"
        
        /**
         * 启动服务
         */
        fun startService(context: Context) {
            val intent = Intent(context, MediaProcessorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, MediaProcessorService::class.java)
            context.stopService(intent)
        }
        
        /**
         * 开始处理媒体
         */
        fun startProcessing(
            context: Context,
            mediaUrl: String,
            options: ProcessingOptions = ProcessingOptions()
        ) {
            val intent = Intent(context, MediaProcessorService::class.java).apply {
                action = ACTION_START_PROCESSING
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
                putExtra(EXTRA_PROCESSING_OPTIONS, options)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 暂停处理
         */
        fun pauseProcessing(context: Context, taskId: String) {
            val intent = Intent(context, MediaProcessorService::class.java).apply {
                action = ACTION_PAUSE_PROCESSING
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
        
        /**
         * 恢复处理
         */
        fun resumeProcessing(context: Context, taskId: String) {
            val intent = Intent(context, MediaProcessorService::class.java).apply {
                action = ACTION_RESUME_PROCESSING
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
        
        /**
         * 取消处理
         */
        fun cancelProcessing(context: Context, taskId: String) {
            val intent = Intent(context, MediaProcessorService::class.java).apply {
                action = ACTION_CANCEL_PROCESSING
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
    }
    
    // 服务绑定器
    inner class MediaProcessorBinder : Binder() {
        fun getService(): MediaProcessorService = this@MediaProcessorService
    }
    
    private val binder = MediaProcessorBinder()
    private lateinit var mediaProcessor: MediaProcessor
    private lateinit var notificationManager: NotificationManager
    
    // 协程作用域
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineName("MediaProcessorService")
    )
    
    // 任务通知映射
    private val taskNotifications = ConcurrentHashMap<String, Int>()
    private var nextNotificationId = NOTIFICATION_ID + 1
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化通知管理器
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // 初始化媒体处理器
        val config = MediaProcessorConfig.default().copy(
            enableBackgroundProcessing = true
        )
        mediaProcessor = MediaProcessor.getInstance(this, config)
        
        // 监听任务状态变化
        serviceScope.launch {
            mediaProcessor.taskUpdates.collect { task ->
                updateTaskNotification(task)
            }
        }
        
        // 监听全局状态变化
        serviceScope.launch {
            mediaProcessor.stateFlow.collect { state ->
                updateServiceNotification(state)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_PROCESSING -> {
                val mediaUrl = intent.getStringExtra(EXTRA_MEDIA_URL)
                val options = intent.getParcelableExtra<ProcessingOptions>(EXTRA_PROCESSING_OPTIONS)
                    ?: ProcessingOptions()
                
                if (mediaUrl != null) {
                    startProcessing(mediaUrl, options)
                }
            }
            
            ACTION_PAUSE_PROCESSING -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    pauseProcessing(taskId)
                }
            }
            
            ACTION_RESUME_PROCESSING -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    resumeProcessing(taskId)
                }
            }
            
            ACTION_CANCEL_PROCESSING -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    cancelProcessing(taskId)
                }
            }
        }
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createServiceNotification())
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 取消协程作用域
        serviceScope.cancel()
        
        // 清理资源
        MediaProcessor.destroy()
    }
    
    /**
     * 获取媒体处理器实例
     */
    fun getMediaProcessor(): MediaProcessor {
        return mediaProcessor
    }
    
    /**
     * 开始处理媒体
     */
    private fun startProcessing(mediaUrl: String, options: ProcessingOptions) {
        serviceScope.launch {
            try {
                val taskId = mediaProcessor.addTask(mediaUrl, options)
                if (taskId.isSuccess) {
                    // 创建任务通知
                    val notificationId = nextNotificationId++
                    taskNotifications[taskId.getOrThrow()] = notificationId
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    /**
     * 暂停处理
     */
    private fun pauseProcessing(taskId: String) {
        serviceScope.launch {
            mediaProcessor.pauseTask(taskId)
        }
    }
    
    /**
     * 恢复处理
     */
    private fun resumeProcessing(taskId: String) {
        serviceScope.launch {
            mediaProcessor.resumeTask(taskId)
        }
    }
    
    /**
     * 取消处理
     */
    private fun cancelProcessing(taskId: String) {
        serviceScope.launch {
            mediaProcessor.cancelTask(taskId)
            
            // 移除任务通知
            taskNotifications[taskId]?.let { notificationId ->
                notificationManager.cancel(notificationId)
                taskNotifications.remove(taskId)
            }
        }
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "媒体处理器后台服务通知"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建服务通知
     */
    private fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("媒体处理器")
            .setContentText("后台服务运行中")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    /**
     * 更新服务通知
     */
    private fun updateServiceNotification(state: MediaProcessingState) {
        val runningTasks = state.getRunningTasks()
        val totalTasks = state.activeTasks.size + state.queuedTasks.size
        
        val contentText = when {
            runningTasks.isEmpty() -> "等待任务中"
            runningTasks.size == 1 -> "正在处理 1 个任务"
            else -> "正在处理 ${runningTasks.size} 个任务"
        }
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("媒体处理器")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 更新任务通知
     */
    private fun updateTaskNotification(task: ProcessingTask) {
        val notificationId = taskNotifications[task.taskId] ?: return
        
        val title = task.mediaInfo.title ?: "未知媒体"
        val contentText = when (task.status) {
            ProcessingStatus.PENDING -> "等待开始"
            ProcessingStatus.PREPARING -> "准备中"
            ProcessingStatus.PROCESSING -> "处理中 ${task.progressPercentage}%"
            ProcessingStatus.PAUSED -> "已暂停"
            ProcessingStatus.COMPLETED -> "已完成"
            ProcessingStatus.FAILED -> "处理失败"
            ProcessingStatus.CANCELLED -> "已取消"
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setAutoCancel(task.isFinished())
        
        // 添加进度条
        if (task.isRunning()) {
            notificationBuilder.setProgress(100, task.progressPercentage, false)
        }
        
        // 添加操作按钮
        when (task.status) {
            ProcessingStatus.PREPARING, ProcessingStatus.PROCESSING -> {
                // 暂停按钮
                val pauseIntent = Intent(this, MediaProcessorService::class.java).apply {
                    action = ACTION_PAUSE_PROCESSING
                    putExtra(EXTRA_TASK_ID, task.taskId)
                }
                val pausePendingIntent = PendingIntent.getService(
                    this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    android.R.drawable.ic_media_pause, "暂停", pausePendingIntent
                )
                
                // 取消按钮
                val cancelIntent = Intent(this, MediaProcessorService::class.java).apply {
                    action = ACTION_CANCEL_PROCESSING
                    putExtra(EXTRA_TASK_ID, task.taskId)
                }
                val cancelPendingIntent = PendingIntent.getService(
                    this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelPendingIntent
                )
            }
            
            ProcessingStatus.PAUSED -> {
                // 恢复按钮
                val resumeIntent = Intent(this, MediaProcessorService::class.java).apply {
                    action = ACTION_RESUME_PROCESSING
                    putExtra(EXTRA_TASK_ID, task.taskId)
                }
                val resumePendingIntent = PendingIntent.getService(
                    this, 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    android.R.drawable.ic_media_play, "恢复", resumePendingIntent
                )
            }
            
            else -> {
                // 其他状态不添加操作按钮
            }
        }
        
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        // 如果任务完成，延迟移除通知
        if (task.isFinished()) {
            serviceScope.launch {
                delay(5000) // 5秒后移除通知
                taskNotifications.remove(task.taskId)
            }
        }
    }
}