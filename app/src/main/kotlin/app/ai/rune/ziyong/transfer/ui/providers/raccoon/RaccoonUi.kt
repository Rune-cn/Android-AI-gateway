package app.ai.rune.ziyong.transfer.ui.providers.raccoon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import app.ai.rune.ziyong.transfer.ui.PointsBillRow
import app.ai.rune.ziyong.transfer.ui.PointsBillsHint
import app.ai.rune.ziyong.transfer.ui.SectionCard
import app.ai.rune.ziyong.transfer.ui.TextDivider
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUi
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import dev.aigw.core.pool.AccountStatus
import dev.aigw.core.provider.ACTION_CHECKIN
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 小浣熊（商汤）：网页登录，用插件账号额度。 */
object RaccoonUi : ProviderUi {

    override val id: String = "raccoon"

    override fun description(): String = "小浣熊（商汤）网页登录，用插件账号额度"

    @Composable
    override fun Icon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.Store,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = modifier,
        )
    }

    @Composable
    override fun LoginEntry(accounts: List<AccountStatus>, actions: ProviderUiActions) {
        var expanded by remember { mutableStateOf(false) }
        var raw by remember { mutableStateOf("") }

        SectionCard {
            Button(
                onClick = { actions.onWebLogin(id, "") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("在浏览器中打开小浣熊登录页")
            }
            TextButton(
                text = if (expanded) "收起粘贴导入" else "粘贴回调链接导入",
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            )
            if (expanded) {
                TextField(
                    value = raw,
                    onValueChange = { raw = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "回调链接（含 authorization_code）",
                    minLines = 2,
                    maxLines = 6,
                )
                Button(
                    onClick = {
                        actions.onImport(id, raw.trim())
                        raw = ""
                    },
                    enabled = raw.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("导入")
                }
            }
        }
    }

    @Composable
    override fun AccountExtra(status: AccountStatus, actions: ProviderUiActions) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "积分",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = if (status.creditsKnown) status.credits.toString() else "—",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                TextButton(
                    text = "签到领积分",
                    onClick = { actions.onAction(id, status.uid, ACTION_CHECKIN, JsonObject()) },
                    minWidth = 0.dp,
                )
            }

            if (status.detail.isNotEmpty()) {
                Text(
                    text = status.detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            TextDivider()
            PointsBillsSection(status, actions)
        }
    }

    /**
     * 积分明细：逐笔展示最近收支（每日赠送 / 桌面端登录奖励 / 消耗都在这条流里）。
     *
     * 余额只说「还剩多少」，看不出「签到到底发没发」；明细才能回答这个问题。
     */
    @Composable
    private fun PointsBillsSection(status: AccountStatus, actions: ProviderUiActions) {
        val view = actions.pointsBillsOf(id, status.uid)
        LaunchedEffect(status.uid) {
            if (view == null) actions.onLoadPointsBills(id, status.uid)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "积分明细",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { actions.onLoadPointsBills(id, status.uid) }) {
                Icon(
                    imageVector = MiuixIcons.Regular.Refresh,
                    contentDescription = "刷新积分明细",
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        }

        when {
            view == null -> PointsBillsHint("正在加载积分明细…")
            view.error.isNotEmpty() -> PointsBillsHint("加载失败：${view.error}", error = true)
            view.bills.isEmpty() -> PointsBillsHint("暂无积分明细")
            else -> for (bill in view.bills) PointsBillRow(bill)
        }
    }
}
