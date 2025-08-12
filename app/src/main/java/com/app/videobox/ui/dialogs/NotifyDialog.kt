package com.app.videobox.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
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
            AsyncImageImpl(
                modifier = Modifier.size(196.dp,73.dp),
                model = R.drawable.icon_notify
            )
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.why_do_we_need_permission),
                fontSize = 18.sp,
                color = Color.White)
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.to_keep_an_eye_on_your_download_progress_in_your_notifications),
                fontSize = 14.sp,
                color = Color(0xFFFFFFFF).copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp)
            )
            Spacer(Modifier.height(35.dp))
            GradientButton(
                text = stringResource(R.string.grant),
                onClick = {
                    onClick.invoke()
                }
            )
        }

    }
}