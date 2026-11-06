package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.aigw.core.security.OutboundEvidence
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.ButtonDefaults

/**
 * 安全防护与风控设置页。
 *
 * 按「可插拔的防护组件」组织：每个组件只负责一类能力（脱敏、限速），
 * 后续新增 IP 管控、指纹脱敏等能力时，加一个组件块即可，不影响既有代码。
 */
@Composable
fun SecurityProtectionScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    var showEvidenceSheet by remember { mutableStateOf(false) }
    var showRateLimitDialog by remember { mutableStateOf(false) }

    PageScaffold(
        title = "安全防护",
        subtitle = "反审核脱敏与账号限速",
        onBack = onBack,
    ) {
        DesensitizationComponent(
            state = state,
            onToggle = { enabled ->
                viewModel.updateSecuritySettings(state.security.copy(sanitizeEnabled = enabled))
            },
            onReloadPipeline = { viewModel.reloadSanitizerPipeline() },
            onViewEvidence = { showEvidenceSheet = true },
        )

        RateLimitComponent(
            state = state,
            onOpenEdit = { showRateLimitDialog = true },
        )
    }

    if (showEvidenceSheet) {
        EvidenceSheet(
            evidences = state.evidences,
            onDismiss = { showEvidenceSheet = false },
            onClear = { viewModel.clearEvidences() },
        )
    }

    if (showRateLimitDialog) {
        RateLimitEditDialog(
            currentMin = state.security.minIntervalMillis,
            currentJitter = state.security.jitterMillis,
            onDismiss = { showRateLimitDialog = false },
            onConfirm = { newMin, newJitter ->
                viewModel.updateSecuritySettings(
                    state.security.copy(
                        minIntervalMillis = newMin,
                        jitterMillis = newJitter,
                    ),
                )
                showRateLimitDialog = false
            },
        )
    }
}

// ==============================================================================
// 独立防护组件 1：反审核脱敏
// ==============================================================================

@Composable
private fun DesensitizationComponent(
    state: AppUiState,
    onToggle: (Boolean) -> Unit,
    onReloadPipeline: () -> Unit,
    onViewEvidence: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "开启脱敏",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "客户端的合规声明里常有 DoS / exploit 等词，会被上游误拦。" +
                        "开启后在这些词内部插零宽空格，只改 system 消息，不影响用户输入",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Switch(
                checked = state.security.sanitizeEnabled,
                onCheckedChange = onToggle,
            )
        }

        TextDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = "重新加载处理链",
                onClick = onReloadPipeline,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = "出网取证",
                onClick = onViewEvidence,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ==============================================================================
// 独立防护组件 2：账号级限速
// ==============================================================================

@Composable
private fun RateLimitComponent(
    state: AppUiState,
    onOpenEdit: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "最小间隔",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${state.security.minIntervalMillis} 毫秒（同一账号两次上游请求的最短间隔）",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        TextDivider()

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "抖动幅度",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "${state.security.jitterMillis} 毫秒（避免固定节拍本身成为特征）",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        TextButton(
            text = "修改限速",
            onClick = onOpenEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ==============================================================================
// 出网取证详情底部抽屉
// ==============================================================================

@Composable
private fun EvidenceSheet(
    evidences: List<OutboundEvidence>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    val copyText = rememberTextCopier()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    WindowBottomSheet(
        show = true,
        title = "出网取证",
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "共 ${evidences.size} 条脱敏证据（只展示 system 改写）",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.weight(1f),
                )
                if (evidences.isNotEmpty()) {
                    TextButton(
                        text = "清空",
                        onClick = onClear,
                        minWidth = 0.dp,
                        colors = ButtonDefaults.textButtonColors(
                            textColor = MiuixTheme.colorScheme.error,
                        ),
                    )
                }
            }

            if (evidences.isEmpty()) {
                EmptyHint(
                    "暂无取证记录",
                    "当客户端发送包含 DoS / exploit 等词的 system 提示词时，脱敏穿透快照会记录在这里。",
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(evidences.size, key = { evidences[it].seq }) { index ->
                        val item = evidences[index]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${timeFormat.format(Date(item.atMillis))} · ${item.model}",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    text = "复制",
                                    onClick = { copyText(item.sanitizedSnippet) },
                                    minWidth = 0.dp,
                                )
                            }

                            Text(
                                text = "命中词汇：${item.matchedTerms.joinToString(", ")}",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.error,
                            )

                            Text(
                                text = "脱敏前片段：",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Text(
                                text = item.originalSnippet,
                                style = MiuixTheme.textStyles.footnote2,
                                fontFamily = FontFamily.Monospace,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )

                            Text(
                                text = "脱敏后（已注入零宽空格）：",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.primary,
                            )
                            Text(
                                text = item.sanitizedSnippet,
                                style = MiuixTheme.textStyles.footnote2,
                                fontFamily = FontFamily.Monospace,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 限速参数修改弹窗
// ==============================================================================

@Composable
private fun RateLimitEditDialog(
    currentMin: Long,
    currentJitter: Long,
    onDismiss: () -> Unit,
    onConfirm: (newMin: Long, newJitter: Long) -> Unit,
) {
    var minText by remember { mutableStateOf(currentMin.toString()) }
    var jitterText by remember { mutableStateOf(currentJitter.toString()) }

    WindowDialog(
        show = true,
        title = "修改账号限速参数",
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = minText,
                onValueChange = { minText = it.filter(Char::isDigit).take(5) },
                label = "最小间隔（毫秒）",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = jitterText,
                onValueChange = { jitterText = it.filter(Char::isDigit).take(5) },
                label = "抖动幅度（毫秒）",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "同一账号连续请求将强制按「最小间隔 + 随机抖动」节拍等待，建议 1000~3000ms。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "保存",
                    onClick = {
                        val min = minText.toLongOrNull() ?: currentMin
                        val jitter = jitterText.toLongOrNull() ?: currentJitter
                        onConfirm(min.coerceIn(0L, 60_000L), jitter.coerceIn(0L, 10_000L))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
