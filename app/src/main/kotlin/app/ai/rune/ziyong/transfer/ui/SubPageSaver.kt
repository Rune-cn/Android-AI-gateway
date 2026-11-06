package app.ai.rune.ziyong.transfer.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * [SubPage] 的实例状态保存器。
 *
 * 为什么不直接依赖 Parcelable：`SubPage` 是本工程内部的密封接口，
 * 让它实现 Parcelable 会把「可序列化」这个存储细节渗进 UI 模型，
 * 且新增页面时必须同时维护一个 Parceler。
 *
 * 这里用「类型标签 + 可选业务键」的扁平结构保存，只依赖两个原始类型，
 * 进程被杀后能精确还原到同一页面。
 */
val SubPageSaver: Saver<SubPage?, Any> = listSaver(
    save = { page ->
        when (page) {
            null -> listOf(TAG_NONE)
            is SubPage.ProviderDetail -> listOf(TAG_PROVIDER_DETAIL, page.providerId)
            is SubPage.CustomProviderEdit -> listOf(TAG_CUSTOM_EDIT, page.key ?: KEY_NULL)
            SubPage.Usage -> listOf(TAG_USAGE)
            SubPage.TaskCenter -> listOf(TAG_TASK_CENTER)
            SubPage.CreditCenter -> listOf(TAG_CREDIT_CENTER)
            is SubPage.ProviderCredits -> listOf(TAG_PROVIDER_CREDITS, page.providerId)
            SubPage.KeepAlive -> listOf(TAG_KEEP_ALIVE)
            SubPage.Proxy -> listOf(TAG_PROXY)
            SubPage.ApiSettings -> listOf(TAG_API_SETTINGS)
            SubPage.SecurityProtection -> listOf(TAG_SECURITY)
            SubPage.Resilience -> listOf(TAG_RESILIENCE)
            SubPage.AppearanceTheme -> listOf(TAG_APPEARANCE)
            SubPage.AccountTransfer -> listOf(TAG_ACCOUNT_TRANSFER)
        }
    },
    restore = { saved ->
        val key = saved.getOrNull(1)?.takeUnless { it == KEY_NULL }
        when (saved.firstOrNull()) {
            TAG_PROVIDER_DETAIL -> key?.let { SubPage.ProviderDetail(it) }
            TAG_CUSTOM_EDIT -> SubPage.CustomProviderEdit(key)
            TAG_USAGE -> SubPage.Usage
            TAG_TASK_CENTER -> SubPage.TaskCenter
            TAG_CREDIT_CENTER -> SubPage.CreditCenter
            TAG_PROVIDER_CREDITS -> key?.let { SubPage.ProviderCredits(it) }
            TAG_KEEP_ALIVE -> SubPage.KeepAlive
            TAG_PROXY -> SubPage.Proxy
            TAG_API_SETTINGS -> SubPage.ApiSettings
            TAG_SECURITY -> SubPage.SecurityProtection
            TAG_RESILIENCE -> SubPage.Resilience
            TAG_APPEARANCE -> SubPage.AppearanceTheme
            TAG_ACCOUNT_TRANSFER -> SubPage.AccountTransfer
            // 未知标签（例如降级安装）时回到主页面，而不是崩溃。
            else -> null
        }
    },
)

/** 让 listSaver 的 `Any` 元素类型与 Compose 的 Saver 契约对齐。 */
@Suppress("unused")
private fun Saver<SubPage?, Any>.asNullable(): Saver<SubPage?, Any> = this

private const val TAG_NONE = "none"
private const val TAG_PROVIDER_DETAIL = "providerDetail"
private const val TAG_CUSTOM_EDIT = "customEdit"
private const val TAG_USAGE = "usage"
private const val TAG_TASK_CENTER = "taskCenter"
private const val TAG_CREDIT_CENTER = "creditCenter"
private const val TAG_PROVIDER_CREDITS = "providerCredits"
private const val TAG_KEEP_ALIVE = "keepAlive"
private const val TAG_PROXY = "proxy"
private const val TAG_API_SETTINGS = "apiSettings"
private const val TAG_SECURITY = "security"
private const val TAG_RESILIENCE = "resilience"
private const val TAG_APPEARANCE = "appearance"
private const val TAG_ACCOUNT_TRANSFER = "accountTransfer"

/** `SubPage.CustomProviderEdit.key` 为 null（新建）时的占位符——Bundle 里不能存 null 值。 */
private const val KEY_NULL = "\u0000null"
