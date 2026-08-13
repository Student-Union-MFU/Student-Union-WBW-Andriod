package th.ac.mfu.su.wbw.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.Deep
import th.ac.mfu.su.wbw.ui.theme.Forest
import th.ac.mfu.su.wbw.ui.theme.Leaf
import th.ac.mfu.su.wbw.ui.theme.LeafLight

/** The five growth stages a participant's tree passes through as they check into bases. */
enum class GrowthPhase(val labelRes: Int) {
    Seed(R.string.phase_seed),
    Sprout(R.string.phase_sprout),
    Sapling(R.string.phase_sapling),
    Young(R.string.phase_young),
    Grown(R.string.phase_grown);

    companion object {
        /** Map check-in progress (0..total) onto a phase. */
        fun forProgress(checkedIn: Int, total: Int): GrowthPhase {
            if (total <= 0) return Seed
            val r = checkedIn.toFloat() / total
            return when {
                r <= 0f -> Seed
                r < 0.25f -> Sprout
                r < 0.55f -> Sapling
                r < 0.85f -> Young
                else -> Grown
            }
        }
    }
}

private val Bark = Color(0xFF6B4A2F)

/**
 * Draws a stylised tree for [phase]. Seeds/sprouts render as a sprig; sapling and
 * up stack layered pine tiers that grow taller and fuller with the phase.
 * [muted] renders the greyed "not yet reached" state used in the phase track.
 */
fun DrawScope.drawPhaseTree(phase: GrowthPhase, muted: Boolean = false) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val grey = Color(0xFF8A9A8F)
    val bark = if (muted) grey else Bark
    fun g(base: Color) = if (muted) grey else base

    when (phase) {
        GrowthPhase.Seed -> {
            drawLine(bark, Offset(cx, h * 0.86f), Offset(cx, h * 0.64f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
            leaf(cx, h * 0.68f, w * 0.15f, left = true, g(Leaf))
            leaf(cx, h * 0.62f, w * 0.16f, left = false, g(LeafLight))
            drawOval(bark.copy(alpha = 0.85f), topLeft = Offset(cx - w * 0.15f, h * 0.85f), size = Size(w * 0.30f, h * 0.05f))
        }
        GrowthPhase.Sprout -> {
            drawLine(bark, Offset(cx, h * 0.88f), Offset(cx, h * 0.5f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
            leaf(cx, h * 0.6f, w * 0.2f, left = true, g(Leaf))
            leaf(cx, h * 0.48f, w * 0.22f, left = false, g(LeafLight))
            leaf(cx, h * 0.38f, w * 0.16f, left = true, g(LeafLight))
        }
        else -> {
            // Layered pine: dark wide base → lighter narrow crown, over a rounded trunk.
            val fullness = when (phase) {
                GrowthPhase.Sapling -> 0.74f
                GrowthPhase.Young -> 0.88f
                else -> 1f
            }
            val groundY = h * 0.9f
            val trunkH = h * 0.15f * fullness
            val trunkTopY = groundY - trunkH
            val trunkW = (w * 0.11f * fullness).coerceAtLeast(3f)
            val apexY = h * 0.86f - h * 0.6f * fullness
            val foliageTop = apexY
            val foliageBottom = trunkTopY + h * 0.03f
            val span = foliageBottom - foliageTop
            val maxHalf = w * 0.40f * fullness

            drawRoundRect(
                color = bark,
                topLeft = Offset(cx - trunkW / 2f, trunkTopY - 2f),
                size = Size(trunkW, groundY - trunkTopY + 2f),
                cornerRadius = CornerRadius(trunkW * 0.35f),
            )
            // top→bottom colours; draw bottom first so the crown sits in front
            val greens = listOf(g(LeafLight), g(Forest), g(Deep))
            for (i in 2 downTo 0) {
                val apex = foliageTop + span * (i * 0.26f)
                val base = foliageTop + span * ((0.5f + i * 0.25f).coerceAtMost(1f))
                val half = maxHalf * (0.5f + 0.25f * i)
                val p = Path().apply {
                    moveTo(cx, apex)
                    lineTo(cx + half, base)
                    lineTo(cx - half, base)
                    close()
                }
                drawPath(p, greens[i])
            }
        }
    }
}

private fun DrawScope.leaf(x: Float, y: Float, r: Float, left: Boolean, color: Color) {
    val dir = if (left) -1f else 1f
    val p = Path().apply {
        moveTo(x, y + r)
        cubicTo(x, y, x + dir * r, y - r * 0.2f, x + dir * r, y - r)
        cubicTo(x + dir * r * 0.2f, y - r, x, y, x, y + r)
    }
    drawPath(p, color)
}

/** Convenience composable that renders [phase] on a canvas. */
@androidx.compose.runtime.Composable
fun PhaseTree(phase: GrowthPhase, modifier: Modifier = Modifier, muted: Boolean = false) {
    Canvas(modifier) { drawPhaseTree(phase, muted) }
}

fun DrawScope.drawProgressRing(progress: Float, track: Color, arc: Color, stroke: Float) {
    val inset = stroke / 2f
    drawArc(
        color = track,
        startAngle = 0f, sweepAngle = 360f, useCenter = false,
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawArc(
        color = arc,
        startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
        topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}
