package com.nexus.core.media.processor.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.*
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.random.Random

/**
 * 安全管理器
 * 
 * 负责媒体处理器的安全相关功能，
 * 包括加密解密、权限验证、安全检查等。
 * 
 * 主要功能：
 * - 文件加密/解密
 * - 权限管理
 * - 安全验证
 * - 密钥管理
 * 
 * 使用示例：
 * ```kotlin
 * val securityManager = SecurityManager(context)
 * val encrypted = securityManager.encryptFile(file, password)
 * val hasPermission = securityManager.checkPermission(permission)
 * ```
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
class SecurityManager(private val context: Context) {
    
    companion object {
        private const val ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_LENGTH = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        // 必需权限列表
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        // 可选权限列表
        private val OPTIONAL_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE
        )
    }
    
    private val secureRandom = SecureRandom()
    private val keyCache = mutableMapOf<String, SecretKey>()
    
    /**
     * 权限检查结果
     */
    data class PermissionCheckResult(
        val hasAllRequired: Boolean,
        val missingRequired: List<String>,
        val hasAllOptional: Boolean,
        val missingOptional: List<String>,
        val recommendations: List<String>
    )
    
    /**
     * 加密结果
     */
    data class EncryptionResult(
        val success: Boolean,
        val encryptedData: ByteArray? = null,
        val iv: ByteArray? = null,
        val error: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as EncryptionResult
            
            if (success != other.success) return false
            if (encryptedData != null) {
                if (other.encryptedData == null) return false
                if (!encryptedData.contentEquals(other.encryptedData)) return false
            } else if (other.encryptedData != null) return false
            if (iv != null) {
                if (other.iv == null) return false
                if (!iv.contentEquals(other.iv)) return false
            } else if (other.iv != null) return false
            if (error != other.error) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = success.hashCode()
            result = 31 * result + (encryptedData?.contentHashCode() ?: 0)
            result = 31 * result + (iv?.contentHashCode() ?: 0)
            result = 31 * result + (error?.hashCode() ?: 0)
            return result
        }
    }
    
    /**
     * 解密结果
     */
    data class DecryptionResult(
        val success: Boolean,
        val decryptedData: ByteArray? = null,
        val error: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as DecryptionResult
            
            if (success != other.success) return false
            if (decryptedData != null) {
                if (other.decryptedData == null) return false
                if (!decryptedData.contentEquals(other.decryptedData)) return false
            } else if (other.decryptedData != null) return false
            if (error != other.error) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = success.hashCode()
            result = 31 * result + (decryptedData?.contentHashCode() ?: 0)
            result = 31 * result + (error?.hashCode() ?: 0)
            return result
        }
    }
    
    /**
     * 安全配置
     */
    data class SecurityConfig(
        val enableEncryption: Boolean = true,
        val enablePermissionCheck: Boolean = true,
        val enableFileIntegrityCheck: Boolean = true,
        val enableSecureRandom: Boolean = true,
        val keyDerivationIterations: Int = 10000,
        val allowWeakPasswords: Boolean = false,
        val maxFailedAttempts: Int = 3
    )
    
    private var securityConfig = SecurityConfig()
    private var failedAttempts = 0
    
    /**
     * 设置安全配置
     * 
     * @param config 安全配置
     */
    fun setSecurityConfig(config: SecurityConfig) {
        this.securityConfig = config
    }
    
    /**
     * 检查所有权限
     * 
     * @return 权限检查结果
     */
    fun checkAllPermissions(): PermissionCheckResult {
        if (!securityConfig.enablePermissionCheck) {
            return PermissionCheckResult(
                hasAllRequired = true,
                missingRequired = emptyList(),
                hasAllOptional = true,
                missingOptional = emptyList(),
                recommendations = emptyList()
            )
        }
        
        val missingRequired = mutableListOf<String>()
        val missingOptional = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // 检查必需权限
        REQUIRED_PERMISSIONS.forEach { permission ->
            if (!checkPermission(permission)) {
                missingRequired.add(permission)
            }
        }
        
        // 检查可选权限
        OPTIONAL_PERMISSIONS.forEach { permission ->
            if (!checkPermission(permission)) {
                missingOptional.add(permission)
            }
        }
        
        // 生成建议
        if (missingRequired.isNotEmpty()) {
            recommendations.add("请授予必需权限以确保应用正常运行")
        }
        
        if (missingOptional.isNotEmpty()) {
            recommendations.add("建议授予可选权限以获得更好的用户体验")
        }
        
        // Android 11+ 存储权限特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            recommendations.add("Android 11+ 建议使用分区存储")
        }
        
        return PermissionCheckResult(
            hasAllRequired = missingRequired.isEmpty(),
            missingRequired = missingRequired,
            hasAllOptional = missingOptional.isEmpty(),
            missingOptional = missingOptional,
            recommendations = recommendations
        )
    }
    
    /**
     * 检查单个权限
     * 
     * @param permission 权限名称
     * @return 是否有权限
     */
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 生成密钥
     * 
     * @param password 密码
     * @param salt 盐值
     * @return 密钥
     */
    fun generateKey(password: String, salt: ByteArray? = null): SecretKey {
        val actualSalt = salt ?: generateSalt()
        val keySpec = deriveKeyFromPassword(password, actualSalt)
        return SecretKeySpec(keySpec, KEY_ALGORITHM)
    }
    
    /**
     * 生成随机密钥
     * 
     * @return 密钥
     */
    fun generateRandomKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM)
        keyGenerator.init(KEY_LENGTH, secureRandom)
        return keyGenerator.generateKey()
    }
    
    /**
     * 加密数据
     * 
     * @param data 原始数据
     * @param key 加密密钥
     * @return 加密结果
     */
    fun encryptData(data: ByteArray, key: SecretKey): EncryptionResult {
        if (!securityConfig.enableEncryption) {
            return EncryptionResult(
                success = true,
                encryptedData = data,
                iv = ByteArray(0)
            )
        }
        
        return try {
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec)
            
            val encryptedData = cipher.doFinal(data)
            
            EncryptionResult(
                success = true,
                encryptedData = encryptedData,
                iv = iv
            )
        } catch (e: Exception) {
            EncryptionResult(
                success = false,
                error = "加密失败: ${e.message}"
            )
        }
    }
    
    /**
     * 解密数据
     * 
     * @param encryptedData 加密数据
     * @param key 解密密钥
     * @param iv 初始化向量
     * @return 解密结果
     */
    fun decryptData(encryptedData: ByteArray, key: SecretKey, iv: ByteArray): DecryptionResult {
        if (!securityConfig.enableEncryption) {
            return DecryptionResult(
                success = true,
                decryptedData = encryptedData
            )
        }
        
        return try {
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)
            
            val decryptedData = cipher.doFinal(encryptedData)
            
            failedAttempts = 0 // 重置失败次数
            
            DecryptionResult(
                success = true,
                decryptedData = decryptedData
            )
        } catch (e: Exception) {
            failedAttempts++
            
            val errorMessage = if (failedAttempts >= securityConfig.maxFailedAttempts) {
                "解密失败次数过多，请检查密码或密钥"
            } else {
                "解密失败: ${e.message}"
            }
            
            DecryptionResult(
                success = false,
                error = errorMessage
            )
        }
    }
    
    /**
     * 加密文件
     * 
     * @param inputFile 输入文件
     * @param outputFile 输出文件
     * @param password 密码
     * @return 是否成功
     */
    fun encryptFile(inputFile: File, outputFile: File, password: String): Boolean {
        if (!securityConfig.enableEncryption) {
            return inputFile.copyTo(outputFile, overwrite = true).exists()
        }
        
        return try {
            val salt = generateSalt()
            val key = generateKey(password, salt)
            val data = inputFile.readBytes()
            
            val encryptionResult = encryptData(data, key)
            if (!encryptionResult.success) {
                return false
            }
            
            // 写入文件：盐值 + IV + 加密数据
            outputFile.outputStream().use { output ->
                output.write(salt)
                output.write(encryptionResult.iv!!)
                output.write(encryptionResult.encryptedData!!)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 解密文件
     * 
     * @param inputFile 输入文件
     * @param outputFile 输出文件
     * @param password 密码
     * @return 是否成功
     */
    fun decryptFile(inputFile: File, outputFile: File, password: String): Boolean {
        if (!securityConfig.enableEncryption) {
            return inputFile.copyTo(outputFile, overwrite = true).exists()
        }
        
        return try {
            val fileData = inputFile.readBytes()
            
            // 读取盐值、IV和加密数据
            val saltSize = 32 // 盐值大小
            val salt = fileData.sliceArray(0 until saltSize)
            val iv = fileData.sliceArray(saltSize until saltSize + GCM_IV_LENGTH)
            val encryptedData = fileData.sliceArray(saltSize + GCM_IV_LENGTH until fileData.size)
            
            val key = generateKey(password, salt)
            val decryptionResult = decryptData(encryptedData, key, iv)
            
            if (!decryptionResult.success) {
                return false
            }
            
            outputFile.writeBytes(decryptionResult.decryptedData!!)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 验证文件完整性
     * 
     * @param file 文件
     * @param expectedHash 期望的哈希值
     * @param algorithm 哈希算法
     * @return 是否完整
     */
    fun verifyFileIntegrity(
        file: File, 
        expectedHash: String, 
        algorithm: String = "SHA-256"
    ): Boolean {
        if (!securityConfig.enableFileIntegrityCheck) {
            return true
        }
        
        return try {
            val actualHash = calculateFileHash(file, algorithm)
            actualHash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 计算文件哈希
     * 
     * @param file 文件
     * @param algorithm 哈希算法
     * @return 哈希值
     */
    fun calculateFileHash(file: File, algorithm: String = "SHA-256"): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(8192)
        
        file.inputStream().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 验证密码强度
     * 
     * @param password 密码
     * @return 密码强度评分 (0-100)
     */
    fun validatePasswordStrength(password: String): Int {
        if (securityConfig.allowWeakPasswords) {
            return 100
        }
        
        var score = 0
        
        // 长度检查
        when {
            password.length >= 12 -> score += 25
            password.length >= 8 -> score += 15
            password.length >= 6 -> score += 10
        }
        
        // 字符类型检查
        if (password.any { it.isLowerCase() }) score += 15
        if (password.any { it.isUpperCase() }) score += 15
        if (password.any { it.isDigit() }) score += 15
        if (password.any { !it.isLetterOrDigit() }) score += 20
        
        // 复杂度检查
        val uniqueChars = password.toSet().size
        if (uniqueChars >= password.length * 0.7) score += 10
        
        return minOf(score, 100)
    }
    
    /**
     * 生成安全的随机字符串
     * 
     * @param length 长度
     * @param includeSymbols 是否包含符号
     * @return 随机字符串
     */
    fun generateSecureRandomString(length: Int, includeSymbols: Boolean = false): String {
        val chars = buildString {
            append("abcdefghijklmnopqrstuvwxyz")
            append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            append("0123456789")
            if (includeSymbols) {
                append("!@#$%^&*()_+-=[]{}|;:,.<>?")
            }
        }
        
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * 清理敏感数据
     * 
     * @param data 敏感数据数组
     */
    fun clearSensitiveData(data: ByteArray) {
        secureRandom.nextBytes(data) // 用随机数据覆盖
        data.fill(0) // 再用零覆盖
    }
    
    /**
     * 清理敏感数据
     * 
     * @param data 敏感数据字符数组
     */
    fun clearSensitiveData(data: CharArray) {
        for (i in data.indices) {
            data[i] = Random.nextInt(65536).toChar() // 随机字符
        }
        data.fill('\u0000') // 再用空字符覆盖
    }
    
    /**
     * 生成盐值
     * 
     * @param size 盐值大小
     * @return 盐值
     */
    private fun generateSalt(size: Int = 32): ByteArray {
        val salt = ByteArray(size)
        secureRandom.nextBytes(salt)
        return salt
    }
    
    /**
     * 从密码派生密钥
     * 
     * @param password 密码
     * @param salt 盐值
     * @return 密钥字节数组
     */
    private fun deriveKeyFromPassword(password: String, salt: ByteArray): ByteArray {
        // 简化的密钥派生，实际应用中建议使用 PBKDF2 或 Argon2
        val digest = MessageDigest.getInstance("SHA-256")
        
        var result = password.toByteArray(StandardCharsets.UTF_8)
        
        repeat(securityConfig.keyDerivationIterations) {
            digest.reset()
            digest.update(result)
            digest.update(salt)
            result = digest.digest()
        }
        
        return result
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        keyCache.clear()
        failedAttempts = 0
    }
}