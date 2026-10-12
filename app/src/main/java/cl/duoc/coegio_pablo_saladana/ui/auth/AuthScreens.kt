package cl.duoc.coegio_pablo_saladana.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.UserRepository
import cl.duoc.coegio_pablo_saladana.data.RutValidator
import cl.duoc.coegio_pablo_saladana.data.UserRole
import cl.duoc.coegio_pablo_saladana.ui.student.StudentDashboard
import cl.duoc.coegio_pablo_saladana.ui.student.StudentEntryForm
import cl.duoc.coegio_pablo_saladana.ui.student.StudentHistory
import cl.duoc.coegio_pablo_saladana.ui.student.StudentNotebookDetail
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherCreateNotebook
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherDashboard
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherReviewEntries
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthDestination {
    Welcome,
    Login,
    Register,
    StudentDashboard,
    StudentNotebook,
    StudentEntry,
    StudentHistory,
    TeacherDashboard,
    TeacherCreateNotebook,
    TeacherReviewEntries
}

@Composable
fun AuthScreens(
    destination: AuthDestination,
    userRepository: UserRepository,
    onDestinationChange: (AuthDestination) -> Unit
) {
    when (destination) {
        AuthDestination.Welcome -> WelcomeScreen(onDestinationChange)
        AuthDestination.Login -> LoginScreen(userRepository, onDestinationChange)
        AuthDestination.Register -> RegisterScreen(userRepository, onDestinationChange)
        AuthDestination.StudentDashboard -> StudentDashboard(
            onLogout = { onDestinationChange(AuthDestination.Welcome) },
            onOpenNotebook = { onDestinationChange(AuthDestination.StudentNotebook) },
            onCreateEntry = { onDestinationChange(AuthDestination.StudentEntry) },
            onOpenHistory = { onDestinationChange(AuthDestination.StudentHistory) }
        )
        AuthDestination.StudentNotebook -> StudentNotebookDetail(
            onBack = { onDestinationChange(AuthDestination.StudentDashboard) },
            onCreateEntry = { onDestinationChange(AuthDestination.StudentEntry) }
        )
        AuthDestination.StudentEntry -> StudentEntryForm(
            onBack = { onDestinationChange(AuthDestination.StudentNotebook) },
            onSaved = { onDestinationChange(AuthDestination.StudentHistory) }
        )
        AuthDestination.StudentHistory -> StudentHistory {
            onDestinationChange(AuthDestination.StudentDashboard)
        }
        AuthDestination.TeacherDashboard -> TeacherDashboard(
            onLogout = { onDestinationChange(AuthDestination.Welcome) },
            onCreateNotebook = { onDestinationChange(AuthDestination.TeacherCreateNotebook) },
            onReviewEntries = { onDestinationChange(AuthDestination.TeacherReviewEntries) }
        )
        AuthDestination.TeacherCreateNotebook -> TeacherCreateNotebook(
            onBack = { onDestinationChange(AuthDestination.TeacherDashboard) },
            onPublished = { onDestinationChange(AuthDestination.TeacherDashboard) }
        )
        AuthDestination.TeacherReviewEntries -> TeacherReviewEntries {
            onDestinationChange(AuthDestination.TeacherDashboard)
        }
    }
}

@Composable
private fun WelcomeScreen(onDestinationChange: (AuthDestination) -> Unit) {
    AuthContainer {
        Text("Bitácora", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Registra y acompaña el aprendizaje de cada clase.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onDestinationChange(AuthDestination.Login) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar sesión")
        }
        OutlinedButton(
            onClick = { onDestinationChange(AuthDestination.Register) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }
    }
}

@Composable
private fun LoginScreen(
    userRepository: UserRepository,
    onDestinationChange: (AuthDestination) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthContainer {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        Text("Ingresa con tu cuenta de docente o estudiante.")

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; message = null },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        message?.let {
            Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    isError = true
                    message = "Completa tu correo y contraseña."
                } else {
                    scope.launch {
                        isLoading = true
                        delay(500)
                        val account = userRepository.login(email, password)
                        if (account == null) {
                            isError = true
                            message = "El correo o la contraseña no son correctos."
                        } else if (account.role == UserRole.Student) {
                            onDestinationChange(AuthDestination.StudentDashboard)
                        } else {
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

@Composable
private fun RegisterScreen(
    userRepository: UserRepository,
    onDestinationChange: (AuthDestination) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var paternalSurname by remember { mutableStateOf("") }
    var maternalSurname by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.Student) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthContainer {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Text("Usaremos datos ficticios para este MVP académico.")

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it; message = null },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = middleName,
            onValueChange = { middleName = it; message = null },
            label = { Text("Segundo nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = paternalSurname,
            onValueChange = { paternalSurname = it; message = null },
            label = { Text("Apellido paterno") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maternalSurname,
            onValueChange = { maternalSurname = it; message = null },
            label = { Text("Apellido materno") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rut,
            onValueChange = { rut = RutValidator.format(it); message = null },
            label = { Text("RUT, por ejemplo 12.345.678-5") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; message = null },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Text("Perfil", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoleOption("Estudiante", UserRole.Student, selectedRole) { selectedRole = it }
            RoleOption("Docente", UserRole.Teacher, selectedRole) { selectedRole = it }
        }

        message?.let {
            Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                when {
                    firstName.isBlank() || paternalSurname.isBlank() || maternalSurname.isBlank() ||
                        rut.isBlank() || email.isBlank() || password.isBlank() -> {
                        isError = true
                        message = "Completa todos los campos."
                    }
                    !RutValidator.isValid(rut) -> {
                        isError = true
                        message = "Ingresa un RUT válido, por ejemplo 12.345.678-5 o 7.654.321-K."
                    }
                    !email.contains("@") -> {
                        isError = true
                        message = "Ingresa un correo válido."
                    }
                    password.length < 6 -> {
                        isError = true
                        message = "La contraseña debe tener al menos 6 caracteres."
                    }
                    else -> {
                        scope.launch {
                            isLoading = true
                            delay(500)
                            if (userRepository.register(
                                    firstName = firstName,
                                    middleName = middleName,
                                    paternalSurname = paternalSurname,
                                    maternalSurname = maternalSurname,
                                    rut = rut,
                                    email = email,
                                    password = password,
                                    role = selectedRole
                                )
                            ) {
                                isError = false
                                message = "Cuenta creada. Ya puedes iniciar sesión."
                            } else {
                                isError = true
                                message = "Ya existe una cuenta con ese correo o RUT."
                            }
                            isLoading = false
                        }
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
private fun AuthContainer(content: @Composable () -> Unit) {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}
