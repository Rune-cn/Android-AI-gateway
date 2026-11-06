package app.ai.rune.ziyong.transfer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

/**
 * 接入设置：客户端要用的地址、端口与网关鉴权 Key。
 *
 * 端口与 Key 都采用「编辑后点保存」而不是即时生效：改端口会重启监听，
 * 逐字符保存会让服务在用户还在输入时反复重启。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiSettingsScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    val settings = state.settings
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var port by remember(settings.port) { mutableStateOf(settings.port.toString()) }
    var apiKey by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }

    PageScaffold(
        title = "接入设置",
        subtitle = "端口与 API Key",
        onBack = onBack,
    ) {
        SectionCard {
            SectionLabel("接入地址")
            AddressTile(state.localUrl.ifEmpty { "http://127.0.0.1:${settings.port}/v1" }) {
                clipboard.setText(AnnotatedString(state.localUrl))
                Toast.makeText(context, "已复制本机地址", Toast.LENGTH_SHORT).show()
            }
            state.lanUrls.forEach { url ->
                AddressTile(url) {
                    clipboard.setText(AnnotatedString(url))
                    Toast.makeText(context, "已复制局域网地址", Toast.LENGTH_SHORT).show()
                }
            }
        }

        SectionCard {
            TextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                label = "监听端口",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(
                title = "同时监听局域网",
                subtitle = "关闭后仅本机 127.0.0.1 可访问",
                checked = settings.exposeLan,
                onCheckedChange = { viewModel.updateSettings(settings.copy(exposeLan = it)) },
            )
        }

        SectionCard {
            TextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = "网关鉴权 Key（留空表示不校验）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(
                title = "允许无 Key 调用",
                subtitle = "关闭后客户端必须携带正确 Key",
                checked = settings.allowNoKey,
                onCheckedChange = { viewModel.updateSettings(settings.copy(allowNoKey = it)) },
            )
        }

        SectionCard {
            SectionLabel("默认供应商")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.providers.forEach { provider ->
                    Chip(
                        text = provider.displayName,
                        selected = settings.defaultProvider == provider.id,
                        onClick = {
                            viewModel.updateSettings(settings.copy(defaultProvider = provider.id))
                        },
                    )
                }
            }
        }

        Button(
            onClick = {
                val parsed = port.toIntOrNull()
                if (parsed == null || parsed !in 1024..65535) {
                    viewModel.notice("端口需要在 1024~65535 之间")
                } else {
                    viewModel.updateSettings(settings.copy(port = parsed, apiKey = apiKey.trim()))
                    viewModel.notice("已保存；端口改动需重启服务生效")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text("保存")
        }
    }
}
