package com.nexus.core.media.processor.network

import com.nexus.core.media.processor.config.ProcessorConfiguration
import com.nexus.core.media.processor.model.NetworkType
import com.nexus.core.media.processor.utils.ProcessorLogUtils
import kotlinx.coroutines.*
import okhttp3.*
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * 媒体下载器
 * 
 * 负责媒体文件的下载，支持断点续传、
 * 分片下载、进度监控等功能。
 * 
 * 主要功能：
 * - HTTP/HTTPS下载
 * - 断点续传
 * - 分片下载
 * - 进度回调
 * - 速度限制
 * - 重试机制
 * 
 * 使用示例：
 * ```kotlin
 * val downloader = MediaDownloader(
 *     configuration = config,
 *     onProgressUpdate = { downloaded, total, speed -> ... }
 * )
 * val result = downloader.download(url, outputPath)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class MediaDownloader(
    private val configuration: ProcessorConfiguration,
    private val onProgressUpdate: suspend (downloadedBytes: Long, totalBytes: Long, speed: Long) -> Unit
) {
    
    companion object {
        private const val LOG_TAG = "MediaDownloader"
        private const val CHUNK_SIZE = 8192 // 8KB
        private const val PROGRESS_UPDATE_INTERVAL = 500L // 500ms
        private const val SPEED_CALCULATION_INTERVAL = 1000L // 1秒
    }
    
    // HTTP客户端
    private val httpClient: OkHttpClient by lazy {
        createHttpClient()
    }
    
    // 下载状态
    @Volatile
    private var isPaused = false
    @Volatile
    private var isCancelled = false
    
    // 进度跟踪
    private var lastProgressUpdate = 0L
    private var lastSpeedCalculation = 0L
    private var lastDownloadedBytes = 0L
    
    /**
     * 下载结果
     */
    data class DownloadResult(
        val success: Boolean,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val errorMessage: String? = null,
        val errorCode: Int? = null
    )
    
    /**
     * 下载媒体文件
     * 
     * @param url 下载URL
     * @param outputPath 输出文件路径
     * @param resumePosition 断点续传位置
     * @param headers 自定义请求头
     * @return 下载结果
     */
    suspend fun download(
        url: String,
        outputPath: String,
        resumePosition: Long = 0L,
        headers: Map<String, String> = emptyMap()
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            ProcessorLogUtils.d(LOG_TAG, "开始下载: $url")
            ProcessorLogUtils.d(LOG_TAG, "输出路径: $outputPath")
            ProcessorLogUtils.d(LOG_TAG, "断点续传位置: $resumePosition")
            
            // 重置状态
            isPaused = false
            isCancelled = false
            lastProgressUpdate = 0L
            lastSpeedCalculation = 0L
            lastDownloadedBytes = resumePosition
            
            // 创建输出文件
            val outputFile = File(outputPath)
            val outputDir = outputFile.parentFile
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // 检查网络类型
            if (!isNetworkAllowed()) {
                return@withContext DownloadResult(
                    success = false,
                    errorMessage = "当前网络类型不允许下载"
                )
            }
            
            // 支持分片下载
            if (configuration.resumeEnabled && resumePosition == 0L) {
                return@withContext downloadWithChunks(url, outputPath, headers)
            } else {
                return@withContext downloadSingleFile(url, outputPath, resumePosition, headers)
            }
            
        } catch (e: CancellationException) {
            ProcessorLogUtils.i(LOG_TAG, "下载被取消: $url")
            DownloadResult(
                success = false,
                errorMessage = "下载被取消"
            )
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "下载失败: $url", e)
            DownloadResult(
                success = false,
                errorMessage = e.message ?: "未知错误",
                errorCode = when (e) {
                    is SocketTimeoutException -> 408
                    is IOException -> 500
                    else -> 999
                }
            )
        }
    }
    
    /**
     * 单文件下载
     */
    private suspend fun downloadSingleFile(
        url: String,
        outputPath: String,
        resumePosition: Long,
        headers: Map<String, String>
    ): DownloadResult {
        return try {
            val request = buildRequest(url, headers, resumePosition)
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return DownloadResult(
                    success = false,
                    errorMessage = "HTTP错误: ${response.code}",
                    errorCode = response.code
                )
            }
            
            val responseBody = response.body ?: return DownloadResult(
                success = false,
                errorMessage = "响应体为空"
            )
            
            val contentLength = responseBody.contentLength()
            val totalBytes = if (contentLength >= 0) {
                contentLength + resumePosition
            } else {
                -1L
            }
            
            ProcessorLogUtils.d(LOG_TAG, "内容长度: $contentLength, 总大小: $totalBytes")
            
            // 写入文件
            val outputFile = File(outputPath)
            val append = resumePosition > 0
            
            outputFile.sink(append = append).buffer().use { sink ->
                responseBody.source().use { source ->
                    var downloadedBytes = resumePosition
                    val buffer = ByteArray(CHUNK_SIZE)
                    
                    while (!isCancelled) {
                        // 检查暂停状态
                        while (isPaused && !isCancelled) {
                            delay(100)
                        }
                        
                        if (isCancelled) break
                        
                        val bytesRead = source.read(buffer)
                        if (bytesRead == -1) break
                        
                        sink.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // 应用速度限制
                        applySpeedLimit(bytesRead)
                        
                        // 更新进度
                        updateProgress(downloadedBytes, totalBytes)
                    }
                    
                    sink.flush()
                }
            }
            
            if (isCancelled) {
                // 删除未完成的文件
                if (resumePosition == 0L) {
                    outputFile.delete()
                }
                DownloadResult(
                    success = false,
                    errorMessage = "下载被取消"
                )
            } else {
                ProcessorLogUtils.i(LOG_TAG, "下载完成: $outputPath")
                DownloadResult(
                    success = true,
                    downloadedBytes = outputFile.length(),
                    totalBytes = totalBytes
                )
            }
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 分片下载
     */
    private suspend fun downloadWithChunks(
        url: String,
        outputPath: String,
        headers: Map<String, String>
    ): DownloadResult {
        return try {
            // 获取文件大小
            val totalBytes = getContentLength(url, headers)
            if (totalBytes <= 0) {
                // 如果无法获取文件大小，回退到单文件下载
                return downloadSingleFile(url, outputPath, 0L, headers)
            }
            
            ProcessorLogUtils.d(LOG_TAG, "文件总大小: $totalBytes, 开始分片下载")
            
            val chunkSize = configuration.chunkSizeKB * 1024L
            val chunks = mutableListOf<ChunkInfo>()
            
            // 计算分片
            var start = 0L
            var chunkIndex = 0
            while (start < totalBytes) {
                val end = min(start + chunkSize - 1, totalBytes - 1)
                chunks.add(ChunkInfo(chunkIndex++, start, end, "${outputPath}.chunk$chunkIndex"))
                start = end + 1
            }
            
            ProcessorLogUtils.d(LOG_TAG, "分片数量: ${chunks.size}")
            
            // 并发下载分片
            val results = coroutineScope {
                val downloadJobs = chunks.map { chunk ->
                    async {
                        downloadChunk(url, chunk, headers)
                    }
                }
                
                // 等待所有分片下载完成
                downloadJobs.awaitAll()
            }
            
            // 检查是否有失败的分片
            val failedChunk = results.find { !it.success }
            if (failedChunk != null) {
                // 清理已下载的分片
                chunks.forEach { chunk ->
                    File(chunk.tempPath).delete()
                }
                return DownloadResult(
                    success = false,
                    errorMessage = failedChunk.errorMessage
                )
            }
            
            // 合并分片
            mergeChunks(chunks, outputPath)
            
            ProcessorLogUtils.i(LOG_TAG, "分片下载完成: $outputPath")
            DownloadResult(
                success = true,
                downloadedBytes = totalBytes,
                totalBytes = totalBytes
            )
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 下载单个分片
     */
    private suspend fun downloadChunk(
        url: String,
        chunk: ChunkInfo,
        headers: Map<String, String>
    ): DownloadResult {
        return try {
            val rangeHeader = "bytes=${chunk.start}-${chunk.end}"
            val chunkHeaders = headers.toMutableMap()
            chunkHeaders["Range"] = rangeHeader
            
            val request = buildRequest(url, chunkHeaders)
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return DownloadResult(
                    success = false,
                    errorMessage = "分片下载失败: ${response.code}",
                    errorCode = response.code
                )
            }
            
            val responseBody = response.body ?: return DownloadResult(
                success = false,
                errorMessage = "分片响应体为空"
            )
            
            // 写入分片文件
            val chunkFile = File(chunk.tempPath)
            chunkFile.sink().buffer().use { sink ->
                responseBody.source().use { source ->
                    var downloadedBytes = 0L
                    val buffer = ByteArray(CHUNK_SIZE)
                    
                    while (!isCancelled) {
                        val bytesRead = source.read(buffer)
                        if (bytesRead == -1) break
                        
                        sink.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                    }
                    
                    sink.flush()
                }
            }
            
            if (isCancelled) {
                chunkFile.delete()
                DownloadResult(
                    success = false,
                    errorMessage = "分片下载被取消"
                )
            } else {
                DownloadResult(
                    success = true,
                    downloadedBytes = chunkFile.length()
                )
            }
            
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                errorMessage = "分片下载异常: ${e.message}"
            )
        }
    }
    
    /**
     * 合并分片
     */
    private suspend fun mergeChunks(chunks: List<ChunkInfo>, outputPath: String) {
        withContext(Dispatchers.IO) {
            val outputFile = File(outputPath)
            outputFile.sink().buffer().use { sink ->
                chunks.sortedBy { it.index }.forEach { chunk ->
                    val chunkFile = File(chunk.tempPath)
                    if (chunkFile.exists()) {
                        chunkFile.source().buffer().use { source ->
                            sink.writeAll(source)
                        }
                        chunkFile.delete()
                    }
                }
                sink.flush()
            }
        }
    }
    
    /**
     * 获取内容长度
     */
    private suspend fun getContentLength(url: String, headers: Map<String, String>): Long {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .head()
                    .apply {
                        headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                
                val response = httpClient.newCall(request).execute()
                response.use {
                    if (it.isSuccessful) {
                        it.body?.contentLength() ?: -1L
                    } else {
                        -1L
                    }
                }
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "获取内容长度失败", e)
                -1L
            }
        }
    }
    
    /**
     * 构建请求
     */
    private fun buildRequest(
        url: String,
        headers: Map<String, String>,
        resumePosition: Long = 0L
    ): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        
        // 添加请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // 添加断点续传头
        if (resumePosition > 0) {
            requestBuilder.addHeader("Range", "bytes=$resumePosition-")
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 创建HTTP客户端
     */
    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(configuration.connectionTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(configuration.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(configuration.writeTimeout, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
        
        // 添加代理
        configuration.proxyConfig?.let { proxy ->
            builder.proxy(proxy.createProxy())
        }
        
        // 添加拦截器
        builder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", configuration.userAgent)
                .build()
            chain.proceed(request)
        }
        
        return builder.build()
    }
    
    /**
     * 检查网络是否允许
     */
    private fun isNetworkAllowed(): Boolean {
        val currentNetworkType = getCurrentNetworkType()
        return when (currentNetworkType) {
            NetworkType.WIFI -> true
            NetworkType.MOBILE -> configuration.allowMobileNetwork
            NetworkType.ETHERNET -> true
            NetworkType.UNKNOWN -> false
            else -> false
        }
    }
    
    /**
     * 获取当前网络类型
     */
    private fun getCurrentNetworkType(): NetworkType {
        // TODO: 实现网络类型检测
        return NetworkType.WIFI
    }
    
    /**
     * 应用速度限制
     */
    private suspend fun applySpeedLimit(bytesTransferred: Int) {
        // 简单的速度控制，避免过快下载
        if (bytesTransferred > CHUNK_SIZE) {
            delay(1) // 简单延迟
        }
    }
    
    /**
     * 更新进度
     */
    private suspend fun updateProgress(downloadedBytes: Long, totalBytes: Long) {
        val currentTime = System.currentTimeMillis()
        
        // 限制更新频率
        if (currentTime - lastProgressUpdate < PROGRESS_UPDATE_INTERVAL) {
            return
        }
        
        // 计算速度
        val speed = if (currentTime - lastSpeedCalculation >= SPEED_CALCULATION_INTERVAL) {
            val timeDiff = currentTime - lastSpeedCalculation
            val bytesDiff = downloadedBytes - lastDownloadedBytes
            
            lastSpeedCalculation = currentTime
            lastDownloadedBytes = downloadedBytes
            
            if (timeDiff > 0) bytesDiff * 1000 / timeDiff else 0L
        } else {
            0L
        }
        
        lastProgressUpdate = currentTime
        
        try {
            onProgressUpdate(downloadedBytes, totalBytes, speed)
        } catch (e: Exception) {
            ProcessorLogUtils.e(LOG_TAG, "进度回调失败", e)
        }
    }
    
    /**
     * 暂停下载
     */
    suspend fun pause() {
        withContext(Dispatchers.IO) {
            isPaused = true
            ProcessorLogUtils.d(LOG_TAG, "下载已暂停")
        }
    }
    
    /**
     * 恢复下载
     */
    suspend fun resume() {
        withContext(Dispatchers.IO) {
            isPaused = false
            ProcessorLogUtils.d(LOG_TAG, "下载已恢复")
        }
    }
    
    /**
     * 取消下载
     */
    suspend fun cancel() {
        withContext(Dispatchers.IO) {
            isCancelled = true
            ProcessorLogUtils.d(LOG_TAG, "下载已取消")
        }
    }
    
    /**
     * 清理资源
     */
    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            try {
                cancel()
                // OkHttp客户端会自动清理连接池
            } catch (e: Exception) {
                ProcessorLogUtils.e(LOG_TAG, "清理下载器资源失败", e)
            }
        }
    }
    
    /**
     * 分片信息
     */
    private data class ChunkInfo(
        val index: Int,
        val start: Long,
        val end: Long,
        val tempPath: String
    )
}