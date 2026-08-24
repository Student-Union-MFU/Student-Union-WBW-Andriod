package th.ac.mfu.su.wbw.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Emergency SOS, matching `internal/model/wbw_sos.go` on the server.
 *
 * The shape of this contract is built around one fact: the person using it is in trouble
 * and may have no signal, no fix, and one hand free. Everything optional below is optional
 * because the server would rather have an incomplete case *now* than a complete one later.
 */

/**
 * What the app sends when the button is held.
 *
 * [clientId] is the idempotency key. The app fires the moment the hold completes — without
 * waiting for GPS — and then sends again with the *same* id once a fix arrives, which the
 * server treats as an update to the one case rather than as a second emergency. That is the
 * whole reason lat/lng/accuracy are nullable: "no position yet" is an ordinary state on
 * this endpoint, not a validation failure.
 *
 * [deviceTime] is the phone's own clock. The server records its own receipt time
 * separately and uses that for the cancel window, so a phone with a wrong clock cannot
 * extend or collapse it.
 *
 * [forOther] separates "I am hurt" from "someone here is hurt". It is not cosmetic — the
 * server refuses to attach the *reporter's* medical history to a case raised for somebody
 * else, since it would be the wrong person's record.
 */
@Serializable
data class SosRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("device_time") val deviceTime: String? = null,
    @SerialName("for_other") val forOther: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    val message: String? = null,
)

/**
 * One case as the person who raised it sees it.
 *
 * [ackedAt] and [ackedByName] are the only things on this screen that matter while waiting:
 * they are the difference between "sent into the dark" and "a named person is coming". The
 * app shows the name the moment it exists.
 *
 * [locSource] says how the server decided where this case is — `gps`, `last_checkin`, or
 * `none` — and is worth surfacing, because "we are guessing from your last checkpoint" is a
 * genuinely different promise from "we have your position".
 *
 * [emergencyPhone] rides on every response so the app's cached copy of the event's central
 * number is corrected without a second call. It can come back empty when the server has no
 * `WBW_EMERGENCY_PHONE` set, which is why the UI falls back rather than showing a blank.
 */
@Serializable
data class SosCase(
    val id: Long = 0,
    @SerialName("for_other") val forOther: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    @SerialName("loc_source") val locSource: String? = null,
    @SerialName("checkpoint_id") val checkpointId: Int? = null,
    @SerialName("checkpoint_name") val checkpointName: String? = null,
    val message: String? = null,
    val resolved: Boolean = false,
    @SerialName("resolve_reason") val resolveReason: String? = null,
    @SerialName("acked_at") val ackedAt: String? = null,
    @SerialName("acked_by_name") val ackedByName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("emergency_phone") val emergencyPhone: String = "",
) {
    /** True once a staff member has said they are on their way. */
    val acknowledged: Boolean get() = ackedAt != null

    /**
     * Whether the server actually knows where this is, as opposed to having inferred it.
     *
     * `last_checkin` is deliberately *not* located: it means the case is pinned to the last
     * checkpoint the participant scanned at, which could be an hour's walk behind them.
     */
    val located: Boolean get() = locSource == "gps"
}
