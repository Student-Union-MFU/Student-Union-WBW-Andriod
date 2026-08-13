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
 * Tuned toward forest rather than toward neutral: the surfaces carry a trace of moss and
 * bark instead of grey, the light card is birch parchment rather than white, and the
 * accent is a leaf rather than a metal.
 *
 * Two rules hold the set together:
 *
 *  1. **One ground, both themes.** The backdrop image is shown as supplied and the
 *     theme never re-tints it — an earlier attempt to wash it lighter for light mode
 *     simply erased the artwork. What the theme changes is what sits *on* the ground:
 *     cards and their text. Which means the ground is dark either way, and text placed
 *     straight on it uses [WbwColors.onBackdrop], not [WbwColors.textPrimary].
 *  2. **The accent is tuned, not flipped.** It keeps its hue in both themes and only
 *     moves in lightness, because the light version is unreadable on a near-white card
 *     and the dark one is invisible on a dark card. Same colour, correct exposure.
 */

// ===== Brand =====

/**
 * The accent: new leaf against old forest.
 *
 * Gold came from the event branding and never sat right here — a warm metallic on a
 * cold green ground reads as brass laid on moss, and at the size an accent is actually
 * used (8sp section labels, a tab icon) the warmth just looked dirty.
 *
 * A literal dark green was tried and rejected on evidence: at #2F6B4A the section labels
 * in Settings sank into the panel and stopped functioning as an accent at all. An accent
 * on a dark ground has to be LIGHTER than the ground — that is not a style preference,
 * it is the only way it can be seen. So: green, as intended, but the green of a new leaf
 * rather than of the canopy behind it.
 */
val WbwAccentDark = Color(0xFF7FCB9B)   // on dark grounds
val WbwAccentLight = Color(0xFF2F7A52)  // on light grounds — same hue, dropped in lightness

/** The softer accent, for headings and de-emphasised marks. */
val WbwAccentSoftDark = Color(0xFFBFE3CC)
val WbwAccentSoftLight = Color(0xFF4C9670)

/**
 * Status green — "on", checked in, progress.
 *
 * Deliberately a step deeper than the accent above rather than a different hue: they are
 * the same family, so a selected control and a completed thing read as related, and the
 * accent still sits on top because it is the lighter of the two.
 */
val WbwGreenDark = Color(0xFF63B283)
val WbwGreenLight = Color(0xFF35784F)

/** Fixed scene backdrop for anything that has to sit behind the image itself. */
val WbwForestVoid = Color(0xFF0B140E)

// ===== Surfaces — dark =====

val WbwBgDark = Color(0xFF0F1610)       // flat screens with no backdrop
val WbwSurfaceDark = Color(0xFF1A2318)  // cards, chat bubbles, fields
val WbwInkDark = Color(0xFFE9EEE0)      // primary text
val WbwMutedDark = Color(0xFFA2AF98)    // secondary text
val WbwLineDark = Color(0xFF2F3B2B)     // hairlines

// ===== Surfaces — light =====
//
// Warm off-whites carrying a trace of the same green, not pure white. Pure white on a
// forest ground reads as a hole punched in the page.

val WbwBgLight = Color(0xFFEDF0E5)
val WbwSurfaceLight = Color(0xFFF5F4E9)
val WbwInkLight = Color(0xFF1B2A1B)
val WbwMutedLight = Color(0xFF58684F)
val WbwLineLight = Color(0xFFDBDFCB)

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
// Named for the colour they used to be. Kept only because screens outside this package
// still import them; new code should read `wbwColors.accent`.
val Gold = WbwAccentDark
val GoldLight = WbwAccentDark
val GoldDark = WbwAccentLight
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
