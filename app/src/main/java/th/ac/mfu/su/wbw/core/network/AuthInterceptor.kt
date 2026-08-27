package th.ac.mfu.su.wbw.core.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import th.ac.mfu.su.wbw.data.local.SessionStore
import java.net.HttpURLConnection

/**
 * Attaches `Authorization: Bearer <token>` when a session token is present, and reports
 * back when the server refuses the one it was given.
 *
 * The backend's RequireAuth middleware matches the "Bearer " prefix exactly.
 *
 * [onUnauthorized] fires on a 401 answered to a request that *carried* a token — see
 * [isRejectedToken] for why that qualifier is the whole of the logic. It runs on OkHttp's
 * thread and must not block: the container hands it a lambda that only launches.
 */
class AuthInterceptor(
    private val sessions: SessionStore,
    private val onUnauthorized: () -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessions.currentToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        val response = chain.proceed(request)
        // Only the code is read. Touching the body here would consume the one-shot stream
        // and leave the converter downstream with nothing to decode.
        if (isRejectedToken(response.code, request, token)) onUnauthorized()
        return response
    }

    private companion object {
        /**
         * Whether a 401 means "the token you are holding is dead" rather than anything else.
         *
         * Two things it must not be confused with, because the server answers 401 to both:
         *
         * A **wrong password**. `POST /wbw/auth/login` returns 401 for bad credentials, and
         * treating that as an expiry would clear a session on every typo — and, worse, put
         * "your session expired" on screen in place of "wrong username or password", which
         * is the one thing the person at that moment actually needs told. Signing in while
         * signed out sends no token, so the token check below already excludes it; the path
         * check is there so that stays true if the interceptor ever starts attaching one.
         *
         * A **request made before the token was loaded**. `SessionStore.prime()` fills the
         * synchronous cache off the main thread at startup, so a request that beats it
         * carries no header and is refused. Nothing has expired — the app simply asked
         * early — and signing the participant out for it would turn a cold-start race into
         * a logout.
         */
        fun isRejectedToken(code: Int, request: Request, token: String?): Boolean =
            code == HttpURLConnection.HTTP_UNAUTHORIZED &&
                !token.isNullOrBlank() &&
                !request.url.encodedPath.endsWith("/auth/login")
    }
}
