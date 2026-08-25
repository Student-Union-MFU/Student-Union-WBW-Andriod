package th.ac.mfu.su.wbw.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.remote.dto.SosStaffCase
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * The staff home: every emergency this account is allowed to see, newest and unclaimed
 * first.
 *
 * This is a console, not a dashboard. It shows one kind of thing and shows all of it,
 * because the alternative — tiles of counts with the cases a tap away — puts a screen
 * between a responder and the only information on it that matters. Nothing here is a
 * summary of something else.
 *
 * Which cases arrive is the server's decision, not a filter offered here: a staff member
 * sees their own checkpoint, cases with no checkpoint at all, checkpoints nobody is assigned
 * to, and anything whose position is too coarse to attribute — with admin, medical and
 * security seeing everything. Fail-open, deliberately: an emergency shown to too many
 * people is a bad afternoon, and one shown to nobody is the thing this exists to prevent.
 */
@Composable
fun StaffHomeScreen(
    session: Session,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    viewModel: StaffHomeViewModel = viewModel(factory = StaffHomeViewModel.Factory),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Driven from the screen, so the held connection lives exactly as long as somebody is
    // looking at it — the same bargain the chat and the participant's own SOS watch make.
    LaunchedEffect(Unit) { viewModel.watch() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        // The greeting and the settings button share one row, and the role pill sits on
        // its own beneath.
        //
        // They used to be a two-line column with the button centred against the whole of
        // it, which put the button's midpoint level with the *gap* between the greeting
        // and the pill — so the name rode visibly above it and nothing on the row lined
        // up with anything else. Centring works when both sides are one line; when one
        // side is a stack, the thing to align to is its first line.
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The username, because it is the only name this shell has. A staff account
            // has no `participant_profile`, so there is no first name to greet them by
            // and no request that would fetch one — see [StaffScaffold].
            Text(
                stringResource(R.string.staff_greeting, session.username),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackdrop,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(42.dp)
                    .glass(CircleShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
                    .clip(CircleShape)
                    .padding(10.dp),
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    stringResource(R.string.settings_title),
                    tint = colors.onBackdrop,
                    modifier = Modifier.fillMaxSize().clickableNoRipple(onOpenSettings),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // The line under the greeting is the count of emergencies nobody has claimed yet —
        // the single most important number on this screen, and it was set at 12sp beside a
        // 9sp pill, which is caption treatment for a figure that means somebody is waiting
        // for help. It is now the size of a thing you are meant to read from arm's length,
        // and it says what it is counting rather than assuming the reader knows.
        Row(verticalAlignment = Alignment.CenterVertically) {
            RolePill(session.role)
            Spacer(Modifier.width(9.dp))
            Text(
                if (state.waiting > 0) {
                    stringResource(R.string.staff_waiting_count, state.waiting)
                } else {
                    stringResource(R.string.staff_all_clear)
                },
                color = if (state.waiting > 0) colors.danger else colors.onBackdropMuted,
                fontSize = 15.sp,
                fontWeight = if (state.waiting > 0) FontWeight.SemiBold else FontWeight.Normal,
            )
        }

        Spacer(Modifier.height(18.dp))

        when {
            state.loading && state.cases.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = colors.onBackdropMuted,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp),
                )
            }

            state.open.isEmpty() && state.recentlyClosed.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        null,
                        tint = colors.onBackdropMuted,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    // Says what it means. "No data" on this screen would be ambiguous
                    // between "nobody needs help" and "the feed is not working", and those
                    // are opposite things to a person on duty.
                    Text(
                        stringResource(R.string.staff_empty),
                        color = colors.onBackdropMuted,
                        fontSize = 13.sp,
                    )
                }
            }

            else -> LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.open, key = { it.id }) { case ->
                    CaseCard(case = case, onAck = { viewModel.ack(case.id) })
                }
                if (state.recentlyClosed.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.staff_recently_closed).uppercase(),
                            color = colors.onBackdropMuted,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(state.recentlyClosed, key = { it.id }) { case ->
                        CaseCard(case = case, onAck = {})
                    }
                }
            }
        }
    }
}

/**
 * One case.
 *
 * Ordered by what a responder does with it: who and where first, then how to reach them,
 * then the medical note if there is one, then the one action. The blood type and notes are
 * last rather than hidden — they matter on arrival, not while deciding whether to go.
 */
@Composable
private fun CaseCard(case: SosStaffCase, onAck: () -> Unit) {
    val colors = wbwColors
    val urgent = !case.resolved && !case.acknowledged

    Column(
        Modifier
            .fillMaxWidth()
            .glass(
                RoundedCornerShape(20.dp),
                // Unclaimed cases carry the danger tint; claimed and closed ones drop back
                // to ordinary glass. The colour tracks "does this still need somebody",
                // which is the only question the list is being scanned for.
                fill = if (urgent) UrgentGlass else GlassSheer,
                border = if (urgent) UrgentGlassBorder else GlassSheerBorder,
                elevation = 0.dp,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (urgent) colors.danger else colors.onBackdropMuted),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(
                    when {
                        case.resolved -> R.string.staff_case_closed
                        case.acknowledged -> R.string.staff_case_claimed
                        else -> R.string.staff_case_waiting
                    },
                ).uppercase(),
                color = if (urgent) colors.danger else colors.onBackdropMuted,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            if (case.forOther) {
                Text(
                    stringResource(R.string.staff_case_for_other).uppercase(),
                    color = colors.onBackdropMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            case.displayName,
            color = colors.onBackdrop,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            listOfNotNull(
                case.bib?.let { stringResource(R.string.staff_case_bib, it) },
                case.groupNumber?.let { stringResource(R.string.staff_case_group, it) },
                case.bloodType?.takeIf { it.isNotBlank() },
            ).joinToString("  ·  "),
            color = colors.onBackdropMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(10.dp))
        Text(
            // "Where" is the whole job. An unlocated case says so plainly rather than
            // leaving the line blank, because a blank reads as a rendering fault and sends
            // somebody looking for information that does not exist.
            case.checkpointName?.let { stringResource(R.string.staff_case_near, it) }
                ?: stringResource(R.string.staff_case_nowhere),
            color = colors.onBackdrop,
            fontSize = 13.sp,
        )
        case.message?.takeIf { it.isNotBlank() }?.let {
            Text(
                "“$it”",
                color = colors.onBackdrop,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        case.healthNotes?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        case.contactPhone?.takeIf { it.isNotBlank() }?.let {
            Text(
                stringResource(R.string.staff_case_phone, it),
                color = colors.onBackdropMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        when {
            case.resolved -> Unit
            case.acknowledged -> Text(
                case.ackedByName?.let { stringResource(R.string.staff_case_claimed_by, it) }
                    ?: stringResource(R.string.staff_case_claimed_anon),
                color = colors.onBackdropMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            else -> {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(colors.danger.copy(alpha = 0.22f))
                        .border(1.dp, colors.danger.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickableNoRipple(onAck)
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.staff_case_ack).uppercase(),
                        color = colors.danger,
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** The account type, as a small outlined pill. */
@Composable
private fun RolePill(role: String) {
    val colors = wbwColors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            (StaffRoleLabels[role.lowercase()]?.let { stringResource(it) } ?: role).uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private val StaffRoleLabels = mapOf(
    "participant" to R.string.role_participant,
    "staff" to R.string.role_staff,
    "admin" to R.string.role_admin,
)

/** No ripple, like every other tap target in this app. */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) })

/** Unclaimed-case glass — the same idea as the SOS button's tint, on a card. */
private val UrgentGlass = Color(0x24E8544A)
private val UrgentGlassBorder = Color(0x40E8735F)
