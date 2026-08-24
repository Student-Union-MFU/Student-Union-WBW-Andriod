package th.ac.mfu.su.wbw.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.CheckinResult
import th.ac.mfu.su.wbw.data.remote.dto.StaffCheckpoint
import th.ac.mfu.su.wbw.data.repository.StaffRepository
import th.ac.mfu.su.wbw.ui.appContainer

data class StaffScanUiState(
    val checkpoints: List<StaffCheckpoint> = emptyList(),
    /** The checkpoint being stamped at. Null only before the list has arrived. */
    val selected: StaffCheckpoint? = null,
    val loadingCheckpoints: Boolean = true,
    /** Set when the checkpoint list could not be fetched — scanning is impossible until it is. */
    val checkpointError: String? = null,

    /** True while a stamp is in flight. The scanner stops reading for its duration. */
    val submitting: Boolean = false,
    /** The last stamp, held until it is replaced or times out. */
    val result: CheckinResult? = null,
    /** Set when a scan was read but the server refused it. */
    val error: String? = null,
) {
    /**
     * Whether the camera should be reading right now.
     *
     * False while a stamp is in flight or its result is on screen: the frame stream does
     * not stop, but a decoded code is dropped rather than acted on. Without this, a pass
     * left in front of the lens for two seconds is stamped a dozen times, and the staff
     * member watches the result card flicker between a dozen identical answers.
     */
    val reading: Boolean get() = !submitting && result == null && error == null && selected != null
}

/**
 * The scanner's state, which is mostly the state of *not* scanning.
 *
 * Three things had to be decided here rather than in the screen, because each of them is
 * about what happens between scans:
 *
 *  - **A code is acted on once.** [lastCode] and the [reading] gate together mean a pass
 *    held steadily in front of the camera produces one stamp, not one per frame.
 *  - **A result clears itself.** The card sits for [ResultHoldMillis] and then goes,
 *    returning the screen to the camera. A staff member with a queue in front of them
 *    should not have to dismiss anything to scan the next person, and the one thing worse
 *    than no confirmation is a stale confirmation still showing when the next pass is
 *    presented.
 *  - **The checkpoint is sticky.** It is chosen once and survives every scan after it,
 *    because a person at a checkpoint is at that checkpoint all afternoon.
 */
class StaffScanViewModel(private val staff: StaffRepository) : ViewModel() {

    private val _state = MutableStateFlow(StaffScanUiState())
    val state: StateFlow<StaffScanUiState> = _state.asStateFlow()

    /**
     * The last code acted on, so the same pass sitting in frame is not stamped repeatedly.
     *
     * Cleared when the result card clears, which is what lets the *same* person be scanned
     * again deliberately a minute later — a staff member re-checking somebody is a real
     * thing to want, and the server answers it with `already_checked_in`.
     */
    private var lastCode: String? = null
    private var clearJob: Job? = null

    fun loadCheckpoints() {
        if (_state.value.checkpoints.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loadingCheckpoints = true, checkpointError = null) }
            when (val r = staff.checkpoints()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        checkpoints = r.data,
                        // First by default, and only if nothing is chosen yet — reloading
                        // must not move a staff member off the checkpoint they picked.
                        selected = it.selected ?: r.data.firstOrNull(),
                        loadingCheckpoints = false,
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(loadingCheckpoints = false, checkpointError = r.message)
                }
            }
        }
    }

    fun select(checkpoint: StaffCheckpoint) {
        _state.update { it.copy(selected = checkpoint) }
    }

    /**
     * A code came off the camera.
     *
     * Everything that decides whether to act on it lives here rather than in the analyzer,
     * so the analyzer stays a decoder and this stays the only place that knows what a
     * duplicate is.
     */
    fun onScanned(code: String) {
        val s = _state.value
        if (!s.reading || code == lastCode) return
        val checkpoint = s.selected ?: return
        lastCode = code
        submit(checkpoint.id, qrToken = code, bib = null)
    }

    /** The manual fallback, for a pass that will not scan. */
    fun onBibEntered(bib: Int) {
        val checkpoint = _state.value.selected ?: return
        if (_state.value.submitting) return
        submit(checkpoint.id, qrToken = null, bib = bib)
    }

    private fun submit(checkpointId: Int, qrToken: String?, bib: Int?) {
        clearJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, result = null, error = null) }
            when (val r = staff.checkin(checkpointId, qrToken = qrToken, bib = bib)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, result = r.data) }
                is ApiResult.Error -> _state.update { it.copy(submitting = false, error = r.message) }
            }
            scheduleClear()
        }
    }

    /** Dismiss the current result now, rather than waiting it out. */
    fun clearResult() {
        clearJob?.cancel()
        lastCode = null
        _state.update { it.copy(result = null, error = null) }
    }

    private fun scheduleClear() {
        clearJob?.cancel()
        clearJob = viewModelScope.launch {
            // An error sits longer than a success. A green card is confirmation of something
            // that already worked and can go as soon as it is read; a red one is a job still
            // to be done, and taking it away before it is understood loses the only notice
            // that this person is not counted.
            delay(if (_state.value.error != null) ErrorHoldMillis else ResultHoldMillis)
            clearResult()
        }
    }

    companion object {
        private const val ResultHoldMillis = 2_600L
        private const val ErrorHoldMillis = 4_500L

        val Factory = viewModelFactory {
            initializer { StaffScanViewModel(appContainer.staffRepository) }
        }
    }
}
