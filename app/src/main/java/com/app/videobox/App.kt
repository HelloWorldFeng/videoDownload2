package com.app.videobox

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.ui.SplashActivity
import com.app.videobox.utils.LanguageUtils.setAppLanguage
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils.OnAppStatusChangedListener
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
    }
    override fun onCreate() {
        super.onCreate()

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