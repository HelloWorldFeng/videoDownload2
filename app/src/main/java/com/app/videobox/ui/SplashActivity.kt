package com.app.videobox.ui

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.BuildConfig
import com.app.videobox.R
import com.app.videobox.ad.UmpHelper
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ad.base.AD_TYPE_NAV
import com.app.videobox.ad.base.AD_TYPE_START
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ext.urlInBrowser
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.LinearProgress
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.blankj.utilcode.util.SPStaticUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.ad.AdManager
import com.app.videobox.service.DownloadService
import com.app.videobox.ui.dialogs.NotifyDialog
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
        
        setContent {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1C1D1E))){
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
                    },
                    onClick = {
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
                                    DownloadService.startService(this@SplashActivity)
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
                iterations = LottieConstants.IterateForever,
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
                adScene = "cold_start",
                closeAction = {
                    safeStartActivity(LanguageActivity::class.java)
                    finish()
                })

        }else{
            AdManager.getFullAdFromPool(
                this,
                adType = "open",
                adScene = if (!AppManager.isInitialized) "cold_start" else "hot_start",
                closeAction = {
                    safeStartActivity(MainActivity::class.java)
                    finish()
                })

        }

        AppManager.isInitialized = true
    }

    private fun handleAppLaunch() {
        if (!AppManager.isInitialized) {
            initForColdLaunch()
        } else {
            initForWarmLaunch()
        }
    }

    private fun initForColdLaunch() {
        UmpHelper.requestUmp(this) {
            AdManager.loadAdmobInstance(AD_TYPE_START, AD_TYPE_NAV, AD_TYPE_INT)
        }
    }

    private fun initForWarmLaunch() {
        AdManager.loadAdmobInstance(AD_TYPE_START, AD_TYPE_NAV, AD_TYPE_INT)
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
    }

    object AppManager {
        @Volatile
        var isInitialized = false

        fun reset() {
            isInitialized = false
        }
    }

}

