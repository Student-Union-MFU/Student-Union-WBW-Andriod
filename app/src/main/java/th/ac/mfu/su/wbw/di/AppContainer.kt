package th.ac.mfu.su.wbw.di

import android.content.Context
import th.ac.mfu.su.wbw.core.network.NetworkModule
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.OpenMeteoApi
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.repository.AuthRepository
import th.ac.mfu.su.wbw.data.repository.ConditionsRepository
import th.ac.mfu.su.wbw.data.repository.NotificationRepository
import th.ac.mfu.su.wbw.data.repository.ProfileRepository

/**
 * Hand-rolled dependency container — one instance per process, created in
 * [th.ac.mfu.su.wbw.WbwApplication]. Kept deliberately small; swap for Hilt if
 * the graph grows. Everything below is a lazily-built singleton.
 */
class AppContainer(context: Context) {

    val sessionStore: SessionStore = SessionStore(context.applicationContext)

    val appSettings: AppSettings = AppSettings(context.applicationContext)

    /**
     * Not lazy: [AuthRepository] must be able to clear it on logout, and the view models
     * read it synchronously while composing their first frame. Building it costs one
     * SharedPreferences handle.
     */
    val responseCache: ResponseCache = ResponseCache(context.applicationContext)

    private val api: WbwApi by lazy { NetworkModule.createApi(sessionStore) }

    /**
     * Deliberately a second client, not the one above — it must never carry the
     * participant's bearer token to a third party. See `NetworkModule.createOpenMeteoApi`.
     */
    private val openMeteoApi: OpenMeteoApi by lazy { NetworkModule.createOpenMeteoApi() }

    val authRepository: AuthRepository by lazy { AuthRepository(api, sessionStore, responseCache) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(api, responseCache) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(api, responseCache) }
    val conditionsRepository: ConditionsRepository by lazy { ConditionsRepository(openMeteoApi, responseCache) }
}
