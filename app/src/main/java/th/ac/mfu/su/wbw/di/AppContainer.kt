package th.ac.mfu.su.wbw.di

import android.content.Context
import th.ac.mfu.su.wbw.core.network.NetworkModule
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.repository.AuthRepository
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

    private val api: WbwApi by lazy { NetworkModule.createApi(sessionStore) }

    val authRepository: AuthRepository by lazy { AuthRepository(api, sessionStore) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(api) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(api) }
}
