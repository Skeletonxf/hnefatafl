import java.util.Properties
import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.Variant

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.gobleyCargo)
    alias(libs.plugins.gobleyUniffi)
    kotlin("plugin.atomicfu") version libs.versions.kotlin
}

val properties = Properties().apply {
    load(rootProject.file("local.properties").reader())
}
val jextractPath = properties["jextract-path"]

val compileRust = tasks.register("compileRust", Exec::class) {
    // The ui folder is inside the root folder, and the root folder is from where we need to reach
    // the rust code
    val sourceRoot = project.projectDir.parent
    workingDir = File(sourceRoot)
    commandLine = listOf(
        "cargo",
        "build",
        "--release"
    )
}

val generateBindings = tasks.register("generateBindings", Exec::class) {
    workingDir = File("${project.projectDir}")
    // The ui folder is inside the root folder, and the root folder is from where we need to reach
    // the rust code. Our multiplatform project is also nested in the root project, so we need
    // to step up 2 levels to reach the actual root of the git repository.
    val sourceRoot = project.projectDir.parentFile.parent
    if (jextractPath == null || jextractPath == "") {
        throw IllegalArgumentException("path to JExtract executable must be specified in local.properties")
    }
    // Always depend on release binary even when app built in debug mode due to speed issues
    // when depth is non-trivial
    commandLine = listOf(
        jextractPath as String,
        "--source",
        "-t",
        "io.github.skeletonxf.bindings",
        "--output",
        "src/main/java/",
        "$sourceRoot/bindings.h",
        "-l",
        "$sourceRoot/target/release/libhnefatafl.so", // FIXME: This won't work on Windows
    ).also { println("Building bindings: ${it.joinToString(separator = " ")}") }

    dependsOn.add(compileRust)
}

tasks.withType(JavaCompile::class).all {
    options.compilerArgs.addAll(listOf("--enable-preview"))
    dependsOn.add(generateBindings)
}

dependencies {

}

cargo {
    builds.jvm {
        // Build Rust library only for the host platform
        embedRustLibrary = (GobleyHost.current.rustTarget == rustTarget)
    }
}

//uniffi {
//    generateFromLibrary {
//        variant = Variant.Release
//    }
//}