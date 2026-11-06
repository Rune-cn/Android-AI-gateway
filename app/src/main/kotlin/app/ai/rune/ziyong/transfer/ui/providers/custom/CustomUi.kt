package app.ai.rune.ziyong.transfer.ui.providers.custom

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.AccountDeleteRow
import app.ai.rune.ziyong.transfer.ui.AccountEnableSwitch
import app.ai.rune.ziyong.transfer.ui.EmptyHint
import app.ai.rune.ziyong.transfer.ui.PagedList
import app.ai.rune.ziyong.transfer.ui.SectionCard
import app.ai.rune.ziyong.transfer.ui.TextDivider
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUi
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import dev.aigw.core.pool.AccountStatus
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.compose.foundation.layout.height

/**
 * 自定义供应商（任意 OpenAI 兼容端点）。
 *
 * 与其他供应商最大的不同：账号不是「登录」来的，而是手工填 API Key + 模型列表。
 * 因此这里提供添加表单与模型勾选面板，并把「拉取模型」做成主路径——
 * 手抄模型 id 极易拼错，而拼错的表现是请求 400，用户很难自己定位。
 */
class CustomUi(override val id: String) : ProviderUi {

    override val managesOwnAccounts: Boolean get() = true

    override fun description() = "手动导入的 OpenAI 兼容供应商"

    @Composable
    override fun Icon(modifier: Modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.Layers,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = modifier,
        )
    }

    @Composable
    override fun LoginEntry(accounts: List<AccountStatus>, actions: ProviderUiActions) {
        AddAccountSection(accounts, actions)
        PagedList(accounts, keyOf = { it.uid }) { _, account ->
            AccountDetailCard(account, actions)
        }
    }

    /** 「添加账号」表单：名称 + API Key + 模型（可拉取/手填），点「添加账号」连同模型列表一并保存。 */
    @Composable
    private fun AddAccountSection(accounts: List<AccountStatus>, actions: ProviderUiActions) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        var fetchedModels by remember { mutableStateOf<List<String>?>(null) }
        var models by rememberSaveable { mutableStateOf(actions.customModelsOf(id).joinToString("\n")) }
        var nickname by remember { mutableStateOf("") }
        var apiKey by remember { mutableStateOf("") }

        SectionCard(modifier = Modifier.animateContentSize()) {
            if (!expanded) {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("添加账号")
                }
                return@SectionCard
            }
            Text(
                text = "添加账号",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            TextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = "名称（可选，默认用供应商名）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = "API Key",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ModelsSection(
                accounts = accounts,
                actions = actions,
                apiKey = apiKey.trim(),
                models = models,
                onFetched = { fetchedModels = it },
                onModelsChange = { models = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        actions.onAddCustomAccount(
                            id,
                            nickname,
                            apiKey,
                            models.lines().map { it.trim() }.filter { it.isNotEmpty() },
                        )
                        nickname = ""
                        apiKey = ""
                        expanded = false
                    },
                    enabled = apiKey.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("添加账号")
                }
                TextButton(
                    text = "取消",
                    onClick = { expanded = false },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        fetchedModels?.let { fetched ->
            ModelPickerSheet(
                fetched = fetched,
                currentModels = models,
                onDismiss = { fetchedModels = null },
                onConfirm = { selected ->
                    // 远端已下架的本地旧模型不再保留；有移除时提示数量，否则沿用原文案
                    val previous = models.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    val removed = previous.count { it !in fetched }
                    models = selected.sorted().joinToString("\n")
                    fetchedModels = null
                    actions.onNotice(
                        if (removed == 0) {
                            "已选择 ${selected.size} 个模型"
                        } else {
                            "已选择 ${selected.size} 个模型，移除 $removed 个远端已下架的模型"
                        },
                    )
                },
            )
        }
    }

    /** 模型管理：云端拉取 + 手动编辑。拉模型优先用填写的 key，否则用第一个已有账号的；成功后弹层勾选。 */
    @Composable
    private fun ModelsSection(
        accounts: List<AccountStatus>,
        actions: ProviderUiActions,
        apiKey: String,
        models: String,
        onFetched: (List<String>) -> Unit,
        onModelsChange: (String) -> Unit,
    ) {
        var loading by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "模型",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(enabled = !loading) {
                    val baseUrl = actions.customBaseUrlOf(id)
                    if (baseUrl.isEmpty()) {
                        actions.onNotice("先保存供应商的接口地址，再拉取模型")
                        return@clickable
                    }
                    val key = apiKey.ifEmpty {
                        accounts.firstOrNull()?.let { actions.accountApiKeyOf(id, it.uid) }.orEmpty()
                    }
                    if (key.isEmpty()) {
                        actions.onNotice("先填写 API Key 再拉取模型")
                        return@clickable
                    }
                    loading = true
                    actions.onFetchCustomModels(baseUrl, key) { fetched, error ->
                        loading = false
                        if (error.isNotEmpty()) {
                            actions.onNotice(error)
                        } else {
                            onFetched(fetched)
                        }
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Download,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = if (loading) "拉取中..." else "拉取模型",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        }
        TextField(
            value = models,
            onValueChange = onModelsChange,
            label = "一行一个模型 id（随账号一起保存）",
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    /** 拉取成功后的模型勾选面板：搜索过滤、整行点选，保存时把选中项写回模型输入框。 */
    @Composable
    private fun ModelPickerSheet(
        fetched: List<String>,
        currentModels: String,
        onDismiss: () -> Unit,
        onConfirm: (List<String>) -> Unit,
    ) {
        // 以云端结果为准建键：本地已保存且仍在云端的才预勾选，远端已下架的不会被保留
        val selected = remember(fetched) {
            val previous = currentModels.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            mutableStateMapOf<String, Boolean>().apply {
                fetched.forEach { put(it, previous.contains(it)) }
            }
        }
        var search by remember { mutableStateOf("") }
        val selectedCount = selected.values.count { it }
        val filtered = remember(fetched, search) { fetched.filter { it.contains(search, ignoreCase = true) } }

        WindowBottomSheet(
            show = true,
            title = "选择模型",
            onDismissRequest = onDismiss,
        ) {
            Column(Modifier.fillMaxWidth()) {
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    label = "搜索模型",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "已选 $selectedCount 个",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                ) {
                    if (filtered.isEmpty()) {
                        item { EmptyHint("没有匹配的模型", "换个关键词试试") }
                    } else {
                        items(filtered, key = { it }) { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected[model] = !(selected[model] ?: false) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = model,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Checkbox(
                                    state = if (selected[model] == true) {
                                        androidx.compose.ui.state.ToggleableState.On
                                    } else {
                                        androidx.compose.ui.state.ToggleableState.Off
                                    },
                                    onClick = { selected[model] = !(selected[model] ?: false) },
                                )
                            }
                        }
                    }
                }
                TextDivider()
                Button(
                    onClick = { onConfirm(selected.filterValues { it }.keys.sorted()) },
                    enabled = selectedCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text("保存所选 $selectedCount 个")
                }
            }
        }
    }

    /** 账号卡片：点击展开详情（名称编辑、启用开关、删除），展开收起带高度过渡。 */
    @Composable
    private fun AccountDetailCard(account: AccountStatus, actions: ProviderUiActions) {
        var expanded by rememberSaveable(account.uid) { mutableStateOf(false) }
        SectionCard(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = account.nickname.ifEmpty { account.uid },
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = app.ai.rune.ziyong.transfer.ui.accountStatusText(account),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
                Icon(
                    imageVector = if (expanded) MiuixIcons.Regular.ExpandLess else MiuixIcons.Regular.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (expanded) {
                AccountDetailBody(account, actions)
            }
        }
    }

    @Composable
    private fun AccountDetailBody(account: AccountStatus, actions: ProviderUiActions) {
        var nickname by remember(account.uid, account.nickname) { mutableStateOf(account.nickname) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = "名称",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = "保存名称",
                onClick = { actions.onRenameAccount(id, account.uid, nickname) },
                modifier = Modifier.fillMaxWidth(),
            )

            TextDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "启用",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                AccountEnableSwitch(account = account, providerId = id, actions = actions)
            }

            Text(
                text = "额度 ${if (account.creditsKnown) account.credits.toString() else "—"}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
            )
            if (account.detail.isNotEmpty()) {
                Text(
                    text = account.detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            AccountDeleteRow(onRemove = { actions.onRemoveAccount(id, account.uid) })
        }
    }
}
