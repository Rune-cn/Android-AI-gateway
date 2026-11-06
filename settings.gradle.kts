pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "R-Transfer"

// :core 为与 UI 无关的网关内核（协议、供应商适配、存储、容错），保持纯 JVM：
// 既能直接跑单元测试，也能原样打进 Android 应用。
include(":core")
include(":cli")
include(":app")
