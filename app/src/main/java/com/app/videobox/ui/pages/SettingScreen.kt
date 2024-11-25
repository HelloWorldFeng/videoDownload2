package com.app.videobox.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.app.videobox.R
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.TextTitle

class SettingScreen:Screen {
    @Composable
    override fun Content() {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            CoilImage(modifier = Modifier
                .padding(11.dp)
                .align(Alignment.Start)
                .size(30.dp), data = R.drawable.icon_arrow)
            Spacer(modifier = Modifier.height(9.dp))

            CoilImage(modifier = Modifier.size(80.dp), data = R.drawable.ic_launcher_background)
            Spacer(modifier = Modifier.height(11.dp))
            TextTitle(text = stringResource(id = R.string.app_name), fontSize = 22.sp,color = Color.White)
            Spacer(modifier = Modifier.height(35.dp))


            ItemView(R.drawable.icon_small_language, stringResource(id = R.string.language), onClick = {})
            ItemView(R.drawable.icon_small_rate, stringResource(R.string.rate_us), onClick = {})
            ItemView(R.drawable.icon_small_share, stringResource(R.string.share_the_app), onClick = {})
            ItemView(R.drawable.icon_small_privacy, stringResource(R.string.privacy_policy), onClick = {})
            ItemView(R.drawable.icon_small_version, stringResource(R.string.version_update), onClick = {})
        }
    }

    @Composable
    fun ItemView(icon:Any,title:String,onClick:()->Unit) {
        Row(
            Modifier
                .padding(bottom = 14.dp)
                .fillMaxWidth(0.9f)
                .height(62.dp)
                .background(color = Color(0xFF2E2F30), shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(modifier = Modifier.size(20.dp), data = icon)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title,fontSize = 15.sp,color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            CoilImage(modifier = Modifier.size(18.dp), data = R.drawable.icon_arrow_right)

        }
    }
}