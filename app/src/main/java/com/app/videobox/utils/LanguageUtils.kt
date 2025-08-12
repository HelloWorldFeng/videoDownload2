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
        return setAppLanguageApi24(context)
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
        configuration.setLocale(locale);
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
    private fun getAppLocale(): Locale {
        try {
            val savedIndex = SPStaticUtils.getInt("selectLanguageIndex", -1)
            
            // 如果用户没有手动设置过语言（-1表示未设置），则尝试匹配系统语言
            if (savedIndex == -1) {
                val systemLocale = Locale.getDefault()
                val matchedIndex = findMatchingLanguageIndex(systemLocale)
                if (matchedIndex != -1) {
                    // 找到匹配的语言，保存并返回
                    SPStaticUtils.put("selectLanguageIndex", matchedIndex)
                    return list[matchedIndex].locale
                } else {
                    // 没找到匹配的语言，使用默认英语但不保存（让用户选择）
                    return list[0].locale
                }
            }
            
            // 用户已手动设置过语言，直接返回
            return list[savedIndex].locale
        } catch (e: Exception) {
            // 如果出现异常（比如SharedPreferences未初始化），返回默认语言
            e.printStackTrace()
            return list[0].locale
        }
    }
    
    /**
     * 查找与系统语言匹配的语言索引
     */
    private fun findMatchingLanguageIndex(systemLocale: Locale): Int {
        val systemLanguage = systemLocale.language
        val systemCountry = systemLocale.country
        
        // 首先尝试精确匹配（语言+国家）
        for (i in list.indices) {
            val locale = list[i].locale
            if (locale.language == systemLanguage && locale.country == systemCountry) {
                return i
            }
        }
        
        // 如果精确匹配失败，尝试只匹配语言
        for (i in list.indices) {
            val locale = list[i].locale
            if (locale.language == systemLanguage) {
                return i
            }
        }
        
        // 没有找到匹配的语言
        return -1
    }
    
    /**
     * 检查是否需要初始化语言设置
     */
    fun initializeLanguageIfNeeded() {
        try {
            // 触发getAppLocale()来执行自动语言检测和设置
            getAppLocale()
        } catch (e: Exception) {
            // 如果在attachBaseContext阶段出现异常，忽略并在onCreate中重试
            e.printStackTrace()
        }
    }
    
    /**
     * 应用启动时检测并设置系统语言
     * 返回true表示已自动设置了系统语言，false表示需要用户手动选择
     */
    fun detectAndSetSystemLanguage(): Boolean {
        try {
            val savedIndex = SPStaticUtils.getInt("selectLanguageIndex", -1)
            
            // 如果已经设置过语言，直接返回true
            if (savedIndex != -1) {
                return true
            }
            
            // 尝试匹配系统语言
            val systemLocale = Locale.getDefault()
            val matchedIndex = findMatchingLanguageIndex(systemLocale)
            
            if (matchedIndex != -1) {
                // 找到匹配的语言，保存并返回true
                SPStaticUtils.put("selectLanguageIndex", matchedIndex)
                return true
            }
            
            // 没找到匹配的语言，返回false让用户选择
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 获取当前选中的语言索引
     */
    fun getCurrentLanguageIndex(): Int {
        return SPStaticUtils.getInt("selectLanguageIndex", 0)
    }



}