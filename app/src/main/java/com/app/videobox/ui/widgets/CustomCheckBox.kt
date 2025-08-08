package com.app.videobox.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.app.videobox.R

@Composable
fun CustomCheckBox(
    modifier: Modifier,
    checked: Boolean,
){
    //根据checked显示不同UI
    Box(modifier = modifier) {
        if (checked) {
            AsyncImageImpl(
                model = R.drawable.icon_check_ed,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }else{
            AsyncImageImpl(
                model = R.drawable.icon_check_not,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
        }
    }

}