package com.app.videobox.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.GradientButton
import com.app.videobox.ui.widgets.ModalBottomSheetV3
import com.app.videobox.ui.widgets.singClick
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyHomeDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onClick:()->Unit
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
    ){
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                AsyncImageImpl(
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(24.dp)
                        .singClick {
                            onDismissRequest.invoke()
                        },
                    model = R.drawable.icon_cancel
                )
            }
            Column(Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally)
            {


                Spacer(Modifier.height(30.dp))
                Text(
                    text = stringResource(R.string.permission_required),
                    fontSize = 18.sp,
                    color = Color.White)
                Spacer(Modifier.height(20.dp))

                AsyncImageImpl(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.3f),
                    model = R.drawable.icon_notify_home
                )
                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.to_downlaod_files_you_need_to),
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(28.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))
                                ),
                                shape = CircleShape
                            )
                    ){
                        Text("1", modifier = Modifier.align(Alignment.Center),color = Color.White)
                    }

                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.open_settings_tap_permission),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Start)
                )
                {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFFFF5A83), Color(0xFFFF7B29))
                                ),
                                shape = CircleShape
                            )
                    ){
                        Text("2", modifier = Modifier.align(Alignment.Center), color = Color.White)
                    }

                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.turn_on_notification),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(20.dp))

                GradientButton(
                    text = stringResource(R.string.update_settings),
                    onClick = {
                        onClick.invoke()
                    }
                )
            }

        }


    }
}