package com.videodownloader.module.api

import android.util.Log

/**
 * 下载事件回调桥接（供 app 模块注册监听）
 * 仅关注与视频下载相关的事件回传，避免侵入 app 业务层。
 */
object DlEvtBridge {

    private const val TAG = "DlEvtBridge"

    /**
     * 对外暴露的下载事件监听器接口
     * app 模块可通过 setListener() 注入实现
     */
    interface OnDlEvtListener {
        /**
         * MP4 下载成功事件
         * @param taskId 任务ID
         * @param outputPaths 成功输出的文件路径列表
         */
        fun onMp4DownloadSuccess(taskId: String, outputPaths: List<String>)
    }

    @Volatile
    private var listener: OnDlEvtListener? = null

    /**
     * 由 app 模块调用：设置/清空监听器
     */
    fun setListener(l: OnDlEvtListener?) {
        Log.d(TAG, "设置下载事件监听器: ${l != null}")
        listener = l
    }

    /**
     * 由下载模块内部调用：分发 MP4 下载成功事件
     */
    fun notifyMp4Success(taskId: String, outputPaths: List<String>) {
        try {
            val l = listener
            if (l == null) {
                Log.d(TAG, "无监听器已注册，跳过回调。taskId=$taskId, outputs=${outputPaths.size}")
                return
            }
            Log.d(TAG, "触发MP4下载成功回调 -> taskId=$taskId, outputs=${outputPaths.size}")
            l.onMp4DownloadSuccess(taskId, outputPaths)
        } catch (e: Throwable) {
            Log.e(TAG, "分发MP4下载成功回调异常: ${e.message}", e)
        }
    }
} 