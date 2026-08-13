package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import th.ac.mfu.su.wbw.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The app-wide backdrop, ported from iOS `AppBackdrop.swift`.
 *
 * iOS draws one full-bleed forest photograph under everything and dims only the top
 * and bottom of it. That dimming is not decoration: the screens over this backdrop
 * were designed when the background was the flat #0A1610, so their text and icons
 * are all white. Against the bright photo they became nearly unreadable — iOS
 * confirmed that from real screenshots before adding the scrim. Covering the whole
 * frame would fix it too but would waste the photo, so only the head and foot (where
 * the header and tab bar actually sit) are dimmed and the middle stays clear.
 *
 * The source image is 1440×2880 (1:2), chosen to sit near the geometric middle of the
 * widest and tallest handsets so cropping stays under ~11% on either axis.
 *
 * The previous procedural sky is kept as [ProceduralSkyBackground] — it is not part
 * of the iOS design, but it is this app's own work and worth not throwing away.
 */
@Composable
fun ForestBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_backdrop),
            contentDescription = null,
            // ContentScale.Crop is SwiftUI's scaledToFill: fill the frame, clip the rest.
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0.00f to Color.Black.copy(alpha = 0.45f),
                            0.28f to Color.Transparent,
                            0.76f to Color.Transparent,
                            1.00f to Color.Black.copy(alpha = 0.40f),
                        ),
                    )
                },
        )
        content()
    }
}

/**
 * The living forest sky. A soft radial gradient (day or night) with a starfield
 * at night and a layered hill silhouette at the foot. Frosted-glass panels are
 * meant to float over this — the softness is why translucency alone reads as glass.
 *
 * Not used while the app matches iOS, which uses a photographic backdrop instead
 * (see [ForestBackground]). Swap the call sites back to this to restore it.
 */
@Composable
fun ProceduralSkyBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = wbwColors
    val night = colors.isDark
    // star = [x, y(0..0.6), brightness, phase]
    val stars = remember(night) {
        if (!night) emptyList()
        else Random(7).let { r -> List(80) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.6f, 0.25f + r.nextFloat() * 0.75f, r.nextFloat()) } }
    }
    // firefly = [x, y(0.46..0.8), phase]
    val fireflies = remember(night) {
        if (!night) emptyList()
        else Random(23).let { r -> List(8) { floatArrayOf(0.08f + r.nextFloat() * 0.84f, 0.46f + r.nextFloat() * 0.33f, r.nextFloat()) } }
    }
    // distant pine = [x, heightScale]
    val pines = remember { Random(41).let { r -> List(12) { floatArrayOf(0.03f + r.nextFloat() * 0.94f, 0.55f + r.nextFloat() * 0.6f) } } }
    val t by rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "t",
    )
    val twoPi = (2.0 * PI).toFloat()

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width; val h = size.height
                // Sky gradient, light source near the top-centre.
                drawRect(Brush.radialGradient(colors.skyStops, center = Offset(w * 0.5f, -h * 0.12f), radius = h * 1.05f))

                if (night) {
                    // Moon: soft halo + disc with a hint of crater shading.
                    val mc = Offset(w * 0.74f, h * 0.12f)
                    drawCircle(Brush.radialGradient(listOf(Color(0x33FFF3D6), Color.Transparent), center = mc, radius = w * 0.55f), radius = w * 0.55f, center = mc)
                    drawCircle(Color(0xFFF4EAD0), radius = w * 0.05f, center = mc)
                    drawCircle(Color(0x18202A20), radius = w * 0.05f, center = mc + Offset(w * 0.02f, -w * 0.012f))
                }

                // Vignette to deepen the edges.
                drawRect(Brush.radialGradient(listOf(Color.Transparent, if (night) Color(0x6B000000) else Color(0x2E142D20)), center = Offset(w * 0.5f, h * 0.34f), radius = w * 0.95f))

                // Twinkling stars.
                stars.forEach { s ->
                    val tw = 0.55f + 0.45f * ((sin(t * twoPi + s[3] * twoPi) + 1f) / 2f)
                    drawCircle(Color(0xFFFDF6E6).copy(alpha = s[2] * 0.75f * tw), radius = (0.5f + s[2]) * density, center = Offset(s[0] * w, s[1] * h))
                }

                // Layered hills (far → front) with distant pines tucked between them + a mist band.
                val far = if (night) Color(0xFF152C1E) else Color(0x807FAE86)
                val mid = if (night) Color(0xFF102217) else Color(0xFF5F9576)
                val front = if (night) Color(0xFF0A160F) else Color(0xFF3F7A58)
                drawPath(hill(w, h, 0.83f, 0.79f, 0.82f, 0.85f, 0.8f), far)
                drawPath(hill(w, h, 0.86f, 0.83f, 0.86f, 0.885f, 0.85f), mid)
                val pineColor = if (night) Color(0xFF0A160F) else Color(0xFF2F5D43)
                pines.forEach { p ->
                    val px = p[0] * w; val baseY = h * 0.858f; val ph = h * 0.026f * p[1]; val pw = ph * 0.62f
                    drawPath(Path().apply { moveTo(px, baseY - ph); lineTo(px + pw, baseY); lineTo(px - pw, baseY); close() }, pineColor)
                }
                drawPath(hill(w, h, 0.9f, 0.87f, 0.905f, 0.92f, 0.9f), front)
                // Mist band drifting just above the hills.
                val mistX = sin(t * twoPi) * w * 0.05f
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, if (night) Color(0x1FBFE0CF) else Color(0x26FFFFFF), Color.Transparent)),
                    topLeft = Offset(mistX - w * 0.1f, h * 0.8f), size = Size(w * 1.2f, h * 0.1f),
                )

                // Fireflies: warm glow drifting and pulsing (night only).
                if (night) {
                    fireflies.forEach { f ->
                        val dx = sin(t * twoPi + f[2] * twoPi) * w * 0.022f
                        val dy = cos(t * twoPi * 0.8f + f[2] * twoPi) * h * 0.012f
                        val pulse = 0.3f + 0.7f * ((sin(t * twoPi * 1.5f + f[2] * twoPi) + 1f) / 2f)
                        val fc = Offset(f[0] * w + dx, f[1] * h + dy)
                        drawCircle(Brush.radialGradient(listOf(Color(0xFFE2B078).copy(alpha = 0.5f * pulse), Color.Transparent), center = fc, radius = w * 0.03f), radius = w * 0.03f, center = fc)
                        drawCircle(Color(0xFFF3D9A8).copy(alpha = 0.9f * pulse), radius = 1.7f * density, center = fc)
                    }
                }
            },
    ) { content() }
}

/** A hill silhouette path: two quadratic humps across the width, filled to the bottom. */
private fun DrawScope.hill(w: Float, h: Float, y0: Float, y1: Float, y2: Float, y3: Float, y4: Float): Path =
    Path().apply {
        moveTo(0f, h * y0)
        quadraticTo(w * 0.28f, h * y1, w * 0.55f, h * y2)
        quadraticTo(w * 0.82f, h * y3, w, h * y4)
        lineTo(w, h); lineTo(0f, h); close()
    }

/** Rounded glass surface: soft drop shadow, near-solid fill, hairline border. */
fun Modifier.glass(
    shape: RoundedCornerShape,
    fill: Color? = null,
    border: Color? = null,
    elevation: Dp = 8.dp,
): Modifier = composed {
    val colors = wbwColors
    shadow(elevation, shape, clip = false, spotColor = Color.Black.copy(alpha = 0.5f), ambientColor = Color.Black.copy(alpha = 0.32f))
        .clip(shape)
        .background(fill ?: colors.glass)
        .border(1.dp, border ?: colors.glassBorder, shape)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    fill: Color? = null,
    content: @Composable () -> Unit,
) {
    val colors = wbwColors
    Box(
        modifier
            .shadow(16.dp, shape, clip = false, spotColor = Color.Black.copy(alpha = 0.5f), ambientColor = Color.Black.copy(alpha = 0.32f))
            .clip(shape)
            .background(fill ?: colors.glass)
            .border(1.dp, colors.glassBorder, shape)
            .padding(contentPadding),
    ) { content() }
}

/**
 * Gold accent brush used on primary actions / the FAB (light top → deep bottom).
 *
 * Runs iOS cream → iOS gold. The old stops (GoldLight → GoldDark) now both resolve to
 * the single iOS gold, which would have flattened this into a solid fill.
 */
fun goldBrush(): Brush = Brush.verticalGradient(listOf(WbwCream, WbwGold))

val CardCorner: Dp = 26.dp
val PanelCorner: Dp = 22.dp
val FieldCorner: Dp = 16.dp
