package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R

/**
 * Two faces, both light: **Athiti** for everything written, **Quicksand** for everything
 * counted.
 *
 * **Why Athiti.** The brief was type that reads thin and long, and "long" is a *width*
 * property, not a weight one — a thin stroke on a wide geometric face is still wide. So the
 * candidates were measured rather than eyeballed: mean advance width over real UI strings,
 * normalised to the em, in both scripts.
 *
 * ```
 *                    latin    thai
 *   Prompt-Light     0.535    0.474
 *   Athiti-Light     0.445    0.419   <- 17% / 12% narrower
 *   Fahkwang-Light   0.542    0.528   <- wider than Prompt, despite the name
 * ```
 *
 * Athiti wins the measurement and keeps every property Prompt was chosen for: Cadson Demak,
 * full Thai, loopless, geometric skeleton. Semi-condensed humanist sans is what "tall and
 * narrow" actually names.
 *
 * **Quicksand for the numerals** — circular bowls, soft terminals, true-circle zeros. Chakra
 * Petch was tried and rejected for reading cyberpunk: squared counters and flat terminals are
 * that vocabulary exactly, and this app is a forest and a flower opening. Mitr lost for
 * keeping a flat hard bar on the 5.
 *
 * **Quicksand carries no Thai, and that is safe only because of how it is used.** It goes on
 * digits and nothing else — bib, group, student id, all numeric by construction on the
 * server. Put it on a word and a Thai device falls back to Noto mid-line. A call site that
 * needs letters wants [BodyFace].
 *
 * **Weights: Light, Regular and Medium.** The scale used to stop at Regular, and the whole of
 * it then moved up exactly one step — ExtraLight to Light for the display sizes, Light to
 * Regular for the titles, Regular to Medium for body and labels. The face was doing what it
 * was picked to do and the app still read faint: Athiti is semi-condensed, so its stems are
 * not only light but narrow, and a narrow light stem on a photograph loses from both sides at
 * once. One step is the whole correction — the voice is meant to stay airy, and the difference
 * between 400 and 500 is felt rather than seen, which is the point.
 *
 * There is still deliberately no SemiBold or Bold registered in either family — not merely
 * unused but *absent*, so a stray `FontWeight.Bold` at some future call site resolves down to
 * Medium rather than quietly reintroducing a heavy voice. Hierarchy still comes from size,
 * letterspacing and ink strength, which is the rule this design system has held since the Itim
 * experiment; the step up moves the whole scale together and so changes no contrast within it.
 *
 * **Small tracked labels** — 8.5–11sp, uppercase, tracked to 1.6–3sp — take the step too, and
 * need it most: at that size a Regular stem was already near the floor of what antialiasing
 * leaves behind, which is why they were held above Light to begin with.
 *
 * **Quicksand needs no new file.** Its numerals sit at Light, and one step up is Regular, which
 * already maps to `quicksand_medium`. Only Athiti gained a face.
 */

/** Numerals only — bib, group, student id. Round, soft. Digits, never words. */
val Numerals = FontFamily(
    Font(R.font.quicksand_light, FontWeight.Light),
    Font(R.font.quicksand_medium, FontWeight.Normal),
)

/** Everything else, Thai and Latin. Named for its role — this is the third face to hold it. */
val BodyFace = FontFamily(
    Font(R.font.athiti_extralight, FontWeight.ExtraLight),
    Font(R.font.athiti_light, FontWeight.Light),
    Font(R.font.athiti_regular, FontWeight.Normal),
    Font(R.font.athiti_medium, FontWeight.Medium),
)

/**
 * Sizes step up a little from the old scale, because Athiti is narrower and sets smaller on
 * the page at the same point size; line heights stay loose because Thai stacks up to two
 * marks above the x-height on one consonant, and on tight metrics those clip the descenders
 * of the line above wherever a Thai string wraps.
 */
val WbwTypography = Typography(
    displaySmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Light, fontSize = 36.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Light, fontSize = 29.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BodyFace, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 15.sp),
)
