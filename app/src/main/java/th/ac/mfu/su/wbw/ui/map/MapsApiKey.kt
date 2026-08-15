package th.ac.mfu.su.wbw.ui.map

import android.content.Context
import android.content.pm.PackageManager

/**
 * The Maps key, read back out of the manifest at runtime.
 *
 * The secrets plugin already injected it into the `com.google.android.geo.API_KEY`
 * meta-data for the native SDK; the 3D WebView and the Places SDK need the same string in
 * code. Reading it here rather than adding a second `BuildConfig` field keeps one source of
 * truth — the key lives in `local.properties` and reaches everything through the manifest.
 *
 * Empty when no key is configured (a keyless checkout), so callers can degrade rather than
 * feed a blank key to a WebView.
 */
fun Context.mapsApiKey(): String {
    val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    return info.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
}
