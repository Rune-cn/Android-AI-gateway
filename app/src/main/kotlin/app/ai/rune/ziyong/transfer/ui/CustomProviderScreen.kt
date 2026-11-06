package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aigw.core.gateway.CustomProviderConfig
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 自定义供应商的新建/编辑。
 *
 * API Key（账号）与模型列表在供应商详情页的「添加账号」里维护，这里只管基本信息。
 * 「短名」建成后不可改：它已经被写进模型路由标识（`custom:<短名>`），
 * 允许修改会让所有历史调用记录与已配置的客户端同时失效。
 */
@Composable
fun CustomProviderScreen(
    existing: CustomProviderConfig?,
    onSave: (CustomProviderConfig) -> Unit,
    onDelete: ((String) -> Unit)?,
    onBack: () -> Unit,
) {
    var key by remember { mutableStateOf(existing?.key.orEmpty()) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var baseUrl by remember { mutableStateOf(existing?.baseUrl.orEmpty()) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }

    PageScaffold(
        title = if (existing == null) "新建供应商" else "编辑供应商",
        subtitle = "OpenAI 兼容接口",
        onBack = onBack,
    ) {
        SectionCard {
            Text(
                text = "基本信息",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            TextField(
                value = name,
                onValueChange = { name = it },
                label = "名称",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = "短名（用于模型前缀 custom:<短名>）",
                singleLine = true,
                enabled = existing == null,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = baseUrl,
                onValueChange = { baseUrl = it.trim() },
                label = "接口地址（到 /v1，或直接写 /v1/chat/completions）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "启用",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
        }

        Button(
            onClick = {
                onSave(
                    CustomProviderConfig(
                        key = key.trim(),
                        name = name.trim().ifEmpty { key.trim() },
                        baseUrl = baseUrl.trim(),
                        apiKeys = existing?.apiKeys.orEmpty(),
                        models = existing?.models.orEmpty(),
                        enabled = enabled,
                    ),
                )
            },
            enabled = key.trim().isNotEmpty() && baseUrl.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text("保存")
        }

        if (existing != null && onDelete != null) {
            TextButton(
                text = "删除该供应商",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error),
            )
        }
    }

    if (confirmDelete && existing != null && onDelete != null) {
        ConfirmDestructiveDialog(
            title = "删除这个供应商？",
            body = "「${existing.name.ifEmpty { existing.key }}」及其全部账号、模型配置都会被移除，" +
                "调用过它的客户端将无法再使用。此操作不可撤销。",
            confirmText = "删除供应商",
            onConfirm = { onDelete(existing.key) },
            onDismiss = { confirmDelete = false },
        )
    }
}

