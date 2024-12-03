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
import com.app.videobox.ad.AdmobManager
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
import com.app.videobox.ad.base.AdPlaceTag

class SplashActivity : BaseActivity() {

    private var showSplashState by mutableStateOf(
        value = SPStaticUtils.getBoolean("firstLaunch",false)
    )
    private var launchTime = if(BuildConfig.DEBUG) 5 else SPStaticUtils.getInt("launchTime",10)
    private var startPlay = mutableStateOf(value = false)

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            handleAppLaunch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        
        setContent {
            Box(modifier = Modifier.fillMaxWidth()){
                CoilImage(
                    modifier = Modifier.fillMaxWidth(),
                    data = R.drawable.bg_splash,
                    contentScale = ContentScale.FillWidth
                )
                Box(modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x1A000000),
                                Color(0xFF000000),
                            )
                        )
                    ))
            }

            if (showSplashState) {
                SplashView()
            }else{
                PrivacyView()
            }
        }
    }

    @Composable
    fun SplashView() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(6f))
            CoilImage(modifier = Modifier.size(100.dp), data = R.mipmap.icon_logo)
            Spacer(modifier = Modifier.height(24.dp))
            TextTitle(text = stringResource(id = R.string.app_name), fontSize = 38.sp,color = Color.White)
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

    @Composable
    fun PrivacyView() {
        val context = LocalContext.current
        Box(modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CoilImage(modifier = Modifier.size(100.dp), data = R.mipmap.icon_logo)
                Spacer(modifier = Modifier.height(24.dp))
                TextTitle(text = stringResource(id = R.string.app_name),color = Color.White, fontSize = 34.sp)
                Spacer(modifier = Modifier.height(50.dp))
                Box(
                    modifier = Modifier

                        .fillMaxWidth(0.9f)
                        .height(57.dp)
                        .background(brush = gradientColor, shape = RoundedCornerShape(35.dp))
                        .singClick {
                            showSplashState = true
                            SPStaticUtils.put("firstLaunch", true)
                        }
                ){
                    Text(
                        text = "Continue", fontSize = 18.sp, color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Spacer(modifier = Modifier.height(23.dp))
                Text(text = "Privacy Policy ",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.singClick {
                        context.urlInBrowser(BuildConfig.privacyUrl)
                    })
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Terms of Service ",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.singClick {
                        context.urlInBrowser(BuildConfig.termUrl)
                    })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    override fun onBackPressed() {}

    private fun navNextStep() {
        if (SPStaticUtils.getBoolean("chooseLanguage", true)) {
            AdmobManager.getFullAdFromPool(
                this,
                adType = "open",
                adScene = "cold_start",
                closeAction = {
                    safeStartActivity(LanguageActivity::class.java)
                    finish()
                })

        }else{
            AdmobManager.getFullAdFromPool(
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
            startPlay.value = true
            AdmobManager.loadAdmobInstance(AD_TYPE_START, AD_TYPE_NAV, AD_TYPE_INT)
        }
    }

    private fun initForWarmLaunch() {
        startPlay.value = true
        AdmobManager.loadAdmobInstance(AD_TYPE_START, AD_TYPE_NAV, AD_TYPE_INT)
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

