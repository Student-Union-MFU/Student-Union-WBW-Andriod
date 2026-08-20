package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /auth/login body. */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

/** The 3-field user object returned by login. */
@Serializable
data class AuthUser(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: String,
)

/** POST /auth/login success response. */
@Serializable
data class AuthResponse(
    val user: AuthUser,
    val token: String,
)

