package com.app.videobox.ui.pages

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.R
import com.app.videobox.ad.AdmobManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.manager.FileManager
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.dialogs.LoadingDialog
import com.app.videobox.ui.pages.video.VideoPlayActivity
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.TitleBar
import com.app.videobox.ui.widgets.singClick

class FolderScreen:Screen {

    @Composable
    override fun Content() {
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
            backPopAd(context, navigator)
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            TitleBar(title = stringResource(R.string.file_director), onBack = {
                backPopAd(context, navigator)
            })
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
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    items(dataList) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .singClick {
                                    navigator.push(LocalVideoScreen(it.second))
                                }
                                .padding(bottom = 16.dp)
                                .fillMaxWidth(0.9f)
                                .height(100.dp)
                                .background(
                                    color = Color(0xFF2E2F30),
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Spacer(modifier = Modifier.width(28.dp))
                            CoilImage(modifier = Modifier.size(55.dp, 39.dp), data = R.drawable.icon_folder)
                            Spacer(modifier = Modifier.width(15.dp))
                            Text(text = it.first, fontSize = 17.sp,color = Color.White)
                        }
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

        LoadingDialog(FileManager.scanFileState.value)
    }

    private fun backPopAd(
        context: BaseActivity,
        navigator: Navigator
    ) {
        if (context.isShowLoading()) {
            return
        }
        AdmobManager.getFullAdFromPool(
            context,
            adType = "int",
            adScene = "back_int",
            closeAction = {
                navigator.pop()
            })
    }
}