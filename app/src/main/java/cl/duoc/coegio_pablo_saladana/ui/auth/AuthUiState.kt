package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.ui.text.input.TextFieldValue
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.AuthFormValidator

/** Estado exclusivo del formulario de inicio de sesión. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val attemptedSubmit: Boolean = false,
    val errors: AuthFormValidator.LoginErrors = AuthFormValidator.login("", "")
)

/** Estado exclusivo del formulario de registro de usuario. */
data class RegisterUiState(
    val firstName: String = "",
    val middleName: String = "",
    val paternalSurname: String = "",
    val maternalSurname: String = "",
    val rut: TextFieldValue = TextFieldValue(),
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val selectedRole: UserRole = UserRole.Student,
    val selectedGender: UserGender = UserGender.Other,
    val selectedCourse: String = SchoolCourses.DEFAULT_STUDENT_COURSE,
    val message: String? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val attemptedSubmit: Boolean = false,
    val errors: AuthFormValidator.RegisterErrors = AuthFormValidator.register(
        "", "", "", "", "", "", "", ""
    )
)
