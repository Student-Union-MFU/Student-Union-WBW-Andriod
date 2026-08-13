package th.ac.mfu.su.wbw.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import th.ac.mfu.su.wbw.data.remote.dto.AuthResponse
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.data.remote.dto.LoginRequest
import th.ac.mfu.su.wbw.data.remote.dto.Notification
import th.ac.mfu.su.wbw.data.remote.dto.NotificationPublic
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.data.remote.dto.RegisterRequest
import th.ac.mfu.su.wbw.data.remote.dto.School

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

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    /** Public — school list for the register form. */
    @GET("admin/schools")
    suspend fun schools(): List<School>

    /** bearer — the logged-in participant's own profile. */
    @GET("me")
    suspend fun me(): ParticipantDetail

    /** bearer — notifications delivered to the logged-in participant. */
    @GET("notifications")
    suspend fun notifications(): List<Notification>

    /** Public — announcements visible without logging in. */
    @GET("notifications/public")
    suspend fun publicNotifications(): List<NotificationPublic>

    /** bearer — event groups. */
    @GET("groups")
    suspend fun groups(): List<Group>
}
