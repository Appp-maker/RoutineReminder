@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Optional: JitPack if you use other libraries from GitHub
        maven { url = uri("https://jitpack.io") }
        // ✅ Add official MapLibre repository
        maven { url = uri("https://api.maplibre.org/maven/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ MapLibre Maven repository (required for maplibre.android)
        maven { url = uri("https://api.maplibre.org/maven/") }
    }
}

rootProject.name = "RoutineReminder"
include(":app")
