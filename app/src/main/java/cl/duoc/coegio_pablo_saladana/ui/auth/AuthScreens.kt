package cl.duoc.coegio_pablo_saladana.ui.auth

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import cl.duoc.coegio_pablo_saladana.data.AuthViewModel
import cl.duoc.coegio_pablo_saladana.data.EmailValidator
import cl.duoc.coegio_pablo_saladana.data.NotebookDraft
import cl.duoc.coegio_pablo_saladana.data.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.RutValidator
import cl.duoc.coegio_pablo_saladana.data.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.data.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.UserAccount
import cl.duoc.coegio_pablo_saladana.data.PersonNameValidator
import cl.duoc.coegio_pablo_saladana.data.UserGender
import cl.duoc.coegio_pablo_saladana.data.UserRole
import cl.duoc.coegio_pablo_saladana.ui.student.StudentDashboard
import cl.duoc.coegio_pablo_saladana.ui.student.StudentEntryForm
import cl.duoc.coegio_pablo_saladana.ui.student.StudentHistory
import cl.duoc.coegio_pablo_saladana.ui.student.StudentNotebookDetail
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherCreateNotebook
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherDashboard
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileScreen
import cl.duoc.coegio_pablo_saladana.ui.components.AppMotionDefaults
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
    Profile
}

@Composable
fun AuthScreens(
    destination: AuthDestination,
    authViewModel: AuthViewModel,
    currentAccount: UserAccount?,
    notebooks: List<NotebookEntity>,
    selectedNotebook: NotebookEntity?,
    studentEntries: List<StudentEntryEntity>,
    profilePhotoUri: Uri?,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit,
    onNotebookSelected: (NotebookEntity) -> Unit,
    onNotebookPublished: suspend (NotebookDraft) -> Boolean,
    onStudentEntrySaved: suspend (StudentEntryDraft, Boolean) -> Boolean,
    onPhotoEdited: (Bitmap) -> Unit,
    onUserLoggedOut: () -> Unit
) {
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (fadeIn(tween(AppMotionDefaults.NavigationDurationMillis)) +
                slideInHorizontally(tween(AppMotionDefaults.NavigationDurationMillis)) { it / 7 } +
                scaleIn(initialScale = 0.985f)) togetherWith
                (fadeOut(tween(AppMotionDefaults.NavigationDurationMillis / 2)) +
                    slideOutHorizontally(tween(AppMotionDefaults.NavigationDurationMillis)) { -it / 10 } +
                    scaleOut(targetScale = 0.99f))
        },
        label = "auth_navigation"
    ) { currentDestination ->
    when (currentDestination) {
        AuthDestination.Welcome -> WelcomeScreen(onDestinationChange)
        AuthDestination.Login -> LoginScreen(authViewModel, onDestinationChange, onUserLoggedIn)
        AuthDestination.Register -> RegisterScreen(authViewModel, onDestinationChange, onUserLoggedIn)
        AuthDestination.StudentDashboard -> StudentDashboard(
            fullName = currentAccount?.fullName ?: "Estudiante",
            rut = currentAccount?.rut ?: "RUT no disponible",
            gender = currentAccount?.gender ?: UserGender.Other,
            profilePhotoUri = profilePhotoUri,
            notebooks = notebooks,
            onLogout = onUserLoggedOut,
            onOpenProfile = { onDestinationChange(AuthDestination.Profile) },
            onOpenNotebook = onNotebookSelected,
            onOpenHistory = { onDestinationChange(AuthDestination.StudentHistory) }
        )
        AuthDestination.StudentNotebook -> StudentNotebookDetail(
            notebook = selectedNotebook,
            onBack = { onDestinationChange(AuthDestination.StudentDashboard) },
            onCreateEntry = { onDestinationChange(AuthDestination.StudentEntry) }
        )
        AuthDestination.StudentEntry -> StudentEntryForm(
            onBack = { onDestinationChange(AuthDestination.StudentNotebook) },
            onSave = onStudentEntrySaved,
            onSaved = { onDestinationChange(AuthDestination.StudentHistory) }
        )
        AuthDestination.StudentHistory -> StudentHistory(studentEntries) {
            onDestinationChange(AuthDestination.StudentDashboard)
        }
        AuthDestination.TeacherDashboard -> TeacherDashboard(
            fullName = currentAccount?.fullName ?: "Docente",
            rut = currentAccount?.rut ?: "RUT no disponible",
            gender = currentAccount?.gender ?: UserGender.Other,
            profilePhotoUri = profilePhotoUri,
            notebooks = notebooks.filter { it.teacherEmail == currentAccount?.email },
            onLogout = onUserLoggedOut,
            onOpenProfile = { onDestinationChange(AuthDestination.Profile) },
            onCreateNotebook = { onDestinationChange(AuthDestination.TeacherCreateNotebook) }
        )
        AuthDestination.TeacherCreateNotebook -> TeacherCreateNotebook(
            onBack = { onDestinationChange(AuthDestination.TeacherDashboard) },
            onPublish = onNotebookPublished,
            onPublished = { onDestinationChange(AuthDestination.TeacherDashboard) }
        )
        AuthDestination.Profile -> ProfileScreen(
            fullName = currentAccount?.fullName ?: "Usuario",
            rut = currentAccount?.rut ?: "RUT no disponible",
            role = currentAccount?.role ?: UserRole.Student,
            photoUri = profilePhotoUri,
            onBack = {
                onDestinationChange(
                    if (currentAccount?.role == UserRole.Teacher) {
                        AuthDestination.TeacherDashboard
                    } else {
                        AuthDestination.StudentDashboard
                    }
                )
            },
            onPhotoEdited = onPhotoEdited,
            onLogout = onUserLoggedOut
        )
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
    authViewModel: AuthViewModel,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthContainer {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        Text("Ingresa con tu cuenta de docente o estudiante.")

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null; isError = false },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = attemptedSubmit && (email.isBlank() || !email.contains("@")),
            supportingText = {
                if (attemptedSubmit && email.isBlank()) Text("Ingresa tu correo electrónico.")
                else if (attemptedSubmit && !email.contains("@")) Text("Ingresa un correo válido.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; message = null; isError = false },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = attemptedSubmit && password.isBlank(),
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
                if (email.isBlank() || password.isBlank()) {
                    isError = true
                    message = "Completa tu correo y contraseña."
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
            enabled = !isLoading && email.contains("@") && password.isNotBlank(),
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
    authViewModel: AuthViewModel,
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var paternalSurname by remember { mutableStateOf("") }
    var maternalSurname by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf(TextFieldValue()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.Student) }
    var selectedGender by remember { mutableStateOf(UserGender.Other) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthContainer {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Text("Usaremos datos ficticios para este MVP académico.")

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Nombre") },
            singleLine = true,
            isError = attemptedSubmit && !PersonNameValidator.isValid(firstName),
            supportingText = { NameError(firstName, attemptedSubmit, "Ingresa un nombre válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = middleName,
            onValueChange = { middleName = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Segundo nombre") },
            singleLine = true,
            isError = attemptedSubmit && middleName.isNotBlank() && !PersonNameValidator.isValid(middleName),
            supportingText = {
                if (attemptedSubmit && middleName.isNotBlank() && !PersonNameValidator.isValid(middleName)) {
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
            isError = attemptedSubmit && !PersonNameValidator.isValid(paternalSurname),
            supportingText = { NameError(paternalSurname, attemptedSubmit, "Ingresa un apellido paterno válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = maternalSurname,
            onValueChange = { maternalSurname = PersonNameValidator.sanitize(it); message = null },
            label = { Text("Apellido materno") },
            singleLine = true,
            isError = attemptedSubmit && !PersonNameValidator.isValid(maternalSurname),
            supportingText = { NameError(maternalSurname, attemptedSubmit, "Ingresa un apellido materno válido.") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rut,
            onValueChange = {
                val formattedRut = RutValidator.format(it.text)
                rut = TextFieldValue(formattedRut, TextRange(formattedRut.length))
                message = null
            },
            label = { Text("RUT, por ejemplo 12.345.678-5") },
            singleLine = true,
            isError = attemptedSubmit && !RutValidator.isValid(rut.text),
            supportingText = {
                if (attemptedSubmit && !RutValidator.isValid(rut.text)) Text("Ingresa un RUT válido.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; message = null },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = attemptedSubmit && (email.isBlank() || !EmailValidator.isValid(email)),
            supportingText = {
                if (attemptedSubmit && email.isBlank()) Text("Ingresa tu correo electrónico.")
                else if (attemptedSubmit && !EmailValidator.isValid(email)) Text("Ingresa un correo válido.")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; message = null },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = attemptedSubmit && password.length < 8,
            supportingText = {
                if (attemptedSubmit && password.isBlank()) Text("Ingresa una contraseña.")
                else if (attemptedSubmit && password.length < 8) Text("Debe tener al menos 8 caracteres.")
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
            isError = attemptedSubmit && (confirmPassword.isBlank() || confirmPassword != password),
            supportingText = {
                if (attemptedSubmit && confirmPassword.isBlank()) Text("Confirma tu contraseña.")
                else if (attemptedSubmit && confirmPassword != password) Text("Las contraseñas no coinciden.")
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
                when {
                    firstName.isBlank() || paternalSurname.isBlank() || maternalSurname.isBlank() ||
                        rut.text.isBlank() || email.isBlank() || password.isBlank() -> {
                        isError = true
                        message = "Completa todos los campos."
                    }
                    !PersonNameValidator.isValid(firstName) ||
                        (middleName.isNotBlank() && !PersonNameValidator.isValid(middleName)) ||
                        !PersonNameValidator.isValid(paternalSurname) ||
                        !PersonNameValidator.isValid(maternalSurname) -> {
                        isError = true
                        message = "Los nombres y apellidos solo pueden contener letras."
                    }
                    !RutValidator.isValid(rut.text) -> {
                        isError = true
                        message = "Ingresa un RUT válido, por ejemplo 12.345.678-5 o 7.654.321-K."
                    }
                    !EmailValidator.isValid(email) -> {
                        isError = true
                        message = "Ingresa un correo válido."
                    }
                    password.length < 8 -> {
                        isError = true
                        message = "La contraseña debe tener al menos 8 caracteres."
                    }
                    confirmPassword != password -> {
                        isError = true
                        message = "Las contraseñas no coinciden."
                    }
                    else -> {
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
                }
            },
            enabled = !isLoading &&
                PersonNameValidator.isValid(firstName) &&
                (middleName.isBlank() || PersonNameValidator.isValid(middleName)) &&
                PersonNameValidator.isValid(paternalSurname) &&
                PersonNameValidator.isValid(maternalSurname) &&
                RutValidator.isValid(rut.text) &&
                EmailValidator.isValid(email) &&
                password.length >= 8 &&
                confirmPassword == password,
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
private fun NameError(value: String, attemptedSubmit: Boolean, requiredMessage: String) {
    if (attemptedSubmit && !PersonNameValidator.isValid(value)) {
        Text(if (value.isBlank()) requiredMessage else "Usa solo letras, espacios o guiones.")
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
