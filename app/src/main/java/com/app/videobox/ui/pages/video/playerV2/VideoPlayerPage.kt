package com.app.videobox.ui.pages.video.playerV2

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.app.videobox.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VideoPlayerPage(
    title: String,
    videoUrl: String
){
    val context = LocalContext.current
    var playerController by remember { mutableStateOf<VideoPlayerController?>(null) }
    var isExiting by remember { mutableStateOf(false) }

    // 处理返回键
    BackHandler {
        if (!isExiting) {
            isExiting = true
            // 先停止播放器，然后退出页面
            playerController?.stopAndRelease()
            // 延迟一点时间让播放器停止，然后退出
            CoroutineScope(Dispatchers.Main).launch {
                delay(100) // 给播放器一点时间停止
                if (context is Activity) {
                    MainActivity.start(context)
                }
            }
        }
    }
    
    val yourVideoResultEntity = VideoResultEntity(
        id = 1,
        preview = videoUrl,
        name = title,
        video = videoUrl
    )
    CustomVideoPlayer(
        video = yourVideoResultEntity,
        isVideoEnded = { /* 结束回调 */ },
        onBack = { 
            // 返回按钮处理 - 触发 BackHandler
            if (!isExiting) {
                isExiting = true
                // 先停止播放器，然后退出页面
                playerController?.stopAndRelease()
                // 延迟一点时间让播放器停止，然后退出
                CoroutineScope(Dispatchers.Main).launch {
                    delay(100) // 给播放器一点时间停止
                    if (context is Activity) {
                        MainActivity.start(context)
                    }
                }
            }
        },
        onDownload = { /* 下载回调 */ },
        onProvideController = { controller ->
            playerController = controller
        }
    )
}