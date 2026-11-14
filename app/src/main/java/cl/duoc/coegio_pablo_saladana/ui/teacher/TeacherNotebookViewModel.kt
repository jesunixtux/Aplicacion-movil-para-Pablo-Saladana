package cl.duoc.coegio_pablo_saladana.ui.teacher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator
import kotlinx.coroutines.launch

/** Gestiona el estado y validación del formulario docente de publicación de bitácoras. */
class TeacherNotebookViewModel : ViewModel() {

    var uiState by mutableStateOf(TeacherNotebookUiState())
        private set

    fun onTitleChange(value: String) {
        updateState(uiState.copy(title = value, message = null))
    }

    fun onPurposeChange(value: String) {
        updateState(uiState.copy(purpose = value, message = null))
    }

    fun onInstructionsChange(value: String) {
        updateState(uiState.copy(instructions = value, message = null))
    }

    fun onQuestionsChange(value: String) {
        updateState(uiState.copy(questions = value, message = null))
    }

    fun onCourseChange(course: String) {
        updateState(uiState.copy(course = course, message = null))
    }

    fun onToggleStudent(email: String) {
        val normalized = email.trim().lowercase()
        val updated = if (normalized in uiState.assignedStudentEmails) {
            uiState.assignedStudentEmails - normalized
        } else {
            uiState.assignedStudentEmails + normalized
        }
        updateState(uiState.copy(assignedStudentEmails = updated, message = null))
    }

    fun publish(
        onPublish: suspend (NotebookDraft) -> Boolean,
        onPublished: () -> Unit
    ) {
        val draft = currentDraft(uiState)
        val errors = NotebookValidator.validate(draft)
        uiState = uiState.copy(attemptedPublish = true, errors = errors)
        if (!errors.isValid) {
            uiState = uiState.copy(message = "Revisa los campos marcados.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(loading = true)
            val published = onPublish(draft)
            uiState = uiState.copy(loading = false)
            if (published) {
                uiState = TeacherNotebookUiState()
                onPublished()
            } else {
                uiState = uiState.copy(
                    message = "No se pudo publicar la bitácora. Inténtalo nuevamente."
                )
            }
        }
    }

    private fun updateState(next: TeacherNotebookUiState) {
        uiState = next.copy(errors = NotebookValidator.validate(currentDraft(next)))
    }

    private fun currentDraft(state: TeacherNotebookUiState): NotebookDraft = NotebookDraft(
        title = state.title,
        purpose = state.purpose,
        instructions = state.instructions,
        questions = state.questions,
        course = state.course,
        assignedStudentEmails = state.assignedStudentEmails
    )
}
