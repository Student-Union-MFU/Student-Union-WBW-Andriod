package th.ac.mfu.su.wbw.ui.staff

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.local.Session
import th.ac.mfu.su.wbw.data.remote.dto.SosOutcome
import th.ac.mfu.su.wbw.data.remote.dto.SosStaffCase
import th.ac.mfu.su.wbw.ui.common.PullRefreshBox
import th.ac.mfu.su.wbw.ui.map.SosButton
import th.ac.mfu.su.wbw.ui.map.SosFullScreen
import th.ac.mfu.su.wbw.ui.map.SosStaffIdentity
import th.ac.mfu.su.wbw.ui.map.SosViewModel
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
 * **The console also carries the staff member's own SOS.** It is the same hold, the same
 * endpoint and the same case a participant raises — `POST /wbw/me/sos` has no role gate on
 * it, and `sos_event.participant_id` references `wbw_user` rather than a participant row —
 * so a staff member who is hurt reaches the same people by the same route. It lives here
 * rather than on the map tab because this is the screen a staff account opens on and the
 * one it sits on all day, and because being on duty is not the same as being safe.
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
    // Keyed to the signed-in account: the feed excludes this account's own emergency, and
    // which account that is has to reach the view model to be excluded.
    viewModel: StaffHomeViewModel = viewModel(factory = StaffHomeViewModel.factoryFor(session.userId)),
) {
    val colors = wbwColors
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    // This account's *own* emergency, which is a different thing from the feed above it.
    //
    // [SosViewModel.StaffFactory] rather than the participant factory: same repository,
    // same idempotency key, same watch loop, but no `/me` fetch behind it — see the note on
    // its `profile` parameter.
    val sosViewModel: SosViewModel = viewModel(factory = SosViewModel.StaffFactory)
    val sos by sosViewModel.state.collectAsStateWithLifecycle()

    // Driven from the screen, so the held connection lives exactly as long as somebody is
    // looking at it — the same bargain the chat and the participant's own SOS watch make.
    LaunchedEffect(Unit) { viewModel.watch() }

    // The second watch is for this account's own case, not for the feed. It keeps running
    // when there is no case, because one can be raised from another device on the same
    // login — and because a staff member who raises one and then locks the phone must come
    // back to a screen that still says somebody is coming.
    LaunchedEffect(Unit) { sosViewModel.watch() }

    // A failed raise is the one SOS error worth interrupting somebody for: it means the
    // hold they just completed did *not* reach anybody, and the honest answer is to say so
    // rather than to leave the console looking exactly as it did before.
    LaunchedEffect(sos.error) {
        sos.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            sosViewModel.dismissError()
        }
    }

    Box(Modifier.fillMaxSize()) {
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
            // One line, one size.
            //
            // The role was a bordered pill at 11sp beside a 15sp status, which read as two
            // unrelated objects that happened to share a row — a badge, and then a sentence.
            // They are one statement: who you are and what is waiting for you. Same size, a
            // middot between them, and the border gone; the role stays muted and the count
            // takes the weight and the colour, so the sentence still has an emphasis without
            // being two components.
            //
            // The count is **not** red any more, now that the SOS pill shares this row.
            // Red was the right emphasis when this line was alone; beside a red pill twelve
            // dp away it is the same collision the pill was kept off the cards to avoid —
            // two reds on one line, at the moment there is least time to tell them apart.
            // Weight carries the emphasis instead, and the row's one red is the control
            // that means "me", which is the thing on it that must never be misread.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                    val waiting = state.waiting > 0
                    // The role gives way first. It is the half a reader already knows —
                    // they are the one wearing it — and the count is the half that changed.
                    // Without this the role takes the whole line in Thai and pushes the
                    // number that matters off the end of it.
                    Text(
                        (StaffRoleLabels[session.role.lowercase()]?.let { stringResource(it) }
                            ?: session.role) + "  ·  ",
                        color = colors.onBackdropMuted,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        if (waiting) {
                            stringResource(R.string.staff_waiting_count, state.waiting)
                        } else {
                            stringResource(R.string.staff_all_clear)
                        },
                        color = colors.onBackdrop,
                        fontSize = 15.sp,
                        fontWeight = if (waiting) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }

                // On this row, and not floating over the list.
                //
                // The participant's map puts the SOS on its own edge, away from the controls
                // pressed most often, and the equivalent here would be a red pill hovering over
                // the case cards. That is exactly where it must not be: those cards already
                // carry red — the alert line, the blood group, the verdict buttons — so a red
                // control floating among them is one more red thing to disambiguate at the
                // moment there is least time for it, and it would sit on top of the actions a
                // responder is reaching for.
                //
                // This row is the one part of the console that is about *this* staff member
                // rather than about other people's emergencies: who you are, and what is
                // waiting for you. Their own way of asking for help belongs on it. It is also
                // in the fixed header, so it neither scrolls away nor covers anything, and it
                // is findable at a glance whatever the list is doing.
                //
                // Unweighted, so it is measured at its own width before the line beside it
                // is given what is left. The text can lose a few characters to a long role
                // label; the emergency control cannot lose pixels to one.
                //
                // It is also now the only red on this row — see the note on the count.
                Spacer(Modifier.width(12.dp))
                SosButton(
                    onFire = { sosViewModel.raise(context) },
                    compact = true,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Two panels, not one scrolling list.
            //
            // Open and closed cases were stacked in a single column under a "closed recently"
            // heading, which meant the thing a responder is on this screen for — what still
            // needs somebody — got shorter the more cases had been dealt with, and eventually
            // sat above a growing pile of finished ones. They are separate views now, and the
            // console opens on the live one.
            //
            // The count rides on the tab rather than being discovered by scrolling to the end
            // of a list: "3 open" is the number somebody is actually asking for.
            var showClosed by rememberSaveable { mutableStateOf(false) }
            PanelSwitch(
                showClosed = showClosed,
                openCount = state.open.size,
                closedCount = state.recentlyClosed.size,
                onSelect = { showClosed = it },
            )

            Spacer(Modifier.height(14.dp))

            val shown = if (showClosed) state.recentlyClosed else state.open

            // A pull on top of the long poll. The feed is already live; this is for the moment
            // somebody standing over a case wants to be told so, rather than trusting a
            // connection they cannot see. See [StaffHomeViewModel.refresh].
            PullRefreshBox(
                refreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
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

                    shown.isEmpty() -> Box(
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
                            // are opposite things to a person on duty. The closed panel gets its
                            // own line: an empty one there means nothing has finished lately,
                            // which is not the same claim at all.
                            Text(
                                stringResource(
                                    if (showClosed) R.string.staff_closed_empty else R.string.staff_empty,
                                ),
                                color = colors.onBackdropMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> LazyColumn(
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(shown, key = { it.id }) { case ->
                            // A closed case has nothing left to do, so both actions are inert
                            // rather than absent — the card decides what to draw from its own
                            // state, and passing it live callbacks would be a lie about that.
                            if (showClosed) {
                                CaseCard(case = case, onAck = {}, onReport = {}, onClose = {})
                            } else {
                                CaseCard(
                                    case = case,
                                    onAck = { viewModel.ack(case.id) },
                                    onReport = { viewModel.report(case.id, it) },
                                    onClose = { viewModel.resolve(case.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // The emergency, over the console.
        //
        // Full screen, exactly as it is for a participant, and for the same reason: while
        // this account has a case open there is nothing else on this screen worth a tap.
        // The feed underneath keeps running — the long poll is not stopped — so whatever
        // arrives while a staff member is waiting for help is already there when they
        // stand down.
        //
        // It arrives rather than appearing: the one moment this screen has to feel like it
        // did something is the moment after a three-second hold.
        AnimatedVisibility(
            visible = sos.active,
            enter = fadeIn(tween(220)) +
                scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialScale = 0.92f,
                ),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f),
        ) {
            // Held, so the screen keeps its contents through the exit animation.
            sos.case?.let { open ->
                SosFullScreen(
                    case = open,
                    // No participant profile behind this account, and none coming — the
                    // card renders the staff variant from `staff` instead.
                    me = null,
                    cancelRefused = sos.cancelRefused,
                    onCancel = { sosViewModel.cancel() },
                    contentPadding = contentPadding,
                    staff = SosStaffIdentity(
                        name = session.username,
                        role = StaffRoleLabels[session.role.lowercase()]
                            ?.let { stringResource(it) } ?: session.role,
                    ),
                )
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
 * The card is the nav bar's own glass, and colour is spent on three things and nothing
 * else: the alert at the top, the blood group, and the verdict buttons. Everything that
 * carried a tint on the way here — the border, the medical note, the two actions, the
 * acknowledge — is neutral now.
 *
 * That restraint is what makes the alert work. When six things on a card are coloured, the
 * one that says whether somebody is waiting for help is just another coloured thing; when
 * it is the only red on an otherwise grey pane, it is the first thing seen. Red means this
 * still wants people, amber means it does not.
 */
@Composable
private fun CaseCard(
    case: SosStaffCase,
    onAck: () -> Unit,
    onReport: (SosOutcome) -> Unit,
    onClose: () -> Unit,
) {
    val colors = wbwColors
    val context = LocalContext.current
    // Reset by the case id, so the confirm does not survive this card being recycled onto
    // a different emergency as the list reorders.
    var confirmingClose by remember(case.id) { mutableStateOf(false) }

    // How serious this is, as one colour, decided once.
    //
    // Amber is for the two outcomes that mean "not an emergency after all" — a false alarm
    // and a minor issue both close the case, and a closed-but-fine card sitting in the
    // recently-closed list should not still be shouting red at somebody scanning for live
    // ones.
    val accent = when {
        // Every closed case is amber, whatever closed it. The distinction that matters at
        // a glance is "does this still want people", and once it does not, a false alarm
        // and a real injury that was dealt with are the same thing to somebody scanning
        // the list — both are done. The reason is still on the card for anyone who cares
        // which it was.
        case.resolved -> CaseAmber
        case.severity == "urgent" -> CaseRedDeep
        else -> CaseRed
    }
    val live = !case.resolved

    Column(
        Modifier
            .fillMaxWidth()
            // The nav bar's material, unchanged. Only the edge carries the state.
            // The nav bar's material and the nav bar's edge — no coloured border.
            //
            // A tinted edge was too quiet to be the alert and too loud to be trim: from a
            // metre away it read as a rendering artefact rather than as a state. The state
            // is said in words instead, at a size meant to be read rather than noticed.
            .glass(
                RoundedCornerShape(20.dp),
                fill = GlassSheer,
                border = GlassSheerBorder,
                elevation = 0.dp,
            )
            .padding(16.dp),
    ) {
        // ===== State =====
        // The alert line. This is the card's headline, not its caption.
        //
        // It was an 8dp dot beside 10sp of letterspaced small-caps — the treatment a label
        // gets, applied to the one line that says whether somebody is waiting for help. It
        // is 12dp and 15sp now, in the state's own colour, and it is the first thing on
        // the card that the eye lands on rather than something found after reading the
        // name. Tracking comes down as the size goes up: 1.6sp of letterspacing is what
        // makes 10sp small-caps readable and what makes 15sp look broken.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(10.dp))
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
                fontSize = 15.sp,
                letterSpacing = 0.6.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }

        // Stage one, said plainly.
        //
        // A case nobody has escalated is showing to this group's staff and to admins, and
        // to nobody else — which is a fact the person looking at it needs, because it means
        // the rest of the event does not know. Without the line the card is
        // indistinguishable from one the whole staff body is already converging on.
        if (!case.escalated && !case.resolved) {
            Text(
                stringResource(R.string.staff_case_stage_one),
                color = colors.onBackdropMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 22.dp, top = 3.dp),
            )
        }

        // Under the alert, not beside it. "Reported for someone else" is a long phrase and
        // the two cases it appears on are the two with the longest alerts — sharing a row
        // meant the headline wrapped to make space for a footnote.
        if (case.forOther) {
            Text(
                stringResource(R.string.staff_case_for_other).uppercase(),
                color = colors.onBackdropMuted,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 22.dp, top = 3.dp),
            )
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
                // Weight rather than colour. Colour on this card is spent on three things
                // only — the alert, the blood group, and the verdict buttons — and a
                // fourth coloured line would start competing with the first.
                color = colors.onBackdrop,
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
                        CaseAction(Icons.Outlined.Place, stringResource(R.string.staff_case_locate), Modifier.weight(1f)) {
                            val label = Uri.encode(case.displayName)
                            val geo = Uri.parse("geo:${case.lat},${case.lng}?q=${case.lat},${case.lng}($label)")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
                                .onFailure {
                                    Toast.makeText(context, R.string.staff_case_no_maps, Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    if (phone != null) {
                        CaseAction(Icons.Outlined.Call, stringResource(R.string.staff_case_call), Modifier.weight(1f)) {
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
                    // Nothing, then yellow, then red, then deeper red — the row is a
                    // scale, and a false alarm is the one verdict that claims no severity
                    // at all, so it carries no colour either.
                    ReportChip(R.string.staff_case_false_alarm, null, false, Modifier.weight(1f)) {
                        onReport(SosOutcome.FalseAlarm)
                    }
                    ReportChip(R.string.staff_case_minor, CaseAmber, false, Modifier.weight(1f)) {
                        onReport(SosOutcome.Minor)
                    }
                    ReportChip(R.string.staff_case_major, CaseRed, case.severity == "major", Modifier.weight(1f)) {
                        onReport(SosOutcome.Major)
                    }
                    ReportChip(R.string.staff_case_urgent, CaseRedDeep, case.severity == "urgent", Modifier.weight(1f)) {
                        onReport(SosOutcome.Urgent)
                    }
                }

                // "It is over."
                //
                // A separate act from the verdict above, and the only end for a case
                // reported major or urgent — those keep the case open on purpose, so
                // without this the console would carry them for the rest of the event.
                // Closing it as a false alarm to be rid of it would file a real emergency
                // as one that never happened.
                //
                // Two taps. This is the button that takes a live emergency off every
                // responder's screen, and a single mis-tap on a phone being carried is not
                // a thing it should be able to do.
                Spacer(Modifier.height(10.dp))
                if (confirmingClose) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CaseTextButton(stringResource(R.string.action_cancel), Modifier.weight(1f)) {
                            confirmingClose = false
                        }
                        CaseTextButton(
                            stringResource(R.string.staff_case_close_confirm),
                            Modifier.weight(1f),
                            strong = true,
                        ) {
                            confirmingClose = false
                            onClose()
                        }
                    }
                } else {
                    CaseTextButton(
                        stringResource(R.string.staff_case_close),
                        Modifier.fillMaxWidth(),
                    ) { confirmingClose = true }
                }
            }
            else -> {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        // The one solid control on the card, and deliberately colourless:
                        // it is the same press whatever state the case is in, and the
                        // alert above has already said how bad that state is.
                        .background(colors.onBackdrop)
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

/**
 * The two panels' switch.
 *
 * A segmented pair rather than the nav bar's tabs, because these are two views of one
 * screen and not two destinations — going from open to closed cases should not be
 * something the back button undoes.
 *
 * Counts live on the labels. The number of open cases is the single figure a person on
 * duty wants, and putting it here means it is answered without scrolling to the bottom of
 * a list to count cards.
 */
@Composable
private fun PanelSwitch(
    showClosed: Boolean,
    openCount: Int,
    closedCount: Int,
    onSelect: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .fillMaxWidth()
            .glass(shape, fill = GlassSheer, border = GlassSheerBorder, elevation = 0.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Neither tab is coloured. The switch says which list you are looking at, not how
        // bad anything is — the cards do that, and a red pill up here was a second alarm
        // competing with the real ones a few dp below it.
        PanelTab(
            label = stringResource(R.string.staff_panel_open, openCount),
            selected = !showClosed,
            modifier = Modifier.weight(1f),
        ) { onSelect(false) }
        PanelTab(
            label = stringResource(R.string.staff_panel_closed, closedCount),
            selected = showClosed,
            modifier = Modifier.weight(1f),
        ) { onSelect(true) }
    }
}

@Composable
private fun PanelTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val shape = RoundedCornerShape(50)
    val fill = colors.onBackdrop
    Box(
        modifier
            .clip(shape)
            .background(if (selected) fill.copy(alpha = 0.92f) else Color.Transparent)
            .clickableNoRipple(onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            // Dark ink on the filled tab, whichever colour it is: both the red and the
            // cream are light, and light-on-light is the mistake this screen has already
            // made once.
            color = if (selected) Color(0xFF1B0B08) else colors.onBackdropMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/**
 * A quiet control on a card whose loud one is the verdict row.
 *
 * Colourless like everything else here except the alert, the blood group and the verdicts
 * — closing a case is an administrative act, not an alarm, and it should not compete with
 * the line saying somebody needs help.
 */
@Composable
private fun CaseTextButton(
    label: String,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val ink = colors.onBackdrop
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (strong) ink.copy(alpha = 0.9f) else ink.copy(alpha = 0.08f))
            .border(1.dp, ink.copy(alpha = if (strong) 0.9f else 0.24f), shape)
            .clickableNoRipple(onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (strong) colors.forestVoid else ink,
            fontSize = 12.sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
        )
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
    tint: Color?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val shape = RoundedCornerShape(50)
    // Glass rather than a flat wash, so the chips are the same material as everything else
    // on the screen and the tint is something the backdrop shows through rather than a
    // painted swatch sitting on top of it.
    val base = Modifier.glass(
        shape,
        fill = tint?.copy(alpha = 0.20f) ?: GlassSheer,
        border = tint?.copy(alpha = 0.50f) ?: GlassSheerBorder,
        elevation = 0.dp,
    )
    Box(
        modifier
            .then(if (selected) Modifier.background(tint ?: colors.onBackdrop, shape) else base)
            .clip(shape)
            .clickableNoRipple(onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(label),
            color = when {
                selected -> Color(0xFF1B0B08)
                tint != null -> tint
                else -> colors.onBackdrop
            },
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    val ink = colors.onBackdrop
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(ink.copy(alpha = 0.10f))
            .border(1.dp, ink.copy(alpha = 0.30f), shape)
            .clickableNoRipple(onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = colors.onBackdrop, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
