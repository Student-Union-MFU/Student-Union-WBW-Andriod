package th.ac.mfu.su.wbw.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
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

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess { _state.value = UiState.Success(it) }
                .onError { _state.value = UiState.Error(it) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ProfileViewModel(appContainer.profileRepository) }
        }
    }
}
