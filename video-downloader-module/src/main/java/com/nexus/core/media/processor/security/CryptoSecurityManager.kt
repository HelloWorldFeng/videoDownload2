package com.nexus.core.media.processor.security

import android.content.Context
import android.util.Base64
import com.nexus.core.media.processor.config.ProcessorConfiguration
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密安全管理器
 * 负责处理数据加密、解密和安全相关功能
 */
class CryptoSecurityManager(
    private val context: Context,
    private val configuration: ProcessorConfiguration
) {
    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyAlgorithm = "AES"
    private val hashAlgorithm = "SHA-256"
    
    /**
     * 生成随机密钥
     */
    fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(keyAlgorithm)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }
    
    /**
     * 从字节数组创建密钥
     */
    fun createSecretKey(keyBytes: ByteArray): SecretKey {
        return SecretKeySpec(keyBytes, keyAlgorithm)
    }
    
    /**
     * 加密数据
     */
    fun encrypt(data: ByteArray, secretKey: SecretKey): EncryptionResult {
        return try {
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)
            
            EncryptionResult(
                success = true,
                encryptedData = encryptedData,
                iv = iv,
                error = null
            )
        } catch (e: Exception) {
            EncryptionResult(
                success = false,
                encryptedData = null,
                iv = null,
                error = e.message
            )
        }
    }
    
    /**
     * 解密数据
     */
    fun decrypt(encryptedData: ByteArray, secretKey: SecretKey, iv: ByteArray): DecryptionResult {
        return try {
            val cipher = Cipher.getInstance(algorithm)
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            
            val decryptedData = cipher.doFinal(encryptedData)
            
            DecryptionResult(
                success = true,
                decryptedData = decryptedData,
                error = null
            )
        } catch (e: Exception) {
            DecryptionResult(
                success = false,
                decryptedData = null,
                error = e.message
            )
        }
    }
    
    /**
     * 计算数据哈希值
     */
    fun calculateHash(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance(hashAlgorithm)
            val hashBytes = digest.digest(data)
            Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 验证数据完整性
     */
    fun verifyIntegrity(data: ByteArray, expectedHash: String): Boolean {
        val actualHash = calculateHash(data)
        return actualHash == expectedHash
    }
    
    /**
     * 生成随机盐值
     */
    fun generateSalt(length: Int = 16): ByteArray {
        val salt = ByteArray(length)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * 生成安全的随机字符串
     */
    fun generateSecureRandomString(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
    
    /**
     * 清理敏感数据
     */
    fun clearSensitiveData(data: ByteArray) {
        data.fill(0)
    }
    
    /**
     * 加密结果
     */
    data class EncryptionResult(
        val success: Boolean,
        val encryptedData: ByteArray?,
        val iv: ByteArray?,
        val error: String?
    )
    
    /**
     * 解密结果
     */
    data class DecryptionResult(
        val success: Boolean,
        val decryptedData: ByteArray?,
        val error: String?
    )
    
    /**
     * 清理资源
     */
    fun cleanup() {
        // 清理相关资源
    }
}