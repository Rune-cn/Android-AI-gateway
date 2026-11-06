package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.aigw.core.failover.CircuitBreakerConfig
import dev.aigw.core.failover.ConcurrencyConfig
import dev.aigw.core.failover.ModelCooldownConfig
import dev.aigw.core.failover.RetryConfig
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.basic.ButtonDefaults

/**
 * 并发与容错设置页：
 * - 并发闸门：全局/单账号在途上限 + 排队超时（作用于上游请求全过程）；
 * - 重试退避：单供应商内换号重试次数与指数退避；
 * - 熔断器：连续失败阈值、静默时长、半开恢复成功数；
 * - 模型级冷却：只针对「该模型不可用」类错误的短时跳过。
 *
 * 全部参数保存后立即生效，无需重启网关。
 */
@Composable
fun ResilienceScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    var editTarget by remember { mutableStateOf<ResilienceEditTarget?>(null) }

    PageScaffold(
        title = "并发与容错",
        subtitle = "并发闸门、重试退避与熔断",
        onBack = onBack,
    ) {
        val failover = state.failover

        ConcurrencyComponent(
            config = failover.concurrency,
            onOpenEdit = { editTarget = ResilienceEditTarget.Concurrency },
        )
        RetryComponent(
            enabled = failover.enabled,
            config = failover.retry,
            onToggle = { viewModel.updateFailoverSettings(failover.copy(enabled = it)) },
            onOpenEdit = { editTarget = ResilienceEditTarget.Retry },
        )
        CircuitBreakerComponent(
            config = failover.circuitBreaker,
            onOpenEdit = { editTarget = ResilienceEditTarget.CircuitBreaker },
        )
        ModelCooldownComponent(
            config = failover.modelCooldown,
            onOpenEdit = { editTarget = ResilienceEditTarget.ModelCooldown },
        )

        Text(
            text = "全部参数保存后立即生效，无需重启网关。并发在途指「已向上游发起、尚未返回」的请求；" +
                "排队超时后该次尝试换下一个账号/供应商。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    editTarget?.let { target ->
        when (target) {
            ResilienceEditTarget.Concurrency -> ConcurrencyEditDialog(
                current = state.failover.concurrency,
                onDismiss = { editTarget = null },
                onConfirm = { next ->
                    viewModel.updateFailoverSettings(state.failover.copy(concurrency = next))
                    editTarget = null
                },
            )

            ResilienceEditTarget.Retry -> RetryEditDialog(
                current = state.failover.retry,
                onDismiss = { editTarget = null },
                onConfirm = { next ->
                    viewModel.updateFailoverSettings(state.failover.copy(retry = next))
                    editTarget = null
                },
            )

            ResilienceEditTarget.CircuitBreaker -> CircuitBreakerEditDialog(
                current = state.failover.circuitBreaker,
                onDismiss = { editTarget = null },
                onConfirm = { next ->
                    viewModel.updateFailoverSettings(state.failover.copy(circuitBreaker = next))
                    editTarget = null
                },
            )

            ResilienceEditTarget.ModelCooldown -> ModelCooldownEditDialog(
                current = state.failover.modelCooldown,
                onDismiss = { editTarget = null },
                onConfirm = { next ->
                    viewModel.updateFailoverSettings(state.failover.copy(modelCooldown = next))
                    editTarget = null
                },
            )
        }
    }
}

private enum class ResilienceEditTarget { Concurrency, Retry, CircuitBreaker, ModelCooldown }

@Composable
private fun ConcurrencyComponent(config: ConcurrencyConfig, onOpenEdit: () -> Unit) {
    SectionCard {
        Text(
            text = "并发闸门",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        ParamRow("单账号在途上限", if (config.maxPerAccount == 0) "不限" else "${config.maxPerAccount} 条")
        TextDivider()
        ParamRow("全局在途上限", if (config.maxGlobal == 0) "不限" else "${config.maxGlobal} 条")
        TextDivider()
        ParamRow("排队超时", "${config.queueTimeoutMs} 毫秒")
        TextButton(
            text = "修改并发参数",
            onClick = onOpenEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RetryComponent(
    enabled: Boolean,
    config: RetryConfig,
    onToggle: (Boolean) -> Unit,
    onOpenEdit: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "启用故障转移",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "关闭后单请求只在本供应商内换号：不退避重试、也不切到其它供应商",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        TextDivider()
        ParamRow("最大尝试次数", "${config.maxAttempts} 次（含首次）")
        TextDivider()
        ParamRow("初始退避", "${config.initialBackoffMs} 毫秒")
        TextDivider()
        ParamRow("退避上限", "${config.maxBackoffMs} 毫秒")
        TextDivider()
        ParamRow("退避倍率", "×${config.backoffMultiplier}")
        TextDivider()
        ParamRow("随机抖动", "±${(config.jitterRatio * 100).toInt()}%")
        TextButton(
            text = "修改重试参数",
            onClick = onOpenEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CircuitBreakerComponent(config: CircuitBreakerConfig, onOpenEdit: () -> Unit) {
    SectionCard {
        Text(
            text = "熔断器（按供应商）",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        ParamRow("连续失败阈值", "${config.failureThreshold} 次")
        TextDivider()
        ParamRow("熔断静默时长", "${config.openDurationMs / 1000} 秒")
        TextDivider()
        ParamRow("半开恢复成功数", "${config.halfOpenSuccessCount} 次")
        TextButton(
            text = "修改熔断参数",
            onClick = onOpenEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ModelCooldownComponent(config: ModelCooldownConfig, onOpenEdit: () -> Unit) {
    SectionCard {
        Text(
            text = "模型级冷却",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        ParamRow("冷却时长", "${config.cooldownMs / 1000} 秒")
        TextDivider()
        ParamRow("冷却上限", "${config.maxCooldownMs / 1000} 秒")
        TextButton(
            text = "修改模型级冷却",
            onClick = onOpenEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ParamRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ConcurrencyEditDialog(
    current: ConcurrencyConfig,
    onDismiss: () -> Unit,
    onConfirm: (ConcurrencyConfig) -> Unit,
) {
    var perAccount by remember { mutableStateOf(current.maxPerAccount.toString()) }
    var global by remember { mutableStateOf(current.maxGlobal.toString()) }
    var timeout by remember { mutableStateOf(current.queueTimeoutMs.toString()) }

    EditDialogScaffold(
        title = "修改并发参数",
        hint = "风控敏感的账号建议设 1；上游稳定时可放宽。全局上限建议 ≥ 单账号上限 × 账号数。",
        onDismiss = onDismiss,
        onConfirm = {
            // 解析失败回落当前值（不能静默变成「0=不限」——那会让风控直接失效）
            onConfirm(
                ConcurrencyConfig(
                    maxPerAccount = ConcurrencyConfig.clampMaxPerAccount(
                        perAccount.toIntOrNull() ?: current.maxPerAccount,
                    ),
                    maxGlobal = ConcurrencyConfig.clampMaxGlobal(global.toIntOrNull() ?: current.maxGlobal),
                    queueTimeoutMs = ConcurrencyConfig.clampQueueTimeoutMs(
                        timeout.toLongOrNull() ?: current.queueTimeoutMs,
                    ),
                ),
            )
        },
    ) {
        NumberField("单账号在途上限（条，0=不限）", perAccount, 2) { perAccount = it }
        NumberField("全局在途上限（条，0=不限）", global, 3) { global = it }
        NumberField("排队超时（毫秒）", timeout, 6) { timeout = it }
    }
}

@Composable
private fun RetryEditDialog(
    current: RetryConfig,
    onDismiss: () -> Unit,
    onConfirm: (RetryConfig) -> Unit,
) {
    var attempts by remember { mutableStateOf(current.maxAttempts.toString()) }
    var initial by remember { mutableStateOf(current.initialBackoffMs.toString()) }
    var maxBackoff by remember { mutableStateOf(current.maxBackoffMs.toString()) }
    var multiplier by remember { mutableStateOf(current.backoffMultiplier.toString()) }
    var jitter by remember { mutableStateOf((current.jitterRatio * 100).toInt().toString()) }

    EditDialogScaffold(
        title = "修改重试参数",
        hint = "退避是指数增长：第 n 次重试等待 = 初始退避 × 倍率^n，并叠加随机抖动。",
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(
                RetryConfig(
                    maxAttempts = (attempts.toIntOrNull() ?: current.maxAttempts).coerceIn(1, 10),
                    initialBackoffMs = (initial.toLongOrNull() ?: current.initialBackoffMs).coerceIn(0L, 10_000L),
                    maxBackoffMs = (maxBackoff.toLongOrNull() ?: current.maxBackoffMs).coerceIn(0L, 60_000L),
                    backoffMultiplier = (multiplier.toDoubleOrNull() ?: current.backoffMultiplier)
                        .coerceIn(1.0, 5.0),
                    jitterRatio = (
                        (jitter.toIntOrNull() ?: (current.jitterRatio * 100).toInt())
                            .coerceIn(0, 100) / 100.0
                        ),
                ),
            )
        },
    ) {
        NumberField("最大尝试次数（1-10）", attempts, 2) { attempts = it }
        NumberField("初始退避（毫秒）", initial, 5) { initial = it }
        NumberField("退避上限（毫秒）", maxBackoff, 6) { maxBackoff = it }
        NumberField("退避倍率（1.0-5.0）", multiplier, 4, allowDecimal = true) { multiplier = it }
        NumberField("随机抖动（0-100%）", jitter, 3) { jitter = it }
    }
}

@Composable
private fun CircuitBreakerEditDialog(
    current: CircuitBreakerConfig,
    onDismiss: () -> Unit,
    onConfirm: (CircuitBreakerConfig) -> Unit,
) {
    var threshold by remember { mutableStateOf(current.failureThreshold.toString()) }
    var openSeconds by remember { mutableStateOf((current.openDurationMs / 1000).toString()) }
    var halfOpen by remember { mutableStateOf(current.halfOpenSuccessCount.toString()) }

    EditDialogScaffold(
        title = "修改熔断参数",
        hint = "客户端 400 类错误不计入失败；熔断只统计上游侧错误。",
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(
                CircuitBreakerConfig(
                    failureThreshold = (threshold.toIntOrNull() ?: current.failureThreshold)
                        .coerceIn(1, 100),
                    openDurationMs = (
                        (openSeconds.toLongOrNull() ?: (current.openDurationMs / 1000))
                            .coerceIn(1L, 3600L) * 1000
                        ),
                    halfOpenSuccessCount = (halfOpen.toIntOrNull() ?: current.halfOpenSuccessCount)
                        .coerceIn(1, 10),
                ),
            )
        },
    ) {
        NumberField("连续失败阈值（1-100）", threshold, 3) { threshold = it }
        NumberField("熔断静默时长（秒，1-3600）", openSeconds, 4) { openSeconds = it }
        NumberField("半开恢复成功数（1-10）", halfOpen, 2) { halfOpen = it }
    }
}

@Composable
private fun ModelCooldownEditDialog(
    current: ModelCooldownConfig,
    onDismiss: () -> Unit,
    onConfirm: (ModelCooldownConfig) -> Unit,
) {
    var cooldownSeconds by remember { mutableStateOf((current.cooldownMs / 1000).toString()) }
    var maxSeconds by remember { mutableStateOf((current.maxCooldownMs / 1000).toString()) }

    EditDialogScaffold(
        title = "修改模型级冷却",
        hint = "上游报「该模型已超限」类错误时，只冷却该模型：不换账号、不计熔断，冷却期内该模型直接跳过。",
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(
                ModelCooldownConfig(
                    cooldownMs = (cooldownSeconds.toLongOrNull() ?: (current.cooldownMs / 1000))
                        .coerceIn(1L, 7200L) * 1000,
                    maxCooldownMs = (maxSeconds.toLongOrNull() ?: (current.maxCooldownMs / 1000))
                        .coerceIn(1L, 7200L) * 1000,
                ),
            )
        },
    ) {
        NumberField("冷却时长（秒）", cooldownSeconds, 5) { cooldownSeconds = it }
        NumberField("冷却上限（秒）", maxSeconds, 5) { maxSeconds = it }
    }
}

/**
 * 参数编辑弹窗的公共骨架：标题 + 字段 + 提示 + 取消/保存。
 *
 * 四个弹窗的差异只有字段与解析规则，把它们统一到这层后，
 * 按钮位置、取消语义与提示文案的位置不会再各自漂移。
 */
@Composable
private fun EditDialogScaffold(
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    fields: @Composable () -> Unit,
) {
    WindowDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fields()
            Text(
                text = hint,
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
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    maxLen: Int,
    allowDecimal: Boolean = false,
    onChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = { input ->
            // 整数字段不放行小数点：避免「1.5」这类输入被 toIntOrNull 静默丢弃后落回默认值
            val filtered = input.filter { it.isDigit() || (allowDecimal && it == '.') }
            onChange(filtered.take(maxLen))
        },
        label = label,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
