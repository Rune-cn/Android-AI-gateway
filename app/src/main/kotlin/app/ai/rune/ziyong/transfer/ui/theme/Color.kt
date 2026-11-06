package app.ai.rune.ziyong.transfer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 语义色板。
 *
 * 这里只放「与主题无关、跨明暗恒定」的语义色；成对的前景/背景色一律从
 * `MiuixTheme.colorScheme` 取（onSurface / surface / error …），不要在页面里硬编码，
 * 否则深色模式下必然出现看不清的灰字。
 */

/** 默认强调色：MIUI 品牌蓝。 */
val MiuixBlue = Color(0xFF3482FF)

/** 强调色的深一档，用于浅色底上的文字/图标。 */
val MiuixBlueDeep = Color(0xFF1F6BE0)

/** 告警琥珀：客户端中断的调用记录与日志 WARN 级别共用（非错误态，区别于 error 红）。 */
val WarningAmber = Color(0xFFC97F0A)

/** 成功绿：状态正常、额度充足。 */
val SuccessGreen = Color(0xFF10B981)

/** 近黑深色：趋势图「输出」序列的线条与图例等需要压深的前景。 */
val DeepInk = Color(0xFF0D0D0D)
