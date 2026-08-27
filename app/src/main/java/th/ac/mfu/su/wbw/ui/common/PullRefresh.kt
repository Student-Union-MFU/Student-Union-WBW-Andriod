package th.ac.mfu.su.wbw.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import th.ac.mfu.su.wbw.ui.theme.GlassSheer
import th.ac.mfu.su.wbw.ui.theme.GlassSheerBorder
import th.ac.mfu.su.wbw.ui.theme.glass
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * Which edge the gesture starts from — i.e. which way the content is anchored.
 *
 * [Bottom] is not a variation for its own sake. The chat's list is `reverseLayout`, so the
 * newest message is at the bottom and that is the end a reader is already sitting at; the
 * over-scroll available there is a drag *upward*. Putting the refresh at the top of that
 * screen would mean scrolling back through the whole conversation to reach the gesture,
 * away from the only messages a refresh could bring.
 */
enum class PullEdge { Top, Bottom }

/**
 * Pull past the edge of the content to refresh it.
 *
 * Hand-rolled rather than `PullToRefreshBox` from Material 3, for two reasons. It only
 * pulls from the top, and half the point here is the chat's upward pull. And its indicator
 * is a Material surface — an opaque tonal circle with an M3 elevation shadow — which is the
 * one thing on these screens that would not be made of the same glass as everything else.
 * The gesture itself is the standard nested-scroll one either way.
 *
 * [refreshing] is the caller's own "a request is in flight" flag: the spinner keeps turning
 * until it goes false, so the screen decides when the refresh is over rather than the
 * animation deciding for it. A caller that never sets it is not left with a spinner stuck
 * on screen — see [PullRefreshDefaults.AcknowledgeTimeoutMillis].
 *
 * @param indicatorInset how far in from [edge] the spinner rests, for screens whose box
 *   reaches under a system bar and would otherwise put it half behind one.
 */
@Composable
fun PullRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    edge: PullEdge = PullEdge.Top,
    indicatorInset: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { PullRefreshDefaults.Threshold.toPx() }
    val indicatorPx = with(density) { PullRefreshDefaults.IndicatorSize.toPx() }
    val insetPx = with(density) { indicatorInset.toPx() }
    // Positive means "away from the edge the gesture starts at", so the whole of the rest
    // of this reads in one direction and only this line knows which way that is.
    val outward = if (edge == PullEdge.Top) 1f else -1f

    val scope = rememberCoroutineScope()
    val liveRefreshing = rememberUpdatedState(refreshing)
    val liveOnRefresh = rememberUpdatedState(onRefresh)

    // Raw finger travel, live while the drag lasts. A plain state rather than an
    // [Animatable] because it is written from inside the scroll dispatch and has to be
    // readable again on the very next event — an animation channel would drop deltas.
    val dragged = remember { mutableFloatStateOf(0f) }
    val dragging = remember { mutableStateOf(false) }
    // Where the spinner sits once the finger is gone: held out at the threshold while the
    // refresh runs, then wound back in.
    val settled = remember { Animatable(0f) }
    // Whether this box is holding the indicator out for a refresh it asked for, as opposed
    // to for a finger. It brackets the whole hold — the release that triggered it, through
    // to the end of the retreat — which is deliberately wider than [refreshing]: the caller
    // only reports the request itself, and there is settle, acknowledgement and retreat
    // around it where the indicator is on screen with nothing in flight. Those windows are
    // the difference between a spinner and a motionless full ring, so what turns follows
    // this rather than the flag.
    val armed = remember { mutableStateOf(false) }

    val connection = remember(thresholdPx, outward, scope) {
        object : NestedScrollConnection {

            private var settleJob: Job? = null

            // Scrolling back towards the edge pays the indicator down before the content
            // moves, which is what makes the pull feel attached to the finger rather than
            // layered over a list that has already started scrolling underneath it.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (source == NestedScrollSource.UserInput && available.y * outward < 0f) {
                    pull(available.y)
                } else {
                    Offset.Zero
                }

            // Whatever the content could not use, past its own end.
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                if (source == NestedScrollSource.UserInput && available.y * outward > 0f) {
                    pull(available.y)
                } else {
                    Offset.Zero
                }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!dragging.value) return Velocity.Zero
                val released = resist(dragged.floatValue, thresholdPx)
                settle(released)
                // The flick is swallowed only if this gesture actually pulled something
                // into view. A drag that never left the content's own range keeps its
                // fling — the list should still coast the way it was thrown.
                return if (released > 0f) available else Velocity.Zero
            }

            private fun pull(deltaY: Float): Offset {
                if (liveRefreshing.value || armed.value) return Offset.Zero
                if (!dragging.value) {
                    settleJob?.cancel()
                    dragged.floatValue = 0f
                    dragging.value = true
                }
                val before = dragged.floatValue
                val after = (before + deltaY * outward).coerceAtLeast(0f)
                if (after == before) return Offset.Zero
                dragged.floatValue = after
                return Offset(0f, (after - before) * outward)
            }

            private fun settle(from: Float) {
                val triggered = from >= thresholdPx && !liveRefreshing.value && !armed.value
                settleJob?.cancel()
                settleJob = scope.launch {
                    // Handing over from the finger to the animation before the drag flag
                    // drops, so the spinner never blinks back to nothing for a frame in
                    // between the two.
                    settled.snapTo(from)
                    dragged.floatValue = 0f
                    dragging.value = false

                    if (!triggered) {
                        settled.animateTo(0f, RetreatSpec)
                        return@launch
                    }
                    try {
                        liveOnRefresh.value()
                        armed.value = true

                        // Watching starts here, at the trigger, and not after the settle
                        // animation below. A refresh answered out of the response cache can
                        // go out and come back inside those 260ms; a watcher started
                        // afterwards would find the flag false, having missed both edges,
                        // read that as "never started" and then sit through the whole
                        // acknowledge timeout. Undispatched so the snapshot observer is
                        // registered before this coroutine yields, leaving no gap for the
                        // flag to flip through unseen.
                        val held = async(start = CoroutineStart.UNDISPATCHED) {
                            // Hold for as long as the caller says it is working. If it
                            // never says it started — a refresh that turned out to be a
                            // no-op, or a screen that forgot to report one — the spinner
                            // still has to come back rather than turn for the rest of the
                            // session.
                            val started = withTimeoutOrNull(
                                PullRefreshDefaults.AcknowledgeTimeoutMillis,
                            ) {
                                snapshotFlow { liveRefreshing.value }.first { it }
                            } != null
                            if (started) snapshotFlow { liveRefreshing.value }.first { !it }
                        }
                        settled.animateTo(thresholdPx, SettleSpec)
                        held.await()
                        // The retreat is inside the hold, so the arc is still turning as it
                        // flies back out. Disarming first would swap a sweeping arc for a
                        // closed ring at full opacity, on the one frame the eye is already
                        // following.
                        settled.animateTo(0f, RetreatSpec)
                    } finally {
                        // Also the cancellation path — leaving composition mid-refresh must
                        // not strand the flag, or the guard in `pull` would refuse every
                        // gesture from then on.
                        armed.value = false
                    }
                }
            }
        }
    }

    val shown = if (dragging.value) resist(dragged.floatValue, thresholdPx) else settled.value
    val fraction = (shown / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier
            .nestedScroll(connection)
            // Makes a page that does not scroll pullable anyway. Home is a fixed layout —
            // greeting, bloom, count — so nothing under it would ever dispatch a scroll
            // for the connection above to hear. A scrollable that consumes none of what it
            // is given changes nothing about the page and gives the gesture something to
            // travel through. Where the content scrolls on its own this sits inert between
            // the two, passing the child's leftovers straight up.
            .scrollable(rememberScrollableState { 0f }, Orientation.Vertical),
    ) {
        content()

        if (shown > 0f || refreshing) {
            Box(
                Modifier
                    .align(if (edge == PullEdge.Top) Alignment.TopCenter else Alignment.BottomCenter)
                    .size(PullRefreshDefaults.IndicatorSize)
                    .graphicsLayer {
                        // At rest it sits its own height beyond the inset edge, so it
                        // arrives from off-screen rather than fading in where it stops.
                        // The inset pushes the whole travel *away* from the edge, which is
                        // what keeps it clear of a status bar rather than tucking it under
                        // one — hence the sign matching `shown` rather than opposing it.
                        translationY = (shown - indicatorPx + insetPx) * outward
                        alpha = fraction
                        scaleX = 0.65f + 0.35f * fraction
                        scaleY = scaleX
                        // Winds with the pull, so the arc is already turning under the
                        // finger before the refresh it is asking for has started.
                        rotationZ = fraction * 150f
                    }
                    .glass(CircleShape, fill = GlassSheer, border = GlassSheerBorder, elevation = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val ink = wbwColors.onBackdrop
                if (armed.value || refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(PullRefreshDefaults.ArcSize),
                        color = ink,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    // Determinate under the finger: the arc closing is what says how much
                    // further there is to pull, which a spinning one cannot.
                    CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.size(PullRefreshDefaults.ArcSize),
                        color = ink,
                        trackColor = Color.Transparent,
                        strokeWidth = 2.5.dp,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

object PullRefreshDefaults {
    /** How far the indicator has to travel before letting go refreshes anything. */
    val Threshold: Dp = 72.dp
    val IndicatorSize: Dp = 38.dp
    val ArcSize: Dp = 19.dp

    /**
     * How long the spinner waits for the caller to report a refresh in flight.
     *
     * Generous on purpose — a view model that flips its flag inside a coroutine launch is
     * a frame or two behind the gesture, and cutting that fine would flash the spinner off
     * and straight back on again.
     */
    const val AcknowledgeTimeoutMillis = 1_200L
}

private val SettleSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
private val RetreatSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)

/**
 * Finger travel, turned into indicator travel.
 *
 * Half speed up to the threshold, and stiffer past it. The pull is not a scroll — there is
 * nothing under it moving one-to-one with the finger — so tracking it exactly reads as the
 * whole screen having come loose; and the change of rate at the threshold is felt before it
 * is seen, which is the cheapest possible way to say "that is far enough".
 */
private fun resist(raw: Float, thresholdPx: Float): Float {
    val tracked = raw * 0.55f
    if (tracked <= thresholdPx) return tracked
    return (thresholdPx + (tracked - thresholdPx) * 0.3f).coerceAtMost(thresholdPx * 1.5f)
}
