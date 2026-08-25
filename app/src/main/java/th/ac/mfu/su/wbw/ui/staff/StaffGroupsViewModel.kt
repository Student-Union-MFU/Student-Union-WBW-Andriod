package th.ac.mfu.su.wbw.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.ui.appContainer

data class StaffGroupsUiState(
    val groups: List<Group> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * The event's groups, for the staff chat picker.
 *
 * `GET /wbw/groups` is the same list the participant group picker uses, and it is not
 * scoped to the caller — it is the event's shape, which a staff account is entitled to see.
 * Reused rather than given its own endpoint for that reason.
 */
class StaffGroupsViewModel(private val profile: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(StaffGroupsUiState())
    val state: StateFlow<StaffGroupsUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.groups.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = profile.groups()) {
                is ApiResult.Success -> _state.update {
                    // By number, so the list reads the way the event is organised rather
                    // than in whatever order the rows came back.
                    it.copy(groups = r.data.sortedBy { g -> g.groupNumber }, loading = false)
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { StaffGroupsViewModel(appContainer.profileRepository) }
        }
    }
}
