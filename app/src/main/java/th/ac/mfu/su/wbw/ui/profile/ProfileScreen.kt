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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.TextUnit
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
import th.ac.mfu.su.wbw.ui.theme.Numerals
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
            // A "GROUP 14" outline pill sat here after them, and has moved down beside
            // the bib — see that row for why. Nothing replaces it: this is the top of the
            // card, and the pass reads better opening on the holder's name than on a
            // chip repeating a number that now has display size of its own.

            // Identity and QR share a row: they are the two things a marshal looks at,
            // and the QR is the one that gets held up. Sitting it beside the name rather
            // than in its own block above lets it be larger and puts the code next to
            // the person it belongs to.
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 14.dp)) {
                    // The name is the headline, the way the reference sets its title —
                    // the largest thing on the panel after the bib. Its size steps down as
                    // the name grows; see [nameSizeFor] for why the step is chosen the way
                    // it is. Deliberately no `maxLines`: a long name is allowed to take two
                    // or three lines, because the alternative is truncating somebody's name
                    // on the card that exists to identify them.
                    val nameSize = nameSizeFor(p.fullName)
                    Text(
                        p.fullName,
                        color = PassInk,
                        fontWeight = FontWeight.Medium,
                        fontSize = nameSize,
                        lineHeight = nameSize * 1.12f,
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
                    // and does not move. And it is Numerals now, the app's numeral face, the
                    // same one the bib below is set in: its digits are round and evenly
                    // weighted, and hold apart at a glance in a way the body face does not.
                    //
                    // 18sp sits between the school line and the name on purpose. Bigger
                    // than it was by half again, and still visibly not the headline.
                    p.studentId?.let {
                        Text(
                            it,
                            color = PassInk,
                            fontFamily = Numerals,
                            fontSize = 18.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Normal,
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

            // BIB and GROUP, set as numbers rather than badges — they are the two things
            // on this screen someone reads out loud, so they get display size too.
            //
            // The group sits here, beside the bib, rather than in a pill up by the title
            // where it used to live. They are one question asked twice — *which* walker,
            // and which of the forty groups to send them back to — and a marshal holding
            // the card wants both in the same glance, not one at the top and one at the
            // bottom. Putting them side by side is also what stops the card telling the
            // same fact in two registers, which is the thing the rest of this panel has
            // been pruned to avoid.
            //
            // Both at the same size. An earlier pass set the group smaller, on the theory
            // that the bib is the hero — it is the number shouted across a valley. But the
            // two sit side by side under matching labels, which makes them read as a pair,
            // and a pair at two sizes reads as a mistake rather than as a ranking. The
            // labels already say which is which, so the type does not need to, and equal
            // figures let the digits share a baseline instead of stepping.
            // Aligned at the top, not the bottom. Bottom-aligning two columns whose
            // figures are different sizes lines the *numbers* up but steps their labels
            // down the card, and the labels are the part that reads as a row. Top puts
            // the two kickers on one line, which is what makes them look like two fields
            // of one record; the pill takes Bottom for itself so it still sits with the
            // numbers rather than up against the labels.
            Row(
                Modifier.padding(top = 16.dp).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Kicker(stringResource(R.string.profile_bib_number))
                    Text(
                        p.bib?.toString() ?: "—",
                        fontFamily = Numerals,
                        color = PassInk,
                        fontWeight = FontWeight.Normal,
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // The same hairline the masthead uses, so the two figures read as two
                // fields of one card rather than as two things that happen to be near
                // each other.
                Box(
                    Modifier
                        // Height comes from the row rather than a number picked by eye.
                        // A fixed 34dp was tuned against the bib's line box and sat above
                        // the figures instead of beside them; `IntrinsicSize.Min` on the
                        // row makes this exactly as tall as the taller of the two fields,
                        // so it keeps bracketing them if either type size ever moves.
                        .padding(horizontal = 16.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(PassHairline),
                )

                Column {
                    Kicker(stringResource(R.string.profile_label_group))
                    Text(
                        // A dash rather than a hidden column when there is no group yet.
                        // The holder of a pass with no group needs to see that something
                        // is missing — the gate will ask them for one on next launch —
                        // and an absent field just looks like a card that never had one.
                        p.groupNumber?.toString() ?: "—",
                        fontFamily = Numerals,
                        color = PassInk,
                        fontWeight = FontWeight.Normal,
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                if (p.checkedIn) {
                    Box(Modifier.align(Alignment.Bottom).padding(bottom = 6.dp)) {
                        FilledPill(stringResource(R.string.profile_checkin_status))
                    }
                }
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
/**
 * Headline size for a participant's name.
 *
 * Stepped from the **longest single word**, not from the total length, because the two
 * cause different problems and only one of them is a defect. Total length decides how many
 * lines a name takes, and a name is allowed two or three. A word that cannot fit the column
 * at any break point is the actual failure: Compose has nowhere legal to break it and splits
 * mid-word, which turned "Kanyarat Thongchaikoson Wattanapongse" into
 * "Thongchaikos / on" and "Wattanapongs / e" — a name cut in half at an arbitrary letter.
 *
 * The column is roughly 190dp: the card, less the QR sitting beside it and the gutter
 * between them. That is about thirteen characters of 28sp Athiti Medium, which is why the
 * first step ends there. The total-length bound on each step is a second, looser guard — it
 * keeps a name of many short words from running to five lines even when no single word is
 * wide enough to trigger the word bound on its own.
 *
 * Splitting on space only, rather than on any whitespace: this is fed by [ParticipantDetail.fullName],
 * which joins the name parts with exactly one.
 *
 * These bounds have moved twice with the body face, which is the point worth remembering:
 * they encode a specific face's advance widths, not a general rule. Sarabun's 12 tightened
 * to 11 for Prompt, which sets wider; Athiti is narrower than either — measured at 0.445 em
 * against Prompt's 0.535 — so they open back up to 13. Character count is only a proxy for
 * width, so each step keeps a margin rather than being fitted to whichever name happened to
 * be on screen. **If the body face changes again, re-measure and re-check these numbers.**
 *
 * Re-measured when the scale stepped up a weight, because a weight moves advance widths too
 * and this is exactly the kind of change that silently invalidates the numbers: Athiti Medium
 * runs 2.2% wider than Regular across Latin and 1.1% across Thai. A worst-case thirteen-letter
 * word at 28sp comes to about 177dp against the ~190dp column, so 13 survives on the margin
 * that was left there deliberately — but it is now a 7% margin rather than a 9% one, and a
 * further step would not fit.
 *
 * Thai needs no special case, and gets none. A Thai name part carries no spaces inside it, so
 * it counts here as one long word and lands on a smaller step — which is the safe direction,
 * and slightly over-cautious besides, since `length` counts combining vowels and tone marks
 * that take no width of their own. The wrapping itself is Android's: the platform line
 * breaker knows Thai and splits at real word boundaries inside the run, so
 * `ณัฐวุฒิ ศรีสุวรรณวัฒนา` breaks as `ศรีสุวรรณ / วัฒนา` with no help from this function.
 * Verified on device rather than assumed.
 */
private fun nameSizeFor(name: String): TextUnit {
    val longestWord = name.split(' ').maxOfOrNull { it.length } ?: 0
    return when {
        longestWord <= 13 && name.length <= 26 -> 28.sp
        longestWord <= 18 && name.length <= 38 -> 22.sp
        else -> 18.sp
    }
}

@Composable
private fun Kicker(text: String) {
    Text(
        text.uppercase(),
        color = PassFaint,
        fontSize = 8.5.sp,
        letterSpacing = 3.sp,
        fontWeight = FontWeight.Medium,
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
        fontWeight = FontWeight.Medium,
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
/** The one solid-filled chip on the panel — the reference uses exactly one too. */
@Composable
private fun FilledPill(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(PassInk)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text.uppercase(), color = Color(0xFF16241A), fontSize = 8.5.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium)
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
            // Normal, not SemiBold, and slightly larger to pay for it. These four rows are
            // reference data — blood type, height, a phone number — read once by somebody
            // who already went looking for them. They were set at the same weight as the
            // name, which made the bottom of the card shout as loudly as the top.
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
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
