package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.ui.activities.ActivitiesScreen
import th.ac.mfu.su.wbw.ui.profile.ProfileScreen
import th.ac.mfu.su.wbw.ui.settings.SettingsScreen
import th.ac.mfu.su.wbw.ui.theme.Cream
import th.ac.mfu.su.wbw.ui.theme.DeepForest
import th.ac.mfu.su.wbw.ui.theme.Forest
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.theme.wbwColors

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

/** Signed-in participant shell: forest background, floating glass nav, routed screens. */
@Composable
fun HomeScaffold(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    val tabs = listOf(
        TabItem("home", Icons.Filled.Park, Icons.Outlined.Park, stringResource(R.string.tab_home)),
        TabItem("map", Icons.Filled.Map, Icons.Outlined.Map, stringResource(R.string.tab_map)),
        TabItem("steps", Icons.Filled.DirectionsWalk, Icons.Outlined.DirectionsWalk, stringResource(R.string.tab_steps)),
        TabItem("activities", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, stringResource(R.string.tab_activities)),
        TabItem("profile", Icons.Filled.Person, Icons.Outlined.Person, stringResource(R.string.profile_title)),
    )
    val currentRoute = current?.route

    // Top handled per-screen via statusBarsPadding(); this carries the bottom inset + floating bar clearance.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = navInset + 96.dp)

    ForestBackground {
        Box(Modifier.fillMaxSize()) {
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
                    HomeScreen(contentPadding = contentPadding, onNavigateBase = { nav.navigate("map") })
                }
                composable("map") { ComingSoonScreen(R.string.tab_map, Icons.Outlined.Map, contentPadding) }
                composable("steps") { ComingSoonScreen(R.string.tab_steps, Icons.Outlined.DirectionsWalk, contentPadding) }
                composable("activities") { ActivitiesScreen(contentPadding = contentPadding) }
                composable("profile") {
                    ProfileScreen(contentPadding = contentPadding, onOpenSettings = { nav.navigate("settings") })
                }
                composable("settings") {
                    SettingsScreen(contentPadding = contentPadding, onBack = { nav.popBackStack() }, onLogout = onLogout)
                }
                composable("checkin") { ComingSoonScreen(R.string.home_scan_qr, Icons.Outlined.QrCodeScanner, contentPadding) }
            }

            // QR scan FAB — only on Home
            if (currentRoute == "home") {
                val colors = wbwColors
                val fabShape = RoundedCornerShape(20.dp)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 22.dp, bottom = navInset + 92.dp)
                        .shadow(16.dp, fabShape, clip = false, spotColor = Color.Black.copy(alpha = 0.5f), ambientColor = Color.Black.copy(alpha = 0.32f))
                        .size(56.dp)
                        .clip(fabShape)
                        .background(if (colors.isDark) Cream else Forest)
                        .clickable(onClick = { nav.navigate("checkin") }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.CropFree, stringResource(R.string.home_scan_qr), tint = if (colors.isDark) DeepForest else Cream, modifier = Modifier.size(26.dp))
                }
            }

            FloatingTabBar(
                items = tabs,
                currentRoute = currentRoute,
                onSelect = { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp)
                    .padding(bottom = navInset + 12.dp),
            )
        }
    }
}
