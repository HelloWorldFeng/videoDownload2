package com.app.videobox.ui

import android.os.Bundle
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
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ext.urlInBrowser
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.LinearProgress
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.blankj.utilcode.util.SPStaticUtils

class SplashActivity : BaseActivity() {

    private var showSplashState by mutableStateOf(
        value = SPStaticUtils.getBoolean("firstLaunch",false)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(modifier = Modifier.fillMaxWidth()){
                CoilImage(
                    modifier = Modifier.fillMaxWidth(),
                    data = R.drawable.bg_splash,
                    contentScale = ContentScale.FillWidth
                )
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
                    .height(5.dp)
            ) {
                if (SPStaticUtils.getBoolean("chooseLanguage", true)) {
                    safeStartActivity(LanguageActivity::class.java)
                }else{
                    safeStartActivity(MainActivity::class.java)
                }
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

}