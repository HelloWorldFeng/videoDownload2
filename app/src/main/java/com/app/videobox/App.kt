package com.app.videobox

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.manager.RemoteConfigManager.checkProbability
import com.app.videobox.ui.SplashActivity
import com.app.videobox.utils.LanguageUtils.setAppLanguage
import com.appsflyer.AppsFlyerLib
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.blankj.utilcode.util.Utils.OnAppStatusChangedListener
import com.collect.CollectManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    init {
        instance = this
    }

    companion object{
         private lateinit var instance: App
        val coroutineScope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
        fun appContext(): Context {
            return instance.applicationContext
        }

        fun initColSdk() {
            CollectManager.init(instance,"com.streambox.player.wx")
            AppsFlyerLib.getInstance().init(BuildConfig.afKey, null, instance)
            AppsFlyerLib.getInstance().start(instance)
        }


    }
    override fun onCreate() {
        super.onCreate()
        initColSdk()
        coroutineScope.launch {
            RemoteConfigManager.fetchConfig()
        }

        AppUtils.registerAppStatusChangedListener(object :OnAppStatusChangedListener{
            override fun onForeground(activity: Activity?) {
                if (activity is SplashActivity) {
                    return
                }
                activity?.safeStartActivity(SplashActivity::class.java)
            }

            override fun onBackground(activity: Activity?) {
            }
        })



    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setAppLanguage(this)
    }

}