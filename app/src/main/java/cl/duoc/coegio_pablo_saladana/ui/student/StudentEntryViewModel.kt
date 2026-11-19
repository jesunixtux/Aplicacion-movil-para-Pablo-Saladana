package cl.duoc.coegio_pablo_saladana.ui.student

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.coegio_pablo_saladana.data.local.StudentEntryEntity
import cl.duoc.coegio_pablo_saladana.domain.model.StudentEntryDraft
import cl.duoc.coegio_pablo_saladana.domain.validation.StudentEntryValidator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** Gestiona el estado y validación del formulario de registro de clase del estudiante. */
class StudentEntryViewModel(initialEntry: StudentEntryEntity? = null) : ViewModel() {

    private var boundEntryId: Long? = initialEntry?.id
    private var hasBoundInitialState: Boolean = initialEntry != null

    var uiState by mutableStateOf(
        createInitialState(initialEntry)
    )
        private set

    fun bindEntry(entry: StudentEntryEntity?) {
        if (!hasBoundInitialState || boundEntryId != entry?.id) {
            boundEntryId = entry?.id
            hasBoundInitialState = true
            uiState = createInitialState(entry)
        }
    }

    fun onDateChange(value: String) {
        updateState(uiState.copy(date = value, message = null))
    }

    fun onContentChange(value: String) {
        updateState(uiState.copy(content = value, message = null))
    }

    fun onActivityChange(value: String) {
        updateState(uiState.copy(activity = value, message = null))
    }

    fun onLearningChange(value: String) {
        updateState(uiState.copy(learning = value, message = null))
    }

    fun onDoubtsChange(value: String) {
        updateState(uiState.copy(doubts = value))
    }

    fun onLinksChange(value: String) = updateState(uiState.copy(links = value, message = null))

    fun onPhotoAdded(uri: String) = updateState(uiState.copy(
        photoUris = (uiState.photoUris.lineSequence().filter(String::isNotBlank).toList() + uri)
            .distinct().joinToString("\n"), message = null
    ))

    fun onFileAdded(uri: String) = updateState(uiState.copy(
        fileUris = (uiState.fileUris.lineSequence().filter(String::isNotBlank).toList() + uri)
            .distinct().joinToString("\n"), message = null
    ))

    fun onRemovePhoto(uri: String) = updateState(uiState.copy(
        photoUris = uiState.photoUris.lineSequence().filter { it.isNotBlank() && it != uri }.joinToString("\n")
    ))

    fun onRemoveFile(uri: String) = updateState(uiState.copy(
        fileUris = uiState.fileUris.lineSequence().filter { it.isNotBlank() && it != uri }.joinToString("\n")
    ))

    fun saveEntry(
        isDraft: Boolean,
        onSave: suspend (StudentEntryDraft, Boolean) -> String?,
        onSaved: () -> Unit
    ) {
        val draft = currentDraft(uiState)
        val errors = StudentEntryValidator.validateSubmission(draft)
        val nextAttemptedSubmit = if (!isDraft) true else uiState.attemptedSubmit
        uiState = uiState.copy(attemptedSubmit = nextAttemptedSubmit, errors = errors)

        if ((!isDraft && !errors.isValid) ||
            (isDraft && !StudentEntryValidator.hasContent(draft))
        ) {
            uiState = uiState.copy(
                message = if (isDraft) {
                    "Escribe al menos un dato antes de guardar el borrador."
                } else {
                    "Revisa los campos marcados."
                }
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val saveError = onSave(draft, isDraft)
            uiState = uiState.copy(isLoading = false)
            if (saveError == null) {
                hasBoundInitialState = false
                boundEntryId = null
                uiState = createInitialState(null)
                onSaved()
            } else {
                uiState = uiState.copy(message = saveError)
            }
        }
    }

    private fun updateState(next: StudentEntryUiState) {
        uiState = next.copy(errors = StudentEntryValidator.validateSubmission(currentDraft(next)))
    }

    private fun currentDraft(state: StudentEntryUiState): StudentEntryDraft = StudentEntryDraft(
        classDate = state.date,
        content = state.content,
        activity = state.activity,
        learning = state.learning,
        doubts = state.doubts,
        photoUris = state.photoUris,
        fileUris = state.fileUris,
        links = state.links
    )

    companion object {
        private fun createInitialState(initialEntry: StudentEntryEntity?): StudentEntryUiState {
            val date = initialEntry?.classDate
                ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val content = initialEntry?.content ?: ""
            val activity = initialEntry?.activity ?: ""
            val learning = initialEntry?.learning ?: ""
            val doubts = initialEntry?.doubts ?: ""
            val photoUris = initialEntry?.photoUris ?: ""
            val fileUris = initialEntry?.fileUris ?: ""
            val links = initialEntry?.links ?: ""
            val draft = StudentEntryDraft(date, content, activity, learning, doubts, photoUris, fileUris, links)
            return StudentEntryUiState(
                date = date,
                content = content,
                activity = activity,
                learning = learning,
                doubts = doubts,
                photoUris = photoUris,
                fileUris = fileUris,
                links = links,
                errors = StudentEntryValidator.validateSubmission(draft)
            )
        }
    }
}
