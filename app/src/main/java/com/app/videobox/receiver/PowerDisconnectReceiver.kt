package com.app.videobox.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app.videobox.powerBtn
import com.app.videobox.utils.NotifyHelper
import com.blankj.utilcode.util.SPStaticUtils

class PowerDisconnectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
            if (SPStaticUtils.getBoolean(powerBtn,true)) {
                NotifyHelper.sendApiNotification(context,"powerBtn")
            }
        }
    }
}