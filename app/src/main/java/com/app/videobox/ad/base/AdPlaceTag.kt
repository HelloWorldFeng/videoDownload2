package com.app.videobox.ad.base


//admob广告类型
const val AD_TYPE_START = "open"
const val AD_TYPE_INT = "int"
const val AD_TYPE_NAV = "nav"
//广告位置
const val ColdStart = "cold_start"
const val HostStart = "hot_start"
const val Home = "home"
const val Finish = "finish"


enum class AdPlaceTag {
    AD_Open, AD_Home, AD_Finish, AD_Unknow;

    fun getAdPlaceType(): List<String> {
        return when (this) {
            AD_Open -> {
                listOf(AD_TYPE_START, AD_TYPE_INT)
            }

            AD_Home -> listOf(
                AD_TYPE_NAV
            )

            AD_Finish -> listOf(AD_TYPE_INT)

            AD_Unknow -> listOf()
        }
    }


    fun getPlaceString(): String {
        return when (this) {
            AD_Open -> ColdStart
            AD_Home -> Home
            AD_Finish -> Finish
            AD_Unknow -> "unknow"
        }
    }
}

fun getAdPlaceTag(place:String): AdPlaceTag {
    return when (place) {
        ColdStart -> AdPlaceTag.AD_Open
        Home -> AdPlaceTag.AD_Home
        Finish -> AdPlaceTag.AD_Finish
        else -> AdPlaceTag.AD_Unknow
    }
}

