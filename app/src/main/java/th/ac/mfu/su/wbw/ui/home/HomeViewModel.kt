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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onError
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.data.remote.dto.CheckinProgress
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.data.repository.ConditionsRepository
import th.ac.mfu.su.wbw.data.repository.NotificationRepository
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.data.repository.ProgressRepository
import th.ac.mfu.su.wbw.data.repository.TrailConditions
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.common.UiState

/**
 * Home dashboard state: the greeting from the profile, and the bloom from real check-ins.
 *
 * The two are separate requests on purpose. `/me` and `/me/progress` fail independently —
 * one is who you are, the other is how far you have got — and folding them into a single
 * [UiState] would mean a failed progress poll blanking the participant's own name, or the
 * greeting waiting on a second round trip before it could be drawn.
 */
class HomeViewModel(
    private val repository: ProfileRepository,
    private val notifications: NotificationRepository,
    private val conditions: ConditionsRepository,
    private val progressRepo: ProgressRepository,
    settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeUiModel>>(UiState.Loading)
    val state = _state.asStateFlow()

    /**
     * Bases collected, as the server counts them.
     *
     * Its own flow rather than a field on [HomeUiModel], because it arrives from a
     * different call at a different time and updates on its own schedule while the
     * profile does not. Null only until the first answer — cache or network — lands.
     */
    private val _progress = MutableStateFlow<CheckinProgress?>(null)
    val progress: StateFlow<CheckinProgress?> = _progress.asStateFlow()

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
        // Same bargain as the greeting: last known progress first, corrected when the
        // network answers. See [ProgressRepository].
        _progress.value = progressRepo.cached()
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

    /**
     * Keeps the bloom current for as long as Home is on screen.
     *
     * A suspending loop the screen drives from its own effect, like the chat's sync and
     * the SOS watch, so nothing polls while nobody is looking. Sixty seconds because a
     * check-in is somebody walking up to a table and being scanned — minute-scale, not
     * second-scale — and the server's own note on this endpoint assumes that cadence.
     *
     * A failed poll leaves the previous count alone. Losing signal is not the same as
     * losing your petals, and a bloom that shrank every time a request timed out would be
     * the most alarming possible way to say "no network".
     */
    suspend fun watchProgress() {
        while (currentCoroutineContext().isActive) {
            progressRepo.progress().onSuccess { _progress.value = it }
            delay(ProgressPollMillis)
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
        private const val ProgressPollMillis = 60_000L

        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    appContainer.profileRepository,
                    appContainer.notificationRepository,
                    appContainer.conditionsRepository,
                    appContainer.progressRepository,
                    appContainer.appSettings,
                )
            }
        }
    }
}

/**
 * Who Home is greeting.
 *
 * Nothing about the bloom lives here any more. It used to carry `checkedInBases`,
 * `totalBases` and a "next base" that were all invented in this file — three bases if the
 * profile said you had checked in anywhere at all, out of a hard-coded eight, next stop
 * "Pine Grove, 480 m" for everybody regardless of where they were standing. Those numbers
 * are now `/wbw/me/progress`, which counts the real rows and reads `total` from the
 * checkpoint table, so it stays right when an admin adds a base on the day.
 */
data class HomeUiModel(
    val displayName: String,
) {
    companion object {
        fun from(p: ParticipantDetail): HomeUiModel = HomeUiModel(
            // The whole name, not just the given name. `fullName` already falls back to
            // the student id and then the uuid when the backend has neither half, so the
            // greeting still addresses *someone* on a half-filled profile.
            displayName = p.fullName,
        )
    }
}
