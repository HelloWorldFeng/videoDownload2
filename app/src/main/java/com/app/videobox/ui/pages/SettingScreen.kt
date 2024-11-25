package com.app.videobox.ui.pages

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
            CoilImage(modifier = Modifier.size(30.dp), data = R.drawable.ic_launcher_background)


            CoilImage(modifier = Modifier.size(50.dp), data = R.drawable.ic_launcher_background)
            TextTitle(text = stringResource(id = R.string.app_name))

            Spacer(modifier = Modifier.height(30.dp))

            ItemView(R.drawable.ic_launcher_background,"Language", onClick = {})

        }
    }

    @Composable
    fun ItemView(icon:Any,title:String,onClick:()->Unit) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(modifier = Modifier.size(30.dp), data = R.drawable.ic_launcher_background)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title)
            Spacer(modifier = Modifier.weight(1f))
            CoilImage(modifier = Modifier.size(10.dp), data = R.drawable.ic_launcher_background)
        }
    }
}