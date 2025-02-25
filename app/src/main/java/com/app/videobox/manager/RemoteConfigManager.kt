package com.app.videobox.manager

import android.annotation.SuppressLint
import com.app.videobox.App
import com.app.videobox.ad.AdmobManager
import com.app.videobox.ad.ConfigBean
import com.app.videobox.ad.base.AdUnitWrapper
import com.blankj.utilcode.util.SPStaticUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import kotlin.random.Random

object RemoteConfigManager {
    @SuppressLint("StaticFieldLeak")
    private lateinit var remoteConfig: FirebaseRemoteConfig
    var adLoadingTime:Long = 3 *  1000
    private var initSdk:Int = 0


    fun fetchConfig() {
        FirebaseApp.initializeApp(App.appContext())
        initRemoteConfig()
    }

    fun checkProbability(percentage: Int): Boolean {
        // 生成一个0到99之间的随机整数
        val randomValue = Random.nextInt(100)
        // 判断随机值是否小于传入的百分比
        return randomValue < percentage
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
                if (checkProbability(initSdk) && SPStaticUtils.getBoolean("notInit",true)) {
                    App.initColSdk()
                }
                SPStaticUtils.put("notInit",false)
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
            initSdk = result.initSdk
            val map = mutableMapOf<String, AdUnitWrapper>()
            result.outerConfigs.forEach { out ->
                //根据广告类型  一条广告类型id用在多个场景
                map[out.format] = AdUnitWrapper(
                    openBtn =  out.adOpen,
                    type = out.format,
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