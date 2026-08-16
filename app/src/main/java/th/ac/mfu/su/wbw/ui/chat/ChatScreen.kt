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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.WbwGreenDark
import th.ac.mfu.su.wbw.ui.theme.WbwInkLight
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Group chat — a placeholder, not a client.
 *
 * Nothing here talks to the server. The iOS app's chat is the single hardest piece in it
 * (an offline outbox, long-poll sync, read cursors, optimistic send, `ChatSession` alone
 * is 420 lines), and none of that exists on this side yet. What this screen is for is
 * the *shape*: a real reading of the layout at real message lengths, so the surrounding
 * design can be judged before the engine is written.
 *
 * The Discord grouping rule is the one thing worth getting right early, because it
 * drives the whole rhythm of the column: consecutive messages from the same author
 * inside a short window drop the avatar and the name and sit tight under the first. It
 * is what stops a conversation reading as a list of cards.
 *
 * [SampleMessages] is fixed rather than random so the screen looks the same in every
 * screenshot and every review.
 */
@Composable
fun ChatScreen(contentPadding: PaddingValues) {
    val colors = wbwColors
    // Local only. Messages live for as long as the screen does — there is no repository
    // to put them in and inventing one would be pretending the wire exists.
    val messages = remember { mutableStateListOf<ChatMessageStub>().also { it.addAll(SampleMessages) } }
    val rows = remember(messages.size) { groupMessages(messages) }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var emojiOpen by rememberSaveable { mutableStateOf(false) }

    fun send() {
        val body = draft.trim()
        if (body.isEmpty()) return
        messages.add(ChatMessageStub(author = "You", time = nowLabel(), body = body))
        draft = ""
        emojiOpen = false
        // Jump to the message just sent. Without this it lands below the fold and the
        // send reads as having done nothing.
        scope.launch { listState.animateScrollToItem((rows.size + 1).coerceAtLeast(0)) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Channel header. Named like a Discord channel because the group *is* the
        // channel here — one per participant group, which is how the iOS app models it.
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 12.dp)) {
            Text(
                stringResource(R.string.chat_channel),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackdrop,
            )
            Text(
                stringResource(R.string.chat_channel_desc),
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
                    is Row_.Message -> MessageRow(row)
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

/** The avatar and the staff tag, each rounded in proportion to its own size. */
private val AvatarShape = RoundedCornerShape(13.dp)
private val TagShape = RoundedCornerShape(6.dp)

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

/** Wall-clock label for a message sent right now, matching the stubs' HH:mm. */
private fun nowLabel(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(colors.glassBorder))
    }
}

@Composable
private fun MessageRow(row: Row_.Message) {
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
                // A rounded square with the app's hairline on it, like the stage chips and
                // the profile button. Staff no longer get a solid fill of their own: the
                // tag beside the name already says staff, and the full-strength disc made
                // the avatar the loudest thing in the thread.
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(AvatarShape)
                        .background(WbwGreenDark.copy(alpha = avatarAlpha(m.author)))
                        .border(1.dp, GlassSheerBorder, AvatarShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        m.author.take(1).uppercase(),
                        // Light, not WbwInkLight. These fills sit between 30% and 55% of
                        // the green over a dark backdrop, so they come out mid-dark and a
                        // near-black initial on them was around 2:1.
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            if (!row.grouped) {
                // CenterVertically, not Bottom: the badge and the timestamp are different
                // heights, and hanging them off a shared baseline left the badge sitting
                // low and looking dropped rather than set into the line.
                //
                // No fixed height. It was pinned to 20dp, which is a *maximum* as far as
                // the children are concerned — the badge measures a little taller than
                // that, so its bottom was sliced off square and the tag rendered as a pill
                // with a flat foot. The row is as tall as what is in it, and the badge
                // controls its own height; at large system font scales the old 20dp would
                // have started cutting the name too.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.author,
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    if (m.staff) {
                        Spacer(Modifier.width(8.dp))
                        StaffTag()
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        m.time,
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
        }
    }
}

@Composable
private fun StaffTag() {
    // Sheer glass and a hairline, like the nav bar — it was the last green-tinted chip
    // left on a screen where nothing else is tinted, so it read as a sticker.
    //
    // An explicit height with the label centred in it, rather than padding around a line
    // of text. A tag this small is judged entirely on whether it sits level with the name
    // beside it, and text metrics — ascent, descent, the font's own top padding — do not
    // centre caps for you.
    //
    // Which is exactly what went wrong: `contentAlignment = Center` centres the text's
    // *line box*, and Sarabun is a Thai face, so its ascent carries room for tone marks
    // stacked above the capitals — room that is empty in a Latin word like "STAFF". The
    // line box came out top-heavy and the caps sat low inside the pill: 22px of space above
    // them against 9px below, measured. The pill itself was level with the name all along;
    // it was the label inside it that was off.
    //
    // [LineHeightStyle] with `Trim.Both` throws away that unused leading and re-centres
    // what is left, so the pill is centred on the glyphs that are actually drawn. It is
    // done here rather than by nudging the text with an offset, because an offset would be
    // tuned to one font at one size and would drift the moment either changed.
    Box(
        Modifier
            .height(TagHeight)
            .clip(TagShape)
            .background(GlassSheer)
            .border(1.dp, GlassSheerBorder, TagShape)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.chat_tag_staff).uppercase(),
            color = wbwColors.onBackdrop,
            fontSize = TagTextSize,
            lineHeight = TagTextSize,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            style = LocalTextStyle.current.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }
}

private val TagHeight = 18.dp
private val TagTextSize = 8.5.sp

/** A placeholder message. No ids, no state — this is not on its way to being a model. */
data class ChatMessageStub(
    val author: String,
    val time: String,
    val body: String,
    val staff: Boolean = false,
    val day: String? = null,
)

/** What the column actually renders: either a day divider or a message. */
private sealed interface Row_ {
    data class Day(val label: String) : Row_
    data class Message(val message: ChatMessageStub, val grouped: Boolean) : Row_
}

/**
 * The Discord grouping rule: a message joins the one above it when the same author sent
 * it and no day divider intervenes.
 *
 * Real chat also breaks a group after a few minutes of silence. That needs timestamps
 * rather than the display strings these stubs carry, so it is left for the real model.
 */
private fun groupMessages(source: List<ChatMessageStub>): List<Row_> {
    val out = ArrayList<Row_>(source.size + 4)
    var lastAuthor: String? = null
    for (m in source) {
        m.day?.let {
            out.add(Row_.Day(it))
            lastAuthor = null
        }
        out.add(Row_.Message(m, grouped = m.author == lastAuthor))
        lastAuthor = m.author
    }
    return out
}

/**
 * A stable per-author strength for the avatar tint.
 *
 * Hashed off the name so a person keeps the same shade for the life of the thread, and
 * kept inside a narrow band — the point is to tell speakers apart at a glance, not to
 * reintroduce the accent colour that was just taken out.
 */
private fun avatarAlpha(author: String): Float {
    val h = author.fold(0) { acc, c -> acc * 31 + c.code } and 0xFFFF
    // 0.30–0.54. The old 0.38–0.74 reached far enough up the green that the brightest
    // avatars stopped carrying a light initial — the range has to stay inside what one
    // ink colour can sit on, since the initial cannot pick a colour per author.
    return 0.30f + (h % 5) * 0.06f
}

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

/**
 * Fixed sample traffic.
 *
 * Written to exercise the layout rather than to look pretty: a staff announcement, a
 * long wrapping message, a run of three from one author, and a one-word reply — the four
 * shapes that break a chat column if the spacing is wrong.
 */
private val SampleMessages = listOf(
    ChatMessageStub("Staff", "08:02", "Base 3 is open. Head up the ridge path — the shortcut is closed today.", staff = true, day = "Yesterday"),
    ChatMessageStub("Ploy", "08:14", "we're at base 2, queue is short right now"),
    ChatMessageStub("Ploy", "08:14", "if you're behind us just come straight up"),
    ChatMessageStub("Nine", "08:20", "on our way 🙌"),
    ChatMessageStub("Bank", "09:41", "Does anyone have water left? We ran out somewhere between base 3 and 4 and the next refill point is apparently at the summit.", day = "Today"),
    ChatMessageStub("Ploy", "09:43", "yeah we have two bottles spare"),
    ChatMessageStub("Bank", "09:44", "legend"),
    ChatMessageStub("Staff", "10:15", "Reminder: last check-in closes at 16:00. Anyone still below base 5 after 14:30 should turn back.", staff = true),
)
