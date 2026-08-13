package th.ac.mfu.su.wbw.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.Notification
import th.ac.mfu.su.wbw.ui.common.EmptyState
import th.ac.mfu.su.wbw.ui.common.ErrorState
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.common.UiState
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

@Composable
fun NotificationsScreen(
    contentPadding: PaddingValues,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = wbwColors

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding).padding(horizontal = 16.dp)) {
        // header
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ALERTS", color = colors.gold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.sp)
                Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            }
            Text(
                stringResource(R.string.notifications_mark_all_read),
                color = colors.gold, style = MaterialTheme.typography.labelMedium,
            )
        }

        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is UiState.Success ->
                if (s.data.isEmpty()) {
                    EmptyState(message = stringResource(R.string.notifications_empty))
                } else {
                    NotificationList(s.data)
                }
        }
    }
}

@Composable
private fun NotificationList(items: List<Notification>) {
    val today = remember2Today()
    val (todayItems, earlier) = items.partition { it.createdAt.take(10) == today }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (todayItems.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.notif_today)) }
            items(todayItems.size) { NotificationCard(todayItems[it]) }
        }
        if (earlier.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.notif_earlier)) }
            items(earlier.size) { NotificationCard(earlier[it]) }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = wbwColors.textMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun NotificationCard(item: Notification) {
    val colors = wbwColors
    val unread = item.readAt == null
    val (accent, icon) = accentFor(item)
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    item.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                )
                Text(shortTime(item.createdAt), color = colors.textMuted, fontSize = 10.sp)
            }
            if (!item.body.isNullOrBlank()) {
                Text(item.body, style = MaterialTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (unread) {
            Box(Modifier.padding(top = 4.dp).size(8.dp).clip(CircleShape).background(colors.gold))
        }
    }
}

@Composable
private fun accentFor(item: Notification): Pair<Color, ImageVector> {
    val colors = wbwColors
    return when (item.level) {
        "emergency" -> colors.danger to Icons.Outlined.WarningAmber
        "warning" -> colors.gold to Icons.Outlined.Place
        else -> when (item.type) {
            "growth", "tree" -> Color(0xFF8FD0A8) to Icons.Outlined.Park
            "schedule", "event" -> colors.gold to Icons.Outlined.CalendarMonth
            else -> Color(0xFF8FD0A8) to Icons.Outlined.Campaign
        }
    }
}

@Composable
private fun remember2Today(): String = androidx.compose.runtime.remember {
    java.time.LocalDate.now().toString()
}

// Backend sends ISO-ish timestamps; show the HH:mm (today) or the date.
private fun shortTime(raw: String): String {
    val datePart = raw.take(10)
    val timePart = raw.drop(11).take(5)
    return if (timePart.isNotBlank()) timePart else datePart
}
