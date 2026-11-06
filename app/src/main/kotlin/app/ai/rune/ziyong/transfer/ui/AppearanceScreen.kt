package app.ai.rune.ziyong.transfer.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.ai.rune.ziyong.transfer.ui.theme.DeepInk
import app.ai.rune.ziyong.transfer.ui.theme.LocalAppColors
import app.ai.rune.ziyong.transfer.ui.theme.ThemeMode
import app.ai.rune.ziyong.transfer.ui.theme.formatHex
import app.ai.rune.ziyong.transfer.ui.theme.parseHexColor
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import top.yukonga.miuix.kmp.basic.ButtonDefaults

/** 页面底色预设：hex 为 null 表示不覆盖（用默认底色）。 */
private data class BgPreset(val label: String, val hex: String?)

private val bgPresets = listOf(
    BgPreset("默认", null),
    BgPreset("冷银灰", "#F7F7F8"),
    BgPreset("暖象牙", "#FBF9F5"),
    BgPreset("曜石黑", "#121214"),
    BgPreset("深空黑", "#0A0A0B"),
)

/** 色相环尺寸参数（统一在此定义，保证拖动命中与绘制一致）。 */
private val WheelSize = 110.dp
private val WheelStroke = 12.dp
private val WheelThumb = 9.dp

/**
 * 外观主题二级页：明暗模式、页面底色、强调色渐变。
 *
 * 明暗与动态取色由 MiuixTheme 接管；本页只负责把用户意图写回 AppViewModel，
 * 界面本身不需要重新计算配色——单一真相源在 Theme.kt。
 */
@Composable
fun AppearanceThemeScreen(state: AppUiState, viewModel: AppViewModel, onBack: () -> Unit) {
    PageScaffold(
        title = "外观主题",
        subtitle = "明暗模式 · 配色方案 · 强调色",
        onBack = onBack,
    ) {
        SectionCard {
            Text(
                text = "明暗模式",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            ModeRow(
                title = "自动",
                subtitle = "跟随 Android 系统的深浅色设置自适应切换",
                selected = state.themeMode == ThemeMode.System,
            ) { viewModel.setThemeMode(ThemeMode.System) }
            TextDivider()
            ModeRow(
                title = "深色模式",
                subtitle = "适合弱光与长时夜间使用",
                selected = state.themeMode == ThemeMode.Dark,
            ) { viewModel.setThemeMode(ThemeMode.Dark) }
            TextDivider()
            ModeRow(
                title = "浅色模式",
                subtitle = "高对比度明亮排版",
                selected = state.themeMode == ThemeMode.Light,
            ) { viewModel.setThemeMode(ThemeMode.Light) }
        }

        SectionCard {
            Text(
                text = "页面底色",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "切换后仅替换页面底色（卡片/表面色不变）：",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val currentHex = state.backgroundOverride?.let { formatHex(it) }
                bgPresets.forEach { preset ->
                    PresetChip(
                        label = preset.label,
                        dotColor = preset.hex?.let { parseHexColor(it) } ?: MiuixBlue(),
                        selected = currentHex == preset.hex,
                        onClick = { viewModel.setBackgroundOverride(preset.hex) },
                    )
                }
            }
            HexField(
                value = state.backgroundOverride?.let { formatHex(it) } ?: "",
                label = "自定义页面底色",
                onCommit = { viewModel.setBackgroundOverride(it) },
                onClear = if (state.backgroundOverride != null) {
                    { viewModel.setBackgroundOverride(null) }
                } else {
                    null
                },
                swatch = state.backgroundOverride,
            )
        }

        SectionCard {
            Text(
                text = "强调色渐变",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "应用主色渐变由起始色与结束色插值生成，可拖动色相环或直接输入色值：",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WheelColumn(
                    title = "起始色",
                    color = state.goldStart,
                    modifier = Modifier.weight(1f),
                    onPick = { viewModel.setGoldColors(formatHex(it), formatHex(state.goldEnd)) },
                )
                WheelColumn(
                    title = "结束色",
                    color = state.goldEnd,
                    modifier = Modifier.weight(1f),
                    onPick = { viewModel.setGoldColors(formatHex(state.goldStart), formatHex(it)) },
                )
            }
            GradientPreview(state.goldStart, state.goldEnd)
        }

        TextButton(
            text = "重置默认",
            onClick = { viewModel.resetAppearance() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                textColor = MiuixTheme.colorScheme.error,
            ),
        )
    }
}

/** 取当前主题的强调色，供未设置底色时作为预设色点。 */
@Composable
private fun MiuixBlue(): Color = LocalAppColors.current.goldStart

/** 单选行：选中 = 主色文字 + 主色对勾；未选中 = 普通文字 + 空心圈。 */
@Composable
private fun ModeRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.subtitle,
                color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Ok,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MiuixTheme.colorScheme.dividerLine, CircleShape),
                )
            }
        }
    }
}

/** 配色预设胶囊：色点 + 名称；选中时主色描边 + 浅底。 */
@Composable
private fun PresetChip(label: String, dotColor: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.primaryContainer
                } else {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = if (selected) {
                MiuixTheme.colorScheme.onPrimaryContainer
            } else {
                MiuixTheme.colorScheme.onSurface
            },
        )
    }
}

/** 等宽 HEX 输入框：满 6 位合法即提交；可选色彩示意块与「清除」入口。 */
@Composable
private fun HexField(
    value: String,
    label: String,
    onCommit: (String) -> Unit,
    onClear: (() -> Unit)? = null,
    swatch: Color? = null,
) {
    var text by remember(value) { mutableStateOf(value.removePrefix("#").uppercase()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (swatch != null) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(swatch),
                )
            }
            TextField(
                value = text,
                onValueChange = { input ->
                    val cleaned = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                    text = cleaned
                    if (cleaned.length == 6 && parseHexColor(cleaned) != null) onCommit("#$cleaned")
                },
                label = label,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
            )
        }
        if (onClear != null) {
            TextButton(
                text = "清除覆盖",
                onClick = onClear,
                minWidth = 0.dp,
            )
        }
    }
}

/** 单个色相环卡片：标题 + 色相环 + HEX 输入框。 */
@Composable
private fun WheelColumn(
    title: String,
    color: Color,
    modifier: Modifier,
    onPick: (Color) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            Text(
                text = title,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${hueOf(color)}°",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
        HueWheel(color = color, onPick = onPick)
        HexField(value = formatHex(color), label = "HEX", onCommit = { onPick(requireColor(it)) })
    }
}

/** 色相环：角度映射色相 H、半径比例映射饱和度 S，明度固定 1；可拖动或点击取色。 */
@Composable
private fun HueWheel(color: Color, onPick: (Color) -> Unit) {
    val (hue, saturation) = hueSatOf(color)
    val hueColors = remember { List(13) { Color.hsv(it * 30f, 1f, 1f) } }
    // 手势块只在 key 变化时重启，若不缓存最新 lambda 就会一直用首次组合时的 onPick；
    // 两个色轮的 onPick 都捕获了对方的当前颜色，冻结后会「拖一端把另一端写回旧值」。
    val currentOnPick by rememberUpdatedState(onPick)
    Canvas(
        modifier = Modifier
            .size(WheelSize)
            .pointerInput(Unit) {
                detectTapGestures { offset -> currentOnPick(colorAt(offset, size.width, size.height, density)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnPick(colorAt(change.position, size.width, size.height, density))
                }
            },
    ) {
        val stroke = WheelStroke.toPx()
        val thumbR = WheelThumb.toPx()
        val ringR = (minOf(size.width, size.height) / 2f - thumbR - stroke / 2f).coerceAtLeast(1f)
        val center = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            brush = Brush.sweepGradient(hueColors),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - ringR, center.y - ringR),
            size = Size(ringR * 2f, ringR * 2f),
            style = Stroke(width = stroke),
        )
        val angleRad = Math.toRadians(hue.toDouble())
        val dist = saturation.coerceIn(0f, 1f) * ringR
        val thumb = Offset(
            center.x + (dist * cos(angleRad)).toFloat(),
            center.y + (dist * sin(angleRad)).toFloat(),
        )
        drawCircle(Color.White, thumbR, thumb)
        drawCircle(DeepInk, thumbR, thumb, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color, thumbR * 0.45f, thumb)
    }
}

/** START→END 渐变预览条 + 两端代码标注 + 渐变文字实测。 */
@Composable
private fun GradientPreview(start: Color, end: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(start, end))),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "START ${formatHex(start)}",
                style = MiuixTheme.textStyles.footnote2,
                fontFamily = FontFamily.Monospace,
                color = start,
            )
            Text(
                text = "END ${formatHex(end)}",
                style = MiuixTheme.textStyles.footnote2,
                fontFamily = FontFamily.Monospace,
                color = end,
            )
        }
        // 渐变文字用 Canvas 之外的常规 Text + brush 无法在 miuix Text 上直接设置，
        // 这里改用一行胶囊底 + 两端色块，同样能看出组合效果，且不依赖 Material3 的 TextStyle 能力。
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(start),
            )
            Text(
                text = "R 中转 · 强调色预览",
                style = MiuixTheme.textStyles.subtitle.copy(
                    brush = Brush.horizontalGradient(listOf(start, end)),
                ),
            )
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(end),
            )
        }
    }
}

/** 触点 → 颜色：角度映射 H，半径比例映射 S，V 固定 1。 */
private fun colorAt(offset: Offset, width: Int, height: Int, density: Float): Color {
    val cx = width / 2f
    val cy = height / 2f
    val dx = offset.x - cx
    val dy = offset.y - cy
    val thumbR = WheelThumb.value * density
    val stroke = WheelStroke.value * density
    val ringR = (minOf(width, height) / 2f - thumbR - stroke / 2f).coerceAtLeast(1f)
    val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()) + 360f) % 360f
    val saturation = (hypot(dx, dy) / ringR).coerceIn(0f, 1f)
    return Color.hsv(angle, saturation, 1f)
}

/** 取颜色的 HSV 色相与饱和度（0..360 / 0..1）。 */
private fun hueSatOf(color: Color): Pair<Float, Float> {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return hsv[0] to hsv[1]
}

private fun hueOf(color: Color): Int = hueSatOf(color).first.toInt()

/** 已校验过的 HEX → 颜色；由 [parseHexColor] 保证非空。 */
private fun requireColor(hex: String): Color = parseHexColor(hex) ?: Color.Unspecified
