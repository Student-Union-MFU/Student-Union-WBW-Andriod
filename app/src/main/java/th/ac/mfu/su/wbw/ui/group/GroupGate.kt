package th.ac.mfu.su.wbw.ui.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.home.HomeScaffold

/**
 * Whether this participant still has to choose a group.
 *
 * Three states rather than a boolean, and the third is the important one. "We do not know
 * yet" has to be distinguishable from "no group", because they demand opposite behaviour:
 * unknown must let the participant through to the app, missing must stop them at the picker.
 */
private enum class GroupStatus { Unknown, Missing, Present }

private data class GateState(val status: GroupStatus = GroupStatus.Unknown)

private class GroupGateViewModel(
    private val profile: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GateState(statusOf(profile)))
    val state: StateFlow<GateState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = profile.me()) {
                is ApiResult.Success -> _state.value = GateState(
                    if (result.data.groupId == null) GroupStatus.Missing else GroupStatus.Present,
                )
                // Deliberately not an error state.
                //
                // A failed `/me` means the network is down, not that the participant has no
                // group, and the two must not be confused: half of this event happens on a
                // hill with one bar of signal, and a fetch that fails there would otherwise
                // shove somebody who has been in group 12 all week into a screen demanding
                // they pick one. Unknown falls through to the app, where the cached pass and
                // map still work.
                is ApiResult.Error -> Unit
            }
        }
    }

    companion object {
        /** Seeded from the cache so a returning participant never sees the gate flicker. */
        private fun statusOf(profile: ProfileRepository): GroupStatus {
            val me = profile.cachedMe() ?: return GroupStatus.Unknown
            return if (me.groupId == null) GroupStatus.Missing else GroupStatus.Present
        }

        val Factory = viewModelFactory {
            initializer { GroupGateViewModel(appContainer.profileRepository) }
        }
    }
}

/**
 * The group choice, in front of the app, exactly once.
 *
 * Participants get their accounts from the website, so the first time one signs in here they
 * have no group yet — and a group is not optional decoration: it is the chat channel, the
 * roster a marshal reads, and the thing the whole walk is organised around. So it is asked
 * for on the way in rather than hidden in a settings screen nobody opens.
 *
 * It is a gate and not a step in a wizard, which means it is re-entrant by construction:
 * anybody who reaches the app without a group — an account created some other way, a group
 * deleted by an admin — meets the picker on their next launch rather than finding a chat tab
 * that silently does nothing.
 */
@Composable
fun GroupGate(session: Session, onLogout: () -> Unit) {
    val viewModel: GroupGateViewModel = viewModel(factory = GroupGateViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.status == GroupStatus.Missing) {
        GroupPickerScreen(onJoined = { viewModel.refresh() })
    } else {
        HomeScaffold(session = session, onLogout = onLogout)
    }
}
