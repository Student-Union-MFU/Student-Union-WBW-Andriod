package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.hypot
import kotlin.math.pow
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
 * [gridDp] is the halftone's pitch and belongs to the surface, not to the drawing — but a
 * small rendering of the plant needs a finer screen than a large one, or its stem comes out
 * thinner than one cell and drops out of the picture entirely. Home's hero can use the
 * coarse default; anything drawn at a couple of hundred dp should ask for less.
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
    breathing: Boolean = true,
    gridDp: Float = 6f,
) {
    // Eased so a check-in *opens* the flower rather than snapping it — and so tapping
    // along the stage strip runs the bloom forwards and backwards instead of cutting.
    val openness by animateFloatAsState(
        targetValue = stage.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "bloom",
    )

    // The breath, on a leash.
    //
    // An `infiniteRepeatable` cannot be stopped, and a running animation means the screen
    // redraws sixty times a second for as long as it is open. Each of those frames is
    // cheap now, but "cheap and constant" is still a GPU kept awake all day on a phone
    // being carried up a mountain — and Home is a screen people leave open.
    //
    // So it is an [Animatable] driven by an effect that can be cancelled, and it resumes
    // from the phase it stopped at rather than restarting the cycle: a flower caught
    // mid-breath must not jump when it starts again. With [breathing] false the canvas has
    // nothing left to invalidate it and the screen goes quiet, the way a screen with no
    // animation on it does.
    val breath = remember { Animatable(0f) }
    LaunchedEffect(breathing) {
        if (!breathing) return@LaunchedEffect
        while (true) {
            val remaining = ((TwoPi - breath.value) / TwoPi * BreathMillis).toInt().coerceAtLeast(16)
            breath.animateTo(TwoPi, tween(remaining, easing = LinearEasing))
            breath.snapTo(0f)
        }
    }

    val field = remember { BloomField() }
    Canvas(modifier) {
        drawBloom(field, openness, ink, breath.value, gridDp = gridDp, centreYFraction = 0.38f)
    }
}

private val TwoPi = (2 * PI).toFloat()

/** One full breath cycle. */
private const val BreathMillis = 7000

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
 *
 * Two things used to make this row hard to use, and both were about size rather than
 * spacing. The chips were the right size — the *drawings inside them* were not: every
 * stage was scaled against the full-bloom head, so a seed came out a 1dp speck and stage 1
 * a short stroke, in a 58dp box that looked empty. And only the selected chip carried a
 * ring, so the other five had no edge, no fill and nothing to say they could be pressed.
 * Now every chip is a ring you can see, and each stage is fitted to its own extent — see
 * [headFit] for why that is only a partial fit.
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
            reached -> 0.78f
            else -> 0.42f   // silhouette: present, clearly not yours yet
        },
        animationSpec = tween(220),
        label = "stageStrength",
    )
    // The chip's own presence, kept separate from the ink so an unreached stage can be a
    // faint drawing inside a perfectly solid control.
    val ringAlpha by animateFloatAsState(
        targetValue = if (selected) 0.34f else if (reached) 0.20f else 0.13f,
        animationSpec = tween(220),
        label = "stageRing",
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (selected) 0.14f else 0.05f,
        animationSpec = tween(220),
        label = "stageFill",
    )
    // The whole cell takes the tap — the circle is what you see, not what you have to
    // hit. On a narrow handset the visible chip is around 50dp and the cell around 58dp,
    // and the difference is exactly the margin that stops neighbouring stages being
    // selected by mistake.
    BoxWithConstraints(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        // Sized from the cell rather than filling it: a rounded shape on a box that is
        // wider than it is tall comes out lopsided, and six lopsided chips read as a row
        // of buttons that did not quite fit.
        val diameter = (minOf(maxWidth, maxHeight) - ChipInset).coerceAtLeast(24.dp)
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(ChipShape)
                    .background(ink.copy(alpha = fillAlpha))
                    .border(1.dp, ink.copy(alpha = ringAlpha), ChipShape),
            )
            val field = remember { BloomField() }
            Canvas(Modifier.matchParentSize().padding(diameter * 0.11f)) {
                // Head only, and a much finer grid. The stem is two thirds of the
                // flower's height and carries none of its identity, so including it left
                // the head a speck; at this size the head *is* the stage.
                //
                // `strength` is applied to the finished dots rather than baked into them,
                // so the 220ms fade between reached and unreached never rebuilds the
                // drawing — six chips redrawing themselves on every frame of that fade is
                // what the field cache exists to avoid.
                drawBloom(
                    field, stage.toFloat(), ink, breath = 0f,
                    gridDp = 1.5f, centreYFraction = 0.5f,
                    alphaScale = strength, kind = Botanical.Head,
                )
            }
        }
    }
}

/**
 * The halftone pass, shared by the hero and the strip.
 *
 * Kept as one function on purpose — two copies of this would drift, and the strip's whole
 * job is to be the same flower the hero is.
 *
 * The geometry is *not* recomputed here. It is built into [field] and reused until the
 * flower or the canvas actually changes; all this does per frame is walk the finished dots
 * and give each one its breath. See [BloomField] for why that mattered so much.
 */
private fun DrawScope.drawBloom(
    field: BloomField,
    openness: Float,
    ink: Color,
    breath: Float,
    gridDp: Float,
    centreYFraction: Float,
    alphaScale: Float = 1f,
    kind: Botanical = Botanical.Plant,
) {
    field.ensure(
        w = size.width,
        h = size.height,
        stage = openness,
        // Fixed density, in dp — the halftone grid belongs to the surface it is printed
        // on, like a print screen, not to the drawing. Scaling it with the flower is what
        // turned the dots into four fat blobs: the bigger the flower got, the coarser its
        // texture.
        step = gridDp.dp.toPx(),
        centreYFraction = centreYFraction,
        kind = kind,
    )

    val dots = field.dots
    var i = 0
    val end = field.count * DotStride
    while (i < end) {
        val n = dots[i + 4]
        // Each dot breathes on its own phase, so the field shimmers instead of pulsing as
        // one block. Zero breath freezes it, which is what the strip wants.
        val puff = if (breath == 0f) 1f else 0.88f + 0.12f * sin(breath + n * 6.2831f)
        val r = dots[i + 2] * puff
        if (r > 0.25f) {
            drawCircle(
                color = ink,
                radius = r,
                center = Offset(dots[i], dots[i + 1]),
                alpha = (dots[i + 3] * alphaScale).coerceIn(0f, 1f),
            )
        }
        i += DotStride
    }
}

/**
 * Which drawing a field holds.
 *
 * All three are the same halftone of the same petal geometry — what changes is which parts
 * are asked for and how the result is fitted to its box. A frond is a leaf spray with no
 * flower head on it at all, which is why it alone has no core.
 */
private enum class Botanical { Plant, Head, Frond }

/**
 * A leaf spray, for decoration.
 *
 * Leaves are already modelled as petals rooted away from the head — that is how the plant's
 * own two leaves work — so a frond is just more of them along a stalk, and the stalk itself
 * is one long thin petal. Nothing new to render, and nothing that can drift out of step
 * with the flower it is meant to belong to.
 *
 * [variant] picks a fixed arrangement rather than seeding a random one: the login screen
 * draws these at opposite corners, and two sprays that are obviously the same drawing
 * mirrored read as wallpaper, while two unrelated ones read as a pair.
 */
private fun frondPetals(variant: Float): List<Petal> {
    val v = variant.toInt()
    val mirror = if (v % 2 == 0) 1f else -1f
    val out = ArrayList<Petal>(12)

    // The stalk. A petal tapers to nothing at both ends, which is exactly what a stem
    // does, so it needs no special case in the coverage test.
    val rootX = CX
    val rootY = CY + 96f
    val stalkAngle = -90f + 5f * mirror
    val stalkRad = stalkAngle * PI.toFloat() / 180f
    val stalkLen = 168f + 20f * hash(v * 5.1f)
    out.add(Petal(rootX, rootY, stalkAngle, stalkLen, 3.2f))

    // Leaves up the stalk, alternating sides, shortening toward the tip — the shape a
    // frond makes.
    //
    // Broad and few, not fine and many. The first version used the flower's own petal
    // proportions (0.34 of their length across, six a side) and at a 3.4dp halftone that
    // is two or three dots wide: the sprays came out as speckle in the corners rather than
    // as leaves. A leaf has to be several dots across before the grid can describe its
    // edge at all.
    val along = floatArrayOf(0.12f, 0.27f, 0.42f, 0.56f, 0.70f, 0.83f)
    for ((k, t) in along.withIndex()) {
        val side = if (k % 2 == 0) 1f else -1f
        val lx = rootX + (stalkLen * t) * cos(stalkRad)
        val ly = rootY + (stalkLen * t) * sin(stalkRad)
        // Sweeping up toward the tip, the way a frond's leaves do — flat at the base,
        // closing on the stalk as they climb.
        val spread = 70f - 30f * t
        val len = (86f - 34f * t) * (0.9f + 0.2f * hash(k * 17.3f + v))
        out.add(Petal(lx, ly, stalkAngle + side * mirror * spread, len, len * 0.30f))
    }
    // One leaf straight off the tip, so the spray ends in a point rather than in a gap
    // between the last pair.
    out.add(
        Petal(
            rootX + stalkLen * 0.92f * cos(stalkRad),
            rootY + stalkLen * 0.92f * sin(stalkRad),
            stalkAngle,
            52f,
            52f * 0.30f,
        ),
    )
    return out
}

/**
 * A frond, drawn in the same dots as the bloom.
 *
 * Decoration only — it takes no state and never animates, so it builds its field once and
 * then costs a walk of that array whenever something else on the screen redraws.
 */
@Composable
fun Frond(
    modifier: Modifier = Modifier,
    variant: Int = 0,
    ink: Color = Color.White,
    alpha: Float = 0.22f,
    gridDp: Float = 2.4f,
) {
    val field = remember { BloomField() }
    Canvas(modifier) {
        drawBloom(
            field, variant.toFloat(), ink, breath = 0f,
            gridDp = gridDp, centreYFraction = 0.5f,
            alphaScale = alpha, kind = Botanical.Frond,
        )
    }
}

/** `x, y, radius, alpha, jitter` per dot, packed flat. */
private const val DotStride = 5

/**
 * The finished halftone, held between frames.
 *
 * This exists because of a measurement, not a hunch. Home ran at ~12fps *while idle* —
 * 81ms frames, every one of them flagged "Slow UI thread" — and a screen with no bloom on
 * it rendered zero frames in the same six seconds. The breath animation invalidates the
 * canvas every frame, and every frame the old code rebuilt the entire drawing from scratch:
 * a grid over the whole canvas (a few thousand cells on the hero), each cell tested against
 * every petal, each test computing that petal's `sin` and `cos` again. Roughly 150k trig
 * calls and a fresh `List<Petal>` sixty times a second, to produce a picture that only
 * changes when you check in.
 *
 * So the picture is built once and kept. What actually varies per frame — the breath, and
 * the strip's fade between reached and unreached — is applied at draw time, which is why
 * neither is baked into the dots.
 *
 * Three things make the rebuild itself cheap enough to happen during the bloom animation:
 *
 *  - Per-petal `sin`/`cos` are hoisted out of the cell loop into [PetalFan]. They were
 *    being recomputed for every cell of every petal.
 *  - The scan is clipped to the flower's own bounds. The hero's canvas is mostly empty sky
 *    and those cells were still being tested against all ~26 petals before failing.
 *  - Each petal gets a bounding box, so a cell that cannot be inside it costs four
 *    comparisons instead of a rotation.
 *
 * And the stage is quantised for the cache key, so the 900ms bloom rebuilds ~16 times
 * rather than on all ~54 frames. A sixteenth of a stage is a couple of percent of a petal's
 * length — invisible at the speed the tween runs.
 */
private class BloomField {
    var dots: FloatArray = FloatArray(0)
        private set
    var count: Int = 0
        private set

    private var keyW = -1f
    private var keyH = -1f
    private var keyStage = Float.NaN
    private var keyStep = -1f
    private var keyCentre = -1f
    private var keyKind: Botanical? = null

    fun ensure(w: Float, h: Float, stage: Float, step: Float, centreYFraction: Float, kind: Botanical) {
        val q = quantise(stage)
        if (w == keyW && h == keyH && q == keyStage && step == keyStep &&
            centreYFraction == keyCentre && kind == keyKind
        ) {
            return
        }
        keyW = w; keyH = h; keyStage = q; keyStep = step; keyCentre = centreYFraction; keyKind = kind
        build(w, h, q, step, centreYFraction, kind)
    }

    private fun add(x: Float, y: Float, r: Float, alpha: Float, jitter: Float) {
        val i = count * DotStride
        if (i + DotStride > dots.size) {
            dots = dots.copyOf(if (dots.size == 0) 1024 else dots.size * 2)
        }
        dots[i] = x; dots[i + 1] = y; dots[i + 2] = r; dots[i + 3] = alpha; dots[i + 4] = jitter
        count++
    }

    private fun build(w: Float, h: Float, stage: Float, step: Float, centreYFraction: Float, kind: Botanical) {
        count = 0
        if (w <= 0f || h <= 0f || step <= 0f) return

        val petals = when (kind) {
            // Leaves live on the stem, so a head-only drawing that keeps them ends up with
            // two strokes floating under a flower and a box half again too tall.
            Botanical.Head -> petalsFor(stage, withLeaves = false)
            Botanical.Plant -> petalsFor(stage, withLeaves = true)
            Botanical.Frond -> frondPetals(stage)
        }
        val fan = PetalFan(petals)
        val withStem = kind == Botanical.Plant
        val withCore = kind != Botanical.Frond

        // Scaled to the flower's own extent, not to its authoring canvas. The prototype
        // draws inside a 300×300 box but only ever fills about a third of it, so scaling by
        // 300 left the bloom a thumbnail in the middle of the screen.
        //
        // The hero has one stage on screen at a time and can use the whole-plant box. A
        // chip cannot: six stages sit side by side and the early ones are a tiny fraction
        // of the last, so they are fitted to themselves instead — see [headFit]. A frond is
        // decoration and simply fills whatever box it is given.
        val bounds = when (kind) {
            Botanical.Head -> headBounds(petals, stage)
            Botanical.Frond -> floatArrayOf(fan.minX, fan.minY, fan.maxX, fan.maxY)
            Botanical.Plant -> null
        }
        val scale = if (bounds != null) {
            val bw = (bounds[2] - bounds[0]).coerceAtLeast(1f)
            val bh = (bounds[3] - bounds[1]).coerceAtLeast(1f)
            val fit = if (kind == Botanical.Head) headFit(maxOf(bw, bh)) else 1f
            minOf(w / bw, h / bh) * fit
        } else {
            minOf(w / FlowerWidth, h / FlowerHeight)
        }
        // What the drawing is centred on. For the head-only fit that is the middle of what
        // is actually drawn; otherwise the authored origin.
        val refX = if (bounds != null) (bounds[0] + bounds[2]) / 2f else CX
        val refY = if (bounds != null) (bounds[1] + bounds[3]) / 2f else CY
        val cx = w / 2f
        // Centred on the flower's real bounds, not on the canvas: the head reaches up by a
        // petal length and the stem down by its own, so the origin is not the middle.
        val cy = if (kind == Botanical.Plant) {
            h / 2f - (FlowerHeight / 2f - MaxPetal) * scale
        } else {
            h * centreYFraction
        }

        val maxR = step * 0.60f
        val coreR = if (withCore) 3.5f + 3f * stage else 0f

        // The strip's chips are drawn back up to the ink they had before the cup shading.
        //
        // The depth model still runs on them — the *relative* tones are part of what makes a
        // chip read as a flower rather than as a smudge — but its absolute level is set for a
        // hero the height of the screen. At 50dp the shading is below what the eye can
        // resolve anyway, and all it did there was take the two faintest stages, seed and
        // bud, below the threshold where an unreached chip is visible at all.
        val gain = if (kind == Botanical.Head) HeadGain else 1f

        // Everything drawn, in flower space, so the scan can skip the empty canvas around
        // it. The core and the stem are in here as well as the petals: the stem is not a
        // petal and stage 0 has no petals at all.
        var minX = minOf(fan.minX, CX - coreR)
        var maxX = maxOf(fan.maxX, CX + coreR)
        var minY = minOf(fan.minY, CY - coreR)
        var maxY = maxOf(fan.maxY, CY + coreR)
        if (withStem) {
            // The sway carries the stem to about 13 either side of centre; the widened
            // taper adds its half-width on top of that.
            minX = minOf(minX, CX - 19f); maxX = maxOf(maxX, CX + 19f)
            minY = minOf(minY, CY - 6f); maxY = maxOf(maxY, CY + StemLength + 4f)
        }

        // Back to canvas pixels, then out to whole grid cells. The cells are indexed from
        // the canvas origin exactly as before — the jitter hash is keyed on that index, so
        // starting the scan anywhere off-grid would reshuffle the whole texture.
        val ix0 = maxOf(0, floor(((minX - refX) * scale + cx) / step).toInt())
        val ix1 = minOf(ceil(w / step).toInt(), ceil(((maxX - refX) * scale + cx) / step).toInt())
        val iy0 = maxOf(0, floor(((minY - refY) * scale + cy) / step).toInt())
        val iy1 = minOf(ceil(h / step).toInt(), ceil(((maxY - refY) * scale + cy) / step).toInt())

        for (iy in iy0..iy1) {
            val gy = iy * step
            if (gy >= h) break
            for (ix in ix0..ix1) {
                val gx = ix * step
                if (gx >= w) break
                val fx = (gx - cx) / scale + refX
                val fy = (gy - cy) / scale + refY

                var cover = fan.coverAt(fx, fy)

                // Stem: a curve, so distance is taken to a sample at this height.
                if (withStem && fy > CY - 6f && fy < CY + StemLength + 4f) {
                    val t = ((fy - CY) / StemLength).coerceIn(0f, 1f)
                    val sx = cubic(CX, CX + 13f, CX - 11f, CX + 3f, t)
                    val d = abs(fx - sx)
                    // Tapers: thick at the root, finer where it meets the head — but far
                    // less finely than it used to. At 0.45 of 3.4 units the top of the stem
                    // came out narrower than a single halftone cell and simply dropped out
                    // of the picture, leaving the head floating clear of its own stalk. The
                    // old flower hid that: its petals wrapped down past horizontal and
                    // covered the junction. A head that opens upward does not, so the stem
                    // now has to survive on its own up there.
                    val halfW = 4.4f * (0.70f + 0.30f * t)
                    if (d < halfW) cover = maxOf(cover, StemShade * (1f - d / halfW))
                }

                // The core: dots crowd where every petal meets, so it is drawn as solid.
                //
                // Drawn at every stage including zero, where it is the seed. Gating it at
                // stage 1 left the first chip in the strip completely blank — stage 0 has
                // no petals, and the strip draws heads without stems, so there was nothing
                // at all to see under a label reading "Seed".
                //
                // Held down by [CoreShade] rather than drawn at full strength. As the
                // brightest thing on the canvas it turned the open stages into a starburst;
                // as the dimmest it is the eye of the cup, and the head gains a near side.
                if (withCore) {
                    val dc = hypot(fx - CX, fy - CY)
                    if (dc < coreR) cover = maxOf(cover, CoreShade * (1f - (dc / coreR) * 0.35f))
                }

                if (cover <= 0.02f) continue
                if (gain != 1f) cover = (cover * gain).coerceAtMost(1f)
                val n = hash(ix * 31f + iy * 57f)
                // Less radius scatter than there was. At +-45% the noise between one dot and
                // its neighbour was larger than the tonal differences the shading is drawing
                // with, so the depth was there in the numbers and invisible on the screen.
                // Enough jitter is kept to break up the grid; not enough to bury the tone.
                val r = maxR * cover.coerceAtMost(1f) * (0.75f + 0.25f * n)
                if (r <= 0.25f) continue
                add(
                    x = gx + (n - 0.5f) * step * 0.35f,
                    y = gy + (hash(n) - 0.5f) * step * 0.35f,
                    // Breath is deliberately absent: it is the one thing that changes
                    // every frame, so it is applied at draw time to the radius here.
                    r = r,
                    alpha = (0.35f + 0.65f * cover).coerceIn(0f, 1f),
                    jitter = n,
                )
            }
        }
    }

    /** The cache key's stage resolution — sixteenths of a stage. */
    private fun quantise(stage: Float): Float = round(stage * 16f) / 16f
}

/**
 * The petals of one flower, as parallel arrays with their trigonometry already done.
 *
 * The per-petal `sin`/`cos` used to be computed inside the innermost loop, once per cell
 * per petal — the single most expensive line in the drawing. Each petal also carries the
 * box it can possibly cover, so a cell outside it is rejected by four comparisons.
 */
private class PetalFan(petals: List<Petal>) {
    private val n = petals.size
    private val ox = FloatArray(n)
    private val oy = FloatArray(n)
    private val cosA = FloatArray(n)
    private val sinA = FloatArray(n)
    private val len = FloatArray(n)
    private val halfWidth = FloatArray(n)
    private val shade = FloatArray(n)
    private val bx0 = FloatArray(n)
    private val bx1 = FloatArray(n)
    private val by0 = FloatArray(n)
    private val by1 = FloatArray(n)

    var minX = Float.MAX_VALUE; private set
    var maxX = -Float.MAX_VALUE; private set
    var minY = Float.MAX_VALUE; private set
    var maxY = -Float.MAX_VALUE; private set

    init {
        for (i in 0 until n) {
            val p = petals[i]
            val a = p.ang * PI.toFloat() / 180f
            val ca = cos(a)
            val sa = sin(a)
            ox[i] = p.cx; oy[i] = p.cy; cosA[i] = ca; sinA[i] = sa
            len[i] = p.len; halfWidth[i] = p.halfWidth; shade[i] = p.shade
            // The widest a petal ever gets is at its waist; 1.1 leaves room for the
            // shoulder in the taper below.
            val pad = p.halfWidth * 1.1f
            val tipX = p.cx + p.len * ca
            val tipY = p.cy + p.len * sa
            bx0[i] = minOf(p.cx, tipX) - pad; bx1[i] = maxOf(p.cx, tipX) + pad
            by0[i] = minOf(p.cy, tipY) - pad; by1[i] = maxOf(p.cy, tipY) + pad
            minX = minOf(minX, bx0[i]); maxX = maxOf(maxX, bx1[i])
            minY = minOf(minY, by0[i]); maxY = maxOf(maxY, by1[i])
        }
        if (n == 0) { minX = CX; maxX = CX; minY = CY; maxY = CY }
    }

    /**
     * How much ink a point in flower space takes, 0..1.
     *
     * Petals are *composited* back to front, not maxed. `max` was the whole reason the open
     * stages had no depth: it keeps only the strongest petal at each point and throws away
     * every fact about which petal is in front, so a pile of overlapping petals came out the
     * same value as one petal — saturated, everywhere.
     *
     * Compositing keeps that ordering. Each petal lays its own [shade] over whatever is
     * already there, using its cross-section as its alpha, so the frontmost petal wins where
     * it is solid and lets the one behind show through at its edges. The list order *is* the
     * depth order — [petalsFor] emits the outer whorl, then the inner one on top of it, then
     * the leaves — so nothing needs sorting here.
     */
    fun coverAt(x: Float, y: Float): Float {
        var acc = 0f
        for (i in 0 until n) {
            if (x < bx0[i] || x > bx1[i] || y < by0[i] || y > by1[i]) continue
            val dx = x - ox[i]
            val dy = y - oy[i]
            // Along and across the petal's own axis.
            val f = dx * cosA[i] + dy * sinA[i]
            if (f < 0f || f > len[i]) continue
            val s = -dx * sinA[i] + dy * cosA[i]
            // Half-width tapers to nothing at root and tip, widest just past the waist.
            val u = f / len[i]
            val hw = halfWidth[i] * 4f * u * (1f - u) * (1.15f - 0.15f * u)
            if (hw <= 0f) continue
            val d = abs(s) / hw
            if (d >= 1f) continue
            // A flat body with a soft edge, so the petal actually occludes what is under it.
            val a = minOf(1f, (1f - d) / PetalEdge)
            // Darker toward its rim — the seam against the petal below — and darker toward
            // its root, because the head is a cup lit from its tips inward.
            val tone = shade[i] * (1f - RimShade * d * d * d) * (RootShade + (1f - RootShade) * u)
            acc = a * tone + (1f - a) * acc
        }
        return acc
    }
}

/**
 * What a head-only drawing actually covers, in flower units: `[minX, minY, maxX, maxY]`.
 *
 * Measured rather than tabulated. The petal table changes often enough — counts, lengths,
 * splay have all been retuned — that a hand-written extent per stage would be wrong within
 * a release and wrong silently, as a chip that no longer quite fits its circle.
 *
 * Sampled at the waist and the tip: the half-width tapers to nothing at both ends, so the
 * tip is exact and the waist is where a petal is widest.
 */
private fun headBounds(petals: List<Petal>, stage: Float): FloatArray {
    val coreR = 3.5f + 3f * stage
    var minX = CX - coreR
    var maxX = CX + coreR
    var minY = CY - coreR
    var maxY = CY + coreR
    for (p in petals) {
        val a = p.ang * PI.toFloat() / 180f
        for (u in floatArrayOf(0.5f, 1f)) {
            val hw = p.halfWidth * 4f * u * (1f - u) * (1.15f - 0.15f * u)
            val px = p.cx + p.len * u * cos(a)
            val py = p.cy + p.len * u * sin(a)
            minX = minOf(minX, px - hw); maxX = maxOf(maxX, px + hw)
            minY = minOf(minY, py - hw); maxY = maxOf(maxY, py + hw)
        }
    }
    return floatArrayOf(minX, minY, maxX, maxY)
}

/**
 * How much of its circle a chip's head is allowed to fill, given how big that head is.
 *
 * A *full* fit — every stage scaled up until it touches its ring — is the obvious answer
 * and the wrong one: it makes a seed exactly as big as a full bloom, and the row stops
 * showing growth at all, which is the only thing it is there to show.
 *
 * A fractional power is the compromise. Filling by the raw extent ratio is what the strip
 * did before and it is far too harsh at the bottom — a seed is 4% of a full head, so it
 * came out invisible. Raising that ratio to 0.4 lifts the small stages into legibility
 * (a seed to roughly a third of its circle, stage 1 to about half) while keeping every
 * step visibly larger than the one before it.
 */
private fun headFit(extent: Float): Float =
    (extent / HeadSpan).coerceIn(0.02f, 1f).pow(HeadFitExponent)

private const val HeadFitExponent = 0.4f

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

/**
 * One petal, in flower space: origin, axis angle, length, half-width at the waist, and how
 * brightly it takes the ink.
 *
 * [shade] is the whole of the depth model. There is no z coordinate and no projection —
 * petals are still flat shapes on a flat canvas — but they are composited in list order and
 * each one carries its own tone, which is enough for the eye to sort them into layers. See
 * [PetalFan.coverAt].
 */
private data class Petal(
    val cx: Float,
    val cy: Float,
    val ang: Float,
    val len: Float,
    val halfWidth: Float,
    val shade: Float = 1f,
)

/**
 * The depth palette.
 *
 * Stages 4 and 5 used to be a flat white mass with a ragged edge — the only part of the
 * drawing you could actually read was the fringe where the petals finally stopped
 * overlapping. The cause was [PetalFan.coverAt] combining petals with `max`: 16 outer petals
 * and 8 inner ones all radiate from the same point across a 332-degree splay, so at the
 * densest stages every petal overlaps its neighbours more than twice over, and `max` means
 * that wherever *any* petal's spine falls the coverage saturates. That is the entire head.
 * Every dot came out at full radius and full alpha, and a shape with one value everywhere
 * has no depth to read.
 *
 * These are the tones that replace it. All of them are multipliers on coverage, because
 * coverage is the only channel a halftone has — it drives both dot radius and dot alpha, and
 * there is no second colour to shade with.
 */
private const val PetalBack = 0.80f
private const val PetalFront = 1.0f

/** The inner whorl, which sits down inside the cup rather than out in the light. */
private const val InnerShade = 0.70f

/** The eye of the flower. Dim on purpose — see [RootShade]. */
private const val CoreShade = 0.62f

/** Leaves and stem, kept just under the head so the bloom stays the brightest thing. */
private const val LeafShade = 0.88f
private const val StemShade = 0.85f

/**
 * How dark a petal is at its root, rising to full at the tip.
 *
 * The head is a cup: its centre is in shadow and the tips are what catch the light. The old
 * drawing lit it the other way round — a solid core drawn at coverage 1.0, brighter than
 * anything around it — and a blazing centre with an even surround is precisely the shape of
 * a splat. Turning the gradient over is the single change that made the head read as
 * something with a near side and a far side.
 */
private const val RootShade = 0.62f

/**
 * How much a petal darkens toward its own rim.
 *
 * This is the line that separates one petal from the one underneath it. Without it two
 * overlapping petals at similar tones still merge into a single blob.
 */
private const val RimShade = 0.30f

/**
 * How far a petal recedes for turning its edge to the viewer.
 *
 * The head opens at the sky, so a petal aimed up shows us its lit inner face while one aimed
 * outward is seen edge-on. On its own this is a plain vertical gradient, but laid over a fan
 * of petals it is what gives the cup an inside.
 */
private const val LeanShade = 0.76f

/**
 * The fraction of a petal's half-width given over to its soft edge.
 *
 * The rest is a flat body at full alpha, so a petal *hides* what is behind it instead of
 * averaging with it. The old cross-section was a dome that faded from its spine all the way
 * out to its edge, which composites into mush: with a dome there is no part of a petal that
 * is unambiguously in front.
 */
private const val PetalEdge = 0.30f

/** How far a chip's head is lifted back toward its pre-shading ink. See [BloomField.build]. */
private const val HeadGain = 1.4f

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
private fun petalsFor(stage: Float, withLeaves: Boolean = true): List<Petal> {
    val s = stage.coerceIn(0f, 5f)
    val i = s.toInt().coerceAtMost(4)
    val t = s - i

    // The splay is what decides which way the flower faces, and it is the whole reason the
    // last stages read as a starburst rather than as a bloom.
    //
    // A splay of 166 is a 332-degree sweep: the petals wrap almost the entire way round, and
    // a fan that closes on itself is by definition a flower seen face-on, pointed at the
    // viewer. Held under about 90 the fan cannot close — every petal keeps an upward
    // component, they converge on the stem at the bottom, and the head reads as a cup
    // opening at the sky. That is the flower this is meant to be: it opens upward through
    // every stage and never turns to face us.
    //
    // The counts come down with it. Petal spacing is the splay divided by the count, so
    // keeping 16 petals inside a 76-degree fan would pack them tighter than the 166-degree
    // one they came from and undo the gaps that let the layering be seen.
    val counts = intArrayOf(0, 1, 6, 9, 12, 14)
    val lengths = floatArrayOf(0f, 30f, 40f, 52f, 64f, 80f)
    val splays = floatArrayOf(0f, 0f, 36f, 54f, 66f, 76f)
    // Wide and blunt as a bud, narrowing as the petals separate. The last two are narrower
    // than they were: at 0.20 a stage-5 petal is about 46 degrees wide with only 21 degrees
    // between it and the next, so the fan closed into a solid disc no amount of shading
    // could open up. Narrower petals leave the gaps that let the layering be seen.
    val ratios = floatArrayOf(0.35f, 0.35f, 0.24f, 0.21f, 0.17f, 0.17f)

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
        // Two depth cues, both carried as tone.
        //
        // Imbrication: petals alternate forward and back, the way a real whorl tucks each
        // petal under the next. A fan whose petals are all the same tone is a fan you cannot
        // count, however many of them there are.
        //
        // Lean: the head is a cup opening upward, so a petal aimed at the sky turns its lit
        // inner face toward us and a petal aimed outward is seen edge-on and loses the
        // light. This is what stops the cup reading as a flat fan of identical strokes.
        val lean = (1f - sin(ang * PI.toFloat() / 180f)) / 2f
        val shade = (if (k % 2 == 0) PetalBack else PetalFront) *
            (LeanShade + (1f - LeanShade) * lean)
        out.add(Petal(CX, CY, ang, len, len * ratio, shade))
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
            out.add(Petal(CX, CY, ang, len, len * 0.26f, InnerShade))
        }
    }

    // Leaves. Modelled as petals rooted on the stem rather than at the head, which is
    // all a leaf is geometrically — the coverage test does not care where a petal starts.
    for ((n, leaf) in Leaves.withIndex()) {
        if (!withLeaves) break
        val grow = ((s - leaf.fromStage) / 0.9f).coerceIn(0f, 1f)
        if (grow <= 0f) continue
        val ly = CY + StemLength * leaf.alongStem
        val lx = cubic(CX, CX + 13f, CX - 11f, CX + 3f, leaf.alongStem)
        val len = leaf.length * grow
        if (len <= 0.01f) continue
        val jitter = 5f * (hash(n * 27.1f) - 0.5f)
        out.add(Petal(lx, ly, leaf.angle + jitter, len, len * 0.30f, LeafShade))
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

private fun cubic(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val u = 1 - t
    return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * The chip's outline: a rounded square, like the profile button and the event cards.
 *
 * Circles were what the row had, and six of them under a round bloom over a round nav bar
 * left the screen with nothing but circles — the stages had no edge of their own to be
 * recognised by. A soft square also gives the drawing inside it more room in the corners,
 * which at this size is most of the room there is.
 */
private val ChipShape = RoundedCornerShape(16.dp)

/**
 * The gap between one chip's ring and the next.
 *
 * Taken out of the circle, not out of the row: the cells stay flush, so every pixel
 * between two rings is still a tap that lands on one of them. Spacing the cells instead
 * would buy the same gap by making the drawings smaller, which is the wrong trade on a
 * row this tight.
 */
private val ChipInset = 6.dp

/** Deterministic 0..1 from a seed — the same trick the prototype used, so no RNG state. */
private fun hash(seed: Float): Float {
    val x = sin(seed * 12.9898f) * 43758.5453f
    return x - floor(x)
}
