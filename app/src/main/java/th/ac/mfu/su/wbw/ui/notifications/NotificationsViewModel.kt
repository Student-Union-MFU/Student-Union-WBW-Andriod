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
import th.ac.mfu.su.wbw.data.remote.dto.Notification
import th.ac.mfu.su.wbw.data.repository.NotificationRepository
import th.ac.mfu.su.wbw.ui.appContainer
import th.ac.mfu.su.wbw.ui.common.UiState

class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Notification>>>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        fetch()
    }

    fun refresh() {
        _refreshing.value = true
        fetch()
    }

    private fun fetch() {
        viewModelScope.launch {
            repository.mine()
                .onSuccess { _state.value = UiState.Success(it) }
                .onError { msg ->
                    // Keep existing list on a refresh failure; only show error if empty.
                    if (_state.value !is UiState.Success) _state.value = UiState.Error(msg)
                }
            _refreshing.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotificationsViewModel(appContainer.notificationRepository) }
        }
    }
}
