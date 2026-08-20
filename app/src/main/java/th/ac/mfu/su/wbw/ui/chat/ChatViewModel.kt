package th.ac.mfu.su.wbw.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.data.remote.dto.ChatMessage
import th.ac.mfu.su.wbw.data.remote.dto.ChatSync
import th.ac.mfu.su.wbw.data.repository.ChatRepository
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.ui.appContainer
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * A message this device has sent but the server has not confirmed.
 *
 * It carries the same `clientId` the request did, which is what lets the confirmation be
 * matched back to it — the server may answer the retry of a timed-out send with the message
 * the *first* attempt already stored, so "did this arrive" cannot be decided on the response
 * to any one request.
 */
data class PendingMessage(
    val clientId: String,
    val body: String,
    val failed: Boolean = false,
)

data class ChatUiState(
    val groupId: Int? = null,
    val groupNumber: Int? = null,
    /** True once we know from the profile that there is no group to chat in. */
    val noGroup: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val pending: List<PendingMessage> = emptyList(),
    val memberCount: Int = 0,
    /** How many other members have read this device's latest message. */
    val readCount: Int = 0,
    val meId: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * The chat engine: long-poll in, optimistic send out.
 *
 * **The polling loop is not started here.** [sync] and [heartbeat] are suspending functions
 * the screen calls from its own effects, so they live exactly as long as the screen is on
 * display. Launching them in `viewModelScope` would have been shorter and wrong: this view
 * model is scoped to a navigation entry that survives tab switches, so a walk to the map and
 * back would leave a connection permanently held open — on a phone being carried up a hill,
 * for a screen nobody is looking at. State stays here; the *driving* belongs to the screen.
 */
class ChatViewModel(
    private val chat: ChatRepository,
    private val profile: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** The highest message id this device has seen — the long-poll's cursor. */
    private var lastId: Long = 0

    init {
        // Seeded from cache before the first frame, so a returning participant opens on the
        // conversation rather than on an empty column with a spinner over it.
        val me = profile.cachedMe()
        val groupId = me?.groupId
        val cached = groupId?.let { chat.cached(it) }.orEmpty()
        lastId = cached.maxOfOrNull { it.id } ?: 0
        _state.value = ChatUiState(
            groupId = groupId,
            groupNumber = me?.groupNumber,
            noGroup = me != null && groupId == null,
            messages = cached,
            meId = me?.id,
            loading = cached.isEmpty(),
        )
        refreshProfile()
    }

    /**
     * Re-reads `/me` for the group id.
     *
     * Cheap and worth it: the participant may have joined a group on another device, or on
     * the website, since this one last cached anything.
     */
    private fun refreshProfile() {
        viewModelScope.launch {
            val result = profile.me()
            if (result is ApiResult.Success) {
                val groupId = result.data.groupId
                _state.update {
                    // A change of group invalidates the thread on screen.
                    val changed = it.groupId != null && it.groupId != groupId
                    it.copy(
                        groupId = groupId,
                        groupNumber = result.data.groupNumber,
                        noGroup = groupId == null,
                        meId = result.data.id,
                        messages = if (changed) emptyList() else it.messages,
                    )
                }
                if (groupId == null || _state.value.messages.isEmpty()) lastId = 0
            }
        }
    }

    /**
     * The long-poll loop. Runs until cancelled — i.e. until the screen goes away.
     *
     * The first pass asks for no hold at all, so opening the screen paints immediately;
     * every pass after it uses the full 25 seconds the server allows, which is what makes
     * this feel live without polling in a spin.
     */
    suspend fun sync() {
        var wait = 0
        var backoff = InitialBackoffMillis
        while (currentCoroutineContext().isActive) {
            val groupId = _state.value.groupId
            if (groupId == null) {
                delay(NoGroupRetryMillis)
                continue
            }
            when (val result = chat.sync(groupId, lastId, wait)) {
                is ApiResult.Success -> {
                    apply(groupId, result.data)
                    backoff = InitialBackoffMillis
                    wait = HoldSeconds
                }
                is ApiResult.Error -> {
                    // 403 means this participant is not in the group any more; re-reading
                    // the profile is the only way back to a correct screen.
                    if (result.code == 403) refreshProfile()
                    _state.update { it.copy(loading = false, error = result.message) }
                    // Backing off matters more than usual here. A failure that returns
                    // instantly — no network, a 500 — would otherwise spin this loop as
                    // fast as the radio can refuse, which is the worst thing an app can do
                    // to a battery on a mountain.
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MaxBackoffMillis)
                }
            }
        }
    }

    private fun apply(groupId: Int, sync: ChatSync) {
        _state.update { current ->
            val merged = merge(current.messages, sync.messages)
            // Anything the server has now confirmed stops being pending. Matched on
            // clientId, not on body: two identical messages are a normal thing to send.
            val confirmed = sync.messages.mapTo(HashSet()) { it.clientId }
            val stillPending = current.pending.filterNot { it.clientId in confirmed }
            val mine = merged.lastOrNull { it.senderId == current.meId }?.id ?: 0
            current.copy(
                messages = merged,
                pending = stillPending,
                memberCount = sync.memberCount,
                readCount = if (mine == 0L) 0 else sync.cursors.count { it.lastReadId >= mine },
                loading = false,
                error = null,
            )
        }
        val messages = _state.value.messages
        lastId = messages.maxOfOrNull { it.id } ?: lastId
        chat.cache(groupId, messages)
    }

    /**
     * The read cursor, on a timer, for as long as the screen is open.
     *
     * This is not bookkeeping that can be skipped. The server reads the same row as a
     * "chat screen is open" heartbeat and suppresses push for whoever is sending it, so a
     * client that never calls it is both invisible in "read by N" *and* gets pushed
     * notifications for messages it is currently displaying. The deploy notes record that
     * happening to an earlier Android build.
     */
    suspend fun heartbeat() {
        while (currentCoroutineContext().isActive) {
            val groupId = _state.value.groupId
            // `lastId` of 0 is sent, not skipped. The old guard held the heartbeat back
            // until something had been read, which is precisely backwards: a member sitting
            // in a group with no messages yet, or in the window before the first sync
            // lands, was not registered as having the screen open — so the server pushed
            // them a notification for the very first message while they were looking at it.
            // That is the failure `docs/chat-v2-deploy.md` records against an earlier build.
            // The server takes 0 happily; `MarkRead` rejects only null and negatives.
            if (groupId != null) {
                chat.markRead(groupId, lastId)
            }
            delay(ReadHeartbeatMillis)
        }
    }

    /**
     * Send, showing the message immediately.
     *
     * The bubble appears before the request does anything, because on this network the
     * round trip is routinely seconds and a composer that clears with nothing to show for
     * it reads as a dropped message.
     */
    fun send(body: String): Boolean {
        val text = body.trim()
        if (text.isEmpty()) return false
        val groupId = _state.value.groupId ?: return false
        // Counted in code points, as the server counts runes — `length` is UTF-16 units, so
        // every emoji counts twice and the two sides disagree about what 2000 means.
        //
        // Returning false rather than swallowing it. This used to `return` after setting
        // `error = null`, and the screen cleared the composer regardless, so a long message
        // was destroyed silently: no bubble, no error, and the text the participant had
        // written was gone. The screen now keeps the draft when this is false.
        if (text.codePointCount(0, text.length) > MaxBodyChars) return false
        val clientId = UUID.randomUUID().toString()
        _state.update { it.copy(pending = it.pending + PendingMessage(clientId, text)) }

        viewModelScope.launch {
            val deviceTime = OffsetDateTime.now().toString()
            when (val result = chat.send(groupId, clientId, text, deviceTime)) {
                is ApiResult.Success -> _state.update { current ->
                    val merged = merge(current.messages, listOf(result.data))
                    lastId = maxOf(lastId, result.data.id)
                    chat.cache(groupId, merged)
                    current.copy(
                        messages = merged,
                        pending = current.pending.filterNot { it.clientId == clientId },
                        error = null,
                    )
                }
                is ApiResult.Error -> _state.update { current ->
                    // Left in place and marked, not deleted: the text a participant typed is
                    // theirs, and silently dropping it is worse than showing it as unsent.
                    current.copy(
                        pending = current.pending.map {
                            if (it.clientId == clientId) it.copy(failed = true) else it
                        },
                        error = result.message,
                    )
                }
            }
        }
        return true
    }

    /** Retry a message that failed to send, reusing its client id so it cannot double-post. */
    fun retry(clientId: String) {
        val pending = _state.value.pending.firstOrNull { it.clientId == clientId } ?: return
        val groupId = _state.value.groupId ?: return
        _state.update { current ->
            current.copy(
                pending = current.pending.map {
                    if (it.clientId == clientId) it.copy(failed = false) else it
                },
            )
        }
        viewModelScope.launch {
            when (val result = chat.send(groupId, clientId, pending.body, null)) {
                is ApiResult.Success -> _state.update { current ->
                    val merged = merge(current.messages, listOf(result.data))
                    lastId = maxOf(lastId, result.data.id)
                    chat.cache(groupId, merged)
                    current.copy(
                        messages = merged,
                        pending = current.pending.filterNot { it.clientId == clientId },
                    )
                }
                is ApiResult.Error -> _state.update { current ->
                    current.copy(
                        pending = current.pending.map {
                            if (it.clientId == clientId) it.copy(failed = true) else it
                        },
                        error = result.message,
                    )
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    /**
     * Merge by id, keeping one copy of each and the order the ids imply.
     *
     * The same message legitimately arrives twice — once as the response to a send, once
     * again in the next sync — so this has to be idempotent rather than an append.
     */
    private fun merge(existing: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> {
        if (incoming.isEmpty()) return existing
        val byId = LinkedHashMap<Long, ChatMessage>(existing.size + incoming.size)
        existing.forEach { byId[it.id] = it }
        incoming.forEach { byId[it.id] = it }
        return byId.values.sortedBy { it.id }
    }

    companion object {
        /** The server clamps to 25; asking for exactly that keeps the round trips minimal. */
        private const val HoldSeconds = 25

        private const val ReadHeartbeatMillis = 10_000L
        private const val InitialBackoffMillis = 2_000L
        private const val MaxBackoffMillis = 30_000L
        private const val NoGroupRetryMillis = 5_000L

        /** `maxBodyLen` in `wbw_chat_service.go`, in runes. Shared with the composer, which
         *  refuses to take more than the server will accept. */
        const val MaxBodyChars = 2_000

        val Factory = viewModelFactory {
            initializer {
                ChatViewModel(appContainer.chatRepository, appContainer.profileRepository)
            }
        }
    }
}

/** Local wall-clock label for a message, from the server's timestamp. */
internal fun ChatMessage.timeLabel(): String {
    val at = createdAt ?: deviceTime ?: return ""
    return runCatching {
        OffsetDateTime.parse(at).atZoneSameInstant(ZoneId.systemDefault())
            .toLocalTime()
            .let { "%02d:%02d".format(it.hour, it.minute) }
    }.getOrDefault("")
}

/** The calendar day a message belongs to, for the dividers. Empty when unparseable. */
internal fun ChatMessage.dayKey(): String {
    val at = createdAt ?: deviceTime ?: return ""
    return runCatching {
        OffsetDateTime.parse(at).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().toString()
    }.getOrDefault("")
}
