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
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ui.MainActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VlcPlayerPage(
    title: String,
    videoUrl: String
){
    val context = LocalContext.current as Activity
    var playerController by remember { mutableStateOf<VideoPlayerController?>(null) }
    var isExiting by remember { mutableStateOf(false) }

    val backAction = {
        if (!isExiting) {
            isExiting = true
            // 先停止播放器，然后退出页面
            playerController?.stopAndRelease()
            MainActivity.start(context)
        }
    }
    // 处理返回键
    BackHandler {
        AdManager.getFullAdFromPool(
            context,
            adType = AD_TYPE_INT,
            adScene = "i_video_player_close",
            closeAction = {
                backAction.invoke()
            })

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
            backAction.invoke()
        },
        onDownload = { /* 下载回调 */ },
        onProvideController = { controller ->
            playerController = controller
        }
    )
}