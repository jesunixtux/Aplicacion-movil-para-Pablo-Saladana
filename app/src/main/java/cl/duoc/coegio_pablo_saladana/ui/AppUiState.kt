package cl.duoc.coegio_pablo_saladana.ui

import android.net.Uri
import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.model.ChileanHoliday
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination

/** Concentra únicamente el estado global de sesión, navegación y datos activos en pantalla. */
data class AppUiState(
    val destination: AuthDestination = AuthDestination.Welcome,
    val currentAccount: UserAccount? = null,
    val notebooks: List<NotebookEntity> = emptyList(),
    val availableStudents: List<UserAccount> = emptyList(),
    val selectedNotebookId: Long? = null,
    val editingEntryId: Long? = null,
    val studentEntries: List<StudentEntryEntity> = emptyList(),
    val teacherEntries: List<StudentEntryEntity> = emptyList(),
    val holidays: List<ChileanHoliday> = emptyList(),
    val profilePhotoUri: Uri? = null,
    val profilePhotoError: String? = null,
    val syncWarning: String? = null,
    val isRestoringSession: Boolean = true
) {
    val selectedNotebook: NotebookEntity?
        get() = notebooks.firstOrNull { it.id == selectedNotebookId }

    val editingEntry: StudentEntryEntity?
        get() = studentEntries.firstOrNull { it.id == editingEntryId }
}
