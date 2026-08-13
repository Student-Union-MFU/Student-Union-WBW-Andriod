package th.ac.mfu.su.wbw.core.network

import okhttp3.Interceptor
import okhttp3.Response
import th.ac.mfu.su.wbw.data.local.SessionStore

/**
 * Attaches `Authorization: Bearer <token>` when a session token is present.
 * The backend's RequireAuth middleware matches the "Bearer " prefix exactly.
 */
class AuthInterceptor(private val sessions: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessions.currentToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
