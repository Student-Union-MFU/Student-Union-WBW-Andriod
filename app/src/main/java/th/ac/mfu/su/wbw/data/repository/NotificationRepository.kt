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
    fun cachedMine(): List<Notification>? = cache.read(ResponseCache.KeyNotifications, listSerializer)

    suspend fun mine(): ApiResult<List<Notification>> =
        apiCall { api.notifications() }.onSuccess { cache.write(ResponseCache.KeyNotifications, listSerializer, it) }

    /** Not cached: only the login screen could use it, and it does not. */
    suspend fun public(): ApiResult<List<NotificationPublic>> = apiCall { api.publicNotifications() }
}
