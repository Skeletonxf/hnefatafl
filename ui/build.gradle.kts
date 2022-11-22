import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

val jvmVersion = 19

val properties = Properties().apply {
    load(rootProject.file("local.properties").reader())
}
val jextractPath = properties["jextract-path"]

val generateBindings = tasks.register("generateBindings", Exec::class) {
    workingDir = File("${project.projectDir}")
    // The ui folder is inside the root folder, and the root folder is from where we need to reach
    // the rust code
    val sourceRoot = project.projectDir.parent
    if (jextractPath == null || jextractPath == "") {
        throw IllegalArgumentException("path to JExtract executable must be specified in local.properties")
    }
    commandLine = listOf(
        jextractPath as String,
        "--source",
        "-t",
        "io.github.skeletonxf.bindings",
        "--output",
        "src/main/kotlin/",
        "$sourceRoot/bindings.h",
        "-l",
        "$sourceRoot/target/debug/libhnefatafl.so", // FIXME: This won't work on Windows
    ).also { println("Building bindings: ${it.joinToString(separator = " ")}") }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    // 19 doesn't seem to be recognised yet?
    // kotlinOptions {
    //     jvmTarget = jvmVersion.toString()
    // }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(jvmVersion))
}

tasks.withType(JavaCompile::class).all {
    options.compilerArgs.add("--enable-preview")
    dependsOn.add(generateBindings)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs.addAll(listOf(
            //"--add-modules",
            //"jdk.incubator.foreign",
            "--enable-native-access=ALL-UNNAMED",
            "--enable-preview"
        ))

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
