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
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

val jvmVersion = 19

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
