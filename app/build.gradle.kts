import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9 内置 Kotlin，无需（也不能）再应用 org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)
}

/**
 * release 签名：优先读 `keystore.properties`，其次读环境变量。
 *
 * 两条路径各有用途：
 *  - 本地：把凭证写进根目录 keystore.properties（已在 .gitignore 中排除）；
 *  - CI：密钥库从 GitHub Secrets 还原成文件，密码直接以环境变量传入，
 *    磁盘上不落明文配置文件。
 *
 * 两者都没有时不做任何签名装配——此时 release 构建会失败并提示缺少签名，
 * 这是期望行为：未签名的 release 包本来就装不上。
 */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/** 读签名项：properties 优先，环境变量兜底；都没有则返回 null。 */
fun signingValue(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStoreFile: File? = (keystoreProps.getProperty("storeFile") ?: System.getenv("RELEASE_STORE_FILE"))
    ?.let { File(it) }
    ?.takeIf { it.isFile }

val releaseStorePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")

/** 四项齐全才认为可以签 release。 */
val hasReleaseSigning: Boolean =
    releaseStoreFile != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null

/** 兼容旧行为：只要存在 keystore.properties 就尝试装配（缺项时给出明确错误）。 */
val hasReleaseKeystore: Boolean = hasReleaseSigning

fun keystoreValue(key: String): String =
    keystoreProps.getProperty(key)
        ?: error("缺少签名配置 $key：请检查根目录 keystore.properties 或对应的 CI 环境变量")

/**
 * debug 签名用的固定 keystore：从 keystore.properties 的 `debugStoreFile` 读，不存在则不装配。
 *
 * 为什么需要固定：CI 每次运行都是全新环境，debug 签名随之改变，
 * 之前装好的 debug 版就再也无法原地升级（INSTALL_FAILED_UPDATE_INCOMPATIBLE）。
 */
val debugKeystoreFile: File? =
    keystoreProps.getProperty("debugStoreFile")?.let { File(it) }?.takeIf { it.isFile }

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
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // v1/v2/v3 全开：老设备与各应用商店对签名方案的识别并不统一，
                // 只开 v2 会让少数老 ROM 报「解析包时出现问题」
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
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
