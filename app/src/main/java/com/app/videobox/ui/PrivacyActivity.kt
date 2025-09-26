package com.app.videobox.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.BuildConfig
import com.app.videobox.R
import com.app.videobox.ad.AdManager
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

class PrivacyActivity : BaseActivity() {




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            PrivacyView()
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
                val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("lottie_splash_logo.json"))
                LottieAnimation(
                    composition = lottie,
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.None
                )
                Spacer(modifier = Modifier.height(50.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(57.dp)
                        .background(brush = gradientColor, shape = RoundedCornerShape(35.dp))
                        .singClick {
                            SPStaticUtils.put("firstLaunch", false)
                            this@PrivacyActivity.safeStartActivity(SplashActivity::class.java)
                        }
                ){
                    Text(
                        text = stringResource(R.string.start), fontSize = 18.sp, color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(23.dp))
                Text(text = "By clicking “Start,” you confirm that you have read and",
                    color = Color.White.copy(alpha = 0.9f))
                Row {
                    Text("accept our ",color = Color.White.copy(alpha = 0.9f))
                    Text(text = stringResource(R.string.privacy_policy),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.singClick {
                            context.urlInBrowser(BuildConfig.privacyUrl)
                        })
                    Text(" and ",color = Color.White.copy(alpha = 0.9f))
                    Text(text = stringResource(R.string.terms_of_service),
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.Underline,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.singClick {
                            context.urlInBrowser(BuildConfig.termUrl)
                        })
                }
                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    override fun onBackPressed() {}



    override fun onDestroy() {
        super.onDestroy()
    }



}