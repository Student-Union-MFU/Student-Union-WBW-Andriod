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
import th.ac.mfu.su.wbw.ui.theme.DangerDark
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.TicketCreamPaper
import th.ac.mfu.su.wbw.ui.theme.WbwGreenDark
import th.ac.mfu.su.wbw.ui.theme.ThemeMode
import th.ac.mfu.su.wbw.ui.theme.glass
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.ApiResult
import th.ac.mfu.su.wbw.ui.theme.WbwAvatars
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

    // The avatar, read from the cached profile so the picker opens with the right one
    // already marked rather than filling in a moment later.
    val profile = remember { (context.applicationContext as WbwApplication).container.profileRepository }
    val scope = rememberCoroutineScope()
    var avatar by remember { mutableStateOf(profile.cachedMe()?.avatar) }
    var avatarSaving by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // The cache can be a day old and an avatar chosen on another device would not be in
        // it, so ask — but only to correct the picker, never to blank it.
        (profile.me() as? ApiResult.Success)?.let { avatar = it.data.avatar }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(contentPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // header
        //
        // The eyebrow above the title used to be the *same word* as the title, uppercased —
        // "SETTINGS" over "Settings" — in `colors.accent`, which is near-black in light
        // theme on a backdrop that is dark in both. So it was a duplicate label that half
        // the users could not read. The back button is a glass chip now, matching the
        // profile button it is the counterpart to, and picking up a real tap target on the
        // way: it was a bare 30dp icon.
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .glass(ChipShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                    .clickableTap(onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), tint = colors.onBackdrop, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, color = colors.onBackdrop)
        }

        // language
        SectionLabel(stringResource(R.string.settings_language))
        LanguageRow(settings)

        // avatar
        SectionLabel(stringResource(R.string.settings_avatar))
        AvatarPicker(
            selected = avatar,
            saving = avatarSaving,
            failed = avatarError,
        ) { key ->
            // Optimistic: the grid marks the new one immediately and puts the old one back
            // if the server refuses. A picker that waits a round trip before showing what
            // was tapped feels broken on a hill, and the failure is recoverable — the value
            // is one of twelve, not something typed.
            val previous = avatar
            avatar = key
            avatarSaving = true
            avatarError = false
            scope.launch {
                when (profile.setAvatar(key)) {
                    is ApiResult.Success -> Unit
                    is ApiResult.Error -> {
                        avatar = previous
                        avatarError = true
                    }
                }
                avatarSaving = false
            }
        }

        // appearance
        SectionLabel(stringResource(R.string.settings_appearance))
        Column(Modifier.fillMaxWidth().panel().padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(stringResource(R.string.settings_theme_light), Icons.Outlined.LightMode, themeMode == ThemeMode.LIGHT, Modifier.weight(1f)) { settings.setThemeMode(ThemeMode.LIGHT) }
                ThemeOption(stringResource(R.string.settings_theme_dark), Icons.Outlined.DarkMode, themeMode == ThemeMode.DARK, Modifier.weight(1f)) { settings.setThemeMode(ThemeMode.DARK) }
                ThemeOption(stringResource(R.string.settings_theme_auto), Icons.Outlined.Brightness4, themeMode == ThemeMode.AUTO, Modifier.weight(1f)) { settings.setThemeMode(ThemeMode.AUTO) }
            }
            Text(stringResource(R.string.settings_appearance_hint), color = colors.onBackdropMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        }

        // notifications
        SectionLabel(stringResource(R.string.settings_notifications))
        Column(Modifier.fillMaxWidth().panel()) {
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
        Column(Modifier.fillMaxWidth().panel()) {
            LinkRow(Icons.Outlined.Info, stringResource(R.string.settings_about)) {}
            Divider()
            LinkRow(Icons.Outlined.HelpOutline, stringResource(R.string.settings_help)) {}
            Divider()
            Row(
                Modifier.fillMaxWidth().clickableTap(onLogout).padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // DangerDark, not `colors.danger`. The themed pair exists so the warning
                // reads on a card that follows the theme; this panel is dark in both, and
                // the light-theme oxblood goes muddy on it.
                Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = DangerDark, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.profile_action_logout), color = DangerDark, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyLarge)
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
        Modifier.fillMaxWidth().panel().padding(start = 15.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.settings_display_language), color = colors.onBackdrop, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.clip(RoundedCornerShape(13.dp)).background(colors.onBackdrop.copy(alpha = 0.07f)).padding(3.dp),
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
    // Selection is a lift in the glass, not a filled accent slab — the same move the nav
    // bar's indicator makes. The accent fill needed its own ink colour per theme to stay
    // readable, which is two more ways for this one control to go wrong.
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) colors.onBackdrop.copy(alpha = 0.18f) else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, GlassSheerBorder, shape) else Modifier)
            .clickableTap(onClick)
            .padding(horizontal = 15.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) colors.onBackdrop else colors.onBackdropMuted,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
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
    // The stage chips' states, at a different size: a fill and a hairline that both step
    // up when selected. The 2dp accent outline this had was the heaviest border in the
    // app, on the least consequential choice in it.
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape)
            .background(colors.onBackdrop.copy(alpha = if (selected) 0.14f else 0.05f))
            .border(1.dp, colors.onBackdrop.copy(alpha = if (selected) 0.34f else 0.10f), shape)
            .clickableTap(onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, tint = if (selected) colors.onBackdrop else colors.onBackdropMuted, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = if (selected) colors.onBackdrop else colors.onBackdropMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Medium,
        )
    }
}

@Composable
private fun NotiToggle(settings: AppSettings, key: String, title: String, desc: String, default: Boolean) {
    val colors = wbwColors
    var checked by rememberSaveable(key) { mutableStateOf(settings.notificationEnabled(key, default)) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        // One weighted child, filling. This used to be `weight(1f, fill = false)` followed
        // by a `Spacer(weight(1f))`, and the pair did two visible kinds of damage.
        //
        // The two weights split the row's free space evenly, so the label got half a row
        // to live in and "Daily activity summary" wrapped onto a second line with an inch
        // of empty glass beside it. And because `fill = false` lets the column keep only
        // what it measures, the width it gave back was never redistributed — so the switch
        // started wherever that row's text happened to end, and the four switches in this
        // panel sat at four different distances from the edge.
        //
        // Filling the whole gap with the text column fixes both at once: the title gets
        // the full width and stays on one line, and every switch is pushed flush right, so
        // they line up down the panel.
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.onBackdrop, style = MaterialTheme.typography.bodyLarge)
            Text(desc, color = colors.onBackdropMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it; settings.setNotificationEnabled(key, it) },
            colors = SwitchDefaults.colors(
                // The status green — the app's one "this is on" colour, already used for
                // progress. `accentSoft` was standing in for it, and in light theme that
                // is a dark olive, which on a dark panel is an off switch that looks on.
                checkedThumbColor = TicketCreamPaper,
                checkedTrackColor = WbwGreenDark.copy(alpha = 0.75f),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = colors.onBackdrop.copy(alpha = 0.8f),
                uncheckedTrackColor = colors.onBackdrop.copy(alpha = 0.12f),
                uncheckedBorderColor = GlassSheerBorder,
            ),
        )
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = wbwColors
    Row(
        Modifier.fillMaxWidth().clickableTap(onClick).padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = colors.onBackdropMuted, modifier = Modifier.size(18.dp))
        Text(label, color = colors.onBackdrop, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = colors.onBackdropMuted, modifier = Modifier.size(18.dp))
    }
}

/**
 * A section heading, on the backdrop rather than on a panel.
 *
 * Which is exactly why it cannot use `colors.accent`: that is near-black in light theme
 * and these sit straight on the dark artwork, so the headings were all but invisible for
 * anyone not in dark mode. Same trap as the greeting on Home and the eyebrow above.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), color = wbwColors.onBackdropMuted, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, letterSpacing = 1.8.sp,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
}

/**
 * The panel: one pane of the app's glass, the same as an event card or the nav bar.
 *
 * These used the themed default, which is cream at 86% in light mode — five opaque slabs
 * stacked down a screen whose ground is a photograph. The corner comes down with it, for
 * the reason the event cards' did: a radius that suits a small pane looks inflated on
 * something the full width of the screen.
 */
private fun Modifier.panel(): Modifier =
    glass(RoundedCornerShape(PanelRadius), fill = GlassSheer, border = GlassSheerBorder)

private val PanelRadius = 18.dp

/** The header chip, matching the profile button on Home. */
private val ChipShape = RoundedCornerShape(14.dp)

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassSheerBorder))
}

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

/**
 * The twelve avatars, as a grid of taps.
 *
 * A grid rather than a row that scrolls: twelve is few enough to show at once, and a
 * horizontal strip hides half the choices behind a gesture nobody is told about. Wrapping
 * means the count can change without the layout needing to.
 *
 * The selected one is marked by the app's own green fill and a hairline, the same pair the
 * rating buttons use — a tick or a ring would be a third idiom for "this one".
 */
@Composable
private fun AvatarPicker(
    selected: String?,
    saving: Boolean,
    failed: Boolean,
    onPick: (String) -> Unit,
) {
    val colors = wbwColors
    Column(Modifier.fillMaxWidth().panel().padding(14.dp)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WbwAvatars.all.forEach { (key, glyph) ->
                val isOn = key == selected
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(ChipShape)
                        .background(
                            if (isOn) WbwGreenDark.copy(alpha = 0.55f)
                            else WbwGreenDark.copy(alpha = 0.14f),
                        )
                        .border(
                            1.dp,
                            if (isOn) colors.onBackdrop.copy(alpha = 0.45f) else GlassSheerBorder,
                            ChipShape,
                        )
                        // Ignored while a save is in flight, so a fast run along the row
                        // does not queue four PATCHes whose order decides the winner.
                        .clickableTap { if (!saving) onPick(key) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(glyph, fontSize = 24.sp)
                }
            }
        }
        Text(
            stringResource(
                if (failed) R.string.settings_avatar_failed else R.string.settings_avatar_hint,
            ),
            color = if (failed) colors.danger else colors.onBackdropMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

