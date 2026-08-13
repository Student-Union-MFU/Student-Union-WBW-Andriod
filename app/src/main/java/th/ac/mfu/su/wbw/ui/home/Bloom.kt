package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import th.ac.mfu.su.wbw.R
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
    stage: Int,
    modifier: Modifier = Modifier,
    ink: Color = Color.White,
) {
    // Eased so a check-in *opens* the flower rather than snapping it — and so tapping
    // along the stage strip runs the bloom forwards and backwards instead of cutting.
    val openness by animateFloatAsState(
        targetValue = stage.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "bloom",
    )
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "breathT",
    )

    Canvas(modifier) {
        drawBloom(openness, ink, breath, gridDp = 6f, centreYFraction = 0.38f)
    }
}

/**
 * One stage as a small silhouette, for the strip.
 *
 * The same flower at the same stage, drawn small — not an abstract dot or a numeral.
 * That is the whole point of the strip: an unreached stage should show you the shape you
 * are working toward, so the row reads as a sequence of blooms rather than as a progress
 * bar with steps.
 *
 * [reached] only changes ink strength. Drawing unreached stages in a different *form*
 * (outline, wireframe) would have meant a second renderer to keep in step with the first.
 */
@Composable
fun BloomStage(
    stage: Int,
    reached: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Color.White,
) {
    val strength by animateFloatAsState(
        targetValue = when {
            selected -> 1f
            reached -> 0.72f
            else -> 0.26f   // silhouette: present, clearly not yours yet
        },
        animationSpec = tween(220),
        label = "stageStrength",
    )
    Box(
        modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // A ring under the selected stage. Without it the strip has no affordance at all
        // — six faint drawings do not look like controls, which is exactly the complaint
        // the strip exists to answer.
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.10f))
                    .border(1.dp, ink.copy(alpha = 0.22f), CircleShape),
            )
        }
        Canvas(Modifier.matchParentSize().padding(7.dp)) {
            // Head only, and a much finer grid. The stem is two thirds of the flower's
            // height and carries none of its identity, so including it left the head a
            // speck; at this size the head *is* the stage.
            drawBloom(
                stage.toFloat(), ink, breath = 0f,
                gridDp = 1.5f, centreYFraction = 0.5f,
                alphaScale = strength, headOnly = true,
            )
        }
    }
}

/**
 * The halftone pass, shared by the hero and the strip.
 *
 * Kept as one function on purpose — two copies of this would drift, and the strip's whole
 * job is to be the same flower the hero is.
 */
private fun DrawScope.drawBloom(
    openness: Float,
    ink: Color,
    breath: Float,
    gridDp: Float,
    centreYFraction: Float,
    alphaScale: Float = 1f,
    headOnly: Boolean = false,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val petals = petalsFor(openness)

    // Scaled to the flower's own extent, not to its authoring canvas. The prototype draws
    // inside a 300×300 box but only ever fills about a third of it, so scaling by 300 left
    // the bloom a thumbnail in the middle of the screen.
    val scale = if (headOnly) minOf(w, h) / HeadSpan else minOf(w / FlowerWidth, h / FlowerHeight)
    val cx = w / 2f
    // Centred on the flower's real bounds, not on the canvas: the head reaches up by a
    // petal length and the stem down by its own, so the origin is not the middle.
    val cy = if (headOnly) h * centreYFraction else h / 2f - (FlowerHeight / 2f - MaxPetal) * scale

    // Fixed density, in dp — the halftone grid belongs to the surface it is printed on,
    // like a print screen, not to the drawing. Scaling it with the flower is what turned
    // the dots into four fat blobs: the bigger the flower got, the coarser its texture.
    val step = gridDp.dp.toPx()
    val maxR = step * 0.60f

    var gy = 0f
    while (gy < h) {
        var gx = 0f
        while (gx < w) {
            val fx = (gx - cx) / scale + CX
            val fy = (gy - cy) / scale + CY
            val cover = coverage(fx, fy, petals, openness, withStem = !headOnly)
            if (cover > 0.02f) {
                val n = hash(floor(gx / step) * 31f + floor(gy / step) * 57f)
                // Each dot breathes on its own phase, so the field shimmers instead of
                // pulsing as one block. Zero breath freezes it, which is what the strip wants.
                val puff = if (breath == 0f) 1f else 0.88f + 0.12f * sin(breath + n * 6.2831f)
                val r = maxR * cover * puff * (0.55f + 0.45f * n)
                if (r > 0.25f) {
                    drawCircle(
                        color = ink,
                        radius = r,
                        center = Offset(gx + (n - 0.5f) * step * 0.35f, gy + (hash(n) - 0.5f) * step * 0.35f),
                        alpha = ((0.35f + 0.65f * cover) * alphaScale).coerceIn(0f, 1f),
                    )
                }
            }
            gx += step
        }
        gy += step
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

/** The label for a bloom stage. Six of them — see the note on the strings. */
fun stageLabel(stage: Int): Int = when (stage.coerceIn(0, 5)) {
    0 -> R.string.bloom_stage_0
    1 -> R.string.bloom_stage_1
    2 -> R.string.bloom_stage_2
    3 -> R.string.bloom_stage_3
    4 -> R.string.bloom_stage_4
    else -> R.string.bloom_stage_5
}

/** One petal, in flower space: origin, axis angle, length, half-width at the waist. */
private data class Petal(val cx: Float, val cy: Float, val ang: Float, val len: Float, val halfWidth: Float)

private const val CX = 150f
private const val CY = 176f

/** The longest a petal ever gets — the head's radius at full bloom. */
private const val MaxPetal = 80f

/**
 * The whole plant's bounding box in flower units: head above, stem and leaves below.
 *
 * Written out rather than composed from [MaxPetal] and [StemLength] — top-level `const`
 * initialisation runs in file order, and the stem is declared further down.
 */
private const val FlowerWidth = 176f
private const val FlowerHeight = 206f   // MaxPetal 80 + StemLength 112 + 14 of air

/** Stem length below the head. */
private const val StemLength = 112f

/**
 * Leaves on the stem: how far down it they sit, which way they point, how big.
 *
 * Two, at different heights and opposite sides, from the prototype. A single leaf reads
 * as a mistake and a symmetrical pair reads as a logo; two at different heights is what
 * makes the stalk look grown rather than drawn.
 *
 * The second appears later than the first, so the plant keeps gaining something through
 * the middle stages instead of only widening its head.
 */
private val Leaves = listOf(
    LeafSpec(alongStem = 0.40f, angle = 150f, length = 34f, fromStage = 1.0f),
    LeafSpec(alongStem = 0.66f, angle = 32f, length = 29f, fromStage = 2.4f),
)

private data class LeafSpec(val alongStem: Float, val angle: Float, val length: Float, val fromStage: Float)

/**
 * The head's own reach, used when the stem is left out.
 *
 * At full bloom the petals radiate a full half-turn, so the head is roughly a disc of the
 * longest petal's radius. Scaling a head-only drawing by [FlowerSpan] would leave two
 * thirds of the box empty — that space belongs to a stem that is not being drawn.
 */
private const val HeadSpan = 160f

/**
 * The flower at a (fractional) stage.
 *
 * Everything here is continuous in [stage], because the stage animates and any quantity
 * that steps will be seen stepping. Three things used to step, and all three were
 * visible as a pop between bud and first bloom:
 *
 *  - **Petal count** snapped at the halfway point, so five petals appeared at once at
 *    full length. Now the target stage's petals all exist from the start of the
 *    transition and the new ones grow their length in from zero.
 *  - **Petal width** was a separate `count == 1` branch using a 0.35 ratio against the
 *    0.20 the others use, so the bud instantly thinned as it split. The ratio is now
 *    interpolated like everything else and the branch is gone.
 *  - **The jitter seed** was `hash(s * 40 + k)` — `s` is the *animating* value, so every
 *    frame reseeded every petal and the flower shimmered its way through the change.
 *    Seeds are per-petal-index only, fixed for the life of the flower.
 */
private fun petalsFor(stage: Float): List<Petal> {
    val s = stage.coerceIn(0f, 5f)
    val i = s.toInt().coerceAtMost(4)
    val t = s - i

    // Stage 5 used to be 18 petals at a full 180-degree splay, which closed the fan into
    // a disc — every gap filled, no silhouette left, and it read as worse than stage 4
    // rather than as more. Fewer petals, stopped short of the half-turn, and longer, so
    // the last stage is bigger and still has an outline.
    val counts = intArrayOf(0, 1, 6, 9, 13, 16)
    val lengths = floatArrayOf(0f, 30f, 40f, 52f, 64f, 80f)
    val splays = floatArrayOf(0f, 0f, 42f, 96f, 140f, 166f)
    // Wide and blunt as a bud, narrowing as the petals separate.
    val ratios = floatArrayOf(0.35f, 0.35f, 0.24f, 0.21f, 0.20f, 0.20f)

    val fromCount = counts[i]
    val toCount = counts[minOf(i + 1, 5)]
    val length = lerp(lengths[i], lengths[minOf(i + 1, 5)], t)
    val splay = lerp(splays[i], splays[minOf(i + 1, 5)], t)
    val ratio = lerp(ratios[i], ratios[minOf(i + 1, 5)], t)

    val out = ArrayList<Petal>(toCount + 12)
    for (k in 0 until toCount) {
        val jitter = 6f * (hash(k * 40.7f) - 0.5f)
        // A petal's angle depends on how many petals share the fan, so it has to be
        // carried across the count change rather than recomputed at the new count —
        // see [fanAngle]. Petals being added take the destination fan directly; they
        // are at zero length here anyway and will be "existing" in the next segment.
        val ang = if (k < fromCount) {
            lerp(fanAngle(k, fromCount, splay, jitter), fanAngle(k, toCount, splay, jitter), t)
        } else {
            fanAngle(k, toCount, splay, jitter)
        }
        // Petals that this stage is adding grow in; ones that already existed stay put.
        val grow = if (k < fromCount) 1f else t
        val len = length * (0.72f + 0.28f * hash(k * 70.3f)) * grow
        if (len <= 0.01f) continue
        out.add(Petal(CX, CY, ang, len, len * ratio))
    }

    // Inner ring — shorter and tighter, which is what makes the centre read as dense
    // rather than as a hole where all the petal roots meet. Faded in over a window
    // rather than switched on at stage 3, for the same reason as everything above.
    val innerGrow = ((s - 2.5f) / 0.9f).coerceIn(0f, 1f)
    if (innerGrow > 0f) {
        val innerFrom = fromCount / 2
        val innerTo = toCount / 2
        for (k in 0 until innerTo) {
            val jitter = 8f * (hash(k * 13.9f) - 0.5f)
            // Same carry as the outer ring: its count changes on the same boundaries.
            val ang = if (k < innerFrom) {
                lerp(innerAngle(k, innerFrom, splay, jitter), innerAngle(k, innerTo, splay, jitter), t)
            } else {
                innerAngle(k, innerTo, splay, jitter)
            }
            val grow = if (k < innerFrom) 1f else t
            val len = length * 0.5f * innerGrow * grow
            if (len <= 0.01f) continue
            out.add(Petal(CX, CY, ang, len, len * 0.26f))
        }
    }

    // Leaves. Modelled as petals rooted on the stem rather than at the head, which is
    // all a leaf is geometrically — the coverage test does not care where a petal starts.
    for ((n, leaf) in Leaves.withIndex()) {
        val grow = ((s - leaf.fromStage) / 0.9f).coerceIn(0f, 1f)
        if (grow <= 0f) continue
        val ly = CY + StemLength * leaf.alongStem
        val lx = cubic(CX, CX + 13f, CX - 11f, CX + 3f, leaf.alongStem)
        val len = leaf.length * grow
        if (len <= 0.01f) continue
        val jitter = 5f * (hash(n * 27.1f) - 0.5f)
        out.add(Petal(lx, ly, leaf.angle + jitter, len, len * 0.30f))
    }
    return out
}

/**
 * Where petal [k] sits when [count] petals share a fan of [splay] degrees either side of up.
 *
 * Pulled out because the same petal has a *different* angle in a six-petal fan than in a
 * nine-petal one, and the stage transition changes the count underneath it. Computing the
 * angle from the destination count alone is what made stages 2→3, 3→4 and 4→5 snap: every
 * existing petal was re-fanned in a single frame. The caller interpolates between the two
 * fans instead, so the petals sweep to their new spacing.
 *
 * The jump is invisible at 1→2 only because the splay there is still zero — the bug was
 * always present, it just had nothing to swing.
 */
private fun fanAngle(k: Int, count: Int, splay: Float, jitter: Float): Float {
    val f = if (count <= 1) 0.5f else k.toFloat() / (count - 1)
    return -90f - splay + 2f * splay * f + jitter
}

/** The inner ring's tighter fan — half the splay, centred. */
private fun innerAngle(k: Int, count: Int, splay: Float, jitter: Float): Float {
    val f = if (count <= 1) 0.5f else k.toFloat() / (count - 1)
    return -90f - splay * 0.5f + splay * f + jitter
}

/** How much flower covers a point in flower space, 0..1. */
private fun coverage(x: Float, y: Float, petals: List<Petal>, stage: Float, withStem: Boolean = true): Float {
    var cover = 0f

    // Stem: a curve, so distance is taken to a handful of samples along it.
    val stemTop = CY
    if (withStem && y > stemTop - 6f && y < stemTop + StemLength + 4f) {
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
    //
    // Drawn at every stage including zero, where it is the seed. Gating it at stage 1
    // left the first chip in the strip completely blank — stage 0 has no petals, and the
    // strip draws heads without stems, so there was nothing at all to see under a label
    // reading "Seed".
    run {
        val d = hypot(x - CX, y - CY)
        val coreR = 3.5f + 3f * stage
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
