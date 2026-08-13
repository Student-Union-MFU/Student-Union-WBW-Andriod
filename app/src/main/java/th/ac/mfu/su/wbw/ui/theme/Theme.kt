package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** User-selectable theme mode. AUTO follows the system (a proxy for the event's day cycle). */
enum class ThemeMode { LIGHT, DARK, AUTO }

/**
 * Extended palette that Material's [androidx.compose.material3.ColorScheme] can't hold:
 * gold accents, the frosted-glass panel tokens, and the forest-sky gradient stops.
 * Read it anywhere via [wbwColors].
 *
 * The token set mirrors the iOS `Color` extension in `Config.swift` one-for-one, so a
 * screen ported from SwiftUI can be translated token-by-token instead of by eye:
 *
 *   iOS wbwBg → [background]   wbwSurface → [glass]   wbwInk → [textPrimary]
 *   wbwMuted  → [textMuted]    wbwLine    → [line]    wbwGold → [gold]
 *   wbwCream  → [goldSoft]     wbwGreen   → [green]
 */
data class WbwColors(
    val isDark: Boolean,
    val gold: Color,
    val goldSoft: Color,
    val green: Color,
    val textPrimary: Color,
    val textMuted: Color,
    /** iOS `wbwBg` — the flat screen background. */
    val background: Color,
    /** iOS `wbwSurface`. Named "glass" for continuity with existing call sites. */
    val glass: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    /** iOS `wbwLine` — hairline dividers. */
    val line: Color,
    /** iOS `wbwForestVoid` — fixed in both themes; a scene backdrop, not a surface. */
    val forestVoid: Color,
    val skyStops: List<Color>,
    val danger: Color,
)

val LightWbwColors = WbwColors(
    isDark = false,
    gold = WbwGold,
    goldSoft = WbwCream,
    green = WbwGreen,
    textPrimary = WbwInkLight,
    textMuted = WbwMutedLight,
    background = WbwBgLight,
    glass = GlassLight,
    glassBorder = GlassLightBorder,
    glassHighlight = GlassLightHighlight,
    line = WbwLineLight,
    forestVoid = WbwForestVoid,
    skyStops = listOf(DaySky1, DaySky2, DaySky3, DaySky4),
    danger = Color(0xFFB5462F),
)

val DarkWbwColors = WbwColors(
    isDark = true,
    gold = WbwGold,
    goldSoft = WbwCream,
    green = WbwGreen,
    textPrimary = WbwInkDark,
    textMuted = WbwMutedDark,
    background = WbwBgDark,
    glass = GlassDark,
    glassBorder = GlassDarkBorder,
    glassHighlight = GlassDarkHighlight,
    line = WbwLineDark,
    forestVoid = WbwForestVoid,
    skyStops = listOf(NightSky1, NightSky2, NightSky3, NightSky4, NightSky5),
    danger = Danger,
)

val LocalWbwColors = staticCompositionLocalOf { DarkWbwColors }

/** Convenient accessor: `wbwColors.gold`, `wbwColors.glass`, … */
val wbwColors: WbwColors
    @Composable @ReadOnlyComposable get() = LocalWbwColors.current

// Material's own scheme, kept in step with the iOS tokens above so that any stock
// Material component (dialogs, snackbars, ripples) lands on the same palette as the
// hand-styled screens. Gold is primary in BOTH themes — on iOS it is a brand colour
// that does not flip, so the previous Forest-in-light / gold-in-dark split is gone.
private val LightColors = lightColorScheme(
    primary = WbwGold,
    onPrimary = WbwInkLight,
    secondary = WbwGreen,
    tertiary = WbwCream,
    background = WbwBgLight,
    onBackground = WbwInkLight,
    surface = WbwSurfaceLight,
    onSurface = WbwInkLight,
    outline = WbwLineLight,
    error = Color(0xFFB5462F),
)

private val DarkColors = darkColorScheme(
    primary = WbwGold,
    onPrimary = WbwInkLight,
    secondary = WbwGreen,
    tertiary = WbwCream,
    background = WbwBgDark,
    onBackground = WbwInkDark,
    surface = WbwSurfaceDark,
    onSurface = WbwInkDark,
    outline = WbwLineDark,
    error = Danger,
)

@Composable
fun WbwTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val wbw = if (darkTheme) DarkWbwColors else LightWbwColors

    androidx.compose.runtime.CompositionLocalProvider(LocalWbwColors provides wbw) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = WbwTypography,
            content = content,
        )
    }
}
