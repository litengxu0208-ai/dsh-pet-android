plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dshpet.android"
    compileSdk = 34

    defaultConfig {
        // 与上游 com.dshpet.android 区分开，两个版本可以同时装在一个手机上
        applicationId = "com.dshpet.android.affinity"
        minSdk = 28
        targetSdk = 34
        versionCode = 200
        versionName = "2.0.0-bond"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            // 用 debug 签名，保证任何机器上都能直接安装
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        // 动画 WebP 已经压过，构建时不要再二次处理
        noCompress += listOf("webp")
    }
}

dependencies {
    // 刻意零第三方依赖：只用 Android 框架 API（minSdk 28 足够）
}
