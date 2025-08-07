# Video Downloader Module ProGuard Rules

# Keep all classes in the video downloader module
-keep class com.videodownloader.module.** { *; }
-keep class com.nexus.core.media.processor.** { *; }

# Keep YouTubeDL Android library
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Keep FFmpeg library
-keep class com.arthenica.mobileffmpeg.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.videodownloader.module.**$$serializer { *; }
-keepclassmembers class com.videodownloader.module.** {
    *** Companion;
}
-keepclasseswithmembers class com.videodownloader.module.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep service classes
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Keep Retrofit and OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep model classes for serialization
-keep class com.videodownloader.module.core.model.** { *; }
-keep class com.nexus.core.media.processor.model.** { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}