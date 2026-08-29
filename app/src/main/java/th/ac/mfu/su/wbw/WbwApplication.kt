package th.ac.mfu.su.wbw

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.di.AppContainer
import th.ac.mfu.su.wbw.walk.WalkTracker

/** Process entry point — builds the [AppContainer] and primes the auth token. */
class WbwApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        container = AppContainer(this)
        // Before any screen exists, so the map's first frame already shows a walk the last
        // process was in the middle of rather than an empty HUD that fills in a beat later.
        WalkTracker.restore()
        // Load the persisted token into the interceptor's synchronous cache so
        // the first authed request after a cold start carries it.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.sessionStore.prime()
        }
    }

    companion object {
        /**
         * Application context for non-UI layers that need string resources
         * (e.g. [th.ac.mfu.su.wbw.core.network.apiCall]). Safe to hold — it is the
         * application context, not an Activity, so it never leaks.
         */
        lateinit var appContext: Context
            private set
    }
}
