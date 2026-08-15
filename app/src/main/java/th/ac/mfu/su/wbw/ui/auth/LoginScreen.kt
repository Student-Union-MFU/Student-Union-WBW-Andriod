package th.ac.mfu.su.wbw.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.DangerDark
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.theme.PassDeepInk
import th.ac.mfu.su.wbw.ui.theme.PassField
import th.ac.mfu.su.wbw.ui.theme.PassInk
import th.ac.mfu.su.wbw.ui.theme.PassMuted
import th.ac.mfu.su.wbw.ui.theme.PassRule
import th.ac.mfu.su.wbw.ui.theme.PassSurface
import th.ac.mfu.su.wbw.ui.theme.WaveLines
import th.ac.mfu.su.wbw.ui.theme.glass

/**
 * The way in, built as a participant pass.
 *
 * The form is one pane with both fields *on* it, separated by a hairline — the pass holds
 * a dozen label/value pairs that way and never draws a box around any of them. Boxing each
 * field, which is what this screen did first, gives you two containers inside a third and
 * a screen made of nested rectangles. The label is the pass's tracked kicker, the value
 * sits directly under it, and the rule does the separating.
 *
 * Everything on the pane is white at the pass's four strengths in both themes, for the
 * same reason the pass is: it sits on the artwork, not on a card.
 *
 * The artwork is [WaveLines] and nothing else — contour lines under the whole screen. The
 * planting that used to sit in two corners is gone: with the wordmark, a pane and one white
 * button already on a photograph, a drawing in the corner was the fourth thing asking to be
 * looked at on a screen with one job.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    var reveal by remember { mutableStateOf(false) }
    val submit: () -> Unit = { viewModel.submit(); keyboard?.hide() }

    ForestBackground {
        Box(Modifier.fillMaxSize()) {
            // The ground, first and furthest back.
            WaveLines(
                modifier = Modifier.fillMaxSize(),
                ink = PassInk,
                alpha = 0.18f,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The event wordmark. Black artwork on transparent, so it is tinted white
                // and used as a mask — the same thing iOS does with `logo_wordmark` over
                // this backdrop. Width-driven at its own aspect ratio rather than pinned to
                // a height, so it scales with the screen instead of clipping the descender
                // on the Thai line.
                Image(
                    painter = painterResource(R.drawable.logo_wordmark),
                    contentDescription = stringResource(R.string.app_name),
                    colorFilter = ColorFilter.tint(PassInk),
                    modifier = Modifier
                        .form()
                        .fillMaxWidth(0.74f)
                        .aspectRatio(LogoAspect),
                )

                Spacer(Modifier.height(26.dp))

                Column(
                    Modifier
                        .form()
                        // An explicit hairline rather than the themed default: `glass`
                        // falls back to `glassBorder`, which is black at 7% in light theme
                        // and disappears on a pane this dark. A pane needs a lit edge to
                        // read as glass at all — it is the edge that says there is a piece
                        // of something there, not the fill.
                        .glass(
                            RoundedCornerShape(PassCorner),
                            fill = PassSurface,
                            border = PassEdge,
                            elevation = 18.dp,
                        )
                        .padding(start = 22.dp, end = 14.dp, top = 20.dp, bottom = 22.dp),
                ) {
                    Heading(stringResource(R.string.login_greeting))

                    Spacer(Modifier.height(20.dp))

                    PassField(
                        label = stringResource(R.string.login_field_username),
                        value = state.username,
                        onValueChange = viewModel::onUsername,
                        placeholder = stringResource(R.string.login_placeholder_username),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    PassRule(Modifier.padding(top = 16.dp, bottom = 16.dp))

                    PassField(
                        label = stringResource(R.string.login_field_password),
                        value = state.password,
                        onValueChange = viewModel::onPassword,
                        placeholder = stringResource(R.string.login_placeholder_password),
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailing = {
                            // Worth the pixels on a field nobody can proofread: this form
                            // is filled in outdoors, one-handed, often in gloves.
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .tapNoRipple { reveal = !reveal },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    stringResource(
                                        if (reveal) R.string.login_password_hide else R.string.login_password_show,
                                    ),
                                    tint = PassMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }

                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        // DangerDark, not the themed pair: this sits on the backdrop, and
                        // the light-theme oxblood disappears into the artwork.
                        color = DangerDark,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.form().padding(top = 14.dp),
                    )
                }

                // The pass carries exactly one solid-filled element — its QR block, white
                // on the glass. This is the login screen's, for the same reason: one thing
                // here is meant to be pressed, and everything else is a surface.
                SignInButton(
                    text = stringResource(R.string.login_action_submit),
                    enabled = state.canSubmit,
                    loading = state.loading,
                    onClick = submit,
                    modifier = Modifier.form().padding(top = 20.dp),
                )

                Row(
                    Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.login_no_account),
                        color = PassMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.login_action_register),
                        color = PassInk,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .tapNoRipple(onNavigateToRegister)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * The pane's heading.
 *
 * Set larger than the pass's 8.5sp kicker, and in a brighter white. A kicker is a label on
 * a value — "BIB NUMBER" over a number — and this one has no value under it: it is the
 * title of the form, and the only line of type on the pane that is not a field.
 */
@Composable
private fun Heading(text: String) {
    Text(
        text.uppercase(),
        color = PassMuted,
        fontSize = 13.sp,
        letterSpacing = 2.4.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SignInButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PassCorner)
    Box(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(if (enabled) PassInk else PassInk.copy(alpha = 0.18f))
            // The edge matters most when the button is disabled and nearly transparent:
            // without it an empty form has a hole where its action should be.
            .border(1.dp, if (enabled) Color.Transparent else PassEdge, shape)
            .tapNoRipple { if (enabled && !loading) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PassDeepInk)
        } else {
            Text(
                text.uppercase(),
                color = if (enabled) PassDeepInk else PassInk.copy(alpha = 0.55f),
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * One width for everything in the form.
 *
 * Capped rather than fixed: the old `width(360.dp)` was wider than a small handset's
 * content area, so on anything narrower than a Pixel the fields ran under the padding.
 */
private fun Modifier.form(): Modifier = this.fillMaxWidth().widthIn(max = 380.dp)

/** Tap with no ripple — a ripple on glass paints a grey disc over the refraction. */
private fun Modifier.tapNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

/** The pass's corner, so the pane and the button below it are cut the same way. */
private val PassCorner = 22.dp

/**
 * The lit edge on every pane here.
 *
 * A touch brighter than the pass's own [PassHairline], which is tuned for rules *inside* a
 * panel. An outer edge has the backdrop on one side of it and has to survive the photograph
 * behind it going pale.
 */
private val PassEdge = Color(0x2EFFFFFF)

/** The wordmark artwork's own width:height (847×473 after trimming its empty canvas). */
private const val LogoAspect = 847f / 473f
