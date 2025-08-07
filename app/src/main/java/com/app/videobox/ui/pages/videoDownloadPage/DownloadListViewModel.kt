package com.app.videobox.ui.pages.videoDownloadPage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import com.videodownloader.module.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * 下载列表页面ViewModel
 * 遵循标准MVI架构模式，管理下载任务状态和用户交互
 */
class DownloadListViewModel : ViewModel() {

}