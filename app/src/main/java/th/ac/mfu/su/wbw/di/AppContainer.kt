package th.ac.mfu.su.wbw.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.NetworkModule
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.data.local.ResponseCache
import th.ac.mfu.su.wbw.data.local.SessionStore
import th.ac.mfu.su.wbw.data.remote.OpenMeteoApi
import th.ac.mfu.su.wbw.data.remote.WbwApi
import th.ac.mfu.su.wbw.data.repository.AuthRepository
import th.ac.mfu.su.wbw.data.repository.ChatRepository
import th.ac.mfu.su.wbw.data.repository.ConditionsRepository
import th.ac.mfu.su.wbw.data.repository.NotificationRepository
import th.ac.mfu.su.wbw.data.repository.ProfileRepository
import th.ac.mfu.su.wbw.data.repository.CheckpointRepository
import th.ac.mfu.su.wbw.data.repository.ProgressRepository
import th.ac.mfu.su.wbw.data.repository.SosRepository
import th.ac.mfu.su.wbw.data.repository.StaffRepository

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

    /**
     * Outlives every screen, on purpose. A session can be refused by the answer to a
     * request whose caller is already gone — a screen closed mid-flight, or the walk
     * tracking service posting from the background with no UI at all — and signing out has
     * to finish regardless of who is still there to see it.
     */
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val api: WbwApi by lazy {
        // `authRepository` is resolved inside the lambda, not captured here: it is built
        // from `api`, so touching it while `api` is still being constructed would deadlock
        // on the lazy. By the time a response can arrive, both exist.
        NetworkModule.createApi(sessionStore) {
            authScope.launch { authRepository.onTokenRejected() }
        }
    }

    /**
     * Deliberately a second client, not the one above — it must never carry the
     * participant's bearer token to a third party. See `NetworkModule.createOpenMeteoApi`.
     */
    private val openMeteoApi: OpenMeteoApi by lazy { NetworkModule.createOpenMeteoApi() }

    val authRepository: AuthRepository by lazy { AuthRepository(api, sessionStore, responseCache) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(api, responseCache) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(api, responseCache) }
    val conditionsRepository: ConditionsRepository by lazy { ConditionsRepository(openMeteoApi, responseCache) }
    val chatRepository: ChatRepository by lazy { ChatRepository(api, responseCache) }
    val progressRepository: ProgressRepository by lazy { ProgressRepository(api, responseCache) }
    val checkpointRepository: CheckpointRepository by lazy { CheckpointRepository(api, responseCache) }

    /** No cache argument, on purpose — see [SosRepository]. */
    val sosRepository: SosRepository by lazy { SosRepository(api) }

    /** Only ever built for a staff or admin session — see [StaffRepository]. */
    val staffRepository: StaffRepository by lazy { StaffRepository(api) }
}
