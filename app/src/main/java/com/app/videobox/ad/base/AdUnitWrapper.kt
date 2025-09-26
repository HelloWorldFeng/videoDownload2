package com.app.videobox.ad.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import com.app.videobox.BuildConfig
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.adLoaders.NavAdmobAdLoader
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.blankj.utilcode.util.SPStaticUtils
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.app.videobox.App
import com.app.videobox.ui.SplashActivity
import com.app.videobox.utils.EventReportUtils
import org.json.JSONObject
import java.math.BigDecimal


class AdUnitWrapper(
    var openBtn: Boolean,
    var type: String ="",
    var adNumberId:String,

    var innerAdList: List<InnerAd>, //这条id用于的广告场景
    var adLoading: Boolean = false, //广告位上 广告加载状态

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
                    uploadFacebookAdValue(
                        valueMicros = adValue.valueMicros,
                        id = adNumberId,
                        type = type,
                        scene = AdManager.nowShowAdScene,
                        precision = adValue.precisionType,
                        currency = adValue.currencyCode
                    )
                    uploadFirebaseAdValue(adValue)
                    impressionEvent(adValue,adNumberId,type)
                }
            }

            is AppOpenAd -> {
                (mAdInstance as AppOpenAd).setOnPaidEventListener {adValue->
                    uploadFacebookAdValue(
                        valueMicros = adValue.valueMicros,
                        id = adNumberId,
                        type = type,
                        scene = AdManager.nowShowAdScene,
                        precision = adValue.precisionType,
                        currency = adValue.currencyCode
                    )
                    uploadFirebaseAdValue(adValue)
                    impressionEvent(adValue,adNumberId,type)
                }
            }

            is NativeAd -> {
                (mAdInstance as NativeAd).setOnPaidEventListener {adValue->
                    uploadFacebookAdValue(
                        valueMicros = adValue.valueMicros,
                        id = adNumberId,
                        type = type,
                        scene = AdManager.nowShowAdScene,
                        precision = adValue.precisionType,
                        currency = adValue.currencyCode
                    )
                    uploadFirebaseAdValue(adValue)
                    impressionEvent(adValue,adNumberId,type)
                }
            }

        }

    }

    fun getAdInstance(): Any {
        return mAdInstance
    }

    fun setAdSourceId(id:String) {
        this.adNumberId = id
    }

    fun getAdSourceId(): String {
        return this.adNumberId
    }

    fun showAdSmall(activity: Context, viewGroup: ViewGroup,isBigStyle: Boolean = false) {
        when (mAdInstance) {
            is NativeAd -> {
                viewGroup.removeAllViews()
                NavAdmobAdLoader.fillNavMaterial(activity, viewGroup, mAdInstance as NativeAd,isBigStyle)
            }
            else ->{}

        }
    }

    fun showAdFull(activity: Activity) {
        when (mAdInstance) {
            is InterstitialAd -> {
                (mAdInstance as InterstitialAd).show(activity)
            }

            is AppOpenAd -> {
                (mAdInstance as AppOpenAd).show(activity)
            }

        }
    }

    fun uploadFacebookAdValue(
        valueMicros: Long,
        id: String,
        scene: String? = null,
        type: String,
        precision: Int,
        currency: String,
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
        if (type != AD_TYPE_NAV) {
            //原生不上报Facebook
            logger.logEvent(AppEventsConstants.EVENT_NAME_PURCHASED, value, params)

        }

        EventReportUtils.afEventLog(eventName = "ud_ad_action_impression", mutableMapOf<String, Any>().apply {
            put("ad_action",31)
            put("ad_format",type)
            put("ad_unit_id",id)
            scene?.let {
                put("ad_scenes",scene)
            }
            put("value",value)
            put("precision",precision)
            put("currency",currency)
            put("interType",SplashActivity.interType)
        })

        EventReportUtils.afEventLog(eventName = "ud_ad_impression", mutableMapOf<String, Any>().apply {
            put("ad_format",type)
            put("ad_unit_id",id)
            scene?.let {
                put("ad_scenes",scene)
            }

            put("value",value)
            put("precision",precision)
            put("currency",currency)
            put("interType",SplashActivity.interType)
        })

        val param = mutableMapOf<String, Any>(
            AFInAppEventParameterName.REVENUE to value,
            AFInAppEventParameterName.CURRENCY to currency,
            AFInAppEventParameterName.QUANTITY to 1,
            AFInAppEventParameterName.CONTENT_TYPE to "IAA",
            AFInAppEventParameterName.CONTENT_ID to id,
        )
        EventReportUtils.afEventLog(AFInAppEventType.PURCHASE, param)
    }

    fun uploadFirebaseAdValue(adValue: AdValue) {
        //firebase
        Firebase.analytics.logEvent("Ad_Impression_Revenue") {
            val currentImpressionRevenue = adValue.valueMicros / 1000000.0
            param(FirebaseAnalytics.Param.VALUE, currentImpressionRevenue)
            param(FirebaseAnalytics.Param.CURRENCY, "USD")

            val precisionType: String = when (adValue.precisionType) {
                0 -> "UNKNOWN"
                1 -> "ESTIMATED"
                2 -> "PUBLISHER_PROVIDED"
                3 -> "PRECISE"
                else -> "Invalid"
            }
            param("precisionType", precisionType)
        }
        Log.e("zzz", "Ad_Impression_Revenue =  ")

        totalRevenueEvent(adValue)
    }

    fun impressionEvent(adValue: AdValue, unitId: String, format: String) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION) {
            val currentImpressionRevenue = adValue.valueMicros / 1000000.0
            param(FirebaseAnalytics.Param.AD_PLATFORM, "adMob")
            param(FirebaseAnalytics.Param.AD_UNIT_NAME, unitId)
            param(FirebaseAnalytics.Param.AD_FORMAT, format)
            param(FirebaseAnalytics.Param.AD_SOURCE, "")
            param(FirebaseAnalytics.Param.VALUE, currentImpressionRevenue)
            param(FirebaseAnalytics.Param.CURRENCY, "USD")
        }
        Log.e("zzz", "AD_IMPRESSION =  ")
    }

    private fun totalRevenueEvent(adValue: AdValue?, valueMicros: Double? = null) {
        adValue?.apply {
            val lastValue = SPStaticUtils.getFloat("AD_VALUE_SURPLUS", 0.0f)
            val currentValue = if (BuildConfig.DEBUG) 0.02 else lastValue + this.valueMicros / 1000000.0

            if (currentValue >= 0.01) {
                Firebase.analytics.logEvent("Total_Ads_Revenue_001") {
                    param(FirebaseAnalytics.Param.VALUE, currentValue)
                    param(FirebaseAnalytics.Param.CURRENCY, "USD")

                }

                val tdParams = JSONObject(
                    mapOf(
                        FirebaseAnalytics.Param.VALUE to currentValue,
                        FirebaseAnalytics.Param.CURRENCY to "USD",
                    )
                )

                Log.e("zzz", "Total_Ads_Revenue_001.param =  $tdParams")
                SPStaticUtils.put("AD_VALUE_SURPLUS", 0.0f)
            } else {
                SPStaticUtils.put("AD_VALUE_SURPLUS", currentValue.toFloat())
            }
            return
        }

    }

}