package com.app.videobox.ad

import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ad.base.SpecialConfig
import com.blankj.utilcode.util.SPStaticUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AdUtils {

    //开屏一小时的展示和请求次数
    private var openShowMaxCalls = 10

    //插页一小时的展示和请求次数
    private var intShowMaxCalls = 10


    private val timeWindow = 60 * 60 * 1000L

    private var openShowInterval = 0L
    private var intShowInterval = 0L

    private var openShowCountDay = 0
    private var intShowCountDay = 0
    private var openRequestCountDay = 0
    private var intRequestCountDay = 0

    fun getOpenRequestCountDay(): Int {
        return openRequestCountDay
    }
    fun getIntRequestCountDay(): Int {
        return intRequestCountDay
    }

    fun getOpenShowCountDay(): Int {
        return openShowCountDay
    }

    fun getIntShowCountDay(): Int {
        return intShowCountDay
    }

    fun getOpenInterval(): Long {
        return openShowInterval
    }
    fun getIntInterval(): Long {
        return intShowInterval
    }


    fun canCallShow(adType: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val callTimesJson = SPStaticUtils.getString("${adType}_show_call_times", "[]")

        val callTimes = try {
            Gson().fromJson<List<Long>>(callTimesJson, object : TypeToken<List<Long>>() {}.type)
        } catch (e: Exception) {
            mutableListOf()
        }

        // 清理过期记录
        val validTimes = callTimes.filter { currentTime - it <= timeWindow }.toMutableList()

        // 检查是否还能调用
        val canCall = validTimes.size < if (adType == AD_TYPE_START) openShowMaxCalls else intShowMaxCalls

        if (canCall) {
            // 记录本次调用
            validTimes.add(currentTime)
            SPStaticUtils.put("${adType}_show_call_times", Gson().toJson(validTimes))
        } else {
            // 更新清理后的记录
            SPStaticUtils.put("${adType}_show_call_times", Gson().toJson(validTimes))
        }

        return canCall
    }


    fun getRemainingShowCalls(adType: String): Int {
        val currentTime = System.currentTimeMillis()
        val callTimesJson = SPStaticUtils.getString("${adType}_show_call_times", "[]")

        val callTimes = try {
            Gson().fromJson<List<Long>>(callTimesJson, object : TypeToken<List<Long>>() {}.type)
        } catch (e: Exception) {
            mutableListOf()
        }

        val validTimes = callTimes.filter { currentTime - it <= timeWindow }
        return (if (adType == AD_TYPE_START) openShowMaxCalls else intShowMaxCalls) - validTimes.size
    }



    fun setFuckConfig(config: SpecialConfig) {

        openShowMaxCalls = config.open_show_count_one_hours

        intShowMaxCalls = config.int_show_count_one_hours

        openShowInterval = config.open_show_interval_time * 1000L
        intShowInterval = config.int_show_interval_time * 1000L

        openShowCountDay = config.open_show_count_one_day
        intShowCountDay = config.int_show_count_one_day
        openRequestCountDay = config.open_request_count_one_day
        intRequestCountDay = config.int_request_count_one_day
    }
}