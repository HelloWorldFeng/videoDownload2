package com.app.videobox.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import com.app.videobox.App.Companion.notLaunchHot
import com.app.videobox.BuildConfig
import com.app.videobox.MAIN_OPERATE
import com.app.videobox.MAIN_SHOW_VIDEO
import com.app.videobox.MAIN_SHOW_WEB
import com.app.videobox.ad.UmpHelper
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.manager.FileManager.fetchPhoneVideo
import com.app.videobox.service.DownloadVideoService
import com.app.videobox.ui.dialogs.NotifyHomeDialog
import com.app.videobox.ui.pages.homePage.HomeViewModel
import com.app.videobox.ui.pages.homePage.NewHomeScreen
import com.app.videobox.ui.pages.video.playerV2.VlcPlayActivity
import com.app.videobox.ui.pages.webViewPage.WebViewActivity
import com.app.videobox.ui.widgets.AsyncImageImpl
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.FileUtils
import com.app.videobox.utils.NotifyHelper
import com.app.videobox.utils.VideoThumbnailExtractor
import com.blankj.utilcode.util.SPStaticUtils
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue

class MainActivity : BaseActivity() {

    companion object{
        fun start(context: Context, extras: Bundle? = null) {
            val intent = Intent(context, MainActivity::class.java)
            extras?.let { intent.putExtras(it) }
            context.startActivity(intent)
        }
    }
    private var showNotifyDialog by mutableStateOf(true)
    private val homeViewModel: HomeViewModel by viewModel()

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
        if (SPStaticUtils.getBoolean("enter_home_first",true)){
            SPStaticUtils.put("enter_home_first",false)
            EventReportUtils.reportTDParams("enter_home_first",
                params = mutableMapOf(), desc = "首次进入主页")
        }

        if (NotifyHelper.checkNotificationPermission(this)){
            EventReportUtils.reportTDParams("push_permission_agree",
                params = mutableMapOf(), desc = "已同意通知权限（主页）")
        }else{
            EventReportUtils.reportTDParams("push_permission_no_agree",
                params = mutableMapOf(), desc = "没有通知权限（主页）")
        }

        setContent {
            BackHandler {}
            Navigator(NewHomeScreen())
            LoadingAdDialog()

            if (SPStaticUtils.getBoolean("showHomeNotify",true)
                && XXPermissions.isGranted(this,
                    Permission.POST_NOTIFICATIONS).not()
                && showNotifyDialog
                ){
                SPStaticUtils.put("showHomeNotify",false)
                NotifyHomeDialog(
                    onDismissRequest = {
                        EventReportUtils.reportTDParams("permission_pop_click", params = mutableMapOf(
                            "click_type" to "home",
                            "action" to "close"
                        ), desc = "新通知权限引导弹窗点击")
                        showNotifyDialog = false
                    },
                    onClick = {
                        EventReportUtils.reportTDParams("permission_pop_click", params = mutableMapOf(
                            "click_type" to "home",
                            "action" to "grant"
                        ), desc = "新通知权限引导弹窗点击")
                        notLaunchHot = true
                        NotifyHelper.openNotificationSettings(this)
                        showNotifyDialog = false
                    }
                )
            }
        }

        DownloadVideoService.startService(this)
        UmpHelper.requestUmp(this) {}


        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // 处理内部 Intent
        val flag = intent.getStringExtra(MAIN_OPERATE)
        when (flag) {
            //跳网页
            MAIN_SHOW_WEB -> {
                intent.getStringExtra("videoUrl")?.let {
                    if (it.startsWith("http")) {
                        WebViewActivity.start(context = this,it)

                    }else{
                        WebViewActivity.start(context = this,"https://google.com/search?q=${it}")
                    }
                }

            }

            MAIN_SHOW_VIDEO -> {
                intent.getStringExtra("videoUrl")?.let {
                    val title = intent.getStringExtra("videoTitle")?:"title"
//                    VlcPlayerActivity.start(
//                        context = this,
//                        videoPath = it,
//                        videoTitle = title
//                    )
                    VlcPlayActivity.start(
                        context = this,
                        videoUrl = it,
                        title = title
                    )
                }
            }
        }
    }

}



