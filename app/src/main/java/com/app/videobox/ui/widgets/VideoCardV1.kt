package com.app.videobox.ui.widgets

import android.R.attr.onClick
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.ext.toDurationText
import com.app.videobox.ext.toFileSizeText
import com.videodownloader.module.download.Task

sealed interface UiAction{
    data class OpenFile(val filePath: String?) : UiAction

    data object Cancel : UiAction

    data object Delete : UiAction

    data object Resume : UiAction

}


@Composable
fun VideoCardV1(
    modifier: Modifier = Modifier,
    viewState: Task.ViewState,
    downloadState: Task.DownloadState,
    stateIndicator: @Composable (BoxScope.() -> Unit)? = null,
    actionButton: @Composable (BoxScope.() -> Unit)? = null,
    progressLinear:@Composable (ColumnScope.()-> Unit)? =null,
    isSelectEnabled: () -> Boolean = { false },
    isSelected: () -> Boolean = { false },
    onSelect: () -> Unit = {},
    onClick: (UiAction) -> Unit,
    onLongClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    with(viewState) {
        var mThumbnailUrl = if (downloadState is Task.DownloadState.Completed){
            downloadState.filePath
        }else{
            thumbnailUrl
        }

        VideoCardV1(
            modifier = with(modifier) {
                if (!isSelectEnabled())
                    combinedClickable(
                        enabled = true,
                        onClick = {
                            when (downloadState) {
                                is Task.DownloadState.Error,Task.DownloadState.Idle -> {
                                    onClick(UiAction.Resume)
                                }

                                is Task.DownloadState.Canceled -> {
                                    onClick(UiAction.Resume)
                                }

                                is Task.DownloadState.Completed -> {
                                    onClick(UiAction.OpenFile(downloadState.filePath))
                                }


                                is Task.DownloadState.Running -> {
                                    onClick(UiAction.Cancel)
                                }
                            }
                        },
                        onClickLabel = "",
                        onLongClick = {
                            onLongClick()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onLongClickLabel = "",
                    )
                else selectable(selected = isSelected(), onClick = onSelect)
            },
            thumbnailModel = mThumbnailUrl,
            title = title,
            duration = duration,
            fileSizeApprox = fileSizeApprox,
            actionButton = actionButton,
            progressLinear = progressLinear,
            isSelectEnabled = isSelectEnabled,
            isSelected = isSelected,
        )
    }
}

@Composable
private fun VideoCardV1(
    modifier: Modifier = Modifier,
    thumbnailModel: Any? = null,
    title: String = "",
    duration: Int = 0,
    fileSizeApprox: Double = .0,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    actionButton: @Composable (BoxScope.() -> Unit)? = null,
    progressLinear:@Composable (ColumnScope.() -> Unit)? = null,
    isSelectEnabled: () -> Boolean = { false },
    isSelected: () -> Boolean = { false },
) {
    val containerColor = Color(0x20FFFFFF)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),) {
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterVertically),
                visible = isSelectEnabled(),
            ) {
                CheckBoxV2(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 16.dp)
                        .size(20.dp),
                    checked = isSelected(),
                )
            }

            Box(modifier = Modifier.width(127.dp)){
                Card(shape = RoundedCornerShape(8.dp)) {
                    CardImage(modifier = Modifier, thumbnailModel = thumbnailModel)
                }
                Box(Modifier.align(Alignment.Center)) { actionButton?.invoke(this) }
                VideoInfoLabel(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    duration = duration,
                    fileSizeApprox = fileSizeApprox,
                )
            }

            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(modifier = Modifier, text = title,color = Color.White)
                Spacer(modifier = Modifier.height(20.dp))
                progressLinear?.invoke(this)
            }
        }
    }
}

@Composable
private fun VideoInfoLabel(modifier: Modifier = Modifier, duration: Int, fileSizeApprox: Double) {
    Surface(
        modifier = modifier.padding(4.dp),
        color = Color.Black.copy(alpha = 0.68f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        val fileSizeText = fileSizeApprox.toFileSizeText()
        val durationText = duration.toDurationText()
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = "$fileSizeText  $durationText",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun CardImage(modifier: Modifier = Modifier, thumbnailModel: Any? = null) {
    if (thumbnailModel != null) {
        AsyncImageImpl(
            modifier =
                modifier
                    .padding()
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true),
            model = thumbnailModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
    } else {
        Surface(
            modifier =
                modifier
                    .padding()
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {}
    }
}

@Composable
fun ProgressLinear(
    modifier: Modifier = Modifier,
    downloadState: Task.DownloadState,
){

    return when(downloadState){
        is Task.DownloadState.Running -> {
            Log.d("ProgressLinear", "下载中 progress:${downloadState.progress},speed->:${downloadState.speed} ")

            ProgressLinear(
                modifier = modifier,
                progress = downloadState.progress,
                downloadState.speed
            )
        }
        is Task.DownloadState.Canceled -> {
            ProgressLinear(
                modifier = modifier,
                progress = 0.1f
            )
        }
        else -> {

        }
    }
}

@Composable
private fun ProgressLinear(modifier: Modifier = Modifier, progress: Float,speedText: String?=null) {
    val animatedProgress by
    animateFloatAsState(
        progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress",
    )

    Column(
        modifier = modifier
    ) {
        GradientProgressLine(
            progress = { animatedProgress },
            modifier = Modifier
                .padding(bottom = 5.dp)
                .fillMaxWidth()
                .height(3.dp)
        )

        speedText?.let {
            var mProgress = (progress * 100).toInt()
            if (mProgress<0){
                mProgress = 0
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(it, fontSize = 12.sp,color = Color.White)
                Text("${mProgress}%",fontSize = 12.sp, color = Color.White)
            }
        }

    }
}