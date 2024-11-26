package com.app.videobox.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

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