package com.app.videobox.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.app.videobox.App
import com.app.videobox.App.Companion.notLaunchHot
import com.app.videobox.BuildConfig
import com.app.videobox.MAIN_OPERATE
import com.app.videobox.MAIN_SHOW_VIDEO
import com.app.videobox.MAIN_SHOW_WEB
import com.app.videobox.ad.UmpHelper
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.manager.FileManager.fetchPhoneVideo
import com.app.videobox.service.DownloadService
import com.app.videobox.ui.dialogs.NotifyHomeDialog
import com.app.videobox.ui.pages.homePage.NewHomeScreen
import com.app.videobox.ui.pages.video.player.VlcPlayerActivity
import com.app.videobox.ui.pages.video.playerV2.VideoPlayActivity
import com.app.videobox.ui.pages.webViewPage.WebViewActivity
import com.app.videobox.ui.pages.webViewPage.WebViewScreen
import com.app.videobox.utils.FileUtils
import com.app.videobox.utils.NotifyHelper
import com.blankj.utilcode.util.SPStaticUtils
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    companion object{
        fun start(context: Context, extras: Bundle? = null) {
            val intent = Intent(context, MainActivity::class.java)
            extras?.let { intent.putExtras(it) }
            context.startActivity(intent)
        }
    }

    private var navigator: Navigator?=null

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

            if (SPStaticUtils.getBoolean("showHomeNotify",true)
                && XXPermissions.isGranted(this,
                    Permission.POST_NOTIFICATIONS).not()){
                SPStaticUtils.put("showHomeNotify",false)
                NotifyHomeDialog(
                    onDismissRequest = {},
                    onClick = {
                        notLaunchHot = true
                        NotifyHelper.openNotificationSettings(this)
                    }
                )
            }
        }

        DownloadService.startService(this)
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
                    VideoPlayActivity.start(
                        context = this,
                        videoUrl = it,
                        title = title
                    )
                }
            }
        }
    }

}



