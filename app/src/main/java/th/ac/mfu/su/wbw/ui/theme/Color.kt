package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Walk Beyond the Wild" palette.
 *
 * Derived from the backdrop rather than from another product. Sampling `bg.jpg` gives
 * a desaturated forest green — hue 110–146 (centre ~130), saturation 15–22%, luminance
 * 16–38%. Every surface below is built on that hue, so cards read as cut from the same
 * material as the ground behind them instead of being dropped onto it.
 *
 * The two palettes this replaced both failed for the same reason: they were designed
 * for a different ground. The website palette assumed a mesh gradient; the iOS palette
 * assumed a bright cream photograph (`wbwBg` #FAF7F0). Against a dark green backdrop
 * their warm neutrals read as grey and their mustard gold went muddy.
 *
 * Two rules hold the set together:
 *
 *  1. **One ground, two exposures.** The backdrop is a single image, so light mode
 *     cannot swap it — it washes it (see [WbwColors.backdropWash]). Light mode is that
 *     image in daylight; dark mode is the same image at dusk. That is what stops the
 *     app looking like two unrelated designs depending on the theme.
 *  2. **Gold is tuned, not flipped.** It stays the same hue in both themes and only
 *     moves in lightness, because bright gold on a near-white card is unreadable and
 *     deep gold on a dark card is invisible. Same colour, correct exposure.
 */

// ===== Brand =====

/** Event gold. Hue ~38 — far enough from the backdrop's green to read as an accent. */
val WbwGoldDark = Color(0xFFE2A63C)   // on dark grounds
val WbwGoldLight = Color(0xFFB07C16)  // on light grounds — same hue, dropped in lightness

/** The soft, sandy gold used for headings and de-emphasised accents. */
val WbwGoldSoftDark = Color(0xFFF2DCA8)
val WbwGoldSoftLight = Color(0xFFC08F2E)

/** Leaf green for "on" states and progress. Lifted well clear of the backdrop's range. */
val WbwGreenDark = Color(0xFF6FBF8B)
val WbwGreenLight = Color(0xFF35835A)

/** Fixed scene backdrop for anything that has to sit behind the image itself. */
val WbwForestVoid = Color(0xFF0B140E)

// ===== Surfaces — dark =====

val WbwBgDark = Color(0xFF101711)       // flat screens with no backdrop
val WbwSurfaceDark = Color(0xFF18211A)  // cards, chat bubbles, fields
val WbwInkDark = Color(0xFFE7EEE3)      // primary text
val WbwMutedDark = Color(0xFF9EAE9C)    // secondary text
val WbwLineDark = Color(0xFF2C3A2D)     // hairlines

// ===== Surfaces — light =====
//
// Warm off-whites carrying a trace of the same green, not pure white. Pure white on a
// forest ground reads as a hole punched in the page.

val WbwBgLight = Color(0xFFEEF1E8)
val WbwSurfaceLight = Color(0xFFF8FAF4)
val WbwInkLight = Color(0xFF1C2A1E)
val WbwMutedLight = Color(0xFF5A6A59)
val WbwLineLight = Color(0xFFDDE3D6)

// ===== Fixed-design colours =====
//
// The Participant Pass is a printed-ticket design, not a themed surface — it looks the
// same in both modes on purpose, exactly as iOS pins its ticket screen. These are the
// pieces of it that the rest of the palette has to stay in step with.

val TicketDeep = Color(0xFF14301F)
val TicketGreen = Color(0xFF2D6A4F)
val TicketCreamPaper = Color(0xFFFBF8EF)

/** Oxblood behind the Medical ID button (iOS `wbwMedical`). */
val WbwMedical = Color(0xFF421717)

// ===== Status =====

val DangerDark = Color(0xFFE88B7A)
val DangerLight = Color(0xFFC0503A)

// ===== Legacy names =====
//
// Screens outside this package reference these directly. Kept as aliases pointing at
// the new set so the restyle propagates without touching call sites; prefer the tokens
// on [WbwColors] in new code.

val Cream = WbwBgLight
val Gold = WbwGoldDark
val GoldLight = WbwGoldDark
val GoldDark = WbwGoldLight
val Leaf = WbwGreenDark
val LeafLight = Color(0xFF8FD3A6)
val Line = WbwLineLight
val Forest = TicketGreen
val Deep = TicketDeep
val DeepForest = Color(0xFF102A1B)
val Ink = WbwSurfaceDark          // a dark SURFACE here, unlike wbwInk (text)
val Body = WbwInkDark
val Danger = DangerDark
val CreamText = WbwInkDark
val CreamMuted = WbwMutedDark
val DeepText = WbwInkLight
val DeepMuted = WbwMutedLight

// ===== Glass =====
//
// Android has no real Liquid Glass, so the frosted panes approximate it with a surface
// plus a hairline. Values come from the surface/line tokens above so a card and a
// glass panel are the same material.

val GlassDark = WbwSurfaceDark
val GlassDarkBorder = WbwLineDark
val GlassDarkHighlight = Color(0x1FFFFFFF)

val GlassLight = WbwSurfaceLight
val GlassLightBorder = WbwLineLight
val GlassLightHighlight = Color(0xCCFFFFFF)

// ===== Sky gradient =====
//
// Only used by [ProceduralSkyBackground], which the app no longer draws. Re-anchored on
// the same green so it does not clash if it is ever switched back on.

val NightSky1 = Color(0xFF2C4A44)
val NightSky2 = Color(0xFF223B34)
val NightSky3 = Color(0xFF1A2E26)
val NightSky4 = Color(0xFF13221A)
val NightSky5 = Color(0xFF0D1812)

val DaySky1 = Color(0xFFCBDFD0)
val DaySky2 = Color(0xFFBCD5C0)
val DaySky3 = Color(0xFFA9C6AE)
val DaySky4 = Color(0xFF8FB196)
