package com.app.videobox.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.app.videobox.App
import com.app.videobox.R
import com.app.videobox.ad.AdmobManager
import com.app.videobox.ad.NativeAdsView
import com.app.videobox.ad.base.AdUnitWrapper
import com.app.videobox.ext.safeStartActivity
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.manager.FileManager
import com.app.videobox.manager.FileManager.fetchPhoneVideo
import com.app.videobox.manager.RemoteConfigManager
import com.app.videobox.ui.dialogs.PermissionDialog
import com.app.videobox.ui.pages.FolderScreen
import com.app.videobox.ui.pages.HotScreen
import com.app.videobox.ui.pages.LocalVideoScreen
import com.app.videobox.ui.pages.SettingScreen
import com.app.videobox.ui.pages.DownloadListScreenWrapper
import com.app.videobox.ui.pages.NewHomeScreen
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.SearchBar
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.FileUtils
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (FileUtils.checkFilePermission(this@MainActivity).not()) {
                return@launch
            }
            FileUtils.requestFilePermission(this@MainActivity){
                fetchPhoneVideo(this@MainActivity)
            }
        }
        setContent {
            BackHandler {}
            Navigator(NewHomeScreen())
            LoadingAdDialog()
        }
    }
}



