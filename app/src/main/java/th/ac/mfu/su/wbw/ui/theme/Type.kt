package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R

/**
 * Matches the su-wbw-website type system:
 *  - Itim         — handwritten headings (Thai + Latin) — the app's display voice
 *  - Patrick Hand — handwritten Latin, for the English wordmark / hero
 *  - Sarabun      — body & UI (Thai + English)
 *  - Kanit        — numerals & accent (bib, big numbers)
 */

/** Handwritten display face (Thai + Latin). Use for headings across the app. */
val Hand = FontFamily(Font(R.font.itim, FontWeight.Normal))

/** Handwritten Latin face — crisper for pure-English wordmarks/hero. */
val HandLatin = FontFamily(Font(R.font.patrick_hand, FontWeight.Normal))

val Kanit = FontFamily(
    Font(R.font.kanit_medium, FontWeight.Medium),
    Font(R.font.kanit_semibold, FontWeight.SemiBold),
    Font(R.font.kanit_bold, FontWeight.Bold),
)

val Sarabun = FontFamily(
    Font(R.font.sarabun_regular, FontWeight.Normal),
    Font(R.font.sarabun_semibold, FontWeight.SemiBold),
    Font(R.font.sarabun_bold, FontWeight.Bold),
)

// Headings use the handwritten Hand (Itim); body/UI use Sarabun.
val WbwTypography = Typography(
    displaySmall = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Normal, fontSize = 29.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Hand, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = Sarabun, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, lineHeight = 13.sp),
)
