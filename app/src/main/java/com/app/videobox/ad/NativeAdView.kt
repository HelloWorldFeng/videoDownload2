package com.app.videobox.ad

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.app.videobox.ad.base.AdUnitWrapper
import kotlinx.coroutines.launch

@Composable
fun NativeAdsView(adUnitWrapper: AdUnitWrapper?, modifier: Modifier,bigStyle:Boolean = true) {
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val recompose = currentRecomposeScope
    var isAdShown by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { FrameLayout(it) },
            update = { frameLayout ->
                if (!isAdShown) {
                    scope.launch {
                        adUnitWrapper?.showSmallAd(context, frameLayout,bigStyle)
                        view.post { view.requestLayout() }
                    }
                }else{
                    isAdShown = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        )

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recompose.invalidate()
            }
        }
    }

}

@Composable
fun NativeAdPlace() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(color = Color(0xFFE2E4E6)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(18.dp))
        Spacer(
            modifier = Modifier
                .size(65.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = Color(0xFFD1D2D4))

        )
        Spacer(modifier = Modifier.width(25.dp))
        Column {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp)
                    .clip(shape = RoundedCornerShape(12.dp))
                    .background(color = Color(0xFFD1D2D4))

            )
            Spacer(modifier = Modifier.height(14.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .clip(shape = RoundedCornerShape(12.dp))
                    .background(color = Color(0xFFD1D2D4))

            )
        }
    }
}

