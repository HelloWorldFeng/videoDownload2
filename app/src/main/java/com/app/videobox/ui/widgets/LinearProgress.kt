package com.app.videobox.ui.widgets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.videobox.ad.AdManager
import kotlinx.coroutines.delay

@Composable
fun LinearProgress(
    modifier: Modifier,
    launchTime: Int = 10,
    startPlay:Boolean = true,
    bgColor: Color = Color.White,
    solidColor:Color = Color.Black,
    finishBlock: () -> Unit
) {
    var mLaunchTime = remember {
        launchTime
    }
    var progressLinear by remember {
        mutableFloatStateOf(0.00f)
    }
    var boolean = remember {
        true
    }

    if (startPlay) {
        LaunchedEffect(Unit) {
            for (i in 0..99) {
                progressLinear += 0.01f
                delay(mLaunchTime * 10L)
                if (AdManager.canSpeedAnim() && boolean) {
                    boolean = false
                    mLaunchTime = 1
                    Log.d("AdLog", "开始加速 ")
                }
            }
            finishBlock.invoke()
        }
    }


    Box(
        modifier = Modifier
            .then(modifier)
            .background(color = bgColor, shape = RoundedCornerShape(18.dp))
    ) {
        // 进度条
        Spacer(
            modifier = Modifier
                .fillMaxWidth(progressLinear)
                .height(10.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))),
                    shape = RoundedCornerShape(10.dp)
                )
        )
    }
}
