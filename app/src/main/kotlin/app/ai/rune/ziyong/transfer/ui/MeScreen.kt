package app.ai.rune.ziyong.transfer.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.ThemeMode
import app.ai.rune.ziyong.transfer.ui.theme.formatHex
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.foundation.clickable
import top.yukonga.miuix.kmp.basic.ButtonDefaults

/**
 * 「我的」页：设置入口的集合。
 *
 * 采用 miuix 的分组卡片 + 行内右侧元信息布局：每行只承担一个跳转，
 * 右侧展示该设置**当前的真实值**（如端口号、脱敏开关、账号数），
 * 避免用户必须点进去才知道现在是什么状态。
 */
@Composable
fun MeScreen(state: AppUiState, viewModel: AppViewModel, open: (SubPage) -> Unit) {
    val context = LocalContext.current
    // 点顶栏标题弹出作者信息
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    PageScaffold(
        title = "我的",
        subtitle = "网关与偏好设置",
    ) {
        // 顶部事实陈述：全部取自可核实的运行状态
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (state.running) "本地网关 · 运行中" else "本地网关 · 已停止",
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "监听 :${state.port} · 供应商 ${state.providers.size}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                StatusChip(state.running)
            }
        }

        SmallTitle("外观风格")
        SectionCard {
            SettingSwitchRow(
                title = "动态取色",
                subtitle = "跟随系统壁纸生成配色（Android 12 及以上）",
                checked = state.dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
            )
            TextDivider()
            SettingRow(
                title = "外观主题",
                subtitle = "明暗模式与强调色",
                meta = appearanceMeta(state),
                onClick = { open(SubPage.AppearanceTheme) },
            )
        }

        SmallTitle("核心功能")
        SectionCard {
            SettingRow(
                title = "接入设置",
                subtitle = "端口与 API Key",
                meta = ":${state.port}",
                onClick = { open(SubPage.ApiSettings) },
            )
            TextDivider()
            SettingRow(
                title = "安全防护",
                subtitle = "反审核脱敏与账号限速",
                meta = if (state.security.sanitizeEnabled) "脱敏开启" else "脱敏关闭",
                onClick = { open(SubPage.SecurityProtection) },
            )
            TextDivider()
            SettingRow(
                title = "并发与容错",
                subtitle = "并发闸门、重试退避与熔断器",
                meta = concurrencySummary(state.failover),
                onClick = { open(SubPage.Resilience) },
            )
            TextDivider()
            SettingRow(
                title = "额度中心",
                subtitle = "按供应商查看额度包明细",
                meta = if (state.pool.creditsKnown > 0) formatCount(state.pool.totalCredits) else "",
                onClick = { open(SubPage.CreditCenter) },
            )
            TextDivider()
            SettingRow(
                title = "用量统计",
                subtitle = "请求数、token 与成功率",
                meta = formatTokens(state.today.totalTokens),
                onClick = { open(SubPage.Usage) },
            )
            TextDivider()
            SettingRow(
                title = "代理设置",
                subtitle = "境外供应商需要走代理",
                meta = if (state.proxy.usable) "已启用" else "未启用",
                onClick = { open(SubPage.Proxy) },
            )
            TextDivider()
            SettingRow(
                title = "保活与权限",
                subtitle = "通知、电池优化与后台限制",
                meta = if (state.keepAlive?.allGood == true) "就绪" else "待配置",
                onClick = { open(SubPage.KeepAlive) },
            )
            TextDivider()
            SettingSwitchRow(
                title = "实时状态通知",
                subtitle = "仅网关有请求时显示，点击回到 App",
                checked = state.liveNotification,
                onCheckedChange = { viewModel.setLiveNotification(it) },
            )
            TextDivider()
            SettingRow(
                title = "账号导入导出",
                subtitle = "按供应商与账号备份、迁移凭证",
                meta = "${state.pool.total} 个账号",
                onClick = { open(SubPage.AccountTransfer) },
            )
        }

        SmallTitle("关于")
        SectionCard {
            SettingRow(
                title = "应用版本",
                subtitle = appVersion(context),
                meta = "",
                onClick = { showAbout = true },
            )
            TextDivider()
            SettingRow(
                title = "检查更新",
                subtitle = "从 GitHub Releases 获取最新版本",
                meta = if (state.updateChecking) "检查中…" else "",
                onClick = { if (!state.updateChecking) viewModel.checkUpdate() },
            )
            TextDivider()
            SettingSwitchRow(
                title = "自动检查更新",
                subtitle = "打开应用时后台检测，每天最多一次",
                checked = state.autoCheckUpdate,
                onCheckedChange = { viewModel.setAutoCheckUpdate(it) },
            )
            TextDivider()
            SettingRow(
                title = "项目地址",
                subtitle = "在浏览器打开源码仓库",
                meta = "GitHub",
                onClick = { viewModel.openProjectRepo() },
            )
        }

        SectionCard {
            Text(
                text = "R 中转 · 本地 OpenAI 兼容网关",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "把多个上游供应商（Trae / Loomy / WorkBuddy / Antigravity / 自定义）" +
                    "统一成一个 OpenAI 兼容端点，供任意客户端调用。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 设置行：左侧标题 + 副标题，右侧当前值 + 箭头。
 *
 * [meta] 为空时不渲染右侧文字——空白占位会让整列看起来参差不齐。
 */
@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    meta: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MiuixTheme.textStyles.footnote1,
                fontFamily = FontFamily.Monospace,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = MiuixIcons.Regular.ChevronForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
    }
}

/** 设置开关行：与 [SettingRow] 同一栅格，右侧换成 Switch。 */
@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * 「并发与容错」入口的右侧元信息。
 *
 * 写成紧凑形式（不带空格）：这一行的标题是全页最长，元信息再用长文案会把标题挤到第二行。
 */
private fun concurrencySummary(failover: dev.aigw.core.failover.ModelFailoverSettings): String {
    val concurrency = failover.concurrency
    return when {
        concurrency.maxPerAccount == 0 && concurrency.maxGlobal == 0 -> "并发不限"
        else -> "单账号${concurrency.maxPerAccount}/全局${concurrency.maxGlobal}"
    }
}

/** 外观主题条目的元信息：当前模式 + 是否自定义强调色。 */
private fun appearanceMeta(state: AppUiState): String {
    val mode = when (state.themeMode) {
        ThemeMode.Light -> "浅色"
        ThemeMode.Dark -> "深色"
        ThemeMode.System -> "自动"
    }
    // 与默认强调色比对：一致时报「默认」，否则给出色值，便于判断是否被改过
    val custom = formatHex(state.goldStart) != "#3482FF" || formatHex(state.goldEnd) != "#1F6BE0"
    return mode + " · " + if (custom) formatHex(state.goldStart) else "默认"
}

private fun appVersion(context: Context): String = runCatching {
    val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
    val name = pkg.versionName.orEmpty()
    @Suppress("DEPRECATION")
    val code = pkg.versionCode
    "${name.ifEmpty { "—" }}（build $code）"
}.getOrDefault("—")

/** 点「应用版本」弹出的作者信息：两行居中。 */
@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    WindowDialog(
        show = true,
        title = "关于",
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "夕白",
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "3884021834",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = "关闭",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
