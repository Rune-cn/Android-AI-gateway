package app.ai.rune.ziyong.transfer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.ai.rune.ziyong.transfer.data.KeepAlive
import app.ai.rune.ziyong.transfer.service.FloatingKeepAlive
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import app.ai.rune.ziyong.transfer.ui.theme.SuccessGreen

/**
 * 保活与权限：把网关在后台持续运行所需的系统条件逐项检测出来，并给出一键跳转。
 *
 * 锁屏后端口还能被客户端调通，需要几件事同时成立：通知权限、应用不在电池优化名单里、
 * 系统没有限制它后台运行。这些都没有 API 能直接代改，只能检测 + 引导。
 */
@Composable
fun KeepAliveScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val status = state.keepAlive

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    // 从系统授权页返回时（ON_RESUME）重检权限并补显示悬浮窗：
    // 用户去设置里授权后回到本页，开关无需重新点一遍。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reapplyOverlayIfEnabled()
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PageScaffold(
        title = "保活与权限",
        subtitle = "让网关在后台持续运行",
        onBack = onBack,
    ) {
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "保活状态",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (status?.allGood == true) "已就绪" else "有待处理",
                    style = MiuixTheme.textStyles.subtitle,
                    color = if (status?.allGood == true) SuccessGreen else MiuixTheme.colorScheme.error,
                )
            }
            Text(
                text = "通知权限 + 电池优化白名单 + 不被限制后台，这几项都满足后才能锁屏稳定可用。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            if (!state.running) {
                Text(
                    text = "当前服务未启动：先去首页启动，否则锁屏后没有进程监听端口。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                )
            }
        }

        SectionCard {
            SwitchRow(
                title = "开机自动恢复",
                subtitle = "手机重启或应用更新后自动拉起网关，按当前端口继续监听",
                checked = state.startOnBoot,
                onCheckedChange = { viewModel.setStartOnBoot(it) },
            )
            Text(
                text = "Android 15 起 specialUse 类型的前台服务允许直接从开机广播启动；该开关不影响本次运行。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        SectionCard {
            SwitchRow(
                title = "悬浮窗保活",
                subtitle = "在屏幕上留一个小悬浮标签，让系统把本应用视为可见进程，降低后台被回收的概率",
                checked = state.overlayKeepAlive,
                onCheckedChange = { viewModel.setOverlayKeepAlive(it) },
            )
            if (state.overlayKeepAlive && !state.running) {
                Text(
                    text = "网关未启动：悬浮窗将在服务启动后显示。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (state.overlayKeepAlive && status?.overlayGranted != true) {
                Text(
                    text = "缺少「显示在其他应用上层」权限，悬浮窗尚未显示。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.error,
                )
            }
            if (status?.overlayGranted != true) {
                TextButton(
                    text = "授予悬浮窗权限",
                    onClick = {
                        launch(context, FloatingKeepAlive.permissionIntent(context), "无法打开悬浮窗权限设置")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = "悬浮窗只影响系统的后台回收策略，不改变请求链路；关闭开关会立即移除悬浮窗。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        SectionCard {
            PermissionStatusRow(
                title = "通知权限",
                detail = if (status?.notificationsGranted == true) {
                    "已允许，常驻通知可见"
                } else {
                    "未允许，后台通知不会显示"
                },
                ok = status?.notificationsGranted == true,
                actionLabel = "去开启",
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    launch(context, KeepAlive.notificationSettingsIntent(context), "无法打开通知设置")
                }
            }
            PermissionStatusRow(
                title = "电池优化",
                detail = if (status?.batteryUnrestricted == true) {
                    "已加入白名单（不限制耗电）"
                } else {
                    "受系统电池优化限制，锁屏后可能被冻结、端口调不通"
                },
                ok = status?.batteryUnrestricted == true,
                actionLabel = "不限制",
            ) {
                // 部分 ROM 不支持直接请求，失败就退回到电池优化列表页
                if (!launch(context, KeepAlive.batteryOptimizationIntent(context), null)) {
                    launch(context, KeepAlive.batterySettingsIntent(), "无法打开电池优化设置")
                }
            }
            PermissionStatusRow(
                title = "后台运行限制",
                detail = if (status?.backgroundRestricted == true) {
                    "系统已限制本应用后台运行"
                } else {
                    "未被系统限制后台运行"
                },
                ok = status?.backgroundRestricted != true,
                actionLabel = "去设置",
            ) {
                launch(context, KeepAlive.appDetailsIntent(context), "无法打开应用详情")
            }
            PermissionStatusRow(
                title = "流量节省",
                detail = if (status?.dataSaverRestricted == true) {
                    "已开启流量节省，后台联网可能受限"
                } else {
                    "未开启流量节省"
                },
                ok = status?.dataSaverRestricted != true,
                actionLabel = "去设置",
            ) {
                launch(context, KeepAlive.appDetailsIntent(context), "无法打开应用详情")
            }
        }

        SectionCard {
            Text(
                text = "厂商后台管理",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "国产系统默认会限制第三方应用后台。下面的入口会尝试直接打开厂商的后台管理页；" +
                    "打不开就按提示手动设置。",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Button(
                onClick = {
                    val opened = KeepAlive.autoStartIntents().any { launch(context, it, null) }
                    if (!opened) {
                        launch(context, KeepAlive.appDetailsIntent(context), "无法打开系统页面")
                    }
                    Toast.makeText(context, KeepAlive.manualHint(Build.MANUFACTURER), Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text("打开厂商后台设置")
            }
            Text(
                text = KeepAlive.manualHint(Build.MANUFACTURER),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        Text(
            text = "说明：本应用用「常驻前台服务 + 通知」保持进程存活，并持有唤醒锁与 Wi-Fi 锁" +
                "（锁屏后 CPU/Wi-Fi 不被省电挂起，已有 TCP 连接不会断），也可开启「悬浮窗保活」进一步降低后台被回收的概率。" +
                "如果系统把本应用放进电池优化名单或限制后台，锁屏一段时间后进程仍可能被冻结，客户端就会连接失败——" +
                "按上面的检测项逐条处理即可。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 权限状态行：标题 + 状态说明 + 右侧动作按钮。不满足时说明文字变红。 */
@Composable
private fun PermissionStatusRow(
    title: String,
    detail: String,
    ok: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MiuixTheme.textStyles.footnote2,
                color = if (ok) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MiuixTheme.colorScheme.error
                },
            )
        }
        TextButton(
            text = actionLabel,
            onClick = onAction,
            minWidth = 0.dp,
        )
    }
}

/** 打开系统页面；失败时（部分 ROM 不支持该 Action）给 Toast 提示而不是静默失败。 */
private fun launch(context: Context, intent: Intent, failureMessage: String?): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: Exception) {
    if (failureMessage != null) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
    false
}
