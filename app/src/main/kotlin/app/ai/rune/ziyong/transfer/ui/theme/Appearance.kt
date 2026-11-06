package app.ai.rune.ziyong.transfer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

/** 明暗模式：跟随系统 / 强制深色 / 强制浅色。 */
enum class ThemeMode { Light, Dark, System }

/**
 * 用户可自定义的外观色。
 *
 * 只包含强调色的两端：其余语义色（绿/红/灰等）不随用户改变，
 * 避免自定义配色把错误态、成功态也染成同一颜色而失去语义。
 *
 * 说明：字段名沿用 `gold*` 是为了与既有存储键（appearance KEY_*）保持兼容——
 * 升级后用户已保存的自定义色仍能读出来，不会因重命名而被静默重置。
 * 双花括号里的注释说明了它在界面上的真实语义（强调色）。
 */
data class AppColors(
    val goldStart: Color,
    val goldEnd: Color,
) {
    /** 强调色文字：浅色底上需要更深一档才够对比度。 */
    val goldText: Color get() = goldEnd

    /**
     * 供应商区分色：首位跟随自定义强调色，其余为固定高区分度色。
     *
     * 供需要「按供应商着色」的地方使用（可用账号占比条、图例等）；按索引取用，
     * 故顺序即是稳定性的一部分，不要随手插色——否则历史图例的颜色会整体错位。
     */
    val providerPalette: List<Color> get() = listOf(
        goldStart,          // 强调色——首位供应商
        Color(0xFF3B82F6),  // 蓝
        Color(0xFF10B981),  // 绿
        Color(0xFF8B5CF6),  // 紫
        Color(0xFFF59E0B),  // 橙
        Color(0xFFEC4899),  // 粉
        Color(0xFF06B6D4),  // 青
    )
}

/** 默认强调色：MIUI 蓝。 */
val defaultAppColors = AppColors(MiuixBlue, MiuixBlueDeep)

/** 当前生效的外观色；由 `RTransferTheme` 提供，UI 里用 `LocalAppColors.current` 读取。 */
val LocalAppColors = staticCompositionLocalOf { defaultAppColors }

/**
 * 解析 `#RRGGBB` 或 `RRGGBB`（大小写不敏感）为不透明颜色；非法输入返回 null。
 */
fun parseHexColor(input: String): Color? {
    val raw = input.trim().removePrefix("#")
    if (raw.length != 6) return null
    val value = raw.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or value)
}

/** 输出 `#RRGGBB`（大写）。 */
fun formatHex(color: Color): String =
    String.format(Locale.US, "#%06X", color.toArgb() and 0xFFFFFF)
