package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Crear cuenta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Completa tus datos para registrarte",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Información personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = authViewModel::onFirstNameChange,
                        label = { Text("Nombre") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        isError = state.attemptedSubmit && state.errors.firstName,
                        supportingText = { NameError(state.firstName, state.attemptedSubmit && state.errors.firstName, "Ingresa un nombre válido.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = state.middleName,
                        onValueChange = authViewModel::onMiddleNameChange,
                        label = { Text("Segundo nombre (opcional)") },
                        singleLine = true,
                        isError = state.attemptedSubmit && state.errors.middleName,
                        supportingText = {
                            if (state.attemptedSubmit && state.errors.middleName) {
                                Text("Usa solo letras, espacios o guiones.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.paternalSurname,
                            onValueChange = authViewModel::onPaternalSurnameChange,
                            label = { Text("Apellido paterno") },
                            singleLine = true,
                            isError = state.attemptedSubmit && state.errors.paternalSurname,
                            supportingText = { NameError(state.paternalSurname, state.attemptedSubmit && state.errors.paternalSurname, "Ingresa un apellido.") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = state.maternalSurname,
                            onValueChange = authViewModel::onMaternalSurnameChange,
                            label = { Text("Apellido materno") },
                            singleLine = true,
                            isError = state.attemptedSubmit && state.errors.maternalSurname,
                            supportingText = { NameError(state.maternalSurname, state.attemptedSubmit && state.errors.maternalSurname, "Ingresa un apellido.") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    OutlinedTextField(
                        value = state.rut,
                        onValueChange = authViewModel::onRutChange,
                        label = { Text("RUT") },
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
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = authViewModel::onRegisterEmailChange,
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
                        onValueChange = authViewModel::onRegisterPasswordChange,
                        label = "Contraseña",
                        passwordVisible = state.passwordVisible,
                        onToggleVisibility = authViewModel::toggleRegisterPasswordVisibility,
                        isError = state.attemptedSubmit && state.errors.password,
                        supportingMessage = when {
                            state.attemptedSubmit && state.password.isBlank() -> "Ingresa una contraseña."
                            state.attemptedSubmit && state.errors.password -> "Debe tener al menos 8 caracteres."
                            else -> null
                        },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
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
                        toggleContentDescription = if (state.confirmPasswordVisible) "Ocultar confirmación" else "Mostrar confirmación",
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    
                    Text("Selecciona tu rol", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedRole == UserRole.Student,
                            onClick = { authViewModel.onRoleSelected(UserRole.Student) },
                            label = { Text("Estudiante") },
                            leadingIcon = if (state.selectedRole == UserRole.Student) {
                                { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        
                        FilterChip(
                            selected = state.selectedRole == UserRole.Teacher,
                            onClick = { authViewModel.onRoleSelected(UserRole.Teacher) },
                            label = { Text("Docente") },
                            leadingIcon = if (state.selectedRole == UserRole.Teacher) {
                                { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    
                    Text("Género", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedGender == UserGender.Male,
                            onClick = { authViewModel.onGenderSelected(UserGender.Male) },
                            label = { Text("Masculino") }
                        )
                        FilterChip(
                            selected = state.selectedGender == UserGender.Female,
                            onClick = { authViewModel.onGenderSelected(UserGender.Female) },
                            label = { Text("Femenino") }
                        )
                        FilterChip(
                            selected = state.selectedGender == UserGender.Other,
                            onClick = { authViewModel.onGenderSelected(UserGender.Other) },
                            label = { Text("Otro") }
                        )
                    }

                    state.message?.let {
                        Text(
                            it,
                            color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            TextButton(onClick = { onDestinationChange(AuthDestination.Login) }) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = Color.White)
            }
            
            TextButton(onClick = { onDestinationChange(AuthDestination.Welcome) }) {
                Text("Volver", color = Color.White.copy(alpha = 0.8f))
            }
            
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NameError(value: String, hasError: Boolean, requiredMessage: String) {
    if (hasError) {
        Text(if (value.isBlank()) requiredMessage else "Usa solo letras, espacios o guiones.")
    }
}
