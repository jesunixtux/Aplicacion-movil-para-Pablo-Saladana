package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.domain.model.NotebookDraft
import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.domain.validation.NotebookValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookDraftTest {
    @Test
    fun normalized_trimsFieldsAndKeepsValidDraft() {
        val draft = NotebookDraft(" Lenguaje ", " Aprender ", " Completar ", " ¿Qué aprendí? ")
            .normalized()

        assertEquals("Lenguaje", draft.title)
        assertEquals("Aprender", draft.purpose)
        assertEquals("Completar", draft.instructions)
        assertEquals("¿Qué aprendí?", draft.questions)
        assertTrue(NotebookValidator.isValid(draft))
    }

    @Test
    fun isValid_rejectsMissingRequiredFields() {
        assertFalse(NotebookValidator.isValid(NotebookDraft("", "Propósito", "Instrucciones", "")))
        assertFalse(NotebookValidator.isValid(NotebookDraft("Título", " ", "Instrucciones", "")))
        assertFalse(NotebookValidator.isValid(NotebookDraft("Título", "Propósito", " ", "")))
    }

    @Test
    fun validateIdentifiesOnlyMissingFields() {
        val errors = NotebookValidator.validate(NotebookDraft("Título", " ", "Instrucciones", ""))
        assertFalse(errors.title)
        assertTrue(errors.purpose)
        assertFalse(errors.instructions)
        assertFalse(errors.isValid)
    }

    @Test
    fun notebookAccess_requiresMatchingCourseAndExplicitAssignmentWhenConfigured() {
        val studentCourseA = student("alumno1@escuela.cl", "5° Básico A")
        val studentCourseB = student("alumno2@escuela.cl", "6° Básico B")
        val invitedStudent = student("invitado@escuela.cl", "5° Básico A")

        val notebookForCourseA = NotebookEntity(
            id = 1,
            title = "Matemática",
            purpose = "Álgebra",
            instructions = "Resolver",
            questions = "",
            teacherEmail = "docente@escuela.cl",
            teacherName = "Docente",
            createdAt = 1L,
            course = "5° Básico A",
            assignedStudentEmails = "invitado@escuela.cl"
        )

        assertFalse(notebookForCourseA.isAccessibleBy(studentCourseA))
        assertTrue(notebookForCourseA.isAccessibleBy(invitedStudent))
        assertFalse(notebookForCourseA.isAccessibleBy(studentCourseB))

        val customNotebook = notebookForCourseA.copy(
            course = SchoolCourses.CUSTOM,
            assignedStudentEmails = "alumno2@escuela.cl"
        )
        assertFalse(customNotebook.isAccessibleBy(studentCourseA))
        assertFalse(customNotebook.isAccessibleBy(studentCourseB))
    }

    @Test
    fun monthlyNotebook_requiresValidMonthYearAndGuidingQuestions() {
        val valid = NotebookDraft("Noviembre", "Aprender", "Registrar", "¿Qué aprendiste?", month = 11, year = 2026)
        assertTrue(NotebookValidator.isValid(valid))
        assertFalse(NotebookValidator.isValid(valid.copy(month = 13)))
        assertFalse(NotebookValidator.isValid(valid.copy(year = 0)))
        assertFalse(NotebookValidator.isValid(valid.copy(questions = " ")))
    }

    private fun student(email: String, course: String) = UserAccount(
        firstName = "Estudiante",
        middleName = "",
        paternalSurname = "Prueba",
        maternalSurname = "Uno",
        rut = "12.345.678-5",
        email = email,
        role = UserRole.Student,
        gender = UserGender.Other,
        course = course
    )
}
