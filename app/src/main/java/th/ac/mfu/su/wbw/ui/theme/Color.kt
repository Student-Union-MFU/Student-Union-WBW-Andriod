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
 * There is no accent hue. The "accent" is simply the text colour.
 *
 * Gold, then leaf green, were both tried and both lost to the same problem: the screen
 * already has a photograph and a set of glass panes doing the visual work, and a
 * highlight colour on top of those is a fourth thing asking for attention. It made every
 * screen busier without making anything clearer.
 *
 * So emphasis is carried the way the participant pass carries it — by weight, size and
 * letterspacing against a single ink. A label reads as a label because it is 8sp,
 * uppercase and widely tracked, not because it is a different colour from its neighbour.
 *
 * The token survives so call sites keep their meaning ("this is emphasised") and so a
 * hue can be reintroduced in one place if it is ever wanted again.
 */
val WbwAccentDark = Color(0xFFE9EEE0)   // = WbwInkDark
val WbwAccentLight = Color(0xFF1B2A1B)  // = WbwInkLight

/** The de-emphasised version, for eyebrows and secondary marks. */
val WbwAccentSoftDark = Color(0xFFA2AF98)
val WbwAccentSoftLight = Color(0xFF58684F)

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
val GlassDarkHighlight = Color(0x1FFFFFFF)

val GlassLight = WbwSurfaceLight
val GlassLightHighlight = Color(0xCCFFFFFF)

/**
 * Pane edges, as light as they can be while still closing the shape.
 *
 * These used to be the solid `WbwLine*` values — fine on an opaque card, wrong on glass:
 * an edge with its own colour reads as an outline *drawn around* the pane, which turns a
 * floating surface into a sticker. A few percent of white (or black on light) catches the
 * edge instead of describing it, and lets the shape be defined by the refraction.
 */
val GlassDarkBorder = Color(0x1AFFFFFF)
val GlassLightBorder = Color(0x12000000)

/**
 * The sheer pane: barely there, and identical in both themes.
 *
 * The themed [GlassLight]/[GlassDark] fills exist so a *card* can be light in light mode
 * — but a light card is by definition nearly opaque here, because the only thing behind
 * it is a dark photograph. Wherever the glass itself is the point rather than the card,
 * that trade is the wrong way round, and this is used instead: the backdrop stays
 * visible and the content on it commits to light ink in both themes, exactly as the
 * participant pass does.
 *
 * One constant rather than a copy per screen — the pass and the chat each carried their
 * own ~0x1F white, which is two chances to drift apart.
 */
val GlassSheer = Color(0x1FFFFFFF)

/**
 * The edge that goes with [GlassSheer].
 *
 * White, like the pane, and pitched against the backdrop rather than against the theme —
 * [GlassLightBorder] is a black hairline, which is correct on a pale card and invisible on
 * a sheer one over a dark photograph.
 *
 * One token because the nav bar, the header chips and the event cards all draw this same
 * edge, and they were each carrying their own `Color.White.copy(alpha = 0.13f)`.
 */
val GlassSheerBorder = Color(0x21FFFFFF)

/**
 * No pane at all: refraction and a hairline, nothing painted on top.
 *
 * [GlassSheer]'s 12% white is still a wash — enough to lighten a large surface into a
 * pale rectangle even though the backdrop shows through it. This paints nothing, so the
 * shape is defined purely by how the glass bends what is behind it and by its edge.
 *
 * Only usable where the content can survive on the raw backdrop, since there is no
 * ground being added underneath it.
 */
val GlassClear = Color.Transparent

// A green-tinted pane lived here for a while: first `TicketGreen` at 30%, then the deeper
// `DeepForest`. Both were wrong in the same way and it took seeing them on the device to
// say why. The backdrop is *already* green. Tinting a pane green on a green ground either
// brightens it (emerald, the loudest thing on screen) or darkens it (a hole), and neither
// is what glass does — glass takes its colour from what is behind it. [GlassSheer] over
// this artwork comes out green on its own, which is the nav bar's whole trick.

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
