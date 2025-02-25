package com.app.videobox.ad

data class ConfigBean(
    val navClick: Int,
    val launchTime:Int,
    val textBack:Boolean,
    val ocrBack:Boolean,
    val speechBack:Boolean,
    val chatBack:Boolean,
    val adLoadingTime:Int,

    val firstCountry:String,
    val outerConfigs: List<OuterConfig>,

    val openFinish:Boolean,
    val connectInt:Boolean,

    val initSdk:Int
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

    var nowClickCount:Int,
    var nowShowCount:Int
)