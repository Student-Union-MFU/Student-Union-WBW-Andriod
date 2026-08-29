package th.ac.mfu.su.wbw.walk

import android.content.Context
import th.ac.mfu.su.wbw.WbwApplication

/**
 * The walk on disk, so a process kill does not take it with it.
 *
 * The service makes the process very unlikely to be killed, not immune to it. Eight
 * kilometres is a few hours with the screen off and the phone in a pocket, and some
 * vendors' battery management will end a foreground service anyway. In memory alone that
 * is a participant who walked six kilometres and has nothing to show — the one outcome
 * worse than a slightly stale number.
 *
 * [SharedPreferences][android.content.SharedPreferences] rather than DataStore, matching
 * [th.ac.mfu.su.wbw.data.local.AppSettings] and
 * [th.ac.mfu.su.wbw.data.local.ResponseCache], and for their reason: the read happens at
 * process start before the first composition, and anything suspending would let a frame
 * through showing no walk before the restored one arrived. Nine primitives is not a
 * payload worth an asynchronous API.
 *
 * Not part of [th.ac.mfu.su.wbw.data.local.ResponseCache]: that holds what the *server*
 * said and is wiped on login and logout, which is right for a pass and wrong for a walk.
 * A walk is measured on this device and belongs to whoever is holding it.
 */
internal object WalkStore {

    private val prefs by lazy {
        WbwApplication.appContext.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
    }

    /** Wall clock of the last write, to throttle. Not [android.os.SystemClock] — see [load]. */
    private var lastWriteAtMillis = 0L

    /**
     * Writes the walk down, at most once every [WriteIntervalMillis] while it runs.
     *
     * Throttled because fixes land every two seconds and a walk lasts hours: writing each
     * one would be thousands of disk touches to save, at most, the last few metres. A
     * walk that ends — by the user or by [WalkTrackingService.onDestroy] — is written
     * immediately regardless, because that is the copy that has to be complete.
     */
    fun save(stats: WalkStats) {
        if (!stats.hasData) return
        val now = System.currentTimeMillis()
        val ending = !stats.active
        if (!ending && now - lastWriteAtMillis < WriteIntervalMillis) return
        lastWriteAtMillis = now

        prefs.edit()
            .putFloat(KeyDistance, stats.distanceMetres.toFloat())
            .putFloat(KeyRoute, (stats.routeMetres ?: -1.0).toFloat())
            .putFloat(KeyRouteLength, stats.routeLengthMetres.toFloat())
            // Raw bits rather than a float. SharedPreferences has no putDouble, and a
            // float carries about seven digits — enough to put a longitude out here by
            // the better part of a metre. That is under [WalkTrackingService]'s own
            // move threshold and would never show, but this is the point the next
            // kilometre is measured from, and it costs nothing to store it exactly.
            .putLong(KeyLat, java.lang.Double.doubleToRawLongBits(stats.fix?.latitude ?: Double.NaN))
            .putLong(KeyLng, java.lang.Double.doubleToRawLongBits(stats.fix?.longitude ?: Double.NaN))
            .putFloat(KeyBearing, stats.fix?.bearingDegrees ?: Float.NaN)
            // What separates "the process died under it" from "the user tapped Stop".
            // Only the first is offered a resume; see [load].
            .putBoolean(KeyActive, stats.active)
            .putLong(KeySavedAt, now)
            .apply()
    }

    /**
     * Milliseconds since the stored walk was last written, or null if there is none.
     *
     * For a walk that was interrupted this is how long the app was gone: the last write
     * happened under the dying process, and nothing writes again until recording restarts.
     * [WalkTrackingService] measures the resume gap with it — see `seedFromRestoredWalk`.
     *
     * Null rather than zero for "no record", so a caller cannot mistake the absence of a
     * walk for a walk that was saved this instant. Negative when the wall clock has moved
     * backwards under the record; every caller treats that as stale rather than as fresh,
     * which is the safe direction to be wrong in.
     */
    fun ageMillis(): Long? {
        if (!prefs.contains(KeySavedAt)) return null
        return System.currentTimeMillis() - prefs.getLong(KeySavedAt, 0L)
    }

    /**
     * The stored walk, or null if there is none or it is too old to mean anything.
     *
     * Always comes back `active = false`: nothing is recording at a cold start, whatever
     * was true when the process died. `interrupted` carries that instead, and it is what
     * the map offers a resume on.
     *
     * How old is "too old" depends on which of those two it is, because they are kept for
     * different reasons — see [MaxInterruptedAgeMillis] and [MaxCompletedAgeMillis].
     *
     * The age check uses the wall clock rather than [android.os.SystemClock.elapsedRealtime]
     * because it has to survive a reboot — a phone that died on the hill and was restarted
     * is exactly the case this exists for, and elapsed-realtime resets to zero there.
     * A clock the user has since moved reads as stale and drops the walk, which is the
     * safe direction to be wrong in.
     */
    fun load(): WalkStats? {
        val age = ageMillis()
        val interrupted = prefs.getBoolean(KeyActive, false)
        val limit = if (interrupted) MaxInterruptedAgeMillis else MaxCompletedAgeMillis
        if (age == null) return null
        if (age !in 0..limit) {
            clear()
            return null
        }

        val distance = prefs.getFloat(KeyDistance, 0f).toDouble()
        // A walk that never got past the move threshold before the process died. Nothing
        // to offer back and nothing to show, so it goes rather than sitting on disk until
        // it ages out — and going means the next `save` starts from a clean record.
        if (distance <= 0.0) {
            clear()
            return null
        }

        val lat = java.lang.Double.longBitsToDouble(prefs.getLong(KeyLat, NaNBits))
        val lng = java.lang.Double.longBitsToDouble(prefs.getLong(KeyLng, NaNBits))
        val bearing = prefs.getFloat(KeyBearing, Float.NaN)
        val route = prefs.getFloat(KeyRoute, -1f).toDouble()

        return WalkStats(
            active = false,
            distanceMetres = distance,
            // Deliberately not restored. Pace is a reading about *now*, and the walker has
            // been standing still since the process died; carrying the old figure over
            // would print a speed nobody is moving at.
            speedMps = 0f,
            fix = if (lat.isNaN() || lng.isNaN()) {
                null
            } else {
                WalkFix(lat, lng, bearing.takeUnless { it.isNaN() })
            },
            routeMetres = route.takeIf { it >= 0.0 },
            routeLengthMetres = prefs.getFloat(KeyRouteLength, 0f).toDouble(),
            interrupted = interrupted,
        )
    }

    fun clear() {
        lastWriteAtMillis = 0L
        prefs.edit().clear().apply()
    }

    private const val Prefs = "wbw_walk"

    private const val KeyDistance = "distance_m"
    private const val KeyRoute = "route_m"
    private const val KeyRouteLength = "route_length_m"
    private const val KeyLat = "fix_lat"
    private const val KeyLng = "fix_lng"
    private const val KeyBearing = "fix_bearing"
    private const val KeyActive = "was_active"
    private const val KeySavedAt = "saved_at"

    /** "No fix stored", in the same encoding [save] writes coordinates in. */
    private val NaNBits = java.lang.Double.doubleToRawLongBits(Double.NaN)

    private const val WriteIntervalMillis = 5_000L

    /**
     * How long an *interrupted* walk is worth offering back.
     *
     * The event is one day, and the walk itself is a few hours. Long enough that a phone
     * which died at the second checkpoint and was charged at the third still has its walk;
     * short enough that opening the app the next morning does not offer to resume
     * yesterday's.
     */
    private const val MaxInterruptedAgeMillis = 6L * 60 * 60 * 1000

    /**
     * How long a walk the participant *finished* is worth showing back.
     *
     * Much shorter, because it answers a different question. An interrupted walk is kept
     * so it can be carried on, which stays worth offering for as long as the event lasts.
     * A finished one is kept only so that a process killed in the minutes after the last
     * tap does not swallow the total — the walk is over either way. On the same six hours
     * it would put the morning's total back on the map at lunchtime, under a button
     * reading "Start walking" that would wipe it, with nothing on screen saying it was
     * hours old.
     */
    private const val MaxCompletedAgeMillis = 30L * 60 * 1000
}
