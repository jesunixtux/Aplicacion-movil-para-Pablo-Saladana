package cl.duoc.coegio_pablo_saladana.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.ProfilePhotoStorage
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.data.remote.HolidayRemoteDataSource
import cl.duoc.coegio_pablo_saladana.data.repository.NotebookRepository
import cl.duoc.coegio_pablo_saladana.data.repository.StudentEntryRepository
import cl.duoc.coegio_pablo_saladana.data.repository.UserRepository
import cl.duoc.coegio_pablo_saladana.domain.error.AppErrors
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination
import cl.duoc.coegio_pablo_saladana.ui.navigation.destinationForSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gestiona el estado principal de la aplicación y coordina repositorios y navegación. */
class AppViewModel(
    private val appContext: Context,
    private val userRepository: UserRepository,
    private val notebookRepository: NotebookRepository,
    private val studentEntryRepository: StudentEntryRepository,
    private val profilePhotoStorage: ProfilePhotoStorage
) : ViewModel() {

    var uiState by mutableStateOf(AppUiState())
        private set

    private var initialized = false

    fun initialize(
        savedDestination: AuthDestination? = null,
        savedNotebookId: Long? = null,
        savedEditingEntryId: Long? = null
    ) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val restoredSession = userRepository.getActiveSession()
            val localNotebooks = notebookRepository.findAll()
            val allStudents = userRepository.findAllStudents()
            val studentEntries = restoredSession?.takeIf { it.role == UserRole.Student }
                ?.let { studentEntryRepository.findLocalByStudent(it.email) }
                ?: emptyList()
            val teacherEntries = restoredSession?.takeIf { it.role == UserRole.Teacher }
                ?.let { studentEntryRepository.findLocalSubmittedByTeacher(it.email) }
                ?: emptyList()
            val photoUri = restoredSession?.let {
                withContext(Dispatchers.IO) { profilePhotoStorage.uriFor(it.email) }
            }
            val notebookId = savedNotebookId ?: uiState.selectedNotebookId
            val editingId = savedEditingEntryId ?: uiState.editingEntryId
            val resolvedDestination = destinationForSession(
                savedDestination,
                restoredSession?.role,
                localNotebooks.any { it.id == notebookId } ||
                    studentEntries.any { it.id == editingId }
            )
            uiState = uiState.copy(
                currentAccount = restoredSession,
                notebooks = localNotebooks,
                availableStudents = allStudents,
                selectedNotebookId = notebookId,
                editingEntryId = editingId,
                studentEntries = studentEntries,
                teacherEntries = teacherEntries,
                profilePhotoUri = photoUri,
                destination = resolvedDestination,
                isRestoringSession = false
            )

            // Mantiene la lectura local; la integración del nuevo modelo con Spring Boot se hará después.
            val syncedNotebooks = notebookRepository.syncWithBackend()
            val syncedStudentEntries = restoredSession?.takeIf { it.role == UserRole.Student }
                ?.let { studentEntryRepository.findByStudent(it.email) } ?: emptyList()
            val syncedTeacherEntries = restoredSession?.takeIf { it.role == UserRole.Teacher }
                ?.let { studentEntryRepository.findSubmittedByTeacher(it.email) } ?: emptyList()
            val fetchedHolidays = HolidayRemoteDataSource.fetchHolidays()
            uiState = uiState.copy(
                notebooks = syncedNotebooks,
                holidays = fetchedHolidays,
                studentEntries = syncedStudentEntries,
                teacherEntries = syncedTeacherEntries,
                syncWarning = HolidayRemoteDataSource.lastError?.let {
                    "$it Se muestra el calendario chileno sin conexión."
                }
            )
        }
    }

    fun navigateTo(destination: AuthDestination) {
        uiState = uiState.copy(destination = destination)
    }

    fun handleBack() {
        val nextDestination = when (uiState.destination) {
            AuthDestination.Login, AuthDestination.Register -> AuthDestination.Welcome
            AuthDestination.StudentNotebook, AuthDestination.StudentHistory -> AuthDestination.StudentDashboard
            AuthDestination.StudentEntry -> if (uiState.editingEntry != null) {
                uiState = uiState.copy(editingEntryId = null)
                AuthDestination.StudentHistory
            } else {
                AuthDestination.StudentNotebook
            }
            AuthDestination.TeacherCreateNotebook -> AuthDestination.TeacherDashboard
            AuthDestination.Profile -> if (uiState.currentAccount?.role == UserRole.Teacher) {
                AuthDestination.TeacherDashboard
            } else {
                AuthDestination.StudentDashboard
            }
            else -> AuthDestination.Welcome
        }
        uiState = uiState.copy(destination = nextDestination)
    }

    fun onUserLoggedIn(account: UserAccount) {
        userRepository.saveSession(account)
        uiState = uiState.copy(currentAccount = account)
        viewModelScope.launch {
            val photoUri = withContext(Dispatchers.IO) {
                profilePhotoStorage.uriFor(account.email)
            }
            val syncedNotebooks = notebookRepository.syncWithBackend()
            val allStudents = userRepository.findAllStudents()
            val studentEntries = if (account.role == UserRole.Student) {
                studentEntryRepository.findByStudent(account.email)
            } else {
                emptyList()
            }
            val teacherEntries = if (account.role == UserRole.Teacher) {
                studentEntryRepository.findSubmittedByTeacher(account.email)
            } else {
                emptyList()
            }
            uiState = uiState.copy(
                profilePhotoUri = photoUri,
                notebooks = syncedNotebooks,
                availableStudents = allStudents,
                studentEntries = studentEntries,
                teacherEntries = teacherEntries
            )
        }
    }

    fun onNotebookSelected(notebook: NotebookEntity) {
        uiState = uiState.copy(
            selectedNotebookId = notebook.id,
            editingEntryId = null,
            destination = AuthDestination.StudentNotebook
        )
    }

    fun onStartNewEntry() {
        uiState = uiState.copy(
            editingEntryId = null,
            destination = AuthDestination.StudentEntry
        )
    }

    fun onEditDraftEntry(draftEntry: StudentEntryEntity) {
        uiState = uiState.copy(
            selectedNotebookId = draftEntry.notebookId,
            editingEntryId = draftEntry.id,
            destination = AuthDestination.StudentEntry
        )
    }

    /** Devuelve null si la bitácora se publicó, o el motivo específico del fallo. */
    suspend fun publishNotebook(draft: NotebookDraft): String? {
        val teacher = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        val created = notebookRepository.create(draft, teacher)
            ?: return notebookRepository.lastError ?: "No se pudo publicar la bitácora."
        uiState = uiState.copy(notebooks = listOf(created) + uiState.notebooks)
        return null
    }

    suspend fun updateNotebookAssignments(
        notebookId: Long,
        course: String,
        studentEmails: Set<String>
    ): String? {
        val teacher = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        val updated = notebookRepository.updateAssignments(
            notebookId = notebookId,
            teacher = teacher,
            course = course,
            studentEmails = studentEmails
        ) ?: return notebookRepository.lastError ?: "No se pudo actualizar la asignación de estudiantes."
        uiState = uiState.copy(
            notebooks = uiState.notebooks.map {
                if (it.id == updated.id) updated else it
            }
        )
        return null
    }

    suspend fun closeNotebook(notebookId: Long): String? {
        val teacher = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        val updated = notebookRepository.close(notebookId, teacher)
            ?: return notebookRepository.lastError ?: "No se pudo cerrar la bitácora."
        uiState = uiState.copy(notebooks = uiState.notebooks.map {
            if (it.id == updated.id) updated else it
        })
        return null
    }

    suspend fun setNotebookReminders(notebookId: Long, enabled: Boolean): String? {
        val teacher = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        val updated = notebookRepository.setRemindersEnabled(notebookId, teacher, enabled)
            ?: return notebookRepository.lastError ?: "No se pudo cambiar el recordatorio."
        uiState = uiState.copy(notebooks = uiState.notebooks.map {
            if (it.id == updated.id) updated else it
        })
        return null
    }

    suspend fun saveStudentEntry(draft: StudentEntryDraft, isDraft: Boolean): String? {
        val student = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        val existingDraft = uiState.editingEntry
        val notebook = uiState.selectedNotebook
        val savedEntry = when {
            existingDraft != null ->
                studentEntryRepository.updateDraft(existingDraft.id, draft, isDraft, student)
            notebook != null ->
                studentEntryRepository.create(draft, isDraft, student, notebook)
            else -> return "No se encontró la bitácora del registro. Vuelve al panel y ábrela de nuevo."
        } ?: return studentEntryRepository.lastError ?: "No se pudo guardar el registro."

        val updatedList = if (existingDraft != null) {
            uiState.studentEntries.map { if (it.id == savedEntry.id) savedEntry else it }
        } else {
            listOf(savedEntry) + uiState.studentEntries
        }
        uiState = uiState.copy(
            studentEntries = updatedList,
            editingEntryId = null
        )
        return null
    }

    suspend fun reviewEntry(entryId: Long, feedback: String): String? {
        val teacher = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        val updated = studentEntryRepository.markReviewed(entryId, teacher, feedback)
            ?: return studentEntryRepository.lastError ?: "No se pudo marcar el registro como revisado."
        uiState = uiState.copy(
            teacherEntries = uiState.teacherEntries.map {
                if (it.id == updated.id) updated else it
            }
        )
        return null
    }

    fun updateProfilePhoto(bitmap: Bitmap) {
        val account = uiState.currentAccount ?: return
        viewModelScope.launch {
            val savedUri = withContext(Dispatchers.IO) {
                profilePhotoStorage.save(account.email, bitmap)
            }
            uiState = if (savedUri != null) {
                uiState.copy(profilePhotoUri = savedUri, profilePhotoError = null)
            } else {
                uiState.copy(
                    profilePhotoError = profilePhotoStorage.lastError
                        ?: "No se pudo guardar la foto de perfil."
                )
            }
        }
    }

    suspend fun assignCourse(course: String, subject: String): String? {
        val teacher = uiState.currentAccount ?: return AppErrors.NOT_LOGGED_IN
        if (teacher.role != UserRole.Teacher) return "Solo los docentes pueden tomar un curso."
        if (teacher.assignedCourse != null) return "Ya tienes un curso asignado; no puedes tomar otro."
        val success = userRepository.assignCourseToTeacher(teacher.email, course, subject)
        if (success) {
            val updatedAccount = teacher.copy(assignedCourse = course, assignedSubject = subject)
            userRepository.saveSession(updatedAccount)
            uiState = uiState.copy(currentAccount = updatedAccount)
            return null
        }
        return userRepository.lastError ?: "No se pudo asignar el curso."
    }

    fun logout() {
        userRepository.clearSession()
        uiState = uiState.copy(
            currentAccount = null,
            selectedNotebookId = null,
            editingEntryId = null,
            studentEntries = emptyList(),
            teacherEntries = emptyList(),
            profilePhotoUri = null,
            profilePhotoError = null,
            syncWarning = null,
            destination = AuthDestination.Welcome
        )
    }

    companion object {
        fun factory(
            appContext: Context,
            userRepository: UserRepository,
            notebookRepository: NotebookRepository,
            studentEntryRepository: StudentEntryRepository,
            profilePhotoStorage: ProfilePhotoStorage
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(
                    appContext = appContext,
                    userRepository = userRepository,
                    notebookRepository = notebookRepository,
                    studentEntryRepository = studentEntryRepository,
                    profilePhotoStorage = profilePhotoStorage
                ) as T
            }
        }
    }
}
