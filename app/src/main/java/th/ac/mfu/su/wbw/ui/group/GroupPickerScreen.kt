package th.ac.mfu.su.wbw.ui.group

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.data.remote.dto.Group
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.Numerals
import th.ac.mfu.su.wbw.ui.theme.WaveLines
import th.ac.mfu.su.wbw.ui.theme.WbwForestVoid
import th.ac.mfu.su.wbw.ui.theme.WbwInkLight
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * Pick a group, once, on the way in.
 *
 * Forty tiles of fifty seats. A grid rather than a list because the only thing that
 * distinguishes one group from another is its number and how full it is — there is no name
 * to read, so a full-width row per group would be forty screens of whitespace to scroll
 * through in search of a two-digit number.
 *
 * The choice is close to permanent: a participant is issued exactly one `leave_quota`, so
 * they can change their mind precisely once for the whole event. That is why tapping a tile
 * opens a confirmation instead of joining — a mis-tap here costs somebody their only
 * transfer, and the sheet is the only place to say so before it is spent.
 */
@Composable
fun GroupPickerScreen(
    onJoined: () -> Unit,
    viewModel: GroupPickerViewModel = viewModel(factory = GroupPickerViewModel.Factory),
) {
    val colors = wbwColors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf<Group?>(null) }

    LaunchedEffect(state.joinedGroupId) {
        if (state.joinedGroupId != null) onJoined()
    }

    ForestBackground {
        Box(Modifier.fillMaxSize()) {
            WaveLines(
                modifier = Modifier.fillMaxSize(),
                ink = colors.onBackdrop,
                alpha = 0.14f,
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 16.dp)) {
                    Text(
                        stringResource(R.string.group_pick_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackdrop,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.group_pick_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBackdropMuted,
                    )
                }

                when {
                    state.loading && state.groups.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.onBackdrop)
                    }

                    state.groups.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.error.orEmpty(),
                                color = colors.onBackdropMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            PillButton(stringResource(R.string.group_pick_retry)) { viewModel.load() }
                        }
                    }

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.groups, key = { it.groupId }) { group ->
                            GroupTile(
                                group = group,
                                busy = state.joining == group.groupId,
                                onClick = { if (group.seatsLeft > 0) confirming = group },
                            )
                        }
                    }
                }
            }

            confirming?.let { group ->
                ConfirmJoinSheet(
                    group = group,
                    joining = state.joining != null,
                    onConfirm = {
                        viewModel.join(group.groupId)
                        confirming = null
                    },
                    onDismiss = { confirming = null },
                )
            }
        }
    }
}

/**
 * One group.
 *
 * The number is the tile — big, and in Numerals, the app's numeral face. A two-digit figure
 * is the only thing anyone is scanning for here, and the numeral face's round, evenly
 * weighted digits hold apart at a glance; this tile was setting them in the body face,
 * against the type system's own rule.
 *
 * Under it, the one fact that decides the tap: how much room is left. It gets a **bar** as
 * well as a word, because forty tiles is far too many to read. A bar is comparable at a
 * glance down the whole grid — the eye finds the empty groups without reading a single
 * number — and the word underneath is there for whoever wants the figure. Before this, the
 * entire basis of the screen was carried by 10sp of muted text, which made all forty tiles
 * look identical: the one thing a participant is choosing on was the one thing they could
 * not see.
 *
 * Seats **left**, not seats taken. Both are the same fact, but only one of them is the
 * question being asked ("can I still get in, and is it filling up?"), and it is the number
 * that shrinks toward the decision rather than growing away from it.
 *
 * Scarcity is carried by weight and strength of ink, never by a hue. The palette has no
 * accent to spend (see [th.ac.mfu.su.wbw.ui.theme.WbwAccentDark]), and `colors.danger` is
 * the wrong token even where it looks right: it flips light in light theme, while this tile
 * sits on the backdrop, which is dark in both. So a group with room sits muted, and one
 * nearly gone comes up to full strength — the same "emphasis from weight, not colour" the
 * participant pass is built on.
 */
@Composable
private fun GroupTile(group: Group, busy: Boolean, onClick: () -> Unit) {
    val colors = wbwColors
    val full = group.seatsLeft <= 0
    val taken = if (group.capacity > 0) {
        (group.memberCount.toFloat() / group.capacity).coerceIn(0f, 1f)
    } else {
        0f
    }
    // "Nearly gone" is a proportion of the group, not a fixed number of seats — a capacity
    // the organisers change should not silently change what counts as urgent.
    val scarce = !full && taken >= ScarceFraction

    // Press feedback. The tile is the whole target and it had none: `clickableTap` passes
    // `indication = null` everywhere on this screen, which is right for a text button and
    // wrong for a 100dp slab that is the only thing to press. A slight settle under the
    // finger is enough, and it does not tint the glass the way a ripple would.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !full) 0.955f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "tilePress",
    )

    Box(
        Modifier
            .height(TileHeight)
            .scale(press)
            .glass(
                TileShape,
                fill = GlassSheer,
                border = GlassSheerBorder,
                elevation = 0.dp,
            )
            .then(
                if (full) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = colors.onBackdrop,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    group.groupNumber.toString(),
                    color = colors.onBackdrop.copy(alpha = if (full) 0.30f else 1f),
                    fontFamily = Numerals,
                    fontWeight = FontWeight.Normal,
                    fontSize = 34.sp,
                    // Pinned, and load-bearing. The numeral face's default line box at this size is
                    // roughly 1.5x the point size — enough that the column overran the
                    // tile, and since the tile centres its content and clips to the glass
                    // shape, what fell off the bottom was the seat bar. It did not look
                    // like an overflow; it looked like the bar had failed to draw.
                    lineHeight = 36.sp,
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    if (full) {
                        stringResource(R.string.group_pick_full)
                    } else {
                        stringResource(R.string.group_pick_left, group.seatsLeft)
                    }.uppercase(),
                    color = when {
                        full -> colors.onBackdropMuted.copy(alpha = 0.45f)
                        scarce -> colors.onBackdrop
                        else -> colors.onBackdropMuted
                    },
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (scarce) FontWeight.Medium else FontWeight.Medium,
                    // The app's label voice — small, uppercase, widely tracked. Same
                    // treatment as the section labels on Settings and Activities.
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                )

                Spacer(Modifier.height(7.dp))

                SeatBar(
                    taken = taken,
                    ink = colors.onBackdrop,
                    dimmed = full,
                )
            }
        }
    }
}

/**
 * How full a group is, as a bar.
 *
 * Drawn rather than laid out, so a group with nobody in it is simply an empty track — a
 * zero-width child would be a layout edge case for the commonest state on this screen.
 *
 * The track is the same white the glass edge uses, so it reads as part of the pane rather
 * than as a control dropped on it; the fill is the tile's own ink. A full group keeps its
 * bar, at low strength: "this one is full" is still information, and a tile that dropped
 * the bar entirely would read as having no data rather than no seats.
 */
@Composable
private fun SeatBar(taken: Float, ink: Color, dimmed: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .drawBehind {
                val r = CornerRadius(size.height / 2f, size.height / 2f)
                // The track has to carry the tile on its own, because the commonest state
                // on this screen is an empty group and an empty group is *all* track. At
                // the 16% this started on it vanished into the pane and the bar only
                // existed for groups that had already filled — which is precisely when it
                // is least needed.
                drawRoundRect(ink.copy(alpha = if (dimmed) 0.14f else 0.30f), cornerRadius = r)
                if (taken > 0f) {
                    drawRoundRect(
                        ink.copy(alpha = if (dimmed) 0.34f else 0.92f),
                        // Never thinner than its own height, or a group with one member in
                        // fifty draws a sliver too short to round and it comes out as a
                        // speck of dirt on the pane instead of a bar.
                        size = Size(
                            width = (size.width * taken).coerceAtLeast(size.height),
                            height = size.height,
                        ),
                        cornerRadius = r,
                    )
                }
            },
    )
}

/**
 * The confirmation.
 *
 * A scrim and a panel rather than a Material dialog, so it is the app's glass rather than a
 * white card dropped onto a photograph. Tapping the scrim dismisses, which is the cheap way
 * out that a decision this expensive ought to have.
 *
 * **The panel is dark on purpose, and this is the one thing not to "fix" back.** Everywhere
 * else in the app a glass pane is pale, because it refracts the wallpaper and the wallpaper
 * is what is behind it. Here it is not: the scrim is, and the scrim is drawn *over* the
 * content while [LocalBackdrop] still points at the unscrimmed wallpaper underneath. A pane
 * tinted with [GlassSheer] therefore samples the bright original and comes out lighter than
 * everything around it — which is what it did, and it read as a pale blob punched through
 * the scrim rather than as a card raised above it. A dark tint puts the panel back on the
 * near side of the scrim where it belongs.
 *
 * The layout answers the tile that opened it. The number is repeated as a chip in the same
 * shape and face the tile used, so the thing being confirmed is the thing that was tapped —
 * a confirmation whose whole job is "did you mean *this* one" should not make that a
 * sentence to read. Under it, seats left, because it is the fact that dates fastest: this
 * sheet can sit open while the group fills.
 */
@Composable
private fun ConfirmJoinSheet(
    group: Group,
    joining: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = wbwColors
    Box(
        Modifier
            .fillMaxSize()
            // No scrim token exists in the palette; the map already uses this near-black
            // for the same job of putting the ground behind something.
            .background(WbwForestVoid.copy(alpha = SheetScrimAlpha))
            .clickableTap(onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                // Capped rather than full-bleed. On a phone this is nearly the whole width
                // anyway; what it stops is a tablet stretching two short lines of text into
                // a letterbox with the buttons a hand-span apart.
                .widthIn(max = SheetMaxWidth)
                .fillMaxWidth()
                // Swallows the tap so a press inside the panel does not dismiss it.
                .clickableTap {}
                .glass(
                    PanelShape,
                    fill = SheetFill,
                    border = GlassSheerBorder,
                    elevation = 22.dp,
                )
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The tapped tile, restated: same corner, same numeral face.
            Box(
                Modifier
                    .size(width = 76.dp, height = 66.dp)
                    .clip(TileShape)
                    .background(colors.onBackdrop.copy(alpha = 0.10f))
                    .border(1.dp, colors.onBackdrop.copy(alpha = 0.18f), TileShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    group.groupNumber.toString(),
                    color = colors.onBackdrop,
                    fontFamily = Numerals,
                    fontWeight = FontWeight.Normal,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                stringResource(R.string.group_pick_left, group.seatsLeft).uppercase(),
                color = colors.onBackdropMuted,
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )

            Spacer(Modifier.height(18.dp))

            Text(
                stringResource(R.string.group_pick_confirm_q),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackdrop,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.group_pick_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackdropMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(22.dp))

            // Stacked, not a Material button row. The two actions are not peers — one
            // spends the participant's single transfer for the whole event and the other
            // costs nothing — so the committing one is the full-width filled control and
            // the way out is quiet type beneath it. Side by side at the bottom right they
            // read as equals, and the cheap escape was the harder of the two to see.
            PillButton(
                text = stringResource(
                    if (joining) R.string.group_pick_joining else R.string.group_pick_confirm_join,
                ),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .clickableTap(onDismiss)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.group_pick_cancel),
                    color = colors.onBackdropMuted,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

/** The one filled control on the screen, matching the login screen's sign-in button. */
@Composable
private fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = wbwColors
    Box(
        modifier
            .clip(PillShape)
            .background(colors.onBackdrop.copy(alpha = 0.92f))
            .clickableTap(onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = WbwInkLight, fontWeight = FontWeight.Normal)
    }
}

private val TileShape = RoundedCornerShape(18.dp)
private val PanelShape = RoundedCornerShape(26.dp)
private val PillShape = RoundedCornerShape(14.dp)

/** Room for the number, its label and the bar, without the three crowding each other. */
private val TileHeight = 100.dp

/** Thin enough to read as a gauge on the pane rather than as a second control. */
private val BarHeight = 4.dp

/** Wide enough for two lines of the warning, narrow enough not to letterbox on a tablet. */
private val SheetMaxWidth = 340.dp

/** Deep enough that the grid behind reads as "not the subject" without going to black. */
private const val SheetScrimAlpha = 0.74f

/**
 * The confirmation panel's tint — dark, unlike every other glass surface in the app.
 *
 * See [ConfirmJoinSheet]: this pane sits above the scrim but refracts what is below it, so
 * a pale tint makes it brighter than its own surroundings. The alpha is high enough to bury
 * the wallpaper and low enough that the refraction still bends the light at the edges, which
 * is the part that makes it read as glass rather than as a flat rectangle.
 */
private val SheetFill = WbwForestVoid.copy(alpha = 0.72f)

/**
 * When a group stops being "has room" and starts being "going".
 *
 * 0.8 rather than a seat count, so it keeps meaning the same thing if capacity ever moves
 * off fifty.
 */
private const val ScarceFraction = 0.8f

private fun Modifier.clickableTap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
