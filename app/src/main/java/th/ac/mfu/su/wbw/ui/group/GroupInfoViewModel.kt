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
    /**
     * The pull gesture's own fetch, as distinct from [loading].
     *
     * [loading] empties the screen for a spinner; this one leaves the roster exactly where
     * it is and turns the indicator over the top of it. A refresh is asked for by somebody
     * already reading the list.
     */
    val refreshing: Boolean = false,
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
    /**
     * Whether [leaveQuota] has actually been answered, as opposed to still being its
     * default.
     *
     * This exists because of a cache older than the field. `leave_quota` was added to the
     * decoder after devices had already written profiles without it, so those entries
     * decode to the default 0 — and 0 is the value that means "you have already moved and
     * cannot again". A participant with their one move still unspent would have been told
     * they had used it, on the strength of a JSON key that was simply absent.
     *
     * So the screen waits. Nothing about leaving is offered until a read has confirmed the
     * number, which takes one request; and a failed request leaves the question open
     * rather than answering it wrongly in the harsher direction.
     */
    val quotaKnown: Boolean = false,
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
        // The id is safe to take from cache — it marks your own row in the roster and is
        // wrong only if the session changed. The quota is not; see [quotaKnown].
        profile.cachedMe()?.let { me -> _state.update { it.copy(meId = me.id) } }
        load()
    }

    /** The same two calls as [load], without taking the roster off the screen for them. */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            when (val r = chat.members(groupId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(members = r.data.members, count = r.data.count, error = null)
                }
                // Kept quiet, unlike [load]'s. The list on screen is still the group.
                is ApiResult.Error -> Unit
            }
            (profile.me() as? ApiResult.Success)?.let { r ->
                _state.update {
                    it.copy(meId = r.data.id, leaveQuota = r.data.leaveQuota, quotaKnown = true)
                }
            }
            _state.update { it.copy(refreshing = false) }
        }
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
                    _state.update {
                        it.copy(meId = r.data.id, leaveQuota = r.data.leaveQuota, quotaKnown = true)
                    }
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
