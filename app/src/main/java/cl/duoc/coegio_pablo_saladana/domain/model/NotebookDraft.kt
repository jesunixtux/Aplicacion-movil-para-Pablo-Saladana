package cl.duoc.coegio_pablo_saladana.domain.model

/** Datos editables antes de publicar una bitácora. */
data class NotebookDraft(
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String,
    val course: String = SchoolCourses.GENERAL,
    val assignedStudentEmails: Set<String> = emptySet()
) {
    fun normalized(): NotebookDraft = copy(
        title = title.trim(),
        purpose = purpose.trim(),
        instructions = instructions.trim(),
        questions = questions.trim(),
        course = course.trim().ifBlank { SchoolCourses.GENERAL },
        assignedStudentEmails = assignedStudentEmails
            .map { it.trim().lowercase() }
            .filter(String::isNotBlank)
            .toSet()
    )
}
