package com.app.videobox.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomCircularProgress(
    progress: () -> Float,
){
    // 将进度转换为整数百分比，避免显示小数点
    val progressText = (progress.invoke() * 100).toInt().toString() + "%"
    Box(
        Modifier.size(45.dp)
    ){
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier
                .size(45.dp),
            color = Color.Red,
            gapSize = 0.dp,
            trackColor = Color.Transparent,
        )
        Text(progressText, color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.Center))
    }

}