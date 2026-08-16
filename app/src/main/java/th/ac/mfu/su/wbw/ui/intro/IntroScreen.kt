package th.ac.mfu.su.wbw.ui.intro

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.home.Bloom
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.theme.PassInk
import th.ac.mfu.su.wbw.ui.theme.WaveLines
import th.ac.mfu.su.wbw.ui.theme.wbwColors

/**
 * The opening screen: the app's flower, opening.
 *
 * It is the same [Bloom] Home draws, run from bud to full bloom once. That is the whole
 * idea — the thing a participant will spend the event growing is the first thing they are
 * shown, so by the time they reach Home the flower is already familiar and the eight
 * check-ins have an obvious point. A separate splash illustration would have been a second
 * piece of art to keep in step with the first.
 *
 * The ground underneath is [ForestBackground] plus [WaveLines], which is exactly what the
 * login screen draws. Nothing moves between the two but the content: the backdrop and the
 * waves are identical in both, so the crossfade in `WbwApp` dissolves a flower into a
 * sign-in form on a ground that never flinches, rather than cutting between two screens.
 *
 * Nothing here is tappable and there is no "get started" button. The screen has one job,
 * it does it in under three seconds, and a button would only ask the participant to
 * confirm that they would like the app to continue starting.
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val colors = wbwColors
    val context = LocalContext.current

    // `onFinished` is read at the end of a delay chain that must not restart, so the
    // effect below is keyed on Unit and reaches the callback through this instead of
    // capturing whichever instance existed when the screen was first composed.
    val finish by rememberUpdatedState(onFinished)

    /**
     * Honour the system's "remove animations" setting.
     *
     * The developer-options animator scale is what accessibility tooling and vestibular
     * settings drive, and at zero it means *skip the animation*, not "play it faster".
     * An intro whose entire content is a three-second animation has nothing to show
     * somebody who has asked not to see one, so it stands aside completely.
     */
    val animationsOff = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f) == 0f
    }

    // Stage 1 is a bud, which is where the story starts. Stage 0 is a bare stem and would
    // have opened the app on an empty screen for a beat.
    var stage by remember { mutableIntStateOf(FirstStage) }

    // The wordmark arrives with the last two stages rather than sitting there from the
    // first frame, so the eye is on the flower while the flower is the thing moving.
    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (stage >= FinalStage - 1) 1f else 0f,
        animationSpec = tween(700),
        label = "introWordmark",
    )

    LaunchedEffect(Unit) {
        if (animationsOff) {
            finish()
            return@LaunchedEffect
        }
        delay(SettleMillis)
        // Stepped rather than jumped straight to the final stage. `Bloom` animates any
        // stage change over a fixed 900ms, so 1 → 5 in one go is the same length as
        // 1 → 2 and comes out as a lurch. Retargeting every 450ms keeps the animation
        // continuous — each step is redirected mid-flight — and lets every stage be seen.
        for (next in FirstStage + 1..FinalStage) {
            stage = next
            delay(StageStepMillis)
        }
        delay(HoldMillis)
        finish()
    }

    ForestBackground {
        Box(Modifier.fillMaxSize()) {
            WaveLines(modifier = Modifier.fillMaxSize(), ink = PassInk, alpha = 0.18f)

            // The wordmark sits above the flower, in the upper third, because that is
            // where the login screen puts it — and this screen dissolves into that one.
            // With it below the flower the crossfade showed the *same wordmark twice*, in
            // two different places, each at half opacity for the length of the fade; the
            // one thing guaranteed to make a dissolve look like a bug. Up here the two
            // land close enough that it reads as the wordmark settling into place while
            // the flower gives way to the form.
            //
            // Weighted spacers rather than fixed dp: the flower has to stay in the middle
            // of the screen on a tall phone and a short one alike, and the wordmark has to
            // stay off the status bar on both.
            Column(
                // 24dp, matching the login screen's own horizontal padding. With 32 here
                // the wordmark came out a few percent narrower than the one it dissolves
                // into, and a logo that changes size across a fade is the thing the eye
                // catches even when it cannot say what moved.
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(0.30f))
                Image(
                    painter = painterResource(R.drawable.logo_wordmark),
                    contentDescription = stringResource(R.string.app_name),
                    colorFilter = ColorFilter.tint(PassInk),
                    modifier = Modifier
                        .alpha(wordmarkAlpha)
                        // The same fraction the login screen uses, so the two are the same
                        // size as well as in the same place.
                        .fillMaxWidth(0.74f)
                        .aspectRatio(LogoAspect),
                )
                Bloom(
                    stage = stage,
                    ink = colors.onBackdrop,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Spacer(Modifier.weight(0.16f))
            }
        }
    }
}

private const val FirstStage = 1
private const val FinalStage = 5

/** A beat before the first petal moves, so the bud is seen as a bud. */
private const val SettleMillis = 260L

/** Between stages. Shorter than `Bloom`'s own 900ms, which is what keeps the motion continuous. */
private const val StageStepMillis = 450L

/** Full bloom, held long enough to register before the screen gives way. */
private const val HoldMillis = 750L

/** The wordmark artwork's own pixel proportions — the same figure the login screen uses. */
private const val LogoAspect = 847f / 473f
