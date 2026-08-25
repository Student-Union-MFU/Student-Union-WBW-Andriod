package th.ac.mfu.su.wbw.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantCheckpoint
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.WbwForestVoid
import th.ac.mfu.su.wbw.ui.theme.WbwGreenDark
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import th.ac.mfu.su.wbw.walk.PermissionActivityRecognition
import th.ac.mfu.su.wbw.walk.WalkStats
import th.ac.mfu.su.wbw.walk.WalkTracker
import kotlin.math.roundToInt

/**
 * The trail map.
 *
 * A full-bleed Google map restyled into the app's forest palette (`res/raw/map_style_forest
 * .json`) so it reads as part of the app rather than a white rectangle dropped into it —
 * Google's chrome is off and replaced with the app's glass. Three things sit on top:
 *
 *  - **3D** tilts the native camera and lets buildings extrude — the Maps SDK's own 3D, so
 *    the forest styling stays on (a photorealistic WebView would have thrown it away and
 *    needed the JavaScript API, which this does not).
 *
 * Location is one fix, not a stream — "where am I on the trail", not turn-by-turn.
 *
 * **Maps SDK only — no Places.** This screen once had an autocomplete search box and a
 * scatter of markers for whatever Google POIs sat near the walker. Both are gone. The
 * search answered a question nobody on a fixed 5km route was asking, and the nearby
 * markers came from `findCurrentPlace`, one of the priciest Places calls, fired
 * automatically on every visit to this tab rather than on a tap — a per-participant cost
 * for decoration. Everything that matters here (the route, its endpoints, "where am I")
 * comes from the baked polyline and the fused location provider, neither of which is
 * billed. Don't reintroduce Places without a feature that needs it.
 *
 * The Maps key comes from `local.properties` via the manifest, read by the SDK itself; with
 * no key the tiles come back blank but nothing here crashes.
 */
@Composable
fun MapScreen(
    contentPadding: PaddingValues,
    /**
     * Whether this map carries the participant's emergency layer — the SOS button, and the
     * long-poll watching for a case to be acknowledged.
     *
     * False for the staff shell, which shows this same map on its own tab. Not a matter of
     * tidiness: both halves of that layer are scoped to a *participant*. `POST /wbw/me/sos`
     * and `GET /wbw/me/sos/active` are `WHERE u.role = 'participant'` like the rest of
     * `/me`, so on a staff account the button raises a case that cannot exist and the watch
     * loop fails every round, backing off to a permanent thirty-second retry against an
     * endpoint that will never answer differently.
     *
     * The staff equivalent is not a button on this screen. It is the alerts console in
     * [th.ac.mfu.su.wbw.ui.staff.StaffHomeScreen] — staff answer emergencies rather than
     * raise them.
     */
    emergency: Boolean = true,
) {
    val colors = wbwColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun granted() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    var hasLocation by remember { mutableStateOf(granted()) }
    var is3d by remember { mutableStateOf(false) }

    // False until the SDK says it has drawn a frame — see the loading cover at the bottom
    // of this Box.
    var mapReady by remember { mutableStateOf(false) }

    /**
     * The map's own padded region, which is a different thing from the screen's
     * [contentPadding] and has to be, because the Maps SDK draws the Google wordmark at the
     * bottom of it.
     *
     * Given the screen's padding, the wordmark landed 96dp up — above the floating nav bar,
     * in the gap between it and the walk button, where it read as a stray label rather than
     * as an attribution. Given only the system inset, it sits under the nav bar and along
     * the bottom edge of the screen, which is where a watermark belongs and where the
     * wordmark is on every other map anyone has used.
     *
     * The band under the floating bar is not tall enough to hold the wordmark outright. On
     * this screen, measured: the bar's lower edge is 110px off the bottom, the wordmark plus
     * the margin the Maps SDK gives it is 65px, and the system inset takes the lowest 63px.
     * 110 − 63 = 47px of genuinely free space for a 65px thing. Something has to give, and
     * which thing depends on what the system inset actually *is*:
     *
     *  - **Gesture navigation** (a shallow inset, ~24dp) is empty apart from the centred
     *    home pill. The wordmark is at the far left and never comes near it, so it may sit
     *    inside that band — [WordmarkLift] drops it there, clear of the bar by ~9dp.
     *  - **Three-button navigation** (~48dp) *is* the buttons. Nothing may sit in it, so the
     *    lift is skipped and the wordmark rests on top of the inset, accepting that the
     *    bar's translucent lower edge grazes it. Readable-through-glass beats hidden-behind-
     *    hardware-buttons; neither is lovely.
     *
     * At exactly the system inset with no lift, which is what this did first, the wordmark's
     * top 15px sat under the bar on gesture navigation too — 3px of clearance, which is to
     * say none once a shadow or a different device is involved.
     */
    val systemNavInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val mapPadding = remember(systemNavInset) {
        val lifted = if (systemNavInset <= GestureInsetCeiling) systemNavInset - WordmarkLift else systemNavInset
        PaddingValues(bottom = lifted.coerceAtLeast(0.dp))
    }

    // The route is the screen's subject, so it is read before anything else — the camera
    // below is framed from it rather than from a guessed centre.
    val route = remember { TrailRoute.load(context) }
    val density = LocalDensity.current
    val routeWidthPx = remember(density) { with(density) { RouteWidth.toPx() } }
    val routeCasingPx = remember(density) { with(density) { (RouteWidth + RouteCasing * 2).toPx() } }
    val fitPaddingPx = remember(density) { with(density) { FitPadding.roundToPx() } }

    /**
     * The leash. The camera's *target* may not leave this box, so the map cannot be
     * dragged off to another province and left there.
     *
     * It is the route's own bounds grown by [RoamMargin] on each side rather than the
     * bounds themselves, for two reasons. A walker standing at the far end of the trail is
     * *on* the boundary, and a camera pinned exactly to it cannot centre on them — the map
     * would fight the recentre button. And a target locked to the route's edge still lets
     * half the screen show what is past it, so a hard edge buys nothing except the feeling
     * of a map that is stuck.
     *
     * This restricts the centre, not the view: at low zoom the surrounding province is
     * still visible around the trail, which is what makes the restriction feel like a map
     * of an area rather than a bug. [MinZoom] is what stops that going as far as the
     * whole country.
     */
    val roamBounds = remember(route) {
        LatLngBounds(
            LatLng(route.bounds.southwest.latitude - RoamMargin, route.bounds.southwest.longitude - RoamMargin),
            LatLng(route.bounds.northeast.latitude + RoamMargin, route.bounds.northeast.longitude + RoamMargin),
        )
    }

    // The wood is derived from the route and nothing else, so it is built once alongside it.
    // A few thousand cells marked by one walk along the trail — cheap enough to sit in a
    // `remember` next to the route's own decode, and never touched again after that.
    val treeTiles = remember(route) { TreeTileProvider(TrailTrees.around(route)) }

    val cameraPositionState = rememberCameraPositionState {
        // A holding frame only. The exact fit needs the map's pixel size, which does not
        // exist until it has laid out, so it happens in onMapLoaded below; this just means
        // the first frame is already over the trail instead of somewhere else entirely.
        position = CameraPosition.fromLatLngZoom(route.bounds.center, DefaultZoom)
    }
    val style = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_forest) }

    // The bases. Seeded synchronously from the cache — see [CheckpointRepository] — so the
    // map opens with its pins already on it, then corrected when the network answers.
    // Which name to label the pins with. Read from the configuration rather than from
    // AppSettings, so it follows a device-language change without the map having to
    // subscribe to anything.
    val thai = LocalConfiguration.current.locales[0].language == "th"
    val checkpointsViewModel: MapCheckpointsViewModel = viewModel(factory = MapCheckpointsViewModel.Factory)
    val checkpoints by checkpointsViewModel.checkpoints.collectAsStateWithLifecycle()

    /** The base whose card is open, or null. Tapping a pin sets it; the card clears it. */
    var selectedCheckpoint by remember { mutableStateOf<ParticipantCheckpoint?>(null) }

    // The card's height, measured rather than assumed.
    //
    // The card used to be placed by a fixed 92dp off the bottom edge, which was really a
    // guess at how tall the walk button is — a guess this file is not entitled to make,
    // since the button's height belongs to the type system and grows with the display font
    // size. Nothing is placed by arithmetic now: the card is the last thing in the column
    // and the column stacks. This is the one height still needed, and only because the SOS
    // lives in the other corner, where no amount of stacking can tell it what happened.
    var checkpointCardHeight by remember { mutableStateOf(0.dp) }
    // BitmapDescriptorFactory throws until the Maps SDK has been initialised, and building
    // a marker icon at composition runs before the GoogleMap below does that. Initialise
    // explicitly first, then the icons are safe to make.
    //
    // All of them are made *here*, inside the one block that has just initialised, rather
    // than in a `remember` of their own further up. That is the whole point of the grouping:
    // an icon built anywhere above this line throws "IBitmapDescriptorFactory is not
    // initialized" the moment the tab is opened, and the failure is positional — it depends
    // on where in the function the call sits, which is not something the next person should
    // have to know. Add new marker icons to [MapIcons], not beside it.
    val icons = remember {
        // LATEST, not whatever this device happens to default to.
        //
        // The one-argument overload asks for no renderer in particular, which means the map
        // is drawn by the legacy one on any device that still falls back to it — and the
        // legacy renderer is where extruded buildings come out as pale, washed-out slabs
        // you can read the ground through, rather than as solid shaded volumes. Pinning it
        // also takes "which renderer did this phone give us" out of the set of things that
        // can make the same build look different on two handsets.
        //
        // The preference only counts if it is expressed before the process creates its
        // first map, which is why it stays here, above the GoogleMap below, rather than
        // moving somewhere tidier.
        MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { }
        MapIcons(
            start = endpointDescriptor(WbwGreenDark.toArgb(), hollow = false),
            finish = endpointDescriptor(WbwGreenDark.toArgb(), hollow = true),
            // One to twenty covers any plausible event; a base numbered beyond that draws
            // without a pin rather than crashing, and nobody is walking twenty bases.
            bases = (1..20).associateWith { baseDescriptor(it, WbwGreenDark.toArgb()) },
        )
    }

    fun flyTo(latLng: LatLng, zoom: Float) {
        scope.launch {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, zoom), 1000)
        }
    }

    @SuppressLint("MissingPermission")
    fun flyToMe() {
        if (!granted()) return
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { loc -> if (loc != null) flyTo(LatLng(loc.latitude, loc.longitude), MeZoom) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocation = result.values.any { it }
        if (hasLocation) flyToMe()
    }

    fun requestLocation() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
        )
    }

    // ===== The walk =====

    val walk by WalkTracker.stats.collectAsStateWithLifecycle()

    // ===== The emergency =====
    //
    // Held here rather than inside the button, so an open case survives the button being
    // replaced by the panel that reports on it — and survives a tab switch, since the
    // view model is scoped to this navigation entry.
    // Both null on the staff shell — see the `emergency` parameter. The view model is not
    // merely unused there, it is not built at all: constructing it would start a profile
    // fetch against `/me`, which a staff account has no row behind.
    val sosViewModel: SosViewModel? =
        if (emergency) viewModel(factory = SosViewModel.Factory) else null
    val sos by (sosViewModel?.state ?: remember { MutableStateFlow(SosUiState()) })
        .collectAsStateWithLifecycle()

    // Driven from the screen, like the chat's long-poll and for the same reason: the held
    // connection should live exactly as long as somebody is looking at it. Unlike the chat,
    // what it is waiting for is a staff member pressing "on my way", which is why it keeps
    // running even when there is no case — one can be raised from another device.
    LaunchedEffect(sosViewModel) { sosViewModel?.watch() }

    fun holds(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * What the walk still needs, asked for in one prompt at the moment Start is pressed
     * rather than on opening the map — a permission dialog makes sense when it is obvious
     * what it is for, and none of these are needed to simply look at the route.
     *
     * Fine location because speed and distance come from the fix and coarse is too vague to
     * measure a walk with; activity recognition for the pedometer; notifications because a
     * foreground service without a visible notification is not a thing Android allows.
     */
    fun missingWalkPermissions(): Array<String> = buildList {
        if (!holds(Manifest.permission.ACCESS_FINE_LOCATION)) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !holds(PermissionActivityRecognition)) {
            add(PermissionActivityRecognition)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !holds(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val walkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasLocation = granted()
        // Location is the only refusal that stops the walk outright — without a position
        // stream there is no distance and no speed to record. A refused pedometer or a
        // refused notification degrades instead: the step count reads as unavailable, and
        // Android simply shows the service's notification silently.
        if (holds(Manifest.permission.ACCESS_FINE_LOCATION) ||
            holds(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            WalkTracker.start(context)
        }
    }

    fun toggleWalk() {
        if (walk.active) {
            WalkTracker.stop(context)
            return
        }
        val missing = missingWalkPermissions()
        if (missing.isEmpty()) WalkTracker.start(context) else walkPermissionLauncher.launch(missing)
    }

    // While a walk is running the camera belongs to it: locked to the walker, tilted, and
    // turned so the way ahead is up. Re-keyed on every fix, which cancels the previous
    // animation and retargets — at one fix per two seconds that reads as continuous motion
    // rather than as a series of jumps.
    LaunchedEffect(walk.active, walk.fix) {
        if (!walk.active) return@LaunchedEffect
        val fix = walk.fix ?: return@LaunchedEffect
        runCatching {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(fix.latitude, fix.longitude))
                        .zoom(FollowZoom)
                        .tilt(TiltDegrees)
                        // The way ahead, in order of what is actually known.
                        //
                        // A measured bearing when there is one. Before that — the first
                        // few fixes of a walk, while the tracker is still refusing to
                        // believe a heading from somebody who has barely moved — the
                        // trail's own direction under the walker's feet, which on a fixed
                        // loop is where they are about to go. That gap used to fall
                        // through to the camera's existing bearing, so starting a walk
                        // pointed the camera north and left it there until the walker had
                        // gone far enough to prove otherwise.
                        //
                        // The existing bearing is still the last resort, for a walker who
                        // is neither moving nor near the route. Once the tracker has a
                        // heading it keeps it, so standing still mid-walk still holds the
                        // last direction rather than snapping anywhere.
                        .bearing(
                            fix.bearingDegrees
                                ?: route.headingAt(fix.latitude, fix.longitude)
                                ?: cameraPositionState.position.bearing,
                        )
                        .build(),
                ),
                FollowAnimationMillis,
            )
        }
    }

    // Ask for permission on open, but do not fly to the walker. The opening camera belongs
    // to the route: this screen exists to show where the walk goes, and someone standing on
    // the trail already knows where they are standing. Recentring on yourself is one tap
    // away on the button below, which is the right way round.
    //
    // Permission is still worth asking for here rather than at the first tap of that
    // button, because the walk tracker below needs it too and one dialog on open beats two
    // interruptions later.
    LaunchedEffect(Unit) {
        if (!hasLocation) requestLocation()
    }

    // 3D is the native camera tilting, so it belongs to the camera, not to a separate view.
    //
    // The first pass is skipped. This effect runs once at composition with `is3d` already
    // false, which animated the camera to a tilt it was, and that animation raced the route
    // fit for the same camera — the map opened somewhere between the two.
    // It also zooms in far enough for buildings to exist, and gives the previous zoom back
    // on the way out.
    //
    // Tilt alone does not produce a 3D scene. The SDK only extrudes buildings from about
    // [BuildingZoom] upward — below that the footprints are drawn as flat coloured polygons
    // no matter how far the camera is leaned over, so tapping 3D at the trail-overview zoom
    // gave a flat map seen at an angle, which is exactly what it looked like.
    //
    // This used to force zoom 18 unconditionally and that was removed for a good reason:
    // once the map opened fitted to the whole 5km route, tapping 3D threw the route off
    // screen and left an empty field. The fix is not to drop the zoom but to make it
    // reversible — remember where the user was, go in far enough to see something, and
    // restore it when they leave 3D. Losing the overview *while in 3D* is not a bug; you
    // cannot see extruded buildings from 5km up, so the button either goes in or does
    // nothing.
    val tiltSettled = remember { mutableStateOf(false) }
    var zoomBefore3d by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(is3d) {
        if (!tiltSettled.value) {
            tiltSettled.value = true
            return@LaunchedEffect
        }
        // A running walk owns the camera. Two effects animating it at once is a fight the
        // user sees as stutter.
        if (WalkTracker.stats.value.active) return@LaunchedEffect
        val cur = cameraPositionState.position
        val zoom = if (is3d) {
            zoomBefore3d = cur.zoom
            // Only ever closer, never further out — somebody already zoomed past this and
            // pressing 3D should not pull them back.
            maxOf(cur.zoom, BuildingZoom)
        } else {
            (zoomBefore3d ?: cur.zoom).also { zoomBefore3d = null }
        }
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(cur.target)
                    .zoom(zoom)
                    .tilt(if (is3d) TiltDegrees else 0f)
                    .bearing(cur.bearing)
                    .build(),
            ),
            700,
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = mapPadding,
            // The white flash, killed at its source. Before the first tiles arrive the
            // MapView paints its default background, which is near-white — a full-screen
            // flash of it every time the tab is opened, on an app that is otherwise a dark
            // forest. Setting it to the same near-black the route's casing uses means the
            // worst case is now the screen staying dark a moment longer.
            googleMapOptionsFactory = {
                GoogleMapOptions().backgroundColor(WbwForestVoid.toArgb())
            },
            // Fired once the map has a size, which is the first moment newLatLngBounds is
            // legal — it throws outright if asked to fit a bounds into a zero-sized map.
            // Wrapped anyway: a camera that failed to frame the route is a worse map, not a
            // broken app, and the holding position above is already a reasonable view.
            onMapLoaded = {
                mapReady = true
                // Not while walking. Coming back to the map tab mid-walk would otherwise
                // yank the camera off the walker to re-frame a route they are standing on.
                if (!WalkTracker.stats.value.active) {
                    runCatching {
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngBounds(route.bounds, fitPaddingPx),
                        )
                    }
                }
            },
            properties = MapProperties(
                mapType = MapType.NORMAL,
                mapStyleOptions = style,
                isMyLocationEnabled = hasLocation,
                // Extruded buildings — what makes the tilted camera read as 3D rather than
                // as a flat map seen at an angle.
                isBuildingEnabled = true,
                // Keep the map on the event. This is a map *of the trail*, not a world
                // map that happens to open there, and every pixel outside the event is a
                // way to get lost in an app whose whole job is the opposite.
                latLngBoundsForCameraTarget = roamBounds,
                // The bounds alone would not hold: pinching out far enough puts the whole
                // country on screen with the target still dutifully inside the box. This
                // is the other half of the same fence.
                minZoomPreference = MinZoom,
            ),
            uiSettings = MapUiSettings(
                compassEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true,
            ),
        ) {
            // The wood, under everything else. Drawn by the app because the SDK has neither
            // trees nor a way to say where a forest is — see [TrailTrees].
            TileOverlay(
                tileProvider = treeTiles,
                zIndex = TreeZ,
                // The tiles arrive a beat after the basemap under them; without this they
                // pop in as a hard grid of squares.
                fadeIn = true,
            )

            // The route, drawn as two stacked lines.
            //
            // A single green stroke gets lost the moment it runs along a road, because the
            // styled roads are greens too (#243425–#354a39) and at trail scale the line
            // and the road it follows are the same few pixels. The casing underneath is
            // the cartographer's fix: a near-black outline that separates the route from
            // whatever it crosses, so the eye follows one continuous thing rather than
            // losing it at every junction.
            //
            // Both are fixed colours rather than themed. The map style is dark in both
            // themes — it is a scene, like the backdrop — so a route that followed the
            // theme would go near-black on a near-black map for half the users.
            Polyline(
                points = route.points,
                color = WbwForestVoid,
                width = routeCasingPx,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND,
                zIndex = RouteCasingZ,
            )
            // Walked behind, still to walk ahead — the navigation idiom, and the one thing
            // that makes a 5km walk legible at a glance: the answer to "how much is left"
            // is the length of the bright half, read without any number at all.
            //
            // The split only exists once a walk has put the participant somewhere on the
            // line. Before that the whole route draws in the one colour, because dimming
            // a section nobody has walked would claim progress that has not happened.
            val walkedMetres = walk.routeMetres
            if (walkedMetres == null) {
                Polyline(
                    points = route.points,
                    color = WbwGreenDark,
                    width = routeWidthPx,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    jointType = JointType.ROUND,
                    zIndex = RouteZ,
                )
            } else {
                val (walked, remaining) = remember(route, walkedMetres) { route.splitAt(walkedMetres) }
                if (walked.size >= 2) {
                    Polyline(
                        points = walked,
                        // Dimmed rather than hidden. The part already walked is still the
                        // route — it is how you get back — and erasing it would leave a
                        // line that appears to start in the middle of a hillside.
                        color = WbwGreenDark.copy(alpha = 0.38f),
                        width = routeWidthPx,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        jointType = JointType.ROUND,
                        zIndex = RouteZ,
                    )
                }
                if (remaining.size >= 2) {
                    Polyline(
                        points = remaining,
                        color = WbwGreenDark,
                        width = routeWidthPx,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        jointType = JointType.ROUND,
                        zIndex = RouteZ + 1f,
                    )
                }
            }

            // The bases, numbered, in trail order.
            //
            // Only rows that carry a position and require a check-in get a pin. The finish
            // is in this list too — it is a checkpoint row — but it is drawn by the
            // endpoint marker below instead, and two markers on one spot would just fight
            // each other for the tap.
            checkpoints.forEach { cp ->
                val lat = cp.lat
                val lng = cp.lng
                if (lat == null || lng == null || !cp.requiresCheckin) return@forEach
                val seq = cp.sequence
                Marker(
                    state = rememberMarkerState(key = "cp-${cp.id}", position = LatLng(lat, lng)),
                    icon = seq?.let { icons.bases[it] } ?: icons.start,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    zIndex = CheckpointZ,
                    // Returning true consumes the tap, which is what suppresses the Maps
                    // SDK's own info window.
                    //
                    // That window is a white callout tethered above the pin: it follows the
                    // map, so reading it means the thing you tapped has moved under your
                    // thumb, it is drawn by Google rather than in this app's glass, and on
                    // a pin near the top of the screen it opens off-screen. A card fixed to
                    // the screen stays where the eye already is and can hold more than two
                    // lines of text.
                    onClick = { selectedCheckpoint = cp; true },
                )
            }

            // Filled for the start, a ring for the finish — the usual reading, and one that
            // survives being 20dp across with no label attached to it.
            Marker(
                state = rememberMarkerState(key = "route-start", position = route.start),
                title = stringResource(R.string.map_route_start),
                icon = icons.start,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                zIndex = EndpointZ,
            )
            Marker(
                state = rememberMarkerState(key = "route-finish", position = route.end),
                title = stringResource(R.string.map_route_finish),
                icon = icons.finish,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                zIndex = EndpointZ,
            )
        }

        val layoutDir = LocalLayoutDirection.current

        // Where each base sits along the route, in metres from the start.
        //
        // Computed once per route/checkpoint set rather than per frame: it is a projection
        // of every pin onto a 504-point line, and the map recomposes on every camera move.
        // A base further from the trail than the route's own tolerance has no place on it
        // and is left out rather than given a wrong one.
        val alongRoute = remember(route, checkpoints) {
            checkpoints.mapNotNull { cp ->
                val la = cp.lat
                val ln = cp.lng
                if (la == null || ln == null || !cp.requiresCheckin) null
                else route.progressFrom(-1.0, la, ln)?.let { cp to it }
            }.sortedBy { it.second }
        }

        // The next base ahead, while walking.
        //
        // "Ahead" is decided by distance along the route rather than by straight-line
        // proximity, which is the difference between the base you are walking towards and
        // the one you passed ten minutes ago that happens to be closer as the crow flies.
        val nextBase = remember(alongRoute, walk.routeMetres) {
            val at = walk.routeMetres
            if (at == null) null else alongRoute.firstOrNull { it.second > at + NextBaseReachedMetres }
        }

        // Top: title + search, with the walk's readout hanging under them. A column rather
        // than a second free-floating overlay, so the HUD's position is derived from the row
        // above it instead of from a hand-tuned offset that breaks on a taller status bar.
        Column(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDir) + ControlsInset,
                    end = contentPadding.calculateEndPadding(layoutDir) + ControlsInset,
                    top = 14.dp,
                ),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // The same treatment as Home's greeting: the screen's name, set large, on
                // the ground rather than in a chip. It was an 11sp tracked label inside a
                // glass pill, which is the app's vocabulary for a *tag* — a small fact
                // about something else — and a screen title is not that. It also meant the
                // map opened with a tiny word in a box while Home opened with a sentence.
                //
                // Nothing under it now. The pill was carrying legibility as well as style,
                // but the map style is dark on both themes (it is a scene, like the
                // backdrop) so onBackdrop reads on it unaided.
                Text(
                    stringResource(R.string.map_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onBackdrop,
                    modifier = Modifier.weight(1f),
                )
            }

            // The emergency, above the walk readout.
            //
            // Ordered deliberately: while a case is open it is the most important thing on
            // the screen, and distance-and-pace is not. It sits at the top rather than over
            // the button it replaced because this is something to *read*, and the top of
            // the screen is where this app puts things to read.
            // Stays up after Stop. Somebody who has just walked the route should not lose the
            // total to the same tap that ended the walk — the next Start clears it.
            if (walk.hasData) {
                Spacer(Modifier.height(12.dp))
                WalkHud(walk, nextBase = nextBase?.let { (cp, at) ->
                    cp.displayName(thai) to (at - (walk.routeMetres ?: 0.0)).coerceAtLeast(0.0)
                })
            }
        }

        // Bottom-left: the map controls, stacked above the walk control.
        //
        // Everything you operate the *map* with is on one side and the emergency is on the
        // other, which is the point of the arrangement. Sharing a corner with 3D and
        // recentre meant the SOS button lived a thumb's width from the two controls pressed
        // most absent-mindedly on this screen; no amount of spacing inside a shared column
        // fixes that as well as putting them on opposite edges does.
        //
        // Recentre sits directly above the walk button because it is the one used mid-walk,
        // so it stays closest to the thumb while 3D moves further up.
        //
        // The checkpoint card is *in* this column rather than floating above it. Placed
        // over the column it covered the recentre and 3D buttons, which is the same bug as
        // covering the walk button and was the more visible half of it — those two are
        // round, so the card cut them in half rather than hiding them outright. A card that
        // is a member of the stack cannot land on the stack: the buttons above it are moved
        // up by its arrival, which is what "above the walk button" has to mean when there
        // are three things in the corner and not one.
        //
        // The gap is per-child padding rather than the column's own `spacedBy`, because
        // `spacedBy` pays out a gap for every child it lays out and [AnimatedVisibility]
        // is a child even with nothing in it — the corner would carry a 12dp hole at all
        // times, held open for a card that is usually not there.
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDir) + ControlsInset,
                    end = contentPadding.calculateEndPadding(layoutDir) + ControlsInset,
                    bottom = contentPadding.calculateBottomPadding() + ControlsBottom,
                ),
            horizontalAlignment = Alignment.Start,
        ) {
            // Hidden mid-walk. A walk is 3D by definition and drives the camera itself, so
            // the button would either do nothing or fight it — and a control that visibly
            // does nothing is worse than one that stepped aside.
            if (!walk.active) {
                GlassIcon(
                    icon = if (is3d) Icons.Outlined.Map else Icons.Outlined.Terrain,
                    description = stringResource(if (is3d) R.string.map_mode_2d else R.string.map_mode_3d),
                    tint = if (is3d) WbwGreenDark else colors.onBackdrop,
                    onClick = { is3d = !is3d },
                    modifier = Modifier.padding(bottom = ControlsGap),
                )
            }
            GlassIcon(
                icon = Icons.Outlined.MyLocation,
                description = stringResource(R.string.map_recenter),
                tint = colors.onBackdrop,
                onClick = { if (hasLocation) flyToMe() else requestLocation() },
                modifier = Modifier.padding(bottom = ControlsGap),
            )

            // The one action on this screen, so it carries a label instead of a glyph.
            WalkButton(active = walk.active, onClick = { toggleWalk() })

            // The tapped base's card, underneath everything else in the corner.
            //
            // Last in the column rather than tucked in above the walk button, so the corner
            // rises off it as one piece — 3D, recentre and the walk button keep the spacing
            // and the order they have when no card is open, and the SOS stays level with
            // the walk button the way it is meant to. Inserted higher up, the card split
            // the stack in two and left the walk button behind at the bottom while the two
            // round controls went up, which turned one group of controls into two.
            //
            // It expands rather than sliding in: everything above it travels the card's
            // height to make room, and that has to be seen to be a push. Sliding the card
            // in over its final height would claim the space in a single frame and the
            // stack would jump while the card was still on its way.
            AnimatedVisibility(
                visible = selectedCheckpoint != null,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180)),
            ) {
                // Held through the exit animation — reading the state directly would empty
                // the card the instant it started leaving.
                val shown = remember(selectedCheckpoint) { selectedCheckpoint }
                shown?.let { cp ->
                    CheckpointCard(
                        checkpoint = cp,
                        thai = thai,
                        // How far along the route it sits, and how far that is from here.
                        // Absent when not walking: "600 m away" from a position the app
                        // does not have would be a number made up to fill a line.
                        metresAway = alongRoute.firstOrNull { it.first.id == cp.id }?.second
                            ?.let { at -> walk.routeMetres?.let { now -> at - now } },
                        onDismiss = { selectedCheckpoint = null },
                        // The gap belongs to the card, not to the button above it: a gap
                        // hung off the button would be paid whether or not a card is there.
                        //
                        // Measured inside that padding, and on the content — which holds
                        // its full height while the container expands around it — so the
                        // SOS is told where the card is going, not where it has got to.
                        modifier = Modifier
                            .padding(top = ControlsGap)
                            .onSizeChanged {
                                checkpointCardHeight = with(density) { it.height.toDp() }
                            },
                    )
                }
            }
        }

        // How far the SOS rises: exactly what the card takes at the bottom of the other
        // corner — its height and the gap above it — so it climbs the same distance the
        // walk button does and the two stay on one line, which is the whole reason they sit
        // at the same height. Zero with no card up, so its resting place is unchanged.
        val sosLift by animateDpAsState(
            targetValue =
                if (selectedCheckpoint != null) checkpointCardHeight + ControlsGap
                else 0.dp,
            // Longer than the card's own 220ms entrance and on the same easing, so the
            // button is still travelling as the card finishes arriving. Matching the
            // durations exactly reads as two things cutting to new places at once; this
            // reads as the card pushing the button out of the way, which is what happens.
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "sosLift",
        )

        // Bottom-right: the emergency, alone on its own edge.
        //
        // The far corner from the map controls, and at the same height as the walk button
        // so the bottom row of the screen reads as the two things you might actually need
        // to do — walk, or call for help — rather than as a stack of glyphs.
        //
        // Gone while a case is open: [SosActivePanel] at the top of the screen takes over,
        // because there is exactly one open case per participant and a second button could
        // not do anything.
        //
        // **It rides up when a checkpoint card opens.** The card is a full-width band across
        // the bottom-left, so it and the SOS want the same strip of screen; the card was
        // winning, and the button it covered is the one control on this map that must never
        // be covered by anything. Moving is better than either shrinking the card to dodge
        // it or letting the card stop short of the right edge, which would leave a notch in
        // the band whose only explanation is a button that is not there most of the time.
        // Scales away as the card arrives and comes back the same way, so the two read as
        // one movement — the button becoming the card — rather than as one thing vanishing
        // and an unrelated thing appearing at the other end of the screen.
        AnimatedVisibility(
            visible = emergency && !sos.active,
            enter = fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.8f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = contentPadding.calculateEndPadding(layoutDir) + ControlsInset,
                    bottom = contentPadding.calculateBottomPadding() + ControlsBottom + sosLift,
                ),
        ) {
            SosButton(onFire = { sosViewModel?.raise(context) })
        }

        // The emergency, over everything.
        //
        // Placed here rather than in the column under the title because it is no longer a
        // card on this screen — it *is* the screen while a case is open. It sits above the
        // map and all of its controls, and below only the loading cover, which still has to
        // win: a half-drawn map behind an emergency screen would be the one thing worse
        // than either on its own.
        //
        // It arrives rather than appearing. It used to cut in between two frames, and a
        // thing that is simply *there* on the next frame reads as a redraw rather than as
        // an answer to what you just did. The one moment this screen has to feel like it
        // did something is the moment after a three-second hold, and that moment was the
        // one with nothing in it.
        //
        // Fade and a small scale-up, on a low-bounce spring: it grows into place from just
        // under full size, the way something arriving in front of you does, rather than
        // sliding in from an edge it has no relationship to. Leaving is faster and plain —
        // a stood-down emergency should get out of the way, not take a bow.
        AnimatedVisibility(
            visible = sos.active,
            enter = fadeIn(tween(220)) +
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialScale = 0.92f,
                ),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
        ) {
            // Held so the screen keeps its contents through the exit animation — reading
            // `sos.case!!` here would throw the moment it became null.
            sos.case?.let { open ->
                SosFullScreen(
                    case = open,
                    me = sos.me,
                    cancelRefused = sos.cancelRefused,
                    onCancel = { sosViewModel?.cancel() },
                    contentPadding = contentPadding,
                )
            }
        }

        // The cover. Last in the Box, so it hides the controls as well as the map — a
        // search field and a walk button floating over an empty green rectangle look
        // broken, where a plain loading screen looks like loading.
        //
        // The map's own background colour already handles the white flash; this handles the
        // second or two after it, where a correctly-coloured but empty map is
        // indistinguishable from a map that has failed. It only ever fades *out*: appearing
        // is the initial state, so an enter animation would be a fade-in from nothing on
        // the very first frame.
        AnimatedVisibility(
            visible = !mapReady,
            enter = EnterTransition.None,
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier.fillMaxSize().background(WbwForestVoid),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(
                    color = colors.onBackdropMuted,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.map_loading).uppercase(),
                    color = colors.onBackdropMuted,
                    fontSize = 11.sp,
                    letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * The live readout: distance, steps, pace.
 *
 * Three columns of equal width rather than content-sized ones, so a number growing a digit
 * does not shove its neighbours sideways mid-walk. Emphasis is carried by size and weight
 * against a single ink, the way the rest of the app does it — no highlight colour.
 */
@Composable
private fun WalkHud(
    stats: WalkStats,
    /** The base being walked towards and how far off it is, or null when not walking. */
    nextBase: Pair<String, Double>? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(HudShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        // How much of the route is left, which is the one thing the three numbers below
        // cannot say. Distance walked counts every detour; this counts the route.
        //
        // Absent until a fix has landed near the trail. Somebody who opened the map at home
        // has no position on this route, and a bar reading 0% would be a claim about their
        // progress rather than an admission that there is nothing to report yet.
        stats.routeFraction?.let { fraction ->
            RouteProgressBar(fraction = fraction, remaining = stats.routeRemainingMetres, complete = stats.routeComplete)
            Spacer(Modifier.height(14.dp))
        }

        // What you are walking towards.
        //
        // The bar above says how much of the whole route is left, which is the wrong scale
        // for the next twenty minutes — five kilometres remaining is not an answer to "how
        // far to the next base". Absent once the last base is behind: there is nothing
        // ahead but the finish, and the bar is already reporting that.
        nextBase?.let { (name, metres) ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.walk_next_base).uppercase(),
                    color = wbwColors.onBackdropMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    name,
                    color = wbwColors.onBackdrop,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDistance(metres),
                    color = wbwColors.onBackdrop,
                    fontSize = 13.sp,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
        WalkStat(
            label = stringResource(R.string.walk_stat_distance),
            value = formatDistance(stats.distanceMetres),
            modifier = Modifier.weight(1f),
        )
        WalkStat(
            label = stringResource(R.string.walk_stat_steps),
            // Null is "this phone cannot count steps", which is not the same claim as zero.
            value = stats.steps?.toString() ?: stringResource(R.string.walk_unavailable),
            modifier = Modifier.weight(1f),
        )
        WalkStat(
            label = stringResource(R.string.walk_stat_pace),
            value = formatPace(stats.speedMps),
            modifier = Modifier.weight(1f),
        )
        }
    }
}

/**
 * One base, as a card on the screen rather than a callout on the map.
 *
 * Carries what somebody tapping a pin is asking: which base this is, what happens there,
 * and — while walking — how far off it is. Not a check-in: a participant is checked in by
 * a staff member scanning their pass, and a button here would promise something this
 * screen cannot do.
 */
@Composable
private fun CheckpointCard(
    checkpoint: ParticipantCheckpoint,
    thai: Boolean,
    metresAway: Double?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = wbwColors
    Row(
        modifier
            .fillMaxWidth()
            .glass(HudShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The number, in the same disc the pin uses, so the card and the marker that
        // opened it are recognisably the same thing.
        checkpoint.sequence?.let { seq ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(WbwGreenDark),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$seq",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(
                checkpoint.displayName(thai),
                color = colors.onBackdrop,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            checkpoint.displayActivity(thai)?.let {
                Text(
                    it,
                    color = colors.onBackdropMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Behind you rather than ahead reads as a negative distance otherwise.
            metresAway?.takeIf { it > 0 }?.let {
                Text(
                    stringResource(R.string.map_checkpoint_away, formatDistance(it)),
                    color = colors.onBackdrop,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .tapNoRipple(onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_close),
                tint = colors.onBackdropMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * The route bar: how much of the route is behind, and how far is left.
 *
 * A bar rather than a percentage on its own, because "68%" of a walk somebody is in the
 * middle of is a number they have to convert into "about two and a half kilometres" before
 * it means anything. The bar is read at a glance and the metres are underneath it for when
 * the glance is not enough.
 */
@Composable
private fun RouteProgressBar(fraction: Float, remaining: Double?, complete: Boolean) {
    val colors = wbwColors
    // Eased so the fill slides rather than stepping on each fix. GPS arrives about once a
    // second and in jumps of a few metres; without this the bar twitches for eight
    // kilometres.
    val shown by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "routeProgress",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.walk_route_label).uppercase(),
                color = colors.onBackdropMuted,
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (complete || remaining == null) {
                    stringResource(R.string.walk_route_complete)
                } else {
                    stringResource(R.string.walk_route_left, formatDistance(remaining))
                },
                color = if (complete) WbwGreenDark else colors.onBackdrop,
                fontSize = 12.sp,
                fontWeight = if (complete) FontWeight.Medium else FontWeight.Normal,
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.onBackdrop.copy(alpha = 0.16f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(shown.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(WbwGreenDark),
            )
        }
    }
}

@Composable
private fun WalkStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = wbwColors
    Column(modifier) {
        Text(
            label.uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 9.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = colors.onBackdrop,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
private fun WalkButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = wbwColors
    Row(
        modifier
            .glass(RoundedCornerShape(50), fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .tapNoRipple(onClick)
            .padding(start = 18.dp, end = 22.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (active) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
            null,
            // The one place a hue is spent on this screen: recording is a state worth
            // being able to spot without reading, and it is the same green the route uses.
            tint = if (active) WbwGreenDark else colors.onBackdrop,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            stringResource(if (active) R.string.walk_stop else R.string.walk_start).uppercase(),
            color = colors.onBackdrop,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Metres until a kilometre reads better than four digits of them. */
@Composable
private fun formatDistance(metres: Double): String =
    if (metres < 1000.0) stringResource(R.string.walk_distance_m, metres.roundToInt())
    else stringResource(R.string.walk_distance_km, metres / 1000.0)

/**
 * Pace, not speed — minutes per kilometre is what a walker plans in.
 *
 * Below a crawl it is not reported at all: as speed approaches zero the figure runs off to
 * infinity, and "412'07\" /km" for somebody standing at a checkpoint is noise dressed as a
 * measurement.
 */
@Composable
private fun formatPace(speedMps: Float): String {
    if (speedMps < MinPaceSpeedMps) return stringResource(R.string.walk_unavailable)
    val secondsPerKm = (1000f / speedMps).roundToInt()
    return stringResource(R.string.walk_pace_min_km, secondsPerKm / 60, secondsPerKm % 60)
}

@Composable
private fun GlassIcon(
    icon: ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(52.dp)
            .glass(CircleShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .tapNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * Every marker bitmap the screen draws, built together.
 *
 * Grouped rather than held one-per-`remember` so they share the single point where the
 * Maps SDK is initialised — see the call site. Bitmaps, so they are made once and reused
 * across recompositions rather than redrawn per frame.
 */
private class MapIcons(
    val start: BitmapDescriptor,
    val finish: BitmapDescriptor,
    /**
     * One pin per base, keyed by the number printed on it.
     *
     * Built up front for the whole set rather than per marker, because a `BitmapDescriptor`
     * is a texture upload and rebuilding nine of them on every recomposition of a map that
     * recomposes on every camera movement is the kind of thing that shows up as a stutter
     * while panning rather than as an error anywhere.
     */
    val bases: Map<Int, BitmapDescriptor>,
)

/**
 * A base: a filled disc with its sequence number on it.
 *
 * Numbered rather than iconographic because the number is the useful fact — the walk is
 * done in order, the bases are announced in order, and "4" locates somebody on the trail in
 * a way a generic pin never does. Drawn at 3x the endpoint size so two digits stay legible
 * at the zoom the whole route fits in.
 */
private fun baseDescriptor(number: Int, fillArgb: Int): BitmapDescriptor {
    val px = 82
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = px / 2f
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillArgb or 0xFF000000.toInt() }
    canvas.drawCircle(c, c, c, ring)
    canvas.drawCircle(c, c, c - 7f, fill)

    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = px * 0.46f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    // Centred on the disc rather than on the baseline: `descent + ascent` is the glyph
    // box's own offset, and halving it puts the digits' optical middle on the centre.
    val baseline = c - (label.descent() + label.ascent()) / 2f
    canvas.drawText(number.toString(), c, baseline, label)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

/**
 * The route's two endpoints: a filled disc for the start, the same disc with its middle
 * punched out for the finish.
 *
 * The hole is a real transparency (PorterDuff CLEAR) rather than a circle painted in the
 * map's background colour — the ground under a marker is whatever the route, a road or a
 * field happens to be there, and a fake hole in one fixed colour only lines up over empty
 * ground. The white ring is there for the same kind of reason: two greens meeting need
 * something achromatic between them.
 */
private fun endpointDescriptor(fillArgb: Int, hollow: Boolean): BitmapDescriptor {
    val px = 62
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = px / 2f
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillArgb or 0xFF000000.toInt() }
    canvas.drawCircle(c, c, c, ring)
    canvas.drawCircle(c, c, c - 6f, fill)
    if (hollow) {
        val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawCircle(c, c, c - 17f, clear)
    }
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

/**
 * The trail's opening frame is derived from the route itself, so there is no centre
 * constant here any more — [TrailRoute.bounds] is the source of truth for where the map
 * looks when it opens.
 */
private const val DefaultZoom = 14.5f

/**
 * How far past the route the camera's centre may roam, in degrees on each side.
 *
 * About 650m. Enough that the recentre button can still frame a walker who has wandered
 * off the line, and enough that the trail does not sit against a wall you can feel.
 *
 * It was double this, and double was too much: what is clamped is the camera's *target*,
 * so the furthest the map can get from the route is this margin plus half a screen. At
 * 1.3km that sum was more than a screen wide and the trail could be panned entirely out of
 * view — bounded, but not visibly so, which is the same experience as unbounded for anyone
 * who did not push until it stopped. At 650m some part of the route stays on screen
 * wherever you drag to, so the map reads as being about the trail at all times.
 */
private const val RoamMargin = 0.006

/**
 * The furthest out the map may be zoomed.
 *
 * The camera-target bounds do not survive zooming out on their own: pinch far enough and
 * the whole country is on screen with the target still dutifully inside its box. At 12.5
 * the view spans roughly 11km, so the trail keeps its surroundings and loses the rest of
 * Thailand. Comfortably below the ~14.3 the route fits at, so the opening frame is never
 * clamped by it.
 */
private const val MinZoom = 12.5f
private const val MeZoom = 16f

/** The route line, and the casing drawn on each side of it. */
private val RouteWidth = 6.dp
private val RouteCasing = 1.5.dp
/** How much breathing room the fitted camera leaves around the route. */
private val FitPadding = 56.dp

// Stacking order on the map. Explicit because the default is 0 for everything, which
// leaves the casing painting over the line it exists to sit under.
/** Under the route and its casing: the wood is ground, not information. */
private const val TreeZ = 0.5f
private const val RouteCasingZ = 1f
private const val RouteZ = 2f
private const val EndpointZ = 3f
/**
 * Bases sit above the route and below the two endpoints.
 *
 * Below the endpoints on purpose: base 1 and the start are the same place, so one of the
 * two has to win the tap, and the endpoint is the one that says "this is where the walk
 * begins" — which is the more useful answer while standing there.
 */
private const val CheckpointZ = 2.5f
// Near the SDK's tilt ceiling (~67.5°) and zoomed in, so buildings stand tall and the
// scene reads as a low aerial rather than a flat map at an angle.
private const val TiltDegrees = 67.5f

/**
 * How far in the 3D button goes, if the user is not already closer.
 *
 * The Maps SDK starts extruding buildings somewhere around zoom 17 and draws them as flat
 * footprints below it — this sits just above that line so the first frame after the tilt
 * already has geometry in it, rather than arriving a moment later when the tiles refine.
 *
 * If buildings still look flat here, the cause is upstream and not fixable in this file:
 * extruded geometry only exists where Google has modelled it, and coverage outside major
 * cities is patchy. Check the same spot in the Google Maps app — if it is flat there too,
 * there is no 3D data for MFU and no camera setting will invent it.
 */
private const val BuildingZoom = 17.5f

// ===== Walking =====

/**
 * How close the camera sits while following a walker. Close enough that the next junction
 * is legible, which is the only question being asked at walking pace.
 */
private const val FollowZoom = 18f

/**
 * Slightly longer than the two-second fix interval, so each animation is still easing when
 * the next fix retargets it. A shorter duration lands early and leaves the camera parked
 * between updates, which reads as a series of hops rather than as travel.
 */
private const val FollowAnimationMillis = 2_200

/** Below this the pace figure is meaningless, so it is not shown. */
private const val MinPaceSpeedMps = 0.35f

/** The HUD's corner — the app's card radius, not the field one. */
private val HudShape = RoundedCornerShape(22.dp)

/**
 * How far past a base counts as having reached it, for "next base" purposes.
 *
 * Without it the base you are standing at stays the next one until you have walked clear
 * of it, which reads as the trail refusing to advance.
 */
private const val NextBaseReachedMetres = 25.0

/**
 * How far the bottom controls sit above the screen's padded edge.
 *
 * It was 38dp, and that was not a spacing choice: the Google wordmark used to be drawn
 * inside the screen's padded region, and the walk button had to clear its height and margin
 * or sit on top of it — which the Maps terms of service do not allow. The wordmark now sits
 * at the bottom of the screen instead (see `mapPadding`), so the reason is gone and the
 * 38dp of dead air it bought went with it.
 *
 * What is left is an ordinary gap. The screen's own bottom padding already clears the
 * floating nav bar by 16dp, so this only has to keep the button from crowding it.
 * Both bottom rows use the same value so they stay on one line.
 */
private val ControlsBottom = 8.dp

/**
 * The gap between two things stacked in the bottom corner — the walk button and the
 * checkpoint card, and the card and the SOS lifted over it.
 *
 * The same 12dp the map controls are spaced by, so the whole corner is on one rhythm
 * whether or not a card is open.
 */
private val ControlsGap = 12.dp

/**
 * How far the bottom controls are inset from the screen edge.
 *
 * **20dp because that is what the floating nav bar uses** (`HomeScaffold` gives it
 * `padding(horizontal = 20.dp)`). It was 18dp, which put the walk button's left edge 5px
 * outside the bar's and the recentre button's right edge 5px outside the other end — not
 * enough to look deliberate, exactly enough to look wrong, since the two sit directly above
 * one another with nothing between them.
 *
 * If the bar's inset ever changes, this has to follow it.
 */
private val ControlsInset = 20.dp

/**
 * How far the Google wordmark is dropped into the gesture inset. See `mapPadding`.
 *
 * Sized from the measurement, not chosen: at zero lift the wordmark's top cleared the
 * floating bar by 3px. 16dp turns that into ~24px (9dp), and still leaves the wordmark's
 * bottom above the home pill's row — which it would not have to, since the pill is centred
 * and the wordmark is hard left, but a gap that survives a differently-sized pill is worth
 * having for free.
 */
private val WordmarkLift = 16.dp

/**
 * The largest bottom inset still treated as gesture navigation.
 *
 * Gesture insets land around 24dp and three-button bars around 48dp, so anything in between
 * is a safe place to split. Erring high would be the dangerous direction — it would let the
 * wordmark drop behind hardware buttons — so the threshold sits nearer the gesture end.
 */
private val GestureInsetCeiling = 32.dp

/** Tap with no ripple — a ripple on a glass pane paints a grey disc over the refraction. */
private fun Modifier.tapNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
