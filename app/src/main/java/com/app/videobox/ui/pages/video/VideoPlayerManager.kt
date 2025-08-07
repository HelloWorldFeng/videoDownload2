package com.app.videobox.ui.pages.video

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 视频播放管理器 - VideoPlayerManager
 * 统一管理视频播放器的启动逻辑，支持新旧播放器切换
 * 混淆命名：VideoPlayerManager (新增的播放器管理组件)
 * 功能：提供统一的视频播放入口，便于后续播放器切换和维护
 */
object VideoPlayerManager {
    
    private const val TAG = "VideoPlayerManager"
    
    // 播放器类型枚举 - 混淆命名
    enum class PlayerType {
        LEGACY,    // 原有播放器 (VideoPlayActivity)
        MODERN,    // 重构播放器 (StreamPlayActivity)
        FIGMA      // Figma设计播放器 (FigmaPlayerActivity)
    }
    
    // 当前使用的播放器类型 - 可通过配置切换
    private var currentPlayerType: PlayerType = PlayerType.FIGMA
    
    /**
     * 启动视频播放 - 统一入口方法
     * 混淆命名：launchVideoPlayer (原直接启动Activity的逻辑)
     * 
     * @param context 上下文
     * @param videoUrl 视频URL
     * @param videoTitle 视频标题
     * @param playerType 指定播放器类型（可选，默认使用当前配置）
     */
    fun launchVideoPlayer(
        context: Context,
        videoUrl: String,
        videoTitle: String,
        playerType: PlayerType? = null
    ) {
        val targetPlayerType = playerType ?: currentPlayerType
        
        Log.d(TAG, "启动视频播放器 - 类型: $targetPlayerType, URL: $videoUrl, 标题: $videoTitle")
        
        try {
            val intent = when (targetPlayerType) {
                PlayerType.LEGACY -> createLegacyPlayerIntent(context, videoUrl, videoTitle)
                PlayerType.MODERN -> createModernPlayerIntent(context, videoUrl, videoTitle)
                PlayerType.FIGMA -> createFigmaPlayerIntent(context, videoUrl, videoTitle)
            }
            
            context.startActivity(intent)
            Log.d(TAG, "视频播放器启动成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "启动视频播放器失败: ${e.message}", e)
            
            // 如果当前播放器启动失败，回退到传统播放器
            if (targetPlayerType != PlayerType.LEGACY) {
                Log.w(TAG, "${targetPlayerType}播放器启动失败，回退到传统播放器")
                launchVideoPlayer(context, videoUrl, videoTitle, PlayerType.LEGACY)
            }
        }
    }
    
    /**
     * 创建传统播放器Intent - 混淆命名
     * 原VideoPlayActivity启动逻辑的封装
     */
    private fun createLegacyPlayerIntent(
        context: Context,
        videoUrl: String,
        videoTitle: String
    ): Intent {
        Log.d(TAG, "创建传统播放器Intent")
        
        return Intent(context, VideoPlayActivity::class.java).apply {
            putExtra("video_url", videoUrl)  // 保持原有参数名
            putExtra("title", videoTitle)    // 保持原有参数名
        }
    }
    
    /**
     * 创建现代播放器Intent - 混淆命名
     * 新StreamPlayActivity启动逻辑
     */
    private fun createModernPlayerIntent(
        context: Context,
        videoUrl: String,
        videoTitle: String
    ): Intent {
        Log.d(TAG, "创建现代播放器Intent")
        
        return Intent(context, StreamPlayActivity::class.java).apply {
            putExtra(StreamPlayActivity.EXTRA_MEDIA_URL, videoUrl)
            putExtra(StreamPlayActivity.EXTRA_MEDIA_TITLE, videoTitle)
        }
    }
    
    /**
     * 创建Figma播放器Intent
     * 使用FigmaPlayerActivity
     */
    private fun createFigmaPlayerIntent(
        context: Context,
        videoUrl: String,
        videoTitle: String
    ): Intent {
        Log.d(TAG, "创建Figma播放器Intent")
        
        return Intent(context, FigmaPlayerActivity::class.java).apply {
            putExtra(FigmaPlayerActivity.EXTRA_VIDEO_URL, videoUrl)
            putExtra(FigmaPlayerActivity.EXTRA_VIDEO_TITLE, videoTitle)
            
            // 添加Activity启动标志
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            Log.d(TAG, "Figma播放器Intent创建完成")
        }
    }
    
    /**
     * 设置默认播放器类型 - 混淆命名
     * 用于全局切换播放器类型
     */
    fun setDefaultPlayerType(playerType: PlayerType) {
        Log.d(TAG, "设置默认播放器类型: $playerType")
        currentPlayerType = playerType
    }
    
    /**
     * 获取当前播放器类型 - 混淆命名
     */
    fun getCurrentPlayerType(): PlayerType {
        return currentPlayerType
    }
    
    /**
     * 检查播放器可用性 - 混淆命名
     * 用于检测特定播放器是否可用
     */
    fun isPlayerAvailable(context: Context, playerType: PlayerType): Boolean {
        return try {
            val activityClass = when (playerType) {
                PlayerType.LEGACY -> VideoPlayActivity::class.java
                PlayerType.MODERN -> StreamPlayActivity::class.java
                PlayerType.FIGMA -> FigmaPlayerActivity::class.java
            }
            
            val intent = Intent(context, activityClass)
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            val isAvailable = resolveInfo != null
            
            Log.d(TAG, "播放器可用性检查 - $playerType: $isAvailable")
            isAvailable
            
        } catch (e: Exception) {
            Log.e(TAG, "检查播放器可用性失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取播放器信息 - 混淆命名
     * 用于调试和日志记录
     */
    fun getPlayerInfo(): String {
        return "当前播放器: $currentPlayerType, 支持类型: ${PlayerType.values().joinToString(", ")}"
    }
    
    /**
     * 兼容性方法 - 保持与原有调用方式的兼容
     * 混淆命名：startVideoPlayActivity (原直接启动Activity的方法名)
     */
    @Deprecated(
        message = "请使用 launchVideoPlayer 方法",
        replaceWith = ReplaceWith("launchVideoPlayer(context, videoUrl, videoTitle)")
    )
    fun startVideoPlayActivity(
        context: Context,
        videoUrl: String,
        videoTitle: String
    ) {
        Log.w(TAG, "使用了已废弃的方法 startVideoPlayActivity，建议使用 launchVideoPlayer")
        launchVideoPlayer(context, videoUrl, videoTitle)
    }
}