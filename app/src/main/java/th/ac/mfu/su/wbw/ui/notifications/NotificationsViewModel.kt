package th.ac.mfu.su.wbw.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onError
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.data.remote.dto.Notification
import th.ac.mfu.su.wbw.data.repository.NotificationRepository
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.common.UiState

/**
 * The announcement feed, plus the mark that says which of it is new.
 *
 * [seenBefore] is the seen mark as it stood *before* this screen opened. It has to be
 * captured at that moment and carried in the model, because opening the screen is the
 * thing that makes these announcements seen — read the setting back afterwards and every
 * item is old the first time it is ever shown, which is precisely when it is not.
 */
data class NotificationFeed(
    val items: List<Notification>,
    val seenBefore: Long,
) {
    /**
     * The server's own read state wins where it is set; the local mark covers the rest.
     * See [AppSettings.lastSeenNotificationId] for why there has to be a local mark.
     */
    fun isNew(item: Notification): Boolean = item.readAt == null && item.id > seenBefore
}

class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NotificationFeed>>(UiState.Loading)
    val state = _state.asStateFlow()

    /**
     * The seen mark as it stood the instant this screen was opened, captured once.
     *
     * It has to be a field rather than read per fetch, because this screen now shows a list
     * twice — the cached one immediately, then the refreshed one — and showing the cache is
     * itself enough to mark those announcements seen. Read the setting again for the second
     * list and everything would have just become old, so the "NEW" marks would vanish from
     * under the reader while they were still looking at them.
     */
    private val seenBefore = settings.lastSeenNotificationId.value

    init {
        repository.cachedMine()?.let { show(it) }
        load()
    }

    fun load() {
        if (_state.value !is UiState.Success) _state.value = UiState.Loading
        viewModelScope.launch {
            repository.mine()
                .onSuccess { show(it) }
                // Keep the cached feed on a failed refresh; an announcement worth reading
                // offline is still worth reading.
                .onError { if (_state.value !is UiState.Success) _state.value = UiState.Error(it) }
        }
    }

    private fun show(items: List<Notification>) {
        // Newest first, by id rather than by timestamp: the ids are a serial primary key,
        // while `created_at` is a string whose ordering only holds if every row carries
        // the same UTC offset.
        val newestFirst = items.sortedByDescending { it.id }
        _state.value = UiState.Success(NotificationFeed(newestFirst, seenBefore))
        // Only after the list has been handed to the UI holding the old mark.
        newestFirst.firstOrNull()?.let { settings.markNotificationsSeen(it.id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NotificationsViewModel(appContainer.notificationRepository, appContainer.appSettings)
            }
        }
    }
}
