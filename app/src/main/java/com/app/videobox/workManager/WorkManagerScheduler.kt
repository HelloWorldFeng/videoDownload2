package com.app.videobox.workManager

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * WorkManager任务调度器
 * 
 * 功能说明：
 * - 统一管理应用中的所有后台任务
 * - 提供生产环境级别的任务调度和监控
 * - 支持任务的启动、停止、状态查询等操作
 * - 确保任务在各种系统状态下的稳定执行
 * 
 * 设计原则：
 * - 单例模式，确保全局唯一的任务管理器
 * - 线程安全，支持多线程环境下的并发操作
 * - 容错处理，应对各种异常情况
 * - 性能优化，最小化资源消耗
 * 
 * @author VideoBox Team
 * @since 1.0.5
 */
class WorkManagerScheduler private constructor() {

    companion object {
        private const val TAG = "WorkManagerScheduler"
        
        // 任务调度相关常量
        private const val PERIODIC_TASK_INTERVAL_MINUTES = 30L
        private const val INITIAL_DELAY_MINUTES = 5L
        private const val FLEX_INTERVAL_MINUTES = 10L
        
        // 网络和电池约束配置
        private const val REQUIRE_NETWORK = true
        private const val REQUIRE_CHARGING = false
        private const val REQUIRE_DEVICE_IDLE = false
        
        @Volatile
        private var INSTANCE: WorkManagerScheduler? = null
        
        /**
         * 获取WorkManagerScheduler单例实例
         * 
         * 使用双重检查锁定模式确保线程安全
         * 
         * @return WorkManagerScheduler实例
         */
        fun getInstance(): WorkManagerScheduler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WorkManagerScheduler().also { INSTANCE = it }
            }
        }
    }

    /**
     * 初始化WorkManager调度器
     * 
     * 配置WorkManager的全局设置
     * 应在Application.onCreate()中调用
     * 
     * @param context 应用上下文
     */
    fun initialize(context: Context) {
        try {
            Log.i(TAG, "初始化WorkManager调度器")
            
            // 配置WorkManager
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                .build()
            
            WorkManager.initialize(context, config)
            
            Log.i(TAG, "WorkManager调度器初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager调度器初始化失败", e)
        }
    }

    /**
     * 启动周期性任务
     * 
     * 创建并启动30分钟间隔的周期性任务
     * 包含完整的约束条件和错误处理
     * 
     * @param context 应用上下文
     * @param taskType 任务类型
     */
    fun startPeriodicTask(context: Context, taskType: String = PeriodicTaskWorker.TASK_TYPE_STATUS_CHECK) {
        try {
            Log.i(TAG, "启动周期性任务: $taskType")
            
            // 创建任务输入数据（简化版本，不需要复杂参数）
            val inputData = Data.Builder()
                .putString("task_type", taskType)
                .build()
            
            // 配置任务约束条件
            val constraints = buildTaskConstraints()
            
            // 创建周期性工作请求
            val periodicWorkRequest = PeriodicWorkRequestBuilder<PeriodicTaskWorker>(
                PERIODIC_TASK_INTERVAL_MINUTES, TimeUnit.MINUTES,
                FLEX_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .setInitialDelay(INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .addTag(taskType)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // 启动任务（使用KEEP策略避免重复任务）
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PeriodicTaskWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            
            Log.i(TAG, "周期性任务启动成功: $taskType")
            
            // 监控任务状态
            observeWorkStatus(context, PeriodicTaskWorker.WORK_NAME)
            
        } catch (e: Exception) {
            Log.e(TAG, "启动周期性任务失败: $taskType", e)
        }
    }

    /**
     * 停止周期性任务
     * 
     * 取消指定的周期性任务
     * 
     * @param context 应用上下文
     */
    fun stopPeriodicTask(context: Context) {
        try {
            Log.i(TAG, "停止周期性任务")
            
            WorkManager.getInstance(context).cancelUniqueWork(PeriodicTaskWorker.WORK_NAME)
            
            Log.i(TAG, "周期性任务已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止周期性任务失败", e)
        }
    }

    /**
     * 立即执行一次性任务
     * 
     * 创建并执行一次性的后台任务
     * 用于需要立即执行的操作
     * 
     * @param context 应用上下文
     * @param taskType 任务类型
     */
    fun executeOneTimeTask(context: Context, taskType: String) {
        try {
            Log.i(TAG, "执行一次性任务: $taskType")
            
            // 创建任务输入数据（简化版本，不需要复杂参数）
            val inputData = Data.Builder()
                .putString("task_type", taskType)
                .build()
            
            // 配置任务约束条件（一次性任务约束较宽松）
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // 创建一次性工作请求
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PeriodicTaskWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(taskType)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // 执行任务
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
            
            Log.i(TAG, "一次性任务已提交: $taskType")
            
        } catch (e: Exception) {
            Log.e(TAG, "执行一次性任务失败: $taskType", e)
        }
    }

    /**
     * 获取任务执行状态
     * 
     * 查询指定任务的当前执行状态
     * 
     * @param context 应用上下文
     * @param workName 任务名称
     * @return WorkInfo.State 任务状态
     */
    fun getWorkStatus(context: Context, workName: String): WorkInfo.State? {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(workName)
                .get()
            
            if (workInfos.isNotEmpty()) {
                val status = workInfos[0].state
                Log.d(TAG, "任务状态查询: $workName -> $status")
                status
            } else {
                Log.d(TAG, "未找到任务: $workName")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "查询任务状态失败: $workName", e)
            null
        }
    }

    /**
     * 取消所有任务
     * 
     * 取消应用中的所有WorkManager任务
     * 通常在应用退出或重置时使用
     * 
     * @param context 应用上下文
     */
    fun cancelAllTasks(context: Context) {
        try {
            Log.i(TAG, "取消所有任务")
            
            WorkManager.getInstance(context).cancelAllWork()
            
            Log.i(TAG, "所有任务已取消")
        } catch (e: Exception) {
            Log.e(TAG, "取消所有任务失败", e)
        }
    }

    /**
     * 构建任务约束条件
     * 
     * 根据生产环境需求配置任务执行约束
     * 确保任务在合适的条件下执行
     * 
     * @return Constraints 约束条件
     */
    private fun buildTaskConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(
                if (REQUIRE_NETWORK) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED
            )
            .setRequiresCharging(REQUIRE_CHARGING)
            .setRequiresDeviceIdle(REQUIRE_DEVICE_IDLE)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }

    /**
     * 监控任务执行状态
     * 
     * 实时监控任务的执行状态变化
     * 用于生产环境的任务监控和问题排查
     * 
     * @param context 应用上下文
     * @param workName 任务名称
     */
    private fun observeWorkStatus(context: Context, workName: String) {
        try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(workName)
                .observeForever { workInfos ->
                    if (workInfos.isNotEmpty()) {
                        val workInfo = workInfos[0]
                        Log.d(TAG, "任务状态变化: $workName -> ${workInfo.state}")
                        
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> {
                                Log.i(TAG, "任务执行成功: $workName")
                            }
                            WorkInfo.State.FAILED -> {
                                Log.w(TAG, "任务执行失败: $workName")
                            }
                            WorkInfo.State.CANCELLED -> {
                                Log.i(TAG, "任务已取消: $workName")
                            }
                            WorkInfo.State.RUNNING -> {
                                Log.d(TAG, "任务正在执行: $workName")
                            }
                            WorkInfo.State.ENQUEUED -> {
                                Log.d(TAG, "任务已入队: $workName")
                            }
                            WorkInfo.State.BLOCKED -> {
                                Log.d(TAG, "任务被阻塞: $workName")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "监控任务状态失败: $workName", e)
        }
    }

    /**
     * 获取所有任务的执行统计信息
     * 
     * 用于生产环境的性能监控和分析
     * 
     * @param context 应用上下文
     * @return Map<String, Any> 统计信息
     */
    fun getTaskStatistics(context: Context): Map<String, Any> {
        return try {
            val workManager = WorkManager.getInstance(context)
            val allWorkInfos = workManager.getWorkInfosByTag(PeriodicTaskWorker.TASK_TYPE_STATUS_CHECK).get()
            
            val statistics = mutableMapOf<String, Any>()
            statistics["total_tasks"] = allWorkInfos.size
            statistics["running_tasks"] = allWorkInfos.count { it.state == WorkInfo.State.RUNNING }
            statistics["succeeded_tasks"] = allWorkInfos.count { it.state == WorkInfo.State.SUCCEEDED }
            statistics["failed_tasks"] = allWorkInfos.count { it.state == WorkInfo.State.FAILED }
            
            Log.d(TAG, "任务统计信息: $statistics")
            statistics
        } catch (e: Exception) {
            Log.e(TAG, "获取任务统计信息失败", e)
            emptyMap()
        }
    }
}