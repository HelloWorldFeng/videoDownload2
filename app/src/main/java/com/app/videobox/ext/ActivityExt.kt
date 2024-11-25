package com.app.videobox.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

fun Context.safeStartActivity(clazz: Class<*>, args: Bundle? = null, options: Bundle? = null) {
    val ctx = this
    val intent = Intent(ctx, clazz).apply {
        if (ctx !is Activity) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (args != null) {
            putExtras(args)
        }
    }
    startActivity(intent, options)
}