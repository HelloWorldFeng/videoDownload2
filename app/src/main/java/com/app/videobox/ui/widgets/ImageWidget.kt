package com.app.videobox.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.app.videobox.App
import com.app.videobox.R

@Composable
fun CoilImage(
    modifier: Modifier,
    data: Any,
    contentScale: ContentScale = ContentScale.Crop,
    filterQuality: FilterQuality = FilterQuality.High,
    backgroundColor: Color = Color.Gray.copy(alpha = 0f),
    contentDescription:String ?= null,
    radius: Shape = RoundedCornerShape(0.dp)
) {
    AsyncImage(
        modifier = modifier
            .background(color = backgroundColor)
            .clip(shape = radius),
        model = ImageRequest.Builder(App.appContext())
            .data(data)
            .decoderFactory(
                VideoFrameDecoder.Factory()
            )
            .crossfade(true)
            .build(),
        contentScale = contentScale,
        filterQuality = filterQuality,
        contentDescription = contentDescription,
    )
}