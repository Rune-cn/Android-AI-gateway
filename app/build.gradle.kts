import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 内置 Kotlin，无需（也不能）再应用 org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)
}

/**
 * release 签名：从 keystore.properties 读，仅在文件存在时装配。
 *
 * 本地开发只出 debug 包，不需要 keystore.properties；云端发版时才由 CI 现生成该文件。
 * 文件缺失时不在配置阶段报错，但 release 构建会因无签名配置而失败——正是期望行为。
 */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val hasReleaseKeystore = rootProject.file("keystore.properties").exists()

/**
 * debug 签名用的固定 keystore：从 keystore.properties 的 `debugStoreFile` 读，不存在则不装配。
 *
 * 为什么需要固定：CI 每次运行都是全新环境，debug 签名随之改变，
 * 之前装好的 debug 版就再也无法原地升级（INSTALL_FAILED_UPDATE_INCOMPATIBLE）。
 */
val debugKeystoreFile: File? =
    keystoreProps.getProperty("debugStoreFile")?.let { File(it) }?.takeIf { it.isFile }

fun keystoreValue(key: String): String =
    keystoreProps.getProperty(key)
        ?: error("缺少签名配置 $key：请检查根目录 keystore.properties")

android {
    namespace = "app.ai.rune.ziyong.transfer"
    // AGP 9 支持 minor 版本号：platform 包名为 platforms;android-37.x
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "app.ai.rune.ziyong.transfer"
        minSdk = 26
        targetSdk = 37
        versionCode = 201
        versionName = "2.25.2"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreValue("storeFile"))
                storePassword = keystoreValue("storePassword")
                keyAlias = keystoreValue("keyAlias")
                keyPassword = keystoreValue("keyPassword")
            }
        }
        if (debugKeystoreFile != null) {
            create("debugStable") {
                storeFile = debugKeystoreFile
                storePassword = keystoreValue("debugStorePassword")
                keyAlias = keystoreValue("debugKeyAlias")
                keyPassword = keystoreValue("debugKeyPassword")
            }
        }
    }

    buildTypes {
        // 两个变体都开 R8。AGP 对 debuggable 构建会禁用优化与混淆，
        // 但代码裁剪照常生效——这才是体积的大头（未被引用的图标类会被裁掉）。
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (debugKeystoreFile != null) signingConfig = signingConfigs.getByName("debugStable")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
        )
    }
}

dependencies {
    implementation(project(":core"))

    // :core 用 implementation 引 gson，不传递；app 里要直接构造 JsonObject（如动作 payload）
    implementation(libs.gson)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    // 登录走 Custom Tab：裸 ACTION_VIEW 会让进程被系统判 idle 冻结，本地回调端口随之失聪
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.security.crypto)

    // miuix：本工程的整套视觉层
    implementation(libs.miuix.core)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.androidx.compose.material.icons)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
