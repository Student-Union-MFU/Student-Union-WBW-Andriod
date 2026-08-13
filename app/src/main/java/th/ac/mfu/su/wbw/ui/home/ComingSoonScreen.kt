package th.ac.mfu.su.wbw.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.GlassCard
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/** Placeholder for trail sections not yet backed by the server (map, fitness, QR check-in). */
@Composable
fun ComingSoonScreen(
    titleRes: Int,
    icon: ImageVector,
    contentPadding: PaddingValues,
) {
    val colors = wbwColors
    Box(Modifier.fillMaxSize().padding(contentPadding).padding(24.dp), contentAlignment = Alignment.Center) {
        GlassCard(shape = RoundedCornerShape(26.dp), contentPadding = PaddingValues(28.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(colors.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = colors.accent, modifier = Modifier.size(30.dp)) }
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge, color = colors.onBackdrop)
                Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.labelLarge, color = colors.accent)
                Text(
                    stringResource(R.string.coming_soon_desc),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onBackdropMuted, textAlign = TextAlign.Center,
                )
            }
        }
    }
}
