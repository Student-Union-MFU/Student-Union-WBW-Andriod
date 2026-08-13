package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The bloom: a flower drawn as a field of dots, opening as checkpoints are collected.
 *
 * Two references fed this. The geometry — petals as cubic curves, stage driving petal
 * count, length and splay — is ported from the stipple flower prototyped for
 * su-clubfair-mobile. The *rendering* follows the halftone poster instead: dots sit on a
 * fixed grid and vary in radius with how much flower covers them, rather than being
 * sprayed along the outlines. That is what gives the printed, dithered look, and it is
 * also what makes the thing read at a glance — a contour spray reads as sparkle, a
 * halftone reads as a shape.
 *
 * Everything is deterministic. The jitter comes from a hash of the cell coordinate, so
 * the same progress always draws the same flower and there is no per-frame allocation or
 * random state to keep. The only animation is a slow breath on the dot radii.
 *
 * Coverage is computed analytically — a point is inside a petal if its distance across
 * the petal's axis is within the petal's half-width at that distance along it. No path
 * rasterisation, no offscreen buffer: at a 7dp grid this is a few thousand cheap tests.
 */
@Composable
fun Bloom(
    checkedIn: Int,
    total: Int,
    modifier: Modifier = Modifier,
    ink: Color = Color.White,
) {
    val stage = stageFor(checkedIn, total)
    // Eased so a check-in *opens* the flower rather than snapping it.
    val openness by animateFloatAsState(
        targetValue = stage.toFloat(),
        animationSpec = tween(durationMillis = 1400, easing = LinearEasing),
        label = "bloom",
    )
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "breathT",
    )

    val petals = remember(openness) { petalsFor(openness) }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Scaled to the flower's own extent, not to its authoring canvas. The prototype
        // draws inside a 300×300 box but only ever fills about a third of it, so scaling
        // by 300 left the bloom a thumbnail in the middle of the screen. The real reach
        // is the longest petal (58 up) and the stem (96 down), about 160 across.
        val scale = minOf(w / FlowerSpan, h / (FlowerSpan * 1.15f))
        val cx = w / 2f
        val cy = h * 0.38f

        // Fixed density, in dp — the halftone grid belongs to the screen, like a print
        // screen, not to the drawing. Scaling it with the flower is what turned the dots
        // into four fat blobs: the bigger the flower got, the coarser its own texture.
        val step = 6.dp.toPx()
        val maxR = step * 0.60f

        var gy = 0f
        while (gy < h) {
            var gx = 0f
            while (gx < w) {
                // Back into flower space to test coverage.
                val fx = (gx - cx) / scale + CX
                val fy = (gy - cy) / scale + CY
                val cover = coverage(fx, fy, petals, openness)
                if (cover > 0.02f) {
                    val n = hash(floor(gx / step) * 31f + floor(gy / step) * 57f)
                    // The breath moves each dot on its own phase, so the field shimmers
                    // instead of pulsing as one block.
                    val puff = 0.88f + 0.12f * sin(breath + n * 6.2831f)
                    val r = maxR * cover * puff * (0.55f + 0.45f * n)
                    if (r > 0.25f) {
                        drawCircle(
                            color = ink,
                            radius = r,
                            center = Offset(gx + (n - 0.5f) * step * 0.35f, gy + (hash(n) - 0.5f) * step * 0.35f),
                            alpha = (0.35f + 0.65f * cover).coerceAtMost(1f),
                        )
                    }
                }
                gx += step
            }
            gy += step
        }
    }
}

/**
 * Which of the six stages a progress maps to.
 *
 * Stage 0 is a bare stem, not an empty frame: someone who has checked in nowhere should
 * still see the thing they are growing.
 */
fun stageFor(checkedIn: Int, total: Int): Int {
    if (total <= 0) return 0
    val f = (checkedIn.toFloat() / total).coerceIn(0f, 1f)
    return when {
        checkedIn <= 0 -> 0
        f < 0.2f -> 1
        f < 0.4f -> 2
        f < 0.65f -> 3
        f < 0.9f -> 4
        else -> 5
    }
}

/** One petal, in flower space: origin, axis angle, length, half-width at the waist. */
private data class Petal(val cx: Float, val cy: Float, val ang: Float, val len: Float, val halfWidth: Float)

private const val CX = 150f
private const val CY = 176f

/** How far across the flower actually reaches, in flower units — see the scale note. */
private const val FlowerSpan = 172f

/**
 * Stem length below the head.
 *
 * Shorter than the prototype's 96. That value was tuned for a 300px thumbnail where the
 * head was tiny; blown up to a phone screen it read as a long wire with a speck on top.
 * The head grew and the stem shrank until the two balanced.
 */
private const val StemLength = 74f

/**
 * The flower at a (fractional) stage.
 *
 * Fractional because the stage animates: petal *count* steps, but length and splay are
 * interpolated, so the opening is continuous rather than five visible jumps.
 */
private fun petalsFor(stage: Float): List<Petal> {
    val s = stage.coerceIn(0f, 5f)
    val i = s.toInt().coerceAtMost(4)
    val t = s - i

    val counts = intArrayOf(0, 1, 6, 9, 13, 18)
    val lengths = floatArrayOf(0f, 30f, 40f, 52f, 64f, 74f)
    val splays = floatArrayOf(0f, 0f, 42f, 96f, 150f, 180f)

    val count = if (s < 1f) 0 else counts[minOf(i + if (t > 0.5f) 1 else 0, 5)]
    val length = lerp(lengths[i], lengths[minOf(i + 1, 5)], t)
    val splay = lerp(splays[i], splays[minOf(i + 1, 5)], t)

    val out = ArrayList<Petal>(count + 12)
    if (count == 1) {
        // A closed bud.
        out.add(Petal(CX, CY, -90f, length, length * 0.35f))
        return out
    }
    for (k in 0 until count) {
        val f = if (count == 1) 0f else k.toFloat() / (count - 1)
        val ang = -90f - splay + 2f * splay * f + 6f * (hash(s * 40f + k) - 0.5f)
        val len = length * (0.82f + 0.18f * hash(s * 70f + k))
        out.add(Petal(CX, CY, ang, len, len * 0.20f))
    }
    // Inner ring — shorter and tighter, which is what makes the centre read as dense
    // rather than as a hole where all the petal roots meet.
    if (s >= 3f) {
        val inner = count / 2
        for (k in 0 until inner) {
            val ang = -90f - splay * 0.5f + splay * k / maxOf(1, inner - 1) + 8f * (hash(k * 13f + s) - 0.5f)
            out.add(Petal(CX, CY, ang, length * 0.5f, length * 0.13f))
        }
    }
    return out
}

/** How much flower covers a point in flower space, 0..1. */
private fun coverage(x: Float, y: Float, petals: List<Petal>, stage: Float): Float {
    var cover = 0f

    // Stem: a curve, so distance is taken to a handful of samples along it.
    val stemTop = CY
    if (y > stemTop - 6f) {
        val t = ((y - stemTop) / StemLength).coerceIn(0f, 1f)
        val sx = cubic(CX, CX + 13f, CX - 11f, CX + 3f, t)
        val d = abs(x - sx)
        // Tapers: thick at the root, fine where it meets the head.
        val halfW = 3.4f * (0.45f + 0.55f * t)
        if (d < halfW) cover = maxOf(cover, 1f - d / halfW)
    }

    for (p in petals) {
        val a = p.ang * PI.toFloat() / 180f
        val dx = x - p.cx
        val dy = y - p.cy
        // Along and across the petal's own axis.
        val f = dx * cos(a) + dy * sin(a)
        val s = -dx * sin(a) + dy * cos(a)
        if (f < 0f || f > p.len) continue
        // Half-width tapers to nothing at root and tip, widest just past the waist.
        val u = f / p.len
        val hw = p.halfWidth * 4f * u * (1f - u) * (1.15f - 0.15f * u)
        if (hw <= 0f) continue
        val d = abs(s) / hw
        if (d < 1f) cover = maxOf(cover, (1f - d * d) * 0.9f + 0.1f)
    }

    // The core: dots crowd where every petal meets, so it is drawn as solid.
    if (stage >= 1f) {
        val d = hypot(x - CX, y - CY)
        val coreR = 5f + 3f * stage
        if (d < coreR) cover = maxOf(cover, 1f - (d / coreR) * 0.35f)
    }
    return cover.coerceIn(0f, 1f)
}

private fun cubic(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val u = 1 - t
    return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Deterministic 0..1 from a seed — the same trick the prototype used, so no RNG state. */
private fun hash(seed: Float): Float {
    val x = sin(seed * 12.9898f) * 43758.5453f
    return x - floor(x)
}
