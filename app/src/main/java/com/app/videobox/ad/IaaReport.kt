package com.app.videobox.ad

import android.util.Log
import com.app.videobox.App
import com.appsflyer.AppsFlyerLib


fun afEventLog(eventName: String, params: Map<String, Any>){
    Log.d("AFLOG", "afEventLog:事件名${eventName} 事件值:${params} ")
    AppsFlyerLib.getInstance().logEvent(App.appContext(), eventName, params)
}