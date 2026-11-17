package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))
            
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "Bienvenido",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Inicia sesión para continuar",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(48.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = authViewModel::onLoginEmailChange,
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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
                        supportingMessage = if (state.attemptedSubmit && state.password.isBlank()) "Ingresa tu contraseña." else null,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )

                    state.message?.let {
                        Text(
                            it,
                            color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            TextButton(onClick = { onDestinationChange(AuthDestination.Register) }) {
                Text("¿No tienes cuenta? Regístrate", color = Color.White)
            }
            
            TextButton(onClick = { onDestinationChange(AuthDestination.Welcome) }) {
                Text("Volver", color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
