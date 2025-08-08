package com.app.videobox.ui.widgets

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.ext.toDurationText
import com.app.videobox.ext.toFileSizeText
import com.app.videobox.ui.pages.videoDownloadPage.DownloadListViewModel.TaskAction
import com.videodownloader.module.download.Task




@Composable
fun VideoCardV1(
    modifier: Modifier = Modifier,
    viewState: Task.ViewState,
    downloadState: Task.DownloadState,
    progressLinear: @Composable (BoxScope.() -> Unit)? = null,
    isSelectEnabled: () -> Boolean = { false },
    isSelected: () -> Boolean = { false },
    onSelect: () -> Unit = {},
    onClick: (TaskAction) -> Unit,
    onLongClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val containerColor = Color(0xFF2E2F30)
    val contentPadding = PaddingValues(12.dp)

    with(viewState) {
        val thumbnailModel = if (downloadState is Task.DownloadState.Completed) {
            downloadState.filePath
        } else {
            thumbnailUrl
        }

        Card(
            modifier = with(modifier) {
                if (!isSelectEnabled()) {
                    combinedClickable(
                        enabled = true,
                        onClick = {
                            when (downloadState) {
                                is Task.DownloadState.Error, Task.DownloadState.Idle -> {
                                    onClick(TaskAction.Resume)
                                }
                                is Task.DownloadState.Canceled -> {
                                    onClick(TaskAction.Resume)
                                }
                                is Task.DownloadState.Completed -> {
                                    onClick(TaskAction.OpenFile(downloadState.filePath))
                                }
                                is Task.DownloadState.Running -> {
                                    onClick(TaskAction.Cancel)
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
                } else {
                    selectable(selected = isSelected(), onClick = onSelect)
                }
            },
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(101.dp)
                    .padding(contentPadding)
            ) {
                Box(modifier = Modifier.width(127.dp)) {
                    Card(shape = RoundedCornerShape(8.dp)) {
                        CardImage(modifier = Modifier, thumbnailModel = thumbnailModel)
                    }
                    Box(Modifier.align(Alignment.Center)) {
                        progressLinear?.invoke(this)
                    }
                    VideoTimeInfoLabel(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        duration = duration,
                    )

                }

                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        modifier = Modifier, 
                        text = title, 
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Row {
                        val fileSizeText = fileSizeApprox.toFileSizeText()
                        Text(
                            modifier = Modifier,
                            text = fileSizeText,
                            color = Color(0xFF898989)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        AnimatedVisibility(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            visible = isSelectEnabled(),
                        ) {
                            CustomCheckBox(
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 16.dp)
                                    .size(20.dp),
                                checked = isSelected(),
                            )
                        }

                    }

                }
            }
        }
    }
}

@Composable
private fun VideoTimeInfoLabel(modifier: Modifier = Modifier, duration: Int) {
    Surface(
        modifier = modifier.padding(4.dp),
        color = Color.Black.copy(alpha = 0.68f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        val durationText = duration.toDurationText()
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = durationText,
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
                    .aspectRatio(16f / 10f),
            model = thumbnailModel,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
        )
    } else {
        Surface(
            modifier =
                modifier
                    .padding()
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
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

            CustomCircularProgress(
                progress = { downloadState.progress }
            )

        }
        is Task.DownloadState.Canceled -> {
            CustomCircularProgress(
                progress = { 0.01f }
            )
        }
        else -> {
            CustomCircularProgress(
                progress = { 0.5f }
            )
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