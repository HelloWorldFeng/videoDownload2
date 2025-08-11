package com.app.videobox.ui.pages.localVideoPage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.manager.FileManager
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.dialogs.LoadingDialog
import com.app.videobox.ui.pages.homePage.StatusBarSection
import com.app.videobox.ui.widgets.AsyncImageImpl

import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.singClick



@Composable
fun FolderScreen(
    onMenuClick: () -> Unit = {}
){
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current as BaseActivity
    val dataList = run {
        val videoMap = mutableMapOf<String,MutableList<FileManager.FileInfo>>()
        FileManager.scanFileResultState.forEach {
            videoMap.getOrPut(it.parentDir){ mutableListOf() }.add(it) // 仅添加存在的文件
        }
        videoMap.toList()
    }

    BackHandler {

    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1D1E))
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        // 状态栏区域
        StatusBarSection(onMenuClick = onMenuClick)


        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .singClick {
                        navigator.push(HotScreen())
                    },
            )
            {
                AsyncImageImpl(
                    modifier = Modifier.matchParentSize(),
                    model = R.drawable.bg_hot_video,
                    contentScale = ContentScale.FillBounds
                )

                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AsyncImageImpl(
                        R.drawable.icon_gift,
                        modifier = Modifier.size(65.dp,79.dp)
                    )
                    Spacer(Modifier.width(22.dp))
                    Column {
                        Text(stringResource(R.string.hot_video), fontSize = 20.sp,color = Color.White)
                        Spacer(Modifier.height(11.dp))
                        Text(stringResource(R.string.here_are_the_popular_videos), fontSize = 12.sp,color = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    AsyncImageImpl(
                        model = R.drawable.icon_arrow_right,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                items(dataList) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .singClick {
                                navigator.push(LocalVideoScreen(it.second))
                            }
                            .padding(bottom = 16.dp)
                            .height(100.dp)
                            .background(
                                color = Color(0xFF2E2F30),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp)
                    ) {
                        CoilImage(modifier = Modifier.size(30.dp,24.dp),
                            data = R.drawable.icon_folder)
                        Spacer(modifier = Modifier.width(15.dp))
                        Column {
                            Text(text = it.first, fontSize = 14.sp,color = Color.White)
                            Text(text = context.getString(R.string.video, it.second.size),
                                fontSize = 12.sp,
                                color = Color(0xFF898989))
                        }
                        Spacer(Modifier.weight(1f))
                        AsyncImageImpl(
                            model = R.drawable.icon_arrow_right,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    LoadingDialog(FileManager.scanFileState.value)
}
