package th.ac.mfu.su.wbw.ui.profile

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.ui.common.ErrorState
import th.ac.mfu.su.wbw.ui.common.QrCode
import th.ac.mfu.su.wbw.ui.common.LoadingState
import th.ac.mfu.su.wbw.ui.common.UiState
import th.ac.mfu.su.wbw.ui.theme.Kanit
import th.ac.mfu.su.wbw.ui.theme.PassFaint
import th.ac.mfu.su.wbw.ui.theme.PassHairline
import th.ac.mfu.su.wbw.ui.theme.PassInk
import th.ac.mfu.su.wbw.ui.theme.PassMuted
import th.ac.mfu.su.wbw.ui.theme.PassSurface
import th.ac.mfu.su.wbw.ui.theme.PassWell
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * The pass reads as one frosted pane over the forest rather than as a paper ticket.
 *
 * Everything on it is white on glass, in both themes, for the same reason the old
 * version pinned a cream palette: the pass is a fixed design you hold up, not a surface
 * that follows the user's appearance setting. What changed is which fixed design — the
 * printed-ticket look (cream stock, perforation, barcode, gradient stubs) fought the
 * backdrop, because a piece of paper laid over a photograph never belongs to it.
 *
 * The layout is editorial: one hairline rule system, one accent, a rotated masthead down
 * the edge, and a lot of air. Weight carries the hierarchy instead of colour, which is
 * what lets a single white do the work of the old palette's seven tones.
 */
// The pass's colours moved to the palette when the login screen took up the same
// vocabulary — see the "pass" block in Color.kt.

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is UiState.Success -> ProfileContent(s.data, contentPadding, onBack, onOpenSettings)
        }
    }
}

@Composable
private fun ProfileContent(
    p: ParticipantDetail,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        // No title bar. This screen is opened from the QR button in the nav bar and closed
        // again, so it needs a way back and nothing else — a header repeating "Participant
        // Pass" over a pass that already says so was spending the top of the screen on
        // nothing.
        //
        // Settings keeps its button here even though Home's corner now reaches it too.
        // Two doors to a screen nobody visits often is not clutter; it is one fewer thing
        // to remember, and this one is already drawn.
        //
        // Outside the scroll on purpose: the pass is taller than the screen, and a back
        // button that scrolls away is one you have to scroll back up to reach.
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(R.string.action_back),
                    tint = PassInk,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            CircleButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    stringResource(R.string.settings_title),
                    tint = PassInk,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(Modifier.verticalScroll(rememberScrollState()).padding(contentPadding)) {
            Pass(p)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .glass(CircleShape, fill = PassSurface)
            .clickableTap(onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Pass(p: ParticipantDetail) {
    val colors = wbwColors
    val shape = RoundedCornerShape(28.dp)
    val total = 8
    val done = if (p.checkedIn) 3 else 0

    Row(
        Modifier
            .fillMaxWidth()
            .glass(shape, fill = PassSurface, elevation = 18.dp)
            .padding(start = 24.dp, end = 10.dp, top = 26.dp, bottom = 24.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Kicker(stringResource(R.string.profile_pass_title))

            // The event dates used to sit here in a pill of their own. They are the one
            // fact on the pass that is identical on every pass ever issued — it is a
            // two-day event — so the card was opening by telling the holder something
            // about the event rather than about them.
            //
            // The whole row goes when there is no group, rather than staying as an empty
            // box: it exists to hold pills, and its top padding would otherwise leave a
            // gap that reads as a missing element.
            p.groupNumber?.let {
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinePill(stringResource(R.string.profile_group_n, it))
                }
            }

            // Identity and QR share a row: they are the two things a marshal looks at,
            // and the QR is the one that gets held up. Sitting it beside the name rather
            // than in its own block above lets it be larger and puts the code next to
            // the person it belongs to.
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 14.dp)) {
                    // The name is the headline, the way the reference sets its title —
                    // the largest thing on the panel after the bib.
                    Text(
                        p.fullName,
                        color = PassInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 31.sp,
                    )
                    p.schoolName?.let {
                        Text(it, color = PassMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    // The major, under the school it belongs to. Same size, one step down
                    // the ink scale — they are the same kind of fact at two levels of
                    // detail, so the type stays put and only the strength moves. A second
                    // 12sp line at full muted would have read as a second school.
                    //
                    // Tighter to the school than the school is to the name (4dp against 8),
                    // because the two of them are one address and should read as one block
                    // rather than two facts that happen to be adjacent.
                    p.major?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = PassFaint, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    // The student id, bare. It read "Student 6931503028" at 11sp, which
                    // spent most of its width telling you what a ten-digit number starting
                    // 693 already is — on a card headed "Participant Pass", next to a
                    // school name, at a university event. The digits are the part anyone
                    // needs to read out or copy down, so they get the space the label was
                    // using.
                    //
                    // Three things were making it hard to see and only one of them was the
                    // size. It was set in `colors.accent`, which is a *theme* token: near
                    // white on a dark card, near black (#1B2A1B) on a light one — and this
                    // pass is a fixed dark design in both themes, so half the users had
                    // near-black digits on dark glass. [PassInk] is the pass's own scale
                    // and does not move. And it is Kanit now, the app's numeral face, the
                    // same one the bib below is set in: its digits are squarer and hold
                    // apart at a glance in a way Sarabun's do not.
                    //
                    // 18sp sits between the school line and the name on purpose. Bigger
                    // than it was by half again, and still visibly not the headline.
                    p.studentId?.let {
                        Text(
                            it,
                            color = PassInk,
                            fontFamily = Kanit,
                            fontSize = 18.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                // The one solid thing amongst the glass, and deliberately the brightest
                // rectangle on the panel — a code that will be scanned in low light
                // under trees should not be competing with a translucent surface.
                //
                // This was an `Icons.Outlined.QrCode2` until now: a Material *drawing* of a
                // QR code, identical on every participant's pass, encoding nothing. It
                // looked exactly like the real thing and would have scanned as nothing at
                // all, at a checkpoint, on the day.
                //
                // What it encodes is `qr_token`, never the id or the student number — see
                // [ParticipantDetail.qrToken]. Pure black on pure white rather than the
                // panel's greens: contrast is the whole job here, and a scanner has no
                // opinion about the design system.
                //
                // No token means no block. A participant whose row predates the column
                // checks in by bib, which the server supports; drawing an empty white
                // square would suggest a code that failed to load and invite them to stand
                // there waiting for it.
                p.qrToken?.takeIf { it.isNotBlank() }?.let { token ->
                    val label = stringResource(R.string.profile_qr_label)
                    Box(
                        Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PassInk)
                            // Labelled on the container, because the code itself is a
                            // Canvas and a screen reader would otherwise walk straight past
                            // the most important object on the pass without announcing it.
                            // The label names the thing; reading out a 24-character hex
                            // token would help nobody.
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCode(
                            content = token,
                            foreground = Color(0xFF16241A),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Rule(Modifier.padding(top = 20.dp))

            // BIB, set as a number rather than a badge — it is the other thing on this
            // screen someone reads out loud, so it gets display size too.
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Kicker(stringResource(R.string.profile_bib_number))
                    Text(
                        p.bib?.toString() ?: "—",
                        fontFamily = Kanit,
                        color = PassInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (p.checkedIn) FilledPill(stringResource(R.string.profile_checkin_status))
            }

            Rule(Modifier.padding(top = 18.dp))

            // Trail stamps: eight cells, filled ones solid. The old version drew a tree
            // glyph in every cell and a second progress bar underneath — two readings of
            // one number, in a design that has room for neither.
            // Just the label now. The "3 / 8 · Sapling" readout that sat opposite it was a
            // third telling of a number the eight cells below already show, after Home's
            // bloom and Home's own count — and the growth-phase word belongs to the flower,
            // which is not on this card.
            Box(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Kicker(stringResource(R.string.profile_trail_stamps))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(total) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (i < done) PassInk else PassWell),
                    )
                }
            }

            Rule(Modifier.padding(top = 20.dp))

            Column(Modifier.padding(top = 6.dp)) {
                DetailRow(stringResource(R.string.profile_label_blood), p.bloodType ?: "—")
                DetailRow(stringResource(R.string.profile_row_height_weight), heightWeight(p))
                DetailRow(stringResource(R.string.profile_row_contact_phone), p.contactPhone ?: "—")
                DetailRow(
                    stringResource(R.string.profile_section_emergency),
                    listOfNotNull(p.emergencyContactName, p.emergencyContactPhone).joinToString(" · ").ifBlank { "—" },
                    last = true,
                )
            }
        }

        // The masthead, running up the edge. Carries the event name and the OFFICIAL
        // mark that used to need a tree icon and a stub header to say the same thing.
        Column(
            Modifier.padding(start = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VerticalLabel(stringResource(R.string.profile_event_name).uppercase(), PassMuted)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.width(1.dp).height(46.dp).background(PassHairline))
            Spacer(Modifier.height(16.dp))
            VerticalLabel(stringResource(R.string.profile_official), PassFaint)
        }
    }
}

/** Small letterspaced uppercase label — the panel's only secondary type style. */
@Composable
private fun Kicker(text: String) {
    Text(
        text.uppercase(),
        color = PassFaint,
        fontSize = 8.5.sp,
        letterSpacing = 3.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * The same label turned on its side.
 *
 * `layout` swaps the measured axes before the rotation, so the row reserves the glyphs'
 * height as its width. Rotating alone leaves the original horizontal box in the layout
 * and the text overhangs the panel.
 */
@Composable
private fun VerticalLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 8.5.sp,
        letterSpacing = 4.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(Constraints(0, constraints.maxHeight, 0, constraints.maxWidth))
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = -(placeable.width / 2 - placeable.height / 2),
                        y = -(placeable.height / 2 - placeable.width / 2),
                    )
                }
            }
            .rotate(90f),
    )
}

/** Hairline-outlined chip, straight from the reference's button treatment. */
@Composable
private fun OutlinePill(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .border(1.dp, PassHairline, CircleShape)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    ) {
        Text(text.uppercase(), color = PassMuted, fontSize = 8.5.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** The one solid-filled chip on the panel — the reference uses exactly one too. */
@Composable
private fun FilledPill(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(PassInk)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text.uppercase(), color = Color(0xFF16241A), fontSize = 8.5.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(PassHairline))
}

@Composable
private fun DetailRow(label: String, value: String, last: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label.uppercase(), color = PassFaint, fontSize = 8.5.sp, letterSpacing = 1.6.sp, modifier = Modifier.padding(top = 2.dp))
        Text(
            value,
            color = PassInk,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
    if (!last) Rule()
}

@Composable
private fun heightWeight(p: ParticipantDetail): String {
    val h = p.heightCm?.let { fmt(it) }
    val w = p.weightKg?.let { fmt(it) }
    return if (h != null && w != null) stringResource(R.string.profile_hw_value, h, w) else "—"
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
