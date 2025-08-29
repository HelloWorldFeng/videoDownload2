package com.app.videobox.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.app.videobox.ui.widgets.singClick

@Composable
fun FeedBackOkDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit = {} // 确认按钮回调，传递选中的反馈类型
) {
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            Modifier
                .width(325.dp,)
                .wrapContentHeight()
                .background(color = Color(0xFF2E2F30), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("feedback_ok.json"))
            LottieAnimation(
                composition = lottie,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.we_have_received_your_suggestions),
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .singClick{
                        onDismissRequest.invoke()
                    }
                ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ok),
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}