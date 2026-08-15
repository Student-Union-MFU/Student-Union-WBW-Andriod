package th.ac.mfu.su.wbw.ui.map

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import th.ac.mfu.su.wbw.R

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

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

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
