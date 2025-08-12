package com.app.videobox.ui.pages.videoDownloadPage

import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.app.videobox.R
import com.app.videobox.ad.AdManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AD_TYPE_INT
import com.app.videobox.ui.pages.video.playerV2.VlcPlayActivity
import com.app.videobox.ui.pages.webViewPage.WebViewModel
import com.app.videobox.ui.widgets.AsyncImageImpl
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
    val context = LocalContext.current as Activity
    
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
                    AdManager.getFullAdFromPool(
                        context,
                        adType = AD_TYPE_INT,
                        adScene = "i_video_click",
                        closeAction = {
                            VlcPlayActivity.start(
                                context = context,
                                videoUrl = effect.filePath,
                                title = File(effect.filePath).name
                            )

                        })

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
                is DownloadListViewModel.TaskAction.Cancel ->
                    downloader.cancel(task)
                is DownloadListViewModel.TaskAction.Delete ->
                    downloader.remove(task)
                is DownloadListViewModel.TaskAction.Resume ->
                    downloader.restart(task)
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

            .background(Color(0xFF1C1D1E))
            .singClick {
                if (uiState.isSelectModeEnabled) {
                    selectedCallback.invoke(true)
                }
            })
    {
        AsyncImageImpl(
            modifier = Modifier.fillMaxWidth(),
            model = R.drawable.bg_comm
        )
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Text(text = stringResource(R.string.download_list), fontSize = 18.sp,color = Color.White)

                Spacer(Modifier.weight(1f))
                //多选状态下才显示
                AnimatedVisibility(
                    visible = uiState.isSelectModeEnabled
                ) {
                    val allTasks = remember(filteredMap) { filteredMap.keys.toList() }
                    val isAllSelected = viewModel.isAllSelected(allTasks)
                    
                    Text(
                        text = if (isAllSelected) stringResource(R.string.cancel_select_all) else stringResource(R.string.select_all),
                        color = Color.White,
                        modifier = Modifier.singClick{
                            if (isAllSelected) {
                                // 当前是全选状态，点击取消全选
                                viewModel.handleIntent(DownloadListViewModel.Intent.ClearSelection)
                            } else {
                                // 当前不是全选状态，点击全选
                                viewModel.handleIntent(DownloadListViewModel.Intent.SelectAllTasks(allTasks))
                            }
                        }
                    )
                }
            }
            NativeAdsView(
                modifier = Modifier
                    .padding(vertical = 15.dp)
                    .fillMaxWidth(1f),
                adScene = "n_download_list"
            )
            if (filteredMap.isEmpty()){
                Spacer(Modifier.weight(1f))
                val emptyLottie by rememberLottieComposition(LottieCompositionSpec.Asset("lottie_empty.json"))
                LottieAnimation(
                    composition = emptyLottie,
                    modifier = Modifier.size(171.dp),
                    contentScale = ContentScale.None
                )
                Text(
                    text = stringResource(R.string.there_is_no_video_please_go_to_add_it),
                    fontSize = 16.sp,
                    color = Color.White
                    )
                Spacer(Modifier.weight(2f))
            }else{
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(12.dp), // 列表项之间的间距
                )
                {
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
                                        onActionPost(task, action)
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

        }


        AnimatedVisibility(
            modifier = Modifier
                .padding(bottom = 80.dp)
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
                    selectedCallback.invoke(true)
                })
        }
    }

}

