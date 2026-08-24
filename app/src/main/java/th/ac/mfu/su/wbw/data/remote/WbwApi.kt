package th.ac.mfu.su.wbw.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import th.ac.mfu.su.wbw.data.remote.dto.AuthResponse
import th.ac.mfu.su.wbw.data.remote.dto.ChatMessage
import th.ac.mfu.su.wbw.data.remote.dto.ChatReadRequest
import th.ac.mfu.su.wbw.data.remote.dto.ChatSync
import th.ac.mfu.su.wbw.data.remote.dto.GroupMembersResponse
import th.ac.mfu.su.wbw.data.remote.dto.JoinGroupResponse
import th.ac.mfu.su.wbw.data.remote.dto.OkResponse
import th.ac.mfu.su.wbw.data.remote.dto.SendMessageRequest
import th.ac.mfu.su.wbw.data.remote.dto.SosCase
import th.ac.mfu.su.wbw.data.remote.dto.SosStaffCase
import th.ac.mfu.su.wbw.data.remote.dto.SosRequest
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.data.remote.dto.LoginRequest
import th.ac.mfu.su.wbw.data.remote.dto.Notification
import th.ac.mfu.su.wbw.data.remote.dto.NotificationPublic
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail

/**
 * The WBW backend (Go/Chi) `/wbw` route group. The base URL configured in
 * [th.ac.mfu.su.wbw.core.network.NetworkModule] already includes the trailing
 * `/wbw/`, so paths here are relative to it.
 *
 * Auth: endpoints below marked "bearer" require a token; [AuthInterceptor]
 * attaches it automatically from the token store.
 */
interface WbwApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    /** bearer — the logged-in participant's own profile. */
    @GET("me")
    suspend fun me(): ParticipantDetail

    /** bearer — notifications delivered to the logged-in participant. */
    @GET("notifications")
    suspend fun notifications(): List<Notification>

    /** Public — announcements visible without logging in. */
    @GET("notifications/public")
    suspend fun publicNotifications(): List<NotificationPublic>

    /** bearer — event groups. 40 of them, capacity 50 each. */
    @GET("groups")
    suspend fun groups(): List<Group>

    /**
     * bearer — join a group.
     *
     * 409 when the group filled up between listing and tapping, or when the participant is
     * already in one; 404 when the group does not exist. All three are ordinary outcomes on
     * this screen, not faults — see `WBWGroupHandler.Join`.
     */
    @POST("groups/{groupId}/join")
    suspend fun joinGroup(@Path("groupId") groupId: Int): JoinGroupResponse

    /**
     * bearer — leave the current group. Costs one `leave_quota`, and a participant starts
     * with exactly one, so this succeeds at most once. 409 once it is spent. Leaving when
     * already in no group answers 200, not an error.
     */
    @POST("groups/leave")
    suspend fun leaveGroup(): OkResponse

    /** bearer — the roster of one group. */
    @GET("groups/{groupId}/members")
    suspend fun groupMembers(@Path("groupId") groupId: Int): GroupMembersResponse

    /**
     * bearer — **long-poll**. Holds the request open for up to [wait] seconds (server
     * clamps to 25) until the group has something new, then returns everything after
     * [after]. 403 when the caller is not in this group.
     *
     * The hold is why [th.ac.mfu.su.wbw.core.network.NetworkModule] gives this one path a
     * longer read timeout than every other call.
     */
    @GET("groups/{groupId}/chat/sync")
    suspend fun chatSync(
        @Path("groupId") groupId: Int,
        @Query("after") after: Long,
        @Query("wait") wait: Int,
    ): ChatSync

    /**
     * bearer — move the read cursor.
     *
     * Two jobs in one call: it records how far this member has read, *and* it is the
     * heartbeat that says the chat screen is open, which suppresses push notifications for
     * them. Stop calling it and the member silently stops counting as a reader.
     */
    @POST("groups/{groupId}/chat/read")
    suspend fun chatRead(
        @Path("groupId") groupId: Int,
        @Body body: ChatReadRequest,
    ): OkResponse

    /** bearer — plain (non-holding) page of messages. */
    @GET("groups/{groupId}/messages")
    suspend fun messages(
        @Path("groupId") groupId: Int,
        @Query("after") after: Long? = null,
        @Query("limit") limit: Int = 50,
    ): List<ChatMessage>

    /** bearer — send. Answers 201 with the stored message. */
    @POST("groups/{groupId}/messages")
    suspend fun sendMessage(
        @Path("groupId") groupId: Int,
        @Body body: SendMessageRequest,
    ): ChatMessage

    // ===== Emergency =====

    /**
     * bearer — raise an emergency, or update the one already open.
     *
     * 201 when a case was created, 200 when this was a repeat: the same `client_id` sent
     * twice, or a second press while a case is still open. Both are ordinary — the server
     * enforces one open case per participant with a unique index, so a panicked double-press
     * lands on the existing row instead of opening a second emergency. Retrofit gives both
     * back as a normal return, so the app does not have to tell them apart.
     */
    @POST("me/sos")
    suspend fun raiseSos(@Body body: SosRequest): SosCase

    /**
     * bearer — **long-poll**. The participant's own open case, or a JSON `null` when there
     * is none. Holds for up to [wait] seconds (server clamps to 25) waiting for the case to
     * change — which in practice means waiting for a staff member to acknowledge it.
     *
     * Returns [JsonElement] rather than a nullable `SosCase`, which looks like a step
     * backwards and is not.
     *
     * The server answers 200 with a body of literally `null` when there is no emergency.
     * Kotlin's nullability does not survive into the `java.lang.Type` Retrofit reflects on,
     * so a `SosCase?` return type reaches the converter as plain `SosCase`, and decoding
     * `null` with a non-nullable serializer **throws** — turning the ordinary answer "you
     * have no emergency" into a parse error on the one screen that must not lie about its
     * state. `JsonElement` decodes `null` to `JsonNull` and hands the decision back to
     * [th.ac.mfu.su.wbw.data.repository.SosRepository], which is where it belongs.
     *
     * Like `chat/sync` this holds, so [th.ac.mfu.su.wbw.core.network.NetworkModule] gives
     * it the longer read timeout.
     */
    @GET("me/sos/active")
    suspend fun activeSos(@Query("wait") wait: Int): JsonElement

    /**
     * bearer — stand down.
     *
     * 409 once a staff member has acknowledged it, and 409 again after the server's 120-second
     * window closes; in both cases somebody is already moving and the app tells the
     * participant to phone rather than silently un-sending. 404 when there is no such case.
     */
    @POST("me/sos/{id}/cancel")
    suspend fun cancelSos(@Path("id") id: Long): OkResponse

    // ===== Staff =====
    //
    // Everything below needs an `admin` or `staff` account and answers 403 otherwise. None
    // of it is reachable from the participant shell.

    /**
     * staff — **long-poll**. Open cases, plus anything closed in the last 30 minutes so a
     * base that has just run to one sees it finish rather than sees it vanish.
     *
     * [since] is the `cursor` of the newest row already seen, verbatim. Blank on the first
     * call. Holds for up to [wait] seconds waiting for something to change.
     *
     * Which cases a given staff member sees is decided by the server, not asked for here:
     * their own checkpoint, cases with no checkpoint, checkpoints with nobody assigned, and
     * anything whose position is too coarse to trust — with `admin`, `medical` and
     * `security` seeing every case regardless.
     */
    @GET("staff/sos")
    suspend fun staffSosFeed(
        @Query("since") since: String,
        @Query("wait") wait: Int,
    ): List<SosStaffCase>

    /**
     * staff — "on my way".
     *
     * First press wins and a second is **not** an error: the server answers with the case
     * carrying the first responder's name, so whoever pressed second sees who is already
     * going instead of a failure.
     */
    @POST("staff/sos/{id}/ack")
    suspend fun ackSos(@Path("id") id: Long): SosStaffCase
}
