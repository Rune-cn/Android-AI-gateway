package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 发现新版本时的更新弹窗。
 *
 * 状态机完全由 [UpdateDownload] 驱动，界面上「主按钮」的文案与动作一一对应：
 * - 未开始 / 失败 → 「应用内更新」或「前往下载」
 * - 下载中 → 「取消下载」
 * - 下载完成 → 「安装」（当前包装不了 APK 时为「到仓库下载」）
 *
 * 「稍后」只在本次会话内生效：关掉后仍可用「我的 → 检查更新」重新拉出弹窗。
 */
@Composable
fun UpdatePromptDialog(state: AppUiState, viewModel: AppViewModel) {
    var dismissed by remember(state.updateInfo?.version) { mutableStateOf(false) }
    val info = state.updateInfo?.takeIf { !dismissed } ?: return

    val download = state.updateDownload
    val updateBusy = download is UpdateDownload.Probing || download is UpdateDownload.Downloading
    // 当前安装包能否自己装 APK（旧版本缺 REQUEST_INSTALL_PACKAGES，必然装不了）
    val canSelfInstall = remember { viewModel.canInstallUpdate() }

    WindowDialog(
        show = true,
        title = "发现新版本 ${info.version}",
        onDismissRequest = { if (!updateBusy) dismissed = true },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!canSelfInstall) {
                // 旧版本缺少 REQUEST_INSTALL_PACKAGES，应用内安装必然失败
                Text(
                    text = "当前版本不支持应用内安装，请到项目仓库下载完整包更新。",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                )
            }

            Text(
                text = "更新日志",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            if (info.notes.isEmpty()) {
                Text(
                    text = "该版本没有提供更新说明。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                Text(
                    text = info.notes,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }

            // 应用内更新的进度就摆在更新日志下方，不另开页面
            when (download) {
                is UpdateDownload.Probing -> Text(
                    text = "正在测速直连与国内镜像…",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                is UpdateDownload.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${download.source} · ${formatBytes(download.downloaded)} / ${formatBytes(download.total)}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    LinearProgressIndicator(
                        progress = if (download.total <= 0) {
                            0f
                        } else {
                            (download.downloaded.toFloat() / download.total).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${formatSpeed(download.bytesPerSecond)} · ${download.threads} 线程并发",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                is UpdateDownload.Ready -> Text(
                    text = if (canSelfInstall) {
                        "安装包已就绪（${download.source}），点「安装」完成更新。"
                    } else {
                        "本版本无法应用内安装，请点下方「到仓库下载」手动更新。"
                    },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                is UpdateDownload.Failed -> Text(
                    text = "下载失败：${download.reason}",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.error,
                )

                null -> Unit
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = "稍后",
                    onClick = {
                        if (updateBusy) viewModel.cancelUpdateDownload()
                        dismissed = true
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = when (download) {
                        is UpdateDownload.Ready -> if (canSelfInstall) "安装" else "到仓库下载"
                        is UpdateDownload.Failed, null -> if (info.apkUrl == null) "前往下载" else "应用内更新"
                        else -> "取消下载"
                    },
                    onClick = {
                        when (download) {
                            is UpdateDownload.Ready ->
                                // 自身包装不了时系统安装器根本不会被唤起，直接给用户可行的出路
                                if (canSelfInstall) viewModel.installUpdate() else viewModel.openReleasePage(info.url)

                            is UpdateDownload.Failed, null ->
                                if (info.apkUrl == null) {
                                    viewModel.openReleasePage(info.url)
                                } else {
                                    viewModel.downloadUpdate(info)
                                }

                            else -> viewModel.cancelUpdateDownload()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
