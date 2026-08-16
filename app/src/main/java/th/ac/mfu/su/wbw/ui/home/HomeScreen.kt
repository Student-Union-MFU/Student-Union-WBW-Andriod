package th.ac.mfu.su.wbw.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.common.ErrorState
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.common.UiState
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

@Composable
fun HomeScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenSettings: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unread by viewModel.hasUnreadNotifications.collectAsStateWithLifecycle()
    val conditions by viewModel.trailConditions.collectAsStateWithLifecycle()

    // Fires on every entry into composition, which for a NavHost destination means every
    // time Home is returned to — including on the way back from the announcements list.
    // See [HomeViewModel.refreshNotificationMark] for why `init` cannot do this.
    LaunchedEffect(Unit) {
        viewModel.refreshNotificationMark()
        viewModel.refreshConditions()
    }

    when (val s = state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
        is UiState.Success ->
            HomeContent(s.data, contentPadding, onOpenSettings, onOpenNotifications, unread, conditions)
    }
}

@Composable
private fun HomeContent(
    model: HomeUiModel,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    hasUnread: Boolean,
    conditions: th.ac.mfu.su.wbw.data.repository.TrailConditions?,
) {
    val colors = wbwColors

    // How long the bloom keeps breathing after you last touched the screen.
    //
    // The breath is what makes the flower look alive, and it is worth having while
    // somebody is actually looking. What it is not worth is running for the whole walk:
    // a screen with an animation on it renders sixty frames a second forever, and this is
    // a screen people leave open in a pocket. Every touch wakes it for another spell.
    //
    // `touches` is a counter rather than a timestamp so that re-touching restarts the
    // countdown by re-keying the effect, without any clock arithmetic.
    var touches by remember { mutableIntStateOf(0) }
    var breathing by remember { mutableStateOf(true) }
    LaunchedEffect(touches) {
        breathing = true
        delay(BreathIdleMillis)
        breathing = false
    }
    // Not scrollable. Home is a single view now — greeting, bloom, count — and the bloom
    // takes the height that is left, which a scrolling column cannot give it (its content
    // is measured against an unbounded height, so `weight` has nothing to divide).
    Column(
        Modifier
            .fillMaxSize()
            // Presses only, on the Initial pass: the children consume their own taps, and
            // a wake-up has no business waiting to see whether they did. Moves are
            // ignored — a counter that ticked on every pointer sample would recompose the
            // screen through a drag to say something it already knew.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) touches++
                    }
                }
            }
            .statusBarsPadding()
            .padding(contentPadding)
            .padding(horizontal = 18.dp),
    ) {
        // The top strip carries the two corner buttons and nothing else. The greeting
        // belongs below it, in the body: it is content — it names the person and the time
        // of day — and pairing it with the buttons turned it into a header for a screen
        // that has no header.
        //
        // Neither of these is in the nav bar, for the same reason: they are not places you
        // move back and forth between while walking the trail. You open them to read or
        // change one thing and close them again. The pass is the opposite — it is held up
        // at every checkpoint — which is why it is the one that got a permanent slot in
        // the bar rather than a corner up here.
        //
        // Announcements sit on the left and settings on the right — the far corners, so
        // neither is reachable by accident from the other, and the one that can demand
        // attention is the one on the side the eye starts from.
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CornerButton(
                icon = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications_title),
                onClick = onOpenNotifications,
                badge = hasUnread,
            )
            CornerButton(
                // Settings, not the profile pass. The pass moved to the QR button in the
                // nav bar, where its own glyph is — see `QrRoute` in HomeScaffold. That
                // left this corner free for the other thing you open once and close again,
                // and settings had been reachable only by going through the pass first.
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_title),
                onClick = onOpenSettings,
            )
        }

        // greeting — the first thing in the body
        //
        // One greeting, not two. There used to be a "GOOD MORNING" chip above the name,
        // which meant the top of the screen greeted the participant twice in a row and
        // spent a whole line doing it. Between the two, the name is the one worth keeping:
        // it is the half that is about *them*.
        //
        // Nothing is lost with the chip. Its sun-or-moon glyph said which half of the day
        // the app thought it was, and the conditions line immediately below now says that
        // better — with the actual sky and the actual temperature rather than a guess made
        // from the clock.
        // The gap the greeting chip used to occupy, given back as air rather than to
        // another element. With the chip gone the name sat almost against the corner
        // buttons, which put the largest type on the screen in the tightest space on it.
        Column(Modifier.padding(top = 44.dp)) {
            Text(
                stringResource(R.string.home_greeting, model.displayName),
                // onBackdrop, not textPrimary: this sits on the dark backdrop image in
                // both themes, and textPrimary goes near-black in light theme.
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackdrop,
            )

            // Below the name and above the bloom, because it belongs to the same opening
            // paragraph: who you are, then what it is like out there. Absent entirely
            // until it has something to say — a third party's outage does not get to
            // leave a hole or a spinner on the participant's home screen, and the bloom
            // takes the height back by way of its weight.
            if (conditions != null && !conditions.isEmpty) {
                TrailConditionsRow(
                    conditions = conditions,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }

        // The bloom is what Home is for: the one thing on the screen that changes as the
        // trail is walked. It replaced the 3D plant hero, the progress line, the phase
        // track and the next-base card — all four were saying the same number in
        // different shapes, and none of them was worth looking at twice.
        val reached = stageFor(model.checkedInBases, model.totalBases)
        // Tapping a stage previews it; null means "show where I actually am". Preview
        // does not persist — leaving Home and coming back returns you to your own bloom,
        // because this is a peek at what is coming, not a setting.
        var preview by remember(reached) { mutableStateOf<Int?>(null) }
        val shown = preview ?: reached

        Bloom(
            stage = shown,
            ink = colors.onBackdrop,
            breathing = breathing,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
        )

        // The stage strip. Every stage is tappable, including ones not yet earned — those
        // draw as faint silhouettes of the same flower, so the row shows what the trail
        // leads to rather than hiding it behind a number.
        // Sized by weight rather than a fixed dp: six chips big enough to read do not fit
        // a phone at any fixed size — at 62dp they overflow a 393dp screen once the
        // screen padding is taken out. Dividing the row gives each one every pixel that
        // is going spare, and stays right on a narrower handset.
        //
        // The cells butt up against each other: the visible chip is a circle inset inside
        // its cell, so the gap is drawn by the inset rather than by spacing, and every
        // pixel between two circles is still a tap that lands on one of them. Explicit
        // spacing here would only take width away from the drawings — which is what made
        // the row hard to use in the first place.
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (s in 0..5) {
                BloomStage(
                    stage = s,
                    reached = s <= reached,
                    selected = s == shown,
                    onClick = { preview = if (s == reached) null else s },
                    ink = colors.onBackdrop,
                    modifier = Modifier.weight(1f).height(64.dp),
                )
            }
        }

        // Two lines: the count, then what the flower has to do with it.
        //
        // The count used to be a single 10sp line tracked at 2.5sp — pass-label treatment,
        // applied to the only number on the screen. And nothing anywhere said what the
        // flower *was*: someone opening Home for the first time got a drawing, a row of
        // silhouettes and "Checked in 3 / 8 bases", with no line connecting them. The
        // caption now carries its own explanation, quietly, under the count.
        //
        // While previewing, both lines change: the stage you are looking at, and a
        // reminder that it is not where you are — so the screen never shows a flower it
        // cannot account for.
        Text(
            if (preview == null) {
                stringResource(R.string.home_checked_in, model.checkedInBases, model.totalBases)
            } else {
                stringResource(stageLabel(shown))
            },
            color = colors.onBackdrop,
            fontSize = 15.sp,
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(
                if (preview == null) R.string.home_bloom_hint else R.string.home_stage_preview_hint,
            ),
            color = colors.onBackdropMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * How long the bloom keeps breathing after a touch.
 *
 * Long enough to cover reading the screen and tapping along the stage strip, short enough
 * that a phone put away stops drawing almost immediately.
 */
private const val BreathIdleMillis = 12_000L

/**
 * One of Home's two corner buttons.
 *
 * Written once and used twice rather than copied, because the pair only works while they
 * are identical — two 46dp panes in opposite corners read as a matched set, and a pane
 * that has drifted a dp or a tint away from its twin reads as a mistake. This is the same
 * rule the palette applies to the pass's "white at 54%": two private copies is how two
 * things stop matching.
 *
 * [badge] is the unread mark. It sits inside the pane's own corner rather than hanging
 * off it, so the button keeps a clean 46dp square in the layout and the two corners stay
 * optically level. It is drawn in `onBackdrop` — the app has no accent hue to spend on it
 * — which is bright enough against the sheer pane to carry on its own, and it is only
 * ever a few pixels from the glyph it belongs to.
 */
@Composable
private fun CornerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    badge: Boolean = false,
) {
    val colors = wbwColors
    Box(
        Modifier
            .size(46.dp)
            .glass(CornerShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = colors.onBackdrop, modifier = Modifier.size(22.dp))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.onBackdrop),
            )
        }
    }
}

/**
 * The corner buttons' shape: a rounded square, not a circle.
 *
 * A circle in the corner of a screen this glassy reads as an avatar that failed to load —
 * it is the one shape the app reserves for photographs of people. The radius is the field
 * corner rather than the card corner, because at 46dp a card's 26dp is most of the way
 * back to a circle.
 */
private val CornerShape = RoundedCornerShape(15.dp)
