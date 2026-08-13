package th.ac.mfu.su.wbw.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.WbwApplication
import th.ac.mfu.su.wbw.data.local.AppSettings
import th.ac.mfu.su.wbw.ui.theme.Ink
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.ThemeMode
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    val colors = wbwColors
    val context = LocalContext.current
    val settings = remember { (context.applicationContext as WbwApplication).container.appSettings }
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // header
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clickableTap(onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), tint = colors.onBackdrop, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(stringResource(R.string.settings_title).uppercase(), color = colors.gold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.sp)
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, color = colors.onBackdrop)
            }
        }

        // language
        SectionLabel(stringResource(R.string.settings_language))
        LanguageRow(settings)

        // appearance
        SectionLabel(stringResource(R.string.settings_appearance))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(PanelCorner)).padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(stringResource(R.string.settings_theme_light), Icons.Outlined.LightMode, themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) { settings.setThemeMode(ThemeMode.LIGHT) }
                ThemeOption(stringResource(R.string.settings_theme_dark), Icons.Outlined.DarkMode, themeMode == ThemeMode.DARK, Modifier.weight(1f)) { settings.setThemeMode(ThemeMode.DARK) }
                ThemeOption(stringResource(R.string.settings_theme_auto), Icons.Outlined.Brightness4, themeMode == ThemeMode.AUTO, Modifier.weight(1f)) { settings.setThemeMode(ThemeMode.AUTO) }
            }
            Text(stringResource(R.string.settings_appearance_hint), color = colors.textMuted, fontSize = 10.5.sp, modifier = Modifier.padding(top = 9.dp))
        }

        // notifications
        SectionLabel(stringResource(R.string.settings_notifications))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(PanelCorner))) {
            NotiToggle(settings, "announcements", stringResource(R.string.settings_noti_announcements), stringResource(R.string.settings_noti_announcements_desc), true)
            Divider()
            NotiToggle(settings, "nearby", stringResource(R.string.settings_noti_nearby), stringResource(R.string.settings_noti_nearby_desc), true)
            Divider()
            NotiToggle(settings, "chat", stringResource(R.string.settings_noti_chat), stringResource(R.string.settings_noti_chat_desc), false)
            Divider()
            NotiToggle(settings, "daily", stringResource(R.string.settings_noti_daily), stringResource(R.string.settings_noti_daily_desc), true)
        }

        // general
        SectionLabel(stringResource(R.string.settings_general))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(PanelCorner))) {
            LinkRow(Icons.Outlined.Info, stringResource(R.string.settings_about)) {}
            Divider()
            LinkRow(Icons.Outlined.HelpOutline, stringResource(R.string.settings_help)) {}
            Divider()
            Row(
                Modifier.fillMaxWidth().clickableTap(onLogout).padding(15.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = colors.danger, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.profile_action_logout), color = colors.danger, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LanguageRow(settings: AppSettings) {
    val colors = wbwColors
    val context = LocalContext.current
    val stored by settings.language.collectAsStateWithLifecycle()
    val currentLang = if (stored.isNotBlank()) stored else LocalConfiguration.current.locales[0].language
    val isThai = currentLang == "th"

    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(PanelCorner)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.settings_display_language), color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(colors.textMuted.copy(alpha = 0.12f)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            LangSegment(stringResource(R.string.settings_lang_english), !isThai) { setLang(context, settings, "en") }
            LangSegment(stringResource(R.string.settings_lang_thai), isThai) { setLang(context, settings, "th") }
        }
    }
}

@Composable
private fun LangSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = wbwColors
    Box(
        Modifier.clip(RoundedCornerShape(9.dp)).background(if (selected) colors.gold else Color.Transparent).clickableTap(onClick).padding(horizontal = 15.dp, vertical = 5.dp),
    ) {
        Text(label, color = if (selected) Ink else colors.textMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun setLang(context: Context, settings: AppSettings, tag: String) {
    if (settings.language.value == tag) return
    settings.setLanguage(tag)
    context.findActivity()?.recreate()
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = wbwColors
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) colors.gold.copy(alpha = 0.2f) else colors.textMuted.copy(alpha = 0.06f))
            .then(if (selected) Modifier.border2(colors.gold, shape) else Modifier)
            .clickableTap(onClick).padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = if (selected) colors.gold else colors.textMuted, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) colors.textPrimary else colors.textMuted, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun NotiToggle(settings: AppSettings, key: String, title: String, desc: String, default: Boolean) {
    val colors = wbwColors
    var checked by rememberSaveable(key) { mutableStateOf(settings.notificationEnabled(key, default)) }
    Row(
        Modifier.fillMaxWidth().padding(15.dp, 13.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(desc, color = colors.textMuted, fontSize = 10.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it; settings.setNotificationEnabled(key, it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.goldSoft,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.textMuted.copy(alpha = 0.3f),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = wbwColors
    Row(
        Modifier.fillMaxWidth().clickableTap(onClick).padding(15.dp, 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = colors.gold, modifier = Modifier.size(18.dp))
        Text(label, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), color = wbwColors.gold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 7.dp),
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(wbwColors.textMuted.copy(alpha = 0.1f)))
}

private fun Modifier.border2(color: Color, shape: RoundedCornerShape): Modifier =
    border(2.dp, color, shape)

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
