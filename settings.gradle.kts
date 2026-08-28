// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("UnstableApiUsage")

rootProject.name = "SiyoX"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("C:/Users/Administrator/Desktop/组件库/MiuiX/build-plugins")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://api.xposed.info/") }
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven { url = uri("https://api.xposed.info/") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app")
include(":baselineprofile")
project(":baselineprofile").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/baselineprofile")
include(":miuix-core")
project(":miuix-core").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-core")
include(":miuix-ui")
project(":miuix-ui").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-ui")
include(":miuix-preference")
project(":miuix-preference").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-preference")
include(":miuix-icons")
project(":miuix-icons").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-icons")
include(":miuix-blur")
project(":miuix-blur").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-blur")
include(":miuix-squircle")
project(":miuix-squircle").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-squircle")
include(":miuix-shader")
project(":miuix-shader").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-shader")
include(":miuix-navigation3-ui")
project(":miuix-navigation3-ui").projectDir = file("C:/Users/Administrator/Desktop/组件库/MiuiX/miuix-navigation3-ui")
