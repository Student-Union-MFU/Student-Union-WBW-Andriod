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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QrCode2
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import th.ac.mfu.su.wbw.ui.chat.ChatScreen
import th.ac.mfu.su.wbw.ui.group.GroupInfoScreen
import th.ac.mfu.su.wbw.ui.map.MapScreen
import th.ac.mfu.su.wbw.ui.notifications.NotificationsScreen
import th.ac.mfu.su.wbw.ui.profile.ProfileScreen
import th.ac.mfu.su.wbw.ui.settings.SettingsScreen
import th.ac.mfu.su.wbw.ui.staff.StaffScaffold
import th.ac.mfu.su.wbw.ui.theme.ForestBackground

// Left→right order of destinations, so tab changes slide toward the tapped tab.
private fun routeOrder(route: String?): Int = when {
    route == null -> 0
    // Parameterised routes arrive here as their *pattern* — "groupInfo/{groupId}/{groupNumber}"
    // — so they have to be matched by prefix. Missing this was the whole bug: the group page
    // fell through to `else -> 0`, which put it *below* chat, so opening it slid in from the
    // left like a step backwards, and closing it slid the same way again. Forward and back
    // animating identically is what made it read as looping rather than as going somewhere
    // and coming back.
    route.startsWith("groupInfo") -> 6
    route == "home" -> 0
    route == "map" -> 1
    route == "chat" -> 2
    route == "notifications" -> 3
    route == "profile" -> 4
    route == "settings" -> 5
    else -> 0
}

/**
 * Where the QR button beside the bar goes: the participant pass.
 *
 * Split out of [tabs] because it is a button beside the bar, not in it — and it keeps the
 * QR glyph because the pass *is* a QR code. The thing a participant holds up at a
 * checkpoint is their own pass, so the button that looks like a QR code should produce
 * one, rather than opening a scanner for reading somebody else's.
 *
 * It used to point at a `checkin` stub that only ever said "coming soon". Scanning — the
 * staff member's side of the same transaction — still has no server behind it; when it arrives
 * it needs its own route rather than this one back.
 */
private const val QrRoute = "profile"

/**
 * The signed-in shell, chosen by what kind of account this is.
 *
 * Staff and admin get an entirely separate scaffold rather than the participant one with
 * pieces hidden. They are not participants with extra buttons: they have no
 * `participant_profile` row at all, which is not a cosmetic difference — `GET /wbw/me` is
 * scoped `WHERE u.role = 'participant'` and answers **404** for them. Every screen below
 * this line is built on that call, so a staff account signing into the participant shell
 * gets a retry card for a name, an empty pass, and a chat that believes they have no group.
 *
 * See [th.ac.mfu.su.wbw.ui.staff.StaffScaffold] for what they get instead.
 */
@Composable
fun HomeScaffold(session: Session, onLogout: () -> Unit) {
    if (session.isStaff) {
        StaffScaffold(session = session, onLogout = onLogout)
        return
    }
    ParticipantScaffold(session = session, onLogout = onLogout)
}

/** Signed-in participant shell: forest background, floating glass nav, routed screens. */
@Composable
private fun ParticipantScaffold(session: Session, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    // Icons follow iOS `MainTabView.swift`: house.fill, map.fill, and — for the chat
    // slot — the message glyph iOS uses once a participant has a group.
    //
    // Three tabs, not four. `activities` was the fourth and is gone: it had no iOS
    // counterpart in the bar, no backend behind it, and it was the only destination in
    // the bar that a participant could open and find nothing to act on. A bar is a
    // promise that each slot leads somewhere worth going.
    //
    // Profile is deliberately not here — it is the avatar in Home's header, which is
    // where iOS puts it too. A destination you open to check a detail and close again
    // does not earn a permanent slot next to the places you walk between.
    val tabs = listOf(
        TabItem("home", Icons.Filled.Home, Icons.Outlined.Home, stringResource(R.string.tab_home)),
        TabItem("map", Icons.Filled.Map, Icons.Outlined.Map, stringResource(R.string.tab_map)),
        TabItem("chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.tab_chat)),
    )
    val currentRoute = current?.route

    // Top handled per-screen via statusBarsPadding(); this carries the bottom inset + floating bar clearance.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = navInset + 96.dp)

    // The bar sits inside ForestBackground rather than beside it, so it refracts the
    // same wallpaper layer the cards do (ForestBackground provides it via LocalBackdrop).
    //
    // It used to sample its own layer wrapping the whole scene, which meant it refracted
    // the screen content scrolling underneath it. That reads well over plain cards and
    // badly over glass ones: with both translucent, the bar was smearing panes that were
    // themselves already refracting the wallpaper. One material, one source.
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
                        HomeScreen(
                            contentPadding = contentPadding,
                            // Pushed rather than tab-switched: settings is opened from
                            // Home and closed back to it, so back should return there
                            // instead of unwinding to a tab you were never on.
                            onOpenSettings = { nav.navigate("settings") },
                            // Same push, for the same reason — announcements are read and
                            // dismissed, not walked between.
                            onOpenNotifications = { nav.navigate("notifications") },
                        )
                    }
                    composable("map") { MapScreen(contentPadding = contentPadding) }
                    composable("chat") {
                    ChatScreen(
                        contentPadding = contentPadding,
                        onOpenInfo = { gid, num -> nav.navigate("groupInfo/$gid/${num ?: -1}") },
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
                    // -1 stands in for "no number", because a nav argument cannot be null.
                    val num = entry.arguments?.getInt("groupNumber")?.takeIf { it >= 0 }
                    GroupInfoScreen(
                        groupId = gid,
                        groupNumber = num,
                        // A participant is in this group, so leaving is theirs to do —
                        // subject to the quota, which the screen reads for itself.
                        canLeave = true,
                        contentPadding = contentPadding,
                        onBack = { nav.popBackStack() },
                    )
                }
                    composable("notifications") {
                        NotificationsScreen(
                            contentPadding = contentPadding,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            contentPadding = contentPadding,
                            onBack = { nav.popBackStack() },
                            onOpenSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(contentPadding = contentPadding, onBack = { nav.popBackStack() }, onLogout = onLogout)
                    }
                }

            // Inside the Box so it can align to the bottom, and inside ForestBackground
            // so LocalBackdrop reaches it.
            FloatingTabBar(
                items = tabs,
                currentRoute = currentRoute,
                onSelect = { route -> navigateTab(nav, route) },
                // Always present, not only on Home. It used to be a FAB that appeared on
                // one screen, and a checkpoint is the last place to make someone navigate
                // home first before they can show anyone anything.
                qrSelected = currentRoute == QrRoute,
                onSelectQr = { navigateTab(nav, QrRoute) },
                qrIcon = Icons.Outlined.QrCode2,
                qrContentDescription = stringResource(R.string.profile_pass_title),
                // Insets measured off the iOS screenshot: the pill clears the screen
                // edge by about 20dp and floats well above the gesture bar.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = navInset + 18.dp),
            )
        }
    }
}

private fun navigateTab(nav: androidx.navigation.NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
