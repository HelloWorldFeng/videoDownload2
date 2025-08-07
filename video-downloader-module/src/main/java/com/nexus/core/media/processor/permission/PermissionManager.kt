package com.nexus.core.media.processor.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// import androidx.fragment.app.Fragment
import com.nexus.core.media.processor.logging.LogManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 权限管理器
 * 负责处理媒体处理器所需的各种权限
 */
class PermissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PermissionManager"
        
        // 权限请求码
        const val REQUEST_CODE_STORAGE = 1001
        const val REQUEST_CODE_NETWORK = 1002
        const val REQUEST_CODE_NOTIFICATION = 1003
        const val REQUEST_CODE_ALL = 1004
        
        // 必需权限
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        // 存储权限
        val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        // 通知权限
        val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
        
        // 所有权限
        val ALL_PERMISSIONS = REQUIRED_PERMISSIONS + STORAGE_PERMISSIONS + NOTIFICATION_PERMISSIONS
    }
    
    private val logManager = LogManager.getInstance(context)
    
    /**
     * 权限检查结果
     */
    data class PermissionResult(
        val granted: Boolean,
        val deniedPermissions: List<String> = emptyList(),
        val shouldShowRationale: List<String> = emptyList()
    )
    
    /**
     * 权限请求回调
     */
    interface PermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied(deniedPermissions: List<String>)
        fun onPermissionRationale(rationalePermissions: List<String>)
    }
    
    /**
     * 检查是否有指定权限
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查是否有多个权限
     */
    fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }
    
    /**
     * 检查必需权限
     */
    fun hasRequiredPermissions(): Boolean {
        return hasPermissions(REQUIRED_PERMISSIONS)
    }
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermissions(): Boolean {
        return hasPermissions(STORAGE_PERMISSIONS)
    }
    
    /**
     * 检查通知权限
     */
    fun hasNotificationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(NOTIFICATION_PERMISSIONS)
        } else {
            true // Android 13以下不需要通知权限
        }
    }
    
    /**
     * 检查所有权限
     */
    fun hasAllPermissions(): Boolean {
        return hasRequiredPermissions() && hasStoragePermissions() && hasNotificationPermissions()
    }
    
    /**
     * 获取缺失的权限
     */
    fun getMissingPermissions(): List<String> {
        return ALL_PERMISSIONS.filter { !hasPermission(it) }
    }
    
    /**
     * 获取缺失的必需权限
     */
    fun getMissingRequiredPermissions(): List<String> {
        return REQUIRED_PERMISSIONS.filter { !hasPermission(it) }
    }
    
    /**
     * 获取缺失的存储权限
     */
    fun getMissingStoragePermissions(): List<String> {
        return STORAGE_PERMISSIONS.filter { !hasPermission(it) }
    }
    
    /**
     * 获取缺失的通知权限
     */
    fun getMissingNotificationPermissions(): List<String> {
        return NOTIFICATION_PERMISSIONS.filter { !hasPermission(it) }
    }
    
    /**
     * 检查权限并返回详细结果
     */
    fun checkPermissions(permissions: Array<String>): PermissionResult {
        val deniedPermissions = permissions.filter { !hasPermission(it) }
        
        if (deniedPermissions.isEmpty()) {
            return PermissionResult(granted = true)
        }
        
        return PermissionResult(
            granted = false,
            deniedPermissions = deniedPermissions
        )
    }
    
    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowRequestPermissionRationale(
        activity: Activity,
        permissions: Array<String>
    ): List<String> {
        return permissions.filter {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }
    
    /**
     * 请求权限（Activity）
     */
    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ) {
        logManager.d(TAG, "请求权限: ${permissions.joinToString(", ")}")
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
    
    /**
     * 请求权限（Fragment）
     */
    /*
    fun requestPermissions(
        fragment: Fragment,
        permissions: Array<String>,
        requestCode: Int
    ) {
        logManager.d(TAG, "请求权限: ${permissions.joinToString(", ")}")        fragment.requestPermissions(permissions, requestCode)
    }
    */
    
    /**
     * 请求必需权限
     */
    fun requestRequiredPermissions(activity: Activity) {
        val missingPermissions = getMissingRequiredPermissions()
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CODE_NETWORK)
        }
    }
    
    /**
     * 请求存储权限
     */
    fun requestStoragePermissions(activity: Activity) {
        val missingPermissions = getMissingStoragePermissions()
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CODE_STORAGE)
        }
    }
    
    /**
     * 请求通知权限
     */
    fun requestNotificationPermissions(activity: Activity) {
        val missingPermissions = getMissingNotificationPermissions()
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CODE_NOTIFICATION)
        }
    }
    
    /**
     * 请求所有权限
     */
    fun requestAllPermissions(activity: Activity) {
        val missingPermissions = getMissingPermissions()
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(activity, missingPermissions.toTypedArray(), REQUEST_CODE_ALL)
        }
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        callback: PermissionCallback
    ) {
        if (permissions.isEmpty() || grantResults.isEmpty()) {
            callback.onPermissionDenied(emptyList())
            return
        }
        
        val deniedPermissions = mutableListOf<String>()
        
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i])
            }
        }
        
        if (deniedPermissions.isEmpty()) {
            logManager.i(TAG, "权限授予成功")
            callback.onPermissionGranted()
        } else {
            logManager.w(TAG, "权限被拒绝: ${deniedPermissions.joinToString(", ")}")
            callback.onPermissionDenied(deniedPermissions)
        }
    }
    
    /**
     * 检查是否可以写入外部存储
     */
    fun canWriteExternalStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * 检查是否可以读取外部存储
     */
    fun canReadExternalStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.READ_MEDIA_VIDEO) ||
            hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * 获取权限状态描述
     */
    fun getPermissionStatusDescription(): String {
        return buildString {
            appendLine("权限状态:")
            appendLine("- 必需权限: ${if (hasRequiredPermissions()) "已授予" else "未授予"}")
            appendLine("- 存储权限: ${if (hasStoragePermissions()) "已授予" else "未授予"}")
            appendLine("- 通知权限: ${if (hasNotificationPermissions()) "已授予" else "未授予"}")
            
            val missingPermissions = getMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                appendLine("缺失权限:")
                missingPermissions.forEach { permission ->
                    appendLine("  - ${getPermissionName(permission)}")
                }
            }
        }
    }
    
    /**
     * 获取权限友好名称
     */
    private fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.INTERNET -> "网络访问"
            Manifest.permission.ACCESS_NETWORK_STATE -> "网络状态"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "读取存储"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "写入存储"
            Manifest.permission.READ_MEDIA_VIDEO -> "读取视频"
            Manifest.permission.READ_MEDIA_AUDIO -> "读取音频"
            Manifest.permission.POST_NOTIFICATIONS -> "发送通知"
            else -> permission
        }
    }
    
    /**
     * 协程方式请求权限
     */
    suspend fun requestPermissionsAsync(
        activity: Activity,
        permissions: Array<String>
    ): PermissionResult = suspendCancellableCoroutine { continuation ->
        val callback = object : PermissionCallback {
            override fun onPermissionGranted() {
                continuation.resume(PermissionResult(granted = true))
            }
            
            override fun onPermissionDenied(deniedPermissions: List<String>) {
                continuation.resume(
                    PermissionResult(
                        granted = false,
                        deniedPermissions = deniedPermissions
                    )
                )
            }
            
            override fun onPermissionRationale(rationalePermissions: List<String>) {
                continuation.resume(
                    PermissionResult(
                        granted = false,
                        shouldShowRationale = rationalePermissions
                    )
                )
            }
        }
        
        // 检查是否已有权限
        val result = checkPermissions(permissions)
        if (result.granted) {
            callback.onPermissionGranted()
            return@suspendCancellableCoroutine
        }
        
        // 检查是否需要显示说明
        val rationalePermissions = shouldShowRequestPermissionRationale(activity, permissions)
        if (rationalePermissions.isNotEmpty()) {
            callback.onPermissionRationale(rationalePermissions)
            return@suspendCancellableCoroutine
        }
        
        // 请求权限
        requestPermissions(activity, permissions, REQUEST_CODE_ALL)
        
        // 注意：实际的权限结果需要在Activity的onRequestPermissionsResult中处理
        // 这里只是演示协程的使用方式
    }
}