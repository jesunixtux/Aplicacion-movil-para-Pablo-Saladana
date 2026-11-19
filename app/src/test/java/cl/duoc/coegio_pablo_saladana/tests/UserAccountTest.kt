package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.SchoolCourses
import cl.duoc.coegio_pablo_saladana.domain.model.UserAccount
import cl.duoc.coegio_pablo_saladana.domain.model.UserGender
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserAccountTest {
    private fun account(
        firstName: String = "Ana",
        middleName: String = "",
        paternalSurname: String = "Pérez",
        maternalSurname: String = "Soto"
    ) = UserAccount(
        firstName = firstName,
        middleName = middleName,
        paternalSurname = paternalSurname,
        maternalSurname = maternalSurname,
        rut = "12.345.678-5",
        email = "ana@escuela.cl",
        role = UserRole.Student,
        gender = UserGender.Other
    )

    @Test
    fun fullNameSkipsBlankMiddleName() {
        assertEquals("Ana Pérez Soto", account().fullName)
    }

    @Test
    fun fullNameJoinsAllFourNamesWithSingleSpaces() {
        assertEquals("Ana María Pérez Soto", account(middleName = "María").fullName)
    }

    @Test
    fun fullNameSkipsAnyBlankPart() {
        assertEquals("Pérez Soto", account(firstName = " ", middleName = "").fullName)
    }

    @Test
    fun newAccountsStartWithDefaultCourseAndNoTeacherAssignment() {
        val account = account()
        assertEquals(SchoolCourses.DEFAULT_STUDENT_COURSE, account.course)
        assertNull(account.assignedCourse)
        assertNull(account.assignedSubject)
    }

    @Test
    fun onlyTwoRolesExist() {
        assertEquals(2, UserRole.entries.size)
        assertEquals(UserRole.Teacher, UserRole.valueOf("Teacher"))
        assertEquals(UserRole.Student, UserRole.valueOf("Student"))
    }
}
