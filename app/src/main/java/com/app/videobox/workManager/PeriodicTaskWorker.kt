package com.app.videobox.workManager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.videobox.utils.NotifyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 周期性任务Worker - 每30分钟执行一次的后台任务
 * 
 * 功能说明：
 * - 执行定期通知发送任务
 * - 使用WorkManager调度，确保任务可靠执行
 * 
 * @author VideoBox Team
 * @since 1.0.5
 */
class PeriodicTaskWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PeriodicTaskWorker"
        
        // 任务执行相关常量
        const val WORK_NAME = "periodic_task_work"
        const val TASK_TYPE_STATUS_CHECK = "status_check"
    }

    /**
     * 执行周期性任务的核心方法
     * 
     * 发送API通知，场景标识为"workManager"
     * 
     * @return Result 任务执行结果
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.i(TAG, "开始执行周期性任务 - 执行时间: ${System.currentTimeMillis()}")
            
            // 发送通知
            NotifyHelper.sendApiNotification(applicationContext, "workManager")
            
            Log.i(TAG, "周期性任务执行成功 - 已发送workManager通知")
            Result.success()
            
        } catch (exception: Exception) {
            Log.e(TAG, "周期性任务执行过程中发生异常", exception)
            Result.failure()
        }
    }
}