package cl.duoc.coegio_pablo_saladana.domain.validation

import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolSubjects
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses

/** Exige los campos necesarios para publicar una bitácora. */
object NotebookValidator {
    data class Errors(
        val title: Boolean = false,
        val purpose: Boolean = false,
        val instructions: Boolean = false,
        val questions: Boolean = false,
        val course: Boolean = false,
        val subject: Boolean = false,
        val period: Boolean = false
    ) {
        val isValid: Boolean get() = !title && !purpose && !instructions && !questions && !course && !subject && !period
    }

    fun validate(draft: NotebookDraft): Errors = Errors(
        title = draft.title.isBlank(),
        purpose = draft.purpose.isBlank(),
        instructions = draft.instructions.isBlank(),
        questions = draft.questions.isBlank(),
        course = draft.course !in SchoolCourses.STUDENT_COURSES,
        subject = draft.subject !in SchoolSubjects.ALL,
        period = draft.month !in 1..12 || draft.year !in 2020..2100
    )

    fun isValid(draft: NotebookDraft): Boolean = validate(draft).isValid
}
