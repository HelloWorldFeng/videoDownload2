package com.app.videobox.ad

import android.util.Log
import cn.thinkingdata.analytics.TDAnalytics
import com.blankj.utilcode.util.SPStaticUtils
import org.json.JSONObject

object UserHelper {
    const val launchTimeFirst = "launchTimeFirst"
    const val ChannelUser = "channel_user"
    const val OriginUser = "origin_user"
    const val GoogleUser = "google_user"
    const val FacebookUser = "facebook_user"

    const val UserType = "user_type"
    const val NewUser = "new_user"
    const val OldUser = "old_user"

    //默认自然用户
    var channelUser = OriginUser
    //默认新用户
    var userType = NewUser
    //默认审核用户
    var powerUser = true

    fun initUserInfo(){
        fun isSameDay(time1: Long, time2: Long): Boolean {
            val calendar1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
            val calendar2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
            return calendar1.get(java.util.Calendar.YEAR) == calendar2.get(java.util.Calendar.YEAR)
                    && calendar1.get(java.util.Calendar.DAY_OF_YEAR) == calendar2.get(java.util.Calendar.DAY_OF_YEAR)
        }

        fun isOver24Hours(time1: Long, time2: Long): Boolean {
            return kotlin.math.abs(time1 - time2) > 24 * 60 * 60 * 1000L
        }
        //新老用户判断
        val userType = JSONObject()
        val firstLaunchTime = SPStaticUtils.getLong(launchTimeFirst, 0L)
        if (firstLaunchTime == 0L) {
            SPStaticUtils.put(launchTimeFirst, System.currentTimeMillis())
            //首次启动直接是新用户
            userType.put("user_type","new_user")
        }else{


            val nowTime = System.currentTimeMillis()

            val isSameDay = isSameDay(nowTime, firstLaunchTime)

            val isOver24h = isOver24Hours(nowTime, firstLaunchTime)

            if(isSameDay){
                userType.put("user_type","new_user")
            }
            else if(isOver24h){
                userType.put("user_type","24_user")
            }else{
                userType.put("user_type","day_user")
            }

            UserHelper.userType = if (isSameDay) NewUser else OldUser
        }
        val user = userType.get("user_type")
        TDAnalytics.userSet(JSONObject(mapOf("user_type" to user)))
        Log.d("BugLog", "新老用户:${user} ")

        channelUser = SPStaticUtils.getString(
            ChannelUser,
            OriginUser
        )

    }

}