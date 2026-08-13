package th.ac.mfu.su.wbw.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import th.ac.mfu.su.wbw.core.network.onError
import th.ac.mfu.su.wbw.core.network.onSuccess
import th.ac.mfu.su.wbw.data.remote.dto.RegisterRequest
import th.ac.mfu.su.wbw.data.remote.dto.School
import th.ac.mfu.su.wbw.data.repository.AuthRepository
import th.ac.mfu.su.wbw.ui.appContainer

data class RegisterUiState(
    val studentId: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val sex: String = "male",
    val contactPhone: String = "",
    val schoolId: Int? = null,
    val schools: List<School> = emptyList(),
    val waiverAccepted: Boolean = false,
    val consentHealthData: Boolean = false,
    val consentEmergencyTreatment: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = studentId.isNotBlank() &&
            password.length >= 8 &&
            firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            waiverAccepted &&
            !loading
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state = _state.asStateFlow()

    init {
        // Load the school list for the picker (public endpoint).
        viewModelScope.launch {
            authRepository.schools().onSuccess { list ->
                _state.update { it.copy(schools = list) }
            }
        }
    }

    fun onStudentId(v: String) = _state.update { it.copy(studentId = v.filter(Char::isDigit), error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onFirstName(v: String) = _state.update { it.copy(firstName = v, error = null) }
    fun onLastName(v: String) = _state.update { it.copy(lastName = v, error = null) }
    fun onSex(v: String) = _state.update { it.copy(sex = v) }
    fun onContactPhone(v: String) = _state.update { it.copy(contactPhone = v.filter(Char::isDigit)) }
    fun onSchool(id: Int?) = _state.update { it.copy(schoolId = id) }
    fun onWaiver(v: Boolean) = _state.update { it.copy(waiverAccepted = v) }
    fun onConsentHealth(v: Boolean) = _state.update { it.copy(consentHealthData = v) }
    fun onConsentEmergency(v: Boolean) = _state.update { it.copy(consentEmergencyTreatment = v) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val request = RegisterRequest(
                studentId = s.studentId,
                password = s.password,
                profile = RegisterRequest.Profile(
                    firstName = s.firstName.trim(),
                    lastName = s.lastName.trim(),
                    sex = s.sex,
                    contactPhone = s.contactPhone.ifBlank { null },
                    schoolId = s.schoolId,
                ),
                consent = RegisterRequest.Consent(
                    consentHealthData = s.consentHealthData,
                    consentEmergencyTreatment = s.consentEmergencyTreatment,
                    waiverAccepted = s.waiverAccepted,
                ),
            )
            authRepository.register(request)
                .onError { msg -> _state.update { it.copy(loading = false, error = msg) } }
                .onSuccess { /* session flow routes to home */ }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RegisterViewModel(appContainer.authRepository) }
        }
    }
}
