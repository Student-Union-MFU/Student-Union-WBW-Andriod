package th.ac.mfu.su.wbw.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.SosStaffCase
import th.ac.mfu.su.wbw.data.repository.StaffRepository
import th.ac.mfu.su.wbw.ui.appContainer

data class StaffUiState(
    val cases: List<SosStaffCase> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    /** Open cases nobody has claimed — the number the header counts. */
    val waiting: Int get() = cases.count { !it.resolved && !it.acknowledged }
    val open: List<SosStaffCase> get() = cases.filterNot { it.resolved }
    val recentlyClosed: List<SosStaffCase> get() = cases.filter { it.resolved }
}

/**
 * The staff console's state: every emergency this person is allowed to see.
 *
 * The feed is **merged**, not replaced. `GET /wbw/staff/sos?since=` is incremental — it
 * returns only what has changed since the cursor — so treating each round as the whole
 * truth would empty the screen the moment a quiet poll came back with nothing. What arrives
 * is folded in by id, newest state winning.
 *
 * As everywhere else in this app the loop is driven by the screen rather than started here,
 * so a held connection lives exactly as long as somebody is looking at it.
 */
class StaffHomeViewModel(private val staff: StaffRepository) : ViewModel() {

    private val _state = MutableStateFlow(StaffUiState())
    val state: StateFlow<StaffUiState> = _state.asStateFlow()

    /** Newest row seen, echoed to the server verbatim. Blank asks for everything. */
    private var since: String = ""

    fun ack(id: Long) {
        viewModelScope.launch {
            // A failure is deliberately quiet. Ack is not the response — walking there is —
            // and the case stays on screen either way; the next poll will show the truth.
            when (val result = staff.ack(id)) {
                is ApiResult.Success -> merge(listOf(result.data))
                is ApiResult.Error -> Unit
            }
        }
    }

    /** The long-poll loop. Runs until the screen goes away. */
    suspend fun watch() {
        var wait = 0
        var backoff = InitialBackoffMillis
        while (currentCoroutineContext().isActive) {
            when (val result = staff.sosFeed(since, wait)) {
                is ApiResult.Success -> {
                    merge(result.data)
                    _state.update { it.copy(loading = false, error = null) }
                    backoff = InitialBackoffMillis
                    wait = HoldSeconds
                }
                is ApiResult.Error -> {
                    // The list is left alone. A failed poll says nothing about whether
                    // anybody needs help, and clearing a console of live emergencies
                    // because one request timed out is the worst thing this screen could do.
                    _state.update { it.copy(loading = false, error = result.message) }
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxBackoffMillis)
                }
            }
        }
    }

    /**
     * Folds an incremental page into what is already held.
     *
     * Sorted unacknowledged-first, then newest — the ordering is the triage. A case nobody
     * has claimed outranks one somebody is already walking to, however long ago it came in.
     */
    private fun merge(incoming: List<SosStaffCase>) {
        if (incoming.isEmpty()) return
        _state.update { current ->
            val byId = current.cases.associateBy { it.id }.toMutableMap()
            incoming.forEach { byId[it.id] = it }
            val ordered = byId.values.sortedWith(
                compareBy<SosStaffCase> { it.resolved }
                    .thenBy { it.acknowledged }
                    .thenByDescending { it.id },
            )
            current.copy(cases = ordered)
        }
        incoming.maxByOrNull { it.updatedAt }?.let { since = it.cursor }
    }

    companion object {
        private const val HoldSeconds = 25
        private const val InitialBackoffMillis = 1_000L
        private const val MaxBackoffMillis = 30_000L

        val Factory = viewModelFactory {
            initializer { StaffHomeViewModel(appContainer.staffRepository) }
        }
    }
}
