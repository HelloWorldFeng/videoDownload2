package com.app.videobox.ad.base

import com.app.videobox.ad.callback.AdCallBack


abstract class BaseAd {

    private lateinit var adCallBack: AdCallBack

    abstract fun loadingAd(id: String,type:String)

    fun getAdCallBackInstance(): AdCallBack {
        return adCallBack
    }

    fun setAdCallBackInstance(adCallBack: AdCallBack) {
        this.adCallBack = adCallBack
    }



}