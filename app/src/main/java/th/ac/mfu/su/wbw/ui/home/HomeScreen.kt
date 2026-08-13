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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
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
    // Tapping a phase chip highlights that stage; otherwise the real one is shown.
    // This used to drive the 3D plant as well — with the plant gone it only moves the
    // highlight, so the chips read as a legend you can point at rather than a preview.
    var previewStage by remember { mutableStateOf<Int?>(null) }
    val activeOrdinal = previewStage ?: model.phase.ordinal
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // greeting
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (morning) R.string.home_good_morning else R.string.home_good_evening),
                    color = colors.gold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.sp,
                )
                Text(
                    stringResource(R.string.home_greeting, model.displayName),
                    style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary,
                )
            }
            // Profile lives here rather than in the nav bar. It isn't somewhere you
            // move back and forth between while walking the trail — you open it to
            // check a detail or sign out — and iOS reaches it the same way, from the
            // avatar in this header.
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .glass(CircleShape)
                    .clickable(onClick = onOpenProfile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Person,
                    stringResource(R.string.profile_title),
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // The 3D plant hero used to sit here, filling half the screen. Removed on
        // request — the backdrop is already a forest, so a tree drawn on top of it was
        // competing with the artwork rather than adding to it. `PlantHero`, `Plant3D`
        // and the growth-stage models are all still in the package, so putting it back
        // is one call.
        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.home_checked_in, model.checkedInBases, model.totalBases),
            style = MaterialTheme.typography.titleMedium, color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        val remaining = (model.totalBases - model.checkedInBases).coerceAtLeast(0)
        Text(
            stringResource(R.string.home_phase_hint, remaining, stringResource(model.phase.labelRes)),
            style = MaterialTheme.typography.bodySmall, color = colors.textMuted,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )

        // growth phase track
        Row(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(PanelCorner)).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GrowthPhase.entries.forEach { phase ->
                PhaseChip(
                    phase = phase,
                    activeOrdinal = activeOrdinal,
                    onClick = { previewStage = phase.ordinal },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // The "next base" card sat here (name, distance, Navigate → the map tab).
        // Removed on request. `model.nextBaseName`/`nextBaseDistance` are still on the
        // ui model and still populated, so nothing had to be unpicked upstream.
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PhaseChip(phase: GrowthPhase, activeOrdinal: Int, onClick: () -> Unit, modifier: Modifier) {
    val colors = wbwColors
    val reached = phase.ordinal <= activeOrdinal
    val active = phase.ordinal == activeOrdinal
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier.clip(shape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(shape)
                .background(if (active) colors.gold.copy(alpha = 0.24f) else colors.gold.copy(alpha = if (reached) 0.12f else 0f))
                .border(if (active) 2.dp else 1.dp, if (active) colors.gold else colors.glassBorder, shape),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PhaseTree(phase, Modifier.padding(bottom = 3.dp).fillMaxSize(0.62f), muted = !reached)
        }
        Text(
            stringResource(phase.labelRes),
            fontSize = 8.5.sp, maxLines = 1,
            color = if (active) colors.textPrimary else if (reached) colors.gold else colors.textMuted,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// morning if between 5:00 and 18:00
@Composable
private fun remember2(): Boolean = remember { LocalTime.now().hour in 5..17 }
