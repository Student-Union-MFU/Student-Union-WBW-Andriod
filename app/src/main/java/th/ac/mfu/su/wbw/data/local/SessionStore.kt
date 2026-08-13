package th.ac.mfu.su.wbw.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wbw_session")

/** The stored auth session — mirrors the AuthUser the backend returns, plus the token. */
data class Session(
    val token: String,
    val userId: String,
    val username: String,
    val role: String,
) {
    val isStaff: Boolean get() = role == "admin" || role == "staff"
}

/**
 * Persists the login session in DataStore and keeps a synchronous copy of the
 * token for [th.ac.mfu.su.wbw.core.network.AuthInterceptor], which runs on
 * OkHttp's thread and can't suspend. Call [prime] once at app start to load the
 * cached token before the first request.
 */
class SessionStore(private val context: Context) {

    @Volatile
    private var cachedToken: String? = null

    /** Synchronous token accessor for the OkHttp interceptor. Null when logged out. */
    fun currentToken(): String? = cachedToken

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN] ?: return@map null
        Session(
            token = token,
            userId = prefs[KEY_USER_ID].orEmpty(),
            username = prefs[KEY_USERNAME].orEmpty(),
            role = prefs[KEY_ROLE] ?: "participant",
        )
    }

    /** Load the persisted token into the synchronous cache. */
    suspend fun prime() {
        cachedToken = context.dataStore.data.map { it[KEY_TOKEN] }.first()
    }

    suspend fun save(session: Session) {
        cachedToken = session.token
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = session.token
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_USERNAME] = session.username
            prefs[KEY_ROLE] = session.role
        }
    }

    suspend fun clear() {
        cachedToken = null
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_ROLE = stringPreferencesKey("role")
    }
}
