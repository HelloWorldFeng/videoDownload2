package com.app.videobox.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.GradientButton
import com.app.videobox.ui.widgets.ModalBottomSheetV3
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.EventReportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onClick:()->Unit
){
    val sheetStateV3 = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) {
        sheetStateV3.show()

        EventReportUtils.reportTDParams("permission_pop_show", params = mutableMapOf(
            "show_type" to "start"
        ), desc = "新通知权限引导弹窗展示")
    }
    val scope = rememberCoroutineScope()
    BackHandler { scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() } }

    ModalBottomSheetV3(
        sheetState = sheetStateV3,
        contentPadding = PaddingValues(),
        onDismissRequest = {
            scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() }
        },
    ){
        Column(Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                AsyncImageImpl(
                    modifier = Modifier
                        .size(24.dp)
                        .singClick {
                            onDismissRequest.invoke()
                        },
                    model = R.drawable.icon_cancel
                )
            }

            Spacer(Modifier.height(30.dp))


            Text(
                text = stringResource(R.string.stay_notified_about_your_downloads),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))

            val lottie by rememberLottieComposition(LottieCompositionSpec.Asset("notify_icon.json"))
            LottieAnimation(
                composition = lottie,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(30.dp))

            Text(
                text = "Turn on notifications for:",
                fontSize = 16.sp,
                color = Color.White
            )
            Column(
                modifier = Modifier.padding(start = 30.dp).align(Alignment.Start),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))
                                ),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("1", color = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.download_completion_alerts),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))
                                ),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("2", color = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.quick_link_detection),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))
                                ),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("3", color = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.trending_video_updates),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

            }

            Spacer(Modifier.height(30.dp))
            GradientButton(
                text = stringResource(R.string.enable_notifications),
                onClick = {
                    onClick.invoke()
                }
            )
        }

    }
}