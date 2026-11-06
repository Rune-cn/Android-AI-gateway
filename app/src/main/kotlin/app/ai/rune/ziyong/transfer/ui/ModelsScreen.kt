package app.ai.rune.ziyong.transfer.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.SuccessGreen
import dev.aigw.core.provider.RoutedModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.basic.ButtonDefaults

/** 模型页：汇总所有供应商的模型（id 带 `provider/` 前缀），按供应商分组展示与流式更新。 */
@Composable
fun ModelsScreen(state: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    // 按 routePrefix 分组：区域型供应商（如 WorkBuddy 国内与国外）各成一个分组
    val grouped = remember(state.models) { state.models.groupBy { it.routePrefix } }
    val exposure = state.modelExposure
    // 折叠状态：记「被折叠」的分组（默认全展开）。
    //
    // 用不可变 Set + rememberSaveable，而不是 SnapshotStateList：
    // 后者在保存时机被 SaveableStateRegistry 读取时，若正好与 LazyColumn 的组合读快照交叠，
    // 会抛「Reading a state that was created after the snapshot was taken」。
    var collapsed by rememberSaveable { mutableStateOf(emptySet<String>()) }
    // 测试失败原文可能很长，行内只展示截断提示，全文点开弹窗查看
    var failureDetail by remember { mutableStateOf<String?>(null) }

    // 动态副标题：流式加载中展示进度
    val totalProviders = state.modelLoadStates.size
    val doneProviders = state.modelLoadStates.values.count { it !is ModelLoadState.Loading }
    val subtitleText = if (state.modelsLoading) {
        if (totalProviders > 0) {
            "共 ${state.models.size} 个 · 载入中 ($doneProviders/$totalProviders)"
        } else {
            "共 ${state.models.size} 个 · 更新中..."
        }
    } else {
        "共 ${state.models.size} 个"
    }

    // 还在加载中的供应商列表
    val pendingProviders = state.modelLoadStates.filter { it.value is ModelLoadState.Loading }.keys

    // 本页用 LazyPageScaffold：模型量大，懒加载避免整页一次性组合。
    LazyPageScaffold(
        title = "模型",
        subtitle = subtitleText,
        actions = {
            ExposureToggle(
                enabled = exposure.filterEnabled,
                onClick = {
                    viewModel.updateModelExposure(exposure.copy(filterEnabled = !exposure.filterEnabled))
                },
            )
            IconButton(onClick = { viewModel.refreshModels(forceRefresh = true) }) {
                Icon(
                    imageVector = MiuixIcons.Regular.Refresh,
                    contentDescription = "刷新",
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        },
    ) {
        // 顶部流式进度条：只在加载中占位，结束后完全收起
        item(key = "progress") {
            AnimatedVisibility(
                visible = state.modelsLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.modelsError.isNotEmpty()) {
            item(key = "error") {
                SectionCard {
                    Text(
                        text = "拉取模型目录时有报错：${state.modelsError}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }
        }

        if (grouped.isEmpty()) {
            if (state.modelsLoading) {
                // 首屏流式载入中骨架屏
                val loadingList = pendingProviders.toList().ifEmpty { listOf("loading_1", "loading_2") }
                items(loadingList, key = { "skeleton-$it" }) { id ->
                    val name = state.providerOf(id)?.displayName ?: "正在拉取模型目录"
                    ModelSkeletonCard(providerName = name)
                }
            } else {
                item(key = "empty") {
                    SectionCard {
                        EmptyHint(
                            title = "还没有模型",
                            message = "无可用账号的供应商不会返回动态模型；可以先刷新试试。",
                            actionLabel = "重新拉取",
                            onAction = { viewModel.refreshModels(forceRefresh = true) },
                        )
                    }
                }
            }
        } else {
            for ((routePrefix, models) in grouped) {
                val title = models.firstOrNull()?.providerName ?: routePrefix
                val allExposed = models.isNotEmpty() && models.all { exposure.allows(it.fullId) }
                // 这一组本轮没拉到上游、用的是上次成功的清单：明确标注，不能让旧清单冒充实时结果
                val cachedGroup = models.isNotEmpty() && models.all { it.cached }
                item(key = "header-$routePrefix") {
                    GroupHeader(
                        title = title,
                        count = models.size,
                        collapsed = routePrefix in collapsed,
                        cached = cachedGroup,
                        exposureEnabled = exposure.filterEnabled,
                        allExposed = allExposed,
                        onToggleCollapse = {
                            collapsed = if (routePrefix in collapsed) {
                                collapsed - routePrefix
                            } else {
                                collapsed + routePrefix
                            }
                        },
                        onToggleAll = { viewModel.toggleGroupExposed(routePrefix, !allExposed) },
                    )
                }
                // 折叠的分组跳过其模型行：只渲染分组头，模型行即时收起
                if (routePrefix in collapsed) continue
                items(models, key = { it.fullId }) { model ->
                    ModelRow(
                        model = model,
                        testStatus = state.modelTestLatencies[model.fullId],
                        exposureEnabled = exposure.filterEnabled,
                        exposed = exposure.allows(model.fullId),
                        onToggleExposed = { viewModel.toggleModelExposed(model.fullId, it) },
                        onCopy = {
                            clipboard.setText(AnnotatedString(model.fullId))
                            Toast.makeText(context, "已复制模型名称：${model.fullId}", Toast.LENGTH_SHORT).show()
                        },
                        onTest = { viewModel.testModel(model) },
                        onShowFailure = { failureDetail = it },
                    )
                }
            }

            // 若已有部分模型展示，而剩余供应商仍在加载中，在下方继续展示该供应商的骨架占位
            if (state.modelsLoading && pendingProviders.isNotEmpty()) {
                items(pendingProviders.toList(), key = { "skeleton-$it" }) { id ->
                    val name = state.providerOf(id)?.displayName ?: id
                    ModelSkeletonCard(providerName = name)
                }
            }
        }
    }

    failureDetail?.let { reason ->
        WindowDialog(
            show = true,
            title = "测试失败原因",
            onDismissRequest = { failureDetail = null },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = reason,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                )
                TextButton(
                    text = "关闭",
                    onClick = { failureDetail = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

/** 分组头：折叠箭头 + 供应商名 + 缓存标记 + 全选 + 数量。 */
@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    collapsed: Boolean,
    cached: Boolean,
    exposureEnabled: Boolean,
    allExposed: Boolean,
    onToggleCollapse: () -> Unit,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onToggleCollapse)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (collapsed) MiuixIcons.Regular.ExpandMore else MiuixIcons.Regular.ExpandLess,
            contentDescription = if (collapsed) "展开" else "折叠",
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        // 缓存标记：本轮没拉到上游时不能让旧清单冒充实时结果
        if (cached) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.errorContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "缓存",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Box(Modifier.weight(1f))
        if (exposureEnabled) {
            TextButton(
                text = if (allExposed) "取消全选" else "全选",
                onClick = onToggleAll,
                minWidth = 0.dp,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "$count 款",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 「开放名单」开关：点击切换是否只对外暴露勾选的模型。
 *
 * 开启时用主色胶囊区分——这是一个会影响所有客户端的开关，
 * 状态必须一眼可见，不能只靠一个细圆点。
 */
@Composable
private fun ExposureToggle(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (enabled) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions,
                ),
        )
        Text(
            text = if (enabled) "开放名单 开" else "开放名单",
            style = MiuixTheme.textStyles.footnote2,
            color = if (enabled) {
                MiuixTheme.colorScheme.onPrimaryContainer
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
        )
    }
}

/** 单个模型行：左侧模型 id 与信息（点击复制），中部测速状态，右侧测速按钮。 */
@Composable
private fun ModelRow(
    model: RoutedModel,
    testStatus: ModelTestStatus?,
    exposureEnabled: Boolean,
    exposed: Boolean,
    onToggleExposed: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onTest: () -> Unit,
    onShowFailure: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 开放名单开启时，左侧显示勾选框决定该模型是否对外暴露
            if (exposureEnabled) {
                Checkbox(
                    state = if (exposed) {
                        androidx.compose.ui.state.ToggleableState.On
                    } else {
                        androidx.compose.ui.state.ToggleableState.Off
                    },
                    onClick = { onToggleExposed(!exposed) },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onCopy)
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = model.fullId,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(model.model.name)
                        if (model.model.contextWindow > 0) {
                            append(" · 上下文 ${model.model.contextWindow}")
                        }
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // 能力标记：只有上游/供应商明确给了才显示，缺就不渲染（不猜）。
                // 各家写 extra 的口径不统一（CodeBuddy 写 tools/vision="true"，Loomy 写 tool_call/attachment="1"），
                // 这里把同义 key 与两种真假写法都认下，否则能力存在却显示不出来。
                val extra = model.model.extra
                fun flag(vararg keys: String): Boolean = keys.any { key ->
                    val v = extra[key]?.trim()?.lowercase()
                    v == "true" || v == "1"
                }
                val tags = buildList {
                    if (flag("reasoning")) add("思考")
                    if (flag("search", "web_search", "online")) add("联网")
                    if (flag("vision", "image", "attachment")) add("识图")
                    if (flag("tools", "tool_call", "toolcall")) add("工具")
                    extra["maxOutput"]?.takeIf { it.isNotBlank() }?.let { add("输出上限 $it") }
                    extra["vendor"]?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(" · "),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // 倍率与限时折扣：供应商（Trae 按积分倍率、CodeBuddy 按 credits）已算好，不显示等于白算
                val rateLabel = extra["rate"]?.takeIf { it.isNotBlank() }
                if (rateLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "倍率 $rateLabel",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.primary,
                        )
                        extra["rateDiscount"]?.takeIf { it.isNotBlank() }?.let { discount ->
                            Text(
                                text = "限时 $discount",
                                style = MiuixTheme.textStyles.footnote2,
                                color = SuccessGreen,
                            )
                        }
                    }
                }
            }

            // 测速状态：<5000ms 绿色，>=5000ms 橙色，失败/超时红色
            when (testStatus) {
                is ModelTestStatus.Testing -> Text(
                    text = "测试中...",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )

                is ModelTestStatus.Success -> Text(
                    text = "${testStatus.latencyMs}ms",
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (testStatus.latencyMs < 5000) SuccessGreen else Color(0xFFEF6C00),
                )

                is ModelTestStatus.Failed -> Text(
                    text = "失败：${testStatus.reason.take(24)}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .clickable { onShowFailure(testStatus.reason) }
                        .padding(2.dp),
                )

                null -> Unit
            }

            TextButton(
                text = "测试",
                onClick = onTest,
                enabled = testStatus !is ModelTestStatus.Testing,
                minWidth = 0.dp,
            )
        }
    }
}

/** 骨架屏占位卡片：呈现正在流式拉取的状态。 */
@Composable
private fun ModelSkeletonCard(
    providerName: String,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = providerName,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "加载中",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        // 骨架块用固定高度的色块表示，不引入微光动画：加载态本身就是短暂的，
        // 无限动画在低端机上反而会拖慢真正的列表组合。
        repeat(2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}
