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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
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
import th.ac.mfu.su.wbw.ui.theme.GlassCard
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
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
 * So the card is now glass with nothing painted on it: [GlassSheer], the nav bar's own
 * surface. Two green tints were tried here first and both were wrong for the same reason —
 * the ground is already green, so tinting the pane green only brightens or darkens it,
 * and neither is a thing glass does. White at 12% over this artwork *comes out* green,
 * which is why the bar has always looked right.
 *
 * The order is the order the card is read: the **name** of the event, then **what kind** it
 * is (mark and status), then **what it is**, then **when**, then **what you can do**. The
 * title leads because on a screen of upcoming events the name is the only thing being
 * chosen between; the previous arrangement opened with a mark and the word "UPCOMING",
 * which every card says, and made you read past it to find out which event you were
 * looking at. The date moved to the foot for the same reason — it matters once you have
 * decided the event is interesting, not before.
 *
 * Type sizes: status and date are 11sp. They were 8.5sp with 2.2sp of tracking, which is a
 * *watermark* size — it works on the pass because the pass has one line of it, and here it
 * was carrying the only two facts that distinguish one upcoming event from another.
 *
 * The rule is full-bleed and sits between the content and the action. Inset, it reads as an
 * underline belonging to the paragraph above it rather than as a seam.
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
    GlassCard(
        shape = RoundedCornerShape(EventCorner),
        contentPadding = PaddingValues(0.dp),
        fill = GlassSheer,
        border = GlassSheerBorder,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackdrop,
                )

                // The mark and the status, together on one line under the name: both
                // answer "what kind of thing is this", and neither is worth a line of
                // its own. The mark shrank from a 44dp badge to a plain 16dp glyph now
                // that it is a piece of a meta line rather than the card's opening move.
                Row(
                    Modifier.padding(top = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = colors.onBackdropMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(statusRes).uppercase(),
                        color = colors.onBackdropMuted,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBackdropMuted,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 13.dp),
                )

                // The date, last and in full ink. It is the one line here that is
                // different on every card, so it ends the block rather than opening it —
                // you read what the event is, then when it is.
                Row(
                    Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        null,
                        tint = colors.onBackdrop,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        date.uppercase(),
                        color = colors.onBackdrop,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Full-bleed: a rule inset from both edges reads as an underline belonging to
            // the paragraph above it, not as the seam between two parts of the card.
            Box(Modifier.fillMaxWidth().height(1.dp).background(GlassSheerBorder))

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickableTap {}
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.event_details).uppercase(),
                    color = colors.onBackdrop,
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    null,
                    tint = colors.onBackdropMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * The event card's corner.
 *
 * Tighter than the app's [CardCorner]. 26dp is right for a small pane — the pass, a
 * settings panel — but these cards are the full width of the screen and 200dp tall, and at
 * that size the same radius eats a visible bite out of every corner and leaves the card
 * looking inflated. Big surfaces want a smaller radius than small ones to read as equally
 * rounded.
 */
private val EventCorner = 18.dp

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
