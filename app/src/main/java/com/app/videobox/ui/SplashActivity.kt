package com.app.videobox.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.app.videobox.BuildConfig
import com.app.videobox.R
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ad.base.AD_TYPE_NAV
import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.LinearProgress
import com.blankj.utilcode.util.SPStaticUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.MAIN_OPERATE
import com.app.videobox.MAIN_SHOW_VIDEO
import com.app.videobox.MAIN_SHOW_WEB
import com.app.videobox.NOTIFY_TYPE
import com.app.videobox.NOTIFY_TYPE_CUSTOM
import com.app.videobox.NOTIFY_TYPE_DOWNLOAD
import com.app.videobox.NOTIFY_TYPE_FOREGROUND
import com.app.videobox.ad.AdManager
import com.app.videobox.service.DownloadVideoService
import com.app.videobox.ui.dialogs.NotifyDialog
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.NotifyHelper
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

class SplashActivity : BaseActivity() {

    companion object{
        var interType = "normal"
    }

    private var launchTime = if(BuildConfig.DEBUG) 1 else SPStaticUtils.getInt("launchTime",10)
    private var startPlay = mutableStateOf(value = false)
    // 控制 NotifyDialog 是否显示的状态
    private var showNotifyDialog by mutableStateOf(false)

    private var videoUrl = ""
    private var videoTitle = ""
    private var imageUrl = ""
    private var pageType = 0    //4-网页类型 其他都是视频类型


    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            if (XXPermissions.isGranted(this@SplashActivity,
                    Permission.POST_NOTIFICATIONS).not()) {
                //没有通知权限-展示自定义样式弹窗
                showNotifyDialog = true
            }
            else{
                //有通知权限-不展示弹窗
                showNotifyDialog = false
                startPlay.value = true
            }

            handleAppLaunch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SPStaticUtils.getBoolean("firstLaunch",true)){
            this.safeStartActivity(PrivacyActivity::class.java)
            return
        }


        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        acceptIntent(intent)
        setContent {
            BackHandler {  }
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1D1E))){
                CoilImage(
                    modifier = Modifier.fillMaxWidth(),
                    data = R.drawable.bg_splash,
                    contentScale = ContentScale.FillWidth
                )
            }

            SplashView()

            if (showNotifyDialog) {
                NotifyDialog(
                    onDismissRequest = {
                        showNotifyDialog = false
                        startPlay.value = true

                        EventReportUtils.reportTDParams("permission_pop_click", params = mutableMapOf(
                            "click_type" to "start",
                            "action" to "close"
                        ), desc = "新通知权限引导弹窗点击")
                    },
                    onClick = {
                        EventReportUtils.reportTDParams("permission_pop_click", params = mutableMapOf(
                            "click_type" to "start",
                            "action" to "grant"
                        ), desc = "新通知权限引导弹窗点击")

                        showNotifyDialog = false
                        XXPermissions
                            .with(this)
                            .permission(Permission.POST_NOTIFICATIONS)
                            .request(object : OnPermissionCallback{
                                override fun onGranted(
                                    permissions: List<String?>,
                                    allGranted: Boolean
                                ) {
                                    // 权限获取后启动 LaunchedEffect
                                    startPlay.value = true
                                    DownloadVideoService.startService(this@SplashActivity)
                                }

                                override fun onDenied(permissions: List<String?>, doNotAskAgain: Boolean) {
                                    super.onDenied(permissions, doNotAskAgain)
                                    // 即使权限被拒绝也启动 LaunchedEffect
                                    startPlay.value = true
                                }
                            })
                    }
                )
            }
        }
    }

    @Composable
    fun SplashView() {
        val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("lottie_splash_logo.json"))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(6f))
            LottieAnimation(
                composition = lottie,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.None
            )
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.weight(1f))
            LinearProgress(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(5.dp),
                startPlay = startPlay.value,
                launchTime = launchTime
            ) {
                navNextStep()
            }
            Spacer(modifier = Modifier.weight(2f))
        }

    }

    override fun onBackPressed() {}

    private fun navNextStep() {
        if (SPStaticUtils.getBoolean("chooseLanguage", true)) {
            AdManager.getFullAdFromPool(
                this,
                adType = "open",
                adScene = "oa_cold_start",
                closeAction = {
                    this.safeStartActivity(LanguageActivity::class.java)
                })

        }else{
            AdManager.getFullAdFromPool(
                this,
                adType = "open",
                adScene = if (!AppManager.isInitialized) "oa_cold_start" else "oa_hot_launch",
                closeAction = {
                    goNextType()
                })

        }

        AppManager.isInitialized = true
    }

    private fun goNextType() {
        if (interType == "media") {
            val operateType = if (pageType == 4) MAIN_SHOW_WEB else MAIN_SHOW_VIDEO
            val extras = Bundle().apply {
                putString(MAIN_OPERATE, operateType)
                putString("videoUrl", videoUrl)
                putString("videoTitle", videoTitle)
                putString("imageUrl", imageUrl)
            }
            MainActivity.start(this, extras = extras)
            finish()
            return
        }

        if (interType == "action_url"){
            val extras = Bundle().apply {
                putString(MAIN_OPERATE, MAIN_SHOW_WEB)
                putString("videoUrl", videoUrl)
            }
            MainActivity.start(this, extras = extras)
            finish()
            return
        }
        MainActivity.start(this)
        finish()
    }


    private fun handleAppLaunch() {

        AdManager.loadAdmobInstance(AD_TYPE_START, AD_TYPE_NAV, AD_TYPE_INT)

    }



    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
    }

    object AppManager {
        @Volatile
        var isInitialized = false

    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptIntent(intent)
    }
    private fun acceptIntent(intent: Intent) {
        when(intent.action){
            Intent.ACTION_VIEW -> {
                // 处理从其他应用打开的链接
                intent.data?.let { uri ->
                    videoUrl = uri.toString()
                    interType = "action_url"
                }
            }

            Intent.ACTION_SEND -> {
                // 处理从其他应用分享的链接
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                    // 尝试从分享的文本中提取 URL
                    fun extractUrlFromText(text: String): String {
                        // 简单的 URL 提取逻辑
                        val urlPattern = Regex("https?://[^\\s]+")
                        return urlPattern.find(text)?.value ?: ""
                    }
                    val url = extractUrlFromText(sharedText)
                    if (url.isNotEmpty()) {
                        interType = "action_url"
                        videoUrl = url
                    }
                }
            }

            else -> {
                val notifyType = intent.getIntExtra(NOTIFY_TYPE, -1)
                Log.d("TestLog", "进入启动页的类型:${notifyType} ")

                if (notifyType == -1) {
                    EventReportUtils.reportTDParams("enter_start", params = mutableMapOf("type" to "active"), desc = "进入启动页->主动点击")
                    return
                }

                val notificationId = intent.getIntExtra("notificationId", 1)
                NotifyHelper.clearNotification(notificationId)

                interType = when (notifyType) {
                    NOTIFY_TYPE_DOWNLOAD -> {
                        EventReportUtils.reportTDParams("push_click", mutableMapOf<String, Any>().apply {
                            put("push_scene", "download")
                        })
                        EventReportUtils.reportTDParams("enter_start", params = mutableMapOf("type" to "download"), desc = "进入启动页->download")
                        "download"
                    }

                    NOTIFY_TYPE_CUSTOM -> {
                        val scene = intent.getStringExtra("scene")?:""
                        videoUrl = intent.getStringExtra("videoUrl") ?: ""
                        videoTitle = intent.getStringExtra("videoTitle") ?: ""
                        imageUrl = intent.getStringExtra("imageUrl") ?: ""
                        pageType = intent.getIntExtra("pageType", 0)

                        EventReportUtils.reportTDParams("push_click", mutableMapOf<String, Any>().apply {
                            put("push_scene", scene)
                            put("content", videoTitle)
                            put("videoUrl",videoUrl)
                            put("pageType",if (pageType == 4) "webUrl" else "video")
                        })
                        EventReportUtils.reportTDParams("enter_start", params = mutableMapOf("type" to "media"), desc = "进入启动页->media")
                        "media"
                    }

                    NOTIFY_TYPE_FOREGROUND -> {
                        EventReportUtils.reportTDParams("permanent_click", desc = "常驻通知栏点击")
                        EventReportUtils.reportTDParams("enter_start", params = mutableMapOf("type" to "permanent"), desc = "进入启动页->permanent")
                        "permanent" // 前台服务通知
                    }
                    else -> {
                        EventReportUtils.reportTDParams("enter_start", params = mutableMapOf("type" to "normal"), desc = "进入启动页->normal")
                        "normal"
                    }
                }
            }
        }

    }

}

