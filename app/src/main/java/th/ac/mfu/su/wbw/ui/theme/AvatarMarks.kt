package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import th.ac.mfu.su.wbw.ui.home.HalftoneMark
import th.ac.mfu.su.wbw.ui.home.Petal

/**
 * The avatars, drawn in the same ink as the bloom.
 *
 * They were emoji. Emoji are somebody else's drawings — they render differently on every
 * Android version, they carry their own colour into a screen that has one, and beside a
 * halftone flower they read as stickers stuck onto the app rather than as part of it.
 *
 * These are made of [Petal]s: tapered strokes with an origin, an axis, a length and a
 * waist, printed through the flower's dot screen. That is the whole vocabulary — a fern is
 * a fan of strokes, a mushroom is a wide flat one over a narrow upright one, a deer is a
 * dozen of them arranged like a deer. Nothing here is a new rendering technique; it is the
 * bloom's own geometry pointed at twelve other things.
 *
 * **Authoring space is the flower's**, so these read against `petalsFor` in `Bloom.kt`:
 * roughly 300 wide, origin near (150, 176), a long petal about 80. Angles are degrees with
 * **-90 pointing up** — screen Y grows downward, so a tip is `(cx + len·cos, cy + len·sin)`.
 * Each mark is fitted to its own bounds when drawn, so only proportion matters, not scale.
 *
 * [Petal.shade] is depth: strokes composite in list order, each laying its own tone over
 * what is already there. Backs go first, fronts last, and a stroke at 0.55 behind one at
 * 1.0 is what stops a twelve-stroke animal reading as one solid blob.
 */
object AvatarMarks {

    /** A stroke that is much longer than it is wide — a stem, a leg, a blade. */
    private fun stroke(
        x: Float,
        y: Float,
        ang: Float,
        len: Float,
        width: Float,
        shade: Float = 1f,
    ) = Petal(x, y, ang, len, width, shade)

    /**
     * A rounded lump — a body, a head, a berry.
     *
     * Built from two opposed strokes rather than a circle primitive, because the engine has
     * no circle: a stroke is widest at its waist, so two of them nose to nose from the same
     * centre make an even blob. Cheaper than adding a second coverage shape to the renderer
     * for something the existing one can already do.
     */
    private fun blob(x: Float, y: Float, r: Float, shade: Float = 1f) = listOf(
        Petal(x, y, -90f, r, r * 0.95f, shade),
        Petal(x, y, 90f, r, r * 0.95f, shade),
    )

    /**
     * A horizontal bar: a stroke laid flat, given by its centre and half-length.
     *
     * The taper runs along the stroke's axis, so a lone stroke is a lens and can never be a
     * triangle. Stacking bars of shrinking length is how a wedge gets made — see [wedge].
     */
    private fun bar(x: Float, y: Float, halfLen: Float, thick: Float, shade: Float = 1f) =
        Petal(x - halfLen, y, 0f, halfLen * 2f, thick, shade)

    /**
     * A triangle, as a stack of bars shrinking toward the apex.
     *
     * The engine has one shape and it is a lens, widest at its waist. Everything with a
     * straight edge — a conifer, a ridge — is built by stacking that lens rather than by
     * adding a second coverage test to the renderer.
     */
    private fun wedge(
        x: Float,
        y: Float,
        baseHalf: Float,
        height: Float,
        rows: Int,
        shade: Float = 1f,
    ): List<Petal> = (0 until rows).map { i ->
        val t = i / (rows - 1f)
        bar(x, y - height * t, baseHalf * (1f - 0.88f * t) + 2f, height / rows * 0.95f, shade)
    }

    /** A fan of strokes from one point — petals, rays, a spray of fronds. */
    private fun fan(
        x: Float,
        y: Float,
        count: Int,
        from: Float,
        to: Float,
        len: Float,
        width: Float,
        shade: Float = 1f,
    ): List<Petal> = (0 until count).map { k ->
        val t = if (count == 1) 0.5f else k.toFloat() / (count - 1)
        Petal(x, y, from + (to - from) * t, len, width, shade)
    }

    private val cx = 150f
    private val cy = 176f

    /** A flower seen face-on: two whorls, the inner one brighter and on top. */
    private val blossom: List<Petal> =
        fan(cx, cy, 8, -180f, 150f, 62f, 15f, 0.70f) +
            fan(cx, cy, 7, -160f, 130f, 44f, 13f, 1f) +
            blob(cx, cy, 13f, 0.55f)

    /** One broad leaf on a short stalk, with a midrib. */
    private val leaf: List<Petal> = listOf(
        stroke(cx - 10f, cy + 74f, -72f, 34f, 4f, 0.6f),
        // Fat and short: the first version was a long thin lens on a long stalk, and once
        // the mark was fitted to those bounds the leaf itself was a sliver.
        Petal(cx - 2f, cy + 46f, -72f, 100f, 40f, 1f),
        stroke(cx - 2f, cy + 46f, -72f, 92f, 3.4f, 0.45f),
    )

    /** A frond: paired leaflets stepping up a spine, shrinking toward the tip. */
    private val fern: List<Petal> = buildList {
        add(stroke(cx, cy + 70f, -84f, 150f, 4.5f, 0.75f))
        repeat(7) { i ->
            val t = i / 6f
            val y = cy + 58f - t * 122f
            val x = cx + (1f - t) * 5f
            val len = 46f * (1f - 0.62f * t) + 8f
            add(stroke(x, y, -32f - 10f * t, len, 7.5f - 3.4f * t, 0.95f - 0.15f * t))
            add(stroke(x, y, -148f + 10f * t, len, 7.5f - 3.4f * t, 0.95f - 0.15f * t))
        }
    }

    /** A tuft of grass: blades leaning off a common root. */
    private val grass: List<Petal> = listOf(
        stroke(cx, cy + 62f, -108f, 96f, 6.5f, 0.7f),
        stroke(cx - 4f, cy + 62f, -84f, 118f, 7f, 1f),
        stroke(cx + 4f, cy + 62f, -66f, 104f, 6.5f, 0.85f),
        stroke(cx - 12f, cy + 62f, -122f, 74f, 5.5f, 0.6f),
        stroke(cx + 12f, cy + 62f, -54f, 80f, 5.5f, 0.6f),
    )

    /** A conifer: three stacked tiers over a short trunk. */
    private val pine: List<Petal> = buildList {
        add(stroke(cx, cy + 84f, -90f, 30f, 7f, 0.6f))
        // Three overlapping wedges rather than one, so the silhouette steps the way a fir
        // does instead of coming out as a plain triangle.
        addAll(wedge(cx, cy + 66f, 56f, 52f, 5, 1f))
        addAll(wedge(cx, cy + 24f, 44f, 46f, 4, 0.92f))
        addAll(wedge(cx, cy - 14f, 32f, 42f, 4, 0.84f))
    }

    /** A mushroom: a wide flat cap over a short stem. */
    private val mushroom: List<Petal> = listOf(
        stroke(cx, cy + 66f, -90f, 52f, 13f, 0.75f),
        // The cap is bars, not a fan: a fan of strokes from one origin bulges into a ball,
        // and what makes a mushroom is the flat underside against the dome.
        bar(cx, cy + 12f, 62f, 15f, 1f),
        bar(cx, cy - 4f, 52f, 15f, 1f),
        bar(cx, cy - 18f, 36f, 14f, 1f),
        bar(cx, cy - 30f, 18f, 12f, 1f),
    )

    /** A deer, in profile: body, neck, head, four legs and a pair of antlers. */
    private val deer: List<Petal> = buildList {
        // Legs first — they sit behind everything and read as the far pair when dim.
        add(stroke(cx - 26f, cy + 6f, 82f, 62f, 5f, 0.5f))
        add(stroke(cx + 24f, cy + 6f, 96f, 62f, 5f, 0.5f))
        add(stroke(cx - 18f, cy + 8f, 88f, 66f, 6f, 0.85f))
        add(stroke(cx + 32f, cy + 8f, 92f, 66f, 6f, 0.85f))
        // Body: one long stroke laid across, thickened at the waist.
        add(Petal(cx - 22f, cy + 4f, -8f, 60f, 24f, 1f))
        // Neck and head.
        add(stroke(cx + 30f, cy + 2f, -62f, 46f, 10f, 1f))
        addAll(blob(cx + 52f, cy - 38f, 15f, 1f))
        add(stroke(cx + 60f, cy - 40f, -18f, 22f, 8f, 1f))
        // Antlers: a fork each side, thin and bright so they stay legible at 38dp.
        add(stroke(cx + 46f, cy - 50f, -108f, 34f, 3.4f, 1f))
        add(stroke(cx + 44f, cy - 74f, -152f, 18f, 3f, 1f))
        add(stroke(cx + 56f, cy - 50f, -66f, 32f, 3.4f, 1f))
        add(stroke(cx + 66f, cy - 74f, -28f, 17f, 3f, 1f))
        // Tail.
        add(stroke(cx - 24f, cy - 2f, -142f, 20f, 6f, 0.8f))
    }

    /** A bird in profile: body, head clear of it, beak, wing and a swept tail. */
    private val bird: List<Petal> = buildList {
        // Tail first, behind everything.
        add(Petal(cx - 22f, cy + 12f, 156f, 62f, 13f, 0.6f))
        add(Petal(cx - 14f, cy + 8f, -18f, 74f, 27f, 1f))
        // The head is set well clear of the body: overlapping them merged the two into one
        // lump and the bird lost its neck entirely.
        addAll(blob(cx + 46f, cy - 34f, 17f, 1f))
        add(stroke(cx + 44f, cy - 12f, -72f, 26f, 9f, 1f))
        add(stroke(cx + 60f, cy - 36f, 4f, 24f, 4.5f, 1f))
        add(Petal(cx + 2f, cy - 2f, -30f, 46f, 13f, 0.66f))
    }

    /** A butterfly: two pairs of wings held clear of a slim body. */
    private val butterfly: List<Petal> = listOf(
        // Wings rooted out on the body rather than all at one point. Four lenses from a
        // single origin overlap into a square; rooted apart they stay four wings.
        Petal(cx - 6f, cy - 6f, -156f, 74f, 26f, 1f),
        Petal(cx + 6f, cy - 6f, -24f, 74f, 26f, 1f),
        Petal(cx - 6f, cy + 10f, 158f, 54f, 19f, 0.72f),
        Petal(cx + 6f, cy + 10f, 22f, 54f, 19f, 0.72f),
        stroke(cx, cy - 26f, 90f, 58f, 5f, 1f),
        stroke(cx - 3f, cy - 28f, -124f, 30f, 2.4f, 1f),
        stroke(cx + 3f, cy - 28f, -56f, 30f, 2.4f, 1f),
    )

    /** A feather: one narrow vane, split by its shaft. */
    private val feather: List<Petal> = listOf(
        // The vane as a single long lens rather than a spray of barbs. Barbs splayed from a
        // spine make a triangle, and a triangle at 38dp is a conifer — this was landing
        // next to `pine` in the grid and reading as a second one.
        Petal(cx, cy + 74f, -84f, 150f, 26f, 0.92f),
        // The shaft, drawn dark through the middle: a feather is the one thing here read by
        // the line *down* it rather than by its outline.
        stroke(cx, cy + 76f, -84f, 146f, 4.5f, 0.28f),
        // Two nicks near the tip, so the top reads as barbs rather than as a leaf.
        stroke(cx + 8f, cy - 52f, -46f, 20f, 5f, 0.35f),
        stroke(cx - 4f, cy - 34f, -134f, 18f, 5f, 0.35f),
    )

    /** A ridge: two wedges, the far one dimmer so the pair reads as depth. */
    private val peak: List<Petal> =
        // Pushed further apart and sized further apart than the first attempt, where the
        // two wedges overlapped into one lump. The far summit clears the near one's
        // shoulder, which is the whole of what makes a ridge read as two mountains.
        wedge(cx + 56f, cy + 46f, 40f, 66f, 5, 0.45f) +
            wedge(cx - 34f, cy + 52f, 58f, 106f, 7, 1f) +
            // Snow on the near summit: a notch of dimmer ink just under the apex, which is
            // what stops a plain triangle reading as a conifer.
            listOf(bar(cx - 34f, cy - 32f, 13f, 11f, 0.25f))

    /** The sun: a disc with rays all the way round. */
    private val sun: List<Petal> =
        fan(cx, cy, 12, -180f, 150f, 74f, 6f, 0.8f) + blob(cx, cy, 38f, 1f)

    /**
     * Key to drawing, in display order.
     *
     * The keys are exactly the ones the server accepts (`avatarKeys` in
     * `wbw_admin_service.go`) and exactly the ones stored — swapping a drawing here changes
     * what everybody sees without touching a stored row, which is the point of storing a
     * name rather than a picture.
     */
    internal val all: List<Pair<String, List<Petal>>> = listOf(
        "pine" to pine,
        "blossom" to blossom,
        "fern" to fern,
        "leaf" to leaf,
        "grass" to grass,
        "mushroom" to mushroom,
        "deer" to deer,
        "bird" to bird,
        "butterfly" to butterfly,
        "feather" to feather,
        "peak" to peak,
        "sun" to sun,
    )

    private val byKey = all.toMap()

    /** True when this build knows how to draw that key. */
    fun has(key: String?): Boolean = key != null && byKey.containsKey(key)

    /**
     * Draw one avatar, or nothing if this build does not know the key.
     *
     * An unknown key is a real case, not a defensive one: the set can grow server-side while
     * somebody is still carrying an older APK, and the honest answer for them is whatever
     * they had before rather than a blank square or a wrong guess.
     */
    @Composable
    fun Mark(
        key: String?,
        modifier: Modifier = Modifier,
        ink: Color = Color.White,
        alpha: Float = 1f,
        gridDp: Float = 3.2f,
    ) {
        val petals = key?.let { byKey[it] } ?: return
        // Remembered against the key so the list is one allocation per avatar, not one per
        // recomposition — and so the halftone's own cache key stays stable.
        val drawing = remember(key) { petals }
        HalftoneMark(
            petals = drawing,
            markKey = key,
            modifier = modifier,
            ink = ink,
            alpha = alpha,
            gridDp = gridDp,
        )
    }
}
