package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Group chat, matching `internal/model/wbw_chat_model.go` on the server.
 *
 * The Go side notes that its json tags have to match what iOS decodes; this file is the
 * third client of the same contract, so the tags are spelled out here rather than inferred.
 */

/**
 * One message.
 *
 * [clientId] is the idempotency key, not a display field: a send that times out can be
 * retried with the same one and the server returns the original message instead of writing
 * a second. It is also how an optimistic bubble on this side is matched to the real message
 * when it comes back, which is why it is carried all the way through the UI.
 *
 * There is no author role. A staff badge cannot be drawn from this — the server does not
 * send one on a message — so the chat column shows names only.
 */
@Serializable
data class ChatMessage(
    val id: Long,
    @SerialName("group_id") val groupId: Int = 0,
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("client_id") val clientId: String = "",
    val body: String = "",
    @SerialName("device_time") val deviceTime: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
) {
    /** Falls back rather than showing a raw UUID to a participant. */
    val authorName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .ifBlank { senderId.take(8) }
}

/** How far another member has read. The server removes the caller from this list. */
@Serializable
data class ReadCursor(
    @SerialName("user_id") val userId: String = "",
    @SerialName("last_read_id") val lastReadId: Long = 0,
)

/**
 * One long-poll round trip: new messages *and* who has read what.
 *
 * [sinceId] is the caller's own join point. The server never returns anything below it, so
 * somebody who joins a group today does not open the screen on a month of conversation they
 * were not part of.
 */
@Serializable
data class ChatSync(
    @SerialName("since_id") val sinceId: Long = 0,
    @SerialName("member_count") val memberCount: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val cursors: List<ReadCursor> = emptyList(),
)

/** POST body for a send. [deviceTime] is the phone's clock, for ordering an offline outbox. */
@Serializable
data class SendMessageRequest(
    @SerialName("client_id") val clientId: String,
    val body: String,
    @SerialName("device_time") val deviceTime: String? = null,
)

/** POST body for the read cursor. Doubles as the "screen is open" heartbeat. */
@Serializable
data class ChatReadRequest(
    @SerialName("last_read_id") val lastReadId: Long?,
)

/** `{"ok": true}` — what the group and chat mutations answer with. */
@Serializable
data class OkResponse(val ok: Boolean = false)

/** `{"ok": true, "group_id": n}` from a successful join. */
@Serializable
data class JoinGroupResponse(
    val ok: Boolean = false,
    @SerialName("group_id") val groupId: Int? = null,
)

/** One member of a group, for the roster. */
@Serializable
data class GroupMember(
    @SerialName("user_id") val userId: String = "",
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val bib: Int? = null,
    val school: String? = null,
)

@Serializable
data class GroupMembersResponse(
    val members: List<GroupMember> = emptyList(),
    val count: Int = 0,
)

/** The last conversation this device saw, so the screen opens on it rather than empty. */
@Serializable
data class CachedChat(
    @SerialName("group_id") val groupId: Int,
    val messages: List<ChatMessage> = emptyList(),
)
