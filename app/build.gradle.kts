import org.gradle.internal.impldep.bsh.commands.dir
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val prop = Properties()
val configDir = file("src/config/app.properties").inputStream()
prop.load(configDir)


android {
    namespace = "com.app.videobox"
    compileSdk = 35

    defaultConfig {
        applicationId = prop.getProperty("packageName")
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String","privacyUrl","\"${prop.getProperty("privacyUrl")}\"")
        buildConfigField("String","termUrl","\"${prop.getProperty("termUrl")}\"")
        buildConfigField("String","afKey","\"${prop.getProperty("afKey")}\"")

        resValue("string", "adMobId", prop.getProperty("admobId"))
        resValue("string", "facebookId", prop.getProperty("facebookId"))
        resValue("string", "facebookToken", prop.getProperty("facebookToken"))
    }
    packaging {
        jniLibs {
            pickFirsts += listOf("**/libc++_shared.so")
            jniLibs.useLegacyPackaging = true
        }
    }
    signingConfigs {
        create("release"){
            storeFile = file("src/config/${prop["storeFile"]}")
            storePassword =  prop.getProperty("storePassword")
            keyAlias = prop.getProperty("keyAlias")
            keyPassword = prop.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    flavorDimensions.add("config")
    productFlavors {
        create("config") {
            dimension = "config"
        }
    }

}

dependencies {
    // 添加video-downloader-module模块依赖
    implementation(project(":video-downloader-module"))
    
    // VLC Player support
    implementation("org.videolan.android:libvlc-all:4.0.0-eap20")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.firebase.messaging.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    //Koin做依赖注入
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    //图标库
    implementation(libs.androidx.compose.material.iconsExtended)

    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-video:2.4.0")

    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation("com.blankj:utilcodex:1.31.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.5.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    implementation("com.github.CarGuo.GSYVideoPlayer:gsyvideoplayer:v10.0.0")
    implementation("com.github.getActivity:XXPermissions:20.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-config")

    val voyagerVersion = "1.1.0-beta02"
    // Navigator
    implementation("cafe.adriel.voyager:voyager-navigator:$voyagerVersion")

    implementation("com.facebook.android:facebook-android-sdk:12.0.1")

    implementation("com.google.android.gms:play-services-ads:23.2.0")
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")
    implementation("com.android.installreferrer:installreferrer:2.2")

    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    implementation("com.appsflyer:af-android-sdk:6.16.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    //ffmpeg
    implementation("com.arthenica:mobile-ffmpeg-full-gpl:4.4.LTS")
    // Paging3 依赖
    implementation("androidx.paging:paging-runtime:3.3.6")
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")
    implementation("androidx.paging:paging-compose:3.3.6")
    //af
    implementation("com.appsflyer:af-android-sdk:6.17.0")
    // 数数分析平台
    implementation("cn.thinkingdata.android:ThinkingAnalyticsSDK:3.0.1")
    implementation("cn.thinkingdata.android:TAThirdParty:2.0.0")
}