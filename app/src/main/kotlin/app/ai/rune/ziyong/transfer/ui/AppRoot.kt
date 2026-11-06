package app.ai.rune.ziyong.transfer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.providers.ProviderUiRegistry
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState

/** 页面切换时长：与 miuix 顶栏收起动画的节奏对齐，避免两层动效互相打架。 */
private const val NAV_ANIM_MILLIS = 260

/**
 * 应用根容器：底部 5 个 Tab + 二级页面栈。
 *
 * 层级关系（返回键的语义即由此决定）：
 * - 二级页存在时，返回键先回到它的**上级**页面（额度明细 → 额度中心 → 首页）；
 * - 没有二级页时，返回键交给系统退出应用。
 *
 * 二级页用 [AnimatedContent] 做推入/推出，而不是引入整套导航库：
 * 本应用的层级只有「主 Tab + 一层二级页（最多两层返回关系）」，
 * 引入 Navigation Compose 反而要为 5 个 Tab 和 13 个页面各写一套 route，
 * 增益只有「深链接」——而本应用没有任何对外深链接需求。
 */
@Composable
fun AppRoot(viewModel: AppViewModel, state: AppUiState) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var subPage by rememberSaveable(stateSaver = SubPageSaver) { mutableStateOf<SubPage?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        if (state.notice.isNotEmpty()) {
            snackbarHostState.showSnackbar(state.notice)
            viewModel.consumeNotice()
        }
    }
    LaunchedEffect(state.busy) {
        if (state.busy.isNotEmpty()) snackbarHostState.showSnackbar(state.busy)
    }

    // 系统返回与页内返回按钮同层级：二级页回它的上级，其余回主 tab。
    BackHandler(enabled = subPage != null) {
        subPage = parentOf(subPage)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(state = snackbarHostState) },
        bottomBar = {
            // 二级页占满屏幕时不显示底部导航，避免用户在深层页面误触切 Tab。
            if (subPage == null) {
                NavigationBar(mode = NavigationBarDisplayMode.IconWithSelectedLabel) {
                    MainTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = {
                                tab = item
                                // 模型页首次进入时才拉目录：供应商多时首次拉取耗时明显，
                                // 不该让冷启动就付这笔开销。
                                if (item == MainTab.Models) viewModel.ensureModelsLoaded()
                            },
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = if (subPage == null) innerPadding.calculateBottomPadding() else 0.dp),
        ) {
            AnimatedContent(
                targetState = subPage,
                transitionSpec = {
                    val forward = targetState != null
                    val distance = { full: Int -> full / 6 }
                    if (forward) {
                        (slideInHorizontally(tween(NAV_ANIM_MILLIS), distance) + fadeIn(tween(NAV_ANIM_MILLIS))) togetherWith
                            fadeOut(tween(NAV_ANIM_MILLIS))
                    } else {
                        fadeIn(tween(NAV_ANIM_MILLIS)) togetherWith
                            (slideOutHorizontally(tween(NAV_ANIM_MILLIS)) { it / 6 } + fadeOut(tween(NAV_ANIM_MILLIS)))
                    }
                },
                label = "subPage",
            ) { page ->
                when (page) {
                    is SubPage.ProviderDetail -> ProviderDetailScreen(
                        providerId = page.providerId,
                        state = state,
                        viewModel = viewModel,
                        onBack = { subPage = null },
                        onEditCustom = {
                            subPage = SubPage.CustomProviderEdit(it.removePrefix(ProviderUiRegistry.CUSTOM_PREFIX))
                        },
                    )

                    is SubPage.CustomProviderEdit -> CustomProviderScreen(
                        existing = state.customProviders.firstOrNull { it.key == page.key },
                        onSave = {
                            viewModel.saveCustomProvider(it)
                            subPage = SubPage.ProviderDetail(it.providerId)
                        },
                        onDelete = {
                            viewModel.removeCustomProvider(it)
                            subPage = null
                        },
                        onBack = {
                            subPage = page.key?.let {
                                SubPage.ProviderDetail(ProviderUiRegistry.CUSTOM_PREFIX + it)
                            }
                        },
                    )

                    SubPage.Usage -> UsageScreen(state, viewModel) { subPage = null }
                    SubPage.TaskCenter -> TaskCenterScreen(
                        state = state,
                        viewModel = viewModel,
                        onOpenProviders = {
                            // 空态里的「去供应商页」：切回供应商 tab，并关掉这个二级页
                            tab = MainTab.Providers
                            subPage = null
                        },
                        onBack = { subPage = null },
                    )
                    SubPage.CreditCenter -> CreditCenterScreen(
                        state = state,
                        onOpen = { subPage = SubPage.ProviderCredits(it) },
                        onBack = { subPage = null },
                    )
                    is SubPage.ProviderCredits -> ProviderCreditsScreen(
                        providerId = page.providerId,
                        state = state,
                        viewModel = viewModel,
                        onBack = { subPage = SubPage.CreditCenter },
                    )
                    SubPage.KeepAlive -> KeepAliveScreen(state, viewModel) { subPage = null }
                    SubPage.Proxy -> ProxyScreen(state, viewModel) { subPage = null }
                    SubPage.ApiSettings -> ApiSettingsScreen(state, viewModel) { subPage = null }
                    SubPage.SecurityProtection -> SecurityProtectionScreen(state, viewModel) { subPage = null }
                    SubPage.Resilience -> ResilienceScreen(state, viewModel) { subPage = null }
                    SubPage.AppearanceTheme -> AppearanceThemeScreen(state, viewModel) { subPage = null }
                    SubPage.AccountTransfer -> AccountTransferScreen(state, viewModel) { subPage = null }
                    null -> MainTabs(
                        viewModel = viewModel,
                        state = state,
                        tab = tab,
                        onSubPage = { subPage = it },
                    )
                }
            }
        }
    }

    UpdatePromptDialog(state = state, viewModel = viewModel)
}

/**
 * 二级页的「上级」判定，集中在一处。
 *
 * 之前这段逻辑散落在 BackHandler 里以 `when (val page = subPage)` 的形式内联，
 * 新增带层级的页面时容易只改一半（系统返回能用、页内返回按钮失效）。
 */
private fun parentOf(page: SubPage?): SubPage? = when (page) {
    is SubPage.ProviderCredits -> SubPage.CreditCenter
    is SubPage.CustomProviderEdit ->
        page.key?.let { SubPage.ProviderDetail(ProviderUiRegistry.CUSTOM_PREFIX + it) }
    else -> null
}

/** 主 Tab 页面：切换时按索引方向做小幅滑移 + 淡入淡出。 */
@Composable
private fun MainTabs(
    viewModel: AppViewModel,
    state: AppUiState,
    tab: MainTab,
    onSubPage: (SubPage?) -> Unit,
) {
    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val offset = { full: Int -> if (forward) full / 12 else -full / 12 }
            (slideInHorizontally(tween(180), offset) + fadeIn(tween(180))) togetherWith
                (slideOutHorizontally(tween(180)) { -it / 12 } + fadeOut(tween(180)))
        },
        label = "tab",
    ) { page ->
        when (page) {
            MainTab.Home -> HomeScreen(
                state = state,
                viewModel = viewModel,
                openTaskCenter = { onSubPage(SubPage.TaskCenter) },
            )
            MainTab.Providers -> ProviderListScreen(
                state = state,
                onOpen = { onSubPage(SubPage.ProviderDetail(it)) },
                onAddCustom = { onSubPage(SubPage.CustomProviderEdit(null)) },
            )
            MainTab.Models -> ModelsScreen(state, viewModel)
            MainTab.Logs -> LogsScreen(state, viewModel)
            MainTab.Me -> MeScreen(state, viewModel, open = { onSubPage(it) })
        }
    }
}
