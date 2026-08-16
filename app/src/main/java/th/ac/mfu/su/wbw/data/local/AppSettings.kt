package th.ac.mfu.su.wbw.data.local

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import th.ac.mfu.su.wbw.ui.theme.ThemeMode
import java.util.Locale

/**
 * Lightweight app preferences (theme + language + notification toggles).
 *
 * Backed by [android.content.SharedPreferences] rather than DataStore because the
 * language must be read *synchronously* in [android.app.Activity.attachBaseContext]
 * to wrap the base context before any resources resolve. In-memory [StateFlow]s
 * drive Compose so theme changes apply live without recreating the activity.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("wbw_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.AUTO.name)!!) }
            .getOrDefault(ThemeMode.AUTO),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    /** "" = follow the device language; otherwise a BCP-47 tag ("en" / "th"). */
    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, "") ?: "")
    val language: StateFlow<String> = _language

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setLanguage(tag: String) {
        _language.value = tag
        prefs.edit().putString(KEY_LANG, tag).apply()
    }

    /**
     * The newest announcement id the participant has already looked at.
     *
     * Tracked here rather than on the server because there is nowhere on the server to
     * put it: the `/wbw` API returns `read_at` on every notification but exposes no
     * endpoint to *set* it, so the app can be told a thing was read and can never say so
     * itself. A bell driven only by `read_at` would light up on the first announcement
     * and stay lit for the rest of the event.
     *
     * So "seen" means "was in the list the last time you opened it": opening the
     * announcements screen advances this to the newest id it showed, and anything above
     * the mark is new. Ids are the table's serial primary key, so larger *is* newer —
     * which is also why the timestamps are not used for this, being strings whose
     * ordering depends on the offset the backend happens to send.
     *
     * `read_at` is still honoured where it is set; this only fills the gap it leaves.
     *
     * A [StateFlow] rather than a plain getter so Home's bell clears the instant the
     * list is opened, instead of on Home's next reload.
     */
    private val _lastSeenNotificationId = MutableStateFlow(prefs.getLong(KEY_LAST_SEEN_NOTI, 0L))
    val lastSeenNotificationId: StateFlow<Long> = _lastSeenNotificationId

    /**
     * Advances the seen mark. Never moves it backwards — a failed or partial fetch that
     * came back with fewer items than last time must not resurrect announcements the
     * participant has already read.
     */
    fun markNotificationsSeen(newestId: Long) {
        if (newestId <= _lastSeenNotificationId.value) return
        _lastSeenNotificationId.value = newestId
        prefs.edit().putLong(KEY_LAST_SEEN_NOTI, newestId).apply()
    }

    fun notificationEnabled(key: String, default: Boolean = true): Boolean =
        prefs.getBoolean("noti_$key", default)

    fun setNotificationEnabled(key: String, value: Boolean) {
        prefs.edit().putBoolean("noti_$key", value).apply()
    }

    /** Synchronous read for attachBaseContext. */
    fun currentLanguageBlocking(): String = prefs.getString(KEY_LANG, "") ?: ""

    companion object {
        private const val PREFS = "wbw_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANG = "language"
        private const val KEY_LAST_SEEN_NOTI = "last_seen_notification_id"

        /**
         * Wrap a base context with the stored app language, if any. Call from
         * [android.app.Activity.attachBaseContext] so all resources resolve in
         * the chosen locale regardless of the device language.
         */
        fun wrap(base: Context): Context {
            val lang = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "") ?: ""
            if (lang.isBlank()) return base
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }
    }
}
