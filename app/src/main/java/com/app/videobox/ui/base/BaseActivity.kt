package com.app.videobox.ui.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.videobox.R
import com.app.videobox.ui.widgets.CoilImage
import com.app.videobox.ui.widgets.SystemBarTheme
import com.app.videobox.utils.LanguageUtils.getAttachBaseContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

abstract class BaseActivity : ComponentActivity() {
    private val focusChangeFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                focusChangeFlow.filter { it }
                hasFocusAfter()
            }
        }
    }

    open fun hasFocusAfter(){}

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focusChangeFlow.value = hasFocus
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { getAttachBaseContext(it) })
    }

    protected fun setContent(
        systemBarTheme: @Composable () -> Unit = {
            SystemBarTheme()
        },
        content: @Composable () -> Unit
    ) {
        setContent(parent = null,
            content = {
                systemBarTheme()

                Box(modifier = Modifier.fillMaxSize().background(color = Color.Black)) {
                    content()
                }
            })
    }
}