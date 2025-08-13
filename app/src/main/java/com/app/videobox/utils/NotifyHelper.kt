package com.app.videobox.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.app.videobox.App
import com.app.videobox.BuildConfig
import com.app.videobox.NOTIFY_COUNT
import com.app.videobox.NOTIFY_LIMIT
import com.app.videobox.NOTIFY_TIME
import com.app.videobox.NOTIFY_TYPE
import com.app.videobox.NOTIFY_TYPE_CUSTOM
import com.app.videobox.NOTIFY_TYPE_DOWNLOAD
import com.app.videobox.R
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.network.DataRepository
import com.app.videobox.ui.SplashActivity
import com.blankj.utilcode.util.SPStaticUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotifyHelper {

    private val TAG = "NotifyHelper"

    //控制通知条数
    var notificationIdList = listOf<Int>(1,2)
    fun createNotificationIdList(size: Int) {
        notificationIdList =  (1..size).toList()
    }

    // 通知管理器
    private lateinit var notificationManager: NotificationManager

    // 通知渠道常量
    const val CHANNEL_ID_HIGH = "channel_high_priority"
    const val CHANNEL_NAME_HIGH = "VideoNewsNotification"
    const val CHANNEL_ID_FOREGROUND = "channel_foreground_service"
    const val CHANNEL_NAME_FOREGROUND = "ForegroundNotification"

    /**
     * 初始化通知系统
     * 兼容不同 Android 版本的通知功能
     */
    fun initNotify(context: Context) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (API 26) 及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
    }

    /**
     * 创建所有通知渠道
     * 仅在 Android 8.0+ 调用
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            // 高优先级渠道 - 用于重要通知如下载完成、错误提醒等
            NotificationChannel(
                CHANNEL_ID_HIGH,
                CHANNEL_NAME_HIGH,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important notice"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },

            // 前台服务渠道 - 用于前台服务常驻通知
            NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                CHANNEL_NAME_FOREGROUND,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "ForegroundNotification"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
        )

        // 批量创建通知渠道
        notificationManager.createNotificationChannels(channels)
    }


    /**
     * 创建前台服务通知
     * @param context 上下文
     * @param title 标题
     * @param content 内容
     * @param notificationId 通知ID，默认为1000
     * @param pendingIntent 点击意图，可选
     * @return 通知对象，用于 startForeground
     */
    fun createForegroundNotification(
        context: Context,
        title: String,
        content: String,
        notificationId: Int = 1000,
        pendingIntent: PendingIntent? = null
    ): android.app.Notification {
        // 检查通知权限
        if (!checkNotificationPermission(context)) {
            // 即使没有权限也要创建通知，因为前台服务必须要有通知
            Log.w(TAG, "Notification permission not granted, but creating foreground notification anyway")
        }

        // 创建默认的 PendingIntent（如果没有提供）
        val finalPendingIntent = pendingIntent ?: createDefaultPendingIntent(context,notificationId)

        // 创建自定义视图
        val customView = createForegroundRemoteViews(context, title, content)
        val customBigView = createForegroundBigRemoteViews(context, title, content)

        // 构建前台服务通知
        return NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND).apply {
            // 设置自定义视图
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setCustomContentView(customView)
                setCustomBigContentView(customBigView)
            } else {
                setCustomContentView(customBigView)
                setCustomBigContentView(customBigView)
            }

            // 设置基本属性
            setSmallIcon(R.drawable.icon_notify_sv)
            setContentIntent(finalPendingIntent)
            setOngoing(true) // 前台服务通知通常不自动取消
            setAutoCancel(false)
            setPriority(NotificationCompat.PRIORITY_DEFAULT)

            // 设置分组和时间戳
            setGroup("Foreground_Service_${System.currentTimeMillis()}")
            setWhen(System.currentTimeMillis())

            // 前台服务通知的特殊设置
            setCategory(NotificationCompat.CATEGORY_SERVICE)
        }.build()
    }

    /**
     * 创建前台服务小视图
     */
    private fun createForegroundRemoteViews(
        context: Context,
        title: String,
        content: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.layout_notify_for_small).apply {
            // 使用布局文件的默认内容，不设置文本
        }
    }

    /**
     * 创建前台服务大视图
     */
    private fun createForegroundBigRemoteViews(
        context: Context,
        title: String,
        content: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.layout_notify_for).apply {
            // 使用布局文件的默认内容，不设置文本
        }
    }



    /**
     * 创建默认的 PendingIntent
     */
    private fun createDefaultPendingIntent(context: Context, notificationId: Int): PendingIntent {
        val intent = Intent(context, SplashActivity::class.java).apply {
            putExtra(NOTIFY_TYPE, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /**
     * 检查通知权限
     * 兼容 Android 13+ 的通知权限
     */
    fun checkNotificationPermission(context: Context): Boolean {
        return when {
            // Android 13+ (API 33) 需要 POST_NOTIFICATIONS 权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }
            // Android 8.0-12 通过系统设置检查
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                notificationManager.areNotificationsEnabled()
            }
            // Android 8.0 以下版本默认支持通知
            else -> true
        }
    }



    /**
     * 清除指定通知
     */
    fun clearNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }




    fun sendDownloadNotify(context: Context){
        EventReportUtils.reportTDParams(
            "push_request_scene",
            mutableMapOf(
                "push_scene" to "download",
            ),
            desc = "触发通知的场景（请求）：download}")


        // 检查通知权限
        if (!checkNotificationPermission(context)) {
            EventReportUtils.reportTDParams(
                "push_request_fail",
                mutableMapOf(
                    "push_scene" to "download",
                    "err_reason" to "no_permission"
                ),
                desc = "通知发送失败：download 没有权限")
            return
        }
        if (getLimit("download")) {
            EventReportUtils.reportTDParams(
                "push_request_fail",
                mutableMapOf(
                    "push_scene" to "download",
                    "err_reason" to "limit"
                ),
                desc = "通知发送失败：download 触发了限制条件")
            return
        }

        val customBigView = RemoteViews(context.packageName, R.layout.layout_notify_download)
        val customView = RemoteViews(context.packageName, R.layout.layout_notify_download_s)
        val notificationId = 1111
        val intent = Intent(context, SplashActivity::class.java).apply {
            putExtra(NOTIFY_TYPE, NOTIFY_TYPE_DOWNLOAD)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val finalPendingIntent = PendingIntent.getActivity(context, NOTIFY_TYPE_DOWNLOAD, intent, flags)
        // 先清除上一条相同 ID 的通知
        clearNotification(notificationId)
        //记录通知发送的时间
        SPStaticUtils.put(NOTIFY_TIME, System.currentTimeMillis())
        //记录通知的次数
        SPStaticUtils.put(NOTIFY_COUNT, (SPStaticUtils.getInt(NOTIFY_COUNT,0) + 1))

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HIGH).apply {
            // 设置自定义视图
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setCustomContentView(customView)
                setCustomBigContentView(customBigView)
            } else {
                setCustomContentView(customBigView)
                setCustomBigContentView(customBigView)
            }

            // 设置基本属性
            setSmallIcon(R.drawable.icon_notify_sv)
            setAutoCancel(true)
            setOngoing(false)
            setContentIntent(finalPendingIntent)
            setPriority(NotificationCompat.PRIORITY_HIGH) // Android 8.0 之前有效

            // 设置分组和时间戳
            setGroup("Custom_Notification_${System.currentTimeMillis()}")
            setWhen(System.currentTimeMillis())
        }.build()

        // 发送通知
        notificationManager.notify(notificationId, notification)

        EventReportUtils.reportTDParams("push_request", mutableMapOf<String, Any>().apply {
            put("push_scene", "download")
        })
    }

    fun sendApiNotification(context: Context, scene: String){
        EventReportUtils.reportTDParams("push_request_scene", mutableMapOf<String, Any>().apply {
            put("push_scene", scene)

        }, desc = "触发通知的场景:${scene}")



        App.coroutineScope.launch {
            try {
                //获取接口通知内容
                val result = DataRepository.GetSceneEventPushMessageData()
                result?.let {
                    val notificationCount = (SPStaticUtils.getInt(NOTIFY_COUNT,0) + 1) % RemoteConfigManager.groupNotify
                    val notificationId = notificationIdList[notificationCount]

                    val imgUrl = it.model.imageUrl
                    val videoUrl = it.model.videoUrl
                    val title = it.model.title
                    val body = it.model.content
                    val pageType = it.model.pageType
                    //发送通知
                    sendContentNotification(
                        context = context,
                        notificationId = notificationId,
                        title = title,
                        content =  body,
                        imageUrl = imgUrl,
                        videoUrl = videoUrl,
                        scene = scene,
                        pageType = pageType

                    )
                }

            }catch (e: Exception){
                e.printStackTrace()
                EventReportUtils.reportTDParams("push_request_fail", mutableMapOf<String, Any>().apply {
                    put("push_scene", scene)
                    put("err_reason","api_err:${e.message}")

                }, desc = "通知发送失败:${scene}")
            }
        }
    }


    fun sendContentNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        imageUrl: String,
        videoUrl: String,
        scene: String,
        pageType: Int
    ){
        EventReportUtils.reportTDParams(
            "push_request_scene",
            mutableMapOf(
                "push_scene" to scene,
            ),
            desc = "触发通知的场景（请求）：${scene}")

        // 使用自定义 PendingIntent
        val customIntent = Intent(context, SplashActivity::class.java).apply {
            putExtra(NOTIFY_TYPE, NOTIFY_TYPE_CUSTOM)
            putExtra("notificationId", notificationId)
            putExtra("scene",scene)
            putExtra("videoTitle",title)
            putExtra("videoUrl",videoUrl)
            putExtra("imageUrl",imageUrl)
            putExtra("pageType",pageType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val customPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFY_TYPE_CUSTOM,
            customIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        sendHighPriorityCustomNotification(
            context = context,
            title = title,
            content = content,
            imageUrl = imageUrl,
            notificationId = notificationId,
            pendingIntent = customPendingIntent,
            scene = scene,
            pageType = pageType,
        )
    }


    fun sendHighPriorityCustomNotification(
        context: Context,
        title: String,
        content: String,
        imageUrl: String,
        notificationId: Int = 1001,
        pendingIntent: PendingIntent? = null,
        scene: String,
        pageType: Int
    ) {
        // 检查通知权限
        if (!checkNotificationPermission(context)) {
            EventReportUtils.reportTDParams(
                "push_request_fail",
                mutableMapOf(
                    "push_scene" to scene,
                    "err_reason" to "no_permission"
                ),
                desc = "通知发送失败：${scene} 没有权限")
            return
        }
        if (getLimit(scene)) {
            EventReportUtils.reportTDParams(
                "push_request_fail",
                mutableMapOf(
                    "push_scene" to scene,
                    "err_reason" to "limit"
                ),
                desc = "通知发送失败：${scene} 触发了限制条件")
            return
        }

        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        App.coroutineScope.launch(Dispatchers.IO) {
            val bitmap = imageLoader.execute(request).drawable?.toBitmap()
            if (bitmap == null) {
                EventReportUtils.reportTDParams(
                    "push_request_fail",
                    mutableMapOf(
                        "push_scene" to scene,
                        "err_reason" to "no_img_data"
                    ),
                    desc = "通知发送失败 $scene  加载图片数据失败")
            }
            // 图片加载成功，创建自定义通知
            bitmap?.let {
                createCustomNotification(
                    context,
                    title,
                    content,
                    bitmap,
                    notificationId,
                    pendingIntent,
                )

                EventReportUtils.reportTDParams("push_request", mutableMapOf<String, Any>().apply {
                    put("push_scene", scene)
                    put("content", content)
                    put("url",imageUrl)
                    put("title",title)
                    if (pageType == 4){ put("pageType","webUrl") }else{ put("pageType","video") }

                }, desc = "发生内容通知:${scene} imgUrl:${imageUrl}")
            }

        }
    }


    private fun createCustomNotification(
        context: Context,
        title: String,
        content: String,
        bitmap: Bitmap?,
        notificationId: Int,
        pendingIntent: PendingIntent?,
    ) {
        try {
            // 先清除上一条相同 ID 的通知
            clearNotification(notificationId)
            //记录通知发送的时间
            SPStaticUtils.put(NOTIFY_TIME, System.currentTimeMillis())
            //记录通知的次数
            SPStaticUtils.put(NOTIFY_COUNT, (SPStaticUtils.getInt(NOTIFY_COUNT,0) + 1))

            // 创建自定义视图
            val customView = createCustomRemoteViews(context, title, content, bitmap)
            val customBigView = createCustomBigRemoteViews(context, title, content, bitmap)

            // 创建默认的 PendingIntent（如果没有提供）
            val finalPendingIntent = pendingIntent ?: createDefaultPendingIntent(
                context,
                notificationId
            )

            // 构建通知
            val notification = NotificationCompat.Builder(context,
                NotifyHelper.CHANNEL_ID_HIGH
            ).apply {
                // 设置自定义视图
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setCustomContentView(customView)
                    setCustomBigContentView(customBigView)
                } else {
                    setCustomContentView(customBigView)
                    setCustomBigContentView(customBigView)
                }

                // 设置基本属性
                setSmallIcon(R.drawable.icon_notify_sv)
                setAutoCancel(true)
                setOngoing(false)
                setContentIntent(finalPendingIntent)
                setPriority(NotificationCompat.PRIORITY_HIGH) // Android 8.0 之前有效
//                setFullScreenIntent(PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE), true)

                // 设置分组和时间戳
                setGroup("Custom_Notification_${System.currentTimeMillis()}")
                setWhen(System.currentTimeMillis())
            }.build()

            // 发送通知
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * 创建自定义小视图
     */
    private fun createCustomRemoteViews(
        context: Context,
        title: String,
        content: String,
        bitmap: Bitmap?
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.layout_notify_custom).apply {
            setTextViewText(R.id.title_tv, title)
            setTextViewText(R.id.content_tv, content)

            // 设置图片（如果有）
            bitmap?.let {
                setImageViewBitmap(R.id.logo_iv, it)
            }
        }
    }

    /**
     * 创建自定义大视图
     */
    private fun createCustomBigRemoteViews(
        context: Context,
        title: String,
        content: String,
        bitmap: Bitmap?
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.layout_notify_custom_big).apply {
            setTextViewText(R.id.title_tv, title)
            setTextViewText(R.id.content_tv, content)

            // 设置图片（如果有）
            bitmap?.let {
                setImageViewBitmap(R.id.img_iv, it)
            }
        }
    }


    fun getLimit(scene: String): Boolean {

        if (BuildConfig.DEBUG) {
            return false
        }

        if (SPStaticUtils.getInt(NOTIFY_COUNT, 0) >= RemoteConfigManager.notifyCount) {
            //通知多少条限制
            EventReportUtils.reportTDParams(NOTIFY_LIMIT, mutableMapOf("scene" to "notifyCount"))
            return true
        }

        val notifyTime = SPStaticUtils.getLong(NOTIFY_TIME,0L)

        //5分钟间隔
        if (notifyTime != 0L && (System.currentTimeMillis() - notifyTime <= RemoteConfigManager.limitTime)) {
            EventReportUtils.reportTDParams(NOTIFY_LIMIT, mutableMapOf("scene" to scene))
            return true
        }
        return false
    }

    /**
     * 跳转到系统通知设置页面
     * 兼容不同 Android 版本的通知设置页面
     * @param context 上下文
     * @return 是否成功跳转
     */
    fun openNotificationSettings(context: Context): Boolean {
        return try {
            val intent = when {
                // Android 8.0+ (API 26) - 跳转到应用通知设置页面
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                    }
                }
                // Android 5.0-7.1 (API 21-25) - 跳转到应用详情页面
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                }
                // Android 5.0 以下 - 跳转到应用管理页面
                else -> {
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                }
            }

            // 添加标志以在新任务中启动
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 检查是否有可以处理此 Intent 的应用
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // 如果无法打开应用通知设置，尝试打开通用设置页面
                openGeneralSettings(context)
            }
        } catch (e: Exception) {
            // 发生异常时尝试打开通用设置页面
            openGeneralSettings(context)
        }
    }

    /**
     * 打开通用设置页面作为备用方案
     * @param context 上下文
     * @return 是否成功跳转
     */
    private fun openGeneralSettings(context: Context): Boolean {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

}