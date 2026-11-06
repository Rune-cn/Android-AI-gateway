package app.ai.rune.ziyong.transfer.ui.providers.loomy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import app.ai.rune.ziyong.transfer.ui.SectionCard
import app.ai.rune.ziyong.transfer.ui.TextDivider
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUi
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import dev.aigw.core.pool.AccountStatus
import dev.aigw.core.provider.ACTION_TASKS
import dev.aigw.core.provider.ProviderTask
import dev.aigw.core.provider.loomy.LoomyProvider
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Loomy：手机号短信登录，走免费积分通道。
 *
 * 登录区包含国家码输入——上游按国家码路由短信通道，写死 86 会让非大陆号码收不到验证码，
 * 所以这里不做「默认中国大陆」的便利性隐藏。
 */
object LoomyUi : ProviderUi {

    override val id: String = "loomy"

    /** 验证码重发间隔（秒）。 */
    private const val RESEND_SECONDS = 60

    override fun description(): String = "手机号短信登录，走免费积分通道"

    @Composable
    override fun Icon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.Messages,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = modifier,
        )
    }

    @Composable
    override fun LoginEntry(accounts: List<AccountStatus>, actions: ProviderUiActions) {
        // 国家码：讯飞按它路由短信通道，写死 86 会让非大陆号码收不到验证码
        var ccode by rememberSaveable { mutableStateOf("86") }
        var phone by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var countdown by remember { mutableIntStateOf(0) }

        // 发送后 60 秒内不允许重发：上游有频率限制，连点只会被拒
        LaunchedEffect(countdown) {
            if (countdown > 0) {
                delay(1_000)
                countdown -= 1
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = ccode,
                    onValueChange = { value ->
                        // 国家码只留数字，最多 4 位（最长的是三位码）
                        ccode = value.filter(Char::isDigit).take(4)
                    },
                    modifier = Modifier.width(96.dp),
                    label = "国家码",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(15) },
                    modifier = Modifier.weight(1f),
                    label = "手机号",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }

            TextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth(),
                label = "验证码",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    text = if (countdown > 0) "${countdown} 秒后重发" else "发送验证码",
                    onClick = {
                        // 先开倒计时，按钮立刻有反馈；发送失败再恢复可点
                        countdown = RESEND_SECONDS
                        actions.onSmsLogin(id, phone.trim(), "", ccode.trim()) { error ->
                            if (error.isNotEmpty()) countdown = 0
                        }
                    },
                    enabled = phone.isNotBlank() && countdown == 0,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { actions.onSmsLogin(id, phone.trim(), code.trim(), ccode.trim()) { } },
                    enabled = phone.isNotBlank() && code.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("登录")
                }
            }
        }
    }

    @Composable
    override fun AccountExtra(status: AccountStatus, actions: ProviderUiActions) {
        var invite by remember(status.uid) { mutableStateOf("") }
        var redeemCode by remember(status.uid) { mutableStateOf("") }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (status.detail.isNotEmpty()) {
                Text(
                    text = status.detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            TextDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = invite,
                    onValueChange = { invite = it },
                    modifier = Modifier.weight(1f),
                    label = "邀请码",
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val value = invite.trim()
                        actions.onAction(
                            id,
                            status.uid,
                            LoomyProvider.ACTION_REDEEM_INVITE,
                            JsonObject().apply { addProperty("inviteCode", value) },
                        )
                        invite = ""
                    },
                    enabled = invite.isNotBlank(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("兑换")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = redeemCode,
                    onValueChange = { redeemCode = it },
                    modifier = Modifier.weight(1f),
                    label = "兑换码",
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val value = redeemCode.trim()
                        actions.onAction(
                            id,
                            status.uid,
                            LoomyProvider.ACTION_REDEEM_CODE,
                            JsonObject().apply { addProperty("code", value) },
                        )
                        redeemCode = ""
                    },
                    enabled = redeemCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("兑换")
                }
            }

            TextButton(
                text = "一键完成新手任务",
                onClick = { actions.onAction(id, status.uid, ACTION_TASKS, JsonObject()) },
                modifier = Modifier.fillMaxWidth(),
            )

            TaskListSection(status, actions)
        }
    }

    /**
     * 新手任务明细：拉取该账号的任务清单，逐条展示完成状态与积分。
     *
     * 「一键完成新手任务」是盲跑的（提交前只知道哪些没完成），这里把清单摊开：
     * 哪些做完了、哪些还没做、每条值多少积分，一眼看得见。单条的「完成」只提交这一条。
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
                text = "新手任务",
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
                text = "上游没有返回任务",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            else -> {
                val done = view.tasks.count { it.completed }
                val earned = view.tasks.filter { it.completed }.sumOf { it.rewardCredit }
                Text(
                    text = "共 ${view.tasks.size} 个 · 已完成 $done 个 · 已得 $earned 积分",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                view.tasks.forEach { task -> TaskRow(task, status.uid, actions) }
            }
        }
    }

    @Composable
    private fun TaskRow(task: ProviderTask, uid: String, actions: ProviderUiActions) {
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
                    text = if (task.completed) "已完成" else "未完成",
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (task.completed) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                )
            }
            if (task.rewardCredit > 0) {
                Text(
                    text = "奖励 ${task.rewardCredit} 积分",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (!task.completed) {
                // 只提交这一条；任务对应真实动作，服务端会校验，失败时上游原话会透出来
                TextButton(
                    text = "完成",
                    onClick = {
                        actions.onAction(
                            id,
                            uid,
                            LoomyProvider.ACTION_COMPLETE_TASK,
                            JsonObject().apply { addProperty("key", task.code) },
                        )
                    },
                    minWidth = 0.dp,
                )
            }
        }
    }
}
