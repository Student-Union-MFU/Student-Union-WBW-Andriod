package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Staff check-in at a checkpoint — `StaffCheckpoint`, `StaffCheckinRequest` and
 * `CheckinResult` in `internal/model/wbw_chat_model.go`.
 */

/**
 * One checkpoint a staff member can stamp people in at.
 *
 * The key is `id`, not `checkpoint_id` — the server's comment says as much, because it was
 * written to match the iOS client and the two names had already diverged once.
 *
 * [sequence] is nullable because a checkpoint need not be on the numbered route: a medical
 * post or the finish desk is a real place to be checked in at without being step 4 of 7.
 */
@Serializable
data class StaffCheckpoint(
    val id: Int,
    val name: String,
    val sequence: Int? = null,
)

/**
 * Who is being checked in, and where.
 *
 * The person is identified by **one** of [qrToken] or [bib], never both — the scanner sends
 * the token it read, and the manual fallback sends the number typed off the person's own
 * bib when a pass will not scan (a cracked screen, a dead phone, a participant who never
 * opened the app). Both paths land on the same row.
 *
 * [checkpointId] is nullable in the contract, but this app always sends it: the picker
 * defaults to the first checkpoint and cannot be cleared, so a stamp is never recorded
 * without saying where it happened.
 */
@Serializable
data class StaffCheckinRequest(
    @SerialName("checkpoint_id") val checkpointId: Int? = null,
    @SerialName("qr_token") val qrToken: String? = null,
    val bib: Int? = null,
)

/**
 * What the checkpoint learns about the person it just scanned.
 *
 * Deliberately thin. There is no participant id on it — the server marks that field `json:"-"`
 * and keeps it server-side for the notification it fires, so a user id never reaches a
 * borrowed phone at a checkpoint. What is here is what somebody standing at a table needs
 * to read off a screen in a second: is this the right person, and is there anything about
 * them I should know.
 *
 * [alreadyCheckedIn] is **not** an error. The server answers 200 for a repeat scan, because
 * the staff member still needs to see the name — the useful answer to scanning the same
 * person twice is "yes, that is Somchai, he came through already", not a red failure that
 * leaves them wondering whether it took.
 *
 * [hasMedicalFlag] is a flag and not the notes themselves. Whether this person has declared
 * something worth knowing is safe to show at a busy table; *what* they declared is not, and
 * the server only ever releases that into an open emergency — see [SosStaffCase].
 */
@Serializable
data class CheckinResult(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val bib: Int? = null,
    @SerialName("has_medical_flag") val hasMedicalFlag: Boolean = false,
    @SerialName("already_checked_in") val alreadyCheckedIn: Boolean = false,
) {
    /** Falls back to the bib — a stamp that recorded fine but has no name is still a stamp. */
    val displayName: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { bib?.let { "BIB $it" } ?: "" }
}
