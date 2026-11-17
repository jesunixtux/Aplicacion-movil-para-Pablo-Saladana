package cl.duoc.coegio_pablo_saladana.ui.teacher

import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator

/** Estado exclusivo del formulario docente para crear una bitácora, elegir curso y asignar estudiantes. */
data class TeacherNotebookUiState(
    val title: String = "",
    val purpose: String = "",
    val instructions: String = "",
    val questions: String = "",
    val course: String = SchoolCourses.DEFAULT_STUDENT_COURSE,
    val subject: String = SchoolSubjects.DEFAULT,
    val monthText: String = (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1).toString(),
    val yearText: String = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString(),
    val assignedStudentEmails: Set<String> = emptySet(),
    val message: String? = null,
    val attemptedPublish: Boolean = false,
    val loading: Boolean = false,
    val errors: NotebookValidator.Errors = NotebookValidator.validate(
        NotebookDraft("", "", "", "")
    )
)
