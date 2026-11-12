package cl.duoc.coegio_pablo_saladana.ui.auth

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import cl.duoc.coegio_pablo_saladana.data.AuthViewModel
import cl.duoc.coegio_pablo_saladana.data.UserAccount
import cl.duoc.coegio_pablo_saladana.data.UserRole
import cl.duoc.coegio_pablo_saladana.validation.AuthFormValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun LoginScreen(
    authViewModel: AuthViewModel,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val errors = AuthFormValidator.login(email, password)

    AuthContainer {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        Text("Ingresa con tu cuenta de docente o estudiante.")

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null; isError = false },
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
            onValueChange = { password = it; message = null; isError = false },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = attemptedSubmit && errors.password,
            supportingText = { if (attemptedSubmit && password.isBlank()) Text("Ingresa tu contraseña.") },
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
                        val account = authViewModel.login(email, password)
                        if (account == null) {
                            isError = true
                            message = "El correo o la contraseña no son correctos."
                        } else if (account.role == UserRole.Student) {
                            onUserLoggedIn(account)
                            onDestinationChange(AuthDestination.StudentDashboard)
                        } else {
                            onUserLoggedIn(account)
                            onDestinationChange(AuthDestination.TeacherDashboard)
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
                Text("Entrar")
            }
        }

        TextButton(onClick = { onDestinationChange(AuthDestination.Register) }) {
            Text("¿No tienes cuenta? Regístrate")
        }
        TextButton(onClick = { onDestinationChange(AuthDestination.Welcome) }) {
            Text("Volver")
        }
    }
}

