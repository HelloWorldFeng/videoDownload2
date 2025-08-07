package com.nexus.core.media.processor.cache

import android.content.Context
import com.nexus.core.media.processor.model.MediaInfo
import com.nexus.core.media.processor.model.MediaFormat
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.security.MessageDigest
import java.util.*
import android.util.Base64
import kotlin.collections.LinkedHashMap

/**
 * 缓存条目
 */
@Serializable
data class CacheEntry<T>(
    val key: String,
    val data: T,
    val timestamp: Long,
    val expirationTime: Long,
    val accessCount: Long = 0,
    val lastAccessTime: Long = timestamp,
    val size: Long = 0
) {
    /**
     * 检查是否过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expirationTime
    }
    
    /**
     * 检查是否有效
     */
    fun isValid(): Boolean {
        return !isExpired()
    }
    
    /**
     * 更新访问信息
     */
    fun updateAccess(): CacheEntry<T> {
        return copy(
            accessCount = accessCount + 1,
            lastAccessTime = System.currentTimeMillis()
        )
    }
}

/**
 * 缓存统计信息
 */
data class CacheStatistics(
    val totalEntries: Int = 0,
    val totalSize: Long = 0,
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val evictionCount: Long = 0,
    val expiredCount: Long = 0
) {
    /**
     * 计算命中率
     */
    fun getHitRate(): Double {
        val total = hitCount + missCount
        return if (total > 0) hitCount.toDouble() / total else 0.0
    }
    
    /**
     * 格式化大小
     */
    fun getFormattedSize(): String {
        return formatBytes(totalSize)
    }
    
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.2f %s".format(size, units[unitIndex])
    }
}

/**
 * 缓存配置
 */
data class CacheConfig(
    val maxMemorySize: Long = 50 * 1024 * 1024, // 50MB
    val maxDiskSize: Long = 200 * 1024 * 1024, // 200MB
    val maxEntries: Int = 1000,
    val defaultTtl: Long = 24 * 60 * 60 * 1000L, // 24小时
    val enableDiskCache: Boolean = true,
    val enableMemoryCache: Boolean = true,
    val enableCompression: Boolean = true,
    val cleanupInterval: Long = 60 * 60 * 1000L, // 1小时
    val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU
) {
    /**
     * 验证配置的有效性
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (maxMemorySize <= 0) {
            errors.add("最大内存缓存大小必须大于0")
        }
        
        if (maxDiskSize <= 0) {
            errors.add("最大磁盘缓存大小必须大于0")
        }
        
        if (maxEntries <= 0) {
            errors.add("最大缓存条目数必须大于0")
        }
        
        if (defaultTtl <= 0) {
            errors.add("默认TTL必须大于0")
        }
        
        if (cleanupInterval <= 0) {
            errors.add("清理间隔必须大于0")
        }
        
        return errors
    }
}

/**
 * 缓存淘汰策略
 */
enum class EvictionPolicy {
    LRU,    // 最近最少使用
    LFU,    // 最少使用频率
    FIFO,   // 先进先出
    TTL     // 基于过期时间
}

/**
 * LRU缓存实现
 */
class LRUCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(16, 0.75f, true) {
    
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > maxSize
    }
    
    /**
     * 获取最老的条目
     */
    fun getEldestEntry(): MutableMap.MutableEntry<K, V>? {
        return entries.firstOrNull()
    }
}

/**
 * 内存缓存管理器
 */
class MemoryCacheManager<T>(
    private val config: CacheConfig
) {
    private val cache = ConcurrentHashMap<String, CacheEntry<T>>()
    private val accessOrder = LRUCache<String, Long>(config.maxEntries)
    private val currentSize = AtomicLong(0)
    
    private var hitCount = AtomicLong(0)
    private var missCount = AtomicLong(0)
    private var evictionCount = AtomicLong(0)
    
    /**
     * 存储缓存条目
     */
    fun put(key: String, data: T, ttl: Long = config.defaultTtl): Boolean {
        if (!config.enableMemoryCache) return false
        
        val now = System.currentTimeMillis()
        val entry = CacheEntry(
            key = key,
            data = data,
            timestamp = now,
            expirationTime = now + ttl,
            size = estimateSize(data)
        )
        
        // 检查大小限制
        if (entry.size > config.maxMemorySize) {
            return false
        }
        
        // 确保有足够空间
        ensureCapacity(entry.size)
        
        cache[key] = entry
        accessOrder[key] = now
        currentSize.addAndGet(entry.size)
        
        return true
    }
    
    /**
     * 获取缓存条目
     */
    fun get(key: String): T? {
        if (!config.enableMemoryCache) return null
        
        val entry = cache[key]
        
        if (entry == null) {
            missCount.incrementAndGet()
            return null
        }
        
        if (entry.isExpired()) {
            remove(key)
            missCount.incrementAndGet()
            return null
        }
        
        // 更新访问信息
        val updatedEntry = entry.updateAccess()
        cache[key] = updatedEntry
        accessOrder[key] = System.currentTimeMillis()
        
        hitCount.incrementAndGet()
        return entry.data
    }
    
    /**
     * 移除缓存条目
     */
    fun remove(key: String): Boolean {
        val entry = cache.remove(key)
        accessOrder.remove(key)
        
        if (entry != null) {
            currentSize.addAndGet(-entry.size)
            return true
        }
        
        return false
    }
    
    /**
     * 清空缓存
     */
    fun clear() {
        cache.clear()
        accessOrder.clear()
        currentSize.set(0)
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Int = cache.size
    
    /**
     * 获取当前使用的内存大小
     */
    fun getCurrentSize(): Long = currentSize.get()
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): CacheStatistics {
        return CacheStatistics(
            totalEntries = cache.size,
            totalSize = currentSize.get(),
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            evictionCount = evictionCount.get()
        )
    }
    
    /**
     * 确保有足够的容量
     */
    private fun ensureCapacity(requiredSize: Long) {
        while (currentSize.get() + requiredSize > config.maxMemorySize || 
               cache.size >= config.maxEntries) {
            
            val keyToEvict = when (config.evictionPolicy) {
                EvictionPolicy.LRU -> findLRUKey()
                EvictionPolicy.LFU -> findLFUKey()
                EvictionPolicy.FIFO -> findFIFOKey()
                EvictionPolicy.TTL -> findExpiredKey()
            }
            
            if (keyToEvict != null) {
                remove(keyToEvict)
                evictionCount.incrementAndGet()
            } else {
                break
            }
        }
    }
    
    private fun findLRUKey(): String? {
        return accessOrder.getEldestEntry()?.key
    }
    
    private fun findLFUKey(): String? {
        return cache.entries.minByOrNull { it.value.accessCount }?.key
    }
    
    private fun findFIFOKey(): String? {
        return cache.entries.minByOrNull { it.value.timestamp }?.key
    }
    
    private fun findExpiredKey(): String? {
        return cache.entries.firstOrNull { it.value.isExpired() }?.key
    }
    
    /**
     * 估算数据大小
     */
    private fun estimateSize(data: T): Long {
        return when (data) {
            is String -> data.length * 2L // Unicode字符
            is ByteArray -> data.size.toLong()
            is MediaInfo -> 1024L // 估算值
            is MediaFormat -> 512L // 估算值
            else -> 256L // 默认估算值
        }
    }
    
    /**
     * 清理过期条目
     */
    fun cleanupExpired(): Int {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { remove(it) }
        return expiredKeys.size
    }
}

/**
 * 磁盘缓存管理器
 */
class DiskCacheManager(
    private val cacheDir: File,
    private val config: CacheConfig
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val currentSize = AtomicLong(0)
    private val metadataFile = File(cacheDir, "cache_metadata.json")
    private val metadata = ConcurrentHashMap<String, CacheMetadata>()
    
    private var hitCount = AtomicLong(0)
    private var missCount = AtomicLong(0)
    
    @Serializable
    data class CacheMetadata(
        val key: String,
        val fileName: String,
        val timestamp: Long,
        val expirationTime: Long,
        val size: Long,
        val accessCount: Long = 0,
        val lastAccessTime: Long = timestamp
    )
    
    init {
        cacheDir.mkdirs()
        loadMetadata()
        calculateCurrentSize()
    }
    
    /**
     * 存储到磁盘缓存
     */
    fun put(key: String, data: String, ttl: Long = config.defaultTtl): Boolean {
        if (!config.enableDiskCache) return false
        
        try {
            val fileName = generateFileName(key)
            val file = File(cacheDir, fileName)
            
            val compressedData = if (config.enableCompression) {
                compressData(data)
            } else {
                data.toByteArray()
            }
            
            // 检查大小限制
            if (compressedData.size > config.maxDiskSize) {
                return false
            }
            
            // 确保有足够空间
            ensureCapacity(compressedData.size.toLong())
            
            file.writeBytes(compressedData)
            
            val now = System.currentTimeMillis()
            val meta = CacheMetadata(
                key = key,
                fileName = fileName,
                timestamp = now,
                expirationTime = now + ttl,
                size = compressedData.size.toLong()
            )
            
            metadata[key] = meta
            currentSize.addAndGet(meta.size)
            saveMetadata()
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 从磁盘缓存获取
     */
    fun get(key: String): String? {
        if (!config.enableDiskCache) return null
        
        val meta = metadata[key]
        
        if (meta == null) {
            missCount.incrementAndGet()
            return null
        }
        
        if (System.currentTimeMillis() > meta.expirationTime) {
            remove(key)
            missCount.incrementAndGet()
            return null
        }
        
        try {
            val file = File(cacheDir, meta.fileName)
            if (!file.exists()) {
                metadata.remove(key)
                missCount.incrementAndGet()
                return null
            }
            
            val fileData = file.readBytes()
            val data = if (config.enableCompression) {
                decompressData(fileData)
            } else {
                String(fileData)
            }
            
            // 更新访问信息
            val updatedMeta = meta.copy(
                accessCount = meta.accessCount + 1,
                lastAccessTime = System.currentTimeMillis()
            )
            metadata[key] = updatedMeta
            
            hitCount.incrementAndGet()
            return data
        } catch (e: Exception) {
            missCount.incrementAndGet()
            return null
        }
    }
    
    /**
     * 移除缓存条目
     */
    fun remove(key: String): Boolean {
        val meta = metadata.remove(key)
        
        if (meta != null) {
            val file = File(cacheDir, meta.fileName)
            if (file.exists()) {
                file.delete()
            }
            currentSize.addAndGet(-meta.size)
            saveMetadata()
            return true
        }
        
        return false
    }
    
    /**
     * 清空缓存
     */
    fun clear() {
        metadata.values.forEach { meta ->
            val file = File(cacheDir, meta.fileName)
            file.delete()
        }
        
        metadata.clear()
        currentSize.set(0)
        saveMetadata()
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Int = metadata.size
    
    /**
     * 获取当前使用的磁盘大小
     */
    fun getCurrentSize(): Long = currentSize.get()
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): CacheStatistics {
        return CacheStatistics(
            totalEntries = metadata.size,
            totalSize = currentSize.get(),
            hitCount = hitCount.get(),
            missCount = missCount.get()
        )
    }
    
    /**
     * 清理过期条目
     */
    fun cleanupExpired(): Int {
        val now = System.currentTimeMillis()
        val expiredKeys = metadata.entries
            .filter { it.value.expirationTime < now }
            .map { it.key }
        
        expiredKeys.forEach { remove(it) }
        return expiredKeys.size
    }
    
    private fun generateFileName(key: String): String {
        val hash = MessageDigest.getInstance("MD5")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "cache_$hash.dat"
    }
    
    private fun ensureCapacity(requiredSize: Long) {
        while (currentSize.get() + requiredSize > config.maxDiskSize) {
            val keyToEvict = when (config.evictionPolicy) {
                EvictionPolicy.LRU -> metadata.entries.minByOrNull { it.value.lastAccessTime }?.key
                EvictionPolicy.LFU -> metadata.entries.minByOrNull { it.value.accessCount }?.key
                EvictionPolicy.FIFO -> metadata.entries.minByOrNull { it.value.timestamp }?.key
                EvictionPolicy.TTL -> metadata.entries.firstOrNull { 
                    System.currentTimeMillis() > it.value.expirationTime 
                }?.key
            }
            
            if (keyToEvict != null) {
                remove(keyToEvict)
            } else {
                break
            }
        }
    }
    
    private fun loadMetadata() {
        if (metadataFile.exists()) {
            try {
                val metadataJson = metadataFile.readText()
                val metadataMap: Map<String, CacheMetadata> = json.decodeFromString(metadataJson)
                metadata.putAll(metadataMap)
            } catch (e: Exception) {
                // 忽略加载错误，使用空的元数据
            }
        }
    }
    
    private fun saveMetadata() {
        try {
            val metadataJson = json.encodeToString(metadata.toMap())
            metadataFile.writeText(metadataJson)
        } catch (e: Exception) {
            // 忽略保存错误
        }
    }
    
    private fun calculateCurrentSize() {
        var totalSize = 0L
        metadata.values.forEach { meta ->
            val file = File(cacheDir, meta.fileName)
            if (file.exists()) {
                totalSize += file.length()
            }
        }
        currentSize.set(totalSize)
    }
    
    private fun compressData(data: String): ByteArray {
        // 简单的压缩实现，实际应用中可以使用 GZIP 等
        return data.toByteArray()
    }
    
    private fun decompressData(data: ByteArray): String {
        // 简单的解压实现
        return String(data)
    }
}

/**
 * 缓存管理器
 * 
 * 负责媒体处理器的缓存管理，
 * 支持内存缓存和磁盘缓存。
 * 
 * 主要功能：
 * - 多级缓存
 * - 自动过期清理
 * - 缓存统计
 * - 容量管理
 * 
 * 使用示例：
 * ```kotlin
 * val cacheManager = CacheManager(context, config)
 * cacheManager.putMediaInfo(url, mediaInfo)
 * val cachedInfo = cacheManager.getMediaInfo(url)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class CacheManager(
    private val context: Context,
    private val config: CacheConfig = CacheConfig()
) {
    
    companion object {
        private const val CACHE_DIR_NAME = "media_processor_cache"
        private const val MEDIA_INFO_PREFIX = "media_info_"
        private const val MEDIA_FORMAT_PREFIX = "media_format_"
        private const val THUMBNAIL_PREFIX = "thumbnail_"
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
    private val json = Json { ignoreUnknownKeys = true }
    
    // 内存缓存
    private val mediaInfoMemoryCache = MemoryCacheManager<MediaInfo>(config)
    private val mediaFormatMemoryCache = MemoryCacheManager<List<MediaFormat>>(config)
    private val thumbnailMemoryCache = MemoryCacheManager<ByteArray>(config)
    
    // 磁盘缓存
    private val diskCache = DiskCacheManager(cacheDir, config)
    
    // 清理任务
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        startPeriodicCleanup()
    }
    
    /**
     * 缓存媒体信息
     */
    fun putMediaInfo(url: String, mediaInfo: MediaInfo, ttl: Long = config.defaultTtl) {
        val key = MEDIA_INFO_PREFIX + generateKey(url)
        
        // 内存缓存
        mediaInfoMemoryCache.put(key, mediaInfo, ttl)
        
        // 磁盘缓存
        coroutineScope.launch {
            try {
                val jsonData = json.encodeToString(mediaInfo)
                diskCache.put(key, jsonData, ttl)
            } catch (e: Exception) {
                // 忽略序列化错误
            }
        }
    }
    
    /**
     * 获取缓存的媒体信息
     */
    suspend fun getMediaInfo(url: String): MediaInfo? {
        val key = MEDIA_INFO_PREFIX + generateKey(url)
        
        // 先尝试内存缓存
        mediaInfoMemoryCache.get(key)?.let { return it }
        
        // 再尝试磁盘缓存
        return withContext(Dispatchers.IO) {
            try {
                val jsonData = diskCache.get(key)
                if (jsonData != null) {
                    val mediaInfo: MediaInfo = json.decodeFromString(jsonData)
                    // 回填到内存缓存
                    mediaInfoMemoryCache.put(key, mediaInfo)
                    mediaInfo
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 缓存媒体格式列表
     */
    fun putMediaFormats(url: String, formats: List<MediaFormat>, ttl: Long = config.defaultTtl) {
        val key = MEDIA_FORMAT_PREFIX + generateKey(url)
        
        // 内存缓存
        mediaFormatMemoryCache.put(key, formats, ttl)
        
        // 磁盘缓存
        coroutineScope.launch {
            try {
                val jsonData = json.encodeToString(formats)
                diskCache.put(key, jsonData, ttl)
            } catch (e: Exception) {
                // 忽略序列化错误
            }
        }
    }
    
    /**
     * 获取缓存的媒体格式列表
     */
    suspend fun getMediaFormats(url: String): List<MediaFormat>? {
        val key = MEDIA_FORMAT_PREFIX + generateKey(url)
        
        // 先尝试内存缓存
        mediaFormatMemoryCache.get(key)?.let { return it }
        
        // 再尝试磁盘缓存
        return withContext(Dispatchers.IO) {
            try {
                val jsonData = diskCache.get(key)
                if (jsonData != null) {
                    val formats: List<MediaFormat> = json.decodeFromString(jsonData)
                    // 回填到内存缓存
                    mediaFormatMemoryCache.put(key, formats)
                    formats
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 缓存缩略图
     */
    fun putThumbnail(url: String, thumbnailData: ByteArray, ttl: Long = config.defaultTtl) {
        val key = THUMBNAIL_PREFIX + generateKey(url)
        
        // 内存缓存
        thumbnailMemoryCache.put(key, thumbnailData, ttl)
        
        // 磁盘缓存（Base64编码）
        coroutineScope.launch {
            try {
                val base64Data = Base64.encodeToString(thumbnailData, Base64.DEFAULT)
                diskCache.put(key, base64Data, ttl)
            } catch (e: Exception) {
                // 忽略编码错误
            }
        }
    }
    
    /**
     * 获取缓存的缩略图
     */
    suspend fun getThumbnail(url: String): ByteArray? {
        val key = THUMBNAIL_PREFIX + generateKey(url)
        
        // 先尝试内存缓存
        thumbnailMemoryCache.get(key)?.let { return it }
        
        // 再尝试磁盘缓存
        return withContext(Dispatchers.IO) {
            try {
                val base64Data = diskCache.get(key)
                if (base64Data != null) {
                    val thumbnailData = Base64.decode(base64Data, Base64.DEFAULT)
                    // 回填到内存缓存
                    thumbnailMemoryCache.put(key, thumbnailData)
                    thumbnailData
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 移除指定URL的所有缓存
     */
    fun removeCache(url: String) {
        val baseKey = generateKey(url)
        
        mediaInfoMemoryCache.remove(MEDIA_INFO_PREFIX + baseKey)
        mediaFormatMemoryCache.remove(MEDIA_FORMAT_PREFIX + baseKey)
        thumbnailMemoryCache.remove(THUMBNAIL_PREFIX + baseKey)
        
        coroutineScope.launch {
            diskCache.remove(MEDIA_INFO_PREFIX + baseKey)
            diskCache.remove(MEDIA_FORMAT_PREFIX + baseKey)
            diskCache.remove(THUMBNAIL_PREFIX + baseKey)
        }
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        mediaInfoMemoryCache.clear()
        mediaFormatMemoryCache.clear()
        thumbnailMemoryCache.clear()
        
        coroutineScope.launch {
            diskCache.clear()
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getStatistics(): Map<String, CacheStatistics> {
        return mapOf(
            "mediaInfo" to mediaInfoMemoryCache.getStatistics(),
            "mediaFormat" to mediaFormatMemoryCache.getStatistics(),
            "thumbnail" to thumbnailMemoryCache.getStatistics(),
            "disk" to diskCache.getStatistics()
        )
    }
    
    /**
     * 获取总缓存大小
     */
    fun getTotalSize(): Long {
        return mediaInfoMemoryCache.getCurrentSize() +
               mediaFormatMemoryCache.getCurrentSize() +
               thumbnailMemoryCache.getCurrentSize() +
               diskCache.getCurrentSize()
    }
    
    /**
     * 手动清理过期缓存
     */
    suspend fun cleanupExpired(): Int {
        return withContext(Dispatchers.IO) {
            val memoryCleanup = mediaInfoMemoryCache.cleanupExpired() +
                               mediaFormatMemoryCache.cleanupExpired() +
                               thumbnailMemoryCache.cleanupExpired()
            
            val diskCleanup = diskCache.cleanupExpired()
            
            memoryCleanup + diskCleanup
        }
    }
    
    /**
     * 生成缓存键
     */
    private fun generateKey(url: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 启动定期清理任务
     */
    private fun startPeriodicCleanup() {
        coroutineScope.launch {
            while (isActive) {
                delay(config.cleanupInterval)
                try {
                    cleanupExpired()
                } catch (e: Exception) {
                    // 忽略清理错误
                }
            }
        }
    }
    
    /**
     * 关闭缓存管理器
     */
    fun close() {
        coroutineScope.cancel()
    }
}