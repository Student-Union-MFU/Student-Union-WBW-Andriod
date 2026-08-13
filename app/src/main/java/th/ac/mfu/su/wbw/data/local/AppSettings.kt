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
