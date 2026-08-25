package th.ac.mfu.su.wbw.ui.staff

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * Every group, for a staff account to open the chat of.
 *
 * A participant's chat tab goes straight into their own group, because they have exactly
 * one. A staff account has none — no `participant_profile` row, so no `group_id` — and the
 * right answer to "which conversation" for somebody coordinating an event is "whichever one
 * you need right now", not one picked for them at login.
 *
 * So the tab is a list first and a conversation second. It is also the only screen in the
 * staff shell that shows the event's shape: twelve groups, how many people are in each.
 */
@Composable
fun StaffGroupsScreen(
    contentPadding: PaddingValues,
    onOpenGroup: (Group) -> Unit,
    viewModel: StaffGroupsViewModel = viewModel(factory = StaffGroupsViewModel.Factory),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Text(
            stringResource(R.string.tab_chat),
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackdrop,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.staff_groups_hint),
            color = colors.onBackdropMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))

        when {
            state.loading && state.groups.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.onBackdropMuted,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp),
                )
            }

            state.error != null && state.groups.isEmpty() -> Text(
                state.error!!,
                color = colors.danger,
                fontSize = 13.sp,
            )

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.groups, key = { it.groupId }) { group ->
                    GroupRow(group = group, onClick = { onOpenGroup(group) })
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: Group, onClick: () -> Unit) {
    val colors = wbwColors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .glass(shape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.chat_channel_group, group.groupNumber),
                color = colors.onBackdrop,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                // Out of capacity, not just a count: a staff member glancing at this is
                // usually asking whether a group has room, or whether everyone turned up.
                stringResource(R.string.staff_group_members, group.memberCount, group.capacity),
                color = colors.onBackdropMuted,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.onBackdropMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
