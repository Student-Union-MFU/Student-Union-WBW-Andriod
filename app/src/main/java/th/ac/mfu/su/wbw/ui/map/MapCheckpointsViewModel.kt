package th.ac.mfu.su.wbw.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantCheckpoint
import th.ac.mfu.su.wbw.data.repository.CheckpointRepository
import th.ac.mfu.su.wbw.ui.appContainer

/**
 * The bases the map draws.
 *
 * A view model rather than a repository call in the composable, because [appContainer] is
 * reachable only from a factory's `CreationExtras` — and because the pins should survive
 * the map being recomposed, which it is on every camera movement.
 */
class MapCheckpointsViewModel(private val repo: CheckpointRepository) : ViewModel() {

    private val _checkpoints = MutableStateFlow(repo.cached().orEmpty())
    val checkpoints: StateFlow<List<ParticipantCheckpoint>> = _checkpoints.asStateFlow()

    init {
        refresh()
    }

    /**
     * Fetch once. A failure keeps whatever the cache held: a route drawn with yesterday's
     * bases is a useful map, and one drawn with none is the map failing at the only
     * question it is asked all day.
     */
    fun refresh() {
        viewModelScope.launch {
            repo.checkpoints().onSuccess { _checkpoints.value = it }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MapCheckpointsViewModel(appContainer.checkpointRepository) }
        }
    }
}
