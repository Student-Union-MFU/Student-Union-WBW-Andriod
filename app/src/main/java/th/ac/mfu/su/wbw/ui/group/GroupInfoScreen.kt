package th.ac.mfu.su.wbw.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.GroupMember
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.WbwAvatars
import th.ac.mfu.su.wbw.ui.theme.WbwGreenDark
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * A group, as a page.
 *
 * Reached from the button in the corner of the chat, which is where somebody asks "who
 * else is in here" — a question the chat header could count but never answer. It carries
 * the roster and the one irreversible thing a participant can do about their group.
 *
 * [canLeave] is false for a staff account, which reaches this screen through the group
 * picker in its own shell and is not a member of anything. The distinction is the caller's
 * to make: this screen would otherwise have to infer membership from a roster it is also
 * displaying, and get it wrong for a staff member who happens to share a user id with
 * nobody in the list.
 */
@Composable
fun GroupInfoScreen(
    groupId: Int,
    groupNumber: Int?,
    canLeave: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: GroupInfoViewModel = viewModel(factory = GroupInfoViewModel.factoryFor(groupId)),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .glass(CircleShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(R.string.action_back),
                    tint = colors.onBackdrop,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    groupNumber?.let { stringResource(R.string.chat_channel_group, it) }
                        ?: stringResource(R.string.chat_channel),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackdrop,
                )
                Text(
                    stringResource(R.string.chat_members, state.count),
                    color = colors.onBackdropMuted,
                    fontSize = 12.sp,
                )
            }
        }

        when {
            state.loading && state.members.isEmpty() -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.onBackdropMuted,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp),
                )
            }

            state.members.isEmpty() -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.error ?: stringResource(R.string.chat_members_empty),
                    color = if (state.error != null) colors.danger else colors.onBackdropMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                Modifier.weight(1f),
                // Enough of a gap that the cards read as separate people.
                //
                // 2dp put a hairline between two glass panes of the same colour, which at a
                // glance is one long pane with lines ruled across it — a roster is a list of
                // people, and each of them should look like one entry.
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.members, key = { it.userId }) { m ->
                    MemberLine(member = m, isMe = m.userId == state.meId)
                }
            }
        }

        // ===== The way out =====
        if (canLeave) {
            Spacer(Modifier.height(10.dp))
            state.leaveError?.let {
                Text(
                    it,
                    color = colors.danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (!state.quotaKnown) {
                // Nothing is claimed until `/me` has answered. Showing the spent message
                // here would be a guess, and it is the guess that costs a participant a
                // move they still had.
                Spacer(Modifier.height(12.dp))
            } else if (state.leaveQuota > 0) {
                // Two taps, and the second one says what it costs.
                //
                // Leaving spends the participant's single move, and the server will not
                // give it back — so the button that does it is not the button they find
                // first. The confirm step is where the price is written, because a warning
                // nobody has asked to see is a warning nobody reads.
                if (confirming) {
                    // The real remaining count, not the word "once".
                    //
                    // The quota is a column an admin can set between 0 and 10, so a
                    // sentence that hard-codes "once" is right for the default and wrong
                    // for anybody who was granted another — and it is wrong in the
                    // direction that costs them a move they actually had.
                    Text(
                        pluralStringResource(
                            R.plurals.group_leave_warning,
                            state.leaveQuota,
                            state.leaveQuota,
                        ),
                        color = colors.onBackdrop,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LeaveButton(
                            label = stringResource(R.string.action_cancel),
                            danger = false,
                            modifier = Modifier.weight(1f),
                        ) { confirming = false }
                        LeaveButton(
                            label = stringResource(R.string.group_leave_confirm),
                            danger = true,
                            busy = state.leaving,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.leave() }
                    }
                } else {
                    LeaveButton(
                        label = stringResource(R.string.group_leave),
                        danger = false,
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        modifier = Modifier.fillMaxWidth(),
                    ) { confirming = true }
                }
            } else {
                // Not a disabled button. A control that cannot be pressed invites pressing
                // it; a sentence says why there is nothing to press.
                Text(
                    stringResource(R.string.group_leave_spent),
                    color = colors.onBackdropMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            }
        }

        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 12.dp))
    }
}

/** One member: name, whether it is you, school under it, bib on the right. */
@Composable
private fun MemberLine(member: GroupMember, isMe: Boolean) {
    val colors = wbwColors
    val name = listOfNotNull(member.firstName, member.lastName)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifBlank { member.bib?.let { "BIB $it" } ?: member.userId.take(8) }

    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(14.dp), fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The chosen avatar, in the same disc chat uses.
        //
        // Nothing where there is no choice, rather than a placeholder: the roster is a list
        // of names and a column of empty circles beside half of them would be a column of
        // absences. Chat can afford the fallback initial because the disc is load-bearing
        // there — it is what separates one speaker's run of messages from the next.
        WbwAvatars.glyph(member.avatar)?.let { glyph ->
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(WbwGreenDark.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, fontSize = 17.sp)
            }
            Spacer(Modifier.width(11.dp))
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    color = colors.onBackdrop,
                    fontSize = 14.sp,
                    fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isMe) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.chat_members_you),
                        color = colors.onBackdropMuted,
                        fontSize = 11.sp,
                    )
                }
            }
            member.school?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = colors.onBackdropMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        member.bib?.let {
            Spacer(Modifier.width(10.dp))
            Text(
                "$it",
                color = colors.onBackdropMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun LeaveButton(
    label: String,
    danger: Boolean,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val ink = if (danger) colors.danger else colors.onBackdrop
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(ink.copy(alpha = if (danger) 0.16f else 0.10f))
            .border(1.dp, ink.copy(alpha = if (danger) 0.5f else 0.28f), shape)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            CircularProgressIndicator(color = ink, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            icon?.let {
                Icon(it, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
