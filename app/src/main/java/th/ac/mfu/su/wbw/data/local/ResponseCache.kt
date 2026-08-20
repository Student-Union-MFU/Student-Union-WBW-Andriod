package th.ac.mfu.su.wbw.data.local

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * The last good copy of what the server said, kept on disk.
 *
 * The point is the *first frame*. Every screen used to open on `UiState.Loading`, which
 * meant that opening the app — or the pass, in front of a marshal — showed a spinner while
 * a round trip completed, every single time, to be replaced by data that had not changed
 * since yesterday. With a cache the screen opens on what it knew last and quietly corrects
 * itself when the network answers. Nothing about the request changes; what changes is that
 * the participant is not made to watch it.
 *
 * This matters more here than in most apps, because of where the app is used. Half of this
 * event happens on a hill with one bar of signal: a fetch that takes eight seconds, or
 * never finishes, is normal rather than exceptional. A cached pass is the difference
 * between showing a marshal your bib number and showing them a spinner.
 *
 * [SharedPreferences][android.content.SharedPreferences] rather than DataStore, matching
 * [AppSettings], because the reads have to be **synchronous**: a view model seeds its state
 * from here inside `init`, before the first composition, and anything suspending would let
 * a `Loading` frame through first — which is the exact flicker this exists to remove. The
 * payloads are a few kilobytes of JSON, so the one-off disk read on first access is not
 * worth an asynchronous API.
 *
 * **Everything in here belongs to the logged-in participant, so [clear] is not optional.**
 * It is called from `AuthRepository` on both login and logout: leave it behind and the next
 * person to use a shared phone opens the app on the previous participant's name, bib,
 * group and medical details, for as long as the first fetch takes. That window is short and
 * that makes it worse, not better — it is exactly long enough to be seen and not long
 * enough to be reported.
 */
class ResponseCache(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Lenient in the same three ways [th.ac.mfu.su.wbw.core.network.NetworkModule] is, and
     * for a stronger reason: this reads JSON written by an *older build of the app*, so a
     * field added or removed since must not throw.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * The cached value, or null if absent or unreadable.
     *
     * Unreadable is treated exactly like absent, never as an error. A cache that has gone
     * stale against a changed model is a cache miss — one wasted fetch — whereas a throw
     * here would crash the app on launch after an update, with no way out but reinstalling.
     */
    fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        val raw = runCatching { json.encodeToString(serializer, value) }.getOrNull() ?: return
        prefs.edit()
            .putString(key, raw)
            .putLong(key + AgeSuffix, System.currentTimeMillis())
            .apply()
    }

    /**
     * How long ago [key] was written, or null if it never was.
     *
     * Null is also returned for a timestamp in the future, which happens when the phone's
     * clock is moved backwards — treating that as "never written" refetches once, where the
     * arithmetic would otherwise return a negative age that reads as fresh forever.
     */
    fun ageMillis(key: String): Long? {
        val at = prefs.getLong(key + AgeSuffix, 0L)
        if (at <= 0L) return null
        val age = System.currentTimeMillis() - at
        return if (age < 0L) null else age
    }

    /** Drops everything. See the note on the class about why this is called on logout. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "wbw_cache"
        private const val AgeSuffix = ".at"

        const val KeyMe = "me"
        const val KeyNotifications = "notifications"
        const val KeyConditions = "conditions"
        const val KeyChat = "chat"
    }
}
