package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /auth/login body. */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

/** The 3-field user object returned by register/login. */
@Serializable
data class AuthUser(
    @SerialName("user_id") val userId: String,
    val username: String,
    val role: String,
)

/** POST /auth/login and /auth/register success response. */
@Serializable
data class AuthResponse(
    val user: AuthUser,
    val token: String,
)

/**
 * POST /auth/register body. Mirrors the Go RegisterRequest exactly — nested
 * objects and snake_case keys the backend destructures. Optional fields default
 * so a minimal registration still serialises to valid JSON.
 */
@Serializable
data class RegisterRequest(
    @SerialName("student_id") val studentId: String? = null,
    val username: String? = null,
    val password: String,
    val profile: Profile,
    val medical: Medical = Medical(),
    val health: Health = Health(),
    val consent: Consent = Consent(),
) {
    @Serializable
    data class Profile(
        @SerialName("first_name") val firstName: String,
        @SerialName("last_name") val lastName: String,
        val sex: String,
        @SerialName("contact_phone") val contactPhone: String? = null,
        @SerialName("school_id") val schoolId: Int? = null,
        val major: String? = null,
        @SerialName("photo_url") val photoUrl: String? = null,
        @SerialName("date_of_birth") val dateOfBirth: String? = null,
        @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
        @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    )

    @Serializable
    data class Medical(
        val birthdate: String? = null,
        @SerialName("weight_kg") val weightKg: Double? = null,
        @SerialName("height_cm") val heightCm: Double? = null,
        @SerialName("blood_type") val bloodType: String? = null,
    )

    @Serializable
    data class Health(
        @SerialName("chronic_conditions") val chronicConditions: List<String> = emptyList(),
    )

    @Serializable
    data class Consent(
        @SerialName("consent_health_data") val consentHealthData: Boolean = false,
        @SerialName("consent_emergency_treatment") val consentEmergencyTreatment: Boolean = false,
        @SerialName("waiver_accepted") val waiverAccepted: Boolean = false,
    )
}
