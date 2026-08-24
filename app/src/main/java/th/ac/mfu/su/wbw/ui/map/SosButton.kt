package th.ac.mfu.su.wbw.ui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.common.QrCode
import th.ac.mfu.su.wbw.data.remote.dto.ParticipantDetail
import th.ac.mfu.su.wbw.data.remote.dto.SosCase
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * Hold to call for help.
 *
 * **A hold, not a tap, and not a confirmation dialog.** The three obvious designs each fail
 * this particular button in a different way. A plain tap in a thumb-sized target on a map
 * that is panned and pinched all day would fire by accident, and a false emergency on this
 * event costs a staff member an actual run up an actual hill. A tap plus an "are you sure?"
 * sheet fixes that by putting a second screen between a hurt person and the only button
 * that matters, and it fails closed at the worst moment — one-handed, in the rain, with the
 * phone at an angle. A hold is a single continuous gesture that cannot happen by accident
 * and cannot be half-completed: let go early and nothing was sent.
 *
 * [HoldMillis] matches what the server's design assumes — it expects the app to fire the
 * moment a three-second hold completes, without waiting for a position, and to send the
 * coordinates afterwards under the same `client_id`.
 *
 * The pill fills left to right as the hold progresses, so the gesture explains its own
 * length while it is happening rather than in a caption nobody reads beforehand. The shape is
 * a horizontal stadium — the icon then the word SOS — sitting on the opposite edge of the
 * screen from the map controls, so the one button nobody should press by accident does not
 * share a corner with the two pressed most often. Haptics mark both ends: a tick
 * when the hold registers, a heavier one at the moment it actually fires, because by then
 * the participant may well be looking at the trail rather than at the screen.
 */
@Composable
fun SosButton(
    onFire: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = wbwColors
    val haptics = LocalHapticFeedback.current

    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Driven by a coroutine rather than by an animation, because the *elapsed time* is the
    // thing being measured — the fill is a readout of it, not the source of truth. Tying
    // the firing to an animation's completion would make it depend on frame delivery, and a
    // dropped frame must not be able to swallow an emergency.
    LaunchedEffect(holding) {
        if (!holding) {
            progress = 0f
            return@LaunchedEffect
        }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val start = System.currentTimeMillis()
        while (isActive) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed.toFloat() / HoldMillis).coerceAtMost(1f)
            if (progress >= 1f) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onFire()
                holding = false
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    Row(
        modifier
            // The nav bar's glass — the same call and the same hairline as the walk button
            // beside it — but tinted red rather than [GlassSheer]'s white.
            //
            // Both halves of that matter. It was a flat danger-coloured slab, which made
            // the one control that most needs to look like it belongs to this app the only
            // one that did not: every other floating thing on this screen refracts the map
            // under it, and this sat on top like a sticker. Going the other way and giving
            // it plain sheer glass fixed the material and lost the alarm — a red glyph on a
            // white-glass pill beside another white-glass pill is a *label*, and this is
            // not a label.
            //
            // Tinting the fill keeps both: the surface still refracts the trail moving
            // underneath, and it is still unmistakably the red one.
            //
            // `glass` clips to the shape internally, so the sweep below is cut to the pill
            // without a clip of its own.
            .glass(PillShape, fill = SosGlass, border = SosGlassBorder, elevation = 0.dp)
            // The hold, drawn as a fill sweeping left to right.
            //
            // It follows the shape's long axis and the direction the label is read in, so
            // the bar and the word it is filling move the same way. On the vertical pill
            // this was a level rising from the bottom, for the same reason: the readout
            // belongs to the axis the shape actually has, not to a ring that would spend
            // most of its travel on the two straight sides where progress cannot be judged.
            .drawBehind {
                if (progress > 0f) {
                    drawRect(
                        color = colors.danger.copy(alpha = 0.38f),
                        topLeft = Offset.Zero,
                        size = Size(size.width * progress, size.height),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    // onPress suspends for the length of the touch, so releasing early
                    // falls straight through to the reset below — the hold cannot be
                    // "left running" by a finger that slid off.
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    },
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Emergency,
            stringResource(R.string.sos_button_hint),
            tint = colors.danger,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(9.dp))

        // One Text rather than the three the vertical pill needed — laid out along the
        // reading axis, the letters are a word again and the type system can space them.
        //
        // Set larger than the map controls' own labels on purpose. Everything else on this
        // screen is sized to sit quietly under a map; this is the one control that has to
        // be found by somebody who is hurt, at a glance, possibly in the rain.
        Text(
            stringResource(R.string.sos_button_label),
            color = colors.danger,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
        )
    }
}

/**
 * What the map shows once an emergency is open: the whole screen.
 *
 * It was a card tucked under the title, and that was the wrong size for what it is. A panel
 * competing with a trail map, a walk button and a nav bar says "here is some information
 * about your emergency"; the screen belongs to one thing now, which says "this is what is
 * happening". There is nothing else to do on this screen while a case is open — the map is
 * still there underneath, dimmed, so the trail has not vanished, but nothing on it is worth
 * a tap until somebody arrives.
 *
 * **The lower half is for whoever reaches you, not for you.** That is the real argument for
 * full screen. Somebody who has held a button for three seconds because they are hurt is
 * quite likely to end up handing the phone to a staff member, a friend, or a stranger — and at
 * that moment the useful thing on the glass is not a status line, it is a blood type, a bib
 * number and a next-of-kin phone. Those are already on the device from the cached profile,
 * so they cost nothing and work with no signal, and a staff member who arrives to an unconscious
 * participant can read them without unlocking anything or knowing this app exists.
 *
 * Only what `/me` actually carries is shown. Allergies, medication and chronic conditions
 * live in `health_details` on the server and are exposed to staff through the SOS feed, not
 * to the participant's own profile — so they are on the staff member's side of this, not here.
 */
@Composable
fun SosFullScreen(
    case: SosCase,
    me: ParticipantDetail?,
    /** A refused cancel, or null. See the note beside where it is drawn. */
    cancelRefused: String?,
    onCancel: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val colors = wbwColors

    // A slow pulse on the dot while nobody has answered, still once somebody has. The
    // change of state is the message, so it is carried by movement stopping rather than by
    // a colour nobody was told the meaning of.
    //
    // An infinite transition rather than `animateFloatAsState`, which is what this was and
    // which never pulsed at all: given a fixed target it runs once and stops, so the dot
    // simply faded to 35% and sat there. A single dimming is indistinguishable from a dot
    // that was always dim — and "nobody has picked this up yet" is exactly the state that
    // must not look settled.
    val breathing = rememberInfiniteTransition(label = "sos-wait")
    val pulse by breathing.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(850, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "sos-pulse",
    )
    val dotAlpha = if (case.acknowledged) 1f else pulse

    Box(
        modifier
            .fillMaxSize()
            // A gradient, not two flat washes stacked.
            //
            // The first attempt laid [SosGlass] over the void colour and the two mixed into
            // brown — red over dark green always will, and brown is the one thing an
            // emergency screen must not look like. Running deep red at the top into the
            // app's own near-black at the bottom keeps the alarm where the words are and
            // lets the bottom of the screen belong to the floating nav bar, which is drawn
            // by the scaffold above this and cannot be covered from here.
            //
            // Still not quite opaque: the trail stays faintly readable underneath, which is
            // the difference between an overlay and a different screen — somebody being
            // helped can point at where they are, and the app has not thrown away the one
            // piece of context this tab exists to provide.
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF04A1A15), Color(0xF51A2418), colors.forestVoid.copy(alpha = 0.96f)),
                ),
            )
            // Swallows taps so nothing behind can be pressed through the scrim. A map that
            // pans under an emergency screen is a map that has not understood the moment.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                // The nav bar is the scaffold's, drawn above this overlay, so its clearance
                // has to be respected rather than covered — otherwise Cancel sits under it.
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(top = 28.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(colors.danger.copy(alpha = dotAlpha)),
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    stringResource(
                        if (case.acknowledged) R.string.sos_state_coming else R.string.sos_state_sent,
                    ).uppercase(),
                    color = colors.danger,
                    fontSize = 12.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(18.dp))

            // The one sentence this screen exists to say, set at headline size. Somebody
            // who has just held a button for three seconds is asking exactly one question,
            // and it should be answered in the largest type on the screen rather than in a
            // caption under a status chip.
            Text(
                stringResource(R.string.sos_notified_title),
                color = colors.onBackdrop,
                style = MaterialTheme.typography.displaySmall,
            )

            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    case.ackedByName != null -> stringResource(R.string.sos_acked_by, case.ackedByName)
                    case.acknowledged -> stringResource(R.string.sos_acked_anon)
                    else -> stringResource(R.string.sos_waiting)
                },
                color = colors.onBackdrop,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    case.checkpointName != null && case.located ->
                        stringResource(R.string.sos_location_near, case.checkpointName)
                    case.checkpointName != null ->
                        stringResource(R.string.sos_location_last_seen, case.checkpointName)
                    case.located -> stringResource(R.string.sos_location_gps)
                    else -> stringResource(R.string.sos_location_unknown)
                },
                color = colors.onBackdropMuted,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(26.dp))

            // ===== The half that is for somebody else =====
            Text(
                stringResource(R.string.sos_show_this).uppercase(),
                color = colors.onBackdropMuted,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    // A plain translucent surface, and deliberately *not* `glass()`.
                    //
                    // The glass modifier refracts `LocalBackdrop`, which is the app's
                    // wallpaper — the right source everywhere else, and the wrong one here:
                    // over this red scrim it rendered as a bright green slab, sampling a
                    // photograph that is nowhere near what is actually behind it. Glass
                    // works because it shows what is behind the pane. When it cannot, a
                    // clean surface is the honest version of the same idea.
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
                    .padding(20.dp),
            ) {
                // Title, name and blood type in one Box rather than a Row above a Column.
                //
                // The blood block is two lines of large type, so as a Row sibling it set the
                // height of the whole title row — and the title, being one short line, sat
                // at the top of it with 40-odd dp of nothing underneath before the name.
                // Floating it at the top-right corner instead lets the title and the name
                // sit at their own natural spacing, which is what they should have had all
                // along; the block is decoration hanging in the corner, not a column the
                // rest of the card has to make room for vertically.
                Box(Modifier.fillMaxWidth()) {
                    // End padding, so a long name runs out of room before it reaches the
                    // blood group rather than sliding underneath it.
                    Column(Modifier.padding(end = 86.dp)) {
                        Text(
                            stringResource(R.string.sos_card_title),
                            color = colors.onBackdrop,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            me?.fullName?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.sos_vital_missing),
                            color = colors.onBackdrop,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Top right, big, with a small label under it — a bare "A+" is ambiguous
                    // enough to be a size or a grade, and this is not a card to be clever on.
                    // It is the single fact a medic wants before any of the rest of it.
                    Column(
                        Modifier.align(Alignment.TopEnd),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            me?.bloodType?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.sos_blood_unknown),
                            color = colors.danger,
                            fontSize = if (me?.bloodType.isNullOrBlank()) 20.sp else 34.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                        )
                        // Nudged up: a 34sp line box carries more leading under the glyph
                        // than the glyph needs, so the label was drifting away from the
                        // letter it belongs to and landing level with the name instead.
                        Text(
                            stringResource(R.string.sos_vital_blood).uppercase(),
                            color = colors.onBackdropMuted,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.offset(y = (-6).dp),
                        )
                    }
                }

                // Major and school on one muted line. Neither is much use alone and both
                // are context rather than instruction, so they read as a subtitle to the
                // name instead of earning a labelled row each.
                listOfNotNull(
                    me?.major?.takeIf { it.isNotBlank() },
                    me?.schoolName?.takeIf { it.isNotBlank() },
                ).joinToString(" · ").takeIf { it.isNotBlank() }?.let { line ->
                    Text(
                        line,
                        color = colors.onBackdropMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }

                // The participant's own number, above the two identifiers rather than
                // below them. It belongs with the name: who this is and how to reach them
                // is one thought, and bib and group are a different one — they identify
                // this person to the *event*, which is what a staff member needs after
                // they have already tried phoning.
                Spacer(Modifier.height(12.dp))
                VitalRow(stringResource(R.string.sos_vital_phone), me?.contactPhone, last = true)

                Spacer(Modifier.height(14.dp))

                // Bib and group side by side — one question asked twice ("which walker,
                // and who with"), and the two numbers a staff member says out loud on
                // radio, so they are set as numbers rather than buried in rows.
                Row(Modifier.fillMaxWidth()) {
                    Stat(stringResource(R.string.sos_vital_bib), me?.bib?.toString(), Modifier.weight(1f))
                    Stat(stringResource(R.string.sos_vital_group), me?.groupNumber?.toString(), Modifier.weight(1f))
                }

                // ===== Emergency contact, as its own section =====
                //
                // Ruled off and titled rather than continuing the list. Every row above is
                // about the participant; these two are about somebody who is not here, and
                // a staff member skim-reading in a hurry must not dial the casualty's own number
                // thinking it is the next of kin's. The heading is the whole safeguard.
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.sos_vital_contact).uppercase(),
                    color = colors.danger,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                VitalRow(stringResource(R.string.sos_contact_name), me?.emergencyContactName)
                VitalRow(
                    stringResource(R.string.sos_contact_phone),
                    me?.emergencyContactPhone,
                    emphasis = true,
                    last = true,
                )

                // ===== The code a staff member actually scans =====
                //
                // The same `qr_token` the pass carries, so a staff member who reaches an
                // emergency can pull the participant's full record — including the
                // allergies and medication that `/me` never sends to this device — with the
                // scanner they already use at every checkpoint. It saves them reading
                // anything above off a cracked screen in the rain.
                //
                // Pure black on pure white, as on the pass: contrast is the whole job and a
                // scanner has no opinion about the design system. No token, no block — a
                // participant whose row predates the column checks in by bib, and an empty
                // white square would suggest a code that failed to load.
                me?.qrToken?.takeIf { it.isNotBlank() }?.let { token ->
                    val label = stringResource(R.string.profile_qr_label)
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(132.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center,
                        ) {
                            QrCode(
                                content = token,
                                foreground = Color(0xFF16241A),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            stringResource(R.string.sos_qr_hint).uppercase(),
                            color = colors.onBackdropMuted,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Offered only before anyone has acknowledged. Once a staff member is walking, the
            // server refuses the cancel anyway (409), and a button that exists in order to
            // be refused is worse than no button — it teaches that the app is broken at the
            // one moment it is working exactly as designed.
            //
            // A refused cancel is shown *here*, beside the button that was refused.
            //
            // It used to be a line in the map's own header column, which was correct until
            // this became a full-screen overlay — at which point it was drawn underneath
            // the very screen the participant was looking at, and pressing "I am okay" did
            // nothing visible at all. That is the exact failure the paragraph below warns
            // about, reached by moving the card rather than by adding a bad button.
            cancelRefused?.let { refusal ->
                Text(
                    refusal.ifBlank { stringResource(R.string.sos_cancel_refused) },
                    color = colors.danger,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }

            if (!case.acknowledged) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .tapNoRippleSos(onCancel)
                        .padding(vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        null,
                        tint = colors.onBackdropMuted,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.sos_cancel).uppercase(),
                        color = colors.onBackdropMuted,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * A number set large with its label under it — bib and group.
 *
 * Display-sized rather than in a [VitalRow] because these two are spoken, not read: they
 * are what a staff member says into a radio, and a number you have to find in a list is a number
 * you misread.
 */
@Composable
private fun Stat(label: String, value: String?, modifier: Modifier = Modifier) {
    val colors = wbwColors
    Column(modifier) {
        Text(
            label.uppercase(),
            color = colors.onBackdropMuted,
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
        )
        Text(
            value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.sos_vital_missing),
            color = if (value.isNullOrBlank()) colors.onBackdropMuted else colors.onBackdrop,
            fontSize = if (value.isNullOrBlank()) 14.sp else 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * One line of the card a staff member reads.
 *
 * A missing value prints "not given" rather than being hidden. An absent row would let the
 * card look complete when it is not — somebody scanning it for a blood type needs to learn
 * that there is not one on file, not to wonder whether they missed it.
 */
@Composable
private fun VitalRow(
    label: String,
    value: String?,
    emphasis: Boolean = false,
    tint: Color? = null,
    last: Boolean = false,
) {
    val colors = wbwColors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fixed width so every value starts on the same vertical line — a column of
        // labels that each end where their own text does is a column with a ragged left
        // edge on the half that actually gets read. Sized for the longest label rather
        // than the average: "Emergency contact" was touching its own value at 104dp.
        Text(
            label,
            color = colors.onBackdropMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(132.dp).padding(end = 8.dp),
        )
        Text(
            value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.sos_vital_missing),
            color = when {
                value.isNullOrBlank() -> colors.onBackdropMuted
                tint != null -> tint
                else -> colors.onBackdrop
            },
            fontSize = if (emphasis) 17.sp else 14.sp,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.Normal,
        )
    }
    if (!last) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
    }
}

/** Local copy so this file does not depend on the map screen's private helper. */
@Composable
private fun Modifier.tapNoRippleSos(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
    )

/**
 * How long the hold has to last.
 *
 * Three seconds is the figure the server's design is written around. It is also long enough
 * that no pocket, no map pan and no accidental brush completes it, and short enough that
 * somebody who means it is not left holding a button while hurt.
 */
private const val HoldMillis = 3_000f

/**
 * The pill's own glass.
 *
 * [GlassSheer] is 12% white; this is the same idea in the danger hue and a little stronger,
 * because a red wash has to survive being laid over a map that is already green — a 12% red
 * over #1a2c1e reads as brown rather than as red. The border matches, so the edge belongs to
 * the surface instead of quoting the white one next to it.
 */
private val SosGlass = Color(0x2EE8544A)
private val SosGlassBorder = Color(0x4DE8735F)

/** A full pill — the corner radius is capped at half the shorter side, so 50% is a stadium. */
private val PillShape = RoundedCornerShape(50)
