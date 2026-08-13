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
 * the accent pair, the frosted-glass panel tokens, the backdrop tokens, and the
 * forest-sky gradient stops. Read it anywhere via [wbwColors].
 *
 * [accent] is the app's one highlight colour — a leaf green. It was gold until the
 * palette was re-derived from the backdrop; the token was renamed off "gold" at the same
 * time so the name cannot outlive the colour it describes.
 */
data class WbwColors(
    val isDark: Boolean,
    val accent: Color,
    val accentSoft: Color,
    val green: Color,
    val textPrimary: Color,
    val textMuted: Color,
    /**
     * Text drawn straight onto the backdrop rather than onto a card.
     *
     * Light in both themes, unlike [textPrimary]. The two sit on different grounds: a
     * card follows the theme, the backdrop does not — it is one dark image either way.
     * Using [textPrimary] here is what makes a title vanish in light mode.
     */
    val onBackdrop: Color,
    val onBackdropMuted: Color,
    /**
     * Painted over the backdrop image to settle it under the content.
     *
     * Deliberately near-nothing. The backdrop is the supplied artwork and the app's
     * strongest visual; anything heavy enough to visibly re-tint it is destroying the
     * thing it is meant to support.
     */
    val backdropWash: Color,
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
    accent = WbwAccentLight,
    accentSoft = WbwAccentSoftLight,
    green = WbwGreenLight,
    textPrimary = WbwInkLight,
    textMuted = WbwMutedLight,
    // Light in BOTH themes: the backdrop is one dark image and stays dark, so text on
    // it cannot follow the card palette. This is the one place the two themes agree.
    onBackdrop = WbwInkDark,
    onBackdropMuted = WbwMutedDark,
    // Transparent. An earlier version washed the image white here to fake a daylight
    // version of it — which flattened the artwork into near-grey and lost the reason
    // for having a backdrop at all. The image is the design; it is shown, not tinted.
    backdropWash = Color.Transparent,
    background = WbwBgLight,
    glass = GlassLight,
    glassBorder = GlassLightBorder,
    glassHighlight = GlassLightHighlight,
    line = WbwLineLight,
    forestVoid = WbwForestVoid,
    skyStops = listOf(DaySky1, DaySky2, DaySky3, DaySky4),
    danger = DangerLight,
)

val DarkWbwColors = WbwColors(
    isDark = true,
    accent = WbwAccentDark,
    accentSoft = WbwAccentSoftDark,
    green = WbwGreenDark,
    textPrimary = WbwInkDark,
    textMuted = WbwMutedDark,
    onBackdrop = WbwInkDark,
    onBackdropMuted = WbwMutedDark,
    // Barely there: just enough to settle the image under the cards and keep the accent
    // the brightest thing on screen.
    backdropWash = Color.Black.copy(alpha = 0.12f),
    background = WbwBgDark,
    glass = GlassDark,
    glassBorder = GlassDarkBorder,
    glassHighlight = GlassDarkHighlight,
    line = WbwLineDark,
    forestVoid = WbwForestVoid,
    skyStops = listOf(NightSky1, NightSky2, NightSky3, NightSky4, NightSky5),
    danger = DangerDark,
)

val LocalWbwColors = staticCompositionLocalOf { DarkWbwColors }

/** Convenient accessor: `wbwColors.accent`, `wbwColors.glass`, … */
val wbwColors: WbwColors
    @Composable @ReadOnlyComposable get() = LocalWbwColors.current

// Material's own scheme, kept in step with the tokens above so that any stock Material
// component (dialogs, snackbars, ripples) lands on the same palette as the hand-styled
// screens.
private val LightColors = lightColorScheme(
    primary = WbwAccentLight,
    onPrimary = TicketCreamPaper,
    secondary = WbwGreenLight,
    tertiary = WbwAccentSoftLight,
    background = WbwBgLight,
    onBackground = WbwInkLight,
    surface = WbwSurfaceLight,
    onSurface = WbwInkLight,
    outline = WbwLineLight,
    error = DangerLight,
)

private val DarkColors = darkColorScheme(
    primary = WbwAccentDark,
    onPrimary = WbwInkLight,
    secondary = WbwGreenDark,
    tertiary = WbwAccentSoftDark,
    background = WbwBgDark,
    onBackground = WbwInkDark,
    surface = WbwSurfaceDark,
    onSurface = WbwInkDark,
    outline = WbwLineDark,
    error = DangerDark,
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
