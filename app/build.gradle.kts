// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "XiYue.SiyoX"
    compileSdk = 37

    defaultConfig {
        applicationId = "XiYue.SiyoX"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("SiyoX.jks")
            storePassword = "SiyoX"
            keyAlias = "SiyoX"
            keyPassword = "SiyoX"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/**/LICENSE*",
                "/META-INF/**/NOTICE*",
                "/META-INF/**/license*",
                "/META-INF/**/notice*",
                "/META-INF/**/*.properties",
                "/META-INF/*.kotlin_module",
                "META-INF/*.kotlin_module",
                "/kotlin/**",
                "kotlin/**",
                "**/*.kotlin_builtins",
                "**/*.kotlin_metadata",
                "DebugProbesKt.bin",
                "/DebugProbesKt.bin",
                "**/*.bin",
                "/assets/dexopt/**",
                "assets/dexopt/**",
                "**/*.prof",
                "**/*.profm"
            )
        }
    }
}

// 彻底禁用 ART Profile / Baseline Profile 生成，移除 assets/dexopt
tasks.matching { it.name.contains("ArtProfile") || it.name.contains("BaselineProfile") }.configureEach {
    enabled = false
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Xposed API (provided at runtime by LSPosed/Xposed framework)
    compileOnly("de.robv.android.xposed:api:82")
}
