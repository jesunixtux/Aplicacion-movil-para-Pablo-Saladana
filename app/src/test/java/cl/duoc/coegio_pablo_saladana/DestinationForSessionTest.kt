package cl.duoc.coegio_pablo_saladana

import cl.duoc.coegio_pablo_saladana.data.UserRole
import cl.duoc.coegio_pablo_saladana.ui.auth.AuthDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class DestinationForSessionTest {
    @Test
    fun guestKeepsOpenAuthenticationForm() {
        assertEquals(AuthDestination.Register, destinationForSession(AuthDestination.Register, null, false))
        assertEquals(AuthDestination.Login, destinationForSession(AuthDestination.Login, null, false))
    }

    @Test
    fun studentKeepsEntryOnlyWhenNotebookExists() {
        assertEquals(
            AuthDestination.StudentEntry,
            destinationForSession(AuthDestination.StudentEntry, UserRole.Student, true)
        )
        assertEquals(
            AuthDestination.StudentDashboard,
            destinationForSession(AuthDestination.StudentEntry, UserRole.Student, false)
        )
    }

    @Test
    fun teacherKeepsOwnFormButCannotOpenStudentForm() {
        assertEquals(
            AuthDestination.TeacherCreateNotebook,
            destinationForSession(AuthDestination.TeacherCreateNotebook, UserRole.Teacher, false)
        )
        assertEquals(
            AuthDestination.TeacherDashboard,
            destinationForSession(AuthDestination.StudentEntry, UserRole.Teacher, true)
        )
    }
}
