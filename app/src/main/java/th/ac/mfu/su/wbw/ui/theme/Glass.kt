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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import th.ac.mfu.su.wbw.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The app-wide backdrop: one full-bleed image under everything.
 *
 * The artwork is a dark, blurred forest texture (736×1471 — the same 1:2 ratio iOS
 * picked so cropping stays under ~11% on either axis). It replaced the iOS
 * `bg_ticket` photograph, which is still in git history if it is ever wanted back.
 *
 * That swap is why the scrim below is mild. iOS needs a heavy head-and-foot scrim
 * (0.45/0.40 black) because its photograph is bright — measured mean luminance 169,
 * with white text over it. This one measures 63 and never exceeds 157 anywhere, so
 * light text reads on it unaided and a heavy scrim would only crush the texture. What
 * is left is a gentle darkening at the very top and bottom, where the status bar and
 * gesture bar sit and the system draws its own glyphs over us.
 *
 * The image is shown as supplied in both themes — it is the app's strongest visual and
 * re-tinting it per theme only destroys it. What the theme changes is what sits *on*
 * it: cards, and their text.
 *
 * The consequence: this ground is dark in both themes, so anything drawn straight onto
 * it must use [WbwColors.onBackdrop], never `textPrimary` — the latter goes near-black
 * in light theme and disappears into the artwork.
 *
 * The previous procedural sky is kept as [ProceduralSkyBackground] — it is not part
 * of the iOS design, but it is this app's own work and worth not throwing away.
 */
@Composable
fun ForestBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = wbwColors
    // The layer the app's glass refracts. It wraps the wallpaper and *only* the
    // wallpaper: panes may not sample a layer they are drawn inside, so the content
    // below sits outside it. Getting this wrong does not fail loudly — the panes smear
    // themselves, which reads as a rendering glitch rather than a structural mistake.
    val wallpaper = rememberLayerBackdrop()

    Box(modifier.fillMaxSize()) {
        // clipToBounds so the overscaled, blurred image below cannot paint outside.
        Box(Modifier.fillMaxSize().clipToBounds().layerBackdrop(wallpaper)) {
            Image(
                painter = painterResource(R.drawable.bg_backdrop),
                contentDescription = null,
                // ContentScale.Crop is SwiftUI's scaledToFill: fill the frame, clip the rest.
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Overscaled *before* the blur so the kernel always has real pixels to
                    // sample. Without this the edge pixels blur toward transparent and the
                    // whole screen picks up a dark frame — visible on a real device, not
                    // in any preview.
                    .graphicsLayer {
                        scaleX = BackdropOverscan
                        scaleY = BackdropOverscan
                    }
                    // A soft defocus so text sits on tone rather than on detail. The
                    // artwork is already a blurred forest, but it keeps enough structure —
                    // bright blooms against near-black — that a line of type crossing one
                    // loses half its contrast midway.
                    //
                    // No-op below API 31, where the app simply gets the artwork as-is.
                    .blur(BackdropBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // A near-nothing settle under the content. Never enough to
                        // re-tint the artwork — that is the whole point of having it.
                        drawRect(colors.backdropWash)
                        // Head-and-foot scrim, much lighter than the iOS original — see
                        // the note above on why the darker artwork does not need more.
                        // Both themes: the image is the same dark forest either way.
                        drawRect(
                            Brush.verticalGradient(
                                0.00f to Color.Black.copy(alpha = 0.22f),
                                0.18f to Color.Transparent,
                                0.84f to Color.Transparent,
                                1.00f to Color.Black.copy(alpha = 0.22f),
                            ),
                        )
                        // An even dim over the whole frame, if one is ever wanted. Zero
                        // with this artwork: already dark enough to carry light text.
                        if (BackdropDim > 0f) drawRect(Color.Black.copy(alpha = BackdropDim))
                    },
            )
        }
        // Inside the provider but outside the layer — so every pane on this screen can
        // refract the wallpaper without any screen having to thread a Backdrop through.
        CompositionLocalProvider(LocalBackdrop provides wallpaper) { content() }
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

/**
 * How far the whole backdrop is dimmed, on top of the head-and-foot scrim.
 *
 * One knob, deliberately: the artwork stays untouched and the ground it provides for
 * text is tuned by a single number, so this is trivial to walk back or re-tune later.
 *
 * 0.00 is the literal iOS treatment.
 */
private const val BackdropDim = 0.00f

/**
 * How far the backdrop is defocused.
 *
 * Small on purpose: enough to stop the artwork's brighter passages competing with text,
 * not so much that it stops being a forest. Above roughly 20dp it turns to flat wash and
 * the app loses the thing the background was for.
 */
private val BackdropBlur = 12.dp

/**
 * How far the backdrop image is scaled up before being blurred.
 *
 * Enough to keep the blur kernel inside real pixels at every edge. The image is a soft
 * forest texture with no subject to lose, so a few percent of crop costs nothing.
 */
private const val BackdropOverscan = 1.12f

/** A hill silhouette path: two quadratic humps across the width, filled to the bottom. */
private fun DrawScope.hill(w: Float, h: Float, y0: Float, y1: Float, y2: Float, y3: Float, y4: Float): Path =
    Path().apply {
        moveTo(0f, h * y0)
        quadraticTo(w * 0.28f, h * y1, w * 0.55f, h * y2)
        quadraticTo(w * 0.82f, h * y3, w, h * y4)
        lineTo(w, h); lineTo(0f, h); close()
    }

/**
 * The wallpaper layer every glass surface in the app refracts.
 *
 * Null means "no layer available here", and the panes fall back to an opaque fill. That
 * is not a rare edge case worth ignoring — a pane may only sample a layer it is drawn
 * *outside* of, so any screen that has not set one up must degrade rather than sample
 * itself and smear.
 *
 * [ForestBackground] provides it, which is why glass works on every screen that sits on
 * the backdrop without any of them having to know about it.
 */
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * How much of the pane's own colour sits over the refraction.
 *
 * The dark value used to be 0.72, which is why cards read as a different material from
 * the participant pass and the nav bar: those tint at 0.12, so they refract the forest
 * while a card at 0.72 buried it and came out a flat tinted rectangle. All three use the
 * same library and the same call — the tint was the whole difference.
 *
 * Light stays high, and cannot come down to match. A light card has to be light, and the
 * only thing behind it is a dark photograph; at a low tint it simply goes dark and the
 * ink on it stops reading. Light mode gets the shape of the glass without much of its
 * transparency, which is a property of putting a pale surface on a dark ground rather
 * than something tuning can fix.
 */
private const val GlassTintDark = 0.30f
private const val GlassTintLight = 0.86f

/**
 * Rounded glass surface: refracts the wallpaper, tinted by the theme, hairline border.
 *
 * The same material as the nav bar, from the same library. Where there is no backdrop
 * layer in scope it degrades to the previous opaque fill, so nothing depends on the
 * shader being available.
 */
fun Modifier.glass(
    shape: RoundedCornerShape,
    fill: Color? = null,
    border: Color? = null,
    elevation: Dp = 8.dp,
): Modifier = composed {
    val colors = wbwColors
    val backdrop = LocalBackdrop.current
    val tint = fill ?: colors.glass.copy(alpha = if (colors.isDark) GlassTintDark else GlassTintLight)
    val base = shadow(
        elevation, shape, clip = false,
        spotColor = Color.Black.copy(alpha = 0.5f),
        ambientColor = Color.Black.copy(alpha = 0.32f),
    )
    if (backdrop != null) {
        base
            .liquidGlass(backdrop, shape, surface = tint, highlight = null)
            .clip(shape)
            .border(1.dp, border ?: colors.glassBorder, shape)
    } else {
        base
            .clip(shape)
            .background(fill ?: colors.glass)
            .border(1.dp, border ?: colors.glassBorder, shape)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    fill: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .glass(shape, fill = fill, elevation = 16.dp)
            .padding(contentPadding),
    ) { content() }
}

val CardCorner: Dp = 26.dp
val PanelCorner: Dp = 22.dp
val FieldCorner: Dp = 16.dp
