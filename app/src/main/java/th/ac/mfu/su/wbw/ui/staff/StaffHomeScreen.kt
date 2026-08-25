package th.ac.mfu.su.wbw.ui.staff

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.remote.dto.SosOutcome
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
                    CaseCard(
                        case = case,
                        onAck = { viewModel.ack(case.id) },
                        onReport = { viewModel.report(case.id, it) },
                    )
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
                        // Closed cases: nothing left to do, so both actions are inert.
                        CaseCard(case = case, onAck = {}, onReport = {})
                    }
                }
            }
        }
    }
}

/**
 * One case, laid out like the participant's own emergency card.
 *
 * The two are read at the same moment by the same two people — one holds up a phone, the
 * other is looking at this — so they are built the same way: the name large, the blood
 * group floated big in the top-right corner where a medic looks first, then the identifiers,
 * then who to phone. Somebody who has read one can read the other without relearning it.
 *
 * Trimmed to what a responder acts on. The participant's card carries major and school
 * because it is proving who its owner is; this one drops them — they are not in the staff
 * payload and they are not what anybody does anything with on arrival.
 *
 * The card is the nav bar's own glass, not a coloured pane. Emergencies are told apart by
 * an accent — red while something still needs people, amber once it turned out to be minor
 * or nothing — carried on the border, the status dot and the blood figure. A list of red
 * slabs has no hierarchy in it; a list of identical panes with differently coloured edges
 * can be scanned in one pass.
 */
@Composable
private fun CaseCard(case: SosStaffCase, onAck: () -> Unit, onReport: (SosOutcome) -> Unit) {
    val colors = wbwColors
    val context = LocalContext.current

    // How serious this is, as one colour, decided once.
    //
    // Amber is for the two outcomes that mean "not an emergency after all" — a false alarm
    // and a minor issue both close the case, and a closed-but-fine card sitting in the
    // recently-closed list should not still be shouting red at somebody scanning for live
    // ones.
    val accent = when {
        case.resolved && (case.resolveReason == "false_alarm" || case.resolveReason == "minor") -> CaseAmber
        case.resolved -> colors.onBackdropMuted
        case.severity == "urgent" -> CaseRedDeep
        else -> CaseRed
    }
    val live = !case.resolved

    Column(
        Modifier
            .fillMaxWidth()
            // The nav bar's material, unchanged. Only the edge carries the state.
            .glass(
                RoundedCornerShape(20.dp),
                fill = GlassSheer,
                border = if (live || case.resolveReason == "false_alarm" || case.resolveReason == "minor") {
                    accent.copy(alpha = 0.55f)
                } else {
                    GlassSheerBorder
                },
                elevation = 0.dp,
            )
            .padding(16.dp),
    ) {
        // ===== State =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(
                    when {
                        case.resolved -> R.string.staff_case_closed
                        // Severity outranks "someone is going": once a case has been seen
                        // and called major, that is the headline, not who is walking.
                        case.severity == "urgent" -> R.string.staff_case_sev_urgent
                        case.severity == "major" -> R.string.staff_case_sev_major
                        case.acknowledged -> R.string.staff_case_claimed
                        else -> R.string.staff_case_waiting
                    },
                ).uppercase(),
                color = accent,
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

        Spacer(Modifier.height(12.dp))

        // ===== Who, and the one fact a medic wants =====
        //
        // A Box rather than a Row, for the reason the participant's card gives: the blood
        // block is two lines of large type and as a Row sibling it would set the height of
        // the whole title area, leaving the name floating at the top of it.
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(end = 76.dp)) {
                Text(
                    case.displayName,
                    color = colors.onBackdrop,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                )
            }
            Column(
                Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val blood = case.bloodType?.takeIf { it.isNotBlank() }
                Text(
                    blood ?: stringResource(R.string.sos_blood_unknown),
                    color = if (blood != null) accent else colors.onBackdropMuted,
                    fontSize = if (blood != null) 30.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                )
                Text(
                    stringResource(R.string.sos_vital_blood).uppercase(),
                    color = colors.onBackdropMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.offset(y = (-5).dp),
                )
            }
        }

        // ===== Where =====
        //
        // Directly under the name, because it is the whole job. An unlocated case says so
        // plainly rather than leaving the line blank — a blank reads as a rendering fault
        // and sends somebody looking for information that does not exist.
        Spacer(Modifier.height(10.dp))
        Text(
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
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        // ===== Identifiers =====
        //
        // Bib and group side by side, as numbers rather than rows: they are the two things
        // a staff member says out loud on a radio.
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            CaseStat(stringResource(R.string.sos_vital_bib), case.bib?.toString(), Modifier.weight(1f))
            CaseStat(stringResource(R.string.sos_vital_group), case.groupNumber?.toString(), Modifier.weight(1f))
        }

        // ===== Who to phone =====
        //
        // The participant's own number first, then next of kin — the order they are tried
        // in. Next of kin is only worth the space when the case is still live; on a closed
        // card it is somebody's parent's phone number sitting on screen for no reason.
        Spacer(Modifier.height(12.dp))
        CaseVital(stringResource(R.string.sos_vital_phone), case.contactPhone)
        if (live) {
            case.emergencyContactPhone?.takeIf { it.isNotBlank() }?.let { ec ->
                CaseVital(
                    case.emergencyContactName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.sos_vital_contact),
                    ec,
                )
            }
        }

        // ===== What to do =====
        if (live) {
            val located = case.lat != null && case.lng != null
            val phone = case.contactPhone?.takeIf { it.isNotBlank() }
            if (located || phone != null) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (located) {
                        CaseAction(Icons.Outlined.Place, stringResource(R.string.staff_case_locate), accent, Modifier.weight(1f)) {
                            val label = Uri.encode(case.displayName)
                            val geo = Uri.parse("geo:${case.lat},${case.lng}?q=${case.lat},${case.lng}($label)")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
                                .onFailure {
                                    Toast.makeText(context, R.string.staff_case_no_maps, Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    if (phone != null) {
                        CaseAction(Icons.Outlined.Call, stringResource(R.string.staff_case_call), accent, Modifier.weight(1f)) {
                            // DIAL, not CALL: no CALL_PHONE permission, and a mis-tap in a
                            // pocket cannot ring somebody who is already hurt.
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }.onFailure { Toast.makeText(context, phone, Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
        }

        when {
            case.resolved -> Unit
            case.acknowledged -> {
                Spacer(Modifier.height(12.dp))
                Text(
                    case.ackedByName?.let { stringResource(R.string.staff_case_claimed_by, it) }
                        ?: stringResource(R.string.staff_case_claimed_anon),
                    color = colors.onBackdropMuted,
                    fontSize = 12.sp,
                )

                // What was actually found, once somebody is there. Only after the case is
                // claimed: offering four verdicts on a situation nobody has looked at
                // invites them to be answered from across a field, which is the guess this
                // is meant to replace.
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.staff_case_report_title).uppercase(),
                    color = colors.onBackdropMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    // Least to most serious, left to right, so the row reads as a scale.
                    // The first two are amber because they are the two that mean "not an
                    // emergency" — the colour says what pressing them claims.
                    ReportChip(R.string.staff_case_false_alarm, CaseAmber, case.severity == null, Modifier.weight(1f)) {
                        onReport(SosOutcome.FalseAlarm)
                    }
                    ReportChip(R.string.staff_case_minor, CaseAmber, case.severity == null, Modifier.weight(1f)) {
                        onReport(SosOutcome.Minor)
                    }
                    ReportChip(R.string.staff_case_major, CaseRed, case.severity == "major", Modifier.weight(1f)) {
                        onReport(SosOutcome.Major)
                    }
                    ReportChip(R.string.staff_case_urgent, CaseRedDeep, case.severity == "urgent", Modifier.weight(1f)) {
                        onReport(SosOutcome.Urgent)
                    }
                }
            }
            else -> {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .clickableNoRipple(onAck)
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.staff_case_ack).uppercase(),
                        color = Color(0xFF1B0B08),
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** A number said out loud on a radio: bib, group. Missing reads as an em dash, not zero. */
@Composable
private fun CaseStat(label: String, value: String?, modifier: Modifier = Modifier) {
    val colors = wbwColors
    Column(modifier) {
        Text(
            label.uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value?.takeIf { it.isNotBlank() } ?: "—",
            color = colors.onBackdrop,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A labelled line: what it is on the left, the value on the right. */
@Composable
private fun CaseVital(label: String, value: String?) {
    val colors = wbwColors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.onBackdropMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.sos_vital_missing),
            color = colors.onBackdrop,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * One of the four verdicts.
 *
 * [tint] is the colour of the thing being claimed, not of the card — amber for the two
 * that mean "not an emergency", red for the two that mean it is. Pressing again re-sends
 * the same verdict rather than being forbidden, because a staff member correcting
 * themselves from major to urgent is the normal case.
 */
@Composable
private fun ReportChip(
    label: Int,
    tint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) tint else tint.copy(alpha = 0.13f))
            .border(1.dp, tint.copy(alpha = if (selected) 1f else 0.42f), shape)
            .clickableNoRipple(onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(label),
            color = if (selected) Color(0xFF1B0B08) else tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * Find them, or phone them.
 *
 * Outlined rather than filled, so neither competes with the one solid control on the card
 * — which is "I am going" while a case is unclaimed, and nothing once it is.
 */
@Composable
private fun CaseAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.40f), shape)
            .clickableNoRipple(onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = colors.onBackdrop, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

/**
 * The case accents.
 *
 * Three, not two, and they are the only thing that distinguishes one card from another:
 * the pane itself is [GlassSheer], the same material as the nav bar, so a console of six
 * cases reads as one surface with six differently-edged panes rather than as a wall of red
 * slabs with no hierarchy in it.
 *
 * Amber is deliberately not a warning colour here — it means *stood down*. A false alarm
 * and a minor issue are the two outcomes that say "not an emergency after all", and a
 * closed-but-fine card sitting in the recently-closed list should stop shouting at
 * somebody scanning for live ones.
 *
 * Fixed values rather than theme tokens, like the pass and the route: these sit on a dark
 * photograph in both themes, so following the palette would make half of them vanish.
 */
private val CaseRed = Color(0xFFE8705C)
private val CaseRedDeep = Color(0xFFFF5A46)
private val CaseAmber = Color(0xFFE9B949)
