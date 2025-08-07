package com.nexus.core.media.processor.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.nexus.core.media.processor.config.ProcessorConfiguration
import java.io.File

/**
 * 访问控制管理器
 * 负责管理权限检查和访问控制
 */
class AccessControlManager(
    private val context: Context,
    private val configuration: ProcessorConfiguration
) {
    
    /**
     * 检查是否有网络权限
     */
    fun hasNetworkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查是否有读取存储权限
     */
    fun hasReadStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查文件访问权限
     */
    fun canAccessFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.canRead()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查文件写入权限
     */
    fun canWriteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            file.canWrite() || (!file.exists() && parentDir?.canWrite() == true)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查目录访问权限
     */
    fun canAccessDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            dir.exists() && dir.isDirectory && dir.canRead()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查URL访问权限
     */
    fun canAccessUrl(url: String): Boolean {
        return try {
            // 基本的URL格式检查
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 验证输出目录权限
     */
    fun validateOutputDirectory(): Boolean {
        return canAccessDirectory(configuration.outputDirectory) && 
               canWriteFile(File(configuration.outputDirectory, "test.tmp").absolutePath)
    }
    
    /**
     * 检查所有必需权限
     */
    fun checkAllPermissions(): PermissionCheckResult {
        val results = mutableMapOf<String, Boolean>()
        
        results["network"] = hasNetworkPermission()
        results["storage_write"] = hasStoragePermission()
        results["storage_read"] = hasReadStoragePermission()
        results["output_directory"] = validateOutputDirectory()
        
        val allGranted = results.values.all { it }
        val missingPermissions = results.filterValues { !it }.keys.toList()
        
        return PermissionCheckResult(
            allGranted = allGranted,
            grantedPermissions = results.filterValues { it }.keys.toList(),
            missingPermissions = missingPermissions
        )
    }
    
    /**
     * 权限检查结果
     */
    data class PermissionCheckResult(
        val allGranted: Boolean,
        val grantedPermissions: List<String>,
        val missingPermissions: List<String>
    )
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 清理相关资源
    }
}