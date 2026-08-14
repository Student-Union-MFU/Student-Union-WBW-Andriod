package th.ac.mfu.su.wbw.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.common.ErrorState
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.common.UiState
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import java.time.LocalTime

@Composable
fun HomeScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenProfile: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
        is UiState.Success -> HomeContent(s.data, contentPadding, onOpenProfile)
    }
}

@Composable
private fun HomeContent(
    model: HomeUiModel,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenProfile: () -> Unit,
) {
    val colors = wbwColors
    val morning = remember2()
    // Not scrollable. Home is a single view now — greeting, bloom, count — and the bloom
    // takes the height that is left, which a scrolling column cannot give it (its content
    // is measured against an unbounded height, so `weight` has nothing to divide).
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(contentPadding)
            .padding(horizontal = 18.dp),
    ) {
        // The top strip carries the profile button and nothing else. The greeting belongs
        // below it, in the body: it is content — it names the person and the time of day —
        // and pairing it with the button turned it into a header for a screen that has no
        // header.
        //
        // Profile stays here rather than in the nav bar. It isn't somewhere you move
        // back and forth between while walking the trail — you open it to check a detail
        // or sign out — and iOS reaches it the same way, from the avatar in this corner.
        //
        // A rounded square, and sheer rather than themed. The circle was filled with
        // `colors.glass` — cream at 86% in light theme — so it came out an opaque white
        // disc with a near-black glyph in it: the brightest thing on the screen, for the
        // least important control on it. Same material as the nav bar now.
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .size(46.dp)
                    .glass(ProfileShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                    .clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Person,
                    stringResource(R.string.profile_title),
                    tint = colors.onBackdrop,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // greeting — the first thing in the body
        Column(Modifier.padding(top = 16.dp)) {
            // The time of day, as a small pane with the sky in it.
            //
            // It was a bare line of type in `colors.accent` — which is near-black in light
            // theme, on a backdrop that is a dark photograph in *both* themes. So half the
            // users got an invisible greeting. That is the trap the palette documents:
            // anything on the backdrop takes `onBackdrop`, never `accent` or `textPrimary`.
            //
            // Made a chip rather than just recoloured because the line has a job the name
            // below cannot do — it says which half of the day the app thinks it is — and a
            // sun or a moon says that faster than the words do.
            Row(
                Modifier
                    .glass(PillShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                    .padding(start = 10.dp, end = 13.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (morning) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
                    null,
                    tint = colors.onBackdrop,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(if (morning) R.string.home_good_morning else R.string.home_good_evening),
                    color = colors.onBackdrop,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 2.4.sp,
                )
            }
            Text(
                stringResource(R.string.home_greeting, model.displayName),
                // onBackdrop, not textPrimary: this sits on the dark backdrop image in
                // both themes, and textPrimary goes near-black in light theme.
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackdrop,
                modifier = Modifier.padding(top = 10.dp),
            )
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

/** The greeting chip. A full pill, so it reads as a tag rather than as a small card. */
private val PillShape = RoundedCornerShape(50)

/**
 * The profile button: a rounded square, not a circle.
 *
 * A circle in the corner of a screen this glassy reads as an avatar that failed to load —
 * it is the one shape the app reserves for photographs of people. The radius is the field
 * corner rather than the card corner, because at 46dp a card's 26dp is most of the way
 * back to a circle.
 */
private val ProfileShape = RoundedCornerShape(15.dp)

// morning if between 5:00 and 18:00
@Composable
private fun remember2(): Boolean = remember { LocalTime.now().hour in 5..17 }
