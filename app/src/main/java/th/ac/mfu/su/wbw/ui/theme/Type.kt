package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R

/**
 * One typeface for the whole app: Sarabun for text, Kanit for numerals.
 *
 * This used to run two voices at once. Headings were Itim — a handwritten face
 * inherited from the su-wbw-website type system — while body and UI were Sarabun. The
 * participant pass never opted into the handwritten voice (it sets size and weight
 * directly, so it fell through to Sarabun), and seeing the two side by side made it
 * obvious the app looked like two products: an editorial panel under a handwritten
 * title.
 *
 * Hierarchy is carried by **weight and size**, never by switching face. That is the
 * same rule the pass follows — one colour, one family, difference from weight — and it
 * is why the pass reads as designed rather than assembled.
 *
 * Itim and Patrick Hand are still in `res/font` if the handwritten voice is ever wanted
 * back for a specific piece; nothing references them now.
 */

/** Numerals and accent figures — bib numbers, big counts. Kanit's digits are squarer. */
val Kanit = FontFamily(
    Font(R.font.kanit_medium, FontWeight.Medium),
    Font(R.font.kanit_semibold, FontWeight.SemiBold),
    Font(R.font.kanit_bold, FontWeight.Bold),
)

/** Everything else, Thai and Latin. */
val Sarabun = FontFamily(
    Font(R.font.sarabun_regular, FontWeight.Normal),
    Font(R.font.sarabun_semibold, FontWeight.SemiBold),
    Font(R.font.sarabun_bold, FontWeight.Bold),
)

val WbwTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 13.sp),
)
