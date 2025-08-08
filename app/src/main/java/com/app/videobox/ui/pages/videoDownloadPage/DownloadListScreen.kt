package com.app.videobox.ui.pages.videoDownloadPage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.pages.video.VideoPlayerManager
import com.videodownloader.module.api.*
import com.app.videobox.ui.pages.video.player.VlcPlayerActivity
import com.app.videobox.ui.widgets.DeleteBarWidget
import com.app.videobox.ui.widgets.ProgressLinear

import com.app.videobox.ui.widgets.VideoCardV1
import com.app.videobox.ui.widgets.singClick
import com.videodownloader.module.download.DownloaderV2
import com.videodownloader.module.download.Task
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import java.io.File

/**
 * 视频下载列表页面
 * 遵循标准MVI架构模式，通过ViewModel管理状态和业务逻辑
 */


/**
 * 下载列表主页面
 * 使用 DownloadListViewModel 进行状态管理
 * 
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    modifier: Modifier = Modifier,
    downloader: DownloaderV2 = koinInject(),
    viewModel: DownloadListViewModel = viewModel(),
) {
    val context = LocalContext.current
    
    // ViewModel状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 处理副作用事件
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is DownloadListViewModel.Effect.ShowToast -> {
                    // 显示Toast消息
                }
                is DownloadListViewModel.Effect.NavigateToPlayer -> {
                    VlcPlayerActivity.start(
                        context = context,
                        videoPath = effect.filePath,
                    )
                }
                is DownloadListViewModel.Effect.ShowError -> {
                    // 显示错误消息
                }
                is DownloadListViewModel.Effect.ScrollToTop -> {
                    // 处理滚动到顶部的逻辑
                }
            }
        }
    }
    
    TaskListContent(
        taskDownloadStateMap = downloader.getTaskStateMap(),
        uiState = uiState,
        viewModel = viewModel,
        selectedCallback = { state ->
            if (state) {
                viewModel.handleIntent(DownloadListViewModel.Intent.DisableSelectMode)
            } else {
                viewModel.handleIntent(DownloadListViewModel.Intent.EnableSelectMode)
            }
        },
        onActionPost = { task, action ->
            when (action) {
                is DownloadListViewModel.TaskAction.Cancel -> downloader.cancel(task)
                is DownloadListViewModel.TaskAction.Delete -> downloader.remove(task)
                is DownloadListViewModel.TaskAction.Resume -> downloader.restart(task)
                is DownloadListViewModel.TaskAction.Pause -> downloader.cancel(task) // 使用cancel代替pause
                is DownloadListViewModel.TaskAction.Retry -> downloader.restart(task)
                is DownloadListViewModel.TaskAction.OpenFile -> {
                    if (action.filePath != null) {
                        viewModel.handleIntent(
                            DownloadListViewModel.Intent.ExecuteTaskAction(task, action)
                        )
                    }
                }
            }
        },
        onTaskSelection = { task ->
            viewModel.handleIntent(
                DownloadListViewModel.Intent.ToggleTaskSelection(task)
            )
        },
        onBatchAction = { action ->
            viewModel.handleIntent(
                DownloadListViewModel.Intent.ExecuteBatchAction(action)
            )
        }
    )
}



/**
 * 任务列表内容组件
 * 使用LazyColumn显示下载任务列表，提供更好的垂直滚动体验
 * 遵循Material Design 3设计规范，支持选择模式和批量操作
 */
@Composable
private fun TaskListContent(
    taskDownloadStateMap: SnapshotStateMap<Task, Task.State>,
    uiState: DownloadListViewModel.UiState,
    viewModel: DownloadListViewModel,
    selectedCallback:(state: Boolean)-> Unit = {},
    onActionPost: (Task, DownloadListViewModel.TaskAction) -> Unit,
    onTaskSelection: (Task) -> Unit = {},
    onBatchAction: (DownloadListViewModel.TaskAction) -> Unit = {},
) {
    // 缓存过滤后的任务映射，避免不必要的重组
    val filteredMap = remember(taskDownloadStateMap) {
        taskDownloadStateMap
    }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    // 使用LazyColumn的状态管理器
    val lazyListState = rememberLazyListState()

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(Color(0xFF1C1D1E))
            .singClick {
                if (uiState.isSelectModeEnabled) {
                    selectedCallback.invoke(true)
                }
            }){
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.download_list), fontSize = 18.sp,color = Color.White)

            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(12.dp), // 列表项之间的间距
            ) {
                items(
                    items = filteredMap.toList().sortedBy { (_, state) -> state.downloadState },
                    key = { (task, _) -> task.id },
                ){ (task, state) ->
                    with(state.viewState){
                        AnimatedVisibility(
                            modifier = Modifier.fillMaxWidth(),
                            visible = true,
                            exit = shrinkVertically() + fadeOut(),
                            enter = expandVertically() + fadeIn(),
                        ){
                            // 记录下载状态日志，便于生产环境问题追踪
                            Log.d("DownloadListScreen", "任务ID: ${task.id}, 下载状态: ${state.downloadState}")

                            VideoCardV1(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp), // 减少垂直内边距，因为LazyColumn已有间距
                                viewState = this@with,
                                downloadState = state.downloadState,
                                progressLinear = {
                                    ProgressLinear(
                                        modifier = Modifier.fillMaxWidth(),
                                        downloadState = state.downloadState
                                    )
                                },
                                isSelectEnabled = {
                                    uiState.isSelectModeEnabled
                                },
                                isSelected = {
                                    uiState.selectedTasks.contains(task)
                                },
                                onSelect = {
                                    onTaskSelection(task)
                                },
                                onClick = { action ->
                                    when (action) {
                                        is DownloadListViewModel.TaskAction.OpenFile -> {
                                            viewModel.handleIntent(
                                                DownloadListViewModel.Intent.ExecuteTaskAction(
                                                    task = task,
                                                    action = action
                                                )
                                            )
                                        }
                                        else -> {
                                            onActionPost(task, action)
                                        }
                                    }
                                },
                                onLongClick = {
                                    // 长按进入选择模式
                                    if (!uiState.isSelectModeEnabled) {
                                        selectedCallback.invoke(false)
                                        onTaskSelection(task)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }


        AnimatedVisibility(
            modifier = Modifier
                .padding(bottom = 30.dp)
                .align(Alignment.BottomCenter),
            visible = uiState.isSelectModeEnabled,
            enter = scaleIn(),
            exit = scaleOut()
        ){
            DeleteBarWidget(
                Modifier.padding(vertical = 20.dp),
                onCancel = {
                    selectedCallback.invoke(true)
                },
                onDelete = {
                    uiState.selectedTasks.forEach {
                        onActionPost(it, DownloadListViewModel.TaskAction.Cancel)
                        onActionPost(it, DownloadListViewModel.TaskAction.Delete)
                    }
                    onBatchAction(DownloadListViewModel.TaskAction.Delete)
                })
        }
    }

}

