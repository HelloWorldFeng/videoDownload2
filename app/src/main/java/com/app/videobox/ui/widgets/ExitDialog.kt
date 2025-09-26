package com.app.videobox.ui.widgets

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R
import com.app.videobox.ad.NativeBigAdsView
import com.app.videobox.utils.EventReportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onClick:()->Unit,
){
    val sheetStateV3 = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { sheetStateV3.show() }
    val scope = rememberCoroutineScope()
    BackHandler { scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() } }

    ModalBottomSheetV3(
        sheetState = sheetStateV3,
        contentPadding = PaddingValues(),
        onDismissRequest = {
            scope.launch { sheetStateV3.hide() }.invokeOnCompletion { onDismissRequest() }
        },
    )
    {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = Color(0xFF2E2F30))
                .padding(23.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(R.string.exit_app),
                fontSize = 18.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.thanks_for_using_steam_box),
                color = Color.White,
                fontSize = 14.sp
            )

            Row(modifier = Modifier
                .padding(top = 25.dp)
                .fillMaxWidth()) {
                Box(
                    modifier
                        .weight(1f)
                        .height(53.dp)
                        .background(
                            color = Color(0xFF515253),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .singClick{
                            onDismissRequest.invoke()
                        },
                ){
                    Text(
                        text = stringResource(R.string.not_now),
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(Modifier.width(14.dp))
                GradientButton(
                    modifier = Modifier.weight(1f).height(53.dp),
                    text = stringResource(R.string.exit_app),
                    onClick = onClick
                )
            }

            NativeBigAdsView(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .fillMaxWidth(1f)
                    .background(color = Color.White),
                adScene = "n_exit"
            )

        }

    }
}