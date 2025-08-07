package com.app.videobox.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.request.ImageRequest

@Composable
fun StateAsyncImageImpl(
    model: Any?,
    contentDescription: String?=null,
    modifier: Modifier = Modifier,
    transform: (AsyncImagePainter.State) -> AsyncImagePainter.State =
        AsyncImagePainter.DefaultTransform,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    onSuccessComposable:@Composable BoxScope.() -> Unit = {},
    onLoadingComposable:@Composable BoxScope.()-> Unit = {},
    onErrorComposable:@Composable BoxScope.()-> Unit = {},
){
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var hasSuccess by remember { mutableStateOf(false) }

    Box(modifier = modifier){

        AsyncImageImpl(
            modifier = Modifier.fillMaxSize(),
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = colorFilter,
            transform = transform,
            alignment = alignment,
            alpha = alpha,
            filterQuality = filterQuality,
            onState = {state->
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        isLoading = true
                        hasError = false
                    }

                    is AsyncImagePainter.State.Error -> {
                        isLoading = false
                        hasError = true
                    }

                    is AsyncImagePainter.State.Success -> {
                        isLoading = false
                        hasError = false
                        hasSuccess = true
                    }

                    is AsyncImagePainter.State.Empty -> { }
                }
            }
        )
        if (hasSuccess) {
            onSuccessComposable.invoke(this)
        }

        // 显示加载状态
        if (isLoading) {
            onLoadingComposable.invoke(this)
        }

        // 显示错误状态
        if (hasError) {
            onErrorComposable.invoke(this)
        }
    }




}

@Composable
fun AsyncImageImpl(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    transform: (AsyncImagePainter.State) -> AsyncImagePainter.State =
        AsyncImagePainter.DefaultTransform,
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
) {
    coil.compose.AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(model)
            .decoderFactory(
                VideoFrameDecoder.Factory()
            )
            .crossfade(true).build(),
        contentDescription = contentDescription,
        imageLoader = LocalContext.current.imageLoader,
        modifier = modifier,
        transform = transform,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
        onState = onState,
    )



}