plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.leonardo.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.leonardo.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 使用括号 () 和双引号 ""
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.6")

// 网络请求 - Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// 图片加载 - Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
// 注意：Kotlin DSL 中通常使用 kapt 或 ksp，而不是 annotationProcessor，
// 如果必须用 annotationProcessor，写法如下：
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

// 视频播放 - ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

}
