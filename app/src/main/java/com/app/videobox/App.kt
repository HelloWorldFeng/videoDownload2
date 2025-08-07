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
import com.app.videobox.ui.pages.webViewPage.WebViewModel
import com.app.videobox.utils.LanguageUtils.setAppLanguage
import com.appsflyer.AppsFlyerLib
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.blankj.utilcode.util.Utils.OnAppStatusChangedListener
import com.collect.CollectManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.videodownloader.module.api.VideoDownloaderApi
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
        initVideoDownloader()
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
    
    /**
     * 初始化视频下载模块
     */
    private fun initVideoDownloader() {
        try {
            VideoDownloaderApi.initialize(this) {
                // 设置下载路径到应用的外部文件目录
                downloadPath = getExternalFilesDir("Downloads")?.absolutePath 
                    ?: File(filesDir, "Downloads").absolutePath
                // 设置最大并发下载数
                maxConcurrentDownloads = 3
                // 启用断点续传
                enableResumeDownload = true
                // 启用下载通知
                enableDownloadNotification = true
                // 设置网络超时
                networkTimeoutMs = 30000
                // 设置最大重试次数
                maxRetryCount = 3
            }
            Log.d("App", "视频下载模块初始化成功")
        } catch (e: Exception) {
            Log.e("App", "视频下载模块初始化失败", e)
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