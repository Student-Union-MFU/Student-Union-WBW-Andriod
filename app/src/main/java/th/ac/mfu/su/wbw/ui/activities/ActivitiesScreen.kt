package th.ac.mfu.su.wbw.ui.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.CardCorner
import th.ac.mfu.su.wbw.ui.theme.Deep
import th.ac.mfu.su.wbw.ui.theme.Forest
import th.ac.mfu.su.wbw.ui.theme.GlassCard
import th.ac.mfu.su.wbw.ui.theme.Leaf
import th.ac.mfu.su.wbw.ui.theme.PillButton
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
            style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary,
        )
        Text(
            stringResource(R.string.activities_subtitle),
            style = MaterialTheme.typography.bodySmall, color = colors.textMuted,
        )
        Spacer(Modifier.height(2.dp))

        EventCard(
            title = stringResource(R.string.event_step_comp_title),
            date = stringResource(R.string.event_step_comp_date),
            description = stringResource(R.string.event_step_comp_desc),
            statusRes = R.string.event_status_upcoming,
            icon = Icons.Outlined.DirectionsWalk,
            banner = listOf(Forest, Leaf),
        )
        EventCard(
            title = stringResource(R.string.event_wbw_title),
            date = stringResource(R.string.event_wbw_date),
            description = stringResource(R.string.event_wbw_desc),
            statusRes = R.string.event_status_upcoming,
            icon = Icons.Outlined.Park,
            banner = listOf(Deep, Forest),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EventCard(
    title: String,
    date: String,
    description: String,
    statusRes: Int,
    icon: ImageVector,
    banner: List<Color>,
) {
    val colors = wbwColors
    GlassCard(shape = RoundedCornerShape(CardCorner), contentPadding = PaddingValues(0.dp)) {
        Column {
            // Coloured banner with a large watermark icon and a status pill.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .background(Brush.linearGradient(banner)),
            ) {
                Icon(
                    icon, null, tint = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 34.dp, y = 20.dp),
                )
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(colors.gold))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(statusRes), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Body
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = colors.gold, modifier = Modifier.size(15.dp))
                    Text(date, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
                Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                Spacer(Modifier.height(4.dp))
                PillButton(
                    text = stringResource(R.string.event_details),
                    onClick = {},
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}
