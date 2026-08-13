package th.ac.mfu.su.wbw.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.repository.AuthRepository

/** Top-level auth state that decides which graph the app shows. */
sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val session: Session) : AuthState
}

/** Observes the persisted session and exposes logout. */
class AppViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val state = authRepository.session
        .map { if (it == null) AuthState.LoggedOut else AuthState.LoggedIn(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    fun logout() = viewModelScope.launch { authRepository.logout() }

    companion object {
        val Factory = viewModelFactory {
            initializer { AppViewModel(appContainer.authRepository) }
        }
    }
}
