package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One base as a participant sees it — `model.ParticipantCheckpoint` on the server.
 *
 * Every checkpoint in the event, not only the ones already collected. That distinction is
 * the reason this exists alongside [CheckinProgress]: progress answers "where have I been",
 * and this answers "where are the bases", which is what a map is for.
 *
 * [lat] and [lng] are nullable because the column is. A row without a position is skipped
 * rather than drawn — a checkpoint pinned at (0, 0) is in the Atlantic, and a marker in the
 * Gulf of Guinea is a worse answer than no marker.
 *
 * The name comes in both languages and the app picks by locale rather than by asking the
 * server for one, so switching language in settings does not need a round trip to relabel
 * the map.
 */
@Serializable
data class ParticipantCheckpoint(
    val id: Int,
    val sequence: Int? = null,
    val name: String = "",
    @SerialName("name_en") val nameEn: String? = null,
    @SerialName("activity_name") val activityName: String? = null,
    @SerialName("activity_name_en") val activityNameEn: String? = null,
    val type: String = "activity",
    @SerialName("requires_checkin") val requiresCheckin: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,

    /**
     * How many participants have been stamped in at this base, or null when the server has
     * not told us.
     *
     * Null rather than zero, and the distinction is the whole point of the field: a base
     * nobody has reached yet and a server that does not send this number are very different
     * claims, and "0 checked in" for the second is a lie the card would have no way of
     * knowing it was telling. The endpoint does not send it today, so it decodes as null
     * everywhere and the card simply leaves the line out; the day `/wbw/checkpoints` grows
     * a `checkin_count`, every card starts showing it with no change on this side.
     */
    @SerialName("checkin_count") val checkinCount: Int? = null,
) {
    /** True when this row can actually be put on a map. */
    val located: Boolean get() = lat != null && lng != null

    /**
     * The name to show, given whether the app is running in Thai.
     *
     * Thai is the source column and English is the translation, so Thai falls back to
     * itself and English falls back to Thai — an untranslated base shows its real name
     * rather than an empty label.
     */
    fun displayName(thai: Boolean): String =
        if (thai) name else nameEn?.takeIf { it.isNotBlank() } ?: name

    /** The activity, in the same way. Null when the base has none — the finish, say. */
    fun displayActivity(thai: Boolean): String? =
        (if (thai) activityName else activityNameEn?.takeIf { it.isNotBlank() } ?: activityName)
            ?.takeIf { it.isNotBlank() }
}
