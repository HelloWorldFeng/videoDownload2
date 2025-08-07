# Consumer ProGuard rules for video-downloader-module

# 保护模块核心API类
-keep class com.nexus.core.media.processor.** { *; }

# 保护数据模型类
-keep class com.nexus.core.media.processor.model.** { *; }

# 保护配置类
-keep class com.nexus.core.media.processor.config.** { *; }

# 保护回调接口
-keep interface com.nexus.core.media.processor.api.** { *; }

# Gson序列化
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Kotlin协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**