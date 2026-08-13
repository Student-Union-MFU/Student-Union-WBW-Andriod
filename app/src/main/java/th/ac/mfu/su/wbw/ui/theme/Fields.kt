package th.ac.mfu.su.wbw.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/** OutlinedTextField styling that sits legibly on the frosted-glass / forest sky. */
@Composable
fun wbwTextFieldColors(): TextFieldColors {
    val c = wbwColors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        focusedContainerColor = c.glass,
        unfocusedContainerColor = c.glass,
        focusedBorderColor = c.gold,
        unfocusedBorderColor = c.glassBorder,
        focusedLabelColor = c.gold,
        unfocusedLabelColor = c.textMuted,
        focusedPlaceholderColor = c.textMuted,
        unfocusedPlaceholderColor = c.textMuted,
        cursorColor = c.gold,
        focusedTrailingIconColor = c.gold,
        unfocusedTrailingIconColor = c.textMuted,
    )
}
