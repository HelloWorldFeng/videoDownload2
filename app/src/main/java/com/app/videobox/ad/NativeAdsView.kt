package com.app.videobox.ad

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.app.videobox.ad.base.AD_TYPE_NAV
import com.app.videobox.ad.base.AdUnitWrapper
import kotlinx.coroutines.launch

@Composable
fun NativeAdsView(modifier: Modifier,adScene: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    
    // 状态管理：广告实例和加载状态
    var navAdInstance by remember { mutableStateOf<AdUnitWrapper?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { FrameLayout(it) },
            update = { frameLayout ->
                // 只有在广告已加载且实例存在时才显示
                if (isAdLoaded && navAdInstance != null) {
                    scope.launch {
                        try {
                            navAdInstance?.showAdSmall(context, frameLayout)
                            view.post { view.requestLayout() }
                        } catch (e: Exception) {
                            // 处理广告显示异常
                            isAdLoaded = false
                            navAdInstance = null
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)

        )

        // 生命周期监听：每次 STARTED 时都重新获取广告
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 每次进入 STARTED 状态都重新获取广告
                isLoading = true
                isAdLoaded = false // 重置加载状态
                try {
                    AdManager.getSmallAdFromPool(
                        adScene = adScene,
                        adType = AD_TYPE_NAV
                    ) { adInstance ->
                        navAdInstance = adInstance
                        isAdLoaded = true
                        isLoading = false
                    }
                } catch (e: Exception) {
                    // 处理广告加载失败
                    isLoading = false
                    isAdLoaded = false
                }
            }
        }
    }
}