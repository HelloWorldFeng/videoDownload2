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
 * 基于Figma设计的视频播放Activity
 * 设计文件：Stream Box - 视频播放窗口-竖屏备份 6
 * 功能：展示Figma设计的播放器界面，集成本地播放器模块
 */
class FigmaPlayerActivity : ComponentActivity() {
    
    // 视频参数
    private var videoUrl: String = ""
    private var videoTitle: String = ""
    
    companion object {
        private const val TAG = "FigmaPlayerActivity"
        
        // Intent参数常量
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "FigmaPlayerActivity onCreate - 初始化Figma播放器界面")
        
        // 获取Intent参数
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "未知视频"
        
        Log.d(TAG, "视频参数 - URL: $videoUrl, 标题: $videoTitle")
        
        // 配置沉浸式模式
        configureImmersiveMode()
        
        // 设置Compose内容
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FigmaPlayerWindow(
                        videoUrl = videoUrl,
                        videoTitle = videoTitle,
                        onBackClick = {
                            Log.d(TAG, "用户点击返回，结束Activity")
                            handleBackNavigation()
                        }
                    )
                }
            }
        }
        
        Log.d(TAG, "FigmaPlayerActivity 初始化完成")
    }
    
    /**
     * 配置沉浸式全屏模式
     * 隐藏状态栏和导航栏，提供最佳的视频观看体验
     */
    private fun configureImmersiveMode() {
        Log.d(TAG, "配置沉浸式全屏模式")
        
        // 设置全屏显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // 配置系统UI可见性
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let { controller ->
            // 隐藏系统栏
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // 设置系统栏行为
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // 设置状态栏和导航栏颜色
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // 强制竖屏显示（根据Figma设计）
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        Log.d(TAG, "沉浸式模式配置完成")
    }
    
    /**
     * 处理返回导航
     */
    private fun handleBackNavigation() {
        Log.d(TAG, "处理返回导航")
        
        try {
            // 清理播放资源
            cleanupPlaybackResources()
            
            // 结束Activity
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "返回导航处理异常: ${e.message}", e)
            finish() // 确保Activity能够正常结束
        }
    }
    
    /**
     * 清理播放资源
     */
    private fun cleanupPlaybackResources() {
        Log.d(TAG, "清理播放资源")
        
        try {
            // TODO: 添加具体的资源清理逻辑
            // 例如：停止播放、释放播放器资源等
            
            Log.d(TAG, "播放资源清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "清理播放资源异常: ${e.message}", e)
        }
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "配置变更: ${newConfig.orientation}")
        
        // 根据Figma设计，保持竖屏模式
        if (newConfig.orientation != android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "强制保持竖屏模式")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity暂停")
        
        // TODO: 暂停视频播放
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity恢复")
        
        // 重新配置沉浸式模式
        configureImmersiveMode()
        
        // TODO: 恢复视频播放
    }
    
    override fun onDestroy() {
        Log.d(TAG, "Activity销毁")
        
        // 清理资源
        cleanupPlaybackResources()
        
        super.onDestroy()
        
        Log.d(TAG, "FigmaPlayerActivity销毁完成")
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "窗口焦点变更: $hasFocus")
        
        if (hasFocus) {
            // 重新应用沉浸式模式
            configureImmersiveMode()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d(TAG, "系统返回键按下")
        handleBackNavigation()
    }
}