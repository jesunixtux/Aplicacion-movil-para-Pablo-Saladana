package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.ui.components.PasswordOutlinedTextField
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination

@Composable
internal fun RegisterScreen(
    authViewModel: AuthViewModel,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit
) {
    val state = authViewModel.registerUiState

    AuthContainer {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Text("Usaremos datos ficticios para este MVP académico.")

        OutlinedTextField(
            value = state.firstName,
            onValueChange = authViewModel::onFirstNameChange,
            label = { Text("Nombre") },
            singleLine = true,
            isError = state.attemptedSubmit && state.errors.firstName,
            supportingText = { NameError(state.firstName, state.attemptedSubmit && state.errors.firstName, "Ingresa un nombre válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.middleName,
            onValueChange = authViewModel::onMiddleNameChange,
            label = { Text("Segundo nombre") },
            singleLine = true,
            isError = state.attemptedSubmit && state.errors.middleName,
            supportingText = {
                if (state.attemptedSubmit && state.errors.middleName) {
                    Text("Usa solo letras, espacios o guiones.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.paternalSurname,
            onValueChange = authViewModel::onPaternalSurnameChange,
            label = { Text("Apellido paterno") },
            singleLine = true,
            isError = state.attemptedSubmit && state.errors.paternalSurname,
            supportingText = { NameError(state.paternalSurname, state.attemptedSubmit && state.errors.paternalSurname, "Ingresa un apellido paterno válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.maternalSurname,
            onValueChange = authViewModel::onMaternalSurnameChange,
            label = { Text("Apellido materno") },
            singleLine = true,
            isError = state.attemptedSubmit && state.errors.maternalSurname,
            supportingText = { NameError(state.maternalSurname, state.attemptedSubmit && state.errors.maternalSurname, "Ingresa un apellido materno válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.rut,
            onValueChange = authViewModel::onRutChange,
            label = { Text("RUT, por ejemplo 12.345.678-5") },
            singleLine = true,
            isError = state.attemptedSubmit && state.errors.rut,
            supportingText = {
                if (state.attemptedSubmit && state.errors.rut) Text("Ingresa un RUT válido.")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = authViewModel::onRegisterEmailChange,
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = state.attemptedSubmit && state.errors.email,
            supportingText = {
                if (state.attemptedSubmit && state.email.isBlank()) Text("Ingresa tu correo electrónico.")
                else if (state.attemptedSubmit && state.errors.email) Text("Ingresa un correo válido.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        PasswordOutlinedTextField(
            value = state.password,
            onValueChange = authViewModel::onRegisterPasswordChange,
            label = "Contraseña",
            passwordVisible = state.passwordVisible,
            onToggleVisibility = authViewModel::toggleRegisterPasswordVisibility,
            isError = state.attemptedSubmit && state.errors.password,
            supportingMessage = when {
                state.attemptedSubmit && state.password.isBlank() -> "Ingresa una contraseña."
                state.attemptedSubmit && state.errors.password -> "Debe tener al menos 8 caracteres."
                else -> null
            }
        )
        PasswordOutlinedTextField(
            value = state.confirmPassword,
            onValueChange = authViewModel::onConfirmPasswordChange,
            label = "Confirmar contraseña",
            passwordVisible = state.confirmPasswordVisible,
            onToggleVisibility = authViewModel::toggleConfirmPasswordVisibility,
            isError = state.attemptedSubmit && state.errors.confirmPassword,
            supportingMessage = when {
                state.attemptedSubmit && state.confirmPassword.isBlank() -> "Confirma tu contraseña."
                state.attemptedSubmit && state.errors.confirmPassword -> "Las contraseñas no coinciden."
                else -> null
            },
            toggleContentDescription = if (state.confirmPasswordVisible) "Ocultar confirmación" else "Mostrar confirmación"
        )

        Text("Perfil", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoleOption("Estudiante", UserRole.Student, state.selectedRole, authViewModel::onRoleSelected)
            RoleOption("Docente", UserRole.Teacher, state.selectedRole, authViewModel::onRoleSelected)
        }

        if (state.selectedRole == UserRole.Student) {
            Text("Curso", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SchoolCourses.STUDENT_COURSES.forEach { courseName ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.selectedCourse == courseName,
                            onClick = { authViewModel.onCourseSelected(courseName) }
                        )
                        Text(courseName)
                    }
                }
            }
        }

        Text("Género", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GenderOption("Masculino", UserGender.Male, state.selectedGender, authViewModel::onGenderSelected)
            GenderOption("Femenino", UserGender.Female, state.selectedGender, authViewModel::onGenderSelected)
            GenderOption("Otro", UserGender.Other, state.selectedGender, authViewModel::onGenderSelected)
        }

        state.message?.let {
            Text(it, color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                authViewModel.submitRegister { account ->
                    onUserLoggedIn(account)
                    onDestinationChange(
                        if (account.role == UserRole.Student) AuthDestination.StudentDashboard
                        else AuthDestination.TeacherDashboard
                    )
                }
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Crear cuenta")
            }
        }

        TextButton(onClick = { onDestinationChange(AuthDestination.Login) }) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
        TextButton(onClick = { onDestinationChange(AuthDestination.Welcome) }) {
            Text("Volver")
        }
    }
}

@Composable
private fun RoleOption(
    label: String,
    role: UserRole,
    selectedRole: UserRole,
    onSelected: (UserRole) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedRole == role, onClick = { onSelected(role) })
        Text(label)
    }
}

@Composable
private fun GenderOption(
    label: String,
    gender: UserGender,
    selectedGender: UserGender,
    onSelected: (UserGender) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selectedGender == gender, onClick = { onSelected(gender) })
        Text(label)
    }
}

@Composable
private fun NameError(value: String, hasError: Boolean, requiredMessage: String) {
    if (hasError) {
        Text(if (value.isBlank()) requiredMessage else "Usa solo letras, espacios o guiones.")
    }
}
