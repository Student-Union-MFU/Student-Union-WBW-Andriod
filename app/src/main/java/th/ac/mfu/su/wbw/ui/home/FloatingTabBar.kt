package th.ac.mfu.su.wbw.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.ui.theme.WbwGold
import th.ac.mfu.su.wbw.ui.theme.WbwInkLight
import th.ac.mfu.su.wbw.ui.theme.liquidGlass
import kotlin.math.roundToInt

data class TabItem(
    val route: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
    val contentDescription: String,
)

/**
 * The floating navigation bar, ported from su-clubfair-mobile's `GlassNavBar`.
 *
 * Shape follows iOS: a pill of destinations with the QR button split out as its own
 * circle beside it. That split is not decoration — on iOS the QR tab is declared
 * `Tab(value: 4, role: .search)` in `MainTabView.swift`, and the `.search` role is
 * exactly what makes SwiftUI lift it out of the bar. It reads correctly too: the
 * other tabs are places to look at, while QR is the thing you hold up at a
 * checkpoint, and mixing an action in with the destinations makes it look like
 * another page.
 *
 * Icons are dark rather than light, which looks inverted next to the app's dark theme
 * until you see why: the pane is real glass over the forest photograph, and that
 * photograph is bright. The bar takes its lightness from what is behind it, in both
 * themes, so dark ink is what stays legible on it — the same reason the iOS bar looks
 * cream even in dark mode.
 */
@Composable
fun FloatingTabBar(
    items: List<TabItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    backdrop: Backdrop,
    qrSelected: Boolean,
    onSelectQr: () -> Unit,
    qrIcon: ImageVector,
    qrContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val selected = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                // No shader rim: `Highlight.Default` ramps in brightness around the
                // outline, and on a shape this wide the ramp reads as a gradient
                // painted onto the bar rather than light catching an edge. An even
                // hairline instead — also the only edge a pre-API-33 phone can draw.
                .liquidGlass(backdrop, PillShape, surface = BarSurface, highlight = null)
                .border(1.dp, EdgeColor, PillShape),
        ) {
            val slotWidthPx = constraints.maxWidth.toFloat() / items.size
            val slotWidth = maxWidth / items.size

            // Where the indicator rests, in pixels from the bar's left edge. An
            // Animatable because the release has to *spring* to a slot.
            val indicator = remember { Animatable(0f) }

            // Where the finger is while it is down; null the rest of the time, and
            // that null is what hands the indicator back to the spring. A plain state
            // rather than driving the Animatable through the drag: `snapTo` suspends,
            // so following a finger with it means a coroutine allocated and
            // superseded a hundred times a second. The finger *is* the animation.
            var dragX by remember { mutableStateOf<Float?>(null) }

            // Where it was let go, so the settle springs from under the finger rather
            // than jumping back to wherever the indicator sat when the drag began.
            var releasedAt by remember { mutableStateOf<Float?>(null) }

            // The gesture below is keyed on slot width, so its coroutine is not
            // restarted when the selection changes — anything it closes over would be
            // frozen at the value it had when the finger went down. Read through these.
            val currentSelected by rememberUpdatedState(selected)
            val currentItems by rememberUpdatedState(items)
            val currentOnSelect by rememberUpdatedState(onSelect)

            // Which tab lights up: the one under the finger while dragging, the real
            // selection otherwise. `derivedStateOf` earns its keep — dragX changes
            // every frame, but the *slot* it lands in changes twice a gesture.
            val highlighted by remember(slotWidthPx) {
                derivedStateOf {
                    dragX?.let { slotIndexAt(it, slotWidthPx, currentItems.size) } ?: currentSelected
                }
            }
            val dragging by remember { derivedStateOf { dragX != null } }

            LaunchedEffect(selected, slotWidthPx, dragging) {
                if (dragging) return@LaunchedEffect
                releasedAt?.let {
                    indicator.snapTo(it)
                    releasedAt = null
                }
                indicator.animateTo(
                    targetValue = selected * slotWidthPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }

            Box(
                // Both reads happen in the lambda, which is layout rather than
                // composition — the indicator tracks the finger for free.
                modifier = Modifier
                    .offset { IntOffset((dragX ?: indicator.value).roundToInt(), 0) }
                    .width(slotWidth)
                    .fillMaxHeight()
                    .padding(IndicatorInset)
                    .clip(PillShape)
                    .background(IndicatorColor),
            )

            Row(
                // Slide along the bar to move between tabs. On the container, not the
                // items: a drag that started on Home has to keep being delivered while
                // the finger is over Map, and a per-item detector only sees its own
                // bounds. Items keep their own `clickable` for taps — `clickable`
                // gives movement up once it passes touch slop, so the two coexist.
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slotWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragX = indicatorXAt(it.x, slotWidthPx, currentItems.size) },
                            // The page changes on release, not slot by slot on the way
                            // across: committing per slot fires a transition each time,
                            // and every frame of those reuploads the whole screen into
                            // the backdrop layer this glass samples through.
                            onDragEnd = {
                                val x = dragX ?: return@detectHorizontalDragGestures
                                releasedAt = x
                                dragX = null
                                val index = slotIndexAt(x, slotWidthPx, currentItems.size)
                                if (index != currentSelected) currentOnSelect(currentItems[index].route)
                            },
                            onDragCancel = {
                                releasedAt = dragX
                                dragX = null
                            },
                        ) { change, _ ->
                            dragX = indicatorXAt(change.position.x, slotWidthPx, currentItems.size)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    NavBarItem(
                        item = item,
                        selected = index == highlighted,
                        onClick = { onSelect(item.route) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        Box(Modifier.width(BarGap))

        QrButton(
            selected = qrSelected,
            onClick = onSelectQr,
            backdrop = backdrop,
            icon = qrIcon,
            contentDescription = qrContentDescription,
        )
    }
}

/**
 * The indicator's left edge for a finger at [touchX], clamped to the bar.
 *
 * Half a slot behind the finger, because the indicator is a slot wide and is being
 * centred under the touch, and clamped so it stops at the first and last tabs instead
 * of sliding out from under the glass.
 */
private fun indicatorXAt(touchX: Float, slotWidth: Float, count: Int): Float =
    (touchX - slotWidth / 2f).coerceIn(0f, slotWidth * (count - 1))

/** Which slot an indicator sitting at [x] is nearest to. */
private fun slotIndexAt(x: Float, slotWidth: Float, count: Int): Int =
    ((x + slotWidth / 2f) / slotWidth).toInt().coerceIn(0, count - 1)

private val PillShape = RoundedCornerShape(50)
private val BarHeight = 62.dp

/**
 * The pane's own lightness, over and above what it refracts.
 *
 * Much heavier than clubfair's 0.06, and the difference is the icon colour. That bar
 * draws its icons white, so it can afford to be nearly clear and let whatever passes
 * underneath show through. This one draws them in ink to match iOS, and ink needs a
 * light pane to sit on — with a near-clear surface the icons vanished the moment a
 * dark card scrolled under the bar (Home's "next base" panel did exactly that).
 *
 * So the pane carries its own light rather than borrowing all of it from behind,
 * which is also what iOS's `.regular` glass does: it lightens what it covers instead
 * of merely transmitting it. Refraction and blur still come through — this only sets
 * the floor.
 */
private val BarSurface = Color.White.copy(alpha = 0.55f)

/** The gap that separates the action from the destinations. */
private val BarGap = 12.dp

/**
 * The hairline round the bar and the QR button.
 *
 * Dimmer than a drawn outline would be: the bar floats over content that scrolls
 * underneath it, and an edge bright enough to read as an outline turns it into a box
 * sitting on the page instead of a pane the page passes behind.
 */
private val EdgeColor = Color.White.copy(alpha = 0.30f)

/**
 * The lit slot behind the selected tab. Brighter than clubfair's 0.16 because this
 * bar sits over a bright photograph rather than a dark mesh, where 0.16 vanishes.
 */
private val IndicatorColor = Color.White.copy(alpha = 0.38f)

/** How far the sliding indicator sits inside the bar, so it reads as within the glass. */
private val IndicatorInset = 6.dp

/**
 * The tap bounce: a quick squash up, then a spring back that overshoots slightly.
 *
 * Two stages rather than one spring from rest, because a spring alone eases *out* of
 * the tap — slowest exactly when the finger lands, which is when the feedback has to
 * arrive. A short tween gets the icon moving immediately and the spring underneath
 * does the part the eye reads as physical.
 */
private const val BouncePeak = 1.28f
private const val BounceRiseMillis = 110

/**
 * Play the bounce.
 *
 * Fires on every tap, including the tab you are already on — the bounce acknowledges
 * that the tap landed, and swallowing it when nothing changes is what makes a bar
 * feel unresponsive. `snapTo` first so a rapid second tap restarts from rest.
 */
private suspend fun Animatable<Float, *>.bounce() {
    snapTo(1f)
    animateTo(BouncePeak, tween(BounceRiseMillis, easing = FastOutSlowInEasing))
    animateTo(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    )
}

@Composable
private fun NavBarItem(
    item: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "navEmphasis",
    )
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .clip(PillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                scope.launch { scale.bounce() }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.contentDescription,
            // Gold when selected, ink otherwise — iOS tints its TabView `.wbwGold`
            // and leaves the rest dark against the bright pane.
            tint = lerp(WbwInkLight, WbwGold, emphasis),
            // graphicsLayer, not a size change: scaling the layer costs no relayout,
            // so the bounce can't shove neighbouring tabs around while it runs. The
            // fade rides in the same layer rather than in the tint, because a tint is
            // a parameter and animating it recomposes on every frame of the ramp.
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    alpha = 0.72f + 0.28f * emphasis
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

/**
 * QR, as a round glass button beside the bar.
 *
 * Fills with gold as it becomes selected rather than only brightening its icon — it's
 * the primary action, so it should look pressed-in when you're on it, not merely lit.
 */
@Composable
private fun QrButton(
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "qrEmphasis",
    )
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(BarHeight)
            // Matched to the bar beside it — two pieces of the same material a gap
            // apart, edged two different ways, is worse than either choice made
            // consistently.
            .liquidGlass(backdrop, CircleShape, surface = BarSurface, highlight = null)
            .clip(CircleShape)
            .background(WbwGold.copy(alpha = 0.28f * emphasis))
            // After the fill, not before: the selected state washes gold across the
            // whole face, and a hairline underneath comes out tinted when selected
            // and white everywhere else.
            .border(1.dp, EdgeColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                scope.launch { scale.bounce() }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = lerp(WbwInkLight, WbwGold, emphasis),
            // The glyph bounces, not the button: scaling the whole pill would pump
            // the blur behind it, which reads as the glass wobbling.
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}
