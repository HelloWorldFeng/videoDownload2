package com.app.videobox.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app.videobox.unLockBtn
import com.app.videobox.utils.NotifyHelper
import com.blankj.utilcode.util.SPStaticUtils

class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 用户唤醒屏幕时执行所需操作
        // 例如：显示通知、执行某些任务等
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("ScreenOnReceiver", "ACTION_SCREEN_ON: ")
            if (SPStaticUtils.getBoolean(unLockBtn,true)) {
                NotifyHelper.sendApiNotification(context,"unlock")
            }
        }

    }
}