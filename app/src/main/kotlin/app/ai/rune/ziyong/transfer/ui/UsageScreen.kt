package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.SuccessGreen
import app.ai.rune.ziyong.transfer.ui.theme.WarningAmber
import dev.aigw.core.usage.StorageAudit
import dev.aigw.core.usage.UsageStats
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用量统计页：今日与累计的请求构成、缓存命中率、存储占用。
 *
 * 与上游的差异：这里只做「事实展示」，不叠加趋势图——趋势图在首页已有完整一份，
 * 两处各画一遍会让同一份数据出现两种口径（首页画 14 天、这里画 30 天），反而更容易误读。
 */
@Composable
fun UsageScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    // 存储占用不挂在全局 refresh 里（要扫全量存储键并解密，代价高），
    // 本页「存储」卡片读的是 state 里的同一份，不主动刷新会恒为 0，故进入本页时算一次。
    LaunchedEffect(Unit) { viewModel.refreshStorage() }

    PageScaffold(
        title = "用量统计",
        subtitle = "累计与今日",
        onBack = onBack,
    ) {
        SectionCard {
            SectionLabel("今日")
            CacheHitGauge(state.today)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatCell(formatCount(state.today.requests), "请求")
                StatCell(formatCount(state.today.success), "成功")
                StatCell(formatCount(state.today.failed), "失败")
                StatCell(formatCount(state.today.aborted), "中断")
            }
        }

        SectionCard {
            SectionLabel("累计")
            CacheHitGauge(state.total)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatCell(formatCount(state.total.requests), "请求")
                StatCell(formatCount(state.total.success), "成功")
                StatCell(formatCount(state.total.failed), "失败")
                StatCell(formatCount(state.total.aborted), "中断")
            }
        }

        SectionCard {
            SectionLabel("Token")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatCell(formatTokens(state.total.promptTokens), "输入")
                StatCell(formatTokens(state.total.completionTokens), "输出")
                StatCell(formatTokens(state.total.totalTokens), "合计")
            }
        }

        SectionCard {
            SectionLabel("存储")
            Text(
                text = "调用记录 ${state.storedCalls} 条，占用约 ${StorageAudit.formatSize(state.storage?.totalChars ?: 0L)}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 输入 token 构成环：命中缓存 / 未命中 / 输出 三段按比例分色填充，中心显示命中率。
 *
 * 没有输入 token 时只显示「—」，不画环——把「没有数据」画成 0% 命中会误导用户去排查缓存。
 * 命中率本身由 core 计算（[UsageStats.cacheHitPercent]），此处不重复算一遍，
 * 避免两处四舍五入口径不一致。
 */
@Composable
private fun CacheHitGauge(stats: UsageStats) {
    val hitPercent = stats.cacheHitPercent
    val hitColor = SuccessGreen
    val missColor = MiuixTheme.colorScheme.primary
    val outputColor = WarningAmber
    val trackColor = MiuixTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(88.dp).height(88.dp)) {
                val strokeWidth = 9.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth),
                )

                if (hitPercent != null && stats.promptTokens > 0) {
                    val cached = stats.cachedTokens.toFloat()
                    val miss = (stats.promptTokens - stats.cachedTokens).coerceAtLeast(0L).toFloat()
                    val output = stats.completionTokens.toFloat()
                    val total = cached + miss + output

                    if (total > 0f) {
                        var startAngle = -90f
                        // 三段依次绘制：每段按自身占比换算成角度，起点接上一段终点
                        listOf(
                            cached to hitColor,
                            miss to missColor,
                            output to outputColor,
                        ).forEach { (value, color) ->
                            val sweep = value / total * 360f
                            if (sweep > 0f) {
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                                )
                                startAngle += sweep
                            }
                        }
                    }
                }
            }
            Text(
                text = hitPercent?.let { "${it.toInt()}%" } ?: "—",
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "缓存命中率",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            GaugeLegend("命中", stats.cachedTokens, hitColor)
            GaugeLegend("未命中", (stats.promptTokens - stats.cachedTokens).coerceAtLeast(0L), missColor)
            GaugeLegend("输出", stats.completionTokens, outputColor)
        }
    }
}

@Composable
private fun GaugeLegend(label: String, tokens: Long, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = "$label ${formatTokens(tokens)}",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
