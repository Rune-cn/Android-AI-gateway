package app.ai.rune.ziyong.transfer.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/** 供非 Composable 场景（如 Activity 首帧）查询当前明暗，避免窗口闪白。 */
private val LocalIsDark = staticCompositionLocalOf { false }

/** 当前是否深色主题。 */
val isDarkTheme: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalIsDark.current

/**
 * 应用主题：整套视觉由 MiuixTheme 承载。
 *
 * 设计要点
 * --------
 * 1. **单一真相源**：明暗与动态取色只在这里解析一次，其余代码一律读 `MiuixTheme.colorScheme`
 *    或 [LocalAppColors]，不允许页面里再判断 `isSystemInDarkTheme()`——两处判断必然在
 *    用户强制浅色/深色时打架。
 * 2. **窗口外观跟随主题**：状态栏/导航栏图标明暗由 [SystemBarAppearance] 同步，
 *    并在 `dark` 变化时把窗口底色钉住，`recreate()` 之后不会闪一帧浅色。
 * 3. **动态取色**：Android 12+ 且用户开启时用系统壁纸色；否则用固定 MIUI 蓝，
 *    以保证跨设备观感一致（动态取色在部分 OEM 上会给出很灰的色）。
 *
 * @param useDynamicColor 是否跟随系统壁纸取色（Android 12+ 生效）。
 * @param themeMode 明暗模式。
 */
@Composable
fun RTransferTheme(
    useDynamicColor: Boolean = false,
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // 必须 remember：ThemeController 内部持有可变状态，每次重组都新建会让颜色动画
    // 反复重置（表现为主题色「抖动」）。
    val colorSchemeMode = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> when (themeMode) {
            ThemeMode.System -> ColorSchemeMode.MonetSystem
            ThemeMode.Light -> ColorSchemeMode.MonetLight
            ThemeMode.Dark -> ColorSchemeMode.MonetDark
        }
        else -> when (themeMode) {
            ThemeMode.System -> ColorSchemeMode.System
            ThemeMode.Light -> ColorSchemeMode.Light
            ThemeMode.Dark -> ColorSchemeMode.Dark
        }
    }

    val controller = remember(colorSchemeMode, dark) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            // monet 模式下 keyColor 才是种子色；固定色模式下 Miuix 用自身默认蓝，
            // 传入同一品牌蓝可让两条路径的观感尽量靠拢。
            keyColor = MiuixBlue,
            paletteStyle = ThemePaletteStyle.TonalSpot,
            colorSpec = ThemeColorSpec.Spec2025,
            isDark = dark,
        )
    }

    CompositionLocalProvider(LocalIsDark provides dark) {
        MiuixTheme(controller = controller) {
            SystemBarAppearance(dark = dark)
            content()
        }
    }
}

/**
 * 同步系统栏外观，并在明暗切换时把窗口背景钉成当前 surface 色。
 *
 * 为什么需要「钉住窗口背景」：`recreate()`（例如切换语言/主题）之后，
 * 窗口会先用 XML 主题里的 `windowBackground` 画一帧再交给 Compose。
 * 若该色与当前主题不符，用户会看到明显闪白。这里在 dark 变化时
 * 把 Activity 窗口底色改成当前 surface，下一个窗口帧就是正确的颜色。
 */
@Composable
private fun SystemBarAppearance(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val window = (view.context as? Activity)?.window ?: return
    // 必须在 MiuixTheme 内部读取，才能拿到当前 colorScheme 的 surface。
    val surfaceArgb = MiuixTheme.colorScheme.surface.toArgb()
    val latestSurface = rememberUpdatedState(surfaceArgb)

    SideEffect {
        val insets = androidx.core.view.WindowCompat.getInsetsController(window, view)
        insets.isAppearanceLightStatusBars = !dark
        insets.isAppearanceLightNavigationBars = !dark
    }

    // 只按 dark 触发：若按 surface 触发，颜色动画每帧都会写盘/改窗口背景。
    LaunchedEffect(dark) {
        val argb = latestSurface.value
        window.setBackgroundDrawable(ColorDrawable(argb))
        SystemBarAppearanceStore.remember(dark, argb)
    }

    // Android 10+ 默认给导航栏加一层半透明遮罩，会盖住 Miuix 的 surface 色。
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        DisposableEffect(window) {
            window.isNavigationBarContrastEnforced = false
            onDispose { }
        }
    }
}

/**
 * 记录「上次解析好的窗口外观」，供下次冷启动在 Compose 之前使用。
 *
 * 只存两个值且生命周期与进程一致，用内存单例即可——这里没有跨进程或持久化需求，
 * 引入 DataStore 反而会让首帧等待磁盘 IO。
 */
object SystemBarAppearanceStore {
    @Volatile
    private var dark: Boolean = false

    @Volatile
    private var surfaceArgb: Int = Color(0xFFF7F7F7).toArgb()

    fun remember(dark: Boolean, surfaceArgb: Int) {
        this.dark = dark
        this.surfaceArgb = surfaceArgb
    }

    fun isDark(): Boolean = dark

    fun surfaceArgb(): Int = surfaceArgb
}
