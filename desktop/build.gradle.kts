plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
}

compose.desktop {
    application {
        mainClass = "com.storagemaid.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            packageName = "BytePurge"
            packageVersion = "1.0.0"
            description = "BytePurge - Offline Storage Cleaner & Shader Hunter"
            copyright = "© 2026 COSMOMIME"
            vendor = "COSMOMIME"
        }
    }
}

if (file("src/main/kotlin/com/storagemaid/desktop/DesktopSnapshotRunner.kt").exists()) {
    tasks.register<JavaExec>("runSnapshot") {
        mainClass.set("com.storagemaid.desktop.DesktopSnapshotRunner")
        classpath = sourceSets["main"].runtimeClasspath
    }
}
