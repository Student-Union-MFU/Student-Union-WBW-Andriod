package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * How far a pane bends what is behind it, and over what depth.
 *
 * [DefaultRefractionHeight] is the thickness of the glass at its edge — how wide the
 * band is where the image bends — and [DefaultRefractionAmount] is how hard it bends.
 * Both are Dp, not raw pixels: `BackdropEffectScope` is a `Density`, so a constant
 * tuned in pixels on one phone is a third of the bend on a low-dpi tablet and twice
 * it on a flagship.
 *
 * Ported from su-clubfair-mobile, which uses the same library for the same bar.
 */
val DefaultRefractionHeight = 15.dp
val DefaultRefractionAmount = 18.dp

/** Softening behind a pane. Wider than the refraction band, so the two read as one material. */
val DefaultGlassBlur = 4.5.dp

/**
 * The liquid glass material, from `io.github.kyant0:backdrop`.
 *
 * This is a different thing from the frosted pane in `Glass.kt`, and the difference
 * is refraction: the frost *blurs* what is behind it, while this *bends* it, so the
 * forest backdrop's trunks and light stretch and curve as they pass under a pane's
 * edge. That bending is the part that reads as iOS 26's Liquid Glass; a blur alone
 * does not, which is why the iOS app forbids faking it with `.blur()`.
 *
 * All five layers come from the library and using fewer is what makes a pane look
 * like a tinted rectangle instead of glass:
 *
 * - [vibrancy] lifts the saturation of what shows through, so the backdrop's greens
 *   survive being seen through a translucent surface instead of going grey.
 * - [blur] softens it. The most expensive effect here.
 * - [lens] is the refraction, and the reason for the library.
 * - [highlight] is edge lighting computed from the shape's own signed distance field
 *   rather than a border painted round the outside. Pass `null` and draw a plain
 *   border where an even edge is wanted.
 * - [shadow]/[innerShadow] are what make a pane sit *on* something.
 *
 * Everything shader-backed needs `RuntimeShader`, so API 33+; the library no-ops
 * `lens` and the highlight below that, and the blur below API 31. This app's minSdk
 * is 26, so on an older phone the bar degrades to a plain translucent pill.
 *
 * [backdrop] must be a layer holding what the pane should see through itself and
 * **nothing it shouldn't** — a pane sampling a layer it is drawn inside is sampling
 * itself. In practice: one `layerBackdrop` around the forest wallpaper and the
 * routed screens, with the bar outside it.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Dp = DefaultGlassBlur,
    refractionHeight: Dp = DefaultRefractionHeight,
    refractionAmount: Dp = DefaultRefractionAmount,
    surface: Color = Color.White.copy(alpha = 0.06f),
    highlight: Highlight? = Highlight.Default,
    shadow: Shadow? = null,
    innerShadow: InnerShadow? = null,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        // Skipped entirely at zero rather than passed a zero radius: the blur is a
        // sampling pass over the backdrop whether or not it moves anything.
        if (blurRadius > 0.dp) blur(blurRadius.toPx())
        lens(
            refractionHeight = refractionHeight.toPx(),
            refractionAmount = refractionAmount.toPx(),
            depthEffect = true,
        )
    },
    highlight = { highlight },
    shadow = { shadow },
    innerShadow = { innerShadow },
    onDrawSurface = { drawRect(surface) },
)
