package com.app.videobox.manager

import android.annotation.SuppressLint
import android.util.Log
import androidx.core.net.toUri
import com.app.videobox.App
import com.app.videobox.BuildConfig
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.AdUtils
import com.app.videobox.ad.UserHelper
import com.app.videobox.ad.base.AdConfig
import com.app.videobox.ad.base.AdLimitConfig
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.network.model.UrlBlack
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
import kotlin.times

object RemoteConfigManager {
    @SuppressLint("StaticFieldLeak")
    private lateinit var remoteConfig: FirebaseRemoteConfig
    
    var groupNotify = 2
    var limitTime = 5 * 60 * 1000L
    var notifyCount = 30
    private var blackUrl: UrlBlack? = null

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

            groupNotify = if (result.groupNotify == 0) 2 else result.groupNotify

            limitTime = if (result.limitTime == 0L) 5 * 60 *1000 else result.limitTime * 60 * 1000
            notifyCount = if (result.notifyCount == 0) 30 else result.notifyCount
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

        try {
            val blackJson = App.appContext().assets.open("black_url.json").use {
                return@use it.readBytes().decodeToString()
            }
            blackUrl = Gson().fromJson(blackJson, UrlBlack::class.java)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun checkUrlInBlackUrl(url: String): Boolean {
        if (blackUrl == null) {
            return true
        }
        if (blackUrl!!.isEmpty()) {
            return true
        }

        val host = url.toUri().host.toString()
        blackUrl!!.forEach {
            if (it.url.toUri().host?.contains(host) == true) {
                return true
            }
        }

        return false
    }
}