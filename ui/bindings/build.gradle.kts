import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.Variant
import gobley.gradle.cargo.dsl.android
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.gobleyCargo)
    alias(libs.plugins.gobleyUniffi)
    kotlin("plugin.atomicfu") version libs.versions.kotlin
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()
}

android {
    namespace = "uniffi.hnefatafl"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

cargo {
    builds.jvm {
        // Build Rust library only for the host platform
        embedRustLibrary = (GobleyHost.current.rustTarget == rustTarget)
        jvmVariant = Variant.Release
    }
    builds.android {
        // any config here?
    }
}
