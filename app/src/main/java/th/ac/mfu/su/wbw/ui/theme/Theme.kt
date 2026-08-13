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
 */
data class WbwColors(
    val isDark: Boolean,
    val gold: Color,
    val goldSoft: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val glass: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val skyStops: List<Color>,
    val danger: Color,
)

val LightWbwColors = WbwColors(
    isDark = false,
    gold = GoldDark,
    goldSoft = Gold,
    textPrimary = DeepText,
    textMuted = DeepMuted,
    glass = GlassLight,
    glassBorder = GlassLightBorder,
    glassHighlight = GlassLightHighlight,
    skyStops = listOf(DaySky1, DaySky2, DaySky3, DaySky4),
    danger = Color(0xFFB5462F),
)

val DarkWbwColors = WbwColors(
    isDark = true,
    gold = GoldLight,
    goldSoft = Gold,
    textPrimary = CreamText,
    textMuted = CreamMuted,
    glass = GlassDark,
    glassBorder = GlassDarkBorder,
    glassHighlight = GlassDarkHighlight,
    skyStops = listOf(NightSky1, NightSky2, NightSky3, NightSky4, NightSky5),
    danger = Danger,
)

val LocalWbwColors = staticCompositionLocalOf { DarkWbwColors }

/** Convenient accessor: `wbwColors.gold`, `wbwColors.glass`, … */
val wbwColors: WbwColors
    @Composable @ReadOnlyComposable get() = LocalWbwColors.current

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Cream,
    secondary = Leaf,
    tertiary = GoldDark,
    background = DaySky3,
    onBackground = DeepText,
    surface = Cream,
    onSurface = DeepText,
    error = Color(0xFFB5462F),
)

private val DarkColors = darkColorScheme(
    primary = GoldLight,
    onPrimary = Ink,
    secondary = Leaf,
    tertiary = Gold,
    background = NightSky5,
    onBackground = CreamText,
    surface = DeepForest,
    onSurface = CreamText,
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
