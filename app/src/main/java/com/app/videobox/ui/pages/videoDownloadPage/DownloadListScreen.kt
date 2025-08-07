package com.app.videobox.ui.pages.videoDownloadPage

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.pages.video.VideoPlayerManager
import com.videodownloader.module.api.*
import com.app.videobox.ui.pages.video.player.VlcPlayerActivity
import com.app.videobox.ui.widgets.DeleteBarWidget
import com.app.videobox.ui.widgets.ProgressLinear
import com.app.videobox.ui.widgets.UiAction
import com.app.videobox.ui.widgets.VideoCardV1
import com.app.videobox.ui.widgets.singClick
import com.videodownloader.module.download.DownloaderV2
import com.videodownloader.module.download.Task
import org.koin.compose.koinInject
import java.io.File
import kotlin.collections.remove
import kotlin.text.clear

/**
 * 视频下载列表页面
 * 遵循标准MVI架构模式，通过ViewModel管理状态和业务逻辑
 */
class VideoDownloadListScreen : Screen {
    
    companion object {
        private const val TAG = "VideoDownloadListScreen"
    }
    
    @SuppressLint("ContextCastToActivity")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        
        DownloadListScreen(
            onNavigateBack = { navigator.pop() }
        )
    }
}

/**
 * 下载列表主页面
 * 使用 DownloadListViewModel 进行状态管理
 * 
 * @param modifier 修饰符
 * @param onNavigateBack 返回导航回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    modifier: Modifier = Modifier,
    downloader: DownloaderV2 = koinInject(),
    viewModel: DownloadListViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    TaskListContent(
        taskDownloadStateMap = downloader.getTaskStateMap(),
        selectedCallback = {state->
            
        },
        onActionPost = { task, action ->
            
            when (action) {
                is UiAction.Cancel -> downloader.cancel(task)
                is UiAction.Delete -> downloader.remove(task)
                is UiAction.Resume -> downloader.restart(task)
                is UiAction.OpenFile -> {
                    if (action.filePath != null){
                        VlcPlayerActivity.start(
                            context = context,
                            videoPath = action.filePath,
                            )
                    }
                }
            }
        }
    )
}


/**
 * 状态栏区域组件
 * 模拟iPhone状态栏样式
 */
@Composable
private fun StatusBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.Transparent)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间显示
        Text(
            text = "9:41",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        // 右侧状态图标区域
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 信号强度图标
            Box(
                modifier = Modifier
                    .size(18.dp, 12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
            // WiFi图标
            Box(
                modifier = Modifier
                    .size(15.dp, 12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
            // 电池图标
            Box(
                modifier = Modifier
                    .size(24.dp, 12.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * 标题栏区域组件
 */
@Composable
private fun TitleBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 显示标题
        Text(
            text = "Download List",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 任务列表内容组件
 * 显示下载任务列表
 */
@Composable
private fun TaskListContent(
    taskDownloadStateMap: SnapshotStateMap<Task, Task.State>,
    selectedCallback:(state: Boolean)-> Unit = {},
    onActionPost: (Task, UiAction) -> Unit,
) {
    val filteredMap = remember(taskDownloadStateMap) {
        taskDownloadStateMap
    }
    
    var isSelectEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val selectedItemIds = remember(filteredMap) { mutableStateListOf<Task>() }

    val lazyListState = rememberLazyGridState()

    Box(Modifier.fillMaxSize().singClick {
        isSelectEnabled = false
        selectedCallback.invoke(true)
        selectedItemIds.clear()
    }){
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding(),
            state = lazyListState,
            columns = GridCells.Adaptive(240.dp),
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),)
        {
            items(
                items = filteredMap.toList().sortedBy { (_, state) -> state.downloadState },
                key = { (task, _) -> task.id },
            ){ (task,state)->
                with(state.viewState){
                    AnimatedVisibility(
                        modifier = Modifier,
                        visible = true,
                        exit = shrinkVertically() + fadeOut(),
                        enter = expandVertically() + fadeIn(),
                    ){

                        Log.d("ProgressLinear", "下载状态:${state.downloadState}")

                        VideoCardV1(
                            modifier = Modifier.padding(bottom = 20.dp),
                            viewState = this@with,
                            downloadState = state.downloadState,
                            actionButton = {},
                            stateIndicator = {},
                            progressLinear = {
                                ProgressLinear(modifier = Modifier
                                    .fillMaxWidth(), downloadState = state.downloadState)
                            },
                            isSelectEnabled = {
                                isSelectEnabled
                            },
                            isSelected = {
                                selectedItemIds.contains(task)
                            },
                            onSelect = {
                                if (selectedItemIds.contains(task)) selectedItemIds.remove(task)
                                else selectedItemIds.add(task)
                            },
                            onClick = {
                                onActionPost(task, it)
                            },
                            onLongClick = {
                                isSelectEnabled = true
                                selectedCallback.invoke(!isSelectEnabled)
                                selectedItemIds.add(task)
                            },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .padding(bottom = 30.dp)
                .align(Alignment.BottomCenter),
            visible = isSelectEnabled,
            enter = scaleIn(),
            exit = scaleOut()
        ){
            DeleteBarWidget(
                Modifier.padding(vertical = 20.dp),
                onCancel = {
                    isSelectEnabled = false
                    selectedCallback.invoke(!isSelectEnabled)
                },
                onDelete = {
                    selectedItemIds.forEach {
                        onActionPost(it, UiAction.Cancel)
                        onActionPost(it, UiAction.Delete)
                    }
                    isSelectEnabled = false
                    selectedCallback.invoke(!isSelectEnabled)
                })
        }
    }

}

