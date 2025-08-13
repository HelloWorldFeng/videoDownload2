package com.app.videobox.utils

import android.webkit.CookieManager
import android.webkit.WebView
import android.util.Log

/**
 * WebView HTTP上下文信息获取工具类
 * 用于获取WebView的cookies、请求头等信息，以便在下载视频时携带这些信息
 */
object WebViewHttpContext {
    
    private const val TAG = "WebViewHttpContext"
    
    /**
     * 获取WebView的HTTP上下文信息
     * @param webView WebView实例
     * @param currentUrl 当前页面URL
     * @return HttpContextInfo 包含cookies、请求头等信息的数据类
     */
    fun getHttpContext(webView: WebView?, currentUrl: String): HttpContextInfo {
        if (webView == null) {
            Log.w(TAG, "WebView为空，返回默认HTTP上下文")
            return HttpContextInfo()
        }
        
        return try {
            val cookies = getCookies(currentUrl)
            val userAgent = getUserAgent(webView)
            val headers = buildDefaultHeaders(userAgent, currentUrl)
            
            HttpContextInfo(
                cookies = cookies,
                userAgent = userAgent,
                referer = currentUrl,
                httpHeaders = headers
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取WebView HTTP上下文失败: ${e.message}", e)
            HttpContextInfo()
        }
    }
    
    /**
     * 获取指定URL的cookies
     * @param url 目标URL
     * @return cookies字符串
     */
    private fun getCookies(url: String): String {
        return try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.getCookie(url) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "获取cookies失败: ${e.message}", e)
            ""
        }
    }
    
    /**
     * 获取WebView的User-Agent
     * @param webView WebView实例
     * @return User-Agent字符串
     */
    private fun getUserAgent(webView: WebView): String {
        return try {
            webView.settings.userAgentString ?: getDefaultUserAgent()
        } catch (e: Exception) {
            Log.e(TAG, "获取User-Agent失败: ${e.message}", e)
            getDefaultUserAgent()
        }
    }
    
    /**
     * 构建默认的HTTP请求头
     * @param userAgent User-Agent字符串
     * @param referer 来源页面URL
     * @return HTTP请求头Map
     */
    private fun buildDefaultHeaders(userAgent: String, referer: String): Map<String, String> {
        return mapOf(
            "User-Agent" to userAgent,
            "Accept" to "*/*",
            "Accept-Encoding" to "identity;q=1, *;q=0",
            "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
            "Cache-Control" to "no-cache",
            "Connection" to "keep-alive",
            "DNT" to "1",
            "Pragma" to "no-cache",
            "Referer" to referer,
            "Sec-Fetch-Dest" to "video",
            "Sec-Fetch-Mode" to "no-cors",
            "Sec-Fetch-Site" to "cross-site",
            "Sec-GPC" to "1",
            "Upgrade-Insecure-Requests" to "1"
        )
    }
    
    /**
     * 获取默认的User-Agent
     * @return 默认User-Agent字符串
     */
    private fun getDefaultUserAgent(): String {
        return "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    
    /**
     * HTTP上下文信息数据类
     * @param cookies cookies字符串
     * @param userAgent User-Agent字符串
     * @param referer 来源页面URL
     * @param httpHeaders HTTP请求头Map
     */
    data class HttpContextInfo(
        val cookies: String = "",
        val userAgent: String = "",
        val referer: String = "",
        val httpHeaders: Map<String, String> = emptyMap()
    )
}