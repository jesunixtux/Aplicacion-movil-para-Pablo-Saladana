package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.coegio_pablo_saladana.domain.auth.AuthOutcome
import cl.duoc.coegio_pablo_saladana.domain.auth.AuthProvider
import cl.duoc.coegio_pablo_saladana.domain.auth.RegistrationData
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.AuthFormValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.PersonNameValidator
import cl.duoc.coegio_pablo_saladana.domain.validation.RutValidator
import kotlinx.coroutines.launch

/** Gestiona el estado de los formularios de autenticación y coordina el acceso con UserRepository. */
class AuthViewModel(private val authProvider: AuthProvider) : ViewModel() {

    var loginUiState by mutableStateOf(LoginUiState())
        private set

    var registerUiState by mutableStateOf(RegisterUiState())
        private set

    fun onLoginEmailChange(email: String) {
        loginUiState = loginUiState.copy(
            email = email,
            message = null,
            isError = false,
            errors = AuthFormValidator.login(email, loginUiState.password)
        )
    }

    fun onLoginPasswordChange(password: String) {
        loginUiState = loginUiState.copy(
            password = password,
            message = null,
            isError = false,
            errors = AuthFormValidator.login(loginUiState.email, password)
        )
    }

    fun toggleLoginPasswordVisibility() {
        loginUiState = loginUiState.copy(passwordVisible = !loginUiState.passwordVisible)
    }

    fun submitLogin(onSuccess: (UserAccount) -> Unit) {
        val errors = AuthFormValidator.login(loginUiState.email, loginUiState.password)
        loginUiState = loginUiState.copy(attemptedSubmit = true, errors = errors)
        if (!errors.isValid) {
            loginUiState = loginUiState.copy(
                isError = true,
                message = "Revisa los campos marcados."
            )
            return
        }
        viewModelScope.launch {
            loginUiState = loginUiState.copy(isLoading = true)
            when (val outcome = authProvider.login(loginUiState.email, loginUiState.password)) {
                is AuthOutcome.Failure -> {
                loginUiState = loginUiState.copy(
                    isLoading = false,
                    isError = true,
                    message = outcome.message
                )
                }
                is AuthOutcome.Success -> {
                loginUiState = LoginUiState()
                onSuccess(outcome.account)
                }
            }
        }
    }

    fun onFirstNameChange(value: String) {
        updateRegisterState(registerUiState.copy(firstName = PersonNameValidator.sanitize(value), message = null))
    }

    fun onMiddleNameChange(value: String) {
        updateRegisterState(registerUiState.copy(middleName = PersonNameValidator.sanitize(value), message = null))
    }

    fun onPaternalSurnameChange(value: String) {
        updateRegisterState(registerUiState.copy(paternalSurname = PersonNameValidator.sanitize(value), message = null))
    }

    fun onMaternalSurnameChange(value: String) {
        updateRegisterState(registerUiState.copy(maternalSurname = PersonNameValidator.sanitize(value), message = null))
    }

    fun onRutChange(value: TextFieldValue) {
        val formattedRut = RutValidator.format(value.text)
        val cursor = RutValidator.formattedCursorPosition(value.text, value.selection.end)
        updateRegisterState(
            registerUiState.copy(
                rut = TextFieldValue(formattedRut, TextRange(cursor)),
                message = null
            )
        )
    }

    fun onRegisterEmailChange(value: String) {
        updateRegisterState(registerUiState.copy(email = value, message = null))
    }

    fun onRegisterPasswordChange(value: String) {
        updateRegisterState(registerUiState.copy(password = value, message = null))
    }

    fun onConfirmPasswordChange(value: String) {
        updateRegisterState(registerUiState.copy(confirmPassword = value, message = null))
    }

    fun toggleRegisterPasswordVisibility() {
        registerUiState = registerUiState.copy(passwordVisible = !registerUiState.passwordVisible)
    }

    fun toggleConfirmPasswordVisibility() {
        registerUiState = registerUiState.copy(confirmPasswordVisible = !registerUiState.confirmPasswordVisible)
    }

    fun onRoleSelected(role: UserRole) {
        registerUiState = registerUiState.copy(selectedRole = role)
    }

    fun onGenderSelected(gender: UserGender) {
        registerUiState = registerUiState.copy(selectedGender = gender)
    }

    fun onCourseSelected(course: String) {
        registerUiState = registerUiState.copy(selectedCourse = course)
    }

    fun submitRegister(onSuccess: (UserAccount) -> Unit) {
        val state = registerUiState
        val errors = validateRegister(state)
        registerUiState = state.copy(attemptedSubmit = true, errors = errors)
        if (!errors.isValid) {
            registerUiState = registerUiState.copy(
                isError = true,
                message = "Revisa los campos marcados."
            )
            return
        }
        viewModelScope.launch {
            registerUiState = registerUiState.copy(isLoading = true)
            val outcome = authProvider.register(RegistrationData(
                firstName = state.firstName,
                middleName = state.middleName,
                paternalSurname = state.paternalSurname,
                maternalSurname = state.maternalSurname,
                rut = state.rut.text,
                email = state.email,
                password = state.password,
                role = state.selectedRole,
                gender = state.selectedGender,
                course = state.selectedCourse
            ))
            when (outcome) {
                is AuthOutcome.Success -> {
                    registerUiState = RegisterUiState()
                    onSuccess(outcome.account)
                }
                is AuthOutcome.Failure -> registerUiState = registerUiState.copy(
                    isLoading = false, isError = true, message = outcome.message
                )
            }
        }
    }

    private fun updateRegisterState(next: RegisterUiState) {
        registerUiState = next.copy(errors = validateRegister(next))
    }

    private fun validateRegister(state: RegisterUiState): AuthFormValidator.RegisterErrors =
        AuthFormValidator.register(
            state.firstName,
            state.middleName,
            state.paternalSurname,
            state.maternalSurname,
            state.rut.text,
            state.email,
            state.password,
            state.confirmPassword
        )

    companion object {
        fun factory(authProvider: AuthProvider): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authProvider) as T
                }
            }
    }
}
