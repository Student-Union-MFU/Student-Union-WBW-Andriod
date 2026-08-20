package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.CachedChat
import th.ac.mfu.su.wbw.data.remote.dto.ChatMessage
import th.ac.mfu.su.wbw.data.remote.dto.ChatReadRequest
import th.ac.mfu.su.wbw.data.remote.dto.ChatSync
import th.ac.mfu.su.wbw.data.remote.dto.OkResponse
import th.ac.mfu.su.wbw.data.remote.dto.SendMessageRequest

/**
 * Group chat over the server's long-poll.
 *
 * Thin on purpose. The interesting behaviour — holding the connection, deciding when to
 * retry, matching an optimistic bubble to the message that comes back — belongs to whoever
 * is driving the loop, because only they know whether the screen is still open. What lives
 * here is the wire and the disk.
 */
class ChatRepository(
    private val api: WbwApi,
    private val cache: ResponseCache,
) {

    /**
     * The last conversation this device saw, read synchronously so the screen's first frame
     * is the thread rather than a spinner — the same bargain the pass and the home screen
     * make. Scoped to a group id: a participant who changed groups must not open the new
     * group's channel on the old group's messages.
     */
    fun cached(groupId: Int): List<ChatMessage> =
        cache.read(ResponseCache.KeyChat, CachedChat.serializer())
            ?.takeIf { it.groupId == groupId }
            ?.messages
            .orEmpty()

    /** Keeps only the tail — this is a warm start, not an archive. */
    fun cache(groupId: Int, messages: List<ChatMessage>) {
        cache.write(
            ResponseCache.KeyChat,
            CachedChat.serializer(),
            CachedChat(groupId, messages.takeLast(CachedMessages)),
        )
    }

    /**
     * One long-poll round trip.
     *
     * [waitSeconds] of 0 makes it an ordinary poll, which is what a first load wants; the
     * loop that follows uses the full hold.
     */
    suspend fun sync(groupId: Int, after: Long, waitSeconds: Int): ApiResult<ChatSync> =
        apiCall { api.chatSync(groupId, after, waitSeconds) }

    suspend fun send(
        groupId: Int,
        clientId: String,
        body: String,
        deviceTime: String?,
    ): ApiResult<ChatMessage> =
        apiCall { api.sendMessage(groupId, SendMessageRequest(clientId, body, deviceTime)) }

    /** Read cursor *and* "the screen is open" heartbeat. See [WbwApi.chatRead]. */
    suspend fun markRead(groupId: Int, lastReadId: Long): ApiResult<OkResponse> =
        apiCall { api.chatRead(groupId, ChatReadRequest(lastReadId)) }

    private companion object {
        /** Enough to fill a screen twice over on open; not enough to bloat the prefs blob. */
        const val CachedMessages = 60
    }
}
