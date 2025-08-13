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
fun WebPathBar(
    currentUrl: String,
    onUrlChanged: (String) -> Unit = {},
    onUrlUpdate:(String) -> Unit = {},
    onRefresh:()-> Unit = {},
    onBack:()-> Unit = {},
    onHome:()-> Unit = {},
    onAd:()-> Unit = {}
){
    val adState = remember { mutableStateOf(true) }
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
            model = R.drawable.icon_back_1,
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
                onUrlUpdate.invoke(it)
                searchText.value = it
            },
            onSearch = {
                searchText.value = it
                onUrlUpdate.invoke(it)
                onUrlChanged.invoke(it)
            },
            onHome = {
                onHome.invoke()
            },
            onRefresh = {
                onRefresh.invoke()
            }
        )

        AsyncImageImpl(
            modifier = Modifier
                .padding(horizontal = 9.dp)
                .size(22.dp)
                .singClick {
                    adState.value = !adState.value
                    onAd.invoke()
                },
            model = if (adState.value) R.drawable.icon_ad else R.drawable.icon_ad_not,
            contentDescription = null
        )
    }
}