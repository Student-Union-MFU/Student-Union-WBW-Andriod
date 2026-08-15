package th.ac.mfu.su.wbw.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
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
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
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
 *  - **Search** is Places autocomplete: find somewhere and the camera flies to it.
 *  - **Places markers** are a nearby search around the trail, drawn as the app's own green
 *    dots rather than Google's red pins.
 *
 * Location is one fix, not a stream — "where am I on the trail", not turn-by-turn.
 *
 * The Maps/Places key comes from `local.properties` via the manifest ([mapsApiKey]); with
 * no key the tiles come back blank and search/markers are skipped, but nothing here crashes.
 */
@Composable
fun MapScreen(contentPadding: PaddingValues) {
    val colors = wbwColors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey = remember { context.mapsApiKey() }
    val hasKey = apiKey.isNotBlank()

    // Legacy init on purpose. The "Places API (New)" endpoint (places.googleapis.com) is a
    // separate switch in the Cloud console and returns "blocked" until it is flipped; the
    // classic Places API — which the autocomplete widget and findCurrentPlace below use —
    // is the one already enabled for this key.
    LaunchedEffect(apiKey) {
        if (hasKey && !Places.isInitialized()) {
            Places.initialize(context.applicationContext, apiKey)
        }
    }

    fun granted() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    var hasLocation by remember { mutableStateOf(granted()) }
    var is3d by remember { mutableStateOf(false) }
    var places by remember { mutableStateOf<List<TrailPlace>>(emptyList()) }

    // The route is the screen's subject, so it is read before anything else — the camera
    // below is framed from it rather than from a guessed centre.
    val route = remember { TrailRoute.load(context) }
    val density = LocalDensity.current
    val routeWidthPx = remember(density) { with(density) { RouteWidth.toPx() } }
    val routeCasingPx = remember(density) { with(density) { (RouteWidth + RouteCasing * 2).toPx() } }
    val fitPaddingPx = remember(density) { with(density) { FitPadding.roundToPx() } }

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
            place = dotDescriptor(WbwGreenDark.toArgb()),
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

    // Places around the device — legacy findCurrentPlace, which the classic Places API
    // serves. Needs a location fix, so it is called once permission is in hand rather than
    // on first composition. A best-effort decoration: any failure just leaves the map
    // unmarked, never crashes.
    @SuppressLint("MissingPermission")
    fun loadNearby() {
        if (!hasKey || !granted() || !Places.isInitialized()) return
        runCatching {
            val client = Places.createClient(context)
            val request = FindCurrentPlaceRequest.newInstance(
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG),
            )
            client.findCurrentPlace(request)
                .addOnSuccessListener { response ->
                    places = response.placeLikelihoods
                        .mapNotNull { likely ->
                            val p = likely.place
                            val ll = p.latLng ?: return@mapNotNull null
                            TrailPlace(p.id ?: ll.toString(), p.name.orEmpty(), ll)
                        }
                        .take(MaxPlaces)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("WbwMap", "nearby failed: ${e.message}", e)
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocation = result.values.any { it }
        if (hasLocation) {
            flyToMe()
            loadNearby()
        }
    }

    val searchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            Autocomplete.getPlaceFromIntent(data).latLng?.let { flyTo(it, MeZoom) }
        }
    }

    fun openSearch() {
        if (!hasKey || !Places.isInitialized()) return
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG),
        ).build(context)
        searchLauncher.launch(intent)
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

    // Permission and nearby places on open — but no longer a fly-to-me. The opening
    // camera belongs to the route: this screen exists to show where the walk goes, and
    // someone standing on the trail already knows where they are standing. Recentring on
    // yourself is one tap away on the button below, which is the right way round.
    LaunchedEffect(Unit) {
        if (hasLocation) loadNearby() else requestLocation()
    }

    // 3D is the native camera tilting, so it belongs to the camera, not to a separate view.
    //
    // The first pass is skipped. This effect runs once at composition with `is3d` already
    // false, which animated the camera to a tilt it was, and that animation raced the route
    // fit for the same camera — the map opened somewhere between the two.
    // It also holds its zoom instead of forcing one. It used to jump to zoom 18, which was
    // survivable when the map opened at a guessed centre and became useless once it opened
    // fitted to the whole 8.4km loop: tapping 3D threw the route off-screen and left an
    // empty field. Tilt is the thing the button is for; how far in the user is zoomed is
    // their business.
    val tiltSettled = remember { mutableStateOf(false) }
    LaunchedEffect(is3d) {
        if (!tiltSettled.value) {
            tiltSettled.value = true
            return@LaunchedEffect
        }
        // A running walk owns the camera. Two effects animating it at once is a fight the
        // user sees as stutter.
        if (WalkTracker.stats.value.active) return@LaunchedEffect
        val cur = cameraPositionState.position
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(cur.target)
                    .zoom(cur.zoom)
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
            contentPadding = contentPadding,
            // Fired once the map has a size, which is the first moment newLatLngBounds is
            // legal — it throws outright if asked to fit a bounds into a zero-sized map.
            // Wrapped anyway: a camera that failed to frame the route is a worse map, not a
            // broken app, and the holding position above is already a reasonable view.
            onMapLoaded = {
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

            places.forEach { place ->
                Marker(
                    state = rememberMarkerState(key = place.id, position = place.position),
                    title = place.name.ifBlank { null },
                    icon = icons.place,
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                )
            }
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
                    start = contentPadding.calculateStartPadding(layoutDir) + 18.dp,
                    end = contentPadding.calculateEndPadding(layoutDir) + 18.dp,
                    top = 14.dp,
                ),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassPill {
                    Text(
                        stringResource(R.string.map_title).uppercase(),
                        color = colors.onBackdrop,
                        fontSize = 11.sp,
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (hasKey) {
                    GlassIcon(
                        icon = Icons.Outlined.Search,
                        description = stringResource(R.string.map_search),
                        tint = colors.onBackdrop,
                        onClick = { openSearch() },
                    )
                }
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
                    start = contentPadding.calculateStartPadding(layoutDir) + 18.dp,
                    bottom = contentPadding.calculateBottomPadding() + ControlsBottom,
                ),
        )

        // Bottom-right: 3D toggle + recenter, above the nav bar.
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = contentPadding.calculateEndPadding(layoutDir) + 18.dp,
                    bottom = contentPadding.calculateBottomPadding() + ControlsBottom,
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
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
private fun GlassPill(content: @Composable () -> Unit) {
    Box(
        Modifier
            .glass(RoundedCornerShape(50), fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
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

/** A place returned by the nearby search, flattened to what a marker needs. */
private data class TrailPlace(val id: String, val name: String, val position: LatLng)

/**
 * Every marker bitmap the screen draws, built together.
 *
 * Grouped rather than held one-per-`remember` so they share the single point where the
 * Maps SDK is initialised — see the call site. Bitmaps, so they are made once and reused
 * across recompositions rather than redrawn per frame.
 */
private class MapIcons(
    val place: BitmapDescriptor,
    val start: BitmapDescriptor,
    val finish: BitmapDescriptor,
)

/**
 * The marker: a small filled dot with a white ring, drawn once to a bitmap.
 *
 * A drawn dot rather than `defaultMarker`, which is Google's red-with-shadow teardrop —
 * on a screen this green it is the one thing that does not belong. The ring keeps it legible
 * where two greens meet.
 */
private fun dotDescriptor(fillArgb: Int): BitmapDescriptor {
    val px = 46
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = px / 2f
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillArgb or 0xFF000000.toInt() }
    canvas.drawCircle(c, c, c, ring)
    canvas.drawCircle(c, c, c - 5f, fill)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

/**
 * The route's two endpoints: a filled disc for the start, the same disc with its middle
 * punched out for the finish.
 *
 * The hole is a real transparency (PorterDuff CLEAR) rather than a circle painted in the
 * map's background colour — the ground under a marker is whatever the route, a road or a
 * field happens to be there, and a fake hole in one fixed colour only lines up over empty
 * ground. The white ring is the same device [dotDescriptor] uses, for the same reason: two
 * greens meeting need something achromatic between them.
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
private const val MaxPlaces = 14

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
 * How far the bottom controls sit above the map's padded edge.
 *
 * Not a spacing choice. The Maps SDK draws the Google wordmark at the bottom of that padded
 * region, and the terms of service require it to stay visible — at the previous 14dp the
 * walk button's corner sat straight on top of it. This clears the wordmark's own height and
 * margin, and both bottom rows use it so they stay on one line.
 */
private val ControlsBottom = 38.dp

/** Tap with no ripple — a ripple on a glass pane paints a grey disc over the refraction. */
private fun Modifier.tapNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
