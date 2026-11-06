package app.ai.rune.ziyong.transfer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.WarningAmber
import dev.aigw.core.gateway.GatewaySettings
import dev.aigw.core.usage.CallRecord
import dev.aigw.core.usage.CallStatus
import dev.aigw.core.usage.CallTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import app.ai.rune.ziyong.transfer.ui.theme.SuccessGreen
import androidx.compose.foundation.clickable
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

private enum class LogTab(val label: String) { Calls("调用记录"), Requests("请求日志") }

private enum class StatusFilter(val label: String) {
    ALL("全部"), SUCCESS("成功"), FAILED("失败"), ABORTED("中断"),
}

private enum class LevelFilter(val label: String) {
    ALL("全部"), INFO("信息"), WARN("警告"), ERROR("错误"),
}

/**
 * 记录页：调用记录与请求日志两个子标签。
 *
 * 两个标签共用同一份「日志设置」面板。面板内容**不随标签变化**——此前按标签切换内容，
 * 导致「某个设置只有某个标签才有」的困惑，同类返工发生过两次，这里刻意合并。
 */
@Composable
fun LogsScreen(state: AppUiState, viewModel: AppViewModel) {
    var tab by remember { mutableStateOf(LogTab.Calls) }
    var filter by remember { mutableStateOf(StatusFilter.ALL) }
    var levelFilter by remember { mutableStateOf(LevelFilter.ALL) }
    var detail by remember { mutableStateOf<CallRecord?>(null) }
    var showLogSettings by remember { mutableStateOf(false) }

    // 停留在本页时自动刷新（只刷记录，不动低频数据）
    LaunchedEffect(tab) {
        while (true) {
            delay(5_000)
            viewModel.refreshRecords()
        }
    }

    val calls = remember(state.calls, filter) {
        state.calls.filter {
            when (filter) {
                StatusFilter.ALL -> true
                StatusFilter.SUCCESS -> it.status == CallStatus.SUCCESS
                StatusFilter.FAILED -> it.status == CallStatus.FAILED
                StatusFilter.ABORTED -> it.status == CallStatus.ABORTED
            }
        }
    }

    val logs = remember(state.logs, levelFilter) {
        when (levelFilter) {
            LevelFilter.ALL -> state.logs
            else -> state.logs.filter { it.level.equals(levelFilter.name, ignoreCase = true) }
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        topBar = {
            top.yukonga.miuix.kmp.basic.TopAppBar(
                title = "记录",
                subtitle = "调用与请求日志",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Refresh,
                            contentDescription = "刷新",
                            tint = MiuixTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
        ) {
            // 筛选与设置区固定在顶部，列表用 LazyColumn 懒加载（记录最多数百条、每条含大段原文）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    top.yukonga.miuix.kmp.basic.TabRow(
                        tabs = listOf(LogTab.Calls.label, LogTab.Requests.label),
                        selectedTabIndex = tab.ordinal,
                        onTabSelected = { tab = LogTab.entries[it] },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showLogSettings = !showLogSettings }) {
                        Icon(
                            imageVector = MiuixIcons.Regular.Settings,
                            contentDescription = "日志设置",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                if (showLogSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 面板较长，限高 + 可滚动，避免挤掉下面的列表
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DiagnosticsSettings(state, viewModel)
                        CallLogSettings(state, viewModel)
                        RequestLogSettings(state, viewModel)
                    }
                }

                if (tab == LogTab.Calls) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusFilter.entries.forEach { option ->
                            FilterChip(
                                text = option.label,
                                selected = filter == option,
                                onClick = { filter = option },
                            )
                        }
                    }
                    Text(
                        text = "共 ${state.calls.size} 条记录 · 点卡片看原文 · 长按删除",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevelFilter.entries.forEach { option ->
                            FilterChip(
                                text = option.label,
                                selected = levelFilter == option,
                                onClick = { levelFilter = option },
                            )
                        }
                    }
                    Text(
                        text = "共 ${logs.size} 条日志 · 容量 ${state.settings.requestLogCapacity} 条",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                overscrollEffect = null,
            ) {
                if (tab == LogTab.Calls) {
                    if (calls.isEmpty()) {
                        item(key = "empty-calls") {
                            SectionCard {
                                EmptyHint(
                                    title = "暂无调用记录",
                                    message = "用任一 API Key 调用本地网关后，记录会出现在这里。",
                                )
                            }
                        }
                    } else {
                        items(calls.size, key = { calls[it].id }) { index ->
                            val record = calls[index]
                            var confirmDelete by remember { mutableStateOf(false) }
                            SectionCard {
                                CallRow(
                                    record = record,
                                    onClick = { detail = record },
                                    // 长按只开确认框：删的是一条带完整原文的记录，误触代价太高
                                    onLongClick = { confirmDelete = true },
                                )
                            }
                            if (confirmDelete) {
                                ConfirmDestructiveDialog(
                                    title = "删除这条调用记录？",
                                    body = "${record.model} · ${statusLabel(record.status)}，删除后不可恢复。",
                                    confirmText = "删除",
                                    onConfirm = { viewModel.deleteCall(record.id) },
                                    onDismiss = { confirmDelete = false },
                                )
                            }
                        }
                    }
                } else {
                    if (logs.isEmpty()) {
                        item(key = "empty-logs") {
                            SectionCard {
                                EmptyHint("暂无请求日志", "启动服务并发起调用后，这里会记录每次转发与换号。")
                            }
                        }
                    } else {
                        // key 用单调自增的 seq：时间戳+内容可能重复（同一秒内多条相同日志），
                        // 重复 key 会让 LazyColumn 直接崩溃。
                        items(logs.size, key = { logs[it].seq }) { index ->
                            val line = logs[index]
                            SectionCard(contentPadding = 12.dp) {
                                Text(
                                    text = line.render(),
                                    style = MiuixTheme.textStyles.footnote2,
                                    fontFamily = FontFamily.Monospace,
                                    color = levelColor(line.level),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { record ->
        WindowBottomSheet(
            show = true,
            title = "调用详情",
            onDismissRequest = { detail = null },
        ) {
            CallDetailSheet(record, viewModel)
        }
    }
}

/** 筛选胶囊：选中态用主色实心，未选中用中性底。 */
@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
            )
            .clickableCompat(onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
        )
    }
}

private fun Modifier.clickableCompat(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CallRow(record: CallRecord, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // 模型名缺失（旧记录或异常写入路径）时给明确占位；纯空白串同样回落，
                // 否则头部会出现「看着像什么都没渲染」的空白。
                text = record.model.ifBlank { "未指定模型" },
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(record.status)
        }
        Text(
            text = buildString {
                append(TIME_FORMAT.format(Date(record.startedAtMillis)))
                append(" · ")
                append(if (record.streaming) "流式" else "非流式")
            },
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 胶囊行用 FlowRow：横向放不下时整块换行，任何宽度下每个胶囊都保持自然宽度，
        // 不会被压到「剩余宽度」里把文字折成多行。
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MetricChip("耗时", formatDuration(record.durationMillis), MiuixTheme.colorScheme.onSurfaceVariantSummary)
            if (record.totalTokens > 0) {
                MetricChip("tokens", formatTokens(record.totalTokens), MiuixTheme.colorScheme.primary)
            }
            if (record.cachedTokens > 0 && record.promptTokens > 0) {
                val hitPercent = String.format(Locale.US, "%.0f", record.cachedTokens * 100.0 / record.promptTokens)
                MetricChip("命中", "$hitPercent%", SuccessGreen)
            }
            if (record.pointsConsumed > 0) {
                MetricChip("积分", record.pointsConsumed.toString(), WarningAmber)
            }
            if (record.accountNickname.isNotEmpty()) {
                MetricChip("账号", record.accountNickname, MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        if (record.error.isNotEmpty()) {
            Text(
                text = record.error,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 一条指标胶囊：等宽数字 + 标签，把 token/命中/耗时等压成一行可扫读的信息。
 *
 * 两个 Text 都禁止折行（maxLines = 1 + softWrap = false）：宽度不足时只会省略显示，
 * 绝不会折成多行把胶囊撑成方块。
 */
@Composable
private fun MetricChip(label: String, value: String, tone: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote2,
            fontFamily = FontFamily.Monospace,
            color = tone,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 状态徽章：成功 / 失败 / 中断三态都有稳定的底色与文字。
 *
 * 不能只留一个无底色的小字——一旦头部字段异常，那一行看起来就像完全没渲染。
 */
@Composable
private fun StatusBadge(status: CallStatus) {
    val tone = statusColor(status)
    Text(
        text = statusLabel(status),
        style = MiuixTheme.textStyles.footnote2,
        color = tone,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun CallDetailSheet(record: CallRecord, viewModel: AppViewModel) {
    val clipboard = LocalClipboardManager.current

    // 打开抽屉时后台加载各字段完整原文：没溢出文件（未超摘要长度或已被清理）时回落用记录内摘要。
    var fullRequest by remember(record.id) { mutableStateOf<String?>(null) }
    var fullResponse by remember(record.id) { mutableStateOf<String?>(null) }
    var fullRaw by remember(record.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(record.id) {
        val loaded = withContext(Dispatchers.IO) {
            Triple(
                viewModel.engineFullText(record.id, CallTextField.REQUEST),
                viewModel.engineFullText(record.id, CallTextField.RESPONSE),
                viewModel.engineFullText(record.id, CallTextField.RAW),
            )
        }
        fullRequest = loaded.first
        fullResponse = loaded.second
        fullRaw = loaded.third
    }

    // 抽屉内容可达几十万字符，必须自身可滚动
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = record.id,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = "${TIME_FORMAT.format(Date(record.startedAtMillis))} · ${statusLabel(record.status)} · HTTP ${record.httpStatus}",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        DetailBlock(
            "请求原文",
            fullRequest ?: record.requestBody,
            truncatedNote(fullRequest == null, record.requestBody),
        ) { clipboard.setText(AnnotatedString(fullRequest ?: record.requestBody)) }
        DetailBlock(
            "响应原文",
            fullResponse ?: record.responseBody,
            truncatedNote(fullResponse == null, record.responseBody),
        ) { clipboard.setText(AnnotatedString(fullResponse ?: record.responseBody)) }
        if (record.rawResponse.isNotEmpty() || fullRaw != null) {
            DetailBlock(
                "返回原文（上游报文）",
                fullRaw ?: record.rawResponse,
                truncatedNote(fullRaw == null, record.rawResponse),
            ) { clipboard.setText(AnnotatedString(fullRaw ?: record.rawResponse)) }
        }
        if (record.error.isNotEmpty()) {
            DetailBlock("错误", record.error, null) { clipboard.setText(AnnotatedString(record.error)) }
        }
    }
}

@Composable
private fun DetailBlock(title: String, body: String, note: String?, onCopy: () -> Unit) {
    // 大文本折叠：全文可达 512K 字符，一次性组合进 Text 会让抽屉首帧明显卡顿；
    // 默认只渲染预览，点「展开全文」后再渲染完整内容。
    var expanded by remember(body) { mutableStateOf(body.length <= PREVIEW_CHARS) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!expanded && body.length > PREVIEW_CHARS) {
                TextButton(
                    text = "展开全文",
                    onClick = { expanded = true },
                    minWidth = 0.dp,
                )
            }
            TextButton(
                text = "复制",
                onClick = onCopy,
                minWidth = 0.dp,
            )
        }
        if (note != null) {
            Text(
                text = note,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
            )
        }
        val visible = if (expanded) body else body.take(PREVIEW_CHARS) + "\n…（点击「展开全文」查看剩余部分）"
        Text(
            text = visible.ifEmpty { "（空）" },
            style = MiuixTheme.textStyles.footnote2,
            fontFamily = FontFamily.Monospace,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .animateContentSize()
                .padding(12.dp),
        )
    }
}

private const val PREVIEW_CHARS = 2_000

private fun statusLabel(status: CallStatus): String = when (status) {
    CallStatus.SUCCESS -> "成功"
    CallStatus.FAILED -> "失败"
    CallStatus.ABORTED -> "中断"
}

@Composable
private fun levelColor(level: String): Color = when (level.uppercase()) {
    "WARN" -> WarningAmber
    "ERROR" -> MiuixTheme.colorScheme.error
    else -> MiuixTheme.colorScheme.onSurface
}

@Composable
private fun statusColor(status: CallStatus): Color = when (status) {
    CallStatus.SUCCESS -> SuccessGreen
    CallStatus.FAILED -> MiuixTheme.colorScheme.error
    CallStatus.ABORTED -> WarningAmber
}

/**
 * 字段是否被截断的说明行：溢出文件缺失时，长度超过摘要阈值即视为「存储中为摘要」。
 */
private fun truncatedNote(fullMissing: Boolean, summary: String): String? = when {
    !fullMissing -> null
    summary.length >= dev.aigw.core.usage.CallLogStore.MAX_SUMMARY_CHARS ->
        "内容较长，仅保留前 ${dev.aigw.core.usage.CallLogStore.MAX_SUMMARY_CHARS} 字符摘要（完整原文已被清理或本次未启用完整保存）"

    else -> null
}

/** 排查诊断：详细日志开关。开启后记录每次调用的请求、转发、发送与返回原文，用于排查 503 类问题。 */
@Composable
private fun DiagnosticsSettings(state: AppUiState, viewModel: AppViewModel) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "详细日志",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "记录每次调用的请求、转发、发送与返回原文，用于排查 503 等问题；关闭后立即生效",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Switch(
                checked = state.settings.verboseLogging,
                onCheckedChange = { viewModel.updateSettings(state.settings.copy(verboseLogging = it)) },
            )
        }
    }
}

/**
 * 调用记录的参数设置：开关、保留天数、清空。
 *
 * 与请求日志、详细日志并列展示在同一面板内——面板内容不随标签变化。
 */
@Composable
private fun CallLogSettings(state: AppUiState, viewModel: AppViewModel) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "记录调用",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "保存每次调用的请求/响应原文与用量；关闭后不再新增（已存记录不受影响）",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Switch(
                checked = state.settings.callLoggingEnabled,
                onCheckedChange = { viewModel.updateSettings(state.settings.copy(callLoggingEnabled = it)) },
            )
        }

        TextDivider()

        Text(
            text = "服务运行时每日自动清理超过该天数的记录，当前 ${state.settings.logRetentionDays} 天。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        RetentionField(state.settings.logRetentionDays) { viewModel.setLogRetentionDays(it) }

        TextDivider()

        var confirmClear by remember { mutableStateOf(false) }
        TextButton(
            text = "清空调用记录",
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                textColor = MiuixTheme.colorScheme.error,
            ),
        )
        if (confirmClear) {
            ConfirmDestructiveDialog(
                title = "清空全部调用记录？",
                body = "将删除本机保存的 ${state.storedCalls} 条调用记录（含完整原文），不可恢复。",
                confirmText = "清空",
                onConfirm = { viewModel.clearCalls() },
                onDismiss = { confirmClear = false },
            )
        }
    }
}

/**
 * 保留天数输入：失焦或回车才提交，避免边输入边保存把「30」中间的「3」当成 30 天。
 * 越界值由 core 夹回，这里只做即时提示。
 */
@Composable
fun RetentionField(current: Int, onCommit: (Int) -> Unit) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null &&
        parsed in GatewaySettings.MIN_RETENTION_DAYS..GatewaySettings.MAX_RETENTION_DAYS
    val changed = parsed != null && parsed != current

    TextField(
        value = text,
        onValueChange = { input -> text = input.filter(Char::isDigit).take(4) },
        modifier = Modifier.fillMaxWidth(),
        label = "保留天数（${GatewaySettings.MIN_RETENTION_DAYS}-${GatewaySettings.MAX_RETENTION_DAYS}）",
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    when {
        text.isEmpty() -> Text(
            text = "不能为空",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.error,
        )

        !valid -> Text(
            text = "需在 ${GatewaySettings.MIN_RETENTION_DAYS}-${GatewaySettings.MAX_RETENTION_DAYS} 之间",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.error,
        )
    }
    if (changed && parsed != null) {
        TextButton(
            text = "保存保留天数",
            onClick = { onCommit(parsed) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 请求日志的参数设置：每项参数一个控件，展开后直接调整，不折叠到弹窗里。
 */
@Composable
private fun RequestLogSettings(state: AppUiState, viewModel: AppViewModel) {
    SectionCard {
        Text(
            text = "日志容量",
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = "环形缓冲最多保留的日志条数，超出后自动丢弃最旧一条；调大占用更多内存与存储。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        LogCapacityField(state.settings.requestLogCapacity) {
            viewModel.updateSettings(state.settings.copy(requestLogCapacity = it))
        }

        TextDivider()

        var confirmClearLogs by remember { mutableStateOf(false) }
        TextButton(
            text = "清空请求日志",
            onClick = { confirmClearLogs = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                textColor = MiuixTheme.colorScheme.error,
            ),
        )
        if (confirmClearLogs) {
            ConfirmDestructiveDialog(
                title = "清空全部请求日志？",
                body = "将删除当前所有请求日志（容量等设置不受影响），不可恢复。",
                confirmText = "清空",
                onConfirm = { viewModel.clearLogs() },
                onDismiss = { confirmClearLogs = false },
            )
        }
    }
}

/** 日志容量输入：按钮提交，越界由 core 夹回。 */
@Composable
private fun LogCapacityField(current: Int, onCommit: (Int) -> Unit) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null &&
        parsed in GatewaySettings.MIN_REQUEST_LOG_CAPACITY..GatewaySettings.MAX_REQUEST_LOG_CAPACITY

    TextField(
        value = text,
        onValueChange = { input -> text = input.filter(Char::isDigit).take(4) },
        modifier = Modifier.fillMaxWidth(),
        label = "请求日志容量（${GatewaySettings.MIN_REQUEST_LOG_CAPACITY}-${GatewaySettings.MAX_REQUEST_LOG_CAPACITY} 条）",
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    if (parsed != null && valid && parsed != current) {
        TextButton(
            text = "保存容量",
            onClick = { onCommit(parsed) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val TIME_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
