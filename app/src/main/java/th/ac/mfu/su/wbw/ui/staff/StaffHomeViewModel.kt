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
import th.ac.mfu.su.wbw.data.remote.dto.SosOutcome
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

    /**
     * Report what was found at a case.
     *
     * Nothing is applied optimistically. The four outcomes do two different things —
     * `false_alarm` and `minor` close the case, `major` and `urgent` raise it and leave it
     * open — and which is which is the server's rule, not this app's. Guessing here would
     * mean a card that vanishes locally and reappears on the next poll, or the reverse.
     * The poll is a second away and it tells the truth.
     */
    fun report(id: Long, outcome: SosOutcome) {
        viewModelScope.launch {
            when (staff.report(id, outcome)) {
                // Ask for the change now rather than waiting out the current long-poll:
                // the caller has just told the server something and expects the list to
                // agree with them.
                is ApiResult.Success -> refreshNow()
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
     * Close a case: it was real, and it is over.
     *
     * The counterpart to [report]'s major and urgent, which keep a case open on purpose.
     * Like report, nothing is applied locally — the feed is asked again and it answers
     * with the closed row, which the server keeps visible for another half hour so the
     * card changes rather than vanishing.
     */
    fun resolve(id: Long) {
        viewModelScope.launch {
            when (staff.resolve(id)) {
                is ApiResult.Success -> refreshNow()
                is ApiResult.Error -> Unit
            }
        }
    }

    /**
     * One immediate, non-holding pass of the feed.
     *
     * `wait = 0` so it returns whatever is true right now instead of parking for
     * twenty-five seconds. It runs alongside the long-poll rather than interrupting it —
     * both fold through [merge], which is keyed by case id, so the worst a race can do is
     * apply the same row twice.
     *
     * A resolved case comes back in the same feed (the server keeps recently-closed rows
     * visible for half an hour), so closing one updates the card in place rather than
     * needing it removed here.
     */
    private fun refreshNow() {
        viewModelScope.launch {
            when (val r = staff.sosFeed(since, 0)) {
                is ApiResult.Success -> merge(r.data)
                is ApiResult.Error -> Unit
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
