package cl.duoc.coegio_pablo_saladana.ui.auth

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.ui.components.AppMotionDefaults
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination
import cl.duoc.coegio_pablo_saladana.ui.profile.ProfileScreen
import cl.duoc.coegio_pablo_saladana.ui.student.StudentDashboard
import cl.duoc.coegio_pablo_saladana.ui.student.StudentEntryForm
import cl.duoc.coegio_pablo_saladana.ui.student.StudentHistory
import cl.duoc.coegio_pablo_saladana.ui.student.StudentNotebookDetail
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherCreateNotebook
import cl.duoc.coegio_pablo_saladana.ui.teacher.TeacherDashboard

/** Enruta las pantallas según el estado de sesión y las acciones del usuario. */
@Composable
fun AuthScreens(
    destination: AuthDestination,
    authViewModel: AuthViewModel,
    currentAccount: UserAccount?,
    notebooks: List<NotebookEntity>,
    availableStudents: List<UserAccount> = emptyList(),
    selectedNotebook: NotebookEntity?,
    editingEntry: StudentEntryEntity?,
    studentEntries: List<StudentEntryEntity>,
    teacherEntries: List<StudentEntryEntity>,
    holidays: List<ChileanHoliday>,
    profilePhotoUri: Uri?,
    darkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onDestinationChange: (AuthDestination) -> Unit,
    onUserLoggedIn: (UserAccount) -> Unit,
    onNotebookSelected: (NotebookEntity) -> Unit,
    onStartNewEntry: () -> Unit,
    onEditDraftEntry: (StudentEntryEntity) -> Unit,
    onNotebookPublished: suspend (NotebookDraft) -> Boolean,
    onAssignCourse: suspend (String, String) -> Boolean,
    onUpdateNotebookAssignments: suspend (Long, String, Set<String>) -> Boolean = { _, _, _ -> false },
    onCloseNotebook: suspend (Long) -> Boolean,
    onSetNotebookReminders: suspend (Long, Boolean) -> Boolean,
    onStudentEntrySaved: suspend (StudentEntryDraft, Boolean) -> Boolean,
    onReviewEntry: suspend (Long, String) -> Boolean,
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
                course = currentAccount?.course ?: SchoolCourses.DEFAULT_STUDENT_COURSE,
                gender = currentAccount?.gender ?: UserGender.Other,
                profilePhotoUri = profilePhotoUri,
                notebooks = notebooks.filter { notebook ->
                    currentAccount?.let { account -> notebook.isAccessibleBy(account) } == true
                },
                entries = studentEntries.filter { it.studentEmail.equals(currentAccount?.email, ignoreCase = true) },
                holidays = holidays,
                onLogout = onUserLoggedOut,
                onOpenProfile = { onDestinationChange(AuthDestination.Profile) },
                onOpenNotebook = onNotebookSelected,
                onOpenHistory = { onDestinationChange(AuthDestination.StudentHistory) }
            )
            AuthDestination.StudentNotebook -> StudentNotebookDetail(
                notebook = selectedNotebook?.takeIf {
                    currentAccount != null && it.isAccessibleBy(currentAccount)
                },
                onBack = { onDestinationChange(AuthDestination.StudentDashboard) },
                onCreateEntry = onStartNewEntry
            )
            AuthDestination.StudentEntry -> StudentEntryForm(
                initialEntry = editingEntry?.takeIf {
                    it.studentEmail.equals(currentAccount?.email, ignoreCase = true)
                },
                notebookPeriod = selectedNotebook?.periodLabel,
                holidays = holidays,
                onBack = {
                    onDestinationChange(
                        if (editingEntry != null) AuthDestination.StudentHistory
                        else AuthDestination.StudentNotebook
                    )
                },
                onSave = onStudentEntrySaved,
                onSaved = { onDestinationChange(AuthDestination.StudentHistory) }
            )
            AuthDestination.StudentHistory -> StudentHistory(
                entries = studentEntries.filter { it.studentEmail.equals(currentAccount?.email, ignoreCase = true) },
                onEditDraft = onEditDraftEntry,
                onBack = { onDestinationChange(AuthDestination.StudentDashboard) }
            )
            AuthDestination.TeacherDashboard -> TeacherDashboard(
                fullName = currentAccount?.fullName ?: "Docente",
                rut = currentAccount?.rut ?: "RUT no disponible",
                gender = currentAccount?.gender ?: UserGender.Other,
                profilePhotoUri = profilePhotoUri,
                assignedCourse = currentAccount?.assignedCourse,
                assignedSubject = currentAccount?.assignedSubject,
                availableCourses = SchoolCourses.STUDENT_COURSES,
                notebooks = notebooks.filter { it.teacherEmail.equals(currentAccount?.email, ignoreCase = true) &&
                    it.course == currentAccount?.assignedCourse && it.subject == currentAccount?.assignedSubject },
                availableStudents = availableStudents,
                submittedEntries = teacherEntries.filter { entry ->
                    notebooks.any { notebook ->
                        notebook.id == entry.notebookId &&
                            notebook.teacherEmail.equals(currentAccount?.email, ignoreCase = true) &&
                            notebook.course == currentAccount?.assignedCourse &&
                            notebook.subject == currentAccount?.assignedSubject
                    }
                },
                holidays = holidays,
                onLogout = onUserLoggedOut,
                onOpenProfile = { onDestinationChange(AuthDestination.Profile) },
                onCreateNotebook = { onDestinationChange(AuthDestination.TeacherCreateNotebook) },
                onAssignCourse = onAssignCourse,
                onUpdateNotebookAssignments = onUpdateNotebookAssignments,
                onCloseNotebook = onCloseNotebook,
                onSetNotebookReminders = onSetNotebookReminders,
                onReviewEntry = onReviewEntry
            )
            AuthDestination.TeacherCreateNotebook -> TeacherCreateNotebook(
                availableStudents = availableStudents,
                teacherCourse = currentAccount?.assignedCourse,
                teacherSubject = currentAccount?.assignedSubject,
                onBack = { onDestinationChange(AuthDestination.TeacherDashboard) },
                onPublish = onNotebookPublished,
                onPublished = { onDestinationChange(AuthDestination.TeacherDashboard) }
            )
            AuthDestination.Profile -> ProfileScreen(
                fullName = currentAccount?.fullName ?: "Usuario",
                rut = currentAccount?.rut ?: "RUT no disponible",
                role = currentAccount?.role ?: UserRole.Student,
                course = currentAccount?.course ?: SchoolCourses.DEFAULT_STUDENT_COURSE,
                photoUri = profilePhotoUri,
                darkMode = darkMode,
                onToggleDarkMode = onToggleDarkMode,
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
internal fun AuthContainer(content: @Composable () -> Unit) {
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
