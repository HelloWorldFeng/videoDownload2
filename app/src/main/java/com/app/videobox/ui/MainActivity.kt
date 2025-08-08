package com.app.videobox.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.manager.FileManager.fetchPhoneVideo
import com.app.videobox.ui.pages.homePage.NewHomeScreen
import com.app.videobox.utils.FileUtils
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



