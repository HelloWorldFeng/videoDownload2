package com.app.videobox.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick

@Composable
fun MoreDialog(
    isVisible: Boolean,
    onDelete: () -> Unit,
    onShare:()->Unit,
    onDismiss: () -> Unit,
) {

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .singClick {
                    onDismiss.invoke()
                }
                .fillMaxSize()
                .background(color = Color(0x99000000))
        )
    }
    AnimatedSlideFormBottom(isVisible, onDismiss = {
        onDismiss.invoke()
    })
    {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.background(
                    color = Color(0xFF2E2F30),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .singClick {
                        onShare.invoke()
                        onDismiss.invoke()
                    }){
                    Text(
                        text = stringResource(R.string.share), fontSize = 16.sp, color = Color.White, modifier = Modifier.align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(55.dp)
                        .background(
                            brush = gradientColor,
                            shape = RoundedCornerShape(30.dp)
                        )
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }


        }
    }

}




@Composable
fun LoadingDialog(isVisible: Boolean,) {
    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0x4D000000))
            .singClick { }){

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(color = Color(0xE6000000), shape = RoundedCornerShape(18.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RotatingCircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Loading", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun RotatingCircularProgressIndicator() {
    // 创建一个 Animatable 对象来控制旋转角度
    val rotation = remember { Animatable(0f) }

    // 使用 LaunchedEffect 来启动动画
    LaunchedEffect(Unit) {
        // 无限循环旋转动画
        while (true) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    CircularProgressIndicator(
        progress = 0.5f,
        modifier = Modifier
            .graphicsLayer(rotationZ = rotation.value), // 应用旋转效果
        color = Color(0xFFFF7B29), // 自定义颜色
        strokeWidth = 4.dp // 自定义宽度
    )
}

@Composable
fun AnimatedSlideFormBottom(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = {
                it
            },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = 100
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = {
                it
            },
            animationSpec = tween(
                durationMillis = 500,
                delayMillis = 100
            )
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = { onDismiss.invoke() },
                    // 去除点击效果
                    indication = null,
                    interactionSource = remember {
                        MutableInteractionSource()
                    })
        ) {
            Box(
                modifier = Modifier
                    .imePadding()
                    .align(Alignment.BottomCenter)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                // 检测手指滑动事件
                                if (dragAmount.y > 0) {
                                    // 手指向上滑动
                                    // 在这里实现你的逻辑
                                    onDismiss.invoke()
                                }
                            })
                    }
            ){
                content()
            }
        }
    }
}
