package th.ac.mfu.su.wbw.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.data.remote.dto.SosCase
import th.ac.mfu.su.wbw.data.remote.dto.SosRequest
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.data.repository.SosRepository
import th.ac.mfu.su.wbw.ui.appContainer
import java.time.OffsetDateTime
import java.util.UUID

data class SosUiState(
    /** The open case, or null when there is no emergency. */
    val case: SosCase? = null,
    /** True from the moment the hold completes until the server has answered. */
    val sending: Boolean = false,
    /** Set when a cancel was refused — someone is already coming, so phone instead. */
    val cancelRefused: String? = null,
    /**
     * The participant's own profile, for the card a staff member reads off the screen.
     *
     * Seeded from cache and never blocking: on a hill with no signal the cached blood type
     * and next-of-kin are still true, and they are the whole reason the emergency screen is
     * worth putting a person's own details on.
     */
    val me: ParticipantDetail? = null,
    val error: String? = null,
) {
    val active: Boolean get() = case != null && !case.resolved
}

/**
 * The emergency, as a piece of state that outlives the button.
 *
 * Scoped to the map's navigation entry rather than to the button, because an emergency is
 * not a screen interaction — it survives switching tabs, and the participant must be able
 * to walk to the chat tab to tell their group and come back to a map that still says help
 * is coming.
 *
 * As with [th.ac.mfu.su.wbw.ui.chat.ChatViewModel], the polling loop is **not** started
 * here: [watch] is a suspending function the screen drives from its own effect, so a held
 * connection lives exactly as long as somebody is looking at it. The difference is what
 * happens when nobody is: the case does not stop existing, it just stops being watched, and
 * the next look re-reads it from the server.
 */
class SosViewModel(
    private val sos: SosRepository,
    private val profile: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SosUiState(me = profile.cachedMe()))
    val state: StateFlow<SosUiState> = _state.asStateFlow()

    init {
        // Refreshed once in the background so a profile edited on the website reaches the
        // emergency card, but the cached copy above is what the first frame draws — this
        // screen must never wait on the network to be able to show somebody's blood type.
        viewModelScope.launch {
            val result = profile.me()
            if (result is ApiResult.Success) _state.update { it.copy(me = result.data) }
        }
    }

    /**
     * The idempotency key for the emergency currently being raised.
     *
     * Held across the whole life of one case rather than generated per request, because
     * that is what makes the second call — the one carrying the GPS fix that arrived a few
     * seconds late — land on the same row instead of opening a second emergency.
     */
    private var clientId: String? = null

    /**
     * Raise the emergency now, with whatever is known now.
     *
     * Deliberately does not wait for a position. The server accepts a case with no
     * coordinates and falls back to the participant's last checkpoint, and a case that
     * reached a staff member without a fix beats a fix that reached nobody: on this trail the
     * moments when GPS takes longest are exactly the moments somebody is most likely to
     * need this. [attachPosition] follows up once the fix lands.
     */
    fun raise(context: Context, forOther: Boolean = false) {
        if (_state.value.sending) return
        val id = clientId ?: UUID.randomUUID().toString().also { clientId = it }
        _state.update { it.copy(sending = true, error = null, cancelRefused = null) }

        viewModelScope.launch {
            val result = sos.raise(
                SosRequest(
                    clientId = id,
                    deviceTime = runCatching { OffsetDateTime.now().toString() }.getOrNull(),
                    forOther = forOther,
                ),
            )
            when (result) {
                is ApiResult.Success ->
                    _state.update { it.copy(case = result.data, sending = false) }
                is ApiResult.Error ->
                    _state.update { it.copy(sending = false, error = result.message) }
            }
        }
        attachPosition(context, id, forOther)
    }

    /**
     * Send the position as a second call once the phone has one.
     *
     * Same `client_id`, so the server treats it as the case being updated rather than as a
     * new emergency. A failure here is silent on purpose: the case is already open and a
     * staff member is already looking at it, and an error toast about coordinates would be noise
     * on top of an emergency the participant can do nothing about.
     */
    @SuppressLint("MissingPermission")
    private fun attachPosition(context: Context, id: String, forOther: Boolean) {
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { loc ->
                if (loc == null) return@addOnSuccessListener
                viewModelScope.launch {
                    val result = sos.raise(
                        SosRequest(
                            clientId = id,
                            deviceTime = runCatching { OffsetDateTime.now().toString() }.getOrNull(),
                            forOther = forOther,
                            lat = loc.latitude,
                            lng = loc.longitude,
                            accuracyM = loc.accuracy.toDouble(),
                        ),
                    )
                    if (result is ApiResult.Success) {
                        _state.update { it.copy(case = result.data) }
                    }
                }
            }
    }

    /**
     * Stand down.
     *
     * A 409 is not an error to be retried — it means a staff member has already acknowledged, or
     * the two-minute window has closed. Either way somebody is moving, so the answer is to
     * tell the participant to phone rather than to leave them believing they un-sent it.
     */
    fun cancel() {
        val id = _state.value.case?.id ?: return
        viewModelScope.launch {
            when (val result = sos.cancel(id)) {
                is ApiResult.Success -> {
                    clientId = null
                    _state.update { it.copy(case = null, cancelRefused = null, error = null) }
                }
                is ApiResult.Error ->
                    if (result.code == 409) {
                        _state.update { it.copy(cancelRefused = result.message) }
                    } else {
                        _state.update { it.copy(error = result.message) }
                    }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null, cancelRefused = null) }

    /**
     * The long-poll loop. Runs until cancelled — i.e. until the map goes away.
     *
     * The first pass asks for no hold so the screen knows immediately whether there is an
     * emergency in progress; every pass after uses the full 25 seconds, which is what turns
     * "a staff member acknowledged your case" into something that appears on its own rather than
     * on the next time somebody thinks to reopen the app.
     *
     * It keeps polling while there is no case, too, because a case can be raised from
     * another device signed into the same account.
     */
    suspend fun watch() {
        var wait = 0
        var backoff = InitialBackoffMillis
        while (currentCoroutineContext().isActive) {
            when (val result = sos.active(wait)) {
                is ApiResult.Success -> {
                    val case = result.data
                    // A resolved case is a closed one. Clearing the client id here is what
                    // lets the next emergency be a genuinely new case rather than an update
                    // to a finished one.
                    if (case == null || case.resolved) clientId = null
                    _state.update { it.copy(case = case?.takeIf { c -> !c.resolved }, error = null) }
                    backoff = InitialBackoffMillis
                    wait = HoldSeconds
                }
                is ApiResult.Error -> {
                    // The case itself is left alone. A failed poll says nothing about
                    // whether help is coming, and blanking a live emergency because one
                    // request timed out on a hill is the worst thing this screen could do.
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxBackoffMillis)
                }
            }
        }
    }

    companion object {
        /** Matches the server's clamp — asking for more just gets clamped down. */
        private const val HoldSeconds = 25
        private const val InitialBackoffMillis = 1_000L
        private const val MaxBackoffMillis = 30_000L

        val Factory = viewModelFactory {
            initializer { SosViewModel(appContainer.sosRepository, appContainer.profileRepository) }
        }
    }
}
