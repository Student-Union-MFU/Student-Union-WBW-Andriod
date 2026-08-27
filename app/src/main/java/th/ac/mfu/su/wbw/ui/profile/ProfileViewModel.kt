package th.ac.mfu.su.wbw.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onError
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.common.UiState

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ParticipantDetail>>(UiState.Loading)
    val state = _state.asStateFlow()

    init {
        // The pass opens on last run's data rather than a spinner. This is the screen where
        // that matters most: it is held up to a staff member at a checkpoint, which is exactly
        // where the signal is worst and where waiting is least acceptable.
        repository.cachedMe()?.let { _state.value = UiState.Success(it) }
        load()
    }

    /**
     * True while the pull gesture's own fetch is in flight.
     *
     * Never [UiState.Loading]. This screen is the pass, and it is refreshed while it is
     * being held up at a checkpoint — taking the QR code away to show a spinner is the one
     * thing it must not do, whatever the network is doing.
     */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            repository.me().onSuccess { _state.value = UiState.Success(it) }
            _refreshing.value = false
        }
    }

    fun load() {
        if (_state.value !is UiState.Success) _state.value = UiState.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess { _state.value = UiState.Success(it) }
                // A stale pass beats no pass — see the note in `init`.
                .onError { if (_state.value !is UiState.Success) _state.value = UiState.Error(it) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ProfileViewModel(appContainer.profileRepository) }
        }
    }
}
