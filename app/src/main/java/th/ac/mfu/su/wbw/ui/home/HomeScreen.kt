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
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // The top strip carries the profile button and nothing else. The greeting moved
        // down into the body: it is content — it names the person and the time of day —
        // and sitting it in the chrome made the top of the screen look like a title bar
        // for a screen that has no title.
        //
        // Profile stays here rather than in the nav bar. It isn't somewhere you move
        // back and forth between while walking the trail — you open it to check a detail
        // or sign out — and iOS reaches it the same way, from the avatar in this corner.
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
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

        // greeting — now the first thing in the body
        Column(Modifier.padding(top = 18.dp)) {
            Text(
                stringResource(if (morning) R.string.home_good_morning else R.string.home_good_evening),
                color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.sp,
            )
            Text(
                stringResource(R.string.home_greeting, model.displayName),
                // onBackdrop, not textPrimary: this sits on the dark backdrop image in
                // both themes, and textPrimary goes near-black in light theme.
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackdrop,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Home has been emptied deliberately, one piece at a time: the 3D plant hero,
        // the "Checked in N / M bases" line and its hint, the "next base" card, and now
        // the Seed→Grown phase track. Only the greeting and the profile button are left.
        //
        // Little of what fed them was unpicked — `PlantHero`/`Plant3D`, the
        // `GrowthPhase` enum with its `PhaseTree` drawing, and
        // `nextBaseName`/`nextBaseDistance` on the ui model are all still here and still
        // populated, so this can be rebuilt from parts rather than from scratch. Only
        // `PhaseChip` went, since it had no caller left.
        Spacer(Modifier.height(4.dp))
    }
}

// morning if between 5:00 and 18:00
@Composable
private fun remember2(): Boolean = remember { LocalTime.now().hour in 5..17 }
