package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Walk Beyond the Wild" palette — a forest-trail system.
 * Names mirror the design brief (cream / forest / deep / leaf / gold / ink).
 */

// Core brand
val Cream = Color(0xFFFAF7F0)
val Forest = Color(0xFF2D6A4F)
val Deep = Color(0xFF1B4332)
val DeepForest = Color(0xFF173D2C)
val Leaf = Color(0xFF40916C)
val LeafLight = Color(0xFF4FA77D)
val Gold = Color(0xFFDDA15E)
val GoldLight = Color(0xFFE2B078)
val GoldDark = Color(0xFFC9883F)
val Ink = Color(0xFF22271F)
val Body = Color(0xFFE7E4DC)
val Danger = Color(0xFFE08A8A)

// Night-forest sky gradient (dark theme background)
val NightSky1 = Color(0xFF3A6D84)
val NightSky2 = Color(0xFF24506A)
val NightSky3 = Color(0xFF173A3A)
val NightSky4 = Color(0xFF0F2A1E)
val NightSky5 = Color(0xFF0A1C14)

// Daytime sky gradient (light theme background)
val DaySky1 = Color(0xFFBFE2F0)
val DaySky2 = Color(0xFFD7ECDF)
val DaySky3 = Color(0xFFC3DDC0)
val DaySky4 = Color(0xFF9FC4A2)

// Text
val CreamText = Color(0xFFFAF7F0)
val CreamMuted = Color(0x9EFAF7F0) // ~.62 alpha
val DeepText = Color(0xFF1B4332)
val DeepMuted = Color(0xFF3D5C4B)

val Line = Color(0xFFE6E1D6)              // hairline on light surfaces

// Glass — dark (near-solid ink card floating over the forest, matches website bg-ink/82)
val GlassDark = Color(0xD122271F)         // ink @ 82%
val GlassDarkBorder = Color(0x26FAF7F0)   // cream/15
val GlassDarkHighlight = Color(0x21FFFFFF)

// Light surfaces — solid card on cream, thin line border (matches website bg-card #fff)
val GlassLight = Color(0xF7FFFFFF)        // white @ 97%
val GlassLightBorder = Line
val GlassLightHighlight = Color(0xCCFFFFFF)
