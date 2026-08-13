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
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.platform.LocalConfiguration
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
import th.ac.mfu.su.wbw.ui.theme.Ink
import th.ac.mfu.su.wbw.ui.theme.PillButton
import th.ac.mfu.su.wbw.ui.theme.Kanit
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import java.time.LocalTime

@Composable
fun HomeScreen(
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onNavigateBase: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
        is UiState.Success -> HomeContent(s.data, contentPadding, onNavigateBase)
    }
}

@Composable
private fun HomeContent(
    model: HomeUiModel,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onNavigateBase: () -> Unit,
) {
    val colors = wbwColors
    val morning = remember2()
    // Tapping a phase chip previews the plant at that growth stage; otherwise show real progress.
    var previewStage by remember { mutableStateOf<Int?>(null) }
    val activeOrdinal = previewStage ?: model.phase.ordinal
    val growthValue = previewStage?.let { stageGrowth(it) } ?: model.progress
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
            Box(
                Modifier.size(42.dp).clip(CircleShape).glass(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Person, null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
            }
        }

        // 3D plant hero — real growth-stage model over the forest bg (no capsule). Drag to rotate.
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        PlantHero(
            growth = growthValue,
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.5f)
                .padding(top = 4.dp),
        )

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

        // next base
        Row(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(PanelCorner)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                    .background(colors.gold.copy(alpha = 0.16f))
                    .border(1.dp, colors.gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Place, null, tint = colors.gold, modifier = Modifier.size(21.dp)) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_next_base), color = colors.textMuted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Text(model.nextBaseName, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_base_distance, model.nextBaseDistance), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            PillButton(
                text = stringResource(R.string.home_navigate),
                onClick = onNavigateBase,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** Growth fraction (0..1) each phase maps to, used to preview the plant at that stage. */
private fun stageGrowth(ordinal: Int): Float =
    floatArrayOf(0.05f, 0.24f, 0.5f, 0.75f, 1f)[ordinal.coerceIn(0, 4)]

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
