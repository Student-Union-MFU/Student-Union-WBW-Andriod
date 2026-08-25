package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One emergency as **staff** see it — `model.SOSStaffCase` on the server.
 *
 * A superset of [SosCase]: the same case, plus who raised it and what a responder needs on
 * arrival. The Go type embeds `SOSCase`, and embedding flattens in JSON, so this is one
 * flat class rather than a nested one.
 *
 * [healthNotes] and [bloodType] are not always sent, and their absence is not a gap in this
 * decoder. The server gates them in SQL on three conditions at once — the participant
 * consented to health data being used, the case is still open, and it was not raised on
 * somebody else's behalf — so a resolved case or a proxy report legitimately arrives
 * without them. Staff are not being handed a browsable medical database.
 */
@Serializable
data class SosStaffCase(
    val id: Long = 0,
    @SerialName("for_other") val forOther: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    @SerialName("loc_source") val locSource: String? = null,
    @SerialName("checkpoint_name") val checkpointName: String? = null,
    val message: String? = null,
    val resolved: Boolean = false,
    @SerialName("resolve_reason") val resolveReason: String? = null,
    @SerialName("acked_at") val ackedAt: String? = null,
    @SerialName("acked_by_name") val ackedByName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,

    @SerialName("participant_id") val participantId: String = "",
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val bib: Int? = null,
    @SerialName("group_number") val groupNumber: Int? = null,
    @SerialName("contact_phone") val contactPhone: String? = null,
    @SerialName("emergency_contact_name") val emergencyContactName: String? = null,
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String? = null,
    @SerialName("blood_type") val bloodType: String? = null,
    @SerialName("health_notes") val healthNotes: String? = null,
    @SerialName("updated_at") val updatedAt: String = "",
    /**
     * What a staff member found when they got there: `minor`, `major`, `urgent`, or null
     * if nobody has reported yet.
     *
     * Not the same axis as [resolved]. `false_alarm` and `minor` close a case, so they
     * arrive as a resolve reason; `major` and `urgent` deliberately do *not* close it and
     * show up here instead — the case stays on every console because it still needs
     * people. A card can therefore be open *and* severity-stamped at once, which is the
     * state this whole field exists to represent.
     */
    val severity: String? = null,
    /**
     * Whether this case has been confirmed and opened to the whole event.
     *
     * False means it is still stage one: raised by a participant and showing only to the
     * staff assigned to their group, plus admins. A case at this stage has not been judged
     * false — it has not been *looked at* — and the group's own staff are the ones walking
     * with them, so they are asked first.
     *
     * True is the actual SOS. Reporting a case major or urgent escalates it, and every
     * staff account sees it from then on.
     */
    val escalated: Boolean = false,
) {
    val acknowledged: Boolean get() = ackedAt != null

    /** Falls back to the bib, then the id — a case with no name is still a case. */
    val displayName: String
        get() = listOf(firstName, lastName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { bib?.let { "BIB $it" } ?: participantId.take(8) }

    /**
     * The cursor to send as `since` on the next poll.
     *
     * `"<updated_at>|<id>"` exactly — the server compares it as a row value because
     * `updated_at` alone is not unique, and two cases raised in the same transaction share
     * a timestamp to the microsecond. Simultaneous emergencies are the situation this
     * screen exists for, so the id half is what stops one of them being skipped forever.
     */
    val cursor: String get() = "$updatedAt|$id"
}

/**
 * What a staff member reports after reaching a case.
 *
 * The four values the server accepts, as a closed set rather than a free string, so a typo
 * is a compile error here instead of a 400 at a checkpoint. Two of them close the case and
 * two do not — see [SosStaffCase.severity]; which is which is the server's decision, and
 * this type deliberately does not encode it.
 */
enum class SosOutcome(val wire: String) {
    FalseAlarm("false_alarm"),
    Minor("minor"),
    Major("major"),
    Urgent("urgent"),
}

@kotlinx.serialization.Serializable
data class SosReportRequest(val outcome: String)
