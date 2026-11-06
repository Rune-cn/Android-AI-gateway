package app.ai.rune.ziyong.transfer.ui

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/**
 * 剪贴板写入的统一入口。
 *
 * 为什么需要这一层：Compose 1.9 起 `LocalClipboardManager` 已废弃，
 * 新 API [LocalClipboard] 的 `setClipEntry` 是 **suspend** 函数，
 * 必须挂在协程作用域里调用。如果每个页面各自 `rememberCoroutineScope()` +
 * `launch { ... }`，会把「复制一次文本」这种琐事写成三行样板，且容易漏掉异常处理。
 *
 * 用法：
 * ```
 * val copy = rememberTextCopier()
 * ...
 * onClick = { copy("text") }
 * ```
 */
@Composable
fun rememberTextCopier(onCopied: (() -> Unit)? = null): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope, onCopied) {
        { text ->
            scope.launch {
                // 无标签不足以让系统识别内容类型，这里用通用纯文本标签；
                // 个别 ROM 的剪贴板服务可能抛异常（如被安全软件拦截），不应让界面崩溃。
                runCatching {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
                }
                onCopied?.invoke()
            }
        }
    }
}
