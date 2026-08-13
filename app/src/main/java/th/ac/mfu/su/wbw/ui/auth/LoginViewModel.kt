package th.ac.mfu.su.wbw.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onError
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.repository.AuthRepository
import th.ac.mfu.su.wbw.ui.appContainer

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = username.isNotBlank() && password.isNotBlank() && !loading
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onUsername(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }

    /** Attempt login. On success the session flow routes the app to home. */
    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(s.username, s.password)
                .onError { msg -> _state.update { it.copy(loading = false, error = msg) } }
                .onSuccess { /* AppViewModel observes the session and navigates */ }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { LoginViewModel(appContainer.authRepository) }
        }
    }
}
