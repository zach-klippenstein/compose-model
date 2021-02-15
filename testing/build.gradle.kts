plugins {
    kotlin("jvm")
}

group = "com.zachklipp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // TODO make multiplatform
    // api("androidx.compose.runtime:runtime:1.0.0-alpha12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}
