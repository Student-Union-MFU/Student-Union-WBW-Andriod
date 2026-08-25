package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a participant says about a base once they have been checked in at it.
 *
 * Four questions rather than one, because "how was it" collapses everything a base is into
 * a single number and tells the organisers nothing they can act on. A base can have a fine
 * view and a dull activity, or good staff and nowhere to sit; asking separately is what
 * makes the answer useful next year.
 *
 * [clientId] is generated on the device before the first attempt and reused on every retry.
 * The server has a unique index on it, so a submission that times out and is sent again
 * lands on the same row rather than becoming a second opinion.
 */
@Serializable
data class FeedbackRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("checkpoint_id") val checkpointId: Int,
    /** Overall, 1–5. The one the server has always had, and the only one it requires. */
    val rating: Int,
    @SerialName("rating_scenery") val ratingScenery: Int? = null,
    /**
     * The space itself — shade, seating, room to stand — as opposed to the view.
     *
     * The server has no `rating_area` column yet and its decoder does not reject fields it
     * does not know, so this rides along and is dropped until the column exists. Sending it
     * early is the point: the day the migration lands, every phone already in participants'
     * hands starts filling it in, and an event app cannot ask its users to update mid-walk.
     */
    @SerialName("rating_area") val ratingArea: Int? = null,
    @SerialName("rating_activity") val ratingActivity: Int? = null,
    @SerialName("rating_staff") val ratingStaff: Int? = null,
    val comment: String? = null,
    @SerialName("device_time") val deviceTime: String,
)

/** One recorded opinion, as the server hands it back. */
@Serializable
data class CheckinFeedback(
    val id: Long = 0,
    @SerialName("checkpoint_id") val checkpointId: Int = 0,
    val rating: Int = 0,
    @SerialName("rating_scenery") val ratingScenery: Int? = null,
    @SerialName("rating_area") val ratingArea: Int? = null,
    @SerialName("rating_activity") val ratingActivity: Int? = null,
    @SerialName("rating_staff") val ratingStaff: Int? = null,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

/**
 * What the walk as a whole was like, asked once at the end rather than per base.
 *
 * The activity question moved here because it is the one thing on the old per-base form
 * that a participant cannot answer honestly while standing at a base: what they did there
 * is one item in a day of them, and it only becomes comparable once the day is over.
 *
 * Not tied to a checkpoint, which is why it needs an endpoint of its own —
 * `checkin_feedback` is keyed by `checkpoint_id` and there is no honest value to put there
 * for an opinion about the route. `POST /wbw/me/event-feedback` does not exist yet; until
 * it does this fails and the gate lets the participant through rather than trapping them
 * behind an endpoint the server has never heard of.
 */
@Serializable
data class EventFeedbackRequest(
    @SerialName("client_id") val clientId: String,
    /** The walk overall, 1–5. */
    val rating: Int,
    @SerialName("rating_activity") val ratingActivity: Int? = null,
    val comment: String? = null,
    @SerialName("device_time") val deviceTime: String,
)

/** The event-level opinion, as the server hands it back. */
@Serializable
data class EventFeedback(
    val id: Long = 0,
    val rating: Int = 0,
    @SerialName("rating_activity") val ratingActivity: Int? = null,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
