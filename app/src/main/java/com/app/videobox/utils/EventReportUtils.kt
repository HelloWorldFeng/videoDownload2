package com.app.videobox.utils

import android.util.Log
import cn.thinkingdata.analytics.TDAnalytics
import com.app.videobox.App
import com.appsflyer.AppsFlyerLib
import org.json.JSONException
import org.json.JSONObject

object EventReportUtils {
    private const val AF_LOG = "AfLog"
    fun afEventLog(eventName: String, params: MutableMap<String, Any>){
        Log.d(AF_LOG, "afEventLog:事件名${eventName} 事件值:${params} ")
        AppsFlyerLib.getInstance().logEvent(App.appContext(), eventName, params)

        reportTDParams(eventName, params)
    }

    //数数打点
    fun reportTDParams(eventName: String, params: MutableMap<String, Any> = mutableMapOf(),desc: String?= null) {
        try {
            val properties = JSONObject()
            params.forEach { (key, value) ->
                properties.put(key, value)
            }

            TDAnalytics.track(eventName, properties)
            Log.d("数数埋点", "${desc}, eventName:${eventName}, 参数:${properties} ")

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

}