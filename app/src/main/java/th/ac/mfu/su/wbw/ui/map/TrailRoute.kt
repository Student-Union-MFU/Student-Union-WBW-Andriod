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
