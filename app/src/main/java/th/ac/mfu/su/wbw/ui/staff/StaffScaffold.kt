package th.ac.mfu.su.wbw.ui.staff

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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Warning
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
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.ui.home.FloatingTabBar
import th.ac.mfu.su.wbw.ui.home.TabItem
import th.ac.mfu.su.wbw.ui.map.MapScreen
import th.ac.mfu.su.wbw.ui.settings.SettingsScreen
import th.ac.mfu.su.wbw.ui.theme.ForestBackground

private fun routeOrder(route: String?): Int = when (route) {
    "alerts" -> 0
    "map" -> 1
    "settings" -> 2
    else -> 0
}

/**
 * The signed-in shell for a **staff or admin** account.
 *
 * A separate scaffold rather than the participant one with tabs hidden, because a staff
 * account is not a participant with extra permissions — it is a different kind of row.
 * There is no `participant_profile` for it, so `GET /wbw/me` answers 404 (the query is
 * scoped `WHERE u.role = 'participant'`), and every participant screen is built on that
 * call. Reusing that shell gave a staff member a retry card where their name should be, an
 * empty pass, and a chat convinced they had no group.
 *
 * Two tabs, and no more than the server can actually back:
 *
 *  - **Alerts** — the live emergency console, `GET /wbw/staff/sos`. The one staff duty that
 *    is fully built end to end, and the one that is worthless if it is not on the screen
 *    somebody is already looking at.
 *  - **Map** — the same trail map participants get, with its emergency layer switched off
 *    (`emergency = false`): the SOS button and its watch belong to a participant, and staff
 *    answer emergencies on the Alerts tab rather than raise them. What is left needs
 *    nothing from `/me`, and knowing where the route runs is as much a staff need as a
 *    walker's.
 *
 * Deliberately absent: chat (staff belong to no group, so there is no channel to open), the
 * pass and its QR (there is no pass to hold up), and the bloom (it counts a participant's
 * own check-ins). Check-in scanning — `POST /wbw/staff/checkin` — exists on the server and
 * has no screen here yet; it needs a camera scanner, which is its own piece of work.
 */
@Composable
fun StaffScaffold(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        TabItem("alerts", Icons.Filled.Warning, Icons.Outlined.Warning, stringResource(R.string.tab_alerts)),
        TabItem("map", Icons.Filled.Map, Icons.Outlined.Map, stringResource(R.string.tab_map)),
    )

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = navInset + 96.dp)

    ForestBackground {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                nav,
                startDestination = "alerts",
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val d = if (routeOrder(targetState.destination.route) >= routeOrder(initialState.destination.route)) SlideDirection.Left else SlideDirection.Right
                    slideIntoContainer(d, tween(280)) + fadeIn(tween(180))
                },
                exitTransition = {
                    val d = if (routeOrder(targetState.destination.route) >= routeOrder(initialState.destination.route)) SlideDirection.Left else SlideDirection.Right
                    slideOutOfContainer(d, tween(280)) + fadeOut(tween(180))
                },
                popEnterTransition = { slideIntoContainer(SlideDirection.Right, tween(280)) + fadeIn(tween(180)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(280)) + fadeOut(tween(180)) },
            ) {
                composable("alerts") {
                    StaffHomeScreen(
                        session = session,
                        contentPadding = contentPadding,
                        onOpenSettings = { nav.navigate("settings") },
                    )
                }
                composable("map") { MapScreen(contentPadding = contentPadding, emergency = false) }
                composable("settings") {
                    SettingsScreen(
                        contentPadding = contentPadding,
                        onBack = { nav.popBackStack() },
                        onLogout = onLogout,
                    )
                }
            }

            // No QR button beside the bar: that button is the participant's pass, and a
            // staff account has no pass. Filling the slot with something else would put a
            // QR glyph on a button that produces no QR code, at a checkpoint, on the day.
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = navInset + 18.dp),
            )
        }
    }
}
