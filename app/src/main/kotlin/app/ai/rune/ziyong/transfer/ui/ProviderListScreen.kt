package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiRegistry
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 供应商列表：每个供应商一张卡片（图标、说明、账号统计），点进详情。
 *
 * 页面本身不认识任何具体供应商——图标与说明都由各自的 `ProviderUi` 组件提供，
 * 新增供应商不需要改动这个文件。
 */
@Composable
fun ProviderListScreen(
    state: AppUiState,
    onOpen: (String) -> Unit,
    onAddCustom: () -> Unit,
) {
    PageScaffold(
        title = "供应商",
        subtitle = "共 ${state.providers.size} 个",
        actions = {
            IconButton(onClick = onAddCustom) {
                Icon(
                    imageVector = MiuixIcons.Regular.Add,
                    contentDescription = "添加自定义供应商",
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        },
    ) {
        if (state.providers.isEmpty()) {
            SectionCard {
                EmptyHint(
                    title = "还没有供应商",
                    message = "内置供应商会在启动时自动登记；自定义供应商可点右上角「+」添加。",
                    actionLabel = "添加自定义供应商",
                    onAction = onAddCustom,
                )
            }
            return@PageScaffold
        }

        // 图标底色渐变整页只建一次，不随每张卡片重建
        val iconBrush = remember(
            MiuixTheme.colorScheme.primaryContainer,
            MiuixTheme.colorScheme.secondaryContainer,
        ) {
            Brush.linearGradient(
                listOf(
                    MiuixTheme.colorScheme.primaryContainer,
                    MiuixTheme.colorScheme.secondaryContainer,
                ),
            )
        }

        for (provider in state.providers) {
            val ui = remember(provider.id) { ProviderUiRegistry.of(provider.id) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpen(provider.id) },
                pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
                insideMargin = androidx.compose.foundation.layout.PaddingValues(16.dp),
                cornerRadius = 16.dp,
                colors = CardDefaults.defaultColors(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        ui?.Icon(Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = provider.displayName,
                            style = MiuixTheme.textStyles.title4,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = ui?.description() ?: provider.id,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AccountBadge(
                                text = "账号 ${provider.accountCount}",
                                color = MiuixTheme.colorScheme.surfaceContainerHigh,
                                textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            if (provider.usableCount > 0) {
                                AccountBadge(
                                    text = "可用 ${provider.usableCount}",
                                    color = MiuixTheme.colorScheme.primaryContainer,
                                    textColor = MiuixTheme.colorScheme.onPrimaryContainer,
                                )
                            } else if (provider.accountCount > 0) {
                                // 有账号但一个都用不了才叫「不可用」；没账号是正常状态，不该报红
                                AccountBadge(
                                    text = "不可用",
                                    color = MiuixTheme.colorScheme.errorContainer,
                                    textColor = MiuixTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = MiuixIcons.Regular.ChevronForward,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountBadge(text: String, color: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, style = MiuixTheme.textStyles.footnote2, color = textColor)
    }
}
