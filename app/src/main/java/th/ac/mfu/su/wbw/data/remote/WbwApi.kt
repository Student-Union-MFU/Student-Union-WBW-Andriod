package th.ac.mfu.su.wbw.data.remote

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
}
