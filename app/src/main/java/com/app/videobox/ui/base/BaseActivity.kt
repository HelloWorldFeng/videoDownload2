package com.app.videobox.ui.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.SystemBarTheme
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.LanguageUtils.getAttachBaseContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

abstract class BaseActivity : ComponentActivity() {
    private val focusChangeFlow = MutableStateFlow(false)
    private var loadingDialogState = mutableStateOf(value = false)

    fun showLoadingDialog() {
        loadingDialogState.value = true
    }

    fun hideLoadingDialog() {
        loadingDialogState.value = false
    }

    @Composable
    fun LoadingAdDialog() {
        AnimatedVisibility(
            visible = loadingDialogState.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color = Color(0x80000000))
                    .singClick { }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(color = Color(0xB3000000), shape = RoundedCornerShape(18.dp))
                        .padding(20.dp)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(43.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ad loading...",
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 23.dp)
                    )
                }


            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                focusChangeFlow.filter { it }
                hasFocusAfter()
            }
        }
    }

    open fun hasFocusAfter(){}

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focusChangeFlow.value = hasFocus
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { getAttachBaseContext(it) })
    }

    protected fun setContent(
        systemBarTheme: @Composable () -> Unit = {
            SystemBarTheme()
        },
        content: @Composable () -> Unit
    ) {
        setContent(parent = null,
            content = {
                systemBarTheme()

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black)) {
                    content()
                }
            })
    }
}