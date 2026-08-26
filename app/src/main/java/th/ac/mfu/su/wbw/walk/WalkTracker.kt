package th.ac.mfu.su.wbw.walk

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The latest position fix, flattened to what the map camera needs. */
data class WalkFix(
    val latitude: Double,
    val longitude: Double,
    /** Smoothed direction of travel, or null while too slow for it to mean anything. */
    val bearingDegrees: Float?,
)

/**
 * A walk in progress, or the one that just finished.
 *
 * Carried no step count since 0.4.1. The pedometer needed ACTIVITY_RECOGNITION, and that
 * one permission put the app under Play's Health apps policy — organization accounts only.
 * Distance is measured from location regardless, so nothing here depended on it.
 */
data class WalkStats(
    val active: Boolean = false,
    val distanceMetres: Double = 0.0,
    /** Smoothed ground speed, metres per second. */
    val speedMps: Float = 0f,
    val fix: WalkFix? = null,

    /**
     * How far along the event's route the walker has got, metres, or null if the route
     * has never had a fix near it this walk.
     *
     * Deliberately not the same number as [distanceMetres]. That one is how far the
     * person has walked — every wander to a food stall included — and it only ever goes
     * up. This one is how much of *the route* is behind them, which is what "am I nearly
     * finished" actually asks. Somebody who walks 500 m to a viewpoint and back has
     * added a kilometre to [distanceMetres] and nothing at all to this.
     */
    val routeMetres: Double? = null,
    /** The route's full length, so a consumer can turn [routeMetres] into a fraction. */
    val routeLengthMetres: Double = 0.0,
) {
    /** True once a walk has produced something worth showing, running or not. */
    val hasData: Boolean get() = active || distanceMetres > 0.0

    /** 0..1 along the route, or null while it is unknown. */
    val routeFraction: Float?
        get() {
            val m = routeMetres ?: return null
            if (routeLengthMetres <= 0.0) return null
            return (m / routeLengthMetres).toFloat().coerceIn(0f, 1f)
        }

    /** Metres of route still to walk, or null while unknown. */
    val routeRemainingMetres: Double?
        get() = routeMetres?.let { (routeLengthMetres - it).coerceAtLeast(0.0) }

    /**
     * Whether the loop has been walked.
     *
     * Short of the full length on purpose. The finish is a place on a hillside, not a
     * line drawn to the metre, and a GPS fix that settles fifteen metres short of the
     * last recorded track point is somebody standing at the finish — refusing them the
     * completion for it would be the app arguing with what they can see.
     */
    val routeComplete: Boolean
        get() = routeRemainingMetres?.let { it <= RouteCompleteSlackMetres } == true
}

/** How close to the end counts as finished. See [WalkStats.routeComplete]. */
const val RouteCompleteSlackMetres = 40.0

/**
 * Process-wide handle on the current walk.
 *
 * A singleton because the two halves of this feature live in different lifecycles: the
 * numbers are produced by [WalkTrackingService], which outlives the screen on purpose, and
 * consumed by the map, which is destroyed and recreated every time the user changes tab.
 * Binding the service to the composable would tie the walk to the thing it is specifically
 * meant to survive.
 *
 * The state lives in memory only. A foreground service makes the process very unlikely to
 * be killed mid-walk, but if it is, the walk is gone — there is no backend endpoint to
 * record it against yet, so nothing is persisted rather than half-persisted.
 */
object WalkTracker {

    private val _stats = MutableStateFlow(WalkStats())
    val stats: StateFlow<WalkStats> = _stats.asStateFlow()

    /** Called only by [WalkTrackingService]. */
    internal fun publish(stats: WalkStats) {
        _stats.value = stats
    }

    /** Clears the previous walk's numbers and starts recording a new one. */
    fun start(context: Context) {
        _stats.value = WalkStats(active = true)
        ContextCompat.startForegroundService(
            context,
            Intent(context, WalkTrackingService::class.java),
        )
    }

    /**
     * Stops recording but keeps the numbers on screen. Somebody who has just walked 8km
     * should not have the total wiped by the same tap that ends the walk; [start] clears
     * them when the next one begins.
     */
    fun stop(context: Context) {
        context.startService(
            Intent(context, WalkTrackingService::class.java).setAction(WalkTrackingService.ActionStop),
        )
    }
}
