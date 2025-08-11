package com.app.videobox.ad

import android.util.Log

import com.app.videobox.ad.adLoaders.IntAdmobAdLoader
import com.app.videobox.ad.adLoaders.NavAdmobAdLoader
import com.app.videobox.ad.adLoaders.OpenAdmobAdLoader
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ad.base.AD_TYPE_NAV
import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ad.callback.AdCallBack
import com.app.videobox.ad.base.AdUnitWrapper
import com.blankj.utilcode.util.SPStaticUtils

class AdLoaderHelper(private val adCfgMap: MutableMap<String, AdUnitWrapper>) {

    companion object{
        var needFillNavAd:Boolean = false
    }

    private val TAG = "AdLog"

    private var clickcallback:(adUnitWrapper: AdUnitWrapper)->Unit = {}
    private lateinit var successCallback: (ads: AdUnitWrapper) -> Unit
    private lateinit var failCallBack: (code: String, msg: String, adUnitWrapper: AdUnitWrapper) -> Unit
    private lateinit var showCallBack:(adUnitWrapper: AdUnitWrapper)->Unit
    private lateinit var closeCallBack:(adUnitWrapper: AdUnitWrapper)->Unit

    fun setAdCallback(
        clickCallBack:(adUnitWrapper: AdUnitWrapper)->Unit,
        successCallback:(ads: AdUnitWrapper)->Unit,
        failCallBack:(code: String, msg: String,  adUnitWrapper: AdUnitWrapper)->Unit,
        showCallBack: (adUnitWrapper: AdUnitWrapper)->Unit,
        closeCallBack: (adUnitWrapper: AdUnitWrapper)->Unit
    ): AdLoaderHelper {
        this.successCallback = successCallback
        this.failCallBack = failCallBack
        this.showCallBack = showCallBack
        this.clickcallback = clickCallBack
        this.closeCallBack = closeCallBack
        return this
    }

    fun loadAdInstance(vararg adTypeTag: String) {
        //检查广告池里有没 对应类型的广告
        adTypeTag.forEach{ type ->
            if (type == AD_TYPE_NAV) {
                if (AdManager.smallAdPool.any { it.type == type }){
                    return@forEach
                }
            }else if (type == AD_TYPE_START || type == AD_TYPE_INT){
                if (AdManager.fullAdPool.any { it.type == type }){
                    return@forEach
                }
            }else {
                return
            }

            adCfgMap[type]?.let {
                if (it.openBtn) {
                    loadAdByType(it)
                }
            }
        }
    }

    private fun loadAdByType(adUnitWrapper: AdUnitWrapper) {

        adUnitWrapper.setAdSourceId(adUnitWrapper.adNumberId)
        adUnitWrapper.type = adUnitWrapper.type


        if (adUnitWrapper.adLoading) {
            Log.d(TAG, "${adUnitWrapper.type} 位置正在请求中---> 等待回调结果")
            return
        }



        if (checkAdTypeRequest(adUnitWrapper.type)) {
            Log.d(TAG, "${adUnitWrapper.type}类型请求达到限制")
            return
        }

        adUnitWrapper.adLoading = true
        Log.d(TAG, "-------------------- 请求 ${adUnitWrapper.type} 广告 id:${adUnitWrapper.getAdSourceId()} 类型:${adUnitWrapper.type} --------------------")
        if (adUnitWrapper.type == AD_TYPE_START || adUnitWrapper.type == AD_TYPE_INT) {
            val count = SPStaticUtils.getInt("${adUnitWrapper.type}_request",0) + 1
            SPStaticUtils.put("${adUnitWrapper.type}_request",count)
        }

        if (adUnitWrapper.type == AD_TYPE_NAV) {
            loadAdMobNavAdInstance( adUnitWrapper)
        }else if(adUnitWrapper.type == AD_TYPE_INT){
            loadAdMobIntAdInstance( adUnitWrapper)
        }else if(adUnitWrapper.type == AD_TYPE_START){
            loadAdMobOpenAdInstance( adUnitWrapper)
        }
        else{
            Log.d(TAG, "${adUnitWrapper.getAdSourceId()} 类型:${adUnitWrapper.type} 错误")
            adUnitWrapper.adLoading = false
        }
    }

    private fun loadAdMobIntAdInstance(adUnitWrapper: AdUnitWrapper) {
        IntAdmobAdLoader().apply {
            setAdCallBackInstance(object : AdCallBack {
                override fun loadSuccess(adInstance: Any) {
                    adUnitWrapper.setAdInstance(adInstance)
                    AdManager.fullAdPool.add(adUnitWrapper)
                    successCallback.invoke(adUnitWrapper)

                    AdManager.finishLoadBlock?.let {
                        it.invoke(true)
                        AdManager.finishLoadBlock = null
                    }
                }

                override fun loadFail(code: Int, msg: String) {
                    failCallBack.invoke(code.toString(), msg, adUnitWrapper)
                    AdManager.finishLoadBlock?.let {
                        it.invoke(false)
                        AdManager.finishLoadBlock = null
                    }
                }

                override fun onShow() {
                    showCallBack.invoke(adUnitWrapper)
                    loadAdInstance(adUnitWrapper.type)
                }

                override fun onClose() {
                    closeCallBack.invoke(adUnitWrapper)
                    AdManager.closeBlock?.let { it() }
                    AdManager.closeBlock = null
                }

                override fun onClick() {
                    clickcallback.invoke(adUnitWrapper)
                }
            })
        }.loadingAd(adUnitWrapper.getAdSourceId(),adUnitWrapper.type)
    }

    private fun loadAdMobOpenAdInstance(adUnitWrapper: AdUnitWrapper) {
        OpenAdmobAdLoader().apply {
            setAdCallBackInstance(object : AdCallBack {
                override fun loadSuccess(adInstance: Any) {
                    adUnitWrapper.setAdInstance(adInstance)
                    AdManager.fullAdPool.add(adUnitWrapper)
                    successCallback.invoke(adUnitWrapper)
                }

                override fun loadFail(code: Int, msg: String) {
                    failCallBack.invoke(code.toString(), msg, adUnitWrapper)
                }

                override fun onShow() {
                    showCallBack.invoke(adUnitWrapper)
                }

                override fun onClose() {
                    closeCallBack.invoke(adUnitWrapper)
                    AdManager.closeBlock?.let { it() }
                    AdManager.closeBlock = null
                }

                override fun onClick() {
                    clickcallback.invoke(adUnitWrapper)
                }
            })
        }.loadingAd(adUnitWrapper.getAdSourceId(),adUnitWrapper.type)
    }

    private fun loadAdMobNavAdInstance(adUnitWrapper: AdUnitWrapper) {
        NavAdmobAdLoader().apply {
            setAdCallBackInstance(object : AdCallBack {
                override fun loadSuccess(adInstance: Any) {
                    if (needFillNavAd) {
                        adUnitWrapper.setAdInstance(adInstance)
                        AdManager.navGetCallback?.invoke(adUnitWrapper)
                        Log.d(TAG, "原生广告 ---->现拉现用")
                        needFillNavAd = false
                        return
                    }

                    adUnitWrapper.setAdInstance(adInstance)
                    AdManager.smallAdPool.add(adUnitWrapper)
                    successCallback.invoke(adUnitWrapper)
                }

                override fun loadFail(code: Int, msg: String) {
                    failCallBack.invoke(code.toString(), msg, adUnitWrapper)
                }

                override fun onShow() {
                    showCallBack.invoke(adUnitWrapper)
                    loadAdInstance(adUnitWrapper.type)
                }

                override fun onClose() {}

                override fun onClick() {
                    clickcallback.invoke(adUnitWrapper)
                }
            })
        }.loadingAd(adUnitWrapper.getAdSourceId(),adUnitWrapper.type)
    }


    //检查请求限制
    private fun checkAdTypeRequest(adType: String): Boolean {
        val nowShowCount = SPStaticUtils.getInt("${adType}_request", 0)

        if (adType == AD_TYPE_START && AdUtils.getOpenRequestCountDay() != 0) {
            return nowShowCount >= AdUtils.getOpenRequestCountDay()
        }
        if (adType == AD_TYPE_INT && AdUtils.getIntRequestCountDay() != 0) {
            return nowShowCount >= AdUtils.getIntRequestCountDay()
        }
        return false
    }

}