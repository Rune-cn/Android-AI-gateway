package app.ai.rune.ziyong.transfer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import dev.aigw.core.pool.AccountStatus
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults

/** 账号状态文案：只有凭证失效与手动停用两种异常态，正常即「可用」。 */
fun accountStatusText(account: AccountStatus): String = when {
    account.disabled -> "凭证失效，需重新登录"
    !account.enabled -> "已停用"
    else -> "可用"
}

/**
 * 账号标题列：上层为昵称（为空时回退到 UID），下层为状态文案。
 *
 * UID 回退是必要的：部分供应商在新登录后拿不到昵称，若直接渲染空串，
 * 用户会在列表里看到若干个只有开关的空白卡片。
 */
@Composable
fun AccountTitleColumn(
    account: AccountStatus,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = account.nickname.ifEmpty { account.uid },
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            text = accountStatusText(account),
            style = MiuixTheme.textStyles.footnote1,
            color = if (account.disabled || !account.enabled) {
                MiuixTheme.colorScheme.error
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            maxLines = 1,
        )
    }
}

/** 账号卡片头部：左侧账号信息，右侧挂接操作控件。 */
@Composable
fun AccountTitleRow(
    account: AccountStatus,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountTitleColumn(account = account, modifier = Modifier.weight(1f))
        trailing()
    }
}

/**
 * 账号优先级箭头（↑/↓）：排在前面的先用，同级仍按余额。
 *
 * 被手动提到默认位置之上的账号额外标一枚「优先」——否则用户看不出哪些号是手动提上来的。
 * 判据用 [AccountStatus.raised] 而不是 priority：↑/↓ 是交换两个账号的优先级，
 * 退回原顺序时高值会跑到另一个账号身上，直接看 priority 会让标记带着跑。
 */
@Composable
fun AccountPriorityArrows(
    account: AccountStatus,
    providerId: String,
    actions: ProviderUiActions,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (account.raised) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "优先",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Icon(
            imageVector = MiuixIcons.Regular.ExpandLess,
            contentDescription = "提高优先级",
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { actions.onMoveAccountPriority(providerId, account.uid, true) }
                .padding(4.dp)
                .size(20.dp),
        )
        Icon(
            imageVector = MiuixIcons.Regular.ExpandMore,
            contentDescription = "降低优先级",
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { actions.onMoveAccountPriority(providerId, account.uid, false) }
                .padding(4.dp)
                .size(20.dp),
        )
    }
}

/** 账号启用/停用开关。 */
@Composable
fun AccountEnableSwitch(
    account: AccountStatus,
    providerId: String,
    actions: ProviderUiActions,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = account.enabled,
        onCheckedChange = { actions.onToggleAccount(providerId, account.uid, it) },
        modifier = modifier,
    )
}

/**
 * 账号删除入口：先弹确认框，确认后才真正删除。
 *
 * 删除会清掉本地凭证且不可恢复，所以这一步不接受「点一下就删」。
 */
@Composable
fun AccountDeleteRow(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by remember { mutableStateOf(false) }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(
            text = "删除账号",
            onClick = { confirm = true },
            minWidth = 0.dp,
            colors = ButtonDefaults.textButtonColors(
                textColor = MiuixTheme.colorScheme.error,
            ),
        )
    }
    if (confirm) {
        ConfirmDestructiveDialog(
            title = "删除这个账号？",
            body = "本地保存的凭证会被清除，之后要重新登录才能再用。此操作不可撤销。",
            onConfirm = onRemove,
            onDismiss = { confirm = false },
        )
    }
}

/**
 * 统一账号卡片外壳：标题行（昵称 + 状态 + 开关）+ 供应商专属信息区 + 删除行。
 *
 * 所有需要自管账号列表的供应商（Trae / WorkBuddy / 自定义）都复用它，
 * 保证同一个概念在不同供应商下长得一样——用户不必重新学习一遍。
 *
 * @param trailing 覆盖右侧控件，默认是「优先级箭头 + 启用开关」。
 * @param extra 供应商专属信息区。
 * @param footer 底部操作区；传 null 时隐藏。
 */
@Composable
fun AccountCardShell(
    account: AccountStatus,
    providerId: String,
    actions: ProviderUiActions,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    extra: @Composable () -> Unit = {},
    footer: (@Composable () -> Unit)? = {
        AccountDeleteRow(onRemove = { actions.onRemoveAccount(providerId, account.uid) })
    },
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountTitleRow(
            account = account,
            trailing = trailing ?: {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountPriorityArrows(account = account, providerId = providerId, actions = actions)
                    AccountEnableSwitch(account = account, providerId = providerId, actions = actions)
                }
            },
        )
        extra()
        footer?.invoke()
    }
}

/**
 * 两段式区域选择器（国内 / 国际）。
 *
 * 用于 Trae、WorkBuddy 这类「同一供应商按站点拆成两套独立账号」的场景。
 * 指示块用弹簧动画滑动：切换区域会同时切换下方账号列表，动效能让「内容变了」
 * 这件事变得可察觉，而不是像页面闪了一下。
 */
@Composable
fun RegionSelector(
    isGlobal: Boolean,
    onSelectGlobal: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labels: List<String> = listOf("国内", "国外"),
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = (index == 1) == isGlobal
            val animProgress by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 600f),
                label = "regionIndicator$index",
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = animProgress))
                    .clickable { onSelectGlobal(index == 1) }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (selected) {
                        MiuixTheme.colorScheme.onPrimary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                )
            }
        }
    }
}
