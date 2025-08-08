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
import com.app.videobox.network.DataRepository
import com.app.videobox.ui.SplashActivity
import com.app.videobox.ui.pages.webViewPage.WebViewModel
import com.app.videobox.utils.LanguageUtils.setAppLanguage
import com.appsflyer.AppsFlyerLib
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.blankj.utilcode.util.Utils.OnAppStatusChangedListener
import com.videodownloader.module.api.VideoDownloaderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.File

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
//            CollectManager.init(instance,"com.streambox.player.wx")
            AppsFlyerLib.getInstance().init(BuildConfig.afKey, null, instance)
            AppsFlyerLib.getInstance().start(instance)
        }


    }
    override fun onCreate() {
        super.onCreate()
        initColSdk()
        initApi()
        //初始化VideoDownloaderManager
        VideoDownloaderManager.initialize(this)
        
        // 从备份恢复下载任务，确保应用重启后任务数据不丢失
        try {
            VideoDownloaderManager.restoreFromBackup()
            Log.i("App", "下载任务恢复成功")
        } catch (e: Exception) {
            Log.e("App", "下载任务恢复失败", e)
        }
        
        //依赖注入
        startKoin {
            androidContext(this@App)
            modules(
                module {
                    viewModel { WebViewModel() }
                }
            )
        }

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

    private fun initApi() {
        coroutineScope.launch {
            launch { DataRepository.fetchWebUrlList() }
            launch { DataRepository.getVideoClass() }
            launch { DataRepository.initWork() }
        }
    }


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setAppLanguage(this)
    }

}