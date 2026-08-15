package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/**
 * Contour lines: the ground the login screen sits on, under everything else.
 *
 * This replaced a pair of large leaf sprays. Leaves in the background and leaves in the
 * foreground is one idea used twice — the corner planting stops reading as planting when
 * the whole screen is already made of it. Lines carry the same outdoors without competing:
 * they are the map the trail is drawn on rather than more of the trail.
 *
 * Two sine harmonics per line, at frequencies that do not divide into each other, so the
 * set never falls into a repeating pattern the eye can lock onto. Everything is seeded off
 * the line's index — same screen, same lines, every launch.
 *
 * The middle of the screen fades to nothing. That band is where the form sits, and a
 * hairline crossing behind type at this contrast is the sort of thing that looks fine in a
 * screenshot and unreadable in sunlight. [clearBand] is how much of the height is kept
 * clear, [clearCentre] where that gap is centred.
 */
@Composable
fun WaveLines(
    modifier: Modifier = Modifier,
    ink: Color = Color.White,
    alpha: Float = 0.16f,
    lines: Int = 16,
    clearBand: Float = 0.34f,
    clearCentre: Float = 0.55f,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val stroke = 1.dp.toPx()
        val twoPi = (2 * PI).toFloat()
        // Sampled rather than curved: at this amplitude a polyline every few pixels is
        // indistinguishable from a spline and costs nothing to build.
        val step = w / 48f

        for (i in 0 until lines) {
            val t = if (lines <= 1) 0.5f else i / (lines - 1f)
            val baseY = h * (0.02f + 0.96f * t)

            // How far this line is outside the clear band, 0 at its edge and 1 at the
            // screen edge — the fade that keeps the form's ground empty.
            val fromCentre = abs(t - clearCentre)
            val reach = (fromCentre - clearBand / 2f) / (0.5f - clearBand / 2f).coerceAtLeast(0.001f)
            val fade = reach.coerceIn(0f, 1f)
            if (fade <= 0.01f) continue

            val seed = hash(i * 12.9f)
            val seed2 = hash(i * 31.7f + 5f)
            val amp = h * (0.010f + 0.022f * seed)
            val amp2 = amp * 0.45f
            val phase = seed * twoPi
            val phase2 = seed2 * twoPi
            // 1.7 and 2.9 waves across the width: not a whole number, so no line closes
            // back on itself at the edges, and not a ratio, so the two never beat.
            val path = Path()
            var x = -step
            while (x <= w + step) {
                val u = x / w
                val y = baseY +
                    amp * sin(u * twoPi * 1.7f + phase) +
                    amp2 * sin(u * twoPi * 2.9f + phase2)
                if (x <= -step + 0.001f) path.moveTo(x, y) else path.lineTo(x, y)
                x += step
            }
            drawPath(
                path,
                color = ink,
                alpha = alpha * fade * (0.55f + 0.45f * seed2),
                style = Stroke(width = stroke),
            )
        }
    }
}

/** Deterministic 0..1 from a seed — the same trick the bloom's halftone uses. */
private fun hash(seed: Float): Float {
    val x = sin(seed * 12.9898f) * 43758.5453f
    return x - floor(x)
}
