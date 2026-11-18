pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "BasaltCore"

include("api", "core", "nms", "plugin", "v1_21_11", "v26_2")
