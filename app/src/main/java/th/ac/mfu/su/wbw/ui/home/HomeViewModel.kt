package th.ac.mfu.su.wbw.ui.home

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

/**
 * Home dashboard state. Loads the participant profile for the greeting and pass
 * details. Base-camp / tree progress is a visual placeholder for now — the Go
 * backend has no bases/check-in-count endpoint yet (see [HomeUiModel.bases]).
 */
class HomeViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeUiModel>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess { _state.value = UiState.Success(HomeUiModel.from(it)) }
                .onError { _state.value = UiState.Error(it) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(appContainer.profileRepository) }
        }
    }
}

data class HomeUiModel(
    val displayName: String,
    val checkedInBases: Int,
    val totalBases: Int,
    val nextBaseName: String,
    val nextBaseDistance: String,
) {
    val phase: GrowthPhase get() = GrowthPhase.forProgress(checkedInBases, totalBases)
    val progress: Float get() = if (totalBases == 0) 0f else checkedInBases.toFloat() / totalBases

    companion object {
        fun from(p: ParticipantDetail): HomeUiModel = HomeUiModel(
            displayName = p.firstName ?: p.fullName,
            // TODO: replace with real base check-in counts once the backend exposes them.
            checkedInBases = if (p.checkedIn) 3 else 0,
            totalBases = 8,
            nextBaseName = "Pine Grove",
            nextBaseDistance = "480 m",
        )
    }
}
