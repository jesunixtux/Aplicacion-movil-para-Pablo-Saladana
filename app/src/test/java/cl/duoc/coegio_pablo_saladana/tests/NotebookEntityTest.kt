package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.local.NotebookEntity
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookEntityTest {
    private fun notebook(
        course: String = "5° Básico A",
        assigned: String = "",
        isClosed: Boolean = false,
        month: Int = 3,
        year: Int = 2026
    ) = NotebookEntity(
        id = 1,
        title = "Bitácora",
        purpose = "Propósito",
        instructions = "Instrucciones",
        questions = "",
        teacherEmail = "docente@escuela.cl",
        teacherName = "Docente",
        createdAt = 1L,
        course = course,
        assignedStudentEmails = assigned,
        isClosed = isClosed,
        month = month,
        year = year
    )

    private fun student(
        email: String = "alumno@escuela.cl",
        course: String = "5° Básico A",
        role: UserRole = UserRole.Student
    ) = UserAccount(
        firstName = "Alumno",
        middleName = "",
        paternalSurname = "Prueba",
        maternalSurname = "Uno",
        rut = "12.345.678-5",
        email = email,
        role = role,
        gender = UserGender.Other,
        course = course
    )

    @Test
    fun isOpenIsTheOppositeOfIsClosed() {
        assertTrue(notebook().isOpen)
        assertFalse(notebook(isClosed = true).isOpen)
    }

    @Test
    fun periodLabelPadsMonthAndYear() {
        assertEquals("03/2026", notebook(month = 3).periodLabel)
        assertEquals("12/2026", notebook(month = 12).periodLabel)
        assertEquals("01/2027", notebook(month = 1, year = 2027).periodLabel)
    }

    @Test
    fun assignedStudentsSetTrimsLowercasesAndDropsBlanks() {
        assertEquals(
            setOf("a@x.cl", "b@y.cl"),
            notebook(assigned = " A@x.cl , b@y.cl ,, A@X.CL ").assignedStudentsSet
        )
        assertEquals(emptySet<String>(), notebook(assigned = "").assignedStudentsSet)
        assertEquals(emptySet<String>(), notebook(assigned = " , , ").assignedStudentsSet)
    }

    @Test
    fun teachersNeverAccessAsStudents() {
        assertFalse(notebook().isAccessibleBy(student(role = UserRole.Teacher)))
    }

    @Test
    fun generalOrBlankCourseIsNotAccessibleByCourse() {
        assertFalse(notebook(course = "General").isAccessibleBy(student()))
        assertFalse(notebook(course = "  ").isAccessibleBy(student()))
    }

    @Test
    fun matchingCourseWithoutAssignmentsOpensToTheWholeCourse() {
        assertTrue(notebook().isAccessibleBy(student()))
        assertTrue(notebook().isAccessibleBy(student(email = "otro@escuela.cl")))
    }

    @Test
    fun courseMatchIgnoresCaseAndSurroundingSpaces() {
        assertTrue(notebook().isAccessibleBy(student(course = " 5° básico a ")))
    }

    @Test
    fun differentCourseIsRejectedEvenIfAssigned() {
        assertFalse(
            notebook(assigned = "alumno@escuela.cl")
                .isAccessibleBy(student(course = "6° Básico A"))
        )
    }

    @Test
    fun explicitAssignmentsRestrictAccessToInvitedEmails() {
        val restricted = notebook(assigned = "invitado@escuela.cl")
        assertTrue(restricted.isAccessibleBy(student(email = "invitado@escuela.cl")))
        assertFalse(restricted.isAccessibleBy(student(email = "alumno@escuela.cl")))
    }

    @Test
    fun assignedEmailMatchIgnoresCaseAndSpaces() {
        assertTrue(
            notebook(assigned = " Invitado@Escuela.cl ")
                .isAccessibleBy(student(email = "invitado@escuela.cl"))
        )
    }
}
