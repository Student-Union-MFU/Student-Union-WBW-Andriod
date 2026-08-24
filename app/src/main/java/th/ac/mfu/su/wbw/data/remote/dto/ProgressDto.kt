package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Check-in progress — `model.CheckinProgress` on the server, and the thing the bloom on
 * Home is actually made of.
 */

/**
 * One checkpoint this participant has been stamped in at.
 *
 * [at] is when it happened, and it is what makes the list an ordering rather than a set:
 * the bloom opens a petal per base *in the order they were collected*, which is not the
 * same as trail order — somebody who joins late and walks the loop backwards still earns
 * their petals in the order they earned them.
 *
 * [answered], [rating] and [comment] belong to the post-base feedback prompt rather than to
 * the bloom. They are decoded here because the endpoint sends them and a second call to
 * find out "have I already rated this base" would be a waste of a request on a hill.
 */
@Serializable
data class CheckinProgressItem(
    @SerialName("checkpoint_id") val checkpointId: Int,
    val name: String,
    @SerialName("activity_name") val activityName: String? = null,
    val sequence: Int? = null,
    val at: String = "",
    val answered: Boolean = false,
    val rating: Int? = null,
    val comment: String? = null,
)

/**
 * How far along this participant is.
 *
 * [total] comes from the database on every call rather than being the constant 8 the app
 * used to assume. Admins add and remove checkpoints during the event through
 * `/wbw/admin/checkpoints`, and a hard-coded denominator turns "5 of 8" into a lie the
 * moment somebody does — the server's own comment on this field says as much.
 *
 * [emergencyPhone] rides along because this endpoint is polled about once a minute while
 * the app is open, which makes it the natural place to keep the event's central number
 * fresh on the device. The SOS screen's fallback "call instead" button needs that number
 * *before* the emergency, not after — [SosCase] also carries it, but that copy only arrives
 * once a case has already been raised, which is too late to be the only source.
 */
@Serializable
data class CheckinProgress(
    val total: Int = 0,
    @SerialName("checked_in") val checkedIn: List<CheckinProgressItem> = emptyList(),
    @SerialName("emergency_phone") val emergencyPhone: String = "",
) {
    val count: Int get() = checkedIn.size

    /** Guarded against a zero denominator: an event with no checkpoints is 0%, not a crash. */
    val fraction: Float get() = if (total <= 0) 0f else (count.toFloat() / total).coerceIn(0f, 1f)

    /** True once every base has been collected. */
    val complete: Boolean get() = total > 0 && count >= total
}
