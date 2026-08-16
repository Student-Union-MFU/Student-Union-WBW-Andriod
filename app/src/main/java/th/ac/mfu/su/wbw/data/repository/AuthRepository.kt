package th.ac.mfu.su.wbw.data.repository

import kotlinx.coroutines.flow.Flow
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.LoginRequest
import th.ac.mfu.su.wbw.data.remote.dto.RegisterRequest
import th.ac.mfu.su.wbw.data.remote.dto.School

/** Auth + the public school list used by registration. Owns the session store. */
class AuthRepository(
    private val api: WbwApi,
    private val sessions: SessionStore,
    private val cache: ResponseCache,
) {
    /** Emits the current session, or null when logged out. */
    val session: Flow<Session?> = sessions.session

    suspend fun login(username: String, password: String): ApiResult<Session> =
        apiCall { api.login(LoginRequest(username.trim(), password)) }
            .persist()

    suspend fun register(request: RegisterRequest): ApiResult<Session> =
        apiCall { api.register(request) }
            .persist()

    /**
     * Clears the session *and* everything cached under it.
     *
     * The cache holds the participant's name, bib, group, school and emergency and medical
     * contacts, and the screens that show them now open on it before any request is made.
     * Left behind, the next person to sign in on the same phone — which on an event like
     * this is a real thing that happens — would see the previous participant's pass until
     * the first fetch landed. Short enough to look like a glitch, long enough to read.
     */
    suspend fun logout() {
        sessions.clear()
        cache.clear()
    }

    suspend fun schools(): ApiResult<List<School>> = apiCall { api.schools() }

    // Turn an AuthResponse into a stored Session on success.
    private suspend fun ApiResult<th.ac.mfu.su.wbw.data.remote.dto.AuthResponse>.persist(): ApiResult<Session> =
        when (this) {
            is ApiResult.Success -> {
                // Belt and braces against [logout] not having run: a token that expired
                // server-side drops the user back to the login screen without this app
                // ever calling logout, so a fresh sign-in must not inherit whatever the
                // last session left on disk.
                cache.clear()
                val s = Session(
                    token = data.token,
                    userId = data.user.userId,
                    username = data.user.username,
                    role = data.user.role,
                )
                sessions.save(s)
                ApiResult.Success(s)
            }
            is ApiResult.Error -> this
        }
}
