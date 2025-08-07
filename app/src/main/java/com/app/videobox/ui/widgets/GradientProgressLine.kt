package com.app.videobox.ui.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GradientProgressLine(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    height: Int = 10,
    backgroundColor: Color = Color(0x1AFFFFFF), // 半透明白色背景
    gradientColors: List<Color> = listOf(Color(0xFFB14FE6), Color(0xFF7863FF)),
    animationDuration: Int = 300
) {
    val currentProgress by animateFloatAsState(
        targetValue = progress().coerceIn(0f, 1f),
        animationSpec = tween(animationDuration),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape = RoundedCornerShape(18.dp))
            .background(backgroundColor)
    ) {
        // 进度条
        Spacer(
            modifier = Modifier
                .fillMaxWidth(currentProgress)
                .height(height.dp)
                .background(
                    brush = Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(18.dp)
                )
        )
    }
}