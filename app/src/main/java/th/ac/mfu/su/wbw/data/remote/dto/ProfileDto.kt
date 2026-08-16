package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /me response — the participant's own full profile. Matches the Go
 * ParticipantDetail model; almost every field is nullable because the backend
 * emits NULLs for anything the participant didn't provide.
 */
@Serializable
data class ParticipantDetail(
    val id: String,
    @SerialName("student_id") val studentId: String? = null,
    val created: String? = null,
    val bib: Int? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val sex: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("school_id") val schoolId: Int? = null,
    @SerialName("school_name") val schoolName: String? = null,
    val major: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("group_number") val groupNumber: Int? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    /**
     * The check-in credential — `participant_profile.qr_token`, 12 random bytes as hex.
     *
     * This is what goes in the QR on the pass, and it is deliberately *not* [id] or
     * [studentId]. Those are identifiers: they name the participant and are printed,
     * spoken and shared, so anyone who saw one could present it as their own. This is a
     * capability — unguessable, unique, and revocable by rotating one column — and the
     * server treats it that way: `POST /wbw/staff/checkin` takes `qr_token` **or** `bib`,
     * and prefers the token when both arrive, because a scan beats a marshal typing.
     *
     * The server has been sending this on every `/me` since before the app had a field
     * for it; it was simply being dropped on the floor by the decoder.
     */
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerialName("blood_type") val bloodType: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("consent_health_data") val consentHealthData: Boolean? = null,
    @SerialName("consent_emergency_treatment") val consentEmergencyTreatment: Boolean? = null,
    @SerialName("waiver_accepted") val waiverAccepted: Boolean? = null,
) {
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { studentId ?: id }
}

/** GET /admin/schools item (public — used by the register form). */
@Serializable
data class School(
    @SerialName("school_id") val schoolId: Int,
    val name: String,
)

/** GET /groups item. */
@Serializable
data class Group(
    @SerialName("group_id") val groupId: Int,
    @SerialName("group_number") val groupNumber: Int,
    val capacity: Int,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("seats_left") val seatsLeft: Int,
)
