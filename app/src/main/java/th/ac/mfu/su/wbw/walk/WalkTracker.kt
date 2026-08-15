package th.ac.mfu.su.wbw.walk

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Spelled out rather than referenced as `Manifest.permission.ACTIVITY_RECOGNITION` so it
 * resolves on every compileSdk — the constant only exists from API 29, while the string is
 * the stable half of the contract. Shared, because the screen asks for it and the service
 * checks it, and two copies of a permission name is how those two drift apart.
 */
internal const val PermissionActivityRecognition = "android.permission.ACTIVITY_RECOGNITION"

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
 * [steps] is nullable rather than defaulting to zero, and that distinction is the whole
 * point of the type: a phone with no pedometer and a walker who has not moved are very
 * different claims, and showing "0 steps" for the first is a lie the UI would have no way
 * to detect. Null means "this device cannot tell you", and the HUD says so.
 */
data class WalkStats(
    val active: Boolean = false,
    val distanceMetres: Double = 0.0,
    val steps: Int? = null,
    /** Smoothed ground speed, metres per second. */
    val speedMps: Float = 0f,
    val fix: WalkFix? = null,
) {
    /** True once a walk has produced something worth showing, running or not. */
    val hasData: Boolean get() = active || distanceMetres > 0.0 || steps != null
}

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
