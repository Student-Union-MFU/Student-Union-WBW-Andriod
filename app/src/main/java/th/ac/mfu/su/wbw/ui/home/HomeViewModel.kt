package th.ac.mfu.su.wbw.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onError
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.data.repository.ConditionsRepository
import th.ac.mfu.su.wbw.data.repository.NotificationRepository
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.data.repository.TrailConditions
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.common.UiState

/**
 * Home dashboard state. Loads the participant profile for the greeting and pass
 * details. Base-camp / tree progress is a visual placeholder for now — the Go
 * backend has no bases/check-in-count endpoint yet (see [HomeUiModel.bases]).
 */
class HomeViewModel(
    private val repository: ProfileRepository,
    private val notifications: NotificationRepository,
    private val conditions: ConditionsRepository,
    settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeUiModel>>(UiState.Loading)
    val state = _state.asStateFlow()

    /**
     * Trail weather and air quality, or null while unknown.
     *
     * Not a [UiState]: there is no loading spinner and no error message for this. It is a
     * third party's data on the app's own home screen, so the only two states worth
     * modelling are "we have it" and "we do not", and the second one draws nothing. See
     * [ConditionsRepository] for why the failures never reach here as errors.
     */
    private val _trailConditions = MutableStateFlow<TrailConditions?>(null)
    val trailConditions: StateFlow<TrailConditions?> = _trailConditions.asStateFlow()

    /**
     * The newest announcement the server is offering, or 0 if it has not answered.
     *
     * Zero on failure rather than a retained previous value, so a flaky network shows no
     * mark instead of inventing one. A bell that lights up because a request timed out is
     * worse than a bell that stays quiet a little too long.
     */
    private val newestUnreadId = MutableStateFlow(0L)

    /**
     * Whether Home's bell shows its mark.
     *
     * Kept apart from [state] on purpose: the badge is a second, independent request, and
     * folding it into the profile's `UiState` would mean a failed announcements fetch
     * could blank the whole screen — or that the greeting had to wait for it.
     */
    val hasUnreadNotifications: StateFlow<Boolean> =
        combine(newestUnreadId, settings.lastSeenNotificationId) { newest, seen -> newest > seen }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // The first emission is last run's profile, if there is one, so Home opens on the
        // participant's own name instead of a spinner. Synchronous — see [ResponseCache].
        repository.cachedMe()?.let { _state.value = UiState.Success(HomeUiModel.from(it)) }
        load()
        _trailConditions.value = conditions.cached()
    }

    fun load() {
        // Only spin when there is genuinely nothing to show. Replacing a cached profile
        // with a spinner on every launch would give back exactly what the cache was for.
        if (_state.value !is UiState.Success) _state.value = UiState.Loading
        viewModelScope.launch {
            repository.me()
                .onSuccess { _state.value = UiState.Success(HomeUiModel.from(it)) }
                // A failed refresh must not take a working screen away. Somewhere up the
                // hill with no signal, the cached greeting and bloom are still true; an
                // error page in their place would be less use and less accurate.
                .onError { if (_state.value !is UiState.Success) _state.value = UiState.Error(it) }
        }
    }

    /**
     * Re-checks whether there is anything new to announce.
     *
     * Driven from Home entering composition rather than from [init], because this view
     * model outlives the screen — it is scoped to the back stack entry, so `init` runs
     * once and would never run again for the rest of the session. Since there is no push
     * delivery yet (the Go backend does not send FCM), a mark that is fetched once per
     * process is a mark that is wrong for most of the walk. Coming back to Home is the
     * cheapest honest moment to look again.
     */
    /**
     * Re-reads the trail conditions. Driven from Home entering composition, for the same
     * reason as [refreshNotificationMark] — and cheap to call often, because the
     * repository holds a ten-minute cache behind it and most calls never leave the phone.
     */
    fun refreshConditions() {
        viewModelScope.launch {
            conditions.trailConditions()?.let { _trailConditions.value = it }
        }
    }

    fun refreshNotificationMark() {
        // Seeded from the cache first so the bell is already right in the opening frame —
        // otherwise an unread announcement takes a round trip to appear, and the dot pops
        // in a second after the screen has settled.
        notifications.cachedMine()?.let { newestUnreadId.value = newestUnread(it) }
        viewModelScope.launch {
            notifications.mine()
                .onSuccess { newestUnreadId.value = newestUnread(it) }
                // Keep whatever the cache said. Clearing the mark here would hide a real
                // unread announcement because one poll happened to fail.
                .onError { }
        }
    }

    /**
     * Only rows the server has not already marked read can raise the mark; the local
     * seen-marker handles the rest (see [AppSettings.lastSeenNotificationId]).
     */
    private fun newestUnread(items: List<th.ac.mfu.su.wbw.data.remote.dto.Notification>): Long =
        items.filter { it.readAt == null }.maxOfOrNull { it.id } ?: 0L

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    appContainer.profileRepository,
                    appContainer.notificationRepository,
                    appContainer.conditionsRepository,
                    appContainer.appSettings,
                )
            }
        }
    }
}

data class HomeUiModel(
    val displayName: String,
    val checkedInBases: Int,
    val totalBases: Int,
    val nextBaseName: String,
    val nextBaseDistance: String,
) {
    val phase: GrowthPhase get() = GrowthPhase.forProgress(checkedInBases, totalBases)
    val progress: Float get() = if (totalBases == 0) 0f else checkedInBases.toFloat() / totalBases

    companion object {
        fun from(p: ParticipantDetail): HomeUiModel = HomeUiModel(
            // The whole name, not just the given name. `fullName` already falls back to
            // the student id and then the uuid when the backend has neither half, so the
            // greeting still addresses *someone* on a half-filled profile.
            displayName = p.fullName,
            // TODO: replace with real base check-in counts once the backend exposes them.
            checkedInBases = if (p.checkedIn) 3 else 0,
            totalBases = 8,
            nextBaseName = "Pine Grove",
            nextBaseDistance = "480 m",
        )
    }
}
