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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCodeScanner
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import th.ac.mfu.su.wbw.ui.chat.ChatScreen
import th.ac.mfu.su.wbw.ui.group.GroupInfoScreen
import th.ac.mfu.su.wbw.ui.chat.ChatViewModel
import th.ac.mfu.su.wbw.ui.map.MapScreen
import th.ac.mfu.su.wbw.ui.settings.SettingsScreen
import th.ac.mfu.su.wbw.ui.theme.ForestBackground

private fun routeOrder(route: String?): Int = when (route) {
    "alerts" -> 0
    "groups" -> 1
    // Both hang off the groups tab, so they slide in from its side rather than from
    // wherever a lexical guess would put them.
    "groupChat/{groupId}/{groupNumber}" -> 1
    "groupInfo/{groupId}/{groupNumber}" -> 1
    "map" -> 2
    "scan" -> 3
    "settings" -> 4
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
 * Beside the bar, where a participant's phone keeps their pass, is the check-in scanner —
 * `POST /wbw/staff/checkin`. A destination rather than a third tab: it is something a staff
 * member goes and does and comes back from, while the two tabs are what the account watches.
 *
 *  - **Chat** — every group, and the conversation inside whichever one is opened. A
 *    participant's chat tab goes straight into their own group because they have exactly
 *    one; a staff account has none, so it picks. The server lets staff into any group's
 *    chat (see `CanUseGroupChat`), which is the point: coordinating means talking to the
 *    group that needs it, not to one assigned at login.
 *
 * Deliberately absent: the pass itself (there is no pass to hold up — the same slot reads
 * codes here instead of showing one), and the bloom (it counts a participant's own
 * check-ins).
 */
@Composable
fun StaffScaffold(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        TabItem("alerts", Icons.Filled.Warning, Icons.Outlined.Warning, stringResource(R.string.tab_alerts)),
        TabItem("groups", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.tab_chat)),
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
                composable("groups") {
                    StaffGroupsScreen(
                        contentPadding = contentPadding,
                        onOpenGroup = { g -> nav.navigate("groupChat/${g.groupId}/${g.groupNumber}") },
                    )
                }
                composable(
                    "groupChat/{groupId}/{groupNumber}",
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.IntType },
                        navArgument("groupNumber") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val groupId = entry.arguments?.getInt("groupId") ?: return@composable
                    val groupNumber = entry.arguments?.getInt("groupNumber")
                    ChatScreen(
                        contentPadding = contentPadding,
                        onOpenInfo = { gid, num -> nav.navigate("groupInfo/$gid/${num ?: -1}") },
                        // Keyed to this entry, so opening group 3 and then group 7 gets two
                        // view models rather than one told to forget the first thread.
                        viewModel = viewModel(factory = ChatViewModel.factoryFor(groupId, groupNumber)),
                    )
                }
                composable(
                    "groupInfo/{groupId}/{groupNumber}",
                    arguments = listOf(
                        navArgument("groupId") { type = NavType.IntType },
                        navArgument("groupNumber") { type = NavType.IntType },
                    ),
                ) { entry ->
                    val gid = entry.arguments?.getInt("groupId") ?: return@composable
                    val num = entry.arguments?.getInt("groupNumber")?.takeIf { it >= 0 }
                    GroupInfoScreen(
                        groupId = gid,
                        groupNumber = num,
                        // Staff belong to no group, so there is nothing here for them to
                        // leave — and `POST /groups/leave` would answer for their own
                        // (nonexistent) membership rather than for the group on screen.
                        canLeave = false,
                        contentPadding = contentPadding,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("map") { MapScreen(contentPadding = contentPadding, emergency = false) }
                composable("scan") {
                    StaffScanScreen(
                        contentPadding = contentPadding,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        contentPadding = contentPadding,
                        onBack = { nav.popBackStack() },
                        onLogout = onLogout,
                    )
                }
            }

            // The button beside the bar is the scanner, where a participant's phone puts
            // their pass. The two are the same gesture from opposite sides of the table:
            // one shows a code, the other reads it. It is a destination rather than a tab
            // because it is a thing you go and do and come back from, not a place the
            // staff shell rests — and because the bar's two tabs are what this account
            // watches, while this is what it acts with.
            FloatingTabBar(
                items = tabs,
                currentRoute = currentRoute,
                qrSelected = currentRoute == "scan",
                onSelectQr = {
                    nav.navigate("scan") {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                qrIcon = Icons.Outlined.QrCodeScanner,
                qrContentDescription = stringResource(R.string.scan_title),
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
