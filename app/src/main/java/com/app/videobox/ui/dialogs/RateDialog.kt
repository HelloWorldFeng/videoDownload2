package com.app.videobox.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.singClick

@Composable
fun RateDialog(
    onDismissRequest:()-> Unit,
    onClick:()-> Unit
){
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            Modifier
                .width(325.dp)
                .wrapContentHeight()
                .background(color = Color(0xFF2E2F30), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 34.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("five_rate.json"))
            LottieAnimation(
                composition = lottie,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.your_feedback_makes_us_better),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.enjoying_your_experience_so_far),
                color = Color.White,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(41.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier
                    .singClick{
                        onDismissRequest.invoke()
                    }
                    .size(97.dp,53.dp)
                    .background(color = Color(0xFF515253), shape = RoundedCornerShape(26.dp))){

                    AsyncImageImpl(
                        modifier = Modifier.align(Alignment.Center).size(26.dp,23.dp),
                        model = R.drawable.icon_like_no
                    )
                }

                Box(Modifier
                    .singClick{
                        onClick.invoke()
                    }
                    .size(97.dp,53.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))
                        ),
                        shape = RoundedCornerShape(26.dp))){
                    AsyncImageImpl(
                        modifier = Modifier.align(Alignment.Center).size(26.dp,23.dp),
                        model = R.drawable.icon_like
                    )
                }
            }
        }
    }
}