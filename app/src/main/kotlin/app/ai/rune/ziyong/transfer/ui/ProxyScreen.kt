package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aigw.core.gateway.ProxyType
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 代理设置：境外供应商（如 Antigravity）需要走代理。
 *
 * 打开总开关后默认所有上游都走代理，个别不需要的可以在下面的列表里单独关掉。
 * 列表用「走代理」的正向语义而不是「排除」：用户想看的是「谁在走代理」。
 */
@Composable
fun ProxyScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    val settings = state.proxy
    var host by remember(settings.host) { mutableStateOf(settings.host) }
    var port by remember(settings.port) { mutableStateOf(settings.port.toString()) }
    var username by remember(settings.username) { mutableStateOf(settings.username) }
    var password by remember(settings.password) { mutableStateOf(settings.password) }
    var excludedText by remember(settings.excludedHosts) {
        mutableStateOf(settings.excludedHosts.joinToString(","))
    }
    var showTypeDialog by remember { mutableStateOf(false) }

    PageScaffold(
        title = "代理设置",
        subtitle = if (settings.usable) "已启用 ${settings.host}:${settings.port}" else "未启用",
        onBack = onBack,
    ) {
        SectionCard {
            SwitchRow(
                title = "启用代理",
                subtitle = "关闭后所有上游直连",
                checked = settings.enabled,
                onCheckedChange = { viewModel.updateProxySettings(settings.copy(enabled = it)) },
            )

            TextDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTypeDialog = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "代理类型",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = settings.type.label,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Icon(
                    imageVector = MiuixIcons.Regular.ChevronForward,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }

            TextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = "代理地址",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                label = "端口",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = username,
                onValueChange = { username = it },
                label = "用户名（可选）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = "密码（可选）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = excludedText,
                onValueChange = { excludedText = it },
                label = "不走代理的地址",
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "逗号分隔；支持通配符 *、host:port、CIDR 网段",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            Button(
                onClick = {
                    val parsed = port.toIntOrNull()
                    if (parsed == null || parsed !in 1..65535) {
                        viewModel.notice("端口需要在 1~65535 之间")
                    } else {
                        // 逗号（中英文）、空白、换行都视作分隔符：用户从别处粘贴的列表往往混用
                        val excludedHosts = excludedText
                            .split(Regex("[,，\\n\\s]+"))
                            .filter { it.isNotEmpty() }
                            .distinct()
                        viewModel.updateProxySettings(
                            settings.copy(
                                host = host.trim(),
                                port = parsed,
                                username = username.trim(),
                                password = password,
                                type = settings.type,
                                excludedHosts = excludedHosts,
                            ),
                        )
                        viewModel.notice("代理设置已保存")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("保存")
            }
        }

        SectionCard {
            SectionLabel("走代理的供应商")
            if (state.providers.isEmpty()) {
                EmptyHint("还没有供应商", "内置供应商会在启动时自动登记。")
            } else {
                for (info in state.providers) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = info.displayName,
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = info.id,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Switch(
                            checked = info.id !in settings.excludedProviderIds,
                            onCheckedChange = { checked ->
                                val excluded = if (checked) {
                                    settings.excludedProviderIds - info.id
                                } else {
                                    settings.excludedProviderIds + info.id
                                }
                                viewModel.updateProxySettings(settings.copy(excludedProviderIds = excluded))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showTypeDialog) {
        WindowDialog(
            show = true,
            title = "代理类型",
            onDismissRequest = { showTypeDialog = false },
        ) {
            Column(Modifier.fillMaxWidth()) {
                for (type in ProxyType.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showTypeDialog = false
                                viewModel.updateProxySettings(settings.copy(type = type))
                                viewModel.notice("代理设置已保存")
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = type.label,
                            style = MiuixTheme.textStyles.body1,
                            color = if (type == settings.type) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (type == settings.type) {
                            Text(
                                text = "当前",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                TextButton(
                    text = "取消",
                    onClick = { showTypeDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
