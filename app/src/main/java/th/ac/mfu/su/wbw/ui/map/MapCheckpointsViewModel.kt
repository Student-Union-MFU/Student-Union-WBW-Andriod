package th.ac.mfu.su.wbw.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    /**
     * Keep the bases fresh while the map is on screen.
     *
     * The pins themselves barely change — an admin moving a base mid-event is rare. What
     * changes every few minutes is `checkin_count`, which is counted live from `check_in`
     * on the server and is the one number on this screen that belongs to everybody rather
     * than to the person holding the phone. Without a poll it would freeze at whatever it
     * was when the map was first opened and quietly stay wrong all day.
     *
     * Driven from the screen, like [th.ac.mfu.su.wbw.ui.home.HomeViewModel.watchProgress],
     * so nothing polls while nobody is looking.
     *
     * Thirty seconds, against Home's sixty. A check-in is somebody walking up to a table
     * and being scanned, so this is minute-scale either way — but the count here is read
     * deliberately, by somebody who tapped a pin to ask, and a number that answers a
     * question should not be a minute stale. It is nine rows either way.
     *
     * A failed poll leaves the last list alone, for the same reason the first fetch does.
     */
    suspend fun watchCheckpoints() {
        while (currentCoroutineContext().isActive) {
            repo.checkpoints().onSuccess { _checkpoints.value = it }
            delay(CheckpointPollMillis)
        }
    }

    companion object {
        private const val CheckpointPollMillis = 30_000L

        val Factory = viewModelFactory {
            initializer { MapCheckpointsViewModel(appContainer.checkpointRepository) }
        }
    }
}
