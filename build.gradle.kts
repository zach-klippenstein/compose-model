import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.4.30" apply false
  kotlin("android") version "1.4.30" apply false
  id("com.google.devtools.ksp") version "1.4.30-1.0.0-alpha02" apply false
}

buildscript {
  dependencies {
    classpath(kotlin("gradle-plugin", version = "1.4.30"))
    classpath("com.android.tools.build:gradle:7.0.0-alpha06")
  }
}

group = "com.zachklipp"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  gradlePluginPortal()
  google()
}

subprojects {
  repositories {
    mavenCentral()
    google()
    jcenter()
  }

  tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
  }
}