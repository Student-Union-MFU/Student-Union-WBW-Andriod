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
    /** The pull gesture's own fetch — see [StaffGroupsViewModel.refresh]. */
    val refreshing: Boolean = false,
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

    /**
     * Asks again, past the "already have it" guard [load] keeps.
     *
     * That guard is what makes [load] safe to call from the screen's every entry into
     * composition, and it is exactly what a pull has to get past: groups fill up during the
     * day, and "how many places are left" is the one thing on this list worth re-reading.
     */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            when (val r = profile.groups()) {
                is ApiResult.Success -> _state.update {
                    it.copy(groups = r.data.sortedBy { g -> g.groupNumber }, error = null)
                }
                is ApiResult.Error -> Unit
            }
            _state.update { it.copy(refreshing = false) }
        }
    }

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
