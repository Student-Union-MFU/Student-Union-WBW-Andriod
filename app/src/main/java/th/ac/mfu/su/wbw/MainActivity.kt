package th.ac.mfu.su.wbw

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.ui.WbwApp
import th.ac.mfu.su.wbw.ui.theme.WbwTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply the stored app language before any resources resolve.
        super.attachBaseContext(AppSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val settings = (application as WbwApplication).container.appSettings
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            WbwTheme(themeMode = themeMode) {
                WbwApp()
            }
        }
    }
}
