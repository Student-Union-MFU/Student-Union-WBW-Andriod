package th.ac.mfu.su.wbw.ui.map

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import th.ac.mfu.su.wbw.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.hypot

/**
 * The event's walking route, baked into the app.
 *
 * Static rather than fetched. The trail does not change during the event, so asking a
 * routing service for it once per user per launch would spend a quota — and require a
 * network — to be told the same 8.3km every time. It also means the line draws on a
 * phone with no signal halfway up the hill, which is the condition this app is actually
 * used in.
 *
 * The geometry lives in `res/raw/route_wbw.json` rather than in a string constant here:
 * it is data, it was generated rather than written, and keeping it in a resource meant
 * that swapping the first reconstruction of the route for the organisers' actual GPX was
 * a one-file change touching no code. See that file's `_comment` for its provenance, and
 * `route.gpx` in the project root for the track it was baked from.
 */
@Serializable
private data class RouteFile(
    val distanceMetres: Int,
    val polyline: String,
)

/**
 * The decoded route: the path itself plus how long it is.
 *
 * No walking time. The route came from a GPX track with no timestamps in it, and the one
 * number that could be put here instead — distance divided by an assumed pace — would be a
 * guess wearing the costume of measured data. If the event wants an expected duration it
 * should come from the organisers, who know the stops.
 */
class TrailRoute(
    val points: List<LatLng>,
    /** Ground distance along the path, metres, summed from the track. */
    val distanceMetres: Int,
) {
    val start: LatLng get() = points.first()
    val end: LatLng get() = points.last()

    /** Everything the path touches — what the camera is fitted to when the map opens. */
    val bounds: LatLngBounds = LatLngBounds.builder().apply {
        points.forEach { include(it) }
    }.build()

    /**
     * The route in local metres, east and north of a fixed origin.
     *
     * A plain equirectangular projection about the route's own centre. Over a box two
     * kilometres across the error against a proper geodesic is centimetres, and the question
     * being asked of it — which way does the path run here — is answered in degrees.
     */
    private val originLat = bounds.center.latitude
    private val originLng = bounds.center.longitude
    private val metresPerDegLng = MetresPerDegree * cos(originLat * PI / 180.0)
    private val east = DoubleArray(points.size) { (points[it].longitude - originLng) * metresPerDegLng }
    private val north = DoubleArray(points.size) { (points[it].latitude - originLat) * MetresPerDegree }

    /**
     * Distance along the path at each point, metres, from the start.
     *
     * Summed from the same local projection the heading uses rather than from
     * [distanceMetres], so a position projected onto segment *i* can be turned into "you
     * are 3,140 m along" by interpolating within that segment. The last entry is the
     * route's own length as measured here, which is within a few metres of the GPX's own
     * figure and is the number progress is reported against — mixing the two would let
     * the walk finish at 99.8%.
     */
    private val cumulative: DoubleArray = DoubleArray(points.size).also { c ->
        for (i in 1 until points.size) {
            val dx = east[i] - east[i - 1]
            val dy = north[i] - north[i - 1]
            c[i] = c[i - 1] + hypot(dx, dy)
        }
    }

    /** The route's length in the projection progress is measured in. */
    val lengthMetres: Double get() = cumulative.lastOrNull() ?: 0.0

    /**
     * How far along the route a position is, given how far along it was last time.
     *
     * Returns metres from the start, or null when the position is further from the path
     * than [OffRouteMetres] — which the caller should treat as "keep the last known
     * progress", not as "you are back at the beginning".
     *
     * **Why this takes the previous value.** The trail doubles back near itself: around
     * the 4.2 km mark it passes within about 120 m of the 4.6 km mark, which is inside the
     * error a phone under tree cover can produce. A plain nearest-point search is
     * therefore ambiguous exactly where being wrong costs the most — one bad fix and a
     * walker is told they have 900 m still to do that they have already done, or the
     * reverse. Searching forward from where they already were resolves it the way a person
     * would: you got here by walking, so you are near where you were.
     *
     * The same reasoning covered the old route, which was a loop finishing where it
     * started; this one runs point to point. The window is what makes the algorithm
     * indifferent to which shape the trail happens to be.
     *
     * The window is asymmetric. [ForwardWindowMetres] ahead, because a phone that lost
     * signal in a dip can legitimately reappear a few hundred metres up the trail, and
     * only [BackWindowMetres] behind, because GPS jitter is metres and doubling back is
     * rare — a wide backward window would let noise drag a walk's progress down again.
     *
     * Pass a negative [fromMetres] for the first fix of a walk, which searches the whole
     * route: there is no previous position to be near, and somebody may well join part
     * way along.
     */
    fun progressFrom(fromMetres: Double, latitude: Double, longitude: Double): Double? {
        if (points.size < 2) return null
        val px = (longitude - originLng) * metresPerDegLng
        val py = (latitude - originLat) * MetresPerDegree

        val acquiring = fromMetres < 0.0
        val lo = if (acquiring) Double.NEGATIVE_INFINITY else fromMetres - BackWindowMetres
        val hi = if (acquiring) Double.POSITIVE_INFINITY else fromMetres + ForwardWindowMetres

        var bestAlong = -1.0
        var bestDistSq = Double.MAX_VALUE
        for (i in 0 until points.size - 1) {
            // Skip whole segments outside the window before doing any work on them.
            if (cumulative[i + 1] < lo || cumulative[i] > hi) continue

            val ax = east[i]; val ay = north[i]
            val dx = east[i + 1] - ax; val dy = north[i + 1] - ay
            val lenSq = dx * dx + dy * dy
            val t = if (lenSq <= 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0.0, 1.0)
            val qx = ax + t * dx - px
            val qy = ay + t * dy - py
            val distSq = qx * qx + qy * qy
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestAlong = cumulative[i] + t * hypot(dx, dy)
            }
        }

        if (bestAlong < 0.0 || bestDistSq > OffRouteMetres * OffRouteMetres) return null
        // Never hand back less than we already had. Within the backward window a jittering
        // fix would otherwise make the walked line twitch backwards on the map, which reads
        // as the app losing track rather than as the metre of noise it is.
        return if (acquiring) bestAlong else maxOf(fromMetres, bestAlong)
    }

    /**
     * The route cut in two at [metres] along it: what has been walked, and what is left.
     *
     * The cut point is interpolated inside whichever segment it falls in and appears as
     * the last point of the first list *and* the first point of the second, so the two
     * polylines meet exactly rather than leaving a gap that widens with segment length.
     *
     * Either half can be empty — at the very start nothing is walked, at the finish
     * nothing remains — and a caller should skip drawing a polyline of fewer than two
     * points rather than hand the Maps SDK a degenerate line.
     */
    fun splitAt(metres: Double): Pair<List<LatLng>, List<LatLng>> {
        if (points.size < 2) return emptyList<LatLng>() to points
        val d = metres.coerceIn(0.0, lengthMetres)

        var seg = 0
        while (seg < points.size - 2 && cumulative[seg + 1] < d) seg++

        val segLen = cumulative[seg + 1] - cumulative[seg]
        val t = if (segLen <= 0.0) 0.0 else ((d - cumulative[seg]) / segLen).coerceIn(0.0, 1.0)
        val a = points[seg]
        val b = points[seg + 1]
        val cut = LatLng(
            a.latitude + t * (b.latitude - a.latitude),
            a.longitude + t * (b.longitude - a.longitude),
        )

        val walked = points.subList(0, seg + 1) + cut
        val remaining = listOf(cut) + points.subList(seg + 1, points.size)
        return walked to remaining
    }

    /**
     * Which way the trail runs nearest to a position, in degrees clockwise from north, or
     * null if that position is further from the path than [OffRouteMetres].
     *
     * This exists for the first seconds of a walk. A heading derived from movement cannot
     * exist until there has been movement — [th.ac.mfu.su.wbw.walk.WalkTrackingService]
     * refuses to believe a bearing below 0.7 m/s, because a stationary phone reports one that
     * wanders freely — so for the first few fixes there is nothing to point the camera with,
     * and it sat facing north until the walker had gone far enough to prove otherwise.
     *
     * But the direction is not actually unknown: this is a fixed loop, the walker is standing
     * on it, and the way the path runs under their feet is the way they are about to go. So
     * the trail answers the question until the walk itself can.
     *
     * The heading is taken over [LookAheadMetres] rather than from the nearest segment alone.
     * The track is a recorded GPX with points a few metres apart, and the direction of any
     * single one of those is mostly the noise in the recording.
     *
     * It can be wrong exactly once: somebody walking the loop against its recorded direction
     * gets pointed backwards until their own bearing arrives a few seconds later. Guessing
     * with the trail beats facing north regardless of where the trail goes.
     */
    fun headingAt(latitude: Double, longitude: Double): Float? {
        if (points.size < 2) return null
        val px = (longitude - originLng) * metresPerDegLng
        val py = (latitude - originLat) * MetresPerDegree

        var bestSeg = -1
        var bestT = 0.0
        var bestDistSq = Double.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val ax = east[i]; val ay = north[i]
            val dx = east[i + 1] - ax; val dy = north[i + 1] - ay
            val lenSq = dx * dx + dy * dy
            val t = if (lenSq <= 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0.0, 1.0)
            val qx = ax + t * dx - px
            val qy = ay + t * dy - py
            val distSq = qx * qx + qy * qy
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestSeg = i
                bestT = t
            }
        }
        if (bestSeg < 0 || bestDistSq > OffRouteMetres * OffRouteMetres) return null

        // Where on the path the walker actually is, then a look ahead from there.
        val fromX = east[bestSeg] + bestT * (east[bestSeg + 1] - east[bestSeg])
        val fromY = north[bestSeg] + bestT * (north[bestSeg + 1] - north[bestSeg])

        var toX = fromX
        var toY = fromY
        var covered = 0.0
        var i = bestSeg + 1
        while (i < points.size && covered < LookAheadMetres) {
            val nx = east[i]
            val ny = north[i]
            covered += hypot(nx - toX, ny - toY)
            toX = nx
            toY = ny
            i++
        }
        // A walker on the final segment has nothing in front of them; the loop's own start is
        // where the path continues, so the heading comes from behind them instead.
        if (covered <= 0.0) {
            toX = fromX + (fromX - east[bestSeg])
            toY = fromY + (fromY - north[bestSeg])
            if (toX == fromX && toY == fromY) return null
        }

        val deg = Math.toDegrees(atan2(toX - fromX, toY - fromY))
        return ((deg + 360.0) % 360.0).toFloat()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Good to a fraction of a percent anywhere, and this is used over two kilometres. */
        private const val MetresPerDegree = 111_320.0

        /**
         * Past this the walker is not on the trail and it has no opinion about where they
         * are going. Wide enough to cover a car park or a wrong turn at a junction.
         */
        private const val OffRouteMetres = 120.0

        /** How far along the path the heading is measured, to average out GPX jitter. */
        private const val LookAheadMetres = 25.0

        /**
         * How far ahead of the last known position [progressFrom] will look.
         *
         * Generous, because the gap it covers is a real one: the trail runs through dips
         * with no signal, and a phone that goes quiet for two minutes of walking
         * reappears a couple of hundred metres further on. Too small a window and that
         * walker's progress sticks at the last place they had a fix.
         */
        private const val ForwardWindowMetres = 400.0

        /** Jitter is metres. This is for that, not for doubling back. */
        private const val BackWindowMetres = 30.0

        /**
         * Read and decode the baked route. Cheap enough to call from composition — a
         * ~900-byte read and a few hundred integer decodes — but [MapScreen] still holds
         * it in a `remember` so a recomposition does not repeat it.
         */
        fun load(context: Context): TrailRoute {
            val text = context.resources.openRawResource(R.raw.route_wbw)
                .bufferedReader()
                .use { it.readText() }
            val file = json.decodeFromString(RouteFile.serializer(), text)
            return TrailRoute(
                points = decodePolyline(file.polyline),
                distanceMetres = file.distanceMetres,
            )
        }
    }
}

/**
 * Google's encoded-polyline format, precision 5.
 *
 * Twenty lines rather than a dependency: `android-maps-utils` carries a `PolyUtil` that
 * does exactly this, but pulling the whole utility library in to decode one baked string
 * is more surface than the problem has. The format is frozen and has been for years.
 *
 * Each coordinate is stored as a delta from the previous one, zig-zag encoded so negatives
 * stay small, then split into 5-bit groups with the high bit set on every group but the
 * last and offset by 63 into printable ASCII.
 */
private fun decodePolyline(encoded: String): List<LatLng> {
    val points = ArrayList<LatLng>(encoded.length / 2)
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        // Latitude delta, then longitude delta — same unpacking both times.
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        result = 0
        shift = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

        points.add(LatLng(lat / 1e5, lng / 1e5))
    }
    return points
}
