package com.app.videobox.ui.pages.video.playerV2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * 视频播放进度管理器
 * 负责保存和恢复用户的视频播放进度，实现断点续播功能
 * 
 * 功能特性：
 * - 自动保存播放进度（每5秒或进度变化超过5%时保存）
 * - 支持多视频进度独立管理
 * - 使用视频URL的MD5哈希作为唯一标识
 * - 异步操作，不阻塞UI线程
 * - 自动清理过期进度记录（超过30天）
 * 
 * 使用方法：
 * ```kotlin
 * val progressManager = VideoProgressManager(context)
 * 
 * // 保存进度
 * progressManager.saveProgress(videoUrl, currentPosition, totalDuration)
 * 
 * // 恢复进度
 * val savedProgress = progressManager.getProgress(videoUrl)
 * if (savedProgress != null && savedProgress.isValid()) {
 *     // 跳转到上次播放位置
 *     player.seekTo(savedProgress.position)
 * }
 * ```
 */
class VideoProgressManager(private val context: Context) {
    
    companion object {
        private const val PROGRESS_MANAGER_TAG = "VideoProgressManager"
        private const val PREFS_NAME = "video_progress_prefs"
        
        // 进度保存策略配置
        private const val MIN_SAVE_INTERVAL_MS = 5000L        // 最小保存间隔：5秒
        private const val MIN_PROGRESS_CHANGE_PERCENT = 0.05f  // 最小进度变化：5%
        private const val MIN_VALID_DURATION_MS = 5000L        // 最小有效视频时长：5秒
        private const val MIN_VALID_POSITION_MS = 3000L        // 最小有效播放位置：3秒
        private const val MAX_VALID_POSITION_PERCENT = 0.95f   // 最大有效播放位置：95%
        
        // 数据清理配置
        private const val PROGRESS_EXPIRE_DAYS = 30            // 进度记录过期天数：30天
        private const val CLEANUP_CHECK_INTERVAL_DAYS = 7      // 清理检查间隔：7天
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 进度保存状态跟踪
    private val lastSaveTimeMap = mutableMapOf<String, Long>()
    private val lastSavedPositionMap = mutableMapOf<String, Long>()
    
    /**
     * 视频播放进度数据类
     * @param position 播放位置（毫秒）
     * @param duration 视频总时长（毫秒）
     * @param timestamp 保存时间戳
     * @param videoUrl 视频URL（用于验证）
     */
    data class VideoProgress(
        val position: Long,
        val duration: Long,
        val timestamp: Long,
        val videoUrl: String
    ) {
        /**
         * 检查进度数据是否有效
         * @return true表示进度有效，可以用于恢复播放
         */
        fun isValid(): Boolean {
            return position > MIN_VALID_POSITION_MS && 
                   duration > MIN_VALID_DURATION_MS &&
                   position < duration * MAX_VALID_POSITION_PERCENT &&
                   System.currentTimeMillis() - timestamp < PROGRESS_EXPIRE_DAYS * 24 * 60 * 60 * 1000L
        }
        
        /**
         * 获取播放进度百分比
         * @return 播放进度百分比（0.0-1.0）
         */
        fun getProgressPercent(): Float {
            return if (duration > 0) position.toFloat() / duration.toFloat() else 0f
        }
        
        /**
         * 获取剩余播放时间
         * @return 剩余时间（毫秒）
         */
        fun getRemainingTime(): Long {
            return (duration - position).coerceAtLeast(0L)
        }
    }
    
    /**
     * 保存视频播放进度
     * 采用智能保存策略，避免频繁写入存储
     * 
     * @param videoUrl 视频URL
     * @param currentPosition 当前播放位置（毫秒）
     * @param totalDuration 视频总时长（毫秒）
     * @return true表示保存成功，false表示跳过保存或保存失败
     */
    suspend fun saveProgress(
        videoUrl: String, 
        currentPosition: Long, 
        totalDuration: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(PROGRESS_MANAGER_TAG, "[进度保存] 开始保存进度 - URL: ${videoUrl.take(50)}..., 位置: ${currentPosition}ms, 总时长: ${totalDuration}ms")
            
            // 参数有效性检查
            if (videoUrl.isBlank() || totalDuration < MIN_VALID_DURATION_MS) {
                Log.w(PROGRESS_MANAGER_TAG, "[进度保存] 跳过保存 - 无效参数: URL=${if(videoUrl.isBlank()) "空" else videoUrl.take(30)}, 时长=${totalDuration}ms (最小要求${MIN_VALID_DURATION_MS}ms)")
                return@withContext false
            }
            
            // 播放位置有效性检查
            if (currentPosition < MIN_VALID_POSITION_MS || 
                currentPosition > totalDuration * MAX_VALID_POSITION_PERCENT) {
                Log.w(PROGRESS_MANAGER_TAG, "[进度保存] 跳过保存 - 无效位置: ${currentPosition}ms/${totalDuration}ms (有效范围: ${MIN_VALID_POSITION_MS}ms - ${(totalDuration * MAX_VALID_POSITION_PERCENT).toLong()}ms)")
                return@withContext false
            }
            
            val videoKey = generateVideoKey(videoUrl)
            val currentTime = System.currentTimeMillis()
            
            // 检查保存间隔策略
            val lastSaveTime = lastSaveTimeMap[videoKey] ?: 0L
            val lastSavedPosition = lastSavedPositionMap[videoKey] ?: 0L
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度保存] 策略检查 - 视频Key: ${videoKey.take(8)}..., 上次保存时间: ${if(lastSaveTime > 0) "${currentTime - lastSaveTime}ms前" else "无"}, 上次位置: ${lastSavedPosition}ms")
            
            val timeSinceLastSave = currentTime - lastSaveTime
            val positionChange = kotlin.math.abs(currentPosition - lastSavedPosition)
            val positionChangePercent = positionChange.toFloat() / totalDuration.toFloat()
            
            // 智能保存策略：时间间隔或进度变化达到阈值时保存
            val shouldSave = timeSinceLastSave >= MIN_SAVE_INTERVAL_MS || 
                           positionChangePercent >= MIN_PROGRESS_CHANGE_PERCENT
            
            if (!shouldSave) {
                Log.v(PROGRESS_MANAGER_TAG, "[进度保存] 跳过保存 - 未达到保存条件: 间隔=${timeSinceLastSave}ms (需要>=${MIN_SAVE_INTERVAL_MS}ms), 变化=${String.format("%.2f", positionChangePercent * 100)}% (需要>=${String.format("%.1f", MIN_PROGRESS_CHANGE_PERCENT * 100)}%)")
                return@withContext false
            }
            
            Log.i(PROGRESS_MANAGER_TAG, "[进度保存] 满足保存条件 - 时间间隔: ${timeSinceLastSave}ms, 进度变化: ${String.format("%.2f", positionChangePercent * 100)}%")
            
            // 创建进度对象并保存
            val progress = VideoProgress(
                position = currentPosition,
                duration = totalDuration,
                timestamp = currentTime,
                videoUrl = videoUrl
            )
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度保存] 开始写入存储 - 进度对象: 位置=${progress.position}ms, 时长=${progress.duration}ms, 百分比=${String.format("%.1f", progress.getProgressPercent() * 100)}%")
            
            val progressJson = serializeProgress(progress)
            sharedPreferences.edit()
                .putString(videoKey, progressJson)
                .putLong("${videoKey}_timestamp", currentTime)
                .apply()
            
            // 更新保存状态跟踪
            lastSaveTimeMap[videoKey] = currentTime
            lastSavedPositionMap[videoKey] = currentPosition
            
            Log.i(PROGRESS_MANAGER_TAG, "[进度保存] 保存成功 - 视频: ${videoUrl.take(50)}..., 位置: ${currentPosition}ms/${totalDuration}ms (${String.format("%.1f", progress.getProgressPercent() * 100)}%), 序列化数据长度: ${progressJson.length}字符")
            
            // 异步执行清理任务
            performPeriodicCleanup()
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(PROGRESS_MANAGER_TAG, "保存进度失败 - 视频: ${videoUrl.take(50)}", e)
            return@withContext false
        }
    }
    
    /**
     * 获取视频播放进度
     * 
     * @param videoUrl 视频URL
     * @return 播放进度对象，如果没有保存的进度或进度无效则返回null
     */
    suspend fun getProgress(videoUrl: String): VideoProgress? = withContext(Dispatchers.IO) {
        try {
            Log.d(PROGRESS_MANAGER_TAG, "[进度获取] 开始获取进度 - URL: ${videoUrl.take(50)}...")
            
            if (videoUrl.isBlank()) {
                Log.w(PROGRESS_MANAGER_TAG, "[进度获取] 获取失败 - 视频URL为空")
                return@withContext null
            }
            
            val videoKey = generateVideoKey(videoUrl)
            Log.d(PROGRESS_MANAGER_TAG, "[进度获取] 生成视频Key: ${videoKey.take(8)}...")
            
            val progressJson = sharedPreferences.getString(videoKey, null)
            
            if (progressJson.isNullOrBlank()) {
                Log.i(PROGRESS_MANAGER_TAG, "[进度获取] 未找到进度记录 - 视频: ${videoUrl.take(50)}..., Key: ${videoKey.take(8)}...")
                return@withContext null
            }
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度获取] 找到进度数据 - 数据长度: ${progressJson.length}字符")
            
            val progress = deserializeProgress(progressJson)
            
            if (progress == null) {
                Log.w(PROGRESS_MANAGER_TAG, "[进度获取] 反序列化失败 - 视频: ${videoUrl.take(50)}..., 原始数据: ${progressJson.take(100)}...")
                // 清理无效数据
                sharedPreferences.edit().remove(videoKey).apply()
                Log.d(PROGRESS_MANAGER_TAG, "[进度获取] 已清理无效数据")
                return@withContext null
            }
            
            if (!progress.isValid()) {
                Log.w(PROGRESS_MANAGER_TAG, "[进度获取] 数据无效 - 视频: ${videoUrl.take(50)}..., 进度: 位置=${progress.position}ms, 时长=${progress.duration}ms, 时间戳=${progress.timestamp}")
                // 清理过期数据
                sharedPreferences.edit().remove(videoKey).apply()
                Log.d(PROGRESS_MANAGER_TAG, "[进度获取] 已清理无效数据")
                return@withContext null
            }
            
            val ageInHours = (System.currentTimeMillis() - progress.timestamp) / (1000 * 60 * 60)
            Log.i(PROGRESS_MANAGER_TAG, "[进度获取] 获取成功 - 视频: ${videoUrl.take(50)}..., 位置: ${progress.position}ms/${progress.duration}ms (${String.format("%.1f", progress.getProgressPercent() * 100)}%), 记录时间: ${ageInHours}小时前")
            return@withContext progress
            
        } catch (e: Exception) {
            Log.e(PROGRESS_MANAGER_TAG, "[进度获取] 获取异常 - 视频: ${videoUrl.take(50)}..., 错误: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * 删除指定视频的进度记录
     * 
     * @param videoUrl 视频URL
     * @return true表示删除成功
     */
    suspend fun removeProgress(videoUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(PROGRESS_MANAGER_TAG, "[进度删除] 开始删除进度 - URL: ${videoUrl.take(50)}...")
            
            if (videoUrl.isBlank()) {
                Log.w(PROGRESS_MANAGER_TAG, "[进度删除] 删除失败 - 视频URL为空")
                return@withContext false
            }
            
            val videoKey = generateVideoKey(videoUrl)
            Log.d(PROGRESS_MANAGER_TAG, "[进度删除] 生成视频Key: ${videoKey.take(8)}...")
            
            // 检查是否存在进度记录
            val existingProgress = sharedPreferences.getString(videoKey, null)
            if (existingProgress.isNullOrBlank()) {
                Log.i(PROGRESS_MANAGER_TAG, "[进度删除] 无需删除 - 不存在进度记录")
                return@withContext true
            }
            
            sharedPreferences.edit()
                .remove(videoKey)
                .remove("${videoKey}_timestamp")
                .apply()
            
            // 清理内存缓存
            val hadCacheTime = lastSaveTimeMap.remove(videoKey) != null
            val hadCachePosition = lastSavedPositionMap.remove(videoKey) != null
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度删除] 清理缓存 - 时间缓存: ${if(hadCacheTime) "已清理" else "无"}, 位置缓存: ${if(hadCachePosition) "已清理" else "无"}")
            Log.i(PROGRESS_MANAGER_TAG, "[进度删除] 删除成功 - 视频: ${videoUrl.take(50)}..., Key: ${videoKey.take(8)}...")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(PROGRESS_MANAGER_TAG, "[进度删除] 删除异常 - 视频: ${videoUrl.take(50)}..., 错误: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * 清理所有过期的进度记录
     * 
     * @return 清理的记录数量
     */
    suspend fun cleanupExpiredProgress(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(PROGRESS_MANAGER_TAG, "[进度清理] 开始清理过期进度记录")
            
            val currentTime = System.currentTimeMillis()
            val expireThreshold = currentTime - PROGRESS_EXPIRE_DAYS * 24 * 60 * 60 * 1000L
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度清理] 清理参数 - 当前时间: ${currentTime}, 过期阈值: ${expireThreshold}, 过期天数: ${PROGRESS_EXPIRE_DAYS}天")
            
            val allEntries = sharedPreferences.all
            val keysToRemove = mutableListOf<String>()
            var validCount = 0
            var expiredCount = 0
            var invalidCount = 0
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度清理] 开始扫描存储记录 - 总记录数: ${allEntries.size}")
            
            for ((key, value) in allEntries) {
                if (key.endsWith("_timestamp") || key == "last_cleanup_time") continue
                
                try {
                    val progressJson = value as? String ?: continue
                    val progress = deserializeProgress(progressJson)
                    
                    if (progress == null) {
                        keysToRemove.add(key)
                        keysToRemove.add("${key}_timestamp")
                        invalidCount++
                        Log.w(PROGRESS_MANAGER_TAG, "[进度清理] 发现无效数据 - Key: ${key.take(8)}..., 原因: 反序列化失败")
                    } else if (progress.timestamp < expireThreshold) {
                        keysToRemove.add(key)
                        keysToRemove.add("${key}_timestamp")
                        expiredCount++
                        val ageInDays = (currentTime - progress.timestamp) / (24 * 60 * 60 * 1000L)
                        Log.d(PROGRESS_MANAGER_TAG, "[进度清理] 发现过期数据 - Key: ${key.take(8)}..., 年龄: ${ageInDays}天, 视频: ${progress.videoUrl.take(30)}...")
                    } else {
                        validCount++
                    }
                } catch (e: Exception) {
                    // 无效数据，标记删除
                    keysToRemove.add(key)
                    keysToRemove.add("${key}_timestamp")
                    invalidCount++
                    Log.w(PROGRESS_MANAGER_TAG, "[进度清理] 发现异常数据 - Key: ${key.take(8)}..., 错误: ${e.message}")
                }
            }
            
            Log.i(PROGRESS_MANAGER_TAG, "[进度清理] 扫描完成 - 有效: ${validCount}, 过期: ${expiredCount}, 无效: ${invalidCount}, 待清理: ${keysToRemove.size / 2}")
            
            if (keysToRemove.isNotEmpty()) {
                Log.d(PROGRESS_MANAGER_TAG, "[进度清理] 开始执行清理操作")
                val editor = sharedPreferences.edit()
                keysToRemove.forEach { editor.remove(it) }
                val success = editor.commit()
                
                if (success) {
                    Log.i(PROGRESS_MANAGER_TAG, "[进度清理] 清理完成 - 成功清理 ${keysToRemove.size / 2} 条记录")
                } else {
                    Log.e(PROGRESS_MANAGER_TAG, "[进度清理] 清理失败 - 存储操作未成功")
                }
            } else {
                Log.i(PROGRESS_MANAGER_TAG, "[进度清理] 无需清理 - 所有记录均有效")
            }
            
            return@withContext keysToRemove.size / 2
            
        } catch (e: Exception) {
            Log.e(PROGRESS_MANAGER_TAG, "[进度清理] 清理异常 - 错误: ${e.message}", e)
            return@withContext 0
        }
    }
    
    /**
     * 获取所有有效的进度记录
     * 
     * @return 有效进度记录列表
     */
    suspend fun getAllValidProgress(): List<VideoProgress> = withContext(Dispatchers.IO) {
        try {
            Log.d(PROGRESS_MANAGER_TAG, "[进度查询] 开始获取所有有效进度记录")
            
            val validProgressList = mutableListOf<VideoProgress>()
            val allEntries = sharedPreferences.all
            var totalCount = 0
            var validCount = 0
            var invalidCount = 0
            
            Log.d(PROGRESS_MANAGER_TAG, "[进度查询] 开始扫描存储 - 总条目数: ${allEntries.size}")
            
            for ((key, value) in allEntries) {
                if (key.endsWith("_timestamp") || key == "last_cleanup_time") continue
                
                totalCount++
                
                try {
                    val progressJson = value as? String ?: continue
                    val progress = deserializeProgress(progressJson)
                    
                    if (progress != null && progress.isValid()) {
                        validProgressList.add(progress)
                        validCount++
                        Log.v(PROGRESS_MANAGER_TAG, "[进度查询] 找到有效记录 - Key: ${key.take(8)}..., 视频: ${progress.videoUrl.take(30)}..., 进度: ${String.format("%.1f", progress.getProgressPercent() * 100)}%")
                    } else {
                        invalidCount++
                        if (progress == null) {
                            Log.w(PROGRESS_MANAGER_TAG, "[进度查询] 跳过无效数据 - Key: ${key.take(8)}..., 原因: 反序列化失败")
                        } else {
                            Log.w(PROGRESS_MANAGER_TAG, "[进度查询] 跳过过期数据 - Key: ${key.take(8)}..., 原因: 数据验证失败")
                        }
                    }
                } catch (e: Exception) {
                    invalidCount++
                    Log.w(PROGRESS_MANAGER_TAG, "[进度查询] 跳过异常数据 - Key: ${key.take(8)}..., 错误: ${e.message}")
                }
            }
            
            // 按时间戳降序排序（最新的在前）
            validProgressList.sortByDescending { it.timestamp }
            
            Log.i(PROGRESS_MANAGER_TAG, "[进度查询] 查询完成 - 总扫描: ${totalCount}, 有效: ${validCount}, 无效: ${invalidCount}, 返回数量: ${validProgressList.size}")
            
            if (validProgressList.isNotEmpty()) {
                val newestProgress = validProgressList.first()
                val oldestProgress = validProgressList.last()
                val newestAge = (System.currentTimeMillis() - newestProgress.timestamp) / (1000 * 60 * 60)
                val oldestAge = (System.currentTimeMillis() - oldestProgress.timestamp) / (1000 * 60 * 60)
                Log.d(PROGRESS_MANAGER_TAG, "[进度查询] 记录时间范围 - 最新: ${newestAge}小时前, 最旧: ${oldestAge}小时前")
            }
            
            return@withContext validProgressList
            
        } catch (e: Exception) {
            Log.e(PROGRESS_MANAGER_TAG, "[进度查询] 查询异常 - 错误: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    // === 私有辅助方法 ===
    
    /**
     * 生成视频唯一标识键
     * 使用MD5哈希确保键的唯一性和长度一致性
     */
    private fun generateVideoKey(videoUrl: String): String {
        return try {
            val md5 = MessageDigest.getInstance("MD5")
            val hashBytes = md5.digest(videoUrl.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(PROGRESS_MANAGER_TAG, "MD5生成失败，使用备用方案", e)
            "video_${videoUrl.hashCode().toString().replace("-", "n")}"
        }
    }
    
    /**
     * 序列化进度对象为JSON字符串
     */
    private fun serializeProgress(progress: VideoProgress): String {
        return "${progress.position}|${progress.duration}|${progress.timestamp}|${progress.videoUrl}"
    }
    
    /**
     * 反序列化JSON字符串为进度对象
     */
    private fun deserializeProgress(progressJson: String): VideoProgress? {
        return try {
            val parts = progressJson.split("|")
            if (parts.size >= 4) {
                VideoProgress(
                    position = parts[0].toLong(),
                    duration = parts[1].toLong(),
                    timestamp = parts[2].toLong(),
                    videoUrl = parts[3]
                )
            } else null
        } catch (e: Exception) {
            Log.w(PROGRESS_MANAGER_TAG, "进度数据反序列化失败: $progressJson", e)
            null
        }
    }
    
    /**
     * 执行周期性清理任务
     * 避免频繁清理，只在需要时执行
     */
    private suspend fun performPeriodicCleanup() {
        try {
            val lastCleanupTime = sharedPreferences.getLong("last_cleanup_time", 0L)
            val currentTime = System.currentTimeMillis()
            val cleanupInterval = CLEANUP_CHECK_INTERVAL_DAYS * 24 * 60 * 60 * 1000L
            val timeSinceLastCleanup = currentTime - lastCleanupTime
            
            Log.d(PROGRESS_MANAGER_TAG, "[周期清理] 检查清理条件 - 上次清理: ${if(lastCleanupTime > 0) "${timeSinceLastCleanup / (1000 * 60 * 60)}小时前" else "从未"}, 清理间隔: ${CLEANUP_CHECK_INTERVAL_DAYS}天")
            
            if (currentTime - lastCleanupTime > cleanupInterval) {
                Log.i(PROGRESS_MANAGER_TAG, "[周期清理] 开始执行周期性清理任务")
                val cleanedCount = cleanupExpiredProgress()
                
                sharedPreferences.edit()
                    .putLong("last_cleanup_time", currentTime)
                    .apply()
                
                Log.i(PROGRESS_MANAGER_TAG, "[周期清理] 周期性清理完成 - 清理记录数: $cleanedCount, 下次清理时间: ${CLEANUP_CHECK_INTERVAL_DAYS}天后")
            } else {
                val remainingHours = (cleanupInterval - timeSinceLastCleanup) / (1000 * 60 * 60)
                Log.v(PROGRESS_MANAGER_TAG, "[周期清理] 跳过清理 - 距离下次清理还有 ${remainingHours} 小时")
            }
        } catch (e: Exception) {
            Log.w(PROGRESS_MANAGER_TAG, "[周期清理] 清理任务异常 - 错误: ${e.message}", e)
        }
    }
}