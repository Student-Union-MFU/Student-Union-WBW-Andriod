package th.ac.mfu.su.wbw.data.repository

import kotlinx.serialization.builtins.ListSerializer
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.remote.dto.Notification
import th.ac.mfu.su.wbw.data.remote.dto.NotificationPublic

/**
 * Notifications. The authenticated feed (GET /notifications) is per-participant;
 * the public feed (GET /notifications/public) needs no login.
 *
 * Note: real *push* delivery (FCM) is not yet implemented on the Go backend, so
 * for now the app pulls this list. Wire FCM here when the backend sends pushes.
 */
class NotificationRepository(
    private val api: WbwApi,
    private val cache: ResponseCache,
) {

    private val listSerializer = ListSerializer(Notification.serializer())

    /** The feed as of the last successful fetch — see [ProfileRepository.cachedMe]. */
    fun cachedMine(): List<Notification>? =
        cache.read(ResponseCache.KeyNotifications, listSerializer)?.let(::announcementsOnly)

    suspend fun mine(): ApiResult<List<Notification>> =
        apiCall { api.notifications() }
            // Cached before filtering, so the stored copy stays a faithful record of what
            // the server said. What this app chooses not to display is a decision for the
            // read path, not something to bake into the cache — a later build that wants
            // these rows should not have to wait for the cache to turn over.
            .onSuccess { cache.write(ResponseCache.KeyNotifications, listSerializer, it) }
            .let { result ->
                when (result) {
                    is ApiResult.Success -> ApiResult.Success(announcementsOnly(result.data))
                    is ApiResult.Error -> result
                }
            }

    /**
     * Announcements only — SOS rows are dropped.
     *
     * The server writes a `sos` notification to the whole group when one of its members
     * raises an emergency, and it is genuinely useful *as a push*: a phone buzzing on a
     * hillside is how somebody's friends find out. It does not belong in this list, which
     * is the event's announcement board — start times, weather calls, where the water is.
     *
     * The two have opposite half-lives. An announcement is worth reading an hour later; an
     * emergency is either being dealt with right now or it is over, and the person who
     * raised it has a whole screen of their own about it. Leaving the row here means the
     * board fills with the stale end of other people's resolved emergencies, and — since
     * the raiser is in the group too — that somebody who has just been helped scrolls past
     * a permanent notice of the worst part of their day.
     *
     * Filtered on the client rather than removed from the server, because the same rows
     * feed the iOS build, which may well want them.
     */
    private fun announcementsOnly(items: List<Notification>): List<Notification> =
        items.filterNot { it.type.equals(SosType, ignoreCase = true) }

    /** Not cached: only the login screen could use it, and it does not. */
    suspend fun public(): ApiResult<List<NotificationPublic>> = apiCall { api.publicNotifications() }

    private companion object {
        /** `notification.type` the server writes for an emergency — see `notifyGroup`. */
        const val SosType = "sos"
    }
}
