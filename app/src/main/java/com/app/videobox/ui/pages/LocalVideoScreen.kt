package com.app.videobox.ui.pages

import android.content.Context
import android.os.Bundle
import android.text.format.Formatter
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ad.AdmobManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AdPlaceTag
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.ext.formatDuration
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ext.shareApp
import com.app.videobox.ext.shareVideo
import com.app.videobox.manager.FileManager
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.dialogs.MoreDialog
import com.app.videobox.ui.dialogs.RenameDialog
import com.app.videobox.ui.pages.video.VideoPlayActivity
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LocalVideoScreen(val dataList:MutableList<FileManager.FileInfo>) :Screen{
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current as BaseActivity
        var showDialog by remember {
            mutableStateOf(value = false)
        }
        var showRename by remember {
            mutableStateOf(value = false)
        }
        var selectFileInfo by remember {
            mutableStateOf<FileManager.FileInfo?>(null)
        }

        BackHandler {
            backPopAd(context, navigator)
        }
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            TitleBar(title = stringResource(id = R.string.local_video)) {
                backPopAd(context, navigator)
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (dataList.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                CoilImage(
                    modifier = Modifier.size(66.dp, 74.dp),
                    data = R.drawable.icon_empty
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(text = stringResource(R.string.no_content_at_the_moment), fontSize = 16.sp,color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
            }else{
                LazyColumn(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    items(dataList){fileInfo->
                        ListItemView(fileInfo,
                            onClick = {
                                AdmobManager.getFullAdFromPool(
                                    context,
                                    adType = "int",
                                    adScene = "play_int",
                                    emptyAction = {
                                        context.lifecycleScope.launch {
                                            context.showLoadingDialog()
                                            delay(RemoteConfigManager.adLoadingTime)
                                            AdmobManager.touchFinishBlock()
                                        }
                                    },
                                    finishLoadAction = { hasAdInstance ->
                                        context.hideLoadingDialog()
                                        AdmobManager.getFullAdFromPool(
                                            context,
                                            adType = "int",
                                            adScene = "play_int",
                                            closeAction = {
                                                context.safeStartActivity(
                                                    VideoPlayActivity::class.java,
                                                    args = Bundle().apply {
                                                        putString("video_url", fileInfo.file.absolutePath)
                                                        putString("title",fileInfo.titleName)
                                                    })
                                            })
                                    },
                                    closeAction = {
                                        context.safeStartActivity(
                                            VideoPlayActivity::class.java,
                                            args = Bundle().apply {
                                                putString("video_url", fileInfo.file.absolutePath)
                                                putString("title",fileInfo.titleName)
                                            })
                                    })

                            },
                            onClickMore = {
                                selectFileInfo = fileInfo
                                showDialog = true
                            })
                    }
                }

                var nativeState by remember {
                    mutableStateOf<AdUnitWrapper?>(null)
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(key1 = lifecycleOwner) {
                    val observer = object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_START)
                        fun onStart() {
                            AdmobManager.getSmallAdFromPool(
                                AdPlaceTag.AD_Home,
                                adType = "nav",
                                adScene = "function_nav"
                            ){
                                nativeState = it
                            }

                        }

                        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                        fun onStop() {
                            nativeState = null
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }

                }

                Spacer(modifier = Modifier.height(10.dp))
                Spacer(modifier = Modifier.weight(1f))
                NativeAdsView(
                    adUnitWrapper = nativeState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth(1f),
                    bigStyle = false
                )
            }

        }

        if (showRename && selectFileInfo != null) {
            RenameDialog(
                isVisible = true,
                fileName = selectFileInfo!!.titleName.split(".").first(),
                onOK = { newName->
                    FileUtils.renameFile(newName, selectFileInfo!!)
                },
                onDismiss = {
                    showRename = false
                }
            )
        }
        MoreDialog(showDialog,
            onDelete = {
                selectFileInfo?.let {
                    FileUtils.deleteFile(context,it)
                    dataList.remove(it)
                }

            },
            onRename = {
                showRename = true
            },
            onShare = {
                selectFileInfo?.let {
                    shareVideo(context,it.file)
                }
            },
            onDismiss = {
                showDialog = false
            })
    }


    private fun backPopAd(
        context: BaseActivity,
        navigator: Navigator
    ) {
        AdmobManager.getFullAdFromPool(
            context,
            adType = "int",
            adScene = "back_int",
            closeAction = {
                navigator.pop()
            })
    }
}

@Composable
fun ListItemView(fileInfo: FileManager.FileInfo, onClick: () -> Unit,onClickMore:()->Unit) {
    val context = LocalContext.current
    val size = remember {
        Formatter.formatFileSize(context, fileInfo.sizeKB)
    }
    Row(
        Modifier
            .singClick {
                onClick.invoke()
            }
            .padding(bottom = 16.dp)
            .fillMaxWidth(0.9f)
            .height(110.dp)
            .background(color = Color(0xFF2E2F30), shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier.size(107.dp, 80.dp)){
            CoilImage(
                modifier = Modifier
                    .size(107.dp, 80.dp)
                    .clip(shape = RoundedCornerShape(14.dp)),
                data = fileInfo.file.absolutePath,
                contentScale = ContentScale.FillBounds
            )
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, bottom = 6.dp)
                    .align(Alignment.BottomStart)
                    .wrapContentSize()
                    .background(color = Color(0x52000000), shape = RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = fileInfo.playTime.formatDuration(),
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(3.dp)
                        .align(Alignment.BottomStart)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(Modifier) {
            TextTitle(text = fileInfo.titleName, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Row {
                Text(text = size, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                CoilImage(modifier = Modifier
                    .singClick {
                        onClickMore.invoke()
                    }
                    .size(20.dp), data = R.drawable.icon_point)
            }
        }
    }
}