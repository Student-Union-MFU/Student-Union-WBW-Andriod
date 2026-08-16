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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
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
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.R
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
 * search answered a question nobody on a fixed 8.4km loop was asking, and the nearby
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
fun MapScreen(contentPadding: PaddingValues) {
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

    val cameraPositionState = rememberCameraPositionState {
        // A holding frame only. The exact fit needs the map's pixel size, which does not
        // exist until it has laid out, so it happens in onMapLoaded below; this just means
        // the first frame is already over the trail instead of somewhere else entirely.
        position = CameraPosition.fromLatLngZoom(route.bounds.center, DefaultZoom)
    }
    val style = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_forest) }
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
        MapsInitializer.initialize(context)
        MapIcons(
            start = endpointDescriptor(WbwGreenDark.toArgb(), hollow = false),
            finish = endpointDescriptor(WbwGreenDark.toArgb(), hollow = true),
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
                        // Keep the last heading when standing still, rather than snapping
                        // north — the walker has not turned round, they have stopped.
                        .bearing(fix.bearingDegrees ?: cameraPositionState.position.bearing)
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
    // once the map opened fitted to the whole 8.4km loop, tapping 3D threw the route off
    // screen and left an empty field. The fix is not to drop the zoom but to make it
    // reversible — remember where the user was, go in far enough to see something, and
    // restore it when they leave 3D. Losing the overview *while in 3D* is not a bug; you
    // cannot see extruded buildings from 8km up, so the button either goes in or does
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
            Polyline(
                points = route.points,
                color = WbwGreenDark,
                width = routeWidthPx,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND,
                zIndex = RouteZ,
            )

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

            // Stays up after Stop. Somebody who has just walked the loop should not lose the
            // total to the same tap that ended the walk — the next Start clears it.
            if (walk.hasData) {
                Spacer(Modifier.height(12.dp))
                WalkHud(walk)
            }
        }

        // Bottom-left: the walk control, opposite the map controls rather than beside them.
        // It is the one action on this screen, so it carries a label instead of a glyph.
        WalkButton(
            active = walk.active,
            onClick = { toggleWalk() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDir) + ControlsInset,
                    bottom = contentPadding.calculateBottomPadding() + ControlsBottom,
                ),
        )

        // Bottom-right: 3D stacked above recentre, above the nav bar.
        //
        // A column rather than a row, so the bottom row of the screen is the walk button
        // and one map control — the two things you reach for — instead of a walk button
        // and a pair of glyphs competing with it for the same line. Recentre keeps the
        // bottom slot because it is the one used mid-walk, so it stays under the thumb
        // while 3D moves up out of the way.
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = contentPadding.calculateEndPadding(layoutDir) + ControlsInset,
                    bottom = contentPadding.calculateBottomPadding() + ControlsBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                )
            }
            GlassIcon(
                icon = Icons.Outlined.MyLocation,
                description = stringResource(R.string.map_recenter),
                tint = colors.onBackdrop,
                onClick = { if (hasLocation) flyToMe() else requestLocation() },
            )
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
                    fontWeight = FontWeight.SemiBold,
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
private fun WalkHud(stats: WalkStats, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .glass(HudShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

@Composable
private fun WalkStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = wbwColors
    Column(modifier) {
        Text(
            label.uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 9.sp,
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = colors.onBackdrop,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
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
            fontWeight = FontWeight.SemiBold,
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
) {
    Box(
        Modifier
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
)

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
private const val RouteCasingZ = 1f
private const val RouteZ = 2f
private const val EndpointZ = 3f
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
