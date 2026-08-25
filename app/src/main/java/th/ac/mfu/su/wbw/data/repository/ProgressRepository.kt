package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.CheckinFeedback
import th.ac.mfu.su.wbw.data.remote.dto.CheckinProgress
import th.ac.mfu.su.wbw.data.remote.dto.EventFeedback
import th.ac.mfu.su.wbw.data.remote.dto.EventFeedbackRequest
import th.ac.mfu.su.wbw.data.remote.dto.FeedbackRequest

/**
 * Check-in progress — the bloom's data source.
 *
 * Cached, like the profile and for the same reason: Home's whole point is the bloom, and
 * opening it on a bare seed while a request completes would tell a participant who has
 * collected five bases that they have collected none. A stale five is true until proven
 * otherwise; a spinner in the shape of a flower is not.
 *
 * The response also carries the event's central emergency number, which this deliberately
 * does nothing with yet. Nothing in the app reads it — the SOS screen has no fallback call
 * button — and a setting written but never read is worse than an absent one, because the
 * next person to touch it has to work out whether anything depends on it. Wire it up when
 * that button exists.
 */
class ProgressRepository(
    private val api: WbwApi,
    private val cache: ResponseCache,
) {

    /** Last run's progress, synchronously, so the first frame is already a bloom. */
    fun cached(): CheckinProgress? =
        cache.read(ResponseCache.KeyProgress, CheckinProgress.serializer())

    /**
     * Send an opinion about a base, then re-read progress.
     *
     * The refresh is part of the operation rather than the caller's problem: `answered`
     * lives on the progress feed, and it is what stops the app asking the same question
     * again the next time Home opens.
     */
    suspend fun submitFeedback(body: FeedbackRequest): ApiResult<CheckinFeedback> =
        apiCall { api.submitFeedback(body) }.onSuccess { progress() }

    /**
     * Send the end-of-route opinion, then re-read progress.
     *
     * The refresh is part of the operation for the same reason it is on [submitFeedback]:
     * `event_feedback_answered` lives on the progress feed, and it is what stops the gate
     * asking again.
     */
    suspend fun submitEventFeedback(body: EventFeedbackRequest): ApiResult<EventFeedback> =
        apiCall { api.submitEventFeedback(body) }.onSuccess { progress() }

    suspend fun progress(): ApiResult<CheckinProgress> =
        apiCall { api.myProgress() }
            .onSuccess { cache.write(ResponseCache.KeyProgress, CheckinProgress.serializer(), it) }
}
