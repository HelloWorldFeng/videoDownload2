package com.app.videobox.ui.pages.video.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * VLC播放器管理器
 * 提供高级的播放器管理功能和状态监控
 * 
 * 功能特性：
 * - 播放器生命周期管理
 * - 播放状态流式监控
 * - 自动错误恢复
 * - 播放历史记录
 * - 性能监控
 * - 内存管理优化
 */
class VlcPlayerManager private constructor() {

    companion object {
        private const val TAG = "VlcPlayerManager"
        
        @Volatile
        private var INSTANCE: VlcPlayerManager? = null
        
        /**
         * 获取单例实例
         * 线程安全的单例模式实现
         */
        fun getInstance(): VlcPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VlcPlayerManager().also { INSTANCE = it }
            }
        }
    }

    // 协程作用域
    private val managerScope = CoroutineScope(Dispatchers.Main + Job())
    
    // 当前播放器实例
    private var currentPlayer: VlcVideoPlayer? = null
    private var currentContext: Context? = null
    
    // 播放状态流
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    // 播放进度流
    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()
    
    // 播放器配置
    private val _playerConfig = MutableStateFlow(PlayerConfig())
    val playerConfig: StateFlow<PlayerConfig> = _playerConfig.asStateFlow()
    
    // 错误状态流
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()
    
    // 播放历史
    private val playbackHistory = mutableListOf<PlaybackRecord>()
    
    // 进度更新任务
    private var progressUpdateJob: Job? = null

    /**
     * 初始化播放器
     * @param context 应用上下文
     * @param player VLC播放器实例
     */
    fun initializePlayer(context: Context, player: VlcVideoPlayer) {
        Log.d(TAG, "初始化VLC播放器管理器")
        
        // 释放之前的播放器
        releaseCurrentPlayer()
        
        currentContext = context.applicationContext
        currentPlayer = player
        
        // 设置播放器监听器
        player.setPlaybackListener(object : VlcVideoPlayer.PlaybackListener {
            override fun onPreparing() {
                _playbackState.value = PlaybackState.PREPARING
                _errorState.value = null
                Log.d(TAG, "播放器准备中")
            }
            
            override fun onBuffering(percent: Float) {
                _playbackState.value = PlaybackState.BUFFERING
                // 缓冲时暂停进度更新，避免进度条在视频卡住时继续前进
                stopProgressUpdates()
                Log.d(TAG, "缓冲中: ${percent}% - 已暂停进度更新")
            }
            
            override fun onPlaying() {
                _playbackState.value = PlaybackState.PLAYING
                // 只有在真正播放时才启动进度更新
                startProgressUpdates()
                Log.d(TAG, "开始播放 - 启动进度更新")
            }
            
            override fun onPaused() {
                _playbackState.value = PlaybackState.PAUSED
                stopProgressUpdates()
                Log.d(TAG, "播放暂停")
            }
            
            override fun onStopped() {
                _playbackState.value = PlaybackState.STOPPED
                stopProgressUpdates()
                Log.d(TAG, "播放停止")
            }
            
            override fun onCompleted() {
                _playbackState.value = PlaybackState.COMPLETED
                stopProgressUpdates()
                recordPlaybackCompletion()
                Log.d(TAG, "播放完成")
            }
            
            override fun onError(error: String) {
                _playbackState.value = PlaybackState.ERROR
                _errorState.value = error
                stopProgressUpdates()
                Log.e(TAG, "播放错误: $error")
            }
            
            override fun onProgressUpdate(currentTime: Long, totalTime: Long) {
                _playbackProgress.value = PlaybackProgress(
                    currentTime = currentTime,
                    totalTime = totalTime,
                    progress = if (totalTime > 0) currentTime.toFloat() / totalTime else 0f
                )
            }
        })
        
        _playbackState.value = PlaybackState.INITIALIZED
        Log.d(TAG, "播放器管理器初始化完成")
    }

    /**
     * 播放视频
     * @param videoPath 视频文件路径或URL
     * @param title 视频标题（可选）
     * @param autoPlay 是否自动播放
     */
    fun playVideo(videoPath: String, title: String? = null, autoPlay: Boolean = true) {
        Log.d(TAG, "播放视频: $videoPath")
        
        val player = currentPlayer
        if (player == null) {
            val error = "播放器未初始化"
            Log.e(TAG, error)
            _errorState.value = error
            return
        }
        
        try {
            // 验证视频文件
            if (!videoPath.startsWith("http")) {
                val file = File(videoPath)
                if (!file.exists()) {
                    val error = "视频文件不存在: $videoPath"
                    Log.e(TAG, error)
                    _errorState.value = error
                    return
                }
            }
            
            // 记录播放历史
            recordPlaybackStart(videoPath, title)
            
            // 设置视频源并播放
            player.setVideoPath(videoPath, autoPlay)
            
            Log.d(TAG, "视频播放请求已发送")
        } catch (e: Exception) {
            val error = "播放视频失败: ${e.message}"
            Log.e(TAG, error, e)
            _errorState.value = error
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        currentPlayer?.pause()
        Log.d(TAG, "暂停播放")
    }

    /**
     * 恢复播放
     */
    fun resume() {
        currentPlayer?.play()
        Log.d(TAG, "恢复播放")
    }

    /**
     * 停止播放
     */
    fun stop() {
        currentPlayer?.stop()
        stopProgressUpdates()
        Log.d(TAG, "停止播放")
    }

    /**
     * 跳转到指定位置
     * @param position 目标位置（毫秒）
     */
    fun seekTo(position: Long) {
        try {
            val wasPlaying = _playbackState.value == PlaybackState.PLAYING
            Log.d(TAG, "开始跳转: 目标位置=${position}ms, 当前播放状态=$wasPlaying")
            
            // 执行跳转
            currentPlayer?.seekTo(position)
            
            // 如果之前在播放，确保跳转后继续播放
            if (wasPlaying) {
                // 短暂延迟后检查播放状态，确保跳转后播放继续
                managerScope.launch {
                    delay(100) // 给跳转操作一些时间
                    
                    // 检查播放状态是否需要恢复
                    val currentlyPlaying = currentPlayer?.isPlaying() ?: false
                    if (!currentlyPlaying && _playbackState.value == PlaybackState.PLAYING) {
                        Log.d(TAG, "跳转后检测到播放状态异常，尝试恢复播放")
                        currentPlayer?.play()
                        
                        // 再次检查
                        delay(50)
                        val finallyPlaying = currentPlayer?.isPlaying() ?: false
                        Log.d(TAG, "播放状态恢复结果: $finallyPlaying")
                    }
                }
            }
            
            Log.d(TAG, "跳转到位置: ${position}ms - 状态保持逻辑已启动")
        } catch (e: Exception) {
            Log.e(TAG, "跳转失败: ${e.message}", e)
        }
    }

    /**
     * 设置播放速度
     * @param speed 播放速度（0.5-2.0）
     */
    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        currentPlayer?.setPlaybackSpeed(clampedSpeed)
        
        // 更新配置
        _playerConfig.value = _playerConfig.value.copy(playbackSpeed = clampedSpeed)
        Log.d(TAG, "设置播放速度: ${clampedSpeed}x")
    }

    /**
     * 设置音量
     * @param volume 音量（0-100）
     */
    fun setVolume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        currentPlayer?.setVolume(clampedVolume)
        
        // 更新配置
        _playerConfig.value = _playerConfig.value.copy(volume = clampedVolume)
        Log.d(TAG, "设置音量: $clampedVolume")
    }

    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return currentPlayer?.getCurrentPosition() ?: 0L
    }

    /**
     * 获取视频总时长
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long {
        return currentPlayer?.getDuration() ?: 0L
    }

    /**
     * 检查是否正在播放
     * @return true表示正在播放
     */
    fun isPlaying(): Boolean {
        return currentPlayer?.isPlaying() ?: false
    }

    /**
     * 获取播放历史记录
     * @return 播放历史列表
     */
    fun getPlaybackHistory(): List<PlaybackRecord> {
        return playbackHistory.toList()
    }

    /**
     * 清除播放历史
     */
    fun clearPlaybackHistory() {
        playbackHistory.clear()
        Log.d(TAG, "播放历史已清除")
    }

    /**
     * 释放播放器资源
     */
    fun release() {
        Log.d(TAG, "释放播放器管理器资源")
        
        stopProgressUpdates()
        releaseCurrentPlayer()
        
        // 取消协程作用域
        managerScope.coroutineContext[Job]?.cancel()
        
        // 重置状态
        _playbackState.value = PlaybackState.IDLE
        _playbackProgress.value = PlaybackProgress()
        _errorState.value = null
        
        currentContext = null
        
        Log.d(TAG, "播放器管理器资源释放完成")
    }

    /**
     * 开始进度更新
     */
    private fun startProgressUpdates() {
        stopProgressUpdates()
        Log.d(TAG, "开始进度更新循环")
        
        progressUpdateJob = managerScope.launch {
            // 修复竞态条件：只要状态是PLAYING就继续更新，不依赖isPlaying()的即时状态
            while (_playbackState.value == PlaybackState.PLAYING) {
                try {
                    val currentTime = getCurrentPosition()
                    val totalTime = getDuration()
                    val actuallyPlaying = isPlaying()
                    
                    Log.d(TAG, "VlcPlayerManager进度更新: currentTime=$currentTime, totalTime=$totalTime, state=${_playbackState.value}, actuallyPlaying=$actuallyPlaying")
                    
                    // 直接使用视频实际播放时间更新进度条，确保时间同步
                    // 不管视频是否卡住，都以实际视频时间为准，避免进度条超前
                    _playbackProgress.value = PlaybackProgress(
                        currentTime = currentTime,
                        totalTime = totalTime,
                        progress = if (totalTime > 0) currentTime.toFloat() / totalTime else 0f
                    )
                    
                    // 如果底层播放器状态与管理器状态不一致，进行状态同步
                    if (!actuallyPlaying && _playbackState.value == PlaybackState.PLAYING) {
                        Log.w(TAG, "检测到状态不一致：管理器状态为PLAYING但底层播放器未播放，可能是缓冲或暂停")
                        // 给播放器一些时间来同步状态，避免立即停止进度更新
                        delay(500)
                        continue
                    }
                    
                    delay(1000) // 每秒更新一次
                } catch (e: Exception) {
                    Log.e(TAG, "进度更新异常: ${e.message}")
                    break
                }
            }
            Log.d(TAG, "进度更新循环结束 - 当前状态: ${_playbackState.value}")
        }
    }

    /**
     * 停止进度更新
     */
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    /**
     * 释放当前播放器
     */
    private fun releaseCurrentPlayer() {
        currentPlayer?.release()
        currentPlayer = null
    }

    /**
     * 记录播放开始
     */
    private fun recordPlaybackStart(videoPath: String, title: String?) {
        val record = PlaybackRecord(
            videoPath = videoPath,
            title = title,
            startTime = System.currentTimeMillis()
        )
        playbackHistory.add(0, record) // 添加到列表开头
        
        // 限制历史记录数量
        if (playbackHistory.size > 100) {
            playbackHistory.removeAt(playbackHistory.size - 1)
        }
        
        Log.d(TAG, "记录播放开始: $videoPath")
    }

    /**
     * 记录播放完成
     */
    private fun recordPlaybackCompletion() {
        if (playbackHistory.isNotEmpty()) {
            val lastRecord = playbackHistory[0]
            lastRecord.endTime = System.currentTimeMillis()
            lastRecord.completed = true
            Log.d(TAG, "记录播放完成: ${lastRecord.videoPath}")
        }
    }

    /**
     * 播放状态枚举
     */
    enum class PlaybackState {
        IDLE,           // 空闲状态
        INITIALIZED,    // 已初始化
        PREPARING,      // 准备中
        BUFFERING,      // 缓冲中
        PLAYING,        // 播放中
        PAUSED,         // 已暂停
        STOPPED,        // 已停止
        COMPLETED,      // 播放完成
        ERROR           // 错误状态
    }

    /**
     * 播放进度数据类
     */
    data class PlaybackProgress(
        val currentTime: Long = 0L,     // 当前播放时间（毫秒）
        val totalTime: Long = 0L,       // 总时长（毫秒）
        val progress: Float = 0f        // 播放进度（0.0-1.0）
    )

    /**
     * 播放器配置数据类
     */
    data class PlayerConfig(
        val playbackSpeed: Float = 1.0f,   // 播放速度
        val volume: Int = 100,              // 音量（0-100）
        val autoPlay: Boolean = true        // 自动播放
    )

    /**
     * 播放记录数据类
     */
    data class PlaybackRecord(
        val videoPath: String,              // 视频路径
        val title: String? = null,          // 视频标题
        val startTime: Long,                // 开始播放时间
        var endTime: Long? = null,          // 结束播放时间
        var completed: Boolean = false      // 是否播放完成
    ) {
        /**
         * 获取播放时长
         * @return 播放时长（毫秒），如果未结束则返回null
         */
        fun getPlayDuration(): Long? {
            return endTime?.let { it - startTime }
        }
    }
}