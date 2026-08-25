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
    @SerialName("rating_activity") val ratingActivity: Int? = null,
    @SerialName("rating_staff") val ratingStaff: Int? = null,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
