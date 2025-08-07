package com.app.videobox.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GradientButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
){
    Box(
        modifier = modifier
            .clickable {
                onClick.invoke()
            }
            .shadow(
                elevation = 8.dp,
                spotColor = Color.White,
                shape = RoundedCornerShape(25.dp)
            )
            .size(300.dp, 50.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFB14FE5), Color(0xFF7864FF))
                ), shape = RoundedCornerShape(25.dp)
            )
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}