package cl.duoc.coegio_pablo_saladana.ui.student

import cl.duoc.coegio_pablo_saladana.domain.validation.StudentEntryValidator

/** Estado exclusivo del formulario de registro de clase del estudiante. */
data class StudentEntryUiState(
    val date: String = "",
    val content: String = "",
    val activity: String = "",
    val learning: String = "",
    val doubts: String = "",
    val message: String? = null,
    val attemptedSubmit: Boolean = false,
    val isLoading: Boolean = false,
    val errors: StudentEntryValidator.Errors = StudentEntryValidator.Errors()
)
