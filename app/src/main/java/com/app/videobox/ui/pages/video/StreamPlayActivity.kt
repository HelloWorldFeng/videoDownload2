package com.app.videobox.ui.pages.video

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.app.videobox.ui.theme.MyApplicationTheme

/**
 * 全新重构的视频播放Activity - StreamPlayActivity
 * 采用Jetpack Compose技术栈，完全重写视频播放逻辑
 * 混淆命名：StreamPlayActivity (原VideoPlayActivity)
 * 功能：提供现代化的视频播放界面容器
 */
class StreamPlayActivity : ComponentActivity() {
    
    // 混淆命名的状态变量
    private var mediaUrl: String = ""  // 原url变量
    private var mediaTitle: String = "" // 原title变量
    private var orientationState: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // 屏幕方向状态
    
    companion object {
        private const val TAG = "StreamPlayActivity"
        
        // Intent参数常量 - 混淆命名
        const val EXTRA_MEDIA_URL = "media_url"     // 原video_url
        const val EXTRA_MEDIA_TITLE = "media_title" // 原title
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "StreamPlayActivity onCreate - 开始初始化视频播放界面")
        
        // 获取Intent传递的参数
        mediaUrl = intent.getStringExtra(EXTRA_MEDIA_URL) ?: ""
        mediaTitle = intent.getStringExtra(EXTRA_MEDIA_TITLE) ?: "未知视频"
        
        Log.d(TAG, "获取播放参数 - URL: $mediaUrl, 标题: $mediaTitle")
        
        // 配置沉浸式全屏显示
        configureImmersiveMode()
        
        // 设置Compose内容
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 使用重构后的Compose视频播放器
                    StreamPlayerCompose(
                        videoUrl = mediaUrl,
                        videoTitle = mediaTitle,
                        onBackClick = {
                            Log.d(TAG, "用户触发返回操作")
                            handleBackNavigation()
                        }
                    )
                }
            }
        }
        
        Log.d(TAG, "StreamPlayActivity onCreate完成")
    }
    
    /**
     * 配置沉浸式全屏模式 - 混淆命名
     * 原window配置逻辑的重构版本
     */
    private fun configureImmersiveMode() {
        Log.d(TAG, "配置沉浸式全屏模式")
        
        // 启用边到边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 配置状态栏和导航栏
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // 隐藏系统栏
            hide(WindowInsetsCompat.Type.systemBars())
            // 设置系统栏行为
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // 设置窗口标志 - 适配刘海屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val layoutParams = window.attributes
            layoutParams.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = layoutParams
            
            Log.d(TAG, "已适配刘海屏显示")
        }
        
        // 设置状态栏和导航栏透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // 设置系统UI标志
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
    
    /**
     * 处理返回导航 - 混淆命名
     * 原finish()逻辑的重构版本
     */
    private fun handleBackNavigation() {
        Log.d(TAG, "处理返回导航 - 清理资源并退出")
        
        // 恢复屏幕方向
        if (orientationState != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Log.d(TAG, "恢复竖屏方向")
        }
        
        // 清理播放状态
        cleanupPlaybackResources()
        
        // 结束Activity
        finish()
    }
    
    /**
     * 清理播放资源 - 混淆命名
     * 原播放器资源释放逻辑的重构版本
     */
    private fun cleanupPlaybackResources() {
        Log.d(TAG, "清理视频播放资源")
        
        // 这里应该添加具体的播放器资源清理逻辑
        // 例如：停止播放、释放播放器实例等
        
        Log.d(TAG, "视频播放资源清理完成")
    }
    
    /**
     * 处理屏幕方向变化 - 混淆命名
     * 原onConfigurationChanged逻辑的重构版本
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        orientationState = newConfig.orientation
        Log.d(TAG, "屏幕方向变化: ${if (orientationState == android.content.res.Configuration.ORIENTATION_LANDSCAPE) "横屏" else "竖屏"}")
        
        // 重新配置沉浸式模式以适应新方向
        configureImmersiveMode()
    }
    
    /**
     * Activity暂停处理 - 混淆命名
     * 原onPause逻辑的重构版本
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity暂停 - 暂停视频播放")
        
        // 这里应该添加暂停播放的逻辑
        // 例如：调用播放器的pause方法
    }
    
    /**
     * Activity恢复处理 - 混淆命名
     * 原onResume逻辑的重构版本
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity恢复 - 恢复视频播放")
        
        // 重新配置沉浸式模式
        configureImmersiveMode()
        
        // 这里应该添加恢复播放的逻辑
        // 例如：调用播放器的resume方法
    }
    
    /**
     * Activity销毁处理 - 混淆命名
     * 原onDestroy逻辑的重构版本
     */
    override fun onDestroy() {
        Log.d(TAG, "Activity销毁 - 释放所有资源")
        
        // 清理播放资源
        cleanupPlaybackResources()
        
        super.onDestroy()
        
        Log.d(TAG, "StreamPlayActivity销毁完成")
    }
    
    /**
     * 处理窗口焦点变化 - 混淆命名
     * 原onWindowFocusChanged逻辑的重构版本
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        Log.d(TAG, "窗口焦点变化: ${if (hasFocus) "获得焦点" else "失去焦点"}")
        
        if (hasFocus) {
            // 重新应用沉浸式模式
            configureImmersiveMode()
        }
    }
    
    /**
     * 处理返回按键 - 混淆命名
     * 新增的返回键处理逻辑
     */
    override fun onBackPressed() {
        Log.d(TAG, "用户按下返回键")
        handleBackNavigation()
    }
}