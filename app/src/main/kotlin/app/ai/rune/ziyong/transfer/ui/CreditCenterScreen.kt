package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aigw.core.provider.ProviderCapability
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 额度中心：列出有账号的供应商，点进去看该供应商各账号的额度包明细。
 */
@Composable
fun CreditCenterScreen(state: AppUiState, onOpen: (String) -> Unit, onBack: () -> Unit) {
    PageScaffold(
        title = "额度中心",
        subtitle = "按供应商查看额度包",
        onBack = onBack,
    ) {
        val providers = state.providers.filter { state.accountsOf(it.id).isNotEmpty() }
        if (providers.isEmpty()) {
            SectionCard {
                EmptyHint("还没有账号", "先去「供应商」页添加账号。")
            }
            return@PageScaffold
        }

        for (info in providers) {
            val accounts = state.accountsOf(info.id)
            val known = accounts.filter { it.creditsKnown }
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpen(info.id) },
                pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
                insideMargin = androidx.compose.foundation.layout.PaddingValues(16.dp),
                cornerRadius = 16.dp,
                colors = CardDefaults.defaultColors(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = info.displayName,
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${accounts.size} 个账号 · 额度合计 " +
                                if (known.isEmpty()) "—" else known.sumOf { it.credits }.toString(),
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        // 可用账号占比：纯数字看不出「这个供应商到底健不健康」，加一条细条一眼可扫
                        ThinRatioBar(
                            fraction = if (accounts.isEmpty()) {
                                null
                            } else {
                                accounts.count { it.usable }.toFloat() / accounts.size
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (accounts.any { !it.usable }) {
                                MiuixTheme.colorScheme.error
                            } else {
                                MiuixTheme.colorScheme.primary
                            },
                        )
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

/**
 * 某个供应商各账号的额度明细：额度包 + （有账本的供应商）逐笔积分流水。
 *
 * 小浣熊这类供应商没有额度包概念，额度就是积分余额，逐笔明细全在流水里；
 * 只列额度包会让它在额度中心里看起来「没有明细」。
 */
@Composable
fun ProviderCreditsScreen(
    providerId: String,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val accounts = state.accountsOf(providerId)
    val displayName = state.providerOf(providerId)?.displayName ?: providerId
    val hasLedger = state.providerOf(providerId)?.capabilities
        ?.contains(ProviderCapability.POINTS_BILLS) == true

    // 进页面拉一次；账号变化时重拉
    LaunchedEffect(providerId, accounts.map { it.uid }) {
        accounts.forEach {
            viewModel.loadCreditPacks(providerId, it.uid)
            if (hasLedger) viewModel.loadPointsBills(providerId, it.uid)
        }
    }

    PageScaffold(
        title = displayName,
        subtitle = "额度包明细",
        onBack = onBack,
    ) {
        if (accounts.isEmpty()) {
            SectionCard {
                EmptyHint("还没有账号", "先去该供应商页添加账号。")
            }
            return@PageScaffold
        }

        for (account in accounts) {
            val packs = state.creditPacks["$providerId/${account.uid}"].orEmpty()
            val bills = state.pointsBills["$providerId/${account.uid}"]

            SectionCard {
                Text(
                    text = account.nickname.ifEmpty { account.uid },
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "余额 ${if (account.creditsKnown) account.credits.toString() else "—"} · ${accountStatusText(account)}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )

                if (packs.isEmpty()) {
                    Text(
                        text = when {
                            // 账本型供应商（如小浣熊）没有额度包，额度就是积分余额，明细在下方流水里
                            hasLedger -> "该供应商没有额度包，积分流水见下"
                            !account.creditsKnown && account.detail.isNotEmpty() -> account.detail
                            else -> "没有额度包明细"
                        },
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                } else {
                    for (pack in packs) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (pack.group.isEmpty()) pack.name else "${pack.name}（${pack.group}）",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = buildString {
                                    if (pack.remain >= 0) {
                                        append("剩余 ")
                                        append(pack.remain)
                                    } else {
                                        append("剩余 —")
                                    }
                                    if (pack.limit > 0) append(" / ").append(pack.limit)
                                    if (pack.used > 0) append(" · 已用 ").append(pack.used)
                                },
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }

                if (hasLedger) {
                    TextDivider()
                    SectionLabel("积分明细")
                    when {
                        bills == null -> PointsBillsHint("正在加载积分明细…")
                        bills.error.isNotEmpty() -> PointsBillsHint("加载失败：${bills.error}", error = true)
                        bills.bills.isEmpty() -> PointsBillsHint("暂无积分明细")
                        else -> for (bill in bills.bills) PointsBillRow(bill)
                    }
                }

                TextButton(
                    text = "刷新",
                    onClick = {
                        viewModel.refreshCredits(providerId, account.uid)
                        viewModel.loadCreditPacks(providerId, account.uid)
                        if (hasLedger) viewModel.loadPointsBills(providerId, account.uid)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
