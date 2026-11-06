package app.ai.rune.ziyong.transfer.ui

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 底部导航的 Tab 定义。
 *
 * 图标刻意统一用 miuix 官方图标集（而非 Material Icons）：它们的线条粗细与
 * NavigationBar 的选中态缩放遵循同一套设计规范，混用会让选中图标看起来忽粗忽细。
 */
enum class MainTab(val label: String, val icon: ImageVector) {
    Home("首页", MiuixIcons.Regular.Home),
    Providers("供应商", MiuixIcons.Regular.Layers),
    Models("模型", MiuixIcons.Regular.ListView),
    Logs("记录", MiuixIcons.Regular.Notes),
    Me("我的", MiuixIcons.Regular.Settings),
}
