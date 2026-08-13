package th.ac.mfu.su.wbw.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
            Image(
                painter = painterResource(R.drawable.ic_wbw_logo),
                contentDescription = null,
                modifier = Modifier.size(92.dp).padding(bottom = 14.dp),
            )
            Text("WBW", style = MaterialTheme.typography.displaySmall, fontFamily = th.ac.mfu.su.wbw.ui.theme.HandLatin, color = colors.textPrimary)
            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
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
                Text(stringResource(R.string.login_link_register), color = colors.gold)
            }
        }
    }
}

// Consistent max field width so the form doesn't stretch edge-to-edge on tablets.
private fun Modifier.fillMaxWidthField(): Modifier = this.width(360.dp)
