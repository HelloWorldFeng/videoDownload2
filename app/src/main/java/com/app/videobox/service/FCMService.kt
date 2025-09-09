package com.app.videobox.service

import com.app.videobox.App
import com.app.videobox.NOTIFY_COUNT
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.network.DataRepository
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.NotifyHelper
import com.app.videobox.utils.NotifyHelper.notificationIdList
import com.blankj.utilcode.util.SPStaticUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FCMService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            if (message.data.isNotEmpty()) {
                // 处理接收到的消息
                val title = message.data["Title"] ?: ""
                val body = message.data["Body"] ?: ""
                val imgUrl = message.data["ImageUrl"] ?: ""
                val videoUrl = message.data["VideoUrl"] ?:""
                val pageType = message.data["pageType"]?.toInt() ?:0

                val notificationCount = (SPStaticUtils.getInt(NOTIFY_COUNT,0) + 1) % RemoteConfigManager.groupNotify
                val notificationId = notificationIdList[notificationCount]
                EventReportUtils.reportTDParams(
                    "push_request_scene",
                    mutableMapOf(
                        "push_scene" to "onMessageReceived",
                    ),
                    desc = "触发通知的场景（请求）：onMessageReceived")

                NotifyHelper.sendContentNotification(
                    context = this,
                    notificationId = notificationId,
                    title = title,
                    content = body,
                    imageUrl = imgUrl,
                    videoUrl = videoUrl,
                    scene = "onMessageReceived",
                    pageType = pageType
                )

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewToken(token: String) {
        App.firebaseToken = token
        App.coroutineScope.launch(Dispatchers.IO) {
            DataRepository.jbLike(this@FCMService,token)
        }
        super.onNewToken(token)
    }
}