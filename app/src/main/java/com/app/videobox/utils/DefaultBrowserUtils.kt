package com.app.videobox.utils

import android.content.Context
import android.content.Intent
import android.os.Build

object DefaultBrowserUtils {
    const val REQUEST_DEFAULT_BROWSER = 111
    fun requestDefaultBrowser(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 RoleManager
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER)) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER)
                if (context is android.app.Activity) {
                    context.startActivityForResult(intent, REQUEST_DEFAULT_BROWSER)
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }
    }

}