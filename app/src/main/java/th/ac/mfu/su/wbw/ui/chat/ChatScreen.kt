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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.PanelCorner
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

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
    val rows = remember { groupMessages(SampleMessages) }

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

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(rows) { row ->
                when (row) {
                    is Row_.Day -> DayDivider(row.label)
                    is Row_.Message -> MessageRow(row)
                }
            }
        }

        // Composer. Disabled on purpose — a box you can type into but that cannot send
        // would be a worse lie than one that plainly is not ready.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = contentPadding.calculateBottomPadding().coerceAtLeast(12.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .glass(RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.AddCircleOutline,
                    null,
                    tint = colors.onBackdropMuted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.chat_composer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBackdropMuted,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(46.dp)
                    .glass(CircleShape)
                    .clickableTap {},
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    stringResource(R.string.chat_send),
                    tint = colors.onBackdropMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

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
            .padding(top = if (row.grouped) 0.dp else 10.dp, bottom = 1.dp),
    ) {
        // The gutter is always the avatar's width, grouped or not — that column is what
        // keeps every line of every message aligned down the page.
        Box(Modifier.width(46.dp), contentAlignment = Alignment.TopCenter) {
            if (!row.grouped) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(colors.glass),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        m.author.take(1).uppercase(),
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            if (!row.grouped) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        m.author,
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                    )
                    if (m.staff) {
                        Spacer(Modifier.width(6.dp))
                        StaffTag()
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(m.time, color = colors.onBackdropMuted, fontSize = 9.5.sp)
                }
                Spacer(Modifier.height(3.dp))
            }
            Text(
                m.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackdrop.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun StaffTag() {
    val colors = wbwColors
    Box(
        Modifier
            .clip(RoundedCornerShape(5.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            stringResource(R.string.chat_tag_staff).uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 7.5.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

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
