package th.ac.mfu.su.wbw.ui.chat

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.ChatMessage
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.WbwGreenDark
import th.ac.mfu.su.wbw.ui.theme.WbwInkLight
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import kotlinx.coroutines.launch

/**
 * Group chat, against the server's long-poll.
 *
 * The layout is unchanged from the placeholder this replaced; what changed is where the
 * messages come from. [ChatViewModel] owns the engine — the hold, the retry backoff, the
 * optimistic send and the read cursor — and this file is the column and the composer.
 *
 * The polling is driven from *here*, in effects tied to the screen's own lifetime, not from
 * `viewModelScope`. That is deliberate: this view model outlives a tab switch, and a
 * connection held open for a screen nobody is looking at is a hole in the battery.
 *
 * The Discord grouping rule is what drives the rhythm of the column: consecutive messages
 * from the same author, close together in time, drop the avatar and the name and sit tight
 * under the first. It is what stops a conversation reading as a list of cards.
 *
 * No staff badge. The server does not put a role on a message, so a "STAFF" tag here would
 * be decoration that could not be trusted; it comes back when the API carries one.
 */
@Composable
fun ChatScreen(
    contentPadding: PaddingValues,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rows = remember(state.messages, state.pending) { groupMessages(state.messages, state.pending) }
    // The read cursor is only worth showing on the newest thing you said — a receipt under
    // every one of your messages is noise, and the server only tells you the high-water mark
    // per member anyway.
    val lastMineId = remember(state.messages, state.meId) {
        state.messages.lastOrNull { it.senderId == state.meId }?.id
    }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var emojiOpen by rememberSaveable { mutableStateOf(false) }

    // The two loops, held for exactly as long as the screen is on display. Leaving the
    // screen cancels both, which closes the held connection and stops the heartbeat — the
    // server then correctly stops treating this member as "looking at the chat".
    LaunchedEffect(Unit) { viewModel.sync() }
    LaunchedEffect(Unit) { viewModel.heartbeat() }

    // Follow the conversation only when already at the bottom. Yanking somebody back down
    // while they are reading history is the standard way to make a chat unusable.
    val atBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(rows.size) {
        if (rows.isNotEmpty() && atBottom) listState.animateScrollToItem(rows.lastIndex)
    }

    fun send() {
        val body = draft.trim()
        if (body.isEmpty()) return
        viewModel.send(body)
        draft = ""
        emojiOpen = false
        // Jump to the message just sent. Without this it lands below the fold and the
        // send reads as having done nothing.
        scope.launch { listState.animateScrollToItem((rows.size).coerceAtLeast(0)) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Channel header. Named like a Discord channel because the group *is* the
        // channel here — one per participant group, which is how the iOS app models it.
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 12.dp)) {
            Text(
                state.groupNumber?.let { stringResource(R.string.chat_channel_group, it) }
                    ?: stringResource(R.string.chat_channel),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackdrop,
            )
            Text(
                when {
                    state.noGroup -> stringResource(R.string.chat_no_group)
                    // A dropped long-poll is the normal condition on this hill, so it is
                    // reported here in the muted line rather than as an alert over the
                    // conversation — the messages already on screen are still true.
                    state.error != null -> stringResource(R.string.chat_offline)
                    state.memberCount > 0 -> stringResource(R.string.chat_members, state.memberCount)
                    else -> stringResource(R.string.chat_channel_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onBackdropMuted,
            )
        }

        // The thread sits directly on the backdrop — no pane under it.
        //
        // It had one, and that is what put two different greens on the screen. Liquid
        // glass lifts what it samples (vibrancy, then the lens), which is exactly right
        // for something small floating on a ground: the nav bar, the composer, a card.
        // The thread is not small. It covers most of the screen, so a pane there stops
        // reading as a surface *on* the background and starts reading as a second
        // background — a big bright rectangle butted against the real one, with a seam
        // down the side.
        //
        // Discord does the same thing for the same reason: its message list sits on the
        // app background, and only the composer and chrome are their own surfaces.
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(rows) { row ->
                when (row) {
                    is Row_.Day -> DayDivider(row.label)
                    is Row_.Message -> MessageRow(
                        row = row,
                        readBy = if (row.message.id == lastMineId && state.readCount > 0) {
                            state.readCount
                        } else {
                            0
                        },
                    )
                    is Row_.Pending -> PendingRow(row.message, onRetry = { viewModel.retry(it) })
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // The emoji strip, open only when asked for. A scrolling row rather than a grid
        // in a sheet: on a screen whose whole job is short replies, the useful set is
        // about twenty and a full picker is a dialog for a one-tap job.
        AnimatedVisibility(visible = emojiOpen) {
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(QuickEmoji) { e ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickableTap { draft += e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 24.sp) }
                }
            }
        }

        // A real composer: it takes text and the button posts it into the thread above.
        // Local only, but a field you can actually type in is the thing that tells you
        // whether the layout works — a painted-on placeholder never does.
        //
        // One shape, with send *inside* it. It was a pill plus a detached round button,
        // which is precisely the nav bar's silhouette — and the nav bar sits directly
        // underneath. Two identical pill-and-circle rows stacked read as two nav bars,
        // and the lower one is the real navigation, so the composer was the one that had
        // to give the pattern up. The bar owns that split (it is the iOS `.search` role);
        // the composer is a single field.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                // contentPadding carries the floating nav bar's clearance. Without it the
                // composer sits under the bar, which overlays everything from the scaffold.
                .padding(bottom = contentPadding.calculateBottomPadding().coerceAtLeast(14.dp))
                .heightIn(min = 56.dp)
                // The same material as the nav bar under it and the event cards: a sheer
                // white pane and a hairline. It was clear glass with a 34% edge, which
                // made it the only surface in the app described by its outline alone —
                // a drawn rectangle rather than a piece of the same glass. A pane needs
                // so little fill to stop being an outline, and this is that little.
                //
                // Squarer than the pill it was, too. At 28dp on a 56dp field the composer
                // was a pill sitting directly above the nav bar's pill; the softer square
                // separates the thing you type in from the thing you navigate with.
                .glass(
                    ComposerShape,
                    fill = GlassSheer,
                    border = GlassSheerBorder,
                    elevation = 0.dp,
                )
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji, replacing the old plus. The plus promised attachments — photos,
            // files, a menu of things — none of which exist and none of which this
            // screen is for.
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickableTap { emojiOpen = !emojiOpen },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (emojiOpen) Icons.Filled.EmojiEmotions else Icons.Outlined.EmojiEmotions,
                    stringResource(R.string.chat_emoji),
                    tint = colors.onBackdrop.copy(alpha = if (emojiOpen) 0.95f else 0.7f),
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) {
                    Text(
                        stringResource(R.string.chat_composer_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackdrop.copy(alpha = 0.62f),
                    )
                }
                // BasicTextField, not a Material TextField: the Material ones bring their
                // own container, indicator line and padding, all of which would paint a
                // background back onto a field that is deliberately clear.
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onBackdrop),
                    cursorBrush = SolidColor(colors.onBackdrop),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(); keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            val canSend = draft.isNotBlank()
            // Filled once there is something to send. Inside the field it needs to lift
            // off the glass to read as a button rather than a second icon, and this is
            // the one control on the screen whose whole job is to be pressed.
            Box(
                Modifier
                    .size(44.dp)
                    .clip(SendShape)
                    .background(colors.onBackdrop.copy(alpha = if (canSend) 0.92f else 0.10f))
                    .clickableTap { send(); keyboard?.hide() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    stringResource(R.string.chat_send),
                    tint = if (canSend) WbwInkLight else colors.onBackdrop.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * The composer's shape, and the send button's inside it.
 *
 * The button's radius is scaled to its own size rather than repeating the field's — 20dp
 * on a 44dp square would be a circle again, and the point is that the two read as the same
 * family of shape at two sizes.
 */
private val ComposerShape = RoundedCornerShape(20.dp)
private val SendShape = RoundedCornerShape(14.dp)

/** The avatar, rounded in proportion to its own size, like the stage chips. */
private val AvatarShape = RoundedCornerShape(13.dp)

/**
 * The emoji offered by the strip.
 *
 * Chosen for this event rather than by frequency: encouragement, arrival, terrain and
 * weather are what a group walking a mountain actually sends each other.
 */
private val QuickEmoji = listOf(
    "\uD83D\uDC4D", "\uD83D\uDE4C", "\uD83D\uDD25", "\uD83D\uDE02", "\u2764\uFE0F",
    "\uD83C\uDF89", "\uD83D\uDCAA", "\uD83E\uDD7E", "\uD83C\uDFC1", "\uD83D\uDCCD",
    "\uD83C\uDF32", "\u26F0\uFE0F", "\uD83C\uDF27\uFE0F", "\u2600\uFE0F", "\uD83D\uDCA7",
    "\uD83D\uDE05", "\uD83D\uDC40", "\uD83D\uDE4F", "\u2705", "\u26A0\uFE0F",
)

@Composable
private fun DayDivider(label: String) {
    val colors = wbwColors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.glassBorder))
        Text(
            label.uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 8.5.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(colors.glassBorder))
    }
}

@Composable
private fun MessageRow(row: Row_.Message, readBy: Int = 0) {
    val colors = wbwColors
    val m = row.message
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = if (row.grouped) 0.dp else 14.dp, bottom = 1.dp),
    ) {
        // The gutter is always the avatar's width, grouped or not — that column is what
        // keeps every line of every message aligned down the page. Wider than the avatar
        // itself so the text starts clear of it rather than tucked against it.
        Box(Modifier.width(52.dp), contentAlignment = Alignment.TopStart) {
            if (!row.grouped) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(AvatarShape)
                        .background(WbwGreenDark.copy(alpha = avatarAlpha(m.senderId)))
                        .border(1.dp, GlassSheerBorder, AvatarShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        m.authorName.take(1).uppercase(),
                        // Light, not WbwInkLight. These fills sit between 30% and 55% of
                        // the green over a dark backdrop, so they come out mid-dark and a
                        // near-black initial on them was around 2:1.
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            if (!row.grouped) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.authorName,
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        m.timeLabel(),
                        color = colors.onBackdropMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(5.dp))
            }
            Text(
                m.body,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 21.sp,
                color = colors.onBackdrop.copy(alpha = 0.88f),
            )
            if (readBy > 0) {
                Text(
                    stringResource(R.string.chat_read_by, readBy),
                    color = colors.onBackdropMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/**
 * A message this device has sent but the server has not acknowledged.
 *
 * Dimmed rather than hidden, and kept in the column rather than shown as a banner: the
 * participant typed it, so it belongs in the conversation where they put it. A failed one
 * offers a retry, which reuses the original client id and therefore cannot double-post.
 */
@Composable
private fun PendingRow(message: PendingMessage, onRetry: (String) -> Unit) {
    val colors = wbwColors
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 1.dp)) {
        Spacer(Modifier.width(52.dp))
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            Text(
                message.body,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 21.sp,
                color = colors.onBackdrop.copy(alpha = if (message.failed) 0.55f else 0.45f),
            )
            Text(
                stringResource(
                    if (message.failed) R.string.chat_send_failed else R.string.chat_sending,
                ),
                color = colors.onBackdropMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .then(
                        if (message.failed) {
                            Modifier.clickableTap { onRetry(message.clientId) }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}


/** What the column actually renders: a day divider, a message, or one still in flight. */
private sealed interface Row_ {
    data class Day(val label: String) : Row_
    data class Message(val message: ChatMessage, val grouped: Boolean, val mine: Boolean) : Row_
    data class Pending(val message: PendingMessage) : Row_
}

/**
 * The Discord grouping rule: a message joins the one above it when the same author sent it,
 * close enough in time, with no day divider between.
 *
 * The time part is what the placeholder could not do — it carried display strings, not
 * timestamps — and it matters: without it, a run of messages from one person hours apart
 * collapses into a single block with one timestamp at the top, which reads as though they
 * were all sent at once.
 *
 * Unsent messages are appended after everything confirmed. They have no id and no server
 * time, so they cannot be ordered against the thread by anything except "later than all of
 * it", which is true by construction.
 */
private fun groupMessages(source: List<ChatMessage>, pending: List<PendingMessage>): List<Row_> {
    val out = ArrayList<Row_>(source.size + pending.size + 4)
    var lastAuthor: String? = null
    var lastDay: String? = null
    var lastAt: Long = Long.MIN_VALUE
    for (m in source) {
        val day = m.dayKey()
        if (day.isNotEmpty() && day != lastDay) {
            out.add(Row_.Day(dayLabel(day)))
            lastDay = day
            lastAuthor = null
        }
        val at = m.epochSecondOrZero()
        val grouped = m.senderId == lastAuthor && at - lastAt in 0..GroupWindowSeconds
        out.add(Row_.Message(m, grouped = grouped, mine = false))
        lastAuthor = m.senderId
        lastAt = at
    }
    pending.forEach { out.add(Row_.Pending(it)) }
    return out
}

/** Messages from one author inside this window sit tight under each other. */
private const val GroupWindowSeconds = 5L * 60L

private fun ChatMessage.epochSecondOrZero(): Long {
    val at = createdAt ?: deviceTime ?: return 0
    return runCatching { java.time.OffsetDateTime.parse(at).toEpochSecond() }.getOrDefault(0)
}

/**
 * A divider's label: today and yesterday by name, anything older by date.
 *
 * Computed against the device's own calendar rather than formatted from the timestamp
 * alone, because "Today" is a fact about when it is being read, not about when it was sent.
 */
private fun dayLabel(key: String): String {
    val date = runCatching { java.time.LocalDate.parse(key) }.getOrNull() ?: return key
    val today = java.time.LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.toString()
    }
}

/**
 * A stable per-author strength for the avatar tint.
 *
 * Hashed off the sender id rather than the display name: two participants can share a first
 * name, and an id is what actually identifies them.
 */
private fun avatarAlpha(key: String): Float {
    val h = key.fold(0) { acc, c -> acc * 31 + c.code } and 0xFFFF
    // 0.30–0.54. The old 0.38–0.74 reached far enough up the green that the brightest
    // avatars stopped carrying a light initial — the range has to stay inside what one
    // ink colour can sit on, since the initial cannot pick a colour per author.
    return 0.30f + (h % 5) * 0.06f
}

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
