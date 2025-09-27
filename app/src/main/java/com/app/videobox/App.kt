package com.app.videobox

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Process
import android.util.Base64
import android.util.Log
import cn.thinkingdata.analytics.TDAnalytics
import cn.thinkingdata.analytics.TDConfig
import com.app.videobox.ad.UserHelper
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.network.DataRepository
import com.app.videobox.receiver.HomeKeyReceiver
import com.app.videobox.receiver.PowerDisconnectReceiver
import com.app.videobox.receiver.ScreenOnReceiver
import com.app.videobox.service.DownloadVideoService
import com.app.videobox.ui.GuiderActivity
import com.app.videobox.ui.PrivacyActivity
import com.app.videobox.ui.SplashActivity
import com.app.videobox.ui.pages.homePage.HomeViewModel
import com.app.videobox.ui.pages.video.playerV2.VlcPlayActivity
import com.app.videobox.ui.pages.webViewPage.WebViewModel
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.LanguageUtils.setAppLanguage
import com.app.videobox.utils.NotifyHelper
import com.app.videobox.workManager.WorkManagerScheduler
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.SPStaticUtils
import com.blankj.utilcode.util.Utils
import com.google.android.gms.ads.AdActivity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.uouo.start.AccountKeepsManager
import com.videodownloader.module.api.DlEvtBridge
import com.videodownloader.module.download.DownloaderV2
import com.videodownloader.module.download.DownloaderV2Impl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leolin.shortcutbadger.ShortcutBadger
import org.json.JSONObject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

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

        var firebaseToken = ""
        var isBackground = true
        var backgroundTime = 0L
        var foregroundTime = 0L
        private var beatCount = 0

        var notLaunchHot = false

    }
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG.not()) {
            AccountKeepsManager.getInstance().initialize(this, DownloadVideoService::class.java)
        }

        val isMainProcess = packageName == getCurrentProcessName()
        if (isMainProcess){
            // 初始化语言设置（必须在其他初始化之前）
            com.app.videobox.utils.LanguageUtils.initializeLanguageIfNeeded()
            setAppLanguage(this)
            NotifyHelper.initNotify(this)
            initTdSdk()
            initFirebase()
            initAppSwitchListener()
            initApi()
            UserHelper.initUserInfo()
            initReceiver()
            
            // 初始化WorkManager任务调度器
            initWorkManager()
            
            //依赖注入
            startKoin {
                androidContext(this@App)
                modules(
                    module {
                        single<DownloaderV2> { DownloaderV2Impl(appContext()) }
                        viewModel { WebViewModel() }
                        viewModel { HomeViewModel() }
                    }
                )
            }


            EventReportUtils.reportTDParams("app_open", desc = "冷启动应用")

        }

    }

    private fun getCurrentProcessName(): String {
        val pid = Process.myPid()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningApps = am.runningAppProcesses
        if (runningApps != null) {
            for (processInfo in runningApps) {
                if (processInfo.pid == pid) {
                    return processInfo.processName
                }
            }
        }
        return packageName
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initReceiver() {
        // 注册屏幕点亮广播接收器
        val screenReceiver = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(ScreenOnReceiver(), screenReceiver)

        // 注册电源断开广播接收器
        val powerReceiver = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(PowerDisconnectReceiver(), powerReceiver)

        try {
            // 注册Home键检测广播接收器
            val homeKeyReceiver = HomeKeyReceiver()
            val homeKeyFilter = IntentFilter().apply {
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                // 可以根据需要添加其他相关的系统广播
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(homeKeyReceiver, homeKeyFilter)

            // 启用Home键检测功能
            homeKeyReceiver.enableHomeKeyDetection()

            Log.d("App", "所有广播接收器注册完成: ScreenOnReceiver, PowerDisconnectReceiver, HomeKeyReceiver")

        }catch (e: Exception){

        }
        // 设置下载模块事件监听器
        DlEvtBridge.setListener(object : DlEvtBridge.OnDlEvtListener {
            override fun onMp4DownloadSuccess(taskId: String, outputPaths: List<String>) {
                // 处理下载成功事件
                NotifyHelper.sendDownloadNotify(this@App,outputPaths.first())
                Log.d("AppDownload", "视频下载成功，已发送通知${outputPaths.first()}")
                ShortcutBadger.applyCount(this@App, 1)
            }
        })
    }
    
    private fun initAppSwitchListener() {
        AppUtils.registerAppStatusChangedListener(object : Utils.OnAppStatusChangedListener{
            override fun onForeground(activity: Activity?) {
                isBackground = false
                foregroundTime = System.currentTimeMillis()
                //15秒内不用重新热启动
                if ((foregroundTime / 1000) - (backgroundTime / 1000) < 15) {
                    return
                }

                if (activity is SplashActivity || activity is PrivacyActivity
                    || activity is AdActivity || notLaunchHot
                    || activity is VlcPlayActivity
                    || activity is GuiderActivity
                ) {
                    notLaunchHot = false
                    return
                }
                activity?.startActivity(Intent(activity, SplashActivity::class.java))
            }

            override fun onBackground(activity: Activity?) {
                isBackground = true
                backgroundTime = System.currentTimeMillis()

                val time = (System.currentTimeMillis() - foregroundTime)/1000
                if (time <= 2) {
                    return
                }
                EventReportUtils.reportTDParams(eventName = "user_time", mutableMapOf("time" to time), desc = "用户前台使用时长:${time}")
            }
        })
    }

    
    private fun initTdSdk() {
        val TdId = Base64.decode("NzY5NGRkNGI5M2NjNDYwMGE0MDVhMjM5MDVmMTQ0NzJ4".toByteArray(), Base64.NO_WRAP).decodeToString().replace("x","")

        val config = TDConfig.getInstance(
            this,
            TdId,
            "https://data.junkfiledetector.com"
        )

        TDAnalytics.init(config)
        // 开启自动采集
        TDAnalytics.enableAutoTrack(
            TDAnalytics.TDAutoTrackEventType.APP_START
                    or TDAnalytics.TDAutoTrackEventType.APP_END
                    or TDAnalytics.TDAutoTrackEventType.APP_INSTALL
                    or TDAnalytics.TDAutoTrackEventType.APP_VIEW_SCREEN
                    or TDAnalytics.TDAutoTrackEventType.APP_CLICK
                    or TDAnalytics.TDAutoTrackEventType.APP_CRASH
        )
    }

    private fun initFirebase() {
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            // 获取注册令牌
            firebaseToken = task.result
        }
        RemoteConfigManager.fetchConfig()
    }

    private fun initApi() {
        coroutineScope.launch {
            launch { DataRepository.fetchWebUrlList() }
            launch { DataRepository.getVideoClass() }
            launch { DataRepository.jbLike(this@App,firebaseToken) }
            launch { DataRepository.initWork() }
            launch { DataRepository.checkAdultUserModel() }
            launch { initAppFlyer() }

            launch {
                delay(400L)
                Log.d("通知日志", "拉活通知:${isBackground} ")

                if (isBackground && SPStaticUtils.getBoolean(appLiveBtn, true)) {
                    NotifyHelper.sendApiNotification(this@App,"app_live")
                }
            }
            launch {
                while (true) {
                    //每分钟累计一次
                    delay(60 * 1000)
                    beatCount++
                    if (beatCount % 10 == 0) {
                        EventReportUtils.reportTDParams("beatCount",mutableMapOf("beatCount" to beatCount),"上报心跳")
                    }
                }
            }
        }
    }

    private suspend fun initAppFlyer() = withContext(Dispatchers.Main){
        val conversionDataListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                data?.let {
                    val status = data["af_status"] as? String
                    if (status == "Non-organic") {
                        SPStaticUtils.put("buyUser",true)
                        val sourceID = data["media_source"]
                        val campaign = data["campaign"]
                        val channel = try {
                            data["channel"]
                        }catch (e: Exception){
                            "unknown"
                        }
                        if (sourceID != null && campaign != null) {
                            Log.d(
                                "zzz",
                                "This is a Non-Organic install. Media source: $sourceID  Campaign: $campaign"
                            )
                            var reportType = sourceID as? String ?: ""
                            if (reportType.isEmpty()) {
                                reportType = "unknown"
                            }

                            if (reportType.contains("Facebook Ads")) {
                                UserHelper.channelUser = UserHelper.FacebookUser
                                SPStaticUtils.put(UserHelper.ChannelUser,UserHelper.FacebookUser)
                            }
                            if (reportType.contains("googleadwords_int")) {
                                UserHelper.channelUser = UserHelper.GoogleUser
                                SPStaticUtils.put(UserHelper.ChannelUser,UserHelper.GoogleUser)
                            }

                            TDAnalytics.userSet(
                                JSONObject(
                                    mapOf(
                                        "media_source" to reportType,
                                        "campaign" to campaign,
                                        "channel" to channel,
                                        "af_status" to status
                                    )
                                )
                            )
                        }
                    } else {
                        TDAnalytics.userSet(JSONObject(mapOf("af_status" to status)))
                    }

                    if (!SPStaticUtils.getBoolean("hasReportInstallReferrer", true)) {
                        SPStaticUtils.put("hasReportInstallReferrer", false)
                        val params = mutableMapOf<String, Any>()
                        val sourceID = data["media_source"] as? String ?: "unknown"
                        val campaign = data["campaign"] as? String ?: "unknown"
                        params["media_source"] = sourceID
                        params["campaign"] = campaign

                        TDAnalytics.track("install_referrer", JSONObject(params.toMap()))
                    }


                }

                coroutineScope.launch {
                    try {
                        val sourceID = data?.get("media_source")?:""
                        val campaign = data?.get("campaign")?:""
                        val channel = data?.get("channel")?:""
                        DataRepository.jbLike(this@App,
                            firebaseToken,
                            sourceID.toString(), campaign.toString(), channel.toString()
                        )
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }


            }

            override fun onConversionDataFail(error: String?) {
                Log.e("zzz", "AppsFlyer error onAttributionFailure:  $error")
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                data?.map {
                    Log.d("zzz", "AppsFlyer onAppOpen_attribute: ${it.key} = ${it.value}")
                }
            }

            override fun onAttributionFailure(error: String?) {
                Log.e("zzz", "AppsFlyer, error onAttributionFailure:  $error")
            }
        }

        AppsFlyerLib.getInstance().init(BuildConfig.afKey, conversionDataListener, this@App)
        AppsFlyerLib.getInstance().start(this@App)
    }

    /**
     * 初始化WorkManager任务调度器
     * 
     * 配置并启动30分钟间隔的周期性后台任务
     * 确保应用在后台能够执行必要的维护操作
     */
    private fun initWorkManager() {
        try {
            Log.i("App", "开始初始化WorkManager任务调度器")
            
            // 初始化WorkManager调度器
            val scheduler = WorkManagerScheduler.getInstance()
            scheduler.initialize(this)
            
            // 启动周期性任务 - 默认执行状态检查任务
            scheduler.startPeriodicTask(
                context = this,
                taskType = com.app.videobox.workManager.PeriodicTaskWorker.TASK_TYPE_STATUS_CHECK
            )
            
            Log.i("App", "WorkManager任务调度器初始化完成")
        } catch (e: Exception) {
            Log.e("App", "WorkManager任务调度器初始化失败", e)
        }
    }


    override fun attachBaseContext(base: Context?) {
        // 在attachBaseContext中设置语言
        val context = base?.let { 
            com.app.videobox.utils.LanguageUtils.initializeLanguageIfNeeded()
            com.app.videobox.utils.LanguageUtils.getAttachBaseContext(it) 
        } ?: base
        super.attachBaseContext(context)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setAppLanguage(this)
    }

}