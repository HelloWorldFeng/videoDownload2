package com.app.videobox.ui.pages

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.ui.pages.videoDownloadPage.VideoDownloadListScreen

/**
 * 下载列表页面的Screen包装器
 * 用于Voyager导航系统
 */
class DownloadListScreenWrapper : Screen {
    
    @Composable
    override fun Content() {
        // 使用新的视频下载列表页面
        VideoDownloadListScreen().Content()
    }
}