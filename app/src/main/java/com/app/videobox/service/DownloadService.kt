package com.app.videobox.service

import android.app.Service
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.app.videobox.NOTIFY_TYPE_FOREGROUND
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.NotifyHelper
import com.blankj.utilcode.util.AppUtils

class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"

        // 服务动作常量
        const val ACTION_START_SERVICE = "action_start_service"
        const val ACTION_STOP_SERVICE = "action_stop_service"

        // 静态方法：启动服务
        fun startService(context: Context) {
            try {
                if (AppUtils.isAppForeground().not()) {
                    return
                }
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_START_SERVICE
                }
                context.startService(intent);
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 静态方法：停止服务
        fun stopService(context: Context) {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "停止服务失败: ${e.message}")
            }
        }

    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START_SERVICE -> {
                    startForegroundService()
                }
                ACTION_STOP_SERVICE -> {
                    stopSelf()
                }
                else -> {
                    stopSelf()
                    Log.w(TAG, "未知的服务动作: ${intent?.action}")
                }
            }
        }catch (e: Exception){

        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: ")
        return null
    }

    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        try {
            // 创建前台服务通知
            val notification = NotifyHelper.createForegroundNotification(
                context = this@DownloadService,
                title = "Video download service",
                content = "Download service is running in the background...",
                notificationId = NOTIFY_TYPE_FOREGROUND
            )

            // 启动前台服务
            startForeground(NOTIFY_TYPE_FOREGROUND, notification)

            EventReportUtils.reportTDParams("permanent_request", mutableMapOf<String, Any>().apply {
                put("is_permission", NotifyHelper.checkNotificationPermission(this@DownloadService))
            }, desc = "常驻通知栏展示")

        }catch (e: Exception){
            Log.d(TAG, "错误:${e.message} ")
        }

    }

}