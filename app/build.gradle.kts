import org.gradle.internal.impldep.bsh.commands.dir
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val prop = Properties()
val configDir = file("src/config/app.properties").inputStream()
prop.load(configDir)


android {
    namespace = "com.app.videobox"
    compileSdk = 34

    defaultConfig {
        applicationId = prop.getProperty("packageName")
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    flavorDimensions.add("config")
    productFlavors {
        create("config") {
            dimension = "config"
        }
    }
}

dependencies {

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.appcompat:appcompat:1.6.1")

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
    // Screen Model
    implementation("cafe.adriel.voyager:voyager-screenmodel:$voyagerVersion")
    // BottomSheetNavigator
    implementation("cafe.adriel.voyager:voyager-bottom-sheet-navigator:$voyagerVersion")
    // TabNavigator
    implementation("cafe.adriel.voyager:voyager-tab-navigator:$voyagerVersion")
    // Transitions
    implementation("cafe.adriel.voyager:voyager-transitions:$voyagerVersion")
    // Koin integration
    implementation("cafe.adriel.voyager:voyager-koin:$voyagerVersion")
    // Android
    // Hilt integration
    implementation("cafe.adriel.voyager:voyager-hilt:$voyagerVersion")
    // LiveData integration
    implementation("cafe.adriel.voyager:voyager-livedata:$voyagerVersion")

}