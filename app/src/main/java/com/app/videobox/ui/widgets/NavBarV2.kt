package com.app.videobox.ui.widgets

import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions

private val ContainColor : Color
    @Composable get() = Color(0xFF363637)

@Composable
fun NavBarV2(
    modifier: Modifier,
    defaultIndex: Int = 0,
    onClickHome:()-> Unit = {},
    onClickDownload:()-> Unit = {},
    onClickVideo:()-> Unit = {}
) {
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(defaultIndex) }
    Row(
        modifier = modifier
            .padding(horizontal = 30.dp)
            .fillMaxWidth()
            .height(54.dp)
            .background(ContainColor, shape = RoundedCornerShape(26.dp))
            .padding(all = 5.dp)
            .singClick {},
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarIcon(
            modifier = Modifier
                .fillMaxHeight()
                .padding(10.dp),
            onClick = {
                if (XXPermissions.isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)){
                    selected = 0
                    onClickVideo.invoke()
                    return@NavBarIcon
                }
                XXPermissions.with(context).permission(arrayOf(Manifest.permission.READ_MEDIA_VIDEO)).request(object : OnPermissionCallback {
                    override fun onGranted(p0: MutableList<String>, p1: Boolean) {
                        selected = 0
                        onClickVideo.invoke()
                    }

                    override fun onDenied(permissions: List<String?>, doNotAskAgain: Boolean) {
                        super.onDenied(permissions, doNotAskAgain)
                        ToastUtils.showShort("Storage permission denied, Unable to access stored videos")
                    }
                })

            },
            selected = selected == 0,
            defaultIcon = R.drawable.icon_nav_video_not,
            checkedIcon = "tab_video.json"
        )

        NavBarIcon(
            modifier = Modifier,
            onClick = {
                selected = 1
                onClickHome.invoke()
            },
            selected = selected == 1,
            checkedIcon = "tab_web.json"
        )

        NavBarIcon(
            modifier = Modifier
                .fillMaxHeight()
                .padding(10.dp),
            onClick = {
                selected = 2
                onClickDownload.invoke()

            },
            selected = selected == 2,
            defaultIcon = R.drawable.icon_nav_download,
            checkedIcon = "tab_download.json"
        )
    }
}

@Composable
private fun NavBarIcon(
    modifier: Modifier,
    onClick: () -> Unit,
    selected: Boolean = false,
    defaultIcon: Any?=null,
    checkedIcon: Any,
){
    Box(
        modifier = modifier
            .aspectRatio(1f / 1f, matchHeightConstraintsFirst = true)
            .singClick {
                onClick.invoke()
            }
    ) {
        if (defaultIcon == null) {
            val lottie by rememberLottieComposition(LottieCompositionSpec.Asset(checkedIcon.toString()))
            LottieAnimation(
                composition = lottie,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        if (selected) {
            val lottie by rememberLottieComposition(LottieCompositionSpec.Asset(checkedIcon.toString()))
            LottieAnimation(
                composition = lottie,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else {
            AsyncImageImpl(
                modifier = Modifier.fillMaxSize(),
                model = defaultIcon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }

}
