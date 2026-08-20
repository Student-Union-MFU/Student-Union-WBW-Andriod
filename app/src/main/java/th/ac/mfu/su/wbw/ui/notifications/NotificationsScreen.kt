package th.ac.mfu.su.wbw.ui.notifications

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
import th.ac.mfu.su.wbw.ui.theme.GlassCard
import th.ac.mfu.su.wbw.ui.theme.GlassPanel
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.PassInk
import th.ac.mfu.su.wbw.ui.theme.PassSurface
import th.ac.mfu.su.wbw.ui.theme.WbwColors
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Announcements from the trail team.
 *
 * Built in the same language as the participant pass and the events list, because it is
 * the third screen of the same kind: a column of glass panes on the backdrop, read from
 * top to bottom. That means [GlassSheer] panes with nothing painted on them, light ink in
 * both themes, and hierarchy from weight and tracking rather than from colour — see the
 * note on `ActivitiesScreen.EventCard` for why the coloured level tiles this screen used
 * to have were the wrong instrument.
 *
 * The one exception is `emergency`. Every other distinction here is worth carrying with
 * type alone; an alert that somebody has to act on is not, and the palette already keeps
 * `danger` for exactly that case.
 *
 * Pushed from Home's bell rather than living in the tab bar, so it opens with a back
 * button and closes back to where it came from.
 */
@Composable
fun NotificationsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
        // Outside the list, like the pass's back button: a way out that scrolls away is
        // one you have to scroll back up to reach.
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).glass(CircleShape, fill = PassSurface).clickableTap(onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(R.string.action_back),
                    tint = PassInk,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            stringResource(R.string.notifications_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackdrop,
        )
        Text(
            stringResource(R.string.notifications_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBackdropMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(14.dp))

        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is UiState.Success ->
                if (s.data.items.isEmpty()) {
                    EmptyState(message = stringResource(R.string.notifications_empty))
                } else {
                    Feed(s.data, contentPadding)
                }
        }
    }
}

@Composable
private fun Feed(feed: NotificationFeed, contentPadding: PaddingValues) {
    // Resolved once per composition rather than per card: every row would otherwise ask
    // the system for the same zone and the same "what day is it" answer.
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val (todayItems, earlier) = remember(feed.items, today) {
        feed.items.partition { localDateOf(it.createdAt, zone) == today }
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (todayItems.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.notif_today)) }
            items(todayItems, key = { it.id }) { NotificationCard(it, feed.isNew(it), zone) }
        }
        if (earlier.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.notif_earlier)) }
            items(earlier, key = { it.id }) { NotificationCard(it, feed.isNew(it), zone) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = wbwColors.onBackdropMuted,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp),
    )
}

/**
 * One announcement.
 *
 * Read in the order it is laid out: what it says, then how much it matters and whether it
 * is new to you, then the detail, then when it arrived. The title leads for the same
 * reason an event's name does — on a screen of announcements it is the only thing telling
 * one card from another.
 */
@Composable
private fun NotificationCard(item: Notification, isNew: Boolean, zone: ZoneId) {
    val colors = wbwColors
    val level = Level.of(item.level)
    val tint = level.tint(colors)

    GlassCard(
        shape = RoundedCornerShape(CardCorner),
        contentPadding = PaddingValues(18.dp),
        fill = GlassPanel,
        border = GlassSheerBorder,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackdrop,
            )

            Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(level.icon, null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(level.labelRes).uppercase(),
                    color = tint,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (isNew) {
                    Spacer(Modifier.width(10.dp))
                    NewMark()
                }
            }

            if (!item.body.isNullOrBlank()) {
                Text(
                    item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBackdropMuted,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 13.dp),
                )
            }

            Text(
                timeLabel(item.createdAt, zone).uppercase(),
                color = colors.onBackdrop,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/**
 * The "new" mark: an outlined chip, not a coloured dot.
 *
 * A dot would have to be a colour to mean anything, and this screen has spent its one
 * colour on `emergency`. An outline says the same thing out of the parts the pass
 * already uses, and it can carry the word, which a dot cannot.
 */
@Composable
private fun NewMark() {
    Text(
        stringResource(R.string.notif_new).uppercase(),
        color = wbwColors.onBackdrop,
        fontSize = 9.sp,
        letterSpacing = 1.4.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .border(1.dp, GlassSheerBorder, RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/**
 * The backend's `noti_level` enum — a closed set of exactly three
 * (`db/migrations/000005_wbw.up.sql`), so it is modelled as one rather than guessed at
 * per call site. `type` is deliberately not read: it is a free-text column with no
 * enumeration behind it, and the previous version's switch over "growth"/"schedule"
 * matched values nothing in the system is obliged to send.
 *
 * The tints escalate by strength rather than by hue — muted, full, then danger — which is
 * how the rest of the app carries importance.
 */
private enum class Level(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Info(R.string.notif_level_info, Icons.Outlined.Campaign),
    Warning(R.string.notif_level_warning, Icons.Outlined.ErrorOutline),
    Emergency(R.string.notif_level_emergency, Icons.Outlined.WarningAmber),
    ;

    fun tint(colors: WbwColors): Color = when (this) {
        Info -> colors.onBackdropMuted
        Warning -> colors.onBackdrop
        Emergency -> colors.danger
    }

    companion object {
        /** Anything unrecognised is treated as [Info] — an unknown level is not an alarm. */
        fun of(raw: String): Level = when (raw.lowercase()) {
            "emergency" -> Emergency
            "warning" -> Warning
            else -> Info
        }
    }
}

private val CardCorner = 18.dp

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/**
 * Parses the backend's timestamp.
 *
 * Go marshals `time.Time` as RFC 3339, so the offset is normally there — but the column
 * has been served both with and without one across environments, and a feed that throws
 * on one row is worse than a feed that shows an unparsed string. Null means "could not
 * read this", and the callers fall back to showing the raw value rather than a wrong one.
 */
private fun parseInstant(raw: String): Instant? =
    runCatching { OffsetDateTime.parse(raw).toInstant() }
        .recoverCatching { LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant() }
        .getOrNull()

/**
 * The local calendar date an announcement landed on.
 *
 * Converted through the zone rather than by slicing the first ten characters off the
 * string, which is what the previous version did: an announcement sent at 21:00 UTC is
 * already tomorrow in Thailand, so string-slicing filed the evening's alerts under
 * "Earlier" the moment they arrived.
 */
private fun localDateOf(raw: String, zone: ZoneId): LocalDate? =
    parseInstant(raw)?.atZone(zone)?.toLocalDate()

/** Time of day for today's announcements, full date for older ones. */
private fun timeLabel(raw: String, zone: ZoneId): String {
    val local = parseInstant(raw)?.atZone(zone) ?: return raw
    return if (local.toLocalDate() == LocalDate.now(zone)) {
        local.format(TimeFormat)
    } else {
        local.format(DateFormat)
    }
}

/** Taps without the ripple, matching the events list and the pass. */
private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
