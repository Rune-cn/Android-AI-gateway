package app.ai.rune.ziyong.transfer.ui.providers.codebuddy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import app.ai.rune.ziyong.transfer.ui.AccountCardShell
import app.ai.rune.ziyong.transfer.ui.AccountDeleteRow
import app.ai.rune.ziyong.transfer.ui.PagedList
import app.ai.rune.ziyong.transfer.ui.RegionSelector
import app.ai.rune.ziyong.transfer.ui.SectionCard
import app.ai.rune.ziyong.transfer.ui.TextDivider
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUi
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import dev.aigw.core.pool.AccountStatus
import dev.aigw.core.provider.ACTION_CHECKIN
import dev.aigw.core.provider.ACTION_TASKS
import dev.aigw.core.provider.ProviderTask
import dev.aigw.core.provider.codebuddy.CodeBuddyProvider
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * WorkBuddy（腾讯 CodeBuddy 的个人版）：设备授权登录，走每日积分。
 *
 * 与 Trae 同类：国内（copilot.tencent.com）与国际（workbuddy.ai）是两套账号体系，
 * 因此自管账号列表并按区域过滤。国际版没有签到与成长中心，相应入口只对国内账号渲染。
 */
object CodeBuddyUi : ProviderUi {

    override val id = CodeBuddyProvider.ID

    override val managesOwnAccounts: Boolean get() = true

    override fun description() = "在浏览器中登录，走每日积分"

    @Composable
    override fun Icon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.Community,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = modifier,
        )
    }

    @Composable
    override fun LoginEntry(accounts: List<AccountStatus>, actions: ProviderUiActions) {
        // 滑块状态持久化在供应商设置里（region=cn/global），登录与账号列表都跟随它
        var global by rememberSaveable {
            mutableStateOf(
                actions.providerOptionOf(id, "region", CodeBuddyProvider.REGION_CN) ==
                    CodeBuddyProvider.REGION_GLOBAL,
            )
        }
        var expanded by remember { mutableStateOf(false) }
        var raw by remember { mutableStateOf("") }
        val region = if (global) CodeBuddyProvider.REGION_GLOBAL else CodeBuddyProvider.REGION_CN

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "服务区域",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                RegionSelector(
                    isGlobal = global,
                    onSelectGlobal = { isGlobal ->
                        global = isGlobal
                        actions.onUpdateProviderOption(
                            id,
                            "region",
                            if (isGlobal) CodeBuddyProvider.REGION_GLOBAL else CodeBuddyProvider.REGION_CN,
                        )
                    },
                )
            }

            TextDivider()

            Button(
                onClick = { actions.onDeviceLogin(id, region) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(if (global) "打开国际版登录页" else "打开国内版登录页")
            }

            TextButton(
                text = if (expanded) "收起粘贴导入" else "粘贴凭证 JSON 导入",
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            )

            if (expanded) {
                TextField(
                    value = raw,
                    onValueChange = { raw = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "凭证 JSON",
                    minLines = 3,
                    maxLines = 8,
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
        if (accounts.isNotEmpty()) {
            AccountRegionSection(global, accounts, actions)
        }
    }

    @Composable
    private fun AccountRegionSection(
        global: Boolean,
        accounts: List<AccountStatus>,
        actions: ProviderUiActions,
    ) {
        val current = if (global) CodeBuddyProvider.REGION_GLOBAL else CodeBuddyProvider.REGION_CN
        val scoped = remember(accounts, current) { accounts.filter { actions.regionOf(id, it.uid) == current } }

        if (scoped.isEmpty()) {
            SectionCard {
                Text(
                    text = if (global) "还没有国际版账号，先登录" else "还没有国内版账号，先登录",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            return
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "账号 · ${scoped.size} 个",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { actions.onRefreshCredits(id, null) }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = "刷新额度",
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            PagedList(scoped, keyOf = { it.uid }) { _, account ->
                AccountBlock(account, actions, Modifier)
                TextDivider()
            }
        }
    }

    @Composable
    private fun AccountBlock(status: AccountStatus, actions: ProviderUiActions, modifier: Modifier) {
        AccountCardShell(
            account = status,
            providerId = id,
            actions = actions,
            modifier = modifier,
            extra = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "额度",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (status.creditsKnown) status.credits.toString() else "—",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
                if (status.detail.isNotEmpty()) {
                    Text(
                        text = status.detail,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                // 国际版（workbuddy.ai）没有签到制度与成长中心，只给国内账号渲染这两个入口
                if (actions.regionOf(id, status.uid) != CodeBuddyProvider.REGION_GLOBAL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            text = "签到",
                            onClick = { actions.onAction(id, status.uid, ACTION_CHECKIN, JsonObject()) },
                            minWidth = 0.dp,
                        )
                        TextButton(
                            text = "成长中心",
                            onClick = { actions.onAction(id, status.uid, ACTION_TASKS, JsonObject()) },
                            minWidth = 0.dp,
                        )
                    }
                    TaskListSection(status, actions)
                }
            },
            footer = {
                AccountDeleteRow(onRemove = { actions.onRemoveAccount(id, status.uid) })
            },
        )
    }

    /**
     * 任务中心明细：逐条展示完成度（已完成 / 可领取 / 进行中 / 未解锁）。
     *
     * 对照参考实现（WorkBuddy 反代）：`GET /v2/activity/growth/tasks` 的 `data.tasks[]`，
     * 每条含 title / progress.current / progress.target / accept_status / reward_credit。
     * 顶部「成长中心」按钮跑全流程：接单 → 把未完成的推进 → 领奖；
     * 单条任务不再有「领取」按钮——读取列表时供应商会自动补齐已达标奖励。
     */
    @Composable
    private fun TaskListSection(status: AccountStatus, actions: ProviderUiActions) {
        val view = actions.taskListOf(id, status.uid)
        LaunchedEffect(status.uid) {
            if (view == null) actions.onLoadTaskList(id, status.uid)
        }

        TextDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "任务中心",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { actions.onLoadTaskList(id, status.uid) }) {
                Icon(
                    imageVector = MiuixIcons.Regular.Refresh,
                    contentDescription = "刷新任务列表",
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        }

        when {
            view == null -> Text(
                text = "正在加载任务列表…",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            view.error.isNotEmpty() && view.tasks.isEmpty() -> Text(
                text = "加载任务失败：${view.error}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.error,
            )

            view.tasks.isEmpty() -> Text(
                // 只有上游明确说了不在期才这么写；没说就照实说没有任务
                text = if (view.inPeriod == false) {
                    "成长活动未开启，暂无可查看的任务"
                } else {
                    "该账号没有返回任何任务"
                },
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            else -> {
                val done = view.tasks.count { it.completed }
                Text(
                    text = "共 ${view.tasks.size} 个任务 · 已完成 $done 个",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                for (task in view.tasks) {
                    TaskRow(task)
                }
            }
        }
    }

    @Composable
    private fun TaskRow(task: ProviderTask) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = taskStatusText(task),
                    style = MiuixTheme.textStyles.footnote2,
                    color = taskStatusColor(task),
                )
            }
            val meta = buildString {
                if (task.target > 0) append("进度 ${task.progressText()}")
                if (task.rewardCredit > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("奖励 ${task.rewardCredit} 积分")
                }
                if (task.description.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append(task.description)
                }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }

    private fun taskStatusText(task: ProviderTask): String = when {
        task.claimed -> "已领取"
        task.completed -> "已完成"
        task.locked -> "未解锁"
        else -> "未完成"
    }

    @Composable
    private fun taskStatusColor(task: ProviderTask) = when {
        task.claimed -> MiuixTheme.colorScheme.onSurfaceVariantSummary
        task.completed -> MiuixTheme.colorScheme.primary
        task.locked -> MiuixTheme.colorScheme.onSurfaceVariantActions
        else -> MiuixTheme.colorScheme.error
    }
}
