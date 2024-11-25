package com.app.videobox.utils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.app.videobox.R
import com.blankj.utilcode.util.SPStaticUtils
import java.util.Locale
import java.util.Stack

object LanguageUtils {

    data class Ls(
        val name:String,
        val locale: Locale,
        val icon:Any?=null
    )
    val list = mutableListOf(
        Ls("English", Locale.ENGLISH),
        Ls("Español", Locale("es")),
        Ls("Filipino", Locale("tl", "PH")),
        Ls("Hindi", Locale("hi", "IN")),
        Ls("ltaliano", Locale.ITALIAN),
        Ls("한국인", Locale.KOREA),
    )


    /**
     * 这个方法是为了让全部的activity都修改语言
     *
     * @param locale
     * @param activity
     * @param context
     */
    fun shiftLanguage(locale: Locale?, activity: Activity, context: Context) {
        val resources = context.resources
        val config = resources.configuration
        val dm = resources.displayMetrics
        config.locale = locale
        resources.updateConfiguration(config, dm)
        activity.recreate()
    }

    /**
     * 这个方法虽然更新了资源但是只能以后的界面生效，之前没有finish的页面还是保留原来的语言
     *
     * @param locale
     * @param context
     */
    fun shiftLanguage(locale: Locale?, context: Context) {
        val resources = context.resources
        val config = resources.configuration
        val dm = resources.displayMetrics
        config.locale = locale
        resources.updateConfiguration(config, dm)
    }


    /**
     * Activity 更新语言资源
     */
    fun getAttachBaseContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return setAppLanguageApi24(context)
        } else {
            setAppLanguage(context)
        }
        return context
    }

    /**
     * 设置应用语言
     */
    @Suppress("DEPRECATION")
    fun setAppLanguage(context: Context) {
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        val configuration = resources.configuration
        // 获取当前系统语言，默认设置跟随系统
        val locale = getAppLocale()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }
        resources.updateConfiguration(configuration, displayMetrics)
    }

    /**
     * 兼容 7.0 及以上
     */
    @TargetApi(Build.VERSION_CODES.N)
    private fun setAppLanguageApi24(context: Context): Context {
        val locale = getAppLocale()
        val resource = context.resources
        val configuration = resource.configuration
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        return context.createConfigurationContext(configuration)
    }



    /**
     * 获取 App 当前语言
     */
    private fun getAppLocale() = list[SPStaticUtils.getInt("selectLanguageIndex", 0)].locale



}