package th.ac.mfu.su.wbw.ui.feedback

import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.CheckinProgress
import th.ac.mfu.su.wbw.data.remote.dto.CheckinProgressItem
import th.ac.mfu.su.wbw.data.repository.ProgressRepository
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.map.SosButton
import th.ac.mfu.su.wbw.ui.map.SosFullScreen
import th.ac.mfu.su.wbw.ui.map.SosViewModel

/**
 * What the gate is currently standing in the way for, if anything.
 *
 * [pending] is the first base checked in at and not yet answered for. One at a time: a
 * participant who was scanned at three bases while their phone was in a pocket meets three
 * forms in a row rather than one form with three headings, because each is about a
 * different place and the answers are not interchangeable.
 */
data class FeedbackGateUiState(
    val pending: CheckinProgressItem? = null,
    val eventDue: Boolean = false,
)

/**
 * Watches the progress feed for a check-in nobody has said anything about yet.
 *
 * It polls rather than waiting to be told. The check-in happens on a staff member's phone —
 * this device is not part of that transaction and finds out only by asking, so "the form
 * appears after you are scanned" is exactly as fast as the poll and no faster. Twenty
 * seconds is the compromise: the participant is standing at a base with a staff member in
 * front of them, so a minute of nothing would read as the scan having failed, and anything
 * much under this is a request per participant per few seconds across the whole event.
 */
class FeedbackGateViewModel(
    private val progress: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackGateUiState())
    val state: StateFlow<FeedbackGateUiState> = _state.asStateFlow()

    /**
     * The end-of-route form was given up on, this run of the app only.
     *
     * Deliberately not remembered anywhere. It is the escape from a send that failed, not
     * a record that the question was answered — the server holds that. Somebody who gives
     * up because the network was down meets the form again next launch, which is right:
     * their opinion still has not been recorded, and by then the network may be back.
     */
    private var dismissedEvent = false

    init {
        // The cache first, so a gate that is already owed a form does not wait a round trip
        // to put it up — the participant was scanned before the app was opened, and the
        // form is the first thing they should see.
        progress.cached()?.let { apply(it) }
        viewModelScope.launch {
            while (isActive) {
                if (progress.progress() is ApiResult.Success) progress.cached()?.let { apply(it) }
                delay(PollMillis)
            }
        }
    }

    private fun apply(p: CheckinProgress) {
        val pending = p.checkedIn.firstOrNull { !it.answered }
        _state.update {
            it.copy(
                pending = pending,
                // Only once every base is behind them, and only when there was a route to
                // finish: `complete` is false for a zero total, which is what an event with
                // no checkpoints configured looks like.
                eventDue = p.complete && pending == null && !p.eventFeedbackAnswered && !dismissedEvent,
            )
        }
    }

    /**
     * Re-read the feed now, without waiting for the next poll.
     *
     * Called the moment a form reports itself sent. Submitting already refreshed progress
     * and rewrote the cache, so this is a read rather than a request — but without it the
     * gate would keep a finished form on screen for the rest of the poll interval, which
     * on a twenty-second cycle is a participant staring at a form they have just answered
     * with no bar, no back and nothing to press.
     */
    fun refreshNow() {
        progress.cached()?.let { apply(it) }
    }

    /**
     * Called when the event form is done with — sent, or given up on after a failed send.
     *
     * A send refreshed the feed on its way through, so [refreshNow] is what actually closes
     * the gate; [dismissedEvent] only covers the give-up path, where the server was never
     * told anything and the feed will keep saying the form is due.
     */
    fun markEventDone() {
        dismissedEvent = true
        refreshNow()
    }

    companion object {
        private const val PollMillis = 20_000L

        val Factory = viewModelFactory {
            initializer { FeedbackGateViewModel(appContainer.progressRepository) }
        }
    }
}

/**
 * The feedback that has to be answered before the app is usable again.
 *
 * A gate rather than a prompt on Home, because a prompt is a request and this is a
 * condition: the organisers get one chance to hear what a base was like, which is while the
 * participant is still standing in it. A card on Home competing with a bloom and a weather
 * line is a card that gets scrolled past, and by the evening the answer is a memory of a
 * memory.
 *
 * **It takes the bottom bar and the back gesture.** It replaces the scaffold rather than
 * covering it, so there are no tabs underneath to reach, and [BackHandler] swallows the
 * system gesture so the form cannot be dismissed by the one control that is always there.
 *
 * **Except the SOS.** The emergency button lives on the map, and the map is behind this
 * gate — so without its own copy, a gate put up the moment somebody is scanned in at a base
 * would stand between an injured participant and the only way this app has of saying so.
 * The whole point of blocking is that the form cannot be walked past; the emergency is not
 * a way of walking past it, and it is worth the one exception.
 */
@Composable
fun FeedbackGate(content: @Composable () -> Unit) {
    val viewModel: FeedbackGateViewModel = viewModel(factory = FeedbackGateViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pending = state.pending
    when {
        pending != null -> Gated {
            FeedbackScreen(
                checkpointId = pending.checkpointId,
                checkpointName = pending.name,
                contentPadding = it,
                // No navigation on the way out — the gate closes by the feed saying this
                // base is answered. But it has to be told to look: submitting rewrote the
                // cache, and without this the gate waits out the rest of its poll before
                // noticing, holding a finished form on screen with nothing to press.
                onDone = { viewModel.refreshNow() },
                blocking = true,
                // Keyed by the base. `viewModel()` without one hands back the same
                // instance for every checkpoint, so answering base one and being scanned
                // at base two would reopen the first form's ratings, its comment and its
                // client id — and post all of it against the wrong checkpoint.
                viewModel = viewModel(
                    key = "feedback-${pending.checkpointId}",
                    factory = FeedbackViewModel.factoryFor(pending.checkpointId),
                ),
            )
        }

        state.eventDue -> Gated {
            FeedbackScreen(
                // Not about any one base. The screen takes an id because the base form
                // needs one; the event form's view model ignores it.
                checkpointId = 0,
                checkpointName = stringResource(R.string.feedback_event_name),
                contentPadding = it,
                onDone = { viewModel.markEventDone() },
                blocking = true,
                // The one escape in the whole gate, and it is not a softening of the
                // blocking rule — it is there because `POST /wbw/me/event-feedback` does
                // not exist. Without it, finishing the route would put up a form that can
                // never be sent, with no bar, no back and no way out, and the app would be
                // over for that participant. It appears only after a send has actually
                // failed, so nobody sees it who is not already stuck.
                onGiveUp = { viewModel.markEventDone() },
                viewModel = viewModel(key = "feedback-event", factory = FeedbackViewModel.eventFactory()),
            )
        }

        else -> content()
    }
}

/**
 * The blocking frame: the background, no bar, no back, and an SOS in the corner.
 *
 * **It draws [ForestBackground] itself.** The wallpaper belongs to the scaffold, and this
 * gate stands in the scaffold's place rather than on top of it — so without its own the
 * form renders on bare white, which is not a theme this app has: every pane on it is glass
 * that refracts the wallpaper, and with nothing behind them the panes and their text go
 * to nearly the same colour.
 *
 * The padding it hands down clears the SOS as well as the navigation bar. The scaffold's
 * own `contentPadding` is no use here — it clears a floating tab bar that is not on screen
 * while this is — but the send button still has something to sit above, because the SOS
 * floats over the form's last inches.
 */
@Composable
private fun Gated(form: @Composable (PaddingValues) -> Unit) {
    val context = LocalContext.current
    val sosViewModel: SosViewModel = viewModel(factory = SosViewModel.Factory)
    val sos by sosViewModel.state.collectAsStateWithLifecycle()

    // Swallowed rather than handled. The participant is not being sent anywhere by pressing
    // back, and popping the host activity to the launcher would be a way out of the gate
    // that leaves the form unanswered.
    BackHandler(enabled = true) {}

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // The SOS gets a band of its own rather than floating over the form.
    //
    // Floating, it collided with the send button — the two ended up side by side on the
    // same row, on a screen where pressing the wrong one either raises a false emergency or
    // fails to raise a real one. Padding the form's bottom does not fix that: the form
    // scrolls, so a floating control lands on whatever happens to be under it at the time,
    // and the send button passes under it on the way. A row the form cannot scroll into
    // is the only arrangement where that cannot happen.
    ForestBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    form(PaddingValues(bottom = 12.dp))
                }

                // Hidden while a case is open, exactly as on the map: [SosFullScreen] takes
                // the screen then, and a second button could not raise a second case.
                if (!sos.active) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 20.dp, top = 4.dp, bottom = navInset + 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        SosButton(onFire = { sosViewModel.raise(context) })
                    }
                }
            }

            val open = sos.case
            if (sos.active && open != null) {
                SosFullScreen(
                    case = open,
                    me = sos.me,
                    cancelRefused = sos.cancelRefused,
                    onCancel = { sosViewModel.cancel() },
                    contentPadding = PaddingValues(bottom = navInset + 24.dp),
                )
            }
        }
    }
}

