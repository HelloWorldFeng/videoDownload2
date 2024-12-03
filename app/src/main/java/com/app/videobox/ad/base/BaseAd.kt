package com.app.videobox.ad.base


abstract class BaseAd {

    private lateinit var adCallBack: AdCallBack

    abstract fun loadingAd(id: String,type:String)

    fun setAdCallBackInstance(adCallBack: AdCallBack) {
        this.adCallBack = adCallBack
    }

    fun getAdCallBackInstance(): AdCallBack {
        return adCallBack
    }

}