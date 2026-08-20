package th.ac.mfu.su.wbw.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin

/**
 * A forest for the trail, drawn by the app rather than by the map.
 *
 * The Maps SDK has no trees and no way to ask for any. The style JSON can only recolour or
 * hide features Google already ships — `poi.park` is tinted `#1c3322` in
 * `map_style_forest.json` and that is as close as styling gets. Nor can the app ask the SDK
 * *where* the forest is: feature geometry is not exposed, so there is nothing to anchor
 * trees to at runtime. Anything that looks like a wood has to be both placed and drawn here.
 *
 * So the trees are hung off the one piece of geography the app already owns: the baked
 * route. They fill a band either side of it, which is the part of the map anyone is looking
 * at anyway, and it costs no new data — [TrailRoute] is already loaded for the polyline.
 * The trade is that the corridor follows the trail rather than the real tree line, so where
 * the trail runs past a building or a car park it will grow trees on them.
 *
 * **Nothing here is billable.** A [TileProvider] is rendered on the device from data already
 * in the APK: no tiles are fetched, no Places or Roads call is made, and the SDK caches what
 * it asks for. That matters on a map that had Places taken out of it for exactly this
 * reason.
 */
class TrailTrees private constructor(
    /** Metres east of [originXNorm], per tree. */
    private val ox: FloatArray,
    /** Metres south of [originYNorm], per tree. */
    private val oy: FloatArray,
    /**
     * The coarsest zoom this tree survives to. A tree with level 2 is drawn whenever the
     * required level is 2 or less, so zooming in only ever *adds* trees — the wood thickens
     * rather than rearranging itself.
     */
    private val lvl: ByteArray,
    private val count: Int,
    private val originXNorm: Double,
    private val originYNorm: Double,
    /** Ground metres spanned by one unit of normalised Mercator, at this latitude. */
    private val metresPerNorm: Double,
    /** The corridor's own bounds in metres, so a tile outside it can be answered instantly. */
    private val minX: Float,
    private val minY: Float,
    private val maxX: Float,
    private val maxY: Float,
) {
    fun size(): Int = count

    /**
     * Which trees fall inside a metre-space rectangle, appended to [out] as indices.
     *
     * A linear scan of every tree. With a corridor this size that is a few thousand float
     * comparisons per tile — far cheaper than the spatial index it would take to avoid them,
     * and this runs on the SDK's tile threads, never on the main one.
     */
    fun collect(x0: Float, y0: Float, x1: Float, y1: Float, level: Int, out: MutableList<Int>) {
        for (i in 0 until count) {
            if (lvl[i] < level) continue
            val tx = ox[i]
            val ty = oy[i]
            if (tx < x0 || tx > x1 || ty < y0 || ty > y1) continue
            out.add(i)
        }
    }

    fun treeX(i: Int): Float = ox[i]
    fun treeY(i: Int): Float = oy[i]

    fun boundsMinX(): Float = minX
    fun boundsMinY(): Float = minY
    fun boundsMaxX(): Float = maxX
    fun boundsMaxY(): Float = maxY

    /** Metre-space x of the left edge of tile column [x] at [zoom]. */
    fun tileLeftMetres(x: Int, zoom: Int): Double =
        (x.toDouble() / (1 shl zoom) - originXNorm) * metresPerNorm

    fun tileTopMetres(y: Int, zoom: Int): Double =
        (y.toDouble() / (1 shl zoom) - originYNorm) * metresPerNorm

    /** Ground metres across one tile at [zoom]. */
    fun tileSpanMetres(zoom: Int): Double = metresPerNorm / (1 shl zoom)

    companion object {
        /**
         * Lattice pitch. Trees sit one to a cell, jittered inside it — a plain grid reads as
         * an orchard, and pure noise clumps and leaves bald patches.
         */
        private const val SpacingMetres = 18f

        /** How far either side of the trail the wood reaches. */
        private const val CorridorMetres = 80f

        /** Mean canopy radius. Generous for a real tree, because it is drawn as texture. */
        const val CanopyMetres = 6.5f

        /** The widest a canopy gets, for tile margins. */
        const val MaxCanopyMetres = CanopyMetres * 1.3f

        private const val EarthCircumference = 40_075_016.686

        /**
         * Build the wood for a route.
         *
         * Walks the trail and marks the lattice cells near it, rather than testing every cell
         * in the route's bounding box against every segment. The trail is a thin thing in a
         * large box, so the second way spends nearly all of its work proving that cells far
         * from the path are far from the path.
         */
        fun around(route: TrailRoute): TrailTrees {
            val pts = route.points
            if (pts.size < 2) return empty()

            val centreLat = (route.bounds.southwest.latitude + route.bounds.northeast.latitude) / 2.0
            val metresPerNorm = EarthCircumference * cos(centreLat * PI / 180.0)
            val originXNorm = lngToNorm(route.bounds.southwest.longitude)
            // Mercator y grows southward, so the north-east corner is the smaller y.
            val originYNorm = latToNorm(route.bounds.northeast.latitude)

            // The route in metres from that origin.
            val n = pts.size
            val rx = FloatArray(n)
            val ry = FloatArray(n)
            for (i in 0 until n) {
                rx[i] = ((lngToNorm(pts[i].longitude) - originXNorm) * metresPerNorm).toFloat()
                ry[i] = ((latToNorm(pts[i].latitude) - originYNorm) * metresPerNorm).toFloat()
            }

            // One pass along the trail, marking every cell whose centre could be in range.
            val cells = HashSet<Long>(8192)
            val reach = CorridorMetres + SpacingMetres
            for (s in 0 until n - 1) {
                val ax = rx[s]; val ay = ry[s]
                val bx = rx[s + 1]; val by = ry[s + 1]
                val i0 = floor((minOf(ax, bx) - reach) / SpacingMetres).toInt()
                val i1 = ceil((maxOf(ax, bx) + reach) / SpacingMetres).toInt()
                val j0 = floor((minOf(ay, by) - reach) / SpacingMetres).toInt()
                val j1 = ceil((maxOf(ay, by) + reach) / SpacingMetres).toInt()
                for (j in j0..j1) {
                    for (i in i0..i1) {
                        val key = pack(i, j)
                        if (key in cells) continue
                        val cx = (i + 0.5f) * SpacingMetres
                        val cy = (j + 0.5f) * SpacingMetres
                        if (distanceToSegment(cx, cy, ax, ay, bx, by) <= CorridorMetres) {
                            cells.add(key)
                        }
                    }
                }
            }

            val total = cells.size
            val ox = FloatArray(total)
            val oy = FloatArray(total)
            val lvl = ByteArray(total)
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var k = 0
            for (key in cells) {
                val i = unpackI(key)
                val j = unpackJ(key)
                // Jitter inside the cell, from the cell's own coordinates — the same trick
                // the bloom's dot field uses, so there is no RNG state to carry and the wood
                // is identical on every device and every launch.
                val jx = hash(i * 12.7f + j * 3.1f) - 0.5f
                val jy = hash(i * 5.3f + j * 19.9f) - 0.5f
                ox[k] = (i + 0.5f + jx * 0.82f) * SpacingMetres
                oy[k] = (j + 0.5f + jy * 0.82f) * SpacingMetres
                lvl[k] = minOf(trailingZeros(i), trailingZeros(j)).coerceAtMost(3).toByte()
                if (ox[k] < minX) minX = ox[k]
                if (ox[k] > maxX) maxX = ox[k]
                if (oy[k] < minY) minY = oy[k]
                if (oy[k] > maxY) maxY = oy[k]
                k++
            }

            return TrailTrees(
                ox, oy, lvl, total, originXNorm, originYNorm, metresPerNorm,
                minX, minY, maxX, maxY,
            )
        }

        private fun empty() = TrailTrees(
            FloatArray(0), FloatArray(0), ByteArray(0), 0, 0.0, 0.0, EarthCircumference,
            0f, 0f, 0f, 0f,
        )

        /** Non-negative indices only — the lattice is anchored at the route's own corner. */
        private fun trailingZeros(v: Int): Int =
            if (v == 0) 8 else Integer.numberOfTrailingZeros(v)

        private fun pack(i: Int, j: Int): Long =
            (i.toLong() and 0xFFFFFFFFL) or (j.toLong() shl 32)

        private fun unpackI(key: Long): Int = key.toInt()
        private fun unpackJ(key: Long): Int = (key shr 32).toInt()

        private fun lngToNorm(lng: Double): Double = (lng + 180.0) / 360.0

        private fun latToNorm(lat: Double): Double {
            val s = sin(lat * PI / 180.0)
            return 0.5 - ln((1 + s) / (1 - s)) / (4 * PI)
        }

        private fun distanceToSegment(
            px: Float, py: Float,
            ax: Float, ay: Float,
            bx: Float, by: Float,
        ): Float {
            val dx = bx - ax
            val dy = by - ay
            val lenSq = dx * dx + dy * dy
            val t = if (lenSq <= 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0f, 1f)
            val qx = ax + t * dx
            val qy = ay + t * dy
            return kotlin.math.hypot(px - qx, py - qy)
        }

        /** Deterministic 0..1 from a seed — the bloom's hash, for the same reasons. */
        fun hash(seed: Float): Float {
            val x = sin(seed * 12.9898f) * 43758.5453f
            return x - floor(x)
        }
    }
}

/**
 * The wood, rendered a tile at a time.
 *
 * Each tree is a little halftone: a disc of dots whose radius falls off from the centre,
 * which is the same material the bloom and the fronds are made of. Literal tree icons were
 * the alternative and would have made the map the one screen in the app drawn in a different
 * language.
 *
 * Two things are deliberately tied to zoom.
 *
 * **Density.** Trees carry a level, and only those at or above the level a zoom asks for are
 * drawn. At z16 an 18-metre lattice is eight screen pixels apart — canopies touching their
 * neighbours, which is mud rather than a wood — so three in four are dropped. By z17 the
 * spacing is wide enough to draw them all. Because the test is a threshold on a fixed
 * per-tree level rather than a reshuffle, a tree that appears at a coarse zoom stays exactly
 * where it is as you zoom in.
 *
 * **Presence.** Below [MinTreeZoom] there is no tile at all: a canopy would be under a pixel
 * across, and a map dusted with sub-pixel speckle just looks like a dirty screen. The trail
 * overview at zoom 14.5 therefore has no trees on it — they arrive as you go in to see where
 * you are, which is also the only zoom at which a tree means anything.
 */
class TreeTileProvider(private val trees: TrailTrees) : TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        if (zoom < MinTreeZoom || trees.size() == 0) return TileProvider.NO_TILE

        val span = trees.tileSpanMetres(zoom)
        val left = trees.tileLeftMetres(x, zoom)
        val top = trees.tileTopMetres(y, zoom)
        val margin = TrailTrees.MaxCanopyMetres

        // Whole tiles outside the corridor are the common case once the camera is anywhere
        // but on the trail, and answering them without allocating a bitmap is most of what
        // keeps this cheap.
        if (left + span + margin < trees.boundsMinX() || left - margin > trees.boundsMaxX()) {
            return TileProvider.NO_TILE
        }
        if (top + span + margin < trees.boundsMinY() || top - margin > trees.boundsMaxY()) {
            return TileProvider.NO_TILE
        }

        val level = levelFor(span)
        val picked = ArrayList<Int>(64)
        trees.collect(
            (left - margin).toFloat(), (top - margin).toFloat(),
            (left + span + margin).toFloat(), (top + span + margin).toFloat(),
            level, picked,
        )
        if (picked.isEmpty()) return TileProvider.NO_TILE

        val pxPerMetre = (TilePx / span).toFloat()
        val bitmap = Bitmap.createBitmap(TilePx, TilePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TreeInk }
        val fade = fadeFor(zoom)

        for (i in picked) {
            val cx = ((trees.treeX(i) - left) * pxPerMetre).toFloat()
            val cy = ((trees.treeY(i) - top) * pxPerMetre).toFloat()
            // Size varies per tree, from its own index, so a wood is not a tiling of one
            // stamp. The floor keeps a canopy visible when the ground truth is sub-pixel.
            val vary = 0.75f + 0.55f * TrailTrees.hash(i * 7.31f)
            val r = (TrailTrees.CanopyMetres * vary * pxPerMetre).coerceAtLeast(MinCanopyPx)
            drawCanopy(canvas, paint, cx, cy, r, fade, i)
        }

        val out = ByteArrayOutputStream(8 * 1024)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return Tile(TilePx, TilePx, out.toByteArray())
    }

    /**
     * One canopy.
     *
     * Below a few pixels there is no room for a halftone — the dots would be larger than the
     * tree — so it degenerates to a single dot and the wood becomes a stipple, which is what
     * a forest looks like from far away anyway.
     */
    private fun drawCanopy(
        canvas: Canvas,
        paint: Paint,
        cx: Float,
        cy: Float,
        r: Float,
        fade: Float,
        seed: Int,
    ) {
        if (r < HalftoneFloorPx) {
            paint.alpha = (255 * 0.85f * fade).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, r, paint)
            return
        }
        // Fixed pitch, not a fraction of the canopy.
        //
        // Scaling the screen with the drawing is the same mistake the bloom's dot field made
        // and documents: tie the pitch to the tree and a canopy four times the size comes out
        // as four fat blobs rather than as four times as many dots, because its texture grew
        // with it. A halftone belongs to the surface it is printed on. Held fixed, zooming in
        // resolves *more* of the canopy, which is what zooming in ought to do.
        val pitch = HalftonePitchPx
        var gy = -r
        while (gy <= r) {
            var gx = -r
            while (gx <= r) {
                val d = kotlin.math.hypot(gx, gy) / r
                if (d <= 1f) {
                    // Denser at the crown, thinning at the rim — the same coverage-to-radius
                    // rule the bloom's dots follow.
                    val cover = 1f - d * d
                    val jitter = TrailTrees.hash(seed * 3.7f + gx * 1.9f + gy * 4.3f)
                    val dot = pitch * 0.46f * cover * (0.7f + 0.3f * jitter)
                    if (dot > 0.35f) {
                        paint.alpha = (255 * (0.45f + 0.55f * cover) * fade).toInt().coerceIn(0, 255)
                        canvas.drawCircle(
                            cx + gx + (jitter - 0.5f) * pitch * 0.3f,
                            cy + gy + (TrailTrees.hash(jitter) - 0.5f) * pitch * 0.3f,
                            dot,
                            paint,
                        )
                    }
                }
                gx += pitch
            }
            gy += pitch
        }
    }

    /**
     * How much of the wood a zoom is allowed to see.
     *
     * Derived from the on-screen pitch rather than tabulated per zoom, so changing
     * `SpacingMetres` cannot leave a hand-written table quietly disagreeing with it.
     */
    private fun levelFor(tileSpanMetres: Double): Int {
        val pitchPx = SpacingMetresOnScreen * TileLogicalPx / tileSpanMetres
        var level = 0
        var pitch = pitchPx
        while (pitch < TargetPitchPx && level < 3) {
            pitch *= 2
            level++
        }
        return level
    }

    /** The coarsest zoom the wood appears at is also the faintest, so it arrives rather than pops. */
    private fun fadeFor(zoom: Int): Float = if (zoom <= MinTreeZoom) 0.85f else 1f

    private companion object {
        /**
         * Rendered at double the nominal tile size. The SDK scales a tile to the same
         * geography whatever its pixel size, so 512 is simply a sharper 256 on the high
         * density screens this runs on, at the cost of a PNG four times the area.
         */
        const val TilePx = 512
        const val TileLogicalPx = 256.0

        /**
         * Below this the canopy is barely a pixel across and the wood is only noise, so no
         * tile is produced at all. The trail overview at zoom 14.5 therefore has no trees on
         * it; they arrive at "where am I" (zoom 16) and are fully there by the 3D and
         * follow cameras (17.5 and 18), which are the zooms at which a tree means anything.
         */
        const val MinTreeZoom = 16

        const val SpacingMetresOnScreen = 18.0

        /** The on-screen pitch the density thinning aims for. */
        const val TargetPitchPx = 13.0

        const val MinCanopyPx = 1.5f
        const val HalftoneFloorPx = 3.2f

        /** The halftone's pitch, in tile pixels. Fixed — see [drawCanopy]. */
        const val HalftonePitchPx = 5.0f

        /**
         * Canopy ink. Lighter than the styled ground (`landscape.natural` is `#1a2c1e`,
         * `poi.park` `#1c3322`) so the wood reads against it, and well short of the route's
         * own green so it never competes with the line the screen is actually about.
         */
        val TreeInk = 0xFF3C6247.toInt()
    }
}
