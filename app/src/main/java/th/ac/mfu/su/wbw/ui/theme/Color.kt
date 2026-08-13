package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Walk Beyond the Wild" palette.
 *
 * Source of truth is the iOS app (`wbw-ios-fontend/WBW/Config.swift`, the `Color`
 * extension). This file previously tracked the *website* palette, which is a
 * different design: its gold was #DDA15E and its ink #22271F, so the screens that
 * exist in both apps rendered in visibly different colours.
 *
 * iOS splits its palette in two, and so do we:
 *
 *  - Brand colours are FIXED in both themes. Gold and green are the identity of
 *    the event, not surface treatment — flipping them per theme makes the app
 *    look like a different product in dark mode.
 *  - Surface colours ADAPT. iOS builds those with `UIColor(dynamicProvider:)`;
 *    the Compose equivalent is choosing the pair in [WbwColors] (see Theme.kt).
 *
 * Legacy names (Forest, Deep, Body, the sky stops …) are kept because screens
 * outside this package reference them directly; where iOS has a counterpart the
 * VALUE now points at iOS, so the restyle propagates without touching call sites.
 */

// ===== Brand — identical in light and dark, straight from iOS =====

/** iOS `wbwGold` #C99A1F. The accent: active tabs, links, primary buttons. */
val WbwGold = Color(0xFFC99A1F)

/** iOS `wbwCream` #DEC684. The soft gold — headings and de-emphasised accents. */
val WbwCream = Color(0xFFDEC684)

/** iOS `wbwGreen` #40916C. Forest green, used for "on" states. */
val WbwGreen = Color(0xFF40916C)

/** iOS `wbwForestVoid` #0A1610. Flat backdrop that replaced the 3D forest scene. */
val WbwForestVoid = Color(0xFF0A1610)

/** iOS `wbwTicketBG` #1A1A1A. The ticket screen is a fixed design, not a surface. */
val WbwTicketBg = Color(0xFF1A1A1A)

/** iOS `wbwMedical` #421717. Oxblood tint behind the Medical ID button. */
val WbwMedical = Color(0xFF421717)

// ===== Surfaces — the adaptive pairs (light, dark) =====

/** iOS `wbwBg` — the screen behind everything. */
val WbwBgLight = Color(0xFFFAF7F0)
val WbwBgDark = Color(0xFF14120F)

/** iOS `wbwSurface` — cards, chat bubbles, text fields. */
val WbwSurfaceLight = Color(0xFFFFFFFF)
val WbwSurfaceDark = Color(0xFF211F1B)

/** iOS `wbwInk` — primary text and dark strokes. The widest-reaching token. */
val WbwInkLight = Color(0xFF2B2B2B)
val WbwInkDark = Color(0xFFEFEBE3)

/** iOS `wbwMuted` — secondary text. */
val WbwMutedLight = Color(0xFF8F8A80)
val WbwMutedDark = Color(0xFFA8A196)

/** iOS `wbwLine` — hairline dividers. */
val WbwLineLight = Color(0xFFECE6DA)
val WbwLineDark = Color(0xFF332F29)

// ===== Legacy names, repointed at iOS =====
//
// Kept as aliases so the ~30 call sites outside this package keep compiling.
// Prefer the Wbw* names above in new code.

val Cream = WbwBgLight               // was #FAF7F0 — already matched iOS wbwBg
val Gold = WbwCream                  // the "soft gold" role → iOS cream #DEC684
val GoldLight = WbwGold              // iOS gold is one value in both themes …
val GoldDark = WbwGold               // … so both ends collapse onto #C99A1F
val Leaf = WbwGreen                  // already #40916C — the one exact match
val Line = WbwLineLight

val CreamText = WbwInkDark           // ink-on-dark
val CreamMuted = WbwMutedDark
val DeepText = WbwInkLight           // ink-on-light
val DeepMuted = WbwMutedLight

/** iOS has no red of its own — `StaffScanView` uses the system red. */
val Danger = Color(0xFFE08A8A)

// Structural greens with no iOS counterpart. iOS carries its green in exactly one
// token (wbwGreen); these extra shades exist only for this app's own chrome, so
// they are left as-is rather than invented against iOS.
val Forest = Color(0xFF2D6A4F)
val Deep = Color(0xFF1B4332)
val DeepForest = Color(0xFF173D2C)
val LeafLight = Color(0xFF4FA77D)
val Ink = Color(0xFF22271F)          // a dark SURFACE here, unlike iOS wbwInk (text)
val Body = WbwInkDark

// ===== Glass =====
//
// iOS uses the real iOS 26 `.glassEffect` (see GlassSurface.swift); Android has no
// equivalent, so these approximate it with a near-solid surface. The values now
// come from iOS `wbwSurface`/`wbwLine` instead of the website's card tokens.
//
// Kept fully opaque to match iOS, where a card sits on a flat background rather
// than a moving one. If the animated sky survives, these want alpha back.

val GlassDark = WbwSurfaceDark
val GlassDarkBorder = WbwLineDark
val GlassDarkHighlight = Color(0x21FFFFFF)

val GlassLight = WbwSurfaceLight
val GlassLightBorder = WbwLineLight
val GlassLightHighlight = Color(0xCCFFFFFF)

// ===== Sky gradient =====
//
// NOT an iOS design — iOS draws a flat background (wbwBg) and has its 3D forest
// switched off. Whether this animated sky survives the visual match is still an
// open decision, so the stops are left exactly as they were.

val NightSky1 = Color(0xFF3A6D84)
val NightSky2 = Color(0xFF24506A)
val NightSky3 = Color(0xFF173A3A)
val NightSky4 = Color(0xFF0F2A1E)
val NightSky5 = Color(0xFF0A1C14)

val DaySky1 = Color(0xFFBFE2F0)
val DaySky2 = Color(0xFFD7ECDF)
val DaySky3 = Color(0xFFC3DDC0)
val DaySky4 = Color(0xFF9FC4A2)
