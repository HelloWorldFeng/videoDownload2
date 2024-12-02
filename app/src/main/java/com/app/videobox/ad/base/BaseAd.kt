package com.app.videobox.ad.base

import com.app.videobox.ad.AdCallBack


abstract class BaseAd {

    private lateinit var adCallBack: AdCallBack

    abstract fun loadingAd(id: String,place: String,type:String)

    fun setAdCallBackInstance(adCallBack: AdCallBack) {
        this.adCallBack = adCallBack
    }

    fun getAdCallBackInstance(): AdCallBack {
        return adCallBack
    }

}