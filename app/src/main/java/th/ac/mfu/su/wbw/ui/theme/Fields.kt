package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A field in the participant pass's language: a tracked micro-label, and the value
 * directly beneath it on the pane itself.
 *
 * The pass is the app's best-looking surface and it holds a dozen label/value pairs
 * without a single box drawn around any of them — the hairline rules between rows do all
 * the separating. A login form is the same thing with two of the values editable, so it
 * gets the same treatment: no container per field, no border per field, and certainly no
 * floating label flying up through a notch cut in an outline.
 *
 * White at the pass's four strengths, in both themes, because this sits on a pane over
 * the artwork and not on a card.
 */
@Composable
fun PassField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier) {
        Text(
            label.uppercase(),
            color = PassFaint,
            fontSize = 8.5.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Medium,
        )
        // Built from the app's own typography rather than a bare `TextStyle`.
        //
        // A `TextStyle()` with no family gets the platform default — Roboto — while the
        // placeholder beside it, being a `Text`, inherits the body face from the theme. The
        // field therefore changed typeface the moment you typed into it, which is the kind
        // of mismatch you see immediately and cannot name.
        val entry = MaterialTheme.typography.bodyLarge.copy(
            color = PassInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(top = 6.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    // The same style as the entry, so the placeholder sits exactly where
                    // the first character will land.
                    Text(placeholder, style = entry.copy(color = PassFaint))
                }
                // BasicTextField, not a Material one: the Material fields bring a
                // container, an indicator line and their own padding, all of which would
                // draw a box back around a field that is deliberately open.
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = entry,
                    cursorBrush = SolidColor(PassInk),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/** The pass's hairline rule, for separating rows on a pane. */
@Composable
fun PassRule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(PassHairline))
}

/** OutlinedTextField styling that sits legibly on the frosted-glass / forest sky. */
@Composable
fun wbwTextFieldColors(): TextFieldColors {
    val c = wbwColors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        focusedContainerColor = c.glass,
        unfocusedContainerColor = c.glass,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.glassBorder,
        focusedLabelColor = c.accent,
        unfocusedLabelColor = c.textMuted,
        focusedPlaceholderColor = c.textMuted,
        unfocusedPlaceholderColor = c.textMuted,
        cursorColor = c.accent,
        focusedTrailingIconColor = c.accent,
        unfocusedTrailingIconColor = c.textMuted,
    )
}
