package th.ac.mfu.su.wbw.ui

import androidx.compose.runtime.Composable
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
@Composable
fun WbwApp() {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    val state by appViewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is AuthState.Loading -> LoadingState()
        is AuthState.LoggedOut -> AuthNavHost()
        is AuthState.LoggedIn -> HomeScaffold(
            session = s.session,
            onLogout = appViewModel::logout,
        )
    }
}
