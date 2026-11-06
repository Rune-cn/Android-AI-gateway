package app.ai.rune.ziyong.transfer.ui.providers.trae

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import dev.aigw.core.provider.trae.TraeRegion
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Trae（原 SOLO）的界面。
 *
 * 特殊之处：国内（trae.cn）与国际（trae.ai）是两套**完全独立**的账号体系，
 * 因此本组件自管账号列表（[managesOwnAccounts]），按区域滑块过滤后再渲染——
 * 页面骨架若统一渲染账号，会把两边账号混在一张表里，用户无法判断某个号属于哪个站点。
 */
object TraeUi : ProviderUi {

    override val id: String = "trae"

    override val managesOwnAccounts: Boolean get() = true

    override fun description(): String = "网页登录导入，走 SOLO 免费通道（国内/国际）"

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
        var expanded by remember { mutableStateOf(false) }
        var raw by remember { mutableStateOf("") }
        // 滑块状态持久化在供应商设置里（region=cn/global）；引擎开登录页时按它选 host
        var global by rememberSaveable {
            mutableStateOf(actions.providerOptionOf(id, "region", "cn") == "global")
        }
        val region = if (global) TraeRegion.INTL.id else TraeRegion.CN.id

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            actions.onUpdateProviderOption(id, "region", if (isGlobal) "global" else "cn")
                        },
                    )
                }

                TextDivider()

                Button(
                    onClick = { actions.onWebLogin(id, region) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(
                        text = if (global) "在浏览器中打开 Trae 国际版登录页" else "在浏览器中打开 Trae 登录页",
                    )
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
                        label = "回调链接 / 凭证 JSON",
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
    }

    /** 按当前滑块区域过滤账号：国内版只列 trae.cn 账号，国际版只列 trae.ai 账号。 */
    @Composable
    private fun AccountRegionSection(
        global: Boolean,
        accounts: List<AccountStatus>,
        actions: ProviderUiActions,
    ) {
        val current = if (global) "global" else "cn"
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
            extra = { AccountExtra(status, actions) },
            footer = {
                AccountDeleteRow(onRemove = { actions.onRemoveAccount(id, status.uid) })
            },
        )
    }

    @Composable
    override fun AccountExtra(status: AccountStatus, actions: ProviderUiActions) {
        val deviceId = actions.deviceIdOf(id, status.uid)
        var manual by remember(status.uid) { mutableStateOf("") }
        val isGlobal = actions.regionOf(id, status.uid) == "global"

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "额度",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = if (status.creditsKnown) status.credits.toString() else "—",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
                // 国际版没有签到制度，只给国内账号渲染签到入口
                if (!isGlobal) {
                    TextButton(
                        text = "签到",
                        onClick = { actions.onAction(id, status.uid, ACTION_CHECKIN, JsonObject()) },
                        minWidth = 0.dp,
                    )
                }
            }

            if (status.detail.isNotEmpty()) {
                Text(
                    text = status.detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            // 设备指纹只对国内签到有效，国际版不展示这块，避免误导用户去改一个用不上的值
            if (!isGlobal) {
                TextDivider()
                Text(
                    text = "设备指纹",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = deviceId.ifEmpty { "—" },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "重新生成",
                        onClick = { actions.onRegenerateDeviceId(id, status.uid) },
                        minWidth = 0.dp,
                    )
                }
                Text(
                    text = "设备指纹用于签到校验，非数字格式会被上游拒绝。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextField(
                    value = manual,
                    onValueChange = { manual = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "手动填写设备指纹",
                    singleLine = true,
                )
                Button(
                    onClick = {
                        actions.onSetDeviceId(id, status.uid, manual.trim())
                        manual = ""
                    },
                    enabled = manual.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("保存")
                }
            }
        }
    }
}
