package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.ui.activities.ActivitiesScreen
import th.ac.mfu.su.wbw.ui.profile.ProfileScreen
import th.ac.mfu.su.wbw.ui.settings.SettingsScreen
import th.ac.mfu.su.wbw.ui.theme.ForestBackground

// Left→right order of destinations, so tab changes slide toward the tapped tab.
private fun routeOrder(route: String?): Int = when (route) {
    "home" -> 0
    "map" -> 1
    "steps" -> 2
    "activities" -> 3
    "profile" -> 4
    "settings" -> 5
    "checkin" -> 6
    else -> 0
}

/** The QR destination. Split out of [tabs] because it is a button beside the bar, not in it. */
private const val QrRoute = "checkin"

/** Signed-in participant shell: forest background, floating glass nav, routed screens. */
@Composable
fun HomeScaffold(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    // Icons follow iOS `MainTabView.swift`: house.fill, map.fill, figure.run.
    // `activities` has no iOS counterpart in the bar, so it keeps its own glyph
    // rather than being forced onto an iOS symbol that means something else.
    //
    // Profile is deliberately not here — it is the avatar in Home's header, which is
    // where iOS puts it too. A destination you open to check a detail and close again
    // does not earn a permanent slot next to the places you walk between.
    val tabs = listOf(
        TabItem("home", Icons.Filled.Home, Icons.Outlined.Home, stringResource(R.string.tab_home)),
        TabItem("map", Icons.Filled.Map, Icons.Outlined.Map, stringResource(R.string.tab_map)),
        TabItem("steps", Icons.AutoMirrored.Filled.DirectionsRun, Icons.AutoMirrored.Outlined.DirectionsRun, stringResource(R.string.tab_steps)),
        TabItem("activities", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, stringResource(R.string.tab_activities)),
    )
    val currentRoute = current?.route

    // Top handled per-screen via statusBarsPadding(); this carries the bottom inset + floating bar clearance.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = navInset + 96.dp)

    // What the nav bar's liquid glass refracts. Captured into a graphics layer by
    // `layerBackdrop` below and sampled by `drawBackdrop` in the bar — so the layer
    // has to wrap everything the bar should see through itself and nothing it
    // shouldn't. The bar is deliberately outside it: a pane sampling a layer it is
    // drawn inside is sampling itself.
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            ForestBackground {
                NavHost(
                    nav,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val d = if (routeOrder(targetState.destination.route) >= routeOrder(initialState.destination.route)) SlideDirection.Left else SlideDirection.Right
                        slideIntoContainer(d, tween(280)) + fadeIn(tween(180))
                    },
                    exitTransition = {
                        val d = if (routeOrder(targetState.destination.route) >= routeOrder(initialState.destination.route)) SlideDirection.Left else SlideDirection.Right
                        slideOutOfContainer(d, tween(280)) + fadeOut(tween(180))
                    },
                    popEnterTransition = {
                        slideIntoContainer(SlideDirection.Right, tween(280)) + fadeIn(tween(180))
                    },
                    popExitTransition = {
                        slideOutOfContainer(SlideDirection.Right, tween(280)) + fadeOut(tween(180))
                    },
                ) {
                    composable("home") {
                        HomeScreen(
                            contentPadding = contentPadding,
                            // Pushed rather than tab-switched: profile is opened from
                            // Home and closed back to it, so back should return there
                            // instead of unwinding to a tab you were never on.
                            onOpenProfile = { nav.navigate("profile") },
                        )
                    }
                    composable("map") { ComingSoonScreen(R.string.tab_map, Icons.Outlined.Map, contentPadding) }
                    composable("steps") { ComingSoonScreen(R.string.tab_steps, Icons.AutoMirrored.Outlined.DirectionsRun, contentPadding) }
                    composable("activities") { ActivitiesScreen(contentPadding = contentPadding) }
                    composable("profile") {
                        ProfileScreen(contentPadding = contentPadding, onOpenSettings = { nav.navigate("settings") })
                    }
                    composable("settings") {
                        SettingsScreen(contentPadding = contentPadding, onBack = { nav.popBackStack() }, onLogout = onLogout)
                    }
                    composable(QrRoute) { ComingSoonScreen(R.string.home_scan_qr, Icons.Outlined.QrCodeScanner, contentPadding) }
                }
            }
        }

        // Outside the backdrop layer above — see [backdrop].
        FloatingTabBar(
            items = tabs,
            currentRoute = currentRoute,
            onSelect = { route -> navigateTab(nav, route) },
            backdrop = backdrop,
            // QR is always present, not only on Home. It used to be a FAB that
            // appeared on one screen; on iOS it is a permanent tab, and a checkpoint
            // is the last place to make someone navigate home first.
            qrSelected = currentRoute == QrRoute,
            onSelectQr = { navigateTab(nav, QrRoute) },
            qrIcon = Icons.Outlined.QrCode2,
            qrContentDescription = stringResource(R.string.home_scan_qr),
            // Insets measured off the iOS screenshot: the pill clears the screen edge
            // by about 20dp and floats well above the gesture bar rather than sitting
            // on it.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = navInset + 18.dp),
        )
    }
}

private fun navigateTab(nav: androidx.navigation.NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
