package app.ai.rune.ziyong.transfer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiActions
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiRegistry
import dev.aigw.core.pool.AccountStatus
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 供应商详情：登录入口 + 账号列表 + 专属设置。
 *
 * 账号卡片里的「专属信息区」由该供应商自己的 UI 组件渲染（[ProviderUiRegistry]），
 * 所以这里不需要认识设备指纹、签到、兑换码这些概念。
 */
@Composable
fun ProviderDetailScreen(
    providerId: String,
    state: AppUiState,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onEditCustom: (String) -> Unit,
) {
    val ui = remember(providerId) { ProviderUiRegistry.require(providerId) }
    val info = state.providerOf(providerId)
    val accounts = state.accountsOf(providerId)

    // 缓存回调集合：30+ 个 lambda 每次重组重建会让账号卡片永远无法跳过重组。
    // creditPacks/taskLists 随 state 变化，用 rememberUpdatedState 读取最新值。
    val creditPacks = rememberUpdatedState(state.creditPacks)
    val taskLists = rememberUpdatedState(state.taskLists)
    val pointsBills = rememberUpdatedState(state.pointsBills)
    val actions = remember(viewModel, providerId) {
        ProviderUiActions(
            // 登录一律交给系统浏览器：本机 WebView 渲染异常（整页缩放错乱、输入框文字镜像），
            // 网关会在本地临时监听回调端口接住登录结果。
            onWebLogin = { id, region -> viewModel.beginBrowserLogin(id, region) },
            onDeviceLogin = { id, region -> viewModel.beginDeviceLogin(id, region) },
            onSmsLogin = { id, phone, code, ccode, onResult ->
                if (code.isEmpty()) {
                    viewModel.sendSmsCode(id, phone, ccode, onResult)
                } else {
                    viewModel.smsLogin(id, phone, code, ccode, onResult)
                }
            },
            onImport = { id, raw -> viewModel.importCredentials(id, raw) },
            onAddCustomAccount = { id, nickname, apiKey, models ->
                viewModel.addCustomAccount(id, nickname, apiKey, models)
            },
            onRenameAccount = { id, uid, nickname -> viewModel.renameAccount(id, uid, nickname) },
            accountApiKeyOf = { id, uid -> viewModel.accountApiKey(id, uid) },
            customModelsOf = { id -> viewModel.customModels(id) },
            customBaseUrlOf = { id -> viewModel.customBaseUrl(id) },
            onFetchCustomModels = { baseUrl, apiKey, onResult ->
                viewModel.fetchCustomModels(baseUrl, apiKey, onResult)
            },
            onRefreshCredits = { id, uid -> viewModel.refreshCredits(id, uid) },
            onAction = { id, uid, action, payload -> viewModel.performAction(id, uid, action, payload) },
            onToggleAccount = { id, uid, enabled -> viewModel.setAccountEnabled(id, uid, enabled) },
            onRemoveAccount = { id, uid -> viewModel.removeAccount(id, uid) },
            onUpdateSecret = { id, uid, secret, notice ->
                viewModel.updateAccountSecret(id, uid, secret)
                viewModel.notice(notice)
            },
            deviceIdOf = { id, uid -> viewModel.deviceIdOf(id, uid) },
            regionOf = { id, uid -> viewModel.accountRegion(id, uid) },
            providerOptionOf = { id, key, fallback -> viewModel.providerOption(id, key, fallback) },
            onUpdateProviderOption = { id, key, value -> viewModel.updateProviderOption(id, key, value) },
            creditPacksOf = { id, uid -> creditPacks.value["$id/$uid"].orEmpty() },
            onLoadCreditPacks = { id, uid -> viewModel.loadCreditPacks(id, uid) },
            taskListOf = { id, uid -> taskLists.value["$id/$uid"] },
            onLoadTaskList = { id, uid -> viewModel.loadTaskList(id, uid) },
            pointsBillsOf = { id, uid -> pointsBills.value["$id/$uid"] },
            onLoadPointsBills = { id, uid -> viewModel.loadPointsBills(id, uid) },
            onRegenerateDeviceId = { id, uid -> viewModel.regenerateDeviceId(id, uid) },
            onSetDeviceId = { id, uid, value -> viewModel.setDeviceId(id, uid, value) },
            onNotice = { viewModel.notice(it) },
            onMoveAccountPriority = { id, uid, up -> viewModel.moveAccountPriority(id, uid, up) },
        )
    }

    PageScaffold(
        title = info?.displayName ?: providerId,
        subtitle = "${accounts.size} 个账号 · 可用 ${accounts.count { it.usable }}",
        onBack = onBack,
        actions = {
            if (providerId.startsWith(ProviderUiRegistry.CUSTOM_PREFIX)) {
                IconButton(onClick = { onEditCustom(providerId) }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Edit,
                        contentDescription = "编辑供应商",
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) {
        // LoginEntry 自带卡片，这里不再包一层，避免双层卡片
        ui.LoginEntry(accounts, actions)

        // 账号列表由供应商组件自管（如 WorkBuddy 按国内外分区）时，不再重复渲染通用账号卡
        if (accounts.isNotEmpty() && !ui.managesOwnAccounts) {
            SectionCard {
                SectionHeader(
                    title = "账号 · ${accounts.size} 个",
                    action = "刷新额度",
                    onAction = { actions.onRefreshCredits(providerId, null) },
                )
                PagedList(accounts, keyOf = { it.uid }) { _, account ->
                    AccountRow(account, ui, actions)
                }
            }
        }

        ui.SettingsSection(viewModel.providerSettings(providerId)) { updated ->
            viewModel.updateProviderSettings(providerId, updated)
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountStatus,
    ui: app.ai.rune.ziyong.transfer.ui.providers.ProviderUi,
    actions: ProviderUiActions,
) {
    AccountCardShell(
        account = account,
        providerId = account.providerId,
        actions = actions,
        modifier = Modifier.fillMaxWidth(),
        extra = { ui.AccountExtra(account, actions) },
        footer = {
            AccountDeleteRow(onRemove = { actions.onRemoveAccount(account.providerId, account.uid) })
        },
    )
}
