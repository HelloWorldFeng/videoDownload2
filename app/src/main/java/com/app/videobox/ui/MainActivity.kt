package com.app.videobox.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.manager.FileManager.fetchPhoneVideo
import com.app.videobox.service.DownloadService
import com.app.videobox.ui.dialogs.NotifyHomeDialog
import com.app.videobox.ui.pages.homePage.NewHomeScreen
import com.app.videobox.utils.FileUtils
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    companion object{
        fun start(context: Context, extras: Bundle? = null) {
            val intent = Intent(context, MainActivity::class.java)
            extras?.let { intent.putExtras(it) }
            context.startActivity(intent)
        }
    }

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
            NotifyHomeDialog(
                onDismissRequest = {},
                onClick = {}
            )
        }

        DownloadService.startService(this)

    }
}



