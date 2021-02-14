plugins {
  kotlin("jvm")
}

group = "com.zachklipp"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  google()
}

dependencies {
  implementation("com.google.devtools.ksp:symbol-processing-api:1.4.30-1.0.0-alpha02")
  implementation("com.squareup.okio:okio:2.10.0")
  implementation("com.squareup:kotlinpoet:1.7.2")

  // Just need these to reflect on some names.
  implementation(project(":runtime"))
  // compileOnly("androidx.compose.foundation:foundation:1.0.0-alpha12") {
  //   this.artifact {
  //     this.extension="aar"
  //   }
  // }
}
