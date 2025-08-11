# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 设置混淆的压缩比率 0 ~ 7
-optimizationpasses 5
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共库的成员
-dontskipnonpubliclibraryclassmembers
# 混淆时不做预校验
-dontpreverify
# 混淆时不记录日志
-verbose
# 确保追溯堆栈跟踪是明确的
-keepattributes LineNumberTable,SourceFile
# 代码优化
-dontshrink
# 不优化输入的类文件
-dontoptimize
# 保留注解不混淆
-keepattributes *Annotation*,InnerClasses
# 避免混淆泛型
-keepattributes Signature
# 保留代码行号，方便异常信息的追踪
-keepattributes SourceFile,LineNumberTable
# 混淆采用的算法
-optimizations !code/simplification/cast,!field/*,!class/merging/*

#-------------- gilde start-------------
-keep class com.bumptech.glide.**{*;}
-keep class github.leavesczy.matisse.**{*;}

 ###################################################
 ## gson
 ###################################################
  # Gson specific classes
 -dontwarn sun.misc.**
-keep class com.google.gson.** {*;}
-keep class com.google.**{*;}
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }

#-------------- okhttp3 start-------------
# OkHttp3
# https://github.com/square/okhttp
# okhttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp.* { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

# okhttp 3
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Okio
-dontwarn com.squareup.**
-dontwarn okio.**
-keep public class org.codehaus.* { *; }
-keep public class java.nio.* { *; }
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn okio.**
#----------okhttp end--------------

# Retrofit
# Retain generic type information for use by reflection by converters and adapters.
# Retain service method parameters.
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keepclasseswithmembernames class * {
    native <methods>;
}

#避免Serializable接口的子类中指定的某些成员变量和方法混淆 *
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keep interface kotlin.** { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclasseswithmembers @kotlin.Metadata class * { *; }
-keepclassmembers class **.WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
-dontwarn  javax.lang.model.element.Modifier

-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

#日志混淆
#-------------- 去掉所有打印 -------------
#-assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** e(...);
#    public static *** i(...);
#    public static *** v(...);
#    public static *** println(...);
#    public static *** w(...);
#    public static *** wtf(...);
#}
-keep class com.adjust.sdk.** { *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }

-keep class com.appsflyer.internal.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class androidx.core.app.ActivityCompat { *; }

# R8 missing classes rules - generated automatically
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn org.tukaani.xz.ARMOptions
-dontwarn org.tukaani.xz.ARMThumbOptions
-dontwarn org.tukaani.xz.DeltaOptions
-dontwarn org.tukaani.xz.FilterOptions
-dontwarn org.tukaani.xz.FinishableOutputStream
-dontwarn org.tukaani.xz.FinishableWrapperOutputStream
-dontwarn org.tukaani.xz.IA64Options
-dontwarn org.tukaani.xz.LZMA2InputStream
-dontwarn org.tukaani.xz.LZMA2Options
-dontwarn org.tukaani.xz.LZMAInputStream
-dontwarn org.tukaani.xz.PowerPCOptions
-dontwarn org.tukaani.xz.SPARCOptions
-dontwarn org.tukaani.xz.SingleXZInputStream
-dontwarn org.tukaani.xz.UnsupportedOptionsException
-dontwarn org.tukaani.xz.X86Options
-dontwarn org.tukaani.xz.XZ
-dontwarn org.tukaani.xz.XZInputStream
-dontwarn org.tukaani.xz.XZOutputStream
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry

# FFmpeg related rules
-keep class com.arthenica.** { *; }
-dontwarn com.arthenica.**
-keep class com.arthenica.mobileffmpeg.** { *; }
-dontwarn com.arthenica.mobileffmpeg.**

# Additional missing classes that might be needed
-dontwarn org.tukaani.xz.**
-keep class org.tukaani.xz.** { *; }

# DOM related
-dontwarn org.w3c.dom.**
-keep class org.w3c.dom.** { *; }

# Java beans
-dontwarn java.beans.**
-keep class java.beans.** { *; }

# Additional rules for release builds
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


-dontobfuscate

-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn cn.thinkingdata.thirdparty.**
-dontwarn cn.thinkingdata.**
-keep class cn.thinkingdata.thirdparty.** { *; }
-keep class cn.thinkingdata.module.routes.** { *; }
-keep class cn.thinkingdata.** { *; }