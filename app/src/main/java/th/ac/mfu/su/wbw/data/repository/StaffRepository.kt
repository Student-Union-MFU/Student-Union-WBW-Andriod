package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.SosStaffCase

/**
 * The staff side of the emergency system.
 *
 * No cache, for the same reason [SosRepository] has none and a stronger one: this is a list
 * of people who may be hurt right now. A stale copy of it is not a helpful head start, it
 * is a responder walking to a case that was closed twenty minutes ago while a live one sits
 * unread underneath.
 */
class StaffRepository(private val api: WbwApi) {

    /**
     * One long-poll round of the case feed.
     *
     * [since] is echoed back from the newest row already held — see [SosStaffCase.cursor]
     * for why it is a compound value rather than a timestamp.
     */
    suspend fun sosFeed(since: String, waitSeconds: Int): ApiResult<List<SosStaffCase>> =
        apiCall { api.staffSosFeed(since, waitSeconds) }

    suspend fun ack(id: Long): ApiResult<SosStaffCase> = apiCall { api.ackSos(id) }
}
