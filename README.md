# R 中转

把 Android 手机变成私有的 AI 模型聚合服务器：将 Trae / Loomy / WorkBuddy / Antigravity / ChatGPT / 小浣熊 以及任意自定义 OpenAI 兼容上游，
统一翻译为一个 OpenAI 兼容端点（`/v1/chat/completions`、`/v1/models`、`/v1/credits`），供任意客户端调用。

- **包名**：`app.ai.rune.ziyong.transfer`
- **界面**：整套视觉基于 [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix)（MIUI / HyperOS 风格），不使用 Material3 组件

## 与上游的关系

本工程 fork 自 [`xibai6668/Android-AI-gateway`](https://github.com/xibai6668/Android-AI-gateway)，
**仅重构了外观与包名**：

| 部分 | 状态 |
|---|---|
| `:core` 网关内核（协议、供应商适配、存储、容错） | 原样保留，零改动 |
| `:cli` 命令行工具 | 原样保留 |
| `app` 数据层与服务层（加密存储、前台服务、磁贴、保活） | 仅改包名 |
| `app` UI 层（页面、组件、主题） | 以 miuix 重写 |

功能语义与上游一致：登录方式、账号池调度、故障转移、限流脱敏、记录与统计均未改变行为。

## 构建

本项目**不做本地构建**，全部由 GitHub Actions 完成。

### 自动构建

- push 到 `main` → 构建 debug APK 并上传产物
- Pull Request → 只跑 `:core` 单元测试 + 构建 debug APK
- 手动触发（Actions → Build → Run workflow）→ 可选择是否同时跑单元测试

产物在对应 workflow run 页面的 **Artifacts** 区域下载。

### 本地构建（可选）

如需本地出包，需自备 Android SDK（platform 37 与 build-tools 36.0.0）与 JDK 21：

```bash
./gradlew :app:assembleDebug
```

## 版本基线

全部为最新正式版，不含 alpha / beta / rc / SNAPSHOT：

| 组件 | 版本 |
|---|---|
| Android Gradle Plugin | 9.5.1 |
| Kotlin | 2.4.21 |
| Gradle | 9.9.0 |
| Compose BOM | 2026.09.00 |
| miuix | 0.9.4 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |

> AGP 9 起 Kotlin 编译内置于 AGP，因此工程**不再应用** `org.jetbrains.kotlin.android` 插件；
> Kotlin 版本通过根 `build.gradle.kts` 的 `buildscript` classpath 提升到 2.4.21（AGP 自带 2.2.10）。

## 签名与发版

release 包需要一份签名密钥库。工程已把密钥库与密码写入**仓库 Secrets**（仅 CI 可见）：

| Secret | 作用 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | 密钥库文件的 base64 内容 |
| `RELEASE_STORE_PASSWORD` | 密钥库口令 |
| `RELEASE_KEY_ALIAS` | 密钥别名 |
| `RELEASE_KEY_PASSWORD` | 密钥口令 |

CI 只在构建时把密钥库还原到临时目录（`$RUNNER_TEMP`），密码通过环境变量传入，
**仓库里不存在明文凭证**，构建结束后临时文件随 runner 销毁。

推送 `main` 即可产出 release 包；缺少上述 Secrets 时 workflow 会跳过 release 并给出 warning。

本地出 release 包时，在根目录创建 `keystore.properties`（已被 `.gitignore` 排除）：

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

> ⚠️ 这份密钥库是自签名调试用证书，**不能用于上架应用商店**（商店要求正式发行证书），
> 也请勿把它的口令复用到其它项目。若要换用正式证书，替换上述四个 Secret 即可，
> 代码与 workflow 无需改动。

## 许可

跟随上游，见 [LICENSE](LICENSE) 与 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。
