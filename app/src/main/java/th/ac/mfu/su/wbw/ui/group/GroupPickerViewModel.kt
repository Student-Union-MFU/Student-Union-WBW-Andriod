package th.ac.mfu.su.wbw.ui.group

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

data class GroupPickerState(
    val groups: List<Group> = emptyList(),
    val loading: Boolean = true,
    val joining: Int? = null,
    val error: String? = null,
    /** Set once the join has landed and the profile confirms it. */
    val joinedGroupId: Int? = null,
)

/**
 * The one-time group choice.
 *
 * There are forty groups of fifty, and a participant belongs to exactly one. The list is
 * fetched rather than assumed: `seats_left` is the whole basis of the screen and it changes
 * under the participant as two thousand people pick at once, which is also why a join can
 * fail with a perfectly ordinary 409 and simply needs re-listing rather than an apology.
 */
class GroupPickerViewModel(
    private val profile: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupPickerState())
    val state: StateFlow<GroupPickerState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = profile.groups()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        groups = result.data.sortedBy { g -> g.groupNumber },
                        loading = false,
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }

    fun join(groupId: Int) {
        if (_state.value.joining != null) return
        _state.update { it.copy(joining = groupId, error = null) }
        viewModelScope.launch {
            when (val result = profile.joinGroup(groupId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(joining = null, joinedGroupId = groupId)
                }
                is ApiResult.Error -> {
                    // A 409 here is the group filling up between the list and the tap, or
                    // this participant already being in one. Both are answered by showing
                    // the current truth rather than by an error the participant can only
                    // stare at, so the list is reloaded underneath the message.
                    _state.update { it.copy(joining = null, error = result.message) }
                    if (result.code == 409 || result.code == 404) load()
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    companion object {
        val Factory = viewModelFactory {
            initializer { GroupPickerViewModel(appContainer.profileRepository) }
        }
    }
}
