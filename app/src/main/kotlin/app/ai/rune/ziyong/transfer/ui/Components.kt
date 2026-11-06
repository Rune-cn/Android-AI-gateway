package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.LocalAppColors
import app.ai.rune.ziyong.transfer.ui.theme.SuccessGreen
import app.ai.rune.ziyong.transfer.ui.theme.WarningAmber
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用展示组件。
 *
 * 这些组件是「一页里反复出现的视觉单元」的唯一实现处。页面里不要各自拼一套卡片/状态点，
 * 否则一次视觉调整要改十几处，且深浅色下容易出现不一致。
 */

/**
 * 内容卡片：使用 miuix 的 surfaceContainer 底色，不带边框与阴影。
 *
 * 与上游「白底描边卡片」的差异是刻意的：MIUI/HyperOS 的分组卡片靠底色与圆角区分层级，
 * 描边会让整页看起来像表格。
 *
 * @param pressable 为 true 时整卡可按下并回弹（入口型卡片专用）。
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    pressable: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            insideMargin = androidx.compose.foundation.layout.PaddingValues(contentPadding),
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            pressFeedbackType = if (pressable) {
                top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink
            } else {
                top.yukonga.miuix.kmp.utils.PressFeedbackType.None
            },
            onClick = onClick,
            content = content,
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            insideMargin = androidx.compose.foundation.layout.PaddingValues(contentPadding),
            cornerRadius = 16.dp,
            colors = CardDefaults.defaultColors(),
            content = content,
        )
    }
}

/** 小节标题：miuix 的 SmallTitle，左对齐、次级色。 */
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.subtitle,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
    )
}

/** 小节头：左侧标题 + 右侧可选动作文字。 */
@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            TextButton(
                text = action,
                onClick = onAction,
                minHeight = 32.dp,
                minWidth = 0.dp,
            )
        }
    }
}

/**
 * 运行状态点：运行中为绿色带光环，停止为灰点。
 *
 * 只画点不画文字——文字由调用方按语境决定（「运行中」「已连接」等），
 * 组件内写死文案会导致同一状态在不同页面叫法不一。
 */
@Composable
fun StatusDot(
    running: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (running) {
            Box(
                Modifier
                    .size(size * 2f)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.18f)),
            )
        }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (running) SuccessGreen else MiuixTheme.colorScheme.onSurfaceVariantActions,
                ),
        )
    }
}

/**
 * 状态胶囊：白底 + 细描边 + 前置状态点。
 *
 * 用 [StatusDot] 复用状态点，保证同一状态在同页不同位置的颜色一致。
 */
@Composable
fun StatusChip(running: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusDot(running)
        Text(
            text = if (running) "运行中" else "已停止",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 统计单元：标签在上（小字、次级色），数值在下（大字）。
 *
 * [indicatorColor] 用于强调数值（例如额度充足用绿、告警用琥珀）；
 * 不传则用当前内容色，避免同一页里一半彩色一半黑白。
 */
@Composable
fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    indicatorColor: Color = Color.Unspecified,
) {
    Column(modifier = modifier) {
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
            color = if (indicatorColor != Color.Unspecified) indicatorColor else MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 可点击复制的地址条：等宽字体，点击后由调用方给 Toast 反馈。 */
@Composable
fun AddressTile(value: String, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 10.dp,
        ),
        cornerRadius = 10.dp,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainerHigh,
            contentColor = MiuixTheme.colorScheme.onSurface,
        ),
        onClick = onCopy,
    ) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 空态 / 错误态提示：标题 + 说明 + 可选的下一步动作。
 *
 * 光说「没有数据」等于把用户晾在那儿；给出 [actionLabel] 时一并渲染按钮，
 * 由调用方决定下一步是什么（去登录、重新拉取、重试）。
 */
@Composable
fun EmptyHint(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 细比例条：用于「已用/可用」「命中率」这类占比展示。
 *
 * [fraction] 为 null 表示占比未知（如额度字段缺失）——这时不画填充条，
 * 避免把「不知道」画成 0%（与本项目「数据来源要诚实」的原则一致）。
 */
@Composable
fun ThinRatioBar(
    fraction: Float?,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    color: Color = LocalAppColors.current.goldStart,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (fraction != null && fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(height)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/**
 * 分页条：上一页 / 页码信息 / 下一页，边界处按钮禁用。
 *
 * 长列表（账号、调用记录）一次组合上千条会明显卡顿，这里配合 `PagedList` 使用。
 */
@Composable
fun PagerBar(
    page: Int,
    pageCount: Int,
    total: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            text = "上一页",
            onClick = { onSelect(page - 1) },
            enabled = page > 0,
            minWidth = 0.dp,
        )
        Text(
            text = "第 ${page + 1} / $pageCount 页 · 共 $total 个",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            text = "下一页",
            onClick = { onSelect(page + 1) },
            enabled = page < pageCount - 1,
            minWidth = 0.dp,
        )
    }
}

/**
 * 账号分页：把超长列表切成每页固定条数，避免一次性组合全部卡片。
 *
 * 页码用不可变 Int 保存（不要用 SnapshotStateList 等快照类型做 rememberSaveable，
 * 本工程曾因快照在保存时机被读取而闪退）。
 *
 * [itemContent] 的第一个参数是页内序号（当前页从 0 起，而非全局序号）。
 */
@Composable
fun <T> PagedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    pageSize: Int = 30,
    keyOf: (T) -> String,
    itemContent: @Composable (pageIndex: Int, item: T) -> Unit,
) {
    var page by remember { mutableStateOf(0) }
    val pageCount = if (items.isEmpty()) 1 else (items.size + pageSize - 1) / pageSize
    // 列表变短（删账号、切区域）后旧页码可能越界：本帧先夹取显示，
    // 否则列表重新变长时会突然跳回那个已失效的大页码。
    val currentPage = page.coerceIn(0, pageCount - 1)
    if (page != currentPage) page = currentPage
    val visible = items.drop(currentPage * pageSize).take(pageSize)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visible.forEachIndexed { pageIndex, item ->
            androidx.compose.runtime.key(keyOf(item)) {
                itemContent(pageIndex, item)
            }
        }
        if (items.size > pageSize) {
            Spacer(Modifier.height(4.dp))
            PagerBar(currentPage, pageCount, items.size) { page = it }
        }
    }
}

/** 横向留白常量：与 miuix 页面内容的标准边距一致。 */
val PageHorizontalPadding: Dp = 12.dp

/**
 * 可选胶囊：选中态实心主色，未选中态中性底。
 *
 * 全应用的「单选/多选标签」都走这里（筛选、默认供应商、区域选项），
 * 保证选中反馈在每个页面一致——用户不需要在每页重新判断「哪个是当前选中」。
 */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface,
        )
    }
}

/**
 * 卡片内的「标题 + 说明 + 开关」行。
 *
 * 抽出来是因为它在设置类页面出现十余次：每次都手写一遍 Row/Column/权重，
 * 必然出现某一行文字垂直居中、另一行顶端对齐的不一致。
 */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        top.yukonga.miuix.kmp.basic.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 卡片内的分组分割线。
 *
 * 比裸 `HorizontalDivider` 多一层语义：它自带上下 8dp 的呼吸感，
 * 让「同一个卡片里的两个分区」看起来是分开的两块，而不是贴在一起的文字。
 */
@Composable
fun TextDivider(modifier: Modifier = Modifier) {
    top.yukonga.miuix.kmp.basic.HorizontalDivider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MiuixTheme.colorScheme.dividerLine,
    )
}

/**
 * 额度包进度条：一行「名称（分组）+ 过期时间」，下方细进度条 + 右侧百分比。
 *
 * 用权重绘制而不是引入进度组件，保证在所有背景色下前景可控；
 * 颜色随余量变化（充足=强调色，偏低=琥珀，告急=错误色），让「快用完」在扫视时就能看出来。
 */
@Composable
fun QuotaPackBar(pack: dev.aigw.core.provider.QuotaPack) {
    // 格式化器创建成本不低，按过期时间缓存结果，避免每次重组都新建
    val expireText = remember(pack.expireAt) {
        if (pack.expireAt > 0) {
            java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.SHORT,
                java.text.DateFormat.SHORT,
            ).format(java.util.Date(pack.expireAt * 1000))
        } else {
            ""
        }
    }
    // limit 为 0 表示上游没给总量：此时「占比」无从谈起，按 null 处理而不是画成 0%
    val fraction = if (pack.limit > 0) {
        (pack.remain.toFloat() / pack.limit).coerceIn(0f, 1f)
    } else {
        null
    }
    val barColor = when {
        fraction == null -> MiuixTheme.colorScheme.primary
        fraction > 0.5f -> MiuixTheme.colorScheme.primary
        fraction > 0.2f -> WarningAmber
        else -> MiuixTheme.colorScheme.error
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (pack.group.isEmpty()) pack.name else "${pack.name}（${pack.group}）",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                // 模型专属配额（如 Gemini 4 Argon 周额度）：不标出来就会被误当成账号总额度
                if (pack.scope.isNotEmpty()) {
                    Text(
                        text = pack.scope,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            if (expireText.isNotEmpty()) {
                Text(
                    text = expireText,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThinRatioBar(
                fraction = fraction,
                modifier = Modifier.weight(1f),
                height = 6.dp,
                color = barColor,
            )
            Text(
                text = if (fraction != null) {
                    "${(fraction * 100).toInt()}% (${pack.remain}/${pack.limit})"
                } else {
                    "${pack.remain}"
                },
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 一笔积分/额度流水：左侧事件名与时间，右侧带符号的变动值。
 *
 * 供应商无关：小浣熊的积分明细与「额度中心」的流水共用这一处渲染。
 */
@Composable
fun PointsBillRow(bill: dev.aigw.core.provider.PointsBill, modifier: Modifier = Modifier) {
    val time = remember(bill.createdAtMillis) {
        if (bill.createdAtMillis > 0) {
            java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.SHORT,
                java.text.DateFormat.SHORT,
            ).format(java.util.Date(bill.createdAtMillis))
        } else {
            ""
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = bill.title,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (time.isNotEmpty()) {
                Text(
                    text = time,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Text(
            text = if (bill.points > 0) "+${bill.points}" else bill.points.toString(),
            style = MiuixTheme.textStyles.footnote1,
            color = when {
                bill.points > 0 -> SuccessGreen
                bill.points < 0 -> MiuixTheme.colorScheme.error
                else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
        )
    }
}

/** 流水区块的提示行（加载中 / 失败 / 空）；[error] 为 true 时用错误色。 */
@Composable
fun PointsBillsHint(text: String, error: Boolean = false) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = if (error) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
