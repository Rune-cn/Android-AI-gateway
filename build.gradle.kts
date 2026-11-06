// AGP 9 内置 Kotlin：自带 KGP 2.2.10，这里把它提升到 2.4.21。
// 必须在 plugins 块之外声明，内置 Kotlin 才会用上该版本。
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
