package com.app.videobox.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

fun Context.safeStartActivity(clazz: Class<*>, args: Bundle? = null, options: Bundle? = null) {
    val intent = Intent(this, clazz).apply {
        if (args != null) {
            putExtras(args)
        }
    }
    startActivity(intent, options)
}

fun Context.urlInBrowser(url:String) {
    val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
        url
    } else {
        "http://$url"
    }
    // 创建一个意图来打开浏览器
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(formattedUrl)
    }
    // 启动意图
    this.startActivity(intent)
}

fun Context.shareApp() {
    try {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=${this@shareApp.packageName}"
            )
            type = "text/plain"
        }
        this.startActivity(intent)
    } catch (_: Exception) { }
}

fun shareVideo(context: Context, videoFile: File) {
    // 获取视频文件的 URI
    val videoUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // 使用 FileProvider 获取 URI
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
    } else {
        // 对于低版本，直接使用文件路径
        Uri.fromFile(videoFile)
    }

    // 创建分享 Intent
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, videoUri)
        type = "video/mp4" // 根据您的视频格式设置 MIME 类型
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予临时读取权限
    }

    // 启动分享对话框
    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
}

/**
 * 直接跳转到谷歌商店应用页面
 */
fun Context.openGooglePlayStore() {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=${this@openGooglePlayStore.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        this.startActivity(intent)
    } catch (e: Exception) {
        // 如果谷歌商店应用不存在，使用浏览器打开
        openGooglePlayStoreInBrowser()
    }
}

/**
 * 在浏览器中打开谷歌商店页面
 */
fun Context.openGooglePlayStoreInBrowser() {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data =
                "https://play.google.com/store/apps/details?id=${this@openGooglePlayStoreInBrowser.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        this.startActivity(intent)
    } catch (e: Exception) {
    }
}

// 判断系统是否处于暗色模式（夜间模式）
fun Context.isDarkModeEnabled(): Boolean {
    val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return mode == Configuration.UI_MODE_NIGHT_YES
}