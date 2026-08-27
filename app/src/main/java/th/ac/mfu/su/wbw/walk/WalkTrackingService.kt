package th.ac.mfu.su.wbw.walk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import th.ac.mfu.su.wbw.MainActivity
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.map.TrailRoute
import kotlin.math.roundToInt

/**
 * Records a walk: distance and speed, for as long as it is running.
 *
 * A foreground service rather than screen-scoped state because of how this app is used —
 * a 5km hike is walked with the phone in a pocket and the screen off, and anything tied
 * to the composable would stop counting the moment that happened, then show a total that
 * silently omitted most of the walk. A wrong number presented confidently is worse than no
 * number, so the tracking outlives the UI or it does not exist.
 *
 * Nothing here binds. The screen reads [WalkTracker.stats]; this only writes to it.
 */
class WalkTrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient

    /**
     * The point distance is measured from — held still until the walker has genuinely left
     * it. Advancing it on every fix would let GPS jitter accumulate into hundreds of metres
     * of "walking" done standing at a checkpoint.
     */
    private var anchor: Location? = null

    /**
     * How far the first fix after a resume may be from the rebuilt [anchor] before that
     * anchor is disbelieved, or null when no resume is pending.
     *
     * A resumed anchor is a position from *before* the process died, so the first fix
     * measured against it spans however long the app was gone rather than the usual two
     * seconds. This is what that gap could plausibly have been walked in; see
     * [seedFromRestoredWalk]. Consumed by the first fix that lands.
     */
    private var resumeBudgetMetres: Double? = null

    private var distanceMetres = 0.0

    /**
     * The event route, and how far along it this walk has got.
     *
     * Held by the service rather than by the map so that progress accrues with the screen
     * off and the phone in a pocket, which is how the eight kilometres are actually
     * walked. Loaded once — it is a resource read, and the trail does not change.
     *
     * -1 means no fix has landed near the route yet this walk, which [TrailRoute.progressFrom]
     * reads as "search the whole line" rather than as "at the start".
     */
    private val route: TrailRoute by lazy { TrailRoute.load(this) }
    private var routeMetres = -1.0
    private var speedMps = 0f
    private var bearing: Float? = null

    /** Last distance the notification was rebuilt for, so it is not rewritten per fix. */
    private var notifiedAtMetres = -1

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onFix)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ActionStop) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ActionResume) seedFromRestoredWalk()

        createChannel()
        startInForeground()
        beginTracking()

        // Still NOT sticky, now for a different reason than before [WalkStore] existed.
        // The numbers survive a kill, so a restart would no longer zero them — but it
        // would restart *recording*, unasked, which the app's FOREGROUND_SERVICE_LOCATION
        // declaration says it never does. The walk is offered back on the map instead and
        // waits for a tap. See [WalkTracker.resume].
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { fused.removeLocationUpdates(locationCallback) }
        // Keep whatever was recorded on screen; only the "recording" flag drops.
        publish(active = false)
        super.onDestroy()
    }

    /**
     * Picks the walk back up where the dead process left it.
     *
     * [WalkTracker.resume] has already put the restored numbers into the flow, so they are
     * read from there rather than passed through the intent — an intent carrying six
     * doubles would be a second copy of the same state to keep in step with the first.
     *
     * The anchor is rebuilt from the stored fix so the first metres after resuming are
     * measured from where the walker actually is. Without it the first fix would become
     * the anchor and the walk would silently lose however far they moved while the app was
     * dead — the same class of quiet undercount the foreground service exists to prevent.
     *
     * But only when that gap is short enough to be worth measuring across, and only for as
     * far as it could have been *walked*. [WalkStore] keeps an interrupted walk for six
     * hours so it can still be offered back, which is a different question from whether
     * its last position is still where the walker is: a phone that died at the second base
     * and was charged in a truck back at the first would otherwise book that whole
     * straight line as walked on the first fix. So the anchor is rebuilt only inside
     * [MaxResumeGapMillis], and [resumeBudgetMetres] caps what the first fix may claim
     * from it. Past either, the anchor is left null and the first live fix becomes it —
     * losing the gap, which is the honest direction to be wrong in.
     */
    private fun seedFromRestoredWalk() {
        val restored = WalkTracker.stats.value
        distanceMetres = restored.distanceMetres
        routeMetres = restored.routeMetres ?: -1.0
        // Restored whatever the gap: it only points the camera, is overwritten by the
        // first fix taken while actually moving, and no distance is measured from it.
        bearing = restored.fix?.bearingDegrees

        // A negative gap is a clock moved backwards under the record, which says nothing
        // about where the walker is; it falls out of the range with everything too old.
        val gapMillis = WalkStore.ageMillis() ?: return
        if (gapMillis !in 0..MaxResumeGapMillis) return
        restored.fix?.let { fix ->
            anchor = Location(ResumeProvider).apply {
                latitude = fix.latitude
                longitude = fix.longitude
            }
            resumeBudgetMetres = gapMillis / 1000.0 * MaxResumeSpeedMps
        }
    }

    private fun beginTracking() {
        fused = LocationServices.getFusedLocationProviderClient(this)

        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UpdateIntervalMillis)
                .setMinUpdateIntervalMillis(MinUpdateIntervalMillis)
                .build()
            runCatching {
                fused.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            }
        }
    }

    private fun onFix(location: Location) {
        // A fix the provider itself does not trust is not evidence of movement. Without
        // this, a bad urban-canyon reading jumps the anchor and books 50m of walking.
        if (location.hasAccuracy() && location.accuracy > MaxAccuracyMetres) return

        val previous = anchor
        if (previous == null) {
            anchor = location
        } else {
            val moved = previous.distanceTo(location)
            // Spent here whether or not it bites: this is the first fix measured against a
            // resumed anchor, and every one after it is an ordinary two-second step.
            val budget = resumeBudgetMetres
            resumeBudgetMetres = null

            if (budget != null && moved > budget) {
                // Further than the gap could have been walked, so it was not walked —
                // a lift between bases, or a phone carried ahead in somebody's bag. The
                // anchor is abandoned and this fix starts the measuring again; the walk
                // keeps the metres it had rather than gaining ones nobody put in.
                anchor = location
            } else if (moved >= MinMoveMetres) {
                distanceMetres += moved
                anchor = location
            }
        }

        // Prefer the provider's own speed — it is derived from Doppler shift rather than
        // from differencing positions, so it is both faster to settle and less noisy.
        val raw = if (location.hasSpeed()) location.speed else speedMps
        speedMps = speedMps + SpeedSmoothing * (raw - speedMps)

        // Bearing is only meaningful once actually moving. Standing still, the reported
        // heading wanders freely, and a camera following it would spin on the spot.
        if (location.hasBearing() && speedMps >= MinBearingSpeedMps) {
            val next = location.bearing
            bearing = bearing?.let { smoothBearing(it, next, BearingSmoothing) } ?: next
        }

        // Where on the route that puts them. A null answer means this fix was further
        // from the trail than the route's own tolerance — off on a side path, or a bad
        // reading — and the right response is to keep the last known progress rather than
        // to reset it. See [TrailRoute.progressFrom].
        route.progressFrom(routeMetres, location.latitude, location.longitude)
            ?.let { routeMetres = it }

        publish()
        updateNotification()
    }

    private fun publish(active: Boolean = true) {
        WalkTracker.publish(
            WalkStats(
                active = active,
                distanceMetres = distanceMetres,
                speedMps = speedMps,
                fix = anchor?.let { WalkFix(it.latitude, it.longitude, bearing) },
                routeMetres = routeMetres.takeIf { it >= 0.0 },
                routeLengthMetres = route.lengthMetres,
            ),
        )
    }

    private fun hasPermission(name: String) =
        ContextCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED

    // ===== Notification =====

    private fun createChannel() {
        val channel = NotificationChannel(
            ChannelId,
            getString(R.string.walk_channel_name),
            // Low: this is a status line for something the user started, not an alert.
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, WalkTrackingService::class.java).setAction(ActionStop),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_wbw_logo)
            .setContentTitle(getString(R.string.walk_notification_title))
            .setContentText(getString(R.string.walk_notification_distance, distanceMetres / 1000.0))
            .setContentIntent(open)
            .addAction(0, getString(R.string.walk_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun startInForeground() {
        // The typed overload is API 29+, and from API 34 the matching FOREGROUND_SERVICE_*
        // permission is enforced — declaring the type is what makes a location service
        // legal to run in the background at all.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationId,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NotificationId, buildNotification())
        }
    }

    /** Rewritten only when the rounded figure would actually change. */
    private fun updateNotification() {
        val shown = (distanceMetres / NotificationStepMetres).toInt()
        if (shown == notifiedAtMetres) return
        notifiedAtMetres = shown
        runCatching {
            getSystemService(NotificationManager::class.java)
                .notify(NotificationId, buildNotification())
        }
    }

    companion object {
        const val ActionStop = "th.ac.mfu.su.wbw.walk.STOP"
        const val ActionResume = "th.ac.mfu.su.wbw.walk.RESUME"

        /** Provider name on the rebuilt anchor. Never read; a [Location] requires one. */
        private const val ResumeProvider = "wbw_resume"

        private const val ChannelId = "wbw_walk"
        private const val NotificationId = 4101

        /** Roughly one fix per two seconds — enough for a walking camera, not a drain. */
        private const val UpdateIntervalMillis = 2_000L
        private const val MinUpdateIntervalMillis = 1_000L

        /** Fixes vaguer than this are discarded rather than believed. */
        private const val MaxAccuracyMetres = 25f

        /** How far from the anchor counts as having walked rather than as GPS noise. */
        private const val MinMoveMetres = 2.5f

        /**
         * How stale the stored fix may be and still be worth resuming the measurement from.
         *
         * Long enough to cover what actually happens — a service killed by battery
         * management, or a phone that died and was restarted — including the moment it
         * takes to reopen the app and tap. Past it, nobody knows where the walker went in
         * between, and the first live fix should start afresh. See [seedFromRestoredWalk].
         */
        private const val MaxResumeGapMillis = 5L * 60 * 1000

        /**
         * The fastest the gap either side of a resume is treated as having been walked.
         *
         * A brisk walk is about 2 m/s. Anything quicker than that over the gap was
         * travelled some other way, and this app's total is meant to mean "you walked
         * this" — so it is a ceiling on what the first resumed fix may claim, not an
         * estimate of anybody's pace.
         */
        private const val MaxResumeSpeedMps = 2.0

        /** Below this, a reported heading is noise, so the camera keeps the last one. */
        private const val MinBearingSpeedMps = 0.7f

        private const val SpeedSmoothing = 0.3f
        private const val BearingSmoothing = 0.25f

        /**
         * How far the walker must go before the notification text is rebuilt.
         *
         * Matched to the resolution the text itself promises. `walk_notification_distance`
         * is "%.2f km", which resolves to ten metres, so rebuilding per hundred left the
         * notification showing multiples of 0.10 km and holding each one until the next
         * hundred-metre boundary — 184m walked read as "0.10 km so far". With the phone
         * pocketed that notification is the only readout there is, which is the whole
         * reason this service outlives the screen, so it is the last place to be a
         * hundred metres out.
         */
        private const val NotificationStepMetres = 10.0
    }
}

/**
 * Exponential smoothing around the compass, taking the short way round.
 *
 * Averaging 350° and 10° numerically gives 180° — the exact opposite of the true heading.
 * Folding the difference into ±180 first is what stops the camera swinging through a half
 * turn every time the walker crosses north.
 */
internal fun smoothBearing(previous: Float, next: Float, alpha: Float): Float {
    val delta = ((next - previous + 540f) % 360f) - 180f
    return (previous + alpha * delta + 360f) % 360f
}
