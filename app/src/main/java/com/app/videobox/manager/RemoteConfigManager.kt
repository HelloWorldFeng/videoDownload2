package com.app.videobox.manager

import android.annotation.SuppressLint
import com.app.videobox.App
import com.app.videobox.BuildConfig
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.AdUtils
import com.app.videobox.ad.UserHelper
import com.app.videobox.ad.base.AdConfig
import com.app.videobox.ad.base.AdLimitConfig
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.utils.NotifyHelper
import com.blankj.utilcode.util.SPStaticUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import kotlin.random.Random
import kotlin.text.format

object RemoteConfigManager {
    @SuppressLint("StaticFieldLeak")
    private lateinit var remoteConfig: FirebaseRemoteConfig
    var adLoadingTime:Long = 3 *  1000
    private var initSdk:Int = 0
    var groupNotify = 2
    var limitTime = 5 * 60 * 1000L
    var notifyCount = 30

    fun fetchConfig() {
        FirebaseApp.initializeApp(App.appContext())
        initRemoteConfig()
    }

    private fun initRemoteConfig() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()

        remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(settings)

        setupConfigParams()



        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                setupConfigParams()
            }
        }
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    setupConfigParams()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {}
        })
    }


    private fun setupConfigParams() {
        try {
            var configJson = remoteConfig.getString("video_config")
            if (configJson.isEmpty()) {
                configJson = App.appContext().assets.open("config.json").use {
                    return@use it.readBytes().decodeToString()
                }
            }
            val result = Gson().fromJson(configJson, AdConfig::class.java)

            val map = mutableMapOf<String, AdUnitWrapper>()
            result.outerConfigs.forEach { out ->
                //根据广告类型  一条广告类型id用在多个场景
                map[out.format] = AdUnitWrapper(
                    openBtn =  out.adOpen,
                    type = out.format,
                    adNumberId = out.adNumber,
                    innerAdList = out.innerAdList
                )
            }


            AdManager.initAdMapConfig(map)

        }catch (e:Exception){
            e.printStackTrace()
        }

        try {
            var adLimitJson = remoteConfig.getString("ad_limit_config")
            if (adLimitJson.isEmpty()) {
                adLimitJson = App.appContext().assets.open("ad_limit_config.json").use {
                    return@use it.readBytes().decodeToString()
                }
            }
            val adLimitConfig = Gson().fromJson(adLimitJson, AdLimitConfig::class.java)
            if (UserHelper.channelUser in adLimitConfig.source && UserHelper.userType in adLimitConfig.user_type) {
                AdUtils.setFuckConfig(adLimitConfig.special_config)
            }else{
                AdUtils.setFuckConfig(adLimitConfig.default_config)
            }

            NotifyHelper.createNotificationIdList(groupNotify)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}