package com.app.videobox

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ui.SplashActivity
import com.app.videobox.utils.LanguageUtils.setAppLanguage
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils.OnAppStatusChangedListener
import com.shuyu.gsyvideoplayer.utils.GSYVideoType

class App : Application() {

    init {
        instance = this
    }

    companion object{
         private lateinit var instance: App

        fun appContext(): Context {
            return instance.applicationContext
        }
    }
    override fun onCreate() {
        super.onCreate()
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setAppLanguage(this)
    }

}