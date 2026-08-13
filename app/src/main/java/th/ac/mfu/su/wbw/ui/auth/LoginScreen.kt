package th.ac.mfu.su.wbw.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import th.ac.mfu.su.wbw.ui.theme.wbwTextFieldColors

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = wbwColors

    ForestBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The event wordmark, replacing the old mark + "WBW" + tagline stack. It
            // already carries the Thai subtitle, so the tagline underneath was saying
            // the same thing twice.
            //
            // Black artwork on transparent, so it is tinted white and used as a mask —
            // the same thing iOS does with `logo_wordmark` over this backdrop.
            // Width-driven at the artwork's own aspect ratio rather than pinned to a
            // height, so it scales with the screen instead of clipping the descender
            // on the Thai line.
            Image(
                painter = painterResource(R.drawable.logo_wordmark),
                contentDescription = stringResource(R.string.app_name),
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(LogoAspect)
                    .padding(bottom = 30.dp),
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsername,
                label = { Text(stringResource(R.string.login_field_username)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = wbwTextFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidthField(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPassword,
                label = { Text(stringResource(R.string.login_field_password)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = wbwTextFieldColors(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidthField().padding(top = 12.dp),
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = colors.danger,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidthField(),
                )
            }

            th.ac.mfu.su.wbw.ui.theme.PillButton(
                text = stringResource(R.string.login_action_submit),
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                loading = state.loading,
                modifier = Modifier.padding(top = 20.dp).fillMaxWidthField(),
            )

            TextButton(onClick = onNavigateToRegister, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.login_link_register), color = colors.accent)
            }
        }
    }
}

// Consistent max field width so the form doesn't stretch edge-to-edge on tablets.
private fun Modifier.fillMaxWidthField(): Modifier = this.width(360.dp)

/** The wordmark artwork's own width:height (847×473 after trimming its empty canvas). */
private const val LogoAspect = 847f / 473f
