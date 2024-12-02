package com.app.videobox.ad.base

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import com.app.videobox.App
import com.app.videobox.ad.AdmobManager
import com.app.videobox.ad.InnerAd
import com.app.videobox.ad.adLoaders.NavAd
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import java.math.BigDecimal


class AdUnitWrapper(
    var openBtn: Boolean,
    var type: String ="",
    var adNumber:String,

    var place: AdPlaceTag,
    var innerAdList: List<InnerAd>, //这条id用于的广告场景
    var adLoading: Boolean = false, //广告位上 广告加载状态

    var weight: Int = 0,
    var time:Long = 0
) {

    private lateinit var mAdInstance: Any

    fun setAdInstance(ad: Any) {
        time = System.currentTimeMillis()
        adLoading = false
        mAdInstance = ad


        when (mAdInstance) {
            is InterstitialAd -> {
                (mAdInstance as InterstitialAd).setOnPaidEventListener {adValue->
                    uploadAdjustAdValue(
                        valueMicros = adValue.valueMicros,
                        id = adNumber,
                        type = type,
                        scene = AdmobManager.nowShowAdScene
                    )
                }
            }

            is AppOpenAd -> {
                (mAdInstance as AppOpenAd).setOnPaidEventListener {adValue->
                    uploadAdjustAdValue(
                        valueMicros = adValue.valueMicros,
                        id = adNumber,
                        type = type,
                        scene = AdmobManager.nowShowAdScene
                    )
                }
            }

            is NativeAd -> {
                (mAdInstance as NativeAd).setOnPaidEventListener {adValue->
                    uploadAdjustAdValue(
                        valueMicros = adValue.valueMicros,
                        id = adNumber,
                        type = type,
                        scene = AdmobManager.nowShowAdScene
                    )
                }
            }

        }

    }

    fun getAdInstance(): Any {
        return mAdInstance
    }

    fun setAdSourceId(id:String) {
        this.adNumber = id
    }

    fun getAdSourceId(): String {
        return this.adNumber
    }

    fun showFullAd(activity: Activity) {
        when (mAdInstance) {
            is InterstitialAd -> {
                (mAdInstance as InterstitialAd).show(activity)
            }

            is AppOpenAd -> {
                (mAdInstance as AppOpenAd).show(activity)
            }

        }
    }

    fun showSmallAd(activity: Activity, viewGroup: ViewGroup,bigStyle:Boolean = true) {
        when (mAdInstance) {
            is NativeAd -> {
                viewGroup.removeAllViews()
                NavAd.fillNavMaterial(activity, viewGroup, mAdInstance as NativeAd,bigStyle)
            }
            else ->{}

        }
    }

    fun uploadAdjustAdValue(
        valueMicros: Long,
        id: String,
        scene: String? = null,
        type: String,
    ) {
        //把原来的千分值转换成0.001
        val value = valueMicros.toBigDecimal().divide(BigDecimal("1000000.0")).toDouble()

        //广告价值 fb 上报
        val logger = AppEventsLogger.newLogger(App.appContext())
        val params = Bundle()
        //用于指定记录事件所用货币
        params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        //广告id
        params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID,id)
        //广告类型
        params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, type)
        //广告场景
        scene?.let {
            params.putString(AppEventsConstants.EVENT_PARAM_DESCRIPTION,it)
        }
        //当用户将商品添加到购物车时记录此事件。传递给 logEvent 的 valueToSum 应该是商品的价格。
        logger.logEvent(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            value,
            params)

    }
}