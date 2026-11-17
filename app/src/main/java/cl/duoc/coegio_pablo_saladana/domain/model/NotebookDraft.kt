package cl.duoc.coegio_pablo_saladana.domain.model

/** Datos editables antes de publicar una bitácora. */
data class NotebookDraft(
    val title: String,
    val purpose: String,
    val instructions: String,
    val questions: String,
    val course: String = SchoolCourses.DEFAULT_STUDENT_COURSE,
    val assignedStudentEmails: Set<String> = emptySet(),
    val subject: String = SchoolSubjects.DEFAULT,
    val month: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
    val year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
) {
    fun normalized(): NotebookDraft = copy(
        title = title.trim(),
        purpose = purpose.trim(),
        instructions = instructions.trim(),
        questions = questions.trim(),
        course = course.trim(),
        subject = subject.trim(),
        assignedStudentEmails = assignedStudentEmails
            .map { it.trim().lowercase() }
            .filter(String::isNotBlank)
            .toSet()
    )
}
