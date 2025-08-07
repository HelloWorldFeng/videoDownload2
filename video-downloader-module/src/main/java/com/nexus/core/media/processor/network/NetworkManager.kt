package com.nexus.core.media.processor.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSource
import okio.Source
import okio.ForwardingSource
import okio.Buffer
import okio.buffer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 网络状态
 */
enum class NetworkState {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    UNKNOWN
}

/**
 * 网络类型
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    UNKNOWN
}

/**
 * 代理配置
 */
data class ProxyConfig(
    val type: Proxy.Type = Proxy.Type.HTTP,
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null
) {
    /**
     * 创建代理对象
     */
    fun createProxy(): Proxy {
        return Proxy(type, InetSocketAddress(host, port))
    }
    
    /**
     * 是否需要认证
     */
    fun requiresAuth(): Boolean {
        return !username.isNullOrEmpty() && !password.isNullOrEmpty()
    }
}

/**
 * 网络配置
 */
data class NetworkConfig(
    val connectTimeout: Long = 30_000L,
    val readTimeout: Long = 60_000L,
    val writeTimeout: Long = 60_000L,
    val callTimeout: Long = 120_000L,
    val retryOnConnectionFailure: Boolean = true,
    val followRedirects: Boolean = true,
    val followSslRedirects: Boolean = true,
    val maxRequests: Int = 64,
    val maxRequestsPerHost: Int = 5,
    val enableLogging: Boolean = false,
    val logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
    val userAgent: String = "MediaProcessor/1.0",
    val enableGzip: Boolean = true,
    val enableBrotli: Boolean = true,
    val proxyConfig: ProxyConfig? = null,
    val customHeaders: Map<String, String> = emptyMap(),
    val enableTrustAllCerts: Boolean = false,
    val connectionPoolMaxIdle: Int = 5,
    val connectionPoolKeepAlive: Long = 5L
) {
    /**
     * 验证配置的有效性
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (connectTimeout <= 0) {
            errors.add("连接超时时间必须大于0")
        }
        
        if (readTimeout <= 0) {
            errors.add("读取超时时间必须大于0")
        }
        
        if (writeTimeout <= 0) {
            errors.add("写入超时时间必须大于0")
        }
        
        if (callTimeout <= 0) {
            errors.add("调用超时时间必须大于0")
        }
        
        if (maxRequests <= 0) {
            errors.add("最大请求数必须大于0")
        }
        
        if (maxRequestsPerHost <= 0) {
            errors.add("每主机最大请求数必须大于0")
        }
        
        if (connectionPoolMaxIdle <= 0) {
            errors.add("连接池最大空闲连接数必须大于0")
        }
        
        if (connectionPoolKeepAlive <= 0) {
            errors.add("连接池保持活跃时间必须大于0")
        }
        
        if (userAgent.isBlank()) {
            errors.add("用户代理不能为空")
        }
        
        return errors
    }
}

/**
 * 网络请求结果
 */
sealed class NetworkResult<T> {
    data class Success<T>(val data: T, val response: Response) : NetworkResult<T>()
    data class Error<T>(val exception: Exception, val response: Response? = null) : NetworkResult<T>()
    data class Loading<T>(val progress: Float = 0f) : NetworkResult<T>()
}

/**
 * 下载进度回调
 */
interface DownloadProgressCallback {
    fun onProgress(bytesRead: Long, totalBytes: Long, percentage: Float)
    fun onStart(totalBytes: Long)
    fun onComplete()
    fun onError(exception: Exception)
}

/**
 * 网络状态监听器
 */
interface NetworkStateListener {
    fun onNetworkStateChanged(state: NetworkState, type: NetworkType)
    fun onNetworkAvailable(type: NetworkType)
    fun onNetworkLost()
}

/**
 * 进度响应体
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressCallback: DownloadProgressCallback
) : ResponseBody() {
    
    private var bufferedSource: BufferedSource? = null
    
    override fun contentType(): MediaType? = responseBody.contentType()
    
    override fun contentLength(): Long = responseBody.contentLength()
    
    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }
    
    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            val contentLength = responseBody.contentLength()
            
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0L
                
                val percentage = if (contentLength > 0) {
                    (totalBytesRead.toFloat() / contentLength.toFloat()) * 100f
                } else {
                    0f
                }
                
                progressCallback.onProgress(totalBytesRead, contentLength, percentage)
                
                return bytesRead
            }
        }
    }
}

/**
 * 进度拦截器
 */
class ProgressInterceptor(
    private val progressCallback: DownloadProgressCallback
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        
        return originalResponse.newBuilder()
            .body(ProgressResponseBody(originalResponse.body!!, progressCallback))
            .build()
    }
}

/**
 * 网络管理器
 * 
 * 负责媒体处理器的网络管理，
 * 包括HTTP请求、下载、网络状态监控等。
 * 
 * 主要功能：
 * - HTTP请求管理
 * - 文件下载
 * - 网络状态监控
 * - 代理支持
 * 
 * 使用示例：
 * ```kotlin
 * val networkManager = NetworkManager(context, config)
 * val response = networkManager.get(url)
 * networkManager.downloadFile(url, file) { progress -> ... }
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class NetworkManager(
    private val context: Context,
    private val config: NetworkConfig = NetworkConfig()
) {
    
    companion object {
        private const val TAG = "NetworkManager"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkStateListeners = mutableSetOf<NetworkStateListener>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // OkHttp客户端
    private val okHttpClient: OkHttpClient by lazy {
        createOkHttpClient()
    }
    
    // 网络状态回调
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            val networkType = getCurrentNetworkType()
            notifyNetworkStateChanged(NetworkState.CONNECTED, networkType)
            notifyNetworkAvailable(networkType)
        }
        
        override fun onLost(network: android.net.Network) {
            notifyNetworkStateChanged(NetworkState.DISCONNECTED, NetworkType.UNKNOWN)
            notifyNetworkLost()
        }
        
        override fun onCapabilitiesChanged(
            network: android.net.Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val networkType = getNetworkType(networkCapabilities)
            notifyNetworkStateChanged(NetworkState.CONNECTED, networkType)
        }
    }
    
    init {
        registerNetworkCallback()
    }
    
    /**
     * 创建OkHttp客户端
     */
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.MILLISECONDS)
            .callTimeout(config.callTimeout, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(config.retryOnConnectionFailure)
            .followRedirects(config.followRedirects)
            .followSslRedirects(config.followSslRedirects)
            .connectionPool(
                ConnectionPool(
                    config.connectionPoolMaxIdle,
                    config.connectionPoolKeepAlive,
                    TimeUnit.MINUTES
                )
            )
        
        // 设置调度器
        val dispatcher = Dispatcher()
        dispatcher.maxRequests = config.maxRequests
        dispatcher.maxRequestsPerHost = config.maxRequestsPerHost
        builder.dispatcher(dispatcher)
        
        // 添加用户代理拦截器
        builder.addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .header("User-Agent", config.userAgent)
            
            // 添加自定义头部
            config.customHeaders.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            chain.proceed(requestBuilder.build())
        }
        
        // 添加日志拦截器
        if (config.enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = config.logLevel
            builder.addInterceptor(loggingInterceptor)
        }
        
        // 设置代理
        config.proxyConfig?.let { proxyConfig ->
            builder.proxy(proxyConfig.createProxy())
            
            if (proxyConfig.requiresAuth()) {
                builder.proxyAuthenticator { _, response ->
                    val credential = Credentials.basic(
                        proxyConfig.username!!, 
                        proxyConfig.password!!
                    )
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }
        }
        
        // 信任所有证书（仅用于测试）
        if (config.enableTrustAllCerts) {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }
        
        return builder.build()
    }
    
    /**
     * GET请求
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): NetworkResult<String> {
        return executeRequest {
            val requestBuilder = Request.Builder().url(url).get()
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            requestBuilder.build()
        }
    }
    
    /**
     * POST请求
     */
    suspend fun post(
        url: String, 
        body: RequestBody, 
        headers: Map<String, String> = emptyMap()
    ): NetworkResult<String> {
        return executeRequest {
            val requestBuilder = Request.Builder().url(url).post(body)
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            requestBuilder.build()
        }
    }
    
    /**
     * HEAD请求
     */
    suspend fun head(url: String, headers: Map<String, String> = emptyMap()): NetworkResult<Response> {
        return suspendCoroutine { continuation ->
            val requestBuilder = Request.Builder().url(url).head()
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            val request = requestBuilder.build()
            
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(NetworkResult.Error(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(NetworkResult.Success(response, response))
                }
            })
        }
    }
    
    /**
     * 下载文件
     */
    suspend fun downloadFile(
        url: String,
        outputFile: java.io.File,
        headers: Map<String, String> = emptyMap(),
        progressCallback: DownloadProgressCallback? = null
    ): NetworkResult<java.io.File> {
        return suspendCoroutine { continuation ->
            val requestBuilder = Request.Builder().url(url).get()
            headers.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            val clientBuilder = okHttpClient.newBuilder()
            
            // 添加进度拦截器
            progressCallback?.let { callback ->
                clientBuilder.addNetworkInterceptor(ProgressInterceptor(callback))
            }
            
            val client = clientBuilder.build()
            val request = requestBuilder.build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    progressCallback?.onError(e)
                    continuation.resume(NetworkResult.Error(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            val exception = IOException("HTTP ${response.code}: ${response.message}")
                            progressCallback?.onError(exception)
                            continuation.resume(NetworkResult.Error(exception, response))
                            return
                        }
                        
                        val responseBody = response.body
                        if (responseBody == null) {
                            val exception = IOException("Response body is null")
                            progressCallback?.onError(exception)
                            continuation.resume(NetworkResult.Error(exception, response))
                            return
                        }
                        
                        progressCallback?.onStart(responseBody.contentLength())
                        
                        // 确保父目录存在
                        outputFile.parentFile?.mkdirs()
                        
                        // 写入文件
                        outputFile.outputStream().use { fileOut ->
                            responseBody.byteStream().use { inputStream ->
                                inputStream.copyTo(fileOut)
                            }
                        }
                        
                        progressCallback?.onComplete()
                        continuation.resume(NetworkResult.Success(outputFile, response))
                        
                    } catch (e: Exception) {
                        progressCallback?.onError(e)
                        continuation.resume(NetworkResult.Error(e, response))
                    }
                }
            })
        }
    }
    
    /**
     * 分片下载文件
     */
    suspend fun downloadFileWithRanges(
        url: String,
        outputFile: java.io.File,
        chunkSize: Long = 1024 * 1024, // 1MB
        headers: Map<String, String> = emptyMap(),
        progressCallback: DownloadProgressCallback? = null
    ): NetworkResult<java.io.File> {
        return try {
            // 首先获取文件大小
            val headResult = head(url, headers)
            if (headResult !is NetworkResult.Success) {
                return NetworkResult.Error(IOException("Failed to get file size"))
            }
            
            val contentLength = headResult.data.header("Content-Length")?.toLongOrNull()
                ?: return downloadFile(url, outputFile, headers, progressCallback)
            
            // 检查是否支持范围请求
            val acceptRanges = headResult.data.header("Accept-Ranges")
            if (acceptRanges != "bytes") {
                return downloadFile(url, outputFile, headers, progressCallback)
            }
            
            progressCallback?.onStart(contentLength)
            
            // 确保父目录存在
            outputFile.parentFile?.mkdirs()
            
            // 分片下载
            outputFile.outputStream().use { fileOut ->
                var downloadedBytes = 0L
                var start = 0L
                
                while (start < contentLength) {
                    val end = minOf(start + chunkSize - 1, contentLength - 1)
                    
                    val rangeHeaders = headers.toMutableMap()
                    rangeHeaders["Range"] = "bytes=$start-$end"
                    
                    val chunkResult = get(url, rangeHeaders)
                    if (chunkResult !is NetworkResult.Success) {
                        throw IOException("Failed to download chunk $start-$end")
                    }
                    
                    val chunkData = chunkResult.data.toByteArray()
                    fileOut.write(chunkData)
                    
                    downloadedBytes += chunkData.size
                    val percentage = (downloadedBytes.toFloat() / contentLength.toFloat()) * 100f
                    progressCallback?.onProgress(downloadedBytes, contentLength, percentage)
                    
                    start = end + 1
                }
            }
            
            progressCallback?.onComplete()
            NetworkResult.Success(outputFile, headResult.data)
            
        } catch (e: Exception) {
            progressCallback?.onError(e)
            NetworkResult.Error(e)
        }
    }
    
    /**
     * 执行请求
     */
    private suspend fun executeRequest(requestBuilder: () -> Request): NetworkResult<String> {
        return suspendCoroutine { continuation ->
            val request = requestBuilder()
            
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(NetworkResult.Error(e))
                }
                
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            continuation.resume(
                                NetworkResult.Error(
                                    IOException("HTTP ${response.code}: ${response.message}"),
                                    response
                                )
                            )
                            return
                        }
                        
                        val responseBody = response.body?.string() ?: ""
                        continuation.resume(NetworkResult.Success(responseBody, response))
                        
                    } catch (e: Exception) {
                        continuation.resume(NetworkResult.Error(e, response))
                    }
                }
            })
        }
    }
    
    /**
     * 检查网络连接
     */
    fun isNetworkAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * 获取当前网络类型
     */
    fun getCurrentNetworkType(): NetworkType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.UNKNOWN
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.UNKNOWN
            return getNetworkType(capabilities)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.UNKNOWN
            return when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                ConnectivityManager.TYPE_VPN -> NetworkType.VPN
                else -> NetworkType.UNKNOWN
            }
        }
    }
    
    /**
     * 获取网络类型
     */
    private fun getNetworkType(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }
    }
    
    /**
     * 注册网络状态监听器
     */
    fun addNetworkStateListener(listener: NetworkStateListener) {
        networkStateListeners.add(listener)
    }
    
    /**
     * 移除网络状态监听器
     */
    fun removeNetworkStateListener(listener: NetworkStateListener) {
        networkStateListeners.remove(listener)
    }
    
    /**
     * 注册网络回调
     */
    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }
    
    /**
     * 通知网络状态变化
     */
    private fun notifyNetworkStateChanged(state: NetworkState, type: NetworkType) {
        coroutineScope.launch {
            networkStateListeners.forEach { listener ->
                try {
                    listener.onNetworkStateChanged(state, type)
                } catch (e: Exception) {
                    // 忽略监听器错误
                }
            }
        }
    }
    
    /**
     * 通知网络可用
     */
    private fun notifyNetworkAvailable(type: NetworkType) {
        coroutineScope.launch {
            networkStateListeners.forEach { listener ->
                try {
                    listener.onNetworkAvailable(type)
                } catch (e: Exception) {
                    // 忽略监听器错误
                }
            }
        }
    }
    
    /**
     * 通知网络丢失
     */
    private fun notifyNetworkLost() {
        coroutineScope.launch {
            networkStateListeners.forEach { listener ->
                try {
                    listener.onNetworkLost()
                } catch (e: Exception) {
                    // 忽略监听器错误
                }
            }
        }
    }
    
    /**
     * 关闭网络管理器
     */
    fun close() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // 忽略注销错误
        }
        
        coroutineScope.cancel()
        networkStateListeners.clear()
        
        // 关闭OkHttp客户端
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }
}