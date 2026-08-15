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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.roundToInt

/**
 * Records a walk: distance, steps and speed, for as long as it is running.
 *
 * A foreground service rather than screen-scoped state because of how this app is used —
 * an 8.4km hike is walked with the phone in a pocket and the screen off, and anything tied
 * to the composable would stop counting the moment that happened, then show a total that
 * silently omitted most of the walk. A wrong number presented confidently is worse than no
 * number, so the tracking outlives the UI or it does not exist.
 *
 * Nothing here binds. The screen reads [WalkTracker.stats]; this only writes to it.
 */
class WalkTrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private var sensors: SensorManager? = null
    private var stepSensor: Sensor? = null

    /**
     * The point distance is measured from — held still until the walker has genuinely left
     * it. Advancing it on every fix would let GPS jitter accumulate into hundreds of metres
     * of "walking" done standing at a checkpoint.
     */
    private var anchor: Location? = null

    /**
     * TYPE_STEP_COUNTER counts from the last reboot, not from now, so the first reading is
     * an offset to subtract rather than a step total. -1 means "not yet seen one".
     */
    private var stepBaseline: Long = -1L
    private var steps: Int? = null

    private var distanceMetres = 0.0
    private var speedMps = 0f
    private var bearing: Float? = null

    /** Last distance the notification was rebuilt for, so it is not rewritten per fix. */
    private var notifiedAtMetres = -1

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onFix)
        }
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val total = event.values.firstOrNull()?.toLong() ?: return
            if (stepBaseline < 0) stepBaseline = total
            steps = (total - stepBaseline).toInt().coerceAtLeast(0)
            publish()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ActionStop) {
            stopSelf()
            return START_NOT_STICKY
        }

        createChannel()
        startInForeground()
        beginTracking()

        // NOT sticky. The walk's numbers live in memory, so a service restarted by the
        // system after a process kill would come back as a fresh session claiming to be
        // the same walk. Ending honestly beats resuming with a zeroed total.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { fused.removeLocationUpdates(locationCallback) }
        runCatching { sensors?.unregisterListener(stepListener) }
        // Keep whatever was recorded on screen; only the "recording" flag drops.
        publish(active = false)
        super.onDestroy()
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

        // Steps are best-effort in two separate ways: the permission may be refused, and
        // plenty of hardware has no pedometer at all (emulators included). Either way
        // `steps` stays null and the HUD says the count is unavailable rather than zero.
        val needsRecognition = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (!needsRecognition || hasPermission(PermissionActivityRecognition)) {
            sensors = getSystemService(SensorManager::class.java)
            stepSensor = sensors?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepSensor?.let { sensors?.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_UI) }
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
            if (moved >= MinMoveMetres) {
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

        publish()
        updateNotification()
    }

    private fun publish(active: Boolean = true) {
        WalkTracker.publish(
            WalkStats(
                active = active,
                distanceMetres = distanceMetres,
                steps = steps,
                speedMps = speedMps,
                fix = anchor?.let { WalkFix(it.latitude, it.longitude, bearing) },
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

        private const val ChannelId = "wbw_walk"
        private const val NotificationId = 4101

        /** Roughly one fix per two seconds — enough for a walking camera, not a drain. */
        private const val UpdateIntervalMillis = 2_000L
        private const val MinUpdateIntervalMillis = 1_000L

        /** Fixes vaguer than this are discarded rather than believed. */
        private const val MaxAccuracyMetres = 25f

        /** How far from the anchor counts as having walked rather than as GPS noise. */
        private const val MinMoveMetres = 2.5f

        /** Below this, a reported heading is noise, so the camera keeps the last one. */
        private const val MinBearingSpeedMps = 0.7f

        private const val SpeedSmoothing = 0.3f
        private const val BearingSmoothing = 0.25f

        /** Notification text is rebuilt per 100m. */
        private const val NotificationStepMetres = 100.0
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
