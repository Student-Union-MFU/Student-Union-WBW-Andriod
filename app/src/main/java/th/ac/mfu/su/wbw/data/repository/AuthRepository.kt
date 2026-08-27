package th.ac.mfu.su.wbw.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.LoginRequest

/** Sign-in and sign-out. Owns the session store. */
class AuthRepository(
    private val api: WbwApi,
    private val sessions: SessionStore,
    private val cache: ResponseCache,
) {
    /** Emits the current session, or null when logged out. */
    val session: Flow<Session?> = sessions.session

    private val _sessionExpired = MutableStateFlow(false)

    /**
     * True when the last sign-out was the server's decision rather than the participant's.
     *
     * The login screen reads this to say *why* it is being shown. Without it an expiry is
     * indistinguishable from a crash: the app is simply on the login screen one moment
     * having been on the map the next, and the natural reading is that it lost the walk.
     */
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    suspend fun login(username: String, password: String): ApiResult<Session> =
        apiCall { api.login(LoginRequest(username.trim(), password)) }
            .persist()

    /**
     * The server refused a token this app sent — 30 days ran out, or an account was
     * disabled mid-event. Same clearing as [logout], plus the flag that explains it.
     *
     * Guarded by [MutableStateFlow.compareAndSet] because a 401 rarely arrives alone: the
     * home screen fetches the profile, notifications and progress together, and a dead
     * token fails all three within a few milliseconds of each other. Doing the work once
     * keeps two of those three from clearing a DataStore that is already empty.
     */
    suspend fun onTokenRejected() {
        if (!_sessionExpired.compareAndSet(expect = false, update = true)) return
        sessions.clear()
        cache.clear()
    }

    /**
     * Drop the notice. Called when the participant acts on it by signing in again — the
     * explanation has been read by then, and leaving it under a failed second attempt
     * would sit "your session expired" next to "wrong password" and contradict it.
     */
    fun acknowledgeExpiry() {
        _sessionExpired.value = false
    }

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

    // Turn an AuthResponse into a stored Session on success.
    private suspend fun ApiResult<th.ac.mfu.su.wbw.data.remote.dto.AuthResponse>.persist(): ApiResult<Session> =
        when (this) {
            is ApiResult.Success -> {
                // Belt and braces against [logout] not having run: a token that expired
                // server-side drops the user back to the login screen without this app
                // ever calling logout, so a fresh sign-in must not inherit whatever the
                // last session left on disk.
                cache.clear()
                _sessionExpired.value = false
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
