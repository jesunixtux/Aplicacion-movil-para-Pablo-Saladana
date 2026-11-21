package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.data.remote.auth.AssignmentDto
import cl.duoc.coegio_pablo_saladana.data.remote.auth.RemoteUserDto
import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteIdentityTest {
    @Test
    fun serverIdentityKeepsVerifiedIdRoleCourseAndAssignment() {
        val account = RemoteUserDto(
            id = 42,
            name = "Docente Demo",
            email = "DOCENTE@DEMO.TEST",
            role = "TEACHER",
            assignments = listOf(AssignmentDto("5° Básico A", "Matemática"))
        ).toAccount()!!

        assertEquals(42L, account.remoteId)
        assertEquals(UserRole.Teacher, account.role)
        assertEquals("docente@demo.test", account.email)
        assertEquals("5° Básico A", account.assignedCourse)
        assertEquals("Matemática", account.assignedSubject)
        assertEquals("", account.rut)
    }

    @Test
    fun inactiveOrUnknownIdentityIsRejected() {
        assertNull(RemoteUserDto(id = 1, name = "A", email = "a@test.cl", role = "ADMIN").toAccount())
        assertNull(RemoteUserDto(id = 1, name = "A", email = "a@test.cl", role = "STUDENT", active = false).toAccount())
        assertNull(RemoteUserDto(id = 1, name = "A", email = "a@test.cl", role = "STUDENT").toAccount())
        assertNull(RemoteUserDto(id = 1, name = "A", email = "a@test.cl", role = "STUDENT", course = "Curso inventado").toAccount())
    }
}
