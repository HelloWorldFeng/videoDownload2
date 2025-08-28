package com.app.videobox.ui.widgets

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

@Composable
fun SystemBarTheme() {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = true
    )
    systemUiController.setNavigationBarColor(
        color = Color.Transparent,
        darkIcons = true,
        navigationBarContrastEnforced = false
    )
}

@Composable
fun TitleBar(title:String,onBack:()->Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        CoilImage(modifier = Modifier
            .padding(start = 15.dp)
            .align(Alignment.CenterStart)
            .size(30.dp)
            .singClick {
                onBack.invoke()
            }, data = R.drawable.icon_arrow)

        TextTitle(text = title, fontSize = 18.sp,color = Color.White, modifier = Modifier.align(
            Alignment.Center))
    }
}


@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun Modifier.singClick(
    enabled: Boolean = true,
    delay: Long = 300,
    onClick: () -> Unit
): Modifier = composed {
    var clicked by remember {
        mutableStateOf(!enabled)
    }

    LaunchedEffect(key1 = clicked, block = {
        if (clicked) {
            delay(delay)
            clicked = !clicked
        }
    })

    clickable(
        enabled = if (enabled) !clicked else false,
        onClick = {
            clicked = !clicked
            onClick()
        },
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    )
}
