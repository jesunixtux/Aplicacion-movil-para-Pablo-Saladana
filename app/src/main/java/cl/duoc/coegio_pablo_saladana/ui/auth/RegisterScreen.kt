package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.UserAccount
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.data.UserRole
import cl.duoc.coegio_pablo_saladana.validation.AuthFormValidator
import cl.duoc.coegio_pablo_saladana.validation.PersonNameValidator
import cl.duoc.coegio_pablo_saladana.validation.RutValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun RegisterScreen(
    authViewModel: AuthViewModel,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var middleName by rememberSaveable { mutableStateOf("") }
    var paternalSurname by rememberSaveable { mutableStateOf("") }
    var maternalSurname by rememberSaveable { mutableStateOf("") }
    var rut by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.Student) }
    var selectedGender by rememberSaveable { mutableStateOf(UserGender.Other) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val errors = AuthFormValidator.register(
        firstName, middleName, paternalSurname, maternalSurname,
        rut.text, email, password, confirmPassword
    )

    AuthContainer {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Text("Usaremos datos ficticios para este MVP académico.")

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Nombre") },
            singleLine = true,
            isError = attemptedSubmit && errors.firstName,
            supportingText = { NameError(firstName, attemptedSubmit && errors.firstName, "Ingresa un nombre válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = middleName,
            onValueChange = { middleName = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Segundo nombre") },
            singleLine = true,
            isError = attemptedSubmit && errors.middleName,
            supportingText = {
                if (attemptedSubmit && errors.middleName) {
                    Text("Usa solo letras, espacios o guiones.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = paternalSurname,
            onValueChange = { paternalSurname = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Apellido paterno") },
            singleLine = true,
            isError = attemptedSubmit && errors.paternalSurname,
            supportingText = { NameError(paternalSurname, attemptedSubmit && errors.paternalSurname, "Ingresa un apellido paterno válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maternalSurname,
            onValueChange = { maternalSurname = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Apellido materno") },
            singleLine = true,
            isError = attemptedSubmit && errors.maternalSurname,
            supportingText = { NameError(maternalSurname, attemptedSubmit && errors.maternalSurname, "Ingresa un apellido materno válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rut,
            onValueChange = {
                val formattedRut = RutValidator.format(it.text)
                val cursor = RutValidator.formattedCursorPosition(it.text, it.selection.end)
                rut = TextFieldValue(formattedRut, TextRange(cursor))
                message = null
            },
            label = { Text("RUT, por ejemplo 12.345.678-5") },
            singleLine = true,
            isError = attemptedSubmit && errors.rut,
            supportingText = {
                if (attemptedSubmit && errors.rut) Text("Ingresa un RUT válido.")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = attemptedSubmit && errors.email,
            supportingText = {
                if (attemptedSubmit && email.isBlank()) Text("Ingresa tu correo electrónico.")
                else if (attemptedSubmit && errors.email) Text("Ingresa un correo válido.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; message = null },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = attemptedSubmit && errors.password,
            supportingText = {
                if (attemptedSubmit && password.isBlank()) Text("Ingresa una contraseña.")
                else if (attemptedSubmit && errors.password) Text("Debe tener al menos 8 caracteres.")
            },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; message = null },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            isError = attemptedSubmit && errors.confirmPassword,
            supportingText = {
                if (attemptedSubmit && confirmPassword.isBlank()) Text("Confirma tu contraseña.")
                else if (attemptedSubmit && errors.confirmPassword) Text("Las contraseñas no coinciden.")
            },
            visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Ocultar confirmación" else "Mostrar confirmación"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Perfil", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoleOption("Estudiante", UserRole.Student, selectedRole) { selectedRole = it }
            RoleOption("Docente", UserRole.Teacher, selectedRole) { selectedRole = it }
        }

        Text("Género", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GenderOption("Masculino", UserGender.Male, selectedGender) { selectedGender = it }
            GenderOption("Femenino", UserGender.Female, selectedGender) { selectedGender = it }
            GenderOption("Otro", UserGender.Other, selectedGender) { selectedGender = it }
        }

        message?.let {
            Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                attemptedSubmit = true
                if (!errors.isValid) {
                    isError = true
                    message = "Revisa los campos marcados."
                } else {
                    scope.launch {
                        isLoading = true
                        delay(500)
                        val account = authViewModel.register(
                            firstName = firstName,
                            middleName = middleName,
                            paternalSurname = paternalSurname,
                            maternalSurname = maternalSurname,
                            rut = rut.text,
                            email = email,
                            password = password,
                            role = selectedRole,
                            gender = selectedGender
                        )
                        if (account != null) {
                            onUserLoggedIn(account)
                            onDestinationChange(
                                if (account.role == UserRole.Student) {
                                    AuthDestination.StudentDashboard
                                } else {
                                    AuthDestination.TeacherDashboard
                                }
                            )
                        } else {
                            isError = true
                            message = "Ya existe una cuenta con ese correo o RUT."
                        }
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
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

