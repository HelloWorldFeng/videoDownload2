package com.app.videobox.ad

import android.util.Log
import com.app.videobox.App
import com.blankj.utilcode.util.SPStaticUtils
import com.app.videobox.ad.base.AdConst.CAN_LOAD_NAV
import com.app.videobox.ad.base.AdConst.CLICK_COUNT
import com.app.videobox.ad.base.AdConst.CLICK_TIME

import com.app.videobox.ad.adLoaders.IntAd
import com.app.videobox.ad.adLoaders.NavAd
import com.app.videobox.ad.adLoaders.OpenAd
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ad.base.AD_TYPE_NAV
import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ad.base.AdCallBack
import com.app.videobox.ad.base.AdUnitWrapper

class AdLoaderHelper(private val adCfgMap: MutableMap<String, AdUnitWrapper>) {

    companion object{
        var needFillNavAd:Boolean = false
    }

    private val TAG = "AdLog"

    private var clickcallback:(adUnitWrapper: AdUnitWrapper)->Unit = {}
    private lateinit var successCallback: (ads: AdUnitWrapper) -> Unit
    private lateinit var failCallBack: (code: String, msg: String, adUnitWrapper: AdUnitWrapper) -> Unit
    private lateinit var showCallBack:(adUnitWrapper: AdUnitWrapper)->Unit

    fun setAdCallback(
        clickCallBack:(adUnitWrapper: AdUnitWrapper)->Unit,
        successCallback:(ads: AdUnitWrapper)->Unit,
        failCallBack:(code: String, msg: String,  adUnitWrapper: AdUnitWrapper)->Unit,
        showCallBack: (adUnitWrapper: AdUnitWrapper)->Unit
    ): AdLoaderHelper {
        this.successCallback = successCallback
        this.failCallBack = failCallBack
        this.showCallBack = showCallBack
        this.clickcallback = clickCallBack
        return this
    }

    fun loadAdInstance(vararg adTypeTag: String) {
        //检查广告池里有没 对应类型的广告
        adTypeTag.forEach{ type ->
            if (type == AD_TYPE_NAV) {
                if (AdmobManager.smallAdPool.any { it.type == type }){
                    return@forEach
                }
            }else if (type == AD_TYPE_START || type == AD_TYPE_INT){
                if (AdmobManager.fullAdPool.any { it.type == type }){
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

        adUnitWrapper.setAdSourceId(adUnitWrapper.adNumber)
        adUnitWrapper.type = adUnitWrapper.type

        if ((adUnitWrapper.type in listOf(AD_TYPE_NAV)) && AdmobManager.permissionNav.not()) {
            Log.d(TAG, "原生限制---->不请求 ")
            return
        }
        if (adUnitWrapper.adLoading) {
            Log.d(TAG, "${adUnitWrapper.type} 位置正在请求中---> 等待回调结果")
            return
        }


        adUnitWrapper.adLoading = true
        Log.d(TAG, "-------------------- 请求 ${adUnitWrapper.type} 广告 id:${adUnitWrapper.getAdSourceId()} 权重:${adUnitWrapper.weight} 类型:${adUnitWrapper.type} --------------------")


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
        afEventLog(eventName = "ud_ad_action_request", mutableMapOf<String, Any>().apply {
            put("ad_format",adUnitWrapper.type)
            put("ad_unit_id",adUnitWrapper.adNumber)
        })
    }

    private fun loadAdMobIntAdInstance(adUnitWrapper: AdUnitWrapper) {
        IntAd().apply {
            setAdCallBackInstance(object : AdCallBack {
                override fun loadSuccess(adInstance: Any) {
                    adUnitWrapper.setAdInstance(adInstance)
                    AdmobManager.fullAdPool.add(adUnitWrapper)
                    successCallback.invoke(adUnitWrapper)

                    AdmobManager.finishLoadBlock?.let {
                        it.invoke(true)
                        AdmobManager.finishLoadBlock = null
                    }
                }

                override fun loadFail(code: Int, msg: String) {
                    failCallBack.invoke(code.toString(), msg, adUnitWrapper)
                    AdmobManager.finishLoadBlock?.let {
                        it.invoke(false)
                        AdmobManager.finishLoadBlock = null
                    }
                }

                override fun onShow() {
                    showCallBack.invoke(adUnitWrapper)
                    loadAdInstance(adUnitWrapper.type)
                }

                override fun onClose() {
                    AdmobManager.closeBlock?.let { it() }
                    AdmobManager.closeBlock = null
                }

                override fun onClick() {
                    clickcallback.invoke(adUnitWrapper)
                }
            })
        }.loadingAd(adUnitWrapper.getAdSourceId(),adUnitWrapper.type)
    }

    private fun loadAdMobOpenAdInstance(adUnitWrapper: AdUnitWrapper) {
        OpenAd().apply {
            setAdCallBackInstance(object : AdCallBack {
                override fun loadSuccess(adInstance: Any) {
                    adUnitWrapper.setAdInstance(adInstance)
                    AdmobManager.fullAdPool.add(adUnitWrapper)
                    successCallback.invoke(adUnitWrapper)
                }

                override fun loadFail(code: Int, msg: String) {
                    failCallBack.invoke(code.toString(), msg, adUnitWrapper)
                }

                override fun onShow() {
                    showCallBack.invoke(adUnitWrapper)
                }

                override fun onClose() {
                    AdmobManager.closeBlock?.let { it() }
                    AdmobManager.closeBlock = null
                }

                override fun onClick() {
                    clickcallback.invoke(adUnitWrapper)
                }
            })
        }.loadingAd(adUnitWrapper.getAdSourceId(),adUnitWrapper.type)
    }

    private fun loadAdMobNavAdInstance(adUnitWrapper: AdUnitWrapper) {
        NavAd().apply {
            setAdCallBackInstance(object : AdCallBack {
                override fun loadSuccess(adInstance: Any) {
                    if (needFillNavAd) {
                        adUnitWrapper.setAdInstance(adInstance)
                        AdmobManager.navGetCallback?.invoke(adUnitWrapper)
                        Log.d(TAG, "原生广告 现拉现用")
                        needFillNavAd = false
                        return
                    }

                    adUnitWrapper.setAdInstance(adInstance)
                    AdmobManager.smallAdPool.add(adUnitWrapper)
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
//                    clickCount()
                }
            })
        }.loadingAd(adUnitWrapper.getAdSourceId(),adUnitWrapper.type)
    }


    private fun clickCount() {
        // 增加点击次数
        AdmobManager.nowClickCount++
        Log.d(TAG, "原生点击次数: ${AdmobManager.nowClickCount}")

        // 存储点击次数和时间
        SPStaticUtils.put(CLICK_COUNT, AdmobManager.nowClickCount)
        SPStaticUtils.put(CLICK_TIME, System.currentTimeMillis())

        // 检查是否达到点击限制
        if (AdmobManager.nowClickCount >= AdmobManager.navClickCountLimit) {
            handleClickLimit()
        }
    }

    // 处理点击次数达到限制的逻辑
    private fun handleClickLimit() {
        AdmobManager.permissionNav = false
        SPStaticUtils.put(CAN_LOAD_NAV, false)
        Log.d(TAG, "已达到点击次数限制，无法加载")
    }

}