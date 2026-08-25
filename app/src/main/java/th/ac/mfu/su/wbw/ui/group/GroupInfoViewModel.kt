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
import th.ac.mfu.su.wbw.data.remote.dto.GroupMember
import th.ac.mfu.su.wbw.data.repository.ChatRepository
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.ui.appContainer

data class GroupInfoUiState(
    val members: List<GroupMember> = emptyList(),
    val count: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,

    /** This device's own user id, so the roster can mark which row is you. */
    val meId: String? = null,
    /**
     * How many more times this participant may leave a group.
     *
     * Read from `/me` rather than assumed to be one: an admin can grant another, and a
     * participant who has already moved once has none. The button says which of those is
     * true instead of finding out by being pressed.
     */
    val leaveQuota: Int = 0,
    val leaving: Boolean = false,
    /** Set when a leave was refused — the server's own words. */
    val leaveError: String? = null,
    /** True once the leave has gone through; the screen closes itself on it. */
    val left: Boolean = false,
)

/**
 * One group, as a page: who is in it, and the way out of it.
 *
 * The roster comes from `GET /groups/{id}/members`, which is not gated on membership — so
 * this screen works unchanged for a staff account looking at a group it does not belong to.
 * What differs is only whether leaving is offered, and that is the caller's call rather
 * than something guessed at here.
 */
class GroupInfoViewModel(
    private val chat: ChatRepository,
    private val profile: ProfileRepository,
    private val groupId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupInfoUiState())
    val state: StateFlow<GroupInfoUiState> = _state.asStateFlow()

    init {
        // Seeded from the cached profile so the quota and "you" marker are right on the
        // first frame rather than a round trip later.
        profile.cachedMe()?.let { me ->
            _state.update { it.copy(meId = me.id, leaveQuota = me.leaveQuota) }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = chat.members(groupId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(members = r.data.members, count = r.data.count, loading = false)
                }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
            }
        }
        viewModelScope.launch {
            // The quota again, from the network this time. Cheap, and the cached copy can
            // be a day old — long enough for an admin to have granted or spent one.
            profile.me().let { r ->
                if (r is ApiResult.Success) {
                    _state.update { it.copy(meId = r.data.id, leaveQuota = r.data.leaveQuota) }
                }
            }
        }
    }

    /**
     * Leave, and let the gate take over.
     *
     * Nothing is done locally beyond flagging [GroupInfoUiState.left]: the repository
     * refreshes `/me` and ticks its membership signal, and
     * [th.ac.mfu.su.wbw.ui.group.GroupGate] is what actually puts the picker on screen.
     * Navigating from here as well would race the gate for control of the same decision.
     */
    fun leave() {
        if (_state.value.leaving) return
        _state.update { it.copy(leaving = true, leaveError = null) }
        viewModelScope.launch {
            when (val r = profile.leaveGroup()) {
                is ApiResult.Success -> _state.update { it.copy(leaving = false, left = true) }
                // The server's message, verbatim. It knows the difference between "you have
                // no group" and "your one move is spent", and either is worth reading.
                is ApiResult.Error -> _state.update { it.copy(leaving = false, leaveError = r.message) }
            }
        }
    }

    companion object {
        fun factoryFor(groupId: Int) = viewModelFactory {
            initializer {
                GroupInfoViewModel(
                    appContainer.chatRepository,
                    appContainer.profileRepository,
                    groupId,
                )
            }
        }
    }
}
