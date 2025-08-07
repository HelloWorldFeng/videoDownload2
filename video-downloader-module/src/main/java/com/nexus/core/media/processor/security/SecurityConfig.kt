package com.nexus.core.media.processor.security

/**
 * 安全配置类
 * 
 * 定义媒体处理器的安全相关配置选项。
 * 
 * @author MediaProcessor Module
 * @version 1.0.0
 * @since 2024-01-01
 */
data class SecurityConfig(
    /** 是否启用加密 */
    val enableEncryption: Boolean = false,
    
    /** 是否启用权限检查 */
    val enablePermissionCheck: Boolean = true,
    
    /** 是否启用文件完整性检查 */
    val enableFileIntegrityCheck: Boolean = true,
    
    /** 是否启用安全随机数 */
    val enableSecureRandom: Boolean = true,
    
    /** 密钥派生迭代次数 */
    val keyDerivationIterations: Int = 10000,
    
    /** 是否允许弱密码 */
    val allowWeakPasswords: Boolean = false,
    
    /** 最大失败尝试次数 */
    val maxFailedAttempts: Int = 3,
    
    /** 加密算法 */
    val encryptionAlgorithm: String = "AES/GCM/NoPadding",
    
    /** 密钥长度 */
    val keyLength: Int = 256,
    
    /** 是否启用SSL验证 */
    val enableSslVerification: Boolean = true,
    
    /** 是否允许自签名证书 */
    val allowSelfSignedCertificates: Boolean = false,
    
    /** 是否启用证书固定 */
    val enableCertificatePinning: Boolean = false,
    
    /** 固定的证书指纹 */
    val pinnedCertificateFingerprints: List<String> = emptyList(),
    
    /** 是否启用网络安全配置 */
    val enableNetworkSecurityConfig: Boolean = true,
    
    /** 是否启用混淆 */
    val enableObfuscation: Boolean = false,
    
    /** 是否启用反调试 */
    val enableAntiDebugging: Boolean = false
) {
    
    /**
     * 验证配置的有效性
     * 
     * @return 错误信息列表，空列表表示配置有效
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (keyDerivationIterations <= 0) {
            errors.add("密钥派生迭代次数必须大于0")
        }
        
        if (maxFailedAttempts <= 0) {
            errors.add("最大失败尝试次数必须大于0")
        }
        
        if (keyLength !in listOf(128, 192, 256)) {
            errors.add("密钥长度必须是128、192或256位")
        }
        
        if (enableEncryption && encryptionAlgorithm.isBlank()) {
            errors.add("启用加密时必须指定加密算法")
        }
        
        if (enableCertificatePinning && pinnedCertificateFingerprints.isEmpty()) {
            errors.add("启用证书固定时必须提供证书指纹")
        }
        
        return errors
    }
    
    /**
     * 是否为高安全级别配置
     * 
     * @return 是否为高安全级别
     */
    fun isHighSecurity(): Boolean {
        return enableEncryption &&
               enablePermissionCheck &&
               enableFileIntegrityCheck &&
               enableSslVerification &&
               !allowSelfSignedCertificates &&
               !allowWeakPasswords
    }
    
    /**
     * 获取安全级别描述
     * 
     * @return 安全级别描述
     */
    fun getSecurityLevelDescription(): String {
        return when {
            isHighSecurity() -> "高安全级别"
            enableEncryption || enableSslVerification -> "中等安全级别"
            else -> "基础安全级别"
        }
    }
    
    companion object {
        /**
         * 创建默认配置
         */
        fun default(): SecurityConfig {
            return SecurityConfig()
        }
        
        /**
         * 创建高安全配置
         */
        fun highSecurity(): SecurityConfig {
            return SecurityConfig(
                enableEncryption = true,
                enablePermissionCheck = true,
                enableFileIntegrityCheck = true,
                enableSecureRandom = true,
                keyDerivationIterations = 50000,
                allowWeakPasswords = false,
                maxFailedAttempts = 3,
                keyLength = 256,
                enableSslVerification = true,
                allowSelfSignedCertificates = false,
                enableCertificatePinning = true,
                enableNetworkSecurityConfig = true,
                enableAntiDebugging = true
            )
        }
        
        /**
         * 创建开发环境配置
         */
        fun development(): SecurityConfig {
            return SecurityConfig(
                enableEncryption = false,
                enablePermissionCheck = true,
                enableFileIntegrityCheck = false,
                enableSecureRandom = false,
                allowWeakPasswords = true,
                maxFailedAttempts = 10,
                enableSslVerification = false,
                allowSelfSignedCertificates = true,
                enableCertificatePinning = false,
                enableAntiDebugging = false
            )
        }
        
        /**
         * 创建最小配置
         */
        fun minimal(): SecurityConfig {
            return SecurityConfig(
                enableEncryption = false,
                enablePermissionCheck = false,
                enableFileIntegrityCheck = false,
                enableSecureRandom = false,
                allowWeakPasswords = true,
                enableSslVerification = false,
                allowSelfSignedCertificates = true
            )
        }
    }
}