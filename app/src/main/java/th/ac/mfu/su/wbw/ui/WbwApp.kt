package th.ac.mfu.su.wbw.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.ui.auth.LoginScreen
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.group.GroupGate
import th.ac.mfu.su.wbw.ui.intro.IntroScreen

/**
 * Root composable. Swaps the entire subtree on auth changes: logged out → the login
 * screen; logged in → the participant home scaffold.
 *
 * Logged out is a single screen rather than a graph. It used to be a NavHost of login ⇄
 * register, but participants do not make their own accounts — the organisers issue them —
 * so registration is gone and a navigation graph with one destination in it is only
 * indirection.
 *
 * In front of both, once per launch, is [IntroScreen] — the bloom opening from bud to full
 * flower. It is deliberately not conditional on being logged out: it is the app starting,
 * not a step in signing in, and it hands over to whichever screen the session resolves to.
 * For a signed-out participant that is the login screen, which is the path most people see.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WbwApp() {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val state by appViewModel.state.collectAsStateWithLifecycle()

    /**
     * `rememberSaveable`, so the intro plays once per *launch* rather than once per
     * composition. A theme switch or a language change recreates the activity, and sitting
     * through the flower again every time somebody toggles dark mode in Settings would
     * turn the nicest thing in the app into a punishment. Saved state does not survive
     * process death, which is exactly the line wanted: cold start replays it, everything
     * else does not.
     */
    var introPlayed by rememberSaveable { mutableStateOf(false) }

    // No overscroll stretch, app-wide. The effect scales the whole scrolling subtree at
    // the end of a list, which drags the glass panes with it — and a pane's refraction is
    // sampled from a layer that is *not* being stretched, so the surface and the image it
    // shows through slide apart for the length of the bounce. On opaque cards the stretch
    // is a flourish; on these it reads as the material tearing.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        // A dissolve rather than a slide or a cut. Both sides draw the same backdrop and
        // the same wave lines, so what actually crossfades is the flower giving way to the
        // sign-in card while the ground under them holds still — which only works because
        // the two screens are built on the same two layers.
        //
        // It also covers the session read for free: the intro is on screen for its own
        // reasons while the token is loaded off disk, so `AuthState.Loading` has almost
        // always resolved by the time anything needs it, and the spinner below is a
        // fallback rather than a step everyone sees.
        Crossfade(
            targetState = introPlayed,
            animationSpec = tween(HandoverMillis),
            label = "intro",
        ) { played ->
            if (!played) {
                IntroScreen(onFinished = { introPlayed = true })
            } else {
                when (val s = state) {
                    is AuthState.Loading -> LoadingState()
                    is AuthState.LoggedOut -> LoginScreen()
                    // Not HomeScaffold directly: a participant signing in for the first
                    // time has no group yet, and [GroupGate] is what stands in front of the
                    // app until they pick one.
                    is AuthState.LoggedIn -> GroupGate(
                        session = s.session,
                        onLogout = appViewModel::logout,
                    )
                }
            }
        }
    }
}

/**
 * The dissolve from the intro into the app.
 *
 * Long for a screen transition, on purpose: it is the seam between the flower and the
 * login card, and both are drawn on the same still background, so there is nothing moving
 * to make a quick cut read as deliberate rather than as a flicker.
 */
private const val HandoverMillis = 650
