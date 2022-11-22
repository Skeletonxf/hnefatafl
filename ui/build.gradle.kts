import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
    // UniFFI dependency
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna
    implementation("net.java.dev.jna:jna:5.12.1")
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

val jvmVersion = 17

val ffiBindings = tasks.register("generateUniFFIBindings", Exec::class) {
    workingDir = File("${project.projectDir}")
    commandLine = listOf(
        "uniffi-bindgen",
        "generate",
        "../src/hnefatafl.udl",
        "--language",
        "kotlin",
        "--out-dir",
        "src/main/kotlin/"
    )
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
     kotlinOptions {
         jvmTarget = jvmVersion.toString()
     }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(jvmVersion))
}

tasks.withType(JavaCompile::class).all {
    //options.compilerArgs.add("--enable-preview")
    dependsOn.add(ffiBindings)
}

// If the bindings are generated in the build dir, they can't be accessed by kotlin source code?
//sourceSets {
//    main {
//        kotlin {
//            include("${buildDir}/generated/source/bindings")
//        }
//    }
//}

compose.desktop {
    application {
        mainClass = "MainKt"
//        jvmArgs.addAll(listOf(
//            //"--add-modules",
//            //"jdk.incubator.foreign",
//            "--enable-native-access=ALL-UNNAMED",
//            "--enable-preview"
//        ))

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinJvmComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
