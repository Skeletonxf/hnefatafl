import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.kotlin.dsl.creating
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.cashappLicensee)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()

    // FIXME: Remove default hierarchy template
    sourceSets {
        val commonJvm by creating {
            dependsOn(commonMain.get())
        }
        val commonJvmTest by creating {
            dependsOn(commonJvm)
            dependsOn(commonTest.get())
        }
        androidMain {
            dependsOn(commonJvm)
        }
        jvmMain {
            dependsOn(commonJvm)
        }
        commonJvm.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.componentsResources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":bindings"))
            // Will also need to credit Google Fonts when adding licences viewer.
            // The close and restart assets are under Apache-2
        }
        commonJvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "io.github.skeletonxf"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.skeletonxf.hnefatafl"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "io.github.skeletonxf.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.skeletonxf"
            packageVersion = "1.0.0"
        }

        jvmArgs.addAll(listOf("--enable-preview"))
    }
}

compose.resources {
    packageOfResClass = "io.github.skeletonxf.ui"
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
}

licensee {
    // The text of licenses like MIT typically vary per project due to the author
    // and the ecosystem for Gradle seems particularly lacking in plugins that
    // can bundle together the project's license text. Apache 2.0 however is very
    // standard and fortunately has no such project specific author line. Hence,
    // MIT licenses in our Rust dependencies are fine because we will bundle their
    // actual text at compile time, but for gradle libraries we only accept Apache 2.0
    // so we can get by with the limitations of licensee and alternatives.
    allow("Apache-2.0")
}

// Make sure Licensee runs when building so artifacts are up to date.
// We want each build variant to put the artifacts.json into the appropriate
// variant/composeResources/files directory so we'll be able to read this
// via Res.readBytes("files/artifacts.json") at runtime on each platform.
val copyArtifactsJvm = tasks.register<Copy>("copyLicenseeArtifactsJvm") {
    from(project.layout.buildDirectory.dir("reports/licensee/jvm"))
    include("artifacts.json")
    into("src/jvmMain/composeResources/files")
    dependsOn(tasks.named("licenseeJvm"))
}
tasks.named("copyNonXmlValueResourcesForJvmMain").dependsOn(copyArtifactsJvm)

val copyArtifactsAndroidDebug = tasks.register<Copy>("copyLicenseeArtifactsAndroidDebug") {
    from(project.layout.buildDirectory.dir("reports/licensee/androidDebug"))
    include("artifacts.json")
    into("src/androidMainDebug/composeResources/files")
    dependsOn(tasks.named("licenseeAndroidDebug"))
}
afterEvaluate {
    tasks.named("copyNonXmlValueResourcesForAndroidDebug").dependsOn(copyArtifactsAndroidDebug)
}

val copyArtifactsAndroidRelease = tasks.register<Copy>("copyLicenseeArtifactsAndroidRelease") {
    from(project.layout.buildDirectory.dir("reports/licensee/androidRelease"))
    include("artifacts.json")
    into("src/androidMainRelease/composeResources/files")
    dependsOn(tasks.named("licenseeAndroidRelease"))
}
afterEvaluate {
    tasks.named("copyNonXmlValueResourcesForAndroidRelease").dependsOn(copyArtifactsAndroidRelease)
}
