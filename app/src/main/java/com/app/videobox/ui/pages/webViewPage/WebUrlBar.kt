package com.app.videobox.ui.pages.webViewPage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.videobox.R
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.ui.widgets.WebUrlInputWidget
import com.app.videobox.ui.widgets.singClick

@Composable
fun WebUrlBar(
    currentUrl: String,
    onUrlChanged: (String) -> Unit = {},
    onRefresh:()-> Unit = {},
    onBack:()-> Unit = {},
    onHome:()-> Unit = {},
    onAd:()-> Unit = {}
){
    val searchText = remember(currentUrl) { mutableStateOf(currentUrl) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImageImpl(
            modifier = Modifier
                .weight(1f)
                .size(34.dp)
                .padding(horizontal = 5.dp)
                .singClick {
                    onBack.invoke()
                },
            model = R.drawable.ic_launcher_background,
            contentDescription = null
        )

        WebUrlInputWidget(
            text = searchText,
            modifier = Modifier
                .weight(9f)
                .height(55.dp)
                .background(
                    color = Color(0x20FFFFFF),
                    shape = RoundedCornerShape(26.dp)
                ),
            onValueChange = {
                searchText.value = it
            },
            onSearch = {
                searchText.value = it
                onUrlChanged.invoke(it)
            },
            onHome = {
                onHome.invoke()
            },
            onAd = {
                onAd.invoke()
            })

        AsyncImageImpl(
            modifier = Modifier
                .weight(1f)
                .size(34.dp)
                .padding(horizontal = 9.dp)
                .singClick {
                    onRefresh.invoke()
                },
            model = R.drawable.ic_launcher_background,
            contentDescription = null
        )
    }
}