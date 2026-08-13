package th.ac.mfu.su.wbw.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.GlassCard
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/** Centered spinner for full-screen loading. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = wbwColors.accent, strokeWidth = 3.dp)
    }
}

/**
 * Centered error message with an optional retry action.
 *
 * Sits on a glass card rather than straight on the backdrop. The backdrop is now the
 * iOS forest photograph, which is brightest through the middle of the frame — exactly
 * where these states land — and iOS only scrims the top and bottom of it. Bare light
 * text there is close to unreadable; iOS gets away with white text because its own
 * mid-screen content always sits on a surface, never directly on the photo.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val colors = wbwColors
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard(shape = RoundedCornerShape(PanelCorner), contentPadding = PaddingValues(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                )
                if (onRetry != null) {
                    th.ac.mfu.su.wbw.ui.theme.PillButton(
                        text = stringResource(R.string.action_retry),
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

/** Centered muted message for empty lists. On a glass card for the same reason as [ErrorState]. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard(shape = RoundedCornerShape(PanelCorner), contentPadding = PaddingValues(20.dp)) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = wbwColors.textMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
