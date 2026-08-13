package th.ac.mfu.su.wbw.ui.activities

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.CardCorner
import th.ac.mfu.su.wbw.ui.theme.GlassCard
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/** Events on the trail (replaces the old Announcements tab). */
@Composable
fun ActivitiesScreen(contentPadding: PaddingValues) {
    val colors = wbwColors
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.activities_title),
            style = MaterialTheme.typography.headlineSmall, color = colors.onBackdrop,
        )
        Text(
            stringResource(R.string.activities_subtitle),
            style = MaterialTheme.typography.bodySmall, color = colors.onBackdropMuted,
        )
        Spacer(Modifier.height(2.dp))

        EventCard(
            title = stringResource(R.string.event_step_comp_title),
            date = stringResource(R.string.event_step_comp_date),
            description = stringResource(R.string.event_step_comp_desc),
            statusRes = R.string.event_status_upcoming,
            icon = Icons.Outlined.DirectionsWalk,
        )
        EventCard(
            title = stringResource(R.string.event_wbw_title),
            date = stringResource(R.string.event_wbw_date),
            description = stringResource(R.string.event_wbw_desc),
            statusRes = R.string.event_status_upcoming,
            icon = Icons.Outlined.Park,
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * One event, in the same language as the participant pass.
 *
 * The old card led with a saturated green gradient band and a big white watermark, which
 * is what made these read as visitors: it was the only solid colour left in the app, and
 * it sat on a glass pane over a photograph. Everything that band was doing — separating
 * the card from the ground, carrying the status, giving the eye somewhere to land — the
 * pass already does with a hairline, a tracked micro-label and one heavy line of type.
 *
 * So the card is now glass with nothing painted on it. The watermark survives at a
 * fraction of its old strength, behind the text rather than in a band of its own, because
 * it is genuinely useful for telling the two events apart at a glance.
 */
@Composable
private fun EventCard(
    title: String,
    date: String,
    description: String,
    statusRes: Int,
    icon: ImageVector,
) {
    val colors = wbwColors
    GlassCard(shape = RoundedCornerShape(CardCorner), contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.fillMaxWidth()) {
            // Watermark, behind the type. Bled off the corner so it reads as a mark on
            // the surface rather than as an illustration boxed inside it.
            Icon(
                icon,
                null,
                tint = colors.textPrimary.copy(alpha = 0.06f),
                modifier = Modifier
                    .size(168.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 44.dp, y = (-26).dp),
            )

            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(colors.textMuted))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        stringResource(statusRes).uppercase(),
                        color = colors.textMuted,
                        fontSize = 8.5.sp,
                        letterSpacing = 2.2.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        date.uppercase(),
                        color = colors.textMuted,
                        fontSize = 8.5.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 14.dp),
                )

                Box(
                    Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.glassBorder),
                )

                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 14.dp),
                )

                // Hairline rather than filled. A solid button is the loudest thing a card
                // can hold, and on a screen of two cards it made the actions shout over
                // the events they belong to.
                Box(
                    Modifier
                        .padding(top = 18.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.glassBorder, CircleShape)
                        .clickableTap {}
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(
                        stringResource(R.string.event_details).uppercase(),
                        color = colors.textPrimary,
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
