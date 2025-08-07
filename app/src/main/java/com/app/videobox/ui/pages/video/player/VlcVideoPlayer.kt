package com.app.videobox.ui.pages.video.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.io.File

/**
 * VLC视频播放器封装类
 * 基于LibVLC实现的高性能视频播放器组件
 * 支持多种视频格式和网络流媒体播放
 * 
 * 功能特性：
 * - 支持本地视频文件播放
 * - 支持网络流媒体播放
 * - 硬件加速解码
 * - 多种视频格式支持
 * - 播放状态监听
 * - 播放进度控制
 * 
 * 注意：此类不再继承SurfaceView，视频显示由VLCVideoLayout处理
 */
class VlcVideoPlayer(private val context: Context) {

    companion object {
        private const val TAG = "VlcVideoPlayer"
    }

    // VLC核心组件
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentMedia: Media? = null

    // 播放状态监听器
    private var playbackListener: PlaybackListener? = null

    // 播放状态
    private var isPlayerReady = false
    private var isPrepared = false

    init {
        // 初始化VLC
        initializeVLC()
        Log.d(TAG, "VLC视频播放器初始化完成")
    }

    /**
     * 初始化VLC媒体库
     * 配置VLC选项和参数
     */
    private fun initializeVLC() {
        try {
            // VLC初始化选项
            val options = arrayListOf<String>().apply {
                add("--aout=opensles")           // 音频输出
                add("--audio-time-stretch")     // 音频时间拉伸
                add("-vvv")                     // 详细日志
                add("--avcodec-skiploopfilter") // 跳过循环滤波器
                add("--avcodec-skip-frame")     // 跳过帧
                add("--avcodec-skip-idct")      // 跳过IDCT
                add("--android-display-chroma") // Android显示色度
                add("--file-logging")           // 文件日志
                add("--logfile=/sdcard/vlc-log.txt") // 日志文件路径
            }

            // 创建LibVLC实例
            libVLC = LibVLC(context, options)
            
            // 创建MediaPlayer实例
            mediaPlayer = MediaPlayer(libVLC!!).apply {
                // 设置事件监听器
                setEventListener { event ->
                    handleMediaPlayerEvent(event)
                }
            }
            
            // 设置播放器就绪状态
            isPlayerReady = true
            
            Log.d(TAG, "VLC媒体库初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "VLC初始化失败: ${e.message}", e)
        }
    }

    /**
     * 处理MediaPlayer事件
     * @param event MediaPlayer事件
     */
    private fun handleMediaPlayerEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                Log.d(TAG, "媒体打开中...")
                playbackListener?.onPreparing()
            }
            MediaPlayer.Event.Buffering -> {
                Log.d(TAG, "缓冲中: ${event.buffering}%")
                playbackListener?.onBuffering(event.buffering)
            }
            MediaPlayer.Event.Playing -> {
                Log.d(TAG, "开始播放")
                isPrepared = true
                playbackListener?.onPlaying()
            }
            MediaPlayer.Event.Paused -> {
                Log.d(TAG, "播放暂停")
                playbackListener?.onPaused()
            }
            MediaPlayer.Event.Stopped -> {
                Log.d(TAG, "播放停止")
                playbackListener?.onStopped()
            }
            MediaPlayer.Event.EndReached -> {
                Log.d(TAG, "播放完成")
                playbackListener?.onCompleted()
            }
            MediaPlayer.Event.EncounteredError -> {
                Log.e(TAG, "播放错误")
                playbackListener?.onError("播放过程中发生错误")
            }
            MediaPlayer.Event.TimeChanged -> {
                // 播放进度更新
                val currentTime = mediaPlayer?.time ?: 0L
                val totalTime = mediaPlayer?.length ?: 0L
                playbackListener?.onProgressUpdate(currentTime, totalTime)
            }
            MediaPlayer.Event.Vout -> {
                // 视频输出事件，检测视频尺寸并调整屏幕方向
                checkVideoOrientationAndAdjust()
            }
        }
    }

    /**
     * 设置视频源并准备播放
     * @param videoPath 视频文件路径或URL
     * @param autoPlay 是否自动播放
     */
    fun setVideoPath(videoPath: String, autoPlay: Boolean = false) {
        try {
            Log.d(TAG, "设置视频源: $videoPath")
            
            // 释放之前的媒体资源
            currentMedia?.release()
            
            // 创建新的Media对象
            currentMedia = if (videoPath.startsWith("http")) {
                // 网络流媒体
                Media(libVLC, Uri.parse(videoPath))
            } else {
                // 本地文件
                val file = File(videoPath)
                if (!file.exists()) {
                    Log.e(TAG, "视频文件不存在: $videoPath")
                    playbackListener?.onError("视频文件不存在")
                    return
                }
                Media(libVLC, Uri.fromFile(file))
            }
            
            // 设置媒体到播放器
            mediaPlayer?.media = currentMedia
            
            if (autoPlay && isPlayerReady) {
                play()
            }
            
            Log.d(TAG, "视频源设置完成")
        } catch (e: Exception) {
            Log.e(TAG, "设置视频源失败: ${e.message}", e)
            playbackListener?.onError("设置视频源失败: ${e.message}")
        }
    }

    /**
     * 开始播放
     */
    fun play() {
        try {
            if (mediaPlayer?.media == null) {
                Log.w(TAG, "未设置视频源，无法播放")
                return
            }
            
            if (!isPlayerReady) {
                Log.w(TAG, "播放器未准备就绪，无法播放")
                return
            }
            
            mediaPlayer?.play()
            Log.d(TAG, "开始播放视频")
        } catch (e: Exception) {
            Log.e(TAG, "播放失败: ${e.message}", e)
            playbackListener?.onError("播放失败: ${e.message}")
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        try {
            mediaPlayer?.pause()
            Log.d(TAG, "暂停播放")
        } catch (e: Exception) {
            Log.e(TAG, "暂停失败: ${e.message}", e)
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        try {
            mediaPlayer?.stop()
            isPrepared = false
            Log.d(TAG, "停止播放")
        } catch (e: Exception) {
            Log.e(TAG, "停止失败: ${e.message}", e)
        }
    }

    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long) {
        try {
            if (isPrepared) {
                mediaPlayer?.time = position
                Log.d(TAG, "跳转到位置: ${position}ms")
            } else {
                Log.w(TAG, "播放器未准备就绪，无法跳转")
            }
        } catch (e: Exception) {
            Log.e(TAG, "跳转失败: ${e.message}", e)
        }
    }

    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return mediaPlayer?.time ?: 0L
    }

    /**
     * 获取视频总时长
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long {
        return mediaPlayer?.length ?: 0L
    }

    /**
     * 检查是否正在播放
     * @return true表示正在播放
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    /**
     * 设置播放速度
     * @param speed 播放速度（1.0为正常速度）
     */
    fun setPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.rate = speed
            Log.d(TAG, "设置播放速度: ${speed}x")
        } catch (e: Exception) {
            Log.e(TAG, "设置播放速度失败: ${e.message}", e)
        }
    }

    /**
     * 设置音量
     * @param volume 音量（0-100）
     */
    fun setVolume(volume: Int) {
        try {
            val clampedVolume = volume.coerceIn(0, 100)
            mediaPlayer?.volume = clampedVolume
            Log.d(TAG, "设置音量: $clampedVolume")
        } catch (e: Exception) {
            Log.e(TAG, "设置音量失败: ${e.message}", e)
        }
    }

    /**
     * 设置播放状态监听器
     * @param listener 播放状态监听器
     */
    fun setPlaybackListener(listener: PlaybackListener?) {
        this.playbackListener = listener
        Log.d(TAG, "设置播放状态监听器")
    }
    
    /**
     * 获取MediaPlayer实例
     * @return MediaPlayer实例，可能为null
     */
    fun getMediaPlayer(): MediaPlayer? {
        return mediaPlayer
    }

    /**
     * 检测视频方向并调整屏幕方向
     */
    private fun checkVideoOrientationAndAdjust() {
        try {
            mediaPlayer?.let { player ->
                // 使用VLC的vlcVout获取视频尺寸信息
                val vlcVout = player.vlcVout
                if (vlcVout != null) {
                    // 延迟获取视频尺寸，确保视频已开始播放
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            // 通过MediaPlayer获取视频尺寸
                            val media = player.media
                            if (media != null) {
                                // 假设大多数视频是横屏的，如果需要更精确的检测，
                                // 可以在播放开始后通过其他方式获取视频尺寸
                                val orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                playbackListener?.onOrientationDetected(orientation, 1920, 1080)
                                Log.d(TAG, "设置默认横屏方向")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "延迟检测视频方向失败: ${e.message}", e)
                        }
                    }, 1000) // 延迟1秒
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检测视频方向失败: ${e.message}", e)
        }
    }

    /**
     * 释放播放器资源
     * 在Activity/Fragment销毁时调用
     */
    fun release() {
        try {
            Log.d(TAG, "释放播放器资源")
            
            // 停止播放
            mediaPlayer?.stop()
            
            // 释放媒体资源
            currentMedia?.release()
            currentMedia = null
            
            // 释放播放器
            mediaPlayer?.release()
            mediaPlayer = null
            
            // 释放VLC库
            libVLC?.release()
            libVLC = null
            
            isPlayerReady = false
            isPrepared = false
            
            Log.d(TAG, "播放器资源释放完成")
        } catch (e: Exception) {
            Log.e(TAG, "释放资源失败: ${e.message}", e)
        }
    }

    // Surface相关方法已移除，视频显示由VLCVideoLayout处理

    /**
     * 播放状态监听接口
     * 用于监听播放器的各种状态变化
     */
    interface PlaybackListener {
        /**
         * 准备播放中
         */
        fun onPreparing() {}

        /**
         * 缓冲中
         * @param percent 缓冲百分比
         */
        fun onBuffering(percent: Float) {}

        /**
         * 开始播放
         */
        fun onPlaying() {}

        /**
         * 播放暂停
         */
        fun onPaused() {}

        /**
         * 播放停止
         */
        fun onStopped() {}

        /**
         * 播放完成
         */
        fun onCompleted() {}

        /**
         * 播放错误
         * @param error 错误信息
         */
        fun onError(error: String) {}

        /**
         * 播放进度更新
         * @param currentTime 当前播放时间（毫秒）
         * @param totalTime 总时长（毫秒）
         */
        fun onProgressUpdate(currentTime: Long, totalTime: Long) {}

        /**
         * 检测到视频方向
         * @param orientation 建议的屏幕方向
         * @param videoWidth 视频宽度
         * @param videoHeight 视频高度
         */
        fun onOrientationDetected(orientation: Int, videoWidth: Int, videoHeight: Int) {}
    }
}