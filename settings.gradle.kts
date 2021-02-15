pluginManagement {
    repositories {
       gradlePluginPortal()
       google()
    }
}
rootProject.name = "ComposeModel"

include(
  "demo",
  "processor",
  "runtime"
)
