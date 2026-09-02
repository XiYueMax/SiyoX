

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
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += setOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = null
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ""
            signingConfig = null
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

tasks.matching { it.name.contains("ArtProfile") || it.name.contains("BaselineProfile") }.configureEach {
    enabled = false
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

compileOnly("de.robv.android.xposed:api:82")
}
