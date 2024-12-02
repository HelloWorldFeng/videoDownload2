package com.app.videobox.manager

import android.annotation.SuppressLint
import com.app.videobox.App
import com.app.videobox.ad.AdmobManager
import com.app.videobox.ad.ConfigBean
import com.app.videobox.ad.base.AdPlaceTag
import com.app.videobox.ad.base.AdUnitWrapper
import com.blankj.utilcode.util.SPStaticUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson

object RemoteConfigManager {
    @SuppressLint("StaticFieldLeak")
    private lateinit var remoteConfig: FirebaseRemoteConfig
    var adLoadingTime:Long = 3 *  1000


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
            val result = Gson().fromJson(configJson, ConfigBean::class.java)
            adLoadingTime = result.adLoadingTime * 1000L

            val map = mutableMapOf<String, AdUnitWrapper>()
            result.outerConfigs.forEach { out ->
                //根据广告类型  一条广告类型id用在多个场景
                map[out.format] = AdUnitWrapper(
                    openBtn =  out.adOpen,
                    type = out.format,
                    place = AdPlaceTag.AD_Open,
                    adNumber = out.adNumber,
                    innerAdList = out.innerAdList
                )
            }

            SPStaticUtils.put("launchTime",result.launchTime)

            AdmobManager.initAdMapConfig(map)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}