package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.ui.components.PasswordOutlinedTextField
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination

@Composable
internal fun LoginScreen(
    authViewModel: AuthViewModel,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit
) {
    val state = authViewModel.loginUiState

    AuthContainer {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        Text("Ingresa con tu cuenta de docente o estudiante.")

        OutlinedTextField(
            value = state.email,
            onValueChange = authViewModel::onLoginEmailChange,
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
            onValueChange = authViewModel::onLoginPasswordChange,
            label = "Contraseña",
            passwordVisible = state.passwordVisible,
            onToggleVisibility = authViewModel::toggleLoginPasswordVisibility,
            isError = state.attemptedSubmit && state.errors.password,
            supportingMessage = if (state.attemptedSubmit && state.password.isBlank()) "Ingresa tu contraseña." else null
        )

        state.message?.let {
            Text(it, color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                authViewModel.submitLogin { account ->
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
