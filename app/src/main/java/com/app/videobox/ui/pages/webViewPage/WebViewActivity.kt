package com.app.videobox.ui.pages.webViewPage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videobox.R
import com.app.videobox.ad.UserHelper
import com.app.videobox.ui.MainActivity
import com.app.videobox.ui.base.BaseActivity
import com.app.videobox.ui.theme.gradientColor
import com.app.videobox.ui.widgets.TextTitle
import com.app.videobox.ui.widgets.singClick
import com.app.videobox.utils.DefaultBrowserUtils
import com.app.videobox.utils.EventReportUtils
import com.app.videobox.utils.LanguageUtils
import com.blankj.utilcode.util.SPStaticUtils
import org.koin.androidx.compose.koinViewModel

class WebViewActivity: BaseActivity() {

    companion object{
        fun start(context: Context, webUrl: String) {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra("web_url",webUrl)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("web_url")?:""

        EventReportUtils.reportTDParams("browser_search_show",
            params = mutableMapOf(
                "web" to url,
            ), desc = "搜索页面展示")


        setContent {
            Content(url)
        }
    }

    @Composable
    private fun Content(url: String) {
        val context = LocalContext.current as BaseActivity
        val viewModel: WebViewModel = koinViewModel()

        WebPageScreen(inputUrl = url,viewModel = viewModel)

        if (SPStaticUtils.getBoolean("SettingBrowser", true) && !UserHelper.powerUser) {
            SPStaticUtils.put("SettingBrowser",false)
            LaunchedEffect(Unit) {
                DefaultBrowserUtils.requestDefaultBrowser(context)
            }
        }
    }


}