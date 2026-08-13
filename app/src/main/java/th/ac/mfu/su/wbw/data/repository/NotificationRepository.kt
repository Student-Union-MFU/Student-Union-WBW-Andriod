package th.ac.mfu.su.wbw.data.repository

import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.core.network.apiCall
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
class NotificationRepository(private val api: WbwApi) {

    suspend fun mine(): ApiResult<List<Notification>> = apiCall { api.notifications() }

    suspend fun public(): ApiResult<List<NotificationPublic>> = apiCall { api.publicNotifications() }
}
