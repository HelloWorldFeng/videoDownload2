package com.app.videobox.network

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.app.videobox.App
import com.google.gson.internal.GsonBuildConfig
import java.security.MessageDigest
import java.util.Locale

object ParamsEncryptUtil {

    val imei2:String @SuppressLint("HardwareIds")
    get() = Settings.Secure.getString(App.appContext().contentResolver, Settings.Secure.ANDROID_ID)


    val langType: String get() =
        App.appContext().resources.configuration.locales[0].language

    val countryCode get() = App.appContext().resources.configuration.locales[0].country


    val networkParams get() = mutableMapOf<String,Any>().apply {
        this["package"] =  App.appContext().packageName
        this["ts"] = System.currentTimeMillis()
        this["os"] = "android"

        this["av"] = try {
            App.appContext().packageManager.getPackageInfo(App.appContext().packageName, 0).versionName?:"Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Unknown"
        }
        this["appid"] = 1042

        this["imei2"] = imei2
        this["language"] = langType
        this["countryCode"] = countryCode

    }

    /**
     * MD5的Key 每个项目不同
     */
    private var MD5_KEY = "MQ6S37HFUQH3CQYZXQNTGBZ8BKZGGB"

    /**
     * POST请求参数加密（鉴权）
     *
     * @param commonParamsMap 公共请求参数
     * @param paramsMap 请求参数
     * @return 返回加密后的sign字符串
     */
    fun encryptData(
        commonParamsMap: Map<String, Any?>,
        paramsMap: Map<String, Any?> = mutableMapOf()
    ): Map<String, Any> {
        // 结果Map
        val resultMap = mutableMapOf<String, Any>()
        // sign字段，未MD5之前的字符串
        val signMD5PreStringBuilder = StringBuilder()

        // 组合接口参数到公共参数中
        val combineMap = mutableMapOf<String, Any?>()
        combineMap.putAll(commonParamsMap)

        combineMap.putAll(paramsMap)

        // 小写Key和原始Key的映射关系Map
        val keyMap = mutableMapOf<String, String>()
        // 外层公共参数Key列表，并所有参数名小写，再进行正序排序（小到大）
        val combineMapKeys = combineMap.keys.map {
            val lowerCaseKey = it.lowercase(Locale.ROOT)
            // 小写Key和原始Key的映射关系
            keyMap[lowerCaseKey] = it
            lowerCaseKey
        }.toList().sorted()

        // 添加参数到sign
        for (key in combineMapKeys) {
            var isUseOriginKey = false

            // 先用小写key找
            var value = combineMap[key]
            val originKey = keyMap[key]
            // 找不到，再用原始key找
            if (value == null) {
                value = combineMap[originKey]
                if (value != null) {
                    isUseOriginKey = true
                }
            }
            if (value != null) {
                // 如果是使用原始Key，则使用原始Key作为参数名
                if (isUseOriginKey) {
                    resultMap[originKey.toString()] = value
                } else {
                    resultMap[key] = value
                }
                signMD5PreStringBuilder.append(value.toString())
            }
        }

        val s = MD5_KEY.lowercase(Locale.ROOT)
        // 计算MD5之前的鉴权字符串，内容在前，key拼在后
        val signPreStr = signMD5PreStringBuilder.toString().lowercase() + s
        Log.d("加密", "signPreStr:$signPreStr")
        // MD5后，存到sign字段
        val hash = signPreStr.toMD5()   //md5(signPreStr)
        resultMap["sign"] = hash

        return resultMap
    }

    /**
     * MD5
     */
    fun String.toMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { String.format("%02x", it) }
    }


    /**
     * 扩展函数，将字符串中的 Unicode 转义序列转换为普通字符
     */
    fun String.unescapeUnicode(): String {
        val stringBuilder = StringBuilder()
        var i = 0

        while (i < this.length) {
            if (this[i] == '\\' && i + 5 < this.length && this[i + 1] == 'u') {
                // 解析 Unicode 转义序列
                val hexValue = this.substring(i + 2, i + 6)
                val intValue = hexValue.toInt(16)
                stringBuilder.append(intValue.toChar())
                i += 6
            } else {
                stringBuilder.append(this[i])
                i++
            }
        }

        return stringBuilder.toString()
    }
}