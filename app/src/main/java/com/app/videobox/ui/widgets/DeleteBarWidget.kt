package com.app.videobox.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R



@Composable
fun DeleteBarWidget(
    modifier: Modifier = Modifier,
    onCancel:()-> Unit,
    onDelete:()-> Unit
){
    Row(
        modifier = modifier
            .width(235.dp)
            .height(73.dp)
            .background(Color(0xFFFFFFFF), shape = RoundedCornerShape(36.dp))
            .padding(all = 5.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).singClick{
            onCancel.invoke()
        }, horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImageImpl(
                modifier = Modifier.size(44.dp),
                model = R.drawable.icon_cancel,
                contentDescription = null
            )
        }
        Column(modifier = Modifier.weight(1f).singClick{
            onDelete.invoke()
        },horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImageImpl(
                modifier = Modifier.size(44.dp),
                model = R.drawable.icon_delete,
                contentDescription = null
            )
        }
    }
}

