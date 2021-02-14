pluginManagement {
    repositories {
       gradlePluginPortal()
       google()
    }
}
rootProject.name = "ComposeData"

include(
  "demo",
  "processor",
  "runtime"
)
