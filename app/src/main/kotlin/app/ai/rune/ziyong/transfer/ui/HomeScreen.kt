package app.ai.rune.ziyong.transfer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.LocalAppColors
import app.ai.rune.ziyong.transfer.ui.theme.SuccessGreen
import app.ai.rune.ziyong.transfer.ui.theme.WarningAmber
import dev.aigw.core.util.startOfDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.MiuixTheme
import android.widget.Toast

/** 趋势图展示天数。 */
private const val TREND_DAYS = 14

/**
 * 首页：网关控制台与遥测概览。
 *
 * 信息层级（从上到下）：能不能用（控制台）→ 在哪用（接入地址）→ 用得怎么样（指标/趋势/模型分布）。
 * 上游用 Canvas 画拓扑图与折线图；这里改为数据卡片 + 条形占比：
 * 手机窄屏上折线图的刻度几乎不可读，而「谁在跑、跑多少」用条形与数字能一眼看出。
 */
@Composable
fun HomeScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    openTaskCenter: () -> Unit,
) {
    val context = LocalContext.current
    val copyText = rememberTextCopier()
    val lanUrl = state.lanUrls.firstOrNull()

    // Android 13+ 通知权限被拒时前台服务照常跑，但常驻通知会被系统静默隐藏，
    // 用户看起来就是「服务没起来」；启动前先补上授权请求。
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.startGateway() }

    fun startWithNotificationPermission() {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startGateway()
        } else {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun copy(text: String, message: String) {
        copyText(text)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    PageScaffold(
        title = "R 中转",
        subtitle = "本地 OpenAI 兼容网关",
        actions = { StatusChip(state.running) },
    ) {
        ConsoleCard(
            state = state,
            lanUrl = lanUrl,
            onStart = { startWithNotificationPermission() },
            onStop = { viewModel.stopGateway() },
            onRefreshCredits = { viewModel.refreshAllCredits() },
            onCopy = { text, message -> copy(text, message) },
        )

        QuickActions(
            onCheckin = { viewModel.checkinAll() },
            onOpenTaskCenter = openTaskCenter,
        )

        // 批量动作进行中/结束：首页也要看得到进度与失败原因（签到按钮就在本页）
        BatchProgressHost(state.batchProgress)
        BatchOutcomeHost(state.batchOutcome) { viewModel.dismissBatchOutcome() }

        TelemetryGrid(state, viewModel)

        ContextCapacityCard(state)

        TrendCard(state)

        AntigravityQuotaSection(state, viewModel)
    }
}

/** 控制台卡：状态行 + 接入地址 + 启停与刷额度。 */
@Composable
private fun ConsoleCard(
    state: AppUiState,
    lanUrl: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRefreshCredits: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusDot(state.running)
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (state.running) "网关运行中" else "网关已停止",
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "监听 :${state.port} · ${state.pool.usable}/${state.pool.total} 个账号可用",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        TextDivider()

        val localUrl = state.localUrl.ifEmpty { "http://127.0.0.1:${state.port}/v1" }
        AddressRow("本机", localUrl) { onCopy(localUrl, "已复制本机地址") }
        AddressRow(
            label = "局域网",
            value = lanUrl ?: "未连接 Wi-Fi（局域网不可用）",
            onCopy = lanUrl?.let { url -> { onCopy(url, "已复制局域网地址") } },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { if (state.running) onStop() else onStart() },
                modifier = Modifier.weight(1f),
                colors = if (state.running) {
                    ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColorsPrimary()
                },
            ) {
                Text(if (state.running) "停止网关" else "启动网关")
            }
            TextButton(
                text = "刷新额度",
                onClick = onRefreshCredits,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 快捷操作：批量签到 + 进入任务中心。 */
@Composable
private fun QuickActions(onCheckin: () -> Unit, onOpenTaskCenter: () -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onCheckin,
                modifier = Modifier.weight(1f),
            ) {
                top.yukonga.miuix.kmp.basic.Icon(
                    imageVector = MiuixIcons.Regular.Tasks,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text("批量签到")
            }
            TextButton(
                text = "任务中心",
                onClick = onOpenTaskCenter,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 遥测指标：2×3 网格。
 *
 * 每个格子只回答一个问题（几个供应商、几个模型、今天多少请求…），
 * 副标题用真实数据而不是「暂无数据」这种占位——空白格会让人怀疑是加载失败。
 */
@Composable
private fun TelemetryGrid(state: AppUiState, viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                label = "供应商",
                value = state.providers.size.toString(),
                note = if (state.providers.isEmpty()) {
                    "暂无"
                } else {
                    state.providers.joinToString(" · ") { it.displayName }
                },
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "账号就绪",
                value = "${state.pool.usable}/${state.pool.total}",
                note = "池内可用账号",
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "聚合模型",
                value = state.models.size.toString(),
                note = when {
                    state.modelsError.isNotEmpty() -> state.modelsError
                    state.models.isEmpty() -> "尚未拉取"
                    else -> "各供应商合计"
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                label = "今日请求",
                value = formatCount(state.today.requests),
                note = requestDeltaNote(state),
                noteColor = requestDeltaColor(state),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "今日 Tokens",
                value = formatTokens(state.today.totalTokens),
                note = "输入 ${formatTokens(state.today.promptTokens)} · 输出 ${formatTokens(state.today.completionTokens)}",
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "已知额度",
                value = if (state.pool.creditsKnown > 0) formatCount(state.pool.totalCredits) else "—",
                note = if (state.pool.creditsKnown > 0) {
                    "已同步 ${state.pool.creditsKnown} 个账号"
                } else {
                    "尚未同步"
                },
                valueColor = if (state.pool.creditsKnown > 0) {
                    LocalAppColors.current.goldText
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 今日请求较昨日的变化文案；没有昨日数据时如实说明，不编造增幅。 */
private fun requestDeltaNote(state: AppUiState): String {
    val yesterday = if (state.daily.size >= 2) state.daily[state.daily.size - 2] else null
    return if (yesterday != null && yesterday.requests > 0) {
        val delta = (state.today.requests - yesterday.requests).toDouble() / yesterday.requests * 100
        (if (delta >= 0) "↑ " else "↓ ") +
            String.format(Locale.US, "%.1f%%", kotlin.math.abs(delta)) + " 较昨日"
    } else {
        "暂无昨日数据"
    }
}

@Composable
private fun requestDeltaColor(state: AppUiState): Color {
    val yesterday = if (state.daily.size >= 2) state.daily[state.daily.size - 2] else null
    if (yesterday == null || yesterday.requests <= 0) return MiuixTheme.colorScheme.onSurfaceVariantSummary
    return if (state.today.requests >= yesterday.requests) SuccessGreen else WarningAmber
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    noteColor: Color = Color.Unspecified,
) {
    Card(
        modifier = modifier,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(10.dp),
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.title3,
            color = if (valueColor != Color.Unspecified) valueColor else MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = note,
            style = MiuixTheme.textStyles.footnote2,
            color = if (noteColor != Color.Unspecified) {
                noteColor
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 上下文容量与模型分布。
 *
 * 数据口径：最近一次请求的输入 token 与对应模型的上限（来自模型目录），
 * 若目录里查不到上限就显示「上限未知」——不猜一个数字出来。
 */
@Composable
private fun ContextCapacityCard(state: AppUiState) {
    val recentCalls = remember(state.calls) { state.calls.take(50) }
    val latestCall = recentCalls.firstOrNull()
    val promptTokens = latestCall?.promptTokens ?: 0L
    val cachedTokens = latestCall?.cachedTokens ?: 0L
    val contextWindow = remember(latestCall, state.models) {
        if (latestCall != null) {
            resolveContextWindow(latestCall.providerId, latestCall.model, state.models)
        } else {
            0L
        }
    }
    val aggregated = remember(recentCalls) {
        recentCalls
            .groupBy { it.model.ifEmpty { "未命名模型" } }
            .map { (model, calls) ->
                ModelTokenAggregation(
                    model = model,
                    requests = calls.size,
                    promptTokens = calls.sumOf { it.promptTokens },
                )
            }
            .sortedByDescending { it.promptTokens }
    }
    val totalPromptTokens = aggregated.sumOf { it.promptTokens }
    val palette = LocalAppColors.current.providerPalette
    var expanded by remember { mutableStateOf(false) }

    SectionCard {
        Text(
            text = "上下文与模型分布",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (latestCall != null) formatTokens(promptTokens) else "—",
                    style = MiuixTheme.textStyles.title2,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                val remainingText = when {
                    contextWindow > 0L && contextWindow >= promptTokens ->
                        "剩余 ${formatTokens(contextWindow - promptTokens)}"

                    contextWindow > 0L -> "超出上限"
                    else -> "剩余未知"
                }
                val limitText = if (contextWindow > 0L) "上限 ${formatTokens(contextWindow)}" else "上限未知"
                Text(
                    text = "$limitText · $remainingText",
                    style = MiuixTheme.textStyles.footnote2,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val hitRateText = if (promptTokens > 0L) {
                    String.format(Locale.US, "%.1f%%", cachedTokens.toDouble() / promptTokens * 100.0)
                } else {
                    "—"
                }
                Text(
                    text = hitRateText,
                    style = MiuixTheme.textStyles.title2,
                    fontFamily = FontFamily.Monospace,
                    color = if (cachedTokens > 0L) SuccessGreen else MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "缓存命中",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        TextDivider()

        Text(
            text = "近 50 次请求的输入 token（合计 ${if (totalPromptTokens > 0L) formatTokens(totalPromptTokens) else "暂无"}）",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )

        // 分段占比条：只有 ≥2 个模型时才画，单模型时下方的明细已经说明一切
        if (totalPromptTokens > 0L && aggregated.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainerHighest),
            ) {
                aggregated.forEachIndexed { index, item ->
                    val fraction = (item.promptTokens.toFloat() / totalPromptTokens).coerceAtLeast(0.001f)
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(palette[index % palette.size]),
                    )
                }
            }
        }

        if (aggregated.isNotEmpty()) {
            val visible = if (expanded) aggregated else aggregated.take(5)
            for ((index, item) in visible.withIndex()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(palette[index % palette.size]),
                    )
                    Text(
                        text = item.model,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${item.requests} 次",
                        style = MiuixTheme.textStyles.footnote2,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = formatTokens(item.promptTokens),
                        style = MiuixTheme.textStyles.footnote2,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
            if (aggregated.size > 5) {
                TextButton(
                    text = if (expanded) "收起" else "显示全部 ${aggregated.size} 项",
                    onClick = { expanded = !expanded },
                    minWidth = 0.dp,
                )
            }
        }
    }
}

/** 单模型的近 50 次请求聚合统计。 */
private data class ModelTokenAggregation(
    val model: String,
    val requests: Int,
    val promptTokens: Long,
)

/**
 * 在 state.models 里匹配 contextWindow：
 * 1. 用 providerId + model（完全匹配 id / name，并支持剥离 routePrefix）；
 * 2. 退化为任意 provider 下匹配模型名；
 * 3. 匹配不到返回 0（表示未知，界面据此显示「上限未知」）。
 */
private fun resolveContextWindow(
    providerId: String,
    modelName: String,
    models: List<dev.aigw.core.provider.RoutedModel>,
): Long {
    if (modelName.isEmpty() || models.isEmpty()) return 0L
    val cleanName = modelName.substringAfterLast('/')

    val matched = models.firstOrNull { routed ->
        routed.providerId.equals(providerId, ignoreCase = true) &&
            (
                routed.model.id.equals(modelName, ignoreCase = true) ||
                    routed.model.id.equals(cleanName, ignoreCase = true) ||
                    routed.model.name.equals(cleanName, ignoreCase = true) ||
                    routed.fullId.equals(modelName, ignoreCase = true)
                )
    }
    if (matched != null && matched.model.contextWindow > 0L) {
        return matched.model.contextWindow
    }

    val fallback = models.firstOrNull { routed ->
        routed.model.id.equals(cleanName, ignoreCase = true) ||
            routed.model.name.equals(cleanName, ignoreCase = true)
    }
    if (fallback != null && fallback.model.contextWindow > 0L) {
        return fallback.model.contextWindow
    }
    return 0L
}

/**
 * 消耗趋势卡：近 14 天的输入/输出 token 横向条形对比。
 *
 * 用条形而非折线：窄屏上折线的横轴标签几乎挤成一团，而条形天然对齐日期，
 * 且能在同一行里同时读出「输入多还是输出多」。无数据时明确说明，不画空坐标系。
 */
@Composable
private fun TrendCard(state: AppUiState) {
    val trend = remember(state.daily) { state.daily.takeLast(TREND_DAYS) }
    val hasData = remember(trend) {
        trend.size >= 2 && trend.any { it.promptTokens > 0 || it.completionTokens > 0 }
    }
    val gold = LocalAppColors.current.goldStart
    val palette = LocalAppColors.current.providerPalette
    val outputColor = palette[1]

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "消耗趋势",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (trend.isEmpty()) "" else "近 ${trend.size} 天",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        if (!hasData) {
            Text(
                text = "暂无足够历史，累积几天后显示趋势",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            return@SectionCard
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(gold, "输入")
            LegendDot(outputColor, "输出")
        }

        val maxValue = remember(trend) {
            trend.maxOf { maxOf(it.promptTokens, it.completionTokens) }.coerceAtLeast(1L)
        }
        val todayStart = remember { startOfDay(System.currentTimeMillis()) }

        for ((index, day) in trend.withIndex()) {
            val dayMillis = todayStart - (trend.size - 1 - index) * 86_400_000L
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = dayLabel(dayMillis),
                    style = MiuixTheme.textStyles.footnote2,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.size(width = 42.dp, height = 14.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ThinRatioBar(
                        fraction = day.promptTokens.toFloat() / maxValue,
                        height = 4.dp,
                        color = gold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ThinRatioBar(
                        fraction = day.completionTokens.toFloat() / maxValue,
                        height = 4.dp,
                        color = outputColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = formatTokens(day.totalTokens),
                    style = MiuixTheme.textStyles.footnote2,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(width = 52.dp, height = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 一行接入地址：等宽地址 + 复制入口；无地址时不可点，避免点了没反应。 */
@Composable
private fun AddressRow(label: String, value: String, onCopy: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(width = 42.dp, height = 14.dp),
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
            fontFamily = FontFamily.Monospace,
            color = if (onCopy != null) {
                MiuixTheme.colorScheme.onSurface
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onCopy != null) {
            TextButton(
                text = "复制",
                onClick = onCopy,
                minWidth = 0.dp,
            )
        }
    }
}

/** Antigravity 的模型组配额窗口（每组 × 每窗口一条），首页与账号详情共用。 */
@Composable
private fun AntigravityQuotaSection(state: AppUiState, viewModel: AppViewModel) {
    val accounts = state.accountsOf("antigravity")
    if (accounts.isEmpty()) return

    // 进首页拉一次；账号集合变化时重拉
    LaunchedEffect(accounts.map { it.uid }) {
        accounts.forEach { viewModel.loadCreditPacks("antigravity", it.uid) }
    }

    SectionCard {
        Text(
            text = "上游配额池",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        for (account in accounts) {
            val packs = state.creditPacks["antigravity/${account.uid}"].orEmpty()
            if (accounts.size > 1) {
                Text(
                    text = account.nickname.ifEmpty { account.uid },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            if (packs.isEmpty()) {
                Text(
                    text = if (account.detail.isNotEmpty()) account.detail else "尚未拉取配额",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (pack in packs) {
                        QuotaPackBar(pack)
                    }
                }
            }
        }
    }
}

private val DAY_LABEL_FORMAT = SimpleDateFormat("M/d", Locale.US)

private fun dayLabel(millis: Long): String = DAY_LABEL_FORMAT.format(Date(millis))
