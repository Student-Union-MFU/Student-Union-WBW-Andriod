package th.ac.mfu.su.wbw.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import th.ac.mfu.su.wbw.R
import th.ac.mfu.su.wbw.ui.theme.ForestBackground
import th.ac.mfu.su.wbw.ui.theme.Ink
import th.ac.mfu.su.wbw.ui.theme.wbwColors
import th.ac.mfu.su.wbw.ui.theme.wbwTextFieldColors

private val FieldShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = wbwColors

    ForestBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.register_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textPrimary,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.studentId,
                    onValueChange = viewModel::onStudentId,
                    label = { Text(stringResource(R.string.register_field_student_id)) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = wbwTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    label = { Text(stringResource(R.string.register_field_password)) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = wbwTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = viewModel::onFirstName,
                        label = { Text(stringResource(R.string.register_field_first_name)) },
                        singleLine = true,
                        shape = FieldShape,
                        colors = wbwTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = viewModel::onLastName,
                        label = { Text(stringResource(R.string.register_field_last_name)) },
                        singleLine = true,
                        shape = FieldShape,
                        colors = wbwTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(stringResource(R.string.register_label_sex), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SexOption(stringResource(R.string.register_sex_male), "male", state.sex, viewModel::onSex)
                    SexOption(stringResource(R.string.register_sex_female), "female", state.sex, viewModel::onSex)
                    SexOption(stringResource(R.string.register_sex_other), "other", state.sex, viewModel::onSex)
                }

                OutlinedTextField(
                    value = state.contactPhone,
                    onValueChange = viewModel::onContactPhone,
                    label = { Text(stringResource(R.string.register_field_contact_phone)) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = wbwTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                SchoolPicker(state, viewModel)

                ConsentRow(state.waiverAccepted, viewModel::onWaiver, stringResource(R.string.register_consent_waiver))
                ConsentRow(state.consentHealthData, viewModel::onConsentHealth, stringResource(R.string.register_consent_health))
                ConsentRow(state.consentEmergencyTreatment, viewModel::onConsentEmergency, stringResource(R.string.register_consent_emergency))

                if (state.error != null) {
                    Text(text = state.error!!, color = colors.danger, style = MaterialTheme.typography.bodySmall)
                }

                th.ac.mfu.su.wbw.ui.theme.PillButton(
                    text = stringResource(R.string.register_title),
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexOption(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    val colors = wbwColors
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.gold,
            selectedLabelColor = Ink,
            labelColor = colors.textPrimary,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchoolPicker(state: RegisterUiState, viewModel: RegisterViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = state.schools.firstOrNull { it.schoolId == state.schoolId }?.name ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.register_field_school)) },
            shape = FieldShape,
            colors = wbwTextFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.schools.forEach { school ->
                DropdownMenuItem(
                    text = { Text(school.name) },
                    onClick = {
                        viewModel.onSchool(school.schoolId)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    val colors = wbwColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = colors.gold, checkmarkColor = Ink, uncheckedColor = colors.textMuted),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}
