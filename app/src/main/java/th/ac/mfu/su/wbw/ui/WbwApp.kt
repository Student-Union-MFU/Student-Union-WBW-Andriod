package th.ac.mfu.su.wbw.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.ui.auth.AuthNavHost
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.home.HomeScaffold

/**
 * Root composable. Swaps the entire subtree on auth changes: logged out → auth
 * graph (login/register); logged in → the participant home scaffold. Login and
 * logout flip the persisted session, which flows back here and re-routes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WbwApp() {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val state by appViewModel.state.collectAsStateWithLifecycle()

    // No overscroll stretch, app-wide. The effect scales the whole scrolling subtree at
    // the end of a list, which drags the glass panes with it — and a pane's refraction is
    // sampled from a layer that is *not* being stretched, so the surface and the image it
    // shows through slide apart for the length of the bounce. On opaque cards the stretch
    // is a flourish; on these it reads as the material tearing.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        when (val s = state) {
            is AuthState.Loading -> LoadingState()
            is AuthState.LoggedOut -> AuthNavHost()
            is AuthState.LoggedIn -> HomeScaffold(
                session = s.session,
                onLogout = appViewModel::logout,
            )
        }
    }
}
