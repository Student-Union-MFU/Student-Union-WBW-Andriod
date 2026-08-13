package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * Primary action, matching the su-wbw-website: a fully-rounded pill — cream on the
 * dark forest scene, forest-green on light — with a soft lift shadow. The accent is kept
 * for accents (eyebrows, active states, rings), not fills.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 26.dp, vertical = 13.dp),
) {
    val c = wbwColors
    val container = if (c.isDark) Cream else Forest
    val content = if (c.isDark) DeepForest else Cream
    Box(
        modifier
            .shadow(if (enabled) 10.dp else 0.dp, PillShape, clip = false, spotColor = Color.Black.copy(alpha = 0.45f), ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(PillShape)
            .background(if (enabled) container else container.copy(alpha = 0.35f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
        } else {
            Text(text, color = if (enabled) content else content.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        }
    }
}

/** Secondary action: outline pill, transparent fill. */
@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 26.dp, vertical = 13.dp),
) {
    val c = wbwColors
    Box(
        modifier
            .clip(PillShape)
            .border(1.dp, c.textMuted.copy(alpha = 0.5f), PillShape)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.textMuted, fontWeight = FontWeight.SemiBold)
    }
}
