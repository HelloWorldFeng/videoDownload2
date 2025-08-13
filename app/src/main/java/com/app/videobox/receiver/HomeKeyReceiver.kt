package com.app.videobox.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app.videobox.utils.NotifyHelper
import com.blankj.utilcode.util.SPStaticUtils

/**
 * Home键点击广播接收器
 * 
 * 注意：由于Android系统安全限制，无法直接监听Home键的物理按下事件。
 * 此接收器通过监听系统广播来间接检测用户返回桌面的行为。
 * 
 * 支持的检测方式：
 * 1. ACTION_CLOSE_SYSTEM_DIALOGS - 系统对话框关闭时触发（包括Home键）
 * 2. 配合应用生命周期管理，检测应用进入后台的情况
 * 
 * 生产环境使用说明：
 * - 需要在AndroidManifest.xml中注册相应的Intent Filter
 * - 建议配合Activity生命周期回调使用，提高检测准确性
 * - 考虑不同Android版本的兼容性问题
 * 
 * @author VideoBox Team
 * @since 2025-01-11
 */
class HomeKeyReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "HomeKeyReceiver"
        
        // Home键检测相关常量
        private const val SYSTEM_DIALOG_REASON_KEY = "reason"
        private const val SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions"
        private const val SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps"
        private const val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"
        private const val SYSTEM_DIALOG_REASON_ASSIST = "assist"
        
        // SharedPreferences键名，用于控制Home键检测功能开关
        private const val HOME_KEY_DETECTION_ENABLED = "home_key_detection_enabled"
    }
    
    /**
     * 广播接收处理方法
     * 
     * @param context 应用上下文
     * @param intent 接收到的Intent，包含广播相关信息
     */
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "HomeKeyReceiver接收到广播: ${intent?.action}")
        
        intent?.let { receivedIntent ->
            when (receivedIntent.action) {
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    handleSystemDialogClose(context, receivedIntent)
                }
                // 可以根据需要添加其他相关的系统广播监听
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "屏幕关闭，可能是通过电源键")
                    handleScreenOff(context)
                }
                else -> {
                    Log.d(TAG, "接收到其他系统广播: ${receivedIntent.action}")
                }
            }
        }
    }
    
    /**
     * 处理系统对话框关闭事件
     * 这是检测Home键点击的主要方法
     * 
     * @param context 应用上下文
     * @param intent 包含关闭原因的Intent
     */
    private fun handleSystemDialogClose(context: Context, intent: Intent) {
        val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
        
        Log.d(TAG, "系统对话框关闭，原因: $reason")
        
        when (reason) {
            SYSTEM_DIALOG_REASON_HOME_KEY -> {
                // 检测到Home键点击
                handleHomeKeyPressed(context)
            }
            SYSTEM_DIALOG_REASON_RECENT_APPS -> {
                // 最近任务键（多任务键）点击
                Log.d(TAG, "检测到最近任务键点击")
                handleRecentAppsPressed(context)
            }
            SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS -> {
                // 长按电源键等全局操作
                Log.d(TAG, "检测到全局操作（如长按电源键）")
            }
            SYSTEM_DIALOG_REASON_ASSIST -> {
                // 助手键点击
                Log.d(TAG, "检测到助手键点击")
            }
            else -> {
                Log.d(TAG, "未知的系统对话框关闭原因: $reason")
            }
        }
    }
    
    /**
     * 处理Home键点击事件
     * 这是核心的业务逻辑处理方法
     * 
     * @param context 应用上下文
     */
    private fun handleHomeKeyPressed(context: Context) {
        Log.d(TAG, "检测到Home键点击事件")
        
        // 检查功能是否启用
        if (!isHomeKeyDetectionEnabled()) {
            Log.d(TAG, "Home键检测功能已禁用，跳过处理")
            return
        }
        
        try {
            // 发送通知或执行相应的业务逻辑
            // 这里参考其他Receiver的实现模式
            NotifyHelper.sendApiNotification(context, "homekey")
            
            // 记录用户行为统计（如果需要）
            recordHomeKeyUsage(context)
            
            Log.i(TAG, "Home键点击事件处理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "处理Home键点击事件时发生错误: ${e.message}", e)
        }
    }
    
    /**
     * 处理最近任务键点击事件
     * 
     * @param context 应用上下文
     */
    private fun handleRecentAppsPressed(context: Context) {
        Log.d(TAG, "用户点击了最近任务键")
        
        // 可以根据业务需求添加相应的处理逻辑
        // 例如：统计用户使用习惯、触发特定功能等
    }
    
    /**
     * 处理屏幕关闭事件
     * 
     * @param context 应用上下文
     */
    private fun handleScreenOff(context: Context) {
        Log.d(TAG, "屏幕已关闭")
        
        // 可以在这里添加屏幕关闭时的处理逻辑
        // 例如：暂停某些后台任务、保存用户状态等
    }
    
    /**
     * 检查Home键检测功能是否启用
     * 
     * @return true表示功能启用，false表示功能禁用
     */
    private fun isHomeKeyDetectionEnabled(): Boolean {
        return SPStaticUtils.getBoolean(HOME_KEY_DETECTION_ENABLED, true)
    }
    
    /**
     * 记录Home键使用统计
     * 用于分析用户行为模式，优化应用体验
     * 
     * @param context 应用上下文
     */
    private fun recordHomeKeyUsage(context: Context) {
        try {
            // 记录使用时间戳
            val currentTime = System.currentTimeMillis()
            SPStaticUtils.put("last_home_key_time", currentTime)
            
            // 增加使用次数计数
            val currentCount = SPStaticUtils.getInt("home_key_usage_count", 0)
            SPStaticUtils.put("home_key_usage_count", currentCount + 1)
            
            Log.d(TAG, "Home键使用统计已更新，总使用次数: ${currentCount + 1}")
            
        } catch (e: Exception) {
            Log.e(TAG, "记录Home键使用统计时发生错误: ${e.message}", e)
        }
    }
    
    /**
     * 启用Home键检测功能
     * 提供给外部调用的公共方法
     */
    fun enableHomeKeyDetection() {
        SPStaticUtils.put(HOME_KEY_DETECTION_ENABLED, true)
        Log.i(TAG, "Home键检测功能已启用")
    }
    
    /**
     * 禁用Home键检测功能
     * 提供给外部调用的公共方法
     */
    fun disableHomeKeyDetection() {
        SPStaticUtils.put(HOME_KEY_DETECTION_ENABLED, false)
        Log.i(TAG, "Home键检测功能已禁用")
    }
}