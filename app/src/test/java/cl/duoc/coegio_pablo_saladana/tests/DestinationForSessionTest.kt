package cl.duoc.coegio_pablo_saladana.tests

import cl.duoc.coegio_pablo_saladana.domain.model.UserRole
import cl.duoc.coegio_pablo_saladana.ui.navigation.AuthDestination
import cl.duoc.coegio_pablo_saladana.ui.navigation.destinationForSession
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
            AuthDestination.TeacherDashboard,
            destinationForSession(AuthDestination.StudentEntry, UserRole.Teacher, true)
        )
        assertEquals(
            AuthDestination.TeacherCreateNotebook,
            destinationForSession(AuthDestination.TeacherCreateNotebook, UserRole.Teacher, false)
        )
    }

    @Test
    fun guestWithoutSavedDestinationLandsOnWelcome() {
        assertEquals(AuthDestination.Welcome, destinationForSession(null, null, false))
        assertEquals(AuthDestination.Welcome, destinationForSession(AuthDestination.Welcome, null, true))
        assertEquals(AuthDestination.Welcome, destinationForSession(AuthDestination.StudentDashboard, null, true))
    }

    @Test
    fun studentWithoutNotebookFallsBackToDashboardFromAnyForeignScreen() {
        assertEquals(AuthDestination.StudentDashboard, destinationForSession(null, UserRole.Student, false))
        assertEquals(AuthDestination.StudentDashboard, destinationForSession(AuthDestination.Login, UserRole.Student, true))
        assertEquals(
            AuthDestination.StudentDashboard,
            destinationForSession(AuthDestination.StudentNotebook, UserRole.Student, false)
        )
        assertEquals(AuthDestination.StudentHistory, destinationForSession(AuthDestination.StudentHistory, UserRole.Student, false))
        assertEquals(AuthDestination.Profile, destinationForSession(AuthDestination.Profile, UserRole.Student, false))
    }

    @Test
    fun teacherWithoutSavedDestinationLandsOnDashboardAndKeepsProfile() {
        assertEquals(AuthDestination.TeacherDashboard, destinationForSession(null, UserRole.Teacher, false))
        assertEquals(AuthDestination.TeacherDashboard, destinationForSession(AuthDestination.Login, UserRole.Teacher, true))
        assertEquals(AuthDestination.Profile, destinationForSession(AuthDestination.Profile, UserRole.Teacher, false))
    }
}
