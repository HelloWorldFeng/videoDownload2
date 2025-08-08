package com.app.videobox.utils

import android.os.Build
import java.util.Locale

object DeviceUtils {

    /**
     * 获取手机品牌
     */
    fun getBrand(): String {
        return Build.BRAND.uppercase(Locale.getDefault())
    }

    /**
     * 获取手机型号
     */
    fun getModel(): String {
        return Build.MODEL
    }

    /**
     * 获取手机制造商
     */
    fun getManufacturer(): String {
        return Build.MANUFACTURER.uppercase(Locale.getDefault())
    }

    /**
     * 获取详细的设备信息
     * 包含品牌、型号、制造商、Android版本等信息
     */
    fun getDetailedDeviceInfo(): String {
        return """
            Brand: ${getBrand()}
            HomeUrlModel: ${getModel()}
            Manufacturer: ${getManufacturer()}
            Android Version: ${Build.VERSION.RELEASE}
            SDK Version: ${Build.VERSION.SDK_INT}
        """.trimIndent()
    }


}