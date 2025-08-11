package com.app.videobox.ad.base


//admob广告类型
const val AD_TYPE_START = "open"
const val AD_TYPE_INT = "int"
const val AD_TYPE_NAV = "nav"

data class AdConfig(
    var groupNotify: Int = 1,
    var limitTime: Long = 0L,
    var notifyCount: Int = 30,

    val guiderGo: Boolean,

    val outerConfigs: List<OuterConfig>,
    )

data class OuterConfig(
    val adNumber: String,
    val adOpen: Boolean,
    val format: String,
    val innerAdList: List<InnerAd>
)

data class InnerAd(
    val adScene: String,
    val clickCount: Int,
    val showBtn: Boolean,
    val showCount: Int,
    val interval:Int,

    var nowClickCount:Int,
    var nowShowCount:Int
)